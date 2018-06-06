###############################################################################
#
# $RCSfile: eServicecommonWzrdListAttrRange.tcl.rca $ $Revision: 1.18 $
#
# Description:  Like mxWzrdListAttrRange.tcl, but if argument #3 is "=default", the
#	          selected range value will be the default.
#
#
#
# Dependencies:
#
###############################################################################

###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

tcl;
eval {

######################################################################
#
# DEFINE UTLOAD
#
######################################################################

proc utLoad { Program } {

    set  sSearch "  code \'"
    set  sOutput [ mql print program $Program ]
    set  iStart  [ string first $sSearch $sOutput ]
    incr iStart  [ string length $sSearch ]
    set  iEnd    [ expr [ string last "\'" $sOutput ] -1 ]
    set  sOutput [ string range $sOutput $iStart $iEnd ]

    return $sOutput
}

######################################################################
#
# LOAD MQL/Tcl TOOLKIT LIBRARIES.
#
#####################################################################

#
# Debugging procedures
#
eval  [ utLoad eServiceSchemaVariableMapping.tcl]
eval  [ utLoad eServicecommonDEBUG.tcl ]
eval  [ utLoad eServicecommonPSUtilities.tcl ]

######################################################################
#
# MAIN
#
# @progdoc        eServicecommonWzrdListAttrRange.tcl
#
# @Brief:         Load an Attribute's Range to a List
#
# @Description:   This program will load the range values of the given
#                 attribute to a listbox or combobox.
#
# @Parameters:    1 - Attribute Title
#                            2 - Range Operator; valid values are:
#                                   \"=\" (default), \"!=\", \"<\", \">\", \"<=\", and \">=\"
#                            3 - Optional selected range value.  If this is not
#                                 specified, the first range value in the list will
#                                 be selected. If specified as "=default", the default
#                                 value for the attribute will be set as selected.
#
# @Returns:       RPE settings for the list of attribute ranges and
#                         the selected value.
#
# @Usage:         This program should be implemented as a load program
#                 for listboxes or comboboxes.
#
#                 The following parameter input string will load the
#                 \"=\" ranges for the Material attribute.
#
#                 Material
#
#                 The following parameter input string will load the
#                 minimum value for the \"Outside Diameter\" attribute.
#
#                 {Outside Diameter} <
#
#                 The following parameter input string will load the
#                 \"=\" ranges for the \"Unit of Measure\" attribute and
#                 set the default to \"Each\".
#
#                 {Unit of Measure} = Each
#
#
######################################################################

    #
    # Debugging trace - note entry
    #
    mxDEBUGIN "eServicecommonWzrdListAttrRange.tcl"

    set sChoices   [ mql get env 0 ]
    set sSelected  [ mql get env 1 ]
    #set sAttr     [lookup attribute [ string trim [ mql get env 2 ] ] ]
    set sAttr      [ mql get env 2 ]
    set sOperator  [ string trim [ mql get env 3 ] ]
    set sSelection [ mql get env 4 ]
    set equalsStr "="

    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set sAttr [eServiceGetCurrentSchemaName  attribute $RegProgName  $sAttr ]

    if { [string length $sOperator] == 0 } { set sOperator $equalsStr }

    set lRanges [ lsort -dictionary [ mxAttGetRanges $sAttr $sOperator ] ]

    if { [string length $sSelection] == 0 } { set sSelection [ lindex $lRanges 0 ] }

    # Added to get default selection
    set defaultValue "=default"
    if { $sSelection == $defaultValue } {
         set sSelection [mql print attribute $sAttr select default dump ]
    }
    mql set env $sChoices $lRanges
    mql set env $sSelected $sSelection

    #
    # Debugging trace - note exit
    #
    mxDEBUGOUT "eServicecommonWzrdListAttrRange.tcl"

}

