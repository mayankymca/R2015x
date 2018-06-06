
#************************************************************************
# @progdoc      eServicecommonSetPassword.tcl
#
# @Brief:       Set the given person's password
#
# @Description: This program sets the given person's password.
#
# @Parameters:  sPersonId - Id of person to set password on.
#               sPassword - New password to set.
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

  # Load in the utLoad procedure and other libraries.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]

  # Get the parameters.
  set sPersonId [ mql get env 1 ]
  set sPassword [ mql get env 2 ]

  # Modify the person's password as the shadow agent.
  utCatch {
    pushShadowAgent
    set sPersonName [ mql print bus $sPersonId select name dump ]
    mql modify person $sPersonName password $sPassword
    popShadowAgent
  }

  exit

}


