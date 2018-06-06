###############################################################################
#
# $RCSfile: eServiceGetUserAbsSuites.tcl.rca $ $Revision: 1.5 $
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

  mql verbose off

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
  set lSiutes "$outStr"

  # Get all registered suite values
  set sCmd {eServiceGetProperty program "$sRegistryProgram" "$sSuitePropertyName" value}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return "1|$outStr"
  }
  set lValues "$outStr"

  foreach sSuite $lSiutes {

      # Get all registered apps
      set sCmd {eServiceGetProperty program "$sSuite" "$sAppPropertyName" to}
      set mqlret [ catch {eval $sCmd} outStr ]
      if {$mqlret != 0} {
          return "1|$outStr"
      }
      set lApps "$outStr"
      set bAcccessFound 0
      foreach sApp $lApps {

          # Get all registered features
          set sCmd {eServiceGetProperty program "$sApp" "$sFeaturePropertyName" to}
          set mqlret [ catch {eval $sCmd} outStr ]
          if {$mqlret != 0} {
              return "1|$outStr"
          }
          set lFeatures "$outStr"
          foreach sFeature $lFeatures {

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
                  break
              }
          }
          if {$bAcccessFound == 1} {
              break
          }
      }
      if {$bAcccessFound == 1} {
          set sValue [lindex $lValues [lsearch $lSiutes $sSuite]]
          append sReturnString "|$sSuite|$sValue"
      }
  }

  return "$sReturnString"
}

