package com.bigboxer23.solar_moon.web;

import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.util.Optional;

/** */
public class TransactionUtil {
	private static final ThreadLocal<String> remoteAddress = new ThreadLocal<>();

	private static final ThreadLocal<String> transactionID = new ThreadLocal<>();

	public static String getRemoteAddress() {
		return remoteAddress.get();
	}

	public static String getTransactionId() {
		return transactionID.get();
	}

	public static String getLoggingStatement() {
		return Optional.ofNullable(getTransactionId()).orElse("")
				+ ":"
				+ Optional.ofNullable(getRemoteAddress()).orElse("");
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
}
