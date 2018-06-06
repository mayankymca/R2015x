###############################################################################
# $RCSfile: eServicecommonTrigcAttribute_if.tcl.rca $ $Revision: 1.44 $
#
# @libdoc       eServicecommonTrigcAttribute_if.tcl
#
# @Library:     Interface for attribute checks for triggers
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

#******************************************************************************
# @procdoc      eServicecommonTrigcAttribute_if
#
# @Brief:       check attribute trigger
#
# @Description: interface to eServicecommonTrigcAttribute to compare an attribute
#               value to a default value
#
# @Parameters:  attName  -- attribute name
#               useDefaultvalFlag -- flag to specify comparison method:
#                               0 -- compare against attribute's default
#                               1 -- compare against given attValue
#               attValue -- attribute value to compare object's value to
#
# @Returns:     0 if attribute differs from default
#               1 otherwise
#
# @Usage:       Supports check attribute trigger
#
#
# @procdoc
#******************************************************************************

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

mql verbose off;

# Load MQL/Tcl utility procedures
eval  [utLoad eServiceSchemaVariableMapping.tcl]
eval  [utLoad eServicecommonTrigcAttribute.tcl]
eval  [utLoad eServicecommonTranslation.tcl]

# set program related variables
set RegProgName   "eServiceSchemaVariableMapping.tcl"

# Get the input arguments
set attName             [string trim [mql get env 1]]
set useDefaultvalFlag   [string trim [mql get env 2]]
set attValue            [string trim [mql get env 3]]

# Get the TNR of the current object
set sType       [ mql get env TYPE ]
set sName       [ mql get env NAME ]
set sRev        [ mql get env REVISION ]

# set the default values for args
if {$useDefaultvalFlag == ""} {
     set useDefaultvalFlag   0
}

# Use lookup to ensure that the name of attribute match that defined
# in the customer's schema.
set sAbsAttName    [eServiceGetCurrentSchemaName  attribute  $RegProgName  $attName]

if {$useDefaultvalFlag == 0} {
      set szCommand {mql print attribute "$sAbsAttName" select default dump}
      if {[catch {eval $szCommand} defaultVal] == 0 } {
             set retval [eServicecommonTrigcAttribute  "$sType" "$sName" "$sRev" "$sAbsAttName" "$defaultVal"]
      } else {
         if {"$sAbsAttName" == ""} {
             set sAbsAttName "$attName"
         }
         set sMsg [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigcAttribute_if.Default" 1 \
                                  "name" "$sAbsAttName" \
                    "" \
                    ]
         mql notice "$sMsg"
         set retval 1
      }
} else {
      set retval [eServicecommonTrigcAttribute  "$sType" "$sName" "$sRev" "$sAbsAttName" "$attValue"]
}

exit $retval
}
# end  eServicecommonTrigcAttribute_if

