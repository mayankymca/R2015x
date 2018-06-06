/*
**  IEFCheckoutMatrixUtils
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program contains various methods used for checkout operation
*/

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class IEFCheckoutMatrixUtils_mxJPO 
{
    private MCADGlobalConfigObject globalConfigObject		= null;
    private MCADMxUtil util									= null;
    private MCADServerGeneralUtil serverGeneralUtil			= null;
    private MCADServerResourceBundle serverResourceBundle	= null;
	private IEFGlobalCache globalCache						= null;

	private String cadTypeAttrActualName					= null;
    private String renamedFromAttrActualName				= null;
	private String uuidAttribActualName						= "";

    public static final String ID                       = "ID";
    public static final String DESIGN                   = "Design";
    public static final String CAD_TYPE                 = "CADType";
    public static final String NAME                     = "Name";
    public static final String MATRIX_TYPE              = "MatrixType";
    public static final String MATRIX_VERSION           = "MatrixVersion";
    public static final String REVISION                 = "Revision";
    public static final String VERSION                  = "Version";
    public static final String IS_LOCKED                = "IsLocked";
    public static final String LOCKED_BY                = "LockedBy";
    public static final String FILE_DETAILS             = "FileDetails";
    public static final String PREVIEW_FILE_DETAILS     = "PreviewFileDetails";
    public static final String RENAMED_FROM   			= "RenamedFrom";
    public static final String ACCESS_STATUS            = "AccessStatus";
    public static final String CHECKOUT_ACCESS 			= "CheckoutAccess";	
    public static final String LOCK_ACCESS 				= "LockAccess";
	public static final String OBJECT_STATE   			= "ObjectState";

    public static final String REVISIONS_LIST			= "RevisionsList";
    public static final String VERSIONS_LIST			= "VersionsList";
     public static final String DESCRIPTION				= "Description";
    public static final String UUID    				    = "UUID";
    public static final String UNLOCK_ACCESS            = "UnLockAccess";

    public IEFCheckoutMatrixUtils_mxJPO ()
    {
    }

    public IEFCheckoutMatrixUtils_mxJPO (Context context, String[] args) throws Exception
    {
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);

        String [] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];

        String languageName = args[2];

        init(context, packedGCO, languageName);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    private void init(Context context, String[] packedGCO, String languageName) throws Exception
    {
        this.serverResourceBundle	= new MCADServerResourceBundle(languageName);
		this.globalCache			= new IEFGlobalCache();
        this.globalConfigObject		= (MCADGlobalConfigObject)JPO.unpackArgs(packedGCO);
        this.util					= new MCADMxUtil(context, serverResourceBundle, globalCache);
        this.serverGeneralUtil		= new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, globalCache);

		this.cadTypeAttrActualName        = serverGeneralUtil.getActualNameForAEFData(context, "attribute_CADType");
        this.renamedFromAttrActualName    = serverGeneralUtil.getActualNameForAEFData(context, "attribute_RenamedFrom");
        this.uuidAttribActualName		  = serverGeneralUtil.getActualNameForAEFData(context, "attribute_IEF-UUID");
    }

	public Hashtable getNodeMatrixData(Context context, String[] args) throws Exception
    {
		Hashtable matrixNodeData = new Hashtable();

		String busID	= args[0];

		BusinessObject busObject = new BusinessObject(busID);
		busObject.open(context);

		String busType          = busObject.getTypeName();
		String busName          = busObject.getName();
		String busRev           = busObject.getRevision();            

		String cadType          = busObject.getAttributeValues(context, cadTypeAttrActualName).getValue();
		String renamedFrom		= busObject.getAttributeValues(context, renamedFromAttrActualName).getValue();
        String uuid				= busObject.getAttributeValues(context, uuidAttribActualName).getValue();
		
		String design           = null;
		String revision         = null;
		String version          = null;
		String accessStatus     = null;
		String objectState		= null;
		String checkoutAccess	= "false";
		String busLocker        = "";
        String description      = null;
		String lockAccess       = getLockAccessFlagOnBusObject(context, busObject);

		Vector revisionsList		= null;
		Vector versionsList			= null;
		String requiredMajorType	= null;
		String requiredMinorType	= null;
        String isLockAccess			= MCADAppletServletProtocol.FALSE;
        String isUnLockAccess		= MCADAppletServletProtocol.FALSE;

		boolean isInputMajor = util.isMajorObject(context, busID);//globalConfigObject.isMajorType(busType); // [NDM] OP6
		if(isInputMajor)
		{
			requiredMajorType = busType;
			requiredMinorType = util.getCorrespondingType(context, busType);
			
			revision      = busRev;
			design        = busType;
			version       = " ";
			versionsList  = getVersionsList(context, null, busObject, requiredMajorType, requiredMinorType);
			revisionsList = getRevisionsList(context, busObject, requiredMajorType, requiredMinorType);
			accessStatus  = getAccessStatusOnBusObject(context, busObject);
			busLocker     = busObject.getLocker(context).getName();
			objectState	  = util.getCurrentState(context, busObject.getObjectId()); 
            description	  = busObject.getDescription(context);
            isLockAccess  = getLockAccess(context, busObject);
            isUnLockAccess = getUnLockAccess(context, busObject);
		}
		else
		{
			requiredMajorType = util.getCorrespondingType(context, busType);
			requiredMinorType = busType;

			BusinessObject majorBusObject = util.getMajorObject(context, busObject);
			majorBusObject.open(context);
			
			revision      = majorBusObject.getRevision();
			design        = majorBusObject.getTypeName();
			version       = MCADUtil.getVersionFromMinorRevision(revision, busRev);

			versionsList  = getVersionsList(context, busObject, majorBusObject, requiredMajorType, requiredMinorType);
			revisionsList = getRevisionsList(context, majorBusObject, requiredMajorType, requiredMinorType);

			accessStatus  = getAccessStatusOnBusObject(context, majorBusObject);
			busLocker     = majorBusObject.getLocker(context).getName();
			objectState	  = util.getCurrentState(context, majorBusObject.getObjectId());
			description	  = busObject.getDescription(context);
			isLockAccess  = getLockAccess(context, busObject);
			isUnLockAccess = getUnLockAccess(context, busObject);

			majorBusObject.close(context);
		}
		
		if(!accessStatus.equals(serverResourceBundle.getString("mcadIntegration.Server.Message.NoAccess")))
		{
			checkoutAccess = "true";
		}

		matrixNodeData.put(ID, busID);
		matrixNodeData.put(DESIGN, design);
		matrixNodeData.put(CAD_TYPE, cadType);
		matrixNodeData.put(NAME, busName);
		matrixNodeData.put(REVISION, revision);
		matrixNodeData.put(VERSION, version);
		matrixNodeData.put(MATRIX_TYPE, busType);
		matrixNodeData.put(MATRIX_VERSION, busRev);
		matrixNodeData.put(LOCKED_BY, busLocker);
		matrixNodeData.put(REVISIONS_LIST, revisionsList);
		matrixNodeData.put(VERSIONS_LIST, versionsList);
		matrixNodeData.put(ACCESS_STATUS, accessStatus);
		matrixNodeData.put(CHECKOUT_ACCESS, checkoutAccess);
		matrixNodeData.put(LOCK_ACCESS, lockAccess);
		matrixNodeData.put(RENAMED_FROM, renamedFrom);
		matrixNodeData.put(OBJECT_STATE, objectState);
        matrixNodeData.put(DESCRIPTION,description);
        matrixNodeData.put(UUID, uuid);
        matrixNodeData.put(LOCK_ACCESS, isLockAccess);
        matrixNodeData.put(UNLOCK_ACCESS, isUnLockAccess);

		Vector fileDetails          = getFileDetails(context, busObject, cadType, requiredMajorType, matrixNodeData);
		Vector previewFileDetails   = getObjectPreviewFileDetails(context, busObject, cadType, requiredMajorType);
		matrixNodeData.put(FILE_DETAILS, fileDetails);
		matrixNodeData.put(PREVIEW_FILE_DETAILS, previewFileDetails);

		return matrixNodeData;
	}

	private Vector getRevisionsList(Context context, BusinessObject majorObject, String requiredMajorType, String requiredMinorType) throws MatrixException, MCADException
    {
        Vector revisionsList = new Vector();

        BusinessObjectList majorObjectsList = majorObject.getRevisions(context);
        BusinessObjectItr majorObjectsItr   = new BusinessObjectItr(majorObjectsList);
        while(majorObjectsItr.next())
        {
            BusinessObject revisionMajorObject = majorObjectsItr.obj();
            revisionMajorObject.open(context);
            String majorType		= revisionMajorObject.getTypeName();
            String majorRevision 	= revisionMajorObject.getRevision();
            String majorID			= revisionMajorObject.getObjectId();
            boolean isFinalized		= isFinalized(context, revisionMajorObject);
            revisionMajorObject.close(context);
            
            String Args[] = new String[3];
            Args[0] = majorID;
            Args[1] = "VersionOf";
            Args[2] = requiredMinorType;
            String queryResult 		= util.executeMQL(context,"expand bus $1 to rel $2 type $3", Args);
            
            if(queryResult.startsWith("true|") && queryResult.length()>10)
            {
            	revisionsList.addElement(majorRevision);
            }
			// [NDM] H68 : will type checking is usefull in NDM
            //else if(queryResult.startsWith("true|") && isFinalized && majorType.equals(requiredMajorType))
            else if(queryResult.startsWith("true|") && majorType.equals(requiredMajorType))
            {
            	revisionsList.addElement(majorRevision);
            }
        }

        return revisionsList;
    }
    
    private Vector getVersionsList(Context context, BusinessObject selectedObject, BusinessObject majorObject, String requiredMajorType, String requiredMinorType) throws Exception
    {
        Vector versionsList = new Vector();

        String majorRevision 	= majorObject.getRevision();
        String majorType		= majorObject.getTypeName();
        String finalizedMinorID = "";
        
        BusinessObject finalizedMinorObject = util.getFinalizedFromMinorObject(context, majorObject);
		if(finalizedMinorObject != null)
		{
			finalizedMinorID = finalizedMinorObject.getObjectId(context);
		}

        BusinessObjectList minorBusObjectsList = null;
    	if(selectedObject == null)
    		minorBusObjectsList = util.getMinorObjects(context, majorObject);
    	else
    		minorBusObjectsList = selectedObject.getRevisions(context);
    	
    	if(isFinalized(context, majorObject) && requiredMajorType.equals(majorType))
    		versionsList.addElement(" ");
    	
    	BusinessObjectItr minorBusObjectsItr = new BusinessObjectItr(minorBusObjectsList);
        while(minorBusObjectsItr.next())
        {
            BusinessObject minorBusObject = minorBusObjectsItr.obj();
            minorBusObject.open(context);
            String minorID			= minorBusObject.getObjectId();
            String minorType 		= minorBusObject.getTypeName();
            String minorRevision 	= minorBusObject.getRevision();
            minorBusObject.close(context);
            
            if(minorType.equals(requiredMinorType) && !minorID.equals(finalizedMinorID))
            {
            	String version = MCADUtil.getVersionFromMinorRevision(majorRevision, minorRevision);
            	versionsList.addElement(version);
            }
        }

        return versionsList;
    }

    private boolean isFinalized(Context context, BusinessObject busObject)throws MCADException
    {
        boolean isFinalized = false;

        isFinalized = serverGeneralUtil.isBusObjectFinalized(context, busObject);

        return isFinalized;
    }

	private String getLockAccessFlagOnBusObject(Context context, BusinessObject busObject) throws MCADException
    {
    	String lockAccessFlag = "false";
        
        try
        {
            Access access  = busObject.getAccessMask(context);
            if(access.hasLockAccess())
            	lockAccessFlag = "true"; 
        }
        catch(Exception exception)
        {
            MCADServerException.createException(exception.getMessage(), exception);
        }

        return lockAccessFlag;
    }

	private String getAccessStatusOnBusObject(Context context, BusinessObject busObject)throws MCADException
    {
        String accessStatus = serverResourceBundle.getString("mcadIntegration.Server.Message.NoAccess");

        try
        {
            Access access = busObject.getAccessMask(context);

            if(access.hasCheckoutAccess() && access.hasLockAccess() && access.hasCheckinAccess() && access.hasReviseAccess() && access.hasModifyAccess())
            {
                accessStatus = serverResourceBundle.getString("mcadIntegration.Server.Message.EditAccess");
            }
            else if(access.hasReadAccess() && access.hasCheckoutAccess())
            {
                accessStatus = serverResourceBundle.getString("mcadIntegration.Server.Message.ViewAccess");
            }
        }
        catch(Exception exception)
        {
            MCADServerException.createException(exception.getMessage(), exception);
        }

        return accessStatus;
    }

	private Vector getFileDetails(Context context, BusinessObject busObject, String cadType, String design, Hashtable matrixNodeData) throws MCADException, MatrixException
    {
        Vector fileDetails = new Vector(4);

        String formatName = globalConfigObject.getFormatsForType(design, cadType);
        String fileName     = null;

        if(formatName != null && !formatName.equals(""))
        {
            fileName = getFilesInFormat(context, busObject, formatName);
        }

        if(formatName != null && !formatName.equals("") && fileName != null && !fileName.equals(""))
        {
            fileDetails.addElement(formatName);
            fileDetails.addElement(fileName);
        }

        return fileDetails;
    }

    private String getFilesInFormat(Context context, BusinessObject busObject, String formatName) throws MatrixException
    {
        String fileNamesList = "";

        FileList fileList = busObject.getFiles(context, formatName);
        FileItr itr = new FileItr(fileList);
        while(itr.next())
        {
            String fileName = itr.obj().getName();
            fileNamesList += "|"+ fileName;
        }
        if(fileNamesList.length() > 0)
        {
            fileNamesList = fileNamesList.substring(1,fileNamesList.length());
        }
        return fileNamesList;
    }

    public Vector getObjectPreviewFileDetails(Context context, BusinessObject busObject, String cadType, String design) throws MCADException, MatrixException
    {
        Vector previewFileDetails = new Vector(10);

        String previewType = serverGeneralUtil.getPreviewType(cadType);
        String formatName = globalConfigObject.getFormatsForType(design, previewType);
        if(globalConfigObject.isCreateDependentDocObj())
        {
            Hashtable relsAndEnds	= serverGeneralUtil.getAllWheareUsedRelationships(context, busObject, true, MCADServerSettings.DERIVEDOUTPUT_LIKE);
            Enumeration keys		= relsAndEnds.keys();
            while(keys.hasMoreElements())
            {
                Relationship rel = (Relationship)keys.nextElement();
                // ACTION : For the case of an instance, in Attrb based config,
                // rel attributes matching instance only should be considered
                BusinessObject depDocObj = rel.getTo();
                getPreviewFilesFromPreviewFormat(context, depDocObj, formatName, previewFileDetails);
            }
        }
        else
        {
            getPreviewFilesFromPreviewFormat(context, busObject, formatName, previewFileDetails);
        }

        return previewFileDetails;
    }

    private void getPreviewFilesFromPreviewFormat(Context context, BusinessObject busObject, String formatName, Vector previewFileDetails) throws MatrixException
    {
        FileList files = busObject.getFiles(context, formatName);

        if(files != null && files.size() > 0)
        {
            previewFileDetails.add(busObject.getObjectId());
            previewFileDetails.add(formatName);
            String filename =((matrix.db.File)files.firstElement()).getName();
            previewFileDetails.add(filename);
        }
    }

   private String getLockAccess(Context context, BusinessObject busObject)throws MCADException
	{
		String lockAccess = MCADAppletServletProtocol.FALSE;
		try
		{
			Access access = busObject.getAccessMask(context);
        	if(access.hasLockAccess())
			  lockAccess = MCADAppletServletProtocol.TRUE;	
			
		}
		catch(Exception exception)
		{
			
		}
		return lockAccess;
	}
	
	private String getUnLockAccess(Context context, BusinessObject busObject)throws MCADException
	{
		String unLockAccess = MCADAppletServletProtocol.FALSE;
		try
		{
			Access access = busObject.getAccessMask(context);
        	if(access.hasUnlockAccess())
        		unLockAccess = MCADAppletServletProtocol.TRUE;	
			
		}
		catch(Exception exception)
		{
			
		}
		return unLockAccess;
	}
}
