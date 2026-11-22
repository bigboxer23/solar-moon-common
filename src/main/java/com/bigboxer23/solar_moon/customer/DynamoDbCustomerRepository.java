package com.bigboxer23.solar_moon.customer;

import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class DynamoDbCustomerRepository extends AuditableAbstractDynamodbRepository<Customer>
		implements CustomerRepository {

	@Override
	public Optional<Customer> findCustomerByEmail(String email) {
		return !StringUtils.isBlank(email)
				? this.getTable()
						.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(email)))
						.stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
				: Optional.empty();
	}

	@Override
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

	@Override
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

	@Override
	public Optional<Customer> findCustomerByAccessKey(String accessKey) {
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

	@Override
	public void delete(Customer customer) {
		getTable().deleteItem(customer);
	}

	@Override
	public Customer changeEmail(Customer customer, String newEmail) {
		DynamoDbTable<Customer> table = getTable();
		String oldEmail = customer.getEmail();
		customer.setEmail(newEmail);
		customer.markUpdated();
		getClient()
				.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
						.addDeleteItem(
								table,
								DeleteItemEnhancedRequest.builder()
										.key(Key.builder()
												.partitionValue(oldEmail)
												.build())
										.conditionExpression(emailCondition("attribute_exists(#email)"))
										.build())
						.addPutItem(
								table,
								PutItemEnhancedRequest.builder(Customer.class)
										.item(customer)
										.conditionExpression(emailCondition("attribute_not_exists(#email)"))
										.build())
						.build());
		return customer;
	}

	private static Expression emailCondition(String expression) {
		return Expression.builder()
				.expression(expression)
				.putExpressionName("#email", "email")
				.build();
	}

	@Override
	protected String getTableName() {
		return "customer";
	}

	@Override
	protected Class<Customer> getObjectClass() {
		return Customer.class;
	}
}
