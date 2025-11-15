package com.bigboxer23.solar_moon.subscription;

import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class DynamoDbSubscriptionRepository extends AuditableAbstractDynamodbRepository<Subscription>
		implements SubscriptionRepository {

	@Override
	public Optional<Subscription> findByCustomerId(String customerId) {
		return !StringUtils.isBlank(customerId)
				? this.getTable()
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(customerId)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
				: Optional.empty();
	}

	@Override
	public void delete(Subscription subscription) {
		getTable().deleteItem(subscription);
	}

	@Override
	protected String getTableName() {
		return "subscription";
	}

	@Override
	protected Class<Subscription> getObjectClass() {
		return Subscription.class;
	}
}
