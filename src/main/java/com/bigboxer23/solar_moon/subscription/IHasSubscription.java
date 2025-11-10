package com.bigboxer23.solar_moon.subscription;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.Subscription;

import java.util.List;

/** */
public interface IHasSubscription {
	void setSubscription(Subscription subscription);

	List<Device> getDevices();
}
