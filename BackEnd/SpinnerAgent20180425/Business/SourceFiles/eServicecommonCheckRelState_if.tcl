###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonCheckRelState_if.tcl.rca $ $Revision: 1.54 $
#
# @libdoc       eServiceCheckRelState_if.tcl
#
# @Library:     Interface for checking related objects' state
#
# @Brief:       Check this objects children prior to promotion of parent.
#
# @Description: This program checks if the children objects related
#                      to the parent with the specified relationships have
#                      reached the state given for their type.
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
# Load MQL/Tcl Toolkit Libraries.
#
###############################################################################
eval  [ utLoad eServicecommonCheckRelState.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServiceCheckRelState_if
#
# @Brief:       Check state of related objects
#
# @Description: Check the incoming parameters for validity,
#                          obtain their "lookup" names in case of customer
#                          modification and pass on the correctly formatted
#                          parameters to the professional services methods.
#
#                          The intent of this program is to provide a function
#                          which checks the state of all objects of
#                          a named object type related to a parent object.
#                          The returned value will inform the parent if all the
#                          requested related objects are at a given state so
#                          that the parent can be promoted to the next state.
#
# @Parameters:  Inputs via RPE:
#                     sType - Parent object's type
#                     sName - Parent object's name
#                      sRev - Parent object's revision number
#               [mql env 1] - Name of relationships to traverse through.  Symbolic
#                             names must be used.  A null entry will indicate
#                             all relationships attached to object.
#               [mql env 2] - Name of types to check. Symbolic names must be used.
#                             A null entry will indicate all types connected to
#                             object.
#                             NOTE: Relationships and Types are combined into one
#                             statement.
#                             Ex: expand bus TNR rel Uses,Ref type Part,Drawing etc...
#               [mql env 3] - Target state being checked for.  Symbolic name
#                             must be used.
#               [mql env 4] - Direction to traverse.  Valid values are "to" and
#                             "from", anything else will indicate both directions.
#               [mql env 5] - Operator to check against state with. Valid entries
#                             are LT, GT, EQ, LE, GE, and NE.
#               [mql env 6] - Is object match required or optional.  Valid entries
#                             are Required and Optional.  A null entry or any other
#                             value other than Required will indicate Optional.
#               [mql env 7] - Is state match required or optional.  Valid entries
#                             are Required and Optional.  A null entry or any other
#                             value other than Optional will indicate Required.
#
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
# @Usage:       For use as trigger check on promotion
#
# @Example:     configure mxTrigMgr thusly:
#               [mql env 1] - relationship_PartSpecification
#               [mql env 2] - type_DrawingPrint type_Specifiation
#               [mql env 3] - state_Release
#               [mql env 4] - from
#               [mql env 5] - GE
#               [mql env 6] - Required
#               [mql env 7] - Required
#
#               The values above will generate the following mql command:
#               mql expand bus TNR from rel "Part Specification" type "Drawing Print,Specification" \
#               select bus current policy dump |
#               An error will be returned if expand command returns nothing.
#
# @procdoc
#************************************************************************

    # Set program name
    set progname "eServiceCheckRelState_if.tcl"
    set RegProgName "eServiceSchemaVariableMapping.tcl"

    # Load Schema Mapping
    eval [utLoad $RegProgName]

    # Initialize variables
    set mqlret 0
    set outstr ""
    set sTargObject ""
    set sRel ""
    set sTObject {}
    set lMsgs {}

    mql verbose off;

    # Get RPE values
    set sRelationship [mql get env 1]
    set sTargetObject [mql get env 2]
    set sTargetState  [mql get env 3]
    set sDirection    [mql get env 4]
    set sComparisonOperator [mql get env 5]
    set sObjectRequired [mql get env 6]
    set sStateRequired [mql get env 7]

    # If no value for operator set it to EQ
    if {[string compare $sComparisonOperator ""] == 0} {
        set sComparisonOperator "EQ"
    }

    # If value for Object Required in not Required set it to Optional
    if {[string compare [string tolower $sObjectRequired] "required"] != 0} {
        set sObjectRequired "Optional"
    }

    # If value for State Required in not Required set it to Optional
    if {[string compare [string tolower $sStateRequired] "optional"] != 0} {
        set sStateRequired "Required"
    }

    # Grab the parent object's "name" to pass into eServicecommonCheckRelState
    set sParentType     [ mql get env TYPE ]
    set sParentName     [ mql get env NAME ]
    set sParentRev      [ mql get env REVISION ]

    if {[llength $sRelationship] > 0} {
        # Create string of all relationships
        foreach sRelation $sRelationship {
            set sRelResult [string trim $sRelation]

            # Get relation mapping
            set sRelMap [eServiceGetCurrentSchemaName relationship $RegProgName $sRelResult]
            if {[string compare $sRelMap ""] == 0} {
                # Error out if not registered
                set mqlret 1
                break
            } else {
                append sRel $sRelMap
                append sRel ","
            }
        }

        # Remove last comma from list of relationships
        set sRel [string trim $sRel ,]
    } else {
        # Set Relationship to * if one is not entered
        set sRel "*"
    }

    if {$mqlret == 0} {
        if {[llength $sTargetObject] > 0} {
            # Create string of all target objects
            foreach sObject $sTargetObject {
                set sTypeResult [string trim $sObject]
                set sTypeMap [eServiceGetCurrentSchemaName type $RegProgName $sTypeResult]

                if {[string compare $sTypeMap ""] == 0} {
                    # Error out if not registered
                    set mqlret 1
                    break
                } else {
                    append sTargObject [eServiceGetCurrentSchemaName type $RegProgName $sTypeResult]
                    append sTargObject ,
                }
            }

            # Remove last comma from list of Target Objects
            set sTargObject [string trim $sTargObject ,]
        } else {
            # Set Target Object to * if one is not entered
            set sTargObject "*"
        }
    }

    if {$mqlret == 0} {
        set mqlret [eServicecommonCheckRelState  "$sParentType" "$sParentName" "$sParentRev" "$sRel" \
                "$sDirection" "$sTargObject" "$sTargetState" "$sComparisonOperator" "$sObjectRequired" "$sStateRequired"]
    }

    #return -code $mqlret
    exit $mqlret

}

# end eServicecommonCheckRelState_if

# End of Module

