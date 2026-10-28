#!/usr/bin/env bash
set -euo pipefail
export AWS_PAGER=""
export AWS_DEFAULT_REGION="${AWS_REGION:-us-east-1}"
stack="${STACK_NAME:-sleapy-demo}"
expected_account="${EXPECTED_AWS_ACCOUNT_ID:?Set EXPECTED_AWS_ACCOUNT_ID to the approved AWS account}"
account="$(aws sts get-caller-identity --query Account --output text)"
if [[ "$account" != "$expected_account" || "${CONFIRM_DELETE:-}" != "$stack" ]]; then
    echo "Set CONFIRM_DELETE=$stack and verify EXPECTED_AWS_ACCOUNT_ID before deleting." >&2
    exit 1
fi
bucket="$(aws cloudformation describe-stacks --stack-name "$stack" \
    --query "Stacks[0].Outputs[?OutputKey=='FrontendBucket'].OutputValue | [0]" --output text)"
if [[ -z "$bucket" || "$bucket" == None ]]; then
    echo "Cannot resolve frontend bucket; refusing to delete." >&2
    exit 1
fi
aws s3 rm "s3://$bucket/" --recursive
aws cloudformation delete-stack --stack-name "$stack"
aws cloudformation wait stack-delete-complete --stack-name "$stack"
echo "sLEAPy stack deleted. RDS retains a final snapshot; it incurs storage charges until explicitly deleted."
aws rds describe-db-snapshots --db-instance-identifier "$stack-postgres" \
    --snapshot-type manual --query 'DBSnapshots[].{Snapshot:DBSnapshotIdentifier,Status:Status}' --output table
