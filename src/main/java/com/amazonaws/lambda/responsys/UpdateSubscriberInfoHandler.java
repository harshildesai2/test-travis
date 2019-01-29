package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.Deserializers.Base;

public class UpdateSubscriberInfoHandler extends BaseResponsysHandler implements RequestStreamHandler {

	private static final String ADD_MEMBER_EP = "/rest/api/v1/lists/CONTACTS_LIST/members";

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		StringBuffer result2 = new StringBuffer("");
    	ObjectMapper mapper = new ObjectMapper();
    	int statusCode = 200;
    	OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

		try {

			//make AUTH TOKEN call
    		String result1 = getAuthTokenFromLambda(context);
    		//parse AUTH TOKEN api response
    		JsonNode authTokenResp = mapper.readTree(result1);
			String apiHost = authTokenResp.get("endPoint").asText();
			String apiAuthToken = authTokenResp.get("authToken").asText();
			
			String input = IOUtils.toString(inputStream);
			URL subcUrl = new URL(apiHost + ADD_MEMBER_EP);
			HttpURLConnection conn2 = (HttpURLConnection) subcUrl.openConnection();
			conn2.setRequestMethod("POST");
			conn2.setRequestProperty("Content-Type", "application/json");
			conn2.setRequestProperty("Authorization", apiAuthToken);
			conn2.setDoOutput(true);
			conn2.getOutputStream().write(input.getBytes("UTF-8"));

			BufferedReader br = new BufferedReader(new InputStreamReader((conn2.getInputStream())));

			String inputLine2;
			while ((inputLine2 = br.readLine()) != null) {
				result2.append(inputLine2);
			}
			br.close();
			conn2.disconnect();
			
			if (conn2.getResponseCode() != 200) {
				statusCode = conn2.getResponseCode();
				throw new RuntimeException("Failed : HTTP error code: " + conn2.getResponseCode());
			}
			
			context.getLogger().log("Response from api call: " + result2.toString() +"\n");
			
			writer.write(getSucessResponse(result2.toString()).toString());  

		} catch (Exception e) {
			e.printStackTrace();
			writer.write(getErrorResponse(statusCode, result2.toString()).toString()); 
		}
		writer.close();
	}
	
}