package com.aegis.portal.db;

import com.aegis.portal.model.OktaEmailFailure;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.util.CommonUtil;

public class DBService {
	private static Logger logger = LoggerFactory.getLogger(DBService.class);
	private String env;
	private String lastsyncdate;
	CommonUtil util = new CommonUtil();

	public DBService(String dbEnv) {
		this.env = dbEnv;
	}

	public void insertUsers(List<PortalUser> users) {
		DBConnection dbConn = new DBConnection(env);

		String mergeSql = "MERGE INTO DPS_USER target "
				+ "USING (SELECT ? AS LOGIN, ? AS EMAIL, ? AS FIRST_NAME, ? AS LAST_NAME FROM dual) source "
				+ "ON (target.EMAIL = source.EMAIL) " + "WHEN NOT MATCHED THEN "
				+ "INSERT (ID, LOGIN, EMAIL, FIRST_NAME, LAST_NAME) "
				+ "VALUES (DPS_USER_SEQ.NEXTVAL, source.LOGIN, source.EMAIL, source.FIRST_NAME, source.LAST_NAME)";

		try (Connection conn = dbConn.getConnection(); PreparedStatement stmt = conn.prepareStatement(mergeSql)) {

			for (PortalUser user : users) {
				stmt.setString(1, user.getLogin());
				stmt.setString(2, user.getEmail());
				stmt.setString(3, user.getFirstName());
				stmt.setString(4, user.getLastName());
				stmt.addBatch();
			}

			int[] result = stmt.executeBatch();
			logger.info("Inserted or skipped {} users into DPS_USER", result.length);

		} catch (Exception e) {
			logger.error("Error inserting users: {}", e.getMessage(), e);
		}
	}

	/*
	 * public List<PortalUser> fetchAndStoreEvents() { DBConnection dbConn = new
	 * DBConnection(env); Connection con = null; PreparedStatement stmt = null;
	 * List<PortalUser> usrList = new ArrayList<>(); lastsyncdate =
	 * util.getLastSyncDate();
	 * 
	 * try { con = dbConn.getConnection(); logger.debug("db Connection = {}", con);
	 * 
	 * String userQuery =
	 * "SELECT ID, login, First_name, Last_name, Email FROM DPS_USER"; stmt =
	 * con.prepareStatement(userQuery); ResultSet rs = stmt.executeQuery();
	 * 
	 * while (rs.next()) { PortalUser usr = new PortalUser();
	 * usr.setUsrID(rs.getString("ID")); usr.setLogin(rs.getString("login"));
	 * usr.setFirstName(rs.getString("First_name"));
	 * usr.setLastName(rs.getString("Last_name"));
	 * usr.setEmail(rs.getString("Email"));
	 * 
	 * usrList.add(usr); } rs.close(); stmt.close();
	 * 
	 * } catch (Exception e) { logger.error("Exception in getAllUsers :: {}",
	 * e.getMessage()); } finally { try { if (stmt != null) stmt.close(); if (con !=
	 * null) con.close(); } catch (SQLException se) {
	 * logger.error("Exception in DB Closing {}", se.getMessage()); } } return
	 * usrList; }
	 */

	public void insertEvents(List<EventsDTO> eventList, Map<String, String> emailToIdMap) {
		String sql = "INSERT INTO DSS_DPS_EVENT (ID, TIMESTAMP, SESSIONID, PROFILEID) "
				+ "VALUES (?, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?)";

		try (Connection con = new DBConnection(env).getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			for (EventsDTO evt : eventList) {
				String userId = emailToIdMap.get(evt.getProfileId());

				if (userId != null) {
					ps.setString(1, evt.getId());
					ps.setString(2, evt.getTimestamp());
					ps.setString(3, evt.getSessionId() != null ? evt.getSessionId() : "");
					ps.setString(4, userId);
					ps.addBatch();
				} else {
					logger.warn("No user ID found for email: {}", evt.getProfileId());
				}
			}

			int[] inserted = ps.executeBatch();
			logger.info("Inserted {} events into DSS_DPS_EVENT", inserted.length);

		} catch (Exception e) {
			logger.error("Error inserting events: {}", e.getMessage(), e);
		}
	}

	public Map<String, String> getUserEmailIdMap() {
		Map<String, String> emailToIdMap = new HashMap<>();
		String sql = "SELECT ID, EMAIL FROM DPS_USER";

		try (Connection con = new DBConnection(env).getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				emailToIdMap.put(rs.getString("EMAIL"), rs.getString("ID"));
			}

		} catch (SQLException e) {
			logger.error("Error fetching user ID map: {}", e.getMessage(), e);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return emailToIdMap;
	}
	
	public void createEmailFailureTable() {
	    DBConnection dbConn = new DBConnection(env);
	    
	    // Correct Oracle syntax - removed GENERATED ALWAYS AS IDENTITY which isn't supported in older Oracle versions
	    String createTableSql = "CREATE TABLE OKTA_EMAIL_FAILURES (" +
	            "ID NUMBER PRIMARY KEY, " +
	            "EVENT_ID VARCHAR2(255) NOT NULL, " +
	            "EVENT_TIME TIMESTAMP NOT NULL, " +
	            "EVENT_TYPE VARCHAR2(100) NOT NULL, " +
	            "SEVERITY VARCHAR2(50), " +
	            "USER_ID VARCHAR2(255), " +
	            "USER_EMAIL VARCHAR2(255), " +
	            "TARGET_USER_ID VARCHAR2(255), " +
	            "TARGET_USER_EMAIL VARCHAR2(255), " +
	            "FAILURE_REASON CLOB, " +
	            "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
	            "PROCESSED NUMBER(1) DEFAULT 0, " +
	            "CONSTRAINT UK_EMAIL_FAILURE_EVENT_ID UNIQUE (EVENT_ID)" +
	            ")";
	    
	    // Create sequence for ID
	    String createSequenceSql = "CREATE SEQUENCE OKTA_EMAIL_FAILURES_SEQ START WITH 1 INCREMENT BY 1";
	    
	    try (Connection conn = dbConn.getConnection();
	         Statement stmt = conn.createStatement()) {
	        
	        // Drop table if exists (optional - remove in production)
	        try {
	            stmt.execute("DROP TABLE OKTA_EMAIL_FAILURES");
	            logger.info("Dropped existing OKTA_EMAIL_FAILURES table");
	        } catch (SQLException e) {
	            // Table doesn't exist, continue
	            logger.info("OKTA_EMAIL_FAILURES table doesn't exist, creating new one");
	        }
	        
	        // Drop sequence if exists
	        try {
	            stmt.execute("DROP SEQUENCE OKTA_EMAIL_FAILURES_SEQ");
	        } catch (SQLException e) {
	            // Sequence doesn't exist, continue
	        }
	        
	        // Create sequence first
	        stmt.execute(createSequenceSql);
	        
	        // Create table
	        stmt.execute(createTableSql);
	        
	        // Create indexes
	        stmt.execute("CREATE INDEX IDX_EMAIL_FAILURE_TIME ON OKTA_EMAIL_FAILURES(EVENT_TIME)");
	        stmt.execute("CREATE INDEX IDX_EMAIL_FAILURE_TARGET ON OKTA_EMAIL_FAILURES(TARGET_USER_EMAIL)");
	        stmt.execute("CREATE INDEX IDX_EMAIL_FAILURE_PROCESSED ON OKTA_EMAIL_FAILURES(PROCESSED)");
	        
	        logger.info("OKTA_EMAIL_FAILURES table and sequence created successfully");
	        
	    } catch (Exception e) {
	        logger.error("Error creating email failure table: {}", e.getMessage(), e);
	    }
	}
	public void storeEmailFailures(List<OktaEmailFailure> failures) {
	    if (failures == null || failures.isEmpty()) {
	        return;
	    }
	    
	    DBConnection dbConn = new DBConnection(env);
	    
	    // Use sequence for ID
	    String insertSql = "INSERT INTO OKTA_EMAIL_FAILURES " +
	            "(ID, EVENT_ID, EVENT_TIME, EVENT_TYPE, SEVERITY, USER_ID, USER_EMAIL, " +
	            "TARGET_USER_ID, TARGET_USER_EMAIL, FAILURE_REASON) " +
	            "VALUES (OKTA_EMAIL_FAILURES_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    
	    try (Connection conn = dbConn.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
	        
	        int insertedCount = 0;
	        for (OktaEmailFailure failure : failures) {
	            pstmt.setString(1, failure.getEventId());
	            pstmt.setTimestamp(2, Timestamp.from(failure.getEventTime()));
	            pstmt.setString(3, failure.getEventType());
	            pstmt.setString(4, failure.getSeverity());
	            pstmt.setString(5, failure.getUserId());
	            pstmt.setString(6, failure.getUserEmail());
	            pstmt.setString(7, failure.getTargetUserId());
	            pstmt.setString(8, failure.getTargetUserEmail());
	            pstmt.setString(9, failure.getFailureReason());
	            
	            try {
	                pstmt.executeUpdate();
	                insertedCount++;
	            } catch (SQLException e) {
	                if (e.getErrorCode() == 1) { // ORA-00001: unique constraint violated
	                    logger.warn("Duplicate event ID skipped: {}", failure.getEventId());
	                } else {
	                    throw e;
	                }
	            }
	        }
	        
	        logger.info("Successfully stored {} email failures in database", insertedCount);
	        
	    } catch (Exception e) {
	        logger.error("Error storing email failures in database: {}", e.getMessage(), e);
	    }
	}

	public List<OktaEmailFailure> getUnprocessedEmailFailures() {
	    List<OktaEmailFailure> failures = new ArrayList<>();
	    DBConnection dbConn = new DBConnection(env);
	    
	    String querySql = "SELECT * FROM OKTA_EMAIL_FAILURES " +
	                      "WHERE PROCESSED = 0 " +  // Oracle uses 1/0 for boolean
	                      "ORDER BY EVENT_TIME DESC";
	    
	    try (Connection conn = dbConn.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(querySql);
	         ResultSet rs = pstmt.executeQuery()) {
	        
	        while (rs.next()) {
	            OktaEmailFailure failure = new OktaEmailFailure();
	            failure.setEventId(rs.getString("EVENT_ID"));
	            failure.setEventTime(rs.getTimestamp("EVENT_TIME").toInstant());
	            failure.setEventType(rs.getString("EVENT_TYPE"));
	            failure.setSeverity(rs.getString("SEVERITY"));
	            failure.setUserId(rs.getString("USER_ID"));
	            failure.setUserEmail(rs.getString("USER_EMAIL"));
	            failure.setTargetUserId(rs.getString("TARGET_USER_ID"));
	            failure.setTargetUserEmail(rs.getString("TARGET_USER_EMAIL"));
	            failure.setFailureReason(rs.getString("FAILURE_REASON"));
	            failure.setProcessed(rs.getInt("PROCESSED") == 1); // Convert 1/0 to boolean
	            
	            failures.add(failure);
	        }
	        
	    } catch (Exception e) {
	        logger.error("Error retrieving unprocessed email failures: {}", e.getMessage(), e);
	    }
	    
	    return failures;
	}

	public void markEmailFailuresAsProcessed(List<String> eventIds) {
	    if (eventIds == null || eventIds.isEmpty()) {
	        return;
	    }
	    
	    DBConnection dbConn = new DBConnection(env);
	    
	    // Create SQL with IN clause
	    StringBuilder sqlBuilder = new StringBuilder();
	    sqlBuilder.append("UPDATE OKTA_EMAIL_FAILURES SET PROCESSED = 1 WHERE EVENT_ID IN (");
	    
	    for (int i = 0; i < eventIds.size(); i++) {
	        if (i > 0) sqlBuilder.append(",");
	        sqlBuilder.append("?");
	    }
	    sqlBuilder.append(")");
	    
	    try (Connection conn = dbConn.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
	        
	        for (int i = 0; i < eventIds.size(); i++) {
	            pstmt.setString(i + 1, eventIds.get(i));
	        }
	        
	        int updated = pstmt.executeUpdate();
	        logger.info("Marked {} email failure events as processed", updated);
	        
	    } catch (Exception e) {
	        logger.error("Error marking email failures as processed: {}", e.getMessage(), e);
	    }
	}
	   
}