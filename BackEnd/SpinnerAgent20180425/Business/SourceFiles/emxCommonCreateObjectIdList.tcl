#################################################################################
#
# $RCSfile: emxCommonCreateObjectIdList.tcl.rca $ $Revision: 1.7 $
#
# Description: This program is used to put object ids in the file and
#              generated files are used by conversion routines.
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
    # Get Inputs
    # Object Type
    set sObjectType [string trim [mql get env 1]]
    # Objects per file
    set nTransactionLimit [string trim [mql get env 2]]
    # Directory in which files to be created
    set sDir [string trim [mql get env 3]]

    # Load utility files
    eval [utLoad $sRegProgName]

    # Object Admin Name
    set sType [eServiceGetCurrentSchemaName type $sRegProgName "$sObjectType"]

    # Get all the Objects of specified type in the database.
    set sCmd {mql temp query bus "$sType" * * select id dump tcl}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">$outStr"
        return -code 1
    }
    
    # For each Route
    set nCounter1 0
    set nCounter2 1
    set lIds {}
    set sBaseFileName "[join [split $sType] ""]List"
    foreach lObj $outStr {
        incr nCounter1
        lappend lIds [lindex [lindex $lObj 3] 0]
        if {"$nCounter1" == "$nTransactionLimit"} {
            set sFileId [open "$sDir/$sBaseFileName$nCounter2" w 0666]
            puts $sFileId "[join $lIds \n]"
            close $sFileId
            set nCounter1 0
            set lIds {}
            incr nCounter2
        }
    }
    
    if {[llength $lIds] > 0} {
        set sFileId [open "$sDir/$sBaseFileName$nCounter2" w 0666]
        puts $sFileId "[join $lIds \n]"
        close $sFileId
    }
}

