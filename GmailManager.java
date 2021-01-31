package com.gapi;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class GmailManager
{
    private static final String APPLICATION_NAME = "APIPROJECT App";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    
    //Reference to credentials.json file
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json"; 
    
    //Folder to hold the tokens generated during the program execution
    private static final String TOKENS_DIRECTORY_PATH = 
    		System.getProperty("user.dir") +
             File.separator + "src" +
             File.separator + "main" +
             File.separator + "resources";

    /**
     * Initialize GmailManager utility
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential init(final NetHttpTransport HTTP_TRANSPORT) throws IOException 
    {
    	//Load client secrets using credentials.json
        InputStream in = GmailManager.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null)
        {
            throw new FileNotFoundException("GmailManager:init: Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        
        //Obtain client secret from the credentials store
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        //Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        		.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        //Initialize the ability to receive an authorization and information
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Return a handle to the Gmail API/Service
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private Gmail getGmailAPIService()
    {
    	try
    	{
	        // Build a new authorized API client service.
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	        Gmail objService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, init(HTTP_TRANSPORT))
	                .setApplicationName(APPLICATION_NAME)
	                .build();
	        return objService;
    	}
    	catch(IOException objIOE)
    	{
    		//Log the message below
    		System.out.println("GmailManager:getGmailAPIService: IO Exception in Obtaining handle to GMAIL API: " + objIOE.getMessage());
    		return null;
    	}
    	catch(GeneralSecurityException objGSE)
    	{
    		//Log the message below
    		System.out.println("GmailManager:getGmailAPIService: General Security Exception in Obtaining handle to GMAIL API: " + objGSE.getMessage());
    		return null;
    	}
    }
    
    /**
     * Retrieve a finite list of email messages (based on upper limit)
     * @param objService
     * @param strUser
     * @param maxLimit
     * @return
     */
    private List<Message> getMessagesUptoLimit(Gmail objService, String strUser, Long maxLimit)
    {
    	try
    	{
    		Gmail.Users.Messages.List objRequest =  objService.users().messages().list(strUser).setMaxResults(maxLimit);
    		//Execute the query
    		ListMessagesResponse objResponse = objRequest.execute();
    		
    		if(objResponse.getMessages() != null)
    		{
    			//Placeholder list to hold Gmail messages
    			List<Message> lstMessages = new ArrayList<Message>();
    			lstMessages.addAll(objResponse.getMessages());
    			return lstMessages;
    		}
    		else
    		{
        		System.out.println("GmailManager:getMessagesUptoLimit: No emails retieved");
    			return null;
    		}
    	}
    	catch(IOException objIOE)
    	{
    		//Log the message below
    		System.out.println("GmailManager:getMessagesUptoLimit: IOException - " + objIOE.getMessage());
    		return null;
    	}
    }
    
    /**
     * Get details of a single GMAIL Message
     * @param objService
     * @param strUser
     * @param strMsgId
     * @return
     */
    private Message getMessage(Gmail objService, String strUser, String strMsgId)
    {
    	try
    	{
        	//Retrieve the message detail from Gmail
        	return objService.users().messages().get(strUser, strMsgId).execute();
    	}
		catch(IOException objIOE)
		{
			//Log the below error
			System.out.println("GmailManager:getMessage: IOException - " + objIOE.getMessage());
			return null;
		}
    }
    
    /**
     * Action !!
     * @param args
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static void main(String... args) throws IOException, GeneralSecurityException 
    {
        GmailManager objGmailMgr = new GmailManager();

        //Create GMAIL Service handle using the Credentials object
        Gmail objService = objGmailMgr.getGmailAPIService();

        //If Gmail API handle could not be created, then exit the program
        if(null == objService)
        {
        	System.out.println("GmailManager:main: Gmail API not accessible");
        	System.exit(-1);
        }
        
        //Let the authenticated user be used
        String strUser = "me";

		//Placeholder list to hold Gmail messages
		List<Message> lstMessages = objGmailMgr.getMessagesUptoLimit(objService, strUser, Long.valueOf(10));
		
		if(lstMessages != null) 
		{
			//Store all messages to the list
			System.out.println("GmailManager:main: No of messages is: " + lstMessages.size());

			for (Message objMsg : lstMessages)
	        {
	        	//Get the message Id
	        	String strMsgId = objMsg.getId();
	        	
	        	//Retrieve the message detail from Gmail
	        	Message objRealMsg = objGmailMgr.getMessage(objService, strUser, strMsgId);
	        	
	        	if(objRealMsg != null)
	        	{
		            //Get the subject through header
		        	MessagePart msgPart = objRealMsg.getPayload();
		            String msgSubject = "";    
		            if (msgPart != null)
		            {
		               List<MessagePartHeader> lstHeaders = msgPart.getHeaders();
		               for (MessagePartHeader objHeader : lstHeaders) 
		               {
		                  //find the subject header and print it
		                  if (objHeader.getName().equals("Subject")) 
		                  {
		                	 msgSubject = objHeader.getValue().trim();
		                	 System.out.println("GmailManager:main: Email Subject: " + msgSubject);
		                     break;
		                  }
		               }
		            }
	            }
	            else
	            {
	            	System.out.println("GmailManager:main: Message data not retrieved for Msg ID " + strMsgId);
	            }
	        }
	    }
		else
		{
			System.out.println("GmailManager:main: No messages recieved");
		}
    }
}