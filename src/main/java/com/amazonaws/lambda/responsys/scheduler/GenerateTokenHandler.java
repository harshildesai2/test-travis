package com.amazonaws.lambda.responsys.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONObject;

import com.amazonaws.lambda.responsys.BaseResponsysHandler;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * Lambda scheduler to generate the AUTH token
 *
 */
public class GenerateTokenHandler extends BaseResponsysHandler implements RequestStreamHandler {

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();
		logger.log("Invoking the generate AUTH token scheduler at UTC time: " + java.time.Clock.systemUTC().instant().toString() + NEW_LINE);
		
    	try {
    		
    		//make AUTH TOKEN call
    		String apiResponse = getAuthToken(logger);
    		
    		if(null == apiResponse || apiResponse.length() < 1) {
    			logger.log("Failed retrieving the AUTH token first time, hence retrying" + NEW_LINE);
    			
    			//retrying AUTH TOKEN call
    			apiResponse = getAuthToken(logger);
    			
    			if(null == apiResponse || apiResponse.length() < 1) {
        			logger.log("Failed retrieving the AUTH token" + NEW_LINE);
        			return;
    			}
    		} else {
    			logger.log("successful response received from AuthToken API call: " + NEW_LINE);
    		}
    		
    		//parse AUTH TOKEN api response
    		JSONObject responseJson = new JSONObject(apiResponse);
    		
    		//Adding the AUTH Token response in DynamoDB
    		if(responseJson != null) {
    			PutItemOutcome outcome = insertTokenInDB(getDynamoDB(), responseJson);
				logger.log("Put item outcome: " + outcome + NEW_LINE);
    		}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return;
    }
}