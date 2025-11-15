package com.bigboxer23.solar_moon.subscription;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class SubscriptionComponent {

	public static final int DEVICES_PER_SUBSCRIPTION = 20;

	public static final int TRIAL_MODE = -1;

	public static final int TRIAL_DEVICE_COUNT = 10;

	public static final long TRIAL_LENGTH = TimeConstants.NINETY_DAYS;

	private SubscriptionRepository repository;

	protected SubscriptionRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbSubscriptionRepository();
		}
		return repository;
	}

	public Optional<Subscription> getSubscription(String customerId) {
		if (StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return getRepository().findByCustomerId(customerId).map(subscription -> {
			if (subscription.getPacks() > 0) {
				subscription.setJoinDate(-1L);
			}
			return subscription;
		});
	}

	public Subscription updateSubscription(String customerId, int seats) {
		if (StringUtils.isBlank(customerId) || seats < -2) {
			log.warn("invalid customer passed " + seats);
			return null;
		}
		long joinDate =
				getSubscription(customerId).map(Subscription::getJoinDate).orElse(System.currentTimeMillis());
		log.warn("Updating subscription: " + customerId + " " + seats);
		Subscription subscription = new Subscription(customerId, seats, joinDate);
		return getRepository().update(subscription).orElse(null);
	}

	public void deleteSubscription(String customerId) {
		log.warn("Deleting subscription: " + customerId);
		getRepository().delete(new Subscription(customerId, 0, -1L));
	}

	public long getSubscriptionDevices(String customerId) {
		Optional<Subscription> subscription = getSubscription(customerId);
		int maxDevices = subscription.map(Subscription::getPacks).orElse(0) * DEVICES_PER_SUBSCRIPTION;
		if (maxDevices < 0
				&& subscription.map(Subscription::getJoinDate).orElse(-1L)
						> (System.currentTimeMillis() - TRIAL_LENGTH)) { // Trial mode
			return TRIAL_DEVICE_COUNT;
		}
		return maxDevices;
	}

	public boolean canAddAnotherDevice(String customerId) {
		return getSubscriptionDevices(customerId)
				> IComponentRegistry.deviceComponent
						.getDevicesForCustomerId(customerId)
						.size();
	}

	public boolean isTrialValid(String customerId) {
		return getSubscription(customerId)
				.map(Subscription::getJoinDate)
				.map(joinDate -> joinDate > System.currentTimeMillis() - SubscriptionComponent.TRIAL_LENGTH)
				.orElse(false);
	}

	/** Maybe trial, add the date. If more devices, obviously not a trial, don't worry about it */
	public void addSubscriptionInformation(IHasSubscription data, String customerId) {
		if (data != null && data.getDevices().size() < SubscriptionComponent.TRIAL_DEVICE_COUNT) {
			data.setSubscription(getSubscription(customerId).orElse(null));
		}
	}
}
