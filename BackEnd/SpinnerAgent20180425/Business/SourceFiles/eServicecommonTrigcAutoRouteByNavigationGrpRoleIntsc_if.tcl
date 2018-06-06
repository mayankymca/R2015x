###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc_if.tcl.rca $ $Revision: 1.42 $
#
# @libdoc       eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc_if
#
# @Library:     Interface for required person checks for triggers
#
# @Brief:       Retrieve user(s) for routing, email messages and reassignment
#               through specified navigation path.
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
eval  [ utLoad eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc_if.tcl
#
# @Brief:       Determine user(s) for routing, reassignment and email messages.
#               User(s) are determined by the intersection of specified Group 
#               and Role.
#
# @Description: Determine user(s) for routing, reassignment and email messages.
#               User(s) are determined by the intersection of specified Group 
#               and Role.  Group is determined by specified navigation path
#               and Role is Explicitly entered as a argument.
#
# @Parameters:  Inputs via RPE:
#                     sType - Parent object's type
#                     sName - Parent object's name
#                      sRev - Parent object's revision number
#               [mql env 1] - Any valid mql select statement that will obtain
#                             a valid Group name.  All references to schema 
#                             object definitions must be entered with there 
#                             symbolic name.
#               [mql env 2] - Operation to be executed.  Valid operations are
#                             SEND, REASSIGN and ROUTE.  SEND will send email
#                             message to all users that intersect group and role.
#                             REASSIGN will change owner to user identified by
#                             the group role intersection.  If more than one user is
#                             identified, first user will be selected.  ROUTE will
#                             reassign and send email message to first user 
#                             identified in group role intersection.  All other
#                             users will be emailed of the action.
#               [mql env 3] - Name of role to intersect with group.  Symbolic name
#                             must be used.
#               [mql env 4] - Subject for email notifications.
#               [mql env 5] - Text message for email notifications.
#               [mql env 6] - Set flag to give confirmation that operation was
#                             successfully completed.  1 or null entry will turn flag
#                             on, anything else turns flag off.
#               [mql env 7] - Set flag to pass TNR of object into email messages.
#                             1 or null entry will turn flag on, anything else
#                             turns flag off.
#               [mql env 8] - Base Name of the property file where subject and text 
#                             should be looked up.
#                             If this is not provided
#                             by default it will look up in
#                             property files with base name emxFrameworkStringResource.
# @Returns:     0 if successful
#               1 if not successful
#
# @Usage:       For use as trigger action on promotion
#
# @Example:     configure mxTrigMgr thusly:
#               [mql env 1] - from[relationship_ECR].businessobject.attribute[attribute_ChangeBoard]
#               [mql env 2] - ROUTE
#               [mql env 3] - role_ECRCoordinator
#               [mql env 4] - Route To ECR Coordinator
#               [mql env 5] - Attached ECR needs your review, please review.
#               [mql env 6] - 1
#               [mql env 7] - 1
#
# @procdoc
#******************************************************************************

    # Get Program names
    set progname      "eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc_if"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"

    # Load Schema Mapping
    eval [utLoad $RegProgName]
    
    # Get data values from RPE
    set sType     [ mql get env TYPE ]
    set sName     [ mql get env NAME ]
    set sRev      [ mql get env REVISION ]
    mql verbose off;
    set sSelect    [mql get env 1]
    set sSendOpt   [mql get env 2]
    set sRole      [mql get env 3]
    set sSubject   [mql get env 4]
    set sText      [mql get env 5]
    set bNotify    [mql get env 6]
    set bPassObj   [mql get env 7]
    set sBasePropFile [mql get env 8]


    if {$sBasePropFile == ""} {
        set sBasePropFile "emxFrameworkStringResource"
    }
  
    # Initialize Error variables
    set mqlret 0
    set outStr ""
    
    # Validate Symbolic Names from select statement
    set sSelectInfo $sSelect
    while {[regexp {\]} $sSelectInfo] != 0} {
        # Get last symbolic name entry from sSelectInfo
        regsub {^(.)*\[} $sSelectInfo "" sNewValues
        regsub {\](.)*$} $sNewValues "" sSymbolicName
    
        # Get Schema type
        set sSchemaType [lindex [split $sSymbolicName _] 0]
    
        # Get schema mapping for symbolic name
        set sSchemaMap [eServiceGetCurrentSchemaName $sSchemaType $RegProgName $sSymbolicName]
        
        # Error out if not registered
        if {$sSchemaMap == ""} {
            set mqlret 1
     	    break
        }
    
        # Replace symbolic names with real schema names
        regsub -all "$sSymbolicName" $sSelect "$sSchemaMap" sSelect
        
        # Remove last symbolic entry and redefine sSelectInfo
        regexp {^(.)*\[} $sSelectInfo sSelectInfo
        regsub {\[$} $sSelectInfo "" sSelectInfo
    }
    
    if {$mqlret == 0} {
        # Get role mapping
     	set sRole [eServiceGetCurrentSchemaName role $RegProgName $sRole]
     	
     	if {$sRole == ""} {
     	    # Error out if not registered
     	    set mqlret 1
        }
    }
    
    if {$mqlret == 0} {
        # Execute command
        set mqlret [eServicecommonAutoRouteByNavigationGrpRoleIntsc $sType $sName $sRev $sSelect \
                           $sSendOpt $sRole $sSubject $sText $bNotify $bPassObj $sBasePropFile]
    }
    
    if {$mqlret == 1} {
        error "ERRORS"
    } else {
        #return -code $mqlret
        exit $mqlret
    }
}
# end eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc_if


# End of Module

