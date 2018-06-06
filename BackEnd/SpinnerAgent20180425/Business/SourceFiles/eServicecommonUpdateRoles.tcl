
#************************************************************************
# @progdoc      eServicecommonUpdateRoles.tcl
#
# @Brief:       Update the roles of a person
#
# @Description: This program updates the roles of a person.
#
# @Parameters:  sPerson - Name of the person to update.
#               lRoles  - List of roles that the person should be assigned to.
#
# @Returns:     Nothing.
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

  # Get the parameters.
  set sPerson [ mql get env 1 ]
  set lRoles  [ mql get env 2 ]

  # Get a list of all the groups.  This is used to determine if an
  # assignement is a group or a role.
  utCatch { set lGroups [ split [ mql list group ] \n ] }

  # Get the current assignments of the person.
  utCatch {
    set sAssignments [ mql print person $sPerson select assignment dump | ]
  }

  # Remove the groups and passed in roles from the
  # assignments to get the list of roles to delete.
  set lDelRoles {}
  foreach sAssignment [ split $sAssignments | ] {

    # If the assignment is not a group then
    # look to see if it is in the passed-in roles.
    if { [ lsearch $lGroups $sAssignment ] == -1 } {

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

  }

  # Create a command to update the roles.
  set lCmd [ list mql modify person $sPerson ]

  # Add the roles to add to the command.
  foreach sRole $lRoles {
    lappend lCmd assign role $sRole
  }

  # Add the roles to remove to the command.
  foreach sRole $lDelRoles {
    lappend lCmd remove assign role $sRole
  }

  # Update the person's roles as the shadow agent.
  utCatch {
    pushShadowAgent
    eval $lCmd
    popShadowAgent
  }

  exit

}


