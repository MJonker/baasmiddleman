package nl.eoffice.baas.trials;

import java.util.Map;

import com.ibm.watson.developer_cloud.conversation.v1.model.Context;

public class Question {
	
	public String newQuestion;
	public Context context;
	public String workspaceid;
	public String credentialsGuid;
	@Override
	public String toString() {
		return "Question [newQuestion=" + newQuestion + ", credentialsGuid=" + credentialsGuid + ", workspaceid=" + workspaceid +"]";
	} 

	
}
