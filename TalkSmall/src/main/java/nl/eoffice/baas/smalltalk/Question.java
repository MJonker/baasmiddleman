package nl.eoffice.baas.smalltalk;

import java.util.Map;

import com.ibm.watson.developer_cloud.conversation.v1.model.Context;

public class Question {
	
	public String newQuestion;
	public Context context;
	@Override
	public String toString() {
		return "Question [newQuestion=" + newQuestion + ", context=" + context + "]";
	} 

	
}
