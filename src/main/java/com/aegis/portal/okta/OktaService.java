package com.aegis.portal.okta;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.OktaEmailFailure;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.util.IConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OktaService {
    private String oktaEnv;
    private String oktaURL;
    private String tokenCredentials; 
    private String OktaAppID;

    private static Logger logger = LoggerFactory.getLogger(OktaService.class);

    public OktaService(String oktaEnv) {
        this.oktaEnv = oktaEnv;
        try {
			initializeConfiguration();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // Initialize on construction
    }

    private void initializeConfiguration() throws Exception {
        ResourceBundle resBundle = ResourceBundle.getBundle("oktaConfiguration");
        if (resBundle == null)
            throw new Exception("Missing OktaConfiguration Properties");

        oktaURL = resBundle.getString(oktaEnv + IConstants.PERIOD + "OrgUrl");
        if (oktaURL == null || oktaURL.equals("null") || oktaURL.isEmpty())
            throw new Exception("Error Initializing Okta Instance: OrgUrl is missing in oktaConfiguration.properties");

        // Clean the token - remove whitespace, newlines, etc.
        String rawToken = resBundle.getString(oktaEnv + IConstants.PERIOD + "TokenClientCredentials");
        if (rawToken == null || rawToken.equals("null") || rawToken.trim().isEmpty())
            throw new Exception("Error Initializing Okta Instance: TokenClientCredentials is missing in oktaConfiguration.properties");
        
        // Clean the token
        tokenCredentials = cleanToken(rawToken);
        logger.info("Cleaned token length: {}", tokenCredentials.length());
        logger.info("Token first 10 chars: {}", tokenCredentials.substring(0, Math.min(10, tokenCredentials.length())));

        OktaAppID = resBundle.getString(oktaEnv + IConstants.PERIOD + "AEGISlinkAppID");
        if (OktaAppID == null || OktaAppID.equals("null") || OktaAppID.isEmpty())
            throw new Exception("Error Initializing Okta Instance: AEGISlinkAppID is missing in oktaConfiguration.properties");

        logger.info("Okta Configuration - URL: {}, AppID: {}", oktaURL, OktaAppID);
    }

    private String cleanToken(String token) {
        if (token == null) return null;
        
        // Remove all whitespace (spaces, tabs, newlines)
        String cleaned = token.trim()
                             .replaceAll("\\s+", "")
                             .replaceAll("\\r", "")
                             .replaceAll("\\n", "");
        
        // Debug logging
        logger.debug("Original token: '{}'", token);
        logger.debug("Cleaned token: '{}'", cleaned);
        logger.debug("Original length: {}, Cleaned length: {}", token.length(), cleaned.length());
        
        return cleaned;
    }
    // Helper method for all HTTP calls
    private JsonNode makeOktaApiCall(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "SSWS " + tokenCredentials);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.toString());
            
        } else {
            // Read error response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            
            logger.error("Okta API Error {}: {}", responseCode, errorResponse.toString());
            throw new Exception("Okta API Error: " + responseCode + " - " + errorResponse.toString());
        }
    }

    public List<PortalUser> fetchAllOktaUsers() {
        List<PortalUser> userList = new ArrayList<>();

        try {
            String urlStr = oktaURL + "/api/v1/users?limit=200";
            
            JsonNode users = makeOktaApiCall(urlStr);
            if (users != null && users.isArray()) {
                for (JsonNode user : users) {
                    PortalUser pUser = new PortalUser();
                    JsonNode profile = user.path("profile");
                    
                    pUser.setLogin(profile.path("login").asText());
                    pUser.setEmail(profile.path("email").asText());
                    pUser.setFirstName(profile.path("firstName").asText());
                    pUser.setLastName(profile.path("lastName").asText());
                    pUser.setUsrID(user.path("id").asText());
                    
                    userList.add(pUser);
                }
            }
            
            logger.info("Fetched {} users from Okta", userList.size());
            
        } catch (Exception e) {
            logger.error("Error fetching users from Okta: {}", e.getMessage(), e);
        }

        return userList;
    }

    public List<String> getUsersLoggedInSince(String sinceDateTime) {
        List<String> emailList = new ArrayList<>();
        try {
            // Convert lastSyncDate to ISO 8601 format
            Instant instant = LocalDateTime.parse(sinceDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault()).toInstant();
            String isoTime = instant.toString();

            String filter = "lastLogin ge \"" + isoTime + "\"";
            String encodedFilter = URLEncoder.encode(filter, "UTF-8");
            
            String urlStr = oktaURL + "/api/v1/users?filter=" + encodedFilter;
            logger.info("Using filter: {}", filter);

            JsonNode users = makeOktaApiCall(urlStr);
            if (users != null && users.isArray()) {
                for (JsonNode user : users) {
                    emailList.add(user.path("profile").path("email").asText());
                }
            }

        } catch (Exception e) {
            logger.error("Error fetching users by login timestamp from Okta: {}", e.getMessage(), e);
        }

        return emailList;
    }

    public List<EventsDTO> fetchEvents(String sinceDateTime) {
        List<EventsDTO> eventsList = new ArrayList<>();

        try {
            // Convert to proper format
            String isoUtcSince = sinceDateTime;
            if (!sinceDateTime.contains("T")) {
                Instant instant = Instant.parse(sinceDateTime);
                isoUtcSince = instant.toString();
            }

            // Prepare the filter and API URL
            String filter = "eventType eq \"user.session.start\" or eventType eq \"user.session.end\" "
                    + "or eventType eq \"user.account.activated\" or eventType eq \"user.account.deactivated\"";
            String encodedFilter = URLEncoder.encode(filter, "UTF-8");
            String encodedSince = URLEncoder.encode(isoUtcSince, "UTF-8");
            String urlStr = oktaURL + "/api/v1/logs?filter=" + encodedFilter + "&since=" + encodedSince;

            // Make the HTTP request using common method
            JsonNode events = makeOktaApiCall(urlStr);

            // Process JSON events
            for (JsonNode event : events) {
                String eventType = event.path("eventType").asText();
                String mappedType = mapOktaEventType(eventType);
                if (mappedType == null) continue;

                EventsDTO eventDto = new EventsDTO();
                eventDto.setId(UUID.randomUUID().toString());
                
                // Parse timestamp
                String published = event.path("published").asText();
                Instant eventInstant = Instant.parse(published);
                LocalDateTime dateTime = LocalDateTime.ofInstant(eventInstant, ZoneId.systemDefault());
                String formattedTimestamp = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                eventDto.setTimestamp(formattedTimestamp);
                
                eventDto.setSessionId(event.path("authenticationContext").path("externalSessionId").asText(""));
                eventDto.setProfileId(event.path("actor").path("alternateId").asText());

                eventsList.add(eventDto);
            }

            logger.info("Fetched {} events from Okta", eventsList.size());

        } catch (Exception e) {
            logger.error("Error fetching events from Okta: {}", e.getMessage(), e);
        }

        return eventsList;
    }

    private String mapOktaEventType(String eventType) {
        switch (eventType) {
        case "user.session.start":
            return "login";
        case "user.session.end":
            return "logout";
        case "user.account.activated":
            return "activation";
        case "user.account.deactivated":
            return "deactivation";
        default:
            return null;
        }
    }

    public List<OktaEmailFailure> fetchEmailFailureEvents(String sinceDateTime) {
        List<OktaEmailFailure> failures = new ArrayList<>();
        
        try {
            // Convert sinceDateTime to ISO format if needed
            String isoUtcSince = sinceDateTime;
            if (!sinceDateTime.contains("T")) {
                Instant instant = Instant.parse(sinceDateTime);
                isoUtcSince = instant.toString();
            }
            
            // Get ALL email delivery events (both success and failure)
            // We'll filter by outcome in the parsing
            String filter = "eventType eq \"system.email.delivery\"";
            String encodedFilter = URLEncoder.encode(filter, "UTF-8");
            String encodedSince = URLEncoder.encode(isoUtcSince, "UTF-8");
            String urlStr = oktaURL + "/api/v1/logs?filter=" + encodedFilter + "&since=" + encodedSince;
            
            logger.info("Fetching email delivery events with filter: {}", filter);
            
            // Make the HTTP request using common method
            JsonNode events = makeOktaApiCall(urlStr);
            
            logger.debug("Raw events count: {}", events.size());
            
            for (JsonNode event : events) {
                OktaEmailFailure failure = parseEmailFailureEvent(event);
                if (failure != null) {
                    failures.add(failure);
                }
            }
            
            logger.info("Found {} email failure/bounce events from Okta", failures.size());
            
        } catch (Exception e) {
            logger.error("Error fetching email failure events from Okta: {}", e.getMessage(), e);
        }
        
        return failures;
    }

    private OktaEmailFailure parseEmailFailureEvent(JsonNode event) {
        try {
            // Check if this is a FAILURE event
            JsonNode outcomeNode = event.path("outcome");
            String outcomeResult = outcomeNode.path("result").asText(null);
            
            // Skip if it's not a failure/bounce
            if (!"FAILURE".equalsIgnoreCase(outcomeResult)) {
                return null;
            }
            
            OktaEmailFailure failure = new OktaEmailFailure();
            
            // Basic event info
            failure.setEventId(event.path("uuid").asText());
            failure.setEventTime(Instant.parse(event.path("published").asText()));
            failure.setEventType(event.path("eventType").asText());
            failure.setSeverity(event.path("severity").asText());
            failure.setDisplayMessage(event.path("displayMessage").asText(null));
            
            // EXTRACT EMAIL FROM TARGET ARRAY
            String email = null;
            JsonNode targetNode = event.path("target");
            
            if (targetNode.isArray() && targetNode.size() > 0) {
                JsonNode firstTarget = targetNode.get(0);
                email = firstTarget.path("displayName").asText(null);
                if (email == null || email.isEmpty()) {
                    email = firstTarget.path("id").asText(null);
                }
            }
            
            failure.setTargetUserEmail(email);
            
            // EXTRACT FAILURE REASON (NOT HARDCODED)
            String failureReason = extractFailureReason(event);
            failure.setFailureReason(failureReason);
            
            logger.info("Parsed bounce event - Email: {}, Reason: {}, EventID: {}", 
                email != null ? email : "NULL", 
                failureReason,
                failure.getEventId());
            
            return failure;
            
        } catch (Exception e) {
            logger.error("Error parsing email failure event: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractFailureReason(JsonNode event) {
        String failureReason = "Unknown failure";
        
        try {
            // 1. First check displayMessage (may contain "FAILURE: bounce")
            String displayMessage = event.path("displayMessage").asText(null);
            if (displayMessage != null && displayMessage.contains("FAILURE:")) {
                // Extract the part after "FAILURE: "
                int failureIndex = displayMessage.indexOf("FAILURE:");
                if (failureIndex != -1) {
                    failureReason = displayMessage.substring(failureIndex + 8).trim();
                }
            }
            
            // 2. Check outcome.reason field
            JsonNode outcomeNode = event.path("outcome");
            String outcomeReason = outcomeNode.path("reason").asText(null);
            if (outcomeReason != null && !outcomeReason.isEmpty()) {
                failureReason = outcomeReason;
            }
            
            // 3. Check debugContext for detailed error information
            JsonNode debugContext = event.path("debugContext");
            if (!debugContext.isMissingNode()) {
                JsonNode debugData = debugContext.path("debugData");
                if (debugData.isObject()) {
                    // Try multiple possible field names for failure details
                    String bounceReason = debugData.path("bounceReason").asText(null);
                    if (bounceReason != null && !bounceReason.isEmpty()) {
                        failureReason = "Bounce: " + bounceReason;
                    }
                    
                    String error = debugData.path("error").asText(null);
                    if (error != null && !error.isEmpty()) {
                        failureReason = error;
                    }
                    
                    String failureMsg = debugData.path("failure").asText(null);
                    if (failureMsg != null && !failureMsg.isEmpty()) {
                        failureReason = failureMsg;
                    }
                    
                    // Check SMTP response for technical details
                    String smtpResponse = debugData.path("smtpResponse").asText(null);
                    if (smtpResponse != null && !smtpResponse.isEmpty()) {
                        // Append SMTP response to the reason
                        if (!failureReason.contains("SMTP")) {
                            failureReason += " (SMTP: " + smtpResponse + ")";
                        }
                    }
                }
            }
            
            // 4. Check severity field for clues
            String severity = event.path("severity").asText(null);
            if (severity != null && !severity.isEmpty()) {
                failureReason += " [" + severity + "]";
            }
            
            // 5. If still generic, check eventType
            if (failureReason.equals("Unknown failure")) {
                String eventType = event.path("eventType").asText("");
                if (eventType.equals("system.email.delivery")) {
                    failureReason = "Email delivery failed";
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting failure reason: {}", e.getMessage());
        }
        
        return failureReason;
    }
}