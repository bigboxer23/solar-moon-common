package com.bigboxer23.solar_moon.mapping;

import com.bigboxer23.solar_moon.ingest.MeterConstants;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class MappingComponent implements MeterConstants {

	private static final Set<String> attributes = new HashSet<>();

	static {
		attributes.add(ENERGY_CONSUMED_LABEL);
		attributes.add(REAL_POWER_LABEL);
		attributes.add(CURRENT_LABEL);
		attributes.add(VOLTAGE_LABEL);
		attributes.add(POWER_FACTOR_LABEL);
	}

	private MappingRepository repository;

	protected MappingRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbMappingRepository();
		}
		return repository;
	}

	public List<AttributeMap> getMappings(String customerId) {
		return getRepository().findByCustomerId(customerId);
	}

	private Optional<AttributeMap> getMapping(String customerId, String mappingName) {
		log.debug("getting mappings");
		return getRepository().findByCustomerIdAndMappingName(customerId, mappingName);
	}

	public Optional<AttributeMap> addMapping(String customerId, String attribute, String mappingName) {
		if (StringUtils.isBlank(customerId) || StringUtils.isBlank(attribute) || StringUtils.isBlank(mappingName)) {
			log.warn("invalid mapping, not adding");
			return Optional.empty();
		}
		if (!attributes.contains(attribute)) {
			log.warn("unsupported mapping, not adding " + attribute);
			return Optional.empty();
		}
		if (getMapping(customerId, mappingName).isPresent()) {
			log.warn("Mapping name already used " + mappingName);
			return Optional.empty();
		}
		log.info("adding mapping " + attribute + " " + mappingName);
		return Optional.ofNullable(getRepository().add(new AttributeMap(customerId, attribute, mappingName)));
	}

	public void deleteMapping(String customerId) {
		getRepository().deleteByCustomerId(customerId);
	}

	public void deleteMapping(String customerId, String mappingName) {
		log.info("deleting mapping " + mappingName);
		getRepository().deleteByCustomerIdAndMappingName(customerId, mappingName);
	}
}
