/*
**  IEFSearchUsers
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Searches for users in database based on given criteria
*/
/**
 * IEFSearchUsers.java
 * This JPO is used for searching users in the database based on input criteria
 * 
 * @since IEF 10.5
 *
*/


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import matrix.db.Context;
import matrix.db.Group;
import matrix.db.GroupItr;
import matrix.db.GroupList;
import matrix.db.JPO;
import matrix.db.PersonItr;
import matrix.db.PersonList;
import matrix.db.Role;
import matrix.db.RoleItr;
import matrix.db.RoleList;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.customTable.CellData;
import com.matrixone.MCADIntegration.utils.customTable.CustomMapList;

public class IEFSearchUsers_mxJPO
{    
    StringList sListPerson;
    StringList sListGroup;
    StringList sListRole;

    String sFilter;

/**
 * This is constructor which intializes variable declared
 * @since IEF 10.5
 */

public IEFSearchUsers_mxJPO(Context context, String[] args) throws Exception
{
}

/**
 * List returned by this method is used to render a table displaying users
 * corresponding to the query entered in the user-search dialog.
 * It returns a CustomMapList containing hashmap objects.
 * Each hashmap descrbes a row in the table.
 *
 *
 * @param Context  context for user logged in
 * @param String array
 *
 * This method expects following parameters to be packed in string array
 *
 *    sTopLevel=<toplevel_value>
 *    sPerson=<person_value>
 *    sGroup=<group_value>
 *    sRole=<role_value>
 *
 * @return Object as CustomMapList
 * @since IEF 10.5
 */

public Object getTableData(Context context, String[] args) throws Exception
{
    CustomMapList usersList = null;

    try 
    {
        usersList = new CustomMapList();

        //Build query using list of parameters received from the caller
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        
        int iFilterCount = getUserList( paramMap , context );
        
		//loop over each of the lists & create a row for each of them
        if( ( ( sListPerson != null ) && ( sListPerson.size() != 0 ) )
            || ( ( sListGroup != null ) && (sListGroup.size() != 0 ) )
            || ( ( sListRole != null ) && (sListRole.size() != 0 ) ) )
        {
            int count = 0;
            count = filterList( sListPerson , iFilterCount , 
            	usersList , "images/iconPerson.gif" , MCADMxUtil.getActualNameForAEFData(context, "type_Person"));
            	
            count += filterList( sListGroup , iFilterCount , 
            	usersList , "images/iconGroup.gif" , "Group");
            	
            count += filterList( sListRole , iFilterCount , 
            	usersList , "images/iconRole.gif" , "Role");

        } // End Of If any of the lists is non empty

	} catch (Exception e) {

	    usersList = new CustomMapList();
    }

	return usersList;
}

private int filterList( StringList currentList ,
	int iLimit , CustomMapList usersList , String sImageSrc , String userType)
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
        		&& ( usersList.size() < iLimit ) ); i++ )
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
				map.put("UserType" , userType);
				
  	            //Create a cell for column for user name          	            
	            cellData = new CellData();
	            cellData.setCellText( sValue );
                cellData.setIconUrl(sImageSrc);
                map.put("Name", cellData);

                // add row for this person to the list
   	            usersList.add(map);
   	            count++;
            }
		}
    }
    return count;
}

private int getUserList( HashMap paramMap , Context context)
{
    int iEntriesFound = 0;

	try
	{
        //To Store all the Params
        String sTopLevel  = (String) paramMap.get("chkbxTopLevel");
        String sPerson    = (String) paramMap.get("chkbxPerson");
        String sGroup     = (String) paramMap.get("chkbxGroup");
        String sRole      = (String) paramMap.get("chkbxRole");
        sFilter    = (String) paramMap.get("txtFilter");

        boolean bTopLevel = false;
        boolean bPerson   = false;
        boolean bGroup    = false;
        boolean bRole     = false;

        // Depending on the checkbox status, set fkag status
        if( (sTopLevel != null) && (sTopLevel.equals("checked") ) ) {
            bTopLevel = true;
        }

        if( (sPerson != null) && (sPerson.equals("checked") ) ) {
            bPerson = true;
        }

        if( (sGroup != null) && (sGroup.equals("checked") ) ) {
            bGroup = true;
        }

        if( (sRole != null) && (sRole.equals("checked") ) ){
            bRole = true;
        }

        if( (sFilter == null) || (sFilter.equals("") ) ){
            sFilter = "";
        }

		sListPerson = new StringList();
        sListGroup = new StringList();
        sListRole = new StringList();
        
        // populate the list of people
        if (bPerson) 
        {
            PersonList personListGeneric = matrix.db.Person.getPersons(context, true);
            PersonItr personItrGeneric = new PersonItr(personListGeneric);
            
            while(personItrGeneric.next()) 
            {
                sListPerson.addElement(personItrGeneric.obj().toString());
                iEntriesFound++;
            }
        }

        // populate the List of groups        
	    GroupList groupListGeneric = null;
        GroupItr groupItrGeneric = null;
        if ( (bTopLevel == false) && (bGroup == true) ) 
        {
            groupListGeneric = Group.getGroups(context, true);
            groupItrGeneric = new GroupItr(groupListGeneric);
            
            while(groupItrGeneric.next()) 
            {
                sListGroup.addElement(groupItrGeneric.obj().toString());
                iEntriesFound++;
            }
        } 
        else 
        {
            if ((bTopLevel == true) && (bGroup == true)) 
            {
                groupListGeneric = Group.getTopLevelGroups(context, true);
                groupItrGeneric = new GroupItr(groupListGeneric);
                
                while(groupItrGeneric.next()) 
                {
                    sListGroup.addElement(groupItrGeneric.obj().toString());
                    iEntriesFound++;
                }
            }
        }

        // populate the List of roles
        // criteria specified.
	    RoleList roleListGeneric = null;
        RoleItr roleItrGeneric = null;

        if ((bTopLevel == false) && (bRole == true)) 
        {
            roleListGeneric = Role.getRoles(context, true);
            roleItrGeneric = new RoleItr(roleListGeneric);
            
            while(roleItrGeneric.next()) 
            {
                String roleName		 = roleItrGeneric.obj().toString();
				String roleAssignments =  com.matrixone.apps.domain.util.PropertyUtil.getAdminProperty(context, "Role", roleName, "IEF-IntegrationAssignments");

				if(roleAssignments != null && !roleAssignments.equals(""))
				{
				    sListRole.addElement(roleName);
                iEntriesFound++;
            }
        } 
        } 
        else 
        {
          if ((bTopLevel == true) && (bRole == true)) 
          {              
              roleListGeneric = Role.getTopLevelRoles(context, true);
              roleItrGeneric = new RoleItr(roleListGeneric);
              
              while(roleItrGeneric.next()) 
              {
				  String roleName		 = roleItrGeneric.obj().toString();
				  String roleAssignments =  com.matrixone.apps.domain.util.PropertyUtil.getAdminProperty(context, "Role", roleName, "IEF-IntegrationAssignments");

				  if(roleAssignments != null && !roleAssignments.equals(""))
				  {
					  sListRole.addElement(roleName);
                  iEntriesFound++;
              }
          }
        }
        }

        sListPerson.sort();
        sListGroup.sort();
        sListRole.sort();        
	}
	catch( Exception ex )
	{
		iEntriesFound = 0;
	}
	return iEntriesFound;
}

}//End of class

