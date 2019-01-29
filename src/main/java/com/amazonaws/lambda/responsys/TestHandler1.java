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
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.Deserializers.Base;

public class TestHandler1 implements RequestStreamHandler {

	private static final String ADD_MEMBER_EP = "/rest/api/v1/lists/CONTACTS_LIST/members";

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		context.getLogger().log("-----in original lamdba1-------");
        
		Regions region = Regions.fromName("us-east-1");
		
		ByteBuffer payload = null;
         try {
        	 
        	 AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard();
        	 AWSLambda client = builder.build();
        	 InvokeRequest invokeRequest = new InvokeRequest().withFunctionName("TestHandler2");
             
             context.getLogger().log("Before Invoke" + "\n");
             InvokeResult result = client.invoke(invokeRequest);

             
             payload = result.getPayload();
             context.getLogger().log("After Inoke" + "\n");
             String converted = new String(payload.array(), "UTF-8");
             context.getLogger().log(converted);
             context.getLogger().log("After Payload logger-----" + "\n");

         } catch (Exception e) {
             // TODO: handle exception
         }
		
	}
	
}