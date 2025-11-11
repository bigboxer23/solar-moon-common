package com.bigboxer23.solar_moon.ingest.sma;

import com.bigboxer23.solar_moon.IComponentRegistry;
import org.junit.jupiter.api.Test;

/** */
public class SMAS3CleanupComponentIntegrationTest
{
	@Test
	public void cleanupEmptyFTPS3Folders() {
		IComponentRegistry.smaCleanupComponent.cleanupEmptyFTPS3Folders();
	}
}
