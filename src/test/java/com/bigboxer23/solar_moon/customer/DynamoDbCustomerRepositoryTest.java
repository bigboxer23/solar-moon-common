package com.bigboxer23.solar_moon.customer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@ExtendWith(MockitoExtension.class)
public class DynamoDbCustomerRepositoryTest {

	@Mock
	private DynamoDbEnhancedClient mockClient;

	@Mock
	private DynamoDbTable<Customer> mockTable;

	private TestableDynamoDbCustomerRepository repository;

	private static final String OLD_EMAIL = "old@example.com";
	private static final String NEW_EMAIL = "new@example.com";

	private class TestableDynamoDbCustomerRepository extends DynamoDbCustomerRepository {
		@Override
		protected DynamoDbTable<Customer> getTable() {
			return mockTable;
		}

		@Override
		protected DynamoDbEnhancedClient getClient() {
			return mockClient;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableDynamoDbCustomerRepository();
		lenient().when(mockTable.tableName()).thenReturn("customer");
		lenient().when(mockTable.tableSchema()).thenReturn(TableSchema.fromBean(Customer.class));
	}

	private Customer createCustomer() {
		Customer customer = new Customer("customer-123", OLD_EMAIL, "access-key", "Test Customer");
		customer.setStripeCustomerId("stripe-123");
		customer.setCreatedAt(1000L);
		customer.setUpdatedAt(1000L);
		return customer;
	}

	@Test
	void testChangeEmail_deletesOldKeyAndPutsNewKeyInOneTransaction() {
		Customer customer = createCustomer();
		ArgumentCaptor<TransactWriteItemsEnhancedRequest> captor =
				ArgumentCaptor.forClass(TransactWriteItemsEnhancedRequest.class);

		repository.changeEmail(customer, NEW_EMAIL);

		verify(mockClient).transactWriteItems(captor.capture());
		verify(mockTable, never()).updateItem(any(Customer.class));
		verify(mockTable, never()).putItem(any(Customer.class));
		verify(mockTable, never()).deleteItem(any(Customer.class));
		var items = captor.getValue().transactWriteItems();
		assertEquals(2, items.size());

		TransactWriteItem delete = items.get(0);
		assertNotNull(delete.delete());
		assertEquals(OLD_EMAIL, delete.delete().key().get("email").s());
		assertEquals("attribute_exists(#email)", delete.delete().conditionExpression());
		assertEquals("email", delete.delete().expressionAttributeNames().get("#email"));

		TransactWriteItem put = items.get(1);
		assertNotNull(put.put());
		assertEquals(NEW_EMAIL, put.put().item().get("email").s());
		assertEquals("customer-123", put.put().item().get("customerId").s());
		assertEquals("access-key", put.put().item().get("accessKey").s());
		assertEquals("stripe-123", put.put().item().get("stripeCustomerId").s());
		assertEquals("attribute_not_exists(#email)", put.put().conditionExpression());
	}

	@Test
	void testChangeEmail_returnsCustomerWithNewEmailAndUpdatedTimestamp() {
		Customer customer = createCustomer();

		Customer result = repository.changeEmail(customer, NEW_EMAIL);

		assertEquals(NEW_EMAIL, result.getEmail());
		assertEquals(1000L, result.getCreatedAt());
		assertTrue(result.getUpdatedAt() > 1000L);
	}

	@Test
	void testChangeEmail_whenConditionFails_propagatesException() {
		doThrow(TransactionCanceledException.builder()
						.message("ConditionalCheckFailed")
						.build())
				.when(mockClient)
				.transactWriteItems(any(TransactWriteItemsEnhancedRequest.class));

		assertThrows(TransactionCanceledException.class, () -> repository.changeEmail(createCustomer(), NEW_EMAIL));
	}
}
