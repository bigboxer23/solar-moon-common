package com.bigboxer23.solar_moon.util;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class TimeUtils implements TimeConstants {
	private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);

	private static final String USER_FACING_DATE_FORMAT = "MMM d, yy h:mm aaa";

	/**
	 * round to nearest 15m interval
	 *
	 * @return
	 */
	public static Date get15mRoundedDate() {
		LocalDateTime now = LocalDateTime.now();
		return Date.from(now.truncatedTo(ChronoUnit.MINUTES)
				.withMinute(now.getMinute() / 15 * 15)
				.atZone(ZoneId.systemDefault())
				.toInstant());
	}

	public static Date getStartOfDay(String timeZone) {
		ZoneId zoneId = ZoneId.of(timeZone);
		LocalDate now = LocalDate.now(zoneId);
		ZonedDateTime zdtStart = now.atStartOfDay(zoneId);
		return Date.from(zdtStart.toInstant());
	}

	public static String getFormattedZonedTime(long epochMs, String timeZone) {
		ZoneId zoneId = ZoneId.of(timeZone);
		Instant i = Instant.ofEpochSecond(epochMs / 1000);
		ZonedDateTime z = ZonedDateTime.ofInstant(i, zoneId);
		return z.format(DateTimeFormatter.ofPattern("MMM d, yy h:mm a"));
	}

	/**
	 * Translate date stamp to proper time
	 *
	 * @param message
	 * @return
	 */
	public static String formatUnixTimestampsInString(String message, String deviceId, String customerId) {
		if (StringUtils.isBlank(message) || (StringUtils.isBlank(deviceId) && StringUtils.isBlank(customerId))) {
			return message;
		}
		Pattern pattern = Pattern.compile("\\d{13}");
		Matcher matcher = pattern.matcher(message);
		if (!matcher.find()) {
			return message;
		}
		String timestamp = matcher.group();
		SimpleDateFormat sdf = new SimpleDateFormat(USER_FACING_DATE_FORMAT);
		IComponentRegistry.deviceComponent
				.findDeviceById(deviceId, customerId)
				.flatMap(TimeUtils::getTimeZone)
				.ifPresent(tz -> sdf.setTimeZone(TimeZone.getTimeZone(tz)));
		return message.replace(timestamp, sdf.format(Long.parseLong(timestamp)));
	}

	public static Optional<String> getTimeZone(Device device) {
		if (device == null) {
			logger.error("device is null, cannot set time zone");
			return Optional.empty();
		}
		if (!DeviceComponent.NO_SITE.equals(device.getSiteId())) {
			Optional<Device> siteDevice =
					IComponentRegistry.deviceComponent.findDeviceById(device.getSiteId(), device.getClientId());
			Optional<String> timeZone = IComponentRegistry.locationComponent.getLocalTimeZone(
					siteDevice.map(Device::getLatitude).orElse(-1.0),
					siteDevice.map(Device::getLongitude).orElse(-1.0));
			if (timeZone.isPresent()) {
				return timeZone;
			}
		}
		return IComponentRegistry.customerComponent
				.findCustomerByCustomerId(device.getClientId())
				.map(Customer::getDefaultTimezone);
	}
}
