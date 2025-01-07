package com.bcop.bcop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bcop.bcop.model.PersonalDetails;

@Repository
public interface PersonalDetailsRepo extends JpaRepository<PersonalDetails, Long>{

}
