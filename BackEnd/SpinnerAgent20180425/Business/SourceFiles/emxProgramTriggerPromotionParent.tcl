###############################################################################
#
# $RCSfile: emxProgramTriggerPromotionParent.tcl.rca $ $Revision: 1.8 $
#
# @progdoc     emxProgramTriggerPromotionParent.tcl
#
# @Brief:      This trigger will promote/demote Parent tasks upon promotion/demotion
#              of a Project or Task object.
#
# @Description:   For Project and Task objects, checks are performed to determine
#                          whether of not to promote Parent tasks when a Project
#                          or Task object is promoted.
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

proc Debug { str } {
 # puts $str
}

#--------------------
#  Main Program
#--------------------
eval {

  mql verbose off;
  mql quote off;

  # Load in the utLoad procedure and other libraries.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]
  eval [ utLoad eServicecommonDEBUG.tcl ]

  # Load the schema mapping program.
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]


  # Load all property definitions in the global RPE
  LoadSchemaGlobalEnv  "eServiceSchemaVariableMapping.tcl";

  # Obtain property values
  set sRelSubtask [mql get env relationship_Subtask]

  mql set env RECURSIVE ""; 
  # Get the object.
  set sType [ mql get env TYPE ]
  set sName [ mql get env NAME ]
  set sRev  [ mql get env REVISION ]
  set sObjectId  [ mql get env OBJECTID ]
  set sObjectCurrentState [ mql get env CURRENTSTATE ]
  set sObjectNextState [ mql get env NEXTSTATE ]
  set sEvent [string tolower [ mql get env EVENT ]]
  set sRecursive [mql get env emxProgramSetting ]

Debug "\n\n emxProgramTriggerPromotionParent.tcl   sRecursive: $sRecursive"
Debug "sType: $sType sName: $sName sEvent: $sEvent"
Debug "OBJECT current = $sObjectCurrentState, next = $sObjectNextState sEvent: $sEvent"

  # Get the objects policy

  set sCmd {mql print bus "$sObjectId" select policy dump}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
    set sErrMsg "$outStr"
    mql notice "Trigger emxProgramTriggerPromotionChild.tcl failure.\n $sCmd \n$sErrMsg" 
    exit 1
    return 1
  }
  # Get the Active, Complete, and Assign state property values from the
  # objects policy
  set sPolicy "$outStr"


  set sActivePropVal "Active"
  set sCmd {mql print policy "$sPolicy" select property\[state_Active\].value dump}
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
    set sActivePropVal "$outStr"
  } 

  set sReviewPropVal "Review"
  set sCmd {mql print policy "$sPolicy" select property\[state_Review\].value dump}
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
    set sReviewPropVal "$outStr"
  } 

  set sCompletePropVal "Complete"
  set sCmd {mql print policy "$sPolicy" select property\[state_Complete\].value dump}
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
    set sCompletePropVal "$outStr"
  } 
 

  set mqlret 0
  set outStr ""

  # Get the objects states
  set sCmd {mql print bus "$sObjectId" select state dump |}
  set mqlret [catch {eval $sCmd} outStr]
  set lObjectStates {}; 
  if {$mqlret != 0} {
      set sErrMsg "$outStr"
  } else {
      set lObjectStates [split $outStr |]
 }

 if {$mqlret == 0 &&  [llength $lObjectStates] > 0  } {
      # Get the position in the state list of the objects next state after being
      # promoted
      set iObjectPos [lsearch $lObjectStates "$sObjectNextState"]

      # Expand the object to get its Parent Task or Project to promote the
      # parrent if all of the siblings are at the same state
      set sCmd {mql expand bus "$sObjectId" to rel "$sRelSubtask" terse select bus name current state dump |}
      set mqlret [catch {eval $sCmd} outStr]
      set lParent {}; 
      if { $mqlret != 0 } {
           set sErrMsg $outStr
      } else {
           set lParent [split $outStr |]
      }
     if {$mqlret == 0 &&  [llength $lParent] > 0  } {
           # Get the Parent objects current state and states
  
           set sParentName [lindex $lParent 4]
           set sParentCurrentState [lindex $lParent 5]
           set lParentStates [lrange $lParent 6 end]
   
           if { "$sEvent" == "promote"  } {  
                 set sParentNextState  [lindex  $lParentStates  [expr [lsearch $lParentStates $sParentCurrentState]  +1] ]
            } else { 
                 set sParentNextState  [lindex  $lParentStates  [expr [lsearch $lParentStates $sParentCurrentState]  -1] ]
             } 

Debug "sParentName: $sParentName sParentCurrentState: $sParentCurrentState   sParentNextState: $sParentNextState"
Debug "parent object current state pos = $sParentCurrentState"

           # Check if the Parent state list is the same as the objects state list
           if { "$sParentCurrentState" == "$sObjectCurrentState"  ||   "$sCompletePropVal"  == "$sObjectNextState" } {
                # Parent and object state names are the same,
                # so check if the Parents state is prior to the objects state

                # Get the position of the parent objects current state in the
                # list of states
                set iParentPos [lsearch $lParentStates "$sParentCurrentState"]
Debug "parent object current state pos = $iParentPos"

                if { ($iParentPos <= $iObjectPos && "$sEvent" == "promote" ) || \
                     ($iParentPos >= $iObjectPos && "$sEvent" == "demote")
                   } {
                     # Parent state is prior to object state, so get the Parent
                     # objects subtasks which are the siblings of the business object
                     set sParentObjectId [lindex $lParent 3]

                     # Expand the Parent object to get its Child Tasks or
                     # Projects (siblings of the object)
                     set sCmd {mql expand bus "$sParentObjectId" from rel "$sRelSubtask" terse select bus name current state dump |}
Debug "\t sCmd: $sCmd"
                     set mqlret [catch {eval $sCmd} outStr]

                     if { $mqlret != 0 } {
                          set sErrMsg $outStr
                     } else {

                          # Initialize a flag that will indicate whether or not
                          # to promote the parent object
                          set iPromoteParent 0
                          set iAllComplete 0 

                          # Go through list of child sibling objects, get the
                          # siblings current state, and create a list of state
                          # names
                          set lAllSiblings [split $outStr \n]
Debug "lAllSiblings: $lAllSiblings" 
                          set iParentPos [lsearch $lParentStates "$sParentCurrentState"]
                          foreach sibling $lAllSiblings {
                             set lSibling [split $sibling |]
                             set sChildName [lindex $lSibling 4]
                             set sChildCurrentState [lindex $lSibling 5]
                             set lChildStates [lrange $lSibling 6 end]
                             set iChildPos1 [lsearch $lChildStates "$sChildCurrentState"];
                             set iChildPos2 [lsearch $lChildStates "$sCompletePropVal"];
                             set sChildNextState [lindex $lChildStates [expr $iChildPos1 + 1] ];
                              if {  "$sChildCurrentState" == "$sCompletePropVal"  && [lindex $lSibling 3] != "$sObjectId" } {  
                                   incr  iAllComplete
                              } 
Debug "\t sChildName: $sChildName iChildPos: $iChildPos1   <  $iChildPos2     $iParentPos"


                             # Skip the current sibling object if its the actual
                             # object that fired this trigger,
                             # Check the object ids

                             if { [lindex $lSibling 3] == "$sObjectId" } {
                                 if {  "$sObjectNextState" == "$sCompletePropVal"  } {  
                                    incr  iAllComplete
                                 } 
                                  continue
                             }
                          
                             # Child and parent state names are the same,
                             # so check if the current sibling objects state
                             # is the same as the objects state after
                             # being promoted (next state)
                             if { "$sEvent" == "promote" && $iChildPos1 <= $iParentPos  &&  ("$sObjectNextState" == "$sCompletePropVal"  || "$sObjectNextState" == "$sReviewPropVal" )   } { 
                                  set iPromoteParent 1
                                  break
                              }

                             if { $sChildCurrentState != $sParentCurrentState && \
                                  $iChildPos1 <= $iParentPos && \
                                  "$sEvent" == "promote"  && "$sObjectNextState" == "$sCompletePropVal"  } {
                              # All siblings are not at the same state
                              # as the object that fired this trigger,
                              # so set the iPromoteFlag to 1 to indicate
                              # not to promote the parent object
                                  set iPromoteParent 1
                                  break
                              }

                          }; # foreach

                          # Promote the parent if iPromoteFlag is 0


#----  Overwrite all the other settings - Check for valid promotes/demotes of parent
#If the object is going into the complete state
# only allow the parent to be promoted if all children
# are completed - overwrite the rules above. 
Debug "All children are complete?  [llength $lAllSiblings] == $iAllComplete " 
if { "$sEvent" == "promote"  && "$sObjectNextState" == "$sCompletePropVal" } { 
  if {  [llength $lAllSiblings] == $iAllComplete  && [llength $lAllSiblings]  > 0  } {
       set iPromoteParent 0
  } else { 
       set iPromoteParent 1
  } 
} 


# Do not promote any parents if the next state is Review
if { $iPromoteParent == 0  && "$sEvent" == "promote"  && "$sObjectNextState" == "$sReviewPropVal"  } {
 set iPromoteParent 1
} 

# Do not promote any parents if 
# If parent is already in the equal or greater state, then don't promote 
if { $iPromoteParent == 0  && $iObjectPos <= $iParentPos  && "$sEvent" == "promote" } {
 set iPromoteParent 1
} 

# If parent  is currently in the Review page
if { $iPromoteParent == 0  && "$sEvent" == "promote"  && "$sParentCurrentState" == "$sReviewPropVal"  } {
 set iPromoteParent 1
} 

#Do not demote the parent if the parent is already at a state less than the object 
if { $iPromoteParent == 0  && $iObjectPos >= $iParentPos  && "$sEvent" == "demote" } {
 set iPromoteParent 1
} 

#Do not demote the parent if the parent is already at state Active 
if { $iPromoteParent == 0  &&  "$sParentCurrentState" == "$sActivePropVal" && "$sEvent" == "demote" } {
 set iPromoteParent 1
} 



# Do not demote parents at all 
# if { "$sEvent" == "demote"  && "$sObjectNextState" == "$sReviewPropVal"  } {
#  set iPromoteParent 1
# } 


                          if { $iPromoteParent == 0   } {
Debug "Promoting the parent object"
                               pushShadowAgent
                                mql set env RECURSIVE "1"; 
                               set sCmd {mql $sEvent bus $sParentObjectId}
                               set mqlret [catch {eval $sCmd} outStr]
                               popShadowAgent
                               if { $mqlret != 0 } {
                                    set sErrMsg $outStr
                              }
                          }; # if
                     }; # mqlret == 0
                }; # iParentPos < iObjectPos
           }; # lParentStates == $lObjectStates
      }; # mqlret == 0
  }; # mqlret == 0


  if { $mqlret != 0 } {
       regsub -all {\"} $sErrMsg {'} sErrMsg
       mql notice "$sErrMsg"
  }

  # return $mqlret
   exit $mqlret
}


