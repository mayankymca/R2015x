/**
 * IEFServerAutoRecognition.java
 * This JPO provides hook for server side auto recognition.
 */

import matrix.db.*;
import java.util.*;
import matrix.util.*;

public class IEFServerAutoRecognition_mxJPO extends IEFServerAutoRecognitionBase_mxJPO
{
	public IEFServerAutoRecognition_mxJPO() throws Exception
	{
	}

	public IEFServerAutoRecognition_mxJPO(Context context, String[] args) throws Exception
	{
	}

	public String recognizeIndividualObject(Context context, String[] args) throws Exception
	{
		String cadType 		= args[0];
		String name 		= args[1];
		String busType	 	= args[2];
		String busName 		= args[3];
		String busRevision	= args[4];
		
		//To change the busType, busName, busRevision for AutoRecognition add custom logic here.
		// return statement should be as follows:
		// return "true|newBusType|newBusName|newBusRevision"
		//If you do not want to change these values for a perticular object, return "false"
		
		return "false";
	}
}
