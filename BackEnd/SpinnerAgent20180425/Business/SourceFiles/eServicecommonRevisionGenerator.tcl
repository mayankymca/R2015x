###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
#
# $RCSfile: eServicecommonRevisionGenerator.tcl.rca $ $Revision: 1.47 $
#
# @progdoc      eServicecommonRevisionGenerator.tcl
#
# @Brief:       The revision generator procedure
#
# @Description: This program is used to automatically generate unique revisions for objects
#
#
# @Parameters:  sObjectType               - Type of an object for which to generate revision
#
#               sObjectName               - Name of the object for which to generate revision
#
#               sObjectPolicy             - Policy supported by the type specified (Optional)
#
# @Returns:     A pipe seperated list of 0, ObjectId, ObjectType, ObjectName, ObjectRev, ObjectVault in sucess
#               A pipe seperated list of 1,Error message in failure
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
    eval  [utLoad eServiceSchemaVariableMapping.tcl]
    eval  [utLoad eServicecommonDEBUG.tcl ]
    eval  [utLoad eServicecommonRevisionGeneratorProc.tcl]

    set lResult ""
    set sProgName "eServicecommonRevisionGenerator.tcl"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set mqlret 0

    set sObjectType         [mql get env 1]
    set sObjectName         [mql get env 2]
    set sObjectPolicy       [mql get env 3]
    set sObjectVault        [mql get env 4]
    set sTypeSearchPattern  [mql get env 5]
    set bExpand             [mql get env 6]
    set sVaultSearchPattern [mql get env 7]

    # Default type search pattern to all
    if {"$sTypeSearchPattern" == ""} {
        set sTypeSearchPattern "*"
    }

    # Default expand to TRUE
    if {"$bExpand" == ""} {
        set bExpand "TRUE"
    }

    # Default vault search pattern to all
    if {"$sVaultSearchPattern" == ""} {
        set sVaultSearchPattern "*"
    }

     set lResult [eServiceRevisionGeneratorProc "$sObjectType" \
                                                "$sObjectName" \
                                                "$sObjectPolicy" \
                                                "$sObjectVault" \
                                                "$sTypeSearchPattern" \
                                                "$bExpand" \
                                                "$sVaultSearchPattern"]

  
    if {[llength $lResult] != 5} {
        return "1|$lResult"
    } else {
        set sResult "0|"
        append sResult [lindex $lResult 0]
        return "$sResult"
    }

}

