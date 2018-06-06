###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.tcl.rca $ $Revision: 1.46 $
#
# @libdoc       eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc
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

###############################################################################
#
# Define Global Variables
#
###############################################################################


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [ utLoad eServicecommonShadowAgent.tcl ]
eval  [ utLoad eServicecommonTranslation.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonAutoRouteByNavigationGrpRoleIntsc
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
#                   sSelect - Any valid mql select statement that will obtain
#                             a valid Group name.  All references to schema
#                             object definitions must be entered with there
#                             symbolic name.
#                  sSendOpt - Operation to be executed.  Valid operations are
#                             SEND, REASSIGN and ROUTE.  SEND will send email
#                             message to all users that intersect group and role.
#                             REASSIGN will change owner to user identified by
#                             the group role intersection.  If more than one user is
#                             identified, first user will be selected.  ROUTE will
#                             reassign and send email message to first user
#                             identified in group role intersection.  All other
#                             users will be emailed of the action.
#                     sRole - Name of role to intersect with group.  Symbolic name
#                             must be used.
#                  sSubject - Subject for email notifications.
#                     sText - Text message for email notifications.
#                   bNotify - Set flag to give confirmation that operation was
#                             successfully completed.  1 or null entry will turn flag
#                             on, anything else turns flag off.
#                  bPassObj - Set flag to pass TNR of object into email messages.
#                             1 or null entry will turn flag on, anything else
#                             turns flag off.
#               sBasePropFile  -- Base Name of the property file where subject and text 
#                                     should be looked up.
#                                     If this is not provided
#                                     by default it will look up in
#                                     property files with base name emxFrameworkStringResource.
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
# @Usage:       For use as trigger action on promotion
#
# @procdoc
#******************************************************************************

proc eServicecommonAutoRouteByNavigationGrpRoleIntsc {sType sName sRev sSelect sSendOpt sRole sSubject sText bNotify bPassObj sBasePropFile} {

    # Get Program name
    set progname "eServicecommonAutoRouteByNavigation"

    # Initialize Variables
    set iReturn 0
    set outstr ""
    set mqlret 0
    set sCmd ""

    # Error out if a select statement was not entered
    if {[string trim $sSelect] == ""} {
        set mqlret 1
        set outstr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.NoSelect" 0 \
                    "" \
                    ]
    }

    if {$mqlret == 0} {
        # Create default subject if one is not supplied
        if {[string trim $sSubject] == ""} {
            set sSubject "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.Subject"
        }

        # Create default text for notification if one is not supplied
        if {[string trim $sText] == ""} {
            set sText "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.Message"
        }

        # Remove all slashes from select statment if any
        regsub -all {\\} $sSelect "" sSelect

        # Add slashes to command to escape brackets
        regsub -all {\[} $sSelect {\[} sSelect
        regsub -all {\]} $sSelect {\]} sSelect

        # Build Command String
        set sCmd {mql print bus "$sType" "$sName" "$sRev" select}
        append sCmd " $sSelect"
        append sCmd " dump |"

        # Execute Command
        set mqlret [catch {eval $sCmd} outstr]

        # Do not - Error out if no group found
        #  If the group is not found then just come out of the program as if it was successful
        if {$outstr == ""} {
        #    set mqlret 1
        #    set outstr [mql execute program emxMailUtil -method getMessage \
        #            "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.NoGroup" 0 \
        #            "" \
        #            ]
            set mqlret 0
            return $mqlret
        }

        # Verify groups exist
        if {$mqlret == 0} {
            set lUsersInGrp {}
            set lGroups [split $outstr |]

            # Get all users from Groups
            foreach sGroup $lGroups {
                # Get users from Group
                set sCmd {mql print group "$sGroup" select person dump |}

                #Execute command
                set mqlret [catch {eval $sCmd} outstr]

                # Error out if problem finding group
                if {$mqlret == 1} {
                    break
                }

                # Create list of users
                set lUsers [split $outstr |]

                # Add users to list if not already there
                foreach sUser $lUsers {
                    if {[lsearch -exact $lUsersInGrp "$sUser"] == -1} {
                        lappend lUsersInGrp "$sUser"
                    }
                }
            }
        }

        # Verify role exist
        if {$mqlret == 0} {
            # Get users from Role
            set sCmd {mql print role "$sRole" select person dump |}

            #Execute command
            set mqlret [catch {eval $sCmd} outstr]

            # Create list of users
            if {$mqlret == 0} {
                set lUsersInRole [split $outstr |]
            }
        }

        # Get intersection of Role and Group
        if {$mqlret == 0} {
            # Initialize variables
            set sFirstUser ""
            set lOtherUsers {}
            set bFlag FALSE

            foreach sUser $lUsersInGrp {
                if {[lsearch -exact "$lUsersInRole" "$sUser"] != -1} {
                    if {$bFlag == "FALSE"} {
                        # Get first user assigned to Role in Group
                        set sFirstUser "$sUser"
                        set bFlag TRUE
                    } else {
                        # Get all other users assigned to Role in Group
                        lappend lOtherUsers "$sUser"
                    }
                }
            }

            if {"$sFirstUser" == ""} {
                set mqlret 1
                set outstr [mql execute program emxMailUtil -method getMessage \
                            "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.NoRole" 2 \
                                  "Role" "$sRole" \
                                  "Groups" "$lGroups" \
                            "" \
                            ]
            }
        }

        if {$mqlret == 0} {
            # Set variable to pass object if left blank
            if {[string trim $bPassObj] == ""} {
                set bPassObj 1
            }

            switch -exact [string toupper $sSendOpt] {
                "SEND" {
                    # Combine first user and all other users and send same message
                    set lAllUsers [linsert $lOtherUsers 0 "$sFirstUser"]

                    foreach sUser $lAllUsers {
                        if {$bPassObj == 1} {
                            set sObjectId [mql get env OBJECTID]
                            set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                                  "$sUser" \
                                                  "$sSubject" 0 \
                                                  "$sText" 0 \
                                                  "$sObjectId" \
                                                  "" \
                                                  "$sBasePropFile" \
                                                  }
                        } else {
                            set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                                  "$sUser" \
                                                  "$sSubject" 0 \
                                                  "$sText" 0 \
                                                  "" \
                                                  "" \
                                                  "$sBasePropFile" \
                                                  }
                        }
                        set mqlret [catch {eval $sCmd} outstr]
                        if {$mqlret != 0} {
                            break
                        }
                    }
                }
                "REASSIGN" {
                    pushShadowAgent
                    set sCmd {mql modify bus "$sType" "$sName" "$sRev" owner "$sFirstUser"}
                    set mqlret [ catch {eval $sCmd} outstr]
                    popShadowAgent
                }
                "ROUTE" {
                    if {$bPassObj == 1} {
                        set sObjectId [mql get env OBJECTID]
                        set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                                  "$sFirstUser" \
                                                  "$sSubject" 0 \
                                                  "$sText" 0 \
                                                  "$sObjectId" \
                                                  "" \
                                                  "$sBasePropFile" \
                                                  }
                    } else {
                        set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                                  "$sFirstUser" \
                                                  "$sSubject" 0 \
                                                  "$sText" 0 \
                                                  "" \
                                                  "" \
                                                  "$sBasePropFile" \
                                                  }
                    }
                    set mqlret [ catch {eval $sCmd} outstr]

                    if {$mqlret == 0} {
                        # Set subject and message text
                        set sSubject "Object Routed To Other User"
                        set sText "$sType $sName $sRev has been routed to $sFirstUser with the following message:\n$sText"

                        foreach sUser $lOtherUsers {
                            if {$bPassObj == 1} {
                                set sObjectId [mql get env OBJECTID]
                                set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                                      "$sUser" \
                                                      "$sSubject" 0 \
                                                      "$sText" 0 \
                                                      "$sObjectId" \
                                                      "" \
                                                      "$sBasePropFile" \
                                                      }
                            } else {
                                set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                                      "$sUser" \
                                                      "$sSubject" 0 \
                                                      "$sText" 0 \
                                                      "" \
                                                      "" \
                                                      "$sBasePropFile" \
                                                      }
                            }
                            set mqlret [catch {eval $sCmd} outstr]
                            if {$mqlret != 0} {
                                break
                            }
                        }
                    }

                    if {$mqlret == 0} {
                        pushShadowAgent
                        set sCmd {mql modify bus "$sType" "$sName" "$sRev" owner "$sFirstUser"}
                        set mqlret [ catch {eval $sCmd} outstr]
                        popShadowAgent
                    }
                }
                default {
                    set mqlret 1
                    set outstr [mql execute program emxMailUtil -method getMessage \
                                    "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.InvalidOption" 1 \
                                          "Option" "$sSendOpt" \
                                    "" \
                                    ]
                }
            }
        }
    }

    if {$mqlret != 0} {
        mql notice $outstr
        return -code $mqlret
    } else {
        if {$bNotify == 1 || [string trim $bNotify] == ""} {
            # Combine first user and all other users and send same message
            set lAllUsers [linsert $lOtherUsers 0 "$sFirstUser"]
            set sMsg [mql execute program emxMailUtil -method getMessage \
                            "emxFramework.ProgramObject.eServicecommonTrigcAutoRouteByNavigationGrpRoleIntsc.Notification" 2 \
                                  "Operation" "$sSendOpt" \
                                  "Persons" "$lAllUsers" \
                            "" \
                            ]
            mql notice $sMsg
        }
        return $mqlret
    }
}
# end eServicecommonAutoRouteByNavigationGrpRoleIntsc


# End of Module

