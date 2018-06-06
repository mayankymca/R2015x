###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
#
# $RCSfile: eServicecommonGetSymbolicStateName.tcl.rca $ $Revision: 1.17 $
#
# @progdoc      eServicecommonGetSymbolicStateName.tcl
#
# @Brief:       Get symbolic name from actual name.
#
# @Description: This program gets symbolic name of the given state on given policy.
#
# @Parameters:  sPolicy - name of the policy state belongs to.
#                         either actual or symbolic name can be passed.
#
#               sState  - Actual name of the state.
#
# @Returns:     0|symbolicName if successful.
#               1|error message if error.
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
#
###############################################################################

###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

tcl;
eval {

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
# end utLoad
    mql verbose off
    set sRegProgName "eServiceSchemaVariableMapping.tcl"
    eval [utLoad "$sRegProgName"]

    # Get env variables.
    set sPolicy [mql get env 1]
    set sState  [mql get env 2]

    # Get actual name from symbolic name
    if {[string first "policy_" "$sPolicy"] == 0} {
        set sTemp $sPolicy
        set sPolicy [eServiceGetCurrentSchemaName policy "$sRegProgName" "$sPolicy"]
        if {$sPolicy == ""} {
            return "1|Error: Property $sTemp does not exists"
        }
    }

    # Get symbolic name from actual name of the state.
    set sCmd {mql print policy "$sPolicy" select property dump |}
    set mqlret [catch {eval $sCmd} outStr]
    set lProp [split $outStr |]
    foreach sProp $lProp {
        set lItem [split $sProp]
        set nIndex [lsearch $lItem "value"]
        set sValue [join [lrange $lItem [expr $nIndex + 1] end]]
        if {"$sValue" == "$sState"} {
            return "0|[lindex $lItem 0]"
        }
    }

    return "1|Error: state '$sState' does not exists in policy '$sPolicy'"
}

