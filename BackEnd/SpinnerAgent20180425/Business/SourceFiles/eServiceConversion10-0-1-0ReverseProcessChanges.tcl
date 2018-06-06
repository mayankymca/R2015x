#################################################################################
#
# $RCSfile: eServiceConversion10-0-1-0ReverseProcessChanges.tcl.rca $ $Revision: 1.5 $
#
# Description:
# DEPENDENCIES: This program has to lie in Schema & Common directories with the
#               same name and should be called from all builds
#################################################################################
#################################################################################
#                                                                               #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                   #
#   This program contains proprietary and trade secret information of           #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not       #
#   evidence any actual or intended publication of such program.                #
#                                                                               #
#################################################################################

tcl;
eval {

###############################################################################
#
# Procedure:    utLoad
#
# Description:  Procedure to load other tcl utilities procedures.
#
# Parameters:   sProgram                - Tcl file to load
#
# Returns:      sOutput                 - Filtered tcl file
#               glUtLoadProgs           - List of loaded programs
#
###############################################################################

proc utLoad { sProgram } {

    global glUtLoadProgs env

    if { ! [ info exists glUtLoadProgs ] } {
        set glUtLoadProgs {}
    }

    if { [ lsearch $glUtLoadProgs $sProgram ] < 0 } {
        lappend glUtLoadProgs $sProgram
    } else {
        return ""
    }

    if { [ catch {
        set sDir "$env(TCL_LIBRARY)/mxTclDev"
        set pFile [ open "$sDir/$sProgram" r ]
        set sOutput [ read $pFile ]
        close $pFile

    } ] == 0 } { return $sOutput }

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utload

    # Get environment variables
    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Load utility files
    eval [utLoad $sRegProgName]

    # Get admin names
    set sPolicyProcess [emxGetCurrentSchemaName policy $sRegProgName policy_Process]
    set sRoleBuyer [emxGetCurrentSchemaName role $sRegProgName role_Buyer]
    set sRoleSupplierRepresentative [emxGetCurrentSchemaName role $sRegProgName role_SupplierRepresentative]
    set sRoleSystemTransitionManager [emxGetCurrentSchemaName role $sRegProgName role_SystemTransitionManager]
    set sRoleSystemConversionManager [emxGetCurrentSchemaName role $sRegProgName role_SystemConversionManager]
    set sRoleBuyerAdministrator [emxGetCurrentSchemaName role $sRegProgName role_BuyerAdministrator]
    set sRoleSupplierDevelopmentManager [emxGetCurrentSchemaName role $sRegProgName role_SupplierDevelopmentManager]
    set sGroupShadowAgent [emxGetCurrentSchemaName group $sRegProgName group_ShadowAgent]

    # Get all the states of policy Process
    set sCmd {mql print policy "$sPolicyProcess" select state property.name dump tcl}
    set mqlret [ catch {eval $sCmd} lOutput ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-1-0ReverseProcessChanges.tcl"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return -code 1
    }
    
    # If Process has multiple states then remove them and add Exists state.
    if {[llength [lindex [lindex $lOutput 0] 0]] != 1} {
        set sCmd {mql modify policy "$sPolicyProcess" \
                      sequence "-" \
                      add \
                      state "Exists" \
                        revision false \
                        public read \
                        owner all \
                        user "$sRoleBuyer" show,read,modify,toconnect,fromconnect,todisconnect,fromdisconnect \
                        user "$sRoleSupplierRepresentative" show,read,modify,toconnect,fromconnect,todisconnect,fromdisconnect \
                        user "$sRoleSystemTransitionManager" all \
                        user "$sRoleSystemConversionManager" all \
                        user "$sGroupShadowAgent" all \
                        user "$sRoleBuyerAdministrator" show,read,modify,fromconnect,toconnect,fromdisconnect,todisconnect \
                        user "$sRoleSupplierDevelopmentManager" show,read,modify,fromconnect,toconnect,fromdisconnect,todisconnect \
                      property "state_Exists" value "Exists" \
                 }

        foreach sState [lindex [lindex $lOutput 0] 0] {
            append sCmd " remove state \"$sState\""
        }

        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-1-0ReverseProcessChanges.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }
        
        # remove registrations of removed states.
        foreach sProp [lindex [lindex $lOutput 0] 1] {
            if {[string first "state_" "$sProp"] == 0 && "$sProp" != "state_Exists"} {
                set sCmd {mql delete property "$sProp" on policy "$sPolicyProcess"}
                set mqlret [ catch {eval $sCmd} outStr ]
                if {$mqlret != 0} {
                    mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-1-0ReverseProcessChanges.tcl"
                    mql trace type INSTALL text ">$outStr"
                    mql trace type INSTALL text ""
                    return -code 1
                }
            }
        }
    }
    
    return -code 0
}

