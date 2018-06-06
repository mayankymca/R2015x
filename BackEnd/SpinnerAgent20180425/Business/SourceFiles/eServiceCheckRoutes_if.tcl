############################################################################
# $RCSfile: eServiceCheckRoutes_if.tcl.rca $ $Revision: 1.49 $
#
# @libdoc       eServicecommonCheckRoutes_if.tcl
#
# @Library:     Check if there are any routes for current state.
#
# @Brief:       Validate that all routes have been completed
#
# @Description: This file contains code related to setting the Originator
#               attribute.
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
tcl;
eval {

###############################################################################
#
# Procedure:    utLoad
#
# Description:  Procedure to load other tcl utilities procedures.
#
# Parameters:   sProgram                - Tcl file to load
#
# Returns:      sOutput                 - Filtered tcl file
#               glUtLoadProgs           - List of loaded programs
#
###############################################################################

proc utLoad { sProgram } {

    global glUtLoadProgs env

    if { ! [ info exists glUtLoadProgs ] } {
        set glUtLoadProgs {}
    }

    if { [ lsearch $glUtLoadProgs $sProgram ] < 0 } {
        lappend glUtLoadProgs $sProgram
    } else {
        return ""
    }

    if { [ catch {
        set sDir "$env(TCL_LIBRARY)/mxTclDev"
        set pFile [ open "$sDir/$sProgram" r ]
        set sOutput [ read $pFile ]
        close $pFile

    } ] == 0 } { return $sOutput }

    set sOutput [ mql print program '$sProgram' select code dump ]

    return $sOutput
}
# end utload

#******************************************************************************
# @procdoc      eServicecommonCheckRoute_if
#
# @Brief:       Check Program to check that all Routes for current state have
#               been completed before promotion can be performed.
#
# @Description: This program will expand on the object to check for all Routes
#               and verify that all routes assigned to current state have been
#               completed.  If all routes have not been completed then this
#               program will prevent the object from being promotted.
#
#
# @Parameters:  Inputs via RPE:
#                     sType - Object's type
#                     sName - Object's name
#                      sRev - Object's revision number
#
#
# @Returns:     0 for success
#               non-zero for failure
#
# @Usage:       For use as trigger check
#
#
# @procdoc
#************************************************************************
  mql verbose off;

  eval  [utLoad eServiceSchemaVariableMapping.tcl]
  eval  [utLoad eServicecommonTranslation.tcl]
  eval  [utLoad eServicecommonShadowAgent.tcl]

  #
  # Set program names
  #
  set progname      "eServiceCheckRoutes_if.tcl"
  set RegProgName   "eServiceSchemaVariableMapping.tcl"

  #
  # Error handling variables
  #
  set mqlret 0
  set outStr ""

  #
  # Set Matrix environment variables.
  #
  set sType   [mql get env TYPE]
  set sName   [mql get env NAME]
  set sRev    [mql get env REVISION]
  set sObjId  [mql get env OBJECTID]
  set sState  [mql get env CURRENTSTATE]
  set sPolicy [mql get env POLICY]
  set sQuotationId  [mql get env OBJECTID]

  #
  # Set variables from symbolic names
  #
  set sRelObjectRoute     [eServiceGetCurrentSchemaName relationship $RegProgName relationship_ObjectRoute]
  set sTypObjectRoute     [eServiceGetCurrentSchemaName type $RegProgName type_Route]
  set sAttRouteBaseState  [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteBaseState]
  set sAttRouteBasePolicy [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteBasePolicy]
  set sAttRouteAction     [eServiceGetCurrentSchemaName attribute $RegProgName attribute_RouteCompletionAction]
  set sStateComplete      [eServiceGetCurrentSchemaName state $RegProgName policy_Route state_Complete]

  if {$mqlret == 0} {
      #
      # Expand object and get all associated Routes
      #
	  pushShadowAgent
      set sCmd {mql expand bus "$sObjId" \
                           from relationship "$sRelObjectRoute" \
                           type "$sTypObjectRoute" \
                           select bus current \
                           select rel attribute\[$sAttRouteBaseState\] \
                           attribute\[$sAttRouteBasePolicy\] \
                           dump | \
                           }
      set mqlret [ catch {eval  $sCmd} outStr ]
      popShadowAgent
      if {$mqlret == 0} {
          set lRoutes [split $outStr \n]

          #
          # Inialize Variables
          #
          set bPromoteFlag 0
          set sInWorkRtes ""

          #
          # Check if there are any Routes for current state
          #
          foreach sRouteData $lRoutes {
              set lRouteData [split $sRouteData |]
              set sRteType [lindex $lRouteData 3]
              set sRteName [lindex $lRouteData 4]
              set sRteRev [lindex $lRouteData 5]
              set sRteCurrentState [lindex $lRouteData 6]
              set sRteBaseState [lindex $lRouteData 7]
              set sRteBasePolicy [lindex $lRouteData 8]

              #
              # Get values from symbolic names
              #
              if {"$sRteBaseState" != "Ad Hoc"} {
                  set sBaseState $sRteBaseState
                  set sBasePolicy $sRteBasePolicy
                  set sRteBaseState [eServiceGetCurrentSchemaName state "$RegProgName" "$sRteBasePolicy" "$sRteBaseState"]
                  set sRteBasePolicy [eServiceGetCurrentSchemaName policy $RegProgName $sRteBasePolicy]

                  if {"$sRteBasePolicy" == ""} {
                      set mqlret 1
                      set outStr [mql execute program emxMailUtil -method getMessage \
                                      "emxFramework.ProgramObject.eServicecommonCheckRoutes_if.InvalidPolicy" 5 \
                                                    "Policy" "$sBasePolicy" \
                                                    "Type" "$sType" \
                                                    "RType" "$sRteType" \
                                                    "RName" "$sRteName" \
                                                    "RRev" "$sRteRev" \
                                      "" \
                                 ]
                  } elseif {"$sRteBaseState" == ""} {
                      set mqlret 1
                      set outStr [mql execute program emxMailUtil -method getMessage \
                                      "emxFramework.ProgramObject.eServicecommonCheckRoutes_if.InvalidState" 5 \
                                                    "State" "$sBaseState" \
                                                    "Type" "$sType" \
                                                    "RType" "$sRteType" \
                                                    "RName" "$sRteName" \
                                                    "RRev" "$sRteRev" \
                                      "" \
                                 ]
                  }
              }
              
              #
              # Get setting from emxSystem.properties file to 
              # check if Ad Hoc routes should be considered or not
              #
              set bConsiderAdhocRoutes [mql execute program emxMailUtil -method getMessage \
                                            "emxFramework.AdHocRoutesBlockLifecycle" 0 \
                                            "" \
                                            "emxSystem" \
                                       ]
              #
              # set default to false if property doesn't exists
              #
              set bConsiderAdhocRoutes [string toupper "$bConsiderAdhocRoutes"]
              if {"$bConsiderAdhocRoutes" != "TRUE"} {
                  set bConsiderAdhocRoutes "FALSE"
              }
              
              #
              # Check if routes for state and Ad Hoc routes are complete
              #
              if {("$bConsiderAdhocRoutes" == "TRUE" && "$sRteBaseState" == "Ad Hoc") || ("$sState" == "$sRteBaseState" && "$sRteBasePolicy" == "$sPolicy")} {
                  # Set flag if Route still in work
                  if {"$sRteCurrentState" != "$sStateComplete"} {
                      set bPromoteFlag 1
                      append sInWorkRtes "'$sRteType' '$sRteName' '$sRteRev'\n"
                  }
              }
          }

	  if {$bPromoteFlag == 1} { 
                set sTypeRTSQuotation     [eServiceGetCurrentSchemaName type $RegProgName type_RTSQuotation]
                if {"$sType" == "$sTypeRTSQuotation"} { 

                set sRelRTSQuotation       [eServiceGetCurrentSchemaName relationship $RegProgName relationship_RTSQuotation]
                set sRelCompanyRFQ       [eServiceGetCurrentSchemaName relationship $RegProgName relationship_CompanyRFQ]
                set sRelRFQHolder       [eServiceGetCurrentSchemaName relationship $RegProgName relationship_RFQHolder]
                set sRelSupplierResponse       [eServiceGetCurrentSchemaName relationship $RegProgName relationship_SupplierResponse]

                set sCmd {mql print bus "$sQuotationId" \
                                                                  select \
                       to\[$sRelRTSQuotation\].from.name \
                                                                dump | \
                }

                set mqlret [catch {eval $sCmd} rfqNameStr]

                set sCmd {mql print bus "$sQuotationId" \
                                                                  select \
                   to\[$sRelRTSQuotation\].from.revision \
                                                                dump | \
                }

                set mqlret [catch {eval $sCmd} rfqRevStr]

                set sCmd {mql print bus "$sQuotationId" \
                                                                  select \
                to\[$sRelRTSQuotation\].from.to\[$sRelCompanyRFQ\].from.to\[$sRelRFQHolder\].from.name \
                                                                 dump | \
                }
                set mqlret [catch {eval $sCmd} rfqCoStr]

                set sCmd {mql print bus "$sQuotationId" \
                                                                  select \
                from\[$sRelSupplierResponse\].to.name \
                                                               dump | \
                }

                set mqlret [catch {eval $sCmd} supplierCoStr]

                set name "$rfqNameStr $rfqRevStr ( $rfqCoStr )"

                set outStr [mql execute program emxMailUtil -method getMessage \
                "emxFramework.ProgramObject.eServicecommonCheckRoutes_if.RouteNotCompleteForRFQQuotation" 3 \
                "QuotationName" "$name" \
                "SupplierCoName" "$supplierCoStr" \
                "WorkRoute" "$sInWorkRtes" \
                "" \
                ]
                set mqlret 1
                } else {
                set outStr [mql execute program emxMailUtil -method getMessage \
                 "emxFramework.ProgramObject.eServicecommonCheckRoutes_if.RouteNotComplete" 4 \
                 "Type" "$sType" \
                 "Name" "$sName" \
                 "Rev" "$sRev" \
                 "WorkRoute" "$sInWorkRtes" \
                 "" \
                 ]
	set mqlret 1
	}
          }
      }
  }

  if {$mqlret != 0} {
      mql notice "$outStr"
  }

  exit $mqlret

}
# end eServiceCheckRoute_if

