###############################################################################
#
# $RCSfile: VariantConfigurationR212CleanSchema.tcl
#
# Description:  This file contains the mql to add and modify schema while migrating data from R211 to R212
#               in FTR Application.
# Dependencies: 
###############################################################################
###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.          #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

tcl;
eval { 
	mql set env REGISTRATIONPROGRAM "eServiceSchemaVariableMapping.tcl"

	proc deleteType { typeName } { 
		set typeName [getActualName $typeName] 
		set sCmd {mql delete type $typeName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Type $typeName successfully deleted" 
		} else {
			puts stdout "Type $typeName deletion failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc deleteRelationship { relName } { 
		set relName [getActualName $relName] 
		set sCmd {mql delete relationship $relName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Relationship $relName successfully deleted" 
		} else {
			puts stdout "Relationship $relName deletion failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc deleteAttribute { attribName } { 
		set attribName [getActualName $attribName] 
		set sCmd {mql delete attribute $attribName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Attribute $attribName successfully deleted" 
		} else {
			puts stdout "Attribute $attribName deletion failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc deletePolicy { policyName } { 
		set policyName [getActualName $policyName] 
		set sCmd {mql delete policy $policyName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Policy $policyName successfully deleted" 
		} else {
			puts stdout "Policy $policyName deletion failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc deleteInterface { interfaceName } { 
		set interfaceName [getActualName $interfaceName] 
		set sCmd {mql delete interface $interfaceName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Interface $interfaceName successfully deleted" 
		} else { 
			puts stdout "Interface $interfaceName deletion failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc getActualName { adminName} { 
		set sRegProgram [mql get env REGISTRATIONPROGRAM] 
		set sCmd {mql print program $sRegProgram select property\[$adminName\].to dump} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			if {[string trim $outStr] != ""} { 
				set adminName [string trim [lrange [split $outStr] 1 end]] 
            } 
		} 
		return $adminName 
	} 

	proc modifyRelRemoveFromToType { relName direction typeName } { 
		set relName [getActualName $relName] 
		set typeName [getActualName $typeName] 
		set sCmd {mql modify relationship $relName $direction remove type $typeName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Relationship $relName successfully modified to remove $typeName" 
		} else {
			puts stdout "Relationship $relName modification failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	}

	proc modifyTypeRemoveAttribute { typeName attributeName } { 
		set attributeName [getActualName $attributeName] 
		set typeName [getActualName $typeName] 
		set sCmd {mql modify type $typeName remove attribute $attributeName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Type $typeName successfully modified to remove $attributeName" 
		} else { 
			puts stdout "Type $typeName modification failed"
			puts stdout "$outStr" 
			puts stdout "" 
		} 
	} 

	proc modifyTypeRemoveDerivation { typeName } { 
		set typeName [getActualName $typeName] 
		set sCmd {mql modify type $typeName remove derived} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Type $typeName successfully modified to remove derivation" 
		} else {
			puts stdout "Type $typeName modification failed to remove derivation"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc resolveVPMCFNaming {} {
		set sCurrentCFName [ getActualName type_ConfigurationFeature ] 
		set sVPMCFName [ getActualName type_FeatureConfiguration ] 
		set sRegProgram [mql get env REGISTRATIONPROGRAM] 

		if { $sVPMCFName != "PreR212~Configuration Feature" &&  $sVPMCFName != "type_FeatureConfiguration"} { 
			if { $sCurrentCFName != "Configuration Feature" } { 
				set sCmd { mql modify type $sVPMCFName name "PreR212~Configuration Feature" } 
				set mqlret [catch {eval $sCmd} outStr] 
				if {$mqlret == 0} { 
					set sCmd { mql modify type $sCurrentCFName name "Configuration Feature" } 
					set mqlret [catch {eval $sCmd} outStr] 
					if {$mqlret == 0} { 
						puts stdout "Configuration Feature type of Variant Configuration and VPM Central renamed successfully"
					} else { 
						puts stdout $outStr 
					} 
				} else { 
					puts stdout $outStr 
				} 
			} 
		} else { 
			puts stdout "No conflict found with VPM Central and Variant Configuration Configuration Feature type"
		} 
	} 

	proc modifyAdminDerived { adminType adminName derivedType } { 
		set adminName [getActualName $adminName] 
		set derivedType [getActualName $derivedType] 
		set sCmd { mql modify $adminType $adminName derived $derivedType } 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "$adminType $adminName successfully modified to change derivation" 
		} else { 
			puts stdout "$adminType $adminName derivation modification failed"
			puts stdout "$outStr" 
			puts stdout "" 
		} 
	} 

	proc modifyRelAddFromToType { relName direction adminType adminName } { 
		set relName [getActualName $relName] 
		set adminName [getActualName $adminName] 
		set sCmd {mql modify relationship $relName $direction add $adminType $adminName} 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Relationship $relName successfully modified to add $adminName" 
		} else { 
			puts stdout "Relationship $relName modification failed"
			puts stdout "$outStr" 
			puts stdout "" 
		} 
	} 

        ##############################Schema Changes to be done when Migration steps not executed as FTR data not present ##################################################### 
	
	eval [ modifyRelAddFromToType relationship_CONFIGURATIONSTRUCTURES "from" "type" type_Features ] 
	eval [ modifyRelAddFromToType relationship_CONFIGURATIONSTRUCTURES "to" "type" type_Features ] 
	eval [ modifyAdminDerived "type" type_SoftwareFeature type_LogicalFeature ] 
	eval [ modifyAdminDerived "relationship" relationship_VariesBy relationship_CONFIGURATIONSTRUCTURES ] 
	eval [ resolveVPMCFNaming ] 

	##################################################Remove the following types###############################
	eval [deleteType type_GBOM] 
	eval [deleteType type_FeatureList] 
	eval [deleteType type_ProductFeatures] 
	eval [deleteType type_EquipmentFeature] 
	eval [modifyTypeRemoveDerivation type_ConfigurableFeature]
	eval [deleteType type_Features] 

   ##################################################Remove the following relationships ###############################
	eval [deleteRelationship relationship_FeatureListTo] 
	eval [deleteRelationship relationship_FeatureListFrom] 
	eval [deleteRelationship relationship_GBOMTo] 
	eval [deleteRelationship relationship_GBOMFrom] 
	eval [deleteRelationship relationship_InactiveGBOMFrom] 
	eval [deleteRelationship relationship_InactiveVariesByGBOMFrom] 
	eval [deleteRelationship relationship_InactiveFeatureListFrom] 
	eval [deleteRelationship relationship_AssignedFeature] 

 ##################################################Remove the following attributes ###############################
	eval [deleteAttribute attribute_FeatureType] 
	eval [deleteAttribute attribute_MarketingFeature] 
	eval [deleteAttribute attribute_TechnicalFeature] 
	eval [deleteAttribute attribute_FeatureClassification] 
	eval [deleteAttribute attribute_FeatureCategory] 
	eval [deleteAttribute attribute_MandatoryFeature] 
	eval [deleteAttribute attribute_FeatureSelectionType] 

##################################################Remove the following Policies ###############################

	eval [deletePolicy policy_ProductFeature] 

###################################Remove the following new added attribute,interface for Migration ##############################
###################################Revert back the changes made in Relationships for Migration ##############################

	eval [deleteAttribute FTRMigrationConflict] 
	eval [deleteAttribute NewFeatureType] 

	eval [deleteInterface FTRIntermediateObjectMigration] 

##############################Modifying the Relationships##################################################### 

	eval [modifyRelRemoveFromToType relationship_CandidateItem to type_CONFIGURATIONFEATURES] 
	eval [modifyRelRemoveFromToType relationship_CandidateItem to type_LOGICALSTRUCTURES] 
	eval [modifyRelRemoveFromToType relationship_CommittedItem to type_CONFIGURATIONFEATURES] 
	eval [modifyRelRemoveFromToType relationship_CommittedItem to type_LOGICALSTRUCTURES] 
	
	eval [modifyTypeRemoveAttribute type_LogicalFeature attribute_MarketingName] 
	eval [modifyTypeRemoveAttribute type_LogicalFeature attribute_DisplayText] 

} 


