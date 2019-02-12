/**
 * 
 */
package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * Class containing the common methods used by Lambda functions
 * @author hdesai
 *
 */
public abstract class BaseResponsysHandler {
	
	//environment variables
	private static final String AUTH_TOKEN_EP = System.getenv("RESPONSYS_AUTH_TOKEN_ENDPOINT");
	private static final String USERNAME = System.getenv("USERNAME");
	private static final String PASSWORD = System.getenv("PASSWORD");
	private static final String AUTH_TYPE = System.getenv("AUTH_TYPE");
	private static final String AUTH_TOKEN_CREDS = (new StringBuffer("user_name=")).append(USERNAME).append("&password=").append(PASSWORD).append("&auth_type=").append(AUTH_TYPE).toString();
	private static final String ITEM_TABLE_NAME = System.getenv("ITEM_TABLE_NAME");
	public static final String ITEM_ID_PARAM_NAME = System.getenv("ITEM_ID_PARAM_NAME");
	
	//Constants
	protected static final String IS_BASE64_ENCODED = "isBase64Encoded";
	protected static final String STATUS_CODE = "statusCode";
	protected static final String BODY = "body";
	protected static final String UTF_8 = "UTF-8";
	protected static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	protected static final String CONTENT_TYPE = "Content-Type";
	protected static final String NO_CACHE = "no-cache";
	protected static final String CACHE_CONTROL = "cache-control";
	protected static final String AUTH_TOKEN = "authToken";
	protected static final String END_POINT = "endPoint";
	protected static final String ISSUED_AT = "issuedAt";
	protected static final String GET = "GET";
	protected static final String POST = "POST";
	protected static final String APPLICATION_JSON = "application/json";
	protected static final String AUTHORIZATION = "Authorization";
	protected static final String NEW_LINE = "\n";
	
	//Dynamo DB constants
	protected static final String ID_PARAM_VALUE = "token";
	protected static final String CREATION_DATE = "creationDate";
	
	/**
	 * Method to invoke the Responsys AuthToken API call and return the response payload
	 * @param inputStream
	 * @param outputStream
	 * @param context
	 * @return
	 * @throws IOException
	 */
	public String getAuthToken(LambdaLogger logger) throws IOException {

		HttpURLConnection conn = null;
		
		try {
			long startTime = System.currentTimeMillis();
			URL url = new URL(AUTH_TOKEN_EP);

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(POST);
			conn.setRequestProperty(CACHE_CONTROL, NO_CACHE);
			conn.setRequestProperty(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
			conn.setDoOutput(true);
			conn.getOutputStream().write(AUTH_TOKEN_CREDS.getBytes(UTF_8));
			
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			
			StringBuffer result = new StringBuffer();
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				result.append(inputLine);
			}
			br.close();
			
			logger.log("Response from AuthToken api call: " + result.toString() + NEW_LINE);
			
			if (conn.getResponseCode() != 200) {
				logger.log("ResponseCode from AuthToken api call: " + conn.getResponseCode() + NEW_LINE);
				throw new RuntimeException("Failed : HTTP error code: " + conn.getResponseCode());
			}
			long endTime = System.currentTimeMillis();
			logger.log("******************Time elapsed in GET AUTH TOKEN call: " + (endTime - startTime) + NEW_LINE);
			return result.toString();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) 
				conn.disconnect();
		}
		return null;
	}
	
	/**
	 * writes response back to response stream
	 * @param outputStream
	 * @param responseBody
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	protected void sendResponse(OutputStream outputStream, JSONObject responseBody)
			throws UnsupportedEncodingException, IOException {
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(responseBody.toString());
		writer.close();
	}
	
	
	/**
	 * Method to convert the errorResponse in json format compatible with
	 * aws-proxy implementation
	 * 
	 * @param errorCode
	 * @param errorResponse
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject getErrorResponse(int errorCode, String errorResponse) {
		
		String bodyContent = null;
		
		// check if the errorMsg is not JsonFormatted, then wrap it in a JSON Object
		try {
			new JSONObject(errorResponse);
			bodyContent = errorResponse;
			
		} catch (JSONException e) {
			JSONObject message = new JSONObject();
			message.put("message", errorResponse);
			bodyContent = message.toString();
		}
		
		JSONObject responseJson = new JSONObject();
		responseJson.put(BODY, bodyContent);
		responseJson.put(IS_BASE64_ENCODED, false);
		responseJson.put(STATUS_CODE, errorCode);
		return responseJson;
	}

	/**
	 * Method to wrap the json formatted message in another json object,
	 * compatible with aws-proxy implementation
	 * 
	 * @param jsonMsg
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject getSucessResponse(String responseMsg)  {
		
		String bodyContent = null;
		
		// check if the errorMsg is not JsonFormatted, then wrap it in a JSON Object
		try {
			new JSONObject(responseMsg);
			bodyContent = responseMsg;
			
		} catch (JSONException e) {
			JSONObject message = new JSONObject();
			message.put("message", responseMsg);
			bodyContent = message.toString();
		}
		
		JSONObject responseJson = new JSONObject();
		responseJson.put(IS_BASE64_ENCODED, false);
		responseJson.put(STATUS_CODE, "200");
		responseJson.put(BODY, bodyContent);
		return responseJson;
	}
	
	
	protected Map<String, String> getResponsysApiInfo (LambdaLogger logger) {
		
		Map<String, String> apiDetailsMap = null;
		Item tokenItem = null;
		
		//Dynamo DB call to get the TOKEN details
		try {
			tokenItem = retrieveTokenFromDB(getDynamoDB());
		} catch (Exception e) {
			logger.log("Error retrieving the token item from dynamoDB: " + e.getStackTrace());
		}
		
		if(tokenItem != null && tokenItem.hasAttribute(AUTH_TOKEN) && tokenItem.hasAttribute(END_POINT)) {
			//parse AUTH TOKEN api response
			apiDetailsMap = new HashMap<String, String>();
			apiDetailsMap.put(END_POINT,tokenItem.get(END_POINT).toString());
			apiDetailsMap.put(AUTH_TOKEN,tokenItem.get(AUTH_TOKEN).toString());
			logger.log("Successfully retrieved the token details from DB" + NEW_LINE);
			
		} else { //make AUTH TOKEN responsys call
			
			logger.log("Unable to retrieve token item from dynamoDB, making direct responsys API call to get the token details:" + NEW_LINE);
    		
			try {
	    		String apiResponse = getAuthToken(logger);
	    		
	    		if(null != apiResponse && apiResponse.length() > 0) {
	    			
	    			logger.log("Success response receivied from AuthToken API call " +NEW_LINE);
		    		
	    			JSONObject responseJson = new JSONObject(apiResponse);
	    			
	    			//add Token to DB
	    			PutItemOutcome outcome = insertTokenInDB(getDynamoDB(), responseJson);
					logger.log("Put item outcome: " + outcome + NEW_LINE);
					
	    			//parse AUTH TOKEN api response
					if(responseJson.get(END_POINT) != null && responseJson.get(AUTH_TOKEN) != null) {
						apiDetailsMap = new HashMap<String, String>();
						apiDetailsMap.put(END_POINT, responseJson.get(END_POINT).toString());
						apiDetailsMap.put(AUTH_TOKEN, responseJson.get(AUTH_TOKEN).toString());
						logger.log("Successfully assigned token details " + NEW_LINE);
					}
	    		}
			} catch (Exception e) {
				logger.log("Error retrieving the token from AUTH TOKEN api call: " + e.getStackTrace() + NEW_LINE);
			}
		}
		return apiDetailsMap;
	}
	
	/**
	 * returns client for DynamoDB
	 * @return
	 */
	protected DynamoDB getDynamoDB() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);
		return dynamoDB;
	}
	
	/**
	 * Method to insert the JSONObject into DB with id=token
	 * @param dynamoDB
	 * @param responseJson
	 * @return
	 */
	protected PutItemOutcome insertTokenInDB (DynamoDB dynamoDB, JSONObject responseJson) {
		Table table = dynamoDB.getTable(ITEM_TABLE_NAME);

		String currentUTCTime = java.time.Clock.systemUTC().instant().toString();
		
		Item item = new Item().withPrimaryKey(ITEM_ID_PARAM_NAME, ID_PARAM_VALUE);
		item.with(AUTH_TOKEN,(String) responseJson.get(AUTH_TOKEN));
		item.with(END_POINT,(String) responseJson.get(END_POINT));
		item.with(ISSUED_AT, responseJson.get(ISSUED_AT));
		item.with(CREATION_DATE, currentUTCTime);
		PutItemOutcome outcome = table.putItem(item);
		
		return outcome;
	}
	
	/**
	 * Method to retrieve DB entry based on the id=token
	 * @param dynamoDB
	 * @return
	 */
	protected Item retrieveTokenFromDB (DynamoDB dynamoDB) {
		
		Table table = dynamoDB.getTable(ITEM_TABLE_NAME);
		Item item = table.getItem(ITEM_ID_PARAM_NAME, ID_PARAM_VALUE);
		return item;
	}
}
