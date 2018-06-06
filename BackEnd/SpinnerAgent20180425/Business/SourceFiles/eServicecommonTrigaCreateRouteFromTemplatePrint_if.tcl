###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigaCreateRouteFromTemplatePrint_if.tcl.rca $ $Revision: 1.46 $
#
# @libdoc       eServicecommonTrigaCreateRouteFromTemplatePrint_if
#
# @Library:     Interface to create a Route from a Template Route
#
# @Brief:       Create a Route based off a Template Route Definition allowing
#               for the use of a Print statement to navigate to related
#               information, if desired.
#
# @Description: see procedure description
#
# @libdoc       Copyright (c) 2001, Matrix One, Inc. All Rights Reserved.
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
eval  [utLoad eServiceSchemaVariableMapping.tcl]
eval  [utLoad eServicecommonCreateRouteFromTemplate.tcl]
eval  [utLoad eServicecommonTrigaCreateRouteFromTemplatePrint.tcl]
eval  [utLoad eServicecommonTranslation.tcl]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigaCreateRouteFromTemplatePrint_if.tcl
#
# @Brief:       Create a Route based off a Templeate Route Definition.
#
# @Description: This program will create a Route object and connect it to an
#               object when object is promoted.  Route will be associated to
#               the NEXT STATE and current Policy of the object being promoted.
#               Logic can be passed in through a mql print statement to determine
#               if a Route should be created.  The Print statement is an argument
#               passed into this program, if one is not supplied there is no
#               check for any related information and a Route will automatically
#               be created.  Another argument to be passed in will determine
#               whether Route should be automatically started (Promoted to In
#               Process) or if an email should be sent to the owner of the route
#               instructing him/her to manually Promote the Route.
#               Additional argument is available to specify the creation of a
#               blank route if the template route object does not exist.
#
# @Parameters:  Inputs via RPE:
#                     sType - Object's type
#                     sName - Name of Object
#                      sRev - Revision of Object
#               [mql env 1] - Name of Route Template to clone from.
#               [mql env 2] - Revision of Route Template to clone from.
#               [mql env 3] - Set owner of Route object to specified user
#                             Valid inputs:
#                                  Template Owner = Owner of Route Template cloned from
#                                  Object Owner = Owner of Object attached to Route
#                                  Current User = Current User Logged On (default)
#               [mql env 4] - Action for route when route is complete
#                             Valid inputs:
#                                  Promote = Promote Connected Object
#                                  Notify = Notify Route Owner (default)
#               [mql env 5] - Manner in which to initiate route.  If set to Manual
#                             notification will be given to owner to start route
#                             if set to Automatic, route will start immediately.
#                             Valid inputs:
#                                  Manual
#                                  Automatic (default)
#                             Note that if a blank route is created i.e. route template did
#                             not exist and Create flag (see below) is set, the Automatic setting
#                             will have no effect and manual notification will be given to owner
#                             to add members and tasks etc. and then to start route
#               [mql env 6] - Name of vault to put route.  If left blank the
#                             safety vault will be used.
#                             Valid inputs:
#                                  Safety Vault = Use eServiceProduction Vault
#                                  User Vault = Vault user logged in with
#                                  symbolic name for vault = A vaults symbolic name
#                                      ex. vault_Manufacturing
#               [mql env 7] - Name of state to associate state to. If left blank
#                             route will be associated to next state.
#                             Valid Inputs:
#                                  Ad Hoc - Don't associate to any state
#                                  NEXTSTATE - Associate to Next State (default)
#               [mql env 8] - Any valid mql print select statement that will obtain a single
#                             value.  A list of matches can be returned from the select statment.
#                             Only the last value returned from the select statement will be
#                             operated on (Multiple return values will not be operated on, only
#                             the last one).  All references to schema object definitions must be
#                             entered with there symbolic name.
#               [mql env 9] - Comparison Operator to check state with. Valid entries are
#                             LT, GT, EQ, LE, GE, and NE.
#              [mql env 10] - Value to compare against.
#              [mql env 11] - Result Operator.  All values returned from select statement are
#                             compared against passed in value [mql env 10].  The operator passed
#                             in here will determine whether or not a Route should be created.
#                             Valid Inputs:
#                                  All (default) - All values must pass comparison in order
#                                                  to create Route.
#                                  SUMMATION - If the product of all values pass the comparison
#                                              create Route.
#                                  NONE - If none of the values pass the comparison create Route.
#                                  ANY - If any of the values pass the comparions create Route.
#              [mql env 12] - Flag as to whether to create a blank route if route template object
#                             specified does not exist.
#                             Valid Inputs:
#                                  CREATE - Create the route object even if the route template
#                                           object does not exist.
#                                  null value - error out if route template object does not exist.
#                             Default is null value.
#
# @Returns:     0 if successful
#               1 if not successful
#
# @Usage:       For use as trigger action on promotion
#
# @Example:     configure mxTrigMgr thusly:
#               [mql env 1] - Sheet Metal
#               [mql env 2] - -
#               [mql env 3] - Object Owner
#               [mql env 4] - Promote
#               [mql env 5] - Automatic
#               [mql env 6] - vault_eServiceSample
#               [mql env 7] - NEXTSTATE
#               [mql env 8] - from[relationship_ECR].businessobject.attribute[attribute_Cost]
#               [mql env 9] - EQ
#              [mql env 10] - 5
#              [mql env 11] - SUMMATION
#              [mql env 12] -
#
# @procdoc
#******************************************************************************
    #
    # Get Program names
    #
    set progname      "eServicecommonTrigaCreateRouteFromTemplatePrint_if.tcl"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"

    mql verbose off;

    #
    # Get data values from RPE
    #
    set sType               [mql get env TYPE]
    set sName               [mql get env NAME]
    set sRev                [mql get env REVISION]
    set sObjectId           [mql get env OBJECTID]
    set sTemplateName       [mql get env 1]
    set sTemplateRev        [mql get env 2]
    set sRouteOwner         [mql get env 3]
    set sRouteAction        [mql get env 4]
    set sStartRoute         [mql get env 5]
    set sRouteVault         [mql get env 6]
    set sStateName          [mql get env 7]
    set sSelect             [mql get env 8]
    set sComparisonOperator [string toupper [mql get env 9]]
    set sValue              [mql get env 10]
    set sResultOperator     [string toupper [mql get env 11]]
    set sRouteCreateFlag    [string toupper [mql get env 12]]

    #
    # Set state for route relationship
    #
    if {[string tolower $sStateName] == "ad hoc"} {
        set sRouteBaseState "Ad Hoc"
        set sRouteBasePolicy ""
    } else {
        #
        # Set policy for route relationship
        #
        set sPolicy [mql get env POLICY]
        set sRouteBasePolicy [emxGetPropertyFromAdminName policy $RegProgName "$sPolicy"]

        set sNextState [mql get env NEXTSTATE]
        if {"$sNextState" == ""} {
           set sNextState [mql get env CURRENTSTATE]
        }
        set sRouteBaseState [emxGetPropertyFromStateName "$sPolicy" "$sNextState"]
    }

    #
    # Initialize check variables
    #
    set mqlret 0
    set outStr ""

    #
    # Get schema mapping for symbolic name
    #
    set sTemplateType [eServiceGetCurrentSchemaName type $RegProgName type_RouteTemplate]

    #
    # Error out if not registered
    #
    if {"$sTemplateType" == ""} {
        set mqlret 1
        set outStr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigaCreateRouteFromTemplatePrint_if.InvalidType" 0 \
                    "" \
                    ]
    }

    #
    # Determine which vault to use for Route
    #
    switch [string trim [string tolower $sRouteVault]] {
        "" {
            set sRouteVault [eServiceGetCurrentSchemaName vault $RegProgName vault_eServiceAdministration]
        }

        "safety vault" {
            set sRouteVault [eServiceGetCurrentSchemaName vault $RegProgName vault_eServiceAdministration]
        }

        "user vault" {
            #
            # If user vault is ADMINISTRATION set to safety vault, else use default vault
            #
            if {[mql get env VAULT] == "ADMINISTRATION"} {
                set sRouteVault [eServiceGetCurrentSchemaName vault $RegProgName vault_eServiceAdministration]
            } else {
                set sRouteVault [mql get env VAULT]
            }
        }

        default {
            set sVault $sRouteVault
            #
            # Get schema mapping for symbolic name
            #
            set sRouteVault [eServiceGetCurrentSchemaName vault $RegProgName $sVault]

            #
            # Error out if not registered
            #
            if {"$sRouteVault" == ""} {
                set mqlret 1
                set outStr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigaCreateRouteFromTemplatePrint_if.InvalidVault" 1 \
                                  "Vault" "$sVault" \
                                "" \
                                ]
            }
        }
    }

    #
    # If a select statement is entered Validate Symbolic Names
    #
    set sSelectInfo $sSelect
    while {[regexp {\]} $sSelectInfo] != 0} {
        #
        # Get last symbolic name entry from sSelectInfo
        #
        regsub {^(.)*\[} $sSelectInfo "" sNewValues
        regsub {\](.)*$} $sNewValues "" sSymbolicName

        #
        # Get Schema type
        #
        set sSchemaType [lindex [split $sSymbolicName _] 0]

        #
        # Get schema mapping for symbolic name
        #
        set sSchemaMap [eServiceGetCurrentSchemaName $sSchemaType $RegProgName $sSymbolicName]

        #
        # Error out if not registered
        #
        if {$sSchemaMap == ""} {
            set mqlret 1
            set outStr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigaCreateRouteFromTemplatePrint_if.InvalidAny" 2 \
                                  "Name" "$sSymbolicName" \
                                  "Type" "$sSchemaType" \
                                "" \
                                ]
            break
        }

        #
        # Replace symbolic names with real schema names
        #
        regsub -all "$sSymbolicName" $sSelect "$sSchemaMap" sSelect

        #
        # Remove last symbolic entry and redefine sSelectInfo
        #
        regexp {^(.)*\[} $sSelectInfo sSelectInfo
        regsub {\[$} $sSelectInfo "" sSelectInfo
    }

    #
    # Display error message if any errors from above
    #
    if {$mqlret == 1} {
        set sErrMsg "$progname :\n"
        mql notice "$sErrMsg $outStr"
    } else {
        #
        # Execute command
        #
        set mqlret [eServicecommonCreateRouteFromTemplatePrint $sSelect $sObjectId $sValue \
                    $sResultOperator $sComparisonOperator $sType $sName $sRev $sTemplateType \
                    $sTemplateName $sTemplateRev $sRouteOwner $sRouteAction $sStartRoute \
                    $sRouteBaseState $sRouteVault $sRouteBasePolicy $sRouteCreateFlag]
    }

    exit $mqlret
}
# end eServicecommonTrigaCreateRouteFromTemplatePrint_if


# End of Module

