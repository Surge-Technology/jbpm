package com.bcop.bcop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bcop.bcop.model.AddressDetails;

@Repository
public interface AddressRepository extends JpaRepository<AddressDetails, Long>{

}
