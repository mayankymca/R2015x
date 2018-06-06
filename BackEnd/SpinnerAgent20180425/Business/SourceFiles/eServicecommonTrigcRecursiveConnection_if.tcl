###############################################################################
# $RCSfile: eServicecommonTrigcRecursiveConnection_if.tcl.rca $ $Revision: 1.17 $
#
# @Library:     Interface for Recursive Connection check.
#
# @Description: check that objects with the same Type and Name as the
#               current object are not connected to the current structure via
#               the specified relationship in the specified direction.
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

###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [utLoad eServiceSchemaVariableMapping.tcl]
eval  [utLoad eServicecommonTrigcRecursiveConnection.tcl]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcRecursiveConnection_if
#
# @Description: check that objects with the same Type and Name as the
#               current object are not connected to the current object via the
#               specified relationship in the specified direction.
#
# @Parameters:  Inputs via RPE:
#                     sType - Object's type
#                     sName - Object's name
#                      sRev - Object's revision
#               [mql env 1] - The relationship to traverse
#               [mql env 2] - The relationship direction "to" or "from"
#               [mql env 3] - Check subtype flag
#                                 1 = Check for objects with same type and name.
#                                 0 = Check for objects with same name.
#               [mql env 4] - Recurse to all flag.
#                                 1 = recurse to all levels
#                                 0 = one level
#
# @Returns:     0 for no recursive connection , 1 otherwise
#
# @Usage:       used as check on promote trigger
#************************************************************************
    mql verbose off
    
    # set program related variables
    set RegProgName "eServiceSchemaVariableMapping.tcl"
    set progname "eServicecommonTrigcRecursiveConnection_if.tcl"
    set mqlret 0
    
    # Get the input arguments
    set sRelationName [string trim [mql get env 1]]
    set sDirection [string tolower [string trim [mql get env 2]]]
    set iSubTypeFlag [string trim [mql get env 3]]
    set iRecursiveFlag [string trim [mql get env 4]]
    
    # Get the TNR of the current object
    set sType [mql get env TYPE]
    set sName [mql get env NAME]
    set sRev [mql get env REVISION]
    
    # Use lookup to ensure that the name of relationship match that defined
    # in the customer's schema.
    set sAbsRelName [eServiceGetCurrentSchemaName relationship $RegProgName $sRelationName]
    
    # set the default for input arguments
    if {$iSubTypeFlag == ""} {
        set iSubTypeFlag 0
    }
    
    if {$iRecursiveFlag == ""} {
        set iRecursiveFlag 0
    }
    
    # check for integrity of input arguments
    if {[regexp  {^1$|^0$} $iSubTypeFlag] == 0} {
        mql notice "Error : $progname - iSubTypeFlag must be 0 or 1"
        set mqlret 1
    }
    
    if {($mqlret == 0) && ([regexp  {^1$|^0$} $iRecursiveFlag] == 0)} {
        mql notice "Error : $progname - iRecursiveFlag must be 0 or 1"
        set mqlret 1
    }
    
    if {($mqlret == 0) && ([regexp {^from$|^to$} $sDirection] == 0 )} {
        mql notice "Error : $progname - sDirection can be either to or from only"
        set mqlret 1
    }
    
    # call eServicecommonTrigcRecursiveConnection
    if {$mqlret == 0} {
        set mqlret [eServicecommonTrigcRecursiveConnection $sType $sName $sRev $sAbsRelName $sDirection $iSubTypeFlag $iRecursiveFlag]
    }
    
    exit $mqlret

}
# end eServicecommonTrigcRecursiveConnection_if

