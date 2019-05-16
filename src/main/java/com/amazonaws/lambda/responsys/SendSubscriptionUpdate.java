package com.amazonaws.lambda.responsys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.util.IOUtils;

/**
 * Lambda class to validate the request payload and write it to the SQS queue
 *
 */
public class SendSubscriptionUpdate extends BaseResponsysHandler implements RequestStreamHandler {
	
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
	private static final String FIFO_QUEUE_NAME = System.getenv("FIFO_QUEUE_NAME");
    private static final String MESSAGE_ID = System.getenv("QUEUE_MSG_ID");
    final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();
    	int errStatusCode = 400;
    	
		try {
			//parse request body
    		JSONObject msgJson = retrievePayload(inputStream, logger);
    		
    		if (msgJson != null) {
    			//get the queue to write
				String fifoQueueUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest(FIFO_QUEUE_NAME)).getQueueUrl();
				//send message to the queue
				SendMessageResult result = writeQueueMessages(sqsClient, fifoQueueUrl, logger, msgJson);
				
				if(result != null) {
					JSONObject resultJson = new JSONObject(result);
					sendResponse(outputStream, getSucessResponse(JSONObject.valueToString(resultJson)));
				}
				
    		} else {
    			sendResponse(outputStream, getErrorResponse(errStatusCode, "Error validating the subscription update message"));
    		}
		} catch (JSONException je) {
			je.printStackTrace();
			sendResponse(outputStream, getErrorResponse(errStatusCode, "Error parsing the data to JSON"));
			
		} catch (Exception e) {
			e.printStackTrace();
			sendResponse(outputStream, getErrorResponse(errStatusCode, "Error sending message to the queue"));
			
		}
	}
	
	/**
	 * method to retrieve the "emailid" param from the input stream
	 * @param inputStream
	 * @return
	 * @throws IOException 
	 * @throws JSONException 
	 */
	private JSONObject retrievePayload(InputStream inputStream, LambdaLogger logger) throws JSONException, IOException {
		
		JSONObject updateMsgJson = new JSONObject(IOUtils.toString(inputStream));
		
		if(null != updateMsgJson.get(BODY)) {
			
			JSONObject subscriberData = new JSONObject((String)updateMsgJson.get(BODY));
			
			logger.log("Message body: " + subscriberData.toString() + NEW_LINE);
			if(subscriberData.has("emailAddress")) {
				String emailid = (String) subscriberData.get("emailAddress");
				
				if (isValid(emailid)) {
					return subscriberData;
				} else {
					logger.log("Message error: invalid emailAddress field " + NEW_LINE);
				}
			} else {
				logger.log("Message error: missing required emailAddress field " + NEW_LINE);
			}
		}
		return null;
	}
	
	/**
	 * function to validate the email
	 * @param email
	 * @return
	 */
	public static boolean isValid(String email) { 
		
        String emailRegex = EMAIL_REGEX; 
        Pattern pat = Pattern.compile(emailRegex); 
        if (email == null)
            return false; 
        
        return pat.matcher(email).matches(); 
    }
	
	/**
	 * Method to write the SQS queue and return the response object
	 * @param logger
	 * @return
	 */
	private SendMessageResult writeQueueMessages(AmazonSQS sqsClient, String fifoQueueUrl, LambdaLogger logger, JSONObject requestPayloadJson) {
		
		logger.log("Starting to send message to queue: " + NEW_LINE);
		final SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(fifoQueueUrl).withMessageBody(requestPayloadJson.toString()).withMessageGroupId(MESSAGE_ID);
		SendMessageResult result = sqsClient.sendMessage(sendMessageRequest);
		logger.log("Send message result: " + result + NEW_LINE);
		return result;
	}
}
