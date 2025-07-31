package com.aegis.portal.migration;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegis.portal.db.DBService;
import com.aegis.portal.model.EventsDTO;
import com.aegis.portal.model.PortalUser;
import com.aegis.portal.okta.OktaService;
import com.aegis.portal.util.CommonUtil;
/*import com.okta.sdk.client.Client;*/
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.client.ApiClient;

public class UserSync {
	private static Logger logger = LoggerFactory.getLogger(UserSync.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub	    
		String portalEnv = null;
		String oktaEnv = null;
		if(args.length == 2) {
			portalEnv = args[0];
			oktaEnv = args[1];
		} else 
		{
			logger.error("Invalid Arguments. Required Portal Environment and Okta Environment");
			System.exit(0);
		}

		DBService dbService = new DBService(portalEnv);		
		OktaService oktaService =  new OktaService(oktaEnv);
		ApiClient client = null;
		try {
			client = oktaService.getClient(oktaEnv);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  
		List<PortalUser> externalUserList = dbService.getExternalUserList();	
		
		if(externalUserList != null && externalUserList.size() > 0)  {
			Iterator <PortalUser> iterator = externalUserList.iterator();
			PortalUser extUser =  null; 
			boolean syncStatus = false;
			while(iterator.hasNext()) {
				extUser = iterator.next();
				syncStatus = oktaService.syncExternalUser(client, extUser);
				logger.info("Sync Status of User {} is {}",extUser.getUserName(),syncStatus);

			}
			logger.info("External User sync completed and starting Internal User sync");
		} else {
			logger.info("No external Users to sync.");
		}
		
		List<PortalUser> internalUserList = dbService.getInternalUserList();
		
		if(internalUserList != null && internalUserList.size() >0) {
			Iterator <PortalUser>  iterator = internalUserList.iterator();
			PortalUser intUser =  null;
			boolean syncStatus = false;
			while(iterator.hasNext()) {
				intUser = iterator.next();
				syncStatus = oktaService.syncInternalUser(client, intUser);
				logger.info("Sync Status of User {} is {}",intUser.getUserName(),syncStatus);

			}
			logger.info("Internal User sync completed, saving Last Sync Date");
		} else {
			logger.info("No internal Users to  sync.");
		}
		

		// Step 3: Fetch Okta events since last sync
		logger.info("Syncing Login and Logout timestamps to AEGISlink");
		List<EventsDTO> events = oktaService.fetchEvents();
		if (!events.isEmpty()) {
			dbService.updateWithProfileIDs(events);
			dbService.addEvents(events);
			
		} else {
			logger.info("No new events fetched.");
		}

		
		// once the sync is successful, update the current date in lastRunDate file.
		CommonUtil commonUtil =  new CommonUtil();
		commonUtil.saveLastSyncDate();

	}

}
