###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
#
# $RCSfile: eServicecommonNameGenerator.tcl.rca $ $Revision: 1.46 $
#
# @progdoc      eServicecommonNameGenerator.tcl
#
# @Brief:       The name generator program
#
# @Description: This program is used to automatically generate unique names for objects
#
# @Parameters:  sObjectType               - Type of an object for which to generate name
#
#               sObjectRev                - Revision of the object for which to generate name (Optional)
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

    eval  [utLoad eServicecommonNameGeneratorProc.tcl]

    set lResult ""
    set sProgName "eServicecommonNameGenerator.tcl"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set mqlret 0

    set sObjectType         [mql get env 1]
    set sObjectRev          [mql get env 2]
    set sObjectPolicy       [mql get env 3]
    set sObjectVault        [mql get env 4]
    set sTypeSearchPattern  [mql get env 5]
    set bExpand             [mql get env 6]
    set sVaultSearchPattern [mql get env 7]

    set bUniqueNameOnly     [mql get env 8]
    set bUseSuperUser       [mql get env 9]

    # Default type search pattern to object type
    if {"$sTypeSearchPattern" == ""} {
        set sTypeSearchPattern "$sObjectType"
    }

    # Default expand to TRUE
    if {"$bExpand" == ""} {
        set bExpand "TRUE"
    }

    # Default Super User to Yes
    if {"$bUseSuperUser" == ""} {
        set bUseSuperUser "TRUE"
    }

    # Default Unique name to No
    if {"$bUniqueNameOnly" == ""} {
        set bUniqueNameOnly "FALSE"
    }
    
    # Default vault search pattern to all
    if {"$sVaultSearchPattern" == ""} {
        set sVaultSearchPattern "*"
    }

    set mqlret [catch {mql start thread} outstr]
    if {$mqlret != 0} {
        return "1|mql start thread failed: $outstr"
    }

    set mqlret [catch {mql start transaction} outstr]
    if {$mqlret != 0} {
        catch {mql kill thread} outstr
        return "1|mql start transaction failed: $outstr"
    }

    set lResult [eServiceNameGeneratorProc "$sObjectType" \
                                            "$sObjectRev" \
                                            "$sObjectPolicy" \
                                            "$sObjectVault" \
                                            "$sTypeSearchPattern" \
                                            "$bExpand" \
                                            "$sVaultSearchPattern" \
                                            "$bUniqueNameOnly" \
                                            "$bUseSuperUser"]

    set mqlret [catch {mql commit transaction} outstr]
    if {$mqlret != 0} {
        catch {mql kill thread} outstr
        return "1|mql commit transaction failed: $outstr"
    }

    set mqlret [catch {mql kill thread} outstr]
    if {$mqlret != 0} {
        return "1|mql kill thread failed: $outstr"
    }

    if {[llength $lResult] != 5 && "[string toupper [string trim "$bUniqueNameOnly"]]" != "TRUE"} {
        return "1|$lResult"
    } else {
        set sResult "0|"
        append sResult [lindex $lResult 0]
        return "$sResult"
    }

}


