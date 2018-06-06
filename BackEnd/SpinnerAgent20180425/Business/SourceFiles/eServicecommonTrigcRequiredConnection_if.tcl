###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcRequiredConnection_if.tcl.rca $ $Revision: 1.20 $
#
# @libdoc       eServicecommonTrigcRequiredConnection_if
#
# @Library:     Interface for required connection checks for triggers
#
# @Brief:       Compare specified attribute to its default value
#
# @Description: see procedure description
#
# @libdoc       Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# The following sample code is provided for your reference purposes in
# connection with your use of the Matrix System (TM) software product
# which you have licensed from MatrixOne, Inc. ("MatrixOne").
# The sample code is provided to you without any warranty of any kind
# whatsoever and you agree to be responsible for the use and/or incorporation
# of the sample code into any software product you develop. You agree to fully
# and completely indemnify and hold MatrixOne harmless from any and all loss,
# claim, liability or damages with respect to your use of the Sample Code.
#
# Subject to the foregoing, you are permitted to copy, modify, and distribute
# the sample code for any purpose and without fee, provided that (i) a
# copyright notice in the in the form of "Copyright 1995 - 1998 MatrixOne Inc.,
# Two Executive Drive, Chelmsford, MA  01824. All Rights Reserved" appears
# in all copies, (ii) both the copyright notice and this permission notice
# appear in supporting documentation and (iii) you are a valid licensee of
# the Matrix System software.
#
###############################################################################

tcl;
eval {

###############################################################################
#
# Define Global Variables
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

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utload


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [ utLoad eServicecommonDEBUG.tcl ]
eval  [ utLoad eServiceSchemaVariableMapping.tcl]
eval  [ utLoad eServicecommonRequiredConnection.tcl ]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcRequiredConnection_if.tcl
#
# @Brief:       Check that at least one object is connected via the
#               indicated relatonship
#
# @Description: Check that at least one object is connected via the
#               indicated relatonship
#
# @Parameters:  direction    -- direction to traverse (to/from)
#               relationList -- relationships to traverse
#                               eg : $relationship_rel1,$relationship_rel2,...
#               typeList     -- types to traverse
#                               eg : $type_type1,$type_type2,..
#
# @Returns:     0 for no connection, 1 otherwise
#
# @Example:     Input to mxTrigManager:
#                 {{ eServicecommonTrigcRequiredConnection_if  {to/from/both} {$relationship_rel1,$relationship_rel2,..} {$type_type1,$type_type2,...} }}
#
# @procdoc
#******************************************************************************


  #
  # Debugging trace - note entry
  #
  set progname      "eServicecommonTrigcRequiredConnection_if"
  set RegProgName   "eServiceSchemaVariableMapping.tcl"
  mxDEBUGIN "$progname"

  #
  # Get data values from RPE
  #
  set sType     [ mql get env TYPE ]
  set sName     [ mql get env NAME ]
  set sRev      [ mql get env REVISION ]
  mql verbose off;
  set direction     [mql get env 1]
  set relationList  [mql get env 2]
  set typeList      [mql get env 3]
  #
  # Error handling variables
  #
  set mqlret 0
  set outStr ""
  if {$direction == "NULL"} {
      set sDirection ""
  } else {
      set sDirection $direction
  }

  if {$relationList == "NULL"} {
      set sRelationList ""
  }  else {
      set lRel {}
      set lTemp [split $relationList ","]
      foreach sItem $lTemp {
          lappend lRel [eServiceGetCurrentSchemaName relationship $RegProgName $sItem ]
      }
      set sRelationList [join $lRel ","]
  }

  if {$typeList == "NULL"} {
      set sTypeList ""
  }  else {
      set lType {}
      set lTemp [split $typeList ","]
      foreach sItem $lTemp {
          lappend lType [eServiceGetCurrentSchemaName type $RegProgName $sItem ]
      }
      set sTypeList [join $lType ","]
  }

  set mqlret [catch {eval eServicecommonRequiredConnection {$sType} {$sName} {$sRev} {$sDirection} {$sRelationList} {$sTypeList}} outStr]

  #
  # Debugging trace - note exit
  #
  mxDEBUGOUT "$progname"

  #return -code $mqlret "$outStr"
  exit $mqlret
}
# end eServicecommonTrigcRequiredConnection_if


# End of Module

