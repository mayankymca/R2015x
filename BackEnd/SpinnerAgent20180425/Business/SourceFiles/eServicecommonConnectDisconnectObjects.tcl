###############################################################################
# $RCSfile: eServicecommonConnectDisconnectObjects.tcl.rca $ $Revision: 1.21 $
#
# @progdoc      eServiceConnectDisconnectObjects.tcl
#
# @Brief:       Connects or disconnects two business objects.
#
# @Description: This program is used to connect or disconnect two given objects with a given relationship.
#               If program is executed for connection then it assignes given values to relationship attributes.
#
# @Parameters:  RPE 0 = the name of the program
#               RPE 1 = connect / disconnect flag
#               RPE 2 = from object id
#               RPE 3 = to object id
#               RPE 4 = the property name of relation
#               RPE 5 = the list of attribute property and it's value.
#                       eg : { {"attribute_attr1" "value1"} {"attribute_attr2" "value2"}...}
#
# @Returns:     sucess or failure.
#
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
###############################################################################

tcl;

# Start eval to prevent echo to stdout.
eval {

###############################################################################
#
# Define Global Variables
#
###############################################################################


###############################################################################
#
# Define Procedures
#
###############################################################################

###############################################################################
#
# Procedure:    utLoad
#
# Description:  Procedure to load other tcl utilities procedures.
#
# Parameters:   sProgram                - Tcl file to load
#
# Returns:      sOutput                 - Filtered tcl file
#               glUtLoadProgs           - List of loaded programs
#
###############################################################################

proc utLoad { sProgram } {

  global glUtLoadProgs env

  if { ! [ info exists glUtLoadProgs ] } {
    set glUtLoadProgs {}
  }

  if { [ lsearch $glUtLoadProgs $sProgram ] < 0 } {
    lappend glUtLoadProgs $sProgram
  } else {
    return ""
  }

  if { [ catch {
    set sDir "$env(TCL_LIBRARY)/mxTclDev"
    set pFile [ open "$sDir/$sProgram" r ]
    set sOutput [ read $pFile ]
    close $pFile
  } ] == 0 } { return $sOutput }

  set sOutput [ mql print program '$sProgram' select code dump ]

  return $sOutput
}
# end utload


######################################################################
#
# LOAD MQL/Tcl TOOLKIT LIBRARIES.
#
#####################################################################

eval [ utLoad eServicecommonDEBUG.tcl ]
eval [ utLoad eServiceSchemaVariableMapping.tcl]
eval [ utLoad eServicecommonShadowAgent.tcl]

######################################################################
#
# MAIN
#
######################################################################

# Return found objects in progname
set sProgName [mql get env 0]
set RegProgName   "eServiceSchemaVariableMapping.tcl"

#
# Error handling variables
#
set mqlret 0
set outStr ""

set bFlag       [mql get env 1]
set sObjectId1  [mql get env 2]
set sObjectId2  [mql get env 3]
set sRelation   [eServiceGetCurrentSchemaName  relationship $RegProgName [mql get env 4] ]
set lAttributes [mql get env 5]

set sCmd "mql $bFlag businessobject $sObjectId1 relationship \"$sRelation\" to $sObjectId2 "
foreach lElement $lAttributes {
    set sAttribute [eServiceGetCurrentSchemaName  attribute $RegProgName [lindex $lElement 0]]
    set sValue     [lindex $lElement 1]
    append sCmd "\"$sAttribute\" " "\"$sValue\" "
}

pushShadowAgent
set mqlret [ catch {eval $sCmd} outStr]
popShadowAgent
if {$mqlret == 0} {
    return "0|"
} else {
    return "1|Error: $sProgName - $outStr"
}

}


# End of Module

