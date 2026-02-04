package com.aegis.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.db.DBService;
import com.aegis.portal.model.OktaEmailFailure;
import com.aegis.portal.okta.OktaService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class EmailMonitorService {
	private static final Logger logger = LoggerFactory.getLogger(EmailMonitorService.class);

	private final OktaService oktaService;
	private final DBService dbService;
	private final String oktaEnv;

	public EmailMonitorService(String oktaEnv, String portalEnv) {
		this.oktaEnv = oktaEnv;
		this.oktaService = new OktaService(oktaEnv);
		this.dbService = new DBService(portalEnv);
	}

	public void monitorEmailFailures(String sinceDateTime) {
		logger.info("Starting Okta email failure monitoring...");

		try {
			// Step 1: Ensure table exists
			dbService.createEmailFailureTable();

			// Step 2: Fetch email failure events from Okta
			List<OktaEmailFailure> failures = oktaService.fetchEmailFailureEvents(sinceDateTime);

			// Step 3: Store failures in database
			dbService.storeEmailFailures(failures);

			// Step 4: Get unprocessed failures for notification
			List<OktaEmailFailure> unprocessed = dbService.getUnprocessedEmailFailures();

			if (!unprocessed.isEmpty()) {
				logger.info("Found {} unprocessed email failures", unprocessed.size());

				// Step 5: Send notifications
				sendNotifications(unprocessed);

				// Step 6: Mark as processed
				List<String> eventIds = unprocessed.stream().map(OktaEmailFailure::getEventId)
						.collect(Collectors.toList());
				dbService.markEmailFailuresAsProcessed(eventIds);

				logger.info("Processed {} email failure notifications", unprocessed.size());
			} else {
				logger.info("No new unprocessed email failures found");
			}
			// -------- FAILED LOGIN ATTEMPTS --------
			oktaService.fetchEvents();
		} catch (Exception e) {
			logger.error("Error in email failure monitoring: {}", e.getMessage(), e);
		}
	}

	private void sendNotifications(List<OktaEmailFailure> failures) {

		ZoneId EASTERN = ZoneId.of("America/New_York");

		DateTimeFormatter etFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(EASTERN);

		for (OktaEmailFailure failure : failures) {

			String eventTime = failure.getEventTime() != null ? etFormatter.format(failure.getEventTime()) + " ET"
					: "UNKNOWN_TIME";

			String email = failure.getTargetUserEmail() != null ? failure.getTargetUserEmail() : "UNKNOWN_EMAIL";

			String reason = failure.getFailureReason() != null ? failure.getFailureReason() : "UNKNOWN_REASON";

			String eventId = failure.getEventId() != null ? failure.getEventId() : "UNKNOWN_EVENT_ID";

			logger.warn("[OKTA_EMAIL_FAILURE] time={} | email={} | reason={} | eventId={}", eventTime, email, reason,
					eventId);
		}
	}
}