package com.bigboxer23.solar_moon.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.internetitem.logback.elasticsearch.config.ElasticsearchProperties;
import com.internetitem.logback.elasticsearch.config.HttpRequestHeaders;
import com.internetitem.logback.elasticsearch.config.Property;
import com.internetitem.logback.elasticsearch.config.Settings;
import com.internetitem.logback.elasticsearch.util.ErrorReporter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenSearchPublisherTest {

	private Context context;
	private Settings settings;

	@BeforeEach
	void setUp() {
		context = new LoggerContext();
		settings = new Settings();
		settings.setIndex("test-index");
	}

	@Test
	void testConstructor_withNoConfiguredOutputs_hasNoPendingData() throws IOException {
		OpenSearchPublisher publisher = createPublisher();

		assertFalse(publisher.hasPendingData());
		assertFalse(publisher.isWorking());
	}

	@Test
	void testAddEvent_withNoOutputs_doesNotStartWorker() throws IOException {
		OpenSearchPublisher publisher = createPublisher();

		publisher.addEvent(loggingEvent("a message", Map.of()));

		assertFalse(publisher.isWorking());
		assertFalse(publisher.hasPendingData());
	}

	@Test
	void testBuildPropertyAndEncoder_returnsClassicEncoderForProperty() throws IOException {
		OpenSearchPublisher publisher = createPublisher();

		assertNotNull(publisher.buildPropertyAndEncoder(context, new Property("level", "%level", true)));
	}

	@Test
	void testSerializeCommonFields_writesTimestampAndMessage() throws IOException {
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("hello world", Map.of()));

		assertTrue(json.contains("\"@timestamp\""));
		assertTrue(json.contains("\"message\":\"hello world\""));
	}

	@Test
	void testSerializeCommonFields_withRawJsonMessage_writesMessageUnquoted() throws IOException {
		settings.setRawJsonMessage(true);
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("{\"nested\":\"value\"}", Map.of()));

		assertTrue(json.contains("\"message\":{\"nested\":\"value\"}"));
	}

	@Test
	void testSerializeCommonFields_truncatesMessageOverMaxSize() throws IOException {
		settings.setMaxMessageSize(5);
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("abcdefghij", Map.of()));

		assertTrue(json.contains("\"message\":\"abcde..\""));
	}

	@Test
	void testSerializeCommonFields_doesNotTruncateMessageAtMaxSize() throws IOException {
		settings.setMaxMessageSize(10);
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("abcdefghij", Map.of()));

		assertTrue(json.contains("\"message\":\"abcdefghij\""));
	}

	@Test
	void testSerializeCommonFields_withoutMaxSize_keepsFullMessage() throws IOException {
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("a considerably longer message", Map.of()));

		assertTrue(json.contains("\"message\":\"a considerably longer message\""));
	}

	@Test
	void testSerializeCommonFields_withIncludeMdc_writesMdcEntries() throws IOException {
		settings.setIncludeMdc(true);
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("message", Map.of("customerId", "customer-1")));

		assertTrue(json.contains("\"customerId\":\"customer-1\""));
	}

	@Test
	void testSerializeCommonFields_withoutIncludeMdc_omitsMdcEntries() throws IOException {
		OpenSearchPublisher publisher = createPublisher();

		String json = serialize(publisher, loggingEvent("message", Map.of("customerId", "customer-1")));

		assertFalse(json.contains("customerId"));
	}

	private OpenSearchPublisher createPublisher() throws IOException {
		return new OpenSearchPublisher(
				context,
				new ErrorReporter(settings, context),
				settings,
				new ElasticsearchProperties(),
				new HttpRequestHeaders());
	}

	private String serialize(OpenSearchPublisher publisher, ILoggingEvent event) throws IOException {
		StringWriter writer = new StringWriter();
		JsonGenerator generator = new JsonFactory().createGenerator(writer);
		generator.writeStartObject();
		publisher.serializeCommonFields(generator, event);
		generator.writeEndObject();
		generator.flush();
		return writer.toString();
	}

	private ILoggingEvent loggingEvent(String message, Map<String, String> mdc) {
		ILoggingEvent event = mock(ILoggingEvent.class);
		lenient().when(event.getTimeStamp()).thenReturn(1736937000000L);
		lenient().when(event.getFormattedMessage()).thenReturn(message);
		lenient().when(event.getMDCPropertyMap()).thenReturn(mdc);
		return event;
	}
}
