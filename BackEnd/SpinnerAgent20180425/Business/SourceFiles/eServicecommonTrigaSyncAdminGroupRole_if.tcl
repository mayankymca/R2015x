
#************************************************************************
# @progdoc        eServiceTeamTrigaSyncAdminGroupRole.tcl
#
# @Brief:         Update group or role when connection is made or broken
#
# @Description:   This program will update the specified group or role when
#                 the specific connection is made or broken with an object
#                 representing a person.
#
# @Parameters:    sGroupRoleType  - The group or role type.  This can be
#                                   either "group" or "role".
#                 sGroupRoleAlias - The alias for the group or role name.
#                                   This is the group or role that the person
#                                   will be added to or removed from when
#                                   the connection is made or broken.
#                 sPersonSide     - The side of the relationship that the
#                                   object representing the person is on.
#                 sPersonSelect   - The selectable that will be used to
#                                   get the name of the admin person object
#                                   from the object representing the person.
#
# @Returns:       Nothing
#
# @Usage:         This program must be implemented as a connect and
#                 disconnect action trigger on a relationship type.
#
# @progdoc        Copyright (c) 2001, MatrixOne, Inc.
#************************************************************************

tcl;

# Start eval to prevent echo to stdout.
eval {

# Load necessary Mql/Tcl libraries.
eval [ mql print program eServicecommonUtil.tcl select code dump ]
eval [ utLoad eServicecommonShadowAgent.tcl ]
eval [ utLoad eServicecommonDEBUG.tcl ]

# Load the schema mapping program.
set sRegProgName "eServiceSchemaVariableMapping.tcl"
eval [ utLoad $sRegProgName ]

# Get the input parameters.
set sGroupRoleType [ string tolower [ mql get env 1 ] ]
set sGroupRoleAlias [ mql get env 2 ]
set sPersonSide [ string toupper [ mql get env 3 ] ]
set sPersonSelect [ string tolower [ mql get env 4 ] ]

# Get the actual group or role name from the alias.
set sGroupRoleName [ eServiceGetCurrentSchemaName \
        $sGroupRoleType $sRegProgName $sGroupRoleAlias ]

# Get environment variables.
set sPersonId [ mql get env ${sPersonSide}OBJECTID ]
set sEvent [ string tolower [ mql get env EVENT ] ]

# Determine if the person should be added or removed from the group/role.
if { $sEvent == "create" } {
  set sAction "add"
} else {
  set sAction "remove"
}

# Get the name of the admin person object and
# update the group/role assignement as the shadow agent.
utCatch {
  pushShadowAgent
  set sPersonName [ mql print bus $sPersonId select $sPersonSelect dump ]
  mql mod $sGroupRoleType $sGroupRoleName $sAction assign person $sPersonName
  popShadowAgent
}

# Exit with success.
exit 0

# End eval to prevent echo to stdout.
}



# End of Module


