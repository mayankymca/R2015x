###############################################################################
#
# $RCSfile: eServiceConversion8-0-1-0MigrateTaskAttributes.tcl.rca $ $Revision: 1.5 $
# Description:  This file helps in migrating the values of attibutes on task
#               to their new names
# Dependencies:
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
    mql verbose on;

    # get needed information
    set RegProgName        [mql get env REGISTRATIONOBJECT]
    set DUMPDELIMITER      "|"
    set mqlret             0
    set outstr             ""

    # Load Utility function
    eval [utLoad $RegProgName]

    set sAttEstimatedMax    [emxGetCurrentSchemaName  attribute    $RegProgName  attribute_EstimatedMax ]
    set sAttEstimatedMin    [emxGetCurrentSchemaName  attribute    $RegProgName  attribute_EstimatedMin ]
    set sAttActualTime      [emxGetCurrentSchemaName  attribute    $RegProgName  attribute_ActualTime ]
    set sTypeTask           [emxGetCurrentSchemaName  type         $RegProgName  type_Task ]
    set sAttEstimatedMaxTsk [emxGetCurrentSchemaName  attribute    $RegProgName  attribute_TaskEstimatedDurationMaximum ]
    set sAttEstimatedMinTsk [emxGetCurrentSchemaName  attribute    $RegProgName  attribute_TaskEstimatedDurationMinimum ]
    set sAttActualTimeTsk   [emxGetCurrentSchemaName  attribute    $RegProgName  attribute_TaskActualDuration ]

    if {"$sAttEstimatedMax" == "" ||
        "$sAttEstimatedMin" == "" ||
        "$sAttActualTime" == ""} {
        return -code 0
    }

    set sCmd {mql print type "$sTypeTask" \
                        select attribute\[$sAttEstimatedMax\].default \
                               attribute\[$sAttEstimatedMin\].default \
                               attribute\[$sAttActualTime\].default \
                               dump $DUMPDELIMITER \
                               }
    set mqlret [ catch {eval $sCmd} outstr ]
    if {$mqlret != 0} {
         mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
         mql trace type INSTALL text ">$outstr"
         mql trace type INSTALL text ""
         return -code 1
    }

    set lData [split $outstr $DUMPDELIMITER]
    set rEstMaxTemp [string trim [lindex $lData 0]]
    set rEstMinTemp [string trim [lindex $lData 1]]
    set rActTmeTemp [string trim [lindex $lData 2]]

    if {$rEstMaxTemp != ""} {
        set sCmd {mql modify attribute "$sAttEstimatedMaxTsk" \
                             default "$rEstMaxTemp" \
                             }
        set mqlret [ catch {eval $sCmd} outstr ]
        if {$mqlret != 0} {
             mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
             mql trace type INSTALL text ">$outstr"
             mql trace type INSTALL text ""
             return -code 1
        }
    }
    if {$rEstMinTemp != ""} {
        set sCmd {mql modify attribute "$sAttEstimatedMinTsk" \
                             default "$rEstMinTemp" \
                             }
        set mqlret [ catch {eval $sCmd} outstr ]
        if {$mqlret != 0} {
             mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
             mql trace type INSTALL text ">$outstr"
             mql trace type INSTALL text ""
             return -code 1
        }
    }
    if {$rActTmeTemp != ""} {
        set sCmd {mql modify attribute "$sAttActualTimeTsk" \
                             default "$rActTmeTemp" \
                             }
        set mqlret [ catch {eval $sCmd} outstr ]
        if {$mqlret != 0} {
             mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
             mql trace type INSTALL text ">$outstr"
             mql trace type INSTALL text ""
             return -code 1
        }
    }

    set sCmd {mql temp query bus "$sTypeTask" * * \
                        select attribute\[$sAttEstimatedMax\].value \
                               attribute\[$sAttEstimatedMin\].value \
                               attribute\[$sAttActualTime\].value \
                               dump $DUMPDELIMITER \
                               }
    set mqlret [ catch {eval $sCmd} outstr ]
    if {$mqlret != 0} {
         mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
         mql trace type INSTALL text ">$outstr"
         mql trace type INSTALL text ""
         return -code 1
    }

    set lData [split $outstr \n]
    foreach lElement $lData {
            set lElement    [split [string trim $lElement] $DUMPDELIMITER]
            set sTaskName   [string trim [lindex $lElement 1]]
            set sTaskVer    [string trim [lindex $lElement 2]]
            set rEstMaxTemp [string trim [lindex $lElement 3]]
            set rEstMinTemp [string trim [lindex $lElement 4]]
            set rActTmeTemp [string trim [lindex $lElement 5]]
            if {$mqlret == 0} {
                 set sCmd {mql modify businessobject "$sTypeTask" "$sTaskName" "$sTaskVer" \
                                      "$sAttEstimatedMaxTsk" "$rEstMaxTemp" \
                                      "$sAttEstimatedMinTsk" "$rEstMinTemp" \
                                      "$sAttActualTimeTsk" "$rActTmeTemp" \
                                      }
                 set mqlret [ catch {eval $sCmd} outstr ]
            }
            if {$mqlret != 0} {
                 mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
                 mql trace type INSTALL text ">$outstr"
                 mql trace type INSTALL text ""
                 return -code 1
            }
    }

    set sCmd {mql delete attribute "$sAttEstimatedMax" \
             }
    set mqlret [ catch {eval $sCmd} outstr ]
    if {$mqlret != 0} {
         mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
         mql trace type INSTALL text ">$outstr"
         mql trace type INSTALL text ""
         return -code 1
    }
    set sCmd {mql delete attribute "$sAttEstimatedMin" \
             }
    set mqlret [ catch {eval $sCmd} outstr ]
    if {$mqlret != 0} {
         mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
         mql trace type INSTALL text ">$outstr"
         mql trace type INSTALL text ""
         return -code 1
    }
    set sCmd {mql delete attribute "$sAttActualTime" \
             }
    set mqlret [ catch {eval $sCmd} outstr ]
    if {$mqlret != 0} {
         mql trace type INSTALL text ">ERROR       : eServiceConversion8-0-1-0MigrateTaskAttributes.tcl"
         mql trace type INSTALL text ">$outstr"
         mql trace type INSTALL text ""
         return -code 1
    }
    
    return -code 0
}

