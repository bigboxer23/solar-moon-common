package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class ResolvedAlertEmailTemplateContent extends AlarmEmailTemplateContent {
	public ResolvedAlertEmailTemplateContent(String customerId, List<Alarm> alarms) {
		super(customerId, alarms);
		setTitle("Alerts have resolved");
		setButtonContent("See resolved alerts");

		setBodyContent2("<br/>Best regards,<br/>Solar Moon Analytics" + " Team");
	}

	@Override
	protected void singleDevice() {
		super.singleDevice();
		alarmsToMail.keySet().stream().findAny().ifPresent(a -> {
			Device d = alarmsToMail.get(a);
			setSubject("🟢 RESOLVED: " + d.getDisplayName() + " no longer has active alerts");
			StringBuilder builder = new StringBuilder("Alerts for your device, <b>" + d.getDisplayName() + "</b>");
			if (!StringUtils.isEmpty(d.getSiteId())) {
				builder.append(" (").append(d.getSite()).append(")");
			}
			builder.append(", have been resolved! <br/><br/>Our monitoring system has"
					+ " indicates that your device has resumed responding and"
					+ " is generating power as expected. Please click the link"
					+ " below to see more detailed information.");
			setBodyContent1(builder.toString());
		});
	}

	@Override
	protected void multipleDevices() {
		super.multipleDevices();
		setSubject("🟢 RESOLVED: Alerts for your solar energy devices have resolved");
		StringBuilder builder = new StringBuilder("Alerts for the following devices have resolved:<br/>");
		alarmsToMail.forEach((a, d) -> {
			builder.append("<br/><b>").append(d.getDisplayName()).append("</b>");
			if (!StringUtils.isEmpty(d.getSiteId())) {
				builder.append(" (").append(d.getSite()).append(")");
			}
		});
		builder.append(" <br/><br/>Our monitoring system indicates that your devices have resumed"
				+ " responding and are generating power as expected. Please click the link"
				+ " below to see more detailed information.");
		setBodyContent1(builder.toString());
	}
}
