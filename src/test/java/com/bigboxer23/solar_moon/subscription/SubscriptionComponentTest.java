package com.bigboxer23.solar_moon.subscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionComponentTest {

	@Mock
	private SubscriptionRepository mockRepository;

	@Mock
	private DeviceComponent mockDeviceComponent;

	private TestableSubscriptionComponent subscriptionComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final long CURRENT_TIME = System.currentTimeMillis();
	private static final long TRIAL_VALID_TIME = CURRENT_TIME - (SubscriptionComponent.TRIAL_LENGTH / 2);
	private static final long TRIAL_EXPIRED_TIME = CURRENT_TIME - (SubscriptionComponent.TRIAL_LENGTH + 1000);

	private static class TestableSubscriptionComponent extends SubscriptionComponent {
		private final SubscriptionRepository repository;
		private final DeviceComponent deviceComponent;

		public TestableSubscriptionComponent(SubscriptionRepository repository, DeviceComponent deviceComponent) {
			this.repository = repository;
			this.deviceComponent = deviceComponent;
		}

		@Override
		protected SubscriptionRepository getRepository() {
			return repository;
		}

		@Override
		public boolean canAddAnotherDevice(String customerId) {
			return getSubscriptionDevices(customerId)
					> deviceComponent.getDevicesForCustomerId(customerId).size();
		}
	}

	@BeforeEach
	void setUp() {
		subscriptionComponent = new TestableSubscriptionComponent(mockRepository, mockDeviceComponent);
	}

	@Test
	void testGetSubscription_withValidCustomerId_returnsSubscription() {
		Subscription expectedSubscription = new Subscription(CUSTOMER_ID, 2, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(expectedSubscription));

		Optional<Subscription> result = subscriptionComponent.getSubscription(CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedSubscription, result.get());
		verify(mockRepository).findByCustomerId(CUSTOMER_ID);
	}

	@Test
	void testGetSubscription_withPaidSubscription_setsJoinDateToNegativeOne() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 2, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		Optional<Subscription> result = subscriptionComponent.getSubscription(CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(-1L, result.get().getJoinDate());
	}

	@Test
	void testGetSubscription_withBlankCustomerId_returnsEmpty() {
		Optional<Subscription> result = subscriptionComponent.getSubscription("");

		assertFalse(result.isPresent());
		verify(mockRepository, never()).findByCustomerId(anyString());
	}

	@Test
	void testGetSubscription_withNullCustomerId_returnsEmpty() {
		Optional<Subscription> result = subscriptionComponent.getSubscription(null);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).findByCustomerId(anyString());
	}

	@Test
	void testGetSubscription_noSubscriptionExists_returnsEmpty() {
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Subscription> result = subscriptionComponent.getSubscription(CUSTOMER_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testUpdateSubscription_withValidParameters_updatesSubscription() {
		Subscription existingSubscription = new Subscription(CUSTOMER_ID, 1, TRIAL_VALID_TIME);
		Subscription updatedSubscription = new Subscription(CUSTOMER_ID, 3, TRIAL_VALID_TIME);

		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingSubscription));
		when(mockRepository.update(any(Subscription.class))).thenReturn(Optional.of(updatedSubscription));

		Subscription result = subscriptionComponent.updateSubscription(CUSTOMER_ID, 3);

		assertNotNull(result);
		assertEquals(updatedSubscription, result);
		verify(mockRepository).update(any(Subscription.class));
	}

	@Test
	void testUpdateSubscription_withBlankCustomerId_returnsNull() {
		Subscription result = subscriptionComponent.updateSubscription("", 2);

		assertNull(result);
		verify(mockRepository, never()).update(any(Subscription.class));
	}

	@Test
	void testUpdateSubscription_withInvalidSeats_returnsNull() {
		Subscription result = subscriptionComponent.updateSubscription(CUSTOMER_ID, -3);

		assertNull(result);
		verify(mockRepository, never()).update(any(Subscription.class));
	}

	@Test
	void testUpdateSubscription_newSubscription_usesCurrentTime() {
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
		when(mockRepository.update(any(Subscription.class)))
				.thenReturn(Optional.of(new Subscription(CUSTOMER_ID, 2, CURRENT_TIME)));

		Subscription result = subscriptionComponent.updateSubscription(CUSTOMER_ID, 2);

		assertNotNull(result);
		verify(mockRepository).update(any(Subscription.class));
	}

	@Test
	void testDeleteSubscription_deletesSubscription() {
		subscriptionComponent.deleteSubscription(CUSTOMER_ID);

		verify(mockRepository).delete(any(Subscription.class));
	}

	@Test
	void testGetSubscriptionDevices_withPaidSubscription_returnsCorrectDeviceCount() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 3, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		long result = subscriptionComponent.getSubscriptionDevices(CUSTOMER_ID);

		assertEquals(3 * SubscriptionComponent.DEVICES_PER_SUBSCRIPTION, result);
	}

	@Test
	void testGetSubscriptionDevices_withValidTrial_returnsTrialDeviceCount() {
		Subscription subscription = new Subscription(CUSTOMER_ID, SubscriptionComponent.TRIAL_MODE, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		long result = subscriptionComponent.getSubscriptionDevices(CUSTOMER_ID);

		assertEquals(SubscriptionComponent.TRIAL_DEVICE_COUNT, result);
	}

	@Test
	void testGetSubscriptionDevices_withExpiredTrial_returnsNegativeValue() {
		Subscription subscription = new Subscription(CUSTOMER_ID, SubscriptionComponent.TRIAL_MODE, TRIAL_EXPIRED_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		long result = subscriptionComponent.getSubscriptionDevices(CUSTOMER_ID);

		assertEquals(SubscriptionComponent.TRIAL_MODE * SubscriptionComponent.DEVICES_PER_SUBSCRIPTION, result);
	}

	@Test
	void testGetSubscriptionDevices_noSubscription_returnsZero() {
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		long result = subscriptionComponent.getSubscriptionDevices(CUSTOMER_ID);

		assertEquals(0, result);
	}

	@Test
	void testCanAddAnotherDevice_withAvailableSlots_returnsTrue() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 2, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));
		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID))
				.thenReturn(Collections.nCopies(10, new Device()));

		boolean result = subscriptionComponent.canAddAnotherDevice(CUSTOMER_ID);

		assertTrue(result);
	}

	@Test
	void testCanAddAnotherDevice_withNoAvailableSlots_returnsFalse() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 2, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));
		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID))
				.thenReturn(Collections.nCopies(40, new Device()));

		boolean result = subscriptionComponent.canAddAnotherDevice(CUSTOMER_ID);

		assertFalse(result);
	}

	@Test
	void testIsTrialValid_withValidTrial_returnsTrue() {
		Subscription subscription = new Subscription(CUSTOMER_ID, -1, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		boolean result = subscriptionComponent.isTrialValid(CUSTOMER_ID);

		assertTrue(result);
	}

	@Test
	void testIsTrialValid_withExpiredTrial_returnsFalse() {
		Subscription subscription = new Subscription(CUSTOMER_ID, -1, TRIAL_EXPIRED_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		boolean result = subscriptionComponent.isTrialValid(CUSTOMER_ID);

		assertFalse(result);
	}

	@Test
	void testIsTrialValid_noSubscription_returnsFalse() {
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

		boolean result = subscriptionComponent.isTrialValid(CUSTOMER_ID);

		assertFalse(result);
	}

	@Test
	void testAddSubscriptionInformation_withFewDevices_addsSubscription() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 1, TRIAL_VALID_TIME);
		when(mockRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(subscription));

		TestHasSubscription data = new TestHasSubscription(5);
		subscriptionComponent.addSubscriptionInformation(data, CUSTOMER_ID);

		assertNotNull(data.getSubscription());
		assertEquals(subscription, data.getSubscription());
	}

	@Test
	void testAddSubscriptionInformation_withManyDevices_doesNotAddSubscription() {
		TestHasSubscription data = new TestHasSubscription(15);
		subscriptionComponent.addSubscriptionInformation(data, CUSTOMER_ID);

		assertNull(data.getSubscription());
		verify(mockRepository, never()).findByCustomerId(anyString());
	}

	@Test
	void testAddSubscriptionInformation_withNullData_doesNotThrowException() {
		subscriptionComponent.addSubscriptionInformation(null, CUSTOMER_ID);

		verify(mockRepository, never()).findByCustomerId(anyString());
	}

	@Data
	private static class TestHasSubscription implements IHasSubscription {
		private final List<Device> devices;
		private Subscription subscription;

		public TestHasSubscription(int deviceCount) {
			this.devices = Collections.nCopies(deviceCount, new Device());
		}
	}
}
