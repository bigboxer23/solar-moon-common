package com.bigboxer23.solar_moon.search.status;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bigboxer23.solar_moon.IComponentRegistry;
import org.junit.jupiter.api.Test;

/** */
public class OpenSearchStatusComponentIntegrationTest implements IComponentRegistry {

	@Test
	public void storeFailure() {
		OpenSearchStatusComponent.storeFailure();
	}

	@Test
	public void hasFailureWithLastThirtyMinutes() {
		assertTrue(OpenSearchStatusComponent.hasFailureWithinLastThirtyMinutes());
	}
}
