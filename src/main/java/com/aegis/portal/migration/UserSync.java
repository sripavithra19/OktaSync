package com.aegis.portal.migration;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.db.DBService;
import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.okta.OktaService;
import com.aegis.portal.util.CommonUtil;

public class UserSync {

	private static final Logger logger = LoggerFactory.getLogger(UserSync.class);

	public static void main(String[] args) {

		logger.info("=== Aegis User and Event Sync ===");

		if (args.length != 2) {
			logger.error("Invalid Arguments. Required: <PortalEnv> <OktaEnv>");
			System.exit(1);
		}

		String portalEnv = args[0];
		String oktaEnv = args[1];

		logger.info("Portal Env: {}", portalEnv);
		logger.info("Okta Env: {}", oktaEnv);

		try {
			OktaService oktaService = new OktaService(oktaEnv);
			DBService dbService = new DBService(portalEnv);
			CommonUtil util = new CommonUtil();

			String lastSyncDate = util.getLastSyncDate();

			// Step 1: Sync users
			List<PortalUser> oktaUsers = oktaService.fetchAllOktaUsers();
			dbService.insertUsers(oktaUsers);

			// Step 2: Get email-to-ID map
			Map<String, String> emailToIdMap = dbService.getUserEmailIdMap();

			// Step 3: Fetch Okta events since last sync
			List<EventsDTO> events = oktaService.fetchEvents(lastSyncDate);

			if (!events.isEmpty()) {
				dbService.insertEvents(events, emailToIdMap);
				util.saveLastSyncDate();
			} else {
				logger.info("No new events fetched.");
			}

		} catch (Exception e) {
			logger.error("Error running sync job: {}", e.getMessage(), e);
		}
	}
}
