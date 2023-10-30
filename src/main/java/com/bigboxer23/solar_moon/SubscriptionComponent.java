package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Subscription;
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
		return customerId != null && !customerId.isEmpty()
				? this.getTable()
						// .index(Customer.CUSTOMER_ID_INDEX)
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(customerId)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
						.map(Subscription::getPacks)
						.orElse(0)
				: 0;
	}

	public Subscription updateSubscription(String customerId, int packs) {
		if (StringUtils.isBlank(customerId) || packs < 0) {
			logger.warn("invalid customer passed, returning 0");
			return null;
		}
		return getTable().updateItem(builder -> builder.item(new Subscription(customerId, packs)));
	}
}
