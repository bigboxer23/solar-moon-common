package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Customer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** */
// @ActiveProfiles("test")
public class TestCustomerComponent {
	protected static final String CUSTOMER_EMAIL = "noop@noreply.org";
	protected static final String CUSTOMER_NAME = "mr fake customer";
	protected static final String CUSTOMER_ACCESS_KEY = "4ab84ed3-0ce1-4615-b919-c34c7b619702";

	protected static final String CUSTOMER_STRIPE_ID = "ST_id_1234";
	private CustomerComponent component = new CustomerComponent();

	private Customer testCustomer = new Customer();

	@Test
	public void testFindCustomerByStripeId() {
		setupTestCustomer();
		assertNull(component.findCustomerByStripeCustomerId(null));
		assertNull(component.findCustomerByStripeCustomerId(""));
		assertNull(component.findCustomerByStripeCustomerId("1234"));
		assertNotNull(component.findCustomerByStripeCustomerId(CUSTOMER_STRIPE_ID));
	}

	@Test
	public void testFindCustomerByAccessKey() {
		setupTestCustomer();
		assertNull(component.findCustomerIdByAccessKey(null));
		assertNull(component.findCustomerIdByAccessKey(""));
		assertNull(component.findCustomerIdByAccessKey("1234"));
		assertNotNull(component.findCustomerIdByAccessKey(CUSTOMER_ACCESS_KEY));
	}

	protected void setupTestCustomer() {
		try {
			component.getTable().putItem(testCustomer);
		} catch (DynamoDbException e) {
			testCustomer.setCustomerId(TestDeviceComponent.clientId);
			testCustomer.setEmail(CUSTOMER_EMAIL);
			testCustomer.setAccessKey(CUSTOMER_ACCESS_KEY);
			testCustomer.setStripeCustomerId(CUSTOMER_STRIPE_ID);
			Customer dbCustomer = component.getTable().getItem(testCustomer);
			if (dbCustomer != null) {
				component.getTable().deleteItem(dbCustomer);
			}
			component.getTable().putItem(testCustomer);
			return;
		}
		fail();
	}
}
