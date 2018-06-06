###############################################################################
#
# $RCSfile: emxProgramTriggerNotifyMembers.tcl.rca $ $Revision: 1.7 $
#
# @progdoc     emxProgramTriggerNotifyMembers.tcl
#
# @Brief:      This trigger will send notifications to assigned members upon promotion to Assign
#
# @Description: Notify persons assigned to Tasks when the task is promoted
#
# @Parameters:  1  = activate side-door true/false
#
# @Returns:     none
#
# @Usage:       
#
# @Example:     
#               
#               
#
# @progdoc      Copyright (c) 2001, Matrix One, Inc. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
#
###############################################################################


tcl;

#-----
# Procedure Section
#-----


#------------------------------------------------------
#
# Procedure Name:
#
# Purpose:  
#
# Parameters: 
#
# Returns: 
#------------------------------------------------------


#-----
# Procedure Section
#-----

proc Debug { str } { 
 # mql notice $str 
}



#--------------------
#  Main Program
#--------------------
eval { 

  mql verbose off;
  mql quote off;

      # Obtain property values
  set output [mql print program eServiceSchemaVariableMapping.tcl select \
    property\[type_ProjectSpace\].to \
    property\[relationship_Subtask\].to \
    property\[relationship_AssignedTasks\].to \
    property\[attribute_TaskEstimatedStartDate\].to \
    property\[attribute_TaskEstimatedFinishDate\].to \
    property\[attribute_TaskEstimatedDuration\].to \
    dump |]

  set output [split $output |]
  foreach {sTypeProject sRelSubtask sRelAssignedTasks \
           sAttrTaskEstimatedStartDate sAttrTaskEstimatedFinishDate \
           sAttrTaskEstimatedDuration} $output {break}

  set sTypeProject [lrange $sTypeProject 1 end]
  set sRelSubtask [lrange $sRelSubtask 1 end]
  set sRelAssignedTasks [lrange $sRelAssignedTasks 1 end]
  set sAttrTaskEstimatedStartDate [lrange $sAttrTaskEstimatedStartDate 1 end]
  set sAttrTaskEstimatedFinishDate [lrange $sAttrTaskEstimatedFinishDate 1 end]
  set sAttrTaskEstimatedDuration [lrange $sAttrTaskEstimatedDuration 1 end]


  # Get the object.
  set sType [ mql get env TYPE ]
  set sName [ mql get env NAME ]
  set sRev  [ mql get env REVISION ]
  set sObjectId  [ mql get env OBJECTID ]
  set sObjectCurrentState [ mql get env CURRENTSTATE ]
  set sObjectNextState [ mql get env NEXTSTATE ]
  set sEvent [string tolower [ mql get env EVENT ]] 

  # Passed Parameters
  # This parameter should be true or false
  set sActivateSideDoor   [string tolower [string trim [mql get env 1]]]


  # Promote Project has no assignees
  if { "$sTypeProject" == "$sType" } {  return 0; exit 0; } 

  set mqlret 0
  set outStr ""

   set lAssignees {}


   set outStr [split [mql print bus $sObjectId select name type  \
    attribute\[$sAttrTaskEstimatedStartDate\] \
    attribute\[$sAttrTaskEstimatedFinishDate\] \
    attribute\[$sAttrTaskEstimatedDuration\] \
    to\[$sRelSubtask\].from.name to\[$sRelSubtask\].from.owner \
    to\[$sRelSubtask\].from.owner.email \
    to\[$sRelAssignedTasks\].from.name \
    dump |] |]

   set sTaskName [lindex $outStr 0]
   set sTaskType [lindex $outStr 1]
   set sTaskEstStartDate [lindex $outStr 2]
   set sTaskEstFinishDate [lindex $outStr 3]
   set sTaskEstDuration [lindex $outStr 4]
   set sParentName [lindex $outStr 5]
   set sParentOwner [lindex $outStr 6]
   set sParentOwnerEmail [lindex $outStr 7]
   set lRecipients [lrange $outStr 8 end]


  #
  # Set the notification subject
  #  
  set sSubject  ""
  set sNotificationText ""
  set sURLString  ""; 

   catch {  
       set sURLString [mql execute program emxMailUtil -method getStreamBaseURL ]

     set sSubject [mql execute program emxProgramCentralUtil -method getMessage \
       "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyMembers.Subject" 0 ]


     set sNotificationText [mql execute program emxProgramCentralUtil -method getMessage  \
        "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyMembers.Message" 5  \
          "TaskType" "$sTaskType" \
          "TaskName" "$sTaskName" \
          "TaskEstStartDate" "$sTaskEstStartDate" \
          "TaskEstFinishDate" "$sTaskEstFinishDate" \
          "TaskEstDuration" "$sTaskEstDuration" \
        ]
        append  sNotificationText "\n<BR>${sURLString}?objectId=${sObjectId}"
  } 

  if { "$lRecipients" != "" } {  
      # Send the notfication
        if { "$sActivateSideDoor" == "true" } { 
           # Java requires only comma delimited list of users 
           set lAssignees [ join $lRecipients "," ]
           set sCmd {mql execute program emxProgramCentralUtil -method sendNotificationToUser \
                    "$lAssignees" \
                    "$sSubject" 0 \
                    "$sNotificationText" 0 \
                    "$sObjectId" \
                    "" \
               }
           set mqlret [ catch {eval  $sCmd} outStr ]
       } else { 
            # TCL requires a comma delimited list of users  with quotes 
            set lAssignees "\"[ join $lRecipients "\" ,\"" ]\""
            set sCmd "mql send mail bus \$sObjectId to $lAssignees subject \$sSubject text \$sNotificationText"
            set mqlret [catch {eval $sCmd} outStr]
       } 

    }; #  lAssignees


# END - EVAL
}

