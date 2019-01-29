package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

public class GetSubscriberInfoHandler extends BaseResponsysHandler implements RequestStreamHandler{

	private static final String AUTH_TOKEN_EP = "https://sdt3b6k380.execute-api.us-east-1.amazonaws.com/qa-environment/authtoken";
	private static final String GET_MEMBER_EP = "/rest/api/v1/lists/CONTACTS_LIST/members/";
	private static final String FIELDS_PARAM = "&fs=EMAIL_ADDRESS_,COUNTRY_,PRODUCT_GENDER,PRODUCT_ACTIVITIES";
	
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

    	
    	StringBuffer result2 = new StringBuffer("");
    	int statusCode = 200;
    	ObjectMapper mapper = new ObjectMapper();
    	OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    	
    	try {
    		//make AUTH TOKEN call
    		String result1 = getAuthTokenFromLambda(context);
    		//parse AUTH TOKEN api response
    		context.getLogger().log("response receivied from AUTH lambda: " + result1 +"\n");
    		JsonNode authTokenResp = mapper.readTree(result1);
			String apiHost = authTokenResp.get("endPoint").asText();
			String apiAuthToken = authTokenResp.get("authToken").asText();
			context.getLogger().log("apiAuthToken: " + apiAuthToken +"\n");
			//get request param "emailid"
			JsonNode json = mapper.readTree(inputStream);
			context.getLogger().log("Email ID passed in request: " + json.toString() +"\n");
			String emailLookup = json.path("id").asText();
			
			if( emailLookup != null && emailLookup.length() > 0) {
				long startTime = System.currentTimeMillis();
				StringBuffer getSubscriberUrl = new StringBuffer();
				URL getSubsciberUrl = new URL((getSubscriberUrl.append(apiHost).append(GET_MEMBER_EP).append("?qa=e&id=").append(emailLookup).append(FIELDS_PARAM)).toString());
				
				context.getLogger().log("Calling API GETSUBSCIBER endpoint: " + getSubsciberUrl +"\n");
				
				HttpURLConnection conn2 = (HttpURLConnection) getSubsciberUrl.openConnection();
				conn2.setRequestMethod("GET");
				conn2.setRequestProperty("Content-Type", "application/json");
				conn2.setRequestProperty("Authorization", apiAuthToken);
				conn2.setDoOutput(true);
				
				BufferedReader br = new BufferedReader(new InputStreamReader((conn2.getInputStream())));
	
				long endTime = System.currentTimeMillis();
				context.getLogger().log("******************Time elapsed in GET SUBSCRIBER call: " + (endTime - startTime) +"\n");
				
				String inputLine2;
				while ((inputLine2 = br.readLine()) != null) {
					result2.append(inputLine2);
				}
				br.close();
				conn2.disconnect();
				context.getLogger().log("Response from api call: " + result2.toString() +"\n");
				
				if (conn2.getResponseCode() != 200) {
					statusCode = conn2.getResponseCode();
					throw new RuntimeException("Failed : HTTP error code: " + conn2.getResponseCode());
				}
				
				writer.write(getSucessResponse(result2.toString()).toString());  
		        
			} else {
				context.getLogger().log("Missing the request param \n");
				writer.write(getErrorResponse(400, "Bad Request").toString()); 
			}
		
		}catch (Exception e) {
			e.printStackTrace();
			writer.write(getErrorResponse(statusCode, result2.toString()).toString()); 
		}
    	 writer.close();
    }
	
}