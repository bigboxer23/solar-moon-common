[![CodeQL](https://github.com/bigboxer23/solar-moon-common/actions/workflows/codeql.yml/badge.svg)](https://github.com/bigboxer23/solar-moon-common/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/bigboxer23/solar-moon-common/graph/badge.svg)](https://codecov.io/gh/bigboxer23/solar-moon-common)

# solar-moon-common

This repository contains common code used by solar moon projects, excluding payment.

It is designed to be leveraged by wrapper webservices which are implemented as lambdas ([solar-moon-ingest-lambda](https://github.com/bigboxer23/solar-moon-ingest-lambda))
or via standalone microservices in a springboot container ([solar-moon-ingest](https://github.com/bigboxer23/solar-moon-ingest)).

## Building

See the [solar-moon-server-config](https://github.com/bigboxer23/solar-moon-server-config/tree/main/scripts/lambda) repository.
Descriptions of what's included in each directory below:

#### config

properties (and test props) each lambda needs to properly run

#### functions

Scripts in this directory are used for building newer versions of each function from subproject source code (common, ingest).
Small amount of source code also exists here for node native lambda functions used

#### layers

Scripts in this directory are used for building a newer version of each layer from source code in the subprojects (common, ingest)

#### top level scripts

These scripts take individual actions done above and roll them into one handy command.
1. `updateAll.sh` Update all layers, functions
2. `updateAllFunctionLayers.sh` Trigger an update of all functions to use the latest version of each layer
3. `updateAllFunctions.sh` Build all lambda functions, push the new version of each to lambda

## Operations

### Restoring data

See [RESTORE.md](RESTORE.md) for recovering DynamoDB and OpenSearch data from backups, and for what isn't backed up.

### Changing a customer's email

A customer's email lives in two places: Cognito (where it's the login) and the `customer` DynamoDB table (where it's the
partition key). `CustomerComponent.changeEmail(customerId, newEmail)` updates both. It changes Cognito first, then swaps
the DynamoDB record in a single transaction, and puts the old Cognito email back if the swap fails. `customerId` (the
Cognito `sub`), the access key, devices, and data are unchanged. **Stripe is not updated.** Change the email on the
Stripe customer separately if receipts should go to the new address.

The new email is marked verified in Cognito, so confirm the customer controls it first (for example, have them email
support from the new address).

Run it locally with `ChangeCustomerEmailRunner`, which has no setup or teardown and does nothing unless its system
properties are set. Never run one-off production changes from the regular integration test classes: their
`@BeforeEach`/`@AfterEach` create and delete data in the live tables.

Prerequisites:

- AWS credentials allowed to call `cognito-idp:AdminUpdateUserAttributes` on the user pool and `dynamodb:TransactWriteItems`
  (Put/Delete) on the `customer` table
- `aws.region` and `aws.cognito.user.pool.id` available to `PropertyUtils`. It reads the classpath
  `application.properties` (for integration runs, `src/integration-test/resources/application.properties`), then falls
  back to an environment variable with the exact same name. It **ignores `-D` flags**. The command below passes them as
  environment variables with `env`, since names containing dots can't be set with `export`.

Do a dry run first. It prints the customer, current email, and new email, and fails if the new email belongs to someone else:

```bash
env 'aws.region=us-west-2' 'aws.cognito.user.pool.id=us-west-2_Z4vEruWJT' \
mvn verify -P integration-tests -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=ChangeCustomerEmailRunner \
  -DchangeEmail.customerId=<cognito sub> \
  -DchangeEmail.newEmail=<new email>
```

If the output looks right, run the same command again with `-DchangeEmail.apply=true` added. In IntelliJ, run
`ChangeCustomerEmailRunner` with the `changeEmail.*` flags as VM options and the two properties as environment variables.

To find a customer's `sub`, look in the `customerId` column of the `customer` table, or run
`aws cognito-idp list-users --user-pool-id <pool id> --filter 'email = "<current email>"'`.

## Creating new release

Within `.github/workflows` there exist two relevant yml files, `main.yml` and `tag_release.yml`. `tag_release` watches for
any pushes which contain a git tag of the format, `v[major version].[minor version].[sub minor version]`.

When found, a new release is created within the github repo with the tag defining the version information. This triggers
the second action, `main`, which builds the repositorya nd publishes artifacts to the GitHub page.

One final note, when creating the new git tag for the version, the version number within the `pom.xml` should also be
incremented to match the tag.
