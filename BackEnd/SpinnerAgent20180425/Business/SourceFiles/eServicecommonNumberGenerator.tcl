###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
#
# $RCSfile: eServicecommonNumberGenerator.tcl.rca $ $Revision: 1.50 $
#
# @progdoc      eServicecommonNumberGenerator.tcl
#
# @Brief:       The number generator procedure
#
# @Description: This program is used to automatically generate number sequences
#               for different admin objects. The procedure also creates the
#               objects for the user. Before creating the object it checks for
#               the uniqueness of the object being created.
#
#
# @Parameters:  ObjectGeneratorName      - Name of AutoName Business Object where
#                                     creation parameters are to be found
#
#               ObjectGeneratorRevision  - Revision of AutoName Business Object
#               CreateAdditional         - A parameter specifying whether to
#        create additional objects. This is a
#        flag having values Null & Additional.
#        A Null means no additional objects
#        to be created.
#        Default is Null.
#               ObjectVault              - Vault in which to create object
#                                          Default is context vault.
#               CustomRevisionLevel      - A parameter specifying custom revision
#                                          level if user specified.
#                                          Default is null value i.e. no custom revision.
#
# @Returns:     A list of lists. Each item in the list holds the following
#  The id, type, name and revision of the object created.
#               If there is an error it returns a list with the first item as null
#  and the sencond item the error message.
#
#
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

     eval  [utLoad eServicecommonObjectGenerator.tcl]

     set lResult ""
     set sProgName "eServicecommonNumberGenerator.tcl"
     set RegProgName   "eServiceSchemaVariableMapping.tcl"
     set mqlret 0

     set sObjGenName      [mql get env 1]
     set sObjGenRev       [mql get env 2]
     set sPolicy          [mql get env 3]
     set sCreateAdditonal [mql get env 4]
     set sVault           [mql get env 5]
     set sCustomRevLevel  [mql get env 6]
     set sObjectType      [mql get env 7]
     set sUniqueNameOnly  [mql get env 8]
     set sUseSuperUser    [mql get env 9]

     #new number generator
     if {$mqlret == 0} {

      set sNewInfo [eServiceNumberGenerator "$sObjGenName" "$sObjGenRev" "$sPolicy" "$sCreateAdditonal" "$sVault" "$sCustomRevLevel" "$sObjectType" "$sUniqueNameOnly" "$sUseSuperUser"]

        set sNewInfoErrorFlag      [lindex $sNewInfo 0]

        if {[string compare $sNewInfoErrorFlag ""] == 0 } {
           set mqlret 1
           set outstr [lindex $sNewInfo 1]
        } else {
           if {"$sUniqueNameOnly" == "Yes"} {
             append lResult "0|[string trim [lindex [lindex $sNewInfo 0] 0]]"
           } else {
              foreach i $sNewInfo {
                 set sNewRuleId     [string trim [lindex "$i" 0]]
                 set sNewRuleType   [string trim [lindex "$i" 1]]
                 set sNewRuleName   [string trim [lindex "$i" 2]]
                 set sNewRuleRev    [string trim [lindex "$i" 3]]
                 set sNewRuleVault  [string trim [lindex "$i" 4]]
                 if {$sNewRuleRev == ""} {
                    set sNewRuleRev " "
                 }
                 append lResult "0|$sNewRuleId | $sNewRuleType | $sNewRuleName | $sNewRuleRev | $sNewRuleVault|"
              }
           }
        }
     }

     if {$mqlret == 0} {
        return $lResult
     } else {
      return "1|Error: $sProgName - $outstr"
     }
}

