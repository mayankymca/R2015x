#************************************************************************
# @progdoc      eServicecommonTrigaCleanupMeetingConnections_if.tcl
#
# @Brief:       Cleans all Assigned Meetings and Meeting Context connections
#               accept Person who created meeting
#
# @Parameters:  None.
#
# @Returns:     Nothing.
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#************************************************************************

tcl;

eval {

  mql verbose off;
  mql quote off;

  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]
  eval [ utLoad eServicecommonDEBUG.tcl ]

  # Get Absolute Names
  set sRelAssignedMeetings [ eServiceGetCurrentSchemaName "relationship" $sRegProgName "relationship_AssignedMeetings" ]
  set sRelMeetingContext   [ eServiceGetCurrentSchemaName "relationship" $sRegProgName "relationship_MeetingContext" ]
  set sAttOriginator       [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_Originator" ]
  set sAttMeetingOwner     [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_MeetingOwner" ]

  # Get the object.
  set sType [ mql get env TYPE ]
  set sName [ mql get env NAME ]
  set sRev  [ mql get env REVISION ]

  # Expand to get all objects connected via Assigned Meetings and Meeting Context
  set sCmd {mql expand bus "$sType" "$sName" "$sRev" \
                           to relationship "$sRelAssignedMeetings,$sRelMeetingContext" \
                           select relationship \
                           attribute\[$sAttMeetingOwner\] \
                           dump | \
                           }
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }

  # disconnect all connections accept host person.
  pushShadowAgent
  set lOutput [split $outStr \n]
  foreach sItem $lOutput {
      set lItem [split $sItem |]
      set sRel     [lindex $lItem 1]
      set sObjType [lindex $lItem 3]
      set sObjName [lindex $lItem 4]
      set sObjRev  [lindex $lItem 5]
      set sOwner  [lindex $lItem 6]

      if {"$sOwner" == "No"} {
          set sCmd {mql disconnect bus "$sObjType" "$sObjName" "$sObjRev" relationship "$sRel" to "$sType" "$sName" "$sRev"}
          set mqlret [catch {eval $sCmd} outStr]
          if {$mqlret != 0} {
              mql notice "$outStr"
              break
          }
      }
  }
  popShadowAgent

  return $mqlret
}

