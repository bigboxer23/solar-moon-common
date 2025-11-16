package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Alarm;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ResolvedAlertEmailTemplateContentTest {

	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-456";

	@Test
	public void testConstructor_setsCorrectTitle() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertEquals("Alerts have resolved", content.getTitle());
		assertEquals("See resolved alerts", content.getButtonContent());
		assertTrue(content.getBodyContent2().contains("Best regards"));
		assertTrue(content.getBodyContent2().contains("Solar Moon Analytics Team"));
	}

	@Test
	public void testConstructor_withNoCustomer() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotNull(content);
		assertFalse(content.isNotificationEnabled());
	}

	@Test
	public void testConstructor_withEmptyAlarmsList() {
		List<Alarm> alarms = Collections.emptyList();
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotNull(content);
		assertFalse(content.isNotificationEnabled());
	}

	@Test
	public void testConstructor_withMultipleAlarms() {
		List<Alarm> alarms = Arrays.asList(createAlarm(), createAlarm("device-789", "Second alarm resolved"));
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotNull(content);
		assertNotNull(content.getAlarmsToMail());
	}

	@Test
	public void testConstructor_extendsAlarmEmailTemplateContent() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content instanceof AlarmEmailTemplateContent);
		assertEquals("email.template.html", content.getTemplateName());
	}

	@Test
	public void testContentDifferentFromAlarmEmail() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());

		AlarmEmailTemplateContent alarmContent = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);
		ResolvedAlertEmailTemplateContent resolvedContent = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotEquals(alarmContent.getTitle(), resolvedContent.getTitle());
		assertNotEquals(alarmContent.getButtonContent(), resolvedContent.getButtonContent());
		assertNotEquals(alarmContent.getBodyContent2(), resolvedContent.getBodyContent2());
	}

	@Test
	public void testIsNotificationEnabled() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertFalse(content.isNotificationEnabled());
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
