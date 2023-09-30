package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class CognitoCommon {
	private String version;
	private String triggerSource;
	private String region;
	private String userPoolId;
	private String userName;
	private CognitoRequest request;
	private CognitoCallerContext callerContext;
	private CognitoResponse response;
}
