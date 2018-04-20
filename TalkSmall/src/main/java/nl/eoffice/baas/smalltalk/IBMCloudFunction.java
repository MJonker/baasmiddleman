package nl.eoffice.baas.smalltalk;


import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.watson.developer_cloud.conversation.v1.Conversation;
import com.ibm.watson.developer_cloud.conversation.v1.model.Context;
import com.ibm.watson.developer_cloud.conversation.v1.model.InputData;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.conversation.v1.model.OutputData;
public class IBMCloudFunction {
	
	private static String userName = "<use your own>";
	private static String password = "<use your own>";
	private static String workspaceId = "<use your own>";
	
    public static JsonObject main(JsonObject args) {
    	
    	
    	Gson gson = new GsonBuilder().create();
    	Question question = gson.fromJson(args, Question.class);
    	
    	
    	Conversation conversationService = new Conversation(Conversation.VERSION_DATE_2017_05_26);
    	conversationService.setUsernameAndPassword(userName, password); //should not be necessary, the binding is done at server configuration level

    	InputData input = new InputData.Builder(question.newQuestion).build();
    	
    	MessageOptions options = new MessageOptions.Builder(workspaceId).
    			input(input).
    			context(question.context).
    			build();
    	
    	MessageResponse response = conversationService.message(options).execute();    	
        JsonObject responseJSON = generateSmalltalkResponse(response);
                
        return responseJSON;
    }

	private static JsonObject generateSmalltalkResponse(MessageResponse response) {
		//Watson stuff
		
		Context context = response.getContext();
		Gson gson = new GsonBuilder().create();
		
		OutputData output = response.getOutput();
		
		//output.get("text");
		List<String> responses = output.getText();
		
		Answer answer = new Answer();
		answer.context=context;
		answer.text=responses;
		
		
		//AnswerForWebClients
		
		JsonElement answerJSON =gson.toJsonTree(answer);
		
		//building expectedInputs
		//IBM Functions need a body
		JsonObject responseJSON = new JsonObject();
		responseJSON.add("body",answerJSON );
		
		return responseJSON;
	}
}