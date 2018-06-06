###############################################################################
#
# $RCSfile: eServicecommonLookupSchemaName.tcl.rca $ $Revision: 1.17 $
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# Input : List of persons to be deleted.
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

    set sProgName     "eServicecommonLookupSchemaName.tcl"

    set sPropName     [string trim [mql get env 1]]
    set sRegProgName  [string trim [mql get env 2]]
    set outStr        ""
    set mqlret        0

    if {"$sRegProgName" == ""} {
        set sRegProgName "eServiceSchemaVariableMapping.tcl"
    }
    set sCmd {mql print program "$sRegProgName" select property\[$sPropName\].to dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        return "1|Error: - $outStr"
    }

    if {"$outStr" == ""} {
        return "1|Error: - Symbolic name '$sPropName' does not exists"
    } else {
        set sAbsName [join [lrange [split "$outStr"] 1 end]]
        return "0|$sAbsName"
    }
}
##################################################################################

