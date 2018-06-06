###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcCompleteTask_if.tcl.rca $ $Revision: 1.20 $
#
# @libdoc       eServicecommonTrigcCompleteTask_if
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
eval  [ utLoad eServicecommonCompleteTask.tcl ]
eval  [ utLoad eServicecommonShadowAgent.tcl ]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcCompleteTask_if.tcl
#
# @Brief:
#
# @Parameters:
#
# @Returns:     0 for no sucess, 1 otherwise
#
# @Example:
#
# @procdoc
#******************************************************************************


  #
  # Debugging trace - note entry
  #
  set progname      "eServicecommonTrigcCompleteTask_if"
  set RegProgName   "eServiceSchemaVariableMapping.tcl"

  #
  # Get data values from RPE
  #
  set sType     [ mql get env TYPE ]
  set sName     [ mql get env NAME ]
  set sRev      [ mql get env REVISION ]

  mql verbose off

  pushShadowAgent

  set mqlret [eServicecommonCompleteTask $sType $sName $sRev]

  popShadowAgent

  exit $mqlret
}
# end eServicecommonTrigcCompleteTask_if

# End of Module

