package com.bcop.bcop;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.bcop.bcop.OTP.OTPTwilioConfig;
import com.twilio.Twilio;

@SpringBootApplication
public class BcopApplication {

	@Autowired
	private OTPTwilioConfig otpTwilioConfig;
	
	@PostConstruct
	public void setup() {
		Twilio.init(otpTwilioConfig.getAccountsid(), otpTwilioConfig.getAuthToken());
	}


	public static void main(String[] args) {
		SpringApplication.run(BcopApplication.class, args);
	}

}
