package com.bigboxer23.solar_moon.web;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.lambda.data.LambdaAuthorizer;
import com.bigboxer23.solar_moon.lambda.data.LambdaClaims;
import com.bigboxer23.solar_moon.lambda.data.LambdaRequest;
import com.bigboxer23.solar_moon.lambda.data.LambdaRequestContext;
import com.bigboxer23.solar_moon.lambda.data.LambdaRequestHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TransactionUtilTest {

	@BeforeEach
	void clearBefore() {
		MDC.clear();
	}

	@AfterEach
	void clearAfter() {
		MDC.clear();
	}

	@Test
	void testGetHostName_returnsCachedValue() {
		assertSame(TransactionUtil.getHostName(), TransactionUtil.getHostName());
	}

	@Test
	void testNewTransaction_withAddress_populatesTransactionContext() {
		TransactionUtil.newTransaction("10.0.0.1");

		assertNotNull(MDC.get("transaction.id"));
		assertEquals("10.0.0.1", MDC.get("transaction.remote"));
		assertEquals(TransactionUtil.getHostName(), MDC.get("transaction.host"));
	}

	@Test
	void testNewTransaction_generatesDistinctTransactionIds() {
		TransactionUtil.newTransaction("10.0.0.1");
		String first = MDC.get("transaction.id");
		TransactionUtil.newTransaction("10.0.0.1");

		assertNotEquals(first, MDC.get("transaction.id"));
	}

	@Test
	void testNewTransaction_withNullAddress_leavesContextEmpty() {
		TransactionUtil.newTransaction((String) null);

		assertNull(MDC.get("transaction.id"));
	}

	@Test
	void testNewTransaction_withBlankAddress_leavesContextEmpty() {
		TransactionUtil.newTransaction("   ");

		assertNull(MDC.get("transaction.id"));
	}

	@Test
	void testClear_removesEverything() {
		TransactionUtil.newTransaction("10.0.0.1");
		TransactionUtil.updateServiceCalled("ingest");

		TransactionUtil.clear();

		assertNull(MDC.get("transaction.id"));
		assertNull(MDC.get("service.name"));
	}

	@Test
	void testAddDeviceId_withDeviceAndSite_setsBoth() {
		TransactionUtil.addDeviceId("device-1", "site-1");

		assertEquals("device-1", MDC.get("device.id"));
		assertEquals("site-1", MDC.get("site.id"));
	}

	@Test
	void testAddDeviceId_withBlankSite_setsDeviceOnly() {
		TransactionUtil.addDeviceId("device-1", "");

		assertEquals("device-1", MDC.get("device.id"));
		assertNull(MDC.get("site.id"));
	}

	@Test
	void testAddDeviceId_withBlankDevice_clearsBoth() {
		TransactionUtil.addDeviceId("device-1", "site-1");

		TransactionUtil.addDeviceId("", "site-1");

		assertNull(MDC.get("device.id"));
		assertNull(MDC.get("site.id"));
	}

	@Test
	void testAddDeviceId_withNullDevice_clearsBoth() {
		TransactionUtil.addDeviceId("device-1", "site-1");

		TransactionUtil.addDeviceId(null, null);

		assertNull(MDC.get("device.id"));
		assertNull(MDC.get("site.id"));
	}

	@Test
	void testUpdateCustomerId_setsCustomerAndClearsDeviceContext() {
		TransactionUtil.addDeviceId("device-1", "site-1");

		TransactionUtil.updateCustomerId("customer-1");

		assertEquals("customer-1", MDC.get("customer.id"));
		assertNull(MDC.get("device.id"));
		assertNull(MDC.get("site.id"));
	}

	@Test
	void testUpdateServiceCalled_setsServiceName() {
		TransactionUtil.updateServiceCalled("ingest");

		assertEquals("ingest", MDC.get("service.name"));
	}

	@Test
	void testNewTransaction_withLambdaRequest_populatesFromHeadersAndClaims() {
		TransactionUtil.newTransaction(lambdaRequest("trace-1", "10.0.0.2", "api.example.com", "customer-1"));

		assertEquals("trace-1", MDC.get("transaction.id"));
		assertEquals("10.0.0.2", MDC.get("transaction.remote"));
		assertEquals("api.example.com", MDC.get("transaction.host"));
		assertEquals("customer-1", MDC.get("customer.id"));
	}

	@Test
	void testNewTransaction_withNullRequest_leavesContextEmpty() {
		TransactionUtil.newTransaction((LambdaRequest) null);

		assertNull(MDC.get("transaction.id"));
	}

	@Test
	void testNewTransaction_withoutHeaders_leavesContextEmpty() {
		TransactionUtil.newTransaction(new LambdaRequest());

		assertNull(MDC.get("transaction.id"));
	}

	@Test
	void testNewTransaction_withoutAuthorizer_populatesTransactionWithoutCustomer() {
		LambdaRequest request = lambdaRequest("trace-2", "10.0.0.3", "api.example.com", null);
		request.getRequestContext().setAuthorizer(null);

		TransactionUtil.newTransaction(request);

		assertEquals("trace-2", MDC.get("transaction.id"));
		assertNull(MDC.get("customer.id"));
	}

	private LambdaRequest lambdaRequest(String traceId, String forwardedFor, String host, String username) {
		LambdaRequestHeaders headers = new LambdaRequestHeaders();
		headers.setAmazonTraceId(traceId);
		headers.setXForwardedFor(forwardedFor);
		headers.setHost(host);

		LambdaClaims claims = new LambdaClaims();
		claims.setUsername(username);
		LambdaAuthorizer authorizer = new LambdaAuthorizer();
		authorizer.setClaims(claims);
		LambdaRequestContext requestContext = new LambdaRequestContext();
		requestContext.setAuthorizer(authorizer);

		LambdaRequest request = new LambdaRequest();
		request.setHeaders(headers);
		request.setRequestContext(requestContext);
		return request;
	}
}
