###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcAutoRouteByNavigation_if.tcl.rca $ $Revision: 1.45 $
#
# @libdoc       eServicecommonTrigcAutoRouteByNavigation_if
#
# @Library:     Interface for required person checks for triggers
#
# @Brief:       Gets person objects through specified navigation path.
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
eval  [ utLoad eServiceSchemaVariableMapping.tcl]
eval  [ utLoad eServicecommonAutoRouteByNavigation.tcl ]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcAutoRouteByNavigation_if.tcl
#
# @Brief:       Do different operation with the person object
#               connected through specified path
#
# @Description: Do different operation with the person object
#               connected through specified path
#
# @Parameters:  sSelect        -- Select statement which gets the user to be routed
#               sOperation     -- Operation to be performed with found person objects.
#                                 ROUTE  -- Assign the person to be owner of the object
#                                           from which trigger has generated and send
#                                           icon mail to that person.
#                                           If more then one person objects are found then
#                                           assign the first person to be owner and send
#                                           mail to all the persons found.
#                                 SEND   -- Send mail to all the persons found.
#                                 ASSIGN -- Same as ROUTE but don't send mail to anyone.
#               bErrNotFound   -- If set to TRUE then program will error out if user doesn't exist.
#               sSubject       -- If operation is ROUTE or SEND then this argument
#                                 specifies subject of the mail to be send.
#               sText          -- mail contents.
#               bPassObj       -- Wheather to pass selected object name into mail.
#                                 TRUE   -- Pass object name into mail
#                                 FALSE  -- Don't pass object name into mail.
#               bNotify        -- Set flag to give confirmation that operation was
#                                 successfully completed.  TRUE or null entry will turn flag
#                                 on, anything else turns flag off.
#               base property file -- Base Name of the property file where subject and text 
#                                     should be looked up.
#                                     If this is not provided
#                                     by default it will look up in
#                                     property files with base name emxFrameworkStringResource.
#
# @Returns:     0 if no person objects found, 1 otherwise
#
# @Example:     Input to eServiceTriggerManager:
#                 {{ eServicecommonTrigcAutoRouteByNavigation_if  {from[relationship_ECR].businessobject.attribute[attribute_ChangeBoard]} {ROUTE} {subject} {contents} }}
#
# @procdoc
#******************************************************************************

  #
  # Debugging trace - note entry
  #
  set progname      "eServicecommonTrigcAutoRouteByNavigation_if"
  set RegProgName   "eServiceSchemaVariableMapping.tcl"

  mql verbose off
  #
  # Get data values from RPE
  #
  set sType        [mql get env TYPE]
  set sName        [mql get env NAME]
  set sRev         [mql get env REVISION]
  set sSelect      [mql get env 1]
  set sOperation   [string toupper [mql get env 2]]
  set bErrNotFound [string toupper [mql get env 3]]
  set sSubject     [mql get env 4]
  set sText        [mql get env 5]
  set bPassObj     [string toupper [mql get env 6]]
  set bNotify      [string toupper [string trim [mql get env 7]]]
  set sBasePropFile [mql get env 8]

  #
  # Error handling variables
  #
  set mqlret 0
  set outStr ""
  if {$sSelect == ""} {
      set sSelect "attribute\[attribute_Originator\]"
  }

  if {$bErrNotFound == ""} {
      set bErrNotFound "TRUE"
  }

  if {$bPassObj == ""} {
      set bPassObj "TRUE"
  }

  if {$bNotify == ""} {
      set bNotify "TRUE"
  }

  if {$sBasePropFile == ""} {
      set sBasePropFile "emxFrameworkStringResource"
  }
  
  # Validate Symbolic Names from select statement
  set sSelectInfo $sSelect
  while {[regexp {\]} $sSelectInfo] != 0} {
      # Get last symbolic name entry from sSelectInfo
      regsub {^(.)*\[} $sSelectInfo "" sNewValues
      regsub {\](.)*$} $sNewValues "" sSymbolicName

      # Get Schema type
      set sSchemaType [lindex [split $sSymbolicName _] 0]

      # Get schema mapping for symbolic name
      set sSchemaMap [eServiceGetCurrentSchemaName $sSchemaType $RegProgName $sSymbolicName]

      # Error out if not registered
      if {$sSchemaMap == ""} {
          return 1
      }

      # Replace symbolic names with real schema names
      regsub -all "$sSymbolicName" $sSelect "$sSchemaMap" sSelect

      # Remove last symbolic entry and redefine sSelectInfo
      regexp {^(.)*\[} $sSelectInfo sSelectInfo
      regsub {\[$} $sSelectInfo "" sSelectInfo
  }

  set mqlret [catch {eval eServicecommonAutoRouteByNavigation {$sType} {$sName} {$sRev} {$sSelect} {$sOperation} {$bErrNotFound} {$sSubject} {$sText} {$bPassObj} {$bNotify} {$sBasePropFile}} outStr]

  return $mqlret
}
# end eServicecommonTrigcRequiredConnection_if


# End of Module

