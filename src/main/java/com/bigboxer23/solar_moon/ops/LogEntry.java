package com.bigboxer23.solar_moon.ops;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.TIMESTAMP;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Data;

/** */
@Data
public class LogEntry {
	@JsonProperty(TIMESTAMP)
	private Date date;

	@JsonProperty("service.name")
	private String serviceName;

	@JsonProperty("customer.id")
	private String customerId;

	@JsonProperty("level")
	private String level;

	@JsonProperty("log_name")
	private String logName;

	@JsonProperty("thread_name")
	private String threadName;

	@JsonProperty("stack_trace")
	private String stackTrace;

	@JsonProperty("message")
	private String message;

	@JsonProperty("type")
	private String type;
}
