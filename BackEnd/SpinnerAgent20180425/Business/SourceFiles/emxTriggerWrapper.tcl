###############################################################################
# $RCSfile: emxTriggerWrapper.tcl.rca $ $Revision: 1.4 $
#
# @progdoc      emxTriggerWrapper.tcl
#
# @progdoc      Copyright (c) 1998, Matrix One, Inc. All Rights Reserved.
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

    set sOutput [ mql print program $sProgram select code dump ]

    return $sOutput
}
# end utload

#************************************************************************
# From mxMultiProgEval to make emxTriggerManager compatible
# START
#************************************************************************
#************************************************************************
# Procedure:   tcl
#
# Description: When a program is executed it starts in mql mode.  The
#              programmer then issues the "tcl" command to go into tcl
#              mode.  The "tcl" command exists in mql mode, but does not
#              exist in tcl mode.  If the mxMultiProgEval.tcl program is
#              used to execute several programs at once, the mode is
#              already set to "tcl" before the other programs are
#              executed.  Therefore, a dummy tcl command must be created
#              to avoid errors.
#
# Parameters:  None.
#
# Returns:     Nothing.
#************************************************************************

proc tcl {} {}

#************************************************************************
# Procedure:   quit
#
# Description: When programs are executed, the programs are always in
#              tcl mode. When a program finishes, it may have a
#              quit statement at the end.  Since the "quit" statement
#              is an mql statement and not a tcl statement, it will
#              issue an error.  Therefore, a dummy quit command must be
#              created to avoid errors.
#
# Parameters:  None.
#
# Returns:     Nothing.
#************************************************************************

proc quit {} {}

#************************************************************************
# Procedure:   exit
#
# Description: The exit command for the evaluated programs must be
#              redefined so that an exit code is stored for the
#              program and mql/tcl execution does not actually stop.
#
# Parameters:  iExitCode - (Optional) The program's exit code.
#                          (Default: 0)
#
# Returns:     Nothing.
#************************************************************************

if {[lsearch [info commands] "exit.emxTriggerWrapper"] >= 0} {
    rename exit.emxTriggerWrapper ""
}
rename exit exit.emxTriggerWrapper

proc exit { { iExitCode 0 } } {
    return $iExitCode
}
# end exit

#************************************************************************
# From mxMultiProgEval to make emxTriggerManager compatible
# END
#************************************************************************

#########################################################################
#
# Load MQL/Tcl utility procedures
#
#########################################################################

  mql verbose off
  # Error Variable
  set sErr 0

  # Create an RPE variable for the program name
  set sProgName [mql get env 16]
  mql set env 0 $sProgName

  # If program is PS trigger manager program
  # then remove new definition of exit.
  if {[string compare "$sProgName" "mxMultiProgEval.tcl"] == 0} {
      rename exit ""
      rename exit.emxTriggerWrapper exit
  }

  # Get code of the program and evaluate it
  set sCmd [ mql print program "$sProgName" select code dump ]
  if {[ catch $sCmd outStr ] == 1} {
      set sErr 1
  }
  if {([string trim "$outStr"] != "") && ($outStr != 0)} {
      set sErr 1
  }

  # remove new definition of exit after use.
  if {[string compare "$sProgName" "mxMultiProgEval.tcl"] != 0} {
      rename exit ""
      rename exit.emxTriggerWrapper exit
  }

  return $sErr
}

