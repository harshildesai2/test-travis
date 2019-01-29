package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.lambda.bean.RecordData;
import com.amazonaws.lambda.bean.SubscriptionReq;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;

public class UpdateSubscriberInfoHandler extends BaseResponsysHandler implements RequestStreamHandler {
	
   	private static final String MERGE_RULE = "mergeRule";
   	private static final String mergeRule = System.getenv("MERGE_RULE_JSON");
    private static final String UPDATE_API_URL = System.getenv("UPDATE_API_URL");

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();
		StringBuffer response = new StringBuffer();
    	HttpURLConnection urlConnection = null;
    	int errStatusCode = 400;
    	
		try {

			//make AUTH TOKEN call
    		String apiResponse = getAuthToken(logger);
    		logger.log("response receivied from AuthToken API call: " + apiResponse + NEW_LINE);
    		
    		if(null == apiResponse || apiResponse.length() < 1) {
    			logger.log("Failed retrieving the AUTH token \n");
				sendResponse(outputStream, getErrorResponse(400, "Failed retrieving the AUTH token"));
				return;
    		}
    		
    		//parse AUTH TOKEN api response
    		JSONObject responseJson = new JSONObject(apiResponse);
    		String apiHost = responseJson.get(END_POINT).toString();
			String apiAuthToken = responseJson.get(AUTH_TOKEN).toString();
			logger.log("apiAuthToken: " + apiAuthToken +NEW_LINE);
			
			//get request payload
			String payload = generateRequestPayload(inputStream, logger);
			
			logger.log("Request payload: " + payload +NEW_LINE);
			
			if(payload != null) {
				
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
					logger.log("Response from api call: " + response.toString() +NEW_LINE);
					
					errStatusCode = urlConnection.getResponseCode();
					br = new BufferedReader(new InputStreamReader((urlConnection.getErrorStream())));
					while ((inputLine2 = br.readLine()) != null) {
						response.append(inputLine2);
					}
					sendResponse(outputStream, getErrorResponse(errStatusCode, response.toString()));
					
				} else {
					br = new BufferedReader(new InputStreamReader((urlConnection.getInputStream())));
					while ((inputLine2 = br.readLine()) != null) {
						response.append(inputLine2);
					}
					sendResponse(outputStream, getSucessResponse(response.toString()));
				}
				br.close();
				
				logger.log("Response from api call: " + response.toString() +NEW_LINE);
				JSONObject responseJson2 = new JSONObject(response.toString());
				logger.log("Response json: " + responseJson2 +NEW_LINE);
				sendResponse(outputStream, getSucessResponse(response.toString()));
				
			} else {
				response.append("Bad Request");
				sendResponse(outputStream, getErrorResponse(errStatusCode, response.toString()));
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
	
	/**
	 * Method to generate the request payload for Responsys from the reuqest received from client
	 * @param inputStream
	 * @param logger
	 * @return
	 */
	private String generateRequestPayload (InputStream inputStream, LambdaLogger logger) {
		
		try {
			JSONObject event = new JSONObject(IOUtils.toString(inputStream));
			if(event.has(BODY)) {
				logger.log("Request BODY content: " + event.toString());
				
				JSONObject subscriberJson = new JSONObject((String)event.get(BODY));
				JSONObject subscriberData = getSubscriberData(subscriberJson, logger);
			
				if(subscriberData != null)
					return subscriberData.toString();
				else 
					logger.log("Subscriber JSON generated is NULL");
				
			} else {
				logger.log("Unable to parse the body in JSONObject");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private JSONObject getSubscriberData (JSONObject subscriberJson, LambdaLogger logger) {
		
		if(subscriberJson != null) {
			
			List <String> fieldNames = new ArrayList<String>();
			List <String> fieldValues = new ArrayList<String>();
			
			final Map<String, String> fieldMappings = createMap();
			
			//generate the fieldNames and fieldValues params
			for (Object key : subscriberJson.keySet()) {
				
				String keyStr = (String)key;
				
				if(keyStr != null && fieldMappings.containsKey(keyStr)) {
					
					Object keyvalue = subscriberJson.get(keyStr);
					
					if(keyvalue != null && keyvalue instanceof String) {
						fieldNames.add(fieldMappings.get(keyStr));
						fieldValues.add((String)keyvalue);
					}
				}
			}
			
			if(fieldNames.size() > 0 && fieldNames.size() == fieldValues.size()) {
				
				RecordData recordData = new RecordData();
				recordData.setFieldNames(fieldNames);
				recordData.setRecords(Arrays.asList(fieldValues));
				
				SubscriptionReq subscriberObj = new SubscriptionReq();
				subscriberObj.setRecordData(recordData);
				  
				//adding merge rule to the generated subscriber JSON
				JSONObject subscriberReq = new JSONObject(subscriberObj);
				JSONObject mergeRuleObj = new JSONObject(mergeRule);
				subscriberReq.put(MERGE_RULE, mergeRuleObj);
				
				return subscriberReq;
			}
			 
		} else {
			logger.log("Subscriber data in the body is NULL");
		}
		return null;
	}
	
	/**
	 * Method to maintain a map of field received from client vs field to be sent to responsys
	 * @return
	 */
	private static Map<String, String> createMap()
    {
        Map<String,String> fieldsMap = new HashMap<String,String>();
        fieldsMap.put("emailAddress", "EMAIL_ADDRESS_");
        fieldsMap.put("country", "COUNTRY_");
        fieldsMap.put("optIn", "EMAIL_PERMISSION_STATUS_");
        fieldsMap.put("userId", "CUSTOMER_ID_");
        fieldsMap.put("firstName", "FIRST_NAME");
        fieldsMap.put("lastName", "LAST_NAME");
        fieldsMap.put("userType", "USER_TYPE");
        fieldsMap.put("lastActivityDate", "LASTACTIVITY_DATE");
        fieldsMap.put("registrationDate", "REGISTRATION_DATE");
        fieldsMap.put("gender", "GENDER");
        fieldsMap.put("dateOfBirth", "DATE_OF_BIRTH");
        fieldsMap.put("employeeType", "EMPLOYEE_TYPE");
        fieldsMap.put("productGender", "PRODUCT_GENDER");
        fieldsMap.put("productActivities", "PRODUCT_ACTIVITIES");
        return fieldsMap;
    }
	
}