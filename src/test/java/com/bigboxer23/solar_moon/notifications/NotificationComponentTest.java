package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NotificationComponentTest {

	private NotificationComponent component;

	@BeforeEach
	public void setup() {
		component = new NotificationComponent();
	}

	@Test
	public void testComponentInstantiation() {
		assertNotNull(component);
	}

	@Test
	public void testSendNotification_withValidTemplate_expectsAwsConfiguration() {
		EmailTemplateContent template =
				new EmailTemplateContent("email.template.html", "Test Title", "John Doe", "Test body", "Click here");

		assertThrows(
				NullPointerException.class,
				() -> component.sendNotification("test@example.com", "Test Subject", template));
	}

	@Test
	public void testSendNotification_withSupportTemplate_expectsAwsConfiguration() {
		SupportEmailTemplateContent template =
				new SupportEmailTemplateContent("Support Subject", "Jane Smith", "Response text", "Previous email");

		assertThrows(
				NullPointerException.class,
				() -> component.sendNotification("support@example.com", "Support Subject", template));
	}

	@Test
	public void testSendNotification_withInvalidTemplate_throwsMustacheException() {
		EmailTemplateContent template = new EmailTemplateContent(
				"nonexistent-template.html", "Title", "User", "Body content goes here", "View More");

		assertThrows(RuntimeException.class, () -> component.sendNotification("test@example.com", "Test", template));
	}

	@Test
	public void testSendNotification_withNullTemplate_throwsNullPointer() {
		EmailTemplateContent template = new EmailTemplateContent(null, "Title", "User", "Body", "Button");

		assertThrows(
				NullPointerException.class, () -> component.sendNotification("test@example.com", "Test", template));
	}

	@Test
	public void testSendNotification_withEmptyRecipient_expectsAwsConfiguration() {
		EmailTemplateContent template = new EmailTemplateContent("email.template.html", "Title", "User", "Body", "Go");

		assertThrows(NullPointerException.class, () -> component.sendNotification("", "Subject", template));
	}

	@Test
	public void testSendResponseMail_expectsAwsConfiguration() {
		assertThrows(
				NullPointerException.class,
				() -> component.sendResponseMail(
						"customer@example.com", "Support Question", "Our response", "Their email"));
	}
}
