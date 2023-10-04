package com.bigboxer23.solar_moon.web;

import com.bigboxer23.solar_moon.lambda.data.LambdaRequest;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.MDC;

/** */
public class TransactionUtil {
	private static final ThreadLocal<String> remoteAddress = new ThreadLocal<>();

	private static final ThreadLocal<String> transactionID = new ThreadLocal<>();

	private static String hostName;

	public static String getRemoteAddress() {
		return remoteAddress.get();
	}

	public static String getTransactionId() {
		return transactionID.get();
	}

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
		remoteAddress.set(address);
		transactionID.set(TokenGenerator.generateNewToken());
	}

	public static void clear() {
		remoteAddress.remove();
		transactionID.remove();
	}

	public static void newTransaction(LambdaRequest request) {
		if (request == null || request.getHeaders() == null) {
			return;
		}
		remoteAddress.set(request.getHeaders().getXForwardedFor());
		transactionID.set(request.getHeaders().getAmazonTraceId());
		hostName = request.getHeaders().getHost();
		addToMDC();
	}

	public static void addToMDC() {
		MDC.put("transaction.id", getTransactionId());
		MDC.put("transaction.remote", getRemoteAddress());
		MDC.put("transaction.host", getHostName());
	}
}
