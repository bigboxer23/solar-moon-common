package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class CognitoRequest {
	private CognitoUserAttributes userAttributes;
	private String validationData;
}
