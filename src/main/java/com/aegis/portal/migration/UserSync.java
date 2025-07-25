package com.aegis.portal.migration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
			String lstSyncDtStr = util.getLastSyncDate(); // IST format like "2025-07-24 12:34:56"

			if (lstSyncDtStr == null || lstSyncDtStr.isEmpty()) {
			    logger.error("Last sync date is null or empty");
			    System.exit(1);
			}

			// Parse IST string to LocalDateTime
			DateTimeFormatter istFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime lstSyncdateTime = LocalDateTime.parse(lstSyncDtStr, istFormatter);

			// Convert LocalDateTime in IST to UTC ISO 8601 format
			ZonedDateTime istZoned = lstSyncdateTime.atZone(ZoneId.of("Asia/Kolkata"));
			ZonedDateTime utcZoned = istZoned.withZoneSameInstant(ZoneOffset.UTC);

			// Format to Okta-compatible ISO 8601
			DateTimeFormatter utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			String formattedDateTime = utcZoned.format(utcFormatter);

			logger.info("Last Sync Date in UTC: {}", formattedDateTime);


			// Step 1: Sync users
			List<PortalUser> oktaUsers = oktaService.fetchAllOktaUsers();
			dbService.insertUsers(oktaUsers);

			// Step 2: Get email-to-ID map
			Map<String, String> emailToIdMap = dbService.getUserEmailIdMap();

			// Step 3: Fetch Okta events since last sync
			List<EventsDTO> events = oktaService.fetchEvents(formattedDateTime);

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
