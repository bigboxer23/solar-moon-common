package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class CognitoResponse {
	private boolean autoConfirmUser;
	private boolean autoVerifyPhone;
	private boolean autoVerifyEmail;
}
