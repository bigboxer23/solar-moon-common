package com.bigboxer23.solar_moon.search.status;

import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Slf4j
public class DynamoDbOpenSearchStatusRepository extends AuditableAbstractDynamodbRepository<OpenSearchStatus>
		implements OpenSearchStatusRepository {

	@Override
	public void storeFailure() {
		log.info("OpenSearch failure detected");
		getTable().updateItem(builder -> builder.item(new OpenSearchStatus(System.currentTimeMillis())));
	}

	@Override
	public boolean hasFailureWithinLastThirtyMinutes() {
		return getTable()
				.query(QueryConditional.sortGreaterThan(builder -> builder.partitionValue(OpenSearchStatus.IDENTITY)
						.sortValue(System.currentTimeMillis() - TimeConstants.THIRTY_MINUTES)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst())
				.isPresent();
	}

	@Override
	public Optional<OpenSearchStatus> getMostRecentStatus() {
		return getTable()
				.query(theBuilder -> theBuilder
						.limit(1)
						.scanIndexForward(false)
						.queryConditional(QueryConditional.keyEqualTo(
								builder -> builder.partitionValue(OpenSearchStatus.IDENTITY))))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst());
	}

	@Override
	protected String getTableName() {
		return "openSearchStatus";
	}

	@Override
	protected Class<OpenSearchStatus> getObjectClass() {
		return OpenSearchStatus.class;
	}
}
