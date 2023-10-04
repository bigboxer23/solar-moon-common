package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
// @Component
public class CustomerComponent extends AbstractDynamodbComponent<Customer> {
	@Override
	protected String getTableName() {
		return "customer";
	}

	@Override
	protected Class<Customer> getObjectClass() {
		return Customer.class;
	}

	public Customer addCustomer(String email, String customerId, String name) {
		if (email == null
				|| customerId == null
				|| name == null
				|| email.isBlank()
				|| customerId.isBlank()
				|| name.isBlank()) {
			logger.warn("email, name or customer id is null/empty, cannot add.");
			return null;
		}
		if (findCustomerByCustomerId(customerId) != null) {
			logger.debug(customerId + ":" + email + " exists, not putting into db");
			return null;
		}
		Customer customer = new Customer(customerId, email, TokenGenerator.generateNewToken(), name);
		logger.info("Adding customer " + email);
		// TODO:MDC this logAction(customer.getCustomerId(), "add");
		getTable().putItem(customer);
		return customer;
	}

	public void updateCustomer(Customer customer) {
		if (customer == null || customer.getCustomerId() == null) {
			logger.warn("invalid customer passed, not updating");
			return;
		}
		logAction(customer.getCustomerId(), "update");
		if (customer.getAccessKey() == null || customer.getAccessKey().isBlank()) {
			logger.info("generating new access key for " + customer.getCustomerId());
			customer.setAccessKey(TokenGenerator.generateNewToken());
		}
		if (customer.isAdmin() && !findCustomerByCustomerId(customer.getCustomerId()).isAdmin()) {
			logger.warn("Not allowing admin escalation" + customer.getCustomerId());
			customer.setAdmin(false);
		}
		// TODO:validation
		getTable().updateItem(builder -> builder.item(customer));
	}

	public void deleteCustomerByEmail(String email) {
		Optional.ofNullable(email).filter(e -> !e.isBlank()).ifPresent(e -> {
			logAction(email, "delete by customer email");
			getTable().deleteItem(new Customer(null, email, null, null));
		});
	}

	public void deleteCustomerByCustomerId(String customerId) {
		Optional.ofNullable(findCustomerByCustomerId(customerId)).ifPresent(customer -> {
			logAction(customer.getCustomerId(), "delete by customer id");
			getTable().deleteItem(customer);
		});
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

	private void logAction(String action, String customerId) {
		logger.info(customerId + " customer " + action);
	}
}
