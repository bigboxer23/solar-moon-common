package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
@Component
public class CustomerComponent extends AbstractDynamodbComponent<Customer> {
	@Override
	protected String getTableName() {
		return "customer";
	}

	@Override
	protected Class<Customer> getObjectClass() {
		return Customer.class;
	}

	public Customer addCustomer(String email, String customerId) {
		if (email == null || customerId == null || email.isBlank() || customerId.isBlank()) {
			logger.warn("email or customer id is null/empty, cannot fetch.");
			return null;
		}
		if (findCustomerByCustomerId(customerId) != null) {
			logger.debug(customerId + ":" + email + " exists, not putting into db");
			return null;
		}
		Customer customer = new Customer(customerId, email, TokenGenerator.generateNewToken());
		logger.info("Adding customer " + email);
		// TODO:MDC this logAction(customer.getCustomerId(), "add");
		getTable().putItem(customer);
		return customer;
	}

	public Customer findCustomerByCustomerId(String customerId) {
		return customerId != null && !customerId.isEmpty()
				? this.getTable()
						.index(Customer.CUSTOMER_ID_INDEX)
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(customerId)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
						.orElse(null)
				: null;
	}

	public Customer findCustomerIdByAccessKey(String accessKey) {
		if (accessKey == null || accessKey.isEmpty()) {
			return null;
		}
		return getTable()
				.index(Customer.ACCESS_KEY_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(accessKey)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst())
				.orElse(null);
	}
}
