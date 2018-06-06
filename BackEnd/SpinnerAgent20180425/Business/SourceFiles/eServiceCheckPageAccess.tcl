###############################################################################
#
# $RCSfile: eServiceCheckPageAccess.tcl.rca $ $Revision: 1.8 $
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

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utload

###############################################################################

  set sProgName     "eServiceCheckPageAccess.tcl"

  mql verbose off;

  set sInputUser               [string trim [mql get env USER]]

  set sInputPageName           [string trim [mql get env 1]]
  set sRole                    [split [mql list "role"] "\n"]
  set sRoleList                ""
  set DUMPDELIMITER            "|"
  set mqlret                   0
  set outstr                   ""
  set iPageFound               0
  set lFileList                ""

         if {$sRole == ""} {
            return "1|Error: $sProgName - No roles defined in the system: Unable to proceed"
         }

         if {$sInputPageName == ""} {
            return "1|Error: $sProgName - Input Page name not specified"
         }

         if {($mqlret == 0) && ($sInputUser != "")} {
            set sCmd {mql print person "$sInputUser" \
                                select assignment    \
                                dump $DUMPDELIMITER  \
                                }

            set mqlret [ catch {eval $sCmd} outstr ]
         }

         if {$mqlret == 0} {
               set lData [split $outstr $DUMPDELIMITER]
               foreach lItem $lData {
                       set lItem [string trim $lItem]
                       foreach lSubItem $sRole {
                               set lSubItem [string trim $lSubItem]
                               if {[string compare "$lItem" "$lSubItem"] == 0} {
                                  lappend sRoleList "$lItem"
                               }
                       }
               }
          }

          if {$mqlret == 0} {
                  if {$sRoleList == ""} {
                       return 1
                  }
                  foreach lRoleItem $sRoleList {
                          set lRolePropList [split [mql list property on role "$lRoleItem"] "\n"]
                          if {$lRolePropList == ""} {
                               continue
                          }
                          foreach lRoleElement $lRolePropList {
                                  set lRoleElement [split [string trim $lRoleElement]]
                                  set iPropIndex   [lsearch -exact $lRoleElement "eServiceFeatureAccess"]
                                  if {$iPropIndex == 0} {
                                     set sProgIndexRole [lsearch -exact $lRoleElement "program"]
                                     if {$sProgIndexRole > 0} {
                                          set sFeaturePlaceHoldName [string trim [lrange $lRoleElement [expr $sProgIndexRole + 1] end]]
                                     }
                                     set lListFeaturePlaceHold [split [mql list property on program "$sFeaturePlaceHoldName"] "\n"]
                                     foreach lListElement $lListFeaturePlaceHold {
                                             set sFeatureFileName [string trim [lindex $lListElement [expr [llength $lListElement] - 1]]]
                                             append lFileList "$sFeatureFileName$DUMPDELIMITER"
                                     }
                                  }
                          }
                  }
                  if {$lFileList != ""} {
                      set lFileList [split $lFileList $DUMPDELIMITER]
                      set iIndex    [lsearch -exact $lFileList "$sInputPageName"]
                      if {$iIndex >= 0} {
                         return 0
                      } else {
                         return 1
                      }
                  } else {
                     return 1
                  }
          }

          if {$mqlret != 0} {
             return "1|Error: $sProgName - $outstr"
          }
}
##################################################################################

