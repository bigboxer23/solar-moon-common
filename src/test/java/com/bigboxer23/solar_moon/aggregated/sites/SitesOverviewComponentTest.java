package com.bigboxer23.solar_moon.aggregated.sites;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.alarm.IAlarmConstants;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

@ExtendWith(MockitoExtension.class)
public class SitesOverviewComponentTest {

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private AlarmComponent mockAlarmComponent;

	@Mock
	private SubscriptionComponent mockSubscriptionComponent;

	@Mock
	private OpenSearchComponent mockOSComponent;

	@Mock
	private LocationComponent mockLocationComponent;

	@Mock
	private SearchResponse<DeviceData> mockSearchResponse;

	@Mock
	private Hit<DeviceData> mockHit;

	private TestableSitesOverviewComponent sitesOverviewComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String SITE_ID = "test-site-456";
	private static final String DEVICE_ID = "test-device-789";
	private static final double LATITUDE = 41.8781;
	private static final double LONGITUDE = -87.6298;
	private static final long START_DATE = System.currentTimeMillis() - TimeConstants.DAY * 7;
	private static final long END_DATE = System.currentTimeMillis();

	private static class TestableSitesOverviewComponent extends SitesOverviewComponent {
		private final DeviceComponent deviceComponent;
		private final AlarmComponent alarmComponent;
		private final SubscriptionComponent subscriptionComponent;
		private final OpenSearchComponent osComponent;
		private final LocationComponent locationComponent;

		public TestableSitesOverviewComponent(
				DeviceComponent deviceComponent,
				AlarmComponent alarmComponent,
				SubscriptionComponent subscriptionComponent,
				OpenSearchComponent osComponent,
				LocationComponent locationComponent) {
			this.deviceComponent = deviceComponent;
			this.alarmComponent = alarmComponent;
			this.subscriptionComponent = subscriptionComponent;
			this.osComponent = osComponent;
			this.locationComponent = locationComponent;
		}

		@Override
		protected DeviceComponent getDeviceComponent() {
			return deviceComponent;
		}

		@Override
		protected AlarmComponent getAlarmComponent() {
			return alarmComponent;
		}

		@Override
		protected SubscriptionComponent getSubscriptionComponent() {
			return subscriptionComponent;
		}

		@Override
		protected OpenSearchComponent getOSComponent() {
			return osComponent;
		}

		@Override
		protected LocationComponent getLocationComponent() {
			return locationComponent;
		}
	}

	@BeforeEach
	void setUp() {
		sitesOverviewComponent = new TestableSitesOverviewComponent(
				mockDeviceComponent,
				mockAlarmComponent,
				mockSubscriptionComponent,
				mockOSComponent,
				mockLocationComponent);
	}

	@Test
	void testGetExtendedSiteOverviewData_withValidSite_returnsSiteData() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		List<Device> devices = createDeviceList();
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(site));
		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID)).thenReturn(devices);
		when(mockAlarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE))
				.thenReturn(Optional.of(LocalDateTime.now()));

		SitesSiteData result = sitesOverviewComponent.getExtendedSiteOverviewData(SITE_ID, searchJson);

		assertNotNull(result);
		assertEquals(site, result.getSite());
		verify(mockDeviceComponent).findDeviceById(SITE_ID, CUSTOMER_ID);
		verify(mockSubscriptionComponent).addSubscriptionInformation(result, CUSTOMER_ID);
	}

	@Test
	void testGetExtendedSiteOverviewData_withNonExistentSite_returnsNull() {
		SearchJSON searchJson = createSearchJSON();

		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		SitesSiteData result = sitesOverviewComponent.getExtendedSiteOverviewData(SITE_ID, searchJson);

		assertNull(result);
		verify(mockDeviceComponent).findDeviceById(SITE_ID, CUSTOMER_ID);
		verify(mockSubscriptionComponent).addSubscriptionInformation(null, CUSTOMER_ID);
	}

	@Test
	void testGetExtendedSiteOverviewData_populatesDevicesAndAlarms() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		List<Device> devices = createDeviceList();
		Alarm alarm = createAlarm();
		List<Alarm> alarms = Collections.singletonList(alarm);

		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(site));
		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID)).thenReturn(devices);
		when(mockAlarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE))
				.thenReturn(Optional.of(LocalDateTime.now()));

		SitesSiteData result = sitesOverviewComponent.getExtendedSiteOverviewData(SITE_ID, searchJson);

		assertNotNull(result);
		assertEquals(devices, result.getDevices());
		assertEquals(1, result.getAlarms().size());
	}

	@Test
	void testGetExtendedSiteOverviewData_filtersOldClosedAlarms() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();

		Alarm activeAlarm = createAlarm();
		activeAlarm.setState(IAlarmConstants.ACTIVE);
		activeAlarm.setStartDate(START_DATE - TimeConstants.DAY * 30);

		Alarm inRangeAlarm = createAlarm();
		inRangeAlarm.setState(IAlarmConstants.RESOLVED);
		inRangeAlarm.setStartDate(START_DATE + TimeConstants.DAY);

		Alarm oldClosedAlarm = createAlarm();
		oldClosedAlarm.setState(IAlarmConstants.RESOLVED);
		oldClosedAlarm.setStartDate(START_DATE - TimeConstants.DAY * 30);

		List<Alarm> alarms = Arrays.asList(activeAlarm, inRangeAlarm, oldClosedAlarm);

		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(site));
		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID)).thenReturn(Collections.emptyList());
		when(mockAlarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE))
				.thenReturn(Optional.of(LocalDateTime.now()));

		SitesSiteData result = sitesOverviewComponent.getExtendedSiteOverviewData(SITE_ID, searchJson);

		assertNotNull(result);
		assertEquals(2, result.getAlarms().size());
		assertTrue(result.getAlarms().contains(activeAlarm));
		assertTrue(result.getAlarms().contains(inRangeAlarm));
		assertFalse(result.getAlarms().contains(oldClosedAlarm));
	}

	@Test
	void testGetWeatherInformation_withValidWeatherData_returnsWeatherData() {
		Device site = createSiteDevice();
		DeviceData deviceData = createDeviceDataWithWeather();

		when(mockOSComponent.getLastDeviceEntry(eq(SITE_ID), any())).thenReturn(deviceData);

		SiteWeatherData result = sitesOverviewComponent.getWeatherInformation(site);

		assertNotNull(result);
		assertEquals("Sunny", result.getWeatherSummary());
		assertEquals(75.5f, result.getTemperature());
		verify(mockOSComponent).getLastDeviceEntry(eq(SITE_ID), any());
	}

	@Test
	void testGetWeatherInformation_withNoWeatherData_returnsNull() {
		Device site = createSiteDevice();

		when(mockOSComponent.getLastDeviceEntry(eq(SITE_ID), any())).thenReturn(null);

		SiteWeatherData result = sitesOverviewComponent.getWeatherInformation(site);

		assertNull(result);
	}

	@Test
	void testGetWeatherInformation_withEmptyWeatherSummary_returnsNull() {
		Device site = createSiteDevice();
		DeviceData deviceData = new DeviceData();
		deviceData.setWeatherSummary("");

		when(mockOSComponent.getLastDeviceEntry(eq(SITE_ID), any())).thenReturn(deviceData);

		SiteWeatherData result = sitesOverviewComponent.getWeatherInformation(site);

		assertNull(result);
	}

	@Test
	void testFillLocalTimeInformation_withValidCoordinates_setsLocalTime() {
		Device site = createSiteDevice();
		SitesSiteData siteData = new SitesSiteData();
		LocalDateTime mockTime = LocalDateTime.now();

		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE)).thenReturn(Optional.of(mockTime));

		sitesOverviewComponent.fillLocalTimeInformation(siteData, site);

		assertNotNull(siteData.getLocalTime());
		verify(mockLocationComponent).getLocalTimeString(LATITUDE, LONGITUDE);
	}

	@Test
	void testFillLocalTimeInformation_withNoLocationData_doesNotSetLocalTime() {
		Device site = createSiteDevice();
		SitesSiteData siteData = new SitesSiteData();

		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

		sitesOverviewComponent.fillLocalTimeInformation(siteData, site);

		assertNull(siteData.getLocalTime());
	}

	@Test
	void testGetMaxInformation_returnsSearchResponse() {
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		SearchResponse<DeviceData> result = sitesOverviewComponent.getMaxInformation(DEVICE_ID, CUSTOMER_ID);

		assertNotNull(result);
		assertEquals(mockSearchResponse, result);
		verify(mockOSComponent).search(any(SearchJSON.class));
	}

	@Test
	void testFillSiteTimeSeries_withSubtractionSite_usesDeviceId() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		site.setSubtraction(true);
		SitesSiteData siteData = new SitesSiteData();
		siteData.setSite(site);

		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		sitesOverviewComponent.fillSiteTimeSeries(siteData, searchJson);

		assertNotNull(siteData.getTimeSeries());
		verify(mockOSComponent)
				.search(argThat(search -> SITE_ID.equals(search.getDeviceId()) && search.getSiteId() == null));
	}

	@Test
	void testFillSiteTimeSeries_withNormalSite_usesSiteId() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		site.setSubtraction(false);
		SitesSiteData siteData = new SitesSiteData();
		siteData.setSite(site);

		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		sitesOverviewComponent.fillSiteTimeSeries(siteData, searchJson);

		assertNotNull(siteData.getTimeSeries());
		verify(mockOSComponent)
				.search(argThat(search -> search.getDeviceId() == null && SITE_ID.equals(search.getSiteId())));
	}

	@Test
	void testGetExtendedSiteOverviewData_populatesDeviceMaps() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		Device device1 = createDevice("device1");
		Device device2 = createDevice("device2");
		List<Device> devices = Arrays.asList(device1, device2);

		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(site));
		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID)).thenReturn(devices);
		when(mockAlarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(Collections.emptyList());
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE))
				.thenReturn(Optional.of(LocalDateTime.now()));

		SitesSiteData result = sitesOverviewComponent.getExtendedSiteOverviewData(SITE_ID, searchJson);

		assertNotNull(result);
		assertNotNull(result.getDeviceAvg());
		assertNotNull(result.getDeviceTotals());
		assertNotNull(result.getDeviceTimeSeries());
		assertNotNull(result.getDeviceWeeklyMaxPower());
		assertEquals(2, result.getDeviceAvg().size());
		assertEquals(2, result.getDeviceTotals().size());
		assertEquals(2, result.getDeviceTimeSeries().size());
		assertEquals(2, result.getDeviceWeeklyMaxPower().size());
	}

	@Test
	void testGetExtendedSiteOverviewData_excludesSiteDevicesFromDeviceMaps() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		Device normalDevice = createDevice("device1");
		Device siteDevice = createDevice("device2");
		siteDevice.setIsSite("1");
		List<Device> devices = Arrays.asList(normalDevice, siteDevice);

		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(site));
		when(mockDeviceComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID)).thenReturn(devices);
		when(mockAlarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(Collections.emptyList());
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockLocationComponent.getLocalTimeString(LATITUDE, LONGITUDE))
				.thenReturn(Optional.of(LocalDateTime.now()));

		SitesSiteData result = sitesOverviewComponent.getExtendedSiteOverviewData(SITE_ID, searchJson);

		assertNotNull(result);
		assertEquals(1, result.getDeviceAvg().size());
		assertEquals(1, result.getDeviceTotals().size());
		assertEquals(1, result.getDeviceTimeSeries().size());
		assertEquals(1, result.getDeviceWeeklyMaxPower().size());
		assertTrue(result.getDeviceAvg().containsKey("device1"));
		assertFalse(result.getDeviceAvg().containsKey("device2"));
	}

	private SearchJSON createSearchJSON() {
		SearchJSON searchJson = new SearchJSON();
		searchJson.setCustomerId(CUSTOMER_ID);
		searchJson.setStartDate(START_DATE);
		searchJson.setEndDate(END_DATE);
		searchJson.setTimeZone("America/Chicago");
		return searchJson;
	}

	private Device createSiteDevice() {
		Device site = new Device();
		site.setId(SITE_ID);
		site.setSiteId(SITE_ID);
		site.setClientId(CUSTOMER_ID);
		site.setName("Test Site");
		site.setIsSite("1");
		site.setLatitude(LATITUDE);
		site.setLongitude(LONGITUDE);
		return site;
	}

	private Device createDevice(String deviceId) {
		Device device = new Device();
		device.setId(deviceId);
		device.setClientId(CUSTOMER_ID);
		device.setName("Device " + deviceId);
		device.setIsSite("");
		return device;
	}

	private List<Device> createDeviceList() {
		return Collections.singletonList(createDevice(DEVICE_ID));
	}

	private Alarm createAlarm() {
		Alarm alarm = new Alarm();
		alarm.setCustomerId(CUSTOMER_ID);
		alarm.setDeviceId(SITE_ID);
		alarm.setStartDate(START_DATE + TimeConstants.DAY);
		alarm.setState(IAlarmConstants.ACTIVE);
		return alarm;
	}

	private DeviceData createDeviceDataWithWeather() {
		DeviceData deviceData = new DeviceData();
		deviceData.setWeatherSummary("Sunny");
		deviceData.setTemperature(75.5f);
		deviceData.setUVIndex(5.0f);
		deviceData.setPrecipitationIntensity(0.0f);
		deviceData.setIcon("clear-day");
		return deviceData;
	}
}
