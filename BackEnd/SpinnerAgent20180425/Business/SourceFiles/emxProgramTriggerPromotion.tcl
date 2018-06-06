###############################################################################
#
# $RCSfile: emxProgramTriggerPromotion.tcl.rca $ $Revision: 1.5 $
#
# @progdoc     emxProgramTriggerPromotion.tcl
#
# @Brief:      This trigger will promote/demote Child subtasks upon promotion/demotion
#              of a Project or Task object.
#
# @Description:   For Project and Task objects, checks are performed to determine whether of not to promote
#                          Child/Parent subtasks when a Project or Task object is promoted.
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

Debug "\n\n emxProgramTriggerPromotion.tcl"


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

  # Get the object.
  set sType [ mql get env TYPE ]
  set sName [ mql get env NAME ]
  set sRev  [ mql get env REVISION ]
  set sObjectId  [ mql get env OBJECTID ]
  set sObjectCurrentState [ mql get env CURRENTSTATE ]
  set sObjectNextState [ mql get env NEXTSTATE ]
  set sEvent [string tolower [ mql get env EVENT ]] 

Debug "\n\n emxProgramTriggerPromotion.tcl"
Debug "OBJECT current = $sObjectCurrentState, next = $sObjectNextState sEvent: $sEvent"

  set mqlret 0
  set outStr ""

  # Get the objects states
  set sCmd {mql print bus "$sObjectId" select state dump |}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      set sErrMsg "$outStr"
  } else {
      set lObjectStates [split $outStr |]

      # Get the position in the state list of the objects next state after being promoted
      set iObjectPos [lsearch $lObjectStates "$sObjectNextState"]

Debug "obj states = $lObjectStates"
Debug "object current (next) state pos = $iObjectPos"
      # Expand the object to get its child subtasks
      set sCmd {mql expand bus "$sObjectId" from rel "$sRelSubtask" terse select bus current state dump |}
      set mqlret [catch {eval $sCmd} outStr]

# Debug "Children = $outStr"

      if { $mqlret != 0 } {
           set sErrMsg $outStr
      } else {

           # Go through list of child objects, get the current state, and the list of state names
           set lChildren [split $outStr \n]
           foreach child $lChildren {
               set lChild [split $child |]
               set sChildObjectId [lindex $lChild 3]
               set sChildCurrentState [lindex $lChild 4]
               set lChildStates [lrange $lChild 5 end]
                              
# Debug "child states = $lChildStates   sChildObjectId= $sChildObjectId"


               # Check if the Child state list is the same as the objects state list
               if { "$sObjectCurrentState" == "$sChildCurrentState" } {
                    # Child and object state names are the same, 
                    # so check if the Childs state is prior to the objects state
Debug "sChildObjectId: $sChildObjectId $sObjectCurrentState == $sChildCurrentState"

                    # Get the position of the child objects current state in the list of states
                    set iChildPos1 [lsearch $lChildStates "$sChildCurrentState"];
                    set iChildPos2 [lsearch $lChildStates "$sObjectNextState"];


# Debug "child object current state pos = $iChildPos1"
Debug "\t iChildPos: $iChildPos1   <  iChildPos2     $iObjectPos"

                    if { ($iChildPos1 < $iChildPos2 && "$sEvent" == "promote") || \
                         ($iChildPos1 > $iChildPos2 && "$sEvent" == "demote")
                        } {
                
                          # Child state is prior to object state, so promote the child up to the objects state
                          while { "$sChildCurrentState" != "$sObjectNextState" } {
Debug "\t $sChildCurrentState  != $sObjectNextState"
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
                                      

               } else {       ; # lChildState == lObjectStates
                    set mqlret 1; 
                    set sErrMsg "Can't promote parent until all children are on the same state"
Debug "\n\t $sErrMsg"
                    break;
               }


           }; # foreach child
          

      }; # mqlret == 0

   }; # mqlret == 0
       
  
  if { $mqlret != 0 } {
       regsub -all {\"} $sErrMsg {'} sErrMsg
       Debug "\n\n emxProgramTriggerNotifyMembers.tcl \n$sErrMsg"
       mql notice "$sErrMsg"
  }


  Debug "\n\n emxProgramTriggerNotifyMembers.tcl successfully..."
  return $mqlret

}


