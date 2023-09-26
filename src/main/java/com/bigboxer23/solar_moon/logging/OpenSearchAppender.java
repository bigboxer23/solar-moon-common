package com.bigboxer23.solar_moon.logging;

import com.internetitem.logback.elasticsearch.ElasticsearchAppender;
import com.internetitem.logback.elasticsearch.config.ElasticsearchProperties;
import com.internetitem.logback.elasticsearch.config.Property;
import com.internetitem.logback.elasticsearch.config.Settings;

/** */
public class OpenSearchAppender extends ElasticsearchAppender {
	public OpenSearchAppender() {
		super();
		setProperties(new ElasticsearchProperties());
	}

	public OpenSearchAppender(Settings settings) {
		super(settings);
		setProperties(new ElasticsearchProperties());
	}

	@Override
	public void setProperties(ElasticsearchProperties elasticsearchProperties) {
		super.setProperties(elasticsearchProperties);
		elasticsearchProperties.addProperty(new Property("level", "%level", true));
		elasticsearchProperties.addProperty(new Property("logger_name", "%logger", true));
		elasticsearchProperties.addProperty(new Property("thread_name", "%thread", true));
		elasticsearchProperties.addProperty(new Property("type", "main", true));
		elasticsearchProperties.addProperty(new Property("stack_trace", "%ex", true));
	}
}
