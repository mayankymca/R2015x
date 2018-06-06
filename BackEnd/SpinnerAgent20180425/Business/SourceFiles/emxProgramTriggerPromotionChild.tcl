###############################################################################
#
# $RCSfile: emxProgramTriggerPromotionChild.tcl.rca $ $Revision: 1.9 $
#
# @progdoc     emxProgramTriggerPromotionChild.tcl
#
# @Brief:      This trigger will promote/demote Child subtasks upon promotion/demotion
#              of a Project or Task object.
#
# @Description:   For Project and Task objects, checks are performed to determine
#                          whether of not to promote Child subtasks when a
#                          Project or Task object is promoted.
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

Debug "\n\n emxProgramTriggerPromotionChild.tcl"
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

  #-- D.E. Check to see if Task has been assigned
  set sRelAssignedTasks [mql get env relationship_AssignedTasks]

  #-- D.E. Check to state dependent routes are assigned to the next state 
  set sPolRoute [mql get env policy_Route]


  # Get the object.
  set sType [ mql get env TYPE ]
  set sName [ mql get env NAME ]
  set sRev  [ mql get env REVISION ]
  set sObjectId  [ mql get env OBJECTID ]
  set sObjectCurrentState [ mql get env CURRENTSTATE ]
  set sObjectNextState [ mql get env NEXTSTATE ]
  set sEvent [string tolower [ mql get env EVENT ]]
  set sRecursiveCall  [mql get env RECURSIVE]  

Debug "\n   sRecursiveCall: $sRecursiveCall  " 
  if { "$sRecursiveCall" == "1" } { 
    return 0;
    exit 0; 
  } 


Debug "sType: $sType sName: $sName \n OBJECT current = $sObjectCurrentState, next = $sObjectNextState sEvent: $sEvent"

  set sErrMsg ""
  set mqlret 0
  set outStr ""

  # Get the objects policy

  set mqlret [catch {mql print bus $sObjectId select policy dump} outStr]
  if {$mqlret != 0} {
    set sErrMsg "$outStr"
    mql notice "Trigger emxProgramTriggerPromotionChild.tcl failure.\n $sCmd \n$sErrMsg" 
    exit 1
    return 1
  }

  # Get the Active, Complete, and Assign state property values from the
  # objects policy
  set sPolicy "$outStr"

 
  set sCreatePropVal "Create"
  set sCmd {mql print policy "$sPolicy" select property\[state_Create\].value dump}
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
     set sCreatePropVal "$outStr"
  } 

  
  set sAssignPropVal "Assign"
  set sCmd {mql print policy "$sPolicy" select property\[state_Assign\].value dump}
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
     set sAssignPropVal "$outStr"
  } 

   
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


#11/1 D.E. Route Dependency Stuff
 set sRouteCompleteStateName "Complete"
  set sCmd {mql print policy "$sPolRoute" select property\[state_Complete\].value dump}
  set mqlret [catch {eval $sCmd} outStr]
  if { $mqlret == 0 } { 
      set sRouteCompleteStateName "$outStr"
  } 
     
Debug "\t sReviewPropVal: $sReviewPropVal sActivePropVal: $sActivePropVal"
Debug "\t sCompletePropVal: $sCompletePropVal  sAssignPropVal: $sAssignPropVal"



  set mqlret 0;
  set outStr "";
  set bAllowPromotion "True" ; 


  # Get the objects states
  set sCmd {mql print bus "$sObjectId" select state dump |}
  set mqlret [catch {eval $sCmd} outStr]



  if {$mqlret != 0} {
      set sErrMsg "$outStr"
  } else {
      set lObjectStates [split $outStr |]

      # Get the position in the state list of the objects next state after being
      # promoted
      set iObjectPos [lsearch $lObjectStates "$sObjectNextState"]

      # Expand the object to get its child subtasks
      set sCmd {mql expand bus "$sObjectId" from rel "$sRelSubtask" terse   \
            select bus current  "to\[$sRelAssignedTasks\]"  "from\[$sRelSubtask\]"  state dump |}
      set mqlret [catch {eval $sCmd} outStr]
Debug "Expand children: $outStr" ;

      if { $mqlret != 0 } {
           set sErrMsg $outStr
           set outStr ""; 
      } 

      # only if expand returned children 
      if  { $outStr != "" } { 


          # Go through list of child objects, get the current state, and the
          # list of state names
          set lChildren [split $outStr \n]
     

Debug  "#1  sObjectCurrentState: $sObjectCurrentState    sEvent: $sEvent   "
          # A summary task/Project cannot be demoted unless one tasks have been demoted
          if { $sObjectCurrentState == $sCompletePropVal && \
                "$sEvent" == "promote" && \
                 [llength $lChildren] > 0 }  {
             foreach child $lChildren {
               set lChild [split $child |]               
               set sChildCurrentState [lindex $lChild 4]
               set lChildStates [lrange $lChild 7 end]
               set iChildPos1 [lsearch $lChildStates "$sChildCurrentState"];
               set iChildPos2 [lsearch $lChildStates "$sCompletePropVal"];
               if { "$sChildCurrentState"  != "$sCompletePropVal" && $iChildPos1 < $iChildPos2  } { 
                  set bAllowPromotion "False" ; 
                  set sErrMsg "A completed project or summary task cannot be promoted to the next state."
                  append sErrMsg "\nAt least one child must be demoted first"
                  break; 
                } 
             }

           } 

Debug  "#2  sObjectNextState: $sObjectNextState    sEvent: $sEvent   "

           # All Children must be completed before a project or summary task can be promoted
           if { $sObjectNextState == $sCompletePropVal && "$sEvent" == "promote" } {
             foreach child $lChildren {
               set lChild [split $child |]               
               set sChildCurrentState [lindex $lChild 4]
               if { $sChildCurrentState  != "$sCompletePropVal" } { 
                  set bAllowPromotion "False" ; 
                  set sErrMsg "All children must be in the Complete state before a parent can be promoted";
                  break; 
               }  
             }

           } 




Debug  "#3 sObjectNextState: $sObjectNextState    sEvent: $sEvent   "
           # At least one child must be Children must be completed before a project or summary task can be promoted
           if { $sObjectNextState == $sReviewPropVal && "$sEvent" == "promote" } {
             foreach child $lChildren {
               set lChild [split $child |]               
               set sChildCurrentState [lindex $lChild 4]
               if { $sChildCurrentState  != "$sCompletePropVal" } { 
                  set bAllowPromotion "False" ; 
                  set sErrMsg "All children must be in the '$sObjectNextState' state before a parent can be promoted";
                  break; 
               } 
             }

           } 


Debug  "bAllowPromotion: $bAllowPromotion    sErrMsg: $sErrMsg "



    if { "$bAllowPromotion" ==  "True"  || ("$sCreatePropVal" == "$sObjectCurrentState" && "$sEvent" == "promote" ) } {   
           # Now go ahead and promote or demote the children
           foreach child $lChildren {
               set lChild [split $child |]
               set sChildObjectId [lindex $lChild 3]
               set sChildCurrentState [lindex $lChild 4]
               set bAssigned  [lindex $lChild 5]
               set bSummaryTask  [lindex $lChild 6]
               set lChildStates [lrange $lChild 7 end]
               set bPromote "false"; 
    
               # if the task has been assigned or the task is a summary task then promote 
               if {  "$bSummaryTask" == "True"  && "$bAssigned" == "False" }  {  
                     set bPromote "true"; 
               }  elseif { "$bAssigned" == "True"  } {
                     set bPromote  "true"; 
               }  

               # Check if the Child state list is the same as the objects state
  
Debug  "sObjectCurrentState: $sObjectCurrentState    sChildCurrentState: $sChildCurrentState  bAssigned: $bAssigned   bSummaryTask: $bSummaryTask  bPromote: $bPromote"

               if { "$sObjectCurrentState" == "$sChildCurrentState" && "$bPromote" == "true"  } { 


                    # Child and object state names are the same,
                    # so check if the Childs state is prior to the objects state

                    # Get the position of the child objects current state in the
                    # list of states
                    set iChildPos1 [lsearch $lChildStates "$sChildCurrentState"];
                    set iChildPos2 [lsearch $lChildStates "$sObjectNextState"];
                    set sChildNextState [lindex $lChildStates [expr $iChildPos1 + 1] ];

                    if { ($iChildPos1 < $iChildPos2 && "$sEvent" == "promote")  || \
                         ($iChildPos1 > $iChildPos2 && "$sEvent" == "demote" && \
                          "$sChildCurrentState" != "$sReviewPropVal" &&  \
                          "$sChildCurrentState"  != "$sCompletePropVal")
                        } {
                          # Child state is prior to object state, so promote the
                          # child up to the objects state
                          while { "$sChildCurrentState" != "$sObjectNextState" } {
                                pushShadowAgent
                                set sCmd {mql $sEvent bus $sChildObjectId }
                                set mqlret [catch {eval $sCmd} outStr]
                                popShadowAgent

                                if { $mqlret != 0 } {
                                     set sErrMsg $outStr
                                     break
                                }
                               set sChildCurrentState [mql print bus $sChildObjectId select current dump;]
                          }; # while
                        }; # iChildPos1 < iChildPos2
               } 
           }; # foreach child
        }; 
      }; # mqlret == 0
  }; # mqlret == 0


  if { $mqlret != 0  || "$bAllowPromotion" == "False"  } {
       regsub -all {\"} $sErrMsg {'} sErrMsg
       mql notice "mqlret: $mqlret $sErrMsg"
      set mqlret 1; 
  }


  Debug "\n\n emxProgramTriggerPromotionChild.tcl  completed....."

  mql set env emxProgramSetting "true"
  # return $mqlret
   exit $mqlret

}


