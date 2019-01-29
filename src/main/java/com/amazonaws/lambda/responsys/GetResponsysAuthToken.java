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

public class GetResponsysAuthToken implements RequestStreamHandler {

	private static final String AUTH_TOKEN_EP = System.getenv("RESPONSYS_AUTH_TOKEN_ENDPOINT");
	private static final String USERNAME = System.getenv("USERNAME");
	private static final String PASSWORD = System.getenv("PASSWORD");
	private static final String AUTH_TYPE = System.getenv("AUTH_TYPE");
	private static final String AUTH_TOKEN_CREDS = (new StringBuffer("user_name=")).append(USERNAME).append("&password=").append(PASSWORD).append("&auth_type=").append(AUTH_TYPE).toString();

	
	//private static final String AUTH_TOKEN_EP = "https://login2.responsys.net/rest/api/v1/auth/token";
	//private static final String AUTH_TOKEN_CREDS = "user_name=loyalty_API&password=Lulu%40lem0n&auth_type=password";

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		try {
			URL url = new URL(AUTH_TOKEN_EP);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("cache-control", "no-cache");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setDoOutput(true);
			conn.getOutputStream().write(AUTH_TOKEN_CREDS.getBytes("UTF-8"));
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code: " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuffer result = new StringBuffer();
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				result.append(inputLine);
			}
			br.close();
			conn.disconnect();
			context.getLogger().log("Response from api call: " + result.toString() +"\n");

			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
			writer.write(result.toString());
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}