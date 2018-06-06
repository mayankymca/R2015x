
#************************************************************************
# @progdoc      eServicecommonTrigaSyncAdminObjectName_if.tcl
#
# @Brief:       Update an admin object based on the trigger
#
# @Description: This program will sync the name of the given admin
#               object type with the name of this object.
#
# @Parameters:  1 - Administrative object type to modify.
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

  # Get parameter values.
  set sAdminType [ mql get env 1 ]

  # Get RPE values.
  set sOldName [ mql get env NAME ]
  set sNewName [ mql get env NEWNAME ]

  # Get a list of all the current admin objects of the specified type.
  utCatch {
    set lAdminObjects [ split [ mql list $sAdminType ] \n ]
  }

  # If the old admin object is in the list, then change it's name.
  if { [ lsearch $lAdminObjects $sOldName ] != -1 } {

    # Update the admin object's name as the shadow agent.
    utCatch {
      pushShadowAgent
      mql mod $sAdminType $sOldName name $sNewName
      popShadowAgent
    }

  }

  exit

}


