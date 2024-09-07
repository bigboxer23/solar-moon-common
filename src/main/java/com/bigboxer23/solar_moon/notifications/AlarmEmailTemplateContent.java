package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	protected Map<Alarm, Device> alarmsToMail = new HashMap<>();

	public AlarmEmailTemplateContent(String customerId, List<Alarm> alarms) {
		super("email.template.html", "Potential issue detected with device", "", "", "See active alerts");
		Optional<Customer> customer = customerComponent.findCustomerByCustomerId(customerId);
		if (customer.isEmpty()) {
			logger.error("cannot find customer for " + customerId);
			return;
		}
		alarms.forEach(a -> deviceComponent
				.findDeviceById(a.getDeviceId(), customerId)
				.map(d -> {
					if (d.isNotificationsDisabled()) {
						TransactionUtil.addDeviceId(d.getId(), d.getSiteId());
						logger.warn("New notification detected, but not sending" + " email  as requested.");
						return null;
					}
					return d;
				})
				.ifPresent(device -> alarmsToMail.put(a, device)));
		setRecipient(customer.get().getEmail());
		setCustomerName("Hello "
				+ (StringUtils.isEmpty(customer.get().getName())
						? customer.get().getEmail()
						: customer.get().getName()));
		if (alarmsToMail.size() == 1) {
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

	private void fillInDeviceBody(StringBuilder builder, Alarm alarm, Device device) {
		builder.append("<br/><b>").append(device.getDisplayName()).append("</b>");
		if (!StringUtils.isEmpty(device.getSiteId())) {
			builder.append(" (").append(device.getSite()).append(")");
		}
		builder.append("<br/><span style='padding-left:15px;'>")
				.append(alarm.getMessage())
				.append("</span>");
	}

	protected void singleDevice() {
		alarmsToMail.keySet().stream().findAny().ifPresent(a -> {
			Device d = alarmsToMail.get(a);
			TransactionUtil.addDeviceId(d.getId(), d.getSiteId());
			setSubject("🚨 ALERT: issue with your solar energy device " + d.getDisplayName());
			setLink("/alerts?deviceId=" + URLEncoder.encode(d.getId(), StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder("There may be an issue with your device:<br/>");
			fillInDeviceBody(builder, a, d);
			builder.append("<br/><br/>Our monitoring system has indicated that your device"
					+ " has stopped responding or is not generating power as"
					+ " expected. Please click the link below to see more"
					+ " detailed information.");
			setBodyContent1(builder.toString());
		});
	}

	protected void multipleDevices() {
		TransactionUtil.addDeviceId(null, null);
		setSubject("🚨 ALERT: issue with " + alarmsToMail.size() + " of your solar energy devices");
		setLink("/alerts");
		StringBuilder builder = new StringBuilder("There may be an issue with some of your devices:<br/>");
		alarmsToMail.forEach((a, d) -> fillInDeviceBody(builder, a, d));
		builder.append("<br/><br/>Our monitoring system has indicated that your devices have stopped"
				+ " responding or are not generating power as expected. Please click the link"
				+ " below to see more detailed information.");
		setBodyContent1(builder.toString());
	}

	public boolean isNotificationEnabled() {
		return !alarmsToMail.isEmpty() && !StringUtils.isBlank(getRecipient());
	}
}
