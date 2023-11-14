package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Customer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** */
// @ActiveProfiles("test")
public class TestCustomerComponent implements IComponentRegistry {
	protected static final String CUSTOMER_EMAIL = "noop@noreply.org";
	protected static final String CUSTOMER_NAME = "mr fake customer";
	protected static final String CUSTOMER_ACCESS_KEY = "4ab84ed3-0ce1-4615-b919-c34c7b619702";

	protected static final String CUSTOMER_STRIPE_ID = "ST_id_1234";

	private final Customer testCustomer = new Customer();

	@Test
	public void testFindCustomerByStripeId() {
		setupTestCustomer();
		assertNull(customerComponent.findCustomerByStripeCustomerId(null));
		assertNull(customerComponent.findCustomerByStripeCustomerId(""));
		assertNull(customerComponent.findCustomerByStripeCustomerId("1234"));
		assertNotNull(customerComponent.findCustomerByStripeCustomerId(CUSTOMER_STRIPE_ID));
	}

	@Test
	public void testFindCustomerByEmail() {
		setupTestCustomer();
		assertNull(customerComponent.findCustomerByEmail(null));
		assertNull(customerComponent.findCustomerByEmail(""));
		assertNull(customerComponent.findCustomerByEmail("1234"));
		assertNotNull(customerComponent.findCustomerByEmail(CUSTOMER_EMAIL));
	}

	@Test
	public void testFindCustomerByAccessKey() {
		setupTestCustomer();
		assertNull(customerComponent.findCustomerIdByAccessKey(null));
		assertNull(customerComponent.findCustomerIdByAccessKey(""));
		assertNull(customerComponent.findCustomerIdByAccessKey("1234"));
		assertNotNull(customerComponent.findCustomerIdByAccessKey(CUSTOMER_ACCESS_KEY));
	}

	protected void setupTestCustomer() {
		try {
			customerComponent.getTable().putItem(testCustomer);
		} catch (DynamoDbException e) {
			testCustomer.setCustomerId(TestDeviceComponent.clientId);
			testCustomer.setEmail(CUSTOMER_EMAIL);
			testCustomer.setAccessKey(CUSTOMER_ACCESS_KEY);
			testCustomer.setStripeCustomerId(CUSTOMER_STRIPE_ID);
			Customer dbCustomer = customerComponent.getTable().getItem(testCustomer);
			if (dbCustomer != null) {
				customerComponent.getTable().deleteItem(dbCustomer);
			}
			customerComponent.getTable().putItem(testCustomer);
			return;
		}
		fail();
	}

	@Test
	public void testAddCustomer() {
		assertNull(customerComponent.addCustomer(
				null, null, TestCustomerComponent.CUSTOMER_NAME, TestCustomerComponent.CUSTOMER_STRIPE_ID));
		assertNull(customerComponent.addCustomer(
				TestCustomerComponent.CUSTOMER_EMAIL,
				null,
				TestCustomerComponent.CUSTOMER_NAME,
				TestCustomerComponent.CUSTOMER_STRIPE_ID));
		assertNull(customerComponent.addCustomer(
				null,
				TestDeviceComponent.clientId,
				TestCustomerComponent.CUSTOMER_NAME,
				TestCustomerComponent.CUSTOMER_STRIPE_ID));
		assertNull(customerComponent.addCustomer("", "", "", ""));
		customerComponent.deleteCustomerByCustomerId(TestDeviceComponent.clientId);
		customerComponent.addCustomer(
				TestCustomerComponent.CUSTOMER_EMAIL,
				TestDeviceComponent.clientId,
				TestCustomerComponent.CUSTOMER_NAME,
				TestCustomerComponent.CUSTOMER_STRIPE_ID);
		assertNotNull(customerComponent.findCustomerByCustomerId(TestDeviceComponent.clientId));
		customerComponent.deleteCustomerByCustomerId(TestDeviceComponent.clientId);
	}

	@Test
	public void testDeleteCustomer() {
		customerComponent.deleteCustomerByEmail(null);
		customerComponent.deleteCustomerByEmail("");
		customerComponent.deleteCustomerByEmail(TestCustomerComponent.CUSTOMER_EMAIL);
		customerComponent.deleteCustomerByCustomerId(null);
		customerComponent.deleteCustomerByCustomerId("");
		customerComponent.deleteCustomerByCustomerId(TestDeviceComponent.clientId);
	}

	@Test
	public void testFindCustomer() {
		new TestCustomerComponent().setupTestCustomer();
		assertNotNull(customerComponent.findCustomerByCustomerId(TestDeviceComponent.clientId));
		assertNull(customerComponent.findCustomerByCustomerId("tacos"));
		assertNull(customerComponent.findCustomerByCustomerId(""));
		assertNull(customerComponent.findCustomerByCustomerId(null));
	}

	@Test
	public void testUpdateCustomer() {
		new TestCustomerComponent().setupTestCustomer();
		Customer customer = customerComponent.findCustomerByCustomerId(TestDeviceComponent.clientId);
		customerComponent.updateCustomer(null);
		customer.setAccessKey("tacos");
		customerComponent.updateCustomer(customer);
		assertEquals(
				"tacos",
				customerComponent.findCustomerByCustomerId(TestDeviceComponent.clientId).getAccessKey());
	}
}
