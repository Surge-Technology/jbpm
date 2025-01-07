package com.bcop.bcop.service;

import java.util.ArrayList;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bcop.bcop.model.AddressDetails;
import com.bcop.bcop.model.PersonalDetails;
import com.bcop.bcop.model.Registeration;
import com.bcop.bcop.repository.BcopRepository;


@Service
public class BCOPService {
	
	@Autowired
	BcopRepository bcopRepository;


	    @Transactional
	    public void addAddressToRegistration(Long registrationId, AddressDetails newAddress) {
	     
	        Optional<Registeration> optionalRegistration = bcopRepository.findById(registrationId);

	        if (optionalRegistration.isPresent()) {
	            Registeration registration = optionalRegistration.get();

	            newAddress.setRegistration(registration);

	            if (registration.getAddressDetails() == null) {
	                registration.setAddressDetails(new ArrayList<>());
	            }
	            registration.getAddressDetails().add(newAddress);
	            bcopRepository.save(registration);
	        } else {
	            throw new RuntimeException("Registration with ID " + registrationId + " not found");
	        }
	    }
	    public Registeration getRegistrationWithAddresses(Long registrationId) {
	        return bcopRepository.findById(registrationId)
	                .orElseThrow(() -> new RuntimeException("Registration ID not found: " + registrationId));
	    }
	    @Transactional
	    public void addpersonalDetailsToRegistration(Long registrationId, PersonalDetails personalDetails) {
	     
	        Optional<Registeration> optionalRegistration = bcopRepository.findById(registrationId);

	        if (optionalRegistration.isPresent()) {
	            Registeration registration = optionalRegistration.get();

	            personalDetails.setRegistration(registration);

	            if (registration.getAddressDetails() == null) {
	                registration.setAddressDetails(new ArrayList<>());
	            }
	            registration.getPersonalDetails().add(personalDetails);
	            bcopRepository.save(registration);
	        } else {
	            throw new RuntimeException("Registration with ID " + registrationId + " not found");
	        }
	    }
		public Registeration getRegistrationWithPersonal(Long registrationId) {
			 return bcopRepository.findById(registrationId)
		                .orElseThrow(() -> new RuntimeException("Registration ID not found: " + registrationId));
		    }
		
}
