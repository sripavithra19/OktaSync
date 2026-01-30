package com.aegis.portal.model;

import java.time.Instant;

public class OktaEmailFailure {
    private String eventId;
    private Instant eventTime;
    private String eventType;
    private String severity;
    private String userId;
    private String userEmail;
    private String targetUserId;
    private String targetUserEmail;
    private String failureReason;
    private String smtpResponse;
    private String displayMessage;
    private boolean processed;
    
    // Constructors
    public OktaEmailFailure() {}
    
    public OktaEmailFailure(String eventId, Instant eventTime, String eventType, String severity,
                           String userId, String userEmail, String targetUserId, 
                           String targetUserEmail, String failureReason) {
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.eventType = eventType;
        this.severity = severity;
        this.userId = userId;
        this.userEmail = userEmail;
        this.targetUserId = targetUserId;
        this.targetUserEmail = targetUserEmail;
        this.failureReason = failureReason;
        this.processed = false;
    }
    
    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    
    public String getTargetUserEmail() { return targetUserEmail; }
    public void setTargetUserEmail(String targetUserEmail) { this.targetUserEmail = targetUserEmail; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

	public String getSmtpResponse() {
		return smtpResponse;
	}

	public void setSmtpResponse(String smtpResponse) {
		this.smtpResponse = smtpResponse;
	}

	public String getDisplayMessage() {
		return displayMessage;
	}

	public void setDisplayMessage(String displayMessage) {
		this.displayMessage = displayMessage;
	}
}