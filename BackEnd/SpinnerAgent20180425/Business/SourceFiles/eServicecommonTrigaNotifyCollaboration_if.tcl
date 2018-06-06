#************************************************************************
# @progdoc      eServicecommonTrigaNotifyCollaboration_if.tcl
#
# @Brief:       Notify Company Reps of Collaboration Events.
#
# @Description: This program will notify the Company Reps involved in a
#               Collaboration when the collaboration is requested,
#               withdrawn, accepted, rejected, or deleted.
#
# @Parameters:  None.
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

  # Define proc to get a list of company reps for the specified company.
  proc getCompanyReps { sCompanyRepRelType sCompanyOid } {

    # Expand to find company reps.
    utCatch {
      set sExpand [ mql expand bus $sCompanyOid \
              from rel $sCompanyRepRelType dump | ]
    }

    # Loop through the expand data and collect company rep names.
    set lCompanyReps {}
    foreach { x x x sType sCompanyRep x } [ split $sExpand \n| ] {
      lappend lCompanyReps $sCompanyRep
    }

    # Return the list of company rep names.
    return $lCompanyReps

  }

# Define proc to get a list of company reps for the specified company.
  proc getParentCompanyReps { sCompanyRepRelType sCompanyOid } {

    # Expand to find company reps.
    utCatch {
      set sExpand [ mql expand bus $sCompanyOid \
              to rel $sCompanyRepRelType select relationship from.id dump | ]
    }

    # Loop through the expand data and collect company rep names.
    set lCompanyReps {}
    foreach { x x x x x x sCompanyRep} [ split $sExpand \n| ] {
      lappend lCompanyReps $sCompanyRep
    }

    # Return the list of company rep names.
    return $lCompanyReps

  }


  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]
  eval [ utLoad eServicecommonTranslation.tcl ]

  # Define schema mapping for various relationships.
  set sCompanyRepRelType [ eServiceGetCurrentSchemaName \
          "relationship" $sRegProgName "relationship_CompanyRepresentative" ]
  set sCollaborationRequestRelType [ eServiceGetCurrentSchemaName \
          "relationship" $sRegProgName "relationship_CollaborationRequest" ]
  set sParentCompanyRepRelType [ eServiceGetCurrentSchemaName \
          "relationship" $sRegProgName "relationship_Division" ]
  set sChildType [ eServiceGetCurrentSchemaName \
          "type" $sRegProgName "type_BusinessUnit" ]
  set sParentType [ eServiceGetCurrentSchemaName \
          "type" $sRegProgName "type_Company" ]

  # Get data from the RPE.
  set sUser [ mql get env USER ]
  set sEvent [ string tolower [ mql get env EVENT ] ]
  set sRelType [ mql get env RELTYPE ]
  set sFromId [ mql get env FROMOBJECTID ]
  set sFromCompany [ mql get env FROMNAME ]
  set sToId [ mql get env TOOBJECTID ]
  set sToCompany [ mql get env TONAME ]
  set sFromType [ mql get env FROMTYPE ]
  set sToType [ mql get env TOTYPE ]


  # Get company rep on from end of rel.
  set lFromReps [ getCompanyReps $sCompanyRepRelType $sFromId ]
  set sFromReps [ join $lFromReps ","]

  # Get company rep on to end of rel.
  set lToReps [ getCompanyReps $sCompanyRepRelType $sToId ]
  set sToReps [ join $lToReps ","]

  if { $sFromType == $sChildType && $sToType != $sParentType } {

  # Get company rep on from end of rel.
  set lPFromReps [ getParentCompanyReps $sParentCompanyRepRelType $sFromId ]
  set sPFromReps [ join $lPFromReps ","]

  # Get company rep on to end of rel.
  set lPToReps [ getParentCompanyReps $sParentCompanyRepRelType $sToId ]
  set sPToReps [ join $lPToReps ","]

  # Get company rep on from end of rel.
  set FromReps [ getCompanyReps $sCompanyRepRelType $sPFromReps]
  set FromReps [ join $FromReps ","]
  append sFromReps ",$FromReps"

  # Get company rep on to end of rel.
  set ToReps [ getCompanyReps $sCompanyRepRelType $sPToReps]
  set ToReps [ join $ToReps ","]
  append sToReps ",$ToReps"


  }

  # Define iconmail from/to subject/text based on create/delete and rel type.
  if { $sRelType == $sCollaborationRequestRelType } {

    if { $sEvent == "create" } {


      # Collaboration is being "requested".
      set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sFromReps" \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectSent" 0 \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageSent" 1 \
                                          "company" "$sToCompany" \
                            "" \
                            "" \
                            }
      set sToCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sToReps" \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectReceived" 0 \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageReceived" 1 \
                                          "company" "$sFromCompany" \
                            "" \
                            "" \
                            }
    } else {

      # Determine who is removing the collaboration request.
      if { [ lsearch $lFromReps $sUser ] != -1 } {

        # Collaboration is being withdrawn by requestor.
        set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sFromReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectWithdrawn" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageWithdrawn" 1 \
                                            "company" "$sToCompany" \
                              "" \
                              "" \
                              }
        set sToCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sToReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectPersonWithdrawn" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessagePersonWithdrawn" 1 \
                                            "company" "$sFromCompany" \
                              "" \
                              "" \
                              }
      } elseif { [ lsearch $lToReps $sUser ] != -1 } {

        # Collaboration is being rejected by requestee.
        set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sFromReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectRejected" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageRejected" 1 \
                                            "company" "$sToCompany" \
                              "" \
                              "" \
                              }
        set sToCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sToReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectPersonRejected" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessagePersonRejected" 1 \
                                            "company" "$sFromCompany" \
                              "" \
                              "" \
                              }
      } else {

        # Collaboration is being removed by someone else (host rep).
        set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sFromReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectRemoved" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageRemoved" 1 \
                                            "company" "$sToCompany" \
                              "" \
                              "" \
                              }
        set sToCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sToReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectPersonRemoved" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessagePersonRemoved" 1 \
                                            "company" "$sFromCompany" \
                              "" \
                              "" \
                              }
      }

    }

  } else {

    if { $sEvent == "create" } {
      # Collaboration is being "accepted".
      set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sFromReps" \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectAccepted" 0 \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageAccepted" 1 \
                                          "company" "$sToCompany" \
                            "" \
                            "" \
                            }
      set sToCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                            "$sToReps" \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectPersonAccepted" 0 \
                            "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessagePersonAccepted" 1 \
                                          "company" "$sFromCompany" \
                            "" \
                            "" \
                            }
    } else {

      # Collaboration is being "removed".

      # Determine who is removing the collaboration partner.
      if { [ lsearch $lFromReps $sUser ] != -1 } {

        # Collaboration is being removed by initiator.
        set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sFromReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectCollPartnerRemoved1" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageCollPartnerRemoved1" 1 \
                                            "company" "$sToCompany" \
                              "" \
                              "" \
                              }
        set sToCmd ""
      } else {

        # Collaboration is being removed by someone else (host rep).
        set sFromCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                              "$sFromReps" \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.SubjectCollPartnerRemoved1" 0 \
                              "emxFramework.ProgramObject.eServicecommonTrigaNotifyCollaboration_if.MessageCollPartnerRemoved5" 1 \
                                            "company" "$sToCompany" \
                              "" \
                              "" \
                              }
        set sToCmd ""
      }

    }

  }

  # Send iconmail to the company reps that initiated the collaboration.
  if { [ llength $lFromReps ] > 0 } {
    utCatch {
      eval $sFromCmd
    }
  }

  # Send iconmail to the company reps that initiated the collaboration.
  if { [ llength $lToReps ] > 0 } {
    utCatch {
      eval $sToCmd
    }
  }

  exit

}

