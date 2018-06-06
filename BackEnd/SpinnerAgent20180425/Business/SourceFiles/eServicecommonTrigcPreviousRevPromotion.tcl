###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcPreviousRevPromotion.tcl.rca $ $Revision: 1.17 $
#
# @libdoc       eServicecommonTrigcPreviousRevPromotion.tcl
#
# @Library:     Interface for checking previous revisions of object
#
# @Brief:       Check this objects previous revisions prior to promotion of object.
#
# @Description: This program checks the current state of object.  Check is done
#                      against a specified comparison operator.
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

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonCheckObjState
#
# @Brief:       Check current state object
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
# @Parameters:  sType                - Object Type
#               sName                - Object Name
#               sRev                 - Object Revision
#               sTargetState         - The state being checked against.
#               sComparisonOperator  - Operator to check state against. Valid entries are
#                                      LT, GT, EQ, LE, GE, and NE.
#
# @Returns:     0 if object state logic satisfies Comparison Operator.
#               1 if a program error is encountered.
#               2 if state in state argument does not exist in objects policy
#               3 if an invalid comparison operator is passed in
#
# @Usage:       For use as trigger check on promotion
#
# @Example:     eServiceCheckRelState Type Name Rev Release GE
#
# @procdoc
#************************************************************************
proc eServicecommonCheckObjState {sType sName sRev sTargetState sComparisonOperator} {
    # Set program name
    set progname "eServiceCheckObjState.tcl"

    # Get the policy, current state and states for object
    set sCmd {mql print bus "$sType" "$sName" "$sRev" select policy current state dump |}
    set mqlret [catch {eval $sCmd} outstr]

    if {$mqlret == 0} {
        set lPolicyData [split $outstr |]
        set sPolicy [lindex $lPolicyData 0]
        set sCurrentState [lindex $lPolicyData 1]
        set lStates [lrange $lPolicyData 2 end]

        # Check if State exist in Policy
        set iIndexTargetState [lsearch $lStates $sTargetState]

        if {$iIndexTargetState < 0} {
            set mqlret 2
        }
    }

    if {$mqlret == 0} {
        # Get current state iStateIndexLoc location in policy list
        set iStateIndexLoc [lsearch $lStates $sCurrentState]

        # Check Target State iStateIndexLoc with object iIndexTargetState location
        switch $sComparisonOperator {
            LT {
                if {$iStateIndexLoc >= $iIndexTargetState} {
                    set mqlret 1
                }
            }

            GT {
                if {$iStateIndexLoc <= $iIndexTargetState} {
                    set mqlret 1
                }
            }

            EQ {
                if {$iStateIndexLoc != $iIndexTargetState} {
                    set mqlret 1
                }
            }

            LE {
                if {$iStateIndexLoc > $iIndexTargetState} {
                    set mqlret 1
                }
            }

            GE {
                if {$iStateIndexLoc < $iIndexTargetState} {
                    set mqlret 1
                }
            }

            NE {
                if {$iStateIndexLoc == $iIndexTargetState} {
                    set mqlret 1
                }
            }

            default {
                set mqlret 3
            }

        }
    }

    return $mqlret
}

# end eServicecommonCheckObjState

# End of Module

