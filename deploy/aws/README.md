# sLEAPy AWS demo

## Demo environment

- Website: https://d1g9ulqqc5um94.cloudfront.net
- AWS account: `504886488007`
- Region: `us-east-1`
- CloudFormation stack / ECS cluster / service: `sleapy-demo`
- RDS instance: `sleapy-demo-postgres`

The address belongs to this deployment; deleting/recreating the stack changes it.

Verified on deployment: CloudFormation update complete, one healthy ECS task and
ALB target, HTTPS homepage/login/signup deep links, database-backed health,
signup/login and JWT forwarding, deposit/withdrawal persistence, a live AAPL price,
SockJS discovery, and a real TLS WebSocket upgrade through CloudFront/ALB.
The temporary test account was deleted. A second database-init task exited
successfully without reseeding existing data.

## Migrated database (2026-10-28)

The backend now uses `sleapy_migrated_20261028` on the same private RDS instance.
It was restored from a verified custom-format dump of `sleapy_db` on the source
reached through the team's existing SSH tunnel. The source database was not changed.
This is a point-in-time copy, not ongoing replication; subsequent source writes
do not appear in RDS automatically.

Verified migrated data: **37 clients, 229 orders, 47 holdings, 51 instruments**.
Record fingerprints matched the dump for account credentials/balances, orders,
holdings, and instrument identities. Password hashes were preserved, so existing
users retain their passwords. Post-cutover signup, login, JWT authentication,
balance reads, deposit/withdrawal writes, and SockJS discovery passed. The
temporary verification account was removed.

The previous demo database, `sleapy_db`, remains intact on RDS with 12 clients.
It was not merged with source data. The encrypted manual safety snapshot
`sleapy-demo-before-migration-202610281530` is also retained.
The temporary encrypted S3 transfer bucket was deleted after restoration.

CloudFormation parameter `AppDatabaseName` controls both the backend database and
the seed task's target. Normal deployments preserve the existing parameter value,
and seed initialization skips databases that already contain `public.clients`.
Do not reset this parameter to its default unless intentionally rolling back.

To switch back to the preserved demo database (brief downtime; does not merge
any writes made to the migrated database):

```bash
aws cloudformation deploy --region us-east-1 --stack-name sleapy-demo \
  --template-file deploy/aws/stack.yaml --capabilities CAPABILITY_IAM \
  --parameter-overrides AppDatabaseName=sleapy_db
```

To switch to the migrated database again, use the same command with
`AppDatabaseName=sleapy_migrated_20261028`.
The safety snapshot survives stack deletion and incurs storage charges until
explicitly deleted, just like the final snapshot described below.

## Architecture and cost

- CloudFront default HTTPS domain serves a private, encrypted S3 bucket using Origin Access Control.
- `/api/*` and `/ws/*` go to an Application Load Balancer with caching disabled, forwarding
  authentication, query strings, and WebSocket headers. Angular deep links rewrite to
  `index.html` only for the S3 behavior; API errors remain API errors.
- One ECS Fargate task: 0.5 vCPU / 2 GiB, with Java and the Python stock-price sidecar.
  The Python port is loopback-only. Redis, Kafka, Airflow, and pgAdmin are not needed
  by this application's deployed request path.
- PostgreSQL 17 on private, single-AZ `db.t4g.micro`, 20 GiB encrypted gp3 storage,
  TLS-required application connections, one-day automated backups, and no public endpoint.
- Tasks have public IPs for outbound image pulls and stock quotes, avoiding a NAT
  Gateway. Their security group accepts backend traffic only from the ALB.
- The ALB accepts only CloudFront origin-facing IPs and requires a generated secret
  origin header. Viewer traffic is HTTPS; CloudFront-to-ALB traffic uses HTTP inside
  the AWS network. This is a demo tradeoff, not end-to-end TLS. For sensitive/production
  data, add a domain, ACM certificate, and HTTPS ALB origin before use.
- Generated database/JWT/origin secrets live in Secrets Manager; ECS receives only
  required secrets. CloudWatch logs expire after seven days.

Estimated **USD 65-85/month in us-east-1** at low demo traffic, before tax/credits:
Fargate approximately $29, ALB approximately $17 plus LCUs, RDS approximately
$12-15 plus approximately $2-3 storage, three public IPv4 addresses approximately
$11, and small ECR, logging, secrets, S3, and CDN charges. This is not a spending
cap. Actual AWS pricing and workload determine the bill.

This is not highly available: the single app task and single-AZ database trade
availability for cost. Updates briefly stop the task. Stock data depends on Yahoo
Finance availability/rate limits. Use synthetic data only; this is not a production
brokerage deployment.

## Deploy / update

Prerequisites: AWS CLI v2 with suitable CloudFormation, IAM, VPC, ELB, ECS, ECR,
RDS, S3, CloudFront, Logs, and Secrets Manager permissions; Docker; Java 21/Maven;
Node 18+ compatible with Angular 17; npm; jq; curl; installed frontend dependencies.
On a clean checkout, run `npm ci` in `starter/frontend` first.

```bash
EXPECTED_AWS_ACCOUNT_ID=504886488007 AWS_REGION=us-east-1 \
  bash deploy/aws/deploy.sh
```

Default stack and AWS-compatible resource names use `sleapy-demo`; team tags and
descriptions use `sLEAPy`. Override `STACK_NAME` only for a separate environment.
The deployment builds/tests before creating resources, creates the infrastructure
with zero app tasks, pushes three version-tagged images, initializes the fresh
database with schema and mock data in one transaction, uploads the frontend, then
starts the app and checks database-backed health. Repeating deployment does **not**
reseed an existing database or copy any pre-existing database.

The initial full backend run found two pre-existing failures in `TradeTimestampTest`
(placement timestamps are recorded after validation). Trading logic is unchanged.
For this demo only, the deployment was run with the explicit environment override
`MAVEN_TEST_SELECTOR='!TradeTimestampTest'`; all other backend tests were still run.
The deployment script defaults to the full test suite, so future runs will stop on
these failures unless they are fixed or the same exclusion is explicitly provided.

The database seed uses `tables.sql`, `mockdata.sql`, then `analystSchema.sql`.
Spring applies its existing non-destructive migrations at startup. Most repository
mock users have placeholder hashes and cannot log in; create a user through signup
for a working client account, or use the team's known passwords for the seeded
admin and analyst. Never enter real personal or payment information.

To retrieve the public URL and resource names:

```bash
aws cloudformation describe-stacks --region us-east-1 --stack-name sleapy-demo \
  --query 'Stacks[0].Outputs' --output table
aws logs tail /ecs/sleapy-demo --region us-east-1 --since 10m
```

If provisioning fails, inspect CloudFormation stack events. Do not retry a stack
in a failed rollback state without addressing the reported failure. Failed
deployments may leave billable resources; review the stack before leaving it.

## Remove after the demo

This permanently removes the demo frontend, containers, infrastructure, images,
and active database. It does not touch any databases outside this stack.

```bash
EXPECTED_AWS_ACCOUNT_ID=504886488007 AWS_REGION=us-east-1 \
  CONFIRM_DELETE=sleapy-demo bash deploy/aws/destroy.sh
```

A **final RDS snapshot is intentionally retained** for recovery and continues to
incur storage charges. The cleanup script lists its identifier. After verifying
that you no longer need its data, explicitly delete that snapshot using
`aws rds delete-db-snapshot --db-snapshot-identifier <snapshot-id> --region us-east-1`.
Stopping ECS alone does not stop ALB/RDS charges. An RDS stop is temporary and AWS
automatically restarts it after seven days; delete the stack when finished.
