package com.bigboxer23.solar_moon.util;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

/** */
public class TableCreationUtils {
	public static <T> void createTable(List<String> indexNames, DynamoDbTable<T> table) {
		table.createTable(builder -> builder.globalSecondaryIndices(getSecondaryIndexes(indexNames)));
	}

	private static List<EnhancedGlobalSecondaryIndex> getSecondaryIndexes(List<String> indexNames) {
		return indexNames == null
				? null
				: indexNames.stream().map(TableCreationUtils::getBuilder).collect(Collectors.toList());
	}

	private static EnhancedGlobalSecondaryIndex getBuilder(String indexName) {
		return EnhancedGlobalSecondaryIndex.builder()
				.indexName(indexName)
				.projection(projectionBuilder ->
						projectionBuilder.projectionType(ProjectionType.ALL).build())
				.build();
	}
}
