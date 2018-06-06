
#************************************************************************
# @progdoc      eServicecommonTrigaNotifyEmployeeStatus_if.tcl
#
# @Brief:       Notify the Employee.
#
# @Description: This program sends iconmail to the employee when their
#               account is activated or de-activated.
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

  # Load the utilities.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonTranslation.tcl ]

  # Get the person name.
  set sPersonName [ mql get env NAME ]

  # Define the iconmail subject and text for activation and de-activation.
  if { [ string tolower [ mql get env EVENT ] ] == "promote" } {
    set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                          "$sPersonName" \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmployeeStatus_if.SubjectActivate" 0 \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmployeeStatus_if.MessageActivate" 1 \
                                        "name" "$sPersonName" \
                          "" \
                          "" \
                          }
  } else {
    set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                          "$sPersonName" \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmployeeStatus_if.SubjectDeactivate" 0 \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyEmployeeStatus_if.MessageDeactivate" 1 \
                                        "name" "$sPersonName" \
                          "" \
                          "" \
                          }
  }

  # Send the iconmail.
  utCatch { eval $sCmd }

  exit

}


