package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.Data;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Data
public class AlarmEmailTemplateContent extends EmailTemplateContent implements IComponentRegistry {
	private String recipient;

	private String subject;

	private String deviceId;

	private Device device;

	public AlarmEmailTemplateContent(String customerId, DeviceData deviceData, Alarm alarm) {
		super("email.template.html", "Potential issue detected with device", "", "", "See active alarms");
		Customer customer = customerComponent.findCustomerByCustomerId(customerId);
		device = deviceComponent.getDevice(deviceData.getDeviceId(), deviceData.getCustomerId());
		setRecipient(customer.getEmail());
		setCustomerName("Hello " + customer.getName());
		setDeviceId(device.getId());
		setSubject("Potential issue with your solar energy device " + device.getDisplayName());
		setLink("/alarms?device=" + URLEncoder.encode(device.getDisplayName(), StandardCharsets.UTF_8));
		StringBuilder builder =
				new StringBuilder("There may be an issue with your device, <b>" + device.getDisplayName() + "</b>");
		if (!StringUtils.isEmpty(device.getSite())) {
			builder.append(" within site ").append(device.getSite());
		}
		builder.append(". <br/><br/>Our monitoring system has indicated that your device has stopped"
				+ " responding or is not generating power as expected. Please click the link"
				+ " below to see more detailed information.");
		setBodyContent1(builder.toString());
		setBodyContent2("If the issue persists or you believe this alert is incorrect, please contact our"
				+ " support team by replying directly to this email. Please provide any"
				+ " additional information or observations that could assist us in diagnosing"
				+ " the problem efficiently.<br/><br/>Best regards,<br/>Solar Moon Analytics"
				+ " Team");
	}

	public boolean isNotificationEnabled() {
		return !device.isNotificationsDisabled();
	}
}
