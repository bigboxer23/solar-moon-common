package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.aggregated.overview.OverviewComponent;
import com.bigboxer23.solar_moon.aggregated.sites.SitesOverviewComponent;
import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.device.*;
import com.bigboxer23.solar_moon.download.DownloadComponent;
import com.bigboxer23.solar_moon.gson.SearchResponseAdapter;
import com.bigboxer23.solar_moon.ingest.IngestComponent;
import com.bigboxer23.solar_moon.ingest.ObviusIngestComponent;
import com.bigboxer23.solar_moon.ingest.sma.SMAIngestComponent;
import com.bigboxer23.solar_moon.ingest.sma.SMAS3CleanupComponent;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.maintenance.MaintenanceComponent;
import com.bigboxer23.solar_moon.mapping.MappingComponent;
import com.bigboxer23.solar_moon.notifications.NotificationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.search.status.OpenSearchStatusComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import com.bigboxer23.solar_moon.weather.CachingPirateWeatherComponent;
import com.bigboxer23.solar_moon.weather.PirateWeatherComponent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.moshi.Moshi;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
public interface IComponentRegistry {
	Moshi moshi = new Moshi.Builder().build();

	Gson gson = new GsonBuilder()
			.registerTypeAdapter(SearchResponse.class, new SearchResponseAdapter())
			.create();

	CustomerComponent customerComponent = new CustomerComponent();

	SubscriptionComponent subscriptionComponent = new SubscriptionComponent();

	DeviceComponent deviceComponent = new CachingDeviceComponent();

	OpenSearchComponent OSComponent = new OpenSearchComponent();

	OpenSearchStatusComponent OpenSearchStatusComponent = new OpenSearchStatusComponent();

	PirateWeatherComponent weatherComponent = new CachingPirateWeatherComponent();

	NotificationComponent notificationComponent = new NotificationComponent();

	AlarmComponent alarmComponent = new AlarmComponent();

	VirtualDeviceComponent virtualDeviceComponent = new VirtualDeviceComponent();

	IngestComponent generationComponent = new IngestComponent();

	ObviusIngestComponent obviousIngestComponent = new ObviusIngestComponent();

	SMAIngestComponent smaIngestComponent = new SMAIngestComponent();

	SMAS3CleanupComponent smaCleanupComponent = new SMAS3CleanupComponent();

	MaintenanceComponent maintenanceComponent = new MaintenanceComponent();

	DeviceUpdateComponent deviceUpdateComponent = new DeviceUpdateComponent();

	LocationComponent locationComponent = new LocationComponent();

	MappingComponent mappingComponent = new MappingComponent();

	OverviewComponent overviewComponent = new OverviewComponent();

	SitesOverviewComponent sitesOverviewComponent = new SitesOverviewComponent();

	DownloadComponent downloadComponent = new DownloadComponent();

	LinkedDeviceComponent linkedDeviceComponent = new LinkedDeviceComponent();
}
