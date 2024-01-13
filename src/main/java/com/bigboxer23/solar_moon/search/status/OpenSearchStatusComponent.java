package com.bigboxer23.solar_moon.search.status;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
public class OpenSearchStatusComponent extends AbstractDynamodbComponent<OpenSearchStatus> {

	public void storeFailure() {
		logger.info("OpenSearch failure detected");
		getTable().updateItem(builder -> builder.item(new OpenSearchStatus(System.currentTimeMillis())));
	}

	public boolean hasFailureWithLastThirtyMinutes() {
		return this.getTable()
				.query(QueryConditional.sortGreaterThan((builder) -> builder.partitionValue("1")
						.sortValue(System.currentTimeMillis() - TimeConstants.THIRTY_MINUTES)))
				.stream()
				.findFirst()
				.flatMap((page) -> page.items().stream().findFirst())
				.isPresent();
	}

	@Override
	protected String getTableName() {
		return "openSearchStatus";
	}

	@Override
	protected Class<OpenSearchStatus> getObjectClass() {
		return OpenSearchStatus.class;
	}

	public void checkAvailability() {
		if (!IComponentRegistry.OSComponent.isOpenSearchAvailable()) {
			storeFailure();
		}
	}
}
