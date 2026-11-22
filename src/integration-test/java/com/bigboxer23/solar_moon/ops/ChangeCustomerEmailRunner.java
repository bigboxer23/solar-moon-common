package com.bigboxer23.solar_moon.ops;

import static com.bigboxer23.solar_moon.util.PropertyConstants.AWS_REGION;
import static com.bigboxer23.solar_moon.util.PropertyConstants.COGNITO_USER_POOL_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.utils.StringUtils;

/**
 * One-off operational runner for changing a real customer's email in Cognito and the customer table.
 * Deliberately has no setup/teardown so nothing else is created or deleted.
 *
 * <p>Only runs when -DchangeEmail.customerId is set, so it never runs as part of the integration
 * suite. Dry run by default; add -DchangeEmail.apply=true to make the change.
 *
 * <pre>
 * mvn verify -P integration-tests -Dit.test=ChangeCustomerEmailRunner \
 *     -DchangeEmail.customerId=&lt;cognito sub&gt; -DchangeEmail.newEmail=&lt;email&gt; [-DchangeEmail.apply=true]
 * </pre>
 */
@EnabledIfSystemProperty(named = "changeEmail.customerId", matches = ".+")
public class ChangeCustomerEmailRunner implements IComponentRegistry {

	@Test
	public void changeEmail() {
		String customerId = System.getProperty("changeEmail.customerId");
		String newEmail = System.getProperty("changeEmail.newEmail");
		boolean apply = Boolean.getBoolean("changeEmail.apply");
		assertNotNull(newEmail, "-DchangeEmail.newEmail is required");
		for (String property : new String[] {AWS_REGION, COGNITO_USER_POOL_ID}) {
			assertFalse(
					StringUtils.isBlank(PropertyUtils.getProperty(property)),
					property
							+ " is not set. PropertyUtils ignores -D flags: set it in"
							+ " src/integration-test/resources/application.properties or as an environment variable"
							+ " (env '" + property + "=...' mvn ...)");
		}

		Optional<Customer> before = customerComponent.findCustomerByCustomerId(customerId);
		assertTrue(before.isPresent(), "no customer with id " + customerId);
		System.out.println("customer:  " + customerId + " (" + before.get().getName() + ")");
		System.out.println("current:   " + before.get().getEmail());
		System.out.println("new:       " + newEmail);
		customerComponent
				.findCustomerByEmail(newEmail.trim())
				.filter(c -> !customerId.equals(c.getCustomerId()))
				.ifPresent(c -> fail("email already belongs to customer " + c.getCustomerId()));

		if (!apply) {
			System.out.println("DRY RUN, nothing changed. Re-run with -DchangeEmail.apply=true to apply.");
			return;
		}
		Optional<Customer> after = customerComponent.changeEmail(customerId, newEmail);
		assertTrue(after.isPresent(), "changeEmail failed, see log output");
		assertEquals(newEmail.trim(), after.get().getEmail());
		System.out.println("DONE, email is now " + after.get().getEmail());
	}
}
