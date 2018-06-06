###############################################################################
#
# $RCSfile: VariantConfigurationR212SchemaChangesForMigration.tcl
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

	proc modifyAdminAddAttribute { adminType adminName attributeName } { 
		set adminName [getActualName $adminName] 
		set attributeName [getActualName $attributeName] 
		set sCmd { mql modify $adminType $adminName add attribute $attributeName } 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "$adminType $adminName successfully modified to add attribute $attributeName" 
		} else { 
			puts stdout "$adminType $adminName modification failed to add attribute $attributeName"
			puts stdout "$outStr" 
			puts stdout "" 
		} 
	} 

	proc modifyInterfaceAddAdmin { interfaceName adminType adminName } { 
		set interfaceName [getActualName $interfaceName] 
		set adminName [getActualName $adminName] 
		set sCmd { mql modify interface $interfaceName add $adminType $adminName } 
		set mqlret [catch {eval $sCmd} outStr] 
		if {$mqlret == 0} { 
			puts stdout "Interface $interfaceName successfully modified to add $adminType $adminName" 
		} else { 
			puts stdout "Interface $interfaceName modification failed to add $adminType $adminName"
			puts stdout "$outStr" 
			puts stdout "" 
		} 
	} 

    proc modifyRelProperties { relName } { 
        set relName [getActualName $relName] 
        set sCmd { mql mod rel $relName from revision replicate cardinality many notpropagateconnection notpreventduplicates to notpropagateconnection} 
        set mqlret [catch {eval $sCmd} outStr] 
        if {$mqlret == 0} { 
            puts stdout "Relationship $relName successfully modified " 
        } else { 
            puts stdout "Relationship $relName modification failed "
            puts stdout "$outStr" 
            puts stdout "" 
        } 
    }

    proc addRelAddAttribute { relName attributeName} { 
        set relName [getActualName $relName]
        set attributeName [getActualName $attributeName]
        set sCmd { mql mod rel $relName add attribute $attributeName} 
        set mqlret [catch {eval $sCmd} outStr] 
        if {$mqlret == 0} { 
            puts stdout "Relationship $relName successfully modified to add attribute $attributeName" 
        } else { 
            puts stdout "Relationship $relName modification failed "
            puts stdout "$outStr" 
            puts stdout "" 
        } 
    }
	proc addAdmin { adminType adminName adminMQL adminSymbolic } { 
		set mqlret [catch {eval $adminMQL} outStr] 
		if {$mqlret == 0} { 
			set sRegProgram [mql get env REGISTRATIONPROGRAM] 
			set sCmd { mql add property $adminSymbolic on program $sRegProgram to $adminType $adminName } 
			set mqlret [catch {eval $sCmd} outStr] 
			if {$mqlret == 0} { 
				puts stdout "$adminType $adminName successfully added" 
			} else { 
				puts stdout "$adminType $adminName successfully added, but registration failed"
				puts stdout "$outStr" 
				puts stdout "" 
			} 
		} else { 
			puts stdout "$adminType $adminName addition failed"
			puts stdout "$outStr" 
			puts stdout "" 
		}
	} 

	proc resolveVPMCFNaming {} {
		set sCurrentCFName [ getActualName type_ConfigurationFeature ] 
		set sVPMCFName [ getActualName type_FeatureConfiguration ] 
		set sRegProgram [mql get env REGISTRATIONPROGRAM] 

		if { $sVPMCFName != "type_FeatureConfiguration" } { 
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

	set sCmd { mql add attribute \"FTRMigrationConflict\" \
			property \"attribute_FTRMigrationConflict\" \
			type \"String\" \
			default \"No\" \
			description \"If object is in heterogeneous structure which cannot be migrated then the value for this attribute is set to Yes\" \
			range = \"Yes\" \
			range = \"No\" } 

	eval [addAdmin "Attribute" "FTRMigrationConflict" $sCmd "attribute_FTRMigrationConflict"] 
	

	set sCmd { mql add attribute \"NewFeatureType\" \
			property \"attribute_NewFeatureType\" \
			type \"String\" \
			default \" \" \
			description \"New Feature Type for the Object  in Migration\" } 
			

	eval [addAdmin "Attribute" "NewFeatureType" $sCmd "attribute_NewFeatureType"] 
	
	set sCmd { mql add interface FTRIntermediateObjectMigration \
			description \"Interface used to define attributes that will get added to types to identify whether to migrate the given object or not and if to migrate then to which type\" \
			property interface_FTRIntermediateObjectMigration \
			attribute \"FTRMigrationConflict" \
			attribute \"NewFeatureType\" } 
	eval [ addAdmin "Interface" "FTRIntermediateObjectMigration" $sCmd "interface_FTRIntermediateObjectMigration"] 
	eval [ modifyInterfaceAddAdmin interface_FTRIntermediateObjectMigration type type_Features ] 
	eval [ modifyInterfaceAddAdmin interface_FTRIntermediateObjectMigration type type_Rule ] 
	eval [ modifyInterfaceAddAdmin interface_FTRIntermediateObjectMigration type type_Products ] 
                eval [ modifyInterfaceAddAdmin interface_FTRIntermediateObjectMigration type type_CONFIGURATIONFEATURES] 
                eval [ modifyInterfaceAddAdmin interface_FTRIntermediateObjectMigration type type_LOGICALSTRUCTURES ] 

	eval [ modifyRelAddFromToType relationship_FeatureListFrom "from" "type" type_CONFIGURATIONFEATURES ] 
	eval [ modifyRelAddFromToType relationship_FeatureListFrom "from" "type" type_LOGICALSTRUCTURES ] 
	eval [ modifyRelAddFromToType relationship_FeatureListTo "to" "type" type_CONFIGURATIONFEATURES ] 
	eval [ modifyRelAddFromToType relationship_FeatureListTo "to" "type" type_LOGICALSTRUCTURES ] 
	eval [ modifyRelAddFromToType relationship_CandidateItem "to" "type" type_CONFIGURATIONFEATURES ] 
	eval [ modifyRelAddFromToType relationship_CandidateItem "to" "type" type_LOGICALSTRUCTURES ] 
	eval [ modifyRelAddFromToType relationship_CommittedItem "to" "type" type_CONFIGURATIONFEATURES ] 
	eval [ modifyRelAddFromToType relationship_CommittedItem "to" "type" type_LOGICALSTRUCTURES ] 
	eval [ modifyRelAddFromToType relationship_InactiveVariesByGBOMFrom "from" "relationship" relationship_InactiveVariesBy ] 
	eval [ modifyRelAddFromToType relationship_InactiveVariesByGBOMFrom "to" "relationship" relationship_InactiveGBOM ] 
	eval [ modifyRelAddFromToType relationship_InactiveVariesBy "from" "type" type_Features ] 
	eval [ modifyRelAddFromToType relationship_InactiveFeatureListFrom "to" "type" type_Features ] 
	eval [ modifyRelAddFromToType relationship_InactiveFeatureListFrom "to" "type" type_CONFIGURATIONFEATURES ] 
	eval [ modifyRelAddFromToType relationship_InactiveFeatureListFrom "to" "relationship" relationship_VariesBy ] 

	eval [ modifyAdminAddAttribute "type" type_LogicalFeature attribute_MarketingName] 
	eval [ modifyAdminAddAttribute "type" type_LogicalFeature attribute_DisplayText] 

	eval [ modifyAdminDerived "type" type_SoftwareFeature type_LogicalFeature ] 

	eval [ modifyRelAddFromToType relationship_CONFIGURATIONSTRUCTURES "from" "type" type_Features ] 
	eval [ modifyRelAddFromToType relationship_CONFIGURATIONSTRUCTURES "to" "type" type_Features ] 

	eval [ modifyAdminDerived "relationship" relationship_VariesBy relationship_CONFIGURATIONSTRUCTURES ] 
	
	eval [ resolveVPMCFNaming ] 

    eval [ modifyRelAddFromToType relationship_CustomGBOM "from" "type" type_LOGICALSTRUCTURES ]
    eval [ modifyRelAddFromToType relationship_CustomGBOM "to" "type" type_Products ]
    eval [ modifyRelAddFromToType relationship_CustomGBOM "to" "type" type_PartFamily ]
    eval [ modifyRelAddFromToType relationship_CustomGBOM "to" "type" type_Part ]
    eval [ modifyRelProperties relationship_CustomGBOM ]
    eval [ addRelAddAttribute relationship_CustomGBOM attribute_Committed ]
	eval [ addRelAddAttribute relationship_CustomGBOM attribute_RuleType]
}

