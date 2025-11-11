package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;

/** */
public class NotificationComponentTest
{

	public void sendNotification() {

		IComponentRegistry.notificationComponent.sendResponseMail(
				"blah@test.com",
				"Test subject",
				"body content",
				"Quoted content: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do"
						+ " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad"
						+ " minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex"
						+ " ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate"
						+ " velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat"
						+ " cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id"
						+ " est laborum");
	}
}
