package com.aegis.portal.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtil {
	private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

	public String getLastSyncDate() {
		String syncDtStr = "";
		try {
			InputStream ios = this.getClass().getClassLoader().getResourceAsStream("lastRunDate.properties");
			BufferedReader bufReader = null;
			if (ios == null)
				return null;
			if (ios != null) {
				bufReader = new BufferedReader(new InputStreamReader(ios));
				syncDtStr = bufReader.readLine();
				ios.close();
			}

		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return syncDtStr;
	}
	


	public void saveLastSyncDate() {
	    try {
	        // Format current time in IST
	        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
	        String syncDtStr = formatter.format(new Date());

	        // Locate the properties file
	        URL url = this.getClass().getClassLoader().getResource("lastRunDate.properties");
	        if (url == null) {
	            logger.error("File lastRunDate.properties is not found in classpath");
	            return;
	        }

	        File file;
	        try {
	            file = new File(url.toURI());
	        } catch (URISyntaxException e) {
	            logger.error("Error converting URL to URI: {}", e.getMessage());
	            return;
	        }

	        // Write IST-formatted date to file
	        try (BufferedWriter bufWriter = new BufferedWriter(new FileWriter(file))) {
	            bufWriter.write(syncDtStr);
	            logger.info("Last Sync date is updated to {}", syncDtStr);
	        }

	    } catch (IOException e) {
	        logger.error("Failed to save last sync date: {}", e.getMessage(), e);
	    }
	}
}
