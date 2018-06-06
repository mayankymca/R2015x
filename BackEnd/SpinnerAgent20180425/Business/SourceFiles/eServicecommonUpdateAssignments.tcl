#************************************************************************
# @progdoc      eServicecommonUpdateAssignments.tcl
#
# @Brief:       Update the roles and groups of a person
#
# @Description: This program updates the roles and groups of a person.
#               For backwards compatibility, the program eServicecommonUpdateRoles.tcl
#               was taken and modified to include Group assignment updates. The
#               program was renamed to eServicecommonUpdateAssignments.tcl.
#
# @Parameters:  sPerson - Name of the person to update.
#               lRoles  - List of roles that the person should be assigned to.
#               lUpdateGroups - List of Groups that the person should be assigned to
#
# @Returns:     1|ERROR MSG             - if Errors Occurs
#               0|Update Successful     - if No Errors
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#************************************************************************

tcl;

eval {

  mql verbose off;
  mql quote off;

  # Load the utilities and other libraries.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]
  eval [ utLoad eServicecommonDEBUG.tcl ]
  eval [ utLoad eServicecommonTranslation.tcl]

  # Get the parameters.
  set sPerson [ mql get env 1 ]
  set lRoles  [ mql get env 2 ]
  set lUpdateGroups [ mql get env 3 ]

  # Get a list of all the groups.  This is used to determine if an
  # assignement is a group or a role.
  utCatch { set lGroups [ split [ mql list group ] \n ] }

  # Get a list of all the roles.  This is used to determine if an
  # assignement is a group or a role.
  utCatch { set lAllRoles [ split [ mql list role ] \n ] }

  # Get the current assignments of the person.
  set sCmd {mql print person "$sPerson" select assignment dump |}
  set mqlret [catch {eval $sCmd} outstr]

  if { $mqlret == 0 } {

      set sAssignments $outstr

      # Remove the groups and passed in roles from the
      # assignments to get the list of roles to delete.
      set lDelRoles {}
      set lDelGroups {}

      foreach sAssignment [ split $sAssignments | ] {
        # If the assignment is not a group then look to see if it is in the passed-in roles.

        if { [ lsearch $lAllRoles $sAssignment ] != -1 } {
            set iPos [ lsearch $lRoles $sAssignment ]
            # If the role is in the list of passed-in roles, then remove it
            # from the list of passed-in roles because it doesn't need to be added.
            # Otherwise, add it to the list of roles to be deleted.
            if { $iPos != -1 } {
                set lRoles [ lreplace $lRoles $iPos $iPos ]
            } else {
                lappend lDelRoles $sAssignment
            }
        }

        if { [ lsearch $lGroups $sAssignment ] != -1  } {
            #The sAssignment is a "Group"
            set iPos [ lsearch $lUpdateGroups $sAssignment ]
            if { $iPos != -1 } {
                set lUpdateGroups [ lreplace $lUpdateGroups $iPos $iPos ]
            } else {
                lappend lDelGroups $sAssignment
            }
        }
      }

      # Create a command to update the roles.
      set lCmd [ list mql modify person $sPerson ]

      # Add the roles to add to the command.
      foreach sRole $lRoles {
        lappend lCmd assign role $sRole
      }

      #Add the groups to add to the command.
      foreach ele $lUpdateGroups {
        lappend lCmd assign group $ele
      }

      # Add the roles to remove to the command.
      foreach sRole $lDelRoles {
        lappend lCmd remove assign role $sRole
      }

      #Add the groups to remove to the command.
      foreach ele $lDelGroups {
        lappend lCmd remove assign group $ele
      }

      #Check to see if there is anything to do. Saves Unnecessary MQL calls.
      if { [ expr  [llength $lRoles ] + \
            [llength $lUpdateGroups ] + \
            [llength $lDelRoles ] + \
            [llength $lDelGroups ] ] != 0 } {

            # Update the person's roles as the shadow agent.
            pushShadowAgent
            set mqlret [catch {eval $lCmd} outstr]
            if { $mqlret != 0 } {
                set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonUpdateAssignments.UpdateFailed" 1 \
                                  "Name" "$sPerson" \
                                "" \
                                ]
            }
            popShadowAgent

      }

  } else {
      set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonUpdateAssignments.InvalidPerson" 1 \
                                  "Name" "$sPerson" \
                                "" \
                                ]
  }

  if {$mqlret != 0} {
       return "1|Error: $outstr"
  } else {
       return "0|Update Successful"
  }

}

