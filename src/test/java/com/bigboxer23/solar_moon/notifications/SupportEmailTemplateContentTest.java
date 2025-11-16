package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SupportEmailTemplateContentTest {

	@Test
	public void testConstructor() {
		SupportEmailTemplateContent content =
				new SupportEmailTemplateContent("Support Request", "John Doe", "This is a response", "Previous email");

		assertEquals("support.email.template.html", content.getTemplateName());
		assertEquals("Support Request", content.getTitle());
		assertEquals("John Doe", content.getCustomerName());
		assertEquals("This is a response", content.getBodyContent1());
		assertEquals("", content.getButtonContent());
		assertEquals("Previous email", content.getBodyContent2());
	}

	@Test
	public void testConstructorWithEmptyStrings() {
		SupportEmailTemplateContent content = new SupportEmailTemplateContent("", "", "", "");

		assertEquals("support.email.template.html", content.getTemplateName());
		assertEquals("", content.getTitle());
		assertEquals("", content.getCustomerName());
		assertEquals("", content.getBodyContent1());
		assertEquals("", content.getButtonContent());
		assertEquals("", content.getBodyContent2());
	}

	@Test
	public void testConstructorWithLongContent() {
		String longResponse = "This is a very long response that contains multiple paragraphs and detailed"
				+ " information about the support request. It includes technical details and"
				+ " troubleshooting steps.";
		String longPrevious = "This is the previous email content that the customer sent, which may contain error"
				+ " messages, screenshots descriptions, and other relevant details about the"
				+ " issue they are experiencing.";

		SupportEmailTemplateContent content =
				new SupportEmailTemplateContent("Complex Support Issue", "Jane Smith", longResponse, longPrevious);

		assertEquals("Complex Support Issue", content.getTitle());
		assertEquals("Jane Smith", content.getCustomerName());
		assertEquals(longResponse, content.getBodyContent1());
		assertEquals(longPrevious, content.getBodyContent2());
	}
}
