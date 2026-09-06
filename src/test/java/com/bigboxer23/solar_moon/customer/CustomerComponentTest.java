package com.bigboxer23.solar_moon.customer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.device.DeviceUpdateComponent;
import com.bigboxer23.solar_moon.device.LinkedDeviceComponent;
import com.bigboxer23.solar_moon.ingest.sma.SMAIngestComponent;
import com.bigboxer23.solar_moon.mapping.MappingComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
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

	@Mock
	private SMAIngestComponent mockSMAIngestComponent;

	@Mock
	private DeviceUpdateComponent mockDeviceUpdateComponent;

	@Mock
	private LinkedDeviceComponent mockLinkedDeviceComponent;

	@Mock
	private SubscriptionComponent mockSubscriptionComponent;

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private MappingComponent mockMappingComponent;

	@Mock
	private OpenSearchComponent mockOpenSearchComponent;

	private TestableCustomerComponent customerComponent;

	private static final String CUSTOMER_ID = "customer-123";
	private static final String EMAIL = "test@example.com";
	private static final String NAME = "Test Customer";
	private static final String STRIPE_CUSTOMER_ID = "stripe-123";
	private static final String ACCESS_KEY = "access-key-123";

	private class TestableCustomerComponent extends CustomerComponent {

		@Override
		protected CustomerRepository getRepository() {
			return mockRepository;
		}

		@Override
		protected SMAIngestComponent getSMAIngestComponent() {
			return mockSMAIngestComponent;
		}

		@Override
		protected DeviceUpdateComponent getDeviceUpdateComponent() {
			return mockDeviceUpdateComponent;
		}

		@Override
		protected LinkedDeviceComponent getLinkedDeviceComponent() {
			return mockLinkedDeviceComponent;
		}

		@Override
		protected SubscriptionComponent getSubscriptionComponent() {
			return mockSubscriptionComponent;
		}

		@Override
		protected DeviceComponent getDeviceComponent() {
			return mockDeviceComponent;
		}

		@Override
		protected MappingComponent getMappingComponent() {
			return mockMappingComponent;
		}

		@Override
		protected OpenSearchComponent getOpenSearchComponent() {
			return mockOpenSearchComponent;
		}
	}

	@BeforeEach
	void setUp() {
		customerComponent = new TestableCustomerComponent();
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

	@Test
	void testDeleteCustomerByEmail_withNonExistentCustomer_doesNotDelete() {
		when(mockRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.empty());

		customerComponent.deleteCustomerByEmail(EMAIL);

		verify(mockRepository, never()).findCustomerByCustomerId(anyString());
		verify(mockRepository, never()).delete(any(Customer.class));
	}

	@Test
	void testUpdateCustomer_withAccessKeyChangeRequested_generatesNewKey() {
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
	void testUpdateCustomer_withAccessKeyChangeNotRequested_keepsOldKey() {
		Customer customer = createTestCustomer();
		String oldAccessKey = customer.getAccessKey();

		Customer existingCustomer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertEquals(oldAccessKey, customer.getAccessKey());
		verify(mockRepository).update(customer);
	}

	@Test
	void testFindCustomerByEmail_withNonExistentCustomer_returnsEmpty() {
		when(mockRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.empty());

		Optional<Customer> result = customerComponent.findCustomerByEmail(EMAIL);

		assertFalse(result.isPresent());
		verify(mockRepository).findCustomerByEmail(EMAIL);
	}

	@Test
	void testFindCustomerByCustomerId_withNonExistentCustomer_returnsEmpty() {
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Customer> result = customerComponent.findCustomerByCustomerId(CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository).findCustomerByCustomerId(CUSTOMER_ID);
	}

	@Test
	void testFindCustomerByStripeCustomerId_withNonExistentCustomer_returnsEmpty() {
		when(mockRepository.findCustomerByStripeCustomerId(STRIPE_CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Customer> result = customerComponent.findCustomerByStripeCustomerId(STRIPE_CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository).findCustomerByStripeCustomerId(STRIPE_CUSTOMER_ID);
	}

	@Test
	void testFindCustomerIdByAccessKey_withNonExistentKey_returnsEmpty() {
		when(mockRepository.findCustomerByAccessKey(ACCESS_KEY)).thenReturn(Optional.empty());

		Optional<Customer> result = customerComponent.findCustomerIdByAccessKey(ACCESS_KEY);

		assertFalse(result.isPresent());
		verify(mockRepository).findCustomerByAccessKey(ACCESS_KEY);
	}

	@Test
	void testUpdateCustomer_withExistingStripeCustomerId_preservesIt() {
		Customer customer = createTestCustomer();
		customer.setStripeCustomerId(null);

		Customer existingCustomer = createTestCustomer();
		String originalStripeId = "original-stripe-id";
		existingCustomer.setStripeCustomerId(originalStripeId);

		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertEquals(originalStripeId, customer.getStripeCustomerId());
		verify(mockRepository).update(customer);
	}

	@Test
	void testUpdateCustomer_withEmptyStripeCustomerId_doesNotOverwrite() {
		Customer customer = createTestCustomer();
		customer.setStripeCustomerId("new-stripe-id");

		Customer existingCustomer = createTestCustomer();
		existingCustomer.setStripeCustomerId("");

		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertEquals("new-stripe-id", customer.getStripeCustomerId());
		verify(mockRepository).update(customer);
	}

	@Test
	void testUpdateCustomer_whenNonAdminTriesToBecomeAdmin_blocked() {
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
	void testUpdateCustomer_whenAdminStaysAdmin_allowed() {
		Customer customer = createTestCustomer();
		customer.setAdmin(true);

		Customer existingCustomer = createTestCustomer();
		existingCustomer.setAdmin(true);

		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));

		customerComponent.updateCustomer(customer);

		assertTrue(customer.isAdmin());
		verify(mockRepository).update(customer);
	}

	@Test
	void testAddCustomer_persistsCustomerAndProvisionsAccessKeyFolder() {
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Customer> result = customerComponent.addCustomer(EMAIL, CUSTOMER_ID, NAME, STRIPE_CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(CUSTOMER_ID, result.get().getCustomerId());
		assertEquals(EMAIL, result.get().getEmail());
		assertEquals(STRIPE_CUSTOMER_ID, result.get().getStripeCustomerId());
		assertNotNull(result.get().getAccessKey());
		verify(mockRepository).add(result.get());
		verify(mockSMAIngestComponent).handleAccessKeyChange(null, result.get().getAccessKey());
	}

	@Test
	void testAddCustomer_whenCustomerExists_returnsExistingWithoutProvisioning() {
		Customer existing = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

		Optional<Customer> result = customerComponent.addCustomer(EMAIL, CUSTOMER_ID, NAME, STRIPE_CUSTOMER_ID);

		assertSame(existing, result.orElseThrow());
		verify(mockRepository, never()).add(any(Customer.class));
		verify(mockSMAIngestComponent, never()).handleAccessKeyChange(anyString(), anyString());
	}

	@Test
	void testAddCustomer_generatesDistinctAccessKeysPerCustomer() {
		when(mockRepository.findCustomerByCustomerId(anyString())).thenReturn(Optional.empty());

		String firstKey = customerComponent
				.addCustomer(EMAIL, CUSTOMER_ID, NAME, STRIPE_CUSTOMER_ID)
				.orElseThrow()
				.getAccessKey();
		String secondKey = customerComponent
				.addCustomer("other@example.com", "customer-456", NAME, STRIPE_CUSTOMER_ID)
				.orElseThrow()
				.getAccessKey();

		assertNotEquals(firstKey, secondKey);
	}

	@Test
	void testUpdateCustomer_whenAccessKeyChangeRequested_rotatesKeyAndNotifiesIngest() {
		Customer customer = createTestCustomer();
		customer.setAccessKey("");
		Customer existing = createTestCustomer();
		existing.setAccessKey("old-access-key");
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

		customerComponent.updateCustomer(customer);

		assertFalse(customer.getAccessKey().isEmpty());
		assertNotEquals("old-access-key", customer.getAccessKey());
		verify(mockRepository).update(customer);
		verify(mockSMAIngestComponent).handleAccessKeyChange("old-access-key", customer.getAccessKey());
	}

	@Test
	void testUpdateCustomer_withoutAccessKeyChange_leavesKeyAndSkipsIngest() {
		Customer customer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		customerComponent.updateCustomer(customer);

		assertEquals(ACCESS_KEY, customer.getAccessKey());
		verify(mockSMAIngestComponent, never()).handleAccessKeyChange(any(), any());
	}

	@Test
	void testUpdateCustomer_blocksAdminEscalation() {
		Customer incoming = createTestCustomer();
		incoming.setAdmin(true);
		Customer existing = createTestCustomer();
		existing.setAdmin(false);
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

		customerComponent.updateCustomer(incoming);

		assertFalse(incoming.isAdmin());
		verify(mockRepository).update(incoming);
	}

	@Test
	void testUpdateCustomer_allowsExistingAdminToStayAdmin() {
		Customer incoming = createTestCustomer();
		incoming.setAdmin(true);
		Customer existing = createTestCustomer();
		existing.setAdmin(true);
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

		customerComponent.updateCustomer(incoming);

		assertTrue(incoming.isAdmin());
	}

	@Test
	void testUpdateCustomer_preservesStripeCustomerIdFromDatabase() {
		Customer incoming = createTestCustomer();
		incoming.setStripeCustomerId(null);
		Customer existing = createTestCustomer();
		existing.setStripeCustomerId("stripe-original");
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

		customerComponent.updateCustomer(incoming);

		assertEquals("stripe-original", incoming.getStripeCustomerId());
	}

	@Test
	void testUpdateCustomer_whenDatabaseHasNoStripeId_keepsIncomingValue() {
		Customer incoming = createTestCustomer();
		incoming.setStripeCustomerId("stripe-incoming");
		Customer existing = createTestCustomer();
		existing.setStripeCustomerId("");
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

		customerComponent.updateCustomer(incoming);

		assertEquals("stripe-incoming", incoming.getStripeCustomerId());
	}

	@Test
	void testDeleteCustomerByCustomerId_cascadesToEveryOwnedResource() {
		Customer customer = createTestCustomer();
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));

		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);

		verify(mockDeviceUpdateComponent).deleteByCustomerId(CUSTOMER_ID);
		verify(mockLinkedDeviceComponent).deleteByCustomerId(CUSTOMER_ID);
		verify(mockSubscriptionComponent).deleteSubscription(CUSTOMER_ID);
		verify(mockDeviceComponent).deleteDevicesByCustomerId(CUSTOMER_ID);
		verify(mockMappingComponent).deleteMapping(CUSTOMER_ID);
		verify(mockOpenSearchComponent).deleteByCustomerId(CUSTOMER_ID);
		verify(mockSMAIngestComponent).handleAccessKeyChange(ACCESS_KEY, null);
		verify(mockRepository).delete(customer);
	}

	@Test
	void testDeleteCustomerByCustomerId_whenCustomerMissing_touchesNothing() {
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);

		verify(mockRepository, never()).delete(any(Customer.class));
		verifyNoInteractions(mockDeviceComponent);
		verifyNoInteractions(mockOpenSearchComponent);
		verifyNoInteractions(mockSMAIngestComponent);
	}

	@Test
	void testDeleteCustomerByEmail_resolvesCustomerThenCascades() {
		Customer customer = createTestCustomer();
		when(mockRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(customer));
		when(mockRepository.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));

		customerComponent.deleteCustomerByEmail(EMAIL);

		verify(mockDeviceComponent).deleteDevicesByCustomerId(CUSTOMER_ID);
		verify(mockRepository).delete(customer);
	}

	@Test
	void testDeleteCustomerByEmail_whenEmailUnknown_touchesNothing() {
		when(mockRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.empty());

		customerComponent.deleteCustomerByEmail(EMAIL);

		verify(mockRepository, never()).delete(any(Customer.class));
		verifyNoInteractions(mockDeviceComponent);
	}

	private Customer createTestCustomer() {
		Customer customer = new Customer(CUSTOMER_ID, EMAIL, ACCESS_KEY, NAME);
		customer.setStripeCustomerId(STRIPE_CUSTOMER_ID);
		customer.setActive(true);
		return customer;
	}
}
