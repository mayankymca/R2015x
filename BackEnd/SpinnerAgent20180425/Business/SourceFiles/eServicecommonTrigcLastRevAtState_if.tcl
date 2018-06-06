###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcLastRevAtState_if.tcl.rca $ $Revision: 1.45 $
#
# @libdoc       eServicecommonTrigcLastRevAtState_if
#
# @Library:     Interface for checking objects current state in relationship
#               to other revisions of the object.
#
# @Brief:       Check that the object being connected is the latest
#               revision in a particular state.
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
eval  [ utLoad eServicecommonTrigcLastRevAtState.tcl ]
eval  [ utLoad eServicecommonTranslation.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcLastRevAtState_if
#
# @Brief:       Check that the object is the latest revision in a state.
#
# @Description: This program will check that the object being connected is
#               the latest revision in a particular state.  State to check
#               against is one of the arguments to be passed in.  Another
#               argument will determine if the "to" or "from" direction object
#               is being checked, "to" is the default.
#
# @Parameters:  Inputs via RPE:
#                     sType - Object's type
#                     sName - Object's name
#                      sRev - Object's revision
#               [mql env 1] - The relationship direction "to" or "from"
#               [mql env 2] - The target state to check for.
#               [mql env 3] - Operator to check target state against. Valid entries
#                             EQ, LE, GE (Defualut is EQ).  Will default to
#                             EQ if invalid parameter passed in.
#               [mql env 4] - The vault to check. (Default is all vaults)
#
# @Returns:     0 if successfull
#               1 if not successfull
#
# @Usage:       For use as trigger check on connection creation
#
# @Example:     configure mxTrigMgr thusly:
#               [mql env 1] - to
#               [mql env 2] - state_Release
#               [mql env 3] - GE
#               [mql env 4] - Production Vault
#
# @procdoc
#******************************************************************************
    # Get Program names
    set progname      "eServicecommonTrigcLastRevAtState_if"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"

    # Load Schema Mapping
    eval [utLoad $RegProgName]

    # Get data values from RPE
    set sDirection [mql get env 1]
    set sTargetState [mql get env 2]
    set sOperator [mql get env 3]
    set sVault [mql get env 4]

    # Get object information
    if {[string tolower $sDirection] == "from"} {
        set sObjType [mql get env FROMTYPE]
        set sObjName [mql get env FROMNAME]
        set sObjRev [mql get env FROMREVISION]
        set sObjId [mql get env FROMOBJECTID]
    } else {
        set sObjType [mql get env TOTYPE]
        set sObjName [mql get env TONAME]
        set sObjRev [mql get env TOREVISION]
        set sObjId [mql get env TOOBJECTID]
    }

    if {"$sVault" == ""} {
        set sVault *
    }

    mql verbose off

    # Initialize Error variables
    set mqlret 0
    set outStr ""

    if {$mqlret == 0} {
        # Get policy of object
        set sCmd {mql print bus $sObjId select policy dump}

        set mqlret [catch {eval $sCmd} outstr]
    }

    if {$mqlret == 0} {
        set sObjPolicy $outstr

        # Get state mapping
     	set sTargetState [eServiceLookupStateName $sObjPolicy $sTargetState]

     	if {$sTargetState == ""} {
     	    # Error out if not registered
     	    set mqlret 1

     	    # Give error message
     	    set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcLastRevAtState_if.InvalidName" 1 \
                                  "State" "$sTargetState" \
                                "" \
                                ]
            mql notice "$progname :\n$outstr"
        }
    }

    if {$mqlret == 0} {
        # Execute command
        set sLastRev [eServicecommonLastRevInState $sObjType $sObjName $sTargetState 0 \
                              $sObjPolicy $sOperator $sVault]

        if {"$sLastRev" == "Error"} {
            # Set return to 1 because procedure had an error
            set mqlret 1
        } elseif {"$sLastRev" == ""} {
            # Set return to 0 because no objects in state
            set mqlret 0
        } else {
            # Check if object is the last revision in specified state
            if {"$sObjRev" != "$sLastRev"} {
                set mqlret 1
     	        set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcLastRevAtState_if.ConnectFail" 6 \
                                  "Type" "$sObjType" \
                                  "Name" "$sObjName" \
                                  "Rev" "$sObjRev" \
                                  "State" "$sTargetState" \
                                  "Operator" "$sOperator" \
                                  "LastRev" "$sLastRev" \
                                "" \
                                ]
                mql notice "$progname :\n$outstr"
            }
        }
    }

    exit $mqlret

}
# end eServicecommonTrigcLastRevAtState_if


# End of Module

