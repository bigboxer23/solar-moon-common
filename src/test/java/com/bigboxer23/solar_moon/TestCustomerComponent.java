package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** */
@ActiveProfiles("test")
public class TestCustomerComponent {
	protected static final String CUSTOMER_EMAIL = "noop@noreply.org";

	protected static final String CUSTOMER_ACCESS_KEY = "4ab84ed3-0ce1-4615-b919-c34c7b619702";

	private CustomerComponent component = new CustomerComponent();

	private Customer testCustomer = new Customer();

	@Test
	public void testFindDeviceByDeviceKey() {
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
