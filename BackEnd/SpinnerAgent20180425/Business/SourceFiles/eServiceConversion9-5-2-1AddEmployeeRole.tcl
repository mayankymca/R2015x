#################################################################################
#
# $RCSfile: eServiceConversion9-5-2-1AddEmployeeRole.tcl.rca $ $Revision: 1.6 $
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

    # return if conversion program is already run
    set sCmd {mql print program eServiceSystemInformation.tcl \
                  select property\[Conversion9-5-2-1AddEmployeeRole\].value \
                  dump | \
                  }
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-2-1AddEmployeeRole.tcl"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return -code 1
    }
    
    if {"$outStr" == "Executed"} {
        return -code 0
    }

    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Load utility files
    eval [utLoad $sRegProgName]

    # Get admin names from properties
    set sGroupCompanyName [eServiceGetCurrentSchemaName role $sRegProgName role_CompanyName]
    set sRoleEmployee [eServiceGetCurrentSchemaName role $sRegProgName role_Employee]

    # Get symbolic name
    set sProp [string trim [mql get env 1]]

    # Get name from property
    set sPolicy [eServiceGetCurrentSchemaName policy $sRegProgName $sProp]

    # Get all the states
    set sCmd {mql print policy "$sPolicy" select state dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-2-1AddEmployeeRole.tcl"
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
            mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-2-1AddEmployeeRole.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }

        if {"$outStr" == ""} {
            continue
        }
        set lOutput [split $outStr |]
        set sAccess [lindex $lOutput 0]
        set sFilter [lindex $lOutput 1]
        regsub -all "\\\\\[" "$sFilter" "\\\\\[" sFilter
        regsub -all "\\\\\]" "$sFilter" "\\\\\]" sFilter

        append sModCmd " state '$sState' add user '$sRoleEmployee' $sAccess filter \"$sFilter\""
        set bFound 1
    }

    if {$bFound == 1} {
        set mqlret [ catch {eval $sModCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-2-1AddEmployeeRole.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }
    }
    
    return -code 0
}

