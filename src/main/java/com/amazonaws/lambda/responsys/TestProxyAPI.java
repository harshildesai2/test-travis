package com.amazonaws.lambda.responsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class TestProxyAPI implements RequestStreamHandler {

	private static final String ADD_MEMBER_EP = "/rest/api/v1/lists/CONTACTS_LIST/members";
	
	private static final String stringVar = System.getenv("json");

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		
		try {
			JSONObject jo = new JSONObject(stringVar);
			context.getLogger().log("JSONObject updated---------:" + jo + "\n");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		/*JSONParser parser = new JSONParser();
		context.getLogger().log("inputStream---------:" + inputStream + "\n");
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			JSONObject event = (JSONObject) parser.parse(reader);
			context.getLogger().log("event body---------:" + event.get("body") + "\n");
			
			Object payload = parser.parse((String)event.get("body"));
			String emailid = null;
			
			if(payload != null && payload instanceof JSONObject) {
				JSONObject subscriberData = (JSONObject) payload;
				
				if(subscriberData.containsKey("emailid")) {
					emailid = (String) subscriberData.get("emailid");
				}
			}
			
			context.getLogger().log("event emailid param---------:" + emailid + "\n");
			
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
			writer.write(event.toString());
			writer.close();
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		context.getLogger().log("=========in lamdba2=========" + "\n");
		
		JSONObject responseJson = new JSONObject();
		responseJson.put("message", "Lambda2");
		*/
		
	}
	
}