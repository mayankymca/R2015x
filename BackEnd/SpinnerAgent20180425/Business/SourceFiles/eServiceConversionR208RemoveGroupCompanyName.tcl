#################################################################################
#
# $RCSfile: eServiceConversionR208RemoveGroupCompanyName.tcl.rca $ $Revision: 1.6 $
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

    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Load utility files
    eval [utLoad $sRegProgName]

    # Get admin names from properties
    set sGroupCompanyName [eServiceGetCurrentSchemaName group $sRegProgName group_CompanyName]
    if {"$sGroupCompanyName" == ""} {
        return -code 0
    }
    set sRoleEmployee [eServiceGetCurrentSchemaName role $sRegProgName role_Employee]
    set sRoleCompanyName [eServiceGetCurrentSchemaName role $sRegProgName role_CompanyName]

    # Get symbolic name
    set sProp [string trim [mql get env 1]]

    # Get name from property
    set sPolicy [eServiceGetCurrentSchemaName policy $sRegProgName $sProp]

    # Get all the states
    set sCmd {mql print policy "$sPolicy" select state dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversionR208RemoveGroupCompanyName.tcl"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return -code 1
    }
    set lStates [split $outStr |]

    # For each state get Company Name group access and filter and add same for Employee role.
    set sModCmd "mql modify policy '$sPolicy'"
    set bFound 0
    foreach sState $lStates {

        # Get group Company Name acceses and filter
        set sCmd {mql print policy "$sPolicy" select state\[$sState\].access\[$sGroupCompanyName\] state\[$sState\].filter\[$sGroupCompanyName\] dump |}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversionR208RemoveGroupCompanyName.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }

        if {"$outStr" == ""} {
            continue
        }
        set lOutput [split $outStr |]
        set sCompanyNameAccess [lindex $lOutput 0]
        set sCompanyNameFilter [lindex $lOutput 1]
        regsub -all "\\\\\[" "$sCompanyNameFilter" "\\\\\[" sCompanyNameFilter
        regsub -all "\\\\\]" "$sCompanyNameFilter" "\\\\\]" sCompanyNameFilter

        # Get role Employee acceses and filter
        set sCmd {mql print policy "$sPolicy" select state\[$sState\].access\[$sRoleEmployee\] state\[$sState\].filter\[$sRoleEmployee\] dump |}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversionR208RemoveGroupCompanyName.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }

        if {"$outStr" == ""} {
            set sEmployeeAccess ""
            set sEmployeeFilter ""
        } else {
            set lOutput [split $outStr |]
            set sEmployeeAccess [lindex $lOutput 0]
            set sEmployeeFilter [lindex $lOutput 1]
        }
        
        if {"$sEmployeeAccess" == "$sCompanyNameAccess" && "$sEmployeeFilter" == "$sCompanyNameFilter"} {
            continue
        }

        append sModCmd " state '$sState' add user '$sRoleEmployee' $sCompanyNameAccess filter \"$sCompanyNameFilter\""
        set bFound 1
    }

    if {$bFound == 1} {
        mql trace type INSTALL text ">Program     : eServiceConversionR208RemoveGroupCompanyName.tcl"
        mql trace type INSTALL text ">Running MQL : $sModCmd"
        set mqlret [ catch {eval $sModCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversionR208RemoveGroupCompanyName.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }
    }
    
    return -code 0
}

