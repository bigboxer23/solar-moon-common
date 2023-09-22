package com.bigboxer23.solar_moon.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.util.HashMap;
import java.util.Map;

/** Unwrapping class for JSON serializability */
public class OpenSearchDTO {
	@JsonAnyGetter
	Map<String, Object> attr = new HashMap<>();

	public OpenSearchDTO(DeviceData deviceData) {
		deviceData.getAttributes().forEach((name, deviceAttributes) -> attr.put(name, deviceAttributes.getValue()));
	}
}
