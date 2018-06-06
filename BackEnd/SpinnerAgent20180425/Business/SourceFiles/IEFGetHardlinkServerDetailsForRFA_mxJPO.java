/*
**  IEFGetHardlinkServerDetailsForRFA
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to determine the hardlink server details for a given location
*/

import java.util.Hashtable;

import matrix.db.Context;

public class IEFGetHardlinkServerDetailsForRFA_mxJPO
{
	//Location-HardlinkServerDetails map
	private Hashtable locationHardlinkServerDetailsMapping	= new Hashtable();

	public IEFGetHardlinkServerDetailsForRFA_mxJPO ()
    {	
		
		/*************************************************************************************
		These are the sample settings for a default 'STORE', store and location. If more stores or locations need to be added,
		please refer to the sample settings below and change the sample settings for default 'STORE', store and location accordingly.
		*************************************************************************************/
		Hashtable defaultSetting = new Hashtable();
		//This could be MCS url, if the path for the default STORE is on MCS.
		//Change the port (8080) to appropriate value.
		defaultSetting.put("HardlinkServerURL", "http://mcs_server_name:8080/mcs_webapp_name");
		defaultSetting.put("ShadowUser", "creator|");
		/*Below two lines can be commented if all users are using UNC path.
		Otherwise, all users have to map hardlinks directory (say c:\Hardlinks) to below mentioned drive.
		This drive should be same in all store definitions.*/
		defaultSetting.put("MappedDrive", "E:");//For Windows clients
		defaultSetting.put("MountPoint", "/data/mount"); //For UNIX clients only
		//This is the absolute path where checked in files goes for this (default) store.
		defaultSetting.put("StorePath", "E:/ENOVIA/STUDIO/Apps/SchemaInstaller/R207/STORE");		
		//The 'key' should be actual name of STORE
		locationHardlinkServerDetailsMapping.put("STORE", defaultSetting);

		
		/********************************************
		These are sample store and location settings 
		*********************************************
		//STORE Settings
		Hashtable STORE_1 = new Hashtable();
		//This should be RFA url
		STORE_1.put("HardlinkServerURL", "http://rfa_server_name:rfa_server_port/rfa_webapp_name");
		STORE_1.put("ShadowUser", "creator|");
		//Below two lines can be commented if all users are using UNC path.
		//Otherwise, all users have to map hardlinks directory (say c:\Hardlinks) to the below mentioned drive.
		STORE_1.put("MappedDrive", "E:");//For Windows clients
		STORE_1.put("MountPoint", "/data/mount"); //For UNIX clients only
		//This is the absolute path where checked in files goes for this (default) store.
		STORE_1.put("StorePath", "E:/ENOVIA/STUDIO/Apps/SchemaInstaller/R207/STORE");
		//The 'key' should be actual name of the store
		locationHardlinkServerDetailsMapping.put("<actaul name of STORE>", STORE_1);
		
		//LOCATION Settings
		Hashtable LOCATION_1 = new Hashtable();
		//This should be RFA url since this is location definition.
		LOCATION_1.put("HardlinkServerURL", "http://rfa1_server_name:rfa1_server_port/rfa1_webapp_name");
		LOCATION_1.put("ShadowUser", "creator|");
		//Below two lines can be commented if all users are using UNC path.
		//Otherwise, all users have to map hardlinks directory (say c:\Hardlinks) to the below mentioned drive.
		LOCATION_1.put("MappedDrive", "E:");//For Windows clients
		LOCATION_1.put("MountPoint", "/data/mount"); //For UNIX clients only
		//This is the absolute path where checked in files goes for this (default) location.
		LOCATION_1.put("StorePath", "E:/ENOVIA/STUDIO/Apps/SchemaInstaller/R207/location");
		//The 'key' should be actual name of the location
		locationHardlinkServerDetailsMapping.put("<actaul name of location>", LOCATION_1);*/
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	public Hashtable getHardlinkServerDetailsForRFA(Context context, String[] args) throws Exception
	{
		Hashtable hardlinkServerLocationDetails = new Hashtable();

		for(int i=0; i< args.length; i++)
		{
			String locationName = args[i];                                                 
			if(locationName != null)
			{
				if(locationHardlinkServerDetailsMapping.containsKey(locationName.trim()))
				{
					hardlinkServerLocationDetails.put(locationName, (Hashtable)locationHardlinkServerDetailsMapping.get(locationName));
				}
			}
		}

		return hardlinkServerLocationDetails;
	}
}
