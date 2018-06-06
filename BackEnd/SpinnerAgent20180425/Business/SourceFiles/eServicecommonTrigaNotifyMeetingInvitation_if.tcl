#************************************************************************
# @progdoc      eServicecommonTrigaNotifyMeetingInvitation_if.tcl
#
# @Brief:       Notify the Person for Meeting
#
# @Description: This program sends iconmail to the Company Representatives
#               when an Employee is connected to a Company.
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

  # Get the company id and the person id and name.
  set sMeetingId   [ mql get env TOOBJECTID ]
  set sMeetingType [ mql get env TOTYPE ]
  set sMeetingName [ mql get env TONAME ]
  set sMeetingRev  [ mql get env TOREVISION ]
  set sPersonName  [ mql get env FROMNAME ]

  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]
  eval [ utLoad eServicecommonTranslation.tcl ]

  # Get Absolute Names
  set sAttOriginator           [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_Originator" ]
  set sAttMeetingStartDateTime [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_MeetingStartDateTime" ]

  # Get Meeting Host, Date, Note, Project Meeting.
  set sCmd {mql print bus "$sMeetingId" \
                           select \
                           description \
                           attribute\[$sAttMeetingStartDateTime\] \
                           attribute\[$sAttOriginator\] \
                           dump | \
                           }
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      mql notice "$outStr"
  }

  set lOutput [split $outStr |]
  set sMeetingNote [lindex $lOutput 0]
  set sMeetingDate [lindex $lOutput 1]
  set sMeetingHost [lindex $lOutput 2]

  # append time zone to date
  append sMeetingDate " [clock format [clock seconds] -format %Z]"

  # Define the iconmail subject and text.
  if {"$sMeetingNote" == ""} {
      set sMeetingNote [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigaNotifyMeetingInvitation_if.None" 0 \
                                "" \
                                ]
  }

  # Send mail
  set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                        "$sPersonName" \
                        "emxFramework.ProgramObject.eServicecommonTrigaNotifyMeetingInvitation_if.Subject" 0 \
                        "emxFramework.ProgramObject.eServicecommonTrigaNotifyMeetingInvitation_if.Message" 3 \
                                      "host" "$sMeetingHost" \
                                      "date" "$sMeetingDate" \
                                      "note" "$sMeetingNote" \
                        "$sMeetingId" \
                        "" \
                        }
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      mql notice "$outStr"
  }

  return $mqlret
}

