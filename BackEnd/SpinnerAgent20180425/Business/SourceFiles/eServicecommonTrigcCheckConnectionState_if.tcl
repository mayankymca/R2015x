###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcCheckConnectionState_if.tcl.rca $ $Revision: 1.42 $
#
# @libdoc       eServicecommonTrigcCheckConnectionState_if.tcl
#
# @Library:     Interface for checking related objects' state
#
# @Brief:       Check objects state prior to connection.
#
# @Description: This program checks if the object to be connected have
#               reached the state given for their type.
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

  eval [ utLoad eServicecommonTranslation.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcCheckConnectionState_if
#
# @Brief:       Check state of object to be connected
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
#                   sTOType - To end object's type
#                   sToName - To end object's name
#                    sToRev - To end object's revision
#                 sFromType - From end object's type
#                 sFromName - From end object's name
#                  sFromRev - From end object's revision
#               [mql env 1] - Target state being checked for.  Symbolic name
#                             must be used.
#               [mql env 2] - Direction to traverse.  Valid values are "to" and
#                             "from", anything else will indicate 'to' direction.
#               [mql env 3] - Operator to check against state with. Valid entries
#                             are LT, GT, EQ, LE, GE, and NE.
#               [mql env 4] - Flag which indicates wheather error message should be
#                             shown if target state does not exists on object to be
#                             connected. The valid values are 'true' and 'false'.
#                             anything else will indicate 'false'.
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
# @Usage:       For use as trigger check on promotion
#
# @Example:     configure mxTrigMgr thusly:
#               [mql env 1] - state_Release
#               [mql env 2] - from
#               [mql env 3] - GE
#               [mql env 4] - Required
#
# @procdoc
#************************************************************************

    # Set program name
    set progname "eServicecommonTrigcCheckConnectionState_if.tcl"
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

    mql verbose off

    # Get RPE values
    set sTargetState [mql get env 1]
    set sDirection [string toupper [mql get env 2]]
    set sComparisonOperator [string toupper [mql get env 3]]
    set sNoStateError [string toupper [mql get env 4]]

    # If no value for operator set it to EQ
    if {[string compare $sComparisonOperator ""] == 0} {
        set sComparisonOperator "EQ"
    }

    # If value for No state error is not specified then set it to false.
    if {[string compare $sNoStateError "TRUE"] != 0} {
        set sNoStateError "FALSE"
    }

    # If value for direction is not specified then set it to 'to'.
    if {[string compare $sDirection "FROM"] != 0} {
        set sDirection "TO"
    }

    # Get parent and child object ids
    if {"$sDirection" == "TO"} {
        set sParentObjectID [mql get env "FROMOBJECTID"]
        set sChildObjectID [mql get env "TOOBJECTID"]
        set sChildType [mql get env "TOTYPE"]
        set sChildName [mql get env "TONAME"]
        set sChildRev [mql get env "TOREVISION"]
    } else {
        set sParentObjectID [mql get env "TOOBJECTID"]
        set sChildObjectID [mql get env "FROMOBJECTID"]
        set sChildType [mql get env "FROMTYPE"]
        set sChildName [mql get env "FROMNAME"]
        set sChildRev [mql get env "FROMREVISION"]
    }

    # Get child object info
    set sCmd {mql print bus $sChildObjectID select policy current state dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql notice "$outStr"
        exit 1
    }
    set lOutput [split $outStr |]
    set sPolicy [lindex $lOutput 0]
    set sState [lindex $lOutput 2]
    set sCurrent [lindex $lOutput 1]
    set lState [lrange $lOutput 2 end]

    # If target state name not specified then use parent object's current state as target state.
    if {"$sTargetState" == ""} {
        set sCmd {mql print bus $sParentObjectID select current dump |}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            mql notice "$outStr"
            exit 1
        }
        set sTargetState "$outStr"
    } else {
        set sTempTargetState [eServiceLookupStateName "$sPolicy" "$sTargetState"]
        if {$sTempTargetState == ""} {
            if {"$sNoStateError" == "TRUE"} {
                set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.NoState" 4 \
                                  "State" "$sTargetState" \
                                  "Type" "$sChildType" \
                                  "Name" "$sChildName" \
                                  "Rev" "$sChildRev" \
                                "" \
                                ]
                mql notice "$sMsg"
                exit 1
            } else {
                return
            }
        } else {
            set sTargetState "$sTempTargetState"
        }
    }

    set sTargetStateIndex [lsearch "$lState" "$sTargetState"]
    set sCurrentStateIndex [lsearch "$lState" "$sCurrent"]
    switch $sComparisonOperator {
        LT {
            set sOperatorDesc [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.BeyondOrEqual" 0 \
                                "" \
                                ]
            if {$sCurrentStateIndex < $sTargetStateIndex} {
                set sExitCode 0
            } else {
                set sExitCode 1
            }
        }
        LE {
            set sOperatorDesc [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.Beyond" 0 \
                                "" \
                                ]
            if {$sCurrentStateIndex <= $sTargetStateIndex} {
                set sExitCode 0
            } else {
                set sExitCode 1
            }
        }
        EQ {
            set sOperatorDesc [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.NotEqual" 0 \
                                "" \
                                ]
            if {$sCurrentStateIndex == $sTargetStateIndex} {
                set sExitCode 0
            } else {
                set sExitCode 1
            }
        }
        GE {
            set sOperatorDesc [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.BeforeOrEqual" 0 \
                                "" \
                                ]
            if {$sCurrentStateIndex >= $sTargetStateIndex} {
                set sExitCode 0
            } else {
                set sExitCode 1
            }
        }
        GT {
            set sOperatorDesc [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.Before" 0 \
                                "" \
                                ]
            if {$sCurrentStateIndex > $sTargetStateIndex} {
                set sExitCode 0
            } else {
                set sExitCode 1
            }
        }
    }

    if {$sExitCode == 1} {
        set sMsg [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigcCheckConnectionState_if.CurrentState" 5 \
                                  "Type" "$sChildType" \
                                  "Name" "$sChildName" \
                                  "Rev" "$sChildRev" \
                                  "Operation" "$sOperatorDesc" \
                                  "State" "$sTargetState" \
                    "" \
                    ]
        mql notice "$sMsg"
    }

    exit $sExitCode
}

# end eServicecommonTrigcCheckConnectionState_if

# End of Module

