package com.bigboxer23.solar_moon.search;

import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import lombok.SneakyThrows;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;

/** */
public class OpenSearchUtils {
	public static String queryToJson(JsonpSerializable obj) {
		StringWriter stringWriter = new StringWriter();
		JsonbJsonpMapper mapper = new JsonbJsonpMapper();
		JsonGenerator generator = mapper.jsonProvider().createGenerator(stringWriter);
		mapper.serialize(obj, generator);
		generator.close();
		return stringWriter.toString();
	}

	@SneakyThrows
	public static void waitForIndexing() {
		Thread.sleep(1500);
	}
}
