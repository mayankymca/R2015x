###############################################################################
#
# $RCSfile: emxProgramTriggerCheckChildrenStates.tcl.rca $ $Revision: 1.4 $
#
# @progdoc     emxProgramTriggerPromotionChild.tcl
#
# @Brief:      This trigger is a check trigger an ensures that all the children on the specified state
#  before allowing the promotion
#
# @Description:   Prevents the promotion of a Parent until all tasks have reached a certain state
#
# @Parameters: eService Parameter 1 - relationship name  (relationship_Subtask)
#                        eService Parameter 2 - state name for all children to be in  (state_Complete) 
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
  # mql notice $str
}

#--------------------
#  Main Program
#--------------------
eval {

Debug "\n\n emxProgramTriggerCheckChildrenStates.tcl"
  mql verbose off;
  mql quote off;

   # Passed Parameters
  # This parameter should be true or false
  set RelName  [string trim [mql get env 1]]
  set StateName  [string trim [mql get env 2]]

     # Obtain property values
  set output [mql print program eServiceSchemaVariableMapping.tcl select \
    property\[$RelName\].to \
    dump |]

  set output [split $output |]
  foreach {sRelSubtask} $output {break}

  set sRelSubtask [lrange $sRelSubtask 1 end]


  # Get the object.
  set sType [ mql get env TYPE ]
  set sName [ mql get env NAME ]
  set sRev  [ mql get env REVISION ]
  set sPolicy  [ mql get env POLICY ]
  set sObjectId  [ mql get env OBJECTID ]
  set sObjectCurrentState [ mql get env CURRENTSTATE ]
  set sObjectNextState [ mql get env NEXTSTATE ]
  set sEvent [string tolower [ mql get env EVENT ]]
  set sRecursiveCall  [mql get env RECURSIVE]  


  set sErrMsg ""
  set mqlret 0
  set outStr ""

  # Get the objects policy
  set sPolicy [mql print bus "$sObjectId" select policy dump ] 
  set sCmd { mql print policy "$sPolicy" select property\[$StateName\].value dump } 
  set mqlret [catch {eval $sCmd} outStr]
  set StateName $outStr


  set mqlret 0;
  set outStr "";
  set bAllowPromotion "True" ; 


  # Get the objects states
  set sCmd {mql print bus "$sObjectId" select state dump |}
  set mqlret [catch {eval $sCmd} outStr]
  set lObjectStates [split $outStr |]

      # Get the position in the state list of the objects next state after being
      # promoted
      set iObjectPos [lsearch $lObjectStates "$sObjectNextState"]

      # Expand the object to get its child subtasks
      set sCmd {mql expand bus "$sObjectId" from rel "$sRelSubtask" terse   \
            select bus current  "to\[$sRelAssignedTasks\]"  "from\[$sRelSubtask\]"  state dump |}
      set mqlret [catch {eval $sCmd} outStr]

      # Get all Children 
     set lChildren [split $outStr \n]
     
      # A summary task/Project cannot be demoted unless one tasks have been demoted
      if { "$sEvent" == "promote" && [llength $lChildren] > 0 }  {
             foreach child $lChildren {
               set lChild [split $child |]               
               set sChildCurrentState [lindex $lChild 4]
               set lChildStates [lrange $lChild 7 end]
               set iChildPos1 [lsearch $lChildStates "$sChildCurrentState"];
               set iChildPos2 [lsearch $lChildStates "$StateName"];
               puts "$sChildCurrentState   $iChildPos1 <=  $iChildPos2 " 
               if { "$sChildCurrentState"  != "$StateName" && $iChildPos1 <= $iChildPos2  } { 
                  set bAllowPromotion "False" ; 
                  set sErrMsg "emxProgramCentral.ProgramObject.emxProgramTriggerCheckChildrenStates.PromoteMessage"
                  break; 
                } 
             }

           } 
  
    if { $mqlret != 0  || "$bAllowPromotion" == "False"  } {
       set sErrMsg  [mql execute program emxProgramCentralUtil -method getMessage "$sErrMsg" 1 "StateName"  "$StateName"]
       mql notice "$sErrMsg"
      set mqlret 1; 
   }


   Debug "\n\n emxProgramTriggerCheckChildrenStates.tcl  completed....."

   mql set env emxProgramSetting "true"
  # return $mqlret
   exit $mqlret

}

