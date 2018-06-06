
#************************************************************************
# @progdoc      eServicecommonTrigaNotifyNewEmployee_if.tcl
#
# @Brief:       Notify the Company Representative.
#
# @Description: This program sends iconmail to the Company Representatives
#               when an Employee is connected to a Company.
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

  mql verbose off
  mql quote off

  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]
  eval [ utLoad eServicecommonTranslation.tcl ]

  # Define schema mapping for the Company Rep relationship.
  set sCompanyRepRelType [ eServiceGetCurrentSchemaName \
          "relationship" $sRegProgName "relationship_CompanyRepresentative" ]
  set sBusinessUnitEmployeeRelType [ eServiceGetCurrentSchemaName \
          "relationship" $sRegProgName "relationship_BusinessUnitEmployee" ]

  # Get the company id,the person id,person name and person loged in.
  set sCompanyId [ mql get env FROMOBJECTID ]
  set sPersonId [ mql get env TOOBJECTID ]
  set sPersonName [ mql get env TONAME ]
  set sLogin [mql get env USER]

  # Initialize the expand to nothing.
  set sExpand ""

  # Look for a business unit connected to the person.
  utCatch {
    set sBusinessUnitId [ mql print bus $sPersonId \
            select to\[$sBusinessUnitEmployeeRelType\].from.id dump | ]
  }

  # If the person is connected to a business unit,
  # then look for company reps there.
  if { $sBusinessUnitId != "" } {
    utCatch {
      set sExpand [ mql expand bus $sBusinessUnitId \
              from rel $sCompanyRepRelType select bus where 'current == Active' dump | ]
    }
  }

  # If no business unit was connected, or no company reps were connected
  # to the business unit, then look fo company reps connected to the company.
  if { $sExpand == "" } {
    utCatch {
      set sExpand [ mql expand bus $sCompanyId \
              from rel $sCompanyRepRelType select bus where 'current == Active' dump | ]
    }
  }

  # Create a comma-delimited "to" list for the iconmail.
  set lTo {}
  foreach { x x x x sCompanyRepName x x } [ split $sExpand \n| ] {
    lappend lTo "$sCompanyRepName"
  }
  set sTo [ join $lTo "," ]

  set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                          "$sTo" \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyNewEmployee_if.SubjectNotify" 0 \
                          "emxFramework.ProgramObject.eServicecommonTrigaNotifyNewEmployee_if.MessageNotify" 1 \
                                        "name" "$sPersonName" \
                          "" \
                          "" \
                          }

  # Send the iconmail.
  if { [ llength $lTo ] > 0 } {
    utCatch {
      eval $sCmd
    }
  }

  exit

}


