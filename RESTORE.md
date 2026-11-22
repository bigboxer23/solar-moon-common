# Restoring Data

Runbook for recovering Solar Moon data after an accidental delete or bad write. Region is `us-west-2` for everything.

**Golden rule: restore into a new table or index, check it, then copy back only what you need.** Neither DynamoDB nor
OpenSearch can restore over a live table or index in place. Copying selectively also keeps the data customers wrote
after the restore point.

## What's backed up

|                                                   Data                                                    |                                  Backup                                   | How far back |
|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|--------------|
| DynamoDB: `customer`, `devices`, `linked_devices`, `subscription`, `mappings`, `alarms`, `device_updates` | Point-in-time recovery (PITR), to the second                              | 35 days      |
| DynamoDB: `downloadRequest`, `lock_table`, `maintenanceMode`, `openSearchStatus`, `weather`               | **None.** These hold temporary, cached, or lock data that rebuilds itself | n/a          |
| OpenSearch `generation-meter` (all device readings)                                                       | AWS automated hourly snapshots, repository `cs-automated-enc`             | ~14 days     |
| OpenSearch `logs-*`                                                                                       | Same hourly snapshots; indices older than 3 months are deleted on purpose | ~14 days     |
| Cognito users                                                                                             | **None.** See [Gaps](#gaps)                                               | n/a          |
| S3 `ftp-solarmoonanalytics`                                                                               | **None** (versioning is off)                                              | n/a          |

## 1. Before you start

1. **Stop the damage.** Kill the process, revert the deploy, or disable whatever is still deleting or writing data.
2. **Find the restore point.** Pick a time shortly _before_ the damage happened. Deployed lambdas log to monthly
   OpenSearch indices (`logs-YYYY.MM`, kept 3 months). Search by message or by service, e.g. `DeviceDelete`:

   ```bash
   curl -s -u "$OS_USER:$OS_PASS" -H "$J" "$OS/logs-*/_search?size=20&sort=@timestamp:desc" \
     -d '{"query":{"match_phrase":{"message":"<text from the log line>"}}}' \
     | jq -r '.hits.hits[]._source | "\(.["@timestamp"]) \(.["service.name"]) \(.message)"'
   ```

   (See [section 3](#3-opensearch) for `OS`, `OS_USER`, `OS_PASS`, `J`.) Not everything is shipped there:
   `CustomerComponent` messages (e.g. `customer delete by customer id`) don't currently appear, and anything run from a
   laptop, such as integration tests, logs only to the console. If you can't find the moment in the logs, check the
   service's CloudWatch logs, or pick a time you know was safe. Going back further only means more data to compare.

3. **Check your credentials.** Restoring needs more than the app uses: `dynamodb:RestoreTableToPointInTime`, `Scan`,
   `PutItem`, `DeleteTable`, plus the OpenSearch snapshot permissions. The `S3Builds` user has **not** been tested for
   these, so use an admin role if a call is denied.

4. Set shared variables in your shell:

   ```bash
   export AWS_REGION=us-west-2
   CUSTOMER_ID=<cognito sub>                   # customerId, same as the Cognito username
   SUFFIX=restore-$(date +%Y%m%d)              # name suffix for the restored copies
   RESTORE_TIME=2026-11-20T14:55:00-06:00      # ISO 8601, include the UTC offset
   ```

## 2. DynamoDB

### 2a. Restore tables to new copies

Restore each affected table to `<table>-$SUFFIX`. Restores usually take 5–30 minutes and run in parallel.

```bash
TABLES=(customer subscription mappings devices linked_devices alarms)   # array: works in bash and zsh
for t in "${TABLES[@]}"; do
  aws dynamodb restore-table-to-point-in-time \
    --source-table-name "$t" \
    --target-table-name "$t-$SUFFIX" \
    --restore-date-time "$RESTORE_TIME" \
    --billing-mode-override PAY_PER_REQUEST \
    --query 'TableDescription.TableStatus' --output text
done
for t in "${TABLES[@]}"; do
  aws dynamodb wait table-exists --table-name "$t-$SUFFIX" && echo "$t-$SUFFIX ready"
done
```

`wait` gives up after about 8 minutes. If it times out, run it again, or check
`aws dynamodb describe-table --table-name <t>-$SUFFIX --query Table.TableStatus`.

To restore to the newest moment available, replace `--restore-date-time ...` with `--use-latest-restorable-time`.

Secondary indexes are restored along with the table. TTL, PITR, tags, deletion protection, and alarms are **not**
copied, which doesn't matter for a temporary copy.

### 2b. Restore one customer (most common)

The `copy_back` helper reads one customer's items from the restored copy and writes them into the live table. It's a dry
run unless `APPLY=1` is set.

```bash
copy_back() {  # usage: copy_back <table> <customer attribute>
  local table=$1 attr=$2
  aws dynamodb scan --table-name "$table-$SUFFIX" \
    --filter-expression "#a = :v" \
    --expression-attribute-names "{\"#a\":\"$attr\"}" \
    --expression-attribute-values "{\":v\":{\"S\":\"$CUSTOMER_ID\"}}" \
    --output json \
  | jq -c '.Items[]' \
  | while read -r item; do
      if [ "$APPLY" = "1" ]; then
        aws dynamodb put-item --table-name "$table" --item "$item" && echo "$table: restored"
      else
        echo "$table: would restore $(echo "$item" | jq -c 'with_entries(select(.key|test("^(email|id|customerId|clientId|alarmId|mappingName|name)$"))) | map_values(.S)')"
      fi
    done
}
```

Do a dry run first, and restore in this order. Devices must exist before the data that hangs off them.

```bash
copy_back customer       customerId
copy_back subscription   customerId
copy_back mappings       customerId
copy_back devices        clientId
copy_back linked_devices customerId
copy_back alarms         customerId
```

If the output looks right, run the same lines again with `APPLY=1` in front of each one (e.g. `APPLY=1 copy_back customer customerId`).

Things to watch for:

- **`put-item` overwrites.** If a live item with the same key exists, it goes back to its value at the restore time. For
  a deleted customer that's what you want. For a bad _update_, check the dry-run output first.
- **The `customer` table is keyed by email.** If the customer's email changed after the restore point, restoring their
  row creates a second row under the old email. Delete one of the two afterwards (`changeEmail` handles the live side).
- **Skip `device_updates`.** It only stores "last data received" times per device, expires on its own (TTL), and refills
  on the next upload.

Then put back what DynamoDB doesn't cover:

```bash
# Customer delete also removes the SMA FTP folder marker. Re-create it from the restored access key:
ACCESS_KEY=$(aws dynamodb scan --table-name "customer-$SUFFIX" --filter-expression "customerId = :v" \
  --expression-attribute-values "{\":v\":{\"S\":\"$CUSTOMER_ID\"}}" --query 'Items[0].accessKey.S' --output text)
aws s3api put-object --bucket ftp-solarmoonanalytics --key "$ACCESS_KEY/"
```

Device readings live in OpenSearch: see [3b](#3b-restore-one-customer).

### 2c. Restore a whole table

**Option 1, copy everything back (preferred, no downtime).** Same as `copy_back`, but without the filter. Items deleted
since the restore point come back, and items created since then stay. Bad _new_ items aren't removed.

```bash
aws dynamodb scan --table-name "<table>-$SUFFIX" --output json | jq -c '.Items[]' \
  | while read -r item; do aws dynamodb put-item --table-name "<table>" --item "$item"; done
```

**Option 2, replace the table (downtime, loses everything after the restore point).** Use this only if the live table
is beyond repair. A table can only be restored under a name that doesn't exist, so the live one has to be deleted first.

1. Stop every writer to the table: ingest lambdas, the Spring service, and the frontend API.
2. `aws dynamodb update-table --table-name <table> --no-deletion-protection-enabled` (if it's enabled), then
   `aws dynamodb delete-table --table-name <table>`.
3. Restore under the original name with `restore-table-to-point-in-time` (as in 2a, but `--target-table-name <table>`).
4. Put back the settings a restore doesn't copy:

   ```bash
   aws dynamodb update-continuous-backups --table-name <table> \
     --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true
   aws dynamodb update-table --table-name <table> --deletion-protection-enabled        # customer has this on
   aws dynamodb update-time-to-live --table-name <table> \
     --time-to-live-specification Enabled=true,AttributeName=expiration                 # alarms, device_updates only
   ```
5. Restart writers.

### 2d. A table was deleted

If PITR was on when a table was deleted, DynamoDB keeps a system backup of it for 35 days:

```bash
aws dynamodb list-backups --backup-type SYSTEM --table-name <table>
aws dynamodb restore-table-from-backup --target-table-name <table> --backup-arn <arn from above>
```

Then do step 4 from [2c option 2](#2c-restore-a-whole-table).

### 2e. Clean up

The restored copies cost money for storage until you delete them:

```bash
for t in "${TABLES[@]}"; do aws dynamodb delete-table --table-name "$t-$SUFFIX" --query 'TableDescription.TableName' --output text; done
```

## 3. OpenSearch

The domain is AWS-managed (`dashboard.solarmoonanalytics.com`). All readings are in one index, `generation-meter`, and
documents belong to a customer through the `customer-id.keyword` field. The domain has a single node, so restore with
0 replicas or the restored index stays yellow.

```bash
OS=https://dashboard.solarmoonanalytics.com
OS_USER=service
read -rs OS_PASS     # typed this way so the password stays out of shell history
J='Content-Type: application/json'
```

The user needs permission to restore snapshots. If `_restore` returns 403, use the domain's master user.

### 3a. Restore the index under a new name

Snapshots are named by UTC start time, for example `2026-11-20t13-57-24.<uuid>`. Pick the newest one taken **before**
the damage:

```bash
curl -s -u "$OS_USER:$OS_PASS" "$OS/_cat/snapshots/cs-automated-enc?h=id,status,end_epoch&s=end_epoch" | tail -20
SNAP=<snapshot id>

curl -s -u "$OS_USER:$OS_PASS" -H "$J" -X POST "$OS/_snapshot/cs-automated-enc/$SNAP/_restore" -d '{
  "indices": "generation-meter",
  "rename_pattern": "generation-meter",
  "rename_replacement": "restored-generation-meter",
  "include_aliases": false,
  "include_global_state": false,
  "index_settings": { "index.number_of_replicas": 0 }
}'

# Watch progress (the index is ~2 GB, a few minutes). Done when health is green and recovery shows nothing active.
curl -s -u "$OS_USER:$OS_PASS" "$OS/_cat/indices/restored-*?v"
curl -s -u "$OS_USER:$OS_PASS" "$OS/_cat/recovery/restored-generation-meter?active_only=true&v"
```

Never restore the system indices (`.opendistro_security`, `.kibana`, `.plugins-*`). Naming `generation-meter`
explicitly, as above, avoids them.

### 3b. Restore one customer

Compare document counts, then copy that customer's documents back. With `op_type: create`, only documents missing from
the live index are added. Existing and newer documents aren't touched, so it's safe to run again.

```bash
COUNT_Q='{"query":{"term":{"customer-id.keyword":"'"$CUSTOMER_ID"'"}}}'
curl -s -u "$OS_USER:$OS_PASS" -H "$J" "$OS/restored-generation-meter/_count" -d "$COUNT_Q"
curl -s -u "$OS_USER:$OS_PASS" -H "$J" "$OS/generation-meter/_count" -d "$COUNT_Q"

curl -s -u "$OS_USER:$OS_PASS" -H "$J" -X POST "$OS/_reindex?wait_for_completion=false" -d '{
  "conflicts": "proceed",
  "source": { "index": "restored-generation-meter",
              "query": { "term": { "customer-id.keyword": "'"$CUSTOMER_ID"'" } } },
  "dest":   { "index": "generation-meter", "op_type": "create" }
}'
# returns {"task":"<id>"}; check it until "completed": true
curl -s -u "$OS_USER:$OS_PASS" "$OS/_tasks/<id>" | jq '.completed, .response.created, .response.failures'
```

Run the live `_count` again afterwards. It should be at least the restored count.

### 3c. Restore the whole index

- **Copy back (preferred, no downtime):** run the `_reindex` above without the `query` block. Deleted documents come
  back and newer documents stay. Bad _new_ documents aren't removed.
- **Replace (loses everything since the snapshot):** stop ingest, run `DELETE /generation-meter`, then repeat 3a
  without `rename_pattern`/`rename_replacement` so it restores as `generation-meter`, and restart ingest.

### 3d. Clean up

```bash
curl -s -u "$OS_USER:$OS_PASS" -X DELETE "$OS/restored-generation-meter"
```

## 4. Check the result

- Log in to the app as (or impersonate) the customer: sites, devices, and history charts show up.
- `aws dynamodb query --table-name customer --index-name customerId-index --key-condition-expression "customerId = :c" --expression-attribute-values "{\":c\":{\"S\":\"$CUSTOMER_ID\"}}"`
  returns exactly **one** row.
- New uploads land: watch for fresh `generation-meter` documents for the customer after their next upload interval.
- The restored copies (`*-$SUFFIX`, `restored-*`) are deleted.

## Gaps

These **can't** be restored today:

- **Cognito users.** Deleting a user is permanent. The user pool has deletion protection, but that only protects the
  pool itself, not individual users. If a user signs up again, they get a new `sub`, and every table and OpenSearch
  document keyed by the old `customerId` would have to be rewritten to the new one. Be extremely careful with
  `admin-delete-user`.
- **S3 `ftp-solarmoonanalytics`.** Versioning is off, so deleted or overwritten uploads are gone. Only the access key
  folder marker can be re-created (see 2b).
- **OpenSearch older than ~14 days.** Automated snapshots only go back about two weeks. A bad delete noticed later than
  that can't be undone without a manual snapshot repository.

