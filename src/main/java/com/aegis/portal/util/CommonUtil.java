package com.aegis.portal.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonUtil {
	private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);
	
	public String getLastSyncDate() {
		String syncDtStr = "";
	 try {
		InputStream ios = this.getClass().getClassLoader().getResourceAsStream("lastRunDate.properties");
		BufferedReader bufReader = null;
		if(ios == null) return null;
		if(ios != null) {
			bufReader = new BufferedReader(new InputStreamReader(ios));
			syncDtStr = bufReader.readLine();
			ios.close();
		}
		
	 } catch(IOException e) {
		 logger.error(e.getMessage());
	 }
	 return syncDtStr;
	}
	
	public void saveLastSyncDate() {
		String syncDtStr = "";
	 try {
		Date date = new Date();  
		//SimpleDateFormat formatter = new SimpleDateFormat("yy-MMM-dd"); 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		syncDtStr = formatter.format(date);
		URL url = this.getClass().getClassLoader().getResource("lastRunDate.properties");
		File file = null;
		try {
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(file == null) {
			logger.error("File lastRunDate.properties is null so not saving lastSync date");
			return;
		}
		BufferedWriter bufWriter = new BufferedWriter(new FileWriter(file));
		
		bufWriter.write(syncDtStr);
		logger.info("Last Sync date is updated to {}", syncDtStr);
		
		if(bufWriter != null) {
			bufWriter.close();
		}
		
	 } catch(IOException e) {
		 logger.error(e.getMessage());
	 }

	}
	
	public String getBase64FromHEX(String input) {

		byte barr[] = new byte[16];
		int bcnt = 0;
		for (int i = 0; i < 32; i += 2) {
			char c1 = input.charAt(i);
			char c2 = input.charAt(i + 1);
			int i1 = intFromChar(c1);
			int i2 = intFromChar(c2);

			barr[bcnt] = 0;
			barr[bcnt] |= (byte) ((i1 & 0x0F) << 4);
			barr[bcnt] |= (byte) (i2 & 0x0F);
			bcnt++;
		}
		byte base64Arry[]  = Base64.getEncoder().encode(barr);
		String base64Str = new String(base64Arry, StandardCharsets.UTF_8);	
		return base64Str;
	}
	private int intFromChar(char c) {
		char[] carr = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		char clower = Character.toLowerCase(c);
		for (int i = 0; i < carr.length; i++) {
			if (clower == carr[i]) {
				return i;
			}
		}

		return 0;
	}
	
	
	public byte[] generateSalt() {      
        final Random RANDOM = new SecureRandom();
        byte bytes[] = new byte[20];
        RANDOM.nextBytes(bytes);
       // String saltStr = new String(bytes, StandardCharsets.UTF_8);
        return bytes;
    }



}
