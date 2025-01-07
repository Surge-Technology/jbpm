package com.bcop.bcop.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bcop.bcop.model.FileEntity;
import com.bcop.bcop.repository.FileRepository;



@Service
public class FileService {

  private final Path uploadDirectory = Paths.get("C:\\Users\\STS177\\Desktop\\FileSystem");  // Your desired directory

  @Autowired
  private FileRepository fileRepository;

 
 public FileEntity saveFile(MultipartFile file, String documentCategory, String emailId) throws IOException {
		// Ensure the upload directory exists
		if (!Files.exists(uploadDirectory)) {
			Files.createDirectories(uploadDirectory); // Create directories if they do not exist
		}

		// Get the original filename
		String filename = file.getOriginalFilename();
		if (filename == null || filename.isEmpty()) {
			throw new IOException("Invalid file name.");
		}

		// Resolve the file path
		Path filePath = uploadDirectory.resolve(filename);

		// Save the file to the file system
		try {
			file.transferTo(filePath); // Transfer the file to the filesystem
		} catch (IOException e) {
			throw new IOException("Error saving the file: " + e.getMessage());
		}

		// Extract MIME type
		String fileType = file.getContentType(); // Get MIME type from the file
		if (fileType == null) {
			// Fallback: If MIME type is not available, use the file extension
			String fileExtension = getFileExtension(filename);
			fileType = "application/octet-stream"; // Default MIME type for binary files
			if (fileExtension != null) {
				fileType = "application/" + fileExtension;
			}
		}
		System.out.println(emailId);
		// Create a FileEntity to store metadata
		FileEntity fileEntity = new FileEntity();
		fileEntity.setFileName(filename);
		fileEntity.setFilepath(filePath.toString());
		fileEntity.setFileType(fileType);
		fileEntity.setDocumentCategory(documentCategory);
		fileEntity.setEmailId(emailId);

		// Save the metadata to the database
		return fileRepository.save(fileEntity);
	}

	// Helper method to extract file extension
	private String getFileExtension(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			return filename.substring(dotIndex + 1).toLowerCase();
		}
		return null; // No extension found
	}

	// Retrieve the file from the database by ID
	public Optional<FileEntity> getFile(Long fileId) {
		return fileRepository.findById(fileId);
	}

	public boolean deleteFileByCategory(String documentCategory, String emailId) throws IOException {
		// Find the file by document category
		Optional<FileEntity> fileEntityOptional = fileRepository.findByDocumentCategoryAndEmailId(documentCategory,
				emailId);

		if (fileEntityOptional.isPresent()) {
			FileEntity fileEntity = fileEntityOptional.get();

			// Delete the file from the filesystem
			Path filePath = Paths.get(fileEntity.getFilepath());
			try {
				Files.deleteIfExists(filePath); // Delete the file from the disk
			} catch (IOException e) {
				throw new IOException("Error deleting file from the filesystem: " + e.getMessage());
			}

			// Delete the file metadata from the database
			fileRepository.delete(fileEntity);
			return true;
		}

		// If no file found with the given document category, return false
		return false;
	}

	public List<FileEntity> getFilesByEmail(String emailId) {
		return fileRepository.findByEmailId(emailId);
	}

	public boolean deleteFilesByCategoryAndEmail(String emailId, String documentCategory) {
		List<FileEntity> filesToDelete = fileRepository.findAllByEmailIdAndDocumentCategory(emailId, documentCategory);

		if (filesToDelete.isEmpty()) {
			return false; // No files found to delete
		}

		for (FileEntity fileEntity : filesToDelete) {
			try {
				// Delete the file from the filesystem
				Files.deleteIfExists(Paths.get(fileEntity.getFilepath())); // Delete file from disk
				// Delete the file record from the database
				fileRepository.delete(fileEntity);
			} catch (IOException e) {
				// Handle file deletion failure
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public Resource downloadFileById(Long fileId) throws Exception {
		Optional<FileEntity> fileEntityOpt = fileRepository.findById(fileId);
//		if (fileEntityOpt.isEmpty()) {
//			throw new Exception("File not found with id: " + fileId);
//		}
		FileEntity fileEntity = fileEntityOpt.get();
		String filepath = fileEntity.getFilepath();
		Resource resource = new UrlResource(Paths.get(filepath).toUri());
		if (resource.exists() || resource.isReadable()) {
			return resource;
		} else {
			throw new Exception("File not found or is unreadable with id: " + fileId);
		}
	}

	@Transactional
	public boolean deleteFileById(Long fileId) {
		Optional<FileEntity> fileEntityOpt = fileRepository.findById(fileId);
		FileEntity fileEntity = fileEntityOpt.get();
		if (fileEntity.getFilepath() != null && !fileEntity.getFilepath().isEmpty()) {
			try {
				File file = new File(fileEntity.getFilepath());
				if (file.exists()) {
					boolean deleted = file.delete();
					if (!deleted) {
						return false;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		fileRepository.deleteById(fileId);
		return true;
	}
}
