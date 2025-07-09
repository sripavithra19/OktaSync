package com.aegis.portal.okta;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.util.IConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.user.User;
import com.okta.sdk.resource.user.UserList;

public class OktaService {

	private String oktaEnv;
	private String oktaURL;
	private String tokenCredentials;
	private String OktaAppID;

	private static Logger logger = LoggerFactory.getLogger(OktaService.class);

	public OktaService(String oktaEnv) {
		this.oktaEnv = oktaEnv;
	}

	public Client getClient(String oktaEnv) throws Exception {

		ResourceBundle resBundle = ResourceBundle.getBundle("oktaConfiguration");
		if (resBundle == null)
			throw new Exception("Missing OktaConfiguration Properties");

		oktaURL = resBundle.getString(oktaEnv + IConstants.PERIOD + "OrgUrl");
		if (oktaURL == null || oktaURL.equals("null") || oktaURL.isEmpty())
			throw new Exception("Error Initializing Okta Instance: OrgUrl is missing in oktaConfiguration.properties");

		tokenCredentials = resBundle.getString(oktaEnv + IConstants.PERIOD + "TokenClientCredentials");
		if (tokenCredentials == null || tokenCredentials.equals("null") || tokenCredentials.isEmpty())
			throw new Exception(
					"Error Initializing Okta Instance: TokenClientCredentials is missing in oktaConfiguration.properties");

		OktaAppID = resBundle.getString(oktaEnv + IConstants.PERIOD + "AEGISlinkAppID");
		if (OktaAppID == null || OktaAppID.equals("null") || OktaAppID.isEmpty())
			throw new Exception(
					"Error Initializing Okta Instance: AEGISlinkAppID is missing in oktaConfiguration.properties");

		logger.info("oktaURL = {}, TokenCredentials = {}, AEGISlinkAppID = {}", oktaURL, tokenCredentials, OktaAppID);

		return Clients.builder().setOrgUrl(oktaURL).setClientCredentials(new TokenClientCredentials(tokenCredentials))
				.build();
	}

	public List<PortalUser> fetchAllOktaUsers() {
		List<PortalUser> userList = new ArrayList<>();

		try {
			Client client = getClient(oktaEnv);

			UserList users = client.listUsers();

			for (User user : users) {
				PortalUser pUser = new PortalUser();
				pUser.setLogin(user.getProfile().getLogin());
				pUser.setEmail(user.getProfile().getEmail());
				pUser.setFirstName(user.getProfile().getFirstName());
				pUser.setLastName(user.getProfile().getLastName());
				userList.add(pUser);
			}

		} catch (Exception e) {
			logger.error("Error fetching users from Okta: {}", e.getMessage());
		}

		return userList;
	}

	public List<String> getUsersLoggedInSince(String sinceDateTime) {
		List<String> emailList = new ArrayList<>();
		try {
			Client client = getClient(oktaEnv);

			// Convert lastSyncDate to ISO 8601 format
			Instant instant = LocalDateTime.parse(sinceDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
					.atZone(ZoneId.systemDefault()).toInstant();
			String isoTime = instant.toString();

			String filter = "lastLogin ge \"" + isoTime + "\"";
			logger.info("Using filter: {}", filter);

			UserList users = client.listUsers(null, null, filter, null, null);
			for (User user : users) {
				emailList.add(user.getProfile().getEmail());
			}

		} catch (Exception e) {
			logger.error("Error fetching users by login timestamp from Okta: {}", e.getMessage());
		}

		return emailList;
	}

	public List<EventsDTO> fetchEvents(String sinceDateTime) {
		List<EventsDTO> eventsList = new ArrayList<>();

		try {

			Instant instant = Instant.parse(sinceDateTime);
			String isoUtcSince = instant.toString();

			// Step 2: Prepare the filter and API URL
			String filter = "eventType eq \"user.session.start\" or eventType eq \"user.session.end\" "
					+ "or eventType eq \"user.account.activated\" or eventType eq \"user.account.deactivated\"";
			String encodedFilter = URLEncoder.encode(filter, "UTF-8");
			String urlStr = oktaURL + "/api/v1/logs?filter=" + encodedFilter + "&since="
					+ URLEncoder.encode(isoUtcSince, "UTF-8");

			// Step 3: Make the HTTP request
			URL url = URI.create(urlStr).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "SSWS " + tokenCredentials);
			conn.setRequestProperty("Accept", "application/json");

			// Step 4: Read and parse response
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				response.append(line);
			}
			in.close();

			// Step 5: Process JSON events
			ObjectMapper mapper = new ObjectMapper();
			JsonNode events = mapper.readTree(response.toString());

			for (JsonNode event : events) {
				String eventId = UUID.randomUUID().toString();
				String published = event.path("published").asText();

				Instant eventInstant = Instant.parse(published);
				LocalDateTime dateTime = LocalDateTime.ofInstant(eventInstant, ZoneId.systemDefault());
				String formattedTimestamp = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

				String eventType = event.path("eventType").asText();
				String sessionId = event.path("authenticationContext").path("externalSessionId").asText();
				String email = event.path("actor").path("alternateId").asText();

				String mappedType = mapOktaEventType(eventType);
				if (mappedType == null)
					continue;

				EventsDTO eventDto = new EventsDTO();
				eventDto.setId(mappedType);
				eventDto.setTimestamp(formattedTimestamp);
				eventDto.setSessionId(sessionId);
				eventDto.setProfileId(email); // TEMP: email, to be mapped later in DBService

				eventsList.add(eventDto);
			}

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
}