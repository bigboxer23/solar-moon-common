package com.bigboxer23.solar_moon.alarm;

import software.amazon.awssdk.utils.StringUtils;

/** */
public class SolectriaErrorOracle implements ISolectriaConstants {
	public static String translateError(String rawErrorCode, boolean criticalErrorType) {
		int errorCode = rawErrorToCode(rawErrorCode);
		if (errorCode == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		String[] newline = {""};
		INFORMATIVE_ERROR_CODES.keySet().forEach(key -> {
			if ((errorCode & key) == key) {
				builder.append(newline[0]).append(INFORMATIVE_ERROR_CODES.get(key));
				newline[0] = "\n";
			}
		});
		return builder.toString();
	}

	protected static int rawErrorToCode(String rawErrorCode) {
		if (StringUtils.isBlank(rawErrorCode)) {
			return 0;
		}
		if (rawErrorCode.contains(".")) {
			rawErrorCode = rawErrorCode.substring(0, rawErrorCode.indexOf("."));
		}
		try {
			return Integer.parseInt(rawErrorCode);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
}
