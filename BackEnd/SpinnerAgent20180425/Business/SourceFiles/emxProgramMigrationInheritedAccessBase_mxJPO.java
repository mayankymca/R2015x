import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.program.ProgramCentralUtil;

/**
 * @author RVW
 *
 */
public class emxProgramMigrationInheritedAccessBase_mxJPO extends
    emxCommonMigration_mxJPO {

    // Class variables
    //
    private static final long serialVersionUID = -8490323668001935723L;

    private static final String SELECT_ID = "id";
    private static final String SELECT_TYPE = "type";
    private static final String SELECT_NAME = "name";
    private static final String SELECT_REVISION = "revision";
    private static final String SELECT_PROJECT = "project";
    private static final String SELECT_ORG = "organization";

    private static final String SELECT_TO_TYPE = "to.type";
    private static final String SELECT_TO_NAME = "to.name";
    private static final String SELECT_TO_ID = "to.id";
    private static final String SELECT_TO_ACCESS = "to.access";
    private static final String SELECT_TO_ACCESS_ACCESS = "to.access.access";

    private static final String SELECT_FROM_TYPE = "from.type";
    private static final String SELECT_FROM_NAME = "from.name";
    private static final String SELECT_FROM_ID = "from.id";

    private static final String TYPE_DOCUMENTS = "type_DOCUMENTS";
    private static final String ACCESS_ALL = "all";

  /**
     * @param context
     * @param args
     * @throws Exception
     */
    public emxProgramMigrationInheritedAccessBase_mxJPO(Context context,
        String[] args) throws Exception {
      super(context, args);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public void migrateObjects(Context context, StringList objectList) throws Exception
    {
        // mqlLogRequiredInformationWriter("In emxSecurityMigrationMigrateProgramObjects 'migrateObjects' method "+"\n");

        StringList mxObjectSelects = new StringList(12);
        mxObjectSelects.addElement(SELECT_ID);
        mxObjectSelects.addElement(SELECT_TYPE);
        mxObjectSelects.addElement(SELECT_NAME);
        mxObjectSelects.addElement(SELECT_TO_ID);
        mxObjectSelects.addElement(SELECT_TO_TYPE);
        mxObjectSelects.addElement(SELECT_TO_NAME);
        mxObjectSelects.addElement(SELECT_TO_ACCESS);
        mxObjectSelects.addElement(SELECT_TO_ACCESS_ACCESS);
        mxObjectSelects.addElement(SELECT_FROM_ID);
        mxObjectSelects.addElement(SELECT_FROM_TYPE);
        mxObjectSelects.addElement(SELECT_FROM_NAME);

        String[] oidsArray = new String[objectList.size()];
        oidsArray = (String[])objectList.toArray(oidsArray);
        MapList projectList = DomainRelationship.getInfo(context, oidsArray, mxObjectSelects);

        Map projectMap;
        String toName, toID, toAccess;
        String toAccessBits;
        
        String args[] = {"", "", TYPE_DOCUMENTS, ACCESS_ALL};

        emxProgramCentralUtil_mxJPO emxProgramCentralUtil = new emxProgramCentralUtil_mxJPO(context, null);

        try{
            ContextUtil.pushContext(context);
            String cmd = "trigger off";
            MqlUtil.mqlCommand(context, mqlCommand,  cmd);

            for (int i=0;i<projectList.size();i++)
            {
                projectMap = (Map)projectList.get(i);

                // to Obj info
                //
                toID = (String)projectMap.get(SELECT_TO_ID);
                toName = (String)projectMap.get(SELECT_TO_NAME);

                args[0] = (String)projectMap.get(SELECT_FROM_ID);
                args[1] = (String)projectMap.get(SELECT_TO_ID);

                emxProgramCentralUtil.setAccessInheritance(context, args);

                mqlLogRequiredInformationWriter("==============================================================================\n");
                mqlLogRequiredInformationWriter("Setting access inheritance for object <<" + toName + ">>" + "  ID <<" + toID + ">>\n\n");

                // Add object to list of converted OIDs
                //
                loadMigratedOids(toID);
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        finally
        {
            String cmd = "trigger on";
            MqlUtil.mqlCommand(context, mqlCommand,  cmd);
            ContextUtil.popContext(context);
        }
    }

    public void mqlLogRequiredInformationWriter(String command) throws Exception
    {
        super.mqlLogRequiredInformationWriter(command +"\n");
    }
    public void mqlLogWriter(String command) throws Exception
    {
        super.mqlLogWriter(command +"\n");
    }
}
