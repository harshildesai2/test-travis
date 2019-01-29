package com.amazonaws.lambda.responsys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.simple.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class TestHandler2 implements RequestStreamHandler {

	private static final String ADD_MEMBER_EP = "/rest/api/v1/lists/CONTACTS_LIST/members";

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		context.getLogger().log("=========in lamdba2=========" + "\n");
		
		JSONObject responseJson = new JSONObject();
		responseJson.put("message", "Lambda2");
		
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		context.getLogger().log("lambda2 response" + responseJson.toJSONString() + "\n");
		writer.write(responseJson.toJSONString());
		writer.close();
	}
	
}