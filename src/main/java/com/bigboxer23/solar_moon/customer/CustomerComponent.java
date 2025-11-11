package com.bigboxer23.solar_moon.customer;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class CustomerComponent {

	private CustomerRepository repository;

	protected CustomerRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbCustomerRepository();
		}
		return repository;
	}

	public Optional<Customer> addCustomer(String email, String customerId, String name, String stripeCustomerId) {
		if (StringUtils.isEmpty(email)
				|| StringUtils.isEmpty(customerId)
				|| StringUtils.isEmpty(name)
				|| StringUtils.isEmpty(stripeCustomerId)) {
			log.warn("email, name strip id, or customer id is null/empty, cannot add.");
			return Optional.empty();
		}
		Optional<Customer> dbCustomer = findCustomerByCustomerId(customerId);
		if (dbCustomer.isPresent()) {
			log.debug(customerId + ":" + email + " exists, not putting into db");
			return dbCustomer;
		}
		Customer customer = new Customer(customerId, email, TokenGenerator.generateNewToken(), name);
		customer.setStripeCustomerId(stripeCustomerId);
		log.info("Adding customer " + email);
		getRepository().add(customer);
		IComponentRegistry.smaIngestComponent.handleAccessKeyChange(null, customer.getAccessKey());
		return Optional.of(customer);
	}

	public void updateCustomer(Customer customer) {
		if (customer == null || customer.getCustomerId() == null) {
			log.warn("invalid customer passed, not updating");
			return;
		}
		logAction("update", customer.getCustomerId());
		if (customer.isAccessKeyChangeRequested()) {
			log.info("generating new access key for " + customer.getCustomerId());
			customer.setAccessKey(TokenGenerator.generateNewToken());
		}
		findCustomerByCustomerId(customer.getCustomerId()).ifPresent(existingCustomer -> {
			if (customer.isAdmin() && !existingCustomer.isAdmin()) {
				log.warn("Not allowing admin escalation" + customer.getCustomerId());
				customer.setAdmin(false);
			}
			if (!StringUtils.isEmpty(existingCustomer.getStripeCustomerId())) {
				customer.setStripeCustomerId(existingCustomer.getStripeCustomerId());
			}
		});
		// TODO:validation
		getRepository().update(customer);
		if (customer.isAccessKeyChangeRequested()) {
			IComponentRegistry.smaIngestComponent.handleAccessKeyChange(
					findCustomerByCustomerId(customer.getCustomerId())
							.map(Customer::getAccessKey)
							.orElse(null),
					customer.getAccessKey());
		}
	}

	public Optional<Customer> findCustomerByEmail(String email) {
		return getRepository().findCustomerByEmail(email);
	}

	public void deleteCustomerByEmail(String email) {
		findCustomerByEmail(email).ifPresent(c -> deleteCustomerByCustomerId(c.getCustomerId()));
	}

	public void deleteCustomerByCustomerId(String customerId) {
		findCustomerByCustomerId(customerId).ifPresent(c -> {
			logAction("delete by customer id", c.getCustomerId());
			IComponentRegistry.deviceUpdateComponent.deleteByCustomerId(customerId);
			IComponentRegistry.linkedDeviceComponent.deleteByCustomerId(customerId);
			IComponentRegistry.subscriptionComponent.deleteSubscription(customerId);
			IComponentRegistry.deviceComponent.deleteDevicesByCustomerId(customerId);
			IComponentRegistry.mappingComponent.deleteMapping(customerId);
			IComponentRegistry.OSComponent.deleteByCustomerId(customerId);
			IComponentRegistry.smaIngestComponent.handleAccessKeyChange(c.getAccessKey(), null);

			getRepository().delete(c);
		});
	}

	public Optional<Customer> findCustomerByCustomerId(String customerId) {
		return getRepository().findCustomerByCustomerId(customerId);
	}

	public Optional<Customer> findCustomerByStripeCustomerId(String stripeCustomerId) {
		return getRepository().findCustomerByStripeCustomerId(stripeCustomerId);
	}

	public Optional<Customer> findCustomerIdByAccessKey(String accessKey) {
		return getRepository().findCustomerByAccessKey(accessKey);
	}

	private void logAction(String action, String customerId) {
		TransactionUtil.updateCustomerId(customerId);
		log.info("customer " + action);
	}
}
