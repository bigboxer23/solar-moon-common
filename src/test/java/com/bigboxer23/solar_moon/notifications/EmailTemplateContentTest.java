package com.bigboxer23.solar_moon.notifications;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class EmailTemplateContentTest {

	@Test
	public void testDefaultConstructor() {
		EmailTemplateContent content = new EmailTemplateContent();

		assertNotNull(content);
		assertNull(content.getTemplateName());
		assertNull(content.getTitle());
		assertNull(content.getCustomerName());
		assertNull(content.getBodyContent1());
		assertNull(content.getButtonContent());
		assertEquals("", content.getBodyContent2());
		assertEquals("", content.getLink());
		assertEquals("#5178c2", content.getButtonColor());
		assertEquals("#eef2f9", content.getBackgroundColor());
		assertEquals("#FFFFFF", content.getPanelColor());
		assertEquals("#FFFFFF", content.getButtonTextColor());
		assertEquals("#000000", content.getFontColor());
		assertEquals("", content.getBorder());
		assertEquals("" + LocalDate.now().getYear(), content.getYear());
	}

	@Test
	public void testParameterizedConstructor() {
		EmailTemplateContent content = new EmailTemplateContent(
				"test.template.html", "Test Title", "John Doe", "This is the body", "Click Me");

		assertEquals("test.template.html", content.getTemplateName());
		assertEquals("Test Title", content.getTitle());
		assertEquals("John Doe", content.getCustomerName());
		assertEquals("This is the body", content.getBodyContent1());
		assertEquals("Click Me", content.getButtonContent());
		assertEquals("", content.getBodyContent2());
		assertEquals("", content.getLink());
		assertEquals("#5178c2", content.getButtonColor());
	}

	@Test
	public void testSettersAndGetters() {
		EmailTemplateContent content = new EmailTemplateContent();

		content.setTemplateName("custom.html");
		content.setTitle("Custom Title");
		content.setCustomerName("Jane Smith");
		content.setBodyContent1("First body content");
		content.setButtonContent("View Details");
		content.setBodyContent2("Second body content");
		content.setLink("https://example.com");
		content.setButtonColor("#FF0000");
		content.setBackgroundColor("#000000");
		content.setPanelColor("#CCCCCC");
		content.setButtonTextColor("#00FF00");
		content.setFontColor("#FFFFFF");
		content.setBorder("1px solid black");

		assertEquals("custom.html", content.getTemplateName());
		assertEquals("Custom Title", content.getTitle());
		assertEquals("Jane Smith", content.getCustomerName());
		assertEquals("First body content", content.getBodyContent1());
		assertEquals("View Details", content.getButtonContent());
		assertEquals("Second body content", content.getBodyContent2());
		assertEquals("https://example.com", content.getLink());
		assertEquals("#FF0000", content.getButtonColor());
		assertEquals("#000000", content.getBackgroundColor());
		assertEquals("#CCCCCC", content.getPanelColor());
		assertEquals("#00FF00", content.getButtonTextColor());
		assertEquals("#FFFFFF", content.getFontColor());
		assertEquals("1px solid black", content.getBorder());
	}

	@Test
	public void testYearIsCurrentYear() {
		EmailTemplateContent content = new EmailTemplateContent();
		int currentYear = LocalDate.now().getYear();

		assertEquals("" + currentYear, content.getYear());
	}
}
