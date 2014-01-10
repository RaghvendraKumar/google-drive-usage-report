/**
 * Author:		Carlos Lallana
 * Last change:	2014/01/10
 * Version:		1.0		
 */

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.drive.Drive;

public class Main 
{
	/** CHANGE THIS VALUES WITH YOUR OWN CONFIGURATION */
	
	/** Email of the Service Account */
	private static final String SERVICE_ACCOUNT_EMAIL = "your_key@developer.gserviceaccount.com";
	/** Path to the Service Account's Private Key file */
	private static final String SERVICE_ACCOUNT_PKCS12_FILE_PATH = "some-folder/privatekey.p12";
	/** OAuth2 scopes */
	private static final List<String> SCOPES = Arrays.asList(	"https://www.googleapis.com/auth/drive",
																"https://www.googleapis.com/auth/admin.directory.user");
	
	/** Email of the admin account */
	private static final String ADMIN_ACCOUNT_EMAIL = "admin@your-domain.com";
	
	// Global variables
	static DriveSdk drive_sdk;
	static AdminSdk admin_sdk;
	static SimpleDateFormat date_format;

	public static void main(String[] args) 
	{
		/** Enable and configure this 2 lines if executing through proxy */
		//System.getProperties().put("http.proxyHost", "10.*.*.*"); 
		//System.getProperties().put("http.proxyPort", "8080");
	
		System.out.println(" -*- STARTING DRIVE USAGE ANALYSIS -*-");
		System.out.println("(... This process may take hours...)\n");
		
		// Date format for the log title
		date_format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		
		// Create the logcat
		try {
			String log_title = "log_" + date_format.format(new Date()) + ".log";
			Handler handler = new FileHandler(log_title);
			handler.setFormatter(new SimpleFormatter());
			Logger.getLogger("DriveReport").addHandler(handler);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			Logger.getLogger("DriveReport");
		}
		
		// Initializate DriveSdk and AdminSdk
		drive_sdk = new DriveSdk(SERVICE_ACCOUNT_EMAIL, SERVICE_ACCOUNT_PKCS12_FILE_PATH, SCOPES);
		admin_sdk = new AdminSdk(SERVICE_ACCOUNT_EMAIL, SERVICE_ACCOUNT_PKCS12_FILE_PATH, SCOPES);
		
		generateReport();
	}
	
	
	/**
	 * Generates a complete report of the number of files each user of the 
	 * domain owns, along with its email, full name, and joining date.
	 */
	private static void generateReport(){
		Logger logger = Logger.getLogger("DriveReport");
	
		logger.info("RETRIEVING USERS...");
		
		// Get the Directory service
		Directory admin_service = admin_sdk.getDirectoryService(ADMIN_ACCOUNT_EMAIL);
		if (admin_service == null){
			logger.severe("Unable to get the admin Drive service.");
			System.exit(-1);
		}
		
		/* Retrieve all the users from the domain.
		 * Hint: To get all the users, the second parameter should be null;
		 * for just one user, should be "email:user-name@your-domain.com*" */
		List<User> all_users = admin_sdk.retrieveAllUsers(admin_service, null);
		if (all_users == null)
			System.exit(-1);
		
		logger.info("Retrieved " + all_users.size() + " users in th domain.\n");
		
		logger.info("RETRIEVING FILES OF EACH USER...\n");

		// Some statistical variables
		int n_files;
		int n_users_using_drive = 0;
		int n_users_not_using_drive = 0;
		int n_suspended_users = 0;
		List<User> error_users = new ArrayList<User>();

		Drive service;
		int flushing = 0;

		for (User user : all_users){
			service = drive_sdk.getDriveService(user.getPrimaryEmail());
			
			if (user.getSuspended())
				n_suspended_users++;
			
			else{
				n_files = drive_sdk.retrieve10FirstFiles(service, null);
				
				if (n_files == -1){
					error_users.add(user);
				}
				else {
					if (n_files > 0)
						n_users_using_drive++;
					else
						n_users_not_using_drive++;
					
					long bytes_used;
					long bytes_total;
					try {
						bytes_used = service.about().get().execute().getQuotaBytesUsed();
						bytes_total = service.about().get().execute().getQuotaBytesTotal();
					} catch (Exception e) {
						bytes_used = -1;
						bytes_total = -1;
					}
					
					System.out.println(	user.getPrimaryEmail() + "," + 
										user.getName().getFullName() + "," +
										n_files + "," +
										bytes_used + "," + bytes_total + "," +
										user.getCreationTime());
					
					FileWriter csv;
					try {
						csv = new FileWriter("report_" + date_format.format(new Date()) + ".csv");
						
						csv.append(	"EMAIL,NAME,N_FILES,USED_STORAGE_bytes," +
									"TOTAL_STORAGE_bytes,DATE_CREATED\n");
						
						csv.append(	user.getPrimaryEmail() + "," + 
								user.getName().getFullName() + "," +
								n_files + "," +
								bytes_used + "," + bytes_total + "," +
								user.getCreationTime() + "\n");
					
					flushing++;
					if (flushing >= 100){
						csv.flush();
						flushing = 0;
					}
					
					csv.close();
					} catch (IOException e) {
						e.printStackTrace();
						logger.severe("Unable to generate CSV.");
					}
				}
			}
		}
		
		String end_report = "END OF REPORT: ";
		end_report += ("\n - USERS WITH AT LEAST 1 FILE: " + n_users_using_drive +
						"\n - USERS WITH NO FILES: " + n_users_not_using_drive +
						"\n - SUSPENDED USERS: " + n_suspended_users +
						"\n - ERRORS: " + error_users.size());
		
		logger.info(end_report);
		
		if (error_users.size() > 0){
			String str_users_error = "\nThe following users caused trouble:";

			for (User e_user : error_users){				
				str_users_error += (	"\n" + e_user.getPrimaryEmail() + "," + 
									e_user.getName().getFullName());
			}
			
			logger.warning(str_users_error);
		}
	}
}
