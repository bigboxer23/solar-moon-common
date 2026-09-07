package com.bigboxer23.solar_moon.logging;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.LoggerContext;
import com.internetitem.logback.elasticsearch.config.ElasticsearchProperties;
import com.internetitem.logback.elasticsearch.config.Property;
import com.internetitem.logback.elasticsearch.config.Settings;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class OpenSearchAppenderTest {

	@Test
	void testDefaultConstructor_registersStaticInstance() {
		OpenSearchAppender appender = new OpenSearchAppender();

		assertSame(appender, OpenSearchAppender.instance);
	}

	@Test
	void testSetProperties_addsStandardLogProperties() {
		OpenSearchAppender appender = new OpenSearchAppender();
		ElasticsearchProperties properties = new ElasticsearchProperties();

		appender.setProperties(properties);

		List<String> names =
				properties.getProperties().stream().map(Property::getName).collect(Collectors.toList());
		assertTrue(names.contains("level"));
		assertTrue(names.contains("log_name"));
		assertTrue(names.contains("thread_name"));
		assertTrue(names.contains("type"));
		assertTrue(names.contains("stack_trace"));
	}

	@Test
	void testSetProperties_marksPropertiesAsAllowingEmptyValues() {
		OpenSearchAppender appender = new OpenSearchAppender();
		ElasticsearchProperties properties = new ElasticsearchProperties();

		appender.setProperties(properties);

		assertTrue(properties.getProperties().stream().allMatch(Property::isAllowEmpty));
	}

	@Test
	void testSetProperties_usesLogbackPatternsForValues() {
		OpenSearchAppender appender = new OpenSearchAppender();
		ElasticsearchProperties properties = new ElasticsearchProperties();

		appender.setProperties(properties);

		Property level = properties.getProperties().stream()
				.filter(p -> "level".equals(p.getName()))
				.findFirst()
				.orElseThrow();
		assertEquals("%level", level.getValue());
	}

	@Test
	void testSettingsConstructor_doesNotOverwriteStaticInstance() {
		OpenSearchAppender defaultAppender = new OpenSearchAppender();

		new OpenSearchAppender(new Settings());

		assertSame(defaultAppender, OpenSearchAppender.instance);
	}

	@Test
	void testBuildElasticsearchPublisher_returnsNullSoStartCanSupplyOwnType() {
		OpenSearchAppender appender = new OpenSearchAppender();

		assertNull(appender.buildElasticsearchPublisher());
	}

	@Test
	void testStart_createsPublisherWithoutThrowing() {
		OpenSearchAppender appender = new OpenSearchAppender();
		appender.setContext(new LoggerContext());

		assertDoesNotThrow(appender::start);
	}

	@Test
	void testWaitForPendingData_withIdlePublisher_returnsImmediately() {
		OpenSearchAppender appender = new OpenSearchAppender();
		appender.setContext(new LoggerContext());
		appender.start();

		assertDoesNotThrow(OpenSearchAppender::waitForPendingData);
	}
}
