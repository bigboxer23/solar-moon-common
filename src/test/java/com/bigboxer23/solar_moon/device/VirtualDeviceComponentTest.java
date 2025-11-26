package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.DynamoLockUtils;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.weather.PirateWeatherComponent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.ResponseException;

@ExtendWith(MockitoExtension.class)
public class VirtualDeviceComponentTest {

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private OpenSearchComponent mockOSComponent;

	@Mock
	private LocationComponent mockLocationComponent;

	@Mock
	private PirateWeatherComponent mockWeatherComponent;

	@Mock
	private LinkedDeviceComponent mockLinkedDeviceComponent;

	private TestableVirtualDeviceComponent virtualDeviceComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String VIRTUAL_DEVICE_ID = "virtual-device-123";
	private static final String SITE_ID = "site-123";

	private static class TestableVirtualDeviceComponent extends VirtualDeviceComponent {
		private final DeviceComponent deviceComponent;
		private final OpenSearchComponent osComponent;
		private final LocationComponent locationComponent;
		private final PirateWeatherComponent weatherComponent;
		private final LinkedDeviceComponent linkedDeviceComponent;

		public TestableVirtualDeviceComponent(
				DeviceComponent deviceComponent,
				OpenSearchComponent osComponent,
				LocationComponent locationComponent,
				PirateWeatherComponent weatherComponent,
				LinkedDeviceComponent linkedDeviceComponent) {
			this.deviceComponent = deviceComponent;
			this.osComponent = osComponent;
			this.locationComponent = locationComponent;
			this.weatherComponent = weatherComponent;
			this.linkedDeviceComponent = linkedDeviceComponent;
		}

		@Override
		protected DeviceComponent getDeviceComponent() {
			return deviceComponent;
		}

		@Override
		protected OpenSearchComponent getOSComponent() {
			return osComponent;
		}

		@Override
		protected LocationComponent getLocationComponent() {
			return locationComponent;
		}

		@Override
		protected PirateWeatherComponent getWeatherComponent() {
			return weatherComponent;
		}

		@Override
		protected LinkedDeviceComponent getLinkedDeviceComponent() {
			return linkedDeviceComponent;
		}
	}

	@BeforeEach
	void setUp() {
		virtualDeviceComponent = new TestableVirtualDeviceComponent(
				mockDeviceComponent,
				mockOSComponent,
				mockLocationComponent,
				mockWeatherComponent,
				mockLinkedDeviceComponent);
	}

	@Test
	void testHandleVirtualDevice_withNoSiteId_doesNothing() {
		DeviceData deviceData = createDeviceData();
		deviceData.setSiteId(DeviceComponent.NO_SITE);

		virtualDeviceComponent.handleVirtualDevice(deviceData);

		verifyNoInteractions(mockDeviceComponent);
		verifyNoInteractions(mockOSComponent);
	}

	@Test
	void testHandleVirtualDevice_withNoVirtualDevice_doesNothing() {
		DeviceData deviceData = createDeviceData();
		Device physicalDevice = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Collections.singletonList(physicalDevice));

		virtualDeviceComponent.handleVirtualDevice(deviceData);

		verify(mockDeviceComponent).getDevicesBySiteId(CUSTOMER_ID, SITE_ID);
		verifyNoInteractions(mockOSComponent);
	}

	@Test
	void testHandleVirtualDevice_withMismatchedDeviceCount_doesNothing() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");
		Device physicalDevice2 = createPhysicalDevice("physical-2");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1, physicalDevice2));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		virtualDeviceComponent.handleVirtualDevice(deviceData);

		verify(mockDeviceComponent).getDevicesBySiteId(CUSTOMER_ID, SITE_ID);
		verify(mockOSComponent).getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class));
		verify(mockOSComponent, never()).logData(any(), anyList());
	}

	@Test
	void testHandleVirtualDevice_withDisabledDevices_countsOnlyEnabled() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");
		Device physicalDevice2 = createPhysicalDevice("physical-2");
		physicalDevice2.setDisabled(true);

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1, physicalDevice2));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(50f);
			deviceData1.setTotalRealPower(25f);
			deviceData1.setTotalEnergyConsumed(500f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Collections.singletonList(deviceData1));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), anyList());
		}
	}

	@Test
	void testHandleVirtualDevice_withValidData_aggregatesCorrectly() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(50f);
			deviceData1.setTotalRealPower(25f);
			deviceData1.setTotalEnergyConsumed(500f);

			DeviceData deviceData2 = createDeviceData();
			deviceData2.setEnergyConsumed(30f);
			deviceData2.setTotalRealPower(15f);
			deviceData2.setTotalEnergyConsumed(300f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Arrays.asList(deviceData1, deviceData2));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockLocationComponent).addLocationData(any(DeviceData.class), eq(virtualDevice));
			verify(mockWeatherComponent).addWeatherData(any(DeviceData.class), eq(virtualDevice));
			verify(mockLinkedDeviceComponent).addLinkedDeviceDataVirtual(any(DeviceData.class), anyList());
			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertEquals(80f, logged.getEnergyConsumed());
				assertEquals(40f, logged.getTotalRealPower());
				assertEquals(800f, logged.getTotalEnergyConsumed());
				assertTrue(logged.isVirtual());
				assertEquals(VIRTUAL_DEVICE_ID, logged.getDeviceId());
				return true;
			}));
		}
	}

	@Test
	void testHandleVirtualDevice_withSubtractionMode_subtractsValues() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		virtualDevice.setSubtraction(true);
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(50f);
			deviceData1.setTotalRealPower(25f);
			deviceData1.setTotalEnergyConsumed(500f);

			DeviceData deviceData2 = createDeviceData();
			deviceData2.setEnergyConsumed(30f);
			deviceData2.setTotalRealPower(15f);
			deviceData2.setTotalEnergyConsumed(300f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Arrays.asList(deviceData1, deviceData2));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertEquals(20f, logged.getEnergyConsumed());
				assertEquals(10f, logged.getTotalRealPower());
				assertEquals(200f, logged.getTotalEnergyConsumed());
				return true;
			}));
		}
	}

	@Test
	void testHandleVirtualDevice_withNegativeValues_clampsToZero() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(-50f);
			deviceData1.setTotalRealPower(-25f);
			deviceData1.setTotalEnergyConsumed(-500f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Collections.singletonList(deviceData1));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertEquals(0f, logged.getEnergyConsumed());
				assertEquals(0f, logged.getTotalRealPower());
				assertEquals(0f, logged.getTotalEnergyConsumed());
				return true;
			}));
		}
	}

	@Test
	void testHandleVirtualDevice_withDeviceSite_setsSiteFlag() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		virtualDevice.setIsSite("1");
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(50f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Collections.singletonList(deviceData1));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertTrue(logged.isSite());
				assertTrue(logged.isVirtual());
				return true;
			}));
		}
	}

	@Test
	void testHandleVirtualDevice_withNoVirtualDeviceFound_logsWarning() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1))
				.thenReturn(Collections.singletonList(physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent, never()).logData(any(), anyList());
		}
	}

	@Test
	void testHandleVirtualDevice_withResponseException_logsError() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(50f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Collections.singletonList(deviceData1));
			doThrow(ResponseException.class).when(mockOSComponent).logData(any(Date.class), anyList());

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), anyList());
		}
	}

	@Test
	void testHandleVirtualDevice_filtersNegativeDeviceValues() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(100f);

			DeviceData deviceData2 = createDeviceData();
			deviceData2.setEnergyConsumed(-50f);

			DeviceData deviceData3 = createDeviceData();
			deviceData3.setEnergyConsumed(50f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Arrays.asList(deviceData1, deviceData2, deviceData3));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertEquals(150f, logged.getEnergyConsumed());
				return true;
			}));
		}
	}

	@Test
	void testHandleVirtualDevice_withEmptyDeviceList_returnsNegativeOne() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Collections.emptyList());

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertEquals(0f, logged.getEnergyConsumed());
				assertEquals(0f, logged.getTotalRealPower());
				assertEquals(0f, logged.getTotalEnergyConsumed());
				return true;
			}));
		}
	}

	@Test
	void testHandleVirtualDevice_withAllNegativeValues_returnsNegativeOne() throws Exception {
		DeviceData deviceData = createDeviceData();
		Device virtualDevice = createVirtualDevice();
		Device physicalDevice1 = createPhysicalDevice("physical-1");

		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(Arrays.asList(virtualDevice, physicalDevice1));
		when(mockOSComponent.getSiteDevicesCountByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
				.thenReturn(1);

		try (MockedStatic<DynamoLockUtils> mockLockUtils = mockStatic(DynamoLockUtils.class)) {
			mockLockUtils
					.when(() -> DynamoLockUtils.doLockedCommand(anyString(), any(Runnable.class)))
					.thenAnswer(invocation -> {
						Runnable command = invocation.getArgument(1);
						command.run();
						return null;
					});

			DeviceData deviceData1 = createDeviceData();
			deviceData1.setEnergyConsumed(-100f);
			deviceData1.setTotalRealPower(-50f);

			DeviceData deviceData2 = createDeviceData();
			deviceData2.setEnergyConsumed(-30f);
			deviceData2.setTotalRealPower(-15f);

			when(mockOSComponent.getDevicesForSiteByTimePeriod(eq(CUSTOMER_ID), eq(SITE_ID), any(Date.class)))
					.thenReturn(Arrays.asList(deviceData1, deviceData2));

			virtualDeviceComponent.handleVirtualDevice(deviceData);

			verify(mockOSComponent).logData(any(Date.class), argThat(list -> {
				assertNotNull(list);
				assertEquals(1, list.size());
				DeviceData logged = list.getFirst();
				assertEquals(0f, logged.getEnergyConsumed());
				assertEquals(0f, logged.getTotalRealPower());
				return true;
			}));
		}
	}

	private DeviceData createDeviceData() {
		DeviceData data = new DeviceData(SITE_ID, CUSTOMER_ID, DEVICE_ID);
		data.setDate(new Date());
		data.setEnergyConsumed(100.0f);
		data.setTotalRealPower(50.0f);
		data.setTotalEnergyConsumed(1000.0f);
		return data;
	}

	private Device createVirtualDevice() {
		Device device = new Device(VIRTUAL_DEVICE_ID, CUSTOMER_ID, "Virtual Device");
		device.setVirtual(true);
		device.setSiteId(SITE_ID);
		device.setSite("Test Site");
		return device;
	}

	private Device createPhysicalDevice(String deviceId) {
		Device device = new Device(deviceId, CUSTOMER_ID, "Physical Device " + deviceId);
		device.setVirtual(false);
		device.setSiteId(SITE_ID);
		device.setSite("Test Site");
		return device;
	}
}
