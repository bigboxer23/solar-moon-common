package com.bigboxer23.solar_moon.web;

import com.bigboxer23.solar_moon.lambda.data.LambdaRequest;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.MDC;

/** */
public class TransactionUtil {
	private static String hostName;

	public static String getHostName() {
		if (hostName == null) {
			try {
				hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException theE) {
			}
		}
		return hostName;
	}

	public static void newTransaction(String address) {
		if (address == null || address.isBlank()) {
			return;
		}
		addToMDC(TokenGenerator.generateNewToken(), address, getHostName(), null);
	}

	public static void clear() {
		MDC.clear();
	}

	public static void addDeviceId(String deviceId) {
		MDC.put("device.id", deviceId);
	}

	public static void newTransaction(LambdaRequest request) {
		if (request == null || request.getHeaders() == null) {
			return;
		}
		addToMDC(
				request.getHeaders().getAmazonTraceId(),
				request.getHeaders().getXForwardedFor(),
				request.getHeaders().getHost(),
				AuthenticationUtils.getCustomerIdFromRequest(request));
	}

	public static void updateCustomerId(String customerId) {
		MDC.put("customer.id", customerId);
	}

	public static void updateServiceCalled(String service) {
		MDC.put("service.name", service);
	}

	private static void addToMDC(String transactionId, String remoteAddress, String hostName, String customerId) {
		MDC.put("transaction.id", transactionId);
		MDC.put("transaction.remote", remoteAddress);
		MDC.put("transaction.host", hostName);
		updateCustomerId(customerId);
	}
}
