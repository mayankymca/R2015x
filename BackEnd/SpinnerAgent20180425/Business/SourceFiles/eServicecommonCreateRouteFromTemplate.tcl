###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonCreateRouteFromTemplate.tcl.rca $ $Revision: 1.49 $
#
# @procdoc      eServicecommonCreateRouteFromTemplate
#
# @Brief:       Create a Route from a Route Template.
#
# @Description: This program will create a Route object and connect it to
#               object.  Route will either be automatically started or an email
#               will be sent to the owner of the route and the owner will have to
#               start the route manually.
#
# @Parameters:        sType - Type of object connected to route
#                     sName - Name of object connected to route
#                      sRev - Revision of object connected to route
#             sTemplateType - Type of Route Template
#             sTemplateName - Name of Route Template
#              sTemplateRev - Revision of Route Template
#               sRouteOwner - Set owner of Route object to specified user
#                             Valid inputs:
#                                     Template Owner = Owner of Route Template cloned from
#                                     Object Owner = Owner of Object attached to Route
#                                     Current User = Current User Logged On (default)
#              sRouteAction - Action to perform when route is completed
#                             Valid Inputs:
#                                     Promote = Promote Connected Object
#                                     Notify = Notify Route Owner (default)
#               sStartRoute - Manner in which to initiate route.  If set to Manual
#                             notification will be given to owner to start route
#                             if set to Automatic, route will start immediately.
#                             Valid inputs:
#                                     Manual
#                                     Automatic (default)
#                             Note that if a blank route is created i.e. route template did
#                             not exist and sRouteCreateFlag (see below) is set, the Automatic setting
#                             will have no effect and manual notification will be given to owner
#                             to add members and tasks etc. and then to start route
#           sRouteBaseState - Name of state to associated route to (symbolic name).
#                             If no value is passed then "Ad Hoc" will be set.
#               sRouteVault - Name of vault to put route.  If left blank the
#                             users current set vault will be used.
#          sRouteBasePolicy - Name of Policy for object (symbolic name)
#         sRouteCreateFlag - (optional) Flag as to whether to create a blank route if route template object
#                            specified does not exist.
#                             Valid Inputs:
#                                  CREATE - Create the route object even if the route template
#                                           object does not exist.
#                                  null value - error out if route template object does not exist.
#                             Default is null value.
#
# @Returns:     0 if successful
#               1 if not successful
#
# @Usage:       For use for triggers that need to create a Route for a object
#               from a Route Template.
#
# @progdoc      Copyright (c) 2001, Matrix One, Inc. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
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

proc eServicecommonCreateRouteFromTemplate {sType sName sRev sTemplateType sTemplateName sTemplateRev sRouteOwner sRouteAction sStartRoute sRouteBaseState {sRouteVault ""} sRouteBasePolicy {sRouteCreateFlag ""}} {
    eval [utLoad eServicecommonShadowAgent.tcl]    
    eval [utLoad eServicecommonTranslation.tcl]

    #
    # Set Current User
    #
    set sUser [mql get env USER]

    #
    # Set context
    #
    pushShadowAgent

    #
    # Get Program name
    #
    set progname "eServicecommonCreateRouteFromTemplate.tcl"
    set RegProgName "eServiceSchemaVariableMapping.tcl"

    #
    # Initialize Variables
    #
    set iReturn 0
    set outStr ""
    set mqlret 0
    set sCmd ""
    set bExistsFlag 0

    #
    # Get schema definitions for route object
    #
    set sRelObject [eServiceGetCurrentSchemaName relationship $RegProgName relationship_ObjectRoute]
    set sBaseState [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteBaseState]
    set sBasePolicy [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteBasePolicy]
    set sActionAttr [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteCompletionAction]
    set sRouteTaskUserAttr [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteTaskUser]
    set sRouteType [eServiceGetCurrentSchemaName type $RegProgName type_Route]
    set sPersonType [eServiceGetCurrentSchemaName type $RegProgName type_Person]
    set sRoutePolicy [eServiceGetCurrentSchemaName policy $RegProgName policy_Route]
    set sRelInitiating [eServiceGetCurrentSchemaName relationship $RegProgName relationship_InitiatingRouteTemplate]
    set sRelProjectRoute [eServiceGetCurrentSchemaName relationship $RegProgName relationship_ProjectRoute]
    set sRelRouteNode [eServiceGetCurrentSchemaName relationship $RegProgName relationship_RouteNode]

    # Access Grant
    set sReadWriteAccess "show,read,checkout,checkin,modify,lock,unlock,revise"

    if {$mqlret == 0} {
        #
        # Set owner for route
        #
        switch [string tolower $sRouteOwner] {
            "template owner" {
                    set sCmd {mql print bus "$sTemplateType" "$sTemplateName" "$sTemplateRev" select owner dump}
                    set mqlret [catch {eval $sCmd} outStr]
                    set sRouteOwner $outStr
                }

            "object owner" {
                    set sCmd {mql print bus "$sType" "$sName" "$sRev" select owner dump}
                    set mqlret [catch {eval $sCmd} outStr]
                    set sRouteOwner $outStr
                }

            default {
                    set sRouteOwner $sUser
                }
        }

        #
        # Set route action
        #
        if {[string tolower $sRouteAction] == "promote"} {
            set sRouteAction "Promote Connected Object"
        } else {
            set sRouteAction "Notify Route Owner"
        }
    }

    #
    # Check if template object exists
    #
    if {$mqlret == 0} {
        set sCmd {mql print bus "$sTemplateType" "$sTemplateName" "$sTemplateRev" select exists dump}
        set mqlret [catch {eval $sCmd} outStr]


        if {$mqlret == 0} {
            if {[string tolower [string trim $outStr]] == "false"} {
                if {$sRouteCreateFlag == "CREATE"} {
                    set bExistsFlag 1
                    set sStartRoute manual
                } else {
                    set outStr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonCreateRouteFromTemplate.NoTemplate" 3 \
                                  "Type" "$sTemplateType" \
                                  "Name" "$sTemplateName" \
                                  "Rev" "$sTemplateRev" \
                                "" \
                                ]
                    set mqlret 1
                }
            }
        }
    }

    #
    # Get next route number
    #
    #Call eServicecommonNameGenerator.tcl, which has transaction boundary 
    set lResult [mql execute program eServicecommonNameGenerator.tcl "$sRouteType" "-" "$sRoutePolicy"]     
    #the first in the lItem is the result status. The second is the id
    set lItem [split $lResult |]
    if {[llength $lItem] != 2} { 
        set mqlret 1
        set outStr "Error: $lItem"
    } else {
        set sRouteId [lindex $lItem 1]
        set sCmd {mql print bus "$sRouteId" select name revision dump |}
        set mqlret [catch {eval $sCmd} retStr]
        if {$mqlret == 0} {
            set lResult [split $retStr |]
            if {[llength $lResult] != 2} {
                set mqlret 1
                set outStr "Error: $lResult"
            } else {
                set sRouteName [lindex $lResult 0]
                set sRouteRev [lindex $lResult 1]        
            }
        }
    }

    #
    # Delete Route after retrieving name for Route
    # only if template exists otherwise this is the new blank route
    if {$mqlret == 0 && $bExistsFlag == 0} {
        set sCmd {mql delete bus $sRouteId}
        set mqlret [catch {eval $sCmd} outStr]
    }

    #
    # Create new Route from Template
    #
    if {$mqlret == 0 && $bExistsFlag == 0} {
        #
        # If no vault passed in use user default vault
        #
        if {"$sRouteVault" == ""} {
            set sCmd {mql copy bus "$sTemplateType" "$sTemplateName" "$sTemplateRev" to "$sRouteName" "$sRouteRev" \
                              owner "$sRouteOwner" type "$sRouteType" policy "$sRoutePolicy"}
        } else {
            set sCmd {mql copy bus "$sTemplateType" "$sTemplateName" "$sTemplateRev" to "$sRouteName" "$sRouteRev" \
                              owner "$sRouteOwner" type "$sRouteType" policy "$sRoutePolicy" vault "$sRouteVault"}
        }
        set mqlret [catch {eval $sCmd} outStr]
    }
      #
      # Code for updating the attribute Route Node id on Route Node Relationship
      #
        
    set sCmd {mql print bus "$sRouteType" "$sRouteName" "$sRouteRev" \
                      select \
                      from\[$sRelRouteNode\].id \
                      dump | \
                      }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret == 0} {
            set lRouteNodes [split $outStr |]
            foreach sEachObject $lRouteNodes {
               set sCmd {mql modify connection  $sEachObject "Route Node ID" "$sEachObject"}
              set mqlret [catch {eval $sCmd} outStr]
              
            }
    }



    #
    # Grant access to route members if template exists.
    #
    if {$mqlret == 0 && $bExistsFlag == 0} {
        set sCmd {mql print bus "$sRouteType" "$sRouteName" "$sRouteRev" \
                      select \
                      from\[$sRelRouteNode\].attribute\[$sRouteTaskUserAttr\] \
                      dump | \
                      }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret == 0} {
            # Push context to Workspace Access Grantor.
            pushShadowAgent person_WorkspaceAccessGrantor

            set lRouteMembers [split $outStr |]
            set lPersonWithQuotes {}
            foreach sRouteTaskUser $lRouteMembers {

                if {"$sRouteTaskUser" != ""} {
                    
                    set sGroupOrRole [ string tolower [ lindex [ split $sRouteTaskUser _ ] 0 ] ]
                    set sRouteTaskUserValue [eServiceGetCurrentSchemaName $sGroupOrRole $RegProgName "$sRouteTaskUser"]
                    lappend lPersonWithQuotes "\"$sRouteTaskUserValue\""
                }
            }

            # Grant Read-Write acceses
            if {[llength $lPersonWithQuotes] > 0} {
                set sPersonList [join $lPersonWithQuotes " , "]

                set sCmd "mql modify bus \"$sRouteType\" \"$sRouteName\" \"$sRouteRev\" grant $sPersonList access $sReadWriteAccess"
                set mqlret [ catch {eval $sCmd} outStr ]
            }

            # Pop Context
            popShadowAgent
        }
        set sCmd {mql expand bus "$sRouteType" "$sRouteName" "$sRouteRev" \
                      from relationship "$sRelRouteNode" type "$sPersonType" \
                      recurse to 1 \
                      select bus \
                      name \
                      dump | \
                      }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret == 0} {
            # Push context to Workspace Access Grantor.
            pushShadowAgent person_WorkspaceAccessGrantor

            set lContentInfo [split $outStr \n]
            set lRouteMembers [split $outStr |]
            set lPersonWithQuotes {}
            foreach sEachObject $lContentInfo {
                set lEachObject [split $sEachObject |]
                set sRouteMember [lindex $lEachObject 6]
                lappend lPersonWithQuotes "\"$sRouteMember\""
            }
            set sPersonList [join $lPersonWithQuotes " , "]

            # Grant Read-Write acceses
            if {[llength $lRouteMembers] > 0} {
                set sCmd "mql modify bus \"$sRouteType\" \"$sRouteName\" \"$sRouteRev\" grant $sPersonList access $sReadWriteAccess"
                set mqlret [ catch {eval $sCmd} outStr ]

            }

            # Pop Context
            popShadowAgent
        }
    }
    #
    # Modify new blank Route if no Template exists
    #
    if {$mqlret == 0 && $bExistsFlag == 1} {
        #
        # If no vault passed in use user default vault
        #
        if {"$sRouteVault" == ""} {
            set sCmd {mql modify bus $sRouteId owner "$sRouteOwner"}
        } else {
            set sCmd {mql modify bus $sRouteId owner "$sRouteOwner" vault "$sRouteVault"}
        }
        set mqlret [catch {eval $sCmd} outStr]
    }

   # Connect ECO owner to the route
        if {$mqlret == 0} {
            set sCmd {mql connect bus $sRouteType $sRouteName $sRouteRev relationship "$sRelProjectRoute" to "$sPersonType" "$sRouteOwner" "-"}
            set mqlret [catch {eval $sCmd} outStr]
       }

    #
    # Modify route action attribute
    #
    if {$mqlret == 0 && "$sType" != ""} {
        set sCmd {mql modify bus $sRouteType $sRouteName $sRouteRev $sActionAttr $sRouteAction}
        set mqlret [catch {eval $sCmd} outStr]
    }

    #
    # Connect Route to Route Template if template exists
    #
    if {$mqlret == 0 && $bExistsFlag == 0} {
        set sCmd {mql connect bus $sTemplateType $sTemplateName $sTemplateRev rel $sRelInitiating from \
                          $sRouteType $sRouteName $sRouteRev}
        set mqlret [catch {eval $sCmd} outStr]
    }

    #
    # Connect object to route
    #
    if {$mqlret == 0 && "$sType" != ""} {
        if {"$sRouteBaseState" == ""} {
            set sCmd {mql connect bus $sRouteType $sRouteName $sRouteRev rel $sRelObject from \
                              $sType $sName $sRev $sBaseState "Ad Hoc"}
            set mqlret [catch {eval $sCmd} outStr]
        } else {
            set sCmd {mql connect bus $sRouteType $sRouteName $sRouteRev rel $sRelObject from \
                              $sType $sName $sRev $sBaseState $sRouteBaseState $sBasePolicy $sRouteBasePolicy}
            set mqlret [catch {eval $sCmd} outStr]
        }

        if {$mqlret == 0} {
            if {[string tolower $sStartRoute] == "manual"} {
                #
                # Notify User he/she needs to start route
                #
                set sObjectId [mql print bus "$sType" "$sName" "$sRev" select id dump]
                if {$bExistsFlag == 0} {
                    set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                    "$sRouteOwner" \
                                    "emxFramework.ProgramObject.eServicecommonCreateRouteFromTemplate.SubjectAssignment" 0 \
                                    "emxFramework.ProgramObject.eServicecommonCreateRouteFromTemplate.MessageAssignment" 6 \
                                        "RType" "$sRouteType" \
                                        "RName" "$sRouteName" \
                                        "RRev" "$sRouteRev" \
                                        "OType" "$sType" \
                                        "OName" "$sName" \
                                        "ORev" "$sRev" \
                                    "$sObjectId" \
                                    "" \
                                    }
                } else {
                    set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                    "$sRouteOwner" \
                                    "emxFramework.ProgramObject.eServicecommonCreateRouteFromTemplate.SubjectAssignment" 0 \
                                    "emxFramework.ProgramObject.eServicecommonCreateRouteFromTemplate.MessageBlankRouteAssignment" 6 \
                                        "RType" "$sRouteType" \
                                        "RName" "$sRouteName" \
                                        "RRev" "$sRouteRev" \
                                        "OType" "$sType" \
                                        "OName" "$sName" \
                                        "ORev" "$sRev" \
                                    "$sObjectId" \
                                    "" \
                                    }
                }
                popShadowAgent

                set mqlret [catch {eval $sCmd} outStr]
            } else {
                #
                # Promote Route to get it started
                #
                set sCmd {mql promote bus $sRouteType $sRouteName $sRouteRev}
                set mqlret [catch {eval $sCmd} outStr]
                popShadowAgent
            }
        }
    }

    if {$mqlret != 0} {
        set sErrMsg "Error - From program $progname\n"

        #
        # Change single quotes to grave qoute (Bug)
        #
        regsub -all {'} $outStr "`" outStr
        mql notice "$sErrMsg $outStr $sCmd"
        popShadowAgent
    }

    return $mqlret
}

