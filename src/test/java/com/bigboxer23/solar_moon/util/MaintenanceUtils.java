package com.bigboxer23.solar_moon.util;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.core.search.Hit;

/** Collect useful utility methods here */
@Slf4j
public class MaintenanceUtils implements IComponentRegistry {
	/*@Test*/
	public void renameDevicesWhichHaveParansNames() {
		String customerId = TestConstants.CUSTOMER_ID;
		deviceComponent.getDevicesForCustomerId(customerId).forEach(device -> {
			String displayName = device.getDisplayName();
			int index1 = displayName.indexOf("(");
			int index2 = displayName.lastIndexOf(")");
			if (index1 > -1 && index2 > -1 && index1 < index2) {
				System.out.println("renaming " + displayName + ":" + device.getId());
				System.out.println(displayName.substring(index1 + 1, index2));
				device.setName(displayName.substring(index1 + 1, index2));
				deviceComponent.updateDevice(device);
			}
		});
	}

	/*@Test*/
	public void deleteSearchDataInRange() {
		String customerId = TestConstants.CUSTOMER_ID;
		SearchJSON search = new SearchJSON();
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(500);
		search.setCustomerId(customerId);
		// search.setDeviceId("xxx");
		search.setEndDate(System.currentTimeMillis() - (TimeConstants.YEAR - 2 * TimeConstants.THIRTY_DAYS));
		search.setStartDate(System.currentTimeMillis() - TimeConstants.YEAR);
		OSComponent.deleteByQuery(search);
	}

	/**
	 * Iterate through all devices for a customer and see if there are any miscalculated energy
	 * consumed data
	 */
	/*@Test*/
	public void correctLargeEnergyConsumedData() {
		String customerId = "xxxx";
		deviceComponent.getDevicesForCustomerId(customerId).forEach(this::findLargeEnergyConsumedForDevice);
	}

	private void findLargeEnergyConsumedForDevice(Device device) {
		log.warn("Checking " + device.getId() + " (" + device.getDisplayName() + ")");
		SearchJSON search = new SearchJSON();
		search.setType(DATA_SEARCH_TYPE);
		search.setLargeEnergyConsumed(true);
		search.setIncludeSource(true);
		search.setSize(2500);
		search.setCustomerId(device.getClientId());
		search.setDeviceId(device.getId());
		search.setEndDate(System.currentTimeMillis());
		search.setStartDate(System.currentTimeMillis() - TimeConstants.YEAR);
		OSComponent.search(search).hits().hits().forEach(this::correctLargeEnergyConsumed);
	}

	private void correctLargeEnergyConsumed(Hit<DeviceData> hit) {
		DeviceData deviceData = hit.source();
		assert deviceData != null;
		log.debug("attempting to correct " + hit.id());
		float previousValue = findPreviousTotalEnergyConsumed(deviceData);
		float correctedValue = deviceData.getTotalEnergyConsumed() - previousValue;
		if (previousValue < 0) {
			log.warn("Could not correct " + hit.id());
			return;
		}
		writeCorrectedEnergyConsumed(deviceData, hit.id(), correctedValue);
	}

	private float findPreviousTotalEnergyConsumed(DeviceData deviceData) {
		SearchJSON search = new SearchJSON();
		search.setType(DATA_SEARCH_TYPE);
		search.setIncludeSource(true);
		search.setSize(100);
		search.setCustomerId(deviceData.getCustomerId());
		search.setDeviceId(deviceData.getDeviceId());
		search.setEndDate(deviceData.getDate().getTime() - 1);
		search.setStartDate(deviceData.getDate().getTime() - TimeConstants.DAY);
		return OSComponent.search(search).hits().hits().stream()
				.map(Hit::source)
				.filter(Objects::nonNull)
				.map(DeviceData::getTotalEnergyConsumed)
				.filter(theTotalEnergyConsumed -> theTotalEnergyConsumed > 0)
				.findAny()
				.orElse(-1f);
	}

	private void writeCorrectedEnergyConsumed(DeviceData data, String id, float correctedValue) {
		log.warn("Correcting " + id + " to " + correctedValue);
		SearchJSON search = new SearchJSON();
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(100);
		search.setId(id);
		search.setCustomerId(data.getCustomerId());
		search.setEndDate(System.currentTimeMillis());
		search.setStartDate(System.currentTimeMillis() - TimeConstants.YEAR);
		OSComponent.updateByQuery(search, MeterConstants.ENG_CONS, correctedValue);
	}

	/**
	 * Walk through all device data in chronological order and validate any stamped energy consumed
	 * data. If incorrect data is found re-stamp the corrected data
	 */
	/*@Test*/
	public void auditEnergyConsumed() {
		String customerId = "xxxx";
		deviceComponent.getDevicesForCustomerId(customerId).forEach(this::checkEnergyConsumed);
	}

	public void checkEnergyConsumed(Device device) {
		log.warn("Auditing " + device.getId());
		SearchJSON search = new SearchJSON();
		search.setType(DATA_SEARCH_TYPE);
		search.setIncludeSource(true);
		search.setSize(10000);
		search.setOffset(0);
		search.setCustomerId(device.getClientId());
		search.setDeviceId(device.getId());
		search.setStartDate(System.currentTimeMillis() - TimeConstants.YEAR);
		search.setEndDate(search.getStartDate() + (30 * TimeConstants.DAY));
		search.setSortAsc(true);
		Hit[] previous = {null};

		while (search.getStartDate() < System.currentTimeMillis()) {
			OSComponent.search(search).hits().hits().forEach(hit -> {
				correctEnergyConsumed(hit, previous[0]);
				previous[0] = hit;
			});
			search.setStartDate(search.getEndDate() + 1);
			search.setEndDate(search.getStartDate() + (30 * TimeConstants.DAY));
			log.info(search.getJavaStartDate() + " " + search.getJavaEndDate());
		}
	}

	private void correctEnergyConsumed(Hit<DeviceData> current, Hit<DeviceData> previous) {
		if (current == null || previous == null) {
			log.warn("cannot correct, null values");
			return;
		}
		DeviceData deviceData = current.source();
		DeviceData previousData = previous.source();
		if (deviceData == null || previousData == null) {
			log.warn(current.id() + " cannot correct, null deviceData");
			return;
		}
		float energyConsumed = deviceData.getTotalEnergyConsumed() - previousData.getTotalEnergyConsumed();
		if (energyConsumed < 0 || energyConsumed > 1000) {
			energyConsumed = 0;
		}
		if (energyConsumed != deviceData.getEnergyConsumed()) {
			log.warn(deviceData.getDate() + " " + deviceData.getEnergyConsumed() + " " + energyConsumed);
			SearchJSON search = new SearchJSON();
			search.setType(DATA_SEARCH_TYPE);
			search.setSize(100);
			search.setId(current.id());
			search.setCustomerId(deviceData.getCustomerId());
			search.setEndDate(System.currentTimeMillis());
			search.setStartDate(System.currentTimeMillis() - TimeConstants.YEAR);
			OSComponent.updateByQuery(search, MeterConstants.ENG_CONS, energyConsumed);
		}
	}
}
