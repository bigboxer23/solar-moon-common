package com.bigboxer23.solar_moon.subscription;

import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class SubscriptionComponent extends AbstractDynamodbComponent<Subscription> {

	@Override
	protected String getTableName() {
		return "subscription";
	}

	@Override
	protected Class<Subscription> getObjectClass() {
		return Subscription.class;
	}

	public int getSubscriptionPacks(String customerId) {
		return !StringUtils.isBlank(customerId)
				? this.getTable()
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(customerId)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
						.map(Subscription::getPacks)
						.orElse(0)
				: 0;
	}

	public Subscription updateSubscription(String customerId, int seats) {
		if (StringUtils.isBlank(customerId) || seats < 0) {
			logger.warn("invalid customer passed, returning 0");
			return null;
		}
		logger.warn("Updating subscription: " + customerId + " " + seats);
		return getTable().updateItem(builder -> builder.item(new Subscription(customerId, seats)));
	}

	public void deleteSubscription(String customerId) {
		logger.warn("Deleting subscription: " + customerId);
		getTable().deleteItem(new Subscription(customerId, -1));
	}
}
