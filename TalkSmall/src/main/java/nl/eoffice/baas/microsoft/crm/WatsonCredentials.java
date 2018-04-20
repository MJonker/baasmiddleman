package nl.eoffice.baas.microsoft.crm;

public class WatsonCredentials {
	private String username;
	private String password;
	private String workspaceid;
	
	public String getWorkspaceid() {
		return workspaceid;
	}
	public void setWorkspaceid(String workspaceid) {
		this.workspaceid = workspaceid;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
	public WatsonCredentials(String username, String password) {
		super();
		this.username = username;
		this.password = password;
	}

}
