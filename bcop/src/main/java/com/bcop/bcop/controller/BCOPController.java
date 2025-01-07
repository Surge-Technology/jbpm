package com.bcop.bcop.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bcop.bcop.model.AddressDetails;
import com.bcop.bcop.model.ApproverDetails;
import com.bcop.bcop.model.FileEntity;
import com.bcop.bcop.model.PersonalDetails;
import com.bcop.bcop.model.Registeration;
import com.bcop.bcop.repository.AddressRepository;
import com.bcop.bcop.repository.BcopRepository;
import com.bcop.bcop.service.BCOPService;
import com.bcop.bcop.service.EmailService;
import com.bcop.bcop.service.FileService;
import com.bcop.bcop.service.VerifyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class BCOPController {

	String serverUrl = "http://localhost:8080/kie-server/services/rest/";

	private static final String USERNAME = "krisv";
	private static final String PASSWORD = "krisv";

	String containerId = "testingSpace_1.0.1-SNAPSHOT";
	String processId = "LoanApplication.passInput";
	private String processInstanceId;
	private Long customerId;
	private String phoneNumber;
	private String emailId;
	private String taskInstanceId;

	@Autowired
	BcopRepository bcopRepository;

	@Autowired
	BCOPService bcopService;

	@Autowired
	private VerifyService verifyService;
	
	@Autowired
	AddressRepository addressRepository;
	
	@Autowired
	FileService fileService;
	@Autowired
	private EmailService emailService;

	RestTemplate restTemplate = new RestTemplate();

	@CrossOrigin
	@PostMapping("/CustomerRegisteration")
	public Map startProcessInstance(@RequestBody Registeration registeration) {
		Registeration savedUser = bcopRepository.save(registeration);
		System.out.println("User saved: " + savedUser.getUserName());

		System.out.println("request Body data"+savedUser.getEmailId());
		String url = String.format("%s/server/containers/%s/processes/%s/instances", serverUrl, containerId, processId);

		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		phoneNumber = savedUser.getPhoneNumber();
		emailId = savedUser.getEmailId();
		String otpResponse = verifyService.sendOtp(phoneNumber);
		System.out.println("OTP Response: " + otpResponse);
		Map<String, Object> variables = new HashMap<>();
	    variables.put("emailId", savedUser.getEmailId()); // Add email as a process variable
	    variables.put("userName", savedUser.getUserName()); 

	    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(variables, headers);
	    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

//		HttpEntity<String> entity = new HttpEntity<>("{}", headers);
//		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		processInstanceId = response.getBody();
		System.out.println("Started Process Instance ID: " + processInstanceId);

		customerId = savedUser.getId();

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("customerId", customerId);

		return result;
	}

	@CrossOrigin
	@GetMapping("/Inbox")
	public ResponseEntity<Map<String, String>> getTasksForProcessInstance()
			throws JsonMappingException, JsonProcessingException {
		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);

		System.out.println("Process Instance ID: " + processInstanceId);
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.getBody());

		Map<String, String> taskDetailsMap = new HashMap<>();
		JsonNode taskSummaryNode = rootNode.path("task-summary");

		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
			JsonNode firstTask = taskSummaryNode.get(0);

			String taskId = String.valueOf(firstTask.path("task-id").asInt());
			taskInstanceId = taskId;
			String taskName = firstTask.path("task-name").asText();
			String assignedTo = firstTask.path("task-actual-owner").asText();
			String taskStatus = firstTask.path("task-status").asText();

			taskDetailsMap.put("id", taskId);
			taskDetailsMap.put("name", taskName);
			taskDetailsMap.put("assignee", assignedTo);
			taskDetailsMap.put("Status", taskStatus);

			System.out.println("Extracted Task Details: " + taskDetailsMap);
			System.out.println(taskInstanceId);
		} else {
			System.out.println("No tasks found for the process instance.");
			taskDetailsMap.put("Message", "No tasks found for the given process instance.");
		}

		return ResponseEntity.ok(taskDetailsMap);
	}

	@CrossOrigin
	@PostMapping("/address/{registrationId}")
	public String addAddress(@PathVariable Long registrationId, @RequestBody AddressDetails addressDetails)
			throws JsonMappingException, JsonProcessingException {
		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);

		System.out.println("Process Instance ID: " + processInstanceId);
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.getBody());

		Map<String, String> taskDetailsMap = new HashMap<>();
		JsonNode taskSummaryNode = rootNode.path("task-summary");

		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
			JsonNode firstTask = taskSummaryNode.get(0);

			String taskId = String.valueOf(firstTask.path("task-id").asInt());
			taskInstanceId = taskId;
			taskDetailsMap.put("Task ID", taskId);
			System.out.println("Extracted Task Details: " + taskDetailsMap);
			System.out.println(taskInstanceId);
		}
		bcopService.addAddressToRegistration(registrationId, addressDetails);
		String starturl = String.format("%s/server/containers/%s/tasks/%s/states/started", serverUrl, containerId,
				taskInstanceId);

		System.out.println(taskInstanceId);
		System.out.println("task reserved");

		ResponseEntity<String> responseStart = restTemplate.exchange(starturl, HttpMethod.PUT, entity, String.class);
		System.out.println("task inprogress");
		String completeurl = String.format("%s/server/containers/%s/tasks/%s/states/completed", serverUrl, containerId,
				taskInstanceId);
		ResponseEntity<String> responseComplete = restTemplate.exchange(completeurl, HttpMethod.PUT, entity,
				String.class);
		System.out.println("task completed");

		return "Address added successfully to registration ID: " + registrationId;
	}

	@GetMapping("/getAddressDetails/{registrationId}")
	public ResponseEntity<?> getRegistrationDetails(@PathVariable Long registrationId) {
		try {
			Registeration registeration = bcopService.getRegistrationWithAddresses(registrationId);
			return ResponseEntity.ok(registeration);
		} catch (RuntimeException e) {
			return ResponseEntity.status(404).body(e.getMessage());
		}
	}

	@CrossOrigin
	@PostMapping("/personalDetails/{registrationId}")
	public String addPersonal(@PathVariable Long registrationId, @RequestBody PersonalDetails personalDetails)
			throws JsonMappingException, JsonProcessingException {
		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);

		System.out.println("Process Instance ID: " + processInstanceId);
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.getBody());

		Map<String, String> taskDetailsMap = new HashMap<>();
		JsonNode taskSummaryNode = rootNode.path("task-summary");

		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
			JsonNode firstTask = taskSummaryNode.get(0);

			String taskId = String.valueOf(firstTask.path("task-id").asInt());
			taskInstanceId = taskId;
			taskDetailsMap.put("Task ID", taskId);
			System.out.println("Extracted Task Details: " + taskDetailsMap);
			System.out.println(taskInstanceId);
		}

		bcopService.addpersonalDetailsToRegistration(registrationId, personalDetails);

		// Start the task
		String starturl = String.format("%s/server/containers/%s/tasks/%s/states/started", serverUrl, containerId,
				taskInstanceId);
		System.out.println(taskInstanceId);
		System.out.println("task reserved");

		ResponseEntity<String> responseStart = restTemplate.exchange(starturl, HttpMethod.PUT, entity, String.class);
		System.out.println("task inprogress");

		// Set the variable
		String variableUrl = String.format("%s/server/containers/%s/processes/instances/%s/variables", serverUrl,
				containerId, processInstanceId);
		Map<String, Object> variableData = new HashMap<>();
		System.out.println(personalDetails.getAge());
		variableData.put("age", personalDetails.getAge());
		variableData.put("annualIncome", personalDetails.getAnnualIncome());
		variableData.put("name", personalDetails.getFirstName());

		HttpEntity<Map<String, Object>> variableEntity = new HttpEntity<>(variableData, headers);
		ResponseEntity<String> variableResponse = restTemplate.exchange(variableUrl, HttpMethod.POST, variableEntity,
				String.class);

		// Complete the task
		String completeurl = String.format("%s/server/containers/%s/tasks/%s/states/completed", serverUrl, containerId,
				taskInstanceId);
		ResponseEntity<String> responseComplete = restTemplate.exchange(completeurl, HttpMethod.PUT, entity,
				String.class);
		System.out.println("task completed");

		return "Personal added successfully to registration ID: " + registrationId;
	}
//	@PostMapping("/personalDetails/{registrationId}")
//	public ResponseEntity<Object> addPersonal(@PathVariable Long registrationId,
//			@RequestBody PersonalDetails personalDetails) throws JsonMappingException, JsonProcessingException {
//		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);
//
//		System.out.println("Process Instance ID: " + processInstanceId);
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("accept", "application/json");
//		headers.set("content-type", "application/json");
//		headers.setBasicAuth(USERNAME, PASSWORD);
//		HttpEntity<String> entity = new HttpEntity<>(headers);
//
//		// Retrieve Task Instance ID from Process
//		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//		ObjectMapper objectMapper = new ObjectMapper();
//		JsonNode rootNode = objectMapper.readTree(response.getBody());
//
//		Map<String, String> taskDetailsMap = new HashMap<>();
//		JsonNode taskSummaryNode = rootNode.path("task-summary");
//
//		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
//			JsonNode firstTask = taskSummaryNode.get(0);
//
//			String taskId = String.valueOf(firstTask.path("task-id").asInt());
//			taskInstanceId = taskId;
//			taskDetailsMap.put("Task ID", taskId);
//			System.out.println("Extracted Task Details: " + taskDetailsMap);
//			System.out.println(taskInstanceId);
//		}
//
//		// Add personal details to the registration
//		bcopService.addpersonalDetailsToRegistration(registrationId, personalDetails);
//
//		// Start the task
//		String starturl = String.format("%s/server/containers/%s/tasks/%s/states/started", serverUrl, containerId,
//				taskInstanceId);
//		System.out.println(taskInstanceId);
//		System.out.println("task reserved");
//
//		ResponseEntity<String> responseStart = restTemplate.exchange(starturl, HttpMethod.PUT, entity, String.class);
//		System.out.println("task inprogress");
//
//		// Complete the task
//		  String completeurl = String.format("%s/server/containers/%s/tasks/%s/states/completed", serverUrl, containerId,
//					taskInstanceId);
//			ResponseEntity<String> responseComplete = restTemplate.exchange(completeurl, HttpMethod.PUT, entity,
//					String.class);
//			System.out.println("task completed");
//		
//		 Map<String, Object> details = new HashMap<>();
//		    details.put("age", personalDetails.getAge());
//		    details.put("annualIncome", personalDetails.getAnnualIncome());
//		    
//		    updateProcessVariables(serverUrl, containerId, processInstanceId, details);
//
//		  	
//		    // Step 6: Set the map variable in the process instance
//		    return setProcessVariable(processInstanceId, "personalDetails", details);
//	
//		//	     return "Personal added successfully to registration ID: " + registrationId;
//	}

	@PostMapping("/credit-approval")
	public ResponseEntity<Integer> approveCredit(@RequestBody PersonalDetails creditDetails) {
		System.out.printf("credit approval - Age: %d, Annual Income: %.2f%n", creditDetails.getAge(),
				creditDetails.getAnnualIncome());
		System.out.printf("Received Personal Details - Age: %d, Annual Income: %.2f%n", creditDetails.getAge(),
				creditDetails.getAnnualIncome());

//		System.out.printf("credit approval - Age: %d, Annual Income: %.2f%n", age, annualIncome);

		// Calculate CIBIL score based on age and income
//		int cibilScore = calculateCibilScore(age, annualIncome);
		int cibilScore = calculateCibilScore(creditDetails.getAge(), creditDetails.getAnnualIncome());

		// Determine credit approval status based on the calculated score
		String approvalStatus = determineApproval(cibilScore);
		System.out.println("cibil Score" + cibilScore);
		PersonalDetails details = new PersonalDetails();
		details.setCreditScore(cibilScore);
		System.out.println(cibilScore);
		// Return just the CIBIL score as an integer
		return ResponseEntity.ok(cibilScore);
	}

	// Logic to calculate CIBIL score
//		private int calculateCibilScore(int age, double annualIncome) {
//		    if (age < 18 || annualIncome < 300000) {
//		        return 350;  // Low score
//		    } else if (age >= 18 && age <= 25 && annualIncome >= 300000) {
//		        return 500;  // Medium score
//		    } else if (age > 25 && annualIncome >= 500000) {
//		        return 700;  // High score
//		    } else {
//		        return 450;  // Default score
//		    }
//		}
	public int calculateCibilScore(int age, double annualIncome) {
		int baseScore = 300; // Base score is constant
		int ageFactor = 10; // Age factor to multiply with age
		double incomeFactor = 0.001; // This can be used for more refined income scaling (optional)

		// Calculate score based on age
		int ageScore = age * ageFactor;

		// Calculate score based on income (you can adjust the factor if needed)
		int incomeScore = (int) (annualIncome * incomeFactor); // Convert incomeScore to an integer directly

		// Final credit score calculation
		int creditScore = baseScore + ageScore + incomeScore;

		// Returning the final score
		return creditScore;
	}

	// Logic to determine approval based on CIBIL score
	private String determineApproval(int cibilScore) {
		if (cibilScore >= 700) {
			return "Approved"; // High score
		} else if (cibilScore <= 700 && cibilScore >= 400) {
			return "Pending"; // Medium score
		} else {
			return "Denied"; // Low score
		}
	}

	@CrossOrigin
	@GetMapping("/getPersonalDetails/{registrationId}")
	public ResponseEntity<?> getRegistrationPersonalDetails(@PathVariable Long registrationId) {
		try {
			Registeration registeration = bcopService.getRegistrationWithPersonal(registrationId);
			return ResponseEntity.ok(registeration);
		} catch (RuntimeException e) {
			return ResponseEntity.status(404).body(e.getMessage());
		}
	}

	@CrossOrigin
	@PostMapping("/completetask")
	public String completetask() throws JsonMappingException, JsonProcessingException {
		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);
		Registeration registeration =new Registeration();
		//System.out.print(registeration.getEmailId());
		System.out.println("Process Instance ID: " + processInstanceId);
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		ObjectMapper objectMapper = new ObjectMapper();
//		JsonNode rootNode = objectMapper.readTree(response.getBody());
		 JsonNode rootNode = objectMapper.readTree(response.getBody());
		   
		Map<String, String> taskDetailsMap = new HashMap<>();
		JsonNode taskSummaryNode = rootNode.path("task-summary");

		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
			JsonNode firstTask = taskSummaryNode.get(0);

			String taskId = String.valueOf(firstTask.path("task-id").asInt());
			taskInstanceId = taskId;
			taskDetailsMap.put("Task ID", taskId);
			System.out.println("Extracted Task Details: " + taskDetailsMap);
			System.out.println(taskInstanceId);
		}

		String starturl = String.format("%s/server/containers/%s/tasks/%s/states/started", serverUrl, containerId,
				taskInstanceId);

		System.out.println(taskInstanceId);
		System.out.println("task reserved");

		ResponseEntity<String> responseStart = restTemplate.exchange(starturl, HttpMethod.PUT, entity, String.class);
		System.out.println("task inprogress");
		String completeurl = String.format("%s/server/containers/%s/tasks/%s/states/completed", serverUrl, containerId,
				taskInstanceId);
		ResponseEntity<String> responseComplete = restTemplate.exchange(completeurl, HttpMethod.PUT, entity,
				String.class);
		System.out.println("task completed");
	
		return "completed";
	}
	
	@CrossOrigin
	@PostMapping("/finalDecision/{registrationId}")
	public String makeDecision(@PathVariable Long registrationId, @RequestBody ApproverDetails formData)
			throws JsonMappingException, JsonProcessingException {
		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);

		System.out.println("Process Instance ID: " + processInstanceId);
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.getBody());

		Map<String, String> taskDetailsMap = new HashMap<>();
		JsonNode taskSummaryNode = rootNode.path("task-summary");

		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
			JsonNode firstTask = taskSummaryNode.get(0);

			String taskId = String.valueOf(firstTask.path("task-id").asInt());
			taskInstanceId = taskId;
			taskDetailsMap.put("Task ID", taskId);
			System.out.println("Extracted Task Details: " + taskDetailsMap);
			System.out.println(taskInstanceId);
		}
//		bcopService.addAddressToRegistration(registrationId, addressDetails);
		String starturl = String.format("%s/server/containers/%s/tasks/%s/states/started", serverUrl, containerId,
				taskInstanceId);

		System.out.println(taskInstanceId);
		System.out.println("task reserved");

		ResponseEntity<String> responseStart = restTemplate.exchange(starturl, HttpMethod.PUT, entity, String.class);
		System.out.println("task inprogress");

		// Set the variable
		String variableUrl = String.format("%s/server/containers/%s/processes/instances/%s/variables", serverUrl,
				containerId, processInstanceId);
		Map<String, Object> variableData = new HashMap<>();
		System.out.println(formData.getFinalDecision());
		variableData.put("finalDecision", formData.getFinalDecision());
	variableData.put("query", formData.getQuery());
	

		HttpEntity<Map<String, Object>> variableEntity = new HttpEntity<>(variableData, headers);
		ResponseEntity<String> variableResponse = restTemplate.exchange(variableUrl, HttpMethod.POST, variableEntity,
				String.class);

		String completeurl = String.format("%s/server/containers/%s/tasks/%s/states/completed", serverUrl, containerId,
				taskInstanceId);
		ResponseEntity<String> responseComplete = restTemplate.exchange(completeurl, HttpMethod.PUT, entity,
				String.class);
		System.out.println(emailId);
		String to = emailId;
		String subject = "Customer Registered Successfully";
		String body = "Hello, Here is your link: <a href='http://localhost:3000/queryReply'>Click here</a>";

		 emailService.sendEmail(to, subject, body);
 System.out.println("mail send");
		System.out.println("task completed");

		return "query returned "+registrationId ;
	}
	@CrossOrigin
	@GetMapping("/getQuerys")
	public String customerReply( @RequestBody ApproverDetails formData)
			throws JsonMappingException, JsonProcessingException {
		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);

		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("content-type", "application/json");
		headers.setBasicAuth(USERNAME, PASSWORD);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.getBody());

		Map<String, String> taskDetailsMap = new HashMap<>();
		JsonNode taskSummaryNode = rootNode.path("task-summary");

		if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
			JsonNode firstTask = taskSummaryNode.get(0);

			String taskId = String.valueOf(firstTask.path("task-id").asInt());
			taskInstanceId = taskId;
			taskDetailsMap.put("Task ID", taskId);
			System.out.println("Extracted Task Details: " + taskDetailsMap);
			System.out.println(taskInstanceId);
		}

		return "query returned " ;
	}
//	@GetMapping("/getQuery")
//	public Map<String, String> customerReplys(@RequestBody ApproverDetails formData)
//	        throws JsonMappingException, JsonProcessingException {
//	    // Construct the API URL
//		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);
//
//	    // Set up HTTP headers
//	    HttpHeaders headers = new HttpHeaders();
//	    headers.set("accept", "application/json");
//	    headers.set("content-type", "application/json");
//	    headers.setBasicAuth(USERNAME, PASSWORD);
//
//	    // Create the request entity
//	    HttpEntity<String> entity = new HttpEntity<>(headers);
//
//	    // Execute the REST API call
//	    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//
//	    // Parse the response
//	    ObjectMapper objectMapper = new ObjectMapper();
//	    JsonNode rootNode = objectMapper.readTree(response.getBody());
//
//	    Map<String, String> taskDetailsMap = new HashMap<>();
//	    JsonNode taskSummaryNode = rootNode.path("task-summary");
//
//	    // Process task-summary and extract required details
//	    if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
//	        JsonNode firstTask = taskSummaryNode.get(0);
//
//	        // Extract task-id
//	        String taskId = firstTask.path("task-id").asText();
//	        taskDetailsMap.put("Task ID", taskId);
//
//	        // Extract username (assume it's in task-owner or another key)
//	        String username = firstTask.path("task-owner").asText();
//	        taskDetailsMap.put("Username", username);
//
//	        // Extract query (if applicable)
//	        String query = firstTask.path("task-name").asText(); // Adjust key if necessary
//	        taskDetailsMap.put("Query", query);
//
//	        // Log for debugging
//	        System.out.println("Extracted Task Details: " + taskDetailsMap);
//	    } else {
//	        taskDetailsMap.put("Error", "No tasks found for the process instance.");
//	        System.out.println("No tasks found for the process instance.");
//	    }
//
//	    return taskDetailsMap;
//	}
@CrossOrigin
	  @GetMapping("/variables")
	    public ResponseEntity<?> getProcessVariables() {
	      
	            String url = String.format(
	                    "%s/server/containers/%s/processes/instances/%s/variables", 
	                    serverUrl, containerId, processInstanceId);
	            
	            
	            
	           // /server/containers/{containerId}/processes/instances/{processInstanceId}/variables
	            // Set up headers if needed (e.g., for authentication)
	            HttpHeaders headers = new HttpHeaders();
	            headers.set("accept", "application/json");
	            headers.set("content-type", "application/json");
	            headers.setBasicAuth(USERNAME, PASSWORD);

	            HttpEntity<String> entity = new HttpEntity<>(headers);

	            // Make the GET request
	            ResponseEntity<String> response = restTemplate.exchange(
	                    url, HttpMethod.GET, entity, String.class);
	            
	            try {
	                // Parse the response to extract `userName` and `query`
	                ObjectMapper objectMapper = new ObjectMapper();
	                JsonNode rootNode = objectMapper.readTree(response.getBody());

	                // Extract values for `userName` and `query`
	                String userName = rootNode.path("userName").asText(); // Default to "" if not found
	                String query = rootNode.path("query").asText(); // Default to "" if not found

	                // Print extracted values (optional)
	                System.out.println("UserName: " + userName);
	                System.out.println("Query: " + query);

	                // Return the extracted values as a response (or map as needed)
	                Map<String, String> result = new HashMap<>();
	                result.put("userName", userName);
	                result.put("query", query);

	                return ResponseEntity.ok(result);
	            } catch (JsonProcessingException e) {
	                // Handle parsing error
	                e.printStackTrace();
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing response");
	            }

	    }
@CrossOrigin
@GetMapping("/emailId")
public ResponseEntity<?> getEmailId() {
    String url = String.format(
            "%s/server/containers/%s/processes/instances/%s/variables", 
            serverUrl, containerId, processInstanceId);
    
    // Set up headers for authentication and content type
    HttpHeaders headers = new HttpHeaders();
    headers.set("accept", "application/json");
    headers.set("content-type", "application/json");
    headers.setBasicAuth(USERNAME, PASSWORD);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
        // Make the GET request
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        // Parse the response JSON to extract the emailId
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.getBody());

        // Extract the `emailId` variable
        String emailId = rootNode.path("emailId").asText(); // Default to "" if not found

        if (emailId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email ID not found");
        }

        // Return the emailId as a response
        return ResponseEntity.ok(Collections.singletonMap("emailId", emailId));
    } catch (JsonProcessingException e) {
        // Handle JSON parsing errors
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing response");
    } catch (Exception ex) {
        // Handle other errors (e.g., HTTP errors)
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching emailId");
    }
}
@CrossOrigin
@PostMapping("/upload")
public ResponseEntity<List<FileEntity>> uploadFiles(@RequestParam("file") List<MultipartFile> file,
		@RequestParam("documentCategory") String documentCategory, @RequestParam("emailId") String emailId)
		throws IOException {
	List<FileEntity> savedFiles = new ArrayList<>();
	for (MultipartFile file1 : file) {
		FileEntity savedFile = fileService.saveFile(file1, documentCategory, emailId);
		savedFiles.add(savedFile);
	}
	return ResponseEntity.ok(savedFiles);
}
@CrossOrigin
	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteFile(@RequestParam("documentCategory") String documentCategory,
			@RequestParam("emailId") String emailId) throws IOException {
		boolean isDeleted = fileService.deleteFileByCategory(documentCategory, emailId);
		if (isDeleted) {
			return ResponseEntity.ok("File corresponding to the category deleted successfully.");
		} else {
			return ResponseEntity.status(404).body("No file found to delete for the provided category.");
		}
	}
@CrossOrigin
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> deleteFile(@PathVariable Long id) {
		boolean isDeleted = fileService.deleteFileById(id);

		if (isDeleted) {
			return ResponseEntity.ok("File with ID " + id + " deleted successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File with ID " + id + " not found.");
		}
	}
@CrossOrigin
	@DeleteMapping("/deleteMultiple")
	public ResponseEntity<String> deleteMultipleFiles(@RequestParam("documentCategory") String documentCategory,
			@RequestParam("emailId") String emailId) {

		boolean isDeleted = fileService.deleteFilesByCategoryAndEmail(emailId, documentCategory);

		if (isDeleted) {
			return ResponseEntity.ok("All files corresponding to the category and emailId deleted successfully.");
		} else {
			return ResponseEntity.status(404).body("No files found to delete for the provided category and emailId.");
		}
	}
@CrossOrigin
@GetMapping("/downloadEmail")

public ResponseEntity<Resource> downloadFilesByEmail(@RequestParam("emailId") String emailId) throws IOException {
	List<FileEntity> fileEntities = fileService.getFilesByEmail(emailId);
	if (fileEntities.isEmpty()) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	}
	Path tempDir = Paths.get("C:\\Users\\STS177\\Desktop\\FileSystem/");
	if (!Files.exists(tempDir)) {
		Files.createDirectories(tempDir);
	}
	Path tempZip = Files.createTempFile(tempDir, "files-", ".zip");
	if (Files.exists(tempZip)) {
		Files.delete(tempZip);
	}
	URI zipUri = URI.create("jar:file:" + tempZip.toUri().getPath());
	try (FileSystem zipFs = FileSystems.newFileSystem(zipUri,Collections.singletonMap("create", "true"))) {
		for (FileEntity fileEntity : fileEntities) {
			Path sourcePath = Paths.get(fileEntity.getFilepath());
			if (Files.exists(sourcePath) && Files.isReadable(sourcePath)) {
				Path destinationPath = zipFs.getPath("/" + fileEntity.getFileName());
				Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	} catch (IOException e) {
		Files.deleteIfExists(tempZip);
		throw e;
	}
	Resource resource = new UrlResource(tempZip.toUri());
	return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"files.zip\"").body(resource);
}
@CrossOrigin
@GetMapping("/fileMetadata")
public ResponseEntity<List<Map<String, Object>>> getFileMetadataByEmail(@RequestParam("emailId") String emailId) {
    List<FileEntity> fileEntities = fileService.getFilesByEmail(emailId);
    if (fileEntities.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
    
    List<Map<String, Object>> fileDetails = new ArrayList<>();
    for (FileEntity fileEntity : fileEntities) {
        Map<String, Object> fileDetail = new HashMap<>();
        fileDetail.put("fileId", fileEntity.getId());
        fileDetail.put("fileName", fileEntity.getFileName());
        fileDetail.put("documentCategory", fileEntity.getDocumentCategory());
        fileDetails.add(fileDetail);
    }
    return ResponseEntity.ok(fileDetails);
}
@CrossOrigin
	@GetMapping("/download/{id}")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
		try {
			Resource resource = fileService.downloadFileById(id);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
	}
}
