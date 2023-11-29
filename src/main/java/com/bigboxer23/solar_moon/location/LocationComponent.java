package com.bigboxer23.solar_moon.location;

import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.SearchForTextResult;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextRequest;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextResponse;

/** */
public class LocationComponent {
	public Optional<SearchForTextResult> getLatLongFromText(String city, String state, String country) {
		try (LocationClient client = LocationClient.builder().build()) {
			SearchPlaceIndexForTextResponse response =
					client.searchPlaceIndexForText(SearchPlaceIndexForTextRequest.builder()
							.text(city + ", " + state + " ," + country)
							.indexName(PropertyUtils.getProperty("aws.location.index"))
							.build());
			if (!response.hasResults()) {
				return Optional.empty();
			}
			return Optional.ofNullable(
					Collections.max(response.results(), Comparator.comparing(SearchForTextResult::relevance)));
		}
	}
}
