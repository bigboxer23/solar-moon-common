package com.bigboxer23.solar_moon.util;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;

/** Collect useful utility methods here */
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
}
