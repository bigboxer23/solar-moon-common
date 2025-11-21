package com.bigboxer23.solar_moon.aggregated.overview;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.aggregated.sites.SitesOverviewComponent;
import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.alarm.IAlarmConstants;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.core.SearchResponse;

@ExtendWith(MockitoExtension.class)
public class OverviewComponentTest {

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private AlarmComponent mockAlarmComponent;

	@Mock
	private SubscriptionComponent mockSubscriptionComponent;

	@Mock
	private OpenSearchComponent mockOSComponent;

	@Mock
	private SitesOverviewComponent mockSitesOverviewComponent;

	@Mock
	private SearchResponse<DeviceData> mockSearchResponse;

	private TestableOverviewComponent overviewComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "test-device-456";
	private static final String SITE_NAME = "Test Site";
	private static final String TIMEZONE = "America/Chicago";
	private static final long START_DATE = System.currentTimeMillis() - TimeConstants.DAY * 7;
	private static final long END_DATE = System.currentTimeMillis();

	private static class TestableOverviewComponent extends OverviewComponent {
		private final DeviceComponent deviceComponent;
		private final AlarmComponent alarmComponent;
		private final SubscriptionComponent subscriptionComponent;
		private final OpenSearchComponent osComponent;
		private final SitesOverviewComponent sitesOverviewComponent;

		public TestableOverviewComponent(
				DeviceComponent deviceComponent,
				AlarmComponent alarmComponent,
				SubscriptionComponent subscriptionComponent,
				OpenSearchComponent osComponent,
				SitesOverviewComponent sitesOverviewComponent) {
			this.deviceComponent = deviceComponent;
			this.alarmComponent = alarmComponent;
			this.subscriptionComponent = subscriptionComponent;
			this.osComponent = osComponent;
			this.sitesOverviewComponent = sitesOverviewComponent;
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
		protected SitesOverviewComponent getSitesOverviewComponent() {
			return sitesOverviewComponent;
		}
	}

	@BeforeEach
	void setUp() {
		overviewComponent = new TestableOverviewComponent(
				mockDeviceComponent,
				mockAlarmComponent,
				mockSubscriptionComponent,
				mockOSComponent,
				mockSitesOverviewComponent);
	}

	@Test
	void testGetOverviewData_withValidData_returnsOverviewData() {
		SearchJSON searchJson = createSearchJSON();
		List<Device> devices = createDeviceList();
		List<Alarm> alarms = createAlarmList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertEquals(devices, result.getDevices());
		verify(mockDeviceComponent).getDevicesForCustomerId(CUSTOMER_ID);
		verify(mockAlarmComponent).getAlarms(CUSTOMER_ID);
		verify(mockSubscriptionComponent).addSubscriptionInformation(result, CUSTOMER_ID);
		assertTrue(searchJson.getIsSite());
	}

	@Test
	void testGetOverviewData_withNoDevices_returnsEmptyOverviewData() {
		SearchJSON searchJson = createSearchJSON();
		List<Alarm> alarms = createAlarmList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(Collections.emptyList());
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertTrue(result.getDevices().isEmpty());
	}

	@Test
	void testGetOverviewData_withNoAlarms_returnsOverviewDataWithEmptyAlarms() {
		SearchJSON searchJson = createSearchJSON();
		List<Device> devices = createDeviceList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(Collections.emptyList());
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertTrue(result.getAlarms().isEmpty());
	}

	@Test
	void testGetOverviewData_withActiveAlarms_includesActiveAlarms() {
		SearchJSON searchJson = createSearchJSON();
		List<Device> devices = createDeviceList();

		Alarm activeAlarm = new Alarm();
		activeAlarm.setState(IAlarmConstants.ACTIVE);
		activeAlarm.setStartDate(START_DATE - TimeConstants.DAY * 30);

		Alarm inRangeAlarm = new Alarm();
		inRangeAlarm.setState(IAlarmConstants.RESOLVED);
		inRangeAlarm.setStartDate(START_DATE + TimeConstants.DAY);

		Alarm oldClosedAlarm = new Alarm();
		oldClosedAlarm.setState(IAlarmConstants.RESOLVED);
		oldClosedAlarm.setStartDate(START_DATE - TimeConstants.DAY * 30);

		List<Alarm> alarms = Arrays.asList(activeAlarm, inRangeAlarm, oldClosedAlarm);

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertEquals(2, result.getAlarms().size());
		assertTrue(result.getAlarms().contains(activeAlarm));
		assertTrue(result.getAlarms().contains(inRangeAlarm));
		assertFalse(result.getAlarms().contains(oldClosedAlarm));
	}

	@Test
	void testGetOverviewData_withSiteDevices_populatesSitesOverviewData() {
		SearchJSON searchJson = createSearchJSON();
		Device site = createSiteDevice();
		List<Device> devices = Collections.singletonList(site);
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertNotNull(result.getSitesOverviewData());
		assertTrue(result.getSitesOverviewData().containsKey(SITE_NAME));
	}

	@Test
	void testGetOverviewData_withTimeZone_populatesOverallData() {
		SearchJSON searchJson = createSearchJSON();
		List<Device> devices = createDeviceList();
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockOSComponent.getAverageEnergyConsumedPerDay(any(SearchJSON.class)))
				.thenReturn(123.45);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertNotNull(result.getOverall());
		assertEquals(123.45, result.getOverall().getDailyEnergyConsumedAverage());
	}

	@Test
	void testGetOverviewData_withoutTimeZone_doesNotPopulateOverallData() {
		SearchJSON searchJson = createSearchJSON();
		searchJson.setTimeZone(null);
		List<Device> devices = createDeviceList();
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertNull(result.getOverall());
	}

	@Test
	void testGetOverviewData_withBlankTimeZone_doesNotPopulateOverallData() {
		SearchJSON searchJson = createSearchJSON();
		searchJson.setTimeZone("");
		List<Device> devices = createDeviceList();
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertNull(result.getOverall());
	}

	@Test
	void testGetOverviewData_withNullDevices_handlesGracefully() {
		SearchJSON searchJson = createSearchJSON();
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(null);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertNull(result.getDevices());
	}

	@Test
	void testGetOverviewData_verifySearchTypesUsed() {
		SearchJSON searchJson = createSearchJSON();
		List<Device> devices = createDeviceList();
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);
		when(mockOSComponent.getAverageEnergyConsumedPerDay(any(SearchJSON.class)))
				.thenReturn(100.0);

		overviewComponent.getOverviewData(searchJson);

		verify(mockOSComponent, atLeastOnce())
				.search(argThat(search -> OpenSearchConstants.AVG_SEARCH_TYPE.equals(search.getType())));
		verify(mockOSComponent, atLeastOnce())
				.search(argThat(search -> OpenSearchConstants.AVG_TOTAL_SEARCH_TYPE.equals(search.getType())));
		verify(mockOSComponent, atLeastOnce()).getAverageEnergyConsumedPerDay(any(SearchJSON.class));
	}

	@Test
	void testGetOverviewData_withMultipleSites_populatesAllSites() {
		SearchJSON searchJson = createSearchJSON();
		Device site1 = createSiteDevice();
		Device site2 = createSiteDevice();
		site2.setName("Site 2");
		List<Device> devices = Arrays.asList(site1, site2);
		List<Alarm> alarms = Collections.emptyList();

		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockAlarmComponent.getAlarms(CUSTOMER_ID)).thenReturn(alarms);
		when(mockOSComponent.search(any(SearchJSON.class))).thenReturn(mockSearchResponse);

		OverviewData result = overviewComponent.getOverviewData(searchJson);

		assertNotNull(result);
		assertNotNull(result.getSitesOverviewData());
		assertEquals(2, result.getSitesOverviewData().size());
		assertTrue(result.getSitesOverviewData().containsKey(SITE_NAME));
		assertTrue(result.getSitesOverviewData().containsKey("Site 2"));
	}

	private SearchJSON createSearchJSON() {
		SearchJSON searchJson = new SearchJSON();
		searchJson.setCustomerId(CUSTOMER_ID);
		searchJson.setStartDate(START_DATE);
		searchJson.setEndDate(END_DATE);
		searchJson.setTimeZone(TIMEZONE);
		return searchJson;
	}

	private List<Device> createDeviceList() {
		List<Device> devices = new ArrayList<>();
		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setName("Test Device");
		device.setIsSite("");
		devices.add(device);
		return devices;
	}

	private Device createSiteDevice() {
		Device site = new Device();
		site.setId(DEVICE_ID);
		site.setClientId(CUSTOMER_ID);
		site.setName(SITE_NAME);
		site.setIsSite("1");
		return site;
	}

	private List<Alarm> createAlarmList() {
		List<Alarm> alarms = new ArrayList<>();
		Alarm alarm = new Alarm();
		alarm.setCustomerId(CUSTOMER_ID);
		alarm.setDeviceId(DEVICE_ID);
		alarm.setStartDate(START_DATE + TimeConstants.DAY);
		alarm.setState(IAlarmConstants.ACTIVE);
		alarms.add(alarm);
		return alarms;
	}
}
