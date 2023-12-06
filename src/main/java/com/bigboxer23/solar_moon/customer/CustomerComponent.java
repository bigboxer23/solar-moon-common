package com.bigboxer23.solar_moon.customer;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class CustomerComponent extends AbstractDynamodbComponent<Customer> {
	@Override
	protected String getTableName() {
		return "customer";
	}

	@Override
	protected Class<Customer> getObjectClass() {
		return Customer.class;
	}

	public Optional<Customer> addCustomer(String email, String customerId, String name, String stripeCustomerId) {
		if (StringUtils.isEmpty(email)
				|| StringUtils.isEmpty(customerId)
				|| StringUtils.isEmpty(name)
				|| StringUtils.isEmpty(stripeCustomerId)) {
			logger.warn("email, name strip id, or customer id is null/empty, cannot add.");
			return Optional.empty();
		}
		if (findCustomerByCustomerId(customerId).isPresent()) {
			logger.debug(customerId + ":" + email + " exists, not putting into db");
			return Optional.empty();
		}
		Customer customer = new Customer(customerId, email, TokenGenerator.generateNewToken(), name);
		customer.setStripeCustomerId(stripeCustomerId);
		logger.info("Adding customer " + email);
		getTable().putItem(customer);
		return Optional.of(customer);
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
		Customer existingCustomer =
				findCustomerByCustomerId(customer.getCustomerId()).get();
		if (customer.isAdmin() && !existingCustomer.isAdmin()) {
			logger.warn("Not allowing admin escalation" + customer.getCustomerId());
			customer.setAdmin(false);
		}
		if (!StringUtils.isEmpty(existingCustomer.getStripeCustomerId())) {
			customer.setStripeCustomerId(existingCustomer.getStripeCustomerId());
		}
		// TODO:validation
		getTable().updateItem(builder -> builder.item(customer));
	}

	public Optional<Customer> findCustomerByEmail(String email) {
		return !StringUtils.isBlank(email)
				? this.getTable()
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(email)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
				: Optional.empty();
	}

	public void deleteCustomerByEmail(String email) {
		findCustomerByEmail(email).ifPresent(c -> deleteCustomerByCustomerId(c.getCustomerId()));
	}

	public void deleteCustomerByCustomerId(String customerId) {
		findCustomerByCustomerId(customerId).ifPresent(c -> {
			logAction(c.getCustomerId(), "delete by customer id");
			IComponentRegistry.subscriptionComponent.deleteSubscription(customerId);
			IComponentRegistry.deviceComponent.deleteDevicesByCustomerId(customerId);
			IComponentRegistry.mappingComponent.deleteMapping(customerId);
			getTable().deleteItem(c);
		});
	}

	public Optional<Customer> findCustomerByCustomerId(String customerId) {
		return customerId == null || customerId.isEmpty()
				? Optional.empty()
				: this.getTable()
						.index(Customer.CUSTOMER_ID_INDEX)
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(customerId)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst());
	}

	public Optional<Customer> findCustomerByStripeCustomerId(String stripeCustomerId) {
		return stripeCustomerId != null && !stripeCustomerId.isEmpty()
				? this.getTable()
						.index(Customer.STRIPE_CUSTOMER_ID_INDEX)
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(stripeCustomerId)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
				: Optional.empty();
	}

	public Optional<Customer> findCustomerIdByAccessKey(String accessKey) {
		if (accessKey == null || accessKey.isEmpty()) {
			return Optional.empty();
		}
		return getTable()
				.index(Customer.ACCESS_KEY_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(accessKey)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst());
	}

	private void logAction(String action, String customerId) {
		logger.info(customerId + " customer " + action);
	}
}
