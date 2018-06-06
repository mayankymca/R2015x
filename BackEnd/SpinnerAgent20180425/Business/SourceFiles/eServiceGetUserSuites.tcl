###############################################################################
#
# $RCSfile: eServiceGetUserSuites.tcl.rca $ $Revision: 1.19 $
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

  set RegProgName   "eServiceRegistry"
  set sProgName     "eServiceGetUserSuites.tcl"

  mql verbose off;

  set sInputUser               [string trim [mql get env USER]]
  set sInputLangType           [string toupper [string trim [mql get env 1]]]

  set sRole                    [split [mql list "role"] "\n"]
  set sRoleList                ""
  set DUMPDELIMITER            "|"
  set mqlret                   0
  set outstr                   ""
  set sSuiteName               ""
  set aSuiteList               {}
  set aFinalList               {}
  set sProgPageName            ""
  set aCheckList               ""
  set lCheckList               ""

         if {$sRole == ""} {
            return "1|Error: $sProgName - No roles defined in the system: Unable to proceed"
         }

         if {[string compare $sInputLangType ""] == 0} {
             set sInputLangType "HTML"
         }

         set sSearchLangProp "eServicePage${sInputLangType}"

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

             set lList [split [mql list property on program "$RegProgName"] "\n"]
             foreach lElement $lList {

                 set lElement [split $lElement]

                 set iIndx [lsearch -exact $lElement "eServiceSuite"]

                 if {$iIndx == 0} {

                     set sProgIndexSte [lsearch -exact $lElement "to"]
                     set sValIndexSte  [lsearch -exact $lElement "value"]

                     if {($sValIndexSte > 0) && ($sProgIndexSte > 0)} {
                        set sProgramName [string trim [lrange $lElement [expr $sProgIndexSte + 2] [expr $sValIndexSte - 1]]]
                        set sSuiteName   [string trim [lrange $lElement [expr $sValIndexSte + 1] end]]
                     }

                     if {$sSuiteName != ""} {
                          append aCheckList "$DUMPDELIMITER$sSuiteName$DUMPDELIMITER"
                          set lListApp [split [mql list property on program "$sProgramName"] "\n"]
                          foreach lAppElement $lListApp {

                                  set lAppElement  [split $lAppElement]
                                  set iAppPropIndx [lsearch -exact $lAppElement "eServiceApplication"]

                                  if {$iAppPropIndx == 0} {

                                             set sProgIndexAppl [lsearch -exact $lAppElement "to"]
                                             set sValIndexAppl  [lsearch -exact $lAppElement "value"]
                                             if {($sValIndexAppl > 0) && ($sProgIndexAppl > 0)} {
                                                set sAppProgramName [string trim [lrange $lAppElement [expr $sProgIndexAppl + 2] [expr $sValIndexAppl - 1]]]
                                             }
                                             set lListprog [split [mql list property on program "$sAppProgramName"] "\n"]

                                             foreach lProgElement $lListprog {

                                                  set lProgElement [split $lProgElement]

                                                  set iTskPropIndx [lsearch -exact $lProgElement "eServiceFeature"]
                                                  if {$iTskPropIndx == 0} {

                                                      set sProgIndexFeat [lsearch -exact $lProgElement "to"]
                                                      set sValIndexFeat  [lsearch -exact $lProgElement "value"]
                                                      if {($sValIndexFeat > 0) && ($sProgIndexFeat > 0)} {
                                                           set sPageProgram [string trim [lrange $lProgElement [expr $sProgIndexFeat + 2] [expr $sValIndexFeat - 1]]]
                                                           if {$sPageProgram != ""} {
                                                               append aCheckList "$sPageProgram$DUMPDELIMITER"
                                                           }
                                                      }
                                                  }
                                             }
                                   }
                          }
                          append aCheckList "^"
                     }
                }
             }
          }

          if {($mqlret == 0) && ($aCheckList != "")} {

              set lCheckList [split $aCheckList "^"]

              foreach lCheckListElement $lCheckList {

                                       set lCheckListElement [split $lCheckListElement $DUMPDELIMITER]

                                       if {$sRoleList == ""} {
                                          return "0|0"
                                       }
                                       mql quote off;
                                       set iBreakSet  0
                                       foreach lRoleItem $sRoleList {

                                           set lRolePropList [split [mql list property on role "$lRoleItem"] "\n"]
                                           if {$lRolePropList == ""} {
                                               continue
                                           }
                                           foreach lRoleElement $lRolePropList {

                                               set lRoleElement [split $lRoleElement]

                                               set iPropIndx    [lsearch -exact $lRoleElement "eServiceFeatureAccess"]
                                               if {$iPropIndx == 0} {

                                                 set sProgIndexRole [lsearch -exact $lRoleElement "program"]
                                                 if {$sProgIndexRole > 0} {
                                                      set sProgPageName [string trim [lrange $lRoleElement [expr $sProgIndexRole + 1] end]]
                                                 }

                                                 if {$sProgPageName != ""} {
                                                        set sIndex 0
                                                        foreach lCheckElement $lCheckListElement {
                                                                set lCheckElement [string trim $lCheckElement]
                                                                if {($lCheckElement != "") && ([string compare "$lCheckElement" "$sProgPageName"] == 0)} {
                                                                     set sIndex 1
                                                                     break
                                                                }
                                                        }
                                                        if {$sIndex > 0} {
                                                           set sCmd {mql print program "$sProgPageName" \
                                                                         select property\[$sSearchLangProp\].name \
                                                                         dump $DUMPDELIMITER }
                                                           set mqlret [catch {eval $sCmd} outstr]
                                                           if {$mqlret != 0} {
                                                              return "1|Error: $sProgName - $outstr"
                                                           } else {
                                                              set outstr [string trim $outstr]
                                                              if {$outstr != ""} {
                                                                 append aSuiteList [string trim [lindex $lCheckListElement 1]]
                                                                 append aSuiteList $DUMPDELIMITER
                                                                 set iBreakSet  1
                                                                 break
                                                              }
                                                           }
                                                        }
                                                 }
                                              }
                                           }
                                           if {$iBreakSet == 1} {
                                              break
                                           }
                                       }
                         }
                                       if {$aSuiteList == ""} {
                                          return "0|0"
                                       } else {
                                          append aFinalList "0$DUMPDELIMITER$aSuiteList"
                                          return $aFinalList
                                       }
          }
          if {$mqlret != 0} {

             return "1|Error: $sProgName - $outstr"
          }
}
##################################################################################

