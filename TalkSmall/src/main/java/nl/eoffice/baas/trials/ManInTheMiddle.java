package nl.eoffice.baas.trials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.watson.developer_cloud.conversation.v1.Conversation;
import com.ibm.watson.developer_cloud.conversation.v1.model.GetWorkspaceOptions;
import com.ibm.watson.developer_cloud.conversation.v1.model.InputData;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.conversation.v1.model.WorkspaceExport;
import com.ibm.watson.developer_cloud.conversation.v1.model.WorkspaceExport.Status;

import nl.eoffice.baas.microsoft.crm.CRMClient;
import nl.eoffice.baas.microsoft.crm.WatsonCredentials;

@Path("/baastrials")
public class ManInTheMiddle {
	@Context ServletContext context;
	private static String trialsAllowedOrigin=System.getenv("corsAllowedOrigin");
	//private static Map<String,WatsonCredentials> workspaceCredentialsMap=new HashMap<String, WatsonCredentials>();
	
	
	@GET
	@Path("/")
	@Produces("application/json")
	public Response getInfo() {
		
		Answer answer = new Answer();
		answer.text.add("Version 0.9.1");
		answer.text.add("please use POST to use this api");	
		return Response
	            .status(200)
	            .header("Access-Control-Allow-Origin", trialsAllowedOrigin)
	            .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
	            .header("Access-Control-Allow-Credentials", "true")
	            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
	            .header("Access-Control-Max-Age", "1209600")
	            .entity(answer)
	            .build();
		

    }
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public Response getResponseFromWatson(Question question) {
		System.out.println(question);
		
		try {
			WatsonCredentials watsonCredentials = getCredentialsForWorkspace(question);
	    	Conversation conversationService = new Conversation(Conversation.VERSION_DATE_2017_05_26);
	    	conversationService.setUsernameAndPassword(watsonCredentials.getUsername(), watsonCredentials.getPassword()); //comes from CRM credentialsGUID ref
	    	Answer answer=new Answer(); 
	    	Boolean watsonIsTraining=false;
	    	//als we al een context hebben dan is het trainen dus gebeurd...
	    	if (question.context==null || question.context.isEmpty()) {
	    	   	//aophalen status elke keer eerste keer hier, misschien cachen als het niet meer aan het trainen is ??
	    		GetWorkspaceOptions gwo = new GetWorkspaceOptions.Builder(question.workspaceid).build();
	    	
	    		WorkspaceExport wex = conversationService.getWorkspace(gwo).execute();
	    		String status = wex.getStatus();
	    		if (status.equalsIgnoreCase(Status.TRAINING)) {
	    			List<String> tmpList = new ArrayList<String>();
	    			tmpList.add("baas is training");
	    			answer.text = tmpList;
	    			watsonIsTraining=true;
	    		} 
	    	}
	    	
	    	if (!watsonIsTraining) 
	    	{
	    	
		    	InputData input = new InputData.Builder(question.newQuestion).build();
		    	
		    	MessageOptions options = new MessageOptions.Builder(question.workspaceid).
		    			input(input).
		    			context(question.context).
		    			build();
		    	
		    	MessageResponse mAnswer = conversationService.message(options).execute();    	
		        
		    	

				Object textObject = mAnswer.getOutput().get("text");
		        if (textObject instanceof List<?>) {
		            answer.text = (List<String>) textObject;
		        } else //probably not entered, text is always array
		        {
		            List<String> tmpList = new ArrayList<String>();
		            tmpList.add(textObject.toString());
		            answer.text = tmpList;
		        }
				
				answer.context = mAnswer.getContext();
	    	}
			return Response
		            .status(200)
		            .header("Access-Control-Allow-Origin", trialsAllowedOrigin)
		            .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
		            .header("Access-Control-Allow-Credentials", "true")
		            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
		            .header("Access-Control-Max-Age", "1209600")
		            .entity(answer)
		            .build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response
		            .status(200)
		            .header("Access-Control-Allow-Origin", trialsAllowedOrigin)
		            .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
		            .header("Access-Control-Allow-Credentials", "true")
		            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
		            .header("Access-Control-Max-Age", "1209600")
		            .entity(e.getMessage())
		            .build();
			
		}

    }

	private WatsonCredentials getCredentialsForWorkspace(Question question) throws IOException {
		
		Map<String,WatsonCredentials> workspaceCredentialsMap=(Map<String, WatsonCredentials>) context.getAttribute("workspaceCredentialsMap");
		if(workspaceCredentialsMap==null) {
			workspaceCredentialsMap = new HashMap<String, WatsonCredentials>();
			context.setAttribute("workspaceCredentialsMap", workspaceCredentialsMap);
		}
		WatsonCredentials watsonCredentials;
		if (!workspaceCredentialsMap.containsKey(question.workspaceid)) {
			//communicate with CRM for credentials
			CRMClient crmClient = new CRMClient();
			
			String crmclientid = System.getenv("crmclientid");
			String crmendpoint = System.getenv("crmendpoint");
			String crmtenantid = System.getenv("crmtenantid");
			String crmuser =  System.getenv("crmuser");
			
			String crmuserpassword = "<use your own>"; 
			
			
			crmClient.setClientId(crmclientid);
			crmClient.setCrmEndpoint(crmendpoint);
			crmClient.setCrmUser(crmuser);
			crmClient.setCrmUserPassword(crmuserpassword);
			crmClient.setCrmTenantId(crmtenantid);
			//
			
			watsonCredentials = crmClient.getCredentialsFromCRMEntry(question.credentialsGuid);
			workspaceCredentialsMap.put(question.workspaceid, watsonCredentials);
			
		} else {
			watsonCredentials=workspaceCredentialsMap.get(question.workspaceid);
			System.out.println("credentials from cache");
		}
		return watsonCredentials;
	}
	
	@OPTIONS
	public Response options() {
		return Response
	            .status(200)
	            .header("Access-Control-Allow-Origin", trialsAllowedOrigin)
	            .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
	            .header("Access-Control-Allow-Credentials", "true")
	            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
	            .header("Access-Control-Max-Age", "1209600")
	            .build();
	}
	
	
}
