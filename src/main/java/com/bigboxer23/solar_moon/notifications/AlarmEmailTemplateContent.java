package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.Data;

/** */
@Data
public class AlarmEmailTemplateContent extends EmailTemplateContent implements IComponentRegistry {
	private String recipient;

	private String subject;

	public AlarmEmailTemplateContent(String customerId, DeviceData deviceData, Alarm alarm) {
		super("email.template.html", "Problem detected with device", "", "", "Go to active alarms");
		Customer customer = customerComponent.findCustomerByCustomerId(customerId);
		setRecipient(customer.getEmail());
		setCustomerName(customer.getName());
		Device device = deviceComponent.getDevice(deviceData.getDeviceId(), deviceData.getCustomerId());
		setLink("/alarms?device=" + URLEncoder.encode(device.getDisplayName(), StandardCharsets.UTF_8));
		setBodyContent1("A problem has been detected with "
				+ device.getDisplayName()
				+ ". Please click the link below to see more information.");
		setSubject("Problem detected with device " + device.getDisplayName());
	}
}
