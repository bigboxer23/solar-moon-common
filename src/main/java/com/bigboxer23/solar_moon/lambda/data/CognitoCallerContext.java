package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class CognitoCallerContext {
	private String awsSdkVersion;
	private String clientId;
}
