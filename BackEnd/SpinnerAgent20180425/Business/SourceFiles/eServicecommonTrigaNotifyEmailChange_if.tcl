
#************************************************************************
# @progdoc      eServicecommonTrigaNotifyEmailChange_if.tcl
#
# @Brief:       Update a Person business object's email address
#
# @Description: This program will notify an employee when their email changes.
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

  mql verbose off
  mql quote off

  # Load the utilities and other libraries.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]
  eval [ utLoad eServicecommonDEBUG.tcl ]
  eval [ utLoad eServicecommonTranslation.tcl ]
  eval [ utLoad eServiceSchemaVariableMapping.tcl ]

  # Get actual state names from properties
  set sActiveState [eServiceGetCurrentSchemaName state eServiceSchemaVariableMapping.tcl policy_Person state_Active ]
  set sInactiveState [eServiceGetCurrentSchemaName state eServiceSchemaVariableMapping.tcl policy_Person state_Inactive ]

  # Get RPE values.
  set sPerson [ mql get env NAME ]
  set sOldEmail [ mql get env ATTRVALUE ]
  set sNewEmail [ mql get env NEWATTRVALUE ]
  set sUser [ mql get env USER ]
  set sTime [ mql get env TIMESTAMP ]
  set sEmailDefault [mql get env ATTRDEFAULT]
  set sCurrentState [mql get env CURRENTSTATE]

  # If the old email is not the default for the attribute,
  # then send an email to the person (still with old email address).
  # Only if the Object is in Active State
  if { "$sCurrentState" == "$sActiveState" } {
    if { $sOldEmail != $sEmailDefault } {
      set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sPerson" \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmailChange_if.Subject" 0 \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmailChange_if.Message" 4 \
                                          "oldEmail" "$sOldEmail" \
                                          "newEmail" "$sNewEmail" \
                                          "user" "$sUser" \
                                          "time" "$sTime" \
                            "" \
                            "" \
                            }
      utCatch { eval $sCmd }
    }
  } elseif { "$sCurrentState" == "$sInactiveState" } {
    set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                          "$sPerson" \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmailChange_if.RegSubject" 0 \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmailChange_if.RegMessage" 0 \
                          "" \
                          "" \
                          }
    utCatch { eval $sCmd }
  }

  # Update the person's email address as the shadow agent.
  utCatch {
    pushShadowAgent
    mql modify person $sPerson email $sNewEmail
    popShadowAgent
  }

  # If the old email is not the default for the attribute,
  # then send an email to the person (with new email address).
  # Only if the Object is in Active State
  if { "$sCurrentState" == "$sActiveState" } {
    if { $sNewEmail != $sEmailDefault } {
      set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sPerson" \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmailChange_if.Subject" 0 \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmailChange_if.Message" 4 \
                                          "oldEmail" "$sOldEmail" \
                                          "newEmail" "$sNewEmail" \
                                          "user" "$sUser" \
                                          "time" "$sTime" \
                            "" \
                            "" \
                            }
      utCatch { eval $sCmd }
    }
  }

  exit

}


