package com.bcop.bcop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bcop.bcop.model.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
	List<FileEntity> findByEmailId(String emailId);
	Optional<FileEntity> findByDocumentCategoryAndEmailId(String documentCategory, String emailId);
	  List<FileEntity> findAllByEmailIdAndDocumentCategory(String emailId, String documentCategory);
	    void deleteAllByEmailIdAndDocumentCategory(String emailId, String documentCategory);

}
