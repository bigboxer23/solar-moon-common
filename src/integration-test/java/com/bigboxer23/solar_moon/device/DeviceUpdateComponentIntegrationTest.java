package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
public class DeviceUpdateComponentIntegrationTest implements IComponentRegistry, TestConstants {

	private static final Set<String> ids = new HashSet<>();

	@BeforeEach
	public void before() {
		ids.forEach(id -> {
			try {
				Thread.sleep(20);
			} catch (InterruptedException theE) {

			}
			deviceUpdateComponent.update(id);
		});
	}

	@BeforeAll
	public static void beforeAll() {
		TestUtils.setupSite();
		deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).forEach(d -> ids.add(d.getId()));
	}

	@AfterAll
	public static void after() {
		ids.forEach(deviceUpdateComponent::delete);
	}

	@Test
	public void queryByDeviceId() {
		Device device = TestUtils.getDevice();
		assertTrue(deviceUpdateComponent.queryByDeviceId(device.getId()) > -1);
		deviceUpdateComponent.delete(device.getId());
		assertEquals(-1, deviceUpdateComponent.queryByDeviceId(device.getId()));
	}

	@Test
	public void getDevices() {
		assertTrue(StreamSupport.stream(deviceUpdateComponent.getDevices().spliterator(), false)
				.map(DeviceUpdateData::getDeviceId)
				.toList()
				.containsAll(ids));
	}

	@Test
	public void queryByTimeRange() {
		int[] wasUpdated = {0};
		deviceUpdateComponent.queryByTimeRange(System.currentTimeMillis() + 40).forEach(d -> {
			wasUpdated[0] = 1;
			System.out.println("was updated");
			assertTrue(d.getLastUpdate() < System.currentTimeMillis() + 40);
		});
		assertEquals(1, wasUpdated[0]);
	}
}
