package com.bcop.bcop.OTP;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="twilio")
public class OTPTwilioConfig{

	private String accountsid;
	private String authToken;
	private String phoneNumber;
	
	public String getAccountsid() {
		return accountsid;
	}
	public void setAccountsid(String accountsid) {
		this.accountsid = accountsid;
	}
	public String getAuthToken() {
		return authToken;
	}
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public OTPTwilioConfig(String accountsid, String authToken, String phoneNumber) {
		super();
		this.accountsid = accountsid;
		this.authToken = authToken;
		this.phoneNumber = phoneNumber;
	}
	public OTPTwilioConfig() {
		super();
	}
	
	
}
