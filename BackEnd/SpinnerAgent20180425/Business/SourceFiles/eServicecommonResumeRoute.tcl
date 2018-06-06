###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonResumeRoute.tcl.rca $ $Revision: 1.52 $
#
# @libdoc       eServicecommonResumeRoute.tcl
#
# @Brief:       Resume Route.
#
# @Description:
#
# @libdoc       Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# The following sample code is provided for your reference purposes in
# connection with your use of the Matrix System (TM) software product
# which you have licensed from MatrixOne, Inc. ("MatrixOne").
# The sample code is provided to you without any warranty of any kind
# whatsoever and you agree to be responsible for the use and/or incorporation
# of the sample code into any software product you develop. You agree to fully
# and completely indemnify and hold MatrixOne harmless from any and all loss,
# claim, liability or damages with respect to your use of the Sample Code.
#
# Subject to the foregoing, you are permitted to copy, modify, and distribute
# the sample code for any purpose and without fee, provided that (i) a
# copyright notice in the in the form of "Copyright 1995 - 1998 MatrixOne Inc.,
# Two Executive Drive, Chelmsford, MA  01824. All Rights Reserved" appears
# in all copies, (ii) both the copyright notice and this permission notice
# appear in supporting documentation and (iii) you are a valid licensee of
# the Matrix System software.
#
###############################################################################
tcl;
eval {

  # Load the utilities and other libraries.
  eval  [ mql print prog eServicecommonUtil.tcl select code dump ]

  eval  [ utLoad eServicecommonDEBUG.tcl ]
  eval  [ utLoad eServiceSchemaVariableMapping.tcl]
  eval  [ utLoad eServicecommonInitiateRoute.tcl]
  eval  [ utLoad eServicecommonTranslation.tcl]
  eval  [ utLoad eServicecommonShadowAgent.tcl]

  set RegProgName   "eServiceSchemaVariableMapping.tcl"
  set progname      [mql get env 0]

  # Get RPE variables
  set sRouteId [string trim [mql get env 1]]

  #
  # Error handling variables
  #
  set outStr ""
  set mqlret 0
  set sCmd ""
  set bPersonFound 0
  #
  # Get absolute names from symbolic names
  #
  set sRelRouteTask        [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_RouteTask"]
  set sRelRouteNode        [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_RouteNode"]
  set sAttCurrentRouteNode [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_CurrentRouteNode"]
  set sAttRouteStatus      [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteStatus"]
  set sAttApprovalStatus   [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ApprovalStatus"]
  set sStateAssigned       [eServiceGetCurrentSchemaName state $RegProgName policy_InboxTask state_Assigned]
  set sAttRouteNodeID   [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteNodeID"]
  #
  # start transaction
  #
  set sCmd {utCheckStartTransaction}
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return "1|$outStr"
  }
  set bTranAlreadyStarted "$outStr"

  #
  # Set Route Status attribute to "Started"
  #
  set sCmd {mql modify bus "$sRouteId" \
                           "$sAttRouteStatus" "Started" \
                           }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      utCheckAbortTransaction $bTranAlreadyStarted
      return "1|$outStr"
  }

  #
  # Modify Approval Status attribute for all Route Node connections whose value is "Reject" to "Ignore".
  #
  set sCmd {mql expand bus "$sRouteId" \
                       from relationship "$sRelRouteNode" \
                       select rel \
                       id \
                       attribute\[$sAttApprovalStatus\] \
                       dump | \
                       }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      utCheckAbortTransaction $bTranAlreadyStarted
      return "1|$outStr"
  }
  set lRouteNode [split $outStr \n]
  foreach sRouteNode $lRouteNode {
      set sConnectionId [lindex [split "$sRouteNode" |] 6]
      set sApprovalStatus [lindex [split "$sRouteNode" |] 7]
      if {"$sApprovalStatus" == "Reject"} {
          set sCmd {mql modify connection "$sConnectionId" "$sAttApprovalStatus" "Ignore"}
          set mqlret [ catch {eval  $sCmd} outStr ]
          if {$mqlret != 0} {
              mql notice "$outStr"
              utCheckAbortTransaction $bTranAlreadyStarted
              return "1|$outStr"
          }
      }
  }

  set sCmd {mql print bus "$sRouteId" \
                      select \
                      id \
                      owner \
                      type \
                      name \
                      revision \
                      attribute\[$sAttCurrentRouteNode\] \
                      dump | \
                      }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      utCheckAbortTransaction $bTranAlreadyStarted
      return "1|$outStr"
  }
  set lPrintOutput      [split $outStr |]

  set sRouteId          [lindex $lPrintOutput 0]
  set sOwner            [lindex $lPrintOutput 1]
  set sRouteType        [lindex $lPrintOutput 2]
  set sRouteName        [lindex $lPrintOutput 3]
  set sRouteRev         [lindex $lPrintOutput 4]
  set sCurrentRouteNode [lindex $lPrintOutput 5]



  
  
    set sCmd {mql expand bus "$sRouteId" \
                       from relationship "$sRelRouteNode" \
                       select rel id \
                       attribute\[$sAttRouteNodeID\] \
                       dump | \
                       }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      utCheckAbortTransaction $bTranAlreadyStarted
      return "1|$outStr"
  }
   set bFound 1
   set lRouteInfo [split $outStr \n]
   foreach sRouteInfo $lRouteInfo {
       set lRouteInfoAtt [split $sRouteInfo |]
       set sRouteNodeID [lindex $lRouteInfoAtt 7]
       set ssCmd {mql expand bus "$sRouteId" \
                      to relationship "$sRelRouteTask" \
                      select bus id \
                      where (attribute\[$sAttRouteNodeID\]==$sRouteNodeID) \
                      dump | \
                      }
      set mqlret [ catch {eval  $ssCmd} soutStr ]
        if {$mqlret != 0} {
      mql notice "$soutStr"
      utCheckAbortTransaction $bTranAlreadyStarted
      return "1|$soutStr"
  }
   if {$soutStr == ""} {
    set bFound 0
	}
  }
 
  #
  # If None of the Inbox Task objects are returned
  #
  if {$bFound == 0} {

      #
      # Expand Route Node relationship and get all Relationship Ids whose
      # Route Sequence == Current Route Node value
      #
      pushShadowAgent
      set mqlret [catch {eval eServicecommonInitiateRoute {$sRouteType} {$sRouteName} {$sRouteRev} {$sCurrentRouteNode} {0} } outStr]
      popShadowAgent
      if {$mqlret != 0} {
          utCheckAbortTransaction $bTranAlreadyStarted
          return "1|$outStr"
      }
        if {$outStr == 0} {
           set sCmd {mql override bus "$sRouteId"}
           set mqlret [ catch {eval  $sCmd} outStr ]
           if {$mqlret != 0} {
                mql notice "$outStr"
                utCheckAbortTransaction $bTranAlreadyStarted
               return "1|$outStr"
            }
       }
  }

     set sCmd {mql expand bus "$sRouteId" \
                        to relationship "$sRelRouteTask" \
                        select bus id \
                                   current \
                        dump | \
                        }
   set mqlret [ catch {eval  $sCmd} outStr ]
   if {$mqlret != 0} {
       mql notice "$outStr"
       utCheckAbortTransaction $bTranAlreadyStarted
       return "1|$outStr"
   }
   set lInboxTask [split $outStr \n]
   set bFound1 0
   foreach sInboxTask $lInboxTask {
       set lInboxTaskAtt [split $sInboxTask |]
       set sState [lindex $lInboxTaskAtt 7]
      if {"$sState" == "$sStateAssigned"} {
           set bFound1 1
       }
   }
  if {$bFound1 == 0} {
             set sCmd {mql promote bus "$sRouteId"}
            set mqlret [ catch {eval  $sCmd} outStr ]
            if {$mqlret != 0} {
               mql notice "$outStr"
              utCheckAbortTransaction $bTranAlreadyStarted
                return "1|$outStr"
            }

          # # #
          # # # Set Route Status attribute to "Finished"
          # # #
            set sCmd {mql modify bus "$sRouteId" \
                                  "$sAttRouteStatus" "Finished" \
                                  }
           set mqlret [ catch {eval  $sCmd} outStr ]
            if {$mqlret != 0} {
               mql notice "$outStr"
                utCheckAbortTransaction $bTranAlreadyStarted
               return "1|$outStr"
            }

          # # # # Send notification to subscribers
            set sCmd {mql execute program emxSubscriptionManager -method publishEvent "Route Completed" "$sRouteId" -construct "$sRouteId"}
            set mqlret [ catch {eval  $sCmd} outStr ]
            if {$mqlret != 0} {
               mql notice "$outStr"
                return "1|$outStr"
            }

            set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                  "$sOwner" \
                                  "emxFramework.ProgramObject.eServicecommonResumeRoute.Subject" 0 \
                                  "emxFramework.ProgramObject.eServicecommonResumeRoute.Message" 3 \
                                                "RType" "$sRouteType" \
                                                "RName" "$sRouteName" \
                                                "RRev" "$sRouteRev" \
                                  "" \
                                  "" \
                                  }
            set mqlret [ catch {eval  $sCmd} outStr ]
            if {$mqlret != 0} {
               mql notice "$outStr"
               utCheckAbortTransaction $bTranAlreadyStarted
               return "1|$outStr"
            }
  
	}
  
  #
  # commit transaction
  #
  set sCmd {utCheckCommitTransaction $bTranAlreadyStarted}
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return "1|$outStr"
  }

  return "0"
}
# end eServicecommonResumeRoute.tcl

# End of Module

