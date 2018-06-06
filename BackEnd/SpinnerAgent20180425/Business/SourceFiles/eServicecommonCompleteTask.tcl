##############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonCompleteTask.tcl.rca $ $Revision: 1.68 $
#
# @libdoc       eServicecommonCompleteTask
#
# @Library:     Interface for required connection checks for triggers
#
# @Brief:       Initiates Route.
#
# @Description: see procedure description
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

###############################################################################
#
# Define Global Variables
#
###############################################################################


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [ utLoad eServicecommonDEBUG.tcl ]
eval  [ utLoad eServiceSchemaVariableMapping.tcl]
eval  [ utLoad eServicecommonInitiateRoute.tcl]
eval  [ utLoad eServicecommonTranslation.tcl ]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonCompleteTask.tcl
#
# @Brief:       Check that at least one object is connected via the
#               indicated relatonship
#
# @Description: Check that at least one object is connected via the
#               indicated relatonship
#
# @Parameters:  direction    -- direction to traverse (to/from)
#               relationList -- relationships to traverse
#                               eg : $relationship_rel1,$relationship_rel2,...
#               typeList     -- types to traverse
#                               eg : $type_type1,$type_type2,..
#
# @Returns:     0 for no connection, 1 otherwise
#
# @Example:     Input to mxTrigManager:
#                 {{ eServicecommonCompleteTask  {to/from/both} {$relationship_rel1,$relationship_rel2,..} {$type_type1,$type_type2,...} }}
#
# @procdoc
#******************************************************************************

proc eServicecommonCompleteTask {sType sName sRev} {

  set RegProgName   "eServiceSchemaVariableMapping.tcl"
  set progname      "eServicecommonCompleteTask"

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
  set sRelRouteTask                  [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_RouteTask"]
  set sRelObjectRoute                [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_ObjectRoute"]
  set sRelRouteNode                  [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_RouteNode"]

  set sAttActualCompletionDate       [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ActualCompletionDate"]
  set sAttComments                   [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_Comments"]
  set sAttApprovalStatus             [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ApprovalStatus"]
  set sAttRouteNodeID                [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteNodeID"]
  set sAttCurrentRouteNode           [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_CurrentRouteNode"]
  set sAttRouteStatus                [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteStatus"]
  set sAttRouteAction                [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteCompletionAction"]
  set sAttParallelNodeProcessionRule [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ParallelNodeProcessionRule"]
  set sAttRouteBaseState             [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteBaseState"]
  set sAttRouteBasePolicy            [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteBasePolicy"]
  set sStateInProcess                [eServiceGetCurrentSchemaName state $RegProgName policy_Route state_InProcess]
  set sStateComplete                 [eServiceGetCurrentSchemaName state $RegProgName policy_Route state_Complete]

  set sTypeProjectMember             [eServiceGetCurrentSchemaName type $RegProgName "type_ProjectMember"]

  set sStateAssigned                 [eServiceGetCurrentSchemaName state $RegProgName policy_InboxTask state_Assigned]
  set sStateReview                   [eServiceGetCurrentSchemaName state $RegProgName policy_InboxTask state_Review]
  set sRouteDelegationGrantor        [eServiceGetCurrentSchemaName person $RegProgName "person_RouteDelegationGrantor"]

  set sDate [mql get env TIMESTAMP]

  #
  # Get setting from emxSystem.properties file to 
  # check if Ad Hoc routes should be considered or not
  #
  set bConsiderAdhocRoutes [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.AdHocRoutesBlockLifecycle" 0 \
                                "" \
                                "emxSystem" \
                           ]
  #
  # set default to false if property doesn't exists
  #
  set bConsiderAdhocRoutes [string toupper "$bConsiderAdhocRoutes"]
  if {"$bConsiderAdhocRoutes" != "TRUE"} {
      set bConsiderAdhocRoutes "FALSE"
  }


  #
  # Set Actual Completion Date attribute in Inbox Task
  #
  set sCmd {mql modify bus "$sType" "$sName" "$sRev" \
                           "$sAttActualCompletionDate" "$sDate" \
                           }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }

  #
  # Copy 'Approval Status', 'Actual Completion Date', 'Comments' to Route Node relationship
  #
  set sCmd {mql print bus "$sType" "$sName" "$sRev" \
                      select attribute\[$sAttRouteNodeID\] \
                             attribute\[$sAttApprovalStatus\] \
                             attribute\[$sAttActualCompletionDate\] \
                             attribute\[$sAttComments\] \
                             from\[$sRelRouteTask\].businessobject.from\[$sRelRouteNode\].id \
                             from\[$sRelRouteTask\].businessobject.from\[$sRelRouteNode\].attribute\[$sAttRouteNodeID\] \
                      dump | \
                      }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }
  set lPrintOutput          [split $outStr |]
  set sRouteNodeIDOnIB      [lindex $lPrintOutput 0]
  set sApprovalStatus       [lindex $lPrintOutput 1]
  set sActualCompletionDate [lindex $lPrintOutput 2]
  set sComments             [lindex $lPrintOutput 3]
  set lRouteNodeInfo        [lrange $lPrintOutput 4 end]
  set lRouteNodeId          [lrange $lRouteNodeInfo 0 [expr [llength $lRouteNodeInfo] / 2 - 1]]
  set lRouteNodeIdAttr      [lrange $lRouteNodeInfo [expr [llength $lRouteNodeInfo] / 2] end]

  #
  # Get matching relationship id
  #
  set bRouteNodeIdFound 0
  foreach sRouteNodeId $lRouteNodeId sRouteNodeIdAttr $lRouteNodeIdAttr {
      if {"$sRouteNodeIDOnIB" == "$sRouteNodeIdAttr"} {
          set bRouteNodeIdFound 1
          break
      }
  }

  #
  # If Route Node Id not found then
  # Error out
  #
  if {$bRouteNodeIdFound == 0} {
      set outStr [mql execute program emxMailUtil -method getMessage \
                      "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidRouteNodeId" 3 \
                          "type" "$sType" \
                          "name" "$sName" \
                          "rev" "$sRev" \
                      "" \
                 ]
      mql notice "$outStr"
      return 1
  }
 
  regsub -all "'" "$sComments" "\\'" sComments

  set sCmd {mql modify connection "$sRouteNodeId" \
                       "$sAttApprovalStatus" "$sApprovalStatus" \
                       "$sAttActualCompletionDate" "$sActualCompletionDate" \
                       "$sAttComments" "$sComments" \
                       }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }

  #
  # Get information on attached route
  #
  set sCmd {mql print connection "$sRouteNodeId" \
                      select \
                      from.id \
                      from.owner \
                      from.type \
                      from.name \
                      from.revision \
                      from.attribute\[$sAttCurrentRouteNode\] \
                      from.attribute\[$sAttRouteStatus\] \
                      from.attribute\[$sAttRouteAction\] \
                      to.name \
                      attribute\[$sAttParallelNodeProcessionRule\] \
                      from.relationship\[$sRelObjectRoute\].from.id \
                      from.relationship\[$sRelObjectRoute\].from.current.satisfied \
                      from.relationship\[$sRelObjectRoute\].from.type \
                      from.relationship\[$sRelObjectRoute\].from.name \
                      from.relationship\[$sRelObjectRoute\].from.revision \
                      from.relationship\[$sRelObjectRoute\].from.current \
                      from.relationship\[$sRelObjectRoute\].from.policy \
                      dump | \
                      }

  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }
  set lPrintOutput      [split $outStr |]
  set sRouteId          [lindex $lPrintOutput 0]
  set sOwner            [lindex $lPrintOutput 1]
  set sRouteType        [lindex $lPrintOutput 2]
  set sRouteName        [lindex $lPrintOutput 3]
  set sRouteRev         [lindex $lPrintOutput 4]
  set sCurrentRouteNode [lindex $lPrintOutput 5]
  set sRouteStatus      [lindex $lPrintOutput 6]
  set sRouteAction      [lindex $lPrintOutput 7]
  set sPerson           [lindex $lPrintOutput 8]
  set sProcessionRule   [lindex $lPrintOutput 9]
  set sObjId            [lindex $lPrintOutput 10]
  set sObjSatisfied     [lindex $lPrintOutput 11]
  set sObjType          [lindex $lPrintOutput 12]
  set sObjName          [lindex $lPrintOutput 13]
  set sObjRev           [lindex $lPrintOutput 14]
  set sObjCurrentState  [lindex $lPrintOutput 15]
  set sObjPolicy        [lindex $lPrintOutput 16]

  #
  # If Approval Status == Reject
  #
  if {$sApprovalStatus == "Reject"} {

      set sInboxObjectId [mql get env OBJECTID]
      
      #
      # Set Route Status attribute to Stopped
      #
      set sCmd {mql modify bus "$sRouteId" \
                           "$sAttRouteStatus" "Stopped" \
                           }
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          mql notice "$outStr"
          return $mqlret
      }

      #
      # Send Iconmail to Route owner
      #
      set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sOwner" \
                            "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectReject" 0 \
                            "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageReject" 7 \
                                          "name" "$sPerson" \
                                          "IBType" "$sType" \
                                          "IBName" "$sName" \
                                          "IBRev" "$sRev" \
                                          "RType" "$sRouteType" \
                                          "RName" "$sRouteName" \
                                          "RRev" "$sRouteRev" \
                            "$sInboxObjectId" \
                            "" \
                            }
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          mql notice "$outStr"
          return $mqlret
      }
  } else {
      #
      # Expand route and get the current state of all Inbox Tasks associated with it
      #
      set sCmd {mql expand bus "$sRouteId" \
                           to relationship "$sRelRouteTask" \
                           select bus id \
                           current \
                           attribute\[$sAttRouteNodeID\] \
                           owner \
                           dump | \
                           }
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          mql notice "$outStr"
          return $mqlret
      }
      set lInboxTask [split $outStr \n]
      set bFound 0
      foreach sInboxTask $lInboxTask {
          set lInboxTaskAtt [split $sInboxTask |]
          set sState [lindex $lInboxTaskAtt 7]
          if {"$sState" == "$sStateAssigned" || "$sState" == "$sStateReview"} {
              # New Feature: AEF9500; Not all tasks have to be signed to proceed
              if {[string tolower $sProcessionRule] == "any"} {
                  # Delete Route Node Connection
                  set sRouteNodeConnectionId [lindex $lInboxTaskAtt 8]
                  set bRouteNodeIdFound 0
                  foreach sRouteNodeId $lRouteNodeId sRouteNodeIdAttr $lRouteNodeIdAttr {
                      if {"$sRouteNodeConnectionId" == "$sRouteNodeIdAttr"} {
                          set bRouteNodeIdFound 1
                          break
                      }
                  }

                  #
                  # If Route Node Id not found then
                  # Error out
                  #
                  if {$bRouteNodeIdFound == 0} {
                      set sCurrTaskType [lindex $lInboxTaskAtt 3]
                      set sCurrTaskName [lindex $lInboxTaskAtt 4]
                      set sCurrTaskRev [lindex $lInboxTaskAtt 5]
                      set outStr [mql execute program emxMailUtil -method getMessage \
                                      "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidRouteNodeId" 3 \
                                          "type" "$sCurrTaskType" \
                                          "name" "$sCurrTaskName" \
                                          "rev" "$sCurrTaskRev" \
                                      "" \
                                 ]
                      mql notice "$outStr"
                      return 1
                  }
  
                  set sCmd {mql disconnect connection "$sRouteNodeId"}
                  set mqlret [ catch {eval $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return $mqlret
                  }
                  # Delete unsigned/non-completed tasks
                  set sTaskId [lindex $lInboxTaskAtt 6]
                  set sCmd {mql delete bus "$sTaskId"}
                  set mqlret [ catch {eval $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return $mqlret
                  }
                  # Send mail to owner of task about deletion
                  set sTaskOwner [lindex $lInboxTaskAtt 9]
                  set sDelTaskType [lindex $lInboxTaskAtt 3]
                  set sDelTaskName [lindex $lInboxTaskAtt 4]
                  set sDelTaskRev [lindex $lInboxTaskAtt 5]
                  set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                "$sTaskOwner" \
                                "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectDeleteTask" 0 \
                                "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageDeletionTask" 6 \
                                              "IBType" "$sDelTaskType" \
                                              "IBName" "$sDelTaskName" \
                                              "IBRev" "$sDelTaskRev" \
                                              "IBType2" "$sType" \
                                              "IBName2" "$sName" \
                                              "IBRev2" "$sRev" \
                                "" \
                                "" \
                                }
                  set mqlret [ catch {eval $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return $mqlret
                  }
              } else {
                  set bFound 1
                  break
              }
          }
      }

      #
      # If None of the Inbox Task objects are returned and Route Status == Started
      #
      if {$bFound == 0 && $sRouteStatus == "Started"} {
          #
          # Increment Current Route Node attribute on attached Route object
          #
          incr sCurrentRouteNode
          set sCmd {mql modify bus "$sRouteId" \
                               "$sAttCurrentRouteNode" "$sCurrentRouteNode" \
                               }
          set mqlret [ catch {eval  $sCmd} outStr ]
          if {$mqlret != 0} {
              mql notice "$outStr"
              return $mqlret
          }
          #
          # Expand Route Node relationship and get all Relationship Ids whose
          # Route Sequence == Current Route Node value
          #
          set mqlret [catch {eval eServicecommonInitiateRoute {$sRouteType} {$sRouteName} {$sRouteRev} {$sCurrentRouteNode} {0} } outStr]
          if {$mqlret != 0} {
              mql notice "$outStr"
              return $mqlret
          }

          #
          # Return 0 if no more tasks for route
          #
          if {$outStr == 0} {
              set sCmd {mql override bus "$sRouteId"}
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return $mqlret
              }
              set sCmd {mql promote bus "$sRouteId"}
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return $mqlret
              }
              set sCmd {mql modify bus "$sRouteId" "$sAttRouteStatus" "Finished"}
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return $mqlret
              }

              # Send notification to subscribers
              set sCmd {mql execute program emxSubscriptionManager -method publishEvent "Route Completed" "$sRouteId" -construct "$sRouteId"}
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return $mqlret
              }

              # AEF9500: Remove temporary access granted to delegators

              #
              # Expand Object Route relationship to get routed items
              #
              set sCmd {mql expand bus $sRouteId \
                        to relationship "$sRelObjectRoute" \
                        terse dump | \
                       }

              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }

              # AEF9500: Route now support multiple objects being routed with the same route
              set lRouteObjects ""
              set lExpandOutput [split $outStr \n]
              foreach sItem $lExpandOutput {
                  set lItem [split $sItem |]
                  lappend lRouteObjects [lindex $lItem 3]
              }

              foreach sObjectId [concat $lRouteObjects [list $sRouteId]] {
                  set sCmd {mql print bus $sObjectId select grantor\[$sRouteDelegationGrantor\] dump}
                  set mqlret [ catch {eval  $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return -code $mqlret $outStr
                  }
                  if {$outStr != "FALSE"} {
                      set sCmd {mql modify bus $sObjectId revoke grantor $sRouteDelegationGrantor}
                      set mqlret [ catch {eval  $sCmd} outStr ]
                      if {$mqlret != 0} {
                          mql notice "$outStr"
                          return -code $mqlret $outStr
                      }
                  }
              }

              if {"$sRouteAction" == "Promote Connected Object"} {

                  # Get all the Route content information
                  set sCmd {mql expand bus "$sRouteId" to relationship "$sRelObjectRoute" \
                                select bus \
                                       id \
                                       current.satisfied \
                                       current \
                                       policy \
                                       state \
                                select relationship \
                                       attribute\[$sAttRouteBaseState\].value \
                                       attribute\[$sAttRouteBasePolicy\].value \
                                dump | \
                                }
                  set mqlret [ catch {eval  $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return $mqlret
                  }

                  set lContentInfo [split $outStr \n]

                  set sObjectsNotSatisfied ""
                  set sRoutesInProcess ""
                  set sPromotedObjects ""
                  foreach sEachObject $lContentInfo {

                      set lEachObject [split $sEachObject |]
                      set sObjType [lindex $lEachObject 3]
                      set sObjName [lindex $lEachObject 4]
                      set sObjRev [lindex $lEachObject 5]
                      set sObjId [lindex $lEachObject 6]
                      set sIsObjSatisfied [lindex $lEachObject 7]
                      set sObjCurrent [lindex $lEachObject 8]
                      set sObjPolicy [lindex $lEachObject 9]
                      set lObjState [lrange $lEachObject 10 [expr [llength $lEachObject] - 3]]
                      set sObjBaseState [lindex $lEachObject [expr [llength $lEachObject] - 2]]
                      set sObjBasePolicy [lindex $lEachObject [expr [llength $lEachObject] - 1]]
                      set bPromoteObject 1

                      # Check if object state and policy maches with base state and policy
                      if {"$sObjBaseState" != "Ad Hoc"} {
                          # Get names from properties
                          set sTempStore "$sObjBaseState"
                          set sObjBaseState [eServiceGetCurrentSchemaName state $RegProgName "$sObjBasePolicy" "$sObjBaseState"]
                          if {"$sObjBaseState" == ""} {
                              set outStr [mql execute program emxMailUtil -method getMessage \
                                              "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidPolicy" 5 \
                                                "State" "$sTempStore" \
                                                "Type" "$sRouteType" \
                                                "OType" "$sObjType" \
                                                "OName" "$sObjName" \
                                                "ORev" "$sObjRev" \
                                              "" \
                                              ]
                             mql notice "$outStr"
                             return 1
                          }
                          set sTempStore "$sObjBasePolicy"
                          set sObjBasePolicy [eServiceGetCurrentSchemaName policy $RegProgName "$sObjBasePolicy"]
                          if {"$sObjBasePolicy" == ""} {
                              set outStr [mql execute program emxMailUtil -method getMessage \
                                              "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidState" 5 \
                                                "Policy" "$sTempStore" \
                                                "Type" "$sRouteType" \
                                                "OType" "$sObjType" \
                                                "OName" "$sObjName" \
                                                "ORev" "$sObjRev" \
                                              "" \
                                              ]
                             mql notice "$outStr"
                             return 1
                          }
                      } else {
                          if {"$bConsiderAdhocRoutes" == "FALSE"} {
                              continue
                          }
                      }
                      if {"$sObjBaseState" != "Ad Hoc" && ("$sObjBaseState" != "$sObjCurrent" || "$sObjBasePolicy" != "$sObjPolicy")} {
                          continue
                      }

                      # Check if object is in the last state
                      if {[lsearch $lObjState "$sObjCurrent"] == [expr [llength $lObjState] - 1]} {
                          continue
                      }

                      set sCmd {mql expand bus "$sObjId" from relationship "$sRelObjectRoute" \
                                    select bus \
                                           current \
                                    select relationship \
                                           attribute\[$sAttRouteBaseState\].value \
                                           attribute\[$sAttRouteBasePolicy\].value \
                                    dump | \
                                    }
                      set mqlret [ catch {eval  $sCmd} outStr ]
                      if {$mqlret != 0} {
                          mql notice "$outStr"
                          return $mqlret
                      }
                      set lRouteInfo [split $outStr \n]

                      # Check for each object if there is any route which is not complete
                      set sInCompleteRoutes ""
                      foreach sEachRoute $lRouteInfo {
                          set lEachRoute [split $sEachRoute |]
                          set sObjRouteBaseState [lindex $lEachRoute 7]
                          set sObjRouteBasePolicy [lindex $lEachRoute 8]
                          set sObjRouteType [lindex $lEachRoute 3]
                          set sObjRouteName [lindex $lEachRoute 4]
                          set sObjRouteRev [lindex $lEachRoute 5]
                          set sObjRouteCurrent [lindex $lEachRoute 6]

                          if {"$sObjRouteBaseState" == ""} {
                              set sObjRouteBaseState "Ad Hoc"
                          }

                          if {"$sObjRouteBaseState" != "Ad Hoc"} {
                              # Get names from properties
                              set sTempStore "$sObjRouteBaseState"
                              set sObjRouteBaseState [eServiceGetCurrentSchemaName state $RegProgName "$sObjRouteBasePolicy" "$sObjRouteBaseState"]
                              if {"$sObjRouteBasePolicy" == ""} {
                                  set outStr [mql execute program emxMailUtil -method getMessage \
                                                  "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidPolicy" 5 \
                                                    "State" "$sTempStore" \
                                                    "Type" "$sRouteType" \
                                                    "OType" "$sObjType" \
                                                    "OName" "$sObjName" \
                                                    "ORev" "$sObjRev" \
                                                  "" \
                                                  ]
                                 mql notice "$outStr"
                                 return 1
                              }
                              set sTempStore "$sObjRouteBasePolicy"
                              set sObjRouteBasePolicy [eServiceGetCurrentSchemaName policy $RegProgName "$sObjRouteBasePolicy"]
                              if {"$sObjRouteBasePolicy" == ""} {
                                  set outStr [mql execute program emxMailUtil -method getMessage \
                                                  "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidState" 5 \
                                                    "Policy" "$sTempStore" \
                                                    "Type" "$sRouteType" \
                                                    "OType" "$sObjType" \
                                                    "OName" "$sObjName" \
                                                    "ORev" "$sObjRev" \
                                                  "" \
                                                  ]
                                 mql notice "$outStr"
                                 return 1
                              }
                          }
                          # If Route Base State is Ad Hoc or Route Base State and Policy are
                          # same as object state and policy
                          if {("$bConsiderAdhocRoutes" == "TRUE" && "$sObjRouteBaseState" == "Ad Hoc") || ("$sObjRouteBaseState" == "$sObjCurrent" && "$sObjRouteBasePolicy" == "$sObjPolicy")} {
                              # Set flag if Route still in work
                              if {"$sObjRouteCurrent" != "$sStateComplete"} {
                                  append sInCompleteRoutes "$sObjRouteType $sObjRouteName $sObjRouteRev,"
                                  set bPromoteObject 0
                              }
                          }
                      }
                      if {"$sInCompleteRoutes" != ""} {
                          append sRoutesInProcess "$sObjType $sObjName $sObjRev : $sInCompleteRoutes\n"
                      }

                      # Check if all the signatures are approved
                      if {"$sIsObjSatisfied" == "FALSE"} {
                          append sObjectsNotSatisfied "$sObjType $sObjName $sObjRev\n"
                          set bPromoteObject 0
                      }

                      if {$bPromoteObject == 1} {
                          set sCmd {mql promote bus $sObjId}
                          set mqlret [ catch {eval  $sCmd} outStr ]
                          if {$mqlret != 0} {
                              mql notice "$outStr"
                              return $mqlret
                          }

                          append sPromotedObjects "$sObjType $sObjName $sObjRev\n"
                      }
                  }

                  if {"$sObjectsNotSatisfied" == "" && "$sRoutesInProcess" == ""} {
                      if {"$sPromotedObjects" == ""} {
                           set sPromotedObjects [mql execute program emxMailUtil -method getMessage \
                                                     "emxFramework.ProgramObject.eServicecommonCompleteTask.None" 0 \
                                                     "" \
                                                ]
                      }
                      set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                            "$sOwner" \
                                            "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectComplete" 0 \
                                            "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageComplete" 4 \
                                                          "RType" "$sRouteType" \
                                                          "RName" "$sRouteName" \
                                                          "RRev" "$sRouteRev" \
                                                          "PromotedObj" "$sPromotedObjects" \
                                            "" \
                                            "" \
                                            }
                      set mqlret [ catch {eval  $sCmd} outStr ]
                      if {$mqlret != 0} {
                          mql notice "$outStr"
                          return $mqlret
                      }
                  } else {
                      if {"$sRoutesInProcess" == ""} {
                           set sRoutesInProcess [mql execute program emxMailUtil -method getMessage \
                                                     "emxFramework.ProgramObject.eServicecommonCompleteTask.None" 0 \
                                                     "" \
                                                ]
                      }
                      if {"$sObjectsNotSatisfied" == ""} {
                           set sObjectsNotSatisfied [mql execute program emxMailUtil -method getMessage \
                                                         "emxFramework.ProgramObject.eServicecommonCompleteTask.None" 0 \
                                                         "" \
                                                    ]
                      }
                      set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                            "$sOwner" \
                                            "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectNotComplete" 0 \
                                            "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageNotComplete" 6 \
                                                          "RType" "$sRouteType" \
                                                          "RName" "$sRouteName" \
                                                          "RRev" "$sRouteRev" \
                                                          "PromotedObj" "$sPromotedObjects" \
                                                          "RInProcess" "$sRoutesInProcess" \
                                                          "ONotApproved" "$sObjectsNotSatisfied" \
                                            "" \
                                            "" \
                                            }
                      set mqlret [ catch {eval  $sCmd} outStr ]
                      if {$mqlret != 0} {
                          mql notice "$outStr"
                          return $mqlret
                      }
                  }
              } else {
                  set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                        "$sOwner" \
                                        "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectRouteComplete" 0 \
                                        "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageRouteComplete" 3 \
                                                      "RType" "$sRouteType" \
                                                      "RName" "$sRouteName" \
                                                      "RRev" "$sRouteRev" \
                                        "" \
                                        "" \
                                        }
                  set mqlret [ catch {eval  $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return $mqlret
                  }
              }
          }
      }
  }

  return $mqlret
}
# end eServicecommonCompleteTask

# End of Module

