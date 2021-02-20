package com.footsites;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
public class Task extends Thread{
	private String taskID;
	private int delay;
	private String proxy;
	private Profile profile;
	private static String sku;
	private String size;
	private static String site;
	private String csrf;
	private String sizeCode;
	private String cartGuid;
	private String uuid;
	private List<Cookie> cartCookies = new ArrayList<Cookie>();
	
	public Task(String taskID, int delay2, Profile testProfile, String sku2, String size2, String site2) {
		super.setName(taskID);
		this.taskID = taskID;
		this.delay = delay2;
		this.profile = testProfile;
		this.sku = sku2;
		this.size = size2;
		this.site = site2;
		this.proxy = Proxies.getProxy();
		this.uuid = UUID.randomUUID().toString();
	}
	
	Settings settings = new Settings();
	MyCookieJar jar = new MyCookieJar();
    
    Proxies proxies = new Proxies();
    String currentProxy = proxies.getProxy();
    
	
	Authenticator proxyAuthenticator = new Authenticator() {
		  @Override public Request authenticate(Route route, Response response) throws IOException {
		       String credential = Credentials.basic(proxies.getUsername(currentProxy), proxies.getPassword(currentProxy));
		       return response.request().newBuilder()
		           .header("Proxy-Authorization", credential)
		           .build();
		}
	};
	
	OkHttpClient httpClient = new OkHttpClient().newBuilder()
		    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxies.getIP(currentProxy),proxies.getPort(currentProxy))))
		    .proxyAuthenticator(proxyAuthenticator)
		    .cookieJar(jar)
		    .build();
	


	public void run(){
		this.log("msg", "Starting Task");
		this.getSession();
		this.findProduct();
		try {
			this.addToCart();
			this.submitEmail();
			this.submitShipping();
			this.submitBilling();
			try {
				this.submitOrder();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}

	
	public void log(String level, String status) {
		String taskID = Thread.currentThread().getName();
		switch(level){
			case "msg": {
				System.out.printf("%s [%23s] [%s] (Task %s) - %s %s\n", Colors.WHITE, getTimestamp(), this.site.toUpperCase(), this.taskID, status, Colors.RESET);
				break;
			}

			case "err": {
				System.out.printf("%s [%23s] [%s] (Task %s) - %s %s\n", Colors.RED, getTimestamp(), this.site.toUpperCase(), this.taskID, status, Colors.RESET);
				break;
			}
			
			
			
			case "success":{
				System.out.printf("%s [%23s] [%s] (Task %s) - %s %s\n", Colors.GREEN, getTimestamp(), this.site.toUpperCase(), this.taskID, status, Colors.RESET);
			}
			
			

			default: {}
		}
	}

	public String getTimestamp(){
		return new Timestamp(System.currentTimeMillis()).toString();
	}

	public void getSession() {
		this.log("msg", "Getting Session");
		final String SITE_SESSION = "https://www." + site +".com/api/v3/session?timestamp=" + System.currentTimeMillis();
		HttpUrl url = HttpUrl.parse(SITE_SESSION);
		String csrf = "";
		while(true){
			try {
				Request request = new Request.Builder()
						.url(SITE_SESSION)
						.addHeader("accept", "application/json")
						.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
						.addHeader("x-fl-request-id", uuid)
						.addHeader("sec-fetch-site", "same-origin")
						.addHeader("sec-fetch-mode", "cors")
						.addHeader("sec-fetch-dest", "empty")
						.addHeader("referer","https://www." + site + ".com/")
						.addHeader("accept-encoding", "deflate, br")
						.addHeader("accept-language", "en-US,en;q=0.9")
						.build();
				try(Response response = httpClient.newCall(request).execute()){
					Headers responseHeaders = response.headers();
					int statusCode = response.code();
					if(statusCode == 429) {
						this.log("err", "Rate Limited Searching for Product, Retrying");
						try {
							Thread.sleep(this.delay);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					if(statusCode == 503) {
						this.log("msg", "Waiting in Queue");
						try {
							putCookiesInJar(url,responseHeaders);
							Thread.sleep(this.delay);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					
					csrf = response.body().string().substring(22,58);
					this.csrf = csrf;
					
					String wholeCookie = responseHeaders.get("set-cookie");
					if(wholeCookie != null) {
						String jsessioncookie = getSplitCookie(wholeCookie);
						Cookie cookie = new Cookie.Builder()
								.domain(site + ".com")
								.path("/")
								.name("jsessionid")
								.value(jsessioncookie.substring(11))
								.httpOnly()
								.secure()
								.build();
						List<Cookie> jsessionlist = new ArrayList<Cookie>();
						jsessionlist.add(cookie);
						jar.saveFromResponse(url, jsessionlist);
						return;
					}
				}
			} catch (IOException e) {
				this.log("err", "Failed Getting Session");
			}
		}
	}

	public void findProduct() {
		this.log("msg", "Searching for Product");
		final String PRODUCT_API_LINK = "https://www." + site + ".com/api/products/pdp/" + sku + "?timestamp=" + System.currentTimeMillis();
		HttpUrl url = HttpUrl.parse(PRODUCT_API_LINK);
		while(true){
			try {
				Request request = new Request.Builder()
						.url(PRODUCT_API_LINK)
						.addHeader("accept","application/json")
						.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
						.addHeader("x-fl-request-id", uuid)
						.addHeader("sec-fetch-site", "same-origin")
						.addHeader("sec-fetch-mode", "cors")
						.addHeader("sec-fetch-dest", "empty")
						.addHeader("referer","https://www." + site + ".com/")
						.addHeader("accept-encoding", "deflate, br")
						.addHeader("accept-language", "en-US,en;q=0.9")
						.build();
				try(Response response = httpClient.newCall(request).execute()){
					Headers responseHeaders = response.headers();
					int statusCode = response.code();
					if(statusCode == 429) {
						this.log("err", "Rate Limited Searching for Product, Retrying");
						Thread.sleep(this.delay);
					}
					
					if(statusCode == 503) {
						this.log("msg", "Waiting in Queue");
						try {
							putCookiesInJar(url,responseHeaders);
							Thread.sleep(this.delay);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					try {
						JSONObject jsonObject = new JSONObject(response.body().string());
						JSONArray getSellableUnits = jsonObject.getJSONArray("sellableUnits");
						HashMap<String, String> sizeSkus = new HashMap<String, String>();
						for(int i = 0; i < getSellableUnits.length(); i++) {
							JSONObject obj = getSellableUnits.getJSONObject(i);
							JSONArray array = obj.getJSONArray("attributes");
							sizeSkus.put(array.getJSONObject(0).getString("value"), array.getJSONObject(0).getString("id"));
	
						}
	
						if(sizeSkus.containsKey(this.size)) {
							this.sizeCode = sizeSkus.get(this.size);
							this.log("msg", "Size Found");
							return;
						}else{
							this.log("msg", "Size Not Found, Retrying");
							Thread.sleep(this.delay);
						}
					}catch(org.json.JSONException e) {
						if(e.getMessage().contains("not found")) {
							this.log("err", "Product Unavailable, Retrying");
						}else {
							this.log("err", "Failed Finding Product, Retrying [PROXY ERROR]");
							
						}
						
						Thread.sleep(this.delay);
					}
				}
			} catch (IOException | InterruptedException e) {
				this.log("err", "Failed Finding Size, Retrying [PROXY ERROR]");
				try {
					Thread.sleep(this.delay);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	public void addToCart() throws InterruptedException {
		try {
		this.log("msg", "Adding to Cart");
		final String PRODUCT_ATC_LINK = "https://www." + site + ".com/api/users/carts/current/entries?timestamp=" + System.currentTimeMillis();
		HttpUrl url = HttpUrl.parse(PRODUCT_ATC_LINK);
		String json = new JSONObject()
				.put("productQuantity","1")
				.put("productId",this.sizeCode)
				.toString();
		@SuppressWarnings("deprecation")
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		Request request = new Request.Builder()
				.url(PRODUCT_ATC_LINK)
				.addHeader("content-length","44")
				.addHeader("x-csrf-token",this.csrf)
				.addHeader("x-fl-productid", this.sizeCode)
				.addHeader("content-type", "application/json")
				.addHeader("accept", "application/json")
				.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
				.addHeader("x-fl-request-id",uuid)
				.addHeader("origin","https://www." + site + ".com/")
				.addHeader("sec-fetch-site","same-origin")
				.addHeader("sec-fetch-mode","cors")
				.addHeader("sec-fetch-dest","empty")
				.addHeader("referer", "https://www." + site + ".com/product/-/" + sku + ".html")
				.addHeader("accept-encoding","deflate, br")
				.addHeader("accept-language", "en-US,en;q=0.9")
				.post(body)
				.build();
		
		while(true){
			try(Response response = httpClient.newCall(request).execute()){
				int statusCode = response.code();
				Headers responseHeaders = response.headers();
				if(statusCode == 200){
						this.log("success", "Product Added to Cart");
						putCookiesInJar(url,responseHeaders);
						return;
					}else if(statusCode == 403){
						
						//TODO: Add 2captcha/other solving for datadome captcha
						this.log("err", "DataDome Captcha, Rotating Proxy");		
						final String newProxy = proxies.getProxy();
						Authenticator newAuthenticator = new Authenticator() {
							  @Override public Request authenticate(Route route, Response response) throws IOException {
							       String credential = Credentials.basic(proxies.getUsername(newProxy), proxies.getPassword(newProxy));
							       return response.request().newBuilder()
							           .header("Proxy-Authorization", credential)
							           .build();
							}
						};
						
						httpClient = new OkHttpClient().newBuilder()
							    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxies.getIP(newProxy),proxies.getPort(newProxy))))
							    .proxyAuthenticator(newAuthenticator)
							    .cookieJar(jar)
							    .build();
						
						putCookiesInJar(url,responseHeaders);
					}else if(statusCode == 429){
						this.log("err", "Rate Limited, Retrying [429]");
						Thread.sleep(this.delay);
					}else if (statusCode == 531){
						this.log("msg", "Product Out of Stock, Retrying");
						Thread.sleep(this.delay);
					}else if (statusCode == 503){
						this.log("msg","Waiting in Queue");
						putCookiesInJar(url,responseHeaders);

					}else {
						this.log("err", "Unknown Status " + statusCode);
					}
			}catch (IOException e) {
				log("err", "Failed Adding to Cart, Retrying [PROXY ERROR]");
				Thread.sleep(this.delay);
				}
		}
		}catch(Exception e) {
			log("err", "Rate Limited, Retrying");
			Thread.sleep(this.delay);
		}
	}	
	
	
	public void submitEmail() throws InterruptedException {
		this.log("success", "Submitting Email");
		final String SUBMIT_EMAIL_LINK =  "https://www." + site + ".com/api/users/carts/current/email/" + profile.getEmail() + "?timestamp=" + System.currentTimeMillis();
		HttpUrl url = HttpUrl.parse(SUBMIT_EMAIL_LINK);
		@SuppressWarnings("deprecation")
		RequestBody body = RequestBody.create(null, new byte[0]);
		Request request = new Request.Builder()
				.url(SUBMIT_EMAIL_LINK)
				.addHeader("content-length","0")
				.addHeader("x-csrf-token",this.csrf)
				.addHeader("x-fl-productid", this.sizeCode)
				.addHeader("content-type", "application/json")
				.addHeader("accept", "application/json")
				.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
				.addHeader("x-fl-request-id",this.uuid)
				.addHeader("origin","https://www." + site + ".com/")
				.addHeader("sec-fetch-site","same-origin")
				.addHeader("sec-fetch-mode","cors")
				.addHeader("sec-fetch-dest","empty")
				.addHeader("referer", "https://www." + site + ".com/checkout")
				.addHeader("accept-encoding","deflate, br")
				.addHeader("accept-language", "en-US,en;q=0.9")
				.put(body)
				.build();
		while(true) {
			try(Response response = httpClient.newCall(request).execute()){
				int statusCode = response.code();
				Headers responseHeaders = response.headers();
				switch(statusCode) {
					case 200: {
						putCookiesInJar(url,responseHeaders);
						return;
					}
					case 403:{
						//TODO: solve datadome
						this.log("err", "DataDome Captcha [EMAIL], Rotating Proxy");		
						final String newProxy = proxies.getProxy();
						Authenticator newAuthenticator = new Authenticator() {
							  @Override public Request authenticate(Route route, Response response) throws IOException {
							       String credential = Credentials.basic(proxies.getUsername(newProxy), proxies.getPassword(newProxy));
							       return response.request().newBuilder()
							           .header("Proxy-Authorization", credential)
							           .build();
							}
						};
						
						httpClient = new OkHttpClient().newBuilder()
							    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxies.getIP(newProxy),proxies.getPort(newProxy))))
							    .proxyAuthenticator(newAuthenticator)
							    .cookieJar(jar)
							    .build();
						
						putCookiesInJar(url,responseHeaders);
					}
					default: {
						this.log("err", "Unknown Status " + statusCode);
					}
				}
			}catch(IOException e) {
				log("err", "Failed to Submit Email, Retrying");
				Thread.sleep(this.delay);
			}
		}
	}
	
	public void submitShipping() throws ParseException {
		this.log("success", "Submitting Shipping");
		final String SUBMIT_SHIPPING_LINK = "https://www." + site + ".com/api/users/carts/current/addresses/shipping?timestamp=" + System.currentTimeMillis();
		HttpUrl url= HttpUrl.parse(SUBMIT_SHIPPING_LINK);
		String str2 = "{\"shippingAddress\":{\"setAsDefaultBilling\":false,\"setAsDefaultShipping\":false,\"firstName\":\"" + this.profile.getFirstName() + "\"" + "," + "\"lastName\":\"" + this.profile.getLastName() + "\"" + "," + "\"email\":\"" + this.profile.getEmail() + "\"" + "," + "\"phone\":\"" + this.profile.getPhone() + "\"" + "," + "\"country\":{\"isocode\":\"US\",\"name\":\"United States\"},\"id\":null,\"setAsBilling\":false,\"region\":{\"countryIso\":\"US\",\"isocode\":\"US-" + this.profile.getState() + "\",\"isocodeShort\":\"" + this.profile.getState() + "\",\"name\":\"" + this.profile.getState() + "\"" + "},\"type\":\"default\",\"LoqateSearch\":\"\",\"line1\":\"" + this.profile.getAddress1() + "\"" + ",\"postalCode\":\"" + this.profile.getZipCode() + "\"" + ",\"town\":\"" + this.profile.getCity() +  "\"" + ",\"regionFPO\":null,\"shippingAddress\":true,\"recordType\":\"S\"}}";
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(str2);
		org.json.simple.JSONObject json = (org.json.simple.JSONObject)obj;
		
		@SuppressWarnings("deprecation")
		RequestBody body = RequestBody.create(MediaType.parse("application/json"),json.toString());
		Request request = new Request.Builder()
				.url(SUBMIT_SHIPPING_LINK)
				.addHeader("x-csrf-token",this.csrf)
				.addHeader("content-type", "application/json")
				.addHeader("accept", "application/json")
				.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
				.addHeader("x-fl-request-id",uuid)
				.addHeader("origin","https://www." + site + ".com/")
				.addHeader("sec-fetch-site","same-origin")
				.addHeader("sec-fetch-mode","cors")
				.addHeader("sec-fetch-dest","empty")
				.addHeader("referer", "https://www." + site + ".com/checkout")
				.addHeader("accept-encoding","deflate, br")
				.addHeader("accept-language", "en-US,en;q=0.9")
				.post(body)
				.build();
		while(true) {
			try(Response response = httpClient.newCall(request).execute()){
				int statusCode = response.code();
				Headers responseHeaders = response.headers();
				switch(statusCode) {
				case 201:{
					putCookiesInJar(url,responseHeaders);
					return;
				}
				case 403:{
					//TODO: solve datadome
					this.log("err", "DataDome Captcha [SHIPPING], Rotating Proxy");		
					final String newProxy = proxies.getProxy();
					Authenticator newAuthenticator = new Authenticator() {
						  @Override public Request authenticate(Route route, Response response) throws IOException {
						       String credential = Credentials.basic(proxies.getUsername(newProxy), proxies.getPassword(newProxy));
						       return response.request().newBuilder()
						           .header("Proxy-Authorization", credential)
						           .build();
						}
					};
					
					httpClient = new OkHttpClient().newBuilder()
						    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxies.getIP(newProxy),proxies.getPort(newProxy))))
						    .proxyAuthenticator(newAuthenticator)
						    .cookieJar(jar)
						    .build();
					
					putCookiesInJar(url,responseHeaders);
				}
				default:
					this.log("err", "Unknown Status " + statusCode);
				}
			} catch (IOException e) {
				this.log("err","Error Submitting Shipping");
			}
		}
		
				
		
	}
	
	public void submitBilling() throws ParseException {
		this.log("success", "Submitting Billing");
		final String SUBMIT_BILLING_LINK = "https://www." + site + ".com/api/users/carts/current/set-billing?timestamp=" + System.currentTimeMillis();
		HttpUrl url = HttpUrl.parse(SUBMIT_BILLING_LINK);
		String str2 = "{\"setAsDefaultBilling\":false,\"setAsDefaultShipping\":false,\"firstName\":\"" + this.profile.getFirstName() + "\"" + ",\"lastName\":\"" + this.profile.getLastName() + "\"" + ",\"email\":\"" + this.profile.getEmail() + "\"" + ",\"phone\":\"" + this.profile.getPhone() + "\"" + ",\"country\":{\"isocode\":\"US\",\"name\":\"United States\"},\"id\":null,\"setAsBilling\":false,\"region\":{\"countryIso\":\"US\",\"isocode\":\"US-" + this.profile.getState() + "\"" + ",\"isocodeShort\":\"" + this.profile.getState() + "\"" + ",\"name\":\"" + this.profile.getState() + "\"" + "},\"type\":\"default\",\"LoqateSearch\":\"\",\"line1\":\"" + this.profile.getAddress1() + "\"" + ",\"postalCode\":\"" + this.profile.getZipCode() + "\"" + ",\"town\":\"" + this.profile.getCity() + "\"" + ",\"regionFPO\":null,\"shippingAddress\":true,\"recordType\":\"S\"}";
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(str2);
		org.json.simple.JSONObject json = (org.json.simple.JSONObject)obj;
		RequestBody body = RequestBody.create(MediaType.parse("application/json"),json.toString());
		Request request = new Request.Builder()
				.url(SUBMIT_BILLING_LINK)
				.addHeader("x-csrf-token",this.csrf)
				.addHeader("content-type", "application/json")
				.addHeader("accept", "application/json")
				.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
				.addHeader("x-fl-request-id",uuid)
				.addHeader("origin","https://www." + site + ".com/")
				.addHeader("sec-fetch-site","same-origin")
				.addHeader("sec-fetch-mode","cors")
				.addHeader("sec-fetch-dest","empty")
				.addHeader("referer", "https://www." + site + ".com/checkout")
				.addHeader("accept-encoding","deflate, br")
				.addHeader("accept-language", "en-US,en;q=0.9")
				.post(body)
				.build();
		while(true) {
			try(Response response = httpClient.newCall(request).execute()){
				int statusCode = response.code();
				Headers responseHeaders = response.headers();
				switch(statusCode) {
				case 200:{
					putCookiesInJar(url,responseHeaders);
					return;
				}
				default:
					this.log("err", "Unknown Status " + statusCode);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void submitOrder() throws IOException, ParseException {
		this.log("success", "Submitting Order");
		final String SUBMIT_PAYMENT_LINK = "https://www." + site + ".com/api/v2/users/orders?timestamp=" + System.currentTimeMillis();
		//error could happen because of this
		Cookie cartID = cartCookies.get(0);
		cartGuid = cartID.value();
		String str2 = "{\"preferredLanguage\":\"en\",\"termsAndCondition\":false,\"deviceId\":\"0400JapG4txqVP4Nf94lis1ztioT9A1DShgAnrp/XmcfWoVVgr+Rt2dAZPhMS97Z4yfjSLOS3mruQCzk1eXuO7gGCUfgUZuLE2xCJiDbCfVZTGBk19tyNs7g85bfV/0/iL/b+brorKDlch/mY3ihe46K5bwut6QIoemZk+2RqB7pLlm0Mm+WlZ9H3WRaXzrV/DlB6Yf0/pTJUUwTWoMCQ0eYOQydKgnOIDKXq4HnEqNOos1c6njJgQh/4vXJiqy0MXMQOThNipDmXv9I185O+yC2f3lLEO0Tay66NZEyiLNePemJKSIdwO9O5ZtntuUkG6NTW3LNfdqVZ1d5bDzZjYXxk5E75agXQtdj5mNV6QxgbcnqT3LYd4+PRSKAsRkLowigbexrsXpZ4bEn0Zqknf78iRTZd0HgWl8Ol+aiq5tq0b/THuCTeq3ZOf3W3iQXeQAQ2/Ec1nc2SuEaTRn0G1NCZX4cq0m8QPH/dOIYgHC/3QY1fZwSVtWNySwAqo7XEZwLfKo1DqAj2pHvUSbJ+Ltz3R3+1lLYqHZEIIGzQUC4uQH55JiFdwKw+M3zpn+qWl91KULPxBs5mS+NIxqp75R9smODtFMnFQgK0/b6EJXfvVsEMUscz7kbcnG9B+SIx5AnCkrOClWCgeIEI8HoRfRY0CpxgW+KnzcbRw6NL2GAwt9rATguwW95O22s0fO33l4+TSvNeefkn5z9dBeIcrk/mRYiq8uOwmnSsj69LfM7VRYrI1FeHSbDspvXHhwykQoiuMSNUUyGAvKLg+9aginJmamvnOVDH3SzV6i8/tb5alAK/XRNo3H5dMIK6EAX6cri0ZP89aYTYsz6GTYKg/e8FVU8NguKENqer1nNtP8OECkk/64lKoqM3wDorceNFA82i2sTVjDNkvFE42ys2aVDQcSm+xRvn4mvnxQhLzE/XHOl2pKAmu1wlufgpbZuv5tFTzEBdNLiXsNC09gR/Q8fGeshaHYP8Oq6k+cVXJiV2yKAmeNHH6+w6uYodcXlMAy5kAI1xal8ExDOIgH5Jy4pdyLxJV0ryW/dwq/pkXOE1supNm2XSaZ80z7M05TiZsVJHH8UoPgdzsWGOhqW/2jvzFq44NyGTZasfYs6ZVAotR1h5Nwu1x6PILE+TZSSoO91gRgN+Z8HAFhGFzd/m6snVqi2XJwwQEvUjPJV6TCEjn+0+9d9ASD4otFWwM1TOcz7giVFA7zpIPQ4hLssfn3WSDtD3YUYq567\",\"cartId\": \"" +  cartGuid +"\"" + "," + encryptedCardDetails().substring(1,encryptedCardDetails().length()-1) + "}";
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(str2);
		org.json.simple.JSONObject json = (org.json.simple.JSONObject)obj;
		@SuppressWarnings("deprecation")
		RequestBody body = RequestBody.create(MediaType.parse("application/json"),json.toString());
		Request request = new Request.Builder()
				.url(SUBMIT_PAYMENT_LINK)
				.addHeader("content-length", "4715")
				.addHeader("x-csrf-token",this.csrf)
				.addHeader("content-type", "application/json")
				.addHeader("accept", "application/json")
				.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
				.addHeader("x-fl-request-id",uuid)
				.addHeader("origin","https://www." + site + ".com/")
				.addHeader("sec-fetch-site","same-origin")
				.addHeader("sec-fetch-mode","cors")
				.addHeader("sec-fetch-dest","empty")
				.addHeader("referer", "https://www." + site + ".com/checkout")
				.addHeader("accept-encoding","gzip, deflate, br")
				.addHeader("accept-language", "en-US,en;q=0.9")
				.post(body)
				.build();
		while(true) {
			try(Response response = httpClient.newCall(request).execute()){
				int statusCode = response.code();
				switch(statusCode) {
				case 201:{
					this.log("success", "Order Placed");
					return;
				}
				case 400:{
					this.log("err", "Payment Declined");
					sendToDiscord();
					return;
				}
				default:
					
					this.log("err", "Unknown Status " + statusCode);
				}
			}
		}
		
		
	}
	public String encryptedCardDetails() throws IOException {
		String username = System.getProperty("user.name");
		Process process = new ProcessBuilder("C:\\Users\\" + username + "\\AppData\\Roaming\\eatshoeFootsites\\adyen.exe",this.profile.getCardNumber(),this.profile.getcvv(),this.profile.getFullName(),this.profile.getCardMonth(),this.profile.getCardYear(),"10001|A237060180D24CDEF3E4E27D828BDB6A13E12C6959820770D7F2C1671DD0AEF4729670C20C6C5967C664D18955058B69549FBE8BF3609EF64832D7C033008A818700A9B0458641C5824F5FCBB9FF83D5A83EBDF079E73B81ACA9CA52FDBCAD7CD9D6A337A4511759FA21E34CD166B9BABD512DB7B2293C0FE48B97CAB3DE8F6F1A8E49C08D23A98E986B8A995A8F382220F06338622631435736FA064AEAC5BD223BAF42AF2B66F1FEA34EF3C297F09C10B364B994EA287A5602ACF153D0B4B09A604B987397684D19DBC5E6FE7E4FFE72390D28D6E21CA3391FA3CAADAD80A729FEF4823F6BE9711D4D51BF4DFCB6A3607686B34ACCE18329D415350FD0654D").start();
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = "";
		while((s = stdInput.readLine()) != null) {
			return s;
		}
		return s;
		
		
	}
	
	
	
	
	public static String getSplitSplitCookie(String cookie) {
		String[] splitSplitCookie = cookie.split("=");
		for(int i = 0; i < splitSplitCookie.length; i++) {
			return splitSplitCookie[i];
		}
		return "";
	}
	
	public static String getSplitCookie(String wholeCookie) {
		String[] splitCurrentCookie = wholeCookie.split(";");
		for(int i = 0; i < splitCurrentCookie.length; i++) {
			return splitCurrentCookie[i];
		}
		
		return "";
		
	}
	
	public void putCookiesInJar(HttpUrl url, Headers responseHeaders) {
		for(int i = 0; i < responseHeaders.size(); i++) {
			if(responseHeaders.name(i).equals("set-cookie")) {
				String daWholeCookie = responseHeaders.value(i);
				String daSplitWholeCookie = getSplitCookie(daWholeCookie);
				String daValueOfDaCookie = daSplitWholeCookie.substring(daSplitWholeCookie.lastIndexOf("=") + 1);
				String daNameOfDaCookie = getSplitSplitCookie(daSplitWholeCookie);

				
				Cookie cookie = new Cookie.Builder()
						.domain(site + ".com")
						.path("/")
						.name(daNameOfDaCookie)
						.value(daValueOfDaCookie)
						.httpOnly()
						.secure()
						.build();
				List<Cookie> atcCookies = new ArrayList<Cookie>();
				atcCookies.add(cookie);
				cartCookies.add(cookie);
				jar.saveFromResponse(url, atcCookies);
						
				
			}
		}
	}
	
	public void sendToDiscord() throws ParseException, IOException {
		String str2 = "{\"content\" : \"eatshoeAIO successfully checked out\", \"embeds\" : null, \"avatar_url\" : \"https://pbs.twimg.com/profile_images/1351739087766474753/9RFkUfka_400x400.jpg\"}";
		
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(str2);
		org.json.simple.JSONObject json = (org.json.simple.JSONObject)obj;
		@SuppressWarnings("deprecation")
		RequestBody body = RequestBody.create(MediaType.parse("application/json"),json.toString());
		Request request = new Request.Builder()
				.url(settings.getWebhook())
				.post(body)
				.build();
		try(Response response = httpClient.newCall(request).execute()){

		}
	}
}