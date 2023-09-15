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
		StringBuilder builder = new StringBuilder(" ");
		builder.append(Optional.ofNullable(getTransactionId())
				.map(trans -> trans + ":")
				.orElse(""));
		builder.append(Optional.ofNullable(getRemoteAddress()).orElse(""));
		return builder.length() == 1 ? "" : builder.toString();
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
