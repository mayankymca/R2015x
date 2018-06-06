###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcLastRevAtState.tcl.rca $ $Revision: 1.45 $
#
# @libdoc       eServicecommonTrigcLastRevAtState.tcl
#
# @Library:     Interface for checking revised objects' state
#
# @Brief:       Check if object being connected matches revision check.
#
# @Description: This program will check if the object being connected
#               is the last revision that passes state check criteria.
#               If a higher revision exists and it passes the state check
#               criteria the current object will not be allowed to be connected.
#               If no higher revision passes the state check criteria the current
#               object will be connected.
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

###############################################################################
#
# Define Global Variables
#
###############################################################################

###############################################################################
#
# Load MQL/Tcl Toolkit Libraries.
#
###############################################################################
eval  [utLoad eServicecommonTranslation.tcl]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonLastRevInState
#
# @Brief:       Get the last revision of an object in a particular state.
#
# @Description: This program will check if the object being connected
#               is the last revision that passes state check criteria.
#               If a higher revision exists and it passes the state check
#               criteria the current object will not be allowed to be connected.
#               If no higher revision passes the state check criteria the current
#               object will be connected.
#
# @Parameters:  sType        - The object type
#               sName        - The name of the object
#               sTargetState - The state being checked
#               iObjId       - Argument to determine whether to return the
#                              revision of the matching object or the id of the
#                              matching object.  Passing in a 1 will return
#                              the object Id. Anything else will return revision
#               sPolicy      - Policy of object
#               sOperator    - Operator to check target state against. Valid entries
#                              EQ, LE, GE (Default is EQ).  Will default to
#                              EQ if invalid parameter passed in.
#               sVault       - Which vault to search (Default is *)
#
# @Returns:     The last object revision or object id in the specified state(s).
#               If there is no match a NULL string is returned.  This will
#               happen if no object is in state(s) specified.  If there is an error
#               "Error" will be returned (This is because 1 can be a valid revision).
#
# @Usage:       Common Procedure
#
# @Example:     eServicecommonLastRevInState "Drawing Print" 112345 Release 0 Production GE
#
# @procdoc
#************************************************************************
proc eServicecommonLastRevInState {sType sName sTargetState iObjId sPolicy sOperator {sVault *}} {
    set progname "eServicecommonTrigcLastRevAtState.tcl"
    set mqlret 0

    if {"$sOperator" != "" || "$sOperator" != "EQ"} {
        # Create command to get object policy states
        set sCmd {mql print policy "$sPolicy" select state dump |}

        # Execute command
        set mqlret [catch {eval $sCmd} outstr]

        # Create list of states
        set lStates [split $outstr |]

        # Get index location of targeted state
        set iIndex [lsearch $lStates "$sTargetState"]

        # Create error message if state does not exist in policy
        if {$iIndex == -1} {
            set mqlret 1
            set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcLastRevAtState.NoState" 2 \
                                  "State" "$sTargetState" \
                                  "Policy" "$sPolicy" \
                                "" \
                                ]
        }

        switch $sOperator {
            GE {
                # Create list of valid states greater than target state
                set lValidStates [lrange $lStates [expr $iIndex + 1] end]
            }

            LE {
                # Create list of valid states less than target state
                set lValidStates [lrange $lStates 0 [expr $iIndex - 1]]
            }

            default {
                set sOperator EQ
            }
        }
    }

    if {$mqlret == 0} {
        # Build query command
        set sCmd {mql temp query bus "$sType" "$sName" *}

        if {"$sOperator" == "" || "$sOperator" == "EQ"} {
            # Continue building query command for Equal
            append sCmd { where "current=='$sTargetState'"}
        } else {
            # Continue building query command for Greator or Equal
            append sCmd { where "current=='$sTargetState'}
            foreach sState $lValidStates {
                append sCmd " || current==\'[subst $sState]\'"
            }
            append sCmd {"}
        }

        # Finish query command
        append sCmd { vault "$sVault" select id revisions dump |}

        # Execute query command
        set mqlret [catch {eval $sCmd} outstr]

        # Initialize variables
        set lValidRevs {}
        set lValidIds {}
        set lRevs {}
        set iRevIndex ""
        set sReturn ""
    }

    # If matches where found get last revision
    if {$mqlret == 0 && "$outstr" != ""} {
        # Create list of matching objects
        set lSearchResults [split $outstr \n]

        # Create list of all revisions in state(s)
        # and list of object revision chain
        foreach sReturn $lSearchResults {
            # Get object data into a list
            set lObjectValues [split $sReturn |]

            # Add rev of object to list of valid revs
            lappend lValidRevs [lindex $lObjectValues 2]

            # Add object id to list of valid ids
            lappend lValidIds [lindex $lObjectValues 3]

            # Create a list of the revision chain
            set lRevs [lrange $lObjectValues 4 end]
        }

        # Go through list of object revisions in reverse order to find
        # the latest revision for state(s)
        for {set iIndex [expr [llength $lRevs] - 1]} {$iIndex >= 0} {incr iIndex -1} {
            set iSearch [lsearch $lValidRevs [lindex $lRevs $iIndex]]
            if {$iSearch != -1} {
               set iRevIndex $iSearch
               break
           }
        }

        # Return either last object id or revision
        if {$iObjId == 1} {
            set sReturn [lindex $lValidIds $iRevIndex]
        } else {
            set sReturn [lindex $lValidRevs $iRevIndex]
        }
    }

    # Return Error if an error with mql command
    if {$mqlret == 1} {
        set sReturn $mqlret
        mql notice "$progname :\n$outstr"
    }

    return $sReturn
}

# end eServicecommonLastRevInState

# End of Module

