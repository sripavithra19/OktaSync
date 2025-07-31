package com.aegis.portal.model;

public class EventsDTO {
	    private String id;
	    private String timestamp;
		private String sessionId;
	    private String profileId;
	    private String email;
	    private String login;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getTimestamp() {
			return timestamp;
		}
		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}
		public String getSessionId() {
			return sessionId;
		}
		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}
		public String getProfileId() {
			return profileId;
		}
		public void setProfileId(String profileId) {
			this.profileId = profileId;
		}
		public String getEmail() {
			return email;
		}
		public void setEmail(String email) {
			this.email = email;
		}
	    public String getLogin() {
			return login;
		}
		public void setLogin(String login) {
			this.login = login;
		}
		@Override
		public String toString() {
			return "EventsDTO [" + (id != null ? "id=" + id + ", " : "")
					+ (timestamp != null ? "timestamp=" + timestamp + ", " : "")
					+ (sessionId != null ? "sessionId=" + sessionId + ", " : "")
					+ (profileId != null ? "profileId=" + profileId + ", " : "")
					+ (email != null ? "email=" + email + ", " : "") + (login != null ? "login=" + login : "") + "]";
		}		
}
