package com.bcop.bcop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bcop.bcop.model.Registeration;

@Repository
public interface BcopRepository extends JpaRepository<Registeration, Long>{

	Optional<Registeration> findById(Registeration registration);
	Optional<Registeration> findById(Long id);

}
