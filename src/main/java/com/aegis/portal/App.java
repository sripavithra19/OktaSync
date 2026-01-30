package com.aegis.portal;
public class App {
    public static void main(String[] args) {
        System.out.println("=== Aegis Portal Sync Application ===");

        if (args.length < 2) {
            System.out.println("Usage: java -jar aegis-portal.jar <PortalEnv> <OktaEnv> [--email-monitor]");
            System.exit(1);
        }

        String portalEnv = args[0];
        String oktaEnv = args[1];

        System.out.println("Starting user sync with:");
        System.out.println(" - Portal Environment: " + portalEnv);
        System.out.println(" - Okta Environment: " + oktaEnv);

        // Check if email monitoring is requested
        boolean enableEmailMonitoring = args.length > 2 && "--email-monitor".equals(args[2]);
        
        // Run the main user sync with only first 2 arguments
        try {
            String[] syncArgs = {portalEnv, oktaEnv}; // Pass only 2 arguments
            com.aegis.portal.migration.UserSync.main(syncArgs);
        } catch (Exception e) {
            System.err.println("User sync failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Then run email monitoring separately if enabled
        // BUT ONLY if it wasn't already run by UserSync
        if (enableEmailMonitoring) {
            System.out.println(" - Email Monitoring: ENABLED");
            runEmailMonitoring(portalEnv, oktaEnv);
        } else {
            System.out.println(" - Email Monitoring: DISABLED (use --email-monitor to enable)");
        }
        
        System.out.println("User sync process completed."); 
    }
    
    private static void runEmailMonitoring(String portalEnv, String oktaEnv) {
        try {
            com.aegis.portal.service.EmailMonitorService emailMonitor = 
                new com.aegis.portal.service.EmailMonitorService(oktaEnv, portalEnv);
            
            // Use current time minus 24 hours for monitoring
            java.time.Instant sinceTime = java.time.Instant.now().minus(java.time.Duration.ofHours(48));
            String sinceDateTime = sinceTime.toString();
            
            System.out.println("Monitoring email failures since: " + sinceDateTime);
            emailMonitor.monitorEmailFailures(sinceDateTime);
            System.out.println("Email monitoring completed successfully.");
            
        } catch (Exception e) {
            System.err.println("Email monitoring failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}