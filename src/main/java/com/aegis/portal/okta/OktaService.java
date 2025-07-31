package com.aegis.portal.okta;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.util.CommonUtil;
import com.aegis.portal.util.IConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.model.UpdateUserRequest;
import com.okta.sdk.resource.model.User;
import com.okta.sdk.resource.model.UserGetSingleton;
import com.okta.sdk.resource.api.ApplicationApi;
import com.okta.sdk.resource.api.ApplicationUsersApi;
import com.okta.sdk.resource.api.GroupApi;
import com.okta.sdk.resource.api.UserApi;
import com.okta.sdk.resource.api.UserLifecycleApi;
import com.okta.sdk.resource.model.AddGroupRequest;
import com.okta.sdk.resource.model.AppUser;
import com.okta.sdk.resource.model.AppUserAssignRequest;
import com.okta.sdk.resource.model.AppUserCredentials;
import com.okta.sdk.resource.model.CreateUserRequest;
import com.okta.sdk.resource.model.Group;
import com.okta.sdk.resource.model.GroupProfile;
import com.okta.sdk.resource.model.OktaUserGroupProfile;
import com.okta.sdk.resource.model.UserProfile;
import com.okta.sdk.resource.model.UserCredentials;
import com.okta.sdk.resource.model.PasswordCredential;
import com.okta.sdk.resource.model.PasswordCredentialHash;
import com.okta.sdk.resource.model.PasswordCredentialHashAlgorithm;
import com.okta.sdk.resource.model.TempPassword;
import com.okta.sdk.resource.group.GroupBuilder;
import java.util.Collections;

public class OktaService {

	String oktaEnv = null;
	String OktaAppID = null;
	String oktaURL = null;
	String tokenCredentials = null;
	private static Logger logger = LoggerFactory.getLogger(OktaService.class);

	public OktaService(String oktaEnv) {
		this.oktaEnv = oktaEnv;
	}

	public ApiClient getClient(String oktaEnv) throws Exception {

		ApiClient lClient = null;

		ResourceBundle resBundle = ResourceBundle.getBundle("oktaConfiguration");
		if (resBundle == null)
			throw new Exception("Missing OktaConfiguration Properties");
		oktaURL = resBundle.getString(oktaEnv + IConstants.PERIOD + "OrgUrl");

		if (oktaURL == null || oktaURL == "null" || oktaURL.isEmpty())
			throw new Exception("Error Initializing Okta Instance: OrgUrl is missing in oktaConfiguration.properties");

		tokenCredentials = resBundle.getString(oktaEnv + IConstants.PERIOD + "TokenClientCredentials");
		if (tokenCredentials == null || tokenCredentials == "null" || tokenCredentials.isEmpty())
			throw new Exception(
					"Error Initializing Okta Instance: TokenClientCredentials is missing in oktaConfiguration.properties");
		logger.info("oktaURL = {}, TokenCredentials = {} ", oktaURL, tokenCredentials);

		OktaAppID = resBundle.getString(oktaEnv + IConstants.PERIOD + "AEGISlinkAppID");
		if (OktaAppID == null || OktaAppID == "null" || OktaAppID.isEmpty())
			throw new Exception(
					"Error Initializing Okta Instance: AEGISlinkAppID is missing in oktaConfiguration.properties");
		logger.info("oktaURL = {}, TokenCredentials = {} , AEGISlinkAppID = {}", oktaURL, tokenCredentials, OktaAppID);

		lClient = Clients.builder().setOrgUrl(oktaURL)
				.setClientCredentials(new TokenClientCredentials(tokenCredentials)).build();
		return lClient;

	}

	public boolean syncExternalUser(ApiClient client, PortalUser pUser) {
		try {
			UserGetSingleton oktaUser = findExternalUser(client, pUser);
			logger.info("oktaUser == {}", oktaUser);

			if (oktaUser == null) {
				logger.info("User {} doesn't exist in Okta. Creating new User", pUser.getEmail());
				return createNewUser(client, pUser);
			} else {
				String userId = oktaUser.getId();

				if (pUser.getMember().equals("0") || pUser.getActiveFlg().equalsIgnoreCase("Inactive")) {
					logger.info("User {} is deactivated in AEGISlink; deactivating in Okta.", pUser.getEmail());
					boolean deActStatus = deActivateUser(client, userId);
					if (deActStatus) {
						logger.info("Deactivated User {}", pUser.getEmail());
						return true;
					}
					return false;
				}

				if (pUser.getMember().equals("1") && pUser.getActiveFlg().equalsIgnoreCase("Active")) {
					logger.info("User {} is Active in AEGISlink so updating their groups in Okta", pUser.getEmail());

					// Check if user is deactivated in Okta, then activate them
					if (!oktaUser.getStatus().toString().equalsIgnoreCase("ACTIVE")) {
						logger.info("User {} is Active in AEGISlink but deactivated in Okta; Activating User",
								pUser.getEmail());
						UserLifecycleApi lifecycleApi = new UserLifecycleApi(client);
						lifecycleApi.activateUser(userId, true);
					}

					deleteExistingGroups(client, oktaUser);
					List<String> grpList = getOktaGroups(client, pUser);
					updateUserProfile(client, oktaUser, pUser);
					updateGroups(new UserApi(client), userId, grpList);
					return true;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while syncing user {}: {}", pUser.getEmail(), e.getMessage(), e);
		}
		return false;
	}

	public boolean syncInternalUser(ApiClient client, PortalUser pUser) {
		try {
			UserGetSingleton oktaUser = findInternalUser(client, pUser);
			logger.info("oktaUser == {}", oktaUser);

			if (oktaUser == null) {
				logger.info("User {} doesn't exist in Okta. Should it be imported from AD?", pUser.getEmail());
				return false;
			} else {
				String userId = oktaUser.getId();
				UserApi userApi = new UserApi(client);

				if (pUser.getMember().equals("1") && pUser.getActiveFlg().equalsIgnoreCase("Active")
						&& oktaUser.getStatus().toString().equalsIgnoreCase("ACTIVE")) {

					logger.info("User {} is existing and Active, Syncing only Groups of type OKTA_GROUP",
							pUser.getEmail());
					deleteExistingGroups(client, oktaUser);
					List<String> grpList = getOktaGroups(client, pUser);
					updateInternalUserProfile(client, oktaUser, pUser);
					updateGroups(userApi, userId, grpList);
					return true;
				} else {
					logger.info("User {} is Inactive. So deactivating in Okta", pUser.getEmail());
					boolean deActStatus = deActivateUser(client, userId);
					if (deActStatus) {
						logger.info("Deactivated User {}", pUser.getEmail());
						return true;
					}
					return false;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while syncing internal user {}: {}", pUser.getEmail(), e.getMessage(), e);
		}
		return false;
	}

	private boolean createNewUser(ApiClient client, PortalUser pUser) {
	    boolean status = true;
	    try {
	        // Create request object
	        CreateUserRequest createUsrReq = new CreateUserRequest();
	        UserProfile userProfile = createOktaProfile(client, pUser);
	        createUsrReq.setProfile(userProfile);
	        createUsrReq.setGroupIds(getOktaGroups(client, pUser));

	        // Activation & credentials
	        Boolean activate = true;
	        UserCredentials credentials = null;
	        boolean stagedUsr = false;

	        if (pUser.getPassword() != null && (pUser.getPassword().equalsIgnoreCase(IConstants.DEFAULT_PWD_SET_SSIS)
	                || pUser.getPassword().equalsIgnoreCase("expired") || pUser.getMember().equals("0")
	                || pUser.getActiveFlg().equalsIgnoreCase("Inactive"))) {
	            activate = false;
	            stagedUsr = true;
	        } else {
	            credentials = getHashCredentials(client, pUser);
	            createUsrReq.setCredentials(credentials);
	        }

	        // Create user in Okta
	        UserApi userApi = new UserApi(client);
	        User oktaUsr = userApi.createUser(createUsrReq, activate, null, null);

	        if (oktaUsr == null) {
	            logger.error("User {} creation failed in Okta", pUser.getEmail());
	            return false;
	        }
	        logger.info("User {} successfully created in Okta", pUser.getEmail());

	        // Assign user to application
	        ApplicationUsersApi appUsersApi = new ApplicationUsersApi(client);
	        AppUserAssignRequest assignReq = new AppUserAssignRequest();
	        assignReq.setId(oktaUsr.getId());
	        assignReq.setScope(AppUserAssignRequest.ScopeEnum.USER);

	        AppUserCredentials appCreds = new AppUserCredentials();
	        appCreds.setUserName(oktaUsr.getProfile().getEmail());
	        assignReq.setCredentials(appCreds);

	        appUsersApi.assignUserToApplication(OktaAppID, assignReq);
	        logger.info("Assigned user {} to application {}", pUser.getEmail(), OktaAppID);

	        // Handle staged users
	        UserLifecycleApi lifecycleApi = new UserLifecycleApi(client);
	        if (stagedUsr) {
	            if (pUser.getMember().equals("1") && pUser.getActiveFlg().equalsIgnoreCase("Active")) {
	                lifecycleApi.activateUser(oktaUsr.getId(), true);
	                logger.info("Sent activation email to {}", pUser.getUsrID());
	            } else {
	                lifecycleApi.deactivateUser(oktaUsr.getId(), false, null);
	                logger.info("Created deactivated user {}", pUser.getUsrID());
	            }
	        }

	        // Secure password expiration handling for SDK 23.0.1
	        if (pUser.getPassword() != null && pUser.getPassword().equalsIgnoreCase("expired")) {
	            try {
	                String url = oktaURL + "/api/v1/users/" + oktaUsr.getId() + "/lifecycle/expire_password";
	                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	                connection.setRequestMethod("POST");
	                connection.setRequestProperty("Authorization", "SSWS " + tokenCredentials);
	                connection.setRequestProperty("Content-Type", "application/json");
	                connection.setDoOutput(true);

	                int responseCode = connection.getResponseCode();
	                if (responseCode == 200) {
	                    logger.info("Password expired for user {}", pUser.getUsrID());
	                } else {
	                    logger.error("Failed to expire password for {}. Response code: {}", pUser.getUsrID(), responseCode);
	                    status = false;
	                }
	            } catch (Exception e) {
	                logger.error("Failed to expire password for user {}: {}", pUser.getUsrID(), e.getMessage());
	                status = false;
	            }
	        }

	    } catch (Exception e) {
	        logger.error("User {} creation failed in Okta: {}", pUser.getEmail(), e.getMessage(), e);
	        status = false;
	    }
	    return status;
	}


	private boolean groupExists(List<Group> oktaGroupList, String grpName) {
		Iterator<Group> iterator = oktaGroupList.iterator();
		Group appGroup = null;
		while (iterator.hasNext()) {
			appGroup = (Group) iterator.next();
			if (appGroup.getProfile().getName().equalsIgnoreCase(grpName)) {
				return true;
			}
		}
		return false;
	}

	private Group findGroup(ApiClient client, String grpName) {
		String grpFilter = "type eq \"OKTA_GROUP\"";
		String q = null;
		if (grpName != null && !grpName.equalsIgnoreCase("null"))
			q = grpName;
		GroupApi groupApi = new GroupApi(client);
		List<Group> groups = groupApi.listGroups(q, grpFilter, null, null, null, null, null, null);
		Iterator<Group> iterator = groups.iterator();
		Group appGroup = null;
		while (iterator.hasNext()) {
			appGroup = (Group) iterator.next();
			logger.info("Group name to find = {} and curr group = {}", grpName, appGroup.getProfile().getName());
			if (appGroup.getProfile().getName().equalsIgnoreCase(grpName)) {
				return appGroup;
			}
		}
		return null;
	}

	public Group buildAndCreate(ApiClient client, String grpName) {
		try {
			GroupApi groupApi = new GroupApi(client);

			// Create the group profile using the correct type
			OktaUserGroupProfile profile = new OktaUserGroupProfile();
			profile.setName(grpName);
			profile.setDescription("System-generated group: " + grpName);

			// Create the group request with the profile
			AddGroupRequest groupRequest = new AddGroupRequest();
			groupRequest.setProfile(profile);

			Group createdGroup = groupApi.addGroup(groupRequest);
			logger.info("Created new group '{}' with ID: {}", grpName, createdGroup.getId());
			return createdGroup;

		} catch (Exception e) {
			logger.error("Group creation failed for '{}'", grpName, e);
			throw new RuntimeException("Failed to create group: " + grpName, e);
		}
	}

	private UserGetSingleton findExternalUser(ApiClient client, PortalUser pUser) {
		logger.info("Inside findExternalUser and User ID ={}", pUser.getUserName());
		String usrFilter = "profile.login eq \"" + pUser.getUserName() + "\"";
		logger.debug("usrFilter = {}", usrFilter);

		UserApi userApi = new UserApi(client);
		try {
			// Updated to match the correct listUsers signature
			List<User> usrList = userApi.listUsers(usrFilter, null, null, null, null, null, null, null);

			if (usrList != null && !usrList.isEmpty()) {
				String userId = usrList.get(0).getId();
				return userApi.getUser(userId, null, null);
			}
		} catch (Exception e) {
			logger.error("Error finding user {}: {}", pUser.getUserName(), e.getMessage(), e);
		}
		return null;
	}

	private UserGetSingleton findInternalUser(ApiClient client, PortalUser pUser) {
		logger.info("Inside findInternalUser for User ID = {}", pUser.getUserName());

		StringBuilder usrFilter = new StringBuilder().append("profile.login eq \"").append(pUser.getUserName())
				.append("@aegislimited.com\"").append(" or profile.login eq \"").append(pUser.getEmail()).append("\"");

		if (pUser.getNetworkID() != null && !pUser.getNetworkID().equalsIgnoreCase("null")) {
			usrFilter.append(" or profile.login eq \"").append(pUser.getNetworkID()).append("@aegislimited.com\"");
		}

		logger.info("usrFilter = {}", usrFilter.toString());

		try {
			UserApi userApi = new UserApi(client);
			List<User> usrList = userApi.listUsers(usrFilter.toString(), null, null, null, null, null, null, null);

			if (usrList != null && !usrList.isEmpty()) {
				String userId = usrList.get(0).getId();
				return userApi.getUser(userId, null, null);
			}
		} catch (Exception e) {
			logger.error("Error finding internal user {}: {}", pUser.getUserName(), e.getMessage(), e);
		}
		return null;
	}

	private User findUser(ApiClient client, PortalUser pUser) {
		// String usrFilter ="profile.email eq \""+pUser.getEmail()+"\"";
		// String usrFilter ="profile.email%20eq%20%22"+pUser.getEmail()+"%22";

		/*
		 * try { usrFilter = URLEncoder.encode(usrFilter, "utf-8"); } catch
		 * (UnsupportedEncodingException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */

		// logger.info("Encoded usrFilter = "+ usrFilter);
		UserApi userApi = new UserApi(client);
		User oktaUser = null;
		List<User> usrList = userApi.listUsers(pUser.getEmail(), null, null, null, null, null, null, null);

		// users = client.listUsers(null, "status eq \"ACTIVE\"", null, null, null);
		Iterator<User> iterator = usrList.iterator();

		if (iterator.hasNext()) {
			oktaUser = iterator.next();
			if (iterator.hasNext()) {
				// chcking if duplicate Users exists with same Email address
				logger.error("Duplicate User Records exists for {}", pUser.getEmail());
				oktaUser = null;
			}
		} else {
			/* Search by networkID@aegislimited */
			logger.info("Search with network ID for user ::{}", pUser.getUserName());
			String usrFilter = "profile.login eq \"" + pUser.getEmail() + "\"";
			usrList = userApi.listUsers(null, usrFilter, null, null, null, null, null, null);
			iterator = usrList.iterator();
			if (iterator.hasNext()) {
				oktaUser = iterator.next();
				if (iterator.hasNext()) {
					// chcking if duplicate Users exists with same Email address
					logger.error("Duplicate User Records exists for {}", pUser.getUserName());
					oktaUser = null;
				}
			}

		}
		return oktaUser;
	}

	private UserCredentials getHashCredentials(ApiClient client, PortalUser pUser) {
		try {
			String pPwd = pUser.getPassword();
			if (pPwd == null) {
				return null;
			}

			if (pPwd.equalsIgnoreCase("expired")) {
				logger.info("Password is expired so setting to default Password1 and then expire in Okta");
				pPwd = "2ac9cb7dc02b3c0083eb70898e549b63"; // Default Password1 hash
			}

			if (pPwd.length() > 10) {
				CommonUtil commonUtil = new CommonUtil();
				logger.info("Inside getHashCredentials and pPwd={} and base64={}", pPwd,
						commonUtil.getBase64FromHEX(pPwd));

				// Create credentials objects
				UserCredentials credentials = new UserCredentials();
				PasswordCredential passwordCredential = new PasswordCredential();
				PasswordCredentialHash pwdCredHash = new PasswordCredentialHash();

				// Configure password hash
				pwdCredHash.setAlgorithm(PasswordCredentialHashAlgorithm.MD5);
				pwdCredHash.setSalt(""); 
				pwdCredHash.setSaltOrder(IConstants.saltOrder);
				pwdCredHash.setValue(commonUtil.getBase64FromHEX(pPwd));

				// Set up the credentials
				passwordCredential.setHash(pwdCredHash);
				credentials.setPassword(passwordCredential);

				return credentials;
			}
		} catch (Exception e) {
			logger.error("Error creating hash credentials: {}", e.getMessage(), e);
		}
		return null;
	}

	private List<String> getOktaGroups(ApiClient client, PortalUser pUser) {

		Set<String> grpSet = pUser.getGroups();
		// Add "AEGISLink Users" group which provisions RSL application
		grpSet.add("AEGISLink Users");
		logger.info("grpSet ={}", grpSet.toString());

		Group nGroup = null;
		List<String> grpList = new ArrayList<String>();
		for (String grpName : grpSet) {
			logger.info("User Groups ={}", grpName);
			nGroup = findGroup(client, grpName);
			logger.info("nGroup= {}", nGroup);
			if (nGroup == null) {
				nGroup = buildAndCreate(client, grpName);
				logger.info("New Group {} is created in Okta successfully", nGroup.getId());
			}
			logger.info("Group ID ={} ", nGroup.getId());
			// oktaUsr.addToGroup(nGroup.getId());
			grpList.add(nGroup.getId());
		}
		return grpList;

	}

	private UserProfile createOktaProfile(ApiClient client, PortalUser pUser) {
		boolean termsViewed = pUser.getTermsviewed() != null && pUser.getTermsviewed().equals("1");

		// Create new UserProfile instance directly
		UserProfile userProfile = new UserProfile();

		// Set standard profile attributes
		userProfile.setFirstName(pUser.getFirstName());
		userProfile.setLastName(pUser.getLastName());
		userProfile.setMiddleName(pUser.getMiddleName());
		userProfile.setEmail(pUser.getEmail());
		userProfile.setTitle(pUser.getTitle());
		userProfile.setDisplayName(pUser.getDisplayName());
		userProfile.setLogin(pUser.getUserName());
		userProfile.setMobilePhone(pUser.getMobileNo());
		userProfile.setOrganization(pUser.getOrgName());
		userProfile.setHonorificPrefix(pUser.getSalutation());
		userProfile.setEmployeeNumber(pUser.getxAEGISNo());

		userProfile.getAdditionalProperties().put("termsOfUse", termsViewed);
		userProfile.getAdditionalProperties().put("insuredCompany", pUser.getOrgName());
		userProfile.getAdditionalProperties().put("insuredCompanyID", pUser.getWorkOrgID());
		userProfile.getAdditionalProperties().put("externalCompanyName", pUser.getExternalCompanyName());
		userProfile.getAdditionalProperties().put("externalCompanyId", pUser.getExternalCompanyId());

		return userProfile;
	}

	private void deleteExistingGroups(ApiClient client, UserGetSingleton oktaUser) {
		try {
			GroupApi groupApi = new GroupApi(client);
			List<Group> allGroups = groupApi.listGroups(null, null, null, null, null, null, null, null);
			logger.info("oktaGroupList inside deleteExistingGroups {}", allGroups);

			StringBuilder grpList = new StringBuilder();
			int cnt = 0;

			for (Group group : allGroups) {
				logger.info("group name {}, and type = {}", group.getProfile().getName(), group.getType());

				if (group.getType().toString().equals("OKTA_GROUP")) {
					grpList.append(",").append(group.getProfile().getName());
					logger.debug("removeUser from this group {}", group.getProfile().getName());
					groupApi.unassignUserFromGroup(group.getId(), oktaUser.getId());
					cnt++;
				}
			}

			logger.info("List of groups deleted for User {} are {}", oktaUser.getProfile().getEmail(),
					grpList.toString());
		} catch (Exception e) {
			logger.error("Error deleting groups for user {}: {}", oktaUser.getProfile().getEmail(), e.getMessage(), e);
		}
	}

	private void updateGroups(UserApi userApi, String userId, List<String> grpList) {
		GroupApi groupApi = new GroupApi(userApi.getApiClient());

		try {
			for (String grpID : grpList) {
				logger.debug("Adding user to group {} for userId {}", grpID, userId);
				groupApi.assignUserToGroup(grpID, userId);
			}
		} catch (Exception e) {
			logger.error("Error updating groups for user {}: {}", userId, e.getMessage(), e);
		}
	}

	private boolean updateUserProfile(ApiClient client, UserGetSingleton oktaUser, PortalUser pUser) {
		try {
			// Create the updated profile
			UserProfile updatedProfile = createOktaProfile(client, pUser);

			// Create an UpdateUserRequest with the new profile
			UpdateUserRequest updateRequest = new UpdateUserRequest();
			updateRequest.setProfile(updatedProfile);

			// Update the user
			UserApi userApi = new UserApi(client);
			userApi.updateUser(oktaUser.getId(), updateRequest, true); // true for partial update

			logger.info("User Profile {} updated successfully", pUser.getUserName());
			return true;
		} catch (Exception e) {
			logger.error("User {} Profile Update failed in Okta: {}", pUser.getUserName(), e.getMessage(), e);
			return false;
		}
	}

	private boolean updateInternalUserProfile(ApiClient client, UserGetSingleton oktaUsr, PortalUser pUser) {
		try {
			// Get the user's profile
			com.okta.sdk.resource.model.UserProfile profile = oktaUsr.getProfile();

			// Update custom profile attributes
			boolean termsViewed = pUser.getTermsviewed() != null && pUser.getTermsviewed().equals("1");

			// Use the additionalProperties map for custom attributes
			profile.getAdditionalProperties().put("termsOfUse", termsViewed);
			profile.getAdditionalProperties().put("insuredCompany", pUser.getOrgName());
			profile.getAdditionalProperties().put("insuredCompanyID", pUser.getWorkOrgID());

			// Create UpdateUserRequest
			UpdateUserRequest updateUserRequest = new UpdateUserRequest().profile(profile);

			// Create UserApi instance and update the user
			UserApi userApi = new UserApi(client);
			userApi.updateUser(oktaUsr.getId(), updateUserRequest, false); // false for partial update

			logger.info("Updated profile for user {}", pUser.getUserName());
			return true;
		} catch (Exception e) {
			logger.error("Failed to update profile for user {}: {}", pUser.getUserName(), e.getMessage(), e);
			return false;
		}
	}

	private boolean deActivateUser(ApiClient apiClient, String userId) {
		UserLifecycleApi userLifecycleApi = new UserLifecycleApi(apiClient);

		try {
			// Fetch user to get email for logging
			UserApi userApi = new UserApi(apiClient);
			UserGetSingleton user = userApi.getUser(userId, null, null);
			String email = user.getProfile().getEmail();

			// Deactivate user
			userLifecycleApi.deactivateUser(userId, null, null);

			logger.info("User {} successfully deactivated", email);
			return true;
		} catch (Exception e) {
			logger.error("Error deactivating user {} :: {}", userId, e.getMessage(), e);
			return false;
		}
	}

	public List<EventsDTO> fetchEvents() {
		List<EventsDTO> eventsList = new ArrayList<EventsDTO>();

		try {

			CommonUtil fileUtil = new CommonUtil();
			String lstSyncDtStr = fileUtil.getLastSyncDate();

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

			LocalDateTime lstSyncdateTime = LocalDateTime.parse(lstSyncDtStr, formatter);

			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

			String formattedDateTime = lstSyncdateTime.format(formatter);
			logger.info("Formatted DateTime123 : " + formattedDateTime);

			// Step 2: Prepare the filter and API URL
			String filter = "eventType eq \"user.session.start\" or eventType eq \"user.session.end\" "
					+ "or eventType eq \"user.account.activated\" or eventType eq \"user.account.deactivated\"";

			logger.info("filter in fetchEvents = {} ", filter);

			String encodedFilter = URLEncoder.encode(filter, "UTF-8");

			Instant instant = Instant.parse(formattedDateTime);
			String isoUtcSince = instant.toString();

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
				eventDto.setEmail(email);

				if (isAEGISEmployee(email))
					eventDto.setLogin(getEmployeeLogin(email));
				else
					eventDto.setLogin(email);

				eventsList.add(eventDto);
			}

		} catch (Exception e) {
			logger.error("Error fetching events from Okta: {}", e.getMessage(), e);
		}

		return eventsList;
	}

	private boolean isAEGISEmployee(String email) {
		if (email.toLowerCase().indexOf("@aegislimited.com") >= 0)
			return true;
		else
			return false;
	}

	private String getEmployeeLogin(String email) {
		String login = null;
		int index = email.toLowerCase().indexOf("@aegislimited.com");
		if (index >= 0)
			login = email.substring(0, index);
		return login;
	}

	private String mapOktaEventType(String eventType) {
		switch (eventType) {
		case "user.session.start":
			return "/login.xml";
		case "user.session.end":
			return "/logout.xml";
		case "user.account.activated":
			return "activation";
		case "user.account.deactivated":
			return "deactivation";
		default:
			return null;
		}
	}
}
