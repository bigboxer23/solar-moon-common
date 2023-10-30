package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Subscription;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class TestSubscriptionComponent
{
	private SubscriptionComponent component = new SubscriptionComponent();

	@Test
	public void testSubscription() {
		Subscription sub = component.updateSubscription(TestDeviceComponent.clientId, 1);
		assertNotNull(sub);
		assertEquals(1, sub.getPacks());
		assertEquals(TestDeviceComponent.clientId, sub.getCustomerId());
		assertEquals(1, component.getSubscriptionPacks(TestDeviceComponent.clientId));
		component.updateSubscription(TestDeviceComponent.clientId, 0);
		assertEquals(0, component.getSubscriptionPacks(TestDeviceComponent.clientId));
		assertEquals(0, component.getSubscriptionPacks("1234"));
	}
}
