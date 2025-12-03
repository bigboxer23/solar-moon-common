package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class ResolvedAlertEmailTemplateContentTest {

	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-456";
	private static final String SITE_ID = "site-789";

	private CustomerComponent originalCustomerComponent;
	private DeviceComponent originalDeviceComponent;
	private CustomerComponent mockCustomerComponent;
	private DeviceComponent mockDeviceComponent;

	@org.junit.jupiter.api.BeforeEach
	public void setupMocks() throws Exception {
		mockCustomerComponent = mock(CustomerComponent.class);
		mockDeviceComponent = mock(DeviceComponent.class);
		injectMocks(mockCustomerComponent, mockDeviceComponent);
	}

	@AfterEach
	public void restoreComponents() throws Exception {
		if (originalCustomerComponent != null) {
			setFinalStatic(IComponentRegistry.class.getField("customerComponent"), originalCustomerComponent);
		}
		if (originalDeviceComponent != null) {
			setFinalStatic(IComponentRegistry.class.getField("deviceComponent"), originalDeviceComponent);
		}
	}

	@Test
	public void testConstructor_setsCorrectTitleAndContent() {
		Customer customer = createCustomer("John Doe", "john@example.com");
		Device device = createDevice("Solar Panel 1", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

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
	public void testConstructor_withSingleDeviceResolution() {
		Customer customer = createCustomer("Jane Smith", "jane@example.com");
		Device device = createDevice("Inverter 1", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.getSubject().contains("🟢 RESOLVED"));
		assertTrue(content.getSubject().contains("Inverter 1"));
		assertTrue(content.getSubject().contains("no longer has active alerts"));
		assertTrue(content.getBodyContent1().contains("Inverter 1"));
		assertTrue(content.getBodyContent1().contains("have been resolved"));
		assertTrue(content.getBodyContent1().contains("resumed responding"));
	}

	@Test
	public void testConstructor_withSingleDeviceAndSite() {
		Customer customer = createCustomer("Bob Johnson", "bob@example.com");
		Device device = createDevice("Solar Panel 1", false);
		device.setSiteId(SITE_ID);
		device.setSite("Main Campus");

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.getBodyContent1().contains("Main Campus"));
		assertTrue(content.getBodyContent1().contains("Solar Panel 1"));
	}

	@Test
	public void testConstructor_withMultipleDeviceResolutions() {
		Customer customer = createCustomer("Alice Brown", "alice@example.com");
		Device device1 = createDevice("Solar Panel 1", false);
		Device device2 = createDevice("Solar Panel 2", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(eq(DEVICE_ID), eq(CUSTOMER_ID))).thenReturn(Optional.of(device1));
		when(mockDeviceComponent.findDeviceById(eq("device-789"), eq(CUSTOMER_ID)))
				.thenReturn(Optional.of(device2));

		List<Alarm> alarms = Arrays.asList(createAlarm(), createAlarm("device-789", "Second alarm resolved"));
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.getSubject().contains("🟢 RESOLVED"));
		assertTrue(content.getSubject().contains("Alerts for your solar energy devices have resolved"));
		assertTrue(content.getBodyContent1().contains("Solar Panel 1"));
		assertTrue(content.getBodyContent1().contains("Solar Panel 2"));
	}

	@Test
	public void testConstructor_withMultipleDevicesIncludingSite() {
		Customer customer = createCustomer("Charlie Davis", "charlie@example.com");
		Device device1 = createDevice("Inverter A", false);
		device1.setSiteId(SITE_ID);
		device1.setSite("North Campus");

		Device device2 = createDevice("Inverter B", false);
		device2.setSiteId("site-999");
		device2.setSite("South Campus");

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(eq(DEVICE_ID), eq(CUSTOMER_ID))).thenReturn(Optional.of(device1));
		when(mockDeviceComponent.findDeviceById(eq("device-999"), eq(CUSTOMER_ID)))
				.thenReturn(Optional.of(device2));

		List<Alarm> alarms = Arrays.asList(createAlarm(), createAlarm("device-999", "Alarm B"));
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.getBodyContent1().contains("North Campus"));
		assertTrue(content.getBodyContent1().contains("South Campus"));
		assertTrue(content.getBodyContent1().contains("Inverter A"));
		assertTrue(content.getBodyContent1().contains("Inverter B"));
	}

	@Test
	public void testConstructor_extendsAlarmEmailTemplateContent() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertInstanceOf(AlarmEmailTemplateContent.class, content);
		assertEquals("email.template.html", content.getTemplateName());
	}

	@Test
	public void testContentDifferentFromAlarmEmail() {
		Customer customer = createCustomer("Test User", "test@example.com");
		Device device = createDevice("Test Device", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		List<Alarm> alarms = Collections.singletonList(createAlarm());

		AlarmEmailTemplateContent alarmContent = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);
		ResolvedAlertEmailTemplateContent resolvedContent = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertNotEquals(alarmContent.getTitle(), resolvedContent.getTitle());
		assertNotEquals(alarmContent.getButtonContent(), resolvedContent.getButtonContent());
		assertNotEquals(alarmContent.getSubject(), resolvedContent.getSubject());
		assertNotEquals(alarmContent.getBodyContent1(), resolvedContent.getBodyContent1());
		assertNotEquals(alarmContent.getBodyContent2(), resolvedContent.getBodyContent2());
	}

	@Test
	public void testIsNotificationEnabled() {
		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertFalse(content.isNotificationEnabled());
	}

	@Test
	public void testResolvedMessageToneIsDifferent() {
		Customer customer = createCustomer("Test User", "test@example.com");
		Device device = createDevice("Test Device", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		ResolvedAlertEmailTemplateContent content = new ResolvedAlertEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.getBodyContent1().contains("resolved"));
		assertFalse(content.getBodyContent2().contains("contact our support team"));
	}

	private void injectMocks(CustomerComponent customerComponent, DeviceComponent deviceComponent) throws Exception {
		originalCustomerComponent = IComponentRegistry.customerComponent;
		originalDeviceComponent = IComponentRegistry.deviceComponent;

		setFinalStatic(IComponentRegistry.class.getField("customerComponent"), customerComponent);
		setFinalStatic(IComponentRegistry.class.getField("deviceComponent"), deviceComponent);
	}

	private void setFinalStatic(Field field, Object newValue) throws Exception {
		field.setAccessible(true);

		java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

		Object staticFieldBase = unsafe.staticFieldBase(field);
		long staticFieldOffset = unsafe.staticFieldOffset(field);
		unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
	}

	private Customer createCustomer(String name, String email) {
		Customer customer = new Customer();
		customer.setCustomerId(CUSTOMER_ID);
		customer.setName(name);
		customer.setEmail(email);
		return customer;
	}

	private Device createDevice(String name, boolean notificationsDisabled) {
		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setName(name);
		device.setNotificationsDisabled(notificationsDisabled);
		device.setVirtual(false);
		return device;
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
