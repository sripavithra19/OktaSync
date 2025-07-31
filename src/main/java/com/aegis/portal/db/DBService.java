package com.aegis.portal.db;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.util.CommonUtil;
import com.aegis.portal.util.IConstants;

public class DBService {
	private static Logger logger = LoggerFactory.getLogger(DBService.class);

	String env =  null;
	public DBService(String dbEnv) {
		this.env = dbEnv;
	}

	public List<PortalUser> getExternalUserList() {
		DBConnection dbConn = new DBConnection(env);
		Connection con =  null;
		PreparedStatement grpStmt = null;
		PreparedStatement pStmt = null;
		List<PortalUser> usrList = new ArrayList<PortalUser>();
		CommonUtil fileUtil =  new CommonUtil();

		try {
			con = dbConn.getConnection();
			logger.debug("db Connection = {}",con);


			String lstSyncDtStr = fileUtil.getLastSyncDate();
			if(lstSyncDtStr == null) {
				logger.error("Missing lastRunDate.properties");
				return null;
			} else if(lstSyncDtStr.isEmpty()) {

			} 
			logger.info("lstSync Timestamp :: {}",lstSyncDtStr);

			String pUserQuery = "SELECT distinct U.ID, login,Network_login_name, Password,First_name,Middle_Name, "
					+ "Last_Name,Email,X_aeg_Person_ID, NickName, Job_Title,Salutation, "
					+ "Work_ph_num,cell_ph_num, Company_Name, U.emplmnt_comp_id Work_Comp_ID, "
					+ "TO_CHAR(U.ROW_MOD_DATE,'YYYY-MM-DD HH24:MI:SS') Mod_DT, "
					+ "TO_CHAR(LOGIN_UNLOCK_DATE,'YYYY-MM-DD HH24:MI:SS') LOGIN_UNLOCK_DATE,"
					+ "Member, U.row_status row_status, termsviewed, EXTERNAL_COMPANY_NAME, EXTERNAL_COMPANY_ID  FROM aegis1.DPS_USER U JOIN aegis1.AG_COMPANY C on "
					+ "U.emplmnt_comp_id = C.ID WHERE "
					+ "emplmnt_comp_id != :1" 
					+ "AND U.ID in (Select USER_ID from aegis1.dps_user_Audit_log where "
					+ " Action_date > to_date(:2,'YYYY-MM-DD HH24:MI:SS' )"
					+ " UNION select  USER_ID from aegis1.AG_WEBSITE_GROUPS_MEMBERS_AUDIT_LOG  where Action_date > to_date(:3,'YYYY-MM-DD HH24:MI:SS' )" 
					+ "	UNION select  USER_ID from aegis1.ag_user_company_role_audit_log where  Action_date > to_date(:4,'YYYY-MM-DD HH24:MI:SS' )  ) " 
					+ "ORDER BY LOGIN ";

			pStmt = con.prepareStatement(pUserQuery);
			if(con != null) {
				pStmt.setString(1, "3700019");				
				pStmt.setString(2, lstSyncDtStr);
				pStmt.setString(3, lstSyncDtStr);
				pStmt.setString(4, lstSyncDtStr);

				ResultSet rs = pStmt.executeQuery();
				//logger.info("Number of Users to sync {} }",r);
				PortalUser Usr =  null;

				String sql = IConstants.GROUP_LIST_QRY;
				grpStmt = con.prepareStatement(sql);

				while(rs.next()) { 
					Usr = getUserDetails(rs);
					UpdateGroups(grpStmt,Usr);
					usrList.add(Usr);
				}
				rs.close();
				pStmt.close();	
				// Update the Group details


			} 
		} catch(Exception e) {
			logger.error("Exception in getUserList :: {}",e.getMessage());
		}
		finally {
			try {
				if(grpStmt != null) 
					grpStmt.close();
				if(pStmt != null) 
					pStmt.close();
				if(con != null)
					con.close();				
			} catch(SQLException se) {
				logger.error("Exception in DB Closing {}",se.getMessage());
			}
		}
		return usrList;
	}


	public List<PortalUser> getInternalUserList() {
		DBConnection dbConn = new DBConnection(env);
		Connection con =  null;
		PreparedStatement grpStmt = null;
		PreparedStatement pStmt = null;
		List<PortalUser> usrList = new ArrayList<PortalUser>();
		CommonUtil fileUtil =  new CommonUtil();

		try {
			con = dbConn.getConnection();
			logger.debug("db Connection = {}",con);


			String lstSyncDtStr = fileUtil.getLastSyncDate();
			if(lstSyncDtStr == null) {
				logger.error("Missing lastRunDate.properties");
				return null;
			} else if(lstSyncDtStr.isEmpty()) {

			} 
			logger.info("lstSync Timestamp :: {}",lstSyncDtStr);

			String pUserQuery = "SELECT distinct U.ID, login, Network_login_name, Password,First_name,Middle_Name, "
					+ "Last_Name,Email,X_aeg_Person_ID, NickName, Job_Title,Salutation, "
					+ "Work_ph_num,cell_ph_num, Company_Name, U.emplmnt_comp_id Work_Comp_ID, "
					+ "TO_CHAR(U.ROW_MOD_DATE,'YYYY-MM-DD HH24:MI:SS') Mod_DT, "
					+ "TO_CHAR(LOGIN_UNLOCK_DATE,'YYYY-MM-DD HH24:MI:SS') LOGIN_UNLOCK_DATE,"
					+ "Member, U.row_status row_status, termsviewed, EXTERNAL_COMPANY_NAME, EXTERNAL_COMPANY_ID  FROM aegis1.DPS_USER U JOIN aegis1.AG_COMPANY C on "
					+ "U.emplmnt_comp_id = C.ID WHERE "
					+ "emplmnt_comp_id = :1 " 
					+ "AND U.ID in (Select USER_ID from aegis1.dps_user_Audit_log where "
					+ " Action_date > to_date(:2,'YYYY-MM-DD HH24:MI:SS' )"
					+ " UNION select  USER_ID from aegis1.AG_WEBSITE_GROUPS_MEMBERS_AUDIT_LOG  where Action_date > to_date(:3,'YYYY-MM-DD HH24:MI:SS' )"
					+ " UNION select  USER_ID from aegis1.ag_user_company_role_audit_log where  Action_date > to_date(:4,'YYYY-MM-DD HH24:MI:SS' )   ) " 
					+ "ORDER BY LOGIN ";

			pStmt = con.prepareStatement(pUserQuery);
			if(con != null) {
				pStmt.setString(1, "3700019");				
				pStmt.setString(2, lstSyncDtStr);
				pStmt.setString(3, lstSyncDtStr);
				pStmt.setString(4, lstSyncDtStr);

				ResultSet rs = pStmt.executeQuery();
				//logger.info("Number of Users to sync {} }",r);
				PortalUser Usr =  null;

				String sql = IConstants.GROUP_LIST_QRY;
				grpStmt = con.prepareStatement(sql);

				while(rs.next()) { 
					Usr = getUserDetails(rs);
					UpdateGroups(grpStmt,Usr);
					usrList.add(Usr);
				}
				rs.close();
				pStmt.close();	
				// Update the Group details


			} 
		} catch(Exception e) {
			logger.error("Exception in getUserList :: {}",e.getMessage());
		}
		finally {
			try {
				if(grpStmt != null) 
					grpStmt.close();
				if(pStmt != null) 
					pStmt.close();
				if(con != null)
					con.close();				
			} catch(SQLException se) {
				logger.error("Exception in DB Closing {}",se.getMessage());
			}
		}
		return usrList;
	}



	private void UpdateGroups(PreparedStatement grpStmt, PortalUser usr) {

		Set<String> grpSet = new HashSet<String>();

		try {

			grpStmt.setString(1, usr.getUsrID());
			grpStmt.setString(2, "Active");
			grpStmt.setString(3, "Active");
			grpStmt.setString(4,  usr.getUsrID());
			logger.info("usr  id = {}, work id = {}",usr.getUsrID(),usr.getWorkOrgID());
			ResultSet rs = grpStmt.executeQuery();
			while (rs.next()) {
				grpSet.add(rs.getString("grpCD"));

			}
			/*
			 * This is being added so Password Policy could be setup for external users, where they
			 * are allowed to reset/Forgot Password.
			 */
			if(usr.getEmail() != null && !(usr.getEmail().toLowerCase().contains("@aegislimited.com"))) {
				logger.info("Adding group AEGISlink_external_user_group to external user");
				grpSet.add("AEGISlink_external_user_group");
			}
			logger.info("group list for user :: {} size is {}, list = {}",usr.getEmail(),grpSet.size(),  Arrays.toString(grpSet.toArray()));
			usr.setGroups(grpSet);
			if(rs!=null)
				rs.close();

		} catch(SQLException e) {
			logger.error(e.getMessage());
		}

	}
	private PortalUser getUserDetails(ResultSet rs) throws SQLException {
		PortalUser usr = new PortalUser();
		usr.setUsrID(rs.getString("ID"));
		usr.setWorkOrgID(rs.getString("Work_Comp_ID"));
		usr.setSalutation(rs.getString("Salutation"));
		usr.setFirstName(rs.getString("First_name"));
		usr.setLastName(rs.getString("Last_Name"));
		usr.setMiddleName(rs.getString("Middle_Name"));
		usr.setDisplayName(rs.getString("First_name") +" "+rs.getString("Last_Name"));
		usr.setEmail(rs.getString("Email"));
		usr.setUserName(rs.getString("login"));	
		usr.setPassword(rs.getString("Password"));
		usr.setMobileNo(rs.getString("cell_ph_num"));		
		usr.setWorkNo(rs.getString("Work_ph_num"));
		usr.setTitle(rs.getString("Job_Title"));
		usr.setOrgName(rs.getString("Company_Name"));
		usr.setxAEGISNo(rs.getString("X_aeg_Person_ID"));
		String rowModDt =  rs.getString("Mod_DT");
		String loginUnlockDt =  rs.getString("LOGIN_UNLOCK_DATE");		
		if(rowModDt != null && rowModDt.equals(loginUnlockDt))
			usr.setPwdModFlg("Y");	
		else
			usr.setPwdModFlg("N"); 
		usr.setMember(rs.getString("Member"));
		usr.setActiveFlg(rs.getString("row_status"));
		usr.setTermsviewed(rs.getString("termsviewed"));
		usr.setNetworkID(rs.getString("Network_login_name"));
		usr.setExternalCompanyId(rs.getString("EXTERNAL_COMPANY_ID"));
		usr.setExternalCompanyName(rs.getString("EXTERNAL_COMPANY_NAME"));
		return usr;
	}

	public void updateWithProfileIDs(List<EventsDTO> eventList) {
		Set<String> loginSet = new HashSet<>();
		Map<String, String> emailToIdMap = new HashMap<>();
	
		for (EventsDTO evt : eventList) 
			loginSet.add(evt.getLogin());

		String loginLst = loginSet.stream()
				.map(s -> "'" + s + "'") // Enclose each element in single quotes
				.collect(Collectors.joining(","));
		logger.info("List of users to sync = {}", loginLst);

		String sql = "SELECT distinct ID, LOGIN from aegis1.DPS_USER where login in ("+loginLst+") or email in ("+ loginLst +") or network_login_name in ("+ loginLst +")";

		try (Connection con = new DBConnection(env).getConnection(); Statement stmt = con.createStatement()) {

			ResultSet rs = stmt.executeQuery(sql);           
			while (rs.next()) {			   
				emailToIdMap.put(rs.getString("LOGIN"), rs.getString("ID"));
				//logger.debug("email = {} and ID = {}", rs.getString("login"), rs.getString("ID"));
			}
		}
		catch (Exception e) {
			logger.error("Error fetching profileIDs for Emails: {}", e.getMessage());
		}

		for (int i=0; i < eventList.size(); i++) {
			EventsDTO evtObj = eventList.get(i);
			String profileId = emailToIdMap.get(evtObj.getLogin());
			if(profileId != null && !profileId.isEmpty())
				evtObj.setProfileId(profileId);
			else 
				evtObj.setProfileId(null);
			eventList.set(i, evtObj);
		}
	}


	public void addEvents(List<EventsDTO> eventList) {
		String sql = "INSERT INTO DSS_DPS_EVENT (ID, TIMESTAMP, SESSIONID, PROFILEID) "
				+ "VALUES (?, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?)";

		try (Connection con = new DBConnection(env).getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			int commitInd =0;
			for (EventsDTO evt : eventList) {				

				if (evt.getProfileId() != null) {
					ps.setString(1, evt.getId());
					ps.setString(2, evt.getTimestamp());
					ps.setString(3, evt.getSessionId() != null ? evt.getSessionId() : "");
					ps.setString(4, evt.getProfileId());
				//	logger.debug("evt details = {}",evt.toString());
					ps.addBatch();
					commitInd++;
					if(commitInd == 25)  {
						int[] inserted = ps.executeBatch();
						ps.clearBatch();
						logger.info("Inserted {} events into DSS_DPS_EVENT", commitInd);
						commitInd = 0;
					}
				} else {
					logger.warn("No user ID found for email: {}", evt.getEmail());
				}				
			}
			if(commitInd > 0)  {
				int[] inserted = ps.executeBatch();
				ps.clearBatch();
				logger.info("Inserted {} events into DSS_DPS_EVENT", commitInd);
			}	

		} catch (Exception e) {
			logger.error("Error inserting events: {}", e.getMessage(), e);
		}		
	}
}
