package com.bigboxer23.solar_moon.ops;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class TestLogEntry {

	@Test
	public void beanRoundTrip() {
		LogEntry entry = new LogEntry();
		Date now = new Date();
		entry.setDate(now);
		entry.setServiceName("svc");
		entry.setCustomerId("cust");
		entry.setLevel("ERROR");
		entry.setLogName("log");
		entry.setThreadName("main");
		entry.setStackTrace("stack");
		entry.setMessage("message");
		entry.setType("type");

		assertEquals(now, entry.getDate());
		assertEquals("svc", entry.getServiceName());
		assertEquals("cust", entry.getCustomerId());
		assertEquals("ERROR", entry.getLevel());
		assertEquals("log", entry.getLogName());
		assertEquals("main", entry.getThreadName());
		assertEquals("stack", entry.getStackTrace());
		assertEquals("message", entry.getMessage());
		assertEquals("type", entry.getType());
	}

	@Test
	public void jacksonMapping() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		String json =
				"""
				{
				"@timestamp":"2024-02-01T12:00:00.000Z",
				"service.name":"svc",
				"customer.id":"cust",
				"level":"ERROR",
				"log_name":"lb-log",
				"thread_name":"main",
				"stack_trace":"trace",
				"message":"boom",
				"type":"java"
				}
				""";

		LogEntry entry = mapper.readValue(json, LogEntry.class);
		assertNotNull(entry.getDate());
		assertEquals("svc", entry.getServiceName());
		assertEquals("cust", entry.getCustomerId());
		assertEquals("ERROR", entry.getLevel());
		assertEquals("lb-log", entry.getLogName());
		assertEquals("main", entry.getThreadName());
		assertEquals("trace", entry.getStackTrace());
		assertEquals("boom", entry.getMessage());
		assertEquals("java", entry.getType());

		// And we can serialize back out without blowing up
		String roundTrip = mapper.writeValueAsString(entry);
		assertNotNull(roundTrip);
	}
}
