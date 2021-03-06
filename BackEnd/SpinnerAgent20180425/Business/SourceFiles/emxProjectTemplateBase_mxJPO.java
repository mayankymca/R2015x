/* emxProjectTemplateBase.java
*
* Copyright (c) 2002-2015 Dassault Systemes.
* All Rights Reserved
* This program contains proprietary and trade secret information of
* MatrixOne, Inc.  Copyright notice is precautionary only and does
* not evidence any actual or intended publication of such program.
*
* static const char RCSID[] = $Id: ${CLASSNAME}.java.rca 1.13.2.2 Thu Dec  4 07:55:07 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.13.2.1 Thu Dec  4 01:53:14 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.13 Tue Oct 28 18:55:12 2008 przemek Experimental przemek $
*/

//
//Change History:
//Date       Change By  Release   Bug/Functionality        Details
//-----------------------------------------------------------------------------------------------------------------------------
//7-Apr-09   wqy        V6R2010   371705                   Modified simpleDateFormat by adding Locale_US in perfomPostProcessActions
//
//

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.program.ProgramCentralConstants;
import com.matrixone.apps.program.ProgramCentralUtil;
import com.matrixone.apps.program.ProjectTemplate;
import com.matrixone.apps.program.ResourcePlanTemplate;
import com.matrixone.apps.program.Task;


/**
 * The <code>emxProjectTemplateBase</code> class represents the Project
 * Template JPO functionality for the AEF type.
 *
 * @version AEF 9.5.1.1 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxProjectTemplateBase_mxJPO extends com.matrixone.apps.program.ProjectTemplate
{
    /** the company id for this project template. */
    protected String _companyId = null;

    /**
     * Constructs a new emxProjectTemplate JPO object.
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - String containing the
     *            id
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.1.1
     */
    public emxProjectTemplateBase_mxJPO (Context context, String[] args) throws Exception
    {
        // Call the super constructor
        super();
        if ((args != null) && (args.length > 0))
        {
            setId(args[0]);
        }
    }

    /**
     * This function verifies user's permission for a given project template.
     * This check is made by verifying the user's company matches the template's
     * company.
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return boolean true or false
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.1.0
     */
    public boolean hasAccess(Context context, String[] args)
      throws FrameworkException
    {
        // program[emxProjectTemplate -method hasAccess
        // -construct ${OBJECTID}] == true
        String personCompanyId = MqlUtil.mqlCommand(context,
                "get env global PERSONCOMPANYID"); //PRG:RG6:R213:Mql Injection:Static Mql:19-Oct-2011
        if ("".equals(personCompanyId))
        {
            com.matrixone.apps.common.Person person = null;
            personCompanyId = person.getPerson(context).getCompanyId(context);
          //PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:start
           String sCommandStatement = "set env global $1 $2";
           MqlUtil.mqlCommand(context, sCommandStatement,"PERSONCOMPANYID",personCompanyId); 
            //PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:End
        }
        if (_companyId == null)
        {
            _companyId = getInfo(context, SELECT_COMPANY_ID);
        }
        return (personCompanyId.equals(_companyId)) ? true : false;
    }

    /***************************************************************************
     * Methods for Config Table Conversion Task
     **************************************************************************/
    /**
     * gets the list of All ProjectTemplates owned by the user
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return MapList containing the ids of ProjectTemplate objects
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllProjectTemplates(Context context, String[] args)
      throws Exception
    {
        return getProjectTemplates(context, null);
    }

    /**
     * gets the list of active ProjectTemplates owned by the user
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return MapList containing the ids of ProjectTemplate objects
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getActiveProjectTemplates(Context context, String[] args)
      throws Exception
    {
        return getProjectTemplates(context, STATE_PROJECT_TEMPLATE_ACTIVE);
    }

    /**
     * gets the list of inactive ProjectTemplates owned by the user
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return MapList containing the ids of ProjectTemplate objects
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getInActiveProjectTemplates(Context context, String[] args)
      throws Exception
    {
        return getProjectTemplates(context, STATE_PROJECT_TEMPLATE_INACTIVE);
    }

    /**
     * gets the list of project template objects owned by the user
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return MapList containing the ids of ProjectTemplate objects
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
    public MapList getProjectTemplates(Context context, String selectState)
      throws Exception
    {
        // Check license while listing Project Template, if license check fails here
        // the templates will not be listed.
        //
        //ComponentsUtil.checkLicenseReserved(context, DomainConstants.TRIGRAM_ENOVIA_PROGRAM_CENTRAL);
    	ComponentsUtil.checkLicenseReserved(context,ProgramCentralConstants.PRG_LICENSE_ARRAY);
                
        
        MapList templateList = null;
        try
        {
            String busWhere = null;
            com.matrixone.apps.program.ProjectTemplate projectTemplate = (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                    DomainConstants.TYPE_PROJECT_TEMPLATE,
                    DomainConstants.PROGRAM);

            // Build the where clause based on the filter request
            // For active task display everything except tasks that are on the
            // complete or archive states
            if (selectState == null)
            {
                // do nothing here
            }
            else if ((STATE_PROJECT_TEMPLATE_INACTIVE).equals(
                      selectState))
            {
                busWhere = "current=='" + STATE_PROJECT_TEMPLATE_INACTIVE +"'";
            }
            else
            {
                // default, sets to Active
                busWhere = "current=='" + STATE_PROJECT_TEMPLATE_ACTIVE + "'";
            }

            String vaultPattern = "";
            String vaultOption = PersonUtil.getSearchDefaultSelection(context);
            vaultPattern = PersonUtil.getSearchVaults(context, false ,vaultOption);

            // use the matchlist keyword to filter by vaults, need this if
            // option is not "All Vaults"
            if (!vaultOption.equals(PersonUtil.SEARCH_ALL_VAULTS) && vaultPattern.length() > 0)
            {
                if ((busWhere == null) || "".equals(busWhere))
                {
                    busWhere = "vault matchlist '" + vaultPattern + "' ','";
                }
                else
                {
                    busWhere += "&& vault matchlist '" + vaultPattern + "' ','";
                }
            }

            // Retrieve the project template list.
            StringList busSelects = new StringList(2);
            busSelects.add(projectTemplate.SELECT_ID);
            busSelects.add(projectTemplate.SELECT_VAULT);

            templateList = projectTemplate.getProjectTemplates(context,
                    busSelects, busWhere);

            // Use query to search objects only on selected vaults
            templateList = projectTemplate.findObjects(context,
                    DomainConstants.TYPE_PROJECT_TEMPLATE, null, null, null,
                    null,// vaultPattern
                    busWhere, true, busSelects);
        }
        catch (Exception ex)
        {
            throw ex;
        }
        finally
        {
            return templateList;
        }
    }

    /**
     * gets the estimated duration of the project templates
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: objectList - Contains a
     *            MapList of Maps which contains object names paramList - Map
     *            containing parameters for cloning the object
     * @return Vector containing the estimated duration value as String
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
    public Vector getEstimateDuration(Context context, String[] args)
      throws Exception
    {
        Vector estimatedDuration = new Vector();
        try
        {
            com.matrixone.apps.program.ProjectTemplate projectTemplate = (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                    DomainConstants.TYPE_PROJECT_TEMPLATE,
                    DomainConstants.PROGRAM);
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Map paramList = (Map) programMap.get("paramList");
            Map objectMap = null;

            Iterator objectListIterator = objectList.iterator();
            String[] objIdArr = new String[objectList.size()];
            int arrayCount = 0;
            while (objectListIterator.hasNext())
            {
                objectMap = (Map) objectListIterator.next();
                objIdArr[arrayCount] = (String) objectMap.get(projectTemplate.SELECT_ID);
                arrayCount++;
            }

            MapList actionList = ProjectTemplate.getInfo(context, objIdArr,
                    new StringList(projectTemplate.SELECT_TASK_ESTIMATED_DURATION));

            Iterator actionsListIterator = actionList.iterator();
            while (actionsListIterator.hasNext())
            {
                objectMap = (Map) actionsListIterator.next();
                String duration = (String) objectMap.get(projectTemplate.SELECT_TASK_ESTIMATED_DURATION);
                estimatedDuration.add(duration);
            }
            // ends while
        }
        catch (Exception ex)
        {
            throw ex;
        }
        finally
        {
            return estimatedDuration;
        }
    }

    /**
     * gets the owner with lastname,firstname format also has a link to open a
     * pop up with the owner
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: objectList - Contains a
     *            MapList of Maps which contains object names paramList - Map
     *            containing parameters for cloning the object
     * @return Vector containing the owner name as String
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
    public Vector getOwner(Context context, String[] args)
      throws Exception
    {
        Vector owner = new Vector();
        try
        {
            com.matrixone.apps.program.ProjectTemplate projectTemplate = (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                    DomainConstants.TYPE_PROJECT_TEMPLATE,
                    DomainConstants.PROGRAM);
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Map paramList = (Map) programMap.get("paramList");
            Map objectMap = null;

            Iterator objectListIterator = objectList.iterator();
            String[] objIdArr = new String[objectList.size()];
            int arrayCount = 0;
            while (objectListIterator.hasNext())
            {
                objectMap = (Map) objectListIterator.next();
                objIdArr[arrayCount] = (String) objectMap.get(projectTemplate.SELECT_ID);
                arrayCount++;
            }

            MapList actionList = projectTemplate.getInfo(context, objIdArr,
                    new StringList(projectTemplate.SELECT_OWNER));

            Iterator actionsListIterator = actionList.iterator();
    		String sPersonFullName = "";
            while (actionsListIterator.hasNext())
            {
                objectMap = (Map) actionsListIterator.next();
                String owners = (String) objectMap.get(projectTemplate.SELECT_OWNER);
    			//Added:PRG:RG6:R212:10-Jun-2011
    			if(ProgramCentralUtil.isNotNullString(owners))
    			{
    				sPersonFullName = PersonUtil.getFullName(context,owners);
    			}
    			else
    			{
    				sPersonFullName = "";
    			}
    			owner.add(XSSUtil.encodeForHTML(context,sPersonFullName));
    			//Added:PRG:RG6:R212:10-Jun-2011
            }
            // ends while
        }
        catch (Exception ex)
        {
            throw ex;
        }
        finally
        {
            return owner;
        }
    }
  /**
     * This method determines if the checkbox needs to be enabled if the logged
     * in person is the owner of the ProjectTemplate in
     * PMCProjectTemplateSummary table.
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: objectList - objectList
     *            Contains a MapList of Maps which contains objects.
     * @return Object of type Vector
     * @throws Exception
     *             if the operation fails
     * @since PMC 10-6
     */
  public Vector showTemplateCheckbox(Context context, String[] args)
    throws Exception
  {
      HashMap programMap = (HashMap) JPO.unpackArgs(args);
      MapList objectList = (MapList) programMap.get("objectList");

      Vector enableCheckbox = new Vector();

      //modified for the bug 328024
      matrix.db.Person loginPerson=new matrix.db.Person(context.getUser());
      loginPerson.open(context);
      //Till here
      try
      {
          com.matrixone.apps.program.ProjectTemplate projectTemplate = (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,DomainConstants.TYPE_PROJECT_TEMPLATE, DomainConstants.PROGRAM);

          Iterator objectListItr = objectList.iterator();
          while (objectListItr.hasNext())
          {
              Map objectMap = (Map) objectListItr.next();
              String projectTemplateId = (String) objectMap.get(projectTemplate.SELECT_ID);

              if (projectTemplateId !=null && !projectTemplateId.equals("null") && !projectTemplateId.equals(""))
              {
                  projectTemplate.setId(projectTemplateId);
                  String projectTemplateOwner = projectTemplate.getInfo(context, projectTemplate.SELECT_OWNER).toString();
                  //modified for the bug 328024
                  if (loginPerson.isAssigned(context,"Project Administrator") || ( projectTemplateOwner !=null && !projectTemplateOwner.equals("null") && !projectTemplateOwner.equals("") && projectTemplateOwner.equals(context.getUser())))
                  {
                      enableCheckbox.add("true");
                  }
                  else
                  {
                      enableCheckbox.add("false");
                  }
              }
          }
      }
      catch (Exception ex)
      {
        throw ex;
      }
      finally
      {
        //added for the bug 328024
        loginPerson.close(context);
        //till here
        return enableCheckbox;
      }
  }
        /**
         * This method gets the Value range values depending upon the property
         * emxFramework.DefaultSearchVaults PMCProjectTemplateCreateForm.
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @param args
         *            holds the following input arguments: objectList -
         *            objectList Contains a MapList of Maps which contains
         *            objects.
         * @return HashMap
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */
        public HashMap getProjTempVaultRangeValues(Context context, String[] args)
        throws Exception
        {

            com.matrixone.apps.common.Person person =
                (com.matrixone.apps.common.Person) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PERSON,DomainConstants.PROGRAM);


            String sLanguage=context.getSession().getLanguage();

            String ctxPersonId = person.getPerson(context).getId();
            person.setId(ctxPersonId);

            String userVault = person.getVault();

            userVault = i18nNow.getMXI18NString(userVault,"",sLanguage,"Vault");

            StringList vaultsList = GetVaults(context, ctxPersonId);

            if(vaultsList.isEmpty()) {
                vaultsList = GetAllVaults(context);
            }
            vaultsList.sort();

            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            // Get internationalized vault names
            StringList i18NVaults = new StringList();
            String i18nVault = "";
            String vaultName = "";
            Iterator vaultItr = vaultsList.iterator();
            while(vaultItr.hasNext()) {
                vaultName = (String)vaultItr.next();
                vaultName = vaultName.trim();
                i18nVault  = i18nNow.getMXI18NString(vaultName,"",sLanguage,"Vault");

                fieldRangeValues.addElement(vaultName);
                fieldDisplayRangeValues.addElement(i18nVault);
            }

            HashMap tempMap = new HashMap();
            tempMap.put("field_choices", fieldRangeValues);
            tempMap.put("field_display_choices", fieldDisplayRangeValues);


            return tempMap;
        }


        /**
         * This method gets all the vaults PMCProjectTemplateCreateForm.
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @return StringList
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */
        public StringList GetAllVaults(Context context)
        throws MatrixException
        {

            // Get all vaults so that user can choose
            // this is all company's vaults not all vaults from all servers
            StringList vaultList = new StringList();

            com.matrixone.apps.common.Person person =
                com.matrixone.apps.common.Person.getPerson(context);
            com.matrixone.apps.common.Company company = person.getCompany(context);

            StringList selectList = new StringList(2);
            selectList.add(DomainConstants.SELECT_VAULT);
            selectList.add(DomainConstants.SELECT_SECONDARY_VAULTS);
            Map companyMap = company.getInfo(context,selectList);
            StringList secVaultList = FrameworkUtil.split((String)companyMap.get(DomainConstants.SELECT_SECONDARY_VAULTS),null);
            Iterator itr = secVaultList.iterator();

            String vaults = (String)companyMap.get(DomainConstants.SELECT_VAULT);
            vaultList.add(vaults);
            while (itr.hasNext() )
            {
                vaultList.add(PropertyUtil.getSchemaProperty(context, (String)itr.next()));
            }

            return vaultList;
        }

        /**
         * This method gets the vaults PMCProjectTemplateCreateForm.
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @return StringList
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */
        public StringList GetVaults(Context context, String theId)
        throws MatrixException
        {
            com.matrixone.apps.common.Person person =
                (com.matrixone.apps.common.Person) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PERSON);

            StringList vaultSL = new StringList();
            if(theId != null && !"".equals(theId)) {
                person.setId(theId);
                vaultSL = convertToStringList(person.getSearchDefaultVaults(context), ",");
            }

            return vaultSL;

        }


        /**
         * This method Splits the string to StringList
         * PMCProjectTemplateCreateForm.
         *
         * @param String
         *            the vault String
         * @param String
         *            the split character
         * @return StringList
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */
        public StringList convertToStringList(String vaultStr, String splitAt)
        throws MatrixException
        {
            StringList vaultSL = new StringList();
            StringList vaultSplit = FrameworkUtil.split(vaultStr, splitAt);
            Iterator vaultItr = vaultSplit.iterator();
            while (vaultItr.hasNext()){
                vaultSL.add(((String) vaultItr.next()).trim());
            }
            return vaultSL;
        }
       /**
         * This method performs the Post Process Actions of Project Template
         * Creation PMCProjectTemplateCreateForm.
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @param String
         *            the split character
         * @return StringList
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */

        @com.matrixone.apps.framework.ui.PostProcessCallable
        public void performPostProcessActions (Context context,String[] args)throws Exception
        {
            // Check license while creating Project Template, if license check fails here
            // the template will not be created by emxCreate.jsp component.
            //
            //ComponentsUtil.checkLicenseReserved(context, DomainConstants.TRIGRAM_ENOVIA_PROGRAM_CENTRAL);
        	ComponentsUtil.checkLicenseReserved(context,ProgramCentralConstants.PRG_LICENSE_ARRAY);
            
            
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String businessUnitId= (String)requestMap.get("BusinessUnitId");
            String defaultVault = (String)requestMap.get("defaultVault");
            String strProjName = (String)requestMap.get("Name");
            String currency = (String)requestMap.get("Currency");
            String objectId = (String)paramMap.get("objectId");
            String strDescription = (String)requestMap.get("ProjectTemplateDescription");

            com.matrixone.apps.program.Program program =
                (com.matrixone.apps.program.Program) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PROGRAM,DomainConstants.PROGRAM);
            com.matrixone.apps.program.ProjectTemplate projectTemplate =
                (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PROJECT_TEMPLATE,DomainConstants.PROGRAM);
            com.matrixone.apps.program.ProjectSpace project =
                (com.matrixone.apps.program.ProjectSpace) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PROJECT_SPACE,DomainConstants.PROGRAM);


            try{
                ContextUtil.startTransaction(context, true);
                projectTemplate.setId(objectId);
                String revision=projectTemplate.getUniqueName(DomainConstants.EMPTY_STRING);
                String type = projectTemplate.getInfo(context, DomainConstants.SELECT_TYPE);
            	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:start
            	String sCommandStatement = "modify bus $1 name $2 revision $3";
            	MqlUtil.mqlCommand(context, sCommandStatement,objectId, strProjName,revision); 
            	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:End

                if(businessUnitId!= null && !businessUnitId.equals("") && !businessUnitId.equals("null")) {
                    projectTemplate.setOrganization(context, businessUnitId);
                }

                DomainObject _accessListObject=DomainObject.newInstance(context);
                // create project access list object and connect to project
                _accessListObject.createAndConnect(context,
                        DomainConstants.TYPE_PROJECT_ACCESS_LIST,
                        projectTemplate.getUniqueName("PAL-"),
                        DomainConstants.EMPTY_STRING,
                        DomainConstants.POLICY_PROJECT_ACCESS_LIST,
                        defaultVault,
                        DomainConstants.RELATIONSHIP_PROJECT_ACCESS_LIST,
                        projectTemplate,
                        false);
                // get person id
                com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);

                // get the company id for this context
                com.matrixone.apps.common.Company company = person.getCompany(context);

                if (projectTemplate.isKindOf(context, DomainConstants.TYPE_PROJECT_TEMPLATE))
                {
            		//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:start
                	sCommandStatement = "connect bus $1 preserve relationship $2 to $3";
                	MqlUtil.mqlCommand(context, sCommandStatement,company.getObjectId() , DomainConstants.RELATIONSHIP_COMPANY_PROJECT_TEMPLATES,objectId); 
                	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:End
                }
                else
                {
            		//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:start
                	sCommandStatement = "connect bus $1 preserve relationship $2 to $3";
                	MqlUtil.mqlCommand(context, sCommandStatement,company.getObjectId() , DomainConstants.RELATIONSHIP_COMPANY_PROJECT,objectId); 
                	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:End

                    String personId = person.getObjectId();

                    // add the originator as a default "Project Owner" member
                    // of this project
                    projectTemplate.addMember(context, personId);
                }

                String originated = projectTemplate.getInfo(context, SELECT_ORIGINATED);

                //
                // In date roll up bean the default start of the business hour is considered as 8 AM.
                // Thus set the time part of the originated date as 8 AM. Due to this the date roll up operation
                // will be performed correctly.
                //
                Date dtOriginated = eMatrixDateFormat.getJavaDate(originated);
                Calendar calOriginated = new GregorianCalendar();
                calOriginated.setTime(dtOriginated);
                calOriginated.set(Calendar.HOUR_OF_DAY, 8);
                calOriginated.set(Calendar.MINUTE, 0);
                calOriginated.set(Calendar.SECOND, 0);
                calOriginated.set(Calendar.MILLISECOND, 0);

                //Modified:7-Apr-09:wqy:R207:PRG 371705
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), Locale.US);
                //End:R207:PRG 371705

                originated = simpleDateFormat.format(calOriginated.getTime());

                HashMap attributes = new HashMap(5);
                attributes.put(DomainConstants.ATTRIBUTE_TASK_ESTIMATED_START_DATE, originated);
                attributes.put(DomainConstants.ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, originated);
                attributes.put(DomainConstants.ATTRIBUTE_TASK_ESTIMATED_DURATION, "0");
                attributes.put(DomainConstants.ATTRIBUTE_ORIGINATOR, context.getUser());
                if(ProgramCentralUtil.isNotNullString(currency))
                	attributes.put(ProgramCentralConstants.ATTRIBUTE_CURRENCY, currency);
                projectTemplate.setAttributeValues(context, attributes);

                if (strDescription != null)
                {
                    projectTemplate.setDescription(context, strDescription);
                }

                String defaultOrg = PersonUtil.getDefaultOrganization(context, context.getUser());
                String defaultProj = PersonUtil.getDefaultProject(context, context.getUser());
                //If collab space is GLOBAL, remove it.
                if ("GLOBAL".equalsIgnoreCase(defaultProj)){
                	projectTemplate.removePrimaryOwnership(context);
                }
                //Template should be available to the whole company.
                DomainAccess.createObjectOwnership(context, projectTemplate.getId(context), defaultOrg, null, "Project Member", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP, true);
    			
                ContextUtil.commitTransaction(context);
            }
            catch(Exception ee)
            {
                ContextUtil.abortTransaction(context);
            }
        }
        /**
         * This method determines if the Vault field needs to be displayed or
         * not value returned should be: true/false
         *
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @return the user access permissions for this project.
         * @throws FrameworkException
         *             if operation fails.
         * @since AEF 9.5.1.0
         */
        public boolean showVaultField(Context context, String[] args)
        throws Exception
        {
             com.matrixone.apps.common.Person person =
                (com.matrixone.apps.common.Person) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PERSON,DomainConstants.PROGRAM);
            String sLanguage=context.getSession().getLanguage();
            String ctxPersonId = person.getPerson(context).getId();
            person.setId(ctxPersonId);
            String userVault = person.getVault();
            userVault = i18nNow.getMXI18NString(userVault,"",sLanguage,"Vault");
            StringList vaultsList = GetVaults(context, ctxPersonId);
            if(vaultsList.size()>1)
            {
                return true;
            }
            else return false;
        }
     /**
         * gets the Originator -Context User Name PMCProjectTemplateCreateForm
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @param args
         *            holds the following input arguments: objectList - Contains
         *            a MapList of Maps which contains object names paramList -
         *            Map containing parameters for cloning the object
         * @return String containing the Originator name as String
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */
    public String getOriginator(Context context, String[] args)
    throws Exception
    {
        String personName = "";
        try{
            com.matrixone.apps.common.Person person =
                (com.matrixone.apps.common.Person) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PERSON,DomainConstants.PROGRAM);
            String ctxPersonId = person.getPerson(context).getId();
            person.setId(ctxPersonId);
            personName = PersonUtil.getFullName(context,person.getName(context));

           }catch(Exception ex){
                throw ex;
            }
            finally
            {
                return personName;
            }
        }
        /**
         * This method gets the Name of the Project Template not value returned
         * should be: true/false
         *
         * PMCProjectTemplateCreateForm
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @return the user access permissions for this project.
         * @throws FrameworkException
         *             if operation fails.
         * @since PMC V6R2008-1
         */

        public String getName(Context context, String[] args)
        throws Exception
        {
            String strOuput ="";
            try
            {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectId = (String)requestMap.get("objectId");
            StringBuffer output = new StringBuffer();
            String sLanguage=context.getSession().getLanguage();
            com.matrixone.apps.program.ProjectTemplate projectTemplate =
                (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PROJECT_TEMPLATE,DomainConstants.PROGRAM);
            String cloneName="";
            if (objectId != null && ! "".equals(objectId) && ! "false".equals(objectId))
            {
                // Clone Name:
                projectTemplate.setId(objectId);
                cloneName = projectTemplate.getName(context);
                cloneName = " " + cloneName;
                // Added:26-march-10:vm3:R209:PRG:Bug 015201
                cloneName = cloneName.replaceAll("&", "&amp;");
                //End-Added:26-march-10:vm3:R209:PRG:Bug 015201
                i18nNow i18nnow = new i18nNow();
                String strCloneMsg = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
        				"emxProgramCentral.Common.CloningOf", sLanguage);
                strCloneMsg=strCloneMsg+XSSUtil.encodeForHTML(context,cloneName);
                output.append("<input type=\"text\" name=\"Name\" value=\""+strCloneMsg+"\"/><script language=\"JavaScript\">assignValidateMethod(\"Name\", \"isBadNameChars\");</script>");
            }

            else
            {
                //ADDED for 357476
                output.append("<input type=\"text\" name=\"Name\" value=\"\"/><script language=\"JavaScript\">assignValidateMethod(\"Name\", \"isBadNameChars\");</script>");
                //ADDED for 357476 ends.
            }
            strOuput =output.toString();
           }catch(Exception ex)
               {
                    throw ex;
               }
           finally
               {
                return strOuput ;
               }
        }
     /**
         * This method gets the Description of the Project Template not
         * value returned should be: true/false
         *
         * PMCProjectTemplateCreateForm
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @return the user access permissions for this project.
         * @throws FrameworkException
         *             if operation fails.
         * @since PMC V6R2008-1
         */

        public String getDescription(Context context, String[] args)
        throws Exception
        {
            String strOuput="";
            try{
            StringBuffer output = new StringBuffer();
            com.matrixone.apps.program.ProjectTemplate projectTemplate =
                (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                        DomainConstants.TYPE_PROJECT_TEMPLATE,DomainConstants.PROGRAM);

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectId = (String)requestMap.get("objectId");

            String cloneDescription="";
            //ADDED for 357476
            if (objectId != null && ! "".equals(objectId) && ! "false".equals(objectId))
            {
                projectTemplate.setId(objectId);
                cloneDescription = projectTemplate.getDescription(context);
                // [MODIFIED::Feb 26, 2011:S4E:R211:IR-051555V6R2012::Start] 
                cloneDescription = ProgramCentralUtil.getHTMLCompatibleString(context, cloneDescription);
                // [MODIFIED::Feb 26, 2011:S4E:R211:IR-051555V6R2012::End] 
                output.append("<textarea name=\"ProjectTemplateDescription\" >"+XSSUtil.encodeForHTML(context,cloneDescription.trim())+"</textarea>");
            }

            else
            {
                output.append("<textarea name=\"ProjectTemplateDescription\" ></textarea>");
            }
            //ADDED for 357476 ends

            strOuput =output.toString();

               }catch(Exception ex)
               {
                    throw ex;
               }
               finally{
                return strOuput ;
               }
        }
     /**
         * This method performs the Post Process Actions for Project
         * Template Clone Creation PMCProjectTemplateCreateForm for Clone
         *
         * @param context
         *            the eMatrix <code>Context</code> object
         * @param String
         *            the split character
         * @return StringList
         * @throws Exception
         *             if the operation fails
         * @since PMC V6R2008-1
         */

        @com.matrixone.apps.framework.ui.PostProcessCallable
        public void performClonePostProcessActions (Context context,String[] args)throws Exception
        {
            try{

                com.matrixone.apps.program.ProjectSpace newProjectTemplate =
                    (com.matrixone.apps.program.ProjectSpace) DomainObject.newInstance(context,
                            DomainConstants.TYPE_PROJECT_SPACE,DomainConstants.PROGRAM);
                com.matrixone.apps.program.ProjectTemplate projectTemplate =
                    (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                            DomainConstants.TYPE_PROJECT_TEMPLATE,DomainConstants.PROGRAM);

                HashMap programMap = (HashMap) JPO.unpackArgs(args);
                HashMap requestMap = (HashMap) programMap.get("requestMap");
                HashMap paramMap = (HashMap) programMap.get("paramMap");
                String defaultVault = (String)requestMap.get("defaultVault");
                    // Clone-- parentObjId is Source, objectId is Target
                String objectId = (String)paramMap.get("objectId");
                String parentObjId = (String)requestMap.get("objectId");
                String strDescription = (String)requestMap.get("ProjectTemplateDescription");
                String strCurrency = (String)requestMap.get("Currency");
                    projectTemplate.setId(parentObjId);
                newProjectTemplate.setId(objectId);

                String templateType = projectTemplate.getInfo(context,DomainConstants.SELECT_TYPE);
                String newTemplateType = newProjectTemplate.getInfo(context, DomainConstants.SELECT_TYPE);
                //newTemplateType is created as Project Template so,
                //if templateType is a sub-type, change newTemplateType to sub-type instead of Project Template

                if ( !newTemplateType.equalsIgnoreCase(templateType)
                     && newProjectTemplate.isKindOf(context, DomainConstants.TYPE_PROJECT_TEMPLATE))
                {
                    newProjectTemplate.change(context, templateType,
                    newProjectTemplate.getName(), newProjectTemplate.getRevision(),
                    newProjectTemplate.getVault().toString(),
                    newProjectTemplate.getPolicy(context).getName());
                   }

                StringList attributeList = DomainObject.getTypeAttributeNames(context, projectTemplate.TYPE_PROJECT_TEMPLATE);

                // let actuals default to empty.
                attributeList.remove(ATTRIBUTE_TASK_ACTUAL_DURATION);
                attributeList.remove(ATTRIBUTE_TASK_ACTUAL_START_DATE);
                attributeList.remove(ATTRIBUTE_TASK_ACTUAL_FINISH_DATE);
                attributeList.remove(ATTRIBUTE_PERCENT_COMPLETE);
                attributeList.remove(ATTRIBUTE_ORIGINATOR);

                Map sourceAttributes = projectTemplate.getAttributeMap(context);

                Map attributes = new HashMap();

                Iterator itr = attributeList.iterator();
                while (itr.hasNext())
                {
                    String attribute = (String) itr.next();
                    String value = (String) sourceAttributes.get(attribute);
                    if (value != null)
                    {
                        attributes.put(attribute, value);
                    }
                }

                DomainObject _accessListObject=DomainObject.newInstance(context);
                // create project access list object and connect to project
                _accessListObject.createAndConnect(context,
                        DomainConstants.TYPE_PROJECT_ACCESS_LIST,
                        newProjectTemplate.getUniqueName("PAL-"),
                        DomainConstants.EMPTY_STRING,
                        DomainConstants.POLICY_PROJECT_ACCESS_LIST,
                        defaultVault,
                        DomainConstants.RELATIONSHIP_PROJECT_ACCESS_LIST,
                        newProjectTemplate,
                        false);
                // get person id
                com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);

                // get the company id for this context
                com.matrixone.apps.common.Company company = person.getCompany(context);

                if (newProjectTemplate.isKindOf(context,DomainConstants.TYPE_PROJECT_TEMPLATE))
                {
                	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:start
                	String sCommandStatement = "connect bus $1 preserve relationship $2 to $3";
                	MqlUtil.mqlCommand(context, sCommandStatement,company.getObjectId() , DomainConstants.RELATIONSHIP_COMPANY_PROJECT_TEMPLATES,objectId); 
                	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:End
                }
                else
                {
                	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:start
                	String sCommandStatement = "connect bus $1 preserve relationship $2 to $3";
                	MqlUtil.mqlCommand(context, sCommandStatement,company.getObjectId() , DomainConstants.RELATIONSHIP_COMPANY_PROJECT,objectId); 
                	//PRG:RG6:R213:Mql Injection:parameterized Mql:19-Oct-2011:End

                    String personId = person.getObjectId();

                    // add the originator as a default "Project Owner" member
                    // of this project
                    newProjectTemplate.addMember(context, personId);
                }

                String originated = newProjectTemplate.getInfo(context, SELECT_ORIGINATED);

                //
                // In date roll up bean the default start of the business hour is considered as 8 AM.
                // Thus set the time part of the originated date as 8 AM. Due to this the date roll up operation
                // will be performed correctly.
                //
                Date dtOriginated = eMatrixDateFormat.getJavaDate(originated);
                Calendar calOriginated = new GregorianCalendar();
                calOriginated.setTime(dtOriginated);
                calOriginated.set(Calendar.HOUR_OF_DAY, 8);
                calOriginated.set(Calendar.MINUTE, 0);
                calOriginated.set(Calendar.SECOND, 0);
                calOriginated.set(Calendar.MILLISECOND, 0);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(),Locale.US);
                originated = simpleDateFormat.format(calOriginated.getTime());

                attributes = new HashMap(4);

                    if (! attributes.containsKey(ATTRIBUTE_TASK_ESTIMATED_DURATION))
                    {
                        attributes.put(DomainConstants.ATTRIBUTE_TASK_ESTIMATED_START_DATE, originated);
                        attributes.put(DomainConstants.ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, originated);
                        attributes.put(DomainConstants.ATTRIBUTE_TASK_ESTIMATED_DURATION, "0");
                    }

                attributes.put(DomainConstants.ATTRIBUTE_ORIGINATOR, context.getUser());
                attributes.put(ATTRIBUTE_CURRENCY, strCurrency);
                newProjectTemplate.setAttributeValues(context, attributes);

                if (strDescription != null)
                {
                    newProjectTemplate.setDescription(context, strDescription);
                }

                ProjectTemplate.cloneStructure(context,projectTemplate,newProjectTemplate,null,true);
        		//Added:29-June-2010:s4e:R210 PRG:ARP
        		//Added to connect clone of All ResourcePlanTemplate to new cloned Projecttemplate 
        		StringList slResourcePlanTemplateList = projectTemplate.getInfoList(context, "to["+ResourcePlanTemplate.RELATIONSHIP_RESOURCE_PLAN_TEMPLATE+"].id");
        		if(null!=slResourcePlanTemplateList && !slResourcePlanTemplateList.isEmpty())
        		{                	
        			for(int nCount=0;nCount<slResourcePlanTemplateList.size();nCount++)
        			{
        				Map resourcePlanTemplateArgsMap = new HashMap();
        				String strResourcePlanTemplateID = (String)slResourcePlanTemplateList.get(nCount);
        				resourcePlanTemplateArgsMap.put("strResourcePlanTemplateId",strResourcePlanTemplateID);
        				resourcePlanTemplateArgsMap.put("strProjectTempalteId",objectId);
        				String[] arrResourcePlanTemplateArgs =JPO.packArgs(resourcePlanTemplateArgsMap);
        				JPO.invoke(context,
        						"emxResourcePlanTemplateBase", null, "cloneResourcePlanTemplate",
        						arrResourcePlanTemplateArgs, null);                		
        			}
        		} 
        		//End:29-June-2010:s4e:R210 PRG:ARP

            }catch(Exception ex)
               {
                      throw ex;
               }


        }


 /**
     * This method gets the Owner name Creation PMCProjectTemplateCreateForm for
     * Clone
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param String
     *            the split character
     * @return StringList
     * @throws Exception
     *             if the operation fails
     * @since PMC V6R2008-1
     */

        public String getOwnerName(Context context, String args[]) throws Exception
    {
        String output = "";
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strMode = (String) requestMap.get("mode");
        String jsTreeID = (String) requestMap.get("jsTreeID");
        com.matrixone.apps.common.Person person =
            (com.matrixone.apps.common.Person) DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
        com.matrixone.apps.program.ProjectTemplate projectTemplate =
                    (com.matrixone.apps.program.ProjectTemplate) DomainObject.newInstance(context,
                            DomainConstants.TYPE_PROJECT_TEMPLATE,DomainConstants.PROGRAM);
        projectTemplate.setId(objectId);
        StringList busSelects = new StringList(2);
        busSelects.add(DomainConstants.SELECT_OWNER);
        Map riskMap = projectTemplate.getInfo(context, busSelects);
        String riskOwner = (String) riskMap.get(DomainConstants.SELECT_OWNER);
        String ownerId = Person.getPerson(context, riskOwner).getId();
        StringBuffer sb = new StringBuffer();

        String userName = projectTemplate.getInfo(context, DomainConstants.SELECT_OWNER);
        person = Person.getPerson(context, userName);
        busSelects.clear();
        busSelects.add(Person.SELECT_LAST_NAME);
        busSelects.add(Person.SELECT_FIRST_NAME);
        Map personFullNameMap = person.getInfo(context, busSelects);
        String strLastName = (String) personFullNameMap.get(Person.SELECT_LAST_NAME);
        String strFirstName = (String) personFullNameMap.get(Person.SELECT_FIRST_NAME);
        String personName = strLastName + ", " + strFirstName;
        if("edit".equalsIgnoreCase(strMode))
        {
        	sb.append("<input type=\"text\" name=\"PersonName\" size=\"36\" value=\""+personName+"\" readonly=\"readonly\"/>");
        	sb.append("<input type=\"hidden\" name=\"Owner\" value=\""+userName+"\"/>");
        	//Added:09-June-2010:vm3:R210 PRG:2011x
        	sb.append("<input type=\"button\" name=\"bType\" size=\"200\" value=\"...\" alt=\"\" onClick=\"javascript:chooseProjectTemplateOwner()\"/>");
        	//sb.append("<input type=\"button\" name=\"bType\" size=\"200\" value=\"...\" alt=\"\" onClick=\"javascript:showModalDialog('../common/emxFullSearch.jsp?field=TYPES=type_Person&amp;table=PMCCommonPersonSearchTable&amp;form=PMCCommonPersonSearchForm&amp;selection=single&amp;objectId="+objectId+ "&amp;submitURL=../programcentral/emxProgramCentralCommonPersonSearchUtil.jsp&amp;fieldNameActual=Owner&amp;fieldNameDisplay=PersonName&amp;mode=addProjectTemplateOwnerAssignee')\"");
        	//sb.append("<input type=\"button\" name=\"bType\" size=\"200\" value=\"...\" alt=\"\" onClick=\"performPersonSearch();\"");
        	//End Added:09-June-2010:vm3:R210 PRG:2011x
        }
        else
        {
        	sb.append(personName);
        }
        output = sb.toString();

        return output;
    }
    /**
     * This method updates the Owner name Creation PMCProjectTemplateCreateForm
     * for Clone
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param String
     *            the split character
     * @return StringList
     * @throws Exception
     *             if the operation fails
     * @since PMC V6R2008-1
     */

    public boolean updateOwner(Context context,String args[]) throws Exception
    {
    	//Added:09-June-2010:vm3:R210 PRG:2011x
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map)programMap.get("requestMap");  
        Map paramMap = (Map) programMap.get("paramMap");  
        String strObjectId = (String)paramMap.get("objectId");
        ProjectTemplate domTemplate =new ProjectTemplate(strObjectId);
        String strPerson = (String)paramMap.get("New Value");
        String tempStrPerson = domTemplate.getInfo(context, SELECT_NAME);
        String strTemplateId = (String)programMap.get("templateId");
        domTemplate.setOwner(context, strPerson);
        //End-Added:09-June-2010:vm3:R210 PRG:2011x
        return true;
    }
    
    
    //Added:27-Apr-2010:s4e:R210 PRG:2011x
    /**
     * This method gets the Field for type of the Project Template and Project Template Subtypes.
     * 
     * PMCProjectTemplateCreateForm
     * 
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return the user access permissions for this project.
     * @throws FrameworkException
     *             if operation fails.
     */
    
    public String getType(Context context, String[] args)
    throws Exception
    {
    	Map programMap = (Map)JPO.unpackArgs(args);
        Map requestMap = (Map)programMap.get("requestMap");        
        String strOuput="";
        StringBuffer output = new StringBuffer();
        i18nNow i18nnow = new i18nNow();
        String sLanguage=context.getSession().getLanguage();
        String strSelectedType = (String)requestMap.get("type");
        if (strSelectedType != null) {
            if (strSelectedType.indexOf(":") != -1) {
                // type=_selectedType:TestChildVault,type_ProjectVault,type_TectChildVault
                StringList slSplitType = FrameworkUtil.split(strSelectedType, ":");
                if (slSplitType.size() > 1) {
                    strSelectedType = (String)slSplitType.get(1);
                    slSplitType = FrameworkUtil.split(strSelectedType, ",");
                    if (slSplitType.size() > 0) {
                        strSelectedType = (String)slSplitType.get(0);
                    }
                    else {
                        strSelectedType = null;
                    }
                }
                else {
                    strSelectedType = null;
                }
            }
            else {
                // If it is just command separated value then take the first value
                StringList slSplitType = FrameworkUtil.split(strSelectedType, ",");
                if (slSplitType.size() > 0) {
                    strSelectedType = (String)slSplitType.get(0);
                }
                else {
                    strSelectedType = null;
                }
            }
        }
        if (strSelectedType == null) {        	
            strSelectedType = DomainConstants.TYPE_WORKSPACE_VAULT; // Default!
        }
        else {
            // If it is symbolic name then convert it to real name
            if (strSelectedType.startsWith("type_")) {
                strSelectedType = PropertyUtil.getSchemaProperty(context, strSelectedType);
            }
        }
        String strType = ""; 
        strType = i18nnow.getAdminI18NString("Type",strSelectedType,sLanguage);
        try{
        	output.append("<input type='hidden' name='TypeActual' value=\""+strSelectedType+"\"/>");
            output.append("<input type='text' readonly='true' name='TypeActualDisplay' value=\""+strType+"\"/>");
            output.append("<input type='button' name='TypeActual' value='...' onclick=\"javascript:showChooser('emxTypeChooser.jsp?SelectType=single&amp;ReloadOpener=true&amp;SelectAbstractTypes=false&amp;InclusionList=type_ProjectTemplate&amp;fieldNameActual=TypeActual&amp;fieldNameDisplay=TypeActualDisplay&amp;fieldNameOID=TypeActualOID&amp;suiteKey=ProgramCentral','500','500')\"/>");
            
            
            strOuput = output.toString();
            
        }catch(Exception ex)
        {
            throw ex;
        }
        return strOuput ;
        
    }
    //End:27-Apr-2010:s4e:R210 PRG:2011x
    
    /**
	 *  This method returns vector containing name of vaults in which Project Template is present.
	 *  @param context the ENOVIA <code>Context</code> object
	 * @param returns vector containing name of the vault in which Project Template is present. 
	 * @param args The arguments, it contains programMap
	 * @throws Exception if operation fails           
	 */
    
    	public Vector getProjectTemplateVaults(Context context,String[] args) throws MatrixException {
          	Vector vProjectVaults = new Vector();
          	try {
          		HashMap programMap = (HashMap) JPO.unpackArgs(args);
          		MapList objectList = (MapList)programMap.get("objectList");
          		Map mpProjectTemplate;
          		String strVault = DomainConstants.EMPTY_STRING;
          		for(int i=0;i<objectList.size();i++)
          		{
          			mpProjectTemplate = (Map)objectList.get(i);
          			strVault = (String)mpProjectTemplate.get(DomainConstants.SELECT_VAULT);
          			vProjectVaults.add(strVault);
          		}
          		
          	}catch(Exception e)
          	{
          		throw new MatrixException(e);
          	}
          	return vProjectVaults;
          }  
	/**
	 * This method returns Project template tasks to assign to Question. it filter 
	 * out tasks which are already connected to any question.
	 * 
	 * @param 	context
	 * 			the ENOVIA <code>Context</code> object
	 * @param 	argumentArray
	 * 			Array which holds Project template id and question id.
	 * 
	 * @return	taskInfoMapList
	 * 			MapList which holds tasks information to list down for connecting 
	 * 			to the question.
	 * 
     * @throws 	FrameworkException		
	 * 			FrameworkException can be thrown in case of method fail to execute. 
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getTaskMapListToAssignQuestion(Context context, String[] argumentArray) throws MatrixException {
    	
    	StringList busSelectList = new StringList(2);
    	busSelectList.add(SELECT_ID);
    	busSelectList.add(SELECT_NAME);
    	busSelectList.add(Task.SELECT_HAS_QUESTIONS);
    	
    	StringList relSelectList = new StringList();
    	String busWhere = EMPTY_STRING;
		String relWhere = EMPTY_STRING; 
    	MapList taskInfoMapList = new MapList();
    	
    	try {
	    	Map programMap = JPO.unpackArgs(argumentArray);
	        String projectTemplateId = (String)programMap.get("projectTemplateId");
	        
	    	ProjectTemplate projectTemplate = new ProjectTemplate(projectTemplateId);
	    	MapList allTaskInfoMapList = projectTemplate.getRelatedObjects(context,RELATIONSHIP_SUBTASK,TYPE_TASK_MANAGEMENT,
	    												busSelectList,relSelectList,false,true,(short)1,busWhere,relWhere,0);
	    	
	    	for(int i=0;i<allTaskInfoMapList.size();i++) {
	    		Map<String,String> templateTaskInfoMap = (Map<String,String>)allTaskInfoMapList.get(i);
	    		String isTaskHasQuestion = templateTaskInfoMap.get(Task.SELECT_HAS_QUESTIONS);
	    		
	    		//Add tasks which are not already assigned to any question.
	    		if ("false".equalsIgnoreCase(isTaskHasQuestion)){
	    			taskInfoMapList.add(templateTaskInfoMap);
	    		}
	    	}
	    	return taskInfoMapList;
	    	
    	} catch (Exception exception) {
    		throw new MatrixException(exception);
    	}
    }
	
	/**
	 * This method returns Project template question for assigning tasks.
	 * 
	 * @param 	context
	 * 			the ENOVIA <code>Context</code> object
	 * @param 	argumentArray
	 * 			Array which holds Project template id.
	 * 
	 * @return	taskInfoMapList
	 * 			MapList which holds question information to list down.
	 * 
     * @throws 	FrameworkException		
	 * 			FrameworkException can be thrown in case of method fail to execute. 
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getProjectTemplateQuestionList(Context context, String[] argumentArray) throws MatrixException {
    	
    	StringList busSelectList = new StringList(2);
    	busSelectList.add(SELECT_ID);
    	busSelectList.add(SELECT_NAME);
    	
    	StringList relSelectList = new StringList();
    	String busWhere = EMPTY_STRING;
		String relWhere = EMPTY_STRING; 
		
    	MapList questionInfoMapList = new MapList();
    	
    	try {
	    	Map programMap = JPO.unpackArgs(argumentArray);
	        String projectTemplateId = (String)programMap.get("projectTemplateId");

	    	ProjectTemplate projectTemplate = new ProjectTemplate(projectTemplateId);
	    	questionInfoMapList = projectTemplate.getRelatedObjects(context,RELATIONSHIP_PROJECT_QUESTION,TYPE_QUESTION,
		            												busSelectList,relSelectList,false,true,(short)1,
		            												busWhere,relWhere,0);
	    	
    	} catch (Exception exception) {
    		throw new MatrixException(exception);
    	}
        return questionInfoMapList;
    }
}
