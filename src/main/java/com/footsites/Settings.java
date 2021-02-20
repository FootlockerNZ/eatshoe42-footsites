package com.footsites;

public class Settings {
	private String webhook_url = "https://canary.discord.com/api/webhooks/812562591938904074/2GIVP4gvbnqQ3tLdC5Xl_GGmy5I1LX-y3ve04lQkoLbvBcy11EtGCk0XheC6RWub6ZLl";
	private int delay = 3500;

	public Settings() {}
	
	public String getWebhook() {
		return webhook_url;
	}
	
	public int getDelay() {
		return delay;
	}
	
	public void setWebhook(String newWebhook) {
		webhook_url = newWebhook;
	}
	
	public void setDelay(int newDelay) {
		delay = newDelay;
	}
	
	
	
	
	
	
}
