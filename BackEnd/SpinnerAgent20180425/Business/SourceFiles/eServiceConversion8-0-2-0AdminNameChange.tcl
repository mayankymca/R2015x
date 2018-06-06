#################################################################################
#
# $RCSfile: eServiceConversion8-0-2-0AdminNameChange.tcl.rca $ $Revision: 1.5 $
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

    set RegProgName   [mql get env REGISTRATIONOBJECT]
    set mqlerr 0

    # Load Utility function
    eval [utLoad $RegProgName]

    # Get new property
    set sNewProperty    [string trim [mql get env 1]]
    # Get admin type
    set ItemType        [lindex [split $sNewProperty "_"] 0]
    # Get new admin name
    set sNewObjectName  [string trim [emxGetCurrentSchemaName  $ItemType $RegProgName  $sNewProperty ]]
    # Get old property
    set sOldProperty    [string trim [mql get env 2]]
    # Get old name
    set sObjectName     [string trim [emxGetCurrentSchemaName  $ItemType $RegProgName  $sOldProperty ]]
    # if old name doesn't exists return
    if {"$sObjectName" == ""} {
        return -code 0
    }

    # delete new object as old object already exists
    set sCmd {mql delete $ItemType "$sNewObjectName"}
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-2-0AdminNameChange.tcl"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outstr"
        mql trace type INSTALL text ""
        return -code 1
    }

    mql trace type INSTALL text [format ">Renaming    : %-13s%-54s%-15s" $ItemType  $sObjectName  "to $sNewObjectName"]
    mql trace type INSTALL text ""
    # delete property on program $RegProgName
    set sCmd {mql delete property "$sOldProperty" on program "$RegProgName" \
                                                to "$ItemType" "$sObjectName" \
                                                }
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-2-0AdminNameChange.tcl"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outstr"
        mql trace type INSTALL text ""
        return -code 1
    }

    # modify name of the admin object
    set sCmd {mql modify "$ItemType" "$sObjectName" \
                         name "$sNewObjectName" \
                         }
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-2-0AdminNameChange.tcl"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outstr"
        mql trace type INSTALL text ""
        return -code 1
    }

    # add new property to modified admin
    set sCmd {mql add property "$sNewProperty" on program "$RegProgName" \
                                               to "$ItemType" "$sNewObjectName" \
                                               }
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-2-0AdminNameChange.tcl"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outstr"
        mql trace type INSTALL text ""
        return -code 1
    }

    return -code 0
}

