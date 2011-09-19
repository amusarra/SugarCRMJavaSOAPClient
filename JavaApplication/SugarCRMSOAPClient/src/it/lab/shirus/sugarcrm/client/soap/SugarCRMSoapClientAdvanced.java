/**
 * 
 */
package it.lab.shirus.sugarcrm.client.soap;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.ServiceFactory;

import org.apache.axis.AxisFault;

import com.sugarcrm.www.sugarcrm.Entry_value;
import com.sugarcrm.www.sugarcrm.Get_entries_count_result;
import com.sugarcrm.www.sugarcrm.Get_entry_list_result_version2;
import com.sugarcrm.www.sugarcrm.Link_name_to_fields_array;
import com.sugarcrm.www.sugarcrm.Name_value;
import com.sugarcrm.www.sugarcrm.SugarsoapBindingStub;
import com.sugarcrm.www.sugarcrm.SugarsoapLocator;
import com.sugarcrm.www.sugarcrm.User_auth;

/**
 * SugarCRM SOAP Client Example Class.
 * 
 * @author Antonio Musarra (antonio.musarra@gmail.com)
 */
public class SugarCRMSoapClientAdvanced {
	private static final String END_POINT_API_URL = "/service/v2/soap.php?wsdl";
	private static final String USER_NAME = "will";
	private static final String USER_PASSWORD = "will";
	private static final String APPLICATION_NAME = Class.class.getName();
	private static final Integer TIMEOUT = 6000;


	/**
	 * Main Program
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {		
		String sugarCRMUrl = null;
		String userName = null;
		String password = null;
		String userId = null;

		URL wsdlUrl = null;
		String sessionID = null;

		if (args.length != 3) {
			System.err.println("Usage: java SugarCRMSoapClientAdvanced {SugarCRM URL} {user name} {password}");
		    System.exit(1);
		} else {
			try {
				// Create a URL end point for the client
				sugarCRMUrl = args[0];
				
				if (sugarCRMUrl.isEmpty()) {
					wsdlUrl = new URL(
							new SugarsoapLocator().getsugarsoapPortAddress()
									+ "?wsdl");
				} else {
					wsdlUrl = new URL(sugarCRMUrl + END_POINT_API_URL);
				}

				System.out.println("URL endpoint created successfully!");
				System.out.println("URL endpoint: " + wsdlUrl.toString());
				
			} catch (MalformedURLException e) {
		         System.err.println("Malformed URL Exception");
		         System.err.println(e.getMessage());
		         System.exit(1);
			}			
		}
		
		try {

			// Create Service for test configuration
			ServiceFactory serviceFactory = ServiceFactory.newInstance();
			Service service = serviceFactory.createService(wsdlUrl,
					new SugarsoapLocator().getServiceName());

			System.out.println("Service created successfully");
			System.out.println("Service Name:"
					+ service.getServiceName().toString());
			System.out.println("Service WSDL:"
					+ service.getWSDLDocumentLocation().toString());

			// Trying to create a stub
			SugarsoapBindingStub binding = new SugarsoapBindingStub(wsdlUrl,
					service);
			binding.setTimeout(TIMEOUT);
			System.out.println("Stub created successfully!");

			System.out.println("SugarCRM Server Info...");
			System.out.println("Version " + binding.get_server_info().getVersion());
			System.out.println("Flavor " + binding.get_server_info().getFlavor());
			
			/**
			 * Try to login on SugarCRM
			 * 
			 * 1) Prepare a MD5 hash password 
			 * 2) Prepare a User Auth object 
			 * 3) Execute login
			 */

			// 1. Prepare a MD5 hash password
			MessageDigest messageDiget = MessageDigest.getInstance("MD5");
			password = (args[2].isEmpty()) ? USER_PASSWORD : args[2];
			messageDiget.update(password.getBytes());

			// 2. Prepare a User Auth object
			User_auth userAuthInfo = new User_auth();
			userName = (args[1].isEmpty()) ? USER_NAME : args[1];
			userAuthInfo.setUser_name(userName);
			userAuthInfo.setPassword((new BigInteger(1, messageDiget.digest()))
					.toString(16));

			try {
				// 3. Execute login
				Entry_value loginResult = binding.login(userAuthInfo,
						APPLICATION_NAME, null);
				System.out.println("Login Successfully for " + userName);
				System.out.println("Your session Id: " + loginResult.getId());
				sessionID = loginResult.getId();
			} catch (RemoteException e) {
				System.err.println("Login failed. Message: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}

			/**
			 * Get UserId from SessionID
			 */
			try {
				userId = binding.get_user_id(sessionID);
				System.out.println("Your UserId is: " + userId);
			} catch (RemoteException e) {
				System.err.println("Get UserId failed. Message: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
			
			/**
			 * Getting an Contacts Entry
			 * 	1) Get entries count
			 * 	2) Get Contacts
			 */

			
			// Trying to Get entries count
			Get_entries_count_result getEntriesCountResponse = null;

			try {
				getEntriesCountResponse = binding.get_entries_count(sessionID, "Contacts", "created_by=" + userId, 0);
				System.out.println("Get entries count was successful! Response: " + getEntriesCountResponse.getResult_count());
			} catch (RemoteException e) {
				System.err.println("Get entries count failed. Message: " + e.getMessage());
				e.printStackTrace();
			}

			// Trying to Get Contacts Entry
			Get_entry_list_result_version2 getEntryListResponse = null;
			Link_name_to_fields_array[] link_name_to_fields_array = null;
			String[] select_fields = null;

			try {
				getEntryListResponse = binding.get_entry_list(sessionID, "Contacts", "contacts.created_by=" + userId, "id", 0, select_fields, link_name_to_fields_array, 100, 0);
				Entry_value[] entryList = getEntryListResponse.getEntry_list();
				for (int k=0; k < entryList.length; k++){
					Entry_value entry = entryList[k];
					Name_value[] entryNameValueList = entry.getName_value_list();
					System.out.println("# Record n¡ " + k);
					for (int j=0; j< entryNameValueList.length; j++){
						Name_value entryNameValue = entryNameValueList[j];
						//Outputting only non empty fields
						if (!entryNameValue.getValue().isEmpty()){
							System.out.println("Attribute Name: '" + entryNameValue.getName() + "' Attribute Value: '" + entryNameValue.getValue() + "'");
						}
					}
				}

			} catch (RemoteException e) {
				System.err.println("Get entry list failed. Message: " + e.getMessage());
				e.printStackTrace();
			}
			
			/**
			 * Logout
			 */
			try {
				binding.logout(sessionID);
				System.out.println("Logout Successfully for " + userName);
				sessionID = null;
			} catch (RemoteException ex) {
				System.out.println("Logout failed. Message: " + ex.getMessage());
				ex.printStackTrace();
				System.exit(1);
			}			
		} catch (ServiceException ex) {
			System.err.println("Service creation failed. Message: "
					+ ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		} catch (AxisFault ex) {
			System.err.println("AxisFault. Message: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
	}
}