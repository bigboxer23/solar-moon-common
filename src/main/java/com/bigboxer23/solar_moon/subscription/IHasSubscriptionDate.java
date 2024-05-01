package com.bigboxer23.solar_moon.subscription;

import com.bigboxer23.solar_moon.data.Device;
import java.util.List;

/** */
public interface IHasSubscriptionDate {
	void setTrialDate(long trialDate);

	List<Device> getDevices();
}
