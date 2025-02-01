package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.utils.properties.PropertyUtils;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class NotificationComponent {
	private static final MustacheFactory mf = new DefaultMustacheFactory("templates");

	private static final String SENDER = PropertyUtils.getProperty("emailer.info");

	private final String recipientOverride = PropertyUtils.getProperty("recipient_override");

	private final String additionalRecipient = PropertyUtils.getProperty("additional_recipient");

	public void sendNotification(String recipient, String subject, EmailTemplateContent template) {
		sendNotification(SENDER, recipient, subject, template);
	}

	public void sendNotification(String sender, String recipient, String subject, EmailTemplateContent template) {
		StringWriter content = new StringWriter();
		try {
			mf.compile(template.getTemplateName()).execute(content, template).flush();
		} catch (IOException e) {
			log.warn("sendNotification: template error", e);
			return;
		}
		getRecipients(recipient).forEach(r -> {
			try (SesClient client = SesClient.builder()
					.region(Region.of(PropertyUtils.getProperty("aws.region")))
					.credentialsProvider(DefaultCredentialsProvider.create())
					.build(); ) {
				log.info("Sending email to " + recipient + " proxy:" + r);
				client.sendEmail(SendEmailRequest.builder()
						.destination(Destination.builder().toAddresses(r).build())
						.message(Message.builder()
								.subject(Content.builder().data(subject).build())
								.body(Body.builder()
										.html(Content.builder()
												.data(content.toString())
												.build())
										.build())
								.build())
						.source(sender)
						.build());
			} catch (SesException e) {
				log.warn(e.awsErrorDetails().errorMessage(), e);
			}
		});
	}

	private List<String> getRecipients(String recipient) {
		List<String> recipients = new ArrayList<>();
		String fetchedRecipient = getRecipient(recipient);
		recipients.add(fetchedRecipient);
		if (!StringUtils.isBlank(additionalRecipient) && !additionalRecipient.equalsIgnoreCase(fetchedRecipient)) {
			recipients.add(additionalRecipient);
		}
		return recipients;
	}

	private String getRecipient(String recipient) {
		return StringUtils.isBlank(recipientOverride) ? recipient : recipientOverride;
	}

	public void sendResponseMail(String recipient, String subject, String responseContent, String previousContent) {
		IComponentRegistry.notificationComponent.sendNotification(
				PropertyUtils.getProperty("emailer.support"),
				recipient,
				subject,
				new SupportEmailTemplateContent(
						subject,
						IComponentRegistry.customerComponent
								.findCustomerByEmail(recipient)
								.map(Customer::getName)
								.orElse(recipient),
						responseContent,
						previousContent));
	}
}
