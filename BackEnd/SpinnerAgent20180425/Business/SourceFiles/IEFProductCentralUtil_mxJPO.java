// emxMsoiProductCentralUtil.java
//
// 
// Copyright (c) 2002 MatrixOne, Inc.
// All Rights Reserved
// This program contains proprietary and trade secret information of
// MatrixOne, Inc.  Copyright notice is precautionary only and does
// not evidence any actual or intended publication of such program.

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.apps.framework.ui.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;

/**
 * The <code>IEFProductCentralUtil</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFProductCentralUtil_mxJPO 
{

/**
   * Constructs a new IEFProductCentralUtil JPO object.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args an array of String arguments for this method
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */
  public IEFProductCentralUtil_mxJPO (Context context, String[] args)
      throws Exception
  {
    // Call the super constructor
    super();
  }
   
  /**
   * Get Products of a the current user
   * Returns a maplist of the current users Product ids
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getCurrentUserProducts(Context context, String[] args) throws MatrixException
   {
     MapList productOwnedList = new MapList();
	 MapList productAllList = new MapList();
	 MapList productList = new MapList();
	 
     try
     {
		// rp3 Fix for JPO compilation issue
		productOwnedList = (MapList)JPO.invoke(context, "emxProduct", null, "getOwnedProducts", args, MapList.class);
		productAllList = (MapList)JPO.invoke(context, "emxProduct", null, "getAllProducts", args, MapList.class);
		
		productList.addAll(productOwnedList);
		productList.addAll(productAllList);
		
		//Construct the object of emxProduct JPO
		//${CLASS:emxProduct} productsObj =  new ${CLASS:emxProduct}(context, new String[] {});
        //Call to the common method that fetches the products owned by the context user.        
		//productList = productsObj.getOwnedProducts(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getCurrentUserProducts : " + ex.toString()) );
     }
     return productList;
   }

  /**
   * Get the folders of a Product
   * Returns a maplist of features 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getOwnedContextFeatures(Context context, String[] args) throws MatrixException
   {
     MapList featureList = new MapList();
	 MapList logicalFeatureList = new MapList();
	 MapList manufacturingFeatureList = new MapList();
	 MapList configurationFeatureList = new MapList();
	 
     try
     {
		// rp3 Fix for JPO compilation issue
		logicalFeatureList = (MapList)JPO.invoke(context, "LogicalFeature", null, "getLogicalFeatureStructure", args, MapList.class);
		manufacturingFeatureList = (MapList)JPO.invoke(context, "ManufacturingFeature", null, "getManufacturingFeatureStructure", args, MapList.class);
		configurationFeatureList = (MapList)JPO.invoke(context, "ConfigurationFeature", null, "getConfigurationFeatureStructure", args, MapList.class);
		
		featureList.addAll(logicalFeatureList);
		featureList.addAll(manufacturingFeatureList);
		featureList.addAll(configurationFeatureList);
		
		//${CLASS:emxFeature} emxFeatureObject = new ${CLASS:emxFeature}(context, new String[] {});
        //featureList = (MapList) emxFeatureObject.getOwnedContextFeatures(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getOwnedContextFeatures: " + ex.toString()) );
     }
     return featureList;
   }

  /**
   * Get the folders of a Product
   * Returns a maplist of features 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getCurrentUserFeatures(Context context, String[] args) throws MatrixException
   {
     MapList featureList = new MapList();
	 MapList logicalFeatureList = new MapList();
	 MapList manufacturingFeatureList = new MapList();
	 MapList configurationFeatureList = new MapList();
	 
     try
     {
		// rp3 Fix for JPO compilation issue
		//SJ7+ - Logically Current User Features are Owned features. Hence not calling getTopLevelLogicalFeatures getTopLevelManufacturingFeatures getTopLevelConfigurationFeatures
		logicalFeatureList = (MapList)JPO.invoke(context, "LogicalFeature", null, "getTopLevelOwnedLogicalFeatures", args, MapList.class);
		manufacturingFeatureList = (MapList)JPO.invoke(context, "ManufacturingFeature", null, "getTopLevelOwnedManufacturingFeatures", args, MapList.class);
		configurationFeatureList = (MapList)JPO.invoke(context, "ConfigurationFeature", null, "getTopLevelOwnedConfigurationFeatures", args, MapList.class);
		
		featureList.addAll(logicalFeatureList);
		featureList.addAll(manufacturingFeatureList);
		featureList.addAll(configurationFeatureList);
				
		//${CLASS:emxFeature} featuresObj =  new ${CLASS:emxFeature}(context, new String[] {});
		//featureList = featuresObj.getOwnedDesktopFeatures(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getCurrentUserFeatures: " + ex.toString()) );
     }
     return featureList;
   }
   
  /**
   * Get the sub folders of a Product
   * Returns a maplist of sub features 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getOptionList(Context context, String[] args) throws MatrixException
   {
     MapList subFeatureList = new MapList();
	 MapList subLogicalFeatureList = new MapList();
	 MapList subManufacturingFeatureList = new MapList();
	 MapList subConfigurationList = new MapList();
	 
     try
     {
		// rp3 Fix for JPO compilation issue
		subLogicalFeatureList = (MapList)JPO.invoke(context, "LogicalFeature", null, "getLogicalFeatureStructure", args, MapList.class);
		subManufacturingFeatureList = (MapList)JPO.invoke(context, "ManufacturingFeature", null, "getManufacturingFeatureStructure", args, MapList.class);
		subConfigurationList = (MapList)JPO.invoke(context, "ConfigurationFeature", null, "getConfigurationFeatureStructure", args, MapList.class);
		
		subFeatureList.addAll(subLogicalFeatureList);
		subFeatureList.addAll(subManufacturingFeatureList);
		subFeatureList.addAll(subConfigurationList);
		
		//${CLASS:emxFeature} emxFeatureObject = new ${CLASS:emxFeature}(context, new String[] {});
		//subFeatureList = (MapList) emxFeatureObject.getOptionList(context, args);
	 }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getOptionList: " + ex.toString()) );
     }
     return subFeatureList;
   }

  /**
   * Get the requirements of a Product
   * Returns a maplist of requirements 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getCurrentUserRequirements(Context context, String[] args) throws MatrixException
   {
     MapList requirementList = new MapList();
     try
     {
		// rp3 Fix for JPO compilation issue
		requirementList = (MapList)JPO.invoke(context, "emxRequirement", null, "getOwnedRequirements", args, MapList.class);
		 //${CLASS:emxRequirement} requirementsObj =  new ${CLASS:emxRequirement}(context, new String[] {});
		 //requirementList = requirementsObj.getOwnedRequirements(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getCurrentUserRequirements: " + ex.toString()) );
     }
     return requirementList;
   }

  /**
   * Get the requirements of a Product
   * Returns a maplist of requirements 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getRelatedRequirements(Context context, String[] args) throws MatrixException
   {
     MapList requirementList = new MapList();
	 MapList ownedList = new MapList();
	 MapList assignedList = new MapList();
	 MapList allList = new MapList();
     try
     {
		// rp3 Fix for JPO compilation issue
		ownedList = (MapList)JPO.invoke(context, "emxRequirement", null, "getOwnedRequirements", args, MapList.class);
		assignedList = (MapList)JPO.invoke(context, "emxRequirement", null, "getAssignedRequirements", args, MapList.class);
		allList = (MapList)JPO.invoke(context, "emxRequirement", null, "getAllRequirements", args, MapList.class);
		
		requirementList.addAll(ownedList);
		requirementList.addAll(assignedList);
		requirementList.addAll(allList);
				
		 //${CLASS:emxRequirement} emxRequirementsObject = new ${CLASS:emxRequirement}(context, new String[] {});
		 //requirementList = (MapList) emxRequirementsObject.getRelatedRequirements(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getRelatedRequirements: " + ex.toString()) );
     }
     return requirementList;
   }

  /**
   * Get the builds of a Product
   * Returns a maplist of builds 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getCurrentUserBuilds(Context context, String[] args) throws MatrixException
   {
     MapList buildsList = new MapList();
	 MapList ownedBuildsList = new MapList();
	 MapList allBuildsList = new MapList();
	 
     try
     {
		// rp3 Fix for JPO compilation issue
		ownedBuildsList = (MapList)JPO.invoke(context, "emxBuild", null, "getOwnedBuilds", args, MapList.class);
		allBuildsList = (MapList)JPO.invoke(context, "emxBuild", null, "getAllBuilds", args, MapList.class);
		
		buildsList.addAll(ownedBuildsList);
		buildsList.addAll(allBuildsList);		
		
		 //${CLASS:emxBuild} buildsObj =  new ${CLASS:emxBuild}(context, args);
		 //buildsList = buildsObj.getOwnedBuilds(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getCurrentUserBuilds: " + ex.toString()) );
     }
     return buildsList;
   }

  /**
   * Get the test cases of a Product
   * Returns a maplist of test cases 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getRelatedTestCases(Context context, String[] args) throws MatrixException
   {
     MapList testCasesList = new MapList();
     try
     {
		// rp3 Fix for JPO compilation issue
		testCasesList = (MapList)JPO.invoke(context, "emxTestCase", null, "getRelatedTestCases", args, MapList.class);
		 //${CLASS:emxTestCase} emxTestCaseObject = new ${CLASS:emxTestCase}(context, new String[] {});
         //testCasesList = (MapList) emxTestCaseObject.getRelatedTestCases(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getRelatedTestCases: " + ex.toString()) );
     }
     return testCasesList;
   }

  /**
   * Get the use cases of a Product
   * Returns a maplist of use cases 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getRelatedUseCases(Context context, String[] args) throws MatrixException
   {
     MapList useCasesList = new MapList();
     try
     {
		// rp3 Fix for JPO compilation issue
		useCasesList = (MapList)JPO.invoke(context, "emxUseCase", null, "getRelatedUseCases", args, MapList.class);
		 //${CLASS:emxUseCase} useCasesObj =  new ${CLASS:emxUseCase}(context, new String[] {});
		 //useCasesList = useCasesObj.getRelatedUseCases(context, args);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiProductCentralUtil:getRelatedUseCases: " + ex.toString()) );
     }
     return useCasesList;
   }
}
