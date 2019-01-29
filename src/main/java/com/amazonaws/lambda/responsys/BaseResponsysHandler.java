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
import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * Class containing the common methods used by Lambda functions
 * @author hdesai
 *
 */
public abstract class BaseResponsysHandler {
	
	
	private static final String AUTH_TOKEN_EP = System.getenv("RESPONSYS_AUTH_TOKEN_ENDPOINT");
	private static final String USERNAME = System.getenv("USERNAME");
	private static final String PASSWORD = System.getenv("PASSWORD");
	private static final String AUTH_TYPE = System.getenv("AUTH_TYPE");
	private static final String AUTH_TOKEN_CREDS = (new StringBuffer("user_name=")).append(USERNAME).append("&password=").append(PASSWORD).append("&auth_type=").append(AUTH_TYPE).toString();

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
	protected static final String GET = "GET";
	protected static final String POST = "POST";
	protected static final String APPLICATION_JSON = "application/json";
	protected static final String AUTHORIZATION = "Authorization";
	protected static final String NEW_LINE = "\n";
	
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
}
