package com.bigboxer23.solar_moon.notifications;

import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class NotificationComponent {

	private static final Logger logger = LoggerFactory.getLogger(NotificationComponent.class);

	private static final MustacheFactory mf = new DefaultMustacheFactory("templates");

	private static final String SENDER = "info@solarmoonanalytics.com";

	private final String recipientOverride = PropertyUtils.getProperty("recipient_override");

	public void sendNotification(String recipient, String subject, EmailTemplateContent template) {
		StringWriter content = new StringWriter();
		try {
			mf.compile(template.getTemplateName()).execute(content, template).flush();
		} catch (IOException e) {
			logger.warn("sendNotification: template error", e);
			return;
		}
		try (SesClient client = SesClient.builder()
				.region(Region.US_WEST_2)
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build(); ) {
			logger.info("Sending email to " + recipient);
			client.sendEmail(SendEmailRequest.builder()
					.destination(Destination.builder()
							.toAddresses(getRecipient(recipient))
							.build())
					.message(Message.builder()
							.subject(Content.builder().data(subject).build())
							.body(Body.builder()
									.html(Content.builder()
											.data(content.toString())
											.build())
									.build())
							.build())
					.source(SENDER)
					.build());
		} catch (SesException e) {
			logger.warn(e.awsErrorDetails().errorMessage(), e);
		}
	}

	private String getRecipient(String recipient) {
		return StringUtils.isBlank(recipientOverride) ? recipient : recipientOverride;
	}
}
