package com.bigboxer23.solar_moon.customer;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.data.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
// @ActiveProfiles("test")
public class TestCustomerComponent implements IComponentRegistry, TestConstants {
	protected static final String CUSTOMER_EMAIL = "noreply@solarmoonanalytics.com";
	protected static final String CUSTOMER_NAME = "mr fake customer";
	protected static final String CUSTOMER_ACCESS_KEY = "4ab84ed3-0ce1-4615-b919-c34c7b619702";

	protected static final String CUSTOMER_STRIPE_ID = "ST_id_1234";

	private final Customer testCustomer = new Customer();

	@Test
	public void testFindCustomerByStripeId() {
		assertNull(customerComponent.findCustomerByStripeCustomerId(null));
		assertNull(customerComponent.findCustomerByStripeCustomerId(""));
		assertNull(customerComponent.findCustomerByStripeCustomerId("1234"));
		assertNotNull(customerComponent.findCustomerByStripeCustomerId(CUSTOMER_STRIPE_ID));
	}

	@Test
	public void testFindCustomerByEmail() {
		assertNull(customerComponent.findCustomerByEmail(null));
		assertNull(customerComponent.findCustomerByEmail(""));
		assertNull(customerComponent.findCustomerByEmail("1234"));
		assertNotNull(customerComponent.findCustomerByEmail(CUSTOMER_EMAIL));
	}

	@Test
	public void testFindCustomerByAccessKey() {
		testCustomer.setAccessKey(CUSTOMER_ACCESS_KEY);
		customerComponent.updateCustomer(testCustomer);
		assertNull(customerComponent.findCustomerIdByAccessKey(null));
		assertNull(customerComponent.findCustomerIdByAccessKey(""));
		assertNull(customerComponent.findCustomerIdByAccessKey("1234"));
		assertNotNull(customerComponent.findCustomerIdByAccessKey(CUSTOMER_ACCESS_KEY));
	}

	@BeforeEach
	protected void beforeEach() {
		testCustomer.setCustomerId(CUSTOMER_ID);
		testCustomer.setEmail(CUSTOMER_EMAIL);
		testCustomer.setStripeCustomerId(CUSTOMER_STRIPE_ID);
		customerComponent.addCustomer(CUSTOMER_EMAIL, CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_STRIPE_ID);
	}

	@AfterEach
	protected void afterEach() {
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
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
				null, CUSTOMER_ID, TestCustomerComponent.CUSTOMER_NAME, TestCustomerComponent.CUSTOMER_STRIPE_ID));
		assertNull(customerComponent.addCustomer("", "", "", ""));
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
		customerComponent.addCustomer(
				TestCustomerComponent.CUSTOMER_EMAIL,
				CUSTOMER_ID,
				TestCustomerComponent.CUSTOMER_NAME,
				TestCustomerComponent.CUSTOMER_STRIPE_ID);
		assertNotNull(customerComponent.findCustomerByCustomerId(CUSTOMER_ID));
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
	}

	@Test
	public void testDeleteCustomer() {
		customerComponent.deleteCustomerByEmail(null);
		customerComponent.deleteCustomerByEmail("");
		customerComponent.deleteCustomerByCustomerId(null);
		customerComponent.deleteCustomerByCustomerId("");
		assertTrue(customerComponent.findCustomerByCustomerId(CUSTOMER_ID).isPresent());
		customerComponent.deleteCustomerByEmail(TestCustomerComponent.CUSTOMER_EMAIL);
		assertFalse(customerComponent.findCustomerByCustomerId(CUSTOMER_ID).isPresent());
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
		assertFalse(customerComponent.findCustomerByCustomerId(CUSTOMER_ID).isPresent());
	}

	@Test
	public void testFindCustomer() {
		assertTrue(customerComponent.findCustomerByCustomerId(CUSTOMER_ID).isPresent());
		assertFalse(customerComponent.findCustomerByCustomerId("tacos").isPresent());
		assertFalse(customerComponent.findCustomerByCustomerId("").isPresent());
		assertFalse(customerComponent.findCustomerByCustomerId(null).isPresent());
	}

	@Test
	public void testUpdateCustomer() {
		Customer customer =
				customerComponent.findCustomerByCustomerId(CUSTOMER_ID).get();
		customerComponent.updateCustomer(null);
		customer.setAccessKey("tacos");
		customerComponent.updateCustomer(customer);
		assertEquals(
				"tacos",
				customerComponent.findCustomerByCustomerId(CUSTOMER_ID).get().getAccessKey());
	}
}
