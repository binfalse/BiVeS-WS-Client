package de.unirostock.sems.bivesWsClient.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.unirostock.sems.bivesWsClient.BivesComparisonRequest;
import de.unirostock.sems.bivesWsClient.BivesComparisonResponse;
import de.unirostock.sems.bivesWsClient.BivesRequest;
import de.unirostock.sems.bivesWsClient.BivesResponse;
import de.unirostock.sems.bivesWsClient.BivesSingleFileRequest;
import de.unirostock.sems.bivesWsClient.BivesSingleFileResponse;
import de.unirostock.sems.bivesWsClient.BivesWs;
import de.unirostock.sems.bivesWsClient.exception.BivesClientException;
import de.unirostock.sems.bivesWsClient.exception.BivesException;

public class HttpBivesClient implements BivesWs {

	private static final String REQUEST_FIELD_FILES = "files";

	private static final String REQUEST_FIELD_COMMANDS = "commands";

	protected String baseUrl;

	protected Gson gson;
	protected HttpClient httpClient;

	public HttpBivesClient( String baseUrl ) {
		this.baseUrl = baseUrl;

		// creates Framework instances
		gson = new Gson();
		httpClient = HttpClientBuilder.create().build();
	}
	
	
	

	protected void performRequest(BivesRequest request, BivesResponse result) throws BivesClientException, BivesException {

		if( request == null || !request.isReady () )
			throw new IllegalArgumentException("The request isn't valid.");

		// generate the Request Parameter
		Map<String, JsonElement> requestJson = new HashMap<String, JsonElement>();
		requestJson.put( REQUEST_FIELD_FILES, gson.toJsonTree(request.getModels()) );
		requestJson.put( REQUEST_FIELD_COMMANDS, gson.toJsonTree(request.getCommands()) );
		String json = gson.toJson(requestJson);

		// prepare http request
		HttpPost httpRequest = new HttpPost( baseUrl );
		// adds the json
		httpRequest.setEntity( new StringEntity(json, ContentType.APPLICATION_JSON) );

		String stringResult = null;
		
		
		
		// Retrieving the Http Response
		try {
			HttpResponse response = httpClient.execute(httpRequest);

			// reads the result
			StringBuilder stringResultBuilder = new StringBuilder();
			BufferedReader resultReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line = "";
			while ((line = resultReader.readLine()) != null) {
				//append              
				stringResultBuilder.append(line);
			}
			stringResult = stringResultBuilder.toString();

		} catch (ClientProtocolException e) {
			throw new BivesClientException("Protocol Exception while fetching content from the server.", e);
		} catch (IOException e) {
			throw new BivesClientException("IO Exception while fetching content from the server.", e);
		}
		
		if( stringResult == null || stringResult.isEmpty() )
			throw new BivesClientException("The result returned from the BiVeS Webservice is empty or null!");
		
		JsonParser parser = new JsonParser();
    JsonObject obj = parser.parse(stringResult).getAsJsonObject ();
    for (Entry<String, JsonElement> entry : obj.entrySet ())
    {
    	String key = entry.getKey ();
    	if (key.equals ("error"))
    	{
    		JsonArray array = entry.getValue ().getAsJsonArray ();
    		for (JsonElement arrayElement : array)
    			result.addError (arrayElement.getAsString ());
    		continue;
    	}
    	result.setResult (key, entry.getValue ().getAsString ());
    }
	}

	@Override
	public BivesSingleFileResponse performRequest (BivesSingleFileRequest request)
		throws BivesClientException,
			BivesException
	{
		BivesSingleFileResponse response = new BivesSingleFileResponse ();
		
		performRequest (request, response);
		response.prostProcess ();
		
		return response;
	}

	@Override
	public BivesComparisonResponse performRequest (BivesComparisonRequest request)
		throws BivesClientException,
			BivesException
	{
		BivesComparisonResponse response = new BivesComparisonResponse ();
		performRequest (request, response);
		response.prostProcess ();
		return response;
	}
	
}
