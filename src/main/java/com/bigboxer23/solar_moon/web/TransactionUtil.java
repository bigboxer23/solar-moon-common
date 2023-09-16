package com.bigboxer23.solar_moon.web;

import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

	private static String getHostName() {
		if (hostName == null) {
			try {
				hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException theE) {
			}
		}
		return hostName;
	}

	public static String getLoggingStatement() {
		return "[" + getHostName() + "][" + getRemoteAddress() + "][" + getTransactionId() + "] ";
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
