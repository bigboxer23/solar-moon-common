package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.utils.StringUtils;

/** */
@EqualsAndHashCode(callSuper = true)
@Data
public class AlarmEmailTemplateContent extends EmailTemplateContent implements IComponentRegistry {
	private String recipient;

	private String subject;

	private String deviceId;

	protected List<Device> devices;

	public AlarmEmailTemplateContent(String customerId, List<Alarm> alarms) {
		super("email.template.html", "Potential issue detected with device", "", "", "See active alerts");
		Optional<Customer> customer = customerComponent.findCustomerByCustomerId(customerId);
		if (customer.isEmpty()) {
			logger.error("cannot find customer for " + customerId);
			return;
		}
		devices = alarms.stream()
				.map(a -> deviceComponent.findDeviceById(a.getDeviceId(), customerId))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(d -> {
					if (d.isNotificationsDisabled()) {
						TransactionUtil.addDeviceId(d.getId(), d.getSiteId());
						logger.warn("New notification detected, but not sending email " + " as requested.");
					}
					return !d.isNotificationsDisabled();
				})
				.toList();
		setRecipient(customer.get().getEmail());
		setCustomerName("Hello "
				+ (StringUtils.isEmpty(customer.get().getName())
						? customer.get().getEmail()
						: customer.get().getName()));
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

	protected void singleDevice() {
		TransactionUtil.addDeviceId(
				devices.getFirst().getId(), devices.getFirst().getSiteId());
		setDeviceId(devices.getFirst().getId());
		setSubject("Potential issue with your solar energy device "
				+ devices.getFirst().getDisplayName());
		setLink("/alerts?deviceId=" + URLEncoder.encode(devices.getFirst().getId(), StandardCharsets.UTF_8));
		StringBuilder builder = new StringBuilder("There may be an issue with your device, <b>"
				+ devices.getFirst().getDisplayName()
				+ "</b>");
		if (!StringUtils.isEmpty(devices.getFirst().getSiteId())) {
			builder.append(" (").append(devices.getFirst().getSite()).append(")");
		}
		builder.append(". <br/><br/>Our monitoring system has indicated that your device has stopped"
				+ " responding or is not generating power as expected. Please click the link"
				+ " below to see more detailed information.");
		setBodyContent1(builder.toString());
	}

	protected void multipleDevices() {
		TransactionUtil.addDeviceId(null, null);
		setSubject("Potential issue with your solar energy devices");
		setLink("/alerts");
		StringBuilder builder = new StringBuilder("There may be an issue with some of your devices:<br/>");
		devices.forEach(d -> {
			builder.append("<br/><b>").append(d.getDisplayName()).append("</b>");
			if (!StringUtils.isEmpty(d.getSiteId())) {
				builder.append(" (").append(d.getSite()).append(")");
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
