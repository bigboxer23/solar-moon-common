package com.bigboxer23.solar_moon.lambda.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** */
public class PropertyUtils {
	private static Properties props;

	public static String getProperty(String property) {
		if (props == null) {
			props = new Properties();
			try (InputStream inputStream =
					PropertyUtils.class.getClassLoader().getResourceAsStream("application.properties")) {
				props.load(inputStream);
			} catch (IOException e) {
				System.out.println("error " + e);
				e.printStackTrace();
				return null;
			}
		}
		return props.getProperty(property);
	}

	/*properties = new Properties();
	InputStream inputStream =
			getClass().getClassLoader().getResourceAsStream("application.properties");
	properties.load(inputStream);*/
}
