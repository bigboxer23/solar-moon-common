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
}
