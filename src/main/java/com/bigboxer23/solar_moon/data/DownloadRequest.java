package com.bigboxer23.solar_moon.data;

import com.bigboxer23.solar_moon.util.TokenGenerator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** */
@DynamoDbBean
@Schema(description = "data object representing a request for download of report data")
@Data
public class DownloadRequest {
	private String customerId;

	private String requestId;

	private long percentComplete = 0;

	private String s3Key;

	private long lastUpdate;

	public DownloadRequest() {}

	public DownloadRequest(String customerId) {
		setCustomerId(customerId);
		setRequestId(TokenGenerator.generateNewToken());
		setLastUpdate(System.currentTimeMillis());
	}

	@DynamoDbPartitionKey
	public String getRequestId() {
		return requestId;
	}
}
