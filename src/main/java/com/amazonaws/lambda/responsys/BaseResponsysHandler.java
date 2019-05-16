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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.integration.aws.auth.AWS4SignerBase;
import com.integration.aws.auth.AWS4SignerForAuthorizationHeader;
import com.integration.util.BinaryUtils;

/**
 * Class containing the common methods used by Lambda functions
 * @author harshildesai
 *
 */
public abstract class BaseResponsysHandler {
	
	//AWS Login call configs
	private static final String AWS_LOGIN_ENDPOINT = System.getenv("LOGIN_ENDPOINT");
	private static final String SECRET_KEY = System.getenv("LOGIN_SECRET_KEY");
	private static final String ACCESS_KEY = System.getenv("LOGIN_ACCESS_KEY");
	private static final String AWS_REGION = System.getenv("LOGIN_REGION");
	private static final String AWS_SERVICE = System.getenv("LOGIN_SERVICE");

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
	protected static final int errStatusCode = 400;
	
	//CORS headers
	protected static final String HEADERS = "headers";
	protected static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	protected static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	protected static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	
	public String getAuthTokenAPI(LambdaLogger logger, boolean isInvalidateCache) throws IOException {
		
		long startTime = System.currentTimeMillis();
		StringBuffer result = new StringBuffer();
		HttpPost postMethod = new HttpPost(AWS_LOGIN_ENDPOINT);
		String payload = "";

		//adding headers content-type and content-length
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(CONTENT_TYPE, APPLICATION_JSON);
		
		if(isInvalidateCache)
			headers.put(CACHE_CONTROL, "max-age=0");
		
		//generating Hex code of payload
		byte[] contentHash = AWS4SignerBase.hash(payload);
		String contentHashString = BinaryUtils.toHex(contentHash);

		//initialize signer
		AWS4SignerForAuthorizationHeader signer = new AWS4SignerForAuthorizationHeader(new URL(AWS_LOGIN_ENDPOINT), POST, AWS_SERVICE, AWS_REGION);

		//calculate authorization header
		String authorization = signer.computeSignature(headers, null, // no query parameters
				contentHashString, ACCESS_KEY, SECRET_KEY);
		
		//add authorization header to request
		headers.put(AUTHORIZATION, authorization);
		
		// Setting headers for the service.
		if (headers != null) {	            
            for ( String headerKey : headers.keySet() ) {	               
            	postMethod.addHeader(headerKey, headers.get(headerKey));
            }
        }
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpResponse httpResponse = httpClient.execute(postMethod);
		
		int respCode = httpResponse.getStatusLine().getStatusCode();
		
		BufferedReader respReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

		String line = null;
		while ((line = respReader.readLine()) != null) {
			result.append(line);
		}
		
		if(HttpStatus.SC_OK != respCode) {
			logger.log("Error Response from api call: " + result.toString() + " ErrorCode: " + respCode + NEW_LINE);
			
			if(!isInvalidateCache) {
				logger.log("Failed retrieving the AUTH token from Login API in FIRST attempt, retrying" + NEW_LINE);
				getAuthTokenAPI(logger, true);
				
			} else {
				logger.log("Failed retrieving the AUTH token from Login API in SECOND attempt" + NEW_LINE);
				return null;
			}
			
		} else {
			logger.log("Successfully retrieved the AUTHTOKEN: " + result.toString() + NEW_LINE);
		}
	
		long endTime = System.currentTimeMillis();
		logger.log("******************Time elapsed in GET AUTH TOKEN call: " + (endTime - startTime) + NEW_LINE);
		return result.toString();
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
		
		//Adding CORS headers
		JSONObject headerJson = new JSONObject();
		headerJson.put(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		headerJson.put(ACCESS_CONTROL_ALLOW_HEADERS, "*");
		headerJson.put(ACCESS_CONTROL_ALLOW_METHODS, "*");
		
		JSONObject responseJson = new JSONObject();
		responseJson.put(IS_BASE64_ENCODED, false);
		responseJson.put(STATUS_CODE, "200");
		responseJson.put(HEADERS, headerJson);
		responseJson.put(BODY, bodyContent);
		return responseJson;
	}
}
