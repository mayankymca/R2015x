###############################################################################
# $RCSfile: eServicecommonWzrdBusNames.tcl.rca $ $Revision: 1.18 $
#
# @progdoc      eServicecommonWzrdBusNames.tcl
#
# @Brief:       List business objects of a given type.
#
# @Description: This file defines the code for a Wizard program to load the
#               names of business objects of a given type.
#
# @Parameters:  RPE 0 = the name of the variable (widget) into which
#                       this function will put the list of found objects
#               RPE 2 = the Type of objects for listing names
#               RPE 3 = if defined, a list of Policy/State pairs,
#                       e.g., { {Policy1 State1} {Policy2 State2}...}
#
# @Returns:     RPE variable identified in RPE 0 is set to a list of found objects.
#
# @Usage:       This program should be implemented as a widget
#               validate program.
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

######################################################################
#
# MAIN
#
######################################################################

# Return found objects in progname
set progname [mql get env 0]
set RegProgName   "eServiceSchemaVariableMapping.tcl"
#
# Debugging trace - note entry
#
mxDEBUGIN "$progname"

#
# Error handling variables
#
set iExit 0
set iErr 0
set outStr ""

#
# Lookup admin names
#
set type ""
set property [mql get env 2]
set type     [eServiceGetCurrentSchemaName  type $RegProgName  $property ]


#
# Get Policy:State
#
set orig_policy ""
set orig_state ""
set policy ""
set state ""
set policy_states [mql get env 3]

#
# Must have at least type name rev
#
if { $type == "" } {
  set queryFields "* * *"
} else {
  set type [join $type]
  set queryFields [ list $type "*" "*" ]
}

#
# Create and evaluate temp query command
#
set busObjs {}
set iErr [catch {eval mql temp query bus $queryFields select dump |} outStr]
if { $iErr != 0 } {
  set iExit $iErr
  set iErr 0
  DEBUG "Error ($progname): $outStr"
} else {
  set busObjs [split $outStr '\n']
  set outStr {}
}

#
# Process Business Objects
#
set namelist {}
set name {}
foreach one $busObjs {
  set bus [split $one '|']
  set type [lindex $bus 0]
  set name [lindex $bus 1]
  set rev [lindex $bus 2]

  if { $policy_states == "" } {
    if { $rev == "" } {
      lappend namelist "$name"
    } else {
      lappend namelist "$name|$rev"
    }
    continue
  }

  #
  # Handle policy states
  #
  set busPolicy [mql print bus $type $name $rev select policy dump]
  set busState [mql print bus $type $name $rev select current dump]
  set found FALSE
  foreach ps $policy_states  {
    set ind [string first ":" $ps ]
    if { $ind <= 0 } {
      continue
    }

    if { [string index $ps [expr $ind + 1]] != ":" } {
      continue
    }
    set orig_policy [string range $ps 0 [expr $ind - 1] ]
    set orig_state [string range $ps [expr $ind + 2] end ]
    set policy [lookup policy $orig_policy]
    set state [lookup state "$orig_policy|$orig_state"]
    if { $policy != $busPolicy } {
      continue
    }
    if { $state != $busState } {
      continue
    }
    set found TRUE
    break;
  }

  if { $found == "TRUE" } {
    if { $rev == "" } {
      lappend namelist "$name"
    } else {
      lappend namelist "$name|$rev"
    }
  }
}
DEBUG "namelist = $namelist"

#
# Return found objects through RPE variable
#
mql set env $progname "$namelist"

#
# Debugging trace - note exit
#
mxDEBUGOUT "$progname"

exit $iExit
# End eval to prevent echo to stdout.
}


# End of Module

