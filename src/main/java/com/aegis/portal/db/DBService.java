package com.aegis.portal.db;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 
}