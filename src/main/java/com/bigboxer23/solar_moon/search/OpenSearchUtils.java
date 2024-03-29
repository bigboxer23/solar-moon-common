package com.bigboxer23.solar_moon.search;

import com.bigboxer23.solar_moon.data.DeviceData;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class OpenSearchUtils {
	private static final Logger logger = LoggerFactory.getLogger(OpenSearchUtils.class);

	public static DeviceData getDeviceDataFromFields(String deviceId, Map<String, Object> fields) {
		if (fields == null) {
			logger.warn("No fields associated with result for " + deviceId);
			return null;
		}
		return new DeviceData(fields);
	}

	public static List<DeviceData> getDeviceDataFromResults(List<Hit<Map>> hits) {
		return hits.stream()
				.map(Hit::source)
				.map(fields -> getDeviceDataFromFields("null", fields))
				.collect(Collectors.toList());
	}

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
