package com.bigboxer23.solar_moon.mapping;

import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import java.util.*;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class MappingComponent extends AbstractDynamodbComponent<AttributeMap> implements MeterConstants {

	private static final Set<String> attributes = new HashSet<>();

	static {
		attributes.add(ENERGY_CONSUMED_LABEL);
		attributes.add(REAL_POWER_LABEL);
		attributes.add(CURRENT_LABEL);
		attributes.add(VOLTAGE_LABEL);
		attributes.add(POWER_FACTOR_LABEL);
	}

	public List<AttributeMap> getMappings(String customerId) {
		return getTable()
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.items()
				.stream()
				.toList();
	}

	private Optional<AttributeMap> getMapping(String customerId, String mappingName) {
		logger.debug("getting mappings");
		return getTable()
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(customerId).sortValue(mappingName)))
				.items()
				.stream()
				.findFirst();
	}

	public Optional<AttributeMap> addMapping(String customerId, String attribute, String mappingName) {
		if (StringUtils.isBlank(customerId) || StringUtils.isBlank(attribute) || StringUtils.isBlank(mappingName)) {
			logger.warn("invalid mapping, not adding");
			return Optional.empty();
		}
		if (!attributes.contains(attribute)) {
			logger.warn("unsupported mapping, not adding " + attribute);
			return Optional.empty();
		}
		if (getMapping(customerId, mappingName).isPresent()) {
			logger.warn("Mapping name already used " + mappingName);
			return Optional.empty();
		}
		logger.info("adding mapping " + attribute + " " + mappingName);
		return Optional.ofNullable(
				getTable().updateItem(builder -> builder.item(new AttributeMap(customerId, attribute, mappingName))));
	}

	public void deleteMapping(String customerId) {
		getMappings(customerId).forEach(map -> {
			deleteMapping(map.getCustomerId(), map.getMappingName());
		});
	}

	public void deleteMapping(String customerId, String mappingName) {
		logger.info("deleting mapping " + mappingName);
		getTable()
				.deleteItem(b -> b.key(Key.builder()
						.partitionValue(customerId)
						.sortValue(mappingName)
						.build()));
	}

	@Override
	protected String getTableName() {
		return "mappings";
	}

	@Override
	protected Class<AttributeMap> getObjectClass() {
		return AttributeMap.class;
	}
}
