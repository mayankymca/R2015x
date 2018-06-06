###############################################################################
# $RCSfile: eServicecommonTrigcRelAttribute_if.tcl.rca $ $Revision: 1.43 $
#
# @libdoc       eServicecommonTrigcRelAttribute_if.tcl
#
# @Library:     Interface for Validate Attribute Value on Relationship
#
# @Brief:
#
# @Description: Compares an attribute value on a relation to a default value
#               or specified value.  Also, checks for unique values.
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
eval  [utLoad eServicecommonTrigcRelAttribute.tcl]
eval  [utLoad eServicecommonTranslation.tcl]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcRelAttribute_if
#
# @Brief:
#
# @Description: Compares an attribute value on a relation to a default value
#               or specified value.  Also, checks for unique values.
#
# @Parameters:  Inputs via RPE:
#                     sType - Object's type
#                     sName - Object's name
#                      sRev - Object's revision
#               [mql env 1] - The relationship to traverse
#               [mql env 2] - Direction to travers, must be to or from.
#               [mql env 3] - Name of the attribute to check on rel
#               [mql env 4] - Attribute flag
#                                 0 = Don't check attribute value (Default)
#                                 1 = Check attribute value against default
#                                     value for attribute.
#                                 2 = Check attribute value against value
#                                     passed in to [mql env 5]
#               [mql env 5] - Value to check against if [mql env 4] is 2
#               [mql env 6] - Unique flag
#                                 0 = Don't check that values are unique (Default)
#                                 1 = Check that values are unique
#
# @Returns:     return 0 if connection's attribute value(s) do not satisfy
#                         attribute value check(s).
#                      1 otherwise
#
# @Usage:       use as promote check trigger
# @procdoc
#******************************************************************************
    mql verbose off

    # set program related variables
    set sRegProgName "eServiceSchemaVariableMapping.tcl"
    set mqlret 0
    set outstr ""

    # get input arguments
    set sRelationName [string trim [mql get env 1]]
    set sDirection [string tolower [string trim [mql get env 2]]]
    set sAttrName [string trim [mql get env 3]]
    set iAttrFlag [string trim [mql get env 4]]
    set sAttrValue [string trim [mql get env 5]]
    set iUniqueFlag [string trim [mql get env 6]]

    # get TNR of the object
    set sType [mql get env TYPE]
    set sName [mql get env NAME]
    set sRev [mql get env REVISION]

    # Use lookup to check symbolic name of relationship and attribute
    set sAttrName [eServiceGetCurrentSchemaName attribute $sRegProgName $sAttrName]

    if {$sAttrName == ""} {
        # Error out if not registered
     	set mqlret 1

     	# Give error message
        set outstr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigcRelAttribute_if.InvalidAtt" 1 \
                                  "Name" "$sAttrName" \
                    "" \
                    ]
     	mql notice "$outstr"
    }

    set sRelName [eServiceGetCurrentSchemaName relationship $sRegProgName $sRelationName]

    if {$sRelName == ""} {
        # Error out if not registered
     	set mqlret 1

     	# Give error message
        set outstr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigcRelAttribute_if.InvalidRel" 1 \
                                  "Name" "$sRelName" \
                    "" \
                    ]

     	mql notice "$outstr"
    }

    if {$mqlret == 0} {
        set mqlret [eServicecommonTrigcRelAttribute $sType $sName $sRev $sRelName $sDirection $sAttrName $iAttrFlag $sAttrValue $iUniqueFlag]
    }

    exit $mqlret
}
# end eServicecommonTrigcRelAttribute_if

