package com.bigboxer23.solar_moon.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.internetitem.logback.elasticsearch.ClassicElasticsearchPublisher;
import com.internetitem.logback.elasticsearch.ElasticsearchAppender;
import com.internetitem.logback.elasticsearch.config.ElasticsearchProperties;
import com.internetitem.logback.elasticsearch.config.Property;
import com.internetitem.logback.elasticsearch.config.Settings;
import java.io.IOException;
import lombok.SneakyThrows;

/** */
public class OpenSearchAppender extends ElasticsearchAppender {

	public static OpenSearchAppender instance;
	private OpenSearchPublisher publisher;

	public OpenSearchAppender() {
		super();
		setProperties(new ElasticsearchProperties());
		instance = this;
	}

	public OpenSearchAppender(Settings settings) {
		super(settings);
		setProperties(new ElasticsearchProperties());
	}

	@Override
	public void setProperties(ElasticsearchProperties elasticsearchProperties) {
		super.setProperties(elasticsearchProperties);
		elasticsearchProperties.addProperty(new Property("level", "%level", true));
		elasticsearchProperties.addProperty(new Property("log_name", "%logger", true));
		elasticsearchProperties.addProperty(new Property("thread_name", "%thread", true));
		elasticsearchProperties.addProperty(new Property("type", "main", true));
		elasticsearchProperties.addProperty(new Property("stack_trace", "%ex", true));
	}

	@Override
	protected void publishEvent(ILoggingEvent eventObject) {
		publisher.addEvent(eventObject);
	}

	@SneakyThrows
	public static void waitForPendingData() {
		int waits = 0;
		while (waits < 30 && (instance.publisher.hasPendingData() || instance.publisher.isWorking())) {
			waits++;
			Thread.sleep(100);
		}
	}

	@Override
	public void start() {
		super.start();
		try {
			this.publisher =
					new OpenSearchPublisher(getContext(), errorReporter, settings, elasticsearchProperties, headers);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Need to call super.start() above, can't make this directly return the publisher type we want,
	 * so return null and return our own type in the start method
	 *
	 * @return
	 */
	@Override
	protected ClassicElasticsearchPublisher buildElasticsearchPublisher() {
		return null;
	}
}
