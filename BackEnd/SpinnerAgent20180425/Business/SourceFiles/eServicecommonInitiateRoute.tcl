###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonInitiateRoute.tcl.rca $ $Revision: 1.69 $
#
# @libdoc       eServicecommonInitiateRoute
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
 eval  [ utLoad eServicecommonObjectGenerator.tcl]
 eval  [ utLoad eServicecommonTranslation.tcl]
 eval  [ utLoad eServicecommonShadowAgent.tcl]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonInitiateRoute.tcl
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
#                 {{ eServicecommonInitiateRoute  {to/from/both} {$relationship_rel1,$relationship_rel2,..} {$type_type1,$type_type2,...} }}
#
# @procdoc
#******************************************************************************

proc eServicecommonInitiateRoute {sType sName sRev sRouteSequenceValue bErrorOnNoConnection} {

  set RegProgName   "eServiceSchemaVariableMapping.tcl"
  set progname      "eServicecommonInitiateRoute"
  set sTreeMenu [mql get env global MX_TREE_MENU]
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
  set sRelRouteNode               [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_RouteNode"]
  set sRelRouteTask               [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_RouteTask"]
  set sRelProjectTask             [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_ProjectTask"]
  set sRelObjectRoute             [eServiceGetCurrentSchemaName relationship $RegProgName "relationship_ObjectRoute"]

  set sAttRouteSequence           [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteSequence"]
  set sAttRouteAction             [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteAction"]
  set sAttRouteInstructions       [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteInstructions"]
  set sAttApprovalStatus          [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ApprovalStatus"]
  set sAttScheduledCompletionDate [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ScheduledCompletionDate"]
  set sAttApproversResponsibility [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ApproversResponsibility"]
  set sAttRouteNodeID             [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteNodeID"]
  set sAttCurrentRouteNode        [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_CurrentRouteNode"]
  set sAttRouteStatus             [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteStatus"]
  set sAttTitle                   [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_Title"]
  set sAttAllowDelegation         [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_AllowDelegation"]
  set sAttReviewTask              [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_ReviewTask"]
  set sAttAssigneeDueDateOpt      [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_AssigneeSetDueDate"]
  set sAttDueDateOffset           [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_DueDateOffset"]
  set sAttDueDateOffsetFrom       [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_DateOffsetFrom"]
  set sAttTemplateTask            [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_TemplateTask"]
  set sAttFirstName               [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_FirstName"]
  set sAttLastName                [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_LastName"]

  set sAttAbsenceStartDate        [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_AbsenceStartDate"]
  set sAttAbsenceEndDate          [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_AbsenceEndDate"]
  set sAttAbsenceDelegate         [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_AbsenceDelegate"]
  set sAttRouteTaskUser           [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteTaskUser"]
  set sAttRouteTaskUserCompany    [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteTaskUserCompany"]

  set sTypePerson                 [eServiceGetCurrentSchemaName type $RegProgName "type_Person"]
  set sTypeRouteTaskUser          [eServiceGetCurrentSchemaName type $RegProgName "type_RouteTaskUser"]

  set sWorkspaceAccessGrantor     [eServiceGetCurrentSchemaName person $RegProgName "person_WorkspaceAccessGrantor"]
  set sRouteDelegationGrantor     [eServiceGetCurrentSchemaName person $RegProgName "person_RouteDelegationGrantor"]
  set routeNodeid                 [eServiceGetCurrentSchemaName attribute $RegProgName "attribute_RouteNodeID"]
  #
  # initialize variable for reference below.
  #
  set fGranteeLookup 0
  #
  # Get some route information.
  #
  set sCmd {mql print bus "$sType" "$sName" "$sRev" \
            select id owner \
            dump | \
           }
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return -code $mqlret $outStr
  }

  set sRouteId    [lindex [split $outStr |] 0]
  set sRouteOwner [lindex [split $outStr |] 1]

  #
  # Expand Object Route relationship to get routed items
  #
  set sCmd {mql expand bus $sRouteId \
            to relationship "$sRelObjectRoute" \
            select bus id \
            dump | \
           }
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return -code $mqlret $outStr
  }

  set sRouteObjects ""
  set lRouteObjects {}
  set lExpandOutput [split $outStr \n]
  foreach sItem $lExpandOutput {
      set lItem [split $sItem |]
      append sRouteObjects "'[lindex $lItem 3]' '[lindex $lItem 4]' '[lindex $lItem 5]'\n"
      # AEF 9500: Route now support multiple objects being routed with the same route
      lappend lRouteObjects [lindex $lItem 6]
  }

  #
  # Expand Route Node relationship
  #
  set sCmd {mql expand bus $sRouteId \
                           from relationship "$sRelRouteNode" \
                           select rel id \
                                      attribute\[$sAttRouteSequence\] \
                                      attribute\[$sAttRouteAction\] \
                                      attribute\[$sAttApprovalStatus\] \
                                      attribute\[$sAttScheduledCompletionDate\] \
                                      attribute\[$sAttApproversResponsibility\] \
                                      attribute\[$sAttTitle\] \
                                      attribute\[$sAttAllowDelegation\] \
                                      attribute\[$sAttReviewTask\] \
                                      attribute\[$sAttAssigneeDueDateOpt\] \
                                      attribute\[$sAttDueDateOffset\] \
                                      attribute\[$sAttDueDateOffsetFrom\] \
                                      attribute\[$sAttTemplateTask\] \
                                      attribute\[$sAttRouteTaskUser\] \
                                      attribute\[$sAttRouteTaskUserCompany\] \
                                      attribute\[$sAttRouteNodeID\] \
                           select bus id \
                                      name \
                                      vault \
                                      attribute\[$sAttAbsenceDelegate\] \
                                      attribute\[$sAttFirstName\] \
                                      attribute\[$sAttLastName\] \
                           dump | \
                           }
  set mqlret [ catch {eval  $sCmd} outStrUse ]
  if {$mqlret != 0} {
      mql notice "$outStrUse"
      return -code $mqlret $outStrUse
  }

  set lExpandOutput [split $outStrUse \n]

  if {[llength $lExpandOutput] == 0} {
      set mqlret 1
      set outStr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonInitiateRoute.NoObjectError" 4 \
                                  "Person" "$sTypePerson" \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                    "" \
                    ]
      mql notice "$outStr"
      return -code $mqlret $outStr
  }
  # Check if Person or Group/Role with required sequence order is present
  # If not then increament sequence order till Person or Group/Role found
  # If no person or group/role found then error out.
  set bGreaterSeqNoFound 1
  while {$bGreaterSeqNoFound == 1} {
      set bGreaterSeqNoFound 0
      foreach sItem $lExpandOutput {
          set lItem [split $sItem |]
          set sRouteSequence [lindex $lItem 13]

          if {$sRouteSequence > $sRouteSequenceValue} {
              set bGreaterSeqNoFound 1
          }

          #
          # Get all the relationships with Route Sequence = 1
          #
          if {$sRouteSequence == "$sRouteSequenceValue"} {
              set bPersonFound             1

              set sRouteNodeId             [lindex $lItem 12]
              set sRouteAction             [lindex $lItem 14]
              set sApprovalStatus          [lindex $lItem 15]
              set sScheduledCompletionDate [lindex $lItem 16]
              set sApproversResponsibility [lindex $lItem 17]
              set sTitle                   [lindex $lItem 18]
              set sAllowDelegation         [lindex $lItem 19]
              set sReviewTask              [lindex $lItem 20]
              set sAssigneeDueDateOpt      [lindex $lItem 21]
              set sDueDateOffset           [lindex $lItem 22]
              set sDueDateOffsetFrom       [lindex $lItem 23]
              set sTemplateTask            [lindex $lItem 24]
              set sRouteTaskUser           [lindex $lItem 25]
              set sRouteTaskUserCompany    [lindex $lItem 26]
              set sRouteNodeIdAttr         [lindex $lItem 27]
              set sPersonId                [lindex $lItem 6]
              set sPersonName              [lindex $lItem 7]
              set sPersonVault             [lindex $lItem 8]
              set sAbsenceDelegate         [lindex $lItem 9]
              set sPersonFirstName         [lindex $lItem 10]
              set sPersonLastName          [lindex $lItem 11]
              #
              # Get multiline attribute Route Instuction from connection id
              #
              set sCmd {mql print connection $sRouteNodeId select attribute\[$sAttRouteInstructions\].value dump |}
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }
              set sRouteInstructions "$outStr"

              set sRPEType     [ mql get env TYPE ]
              set sRPEName     [ mql get env NAME ]
              set sRPERev      [ mql get env REVISION ]

              if {"$sTreeMenu" != ""} {
                  mql set env global MX_TREE_MENU "$sTreeMenu"
              }
              set ssCmd {mql expand bus "$sRouteId" \
                      to relationship "$sRelRouteTask" \
                      select bus id \
                      where (attribute\[$sAttRouteNodeID\]==$sRouteNodeId) \
                      dump | \
                      }
            set mqlret [ catch {eval  $ssCmd} outStr1 ]
            if {$mqlret != 0} {
                mql notice "$outStr"
                utCheckAbortTransaction $bTranAlreadyStarted
                return "1|$outStr1"
            }
            if {$outStr1 == ""} {
              #
              # Create a Inbox Task
              #
              set lNumGenOutput [eServiceNumberGenerator "type_InboxTask" "" "" "Null" "[mql get env VAULT]"]

              set sNumGenError [lindex $lNumGenOutput 0]

              if {[string compare $sNumGenError ""] == 0 } {
                  set mqlret 1
                  set outStr [lindex $lNumGenOutput 1]
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }
              set sInboxTaskId   [lindex [lindex $lNumGenOutput 0] 0]
              set sInboxTaskType [lindex [lindex $lNumGenOutput 0] 1]
              set sInboxTaskName [lindex [lindex $lNumGenOutput 0] 2]
              set sInboxTaskRev  [lindex [lindex $lNumGenOutput 0] 3]

              # Before connecting the task to the person, check to see if
              # delegation is required:

              set sDelegatorId ""

              if {$sAbsenceDelegate != "" && [string toupper $sAllowDelegation] == "TRUE"} {
                  # Rather than dealing with date/time formats, get the current
                  # time from the server.  The RPE timestamp available in triggers
                  # is not necessarily the same format of the server.
                  set sCurrentTime [mql print bus $sInboxTaskId select originated dump]

                  set sCmd "mql temp query bus \$sTypePerson \$sPersonName '*' \
                            vault \$sPersonVault \
                            where {'attribute\[$sAttAbsenceStartDate\]' <= \
                                  '$sCurrentTime' && \
                                  'attribute\[$sAttAbsenceEndDate\]' >= \
                                  '$sCurrentTime' \
                                  } \
                            dump \
                           "

                  set mqlret [ catch {eval $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice $outStr
                      return -code $mqlret $outStr
                  }

                  if {$outStr != ""} {
                      # The person is absent; delegate the person's task
                      # Just re-set sPersonId & sPersonName to the new person
                      # Look up this new person
                      set sCmd {mql temp query bus "$sTypePerson" "$sAbsenceDelegate" "*" \
                                vault "*" \
                                select id \
                                       attribute\[$sAttFirstName\] \
                                       attribute\[$sAttLastName\] \
                                dump | \
                               }

                      set mqlret [ catch {eval $sCmd} outStr ]
                      if {$mqlret != 0} {
                          mql notice $outStr
                          return -code $mqlret $outStr
                      }

                      set sDelegatorName      $sAbsenceDelegate
                      set lDelegatorItems     [split $outStr |]
                      set sDelegatorId        [lindex $lDelegatorItems 3]
                      set sDelegatorFullName  "[lindex $lDelegatorItems 4] [lindex $lDelegatorItems 5]"

                      # Change route node relationship to reflect new delegator
                      set sCmd {mql modify connection $sRouteNodeId to $sDelegatorId}

                      set mqlret [ catch {eval $sCmd} outStr ]
                      if {$mqlret != 0} {
                          mql notice $outStr
                          return -code $mqlret $outStr
                      }

                      # Grant access to the route and route objects to new person
                      # based on access to original assignee.  Do this by looking
                      # up grants from 'Workspace Access Grantor' and granting
                      # to new user using 'Route Delegation Grantor'.

                      if {$fGranteeLookup == 0} {
                          set fGranteeLookup 1
                          set lGrants ""

                          foreach sObjectId [concat $lRouteObjects [list $sRouteId]] {
                              set lGrantor [split [mql print bus $sObjectId select grantor dump |] |]
                              if {$lGrantor == ""} {continue}
                              set outStr [split [mql print bus $sObjectId select grantee granteeaccess dump |] |]
                              set lGrantee [lrange $outStr 0 [expr [llength $lGrantor] -1]]
                              set lGranteeAccess [lrange $outStr [llength $lGrantor] end]
                              lappend lGrants [list $sObjectId $lGrantor $lGrantee $lGranteeAccess]
                          }
                      }

                      # this code is running under Shadow Agent
                      if {$lGrants != ""} {
                          pushShadowAgent "$sRouteDelegationGrantor"
                      }

                      foreach grant $lGrants {
                          foreach {sObjectId lGrantor lGrantee lGranteeAccess} $grant {}
                          foreach grantor $lGrantor grantee $lGrantee access $lGranteeAccess {
                              if {$grantor == $sWorkspaceAccessGrantor && \
                                  $grantee == $sPersonName} {
                                  set sCmd {mql modify bus $sObjectId grant $sDelegatorName access $access}
                                  set mqlret [ catch {eval $sCmd} outStr ]
                                  if {$mqlret != 0} {
                                      mql notice $outStr
                                      return -code $mqlret $outStr
                                  }
                              }

                          }
                      }

                      if {$lGrants != ""} {
                          popShadowAgent
                      }

                      # Override old assignee id and name to new delegator
                      set sPersonId   $sDelegatorId
                      set sPersonName $sDelegatorName
                  }
              }

              #
              # Connect Inbox Task via Project Task relationship to the Person or RTU the Route is attached to
              #
              set sCmd {mql connect bus "$sInboxTaskId" \
                                    relationship "$sRelProjectTask" \
                                    to "$sPersonId" \
                                    }
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }

              # set person variable to group/role if applicable
              if {$sRouteTaskUser != ""} {
                  set sGroupOrRole [ string tolower [ lindex [ split $sRouteTaskUser _ ] 0 ] ]
                  set sRouteTaskUserValue [eServiceGetCurrentSchemaName $sGroupOrRole $RegProgName "$sRouteTaskUser"]
                  set sPersonName "$sRouteTaskUserValue"
                  set sGroupOrRoleTitle "[string toupper [string range $sGroupOrRole 0 0] ][ string range $sGroupOrRole 1 end ]"
                  set sGRName "$sGroupOrRoleTitle - $sPersonName"
              }
              set sCmd {mql modify bus "$sInboxTaskId" \
                                   owner "$sPersonName" \
                                   }
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }

              #
              # Send Icon mail to the Group or Role if applicable.
              #
              if {"$sRPEType" == "$sInboxTaskType" && $sRouteTaskUser != ""} {

                  set sCmd {mql expand bus "$sRPEType" "$sRPEName" "$sRPERev" \
                                       from relationship "$sRelProjectTask" \
                                       dump | \
                                       }
                  set mqlret [ catch {eval  $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return -code $mqlret $outStr
                  }

                  set sRPEOwner [lindex [split $outStr |] 4]

                  if {"$sRouteObjects" == ""} {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice3" 2 \
                                                         "Name" "$sPersonName" \
                                                         "GroupOrRole" "$sGroupOrRoleTitle" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.MessageNotice2" 12 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "GRName" "$sGRName" \
                                                         "RPEType" "$sRPEType" \
                                                         "RPEName" "$sRPEName" \
                                                         "RPERev" "$sRPERev" \
                                                         "RPEOwner" "$sRPEOwner" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }

                  } else {
                         set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice3" 2 \
                                                         "Name" "$sPersonName" \
                                                         "GroupOrRole" "$sGroupOrRoleTitle" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.MessageNoticeWithObjects2" 13 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "GRName" "$sGRName" \
                                                         "RPEType" "$sRPEType" \
                                                         "RPEName" "$sRPEName" \
                                                         "RPERev" "$sRPERev" \
                                                         "RPEOwner" "$sRPEOwner" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "RObjects" "$sRouteObjects" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }

                  }
              } elseif {"$sRPEType" != "$sInboxTaskType" && $sRouteTaskUser != ""}  {
                  if {"$sRouteObjects" == ""} {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice3" 2 \
                                                         "Name" "$sPersonName" \
                                                         "GroupOrRole" "$sGroupOrRoleTitle" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.FirstMessage2" 8 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "GRName" "$sGRName" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }

                  } else {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice3" 2 \
                                                         "Name" "$sPersonName" \
                                                         "GroupOrRole" "$sGroupOrRoleTitle" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.FirstMessageWithObjects2" 9 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "GRName" "$sGRName" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "RObjects" "$sRouteObjects" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }

                  }
              }

              #
              # Send Icon mail to the Person if applicable.
              #
              if {"$sRPEType" == "$sInboxTaskType" && $sRouteTaskUser == ""} {

                  set sCmd {mql expand bus "$sRPEType" "$sRPEName" "$sRPERev" \
                                       from relationship "$sRelProjectTask" \
                                       dump | \
                                       }
                  set mqlret [ catch {eval  $sCmd} outStr ]
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return -code $mqlret $outStr
                  }

                  set sRPEOwner [lindex [split $outStr |] 4]

                  if {"$sRouteObjects" == ""} {
                      if {$sDelegatorId == ""} {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.MessageNotice" 11 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "RPEType" "$sRPEType" \
                                                         "RPEName" "$sRPEName" \
                                                         "RPERev" "$sRPERev" \
                                                         "RPEOwner" "$sRPEOwner" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      } else {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.DelegatorMessageNotice" 12 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "RPEType" "$sRPEType" \
                                                         "RPEName" "$sRPEName" \
                                                         "RPERev" "$sRPERev" \
                                                         "RPEOwner" "$sRPEOwner" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "Delegator" "$sPersonFirstName $sPersonLastName" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      }
                  } else {
                     if {$sDelegatorId == ""} {
                         set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.MessageNoticeWithObjects" 12 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "RPEType" "$sRPEType" \
                                                         "RPEName" "$sRPEName" \
                                                         "RPERev" "$sRPERev" \
                                                         "RPEOwner" "$sRPEOwner" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "RObjects" "$sRouteObjects" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      } else {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.DelegatorMessageNoticeWithObjects" 13 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "RPEType" "$sRPEType" \
                                                         "RPEName" "$sRPEName" \
                                                         "RPERev" "$sRPERev" \
                                                         "RPEOwner" "$sRPEOwner" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "RObjects" "$sRouteObjects" \
                                                         "Delegator" "$sPersonFirstName $sPersonLastName" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      }
                  }
              } elseif {"$sRPEType" != "$sInboxTaskType" && $sRouteTaskUser == ""} {
                  if {"$sRouteObjects" == ""} {
                      if {$sDelegatorId == ""} {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice2" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.FirstMessage" 7 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      } else {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice2" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.FirstDelegatorMessage" 8 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "Delegator" "$sPersonFirstName $sPersonLastName" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      }
                  } else {
                      if {$sDelegatorId == ""} {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice2" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.FirstMessageWithObjects" 8 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "RObjects" "$sRouteObjects" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      } else {
                          set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sPersonName" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectNotice2" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.FirstDelegatorMessageWithObjects" 9 \
                                                         "IBType" "$sInboxTaskType" \
                                                         "IBName" "$sInboxTaskName" \
                                                         "IBRev" "$sInboxTaskRev" \
                                                         "Type" "$sType" \
                                                         "Name" "$sName" \
                                                         "Rev" "$sRev" \
                                                         "ROwner" "$sRouteOwner" \
                                                         "RObjects" "$sRouteObjects" \
                                                         "Delegator" "$sPersonFirstName $sPersonLastName" \
                                           "$sInboxTaskId" \
                                           "" \
                                           }
                      }
                  }
              }

              popShadowAgent
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }
              pushShadowAgent

              #
              # Copy 'Route Action' 'Route Instructions' 'Approval Status' 'Scheduled Completion Date'
              #      'Route Task User' 'Route Task User Company'
              #      'Review Task'  'Assignee Set Due Date'
              #      'Due Date Offset' 'Date Offset From'
              #      'Approvers Responsibility' attributes from relationship Route Node to Inbox Task
              # Set 'Route Node Id' attribute of Inbox Task to 'Route Node' Relationship ID

             
              set sCmd {mql modify bus "$sInboxTaskId" \
                                       "$sAttRouteAction" "$sRouteAction" \
                                       "$sAttRouteInstructions" "$sRouteInstructions" \
                                       "$sAttApprovalStatus" "None" \
                                       "$sAttScheduledCompletionDate" "$sScheduledCompletionDate" \
                                       "$sAttApproversResponsibility" "$sApproversResponsibility" \
                                       "$sAttRouteNodeID" "$sRouteNodeIdAttr" \
                                       "$sAttTitle" "$sTitle" \
                                       "$sAttAllowDelegation" "$sAllowDelegation" \
                                       "$sAttReviewTask" "$sReviewTask" \
                                       "$sAttAssigneeDueDateOpt" "$sAssigneeDueDateOpt" \
                                       "$sAttDueDateOffset" "$sDueDateOffset" \
                                       "$sAttDueDateOffsetFrom" "$sDueDateOffsetFrom" \
                                       "$sAttTemplateTask" "$sTemplateTask" \
                                       "$sAttRouteTaskUser" "$sRouteTaskUser" \
                                       "$sAttRouteTaskUserCompany" "$sRouteTaskUserCompany" \
                                       }
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }
			set sCmd1 [mql execute program emxRoute -method setAssigneeDueDate "$sRouteId" "$sInboxTaskId" "$sAssigneeDueDateOpt" "$sRouteSequenceValue"]
			set mqlret [ catch {eval  $sCmd1} outStr1 ]
			if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }
		
              #
              # Connect 'Inbox Task' to Route via Route Task relationship
              #
              set sCmd {mql connect bus "$sInboxTaskId" \
                                    relationship "$sRelRouteTask" \
                                    to "$sType" "$sName" "$sRev" \
                                    }
              set mqlret [ catch {eval  $sCmd} outStr ]
              if {$mqlret != 0} {
                  mql notice "$outStr"
                  return -code $mqlret $outStr
              }

              #
              # Inform route owner that task was delegated to someone else
              #
              if {$sDelegatorId != ""} {
                  set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                           "$sRouteOwner" \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.SubjectDelegator" 0 \
                                           "emxFramework.ProgramObject.eServicecommonInitiateRoute.MessageDelegator" 4 \
                                                         "IBName" "$sInboxTaskName" \
                                                         "Name" "$sName" \
                                                         "Delegator" "$sDelegatorFullName" \
                                                         "FullName" "$sPersonFirstName $sPersonLastName" \
                                           "" \
                                           "" \
                                           }
                  popShadowAgent
                  set mqlret [ catch {eval  $sCmd} outStr ]
                  pushShadowAgent
                  if {$mqlret != 0} {
                      mql notice "$outStr"
                      return -code $mqlret $outStr
                  }
              }

          #
          # Set Route Status attribute on Route to equal Started
          # Set Current Route Node attribute on Route to equal 1
          #
          set sCmd {mql modify bus "$sType" "$sName" "$sRev" \
                                   "$sAttCurrentRouteNode" "$sRouteSequenceValue" \
                                   "$sAttRouteStatus" "Started" \
                                   }
          set mqlret [ catch {eval  $sCmd} outStr ]
          if {$mqlret != 0} {
              mql notice "$outStr"
              return -code $mqlret $outStr
          }
      }
      }
      }
      if {$bPersonFound == 1} {
          break
      }
      if {$bGreaterSeqNoFound == 1} {
          incr sRouteSequenceValue
      }
  }

  #
  # Error if no Person object found
  #
  if {$bPersonFound == 0 && $bErrorOnNoConnection == 1} {
      if {$sRouteTaskUser == "" } {
          set sTypePerson [eServiceGetCurrentSchemaName type $RegProgName "type_Person"]
          set mqlret 1
          set outStr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonInitiateRoute.NoObjectError" 4 \
                                  "Person" "$sTypePerson" \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                    "" \
                    ]
         } else {
          set sTypeRouteTaskUser [eServiceGetCurrentSchemaName type $RegProgName "type_RouteTaskUser"]
          set mqlret 1
          set outStr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonInitiateRoute.NoObjectError" 4 \
                                  "Person" "$sTypeRouteTaskUser" \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                    "" \
                    ]
        }
      mql notice "$outStr"
      return -code $mqlret $outStr
  }

  if {$bErrorOnNoConnection == 0} {
      return -code $mqlret $bPersonFound
  } else {
      return -code $mqlret $outStr
  }
}
# end eServicecommonInitiateRoute

# End of Module

