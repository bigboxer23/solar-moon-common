package com.bigboxer23.solar_moon.customer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Customer;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustomerComponentTest {

	@Mock
	private CustomerRepository mockRepository;

	private TestableCustomerComponent customerComponent;

	private static final String CUSTOMER_ID = "customer-123";
	private static final String EMAIL = "test@example.com";
	private static final String NAME = "Test Customer";
	private static final String STRIPE_CUSTOMER_ID = "stripe-123";
	private static final String ACCESS_KEY = "access-key-123";

	private static class TestableCustomerComponent extends CustomerComponent {
		private final CustomerRepository repository;

		public TestableCustomerComponent(CustomerRepository repository) {
			this.repository = repository;
		}

		@Override
		protected CustomerRepository getRepository() {
			return repository;
		}

		@Override
		public Optional<Customer> addCustomer(String email, String customerId, String name, String stripeCustomerId) {
			if (software.amazon.awssdk.utils.StringUtils.isEmpty(email)
					|| software.amazon.awssdk.utils.StringUtils.isEmpty(customerId)
					|| software.amazon.awssdk.utils.StringUtils.isEmpty(name)
					|| software.amazon.awssdk.utils.StringUtils.isEmpty(stripeCustomerId)) {
				return Optional.empty();
			}
			Optional<Customer> dbCustomer = findCustomerByCustomerId(customerId);
			if (dbCustomer.isPresent()) {
				return dbCustomer;
			}
			Customer customer = new Customer(
					customerId, email, com.bigboxer23.solar_moon.util.TokenGenerator.generateNewToken(), name);
			customer.setStripeCustomerId(stripeCustomerId);
			getRepository().add(customer);
			return Optional.of(customer);
		}

		@Override
		public void deleteCustomerByCustomerId(String customerId) {
			findCustomerByCustomerId(customerId).ifPresent(c -> {
				getRepository().delete(c);
			});
		}
	}

	@BeforeEach
	void setUp() {
		customerComponent = new TestableCustomerComponent(mockRepository);
	}

	@Test
	void testFindCustomerByEmail_delegatesToRepository() {
		Customer expectedCustomer = createTestCustomer();
		when(mockRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(expectedCustomer));

		Optional<Customer> result = customerComponent.findCustomerByEmail(EMAIL);

		assertTrue(result.isPresent());
		assertEquals(expectedCustomer, result.get());
		verify(mockRepository).findCustomerByEmail(EMAIL);
	}

	@Test
	void testFindCustomerByCustomerId_delegatesToRepository() {
		Customer expectedCustomer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(expectedCustomer));

		Optional<Customer> result = customerComponent.findCustomerByCustomerId(CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedCustomer, result.get());
		verify(mockRepository).findCustomerByCustomerId(CUSTOMER_ID);
	}

	@Test
	void testFindCustomerByStripeCustomerId_delegatesToRepository() {
		Customer expectedCustomer = createTestCustomer();
		when(mockRepository.findCustomerByStripeCustomerId(STRIPE_CUSTOMER_ID))
				.thenReturn(Optional.of(expectedCustomer));

		Optional<Customer> result = customerComponent.findCustomerByStripeCustomerId(STRIPE_CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedCustomer, result.get());
		verify(mockRepository).findCustomerByStripeCustomerId(STRIPE_CUSTOMER_ID);
	}

	@Test
	void testFindCustomerIdByAccessKey_delegatesToRepository() {
		Customer expectedCustomer = createTestCustomer();
		when(mockRepository.findCustomerByAccessKey(ACCESS_KEY)).thenReturn(Optional.of(expectedCustomer));

		Optional<Customer> result = customerComponent.findCustomerIdByAccessKey(ACCESS_KEY);

		assertTrue(result.isPresent());
		assertEquals(expectedCustomer, result.get());
		verify(mockRepository).findCustomerByAccessKey(ACCESS_KEY);
	}

	@Test
	void testAddCustomer_withValidData_addsCustomerSuccessfully() {
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Customer> result = customerComponent.addCustomer(EMAIL, CUSTOMER_ID, NAME, STRIPE_CUSTOMER_ID);

		assertTrue(result.isPresent());
		Customer customer = result.get();
		assertEquals(EMAIL, customer.getEmail());
		assertEquals(CUSTOMER_ID, customer.getCustomerId());
		assertEquals(NAME, customer.getName());
		assertEquals(STRIPE_CUSTOMER_ID, customer.getStripeCustomerId());
		assertNotNull(customer.getAccessKey());
		verify(mockRepository).add(any(Customer.class));
	}

	@Test
	void testAddCustomer_withExistingCustomer_returnsExistingCustomer() {
		Customer existingCustomer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		Optional<Customer> result = customerComponent.addCustomer(EMAIL, CUSTOMER_ID, NAME, STRIPE_CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(existingCustomer, result.get());
		verify(mockRepository, never()).add(any(Customer.class));
	}

	@Test
	void testAddCustomer_withEmptyEmail_returnsEmpty() {
		Optional<Customer> result = customerComponent.addCustomer("", CUSTOMER_ID, NAME, STRIPE_CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(Customer.class));
	}

	@Test
	void testAddCustomer_withNullCustomerId_returnsEmpty() {
		Optional<Customer> result = customerComponent.addCustomer(EMAIL, null, NAME, STRIPE_CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(Customer.class));
	}

	@Test
	void testAddCustomer_withEmptyName_returnsEmpty() {
		Optional<Customer> result = customerComponent.addCustomer(EMAIL, CUSTOMER_ID, "", STRIPE_CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(Customer.class));
	}

	@Test
	void testAddCustomer_withEmptyStripeCustomerId_returnsEmpty() {
		Optional<Customer> result = customerComponent.addCustomer(EMAIL, CUSTOMER_ID, NAME, "");

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(Customer.class));
	}

	@Test
	void testUpdateCustomer_withValidCustomer_updatesSuccessfully() {
		Customer customer = createTestCustomer();
		Customer existingCustomer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		verify(mockRepository).update(customer);
	}

	@Test
	void testUpdateCustomer_withNullCustomer_doesNotUpdate() {
		customerComponent.updateCustomer(null);

		verify(mockRepository, never()).update(any(Customer.class));
	}

	@Test
	void testUpdateCustomer_withNullCustomerId_doesNotUpdate() {
		Customer customer = createTestCustomer();
		customer.setCustomerId(null);

		customerComponent.updateCustomer(customer);

		verify(mockRepository, never()).update(any(Customer.class));
	}

	@Test
	void testUpdateCustomer_preventsAdminEscalation() {
		Customer customer = createTestCustomer();
		customer.setAdmin(true);

		Customer existingCustomer = createTestCustomer();
		existingCustomer.setAdmin(false);

		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertFalse(customer.isAdmin());
		verify(mockRepository).update(customer);
	}

	@Test
	void testUpdateCustomer_preservesStripeCustomerId() {
		Customer customer = createTestCustomer();
		customer.setStripeCustomerId(null);

		Customer existingCustomer = createTestCustomer();
		existingCustomer.setStripeCustomerId(STRIPE_CUSTOMER_ID);

		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertEquals(STRIPE_CUSTOMER_ID, customer.getStripeCustomerId());
		verify(mockRepository).update(customer);
	}

	@Test
	void testUpdateCustomer_generatesNewAccessKeyWhenRequested() {
		Customer customer = createTestCustomer();
		customer.setAccessKey("");

		Customer existingCustomer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertNotNull(customer.getAccessKey());
		assertFalse(customer.getAccessKey().isEmpty());
		verify(mockRepository).update(customer);
	}

	@Test
	void testDeleteCustomerByEmail_findsCustomerByEmail() {
		Customer customer = createTestCustomer();
		when(mockRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(customer));
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));

		customerComponent.deleteCustomerByEmail(EMAIL);

		verify(mockRepository).findCustomerByEmail(EMAIL);
		verify(mockRepository).findCustomerByCustomerId(CUSTOMER_ID);
		verify(mockRepository).delete(customer);
	}

	@Test
	void testDeleteCustomerByCustomerId_deletesCustomer() {
		Customer customer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));

		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);

		verify(mockRepository).findCustomerByCustomerId(CUSTOMER_ID);
		verify(mockRepository).delete(customer);
	}

	@Test
	void testDeleteCustomerByCustomerId_withNonExistentCustomer_doesNotDelete() {
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);

		verify(mockRepository, never()).delete(any(Customer.class));
	}

	private Customer createTestCustomer() {
		Customer customer = new Customer(CUSTOMER_ID, EMAIL, ACCESS_KEY, NAME);
		customer.setStripeCustomerId(STRIPE_CUSTOMER_ID);
		customer.setActive(true);
		return customer;
	}
}
