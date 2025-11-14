package com.bigboxer23.solar_moon.mapping;

import java.util.List;
import java.util.Optional;

public interface MappingRepository {
	List<AttributeMap> findByCustomerId(String customerId);

	Optional<AttributeMap> findByCustomerIdAndMappingName(String customerId, String mappingName);

	AttributeMap add(AttributeMap attributeMap);

	Optional<AttributeMap> update(AttributeMap attributeMap);

	void deleteByCustomerIdAndMappingName(String customerId, String mappingName);

	void deleteByCustomerId(String customerId);
}
