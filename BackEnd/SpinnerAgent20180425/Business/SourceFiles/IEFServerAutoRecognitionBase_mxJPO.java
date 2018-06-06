/**
 * IEFServerAutoRecognitionBase.java
 * This is the base JPO to provides hook for server side auto recognition
 * DO NOT CUSTOMIZE this JPO, use IEFServerAutoRecognition for customization.
 */

import matrix.db.*;
import java.util.*;
import matrix.util.*;

import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;

public abstract class IEFServerAutoRecognitionBase_mxJPO
{
	private boolean isDebug 		= false;
	private String language	 		= "en";
	
	abstract public String recognizeIndividualObject(Context context, String[] args) throws Exception;
	
	public IEFServerAutoRecognitionBase_mxJPO() throws Exception
	{
	}

	public IEFServerAutoRecognitionBase_mxJPO(Context context, String[] args) throws Exception
	{
	}

	public Hashtable recognizeObjects(Context context, String[] args) throws Exception
	{
		Hashtable autoRecognitionResultTable = new Hashtable();
		
		Hashtable argumentsTable	    	= (Hashtable) JPO.unpackArgs(args);
		Hashtable autoRecognitionDataTable	= (Hashtable) argumentsTable.get("inputTable");
		
		this.isDebug 	= ((String) argumentsTable.get("isDebugOn")).equalsIgnoreCase("true");
		this.language 	= (String) argumentsTable.get("language");
		
		Enumeration cadids 	= autoRecognitionDataTable.keys();
		while(cadids.hasMoreElements())
		{
			String currentCadId 	= (String)cadids.nextElement();
			IEFXmlNode currentNode 	= (IEFXmlNode) autoRecognitionDataTable.get(currentCadId);
			
			String[] jpoArgs = new String [5];
			jpoArgs[0] = currentNode.getAttribute("type");
			jpoArgs[1] = currentNode.getAttribute("name");
			jpoArgs[2] = currentNode.getAttribute("bustype");
			jpoArgs[3] = currentNode.getAttribute("busname");
			jpoArgs[4] = currentNode.getAttribute("busrevision");
			
			String outputTNR = recognizeIndividualObject(context, jpoArgs);
			if(outputTNR.startsWith("true|"))
			{
				StringTokenizer tokenizer = new StringTokenizer(outputTNR, "|");
				String status 	= (String)tokenizer.nextElement();
				String busType 	= (String)tokenizer.nextElement();
				String busName 	= (String)tokenizer.nextElement();
				String busRev 	= (String)tokenizer.nextElement();
				
				Hashtable currentAttrs	= currentNode.getAttributes();
				currentAttrs.put("bustype", busType);
				currentAttrs.put("busname", busName);
				currentAttrs.put("busrevision", busRev);
				currentNode.setAttributes(currentAttrs);
			}
			
			autoRecognitionResultTable.put(currentCadId, currentNode);
		}
		
		return autoRecognitionResultTable;
	}
	
}

