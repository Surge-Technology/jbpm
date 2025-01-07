package com.bcop.bcop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bcop.bcop.service.VerifyService;

@RestController
public class VerifyController {


    @Autowired
    private VerifyService verifyService;

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(@RequestParam String phoneNumber) {
        try {
            String response = verifyService.sendOtp(phoneNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending OTP: " + e.getMessage());
        }
    }
    @CrossOrigin
    @PostMapping("/verify")
    public ResponseEntity<String> verifyOtp(@RequestParam String otp) {
    	System.out.println("otp------>"+otp);
        try {
            String response = verifyService.verifyOtp(otp);
            System.out.println("response"+response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error verifying OTP: " + e.getMessage());
        }
    }
}

