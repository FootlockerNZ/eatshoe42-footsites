package com.footsites;

import java.util.HashMap;

public class Profile {

	private String profileName;
	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;
	private String address1;
	private String address2;
	private String zipCode;
	private String city;
	private String state;
	private String card_number;
	private String card_month;
	private String card_year;
	private String cvv;
	

	
	
	public Profile(String profileName, String firstName, String lastName, String email, String phoneNumber, String address1, String address2, String zipCode, String city, String state, String card_number, String card_month, String card_year, String cvv ) {
		this.profileName = profileName;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.address1 = address1;
		this.address2 = address2;
		this.zipCode = zipCode;
		this.city = city;
		this.state = state;
		this.card_number = card_number;
		this.card_month = card_month;
		this.card_year = card_year;
		this.cvv = cvv;
	}
	
	public String getProfileName() {
		return this.profileName;
	}
	
	public String getCardNumber() {
		return this.card_number;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public String getcvv() {
		return this.cvv;
	}
	
	public String getFullName() {
		return this.firstName + " " + this.lastName;
	}
	
	public String getCardMonth() {
		return this.card_month;
	}
	
	public String getCardYear() {
		return this.card_year;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	
	public String getPhone() {
		return this.phoneNumber;
	}
	
	public String getAddress1() {
		return this.address1;
	}
	
	public String getZipCode() {
		return this.zipCode;
	}
	
	public String getCity() {
		return this.city;
	}
	
	public String getState() {
		return this.state;
	}

}