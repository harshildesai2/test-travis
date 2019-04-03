package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * Lambda to get the subscriber info based on the emailId passed by the guest
 *
 */
public class LoginHandler extends BaseResponsysHandler implements RequestStreamHandler{

	//Responsys call details
	private static final String AUTH_TOKEN_EP = System.getenv("RESPONSYS_AUTH_TOKEN_ENDPOINT");
	private static final String USERNAME = System.getenv("RESPONSYS_USERNAME");
	private static final String PASSWORD = System.getenv("RESPONSYS_PASSWORD");
	private static final String AUTH_TYPE = System.getenv("RESPONSYS_AUTH_TYPE");
	private static final String AUTH_TOKEN_CREDS = (new StringBuffer("user_name=")).append(USERNAME).append("&password=").append(PASSWORD).append("&auth_type=").append(AUTH_TYPE).toString();

	/* (non-Javadoc)
	 * @see com.amazonaws.services.lambda.runtime.RequestStreamHandler#handleRequest(java.io.InputStream, java.io.OutputStream, com.amazonaws.services.lambda.runtime.Context)
	 */
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();
    	int errStatusCode = 400;
    	
    	try {
    		processAuthTokenCall(logger, outputStream, false);
    		
    	} catch (Exception e) {
			e.printStackTrace();
			sendResponse(outputStream, getErrorResponse(errStatusCode, "Bad Request"));
		}
    }
	
	private void processAuthTokenCall(LambdaLogger logger, OutputStream outputStream, boolean isRetry) throws UnsupportedEncodingException, IOException {
		
    	//make AUTH TOKEN call
		String apiResponse = getAuthToken(logger);
		
		if(null == apiResponse || apiResponse.length() < 1) {
			
			if(isRetry) {
				logger.log("Failed retrieving the AUTH token in first attempt, retrying" + NEW_LINE);
				processAuthTokenCall(logger, outputStream, false);
				
			} else {
				logger.log("Failed retrieving the AUTH token in second attempt" + NEW_LINE);
				sendResponse(outputStream, getErrorResponse(errStatusCode, "Bad Request"));
				return;
			}
			
		} else {
			sendResponse(outputStream, getSucessResponse(apiResponse));
		}
	}
	
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
		long startTime = System.currentTimeMillis();
		
		try {
			
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
				
			} else {
				return result.toString();
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) 
				conn.disconnect();
			long endTime = System.currentTimeMillis();
			logger.log("******************Time elapsed in GET AUTH TOKEN call: " + (endTime - startTime) + NEW_LINE);
		}
		return null;
	}
}