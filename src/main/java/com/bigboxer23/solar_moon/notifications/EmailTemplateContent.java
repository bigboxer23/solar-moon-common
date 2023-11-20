package com.bigboxer23.solar_moon.notifications;

import lombok.Data;

/** */
@Data
public class EmailTemplateContent {
	public EmailTemplateContent(
			String templateName, String title, String customerName, String bodyContent1, String buttonContent) {
		setTemplateName(templateName);
		setTitle(title);
		setCustomerName(customerName);
		setBodyContent1(bodyContent1);
		setButtonContent(buttonContent);
	}

	private String templateName;

	private String title;

	private String customerName;

	private String bodyContent1;

	private String buttonContent;

	private String bodyContent2 = "";

	private String link = "";

	private String buttonColor = "#5178c2";

	private String backgroundColor = "#f6f6f6";

	private String panelColor = "#FFFFFF";

	private String buttonTextColor = "#FFFFFF";

	private String fontColor = "#000000";

	private String border = "";

	/*
	Dark

	private String backgroundColor = "#23272D";

	private String panelColor = "#282b2f";

	private String buttonTextColor = "#FFFFFF";

	private String fontColor = "#FFFFFF";

	private String border = "1px solid rgba(255, 255, 255, 0.15)";*/
}
