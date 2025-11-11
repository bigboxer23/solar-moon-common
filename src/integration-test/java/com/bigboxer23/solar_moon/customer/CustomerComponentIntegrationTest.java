package com.bigboxer23.solar_moon.customer;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
// @ActiveProfiles("test")
public class CustomerComponentIntegrationTest implements IComponentRegistry, TestConstants {
	private Customer testCustomer;

	@Test
	public void testFindCustomerByStripeId() {
		assertFalse(customerComponent.findCustomerByStripeCustomerId(null).isPresent());
		assertFalse(customerComponent.findCustomerByStripeCustomerId("").isPresent());
		assertFalse(customerComponent.findCustomerByStripeCustomerId("1234").isPresent());
		assertTrue(customerComponent
				.findCustomerByStripeCustomerId(CUSTOMER_STRIPE_ID)
				.isPresent());
	}

	@Test
	public void testFindCustomerByEmail() {
		assertFalse(customerComponent.findCustomerByEmail(null).isPresent());
		assertFalse(customerComponent.findCustomerByEmail("").isPresent());
		assertFalse(customerComponent.findCustomerByEmail("1234").isPresent());
		assertTrue(customerComponent.findCustomerByEmail(CUSTOMER_EMAIL).isPresent());
	}

	@Test
	public void testFindCustomerByAccessKey() {
		testCustomer.setAccessKey(CUSTOMER_ACCESS_KEY);
		customerComponent.updateCustomer(testCustomer);
		assertFalse(customerComponent.findCustomerIdByAccessKey(null).isPresent());
		assertFalse(customerComponent.findCustomerIdByAccessKey("").isPresent());
		assertFalse(customerComponent.findCustomerIdByAccessKey("1234").isPresent());
		assertTrue(
				customerComponent.findCustomerIdByAccessKey(CUSTOMER_ACCESS_KEY).isPresent());
	}

	@BeforeEach
	protected void beforeEach() {
		testCustomer = TestUtils.setupCustomer().orElse(null);
	}

	@AfterEach
	protected void afterEach() {
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
	}

	@Test
	public void testAddCustomer() {
		assertFalse(customerComponent
				.addCustomer(null, null, CustomerComponentIntegrationTest.CUSTOMER_NAME, CustomerComponentIntegrationTest.CUSTOMER_STRIPE_ID)
				.isPresent());
		assertFalse(customerComponent
				.addCustomer(
						CustomerComponentIntegrationTest.CUSTOMER_EMAIL,
						null,
						CustomerComponentIntegrationTest.CUSTOMER_NAME,
						CustomerComponentIntegrationTest.CUSTOMER_STRIPE_ID)
				.isPresent());
		assertFalse(customerComponent
				.addCustomer(
						null,
						CUSTOMER_ID,
						CustomerComponentIntegrationTest.CUSTOMER_NAME,
						CustomerComponentIntegrationTest.CUSTOMER_STRIPE_ID)
				.isPresent());
		assertTrue(customerComponent.addCustomer("", "", "", "").isEmpty());
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
		customerComponent.addCustomer(
				CustomerComponentIntegrationTest.CUSTOMER_EMAIL,
				CUSTOMER_ID,
				CustomerComponentIntegrationTest.CUSTOMER_NAME,
				CustomerComponentIntegrationTest.CUSTOMER_STRIPE_ID);
		assertTrue(customerComponent.findCustomerByCustomerId(CUSTOMER_ID).isPresent());
		assertTrue(customerComponent
				.addCustomer(
						CustomerComponentIntegrationTest.CUSTOMER_EMAIL,
						CUSTOMER_ID,
						CustomerComponentIntegrationTest.CUSTOMER_NAME,
						CustomerComponentIntegrationTest.CUSTOMER_STRIPE_ID)
				.isPresent());
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
	}

	@Test
	public void testDeleteCustomer() {
		customerComponent.deleteCustomerByEmail(null);
		customerComponent.deleteCustomerByEmail("");
		customerComponent.deleteCustomerByCustomerId(null);
		customerComponent.deleteCustomerByCustomerId("");
		assertTrue(customerComponent.findCustomerByCustomerId(CUSTOMER_ID).isPresent());
		customerComponent.deleteCustomerByEmail(CustomerComponentIntegrationTest.CUSTOMER_EMAIL);
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
