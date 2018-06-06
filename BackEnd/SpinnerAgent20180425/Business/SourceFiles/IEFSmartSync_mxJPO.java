/*
 **  IEFSmartSync
 **
 **  Copyright Dassault Systemes, 1992-2012.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to determine the nearest source location for a given destination location for replicating file in eFCS
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.fcs.mcs.ReplicationTrigger;


public class IEFSmartSync_mxJPO implements ReplicationTrigger{

	// Proximity map of closest source locations
	private Hashtable _proximityMap = new Hashtable();

	private boolean DEBUG = false;

	private void buildProximityMap()
	{
		/*
		   Following steps are required for setting up the proximity map of locations.
		   It will be used to find out the location/Store from which file will be
		   picked up and synchronized to the destination location.
		   1. Uncomment the following sample entry (by removing "//" characters at the 
			  beginning)
		   2. For each location/store, atleast one entry is required.
		   3. In the sample entry below, "loc1" is the name of the destination for which 
			  preferred locations are to be entered
		   4. the String curly braces (inside {}) has the list of preferred locations
		   5. The sample entry below indicates that the first preferred location is loc2.
		   6. You can add as many entries for preferred locations, in the order of preference.
			  The first one is taken as the most preferred location
		   7. Note that the format of the entries should be exactly as indicated below.
		 */
		//_proximityMap.put("loc1", new String[]{"loc2", "loc3"});
	}

	public IEFSmartSync_mxJPO ()
	{
	}

	public IEFSmartSync_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	public ArrayList check(Context context, String mcsURL, ArrayList sourceList, ArrayList destinationList) throws Exception
	{
		buildProximityMap();

		ArrayList returnList = new ArrayList();

		for (int t = 0; t < sourceList.size(); t++) 
		{
			BusinessObjectProxy sourceBOP = (BusinessObjectProxy) sourceList.get(t);

			BusinessObjectProxy correspondingBOP = (BusinessObjectProxy) destinationList.get(t);

			String destinationLocation = correspondingBOP.getLocationOverride();

			this.updateNearestLocation(context, sourceBOP, destinationLocation);

			returnList.add(sourceBOP);
		}

		return returnList;
	}

	/** 
	 *
	 */
	private void updateNearestLocation(Context context, BusinessObjectProxy sourceBOP, String destinationLocation) throws Exception
	{
		if(DEBUG)
			System.out.println("**IEFSmartSync : updateNearestLocation..");

		String busID             = sourceBOP.getOID();
		String format            = sourceBOP.getFormat();
		String fileName          = sourceBOP.getFileName();
		String defaultSourceName = sourceBOP.getLocationOverride();

		if(DEBUG)
		{
			System.out.println("**IEFSmartSync : File Name 			   : " + fileName);
			System.out.println("**IEFSmartSync : Input Source location : " + defaultSourceName);
		}

		// Initialize the user settings for proximity map
		String[] closestLocations = null;
		if (_proximityMap != null && _proximityMap.size() > 0 && ((closestLocations = (String[])_proximityMap.get(destinationLocation)) != null ))
		{
			boolean isCurrentSourceNearest = isCurrentSourceNearest(defaultSourceName, closestLocations);

			if(isCurrentSourceNearest)
			{
				if(DEBUG)
					System.out.println("**IEFSmartSync : Input Source Location is mapped as the most preferred location..");

				return;
			}

			Set latestFileLocations = getSynchronizedLocations(context, busID, fileName, format);
			if (latestFileLocations != null && !latestFileLocations.isEmpty() && latestFileLocations.size() > 1)
			{
				for (int i = 0; i < closestLocations.length; i++)
				{
					String curClosestLocation = closestLocations[i].trim();

					if(DEBUG)
						System.out.println("**IEFSmartSync : Current Closest Location " + curClosestLocation);


					if(curClosestLocation.equals(defaultSourceName))
					{
						if(DEBUG)
							System.out.println("**IEFSmartSync : Input Source Location is mapped as a preferred location, hence not overridding...");

						return;
					}

					if(latestFileLocations.contains(curClosestLocation))
					{
						if(DEBUG)
							System.out.println("**IEFSmartSync : The Latest File locations list contains mapped curClosestLocation " + curClosestLocation);

						String locationHashedFile = getLocationFileName(context, busID, fileName, format, curClosestLocation);

						if(locationHashedFile != null && !locationHashedFile.equals(""))
						{
							if(DEBUG)
								System.out.println("**IEFSmartSync : Overriding the input source location..");

							sourceBOP.setLocationOverride(curClosestLocation);

							sourceBOP.setHashName(locationHashedFile);

							break;
						}
					}
				}
			}
			else if(DEBUG)
				System.out.println("**IEFSmartSync : File is present in only one location (hence no override required) or the file location could not be retrieved");

		}
		else
		{
			if(DEBUG)
				System.out.println("**IEFSmartSync : Proximity Map is NOT defined for location:" + destinationLocation);
		}
	}

	private boolean isCurrentSourceNearest(String defaultSourceName, String[] closestLocations)
	{
		boolean isCurrentSourceNearest = false;

		if (closestLocations.length >= 1)
		{
			String locationName = closestLocations[0];

			if(locationName.equals(defaultSourceName))
				isCurrentSourceNearest = true;
		}

		return isCurrentSourceNearest;
	}

	/**
	 * get the location where the most updated file is located by using MQL command.
	 * @param context  - Matrix Context object
	 * @param oid 	   - business object OID
	 * @param fileName - file name
	 * @param format   - format name
	 * @return		   - a Set of locations where the most updated file is located
	 */
	private Set getSynchronizedLocations(Context context, String oid, String fileName, String format)
	{
		Set locations = new HashSet();

		try
		{
			String separator = "|";
			if(DEBUG)
				System.out.println("**IEFSmartSync : Command for getting updated locations: ");

			MQLCommand mql = new MQLCommand();
			mql.executeCommand(context, "print bus $1 select $2 dump $3",oid,"format[" + format + "].file[" + fileName + "].synchronized",separator);
			String result = mql.getResult(); 
			result = result.trim();

			if (result != null && result.length() > 0)
			{
				if(DEBUG)
					System.out.println("**IEFSmartSync : Cur File LocationsForFile:" + result);

				StringTokenizer tokenizer = new StringTokenizer(result,separator);
				while(tokenizer.hasMoreTokens())
				{
					String loc = tokenizer.nextToken();
					loc = loc.trim();
					locations.add(loc);
				}
			}
			else
			{
				if(DEBUG)
					System.out.println("**IEFSmartSync : Latest file not found in any of the locations, file is :"+ fileName);
			}
		}
		catch (MatrixException e)
		{
			if(DEBUG)
				System.out.println("Exception in get current synchronized location : " + e.getMessage());
		}

		return locations;
	}

	/**
	 * get the location where the most updated file is located by using MQL command.
	 * @param context  		 - Matrix Context object
	 * @param oid 	   		 - business object OID
	 * @param fileName 		 - file name
	 * @param format   		 - format name
	 * @param locationName   - location name containg the file
	 * @return		   		 - the hashed path of the file inside the location 
	 */
	private String getLocationFileName(Context context, String busID, String fileName, String format, String locationName) throws MatrixException
	{
		String capturedFileName = null;
		String hashFileName = "format[" + format + "].file[" + fileName + "].locationfile[" + locationName + "]";

		StringList selectList = new StringList();
		selectList.add(hashFileName);

		boolean isPushed    = false;

		try
		{
			com.matrixone.apps.domain.util.ContextUtil.pushContext(context);

			isPushed    = true;

			BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(context, new String[]{busID}, selectList).getElement(0);

			capturedFileName = busWithSelect.getSelectData(hashFileName);
		}
		finally
		{
			if(isPushed)
				com.matrixone.apps.domain.util.ContextUtil.popContext(context);
		}

		return capturedFileName;
	}
}
