package com.bigboxer23.solar_moon.mapping;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** */
public class TestMappingComponent implements IComponentRegistry, TestConstants, MeterConstants {
	private static final String TEST_MAPPING = "testMapping";

	@Test
	public void mapping() {
		mappingComponent.deleteMapping(CUSTOMER_ID);
		assertTrue(mappingComponent.getMappings(CUSTOMER_ID).isEmpty());

		// Test bad attr
		Optional<AttributeMap> map = mappingComponent.addMapping(CUSTOMER_ID, "taco", TEST_MAPPING);
		assertFalse(map.isPresent());

		// Test good add
		map = mappingComponent.addMapping(CUSTOMER_ID, AVG_VOLT, TEST_MAPPING);
		assertTrue(map.isPresent());

		// Test dup add
		map = mappingComponent.addMapping(CUSTOMER_ID, AVG_CURRENT, TEST_MAPPING);
		assertFalse(map.isPresent());
		assertEquals(1, mappingComponent.getMappings(CUSTOMER_ID).size());
		assertEquals(AVG_VOLT, mappingComponent.getMappings(CUSTOMER_ID).get(0).getAttribute());

		// Test add'n good add
		mappingComponent.addMapping(CUSTOMER_ID, AVG_VOLT, TEST_MAPPING + 1);
		assertEquals(2, mappingComponent.getMappings(CUSTOMER_ID).size());

		// Test bad delete
		mappingComponent.deleteMapping(CUSTOMER_ID, TEST_MAPPING + 3);
		assertEquals(2, mappingComponent.getMappings(CUSTOMER_ID).size());

		// Test targeted delete
		mappingComponent.deleteMapping(CUSTOMER_ID, TEST_MAPPING + 1);
		assertEquals(1, mappingComponent.getMappings(CUSTOMER_ID).size());
	}
}
