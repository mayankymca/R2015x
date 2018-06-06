import matrix.db.Context;
import com.matrixone.MCADIntegration.server.MCADServerException;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import java.util.Hashtable;
import matrix.db.JPO;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import java.util.Vector;

public class DSCStoreNameForCheckin_mxJPO
{

	public DSCStoreNameForCheckin_mxJPO(Context context, String[] args) throws Exception
	{
		
	}

	//This method should retrun Store name
	public String getObjectStoreForFile(Context context, String[] args) throws Exception
	{

		String storeName = "";
		try
		{
			Hashtable argsTable			= (java.util.Hashtable) JPO.unpackArgs(args);
			Vector checkinFileDetails	= (Vector)argsTable.get("checkinFileDetails");
			if(checkinFileDetails != null)
				storeName = (String)checkinFileDetails.elementAt(2);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
			MCADServerException.createException(e.getMessage(), e);
		}
        
		//Note : return "" if failed
		//       return "storeName" if succeed
		return storeName;
	}
}
