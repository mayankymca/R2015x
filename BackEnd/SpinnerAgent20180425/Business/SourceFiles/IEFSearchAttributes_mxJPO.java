/**
 * IEFSearchAttributes.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
 
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.utils.customTable.CellData;
import com.matrixone.MCADIntegration.utils.customTable.CustomMapList;

public class IEFSearchAttributes_mxJPO
{    
    StringList sListBasic		= null;
    StringList sListAttribute	= null;
    String sFilter				= null;

	/**
	 * This is constructor which intializes variable declared
	 * @since IEF 10.5
	 */

	public IEFSearchAttributes_mxJPO(Context context, String[] args) throws Exception
	{
	}

	/**
	 * List returned by this method is used to render a table displaying attributes
	 * corresponding to the query entered in the attribute-search dialog.
	 * It returns a CustomMapList containing hashmap objects.
	 * Each hashmap descrbes a row in the table.
	 *
	 *
	 * @param Context  context for user logged in
	 * @param String array
	 *
	 * This method expects following parameters to be packed in string array
	 *
	 *    sBasic=<basic_value>
	 *    sAttribute=<attribute_value>
	 *    
	 *
	 * @return Object as CustomMapList
	 * @since IEF 10.5
	 */

	public Object getTableData(Context context, String[] args) throws Exception
	{
		CustomMapList attributeList = null;

		try 
		{
			attributeList = new CustomMapList();

			//Build query using list of parameters received from the caller
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			
			int iFilterCount = getAttributeList( paramMap , context );
			//loop over each of the lists & create a row for each of them
			if( ( sListAttribute != null ) && ( sListAttribute.size() != 0 ) )            
			{
				int count = 0;
				count = filterList( sListAttribute , iFilterCount , 
				attributeList ,"images/iconSmallAttributeGroup.gif",  "Attribute"); 

			} // End Of If any of the lists is non empty

		} 
		catch (Exception e) 
		{
			attributeList = new CustomMapList();
		}

		return attributeList;
	}

	private int filterList( StringList currentList ,
		int iLimit , CustomMapList attributeList , String sImageSrc , String expressionType)
	{
		int count = 0 ;	
		Pattern patternGeneric = null;
		CellData cellData = null;
		Map  map = null;
		
		//Iterate through given list 
		if( currentList != null && currentList.size() > 0 )
		{        
			for( int i = 0 ; 
				( ( i < currentList.size() ) 
					&& ( attributeList.size() < iLimit ) ); i++ )
			{
				String sValue = "";
				String sReassign = (String) currentList.elementAt( i );
				
				if ( (sFilter == null ) && ( sFilter.equals("")) ) 
				{
					sValue = sReassign;
				}
				else 
				{
					patternGeneric = new Pattern(sFilter);
					if (patternGeneric.match(sReassign)) 
					{
						sValue = sReassign;
					}
				}

				if(!sValue.equals(""))
				{
					//Create a map representing a row of table displaying list of users
					map = new Hashtable();
					
					// put user type (whether its a person, role, or group)
					map.put("ExpressionType" , expressionType);
					
					
					
					//Create a cell for column for attributes       	            
					cellData = new CellData();
					cellData.setCellText( sValue );
					cellData.setIconUrl(sImageSrc);
					map.put("Name", cellData);

					// add row for this attribute to the list
					attributeList.add(map);
					count++;
				}
			}
		}

		return count;
	}

	private int getAttributeList( HashMap paramMap , Context context)
	{
		int iEntriesFound = 0;

		try
		{
			//To Store all the Params
			String sBasic  = (String) paramMap.get("chkbxBasic");
			String sAttribute    = (String) paramMap.get("chkbxAttribute");
			
			sFilter    = (String) paramMap.get("txtFilter");

			boolean bBasic = false;
			boolean bAttribute   = false;
			

			// Depending on the checkbox status, set fkag status
			if( (sBasic != null) && (sBasic.equals("checked") ) ) 
			{
				bBasic = true;
			}

			if( (sAttribute != null) && (sAttribute.equals("checked") ) ) 
			{
				bAttribute = true;
			}        

			if( (sFilter == null) || (sFilter.equals("") ) )
			{
				sFilter = "";
			}

			sListAttribute = new StringList();
			
			// populate the list of attributes
			if (bAttribute) 
			{
				String sCommand = "list $1";
				String Args[] = new String[1];
				Args[0] = "attribute";
				String sAttribString = executeMQL(sCommand, context, Args);
				System.out.println("In getAttributeList3");
				StringTokenizer token = new StringTokenizer(sAttribString, "\n");
				String sAttrib = "";System.out.println("In getAttributeList4");
				while(token.hasMoreTokens())
				{
					sAttrib = (String)token.nextElement();
					sListAttribute.addElement(sAttrib);
				}
				System.out.println("sListAttribute.size() = " + sListAttribute.size());
				iEntriesFound = sListAttribute.size();
			}       

			sListAttribute.sort();
				 
		}
		catch( Exception ex )
		{
			iEntriesFound = 0;
		}

		return iEntriesFound;
	}

	public String executeMQL(String mqlCmd, matrix.db.Context context , String args[])
    {
        String MQLResult = "";
        try
        {
            if(context!=  null)
            {
                MQLCommand mqlc = new MQLCommand();
                boolean bRet = mqlc.executeCommand(context, mqlCmd, args);
                if (bRet)
                {
                    // ok
                    MQLResult = mqlc.getResult();
                    if (MQLResult == null || MQLResult.length() == 0)
                    {
                        // Handle if necessary
                    }
                    MQLResult = "true|" + MQLResult;
                }
                else
                {
                    // get error msg
                    MQLResult = mqlc.getError();
                    MQLResult = "false|" + MQLResult;
                }
                // remove extra new line character at the end of result, if any
                if(MQLResult.endsWith("\n"))
                {
                    MQLResult = MQLResult.substring(0, (MQLResult.lastIndexOf("\n")));
                }
            }
            else
            {
                MQLResult = "false|" + "InvalidContext";
            }
        }
        catch(MatrixException me)
        {
            MQLResult = "false|" + me.getMessage();
        }

        return MQLResult;
    }
}
