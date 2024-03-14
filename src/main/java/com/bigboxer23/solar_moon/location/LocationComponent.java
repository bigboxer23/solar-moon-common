package com.bigboxer23.solar_moon.location;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.util.TimezoneMapper;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import net.time4j.Moment;
import net.time4j.PlainDate;
import net.time4j.calendar.astro.SolarTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.SearchForTextResult;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextRequest;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextResponse;

/** */
public class LocationComponent {
	private static final Logger logger = LoggerFactory.getLogger(LocationComponent.class);

	public Optional<SearchForTextResult> getLatLongFromText(String city, String state, String country) {
		String locationString = city + ", " + state + ", " + country;
		logger.info("fetching location data for " + locationString);
		try (LocationClient client = LocationClient.builder().build()) {
			SearchPlaceIndexForTextResponse response =
					client.searchPlaceIndexForText(SearchPlaceIndexForTextRequest.builder()
							.text(locationString)
							.indexName(PropertyUtils.getProperty("aws.location.index"))
							.build());
			if (!response.hasResults()) {
				return Optional.empty();
			}
			return Optional.ofNullable(
					Collections.max(response.results(), Comparator.comparing(SearchForTextResult::relevance)));
		}
	}

	public void addLocationData(DeviceData data, Device site) {
		if (data == null) {
			logger.warn("Device invalid, not adding location data");
			return;
		}
		if (site == null) {
			logger.debug("No site, not adding location data");
			return;
		}
		if (site.getLatitude() == -1 && site.getLongitude() == -1) {
			logger.debug("no location data, can't write day/night");
			return;
		}
		if (data.getDate() == null) {
			logger.info("no date, can't write day/night");
			return;
		}
		try {
			data.setDaylight(isDay(data.getDate(), site.getLatitude(), site.getLongitude()));
		} catch (Exception e) {
			logger.warn("addLocationData", e);
		}
	}

	public boolean isDay(Date dateToCheck, double latitude, double longitude) throws Exception {
		SolarTime location = SolarTime.ofLocation(latitude, longitude);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateToCheck);
		PlainDate plainDate = PlainDate.of(
				calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
		Optional<Moment> sunrise = plainDate.get(location.sunrise());
		Optional<Moment> sunset = plainDate.get(location.sunset());
		if (sunrise.isEmpty() || sunset.isEmpty()) {
			throw new Exception("Cannot find sunrise or sunset" + sunrise.isEmpty() + " " + sunset.isEmpty());
		}
		return sunrise.get().compareTo(Moment.from(dateToCheck.toInstant())) <= 0
				&& sunset.get().compareTo(Moment.from(dateToCheck.toInstant())) > 0;
	}

	public Optional<LocalDateTime> getLocalTimeString(double latitude, double longitude) {
		Optional<String> TZString = getLocalTimeZone(latitude, longitude);
		if (TZString.isEmpty()) {
			logger.error("unknown timezone: " + latitude + "," + longitude);
			return Optional.empty();
		}
		return Optional.of(LocalDateTime.ofInstant(Instant.now(), ZoneId.of(TZString.get())));
	}

	public Optional<String> getLocalTimeZone(double latitude, double longitude) {
		if (latitude == -1 && longitude == -1) {
			return Optional.empty();
		}
		if (latitude > 90 || latitude < -90) {
			logger.error("latitude is not valid " + latitude);
			return Optional.empty();
		}
		if (longitude > 180 || longitude < -180) {
			logger.error("longitude is not valid " + latitude);
			return Optional.empty();
		}
		String TZString = TimezoneMapper.latLngToTimezoneString(latitude, longitude);
		if (TZString.equalsIgnoreCase("unknown")) {
			logger.error("unknown timezone: " + latitude + "," + longitude);
			return Optional.empty();
		}
		return Optional.of(TZString);
	}
}
