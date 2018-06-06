###############################################################################
#
# $RCSfile: emxProgramTriggerApproveConceptProject.tcl.rca $ $Revision: 1.7 $
#
# @progdoc     emxProgramTriggerApproveConceptProject.tcl
#
# @Brief:      This trigger will change the type of the concept project after copy.
#
# @Description:   copy the project concept and vaults, then change type
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
# 04.24.02 D.E
#  Change the default type Project to ProjectSpace
###############################################################################


tcl;

#-----
# Procedure Section
#-----

proc Debug { str } {
 # puts $str
}


#------------------------------------------------------
#
# Procedure Name:  ModifyAttr
#
# Purpose:  Modify an attribute of an object
#
# Parameters:  sObjectId - object id to modify
#                      sAttrName - attribute to modify
#                      sAttrVal - value to modify with
#
# Returns: 0 - successful
#               1 - unsuccessful
#------------------------------------------------------
proc ModifyAttr { sObjectId sAttrName sAttrVal } {

     global outStr

     # Modify the objects attribute to the attribute value
     set sCmd {mql modify bus "$sObjectId" "$sAttrName" "$sAttrVal" }
     pushShadowAgent
     set mqlret [catch {eval $sCmd} outStr]
     popShadowAgent

     return $mqlret
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
  set sAttrPercentComplete [mql get env attribute_PercentComplete]
  set sAttrCurrency [mql get env attribute_Currency]  
  set sPrefCurrency property\[preference_Currency\].value;

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

  # Set the date format
  #set MXDATE_FORMAT "%a %b %d, %Y %I:%M:%S %p"
  set MXDATE_FORMAT "%a %b %d, %Y"
  set sSystemDate [ clock format [ clock seconds ] -format $MXDATE_FORMAT ]


Debug "\n\n emxProgramTriggerPromotion.tcl"
Debug "OBJECT current = $sObjectCurrentState, next = $sObjectNextState sEvent: $sEvent"

  set mqlret 0
  set outStr ""
  set sErrMsg ""

   # D.E. 04.24.02
  set sProjectType "[mql get env type_ProjectSpace]";
  set sProjectDfltPolicy "[mql get env policy_ProjectSpace]";

  # Get the objects states
  set sCmd {mql modify bus "$sObjectId" type "$sProjectType" policy $sProjectDfltPolicy  \
     "$sAttrPercentComplete" "";
     }
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      set sErrMsg "$outStr"
  }

  Debug "Approval Successful?  mqlret: $mqlret  sErrMsg: $outStr"


  if { $mqlret != 0 } {
       regsub -all {\"} $sErrMsg {'} sErrMsg
       mql notice "$sErrMsg"
  }

  # Set user's preffered currency as Project Space base currency.  
  set getUserCmd {mql print context select user dump;}
  set sUsername [ eval $getUserCmd ]
  puts "$sUsername"
  set getUserCurrencyCmd {mql list person "$sUsername" select "$sPrefCurrency" dump ;}
  set sPreferredCurrency [ eval $getUserCurrencyCmd ] 

  # If user currency is "As Entered" or "Unassigned", base currency will be Dollar.
  if {$sPreferredCurrency=="" || $sPreferredCurrency=="As Entered" || $sPreferredCurrency=="Unassigned"} {
      set sProjectCurrencyCmd  {mql modify bus "$sObjectId" "$sAttrCurrency" "Dollar";}
      set result [ eval $sProjectCurrencyCmd ]
  } else {
    set sProjectCurrencyCmd  {mql modify bus "$sObjectId" "$sAttrCurrency" "$sPreferredCurrency";}
    set result [ eval $sProjectCurrencyCmd ]
  }

  return $mqlret
}

