package com.bigboxer23.solar_moon.util;

import com.bigboxer23.solar_moon.IComponentRegistry;
import org.junit.jupiter.api.Test;

/**
 * Collect useful utility methods here
 */
public class MaintenanceUtils implements IComponentRegistry
{
	@Test
	public void renameDevicesWhichHaveParansNames()
	{
		String customerId = "";
		deviceComponent.getDevicesForCustomerId(customerId).forEach(device -> {
			String displayName = device.getDisplayName();
			int index1 = displayName.indexOf("(");
			int index2 = displayName.lastIndexOf(")");
			if (index1 > -1 && index2 > -1 && index1 < index2)
			{
				System.out.println("renaming " + displayName + ":" + device.getId());
				System.out.println(displayName.substring(index1 + 1, index2));
				device.setName(displayName.substring(index1 + 1, index2));
				deviceComponent.updateDevice(device);
			}
		});
	}
}
