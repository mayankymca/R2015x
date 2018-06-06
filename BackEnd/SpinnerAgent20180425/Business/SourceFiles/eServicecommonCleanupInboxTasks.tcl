###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
#
# $RCSfile: eServicecommonCleanupInboxTasks.tcl.rca $ $Revision: 1.17 $
#
# @progdoc      eServicecommonCleanupInboxTasks.tcl
#
# @Brief:       Cleanup Procedure for Inbox Task
#
# @Description: This program is used to delete all the tasks connected to given route.
#
#
# @Parameters:  sObjectID               - Route Object ID
#
# @Returns:     0 in sucess
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
     set outStr ""
     set sProgName "eServicecommonCleanupInboxTasks.tcl"
     set RegProgName "eServiceSchemaVariableMapping.tcl"
     set mqlret 0

     eval [utLoad $RegProgName]
     eval [utLoad eServicecommonShadowAgent.tcl]
     eval [utLoad eServicecommonDEBUG.tcl ]


     set sRouteObjectID [mql get env 1]

     set sRelRouteTask  [eServiceGetCurrentSchemaName relationship $RegProgName relationship_RouteTask]

     pushShadowAgent

     set sCmd {mql expand bus "$sRouteObjectID" relationship "$sRelRouteTask" dump |}
     set mqlret [catch {eval $sCmd} outStr]
     if {$mqlret != 0} {
         popShadowAgent
         return "1|$outStr"
     }

     set lExpandOutput [split $outStr \n]

     foreach sItem $lExpandOutput {
         set lItem          [split $sItem |]
         set sTypeInboxTask [lindex $lItem 3]
         set sNameInboxTask [lindex $lItem 4]
         set sRevInboxTask  [lindex $lItem 5]

         set sCmd {mql delete bus "$sTypeInboxTask" "$sNameInboxTask" "$sRevInboxTask"}
         set mqlret [catch {eval $sCmd} outStr]
         if {$mqlret != 0} {
             popShadowAgent
             return "1|$outStr"
         }
     }

     popShadowAgent
     return "0"
}

