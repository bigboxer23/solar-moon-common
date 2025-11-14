package com.bigboxer23.solar_moon.mapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.ingest.MeterConstants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MappingComponentTest implements MeterConstants {

	@Mock
	private MappingRepository mockRepository;

	private MappingComponent component;

	private static final String TEST_CUSTOMER_ID = "test-customer-123";
	private static final String TEST_MAPPING_NAME = "test-mapping";
	private static final String TEST_MAPPING_NAME_2 = "test-mapping-2";

	@BeforeEach
	public void setup() {
		component = new MappingComponent() {
			@Override
			protected MappingRepository getRepository() {
				return mockRepository;
			}
		};
	}

	@Test
	public void testGetMappings_returnsListOfMappings() {
		AttributeMap mapping1 = new AttributeMap(TEST_CUSTOMER_ID, VOLTAGE_LABEL, TEST_MAPPING_NAME);
		AttributeMap mapping2 = new AttributeMap(TEST_CUSTOMER_ID, CURRENT_LABEL, TEST_MAPPING_NAME_2);

		when(mockRepository.findByCustomerId(TEST_CUSTOMER_ID)).thenReturn(List.of(mapping1, mapping2));

		List<AttributeMap> result = component.getMappings(TEST_CUSTOMER_ID);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(TEST_MAPPING_NAME, result.get(0).getMappingName());
		assertEquals(TEST_MAPPING_NAME_2, result.get(1).getMappingName());
		verify(mockRepository).findByCustomerId(TEST_CUSTOMER_ID);
	}

	@Test
	public void testGetMappings_emptyList() {
		when(mockRepository.findByCustomerId(TEST_CUSTOMER_ID)).thenReturn(List.of());

		List<AttributeMap> result = component.getMappings(TEST_CUSTOMER_ID);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testAddMapping_validMapping() {
		AttributeMap expectedMapping = new AttributeMap(TEST_CUSTOMER_ID, VOLTAGE_LABEL, TEST_MAPPING_NAME);

		when(mockRepository.findByCustomerIdAndMappingName(TEST_CUSTOMER_ID, TEST_MAPPING_NAME))
				.thenReturn(Optional.empty());
		when(mockRepository.add(any(AttributeMap.class))).thenReturn(expectedMapping);

		Optional<AttributeMap> result = component.addMapping(TEST_CUSTOMER_ID, VOLTAGE_LABEL, TEST_MAPPING_NAME);

		assertTrue(result.isPresent());
		assertEquals(TEST_CUSTOMER_ID, result.get().getCustomerId());
		assertEquals(VOLTAGE_LABEL, result.get().getAttribute());
		assertEquals(TEST_MAPPING_NAME, result.get().getMappingName());
		verify(mockRepository).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_allValidAttributes() {
		when(mockRepository.findByCustomerIdAndMappingName(anyString(), anyString()))
				.thenReturn(Optional.empty());
		when(mockRepository.add(any(AttributeMap.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertTrue(component
				.addMapping(TEST_CUSTOMER_ID, ENERGY_CONSUMED_LABEL, "mapping1")
				.isPresent());
		assertTrue(component
				.addMapping(TEST_CUSTOMER_ID, REAL_POWER_LABEL, "mapping2")
				.isPresent());
		assertTrue(component
				.addMapping(TEST_CUSTOMER_ID, CURRENT_LABEL, "mapping3")
				.isPresent());
		assertTrue(component
				.addMapping(TEST_CUSTOMER_ID, VOLTAGE_LABEL, "mapping4")
				.isPresent());
		assertTrue(component
				.addMapping(TEST_CUSTOMER_ID, POWER_FACTOR_LABEL, "mapping5")
				.isPresent());

		verify(mockRepository, times(5)).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_blankCustomerId() {
		Optional<AttributeMap> result = component.addMapping("", VOLTAGE_LABEL, TEST_MAPPING_NAME);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_nullCustomerId() {
		Optional<AttributeMap> result = component.addMapping(null, VOLTAGE_LABEL, TEST_MAPPING_NAME);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_blankAttribute() {
		Optional<AttributeMap> result = component.addMapping(TEST_CUSTOMER_ID, "", TEST_MAPPING_NAME);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_nullAttribute() {
		Optional<AttributeMap> result = component.addMapping(TEST_CUSTOMER_ID, null, TEST_MAPPING_NAME);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_blankMappingName() {
		Optional<AttributeMap> result = component.addMapping(TEST_CUSTOMER_ID, VOLTAGE_LABEL, "");

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_nullMappingName() {
		Optional<AttributeMap> result = component.addMapping(TEST_CUSTOMER_ID, VOLTAGE_LABEL, null);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_unsupportedAttribute() {
		Optional<AttributeMap> result =
				component.addMapping(TEST_CUSTOMER_ID, "UnsupportedAttribute", TEST_MAPPING_NAME);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testAddMapping_duplicateMappingName() {
		AttributeMap existingMapping = new AttributeMap(TEST_CUSTOMER_ID, VOLTAGE_LABEL, TEST_MAPPING_NAME);

		when(mockRepository.findByCustomerIdAndMappingName(TEST_CUSTOMER_ID, TEST_MAPPING_NAME))
				.thenReturn(Optional.of(existingMapping));

		Optional<AttributeMap> result = component.addMapping(TEST_CUSTOMER_ID, CURRENT_LABEL, TEST_MAPPING_NAME);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).add(any(AttributeMap.class));
	}

	@Test
	public void testDeleteMapping_singleMapping() {
		component.deleteMapping(TEST_CUSTOMER_ID, TEST_MAPPING_NAME);

		verify(mockRepository).deleteByCustomerIdAndMappingName(TEST_CUSTOMER_ID, TEST_MAPPING_NAME);
	}

	@Test
	public void testDeleteMapping_byCustomerId() {
		component.deleteMapping(TEST_CUSTOMER_ID);

		verify(mockRepository).deleteByCustomerId(TEST_CUSTOMER_ID);
	}
}
