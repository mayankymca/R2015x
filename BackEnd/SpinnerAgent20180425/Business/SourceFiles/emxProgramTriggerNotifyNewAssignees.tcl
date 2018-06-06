###############################################################################
#
# $RCSfile: emxProgramTriggerNotifyNewAssignees.tcl.rca $ $Revision: 1.14 $
#
# @progdoc     emxProgramTriggerNotifyNewAssignees.tcl
#
# @Brief:      This trigger will send notifications to assigned members 
#              when the task has gone behond the state Create/Assigned and
#              new assignees have been added.
#
# @Description: Notify persons assigned to Tasks while the task is active
#
# @Parameters:  none
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
  #  mql notice  $str
}


#--------------------
#  Main Program
#--------------------
eval {

  mql verbose off;
  mql quote off;


Debug "\n\n emxProgramTriggerNotifyNewAssignees.tcl"

      # Obtain property values
  set output [mql print program eServiceSchemaVariableMapping.tcl select \
    property\[type_ProjectSpace\].to \
    property\[relationship_Subtask\].to \
    property\[relationship_AssignedTasks\].to \
    property\[attribute_TaskEstimatedStartDate\].to \
    property\[attribute_TaskEstimatedFinishDate\].to \
    property\[attribute_TaskEstimatedDuration\].to \
    property\[type_PartQualityPlan\].to \
    property\[attribute_CriticalTask\].to \
    dump |]

  set output [split $output |]
  foreach {sTypeProject sRelSubtask sRelAssignedTasks \
           sAttrTaskEstimatedStartDate sAttrTaskEstimatedFinishDate \
           sAttrTaskEstimatedDuration sTypePartQualityPlan sAttrCriticalTask} $output {break}

  set sTypeProject [lrange $sTypeProject 1 end]
  set sRelSubtask [lrange $sRelSubtask 1 end]
  set sRelAssignedTasks [lrange $sRelAssignedTasks 1 end]
  set sAttrTaskEstimatedStartDate [lrange $sAttrTaskEstimatedStartDate 1 end]
  set sAttrTaskEstimatedFinishDate [lrange $sAttrTaskEstimatedFinishDate 1 end]
  set sAttrTaskEstimatedDuration [lrange $sAttrTaskEstimatedDuration 1 end]
  set sTypePartQualityPlan [lrange $sTypePartQualityPlan 1 end]
  set sAttrCriticalTask [lrange $sAttrCriticalTask 1 end]


  # Get the object.
 
  set sEvent [string tolower [ mql get env EVENT ]] 
  set sObjectId  [ mql get env TOOBJECTID ]
  set sType  [ mql get env TOTYPE ]
  set sName  [ mql get env TONAME ]
  set sRev  [ mql get env TOREVISION ]

  # Assignee Information
  set sAssigneeId  [ mql get env FROMOBJECTID ]
  set sAssigneeType  [ mql get env FROMTYPE ]
  set sAssigneeName [ mql get env FROMNAME ]
  set sAssigneeRev  [ mql get env FROMREVISION ]

  # Passed Parameters
  # This parameter should be true or false
 set sActivateSideDoor   [string tolower [string trim [mql get env 1]]]


  set mqlret 0
  set outStr ""
  set sErrMsg ""

Debug "OBJECT sAssigneeName = $sAssigneeName, task = $sName sEvent: $sEvent"

   set outStr [split [mql print bus $sObjectId select name type current  \
    attribute\[$sAttrTaskEstimatedStartDate\] \
    attribute\[$sAttrTaskEstimatedFinishDate\] \
    attribute\[$sAttrTaskEstimatedDuration\] \
    attribute\[$sAttrCriticalTask\] \
    policy state dump |] |]

   set sTaskName [lindex $outStr 0]
   set sTaskType [lindex $outStr 1]
   set sTaskCurrent [lindex $outStr 2]
   set sTaskEstStartDate [lindex $outStr 3]
   set sTaskEstFinishDate [lindex $outStr 4]
   set sTaskEstDuration [lindex $outStr 5]
   set sCritikTask [lindex $outStr 6]
   set sPolicy [lindex $outStr 7]
   set lStates [lrange $outStr 8 end]
  # Get the Assign State Name
  set sAssignPropVal "Assign"
  set sCmd {mql print policy "$sPolicy" select property\[state_Assign\].value dump}
  if { "$sPolicy" == "Project Review" } {
    set sAssignPropVal "Create"
    set sCmd {mql print policy "$sPolicy" select property\[state_Create\].value dump}
  }
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
     set sAssignPropVal "$outStr"
  } 
  
  # Get Parents state position
  set iAssignPos [lsearch $lStates "$sAssignPropVal"];
  set iTaskPos [lsearch $lStates "$sTaskCurrent"];

  #Added:nr2:PRG:R212:12 July 2011:IR-084688V6R2012x
  set sPrjId ""
  set bIsMember 1
  catch {
          set sPrjId [ mql execute program emxProgramCentralUtil -method getParentProject "$sObjectId" ] 
   
          if { "$sPrjId" != "" } {
              set outStr [split [mql print bus $sPrjId select from\[Member\].to.name dump |] |]
              set lMemberName [lrange $outStr 0 end]
              set bIsMember [lsearch $lMemberName "$sAssigneeName"]
           } 
    } 
  #Endd:nr2:PRG:R212:12 July 2011:IR-084688V6R2012x
  
  #
  # Set the notification subject
  #  
  if { "$sEvent" == "create" } { 
     set sSubject  "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyMembers.Subject"
     if { "$sCritikTask" == "TRUE" } {
     set sNotificationText "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyMembers.CriticalMessage"
     } else {
     set sNotificationText "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyMembers.Message"
     }
  } else { 
     set sSubject "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyNewAssignees.RevokeSubject"
     set sNotificationText "emxProgramCentral.ProgramObject.emxProgramTriggerNotifyNewAssignees.RevokeMessage"
  }
  set sURLString  ""; 

   catch {  
       }



  # Send Notification only if the Task has passed the Create State 
  if { $iTaskPos >= $iAssignPos  } { 

    # Find all business objects related to a task, send notification if connected to any PMC type
    set outStrTypes ""
    set sFound 0
    set sCmdGetTypes {mql expand bus $sObjectId to rel $sRelSubtask recurse to all select bus type dump |}
    set mqlret [ catch {eval $sCmdGetTypes} outStr ]

 
    if { $mqlret == 0 } {
      set lTypes [split $outStr \n]

      foreach sType $lTypes {
        set lType [split "$sType" |]
        set sType [lindex $lType 6]
        
        if {"$sType" == "$sTypePartQualityPlan"} {
          set sFound 1
        }
      }
    }
    
      if { $sFound == 0 } {
        # Send the notfication
        if { "$sActivateSideDoor" == "" } { 
              
             set sCmd {mql execute program emxProgramCentralUtil -method sendNotificationToUser \
                    "$sAssigneeName" \
                    "$sSubject" 0 \
                    "$sNotificationText" 5  \
                    "TaskType" "$sTaskType" \
                    "TaskName" "$sTaskName" \
                    "TaskEstStartDate" "$sTaskEstStartDate" \
                    "TaskEstFinishDate" "$sTaskEstFinishDate" \
                    "TaskEstDuration" "$sTaskEstDuration" \
                    "$sObjectId" \
                    "" \
             }
          set mqlret [ catch {eval  $sCmd} outStr ]
        } else { 
             puts "Executing Mql"
          set sURLString [mql execute program emxMailUtil -method getStreamBaseURL ]

           set sSubject [mql execute program emxProgramCentralUtil -method getMessage \
                         "$sSubject" 0 ]


     set sNotificationText [mql execute program emxProgramCentralUtil -method getMessage  \
        "$sNotificationText" 5  \
          "TaskType" "$sTaskType" \
          "TaskName" "$sTaskName" \
          "TaskEstStartDate" "$sTaskEstStartDate" \
          "TaskEstFinishDate" "$sTaskEstFinishDate" \
          "TaskEstDuration" "$sTaskEstDuration" \
        ]
     if { $bIsMember != -1 } {
             append  sNotificationText "\n\n${sURLString}?objectId=${sObjectId}"
         } 
            set sCmd "mql send mail bus \$sObjectId to \$sAssigneeName subject \$sSubject text \$sNotificationText"
            set mqlret [catch {eval $sCmd} outStr]
        }
      }
    }

    
}

