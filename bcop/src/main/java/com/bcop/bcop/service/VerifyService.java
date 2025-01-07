package com.bcop.bcop.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bcop.bcop.OTP.OTPTwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class VerifyService {

    @Autowired
    private OTPTwilioConfig otpTwilioConfig;


    Set<String> otpSet = new HashSet<>();

    public String sendOtp(String phoneNumber) {
        PhoneNumber recipientPhoneNumber = new PhoneNumber(phoneNumber);
        PhoneNumber senderPhoneNumber = new PhoneNumber(otpTwilioConfig.getPhoneNumber());

        String otp = generateOtp();
        String messageBody = "Your OTP is " + otp;

        Message message = Message.creator(recipientPhoneNumber, senderPhoneNumber, messageBody).create();

        otpSet.add(otp);

        return "OTP sent successfully";
    }

    public String verifyOtp(String userOtp) {
        if (otpSet.contains(userOtp)) {
            otpSet.remove(userOtp);
            return "OTP verified successfully";
        } else {
            return "Invalid OTP. Please try again.";
        }
    }

    private String generateOtp() {
        int otp = (int) (Math.random() * 1000000);
        return String.format("%06d", otp);
    }
}
