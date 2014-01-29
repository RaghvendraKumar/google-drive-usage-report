/**
 * Author:		Carlos Lallana
 * Last change:	2014/01/10
 * Version:		1.0		
 */

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.admin.directory.*;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;

public class AdminSdk {

	/** Email of the Service Account */
	private String SERVICE_ACCOUNT_EMAIL;
	/** Path to the Service Account's Private Key file */
	private String SERVICE_ACCOUNT_PKCS12_FILE_PATH;
	/** OAuth2 scopes */
	private List<String> SCOPES;
	/** Main domain */
	private String DOMAIN;
	
	/**
	 * Class constructor
	 * @param s_a_e Service account email
	 * @param s_a_p12_f_p Service account PKCS12 file path
	 * @param scopes OAuth2 scopes needed
	 */
	public AdminSdk(String s_a_e, String s_a_p12_f_p, List<String> scopes, String d) {
		SERVICE_ACCOUNT_EMAIL = s_a_e;
		SERVICE_ACCOUNT_PKCS12_FILE_PATH = s_a_p12_f_p;
		SCOPES = scopes;
		DOMAIN = d;
	}
	
	/**
	 * Builds and returns a Directory service object authorized with the service
	 * accounts that act on behalf of the given user.
	 * 
	 * @param userEmail The email of the user to be impersonated
	 * @return Directory service object that is ready to make requests.
	 */
	public Directory getDirectoryService(String userEmail) {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();

		GoogleCredential credential;
		try {
			credential = new GoogleCredential.Builder()
					.setTransport(httpTransport)
					.setJsonFactory(jsonFactory)
					.setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
					.setServiceAccountScopes(SCOPES)
					.setServiceAccountPrivateKeyFromP12File(
							new java.io.File(SERVICE_ACCOUNT_PKCS12_FILE_PATH))
					.setServiceAccountUser(userEmail)
					.build();
			
			Directory service = new Directory.Builder(httpTransport, jsonFactory,
					credential).setApplicationName("Backup GMail").build();

			return service;
			
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
			Logger.getLogger("DriveReport").severe("Unable to get the Directory Service.");
			return null;
		}
	}
	
	/**
	 * Retrieve a list of User resources.
	 * 
	 * @param service Drive API service instance.
	 * @return List of User resources.
	 */
	public List<User> retrieveAllUsers(Directory service, String query) 
	{
		List<User> result = new ArrayList<User>();
		Directory.Users.List list;
		
		try {
			list = service.users().list().setQuery(query).setSortOrder("ASCENDING");

			do {
				list.setDomain(DOMAIN);
				Users users = list.execute();
				result.addAll(users.getUsers());
				list.setPageToken(users.getNextPageToken());
				
			} while (list.getPageToken() != null
					&& list.getPageToken().length() > 0);
	
			return result;
			
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger("DriveReport").severe("Unable to retrieve the users.");
			return null;
		}
	}
}
