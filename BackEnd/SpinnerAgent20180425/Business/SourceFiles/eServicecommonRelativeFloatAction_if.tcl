###############################################################################
# $RCSfile: eServicecommonRelativeFloatAction_if.tcl.rca $ $Revision: 1.52 $
#
# @libdoc       eServicecommonRelativeFloatAction_if.tcl
#
#
# @Brief:       Check this objects children prior to promotion of parent.
#
# @Description: This program gets the latest revision for any given object
#               that is at an indicated state
#
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
#******************************************************************************
# @procdoc      eServicecommonRelativeFloatAction_if
#
# @Brief:       Get the latest revision that is at a specified state
#
# @Description:
#               The program eServicecommonRelativeFloatAction_if.tcl is a promote action trigger.
#               When an object is promoted, the program gets the previous revision of the object and
#               floats all the specified relationships in the specified direction.
#
# @Parameters:
#           sType          - The TYPE name whose revisions are to be checked for.
#           sName          - Name of the object whose revisions are to be checked for
#           sRev           - Revision of the object whose revisions are to be looked for.
#           sState         - The state to look for.
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
#
# @procdoc
#*******************************************************************************

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

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utLoad

###############################################################################

mql verbose off;

# Load MQL/Tcl Toolkit Libraries
eval  [utLoad eServicecommonRelativeFloatAction.tcl]
eval  [utLoad eServiceSchemaVariableMapping.tcl]
eval  [utLoad eServicecommonTrigcLastRevAtState.tcl]
eval  [utLoad eServicecommonShadowAgent.tcl]
eval  [utLoad eServicecommonCheckStatePosition.tcl]
eval  [utLoad eServicecommonTranslation.tcl]

  # Set name of program
  set progname "eServicecommonRelativeFloatAction_if.tcl"

  set mqlret 0
  set outstr ""

  set sRel              [string trim [mql get env 1]]
  set sDirection        [string tolower [string trim [mql get env 2]]]
  set sOperation        [string toupper [string trim [mql get env 3]]]
  set sState            [string trim [mql get env 4]]

  # Grab the parent object's "name" to pass into eServiceCheckRelState
  set sParentType     [ mql get env TYPE ]
  set sParentName     [ mql get env NAME ]
  set sParentRev      [ mql get env REVISION ]
  set RegProgName     "eServiceSchemaVariableMapping.tcl"

  # get the absolute name of relationship
  set sRel    [eServiceGetCurrentSchemaName  relationship  $RegProgName  $sRel]

  # get the policy attached to current object
  if {$mqlret == 0} {
     set sCmd {mql print bus "$sParentType" "$sParentName" "$sParentRev" select policy dump}
     set mqlret [catch {eval $sCmd} outstr]

     set sPolicy [string trim $outstr]
  }

  # get the absolute name of the input symbolic state name, based on the policy
  if {$mqlret == 0} {
       set sState [eServiceLookupStateName  $sPolicy  $sState]
       if {[string trim $sState] == ""} {
            set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonRelativeFloatAction_if.UnDefinedState" 1 \
                                  "Program" "$progname" \
                                "" \
                                ]
            set mqlret 1
       }
  }

  if {$mqlret == 0} {
     set mqlret  [eServicecommonRelativeFloatAction $sParentType $sParentName $sParentRev $sRel $sDirection $sOperation $sState]
  } else {
     mql notice "$progname :\n$outstr"
  }

  # this is done to rollback a transaction in case of error, because enclosing
  # the code in a transaction boundary is not possible, as there is no way to
  # distinguish whether a transaction is started by matrix or another tcl program
  # hence starting a transaction, if already started would end in rolling back the
  # whole transaction, if not started then the abort transaction would give the
  # same effect
  if {$mqlret != 0} {
       set sCmd {mql start transaction}
       catch {eval $sCmd} outstr
       set sStatus [string trim [lindex [split [mql print transaction]] 1]]
       if {$sStatus != "inactive"} {
            mql abort transaction
       }
  }

  exit $mqlret
}
## end eServicecommonRelativeFloatAction_if

