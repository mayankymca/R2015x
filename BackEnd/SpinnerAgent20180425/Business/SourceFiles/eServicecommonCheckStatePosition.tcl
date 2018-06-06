###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonCheckStatePosition.tcl.rca $ $Revision: 1.43 $
#
# @libdoc       eServicecommonCheckStatePosition.tcl
#
# @Library:     Interface for checking objects' state location
#
# @Brief:       Check state location relative to a particular state
#
# @Description: This program will check the current state location of an
#                      object against a specified state.  Program will tell
#                      if the current state is before, after or equals
#                      a particular state.
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
eval  [ utLoad eServicecommonTranslation.tcl ]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonCheckStatePosition
#
# @Brief:       Check state location relative to a specified state
#
# @Description: The intent of this program is to provide a function
#                          which checks the state of an object relative to
#                          another state.
#
# @Parameters:  sType                - Object Type
#               sName                - Object Name
#               sRev                 - Object Revision
#               sTargetState         - The state being checked against
#               sComparisonOperator  - Operator to check state with. Valid entries are
#                                      LT, GT, EQ, LE, GE, and NE.
#
# @Returns:     0 if object meets Comparison
#               2 if object does not meet Comparison
#               1 if error, or if state does not exist in objects policy.
#
# @Usage:       Used in triggers that need to check an objects state position
#               relative to another state.
#
# @Example:     eServicecommonCheckStatePos Part 12345 A Release EQ
#
# @procdoc
#************************************************************************
proc eServicecommonCheckStatePosition  {sType sName sRev sTargetState sComparisonOperator} {
    # Set program name
    set progname "eServicecommonCheckStatePosition.tcl"

    # Initialize variables
    set mqlret 0
    set retCode 0

    if {$mqlret == 0} {
        # Get all states for object
        set sCmd {mql print bus "$sType" "$sName" "$sRev" select policy current state dump |}
        set mqlret [catch {eval $sCmd} outstr]

        if {$mqlret == 0} {
            set lObjectData [split $outstr |]
            set sPolicy [lindex $lObjectData 0]
            set sCurrentState [lindex $lObjectData 1]
            set lStates [lrange $lObjectData 2 end]

            # Get index location of Target State
            set iTargetState [lsearch $lStates $sTargetState]

            # Check if state is in objects policy
            if {$iTargetState == -1} {
                set retCode 1
                set outstr [mql execute program emxMailUtil -method getMessage \
                                  "emxFramework.ProgramObject.eServicecommonCheckStatePosition.InvalidState" 5 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                                  "Policy" "$sPolicy" \
                                  "State" "$sTargetState" \
                                  "" \
                                  ]
            }
        }

        if {$retCode == 0 && $mqlret == 0} {
            # Get index location for objects current state
            set iCurrentLoc [lsearch $lStates $sCurrentState]

            # Check Target State index with object index location
            switch $sComparisonOperator {
                LT {
                    if {$iCurrentLoc >= $iTargetState} {
                        set retCode 2
                    }
                }

                GT {
                    if {$iCurrentLoc <= $iTargetState} {
                        set retCode 2
                    }
                }

                EQ {
                    if {$iCurrentLoc != $iTargetState} {
                        set retCode 2
                    }
                }

                LE {
                    if {$iCurrentLoc > $iTargetState} {
                        set retCode 2
                    }
                }

                GE {
                    if {$iCurrentLoc < $iTargetState} {
                        set retCode 2
                    }
                }

                NE {
                    if {$iCurrentLoc == $iTargetState} {
                        set retCode 2
                    }
                }

                default {
                    set outstr [mql execute program emxMailUtil -method getMessage \
                                  "emxFramework.ProgramObject.eServicecommonCheckStatePosition.InvalidOperation" 1 \
                                  "Operation" "$sComparisonOperator" \
                                  "" \
                                  ]
                    set retCode 1
                }

            }
        } else {
            set mqlret 1
        }
    }

    if {$retCode == 1} {
        mql notice "$progname :\n$outstr"
    } elseif {$mqlret == 1} {
        mql notice "$progname :\n$outstr"
        set retCode $mqlret
    }

    return $retCode
}

# end eServicecommonCheckStatePosition

# End of Module

