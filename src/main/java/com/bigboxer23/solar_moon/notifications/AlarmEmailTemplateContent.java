package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Data;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Data
public class AlarmEmailTemplateContent extends EmailTemplateContent implements IComponentRegistry {
	private String recipient;

	private String subject;

	private String deviceId;

	private List<Device> devices;

	public AlarmEmailTemplateContent(String customerId, List<Alarm> alarms) {
		super("email.template.html", "Potential issue detected with device", "", "", "See active alarms");
		Optional<Customer> customer = customerComponent.findCustomerByCustomerId(customerId);
		devices = alarms.stream()
				.map(a -> deviceComponent.getDevice(a.getDeviceId(), customerId))
				.filter(Objects::nonNull)
				.filter(d -> {
					if (d.isNotificationsDisabled()) {
						TransactionUtil.addDeviceId(d.getId());
						logger.warn("New notification detected, but not sending email" + " as requested.");
					}
					return !d.isNotificationsDisabled();
				})
				.toList();
		setRecipient(customer.map(Customer::getEmail).orElse(null));
		setCustomerName("Hello " + customer.map(Customer::getName).orElse(null));
		if (devices.size() == 1) {
			singleDevice();
		} else {
			multipleDevices();
		}

		setBodyContent2("If the issue persists or you believe this alert is incorrect, please contact our"
				+ " support team by replying directly to this email. Please provide any"
				+ " additional information or observations that could assist us in diagnosing"
				+ " the problem efficiently.<br/><br/>Best regards,<br/>Solar Moon Analytics"
				+ " Team");
	}

	private void singleDevice() {
		TransactionUtil.addDeviceId(devices.get(0).getId());
		setDeviceId(devices.get(0).getId());
		setSubject("Potential issue with your solar energy device "
				+ devices.get(0).getDisplayName());
		setLink("/alarms?device=" + URLEncoder.encode(devices.get(0).getDisplayName(), StandardCharsets.UTF_8));
		StringBuilder builder = new StringBuilder(
				"There may be an issue with your device, <b>" + devices.get(0).getDisplayName() + "</b>");
		if (!StringUtils.isEmpty(devices.get(0).getSite())) {
			builder.append(" within site ").append(devices.get(0).getSite());
		}
		builder.append(". <br/><br/>Our monitoring system has indicated that your device has stopped"
				+ " responding or is not generating power as expected. Please click the link"
				+ " below to see more detailed information.");
		setBodyContent1(builder.toString());
	}

	private void multipleDevices() {
		TransactionUtil.addDeviceId(null);
		setSubject("Potential issue with your solar energy devices");
		setLink("/alarms");
		StringBuilder builder = new StringBuilder("There may be an issue with some of your devices:<br/>");
		devices.forEach(d -> {
			builder.append("<br/><b>").append(d.getDisplayName()).append("</b>");
			if (!StringUtils.isEmpty(d.getSite())) {
				builder.append(" within site ").append(d.getSite());
			}
		});
		builder.append("<br/><br/>Our monitoring system has indicated that your devices have stopped"
				+ " responding or are not generating power as expected. Please click the link"
				+ " below to see more detailed information.");
		setBodyContent1(builder.toString());
	}

	public boolean isNotificationEnabled() {
		return !devices.isEmpty() && !StringUtils.isBlank(getRecipient());
	}
}
