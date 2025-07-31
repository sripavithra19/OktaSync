package com.aegis.portal.util;

public interface IConstants {
	public String PERIOD = ".";
	public String DEFAULT_PWD_SET_SSIS = "aegis123";
	public String saltOrder = "POSTFIX";	
	public String GROUP_LIST_QRY = "select G.group_code grpCD from aegis1.ag_website_groups_members GM "
						    		+ "Join aegis1.ag_website_groups G on G.group_id =  GM.group_id "
						    		+ "where GM.user_id = :1 and GM.row_status = :2 "
						    		+ " union "
						    		+ " select decode(CR.role_id,"
						    		+ " 1,'claims_manager_group',"
						    		+ " 2,'aegis_employee_group',"
						    		+ " 3,'risk_manager_group',"
						    		+ " 4,'broker_group',"
						    		+ " 5,'underwriter_group', "
						    		+ " 6,'',"
						    		+ " 7,'assistant_underwriter_group',"
						    		+ " 8,'aegis_consultant_group',"
						    		+ " 9,'principal_risk_manager_group',"
						    		+ " 10,'online_app_team_member_group',"
						    		+ " 11,'claims_task_force_group',"
						    		+ " 12,'lc_wfm_access',"
						    		+ " 13,'loss_control_task_force_group',"
						    		+ " 14,'rmac_member_group',"
						    		+ " 15,'claims_viewer_group',"
						    		+ " 16,'policy_viewer_group',"
						    		+ " 17,'inactive_company_policy_viewer',"
						    		+ " 18,'My eRisk Assessments RMS',"
						    		+ " role_type) from  aegis1.ag_user_Company_role CR, aegis1.ag_role R where "
						    		+ " CR.role_id = R.role_id and CR.row_status = :3 "
						    		+ " and User_id = :4";		
}
