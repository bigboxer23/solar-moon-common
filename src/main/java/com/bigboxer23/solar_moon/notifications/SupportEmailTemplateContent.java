package com.bigboxer23.solar_moon.notifications;

/** */
public class SupportEmailTemplateContent extends EmailTemplateContent {
	public SupportEmailTemplateContent(
			String subject, String customerName, String responseContent, String previousContent) {
		super("support.email.template.html", subject, customerName, responseContent, "");
		setBodyContent2(previousContent);
	}
}
