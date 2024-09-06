package com.bigboxer23.solar_moon.alarm;

import java.util.Map;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class SolectriaErrorOracle implements ISolectriaConstants {
	public static String translateError(int errorCode, boolean criticalErrorType) {
		if (errorCode == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		String[] newline = {""};
		Map<Integer, String> errorCodeMap = (criticalErrorType ? CRITICAL_ERROR_CODES : INFORMATIVE_ERROR_CODES);
		errorCodeMap.keySet().forEach(key -> {
			if ((errorCode & key) == key) {
				builder.append(newline[0]).append(errorCodeMap.get(key));
				newline[0] = "\n";
			}
		});
		return builder.toString();
	}

	public static int rawErrorToCode(String rawErrorCode) {
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
