package nl.eoffice.baas.microsoft.crm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.developer_cloud.conversation.v1.model.WorkspaceExport.Status;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CRMClient {
	
	private static String API_VERSION="v8.2";
	private static String AUTHORITY_PREFIX = "https://login.microsoftonline.com";
	private String clientId;
	private String crmEndpoint;
	private String crmTenantId;
	private String crmUser;
	private String crmUserPassword;
	private String accesstoken;
	private Calendar accessTokenExpiresOn;
	
	
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getCrmEndpoint() {
		return crmEndpoint;
	}
	public void setCrmEndpoint(String crmEndpoint) {
		this.crmEndpoint = crmEndpoint;
	}
	public String getCrmTenantId() {
		return crmTenantId;
	}
	public void setCrmTenantId(String crmTenantId) {
		this.crmTenantId = crmTenantId;
	}
	public String getCrmUser() {
		return crmUser;
	}
	public void setCrmUser(String crmUser) {
		this.crmUser = crmUser;
	}
	public String getCrmUserPassword() {
		return crmUserPassword;
	}
	public void setCrmUserPassword(String crmUserPassword) {
		this.crmUserPassword = crmUserPassword;
	}
	
	public WatsonCredentials getCredentialsFromCRMEntry(String credentialsGUID) throws IOException {
		System.out.println("credentialsGUID: "+credentialsGUID);
		authenticateOldSchool();
		HttpURLConnection connection = null;
        
        String fullOdataURL=crmEndpoint + "/api/data/"+API_VERSION+"/eof_watsoncredentialses("+credentialsGUID+")?$select=eof_username,eof_password";
        System.out.println(fullOdataURL);
        
        URL url = new URL(fullOdataURL);
       
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("OData-MaxVersion", "4.0");
        connection.setRequestProperty("OData-Version", "4.0");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Prefer", "odata.include-annotations=*");
        connection.addRequestProperty("Authorization", "Bearer " + accesstoken);

        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        System.out.println(response);
        
        if (responseCode==500) {
        	throw new IOException("error "+response);
        }
        JsonParser parser = new JsonParser();
        JsonElement jResponse = parser.parse(response.toString());
        JsonObject credentialsFromCrm = jResponse.getAsJsonObject();
        
        WatsonCredentials watsonCredentials = new WatsonCredentials(credentialsFromCrm.get("eof_username").getAsString(), credentialsFromCrm.get("eof_password").getAsString());
        
        return watsonCredentials;
		
		
	}
	
	
	
	public WatsonCredentials getSmallTalkInfo(String botlanguage) throws IOException {
		//eof_watsoncredentialses?$select=eof_username,eof_password,eof_workspaceid&$filter=eof_workspaceid ne null";
	
		authenticateOldSchool();
		
		
		HttpURLConnection connection = null;
        
        String fullOdataURL=crmEndpoint + "/api/data/"+API_VERSION+"/eof_watsoncredentialses?$select=eof_name,eof_username,eof_password,eof_workspaceid&$filter=eof_workspaceid%20ne%20null";
       
        
        URL url = new URL(fullOdataURL);
       
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("OData-MaxVersion", "4.0");
        connection.setRequestProperty("OData-Version", "4.0");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Prefer", "odata.include-annotations=*");
        connection.addRequestProperty("Authorization", "Bearer " + accesstoken);

        int responseCode = connection.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        
        JsonParser parser = new JsonParser();
        JsonElement jResponse = parser.parse(response.toString());
	
		JsonElement valueResponse= jResponse.getAsJsonObject().get("value");
		JsonArray smalltalkCredentialses = valueResponse.getAsJsonArray();
		 WatsonCredentials watsonCredentials = null;
		for (Iterator iterator = smalltalkCredentialses.iterator(); iterator.hasNext();) {
			
			JsonObject credentialsFromCrm = ((JsonElement)iterator.next()).getAsJsonObject(); //should be two (nl en en)
			String name = credentialsFromCrm.get("eof_name").getAsString();
			if (name.endsWith(botlanguage)) {
				watsonCredentials = new WatsonCredentials(credentialsFromCrm.get("eof_username").getAsString(), credentialsFromCrm.get("eof_password").getAsString());
		        watsonCredentials.setWorkspaceid(credentialsFromCrm.get("eof_workspaceid").getAsString());
		        System.out.println("got smalltalk info");
			}
		}
		
       if (watsonCredentials==null) {
    	   throw new IOException("No smalltalk info found for language : "+botlanguage);
       } else {
    	   return watsonCredentials;
       }
}

	/*
	 * for async implementation , result of BAAS-83
	 */
	public void changeChatbotBuilderStatus(String status, String chatbotbuilderGuid,String workspaceid) throws IOException {
		authenticateOldSchool();
		
		HttpURLConnection connection = null;
        
        String fullOdataURL=crmEndpoint + "/api/data/"+API_VERSION+"/eof_chatbotbuilders("+chatbotbuilderGuid+")";
       
        JsonObject chatbotbuilder=new JsonObject();
        if (status.equalsIgnoreCase(Status.TRAINING)) {
        	chatbotbuilder.addProperty("statuscode", 178160006); //Training
        }
        if (status.equalsIgnoreCase(Status.AVAILABLE)) {
        	chatbotbuilder.addProperty("statuscode", 178160001); //
        }
        chatbotbuilder.addProperty("statecode", 0); //Actief
        chatbotbuilder.addProperty("eof_workspaceid",workspaceid);
        
        
        OkHttpClient client = new OkHttpClient.Builder()
        		//.connectTimeout(10, TimeUnit.SECONDS).writeTimeout(30,TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        		.build();
      
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody body = RequestBody.create(mediaType, chatbotbuilder.toString());

        Request request = new Request.Builder()
        .url(fullOdataURL)
        .patch(body)
        .addHeader("content-type", "application/json")
        .addHeader("cache-control", "no-cache")
        .addHeader("OData-MaxVersion", "4.0")
        .addHeader("OData-Version", "4.0")
        .addHeader("Authorization", "Bearer " + accesstoken)
        .build();

        Response response = client.newCall(request).execute();

        
        JsonParser parser = new JsonParser();
        JsonElement jResponse = parser.parse(response.body().string());
	
		System.out.println(jResponse);
		
	}

	
	private synchronized void authenticateOldSchool() throws IOException {
		//logger.fine("getting properties from "+crmInfo);
		//TODO only fetch a new token when it is about to expire
		//USE the refresh token to achieve this.
		
		//check if the token is still valid
		if (accesstoken!=null && Calendar.getInstance().before(accessTokenExpiresOn)) {
			return ;
		} 
			
		//if not, get a new one:
		
		String client_id=URLEncoder.encode(clientId,"UTF-8");
		String tenantid=URLEncoder.encode(crmTenantId,"UTF-8");
		String resource=URLEncoder.encode(crmEndpoint,"UTF-8");
		String username=URLEncoder.encode(crmUser,"UTF-8");
		String password=URLEncoder.encode(crmUserPassword,"UTF-8");
		
		URL obj = new URL(AUTHORITY_PREFIX+"/"+tenantid+"/oauth2/token");
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		//System.out.println(obj);
		//add request header
		con.setRequestMethod("POST");

		
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		
		String urlParameters = "grant_type=password&client_id="+client_id+"&resource="+resource+"&password="+password+"&username="+username+"&scope=openid";
		// Send post request
		System.out.println(urlParameters);
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		//int responseCode = con.getResponseCode();
		
		//System.out.println("Post parameters : " + urlParameters);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		//print result
		String responseContent =response.toString();
		//get the expires_on on store that one as well
		Gson gson = new Gson();
		JsonParser parser = new JsonParser();
		JsonObject authResponse =parser.parse(responseContent).getAsJsonObject();
		String expiresIn = authResponse.get("expires_in").getAsString();
		
		accessTokenExpiresOn = Calendar.getInstance();
		accessTokenExpiresOn.add(Calendar.MILLISECOND, Integer.parseInt(expiresIn));
		
		
		this.accesstoken=authResponse.get("access_token").getAsString();
		
		
        
	}
	
	

}
