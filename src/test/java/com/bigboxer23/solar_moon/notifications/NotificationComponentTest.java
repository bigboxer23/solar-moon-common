package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.github.mustachejava.MustacheNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

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
		Exception exception = assertThrows(
				Exception.class,
				() -> component.sendResponseMail(
						"customer@example.com", "Support Question", "Our response", "Their email"));
		assertTrue(
				exception instanceof NullPointerException || exception instanceof SdkClientException,
				"Expected NullPointerException or SdkClientException but got: "
						+ exception.getClass().getName());
	}

	private SesClient mockSesClient;
	private CustomerComponent mockCustomerComponent;

	private class TestableNotificationComponent extends NotificationComponent {
		private final String recipientOverride;
		private final String additionalRecipient;

		TestableNotificationComponent(String recipientOverride, String additionalRecipient) {
			this.recipientOverride = recipientOverride;
			this.additionalRecipient = additionalRecipient;
		}

		@Override
		protected SesClient getSesClient() {
			return mockSesClient;
		}

		@Override
		protected CustomerComponent getCustomerComponent() {
			return mockCustomerComponent;
		}

		@Override
		protected String getRecipientOverride() {
			return recipientOverride;
		}

		@Override
		protected String getAdditionalRecipient() {
			return additionalRecipient;
		}
	}

	private TestableNotificationComponent testableComponent(String recipientOverride, String additionalRecipient) {
		mockSesClient = mock(SesClient.class);
		mockCustomerComponent = mock(CustomerComponent.class);
		return new TestableNotificationComponent(recipientOverride, additionalRecipient);
	}

	private EmailTemplateContent template() {
		return new EmailTemplateContent("email.template.html", "Test Title", "John Doe", "Test body", "Click here");
	}

	@Test
	public void testSendNotification_sendsRenderedEmailToRecipient() {
		TestableNotificationComponent component = testableComponent(null, null);

		component.sendNotification("sender@example.com", "user@example.com", "Test Subject", template());

		ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(mockSesClient).sendEmail(captor.capture());
		SendEmailRequest request = captor.getValue();
		assertEquals("sender@example.com", request.source());
		assertEquals(List.of("user@example.com"), request.destination().toAddresses());
		assertEquals("Test Subject", request.message().subject().data());
		assertTrue(request.message().body().html().data().contains("Test body"));
	}

	@Test
	public void testSendNotification_closesSesClient() {
		TestableNotificationComponent component = testableComponent(null, null);

		component.sendNotification("sender@example.com", "user@example.com", "Subject", template());

		verify(mockSesClient).close();
	}

	@Test
	public void testSendNotification_withRecipientOverride_redirectsEmail() {
		TestableNotificationComponent component = testableComponent("override@example.com", null);

		component.sendNotification("sender@example.com", "user@example.com", "Subject", template());

		ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(mockSesClient).sendEmail(captor.capture());
		assertEquals(
				List.of("override@example.com"), captor.getValue().destination().toAddresses());
	}

	@Test
	public void testSendNotification_withAdditionalRecipient_sendsSeparateEmail() {
		TestableNotificationComponent component = testableComponent(null, "cc@example.com");

		component.sendNotification("sender@example.com", "user@example.com", "Subject", template());

		verify(mockSesClient, times(2)).sendEmail(any(SendEmailRequest.class));
	}

	@Test
	public void testSendNotification_whenAdditionalRecipientMatchesRecipient_sendsOnce() {
		TestableNotificationComponent component = testableComponent(null, "user@example.com");

		component.sendNotification("sender@example.com", "user@example.com", "Subject", template());

		verify(mockSesClient, times(1)).sendEmail(any(SendEmailRequest.class));
	}

	@Test
	public void testSendNotification_whenSesFails_doesNotPropagate() {
		TestableNotificationComponent component = testableComponent(null, null);
		when(mockSesClient.sendEmail(any(SendEmailRequest.class)))
				.thenThrow(SesException.builder()
						.awsErrorDetails(AwsErrorDetails.builder()
								.errorMessage("rejected")
								.build())
						.build());

		assertDoesNotThrow(
				() -> component.sendNotification("sender@example.com", "user@example.com", "Subject", template()));
	}

	@Test
	public void testSendNotification_withUnknownTemplate_propagatesAndSendsNothing() {
		TestableNotificationComponent component = testableComponent(null, null);
		EmailTemplateContent broken =
				new EmailTemplateContent("does-not-exist.html", "Title", "Name", "Body", "Button");

		assertThrows(
				MustacheNotFoundException.class,
				() -> component.sendNotification("sender@example.com", "user@example.com", "Subject", broken));

		verify(mockSesClient, never()).sendEmail(any(SendEmailRequest.class));
	}

	@Test
	public void testGetRecipients_withoutOverrides_returnsOnlyRecipient() {
		assertEquals(List.of("user@example.com"), testableComponent(null, null).getRecipients("user@example.com"));
	}

	@Test
	public void testGetRecipients_withOverrideAndAdditional_returnsBoth() {
		assertEquals(
				List.of("override@example.com", "cc@example.com"),
				testableComponent("override@example.com", "cc@example.com").getRecipients("user@example.com"));
	}

	@Test
	public void testGetRecipients_withBlankAdditional_returnsOnlyRecipient() {
		assertEquals(List.of("user@example.com"), testableComponent(null, "  ").getRecipients("user@example.com"));
	}

	@Test
	public void testSendResponseMail_usesCustomerNameWhenKnown() {
		TestableNotificationComponent component = testableComponent(null, null);
		Customer customer = new Customer();
		customer.setName("Jane Smith");
		when(mockCustomerComponent.findCustomerByEmail("user@example.com")).thenReturn(Optional.of(customer));

		component.sendResponseMail("user@example.com", "Re: help", "our reply", "their question");

		ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(mockSesClient).sendEmail(captor.capture());
		assertTrue(captor.getValue().message().body().html().data().contains("Jane Smith"));
	}

	@Test
	public void testSendResponseMail_fallsBackToEmailWhenCustomerUnknown() {
		TestableNotificationComponent component = testableComponent(null, null);
		when(mockCustomerComponent.findCustomerByEmail("user@example.com")).thenReturn(Optional.empty());

		component.sendResponseMail("user@example.com", "Re: help", "our reply", "their question");

		ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(mockSesClient).sendEmail(captor.capture());
		assertTrue(captor.getValue().message().body().html().data().contains("user@example.com"));
	}
}
