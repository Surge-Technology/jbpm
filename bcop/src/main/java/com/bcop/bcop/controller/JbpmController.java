//package com.bcop.bcop.controller;
//
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//@RestController
//public class JbpmController {
//
//	String serverUrl = "http://localhost:8080/kie-server/services/rest/";
//
//	private static final String USERNAME = "krisv";
//	private static final String PASSWORD = "krisv";
//
//	String containerId = "BCOP_1.0.0-SNAPSHOT";
//	String processId = "BCOP.bcop";
//	private String processInstanceId;
//	private String taskInstanceId;
//
//	RestTemplate restTemplate = new RestTemplate();
//
//	@PostMapping("/startWorkFlow")
//	public ResponseEntity<String> startProcessInstance() {
//		String url = String.format("%s/server/containers/%s/processes/%s/instances", serverUrl, containerId, processId);
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("accept", "application/json");
//		headers.set("content-type", "application/json");
//		 headers.setBasicAuth(USERNAME, PASSWORD);
//		HttpEntity<String> entity = new HttpEntity<>("{}", headers);
//		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//		processInstanceId = response.getBody();
//		System.out.println(processInstanceId);
//		return ResponseEntity.status(response.getStatusCode()).body("Process instance started successfully");
//	}
//
//	@GetMapping("getActiveTaskId")
//	public ResponseEntity<String> getTasksForProcessInstance() throws JsonMappingException, JsonProcessingException {
//		String url = String.format("%s/server/queries/tasks/instances/process/%s", serverUrl, processInstanceId);
//
//		System.out.println(processInstanceId);
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("accept", "application/json");
//		headers.set("content-type", "application/json");
//		headers.setBasicAuth(USERNAME, PASSWORD);
//		HttpEntity<String> entity = new HttpEntity<>(headers);
//		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//	      ObjectMapper objectMapper = new ObjectMapper();
//	        JsonNode rootNode = objectMapper.readTree(response.getBody());
//
//	        JsonNode taskSummaryNode = rootNode.path("task-summary");
//	        if (taskSummaryNode.isArray() && taskSummaryNode.size() > 0) {
//	            JsonNode firstTask = taskSummaryNode.get(0);
//	            int taskId = firstTask.path("task-id").asInt(); 
//	            taskInstanceId = String.valueOf(taskId);
//	            System.out.println("Extracted Task ID: " + taskInstanceId);
//	        }
//		System.out.println(taskInstanceId);
//		return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
//	}
//
//	@PutMapping("/startTask")
//	public ResponseEntity<String> startTask() {
//		String url = String.format("%s/server/containers/%s/tasks/%s/states/started", serverUrl, containerId,
//				taskInstanceId);
//
//		System.out.println(taskInstanceId);
//		System.out.println("task reserved");
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("accept", "application/json");
//		headers.set("content-type", "application/json");
//		headers.setBasicAuth(USERNAME, PASSWORD);
//		HttpEntity<String> entity = new HttpEntity<>(headers);
//		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
//		System.out.println("task inprogress");
//		return ResponseEntity.status(response.getStatusCode()).body("Task started successfully");
//	}
//
//	@PutMapping("/completeTask")
//	public ResponseEntity<String> completeTask() {
//		String url = String.format("%s/server/containers/%s/tasks/%s/states/completed", serverUrl, containerId,
//				taskInstanceId);
//		System.out.println(taskInstanceId);
//		System.out.println("task inprogress");
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("accept", "application/json");
//		headers.set("content-type", "application/json");
//		headers.setBasicAuth(USERNAME, PASSWORD);
//		HttpEntity<String> entity = new HttpEntity<>(headers);
//		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
//		System.out.println("task completed");
//		return ResponseEntity.status(response.getStatusCode()).body("Task completed successfully");
//	}
//
////	PUT -> /server/containers/{containerId}/tasks/{taskInstanceId}/states/started (Starts a specified task instance).
////	GET -> /server/containers/{containerId}/tasks/{taskInstanceId}  (Returns information about a specified task instance).
////	PUT -> /server/containers/{containerId}/tasks/{taskInstanceId}/states/completed  (Completes a specified task instance).
////	POST -> /server/containers/{containerId}/processes/{processId}/instances (Starts a new process instance of a specified process).
////	GET -> /server/queries/tasks/instances/process/{processInstanceId}	(Returns task instances associated with a specified process instance).
//}
