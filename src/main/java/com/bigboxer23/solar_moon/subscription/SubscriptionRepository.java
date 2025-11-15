package com.bigboxer23.solar_moon.subscription;

import com.bigboxer23.solar_moon.data.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {

	Optional<Subscription> findByCustomerId(String customerId);

	Subscription add(Subscription subscription);

	Optional<Subscription> update(Subscription subscription);

	void delete(Subscription subscription);
}
