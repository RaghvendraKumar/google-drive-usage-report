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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DriveSdk {

	/** Email of the Service Account */
	private String SERVICE_ACCOUNT_EMAIL;
	/** Path to the Service Account's Private Key file */
	private String SERVICE_ACCOUNT_PKCS12_FILE_PATH;
	/** OAuth2 scopes */
	private List<String> SCOPES;

	
	/**
	 * Class constructor
	 * @param s_a_e Service account email
	 * @param s_a_p12_f_p Service account PKCS12 file path
	 * @param scopes OAuth2 scopes needed
	 */
	public DriveSdk(String s_a_e, String s_a_p12_f_p, List<String> scopes) {
		SERVICE_ACCOUNT_EMAIL = s_a_e;
		SERVICE_ACCOUNT_PKCS12_FILE_PATH = s_a_p12_f_p;
		SCOPES = scopes;
	}
	
	/**
	 * Builds and returns a Drive service object authorized with the service
	 * accounts that act on behalf of the given user.
	 * 
	 * @param userEmail The email of the user to be impersonated
	 * @return Drive service object that is ready to make requests.
	 */
	public Drive getDriveService(String userEmail)
	{
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

			// Drive service = new Drive.Builder(httpTransport, jsonFactory, null)
			// .setHttpRequestInitializer(credential).build();
			Drive service = new Drive.Builder(httpTransport, jsonFactory, credential)
							.setApplicationName("Backup Files").build();
			
			service.about().get().execute();
			
			return service;

		} catch (GeneralSecurityException | IOException e) {
			Logger.getLogger("DriveReport").severe("Unable to get the Drive Service for " + userEmail);
			return null;
		}
	}

	/**
	 * Retrieve a list of File resources.
	 * 
	 * @param service Drive API service instance.
	 * @return List of File resources.
	 */
	public static List<File> retrieveAllFiles(Drive service, String query) 
			throws IOException {
		List<File> result = new ArrayList<File>();
		//Files.List request = service.files().list();
		Files.List request = service.files().list().setQ(query); //"trashed = false"

		do {
			try {
				FileList files = request.execute();
				result.addAll(files.getItems());
				request.setPageToken(files.getNextPageToken());
			} 

			catch (IOException e) {
				//System.out.println("Error al intentar recuperar los ficheros del Drive: " + e);
				request.setPageToken(null);
				result = null;
			}
			
		} while (request.getPageToken() != null
				&& request.getPageToken().length() > 0);

		return result;
	}

	/**
	 * Retrieve the 10 first File resources.
	 * 
	 * @param service Drive API service instance.
	 * @return Number of File resources.
	 */
	public int retrieve10FirstFiles(Drive service, String query) {
		
		int n_files;

		try {
			n_files = service.files().list().setMaxResults(10).execute().getItems().size();
		}

		catch (IOException e) {
			//System.out.println("Error al intentar recuperar los ficheros del Drive: " + e);
			return -1;
		}

		return n_files;
	}

	/**
	 * Print information about the current user along with the Drive API
	 * settings.
	 * 
	 * @param service Drive API service instance.
	 */
	public static void printAbout(Drive service) {
		try {
			About about = service.about().get().execute();

			System.out.println("Current user name: " + about.getName());
			System.out.println("Root folder ID: " + about.getRootFolderId());
			System.out.println("Total quota (bytes): "
					+ about.getQuotaBytesTotal());
			System.out.println("Used quota (bytes): "
					+ about.getQuotaBytesUsed());
			System.out.println("Quota left (bytes): "
					+ (about.getQuotaBytesTotal() - about.getQuotaBytesUsed()));
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}
}
