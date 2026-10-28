#!/usr/bin/env bash
set -euo pipefail
export AWS_PAGER=""
export AWS_DEFAULT_REGION="${AWS_REGION:-us-east-1}"

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$root"
stack="${STACK_NAME:-sleapy-demo}"
expected_account="${EXPECTED_AWS_ACCOUNT_ID:?Set EXPECTED_AWS_ACCOUNT_ID to the approved AWS account}"
account="$(aws sts get-caller-identity --query Account --output text)"
if [[ "$account" != "$expected_account" ]]; then
    echo "AWS account mismatch; refusing to deploy to $account." >&2
    exit 1
fi

for tool in jq docker mvn npm; do
    command -v "$tool" >/dev/null || { echo "Missing required tool: $tool" >&2; exit 1; }
done

output() {
    aws cloudformation describe-stacks --stack-name "$stack" \
        --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue | [0]" --output text
}

prefix="$(aws ec2 describe-managed-prefix-lists \
    --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing \
    --query 'PrefixLists[0].PrefixListId' --output text)"
if [[ -z "$prefix" || "$prefix" == None ]]; then
    echo "CloudFront origin-facing prefix list unavailable in this region." >&2
    exit 1
fi

deploy_stack() {
    aws cloudformation deploy --stack-name "$stack" \
        --template-file deploy/aws/stack.yaml --capabilities CAPABILITY_IAM \
        --parameter-overrides "CloudFrontPrefixList=$prefix" "ImageTag=$1" "DesiredCount=$2" \
        --tags Team=sLEAPy Purpose=demo --no-fail-on-empty-changeset
}

echo "Building and testing sLEAPy before creating or updating AWS resources."
maven_args=(-B package)
if [[ -n "${MAVEN_TEST_SELECTOR:-}" ]]; then
    echo "Explicit Maven test selection: $MAVEN_TEST_SELECTOR"
    maven_args+=("-Dtest=$MAVEN_TEST_SELECTOR")
fi
(cd starter/backend && mvn "${maven_args[@]}")
(cd starter/frontend && node --test tests/*.test.cjs && npm run build)
aws cloudformation validate-template --template-body file://deploy/aws/stack.yaml >/dev/null

# Listing stacks distinguishes an absent stack from authentication/permission failures.
status="$(aws cloudformation list-stacks \
    --query "StackSummaries[?StackName=='$stack' && StackStatus!='DELETE_COMPLETE'].StackStatus | [0]" \
    --output text)"
if [[ "$status" == None ]]; then
    deploy_stack bootstrap 0
elif [[ "$status" != CREATE_COMPLETE && "$status" != UPDATE_COMPLETE && "$status" != UPDATE_ROLLBACK_COMPLETE ]]; then
    echo "Stack $stack is $status; inspect CloudFormation events before retrying." >&2
    exit 1
fi

tag="$(date -u +%Y%m%d%H%M%S)-$(git rev-parse --short HEAD)"
registry="$account.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com"
export DOCKER_CONFIG
DOCKER_CONFIG="$(mktemp -d)"
trap 'find "$DOCKER_CONFIG" -depth -delete' EXIT
aws ecr get-login-password | docker login --username AWS --password-stdin "$registry"
for component in backend prices database; do
    case "$component" in
        backend) repository="$(output BackendRepository)" ;;
        prices) repository="$(output PricesRepository)" ;;
        database) repository="$(output SeedRepository)" ;;
    esac
    docker build --platform linux/amd64 -f "deploy/aws/Dockerfile.$component" -t "$repository:$tag" .
    docker push "$repository:$tag"
done

# Demo updates briefly stop the sole task rather than paying for overlapping tasks.
deploy_stack "$tag" 0
cluster="$(output Cluster)"
network="$(jq -cn --arg subnet "$(output PublicSubnet)" --arg sg "$(output TaskSecurityGroup)" \
    '{awsvpcConfiguration:{subnets:[$subnet],securityGroups:[$sg],assignPublicIp:"ENABLED"}}')"
result="$(aws ecs run-task --cluster "$cluster" --launch-type FARGATE \
    --task-definition "$(output SeedTask)" --network-configuration "$network" --output json)"
if [[ "$(jq '.failures | length' <<<"$result")" != 0 || "$(jq '.tasks | length' <<<"$result")" != 1 ]]; then
    echo "$result" >&2
    exit 1
fi
task="$(jq -r '.tasks[0].taskArn' <<<"$result")"
aws ecs wait tasks-stopped --cluster "$cluster" --tasks "$task"
result="$(aws ecs describe-tasks --cluster "$cluster" --tasks "$task")"
if [[ "$(jq -r '.tasks[0].containers[0].exitCode' <<<"$result")" != 0 ]]; then
    echo "$result" >&2
    echo "Database initialization failed. Inspect $(output LogGroup) before retrying." >&2
    exit 1
fi

bucket="$(output FrontendBucket)"
aws s3 sync starter/frontend/dist/sleapy-stocks/browser/ "s3://$bucket/" \
    --delete --exclude index.html --cache-control 'public,max-age=31536000,immutable'
aws s3 cp starter/frontend/dist/sleapy-stocks/browser/index.html "s3://$bucket/index.html" \
    --cache-control 'no-cache,no-store,must-revalidate' --content-type text/html
deploy_stack "$tag" 1
aws ecs wait services-stable --cluster "$cluster" --services "$(output Service)"
invalidation="$(aws cloudfront create-invalidation --distribution-id "$(output DistributionId)" \
    --paths '/*' --query Invalidation.Id --output text)"
aws cloudfront wait invalidation-completed --distribution-id "$(output DistributionId)" --id "$invalidation"
url="$(output WebsiteUrl)"
curl --fail --silent --show-error --retry 5 --retry-delay 10 "$url/api/health"
printf '\nsLEAPy is deployed: %s\n' "$url"
