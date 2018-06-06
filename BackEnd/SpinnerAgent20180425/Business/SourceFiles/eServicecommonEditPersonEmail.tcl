###############################################################################
#
# $RCSfile: eServicecommonEditPersonEmail.tcl.rca $ $Revision: 1.42 $
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

eval  [ utLoad eServicecommonPSUtilities.tcl ]
eval  [ utLoad eServicecommonShadowAgent.tcl ]
eval  [ utLoad eServicecommonDEBUG.tcl]

    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set sProgName     "eServiceprofileEditPersonEmail.tcl"

    eval [utLoad $RegProgName]

    mql verbose off;
    mql quote off;

    set sName		            [string trim [mql get env 1]]
    set sEmailFlag	            [string trim [mql get env 2]]
    set sEmailAdd	            [string trim [mql get env 3]]
    set mqlret  0

    if {"$sEmailFlag" == "" } {
       set sEmailFlag "disable"
    }

    if {$mqlret == 0} {
       if {[mql list person "$sName"] != "$sName"} {
	  set outstr "The user $sName doesnot exist"
	  set mqlret 1
       } else {
       	  set mqlret [pushShadowAgent]
          if {$mqlret == 0} {
             set sCmd {mql modify person "$sName" "$sEmailFlag" email email "$sEmailAdd"}
             set mqlret [catch {eval $sCmd} outstr]
          }
         set mqlret [popShadowAgent]
       }
    }

    if {$mqlret != 0} {
       return "1|Error: - $outstr"
    } else {
       return "0|"
    }
}

