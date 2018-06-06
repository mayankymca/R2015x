#************************************************************************
# @progdoc      emxEngineeringCentralChangeVault.tcl
#
# @Brief:       Sets the given person's Default Vault
#
# @Description: This program sets the given person's Default Vault.
#
# @Parameters:  sPersonId - Id of person to set Default Vault on.
#               sDefaultVault - New Default Vault to set.
#
# @Returns:     Nothing.
#
#************************************************************************
###############################################################################
#                                                                             #
#   Copyright (c) 2003-2015 Dassault Systemes.  All Rights Reserved.          #
#   This program contains proprietary and trade secret information of         #
#   Dassault Systemes.  Copyright notice is precautionary only and does not   #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

tcl;

eval {

  mql verbose off;
  mql quote off;

  # Load in the utLoad procedure and other libraries.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]

  # Get the parameters.
  set sPersonId [ mql get env 1 ]
  set sDefaultVault [ mql get env 2 ]

  # Modify the person's Default Vault as the shadow agent.
  utCatch {
    pushShadowAgent
    set sPersonName [ mql print bus $sPersonId select name dump ]
    mql modify person $sPersonName vault $sDefaultVault
    popShadowAgent
  }

  exit

}

