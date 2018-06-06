###############################################################################
#
# $RCSfile: eServiceGetUserAbsFeatures.tcl.rca $ $Revision: 1.6 $
#
# Description:
# DEPENDENCIES:
#
###############################################################################
###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################
tcl;
eval {

  # Load the utilities and other libraries.
  eval  [ mql print prog eServicecommonUtil.tcl select code dump ]

  set sSuitePropertyName   "eServiceSuite"
  set sAppPropertyName     "eServiceApplication"
  set sFeaturePropertyName "eServiceFeature"
  set sAccessPropertyName  "eServiceFeatureAccess"
  set sPagePropertyName    "eServicePage"
  set sRegistryProgram     "eServiceRegistry"
  set sReturnString        "0"
  set sPageType            "HTML"

  mql verbose off

  # Get user input from RPE variables.
  set sInputSuiteName [string trim [mql get env 1]]
  set sInputAppName   [string trim [mql get env 2]]

  # Get assigned roles.
  set sCmd {eServiceGetAssignments role}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return "1|$outStr"
  }
  set lAssignedRoles "$outStr"

  # Get all registered suites
  set sCmd {eServiceGetProperty program "$sRegistryProgram" "$sSuitePropertyName" to}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return "1|$outStr"
  }
  if {[lsearch $outStr "$sInputSuiteName"] == "-1"} {
      return "1|Error: Suite '$sInputSuiteName' does not exists"
  }

  # Get all registered apps
  set sCmd {eServiceGetProperty program "$sInputSuiteName" "$sAppPropertyName" to}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return "1|$outStr"
  }
  if {[lsearch $outStr "$sInputAppName"] == "-1"} {
      return "1|Error: Application '$sInputAppName' does not exists"
  }

  # Get all registered features
  set sCmd {eServiceGetProperty program "$sInputAppName" "$sFeaturePropertyName" to}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return "1|$outStr"
  }
  set lFeatures "$outStr"

  # Get all registered feature values
  set sCmd {eServiceGetProperty program "$sInputAppName" "$sFeaturePropertyName" value}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return "1|$outStr"
  }
  set lValues "$outStr"

  foreach sFeature $lFeatures {

      set bAcccessFound 0
      foreach sAssignedRole $lAssignedRoles {

          # Get all access features
          set sCmd {eServiceGetProperty role "$sAssignedRole"}
          set mqlret [ catch {eval $sCmd} outStr ]
          if {$mqlret != 0} {
              return "1|$outStr"
          }
          foreach sProp $outStr {
              set lProp [split $sProp]
              if {[lindex $lProp 0] == "$sAccessPropertyName"} {
                  set nIndex [lsearch $lProp to]
                  set sSupportedFeature [join [lrange $lProp [expr $nIndex + 2] end]]
                  if {"$sSupportedFeature" == "$sFeature"} {
                      set bAcccessFound 1
                      break
                  }
              }
          }
          if {$bAcccessFound == 1} {
              break
          }
      }
      if {$bAcccessFound == 1} {
          set sValue [lindex $lValues [lsearch $lFeatures $sFeature]]

          # Get registered page
          set sCmd {eServiceGetProperty program "$sFeature" "$sPagePropertyName${sPageType}" to}
          set mqlret [ catch {eval $sCmd} outStr ]
          if {$mqlret != 0} {
              return "1|$outStr"
          }

          set sPage [lindex $outStr 0]
          append sReturnString "|$sFeature|$sValue|$sPage"
      }
  }

  return "$sReturnString"
}

