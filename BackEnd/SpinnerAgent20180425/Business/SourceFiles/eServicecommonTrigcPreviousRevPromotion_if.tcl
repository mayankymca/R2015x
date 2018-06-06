###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcPreviousRevPromotion_if.tcl.rca $ $Revision: 1.47 $
#
# @libdoc       eServicecommonTrigcPreviousRevPromotion_if
#
# @Library:     Interface for checking previous revisions of object
#
# @Brief:       Check this objects previous revisions prior to promotion of object.
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
eval  [ utLoad eServicecommonTrigcPreviousRevPromotion.tcl ]
eval  [ utLoad eServicecommonTranslation.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcPreviousRevPromotion_if
#
# @Brief:       Check the state of previous revisions
#
# @Description: This program will check the current state of previous revisions.
#               The operator to apply to current state is passed in as a
#               program argument.  All revisions must satisfy operation.
#
# @Parameters:  Inputs via RPE:
#                     sType - Object's type
#                     sName - Object's name
#                      sRev - Object's revision
#               [mql env 1] - The state to check for.
#               [mql env 2] - The comparison operator used to check state with.
#                             Valid entries LT, GT, EQ, LE, GE, and NE.
#               [mql env 3] - If different policies are used for different objects in the revision chain,
#                             the state names might differ. This parameter controls the behavior if the
#                             lookup of the state fails. The value could be either OPTIONAL or REQUIRED.
#                             The default value OPTIONAL allows hides the error and the trigger code will
#                             continue to try the next previous revision. The option REQUIRED will present
#                             the lookup error message and the whole trigger check fails.
#               [mql env 4] - The value ALL will check all previous revisions all the way to the first
#                             revision. Default value is PREVIOUS and will only check the previous revision.
#
# @Returns:     0 if successfull
#               1 if not successfull
#
# @Usage:       For use as trigger check on promotion
#
# @Example:     configure mxTrigMgr thusly:
#               [mql env 1] - state_release
#               [mql env 2] - GE
#               [mql env 3] - OPTIONAL
#               [mql env 4] - PREVIOUS
#
# @procdoc
#******************************************************************************
    # Get Program names
    set progname      "eServicecommonTrigcPreviousRevPromotion_if"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"

    # Load Schema Mapping
    eval [utLoad $RegProgName]

    # Get data values from RPE
    set sType [mql get env TYPE]
    set sObjType $sType
    set sName [mql get env NAME]
    set sObjName $sName
    set sRev [mql get env REVISION]
    set sObjRev $sRev
    set sPolicy [mql get env POLICY]
    set sSymbolicComparisonState [mql get env 1]
    set sComparisonOper [mql get env 2]
    set sErrorOnNoState [string toupper [mql get env 3]]
    if {"$sErrorOnNoState" != "REQUIRED"} {
        set sErrorOnNoState "OPTIONAL"
    }
    set sPreviousRevisionLevels [string toupper [mql get env 4]]
    if {"$sPreviousRevisionLevels" != "ALL"} {
        set sPreviousRevisionLevels "PREVIOUS"
    }
    mql verbose off

    # Initialize Error variables
    set mqlret 0
    set outStr ""

    if {$mqlret == 0} {
    
        # Go through all the previous objects in the revision sequense to see that they
        # satisfy the state requirement (if env 4 is set to PREVIOUS only the previous
        # is checked)
        while {1 == 1} {

            set bDoComparison TRUE
        
            set sCmd {mql print bus "$sType" "$sName" "$sRev" select previous previous.policy previous.current previous.type previous.name dump |}

            # Execute Command
            set mqlret [catch {eval $sCmd} outstr]

            if {$mqlret == 0} {
                if {"$outstr" == ""} {
                    # If no previous revs breakout of loop
                    break
                } else {
                    set lObjectData [split $outstr |]
                    set sRev [lindex $lObjectData 0]
                    set sPolicy [lindex $lObjectData 1]
                    set sState [lindex $lObjectData 2]
                    set sType [lindex $lObjectData 3]
                    set sName [lindex $lObjectData 4]

                    # Look up the absolute state on the policy for this object
                    set sAbsState [eServiceLookupStateName "$sPolicy" "$sSymbolicComparisonState"]

                    if {$sAbsState == ""} {
                        if {"$sErrorOnNoState" == "REQUIRED"} {
                            # Error out if not registered
                            set mqlret 1

                            # Give error message
                            set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicecommonTrigcPreviousRevPromotion_if.InvalidState" 1 \
                                        "State" "$sSymbolicComparisonState" \
                                        "" \
                                        ]
                            break
                        } else {
                            # We should jump to the next previous rev without any comparison
                            set bDoComparison FALSE
                        }
                    }

                    # Execute command
                    if { ($mqlret == 0) && ("$bDoComparison" == "TRUE") } {
                        set mqlret [eServicecommonCheckObjState $sType $sName $sRev $sAbsState $sComparisonOper]

             set sTempType [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.Type.[join [split $sObjType] "_"]" 0 \
                                "" \
                                ]

                    set sTempState [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.State.[join [split $sPolicy] "_"].[join [split $sState] "_"]" 0\
                                "" \
                                ]
             
                   set sTempAbsState [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.State.[join [split $sPolicy] "_"].[join [split $sAbsState] "_"]" 0\
                                "" \
                                ]

                        if {$mqlret != 0} {
                            # Give error message
                            set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicecommonTrigcPreviousRevPromotion_if.NoPromote" 6 \
                                                "Type" "$sTempType" \
                                                "Name" "$sObjName" \
                                                "Rev" "$sObjRev" \
                                                "PrevRev" "$sRev" \
                                                "PrevState" "$sTempState" \
                                                "State" "$sTempAbsState" \
                                        "" \
                                        ]

                            # Set return value
                            set mqlret 1

                            # Breakout of loop because revision does not meet state check
                            break
                        }
                    }
                }
            } else {
                break
            }
            
            # Jump out if we only are checking one previous revision
            if { "$sPreviousRevisionLevels" == "PREVIOUS" } {
                break
            }
        }
    }

    if {$mqlret == 1} {
        mql notice "$outstr"
    }

    exit $mqlret

}
# end eServicecommonTrigcPreviousRevPromotion_if


# End of Module

