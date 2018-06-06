###############################################################################
#
# $RCSfile: eServicecommonStateNameWrapper.tcl.rca $ $Revision: 1.17 $
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


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################

    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set sProgName     "eServicecommonStateNameWrapper.tcl"

    # Load Utility function
    eval [utLoad $RegProgName]

    mql verbose off

    set sAbsPolicyName              [string trim [mql get env 1]]
    set sSymStateName               [string trim [mql get env 2]]

    set sPrefixPolicy               [string trim [lindex [split $sAbsPolicyName "_"] 0]]
    set sPrefixState                [string trim [lindex [split $sSymStateName "_"] 0]]

    if {[string compare "$sPrefixPolicy" "policy"] == 0} {
       return "1|Error: $sProgName - policy name cannot be a symbolic name"
    }
    if {[string compare "$sPrefixState" "state"] != 0} {
       return "1|Error: $sProgName - state name cannot be an absolute name"
    }

    set sReturnValue  [string trim [eServiceLookupStateName "$sAbsPolicyName" "$sSymStateName"]]

    if {$sReturnValue == ""} {
       return "1|Error: $sProgName - unable to get the state name - Check the policy name and properties on it"
    } else {
       return "0|$sReturnValue|  "
    }
}
##################################################################################

