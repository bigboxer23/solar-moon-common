package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Alarm;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AlarmEmailTemplateContentTest {

	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-456";

	@Test
	public void testConstructor_withNoCustomer() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotNull(content);
		assertFalse(content.isNotificationEnabled());
	}

	@Test
	public void testConstructor_withEmptyAlarmsList() {
		List<Alarm> alarms = Collections.emptyList();
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotNull(content);
		assertFalse(content.isNotificationEnabled());
	}

	@Test
	public void testConstructor_setsTemplateProperties() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertEquals("email.template.html", content.getTemplateName());
		assertEquals("Potential issue detected with device", content.getTitle());
		assertEquals("See active alerts", content.getButtonContent());
		assertNotNull(content.getBodyContent2());
	}

	@Test
	public void testConstructor_withMultipleAlarms() {
		List<Alarm> alarms = Arrays.asList(createAlarm(), createAlarm("device-789", "Second alarm"));
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotNull(content);
		assertNotNull(content.getAlarmsToMail());
	}

	@Test
	public void testSettersAndGetters() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		content.setRecipient("test@example.com");
		content.setSubject("Test Subject");

		assertEquals("test@example.com", content.getRecipient());
		assertEquals("Test Subject", content.getSubject());
	}

	private Alarm createAlarm() {
		return createAlarm(DEVICE_ID, "Test alarm message");
	}

	private Alarm createAlarm(String deviceId, String message) {
		Alarm alarm = new Alarm();
		alarm.setDeviceId(deviceId);
		alarm.setCustomerId(CUSTOMER_ID);
		alarm.setMessage(message);
		alarm.setStartDate(System.currentTimeMillis());
		return alarm;
	}
}
