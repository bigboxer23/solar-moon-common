package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

public class AlarmEmailTemplateContentTest {

	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-456";
	private static final String SITE_ID = "site-789";

	private CustomerComponent originalCustomerComponent;
	private DeviceComponent originalDeviceComponent;

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
	public void testConstructor_withSingleDevice() throws Exception {
		CustomerComponent mockCustomerComponent = mock(CustomerComponent.class);
		DeviceComponent mockDeviceComponent = mock(DeviceComponent.class);

		Customer customer = createCustomer("John Doe", "john@example.com");
		Device device = createDevice("Solar Panel 1", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		injectMocks(mockCustomerComponent, mockDeviceComponent);

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.isNotificationEnabled());
		assertEquals("john@example.com", content.getRecipient());
		assertEquals("Hello John Doe", content.getCustomerName());
		assertNotNull(content.getSubject());
		assertTrue(content.getSubject().contains("🚨 ALERT"));
		assertTrue(content.getSubject().contains("Solar Panel 1"));
		assertNotNull(content.getBodyContent1());
		assertTrue(content.getBodyContent1().contains("Solar Panel 1"));
		assertNotNull(content.getLink());
		assertTrue(content.getLink().contains("/alerts"));
	}

	@Test
	public void testConstructor_withSingleDeviceAndSite() throws Exception {
		CustomerComponent mockCustomerComponent = mock(CustomerComponent.class);
		DeviceComponent mockDeviceComponent = mock(DeviceComponent.class);

		Customer customer = createCustomer("Jane Smith", "jane@example.com");
		Device device = createDevice("Inverter 1", false);
		device.setSiteId(SITE_ID);
		device.setSite("Main Campus");

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		injectMocks(mockCustomerComponent, mockDeviceComponent);

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.isNotificationEnabled());
		assertTrue(content.getBodyContent1().contains("Main Campus"));
	}

	@Test
	public void testConstructor_withMultipleDevices() throws Exception {
		CustomerComponent mockCustomerComponent = mock(CustomerComponent.class);
		DeviceComponent mockDeviceComponent = mock(DeviceComponent.class);

		Customer customer = createCustomer("Bob Johnson", "bob@example.com");
		Device device1 = createDevice("Solar Panel 1", false);
		Device device2 = createDevice("Solar Panel 2", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(eq(DEVICE_ID), eq(CUSTOMER_ID))).thenReturn(Optional.of(device1));
		when(mockDeviceComponent.findDeviceById(eq("device-789"), eq(CUSTOMER_ID)))
				.thenReturn(Optional.of(device2));

		injectMocks(mockCustomerComponent, mockDeviceComponent);

		List<Alarm> alarms = Arrays.asList(createAlarm(), createAlarm("device-789", "Second alarm"));
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertTrue(content.isNotificationEnabled());
		assertEquals("bob@example.com", content.getRecipient());
		assertNotNull(content.getSubject());
		assertTrue(content.getSubject().contains("🚨 ALERT"));
		assertTrue(content.getSubject().contains("2"));
		assertTrue(content.getBodyContent1().contains("Solar Panel 1"));
		assertTrue(content.getBodyContent1().contains("Solar Panel 2"));
	}

	@Test
	public void testConstructor_withDisabledNotifications() throws Exception {
		CustomerComponent mockCustomerComponent = mock(CustomerComponent.class);
		DeviceComponent mockDeviceComponent = mock(DeviceComponent.class);

		Customer customer = createCustomer("Alice Brown", "alice@example.com");
		Device device = createDevice("Solar Panel 1", true);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		injectMocks(mockCustomerComponent, mockDeviceComponent);

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertFalse(content.isNotificationEnabled());
	}

	@Test
	public void testConstructor_withCustomerNoName() throws Exception {
		CustomerComponent mockCustomerComponent = mock(CustomerComponent.class);
		DeviceComponent mockDeviceComponent = mock(DeviceComponent.class);

		Customer customer = createCustomer(null, "noreply@example.com");
		Device device = createDevice("Device 1", false);

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		injectMocks(mockCustomerComponent, mockDeviceComponent);

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertEquals("Hello noreply@example.com", content.getCustomerName());
	}

	@Test
	public void testConstructor_withDeviceNotFound() throws Exception {
		CustomerComponent mockCustomerComponent = mock(CustomerComponent.class);
		DeviceComponent mockDeviceComponent = mock(DeviceComponent.class);

		Customer customer = createCustomer("Test User", "test@example.com");

		when(mockCustomerComponent.findCustomerByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		injectMocks(mockCustomerComponent, mockDeviceComponent);

		List<Alarm> alarms = Collections.singletonList(createAlarm());
		AlarmEmailTemplateContent content = new AlarmEmailTemplateContent(CUSTOMER_ID, alarms);

		assertFalse(content.isNotificationEnabled());
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
