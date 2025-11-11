package com.bigboxer23.solar_moon.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import org.junit.jupiter.api.Test;

/** */
public class DownloadComponentIntegrationTest implements IComponentRegistry, TestConstants {
	@Test
	public void getPageSizeDays() {
		assertEquals(104, downloadComponent.getPageSizeDays(1));
		assertEquals(6, downloadComponent.getPageSizeDays(16));
		assertEquals(1, downloadComponent.getPageSizeDays(104));
		assertEquals(0, downloadComponent.getPageSizeDays(105));
		assertEquals(0, downloadComponent.getPageSizeDays(0));
	}
}
