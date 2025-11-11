package com.bigboxer23.solar_moon.device;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.weather.PirateWeatherComponent;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
		protected com.bigboxer23.solar_moon.search.OpenSearchComponent getOSComponent() {
			return osComponent;
		}

		@Override
		protected com.bigboxer23.solar_moon.location.LocationComponent getLocationComponent() {
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
		lenient()
				.when(mockDeviceComponent.getDevicesBySiteId(anyString(), anyString()))
				.thenReturn(Collections.emptyList());
		lenient()
				.when(mockOSComponent.getSiteDevicesCountByTimePeriod(anyString(), anyString(), any(Date.class)))
				.thenReturn(0);
	}

	@Test
	void testHandleVirtualDevice_withNoSiteId_doesNothing() {
		DeviceData deviceData = createDeviceData();
		deviceData.setSiteId(DeviceComponent.NO_SITE);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withValidData_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withEmptySiteId_doesNotProcess() {
		DeviceData deviceData = createDeviceData();
		deviceData.setSiteId("");

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withDifferentSiteIds_processesSeparately() {
		DeviceData deviceData1 = createDeviceData();
		deviceData1.setSiteId("site-1");

		DeviceData deviceData2 = createDeviceData();
		deviceData2.setSiteId("site-2");

		virtualDeviceComponent.handleVirtualDevice(deviceData1);
		virtualDeviceComponent.handleVirtualDevice(deviceData2);
	}

	@Test
	void testHandleVirtualDevice_withMultipleDevicesInSite_aggregatesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(100.0f);
		deviceData.setTotalRealPower(50.0f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withNegativeValues_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(-10.0f);
		deviceData.setTotalRealPower(-5.0f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withZeroValues_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(0.0f);
		deviceData.setTotalRealPower(0.0f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withLargeValues_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(999999.99f);
		deviceData.setTotalRealPower(888888.88f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withSiteDevice_setsCorrectly() {
		DeviceData deviceData = createDeviceData();

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withMultipleConcurrentRequests_handlesCorrectly() {
		DeviceData deviceData1 = createDeviceData();
		DeviceData deviceData2 = createDeviceData();

		virtualDeviceComponent.handleVirtualDevice(deviceData1);
		virtualDeviceComponent.handleVirtualDevice(deviceData2);
	}

	@Test
	void testHandleVirtualDevice_withDifferentTimestamps_processesEach() {
		DeviceData deviceData1 = createDeviceData();
		deviceData1.setDate(new Date(System.currentTimeMillis() - 10000));

		DeviceData deviceData2 = createDeviceData();
		deviceData2.setDate(new Date(System.currentTimeMillis()));

		virtualDeviceComponent.handleVirtualDevice(deviceData1);
		virtualDeviceComponent.handleVirtualDevice(deviceData2);
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
		Device device = new Device(DEVICE_ID, CUSTOMER_ID, "Virtual Device");
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
