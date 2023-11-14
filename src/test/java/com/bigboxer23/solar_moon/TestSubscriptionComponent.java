package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Subscription;
import org.junit.jupiter.api.Test;

/** */
public class TestSubscriptionComponent implements IComponentRegistry {
	@Test
	public void testSubscription() {
		Subscription sub = subscriptionComponent.updateSubscription(TestDeviceComponent.clientId, 1);
		assertNotNull(sub);
		assertEquals(1, sub.getPacks());
		assertEquals(TestDeviceComponent.clientId, sub.getCustomerId());
		assertEquals(1, subscriptionComponent.getSubscriptionPacks(TestDeviceComponent.clientId));
		subscriptionComponent.updateSubscription(TestDeviceComponent.clientId, 0);
		assertEquals(0, subscriptionComponent.getSubscriptionPacks(TestDeviceComponent.clientId));
		assertEquals(0, subscriptionComponent.getSubscriptionPacks("1234"));
	}

	@Test
	public void testDeleteSubscription() {
		subscriptionComponent.updateSubscription(TestDeviceComponent.clientId, 1);
		assertEquals(1, subscriptionComponent.getSubscriptionPacks(TestDeviceComponent.clientId));
		subscriptionComponent.deleteSubscription(TestDeviceComponent.clientId);
		assertEquals(0, subscriptionComponent.getSubscriptionPacks(TestDeviceComponent.clientId));
	}
}
