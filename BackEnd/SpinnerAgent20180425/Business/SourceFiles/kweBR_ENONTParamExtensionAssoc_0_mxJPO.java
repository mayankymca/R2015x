
import matrix.db.Context;
import java.util.List;
import java.util.Iterator;


/**
 * ${CLASSNAME}
 */
public final class kweBR_ENONTParamExtensionAssoc_0_mxJPO extends com.dassault_systemes.knowledge_impls.KweWithJPOServerAdapter {

	public kweBR_ENONTParamExtensionAssoc_0_mxJPO (Context context, String[] args) throws Exception { super(); }

	/**
	 * evaluate
	 * @param iContext
	 * @param ThisObject
	 * @param Parameters
	 */
	public final void evaluate(matrix.db.Context iContext, com.dassault_systemes.knowledge_itfs.IKweValue ThisObject, com.dassault_systemes.knowledge_itfs.IKweValue Parameters)	
			throws Exception {

		/**
		 * Local constant variables
		 */
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_0__VPMReference_ = findTypeAndCreateInternal("String", "VPMReference");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_1__XP_VPMReference_Ext_ = findTypeAndCreateInternal("String", "XP_VPMReference_Ext");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_2__ParamActive_ = findTypeAndCreateInternal("String", "ParamActive");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_3__DeploymentExtensionNa = findTypeAndCreateInternal("String", "DeploymentExtensionName");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_4__VPMRepReference_ = findTypeAndCreateInternal("String", "VPMRepReference");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_5__XP_VPMRepReference_Ex = findTypeAndCreateInternal("String", "XP_VPMRepReference_Ext");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_6__Part_ = findTypeAndCreateInternal("String", "Part");
		com.dassault_systemes.knowledge_itfs.IKweValue _STRING_7__XP_Part_Ext_ = findTypeAndCreateInternal("String", "XP_Part_Ext");
 {
			com.dassault_systemes.knowledge_itfs.IKweValue TypeExtension = findTypeAndCreateInternal("Type", null);
			com.dassault_systemes.knowledge_itfs.IKweValue _BOOLEAN_CONSTANT_TRUE_ = findConstant("TRUE");
			 {
				setCurrentSourceLine(5);
				if ( ThisObject.invokeMethod("IsASortOf", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_0__VPMReference_} ).booleanValue() ) {
					 {
						 {
							setCurrentSourceLine(7);
							TypeExtension.valuateWithErrors( findFunction("FindType", new com.dassault_systemes.knowledge_itfs.IKweType[] {findType("String")} ).invokeMethod(iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] { _STRING_1__XP_VPMReference_Ext_ } ) );
						}
;
						 {
							setCurrentSourceLine(8);
							if ( TypeExtension.invokeMethod("HasAttribute", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_2__ParamActive_} ).invokeMethod("==", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_BOOLEAN_CONSTANT_TRUE_} ).booleanValue() ) {
								 {
									 {
										setCurrentSourceLine(10);
										Parameters.invokeMethod("SetAttributeString", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_3__DeploymentExtensionNa, _STRING_1__XP_VPMReference_Ext_} );
									}
;
								}
;
							}
;
						}
					}
;
				}
				else {
					 {
						setCurrentSourceLine(13);
						if ( ThisObject.invokeMethod("IsASortOf", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_4__VPMRepReference_} ).booleanValue() ) {
							 {
								 {
									setCurrentSourceLine(15);
									TypeExtension.valuateWithErrors( findFunction("FindType", new com.dassault_systemes.knowledge_itfs.IKweType[] {findType("String")} ).invokeMethod(iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] { _STRING_5__XP_VPMRepReference_Ex } ) );
								}
;
								 {
									setCurrentSourceLine(16);
									if ( TypeExtension.invokeMethod("HasAttribute", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_2__ParamActive_} ).invokeMethod("==", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_BOOLEAN_CONSTANT_TRUE_} ).booleanValue() ) {
										 {
											 {
												setCurrentSourceLine(18);
												Parameters.invokeMethod("SetAttributeString", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_3__DeploymentExtensionNa, _STRING_5__XP_VPMRepReference_Ex} );
											}
;
										}
;
									}
;
								}
							}
;
						}
						else {
							 {
								setCurrentSourceLine(21);
								if ( ThisObject.invokeMethod("IsASortOf", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_6__Part_} ).booleanValue() ) {
									 {
										 {
											setCurrentSourceLine(23);
											TypeExtension.valuateWithErrors( findFunction("FindType", new com.dassault_systemes.knowledge_itfs.IKweType[] {findType("String")} ).invokeMethod(iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] { _STRING_7__XP_Part_Ext_ } ) );
										}
;
										 {
											setCurrentSourceLine(24);
											if ( TypeExtension.invokeMethod("HasAttribute", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_2__ParamActive_} ).invokeMethod("==", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_BOOLEAN_CONSTANT_TRUE_} ).booleanValue() ) {
												 {
													 {
														setCurrentSourceLine(26);
														Parameters.invokeMethod("SetAttributeString", iContext, new com.dassault_systemes.knowledge_itfs.IKweValue[] {_STRING_3__DeploymentExtensionNa, _STRING_7__XP_Part_Ext_} );
													}
;
												}
;
											}
;
										}
									}
;
								}
;
							}
						}
;
					}
				}
;
			}
		}
	}

	public int getVersion() { return 13; }

	public long getSourceTimeStamp() { return 1381847017000L; }

	public long getSourceCheckSum() { return 4140252721L; }

	public static String[][] getKweArgTypes() { return new String[][] { new String[] {"Context", "In", "iContext" }, new String[] {"Part", "In", "ThisObject" }, new String[] {"RuleContext_INFRADeploymentExtensionComputation", "In", "Parameters" }}; }
}
