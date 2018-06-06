###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: emxcommonTrigaNotifyNewOwner_if.tcl.rca $ $Revision: 1.43 $
#
# @libdoc       emxcommonTrigaNotifyNewOwner_if
#
# @Library:     Interface for changing ownership of an object.
#
# @Brief:       Change the ownership of an object to a specified user or
#               user logged on.
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

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      emxcommonTrigaNotifyNewOwner_if
#
# @Brief:       Notifies change of ownership.
#
# @Description: This program will notify new owner about assignment of object.
#
# @Parameters:  Inputs via RPE:
#                     sObjId      - Object's Id
#                     [mql env 1] - Subject of message
#                     [mql env 2] - Message
#                     [mql env 3] - TRUE/FALSE whether business object is required or not
#                     [mql env 4] - Base Name of the property file where subject and text 
#                                   should be looked up.
#                                   If this is not provided
#                                   by default it will look up in
#                                   property files with base name emxFrameworkStringResource.
# @Returns:     0 if successfull
#               1 if not successfull
#
# @Usage:       For use as trigger action on change owner
#
# @procdoc
#******************************************************************************
    # Get Program names
    set progname      [mql get env 0]
    set RegProgName   "eServiceSchemaVariableMapping.tcl"

    # get agent name from property.
    set userAgent [eServiceGetCurrentSchemaName person "eServiceSchemaVariableMapping.tcl" "person_UserAgent"]

    mql verbose off

    # Load Schema Mapping
    eval [utLoad $RegProgName]

    # Get data values from RPE
    set sObjId [mql get env OBJECTID]
    set sNewOwner [mql get env NEWOWNER]
    set sRealOwner [mql get env APPREALUSER]
    #set sUser [mql get env USER]
    set sSubject [mql get env 1]
    set sMessage [mql get env 2]
    set sBusinessObjectRequired [string toupper [mql get env 3]]
    set sBasePropFile [mql get env 4]

    # Don not send a message if the current owner is person_UserAgent. If this is the case
    # the change owner operation is just a background process and no mail notification 
    # should be sent. Jump out of this procedure right away. 


    if {"$sNewOwner" == "$sRealOwner"} {
        return 0
    }

    # Default business object required to TRUE
    if {"$sBusinessObjectRequired" != "FALSE"} {
        set sBusinessObjectRequired "TRUE"
    }

    # If base property file is not provided then default it to emxFrameworkStringResource
    if {"$sBasePropFile" == ""} {
        set sBasePropFile "emxFrameworkStringResource"
    }

    # Initialize Error variables
    set mqlret 0
    set outStr ""

    if {"$sBusinessObjectRequired" == "TRUE"} {
        set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sNewOwner" \
                              "$sSubject" 0 \
                              "$sMessage" 0 \
                              "$sObjId" \
                              "" \
                              "$sBasePropFile" \
                              }
    } else {
        set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sNewOwner" \
                              "$sSubject" 0 \
                              "$sMessage" 0 \
                              "" \
                              "" \
                              "$sBasePropFile" \
                              }
    }
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret == 1} {
        mql notice "$progname :\n\n$outStr"
        return 1
    } else {
        return $mqlret
    }
}
# end emxcommonTrigaNotifyNewOwner_if


# End of Module

