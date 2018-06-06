###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcCheckSelfConnection_if.tcl.rca $ $Revision: 1.47 $
#
# @libdoc       eServicecommonTrigcCheckSelfConnection_if.tcl
#
# @Brief:       This trigger program fails if to and from objects of
#               connection are same or revisions of same object.
#
# @Input Parameters: sCheckTypeName = TRUE then check revisions and self connection
#                                   = FALSE then check only self connection.
#                                   This input defaults to TRUE.
# @Description: This program can be configured as connect trigger
#               to check to and from objects are not same.
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

    # Set program name
    set progname "eServicecommonTrigcCheckSelfConnection_if.tcl"

    set sCheckTypeName [string toupper [mql get env 1]]
    if {"$sCheckTypeName" == ""} {
        set sCheckTypeName "TRUE"
    }
    set sToObjectId [mql get env TOOBJECTID]
    set sFromObjectId [mql get env FROMOBJECTID]
    set sRelationship [mql get env RELTYPE]
    set sToType [mql get env TOTYPE]
    set sToName [mql get env TONAME]
    set sFromType [mql get env FROMTYPE]
    set sFromName [mql get env FROMNAME]

    if {"$sCheckTypeName" == "FALSE"} {
        if {$sToObjectId == $sFromObjectId} {
            set sMsg [mql execute program emxMailUtil -method getMessage \
                          "emxFramework.ProgramObject.eServicecommonTrigcCheckSelfConnection_if.SelfConnect" 1 \
                                  "Rel" "$sRelationship" \
                          "" \
                     ]
            mql notice "$sMsg"
            exit 1
        } else {
            exit 0
        }
    } else {
        set checkName [string compare $sFromName $sToName];
         set checkType [string compare $sFromType $sToType];
        if { ( $checkType == 0 ) && ( $checkName == 0 ) } {
            set sMsg [mql execute program emxMailUtil -method getMessage \
                          "emxFramework.ProgramObject.eServicecommonTrigcCheckSelfConnection_if.SelfOrRevConnect" 1 \
                                  "Rel" "$sRelationship" \
                          "" \
                     ]
            mql notice "$sMsg"
            exit 1
        } else {
            exit 0
        }
    }
}

# end eServicecommonCheckRelState_if

# End of Module

