package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class TestDeviceUpdateComponent implements IComponentRegistry {

	private static final Set<String> ids = new HashSet<>();

	@BeforeEach
	public void before() {
		int[] ai = {0};
		ids.forEach(id -> {
			ai[0]++;
			deviceUpdateComponent.update(id, 10L * ai[0]);
		});
	}

	@BeforeAll
	public static void beforeAll() {
		TestUtils.setupSite();
		deviceComponent.getDevicesForCustomerId(TestDeviceComponent.clientId).forEach(d -> ids.add(d.getId()));
	}

	@AfterAll
	public static void after() {
		ids.forEach(deviceUpdateComponent::delete);
	}

	@Test
	public void queryByDeviceId() {
		Device device = TestUtils.getDevice();
		assertEquals(20, deviceUpdateComponent.queryByDeviceId(device.getId()));
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
		deviceUpdateComponent.queryByTimeRange(40).forEach(d -> {
			wasUpdated[0] = 1;
			assertTrue(d.getLastUpdate() < 40);
		});
		assertEquals(1, wasUpdated[0]);
	}
}
