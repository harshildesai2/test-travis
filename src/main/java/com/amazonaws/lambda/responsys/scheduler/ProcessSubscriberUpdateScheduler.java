package com.amazonaws.lambda.responsys.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.lambda.bean.RecordData;
import com.amazonaws.lambda.bean.SubscriptionReq;
import com.amazonaws.lambda.responsys.BaseResponsysHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * Lambda scheduler to generate the AUTH token
 *
 */
public class ProcessSubscriberUpdateScheduler extends BaseResponsysHandler implements RequestStreamHandler {
	
   	private static final String MERGE_RULE = "mergeRule";
   	private static final String MERGE_RULE_JSON = System.getenv("MERGE_RULE_JSON");
    private static final String UPDATE_API_URL = System.getenv("UPDATE_API_URL");
    private static final String FIFO_QUEUE_NAME = System.getenv("FIFO_QUEUE_NAME");
    //no of queue messages to be read per read
    private static final String BATCH_SIZE = System.getenv("BATCH_SIZE");
    //no of batches to be read per lambda invocation - total no of messages read per lambda invocation: BATCH_SIZE * BATCH_COUNT
    private static final String BATCH_COUNT = System.getenv("BATCH_COUNT");
    //wait time in seconds between each batch run
    private static final String WAIT_TIME = System.getenv("WAIT_TIME");
    //Lambda Schedule period in minutes
    private static final String SCHEDULE_PERIOD = System.getenv("SCHEDULE_PERIOD");

    static final int waitTime = getWaitTime();
    static final int batchSize = getBatchSize();
    static final int batchCount = getBatchCount();
    
    
    
    // Ordered Map of field received from client and fieldNames to be sent to responsys
    static Map<String, String> fieldMappings = new LinkedHashMap<String,String>();
    static {
    	fieldMappings.put("emailAddress", "EMAIL_ADDRESS_");
    	fieldMappings.put("country", "COUNTRY_");
    	fieldMappings.put("optIn", "EMAIL_PERMISSION_STATUS_");
    	fieldMappings.put("userId", "CUSTOMER_ID_");
    	fieldMappings.put("firstName", "FIRST_NAME");
    	fieldMappings.put("lastName", "LAST_NAME");
    	fieldMappings.put("userType", "USER_TYPE");
    	fieldMappings.put("lastActivityDate", "LASTACTIVITY_DATE");
    	fieldMappings.put("registrationDate", "REGISTRATION_DATE");
    	fieldMappings.put("gender", "GENDER");
    	fieldMappings.put("dateOfBirth", "DATE_OF_BIRTH");
    	fieldMappings.put("employeeType", "EMPLOYEE_TYPE");
    	fieldMappings.put("productGender", "PRODUCT_GENDER");
    	fieldMappings.put("productActivities", "PRODUCT_ACTIVITIES");
    }
    
    static final List<String> fieldList = getFieldNames();

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();
		StringBuffer response = new StringBuffer();
    	HttpURLConnection urlConnection = null;
    	int errStatusCode = 400;
    	
		try {

			//make AUTH TOKEN call
    		String apiResponse = getAuthTokenAPI(logger, false);
    		logger.log("response receivied from AuthToken API call: " + apiResponse + NEW_LINE);
    		
    		if(null == apiResponse || apiResponse.length() < 1) {
    			logger.log("Failed retrieving the AUTH token" + NEW_LINE);
				sendResponse(outputStream, getErrorResponse(errStatusCode, "Failed retrieving the AUTH token and EndPoint"));
				return;
    		}
    		
    		//parse AUTH TOKEN api response
    		JSONObject loginAPIResponseJson = new JSONObject(apiResponse);
    		String apiHost = loginAPIResponseJson.get(END_POINT).toString();
			String apiAuthToken = loginAPIResponseJson.get(AUTH_TOKEN).toString();
			logger.log("apiAuthToken: " + apiAuthToken +NEW_LINE);
			
			//get request payload
			List<Message> failedMessages = new ArrayList<Message>();
			List<Message> successMessages = new ArrayList<Message>();
			List<Message> messages = new ArrayList<Message>();
			//get the queue messages
			final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
			String fifoQueueUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest(FIFO_QUEUE_NAME)).getQueueUrl();
	        
			long startTime = 0, endTime = 0;
			
			for (int i = 0; i < batchCount; i++) {
				
				startTime = System.currentTimeMillis();
				messages.clear();
				failedMessages.clear();
				successMessages.clear();
				
				logger.log("******************Starting the batch processing for batch: " + i+1 + " of " + batchCount + NEW_LINE);
				
				messages = readQueueMessages(sqsClient, fifoQueueUrl, logger);
			
				if(messages != null && !messages.isEmpty()) {
					
					int totalMsgsRead = messages.size();
					
					//process messages and execute the responsysupdate API
					processUpdateaAPICall(apiHost, apiAuthToken, logger, messages, failedMessages, successMessages);
					
					logger.log("Failed messages " + failedMessages.size() + NEW_LINE);
					
					// process and execute the failed messages again
					if(failedMessages != null && !failedMessages.isEmpty()) {
						
						messages.removeAll(failedMessages);
						
					}
					//deleting the successful messages in batch from the SQS Queue
					if(!messages.isEmpty())
						deleteBatchMsgsFromQueue(sqsClient, fifoQueueUrl, messages, logger);
					
					endTime = System.currentTimeMillis();
					logger.log("******************Time elapsed in processing for batch " + i+1  + ": " + (endTime - startTime) + NEW_LINE);
					
					//checking if there is a possibility of more msgs in the queue, then wait for last batch to complete and then execute the read again
					if (totalMsgsRead == batchSize)
						TimeUnit.SECONDS.sleep(waitTime);
					
				} else {
					logger.log("Ending the batch read processing after batch " + i+1 + ", no more messages available in queue");
					break;
				}
			} 

		} catch (JSONException je) {
			je.printStackTrace();
			sendResponse(outputStream, getErrorResponse(errStatusCode, "Error parsing the data to JSON"));
			
		} catch (Exception e) {
			e.printStackTrace();
			sendResponse(outputStream, getErrorResponse(errStatusCode, response.toString()));
			
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}
	
	private void deleteBatchMsgsFromQueue(AmazonSQS sqsClient, String fifoQueueUrl, List<Message> deleteMsgList, LambdaLogger logger) {
		
		final DeleteMessageBatchRequest batchRequest = new DeleteMessageBatchRequest().withQueueUrl(fifoQueueUrl);
		final List<DeleteMessageBatchRequestEntry> entries = new ArrayList<DeleteMessageBatchRequestEntry>();
		
		try {
		
			for (int i = 0, n = deleteMsgList.size(); i < n; i++)
				entries.add(new DeleteMessageBatchRequestEntry().withId(Integer.toString(i)).withReceiptHandle(deleteMsgList.get(i).getReceiptHandle()));
			
			batchRequest.setEntries(entries);
	
			logger.log("Deleting successful messages from the queue: " + deleteMsgList.size() + NEW_LINE);
			final DeleteMessageBatchResult batchResult = sqsClient.deleteMessageBatch(batchRequest);
	
			// Because DeleteMessageBatch can return successfully, but individual batch items can fail, retry the failed batch items.
			if (!batchResult.getFailed().isEmpty()) {
				
				final int n = batchResult.getFailed().size();
				
				logger.log("Retrying deleting " + n + " messages");
				for (BatchResultErrorEntry e : batchResult.getFailed()) {
					sqsClient.deleteMessage(new DeleteMessageRequest(fifoQueueUrl, deleteMsgList.get(Integer.parseInt(e.getId())).getReceiptHandle()));
				}
			}
		} catch (AmazonClientException e) {
			// By default, AmazonSQSClient retries calls 3 times before failing. If this unlikely condition occurs, stop.
			logger.log("BatchConsumer: " + e.getMessage());
        }
	}
	
	/**
	 * Method to process the messages: 
	 * - generate payload using update messages passed as method attribute, 
	 * - make Responsys MergeList API call, 
	 * - parse the response from API call
	 * 
	 * @param apiHost
	 * @param apiAuthToken
	 * @param logger
	 * @param messages
	 * @param failedMessages
	 * @param successMessages
	 */
	private void processUpdateaAPICall (String apiHost, String apiAuthToken, LambdaLogger logger, List<Message> messages, List<Message> failedMessages, List<Message> successMessages) {
		
		//get request payload
		JSONObject payloadJson = generateRequestPayload(messages, logger, failedMessages, successMessages);
		
		//execute the API call
		if(payloadJson != null) {
			logger.log("Request payload: " + payloadJson.toString() + NEW_LINE);
			JSONObject responseJson = executeUpdateResponsysAPI(payloadJson.toString(), apiHost, apiAuthToken, logger);
			
			if(responseJson != null) {
				parseResponsePayload(responseJson, logger, failedMessages, successMessages);				
			}
		}
	}
	
	private void parseResponsePayload (JSONObject responseJson, LambdaLogger logger, List<Message> failedMessages, List<Message> successMessages) {
		
		try {
			if(responseJson.has("recordData") && responseJson.get("recordData") instanceof JSONObject) {
				
				JSONObject recordDataJson = (JSONObject)responseJson.get("recordData");
				if(recordDataJson != null && recordDataJson.has("records") && recordDataJson.get("records") instanceof JSONArray) {
				
					JSONArray recordList = (JSONArray)recordDataJson.get("records");	
					
					for(int i=0; recordList != null && i<recordList.length(); i++) {
						if(recordList.get(i) != null && recordList.get(i) instanceof JSONArray) {
							
							JSONArray recordPropList = (JSONArray)recordList.get(i);
							
							if(recordPropList.get(0) != null && recordPropList.get(0) instanceof String) {
								String recordMsg = (String) recordPropList.get(0);
								
								if(recordMsg.contains("MERGEFAILED")) {
									logger.log("Following message failed after MergeList api call: " + successMessages.get(i) + " adding to failedList" + NEW_LINE);
									failedMessages.add(successMessages.get(i));
								}
								
							}
							
						}
					}
					
				} else {
					logger.log("Response parsing issue: unable to find the records in response" + NEW_LINE);
				}
			} else {
				logger.log("Response parsing issue: unable to find the recordData in response" + NEW_LINE);
			}
			
			
		} catch(Exception e) {
			
		}
		
	}
	
	private JSONObject executeUpdateResponsysAPI(String payload, String apiHost, String apiAuthToken, LambdaLogger logger) {
		
		HttpURLConnection urlConnection = null;
		StringBuffer response = new StringBuffer();
		JSONObject responseJson = null;
		
		logger.log("******************Starting to make Responsys MERGE LIST API call" + NEW_LINE);
		
		try {
			long startTime = System.currentTimeMillis();
			URL subcUrl = new URL(apiHost + UPDATE_API_URL);
			urlConnection = (HttpURLConnection) subcUrl.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
			urlConnection.setRequestProperty(AUTHORIZATION, apiAuthToken);
			urlConnection.setDoOutput(true);
			urlConnection.getOutputStream().write(payload.toString().getBytes(UTF_8));

			BufferedReader br = null;
			String inputLine2;
			
			if (urlConnection.getResponseCode() != 200) {
				
				br = new BufferedReader(new InputStreamReader((urlConnection.getErrorStream())));
				while ((inputLine2 = br.readLine()) != null) {
					response.append(inputLine2);
				}
				logger.log("Responsys MERGE LIST API failed with following status code" + urlConnection.getResponseCode() + " response message: " + response + NEW_LINE);
				
			} else {
				br = new BufferedReader(new InputStreamReader((urlConnection.getInputStream())));
				while ((inputLine2 = br.readLine()) != null) {
					response.append(inputLine2);
				}
				responseJson = new JSONObject(response.toString());
				logger.log("Response JSON: " + responseJson.toString() + NEW_LINE);
			}
			br.close();
			
			long endTime = System.currentTimeMillis();
			logger.log("******************Time elapsed in Responsys MERGE LIST service call: " + (endTime - startTime) + NEW_LINE);
		
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
		return responseJson;
	}
	
	/**
	 * Method to read the SQS queue and return a batch of 'X' messages
	 * @param logger
	 * @return
	 */
	private List<Message> readQueueMessages(AmazonSQS sqsClient, String fifoQueueUrl, LambdaLogger logger) {
		
		logger.log("Reading messages from queue: " + NEW_LINE);
		
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(fifoQueueUrl).withMaxNumberOfMessages(batchSize);
		ReceiveMessageResult result = sqsClient.receiveMessage(receiveMessageRequest);
		
		if (result.getMessages() != null && !result.getMessages().isEmpty()) {
			logger.log("Total Messages read from the queue: " + result.getMessages().size() + NEW_LINE);
			return result.getMessages();
		} else {
			logger.log("Queue empty, no message read" + NEW_LINE);
			return null;
		}
	}
	
	private JSONObject generateRequestPayload (List<Message> messages, LambdaLogger logger, List<Message> failedMessages, List<Message> successMessages) {
		
		logger.log("Starting to generate the request payload: " + NEW_LINE);
		
		List <List<String>> records = getSubscriberRecordList(messages, logger, failedMessages, successMessages);
		
		if(records != null) {
			
			RecordData recordData = new RecordData();
			recordData.setFieldNames(fieldList);
			recordData.setRecords(records);
			
			SubscriptionReq subscriberObj = new SubscriptionReq();
			subscriberObj.setRecordData(recordData);
			  
			//adding merge rule to the generated subscriber JSON
			JSONObject subscriberReq = new JSONObject(subscriberObj);
			JSONObject mergeRuleObj = new JSONObject(MERGE_RULE_JSON);
			subscriberReq.put(MERGE_RULE, mergeRuleObj);
			
			return subscriberReq;
		}
		 
		
		return null;
	}
	
	private List <List<String>> getSubscriberRecordList (List<Message> allMessages, LambdaLogger logger, List<Message> failedMessages, List<Message> successMessages ) {
		
		List <List<String>> records = new ArrayList<List<String>>();
		List<String> recordPropertyList = null;
		
		//generate the fieldNames and fieldValues params
		for (Message message : allMessages) {
			
			if(message.getBody() != null) {
				
				try {
					String msgBodyStr = message.getBody().toString();
					JSONObject msgBody = new JSONObject(msgBodyStr.replace("\\",""));
					
					logger.log("Queue Message BODY content: " + msgBody.toString());
					
					recordPropertyList = new ArrayList<String>();
					for(Map.Entry<String, String> fieldSet : fieldMappings.entrySet()) {
						if(msgBody.has(fieldSet.getKey())) {
							recordPropertyList.add(msgBody.getString(fieldSet.getKey()));
						} else {
							recordPropertyList.add(null);
						}
					}
					if(recordPropertyList.size() > 0) {
						records.add(recordPropertyList)	;
					}
					
					if (failedMessages == null)
						failedMessages = new ArrayList<Message>();
					
					successMessages.add(message);
					
				} catch (Exception e) {
					e.printStackTrace();
					if (failedMessages == null)
						failedMessages = new ArrayList<Message>();
					
					logger.log("Adding following message to the failed list: " + message.getMessageId() + NEW_LINE);
					failedMessages.add(message);
				}
			}
		}
		
		if(records.size() > 0) {
			logger.log("Count of Messages successfully added as records for processing: " + records.size() + NEW_LINE);
			return records;
		}
		
		logger.log("No messages found in the queue: " + NEW_LINE);
		return null;
	}
	
	static List <String> getFieldNames () {
		
		String value = null;
		List <String> fieldList = new ArrayList<String> ();
		
		for(Map.Entry<String, String> entrySet : fieldMappings.entrySet()) {
			value = entrySet.getValue();
			fieldList.add(value);
		}
		return fieldList;
	}

	static int getBatchSize () {
		
		int batchSize = 10;
		try {
			int batch = Integer.parseInt(BATCH_SIZE);
			batchSize = batch > 10 ? 10 : batch;
		} catch (Exception ne) {
			ne.printStackTrace();
		}
		return batchSize;
	}
	
	static int getBatchCount () {
		
		int scheduler_period = 1;
		int batchCount = 5;
		try {
			scheduler_period = Integer.parseInt(SCHEDULE_PERIOD);
		} catch (Exception ne) {
			ne.printStackTrace();
		}
		try {
			int maxBatches = scheduler_period * 60/waitTime;
			int batch = Integer.parseInt(BATCH_COUNT);
			batchCount = batch > maxBatches ? maxBatches : batch;
		} catch (Exception ne) {
			ne.printStackTrace();
		}
		return batchCount;
	}
	
	
	static int getWaitTime () {
		
		int waitTime = 45;
		try {
			int time = Integer.parseInt(WAIT_TIME);
			waitTime = time > 45 ? 45 : time;
		} catch (Exception ne) {
			ne.printStackTrace();
		}
		return waitTime;
	}
}
