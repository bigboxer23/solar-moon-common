# Testing Strategy

## Overview

This project uses a two-tier testing approach:

1. **Unit Tests** - Fast, isolated tests that mock external dependencies
2. **Integration Tests** - Tests that interact with real AWS services, OpenSearch, and external APIs

## Directory Structure

```
src/
├── main/java/                          # Production code
├── test/java/                          # Unit tests (fast, isolated)
│   └── com/bigboxer23/solar_moon/
│       ├── subscription/
│       │   └── SubscriptionComponentTest.java
│       ├── TestUtils.java
│       └── TestConstants.java
└── integration-test/java/              # Integration tests (live services)
    └── com/bigboxer23/solar_moon/
        └── subscription/
            └── SubscriptionComponentIntegrationTest.java
```

## Naming Conventions

|  Test Type  |           Location           |     Naming Pattern      |                Example                 |
|-------------|------------------------------|-------------------------|----------------------------------------|
| Unit Test   | `src/test/java/`             | `*Test.java`            | `SubscriptionComponentTest`            |
| Integration | `src/integration-test/java/` | `*IntegrationTest.java` | `SubscriptionComponentIntegrationTest` |

## Running Tests

### Unit Tests Only (Default)

Fast tests that run without external dependencies:

```bash
mvn test
```

This will ONLY run tests in the `unit/` directory matching `*Test.java`.

### Integration Tests

Tests that require AWS credentials and live services:

```bash
mvn verify -P integration-tests
```

Or run both unit and integration tests:

```bash
mvn clean verify -P integration-tests
```

### Run Specific Test

```bash
# Unit test
mvn test -Dtest=SubscriptionComponentTest

# Integration test
mvn verify -Dtest=SubscriptionComponentIntegrationTest -P integration-tests
```

## Writing Unit Tests

### Key Principles

1. **Mock all external dependencies** (DynamoDB, S3, OpenSearch, external APIs)
2. **Test business logic in isolation**
3. **Fast execution** (milliseconds per test)
4. **No network calls**
5. **No test data cleanup required**

### Unit Test Pattern

Use the testable component pattern to inject mocks:

```java
@ExtendWith(MockitoExtension.class)
public class SubscriptionComponentTest {

    @Mock
    private DynamoDbTable<Subscription> mockTable;

    @Mock
    private DeviceComponent mockDeviceComponent;

    private TestableSubscriptionComponent subscriptionComponent;

    // Testable subclass that accepts mocked dependencies
    private static class TestableSubscriptionComponent extends SubscriptionComponent {
        private final DynamoDbTable<Subscription> table;
        private final DeviceComponent deviceComponent;

        public TestableSubscriptionComponent(
                DynamoDbTable<Subscription> table,
                DeviceComponent deviceComponent) {
            this.table = table;
            this.deviceComponent = deviceComponent;
        }

        @Override
        protected DynamoDbTable<Subscription> getTable() {
            return table;
        }

        // Override other dependencies as needed
    }

    @BeforeEach
    void setUp() {
        subscriptionComponent = new TestableSubscriptionComponent(
            mockTable,
            mockDeviceComponent
        );
    }

    @Test
    void testGetSubscription_withValidCustomerId_returnsSubscription() {
        // Arrange
        Subscription expected = new Subscription("customer-123", 2, 12345L);
        when(mockTable.query(any(QueryConditional.class)))
            .thenReturn(mockPageIterable);
        // ... setup mock chain

        // Act
        Optional<Subscription> result = subscriptionComponent.getSubscription("customer-123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
        verify(mockTable).query(any(QueryConditional.class));
    }
}
```

### Making Components Testable

To enable unit testing, components should:

1. **Extend base class with protected getters for dependencies**

```java
public class MyComponent extends AbstractDynamodbComponent<MyData> {

    // Make table accessible for testing
    @Override
    protected DynamoDbTable<MyData> getTable() {
        return super.getTable();
    }

    // Make S3 client accessible for testing
    protected S3Client getS3Client() {
        return IComponentRegistry.smaIngestComponent.getS3Client();
    }
}
```

2. **Avoid hardcoded static dependencies** - use protected methods that can be overridden

### What to Unit Test

- **Business logic** (calculations, validations, transformations)
- **Conditional logic** (if/else branches, edge cases)
- **Error handling** (null checks, invalid inputs)
- **Data mapping** (entity transformations)
- **Validation methods** (isValid*, can*, should*)

### What NOT to Unit Test

- DynamoDB query syntax (trust AWS SDK)
- JSON serialization (trust Gson)
- AWS SDK behavior (trust Amazon)

## Writing Integration Tests

### Key Principles

1. **Test end-to-end flows** with real services
2. **Use actual AWS resources** (DynamoDB, S3, OpenSearch)
3. **Clean up test data** in `@AfterAll`
4. **Test real-world scenarios**

### Integration Test Pattern

```java
public class OpenSearchComponentIT implements IComponentRegistry, TestConstants {

    @BeforeEach
    public void setup() {
        TestUtils.setupSite();  // Creates test devices and data
    }

    @AfterAll
    public static void afterAll() {
        TestUtils.nukeCustomerId(CUSTOMER_ID);  // Cleans up all test data
    }

    @Test
    public void testTimeSeriesSearch() throws IOException, InterruptedException {
        // Seed real data in OpenSearch
        TestUtils.seedOpenSearchData();
        OpenSearchUtils.waitForIndexing();

        // Execute real search query
        SearchResponse<DeviceData> response = OSComponent.getTimeSeries(...);

        // Assert on real results
        assertNotNull(response);
        assertTrue(response.hits().hits().size() > 0);
    }
}
```

### What to Integration Test

- **OpenSearch queries** (time series, aggregations, complex queries)
- **Data ingestion pipelines** (FTP → S3 → DynamoDB → OpenSearch)
- **AWS service interactions** (S3 file operations, DynamoDB CRUD)
- **External API calls** (weather, geocoding)
- **Multi-component workflows** (alarm detection, notifications)

### Test Data Management

Use `TestUtils` for consistent test data:

```java
// Setup
TestUtils.setupSite();                    // Creates standard test device
TestUtils.seedOpenSearchData();           // Populates OpenSearch indices
TestUtils.fetchTestFiles();               // Downloads test files from S3

// Cleanup
TestUtils.nukeCustomerId(CUSTOMER_ID);    // Deletes all test data
```

## Migration Path

### Gradual Migration Strategy

1. **New tests** should be written as unit tests when possible
2. **Existing integration tests** remain functional in their current location
3. **Gradually migrate** tests to new structure as you touch them

### Migrating an Integration Test

1. Move test to `src/integration-test/java/` directory (same package structure)
2. Rename to `*IntegrationTest.java` suffix

```bash
# Example
mv src/test/java/com/bigboxer23/solar_moon/device/TestDeviceComponent.java \
   src/integration-test/java/com/bigboxer23/solar_moon/device/DeviceComponentIntegrationTest.java
```

3. Update the class name in the file to match the new filename

### Converting Integration Test to Unit Test

1. Identify external dependencies (DynamoDB, S3, APIs)
2. Create testable component with constructor injection
3. Mock all external dependencies with Mockito
4. Test business logic in isolation
5. Place in `unit/` directory with `*Test.java` suffix

See `SubscriptionComponentTest` for a complete example.

## Test Utilities

### TestUtils.java

Provides reusable test data setup and cleanup:

- `setupSite()` - Creates test customer, devices, and site
- `seedOpenSearchData()` - Populates OpenSearch with device data
- `nukeCustomerId(customerId)` - Deletes all data for customer
- `fetchTestFiles()` - Downloads test files from S3

### TestConstants.java

Defines shared constants:

- `CUSTOMER_ID` - Standard test customer ID
- `DEVICE_ID` - Standard test device ID
- XML test fixtures
- Test file locations

## Best Practices

### Unit Tests

- **One assertion per test** when possible
- **Descriptive test names** (e.g., `testGetSubscription_withExpiredTrial_returnsEmpty`)
- **AAA pattern**: Arrange, Act, Assert
- **No comments** - test names should be self-documenting
- **Mock at boundaries** - mock only external dependencies

### Integration Tests

- **Test realistic scenarios** - use real data patterns
- **Clean up after yourself** - always use `@AfterAll`
- **Wait for async operations** - use `OpenSearchUtils.waitForIndexing()`
- **Verify side effects** - check data was actually written

### General

- **Keep tests independent** - no test should depend on another
- **Avoid test data pollution** - use unique IDs when needed
- **Test edge cases** - null, empty, invalid inputs
- **Test error conditions** - exceptions, timeouts, failures

## Continuous Integration

### Local Development

```bash
# Fast feedback loop - unit tests only
mvn test

# Full validation before commit
mvn clean verify -P integration-tests
```

### CI/CD Pipeline Recommendations

```yaml
# Fast feedback - run on every push
unit-tests:
  script: mvn clean test

# Comprehensive - run before merge
integration-tests:
  script: mvn clean verify -P integration-tests
  only:
    - merge_requests
    - main
```

## Common Patterns

### Mocking DynamoDB Query

```java
@Mock
private PageIterable<Subscription> mockPageIterable;
@Mock
private Page<Subscription> mockPage;

when(mockTable.query(any(QueryConditional.class)))
    .thenReturn(mockPageIterable);
when(mockPageIterable.stream())
    .thenReturn(Stream.of(mockPage));
when(mockPage.items())
    .thenReturn(Collections.singletonList(subscription));
```

### Mocking Component Dependencies

```java
// Override IComponentRegistry components in testable subclass
private static class TestableComponent extends MyComponent {
    private final DeviceComponent deviceComponent;

    public TestableComponent(DeviceComponent deviceComponent) {
        this.deviceComponent = deviceComponent;
    }

    // Override methods that use IComponentRegistry
    @Override
    public boolean canAddDevice(String customerId) {
        return deviceComponent.getDevicesForCustomerId(customerId).size() < 10;
    }
}
```

### Time-Based Testing

```java
// For components with time-dependent logic, use constants
private static final long CURRENT_TIME = System.currentTimeMillis();
private static final long EXPIRED_TIME = CURRENT_TIME - (TRIAL_LENGTH + 1000);
private static final long VALID_TIME = CURRENT_TIME - (TRIAL_LENGTH / 2);

Subscription expired = new Subscription("customer", -1, EXPIRED_TIME);
Subscription valid = new Subscription("customer", -1, VALID_TIME);
```

## Troubleshooting

### Unit Tests Not Running

Check that:

- Test is in `src/test/java/com/bigboxer23/solar_moon/unit/`
- File name ends with `Test.java`
- Class is `public` with `public` test methods
- Methods annotated with `@Test`

### Integration Tests Not Running

Check that:

- Test is in `src/test/java/com/bigboxer23/solar_moon/integration/`
- File name ends with `IT.java`
- Using `-P integration-tests` profile
- AWS credentials configured

### Mocks Not Working

Check that:

- Using `@ExtendWith(MockitoExtension.class)`
- Mocks annotated with `@Mock`
- Setup in `@BeforeEach` method
- Using `when(...).thenReturn(...)` syntax

## Examples

### Complete Examples

1. **Unit Test**: `src/test/java/com/bigboxer23/solar_moon/subscription/SubscriptionComponentTest.java`
   - Mocks DynamoDB table and other components
   - Tests business logic for subscriptions and trials
   - Shows testable component pattern
2. **Integration Test**: `src/integration-test/java/com/bigboxer23/solar_moon/subscription/SubscriptionComponentIntegrationTest.java`
   - Uses real DynamoDB and services
   - Tests subscription limits and trial mode
   - Shows data setup and cleanup
3. **Unit Test (Repository Pattern)**: `src/test/java/com/bigboxer23/solar_moon/device/DeviceComponentTest.java`
   - Mocks DeviceRepository
   - Alternative mocking approach for components with repositories

## Questions?

For questions or discussions about testing strategy, refer to:

- Maven Surefire documentation: https://maven.apache.org/surefire/maven-surefire-plugin/
- Maven Failsafe documentation: https://maven.apache.org/surefire/maven-failsafe-plugin/
- Mockito documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- JUnit 5 documentation: https://junit.org/junit5/docs/current/user-guide/

