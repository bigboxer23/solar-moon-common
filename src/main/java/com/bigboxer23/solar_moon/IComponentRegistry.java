package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.device.DeviceUpdateComponent;
import com.bigboxer23.solar_moon.device.SiteComponent;
import com.bigboxer23.solar_moon.gson.SearchResponseAdapter;
import com.bigboxer23.solar_moon.ingest.GenerationMeterComponent;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.maintenance.MaintenanceComponent;
import com.bigboxer23.solar_moon.mapping.MappingComponent;
import com.bigboxer23.solar_moon.notifications.NotificationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import com.bigboxer23.solar_moon.weather.PirateWeatherComponent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.moshi.Moshi;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public interface IComponentRegistry {
	Moshi moshi = new Moshi.Builder().build();

	Gson gson = new GsonBuilder()
			.registerTypeAdapter(SearchResponse.class, new SearchResponseAdapter())
			.create();

	CustomerComponent customerComponent = new CustomerComponent();

	SubscriptionComponent subscriptionComponent = new SubscriptionComponent();

	DeviceComponent deviceComponent = new DeviceComponent(subscriptionComponent);

	OpenSearchComponent OSComponent = new OpenSearchComponent();

	PirateWeatherComponent weatherComponent = new PirateWeatherComponent();

	NotificationComponent notificationComponent = new NotificationComponent();

	AlarmComponent alarmComponent = new AlarmComponent(deviceComponent, OSComponent, notificationComponent);

	SiteComponent siteComponent = new SiteComponent(OSComponent, deviceComponent);

	GenerationMeterComponent generationComponent =
			new GenerationMeterComponent(OSComponent, alarmComponent, deviceComponent, siteComponent);

	MaintenanceComponent maintenanceComponent = new MaintenanceComponent();

	DeviceUpdateComponent deviceUpdateComponent = new DeviceUpdateComponent();

	LocationComponent locationComponent = new LocationComponent();

	MappingComponent mappingComponent = new MappingComponent();

	Logger logger = LoggerFactory.getLogger(IComponentRegistry.class);
}
