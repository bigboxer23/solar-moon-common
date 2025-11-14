package com.bigboxer23.solar_moon.mapping;

import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Slf4j
public class DynamoDbMappingRepository extends AuditableAbstractDynamodbRepository<AttributeMap>
		implements MappingRepository {

	@Override
	public List<AttributeMap> findByCustomerId(String customerId) {
		return getTable()
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.items()
				.stream()
				.toList();
	}

	@Override
	public Optional<AttributeMap> findByCustomerIdAndMappingName(String customerId, String mappingName) {
		return getTable()
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(customerId).sortValue(mappingName)))
				.items()
				.stream()
				.findFirst();
	}

	@Override
	public void deleteByCustomerIdAndMappingName(String customerId, String mappingName) {
		getTable()
				.deleteItem(b -> b.key(Key.builder()
						.partitionValue(customerId)
						.sortValue(mappingName)
						.build()));
	}

	@Override
	public void deleteByCustomerId(String customerId) {
		findByCustomerId(customerId)
				.forEach(map -> deleteByCustomerIdAndMappingName(map.getCustomerId(), map.getMappingName()));
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
