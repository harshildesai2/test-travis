package com.amazonaws.lambda.responsys.scheduler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.amazonaws.lambda.responsys.BaseResponsysHandler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
/**
 * Lambda to get the subscriber info based on the emailId passed by the guest 
 *
 */
public class GenerateTokenScheduler extends BaseResponsysHandler implements RequestHandler<ScheduledEvent, String> {

	@Override
	public String handleRequest(ScheduledEvent event, Context context) {
	
		
		LambdaLogger logger = context.getLogger();
		logger.log("---------time of scheduler call: " + event.getTime());
		
    	HttpURLConnection urlConnection = null;
    	
    	try {
    		
    		//make AUTH TOKEN call
    		String apiResponse = getAuthToken(logger);
    		logger.log("response receivied from AuthToken API call: " + apiResponse + NEW_LINE);
    		
    		if(null == apiResponse || apiResponse.length() < 1) {
    			logger.log("Failed retrieving the AUTH token \n");
				return null;
    		}
    		
    		//parse AUTH TOKEN api response
    		JSONObject responseJson = new JSONObject(apiResponse);
    		
    		if(responseJson != null) {
    			
				AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
				DynamoDB dynamoDB = new DynamoDB(client);
				
				Table table = dynamoDB.getTable("subscriber-mgt-data");

				String currentUTCTime = java.time.Clock.systemUTC().instant().toString();
				
				Item item = new Item().withPrimaryKey("authToken", (String) responseJson.get("authToken"));
				item.with("endPoint",(String) responseJson.get("endPoint"));
				item.with("issuedAt",(String) responseJson.get("issuedAt"));
				item.with("date", currentUTCTime);
				PutItemOutcome outcome = table.putItem(item);
				logger.log("outcome: " + outcome);
    				
    		}
			
		
		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			if(urlConnection != null)
				urlConnection.disconnect();
		}
    	
    	return null;
    }
	
	
	
	private static Map<String, String> getAuthTokenResponsys() {
		
		System.out.println("--------getting AUTHTOKEN-----------");
		String apiResponse;
		try {
			apiResponse = getAuthToken();
		
		
			if(null == apiResponse || apiResponse.length() < 1) {
				return null;
			}
			
			//parse AUTH TOKEN api response
			JSONObject responseJson = new JSONObject(apiResponse);
			String apiHost = responseJson.get(END_POINT).toString();
			String apiAuthToken = responseJson.get(AUTH_TOKEN).toString();
			
			if(apiHost != null && apiAuthToken != null) {
				Map<String, String> responsysMap = new HashMap<String, String>();
				responsysMap.put("apiHost", apiHost);
				responsysMap.put("apiAuthToken", apiAuthToken);
				return responsysMap;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}