package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.maintenance.MaintenanceComponent;
import com.bigboxer23.solar_moon.notifications.NotificationComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.squareup.moshi.Moshi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public interface IComponentRegistry {
	Moshi moshi = new Moshi.Builder().build();
	CustomerComponent customerComponent = new CustomerComponent();

	SubscriptionComponent subscriptionComponent = new SubscriptionComponent();

	DeviceComponent deviceComponent = new DeviceComponent(subscriptionComponent);

	OpenSearchComponent OSComponent = new OpenSearchComponent();

	OpenWeatherComponent weatherComponent = new OpenWeatherComponent();

	NotificationComponent notificationComponent = new NotificationComponent();

	AlarmComponent alarmComponent =
			new AlarmComponent(weatherComponent, deviceComponent, OSComponent, notificationComponent);

	SiteComponent siteComponent = new SiteComponent(OSComponent, deviceComponent);

	GenerationMeterComponent generationComponent =
			new GenerationMeterComponent(OSComponent, alarmComponent, deviceComponent, siteComponent);

	MaintenanceComponent maintenanceComponent = new MaintenanceComponent();

	DeviceUpdateComponent deviceUpdateComponent = new DeviceUpdateComponent();

	Logger logger = LoggerFactory.getLogger(IComponentRegistry.class);
}
