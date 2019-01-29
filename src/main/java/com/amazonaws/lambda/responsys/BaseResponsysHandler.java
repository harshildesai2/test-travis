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

import org.json.simple.JSONObject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * @author hdesai
 *
 */
public abstract class BaseResponsysHandler {
	
	private static final String AUTH_TOKEN_EP = System.getenv("RESPONSYS_AUTH_TOKEN_ENDPOINT");
	private static final String USERNAME = System.getenv("USERNAME");
	private static final String PASSWORD = System.getenv("PASSWORD");
	private static final String AUTH_TYPE = System.getenv("AUTH_TYPE");
	private static final String AUTH_TOKEN_CREDS = (new StringBuffer("user_name")).append(USERNAME).append("&password").append(PASSWORD).append("&auth_type").append(AUTH_TYPE).toString();

	public static final String IS_BASE64_ENCODED = "isBase64Encoded";
	public static final String STATUS_CODE = "statusCode";
	public static final String BODY = "body";
	
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
	
	public static JSONObject getErrorResponse(int errorCode, String errorMessage) {
		JSONObject responseJson = new JSONObject();
		
		JSONObject message = new JSONObject();
		message.put("message", errorMessage);
		
		responseJson.put(BODY, message.toJSONString());
		
		responseJson.put(IS_BASE64_ENCODED, false);
		responseJson.put(STATUS_CODE, errorCode);
		return responseJson;
	}

	public static JSONObject getSucessResponse(String responseBody) {
		JSONObject responseJson = new JSONObject();
		
		JSONObject message = new JSONObject();
		message.put("message", responseBody);
		
		responseJson.put(IS_BASE64_ENCODED, false);
		responseJson.put(STATUS_CODE, "200");
		responseJson.put(BODY, message.toJSONString());
		return responseJson;
	}
	
	public String getAuthTokenResponse(Context context) {
		
		long startTime = System.currentTimeMillis();
		
		context.getLogger().log("GET TOKEN call endpoint " + AUTH_TOKEN_EP+"\n");
		
		StringBuffer result1 = new StringBuffer("");
		HttpURLConnection conn1;
		
		try {
			URL url = new URL(AUTH_TOKEN_EP);
			conn1 = (HttpURLConnection) url.openConnection();
			conn1.setRequestMethod("GET");
			conn1.setRequestProperty("cache-control", "no-cache");
			conn1.setRequestProperty("Content-Type", "application/json");
			
			if (conn1.getResponseCode() != 200) {
				context.getLogger().log("Error response from getAuthToken");
				throw new RuntimeException("Failed : HTTP error code:: " + conn1.getResponseMessage());
			}
	
			BufferedReader br = new BufferedReader(new InputStreamReader((conn1.getInputStream())));
	
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				result1.append(inputLine);
			}
			context.getLogger().log("GET TOKEN call response: " + result1 +"\n");
			br.close();
			conn1.disconnect();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		context.getLogger().log("******************Time elapsed in GET TOKEN call: " + (endTime - startTime) +"\n");
		return result1.toString();
	}
	
	public String getAuthTokenFromLambda(Context context) {
		
		AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard();
        ByteBuffer payload = null;
        String converted = null;
        try {
        	AWSLambda client = builder.build();
        	InvokeRequest invokeRequest = new InvokeRequest().withFunctionName("GetResponsysAuthToken");
             
        	InvokeResult result = client.invoke(invokeRequest);

            context.getLogger().log("Before Invoke" + "\n");
            payload = result.getPayload();
 	        converted = new String(payload.array(), "UTF-8");

            context.getLogger().log(converted.toString());
            context.getLogger().log("After Invoke:: payload" + converted + "\n");

         } catch (Exception e) {
        	 e.printStackTrace();
         }
		
		return converted;
	}
}
