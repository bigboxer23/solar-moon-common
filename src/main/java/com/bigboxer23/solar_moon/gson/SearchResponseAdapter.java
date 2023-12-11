package com.bigboxer23.solar_moon.gson;

import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.google.gson.*;
import java.lang.reflect.Type;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
public class SearchResponseAdapter implements JsonSerializer<SearchResponse>, JsonDeserializer<SearchResponse> {
	private static final Gson localGSON = new Gson();

	@Override
	public JsonElement serialize(
			final SearchResponse search, final Type typeOfSrc, final JsonSerializationContext context) {
		return localGSON.fromJson(OpenSearchUtils.queryToJson(search), JsonElement.class);
	}

	@Override
	public SearchResponse deserialize(
			final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		return null;
	}
}
