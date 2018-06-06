###############################################################################
# $RCSfile: eServicecommonTrigcAutoRouteByAttribute_if.tcl.rca $ $Revision: 1.44 $
#
# @libdoc       eServicecommonTrigcAutoRouteByAttibute_if.tcl
#
# @Library:     Procedure for simple auto route action
#
# @Brief:       Routes or notifies person named in specified attribute.
#
# @Description: This program automatically routes, sends, or reassigns an
#                    object to the user who is indicated in the specified attribute.
#                    This user can be a person, group, or role.
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

#******************************************************************************
# @procdoc      eServicecommonTrigcAutoRouteByAttibute_if
#
# @Brief:       Routes or notifies person named in specified attribute.
#
# @Description: This procedure is passed in the name of an
#               attribute to read.  This attribute contains the
#               target to which the operation relates.  The attribute
#               can contain a user, role or group identifier.
#
#               The sendoption method instructs this procedure as
#               to the operation to perform on behalf of the target
#               object:
#
#               send - send a message and object copy to target
#               assign - assign target as new owner of this object
#               route - perform both operations
#
#               If the sendoption is assign or route, this object will be
#               owned by the target (person / group / role) specified.
#
# @Parameters:  attributeName  -- attribute used to get 'user name'
#               sendoption     -- action to be performed (route/send/reassign)
#               subject        -- the subject text of the message if sendoption
#                                 is route or send (opitonal)
#               text           -- mail contents.
#               passObject     -- Wheather to pass selected object name into mail.
#                                 TRUE   -- Pass object name into mail
#                                 FALSE  -- Don't pass object name into mail.
#               notify         -- Set flag to give confirmation that operation was
#                                 successfully completed.  TRUE or null entry will turn flag
#                                 on, anything else turns flag off.
#               base property file -- Base Name of the property file where subject and text 
#                                     should be looked up.
#                                     If this is not provided
#                                     by default it will look up in
#                                     property files with base name emxFrameworkStringResource.
#
# @Returns:     0 for success; 1 for failure
#
# @Usage:
#
# @procdoc
#*******************************************************************************

tcl;
eval {

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

    set sOutput [ mql print program $sProgram select code dump ]

    return $sOutput
}
# end utload

###############################################################################

mql verbose off;

# Load MQL/Tcl utility procedures
eval  [utLoad eServiceSchemaVariableMapping.tcl]
eval  [utLoad eServicecommonAutoRouteByAttribute.tcl]

# get the input arguments
set attributeName  [string trim [mql get env 1]]
set sendoption     [string tolower [string trim [mql get env 2]]]
set subject        [string trim [mql get env 3]]
set text           [string trim [mql get env 4]]
set passObject     [string toupper [string trim [mql get env 5]]]
set notify         [string toupper [string trim [mql get env 6]]]
set sBasePropFile  [mql get env 7]

# get the TNR of the object
set sType  [ mql get env TYPE ]
set sName  [ mql get env NAME ]
set sRev   [ mql get env REVISION ]

# set program related variables
set progname    "eServicecommonTrigcAutoRouteByAttibute_if"
set RegProgName "eServiceSchemaVariableMapping.tcl"
set mqlret      0
set outStr      ""

# Get absolute name of attribute and owner

set sAbsAttName    [eServiceGetCurrentSchemaName  attribute $RegProgName  $attributeName]

if {$subject == "NULL"} {
    set subject ""
}

if {$text == "NULL"} {
    set text ""
}

# reassign input added to make it consistant with all other trigger programs
if {"$sendoption" == "reassign"} {
    set sendoption "assign"
}

# If base property file is not provided then default it to emxFrameworkStringResource
if {"$sBasePropFile" == ""} {
    set sBasePropFile "emxFrameworkStringResource"
}

set mqlret [catch {eval eServicecommonAutoRouteByAttribute {$sType} {$sName} {$sRev} {$sAbsAttName} {$sendoption} {$subject} {$text} {$passObject} {$notify} {$sBasePropFile}} outStr]

exit $mqlret
}
# end eServicecommonTrigcAutoRouteByAttibute_if

