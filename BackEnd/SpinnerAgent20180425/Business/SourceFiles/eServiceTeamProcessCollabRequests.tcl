
#************************************************************************
# @progdoc      eServiceTeamProcessCollabRequests.tcl
#
# @Brief:       Process the accepting and rejecting of Collaboration Partners.
#
# @Description: This program is given a set of Collaboration Requests to
#               accept and a set to reject.  The accepted set has their
#               relationship changed from a Request to a Partner.  The
#               rejected set has their relationship removed.
#
# @Parameters:  sAccepted - A comma-delimited list of accepted requests.
#               sRejected - A comma-delimited list of rejected requests.
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
  eval [ utLoad eServicecommonDEBUG.tcl ]

  # Load the schema mapping program.
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]

  # Define schema mapping for the partner relationship.
  set sCollabPartnerRel [ eServiceGetCurrentSchemaName \
          "relationship" $sRegProgName "relationship_CollaborationPartner" ]

  # Get the parameters.
  set sAccepted [ mql get env 1 ]
  set sRejected [ mql get env 2 ]

  # Loop through the accepted requests and convert the rels to partners.
  foreach sRelId [ split $sAccepted , ] {

    # Get the from and to objects connected to this relationship.
    set sData [ mql print connection $sRelId select from.id to.id dump | ]
    foreach {sFromId sToId} [ split $sData | ] {}

    # Turn triggers off and remove the request relationship.
    utCatch {
      pushShadowAgent
      mql trigger off
      mql disconnect connection $sRelId
      mql trigger on
      popShadowAgent
    }

    # With triggers back on and as myself, create the partner relationship.
    utCatch {
      mql connect bus $sFromId relationship $sCollabPartnerRel to $sToId
      mql connect bus $sToId relationship $sCollabPartnerRel to $sFromId
    }
  }

  # Loop through the rejected requests and disconnect the rels.
  foreach sRelId [ split $sRejected , ] {
    utCatch {
      mql disconnect connection $sRelId
    }
  }

  exit

}


