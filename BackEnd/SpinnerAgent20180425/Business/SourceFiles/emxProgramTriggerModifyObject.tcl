###############################################################################
#
# $RCSfile: emxProgramTriggerModifyObject.tcl.rca $ $Revision: 1.6 $
#
# @progdoc     emxProgramTriggerModifyObject.tcl
#
# @Brief:      This trigger will modify attributes based on the event
#
# @Description:   For Project and Task objects, modifies the Task Actual Start Date upon promotion to/demotion from
#                          the Active state and modifies the Task Actual Finish Date and Task Actual Duration upone promotion
#                          to/demotion from the Complete state.
#
# @Parameters: none
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
proc Debug {str} {
   #puts "$str"
}


#--------------------
#  Main Program
#--------------------
eval {

  mql verbose off;
  mql quote off;

  # Obtain property values
  set output [mql print program eServiceSchemaVariableMapping.tcl select \
    property\[attribute_TaskActualStartDate\].to \
    property\[attribute_TaskActualFinishDate\].to \
    property\[attribute_TaskActualDuration\].to dump |]

  set output [split $output |]
  foreach {sAttrTaskActualStartDate sAttrTaskActualFinishDate \
           sAttrTaskActualDuration} $output {break}

  set sAttrTaskActualStartDate [lrange $sAttrTaskActualStartDate 1 end]
  set sAttrTaskActualFinishDate [lrange $sAttrTaskActualFinishDate 1 end]
  set sAttrTaskActualDuration [lrange $sAttrTaskActualDuration 1 end]


Debug "\n emxProgramTriggerModifyObject.tcl"


  # Get the object.
  set sObjectId  [ mql get env OBJECTID ]
  set sEvent  [ mql get env EVENT ]
  set sCurrentState [ mql get env CURRENTSTATE ]
  set sNextState [ mql get env NEXTSTATE ]
Debug "\t sEvent= $sEvent sCurrentState= $sCurrentState sNextState: $sNextState"

  # Set the date format
  # set MXDATE_FORMAT "%a %b %d, %Y %I:%M:%S %p"
  set MXDATE_FORMAT "%a %b %d, %Y"


  set mqlret 0
  set outStr ""

  # Get the objects policy
  #set sCmd {mql print bus "$sObjectId" select policy dump}
  set sCmd {mql print bus $sObjectId select \
                policy \
                policy.property\[state_Assign\] \
                policy.property\[state_Active\] \
                policy.property\[state_Complete\] \
            dump |}

  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
    set sErrMsg "$outStr"
  }

  if { $mqlret == 0 } {
       set outStr [split $outStr |]
       foreach {policy sAssignPropVal sActivePropVal sCompletePropVal} $outStr {break}
       set sAssignPropVal [lrange $sAssignPropVal 2 end]
       set sActivePropVal [lrange $sActivePropVal 2 end]
       set sCompletePropVal [lrange $sCompletePropVal 2 end]

Debug "\t sActivePropVal: $sActivePropVal   sCompletePropVal: $sCompletePropVal  sAssignPropVal: $sAssignPropVal"
       # Check if any of the state property values are null
       if { "$sActivePropVal" == "" || "$sCompletePropVal" == "" || "$sAssignPropVal" == "" } {
            set mqlret 1
            set sErrMsg "Policy '$sPolicy' was not defined properly!"
       }

       if { $mqlret == 0 } {

            # Check the event
            if { $sEvent == "Promote" } {
                 # Check the current state of the object, set the attribute to be modified, and modify the attribute
                 if { "$sCurrentState" == "$sActivePropVal" || "$sNextState" == "$sActivePropVal"  } {
                      # Promoting from Assign to Active
                      set sAttrName "$sAttrTaskActualStartDate"
                      set sAttrVal [ clock format [ clock seconds ] -format $MXDATE_FORMAT ]
                      mql modify bus $sObjectId $sAttrName $sAttrVal

                 } elseif { "$sCurrentState" == "$sCompletePropVal" || "$sNextState" == "$sCompletePropVal" } {
                      # Promoting from Active to Complete
                      set sAttrName "$sAttrTaskActualFinishDate"
                      set sAttrVal [ clock format [ clock seconds ] -format $MXDATE_FORMAT ]

                      # Get the objects Task Actual Start Date attribute value
                      set sCmd {mql print bus "$sObjectId" select attribute\[$sAttrTaskActualStartDate\] dump}
                      set mqlret [catch {eval $sCmd} outStr]
                      if {$mqlret != 0} {
                          set sErrMsg "$outStr"
                      } else {
                          set iStartDate [clock scan $outStr]
                          set iEndDate [clock scan "$sAttrVal"]

                          # Calculate the actual duration from Start to Finish in days
                          set sAttrVal2 [expr [expr ($iEndDate - $iStartDate)/86400] + 1]
                          set sAttrName2 "$sAttrTaskActualDuration"

                          mql modify bus $sObjectId $sAttrName $sAttrVal $sAttrName2 $sAttrVal2
                      }
                 }

            } else {
                 # Check the current state of the object, set the attribute to be cleared out, and modify the attribute
                 if { "$sCurrentState" == "$sActivePropVal" } {
                      # Demoting from Active to Assign
                      set sAttrName "$sAttrTaskActualStartDate"
                      mql modify bus $sObjectId $sAttrName ""
                 } elseif { "$sCurrentState" == "$sCompletePropVal" } {
                      set sAttrName "$sAttrTaskActualFinishDate"
                      set sAttrName2 "$sAttrTaskActualDuration"
                      mql modify bus $sObjectId $sAttrName "" $sAttrName2 ""
                 }

            }

       }

  }

  if { $mqlret != 0 } {
       regsub -all {\"} $sErrMsg {'} sErrMsg
       mql notice "$sErrMsg"
  }



Debug "\n Done... emxProgramTriggerModifyObject.tcl"

  return $mqlret

}


