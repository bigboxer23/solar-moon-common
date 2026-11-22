package com.bigboxer23.solar_moon.customer;

import com.bigboxer23.solar_moon.data.Customer;
import java.util.Optional;

public interface CustomerRepository {

	Optional<Customer> findCustomerByEmail(String email);

	Optional<Customer> findCustomerByCustomerId(String customerId);

	Optional<Customer> findCustomerByStripeCustomerId(String stripeCustomerId);

	Optional<Customer> findCustomerByAccessKey(String accessKey);

	Customer add(Customer customer);

	Optional<Customer> update(Customer customer);

	void delete(Customer customer);

	/**
	 * Atomically moves a customer to a new email (the table's partition key) by deleting the old
	 * item and writing a copy keyed by the new email. Fails if the new email is already in use.
	 */
	Customer changeEmail(Customer customer, String newEmail);
}
