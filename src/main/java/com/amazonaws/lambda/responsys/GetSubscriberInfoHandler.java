package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;

/**
 * Lambda to get the subscriber info based on the emailId passed by the guest 
 *
 */
public class GetSubscriberInfoHandler extends BaseResponsysHandler implements RequestStreamHandler{

	private static final String EMAILID_PARAM = "emailid";
	private static final String GET_MEMBER_API_URL = System.getenv("GET_MEMBER_API_URL");
	
	
	/* (non-Javadoc)
	 * @see com.amazonaws.services.lambda.runtime.RequestStreamHandler#handleRequest(java.io.InputStream, java.io.OutputStream, com.amazonaws.services.lambda.runtime.Context)
	 */
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();
    	StringBuffer result = new StringBuffer();
    	HttpURLConnection urlConnection = null;
    	int errStatusCode = 400;
    	
    	try {
    		//make AUTH TOKEN call
    		String apiResponse = getAuthToken(logger);
    		logger.log("Response receivied from AuthToken API call: " + apiResponse +NEW_LINE);
    		
    		if(null == apiResponse || apiResponse.length() < 1) {
    			logger.log("Failed retrieving the AUTH token \n");
				sendResponse(outputStream, getErrorResponse(errStatusCode, "Failed retrieving the AUTH token"));
				return;
    		}
    		
    		//parse AUTH TOKEN api response
    		JSONObject responseJson = new JSONObject(apiResponse);
    		String apiHost = responseJson.get(END_POINT).toString();
			String apiAuthToken = responseJson.get(AUTH_TOKEN).toString();
			
			//get request post param "emailid"
			String emailId = getSubscriberEmail(inputStream);
			logger.log("Email ID passed in request: " + emailId +NEW_LINE);
			
			// Calling responsys to get the subscriber details
			if( emailId != null && emailId.length() > 0) {
				long startTime = System.currentTimeMillis();
				StringBuffer getSubscriberUrl = new StringBuffer();
				final String GET_FIELD_PARAMS = System.getenv("GET_FIELD_PARAMS");
				final String FIELDS_PARAM = "&fs=" + GET_FIELD_PARAMS;
				
				URL getSubsciberUrl = new URL((getSubscriberUrl.append(apiHost).append(GET_MEMBER_API_URL).append("?qa=e&id=").append(emailId).append(FIELDS_PARAM)).toString());
				
				logger.log("Calling API GETSUBSCIBER endpoint: " + getSubsciberUrl +NEW_LINE);
				
				urlConnection = (HttpURLConnection) getSubsciberUrl.openConnection();
				urlConnection.setRequestMethod(GET);
				urlConnection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
				urlConnection.setRequestProperty(AUTHORIZATION, apiAuthToken);
				urlConnection.setDoOutput(true);
				
				BufferedReader br = null;
				String inputLine2;
				
				if (urlConnection.getResponseCode() != 200) {
					logger.log("Response from api call: " + result.toString() +NEW_LINE);
					
					errStatusCode = urlConnection.getResponseCode();
					br = new BufferedReader(new InputStreamReader((urlConnection.getErrorStream())));
					while ((inputLine2 = br.readLine()) != null) {
						result.append(inputLine2);
					}
					sendResponse(outputStream, getErrorResponse(errStatusCode, result.toString()));
					
				} else {
					br = new BufferedReader(new InputStreamReader((urlConnection.getInputStream())));
					while ((inputLine2 = br.readLine()) != null) {
						result.append(inputLine2);
					}
					final String IS_TRANSFORM_RESPONSE = System.getenv("IS_TRANSFORM_RESPONSE");
					if("TRUE".equalsIgnoreCase(IS_TRANSFORM_RESPONSE)) {
						
						sendResponse(outputStream, getSucessResponse(getTransformedResponse(result.toString(), logger).toString()));
					} else {
						sendResponse(outputStream, getSucessResponse(result.toString()));
					}
					
				}
				br.close();
				
				long endTime = System.currentTimeMillis();
				logger.log("******************Time elapsed in GET SUBSCRIBER call: " + (endTime - startTime) + NEW_LINE);
				logger.log("Response from api call: " + result.toString() + NEW_LINE);
		        
			} else {
				logger.log("Missing the request param \n");
				sendResponse(outputStream, getErrorResponse(errStatusCode, "Bad Request"));
			}
		
		} catch (Exception e) {
			e.printStackTrace();
			sendResponse(outputStream, getErrorResponse(errStatusCode, e.getMessage()));
			
		} finally {
			if(urlConnection != null)
				urlConnection.disconnect();
		}
    }
	
	/**
	 * method to retrieve the "emailid" param from the input stream
	 * @param inputStream
	 * @return
	 */
	private String getSubscriberEmail(InputStream inputStream) {
		
		String emailid = null;
		
		try {
			JSONObject event = new JSONObject(IOUtils.toString(inputStream));
			
			if(null != event.get(BODY)) {
				
				JSONObject subscriberData = new JSONObject((String)event.get(BODY));
				
				if(subscriberData.has(EMAILID_PARAM)) {
					emailid = (String) subscriberData.get(EMAILID_PARAM);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return emailid;
	}
	
	/**
	 * Method to transform the response to retrieve the subscription flag from the passed payload
	 * @param responsePayload
	 * @param logger
	 * @return
	 */
	private String getTransformedResponse(String responsePayload, LambdaLogger logger) {

		JSONObject jsonResponse = new JSONObject(responsePayload);

		if (jsonResponse.has("recordData") && ((JSONObject) jsonResponse.get("recordData")).has("records")) {

			JSONArray recordList = (JSONArray) (((JSONObject) jsonResponse.get("recordData")).get("records"));

			if (recordList != null && recordList.length() > 0) {
				if (recordList.get(0) != null) {
					JSONArray recordFields = (JSONArray) recordList.get(0);
					if (recordFields != null && recordFields.length() >= 2) {
						logger.log("Record data received from Responsys: " + recordFields + NEW_LINE);
						JSONObject responseObj = new JSONObject();
						responseObj.put("emailid", (String) recordFields.get(0));
						responseObj.put("subscribed",
								"I".equalsIgnoreCase((String) recordFields.get(1)) ? "true" : "false");

						return responseObj.toString();
					}
				}
			}
		}
		logger.log("The response payload does not have the expected data, returning null from getTransformedResponse() method");
		return null;
	}
	
}