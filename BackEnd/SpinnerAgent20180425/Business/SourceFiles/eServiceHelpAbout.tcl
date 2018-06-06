###############################################################################
#
# $RCSfile: eServiceHelpAbout.tcl.rca $ $Revision: 1.5 $
#
# Description:
# DEPENDENCIES: This program has to lie in Schema & Common directories with the
#               same name and should be called from all builds
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

 set sProgramName "eServiceSystemInformation.tcl"
 set sAppOnly [mql get env 1]

 set lList          [split [mql list program] "\n"]
 set iProgIndx      [lsearch -exact $lList "$sProgramName"]
 set aAppList       ""
 set DUMPDELIMITER  "|"

 if {$iProgIndx == -1} {
      return "1|Error: No Applications are installed"
 } else {
    #get all the properties on the found program
    set lProperties [lindex [lindex [mql print program $sProgramName select property.name property.value dump tcl] 0] 0]
    set lValues [lindex [lindex [mql print program $sProgramName select property.name property.value dump tcl] 0] 1]

    # for each property
    foreach sProp $lProperties sValue $lValues {

        if {[string first "appVersion" "$sProp"] == 0} {
            regsub "appVersion" "$sProp" "" sAppName
            set nNightlyIndex [lsearch $lProperties "appNightly$sAppName"]
            set sVersion "$sValue"
            if {"$nNightlyIndex" >= 0} {
                set sNightlyValue [lindex $lValues $nNightlyIndex]
                append sVersion " ($sNightlyValue)"
            }
            
            append aAppList "$sAppName$DUMPDELIMITER$sVersion$DUMPDELIMITER"
        }
        if {"$sAppOnly" != "TRUE"} {
            if {[string first "featureVersion" "$sProp"] == 0} {
                regsub "featureVersion" "$sProp" "" sAppName
                set nNightlyIndex [lsearch $lProperties "appNightly$sAppName"]
                set sVersion "$sValue"
                if {"$nNightlyIndex" >= 0} {
                    set sNightlyValue [lindex $lValues $nNightlyIndex]
                    append sVersion " ($sNightlyValue)"
                }
                
                append aAppList "$sAppName$DUMPDELIMITER$sVersion$DUMPDELIMITER"
            }
        }
     }

     if {$aAppList == ""} {
        return "0"
     } else {
        return "0|$aAppList"
     }
 }
}

