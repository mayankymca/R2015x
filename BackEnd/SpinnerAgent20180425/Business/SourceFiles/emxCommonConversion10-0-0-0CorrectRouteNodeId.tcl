#################################################################################
#
# $RCSfile: emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl.rca $ $Revision: 1.7 $
#
# Description: This conversion routine needs to be run only if AEF is upgraded 
#              from pre 10-0-0-0 to 10-5 or above.
#
#################################################################################
#################################################################################
#                                                                               #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                   #
#   This program contains proprietary and trade secret information of           #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not       #
#   evidence any actual or intended publication of such program.                #
#                                                                               #
#################################################################################

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

    set sProgramName "eServiceSystemInformation.tcl"
    # Get Input
    # Directory in which Route files are stored
    set sDir [string trim [mql get env 1]]
    # Log file name
    set sLogFile [string trim [mql get env 2]]
    # File begin and end number
    set sRouteFileIndex [string trim [mql get env 3]]
    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    if {"$sRouteFileIndex" == ""} {
        set nRouteStartIndex 1
        set nRouteEndIndex 100000
    } else {
        set nRouteStartIndex [lindex [split $sRouteFileIndex ,] 0]
        set nRouteEndIndex [lindex [split $sRouteFileIndex ,] 1]
    }
    set sLogFile [open "$sLogFile" a 0666]
    # Load utility files
    eval [utLoad $sRegProgName]

    # Check if conversion routine is already executed.
    set sCmd {mql print program "eServiceSystemInformation.tcl" \
                  select property\[Conversion10-0-0-0CorrectRouteNodeId\].value \
                  dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
        puts $sLogFile "$outStr"
        return -code 1
    }
    if {"$outStr" == "Executed"} {
        puts stdout "Conversion10-0-0-0CorrectRouteNodeId already executed"
        puts stdout "No need to run it agian"
        close $sLogFile
        return -code 0
    }

    # Get admin names from properties
    set sTypeRoute [eServiceGetCurrentSchemaName type $sRegProgName type_Route]
    set sTypeInboxTask [eServiceGetCurrentSchemaName type $sRegProgName type_InboxTask]
    set sAttRouteNodeId [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_RouteNodeID]
    set sAttScheduledCompletionDate [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_ScheduledCompletionDate]
    set sAttAllowDelegation [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_AllowDelegation]
    set sAttRouteTaskUser [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_RouteTaskUser]
    set sAttRouteTaskUserCompany [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_RouteTaskUserCompany]
    set sAttApproversResponsibility [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_ApproversResponsibility]
    set sAttRouteAction [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_RouteAction]
    set sAttRouteInstructions [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_RouteInstructions]
    set sAttTitle [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_Title]
    set sRelRouteTask [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_RouteTask]
    set sRelRouteNode [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_RouteNode]

    set sBaseFileName "[join [split $sTypeRoute] ""]List"

    # For each Route List File
    for {set nCounter $nRouteStartIndex} {$nCounter <= $nRouteEndIndex} {incr nCounter} {
        if {[file exists "$sDir/$sBaseFileName$nCounter"] != 1} {
            break
        }
        puts $sLogFile "*****************************************************"
        puts $sLogFile "*Processing Routes In $sDir/$sBaseFileName$nCounter File..."
        puts $sLogFile "*****************************************************"
        
        # Start Transaction
        set sCmd {mql start transaction}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
            puts $sLogFile "$outStr"
            return -code 1
        }

        # Read all the entries in the file and form a set
        set sFileId [open "$sDir/$sBaseFileName$nCounter" r]
        set sCmd {mql add set "emxRouteSet"}
        while {[gets $sFileId sLine] != -1} {
            set sLine [string trim $sLine]
            # continue if line is empty
            if {"$sLine" == ""} {
                continue
            }
            
            append sCmd " member bus \"$sLine\""

        }
        close $sFileId
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
            puts $sLogFile "$outStr"
            mql abort transaction
            return -code 1
        }

        # Get all the Routes and it's connection info
        set sCmd {mql print set "emxRouteSet" \
                      select \
                      type \
                      name \
                      revision \
                      from\[$sRelRouteNode\].id \
                      from\[$sRelRouteNode\].attribute\[$sAttAllowDelegation\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttApproversResponsibility\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttRouteAction\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttRouteInstructions\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttRouteTaskUser\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttRouteTaskUserCompany\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttScheduledCompletionDate\].value \
                      from\[$sRelRouteNode\].attribute\[$sAttTitle\].value \
                      to\[$sRelRouteTask\].businessobject.id \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttAllowDelegation\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttApproversResponsibility\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttRouteAction\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttRouteInstructions\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttRouteTaskUser\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttRouteTaskUserCompany\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttScheduledCompletionDate\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttTitle\].value \
                      to\[$sRelRouteTask\].businessobject.attribute\[$sAttRouteNodeId\].value \
                      dump tcl \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
            puts $sLogFile ">$outStr"
            mql abort transaction
            return -code 1
        }
        set lOutput $outStr

        # Foreach Route object
        foreach sObj $lOutput {
            # Get Route Info
            set sRouteName [lindex $sObj 1]
            set sRouteRev [lindex $sObj 2]

            # Get Route Node Id info
            set lRN_Id [lindex $sObj 3]
            if {"[string trim [lindex $lRN_Id 0]]" == ""} {
                continue
            }
            set lRN_AttAllowDelegation [lindex $sObj 4]
            set lRN_AttApproversResponsibility [lindex $sObj 5]
            set lRN_AttRouteAction [lindex $sObj 6]
            set lRN_AttRouteInstructions [lindex $sObj 7]
            set lRN_AttRouteTaskUser [lindex $sObj 8]
            set lRN_AttRouteTaskUserCompany [lindex $sObj 9]
            set lRN_AttScheduledCompletionDate [lindex $sObj 10]
            set lRN_AttTitle [lindex $sObj 11]

            # Get Inbox Task Info
            set lIB_Id [lindex $sObj 12]
            set lIB_AttAllowDelegation [lindex $sObj 13]
            set lIB_AttApproversResponsibility [lindex $sObj 14]
            set lIB_AttRouteAction [lindex $sObj 15]
            set lIB_AttRouteInstructions [lindex $sObj 16]
            set lIB_AttRouteTaskUser [lindex $sObj 17]
            set lIB_AttRouteTaskUserCompany [lindex $sObj 18]
            set lIB_AttScheduledCompletionDate [lindex $sObj 19]
            set lIB_AttTitle [lindex $sObj 20]
            set lIB_AttRouteNodeId [lindex $sObj 21]

            # Foreach Route Node Id
            foreach sRouteNodeId $lRN_Id {

                # Populate attribute Route Node ID on Route Node connection with
                # connection id.
                set sCmd {mql modify connection $sRouteNodeId \
                              "$sAttRouteNodeId" "$sRouteNodeId" \
                              }
                set mqlret [ catch {eval $sCmd} outStr ]
                if {$mqlret != 0} {
                    puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
                    puts $sLogFile ">$outStr"
                    mql abort transaction
                    return -code 1
                }
            }

            # Continue if there are no Inbox Tasks
            if {"[string trim [lindex $lIB_Id 0]]" == ""} {
                continue
            }

            # Foreach Inbox Task
            foreach sInboxTaskId $lIB_Id \
                    sIB_AttAllowDelegation $lIB_AttAllowDelegation \
                    sIB_AttRouteNodeId $lIB_AttRouteNodeId \
                    sIB_AttApproversResponsibility $lIB_AttApproversResponsibility \
                    sIB_AttRouteAction $lIB_AttRouteAction \
                    sIB_AttRouteInstructions $lIB_AttRouteInstructions \
                    sIB_AttRouteTaskUser $lIB_AttRouteTaskUser \
                    sIB_AttRouteTaskUserCompany $lIB_AttRouteTaskUserCompany \
                    sIB_AttScheduledCompletionDate $lIB_AttScheduledCompletionDate \
                    sIB_AttTitle $lIB_AttTitle {

                # check if Route Node connection exists
                # as per data in attribute Route Node ID on Inbox Task
                # If exists then continue to next Inbox Task
                if {[lsearch $lRN_Id "$sIB_AttRouteNodeId"] >= 0} {
                    continue
                }

                # else Route Node connection Id has changed
                # check each Route Node Id data and get corresponding
                # Route Node connection.
                set lRN_Id_Match {}
                foreach sRN_Id $lRN_Id \
                        sRN_AttAllowDelegation $lRN_AttAllowDelegation \
                        sRN_AttApproversResponsibility $lRN_AttApproversResponsibility \
                        sRN_AttRouteAction $lRN_AttRouteAction \
                        sRN_AttRouteInstructions $lRN_AttRouteInstructions \
                        sRN_AttRouteTaskUser $lRN_AttRouteTaskUser \
                        sRN_AttRouteTaskUserCompany $lRN_AttRouteTaskUserCompany \
                        sRN_AttScheduledCompletionDate $lRN_AttScheduledCompletionDate \
                        sRN_AttTitle $lRN_AttTitle {

                    # Make a list of all the Route Node connections
                    # that has matching attribute settings as on Inbox Task
                    if {"$sRN_AttAllowDelegation" == "$sIB_AttAllowDelegation" && \
                        "$sRN_AttApproversResponsibility" == "$sIB_AttApproversResponsibility" && \
                        "$sRN_AttRouteAction" == "$sIB_AttRouteAction" && \
                        "$sRN_AttRouteInstructions" == "$sIB_AttRouteInstructions" && \
                        "$sRN_AttRouteTaskUser" == "$sIB_AttRouteTaskUser" && \
                        "$sRN_AttRouteTaskUserCompany" == "$sIB_AttRouteTaskUserCompany" && \
                        "$sRN_AttScheduledCompletionDate" == "$sIB_AttScheduledCompletionDate" && \
                        "$sRN_AttTitle" == "$sIB_AttTitle"} {

                            lappend lRN_Id_Match "$sRN_Id"
                    }
                }

                set nMatches [llength $lRN_Id_Match]
                switch $nMatches {
                    0 {
                        # If none of them are matching then
                        # give a warning about unsolved Inbox Task
                        puts $sLogFile ">WARNING     : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
                        puts $sLogFile ">$sTypeInboxTask with id = $sInboxTaskId is storing wrong Route Node Connection Id"
                    }
                    1 {
                        # If only one connection found then
                        # modify Inbox Task to hold proper Route Node Id
                        set sCmd {mql modify bus $sInboxTaskId \
                                      "$sAttRouteNodeId" "[lindex $lRN_Id_Match 0]" \
                                      }
                        set mqlret [ catch {eval $sCmd} outStr ]
                        if {$mqlret != 0} {
                            puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
                            puts $sLogFile ">$outStr"
                            mql abort transaction
                            return -code 1
                        }
                    }
                    default {
                        # If more then one are matching then
                        # give a warning about unsolved Inbox Task
                        puts $sLogFile ">WARNING     : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
                        puts $sLogFile ">$sTypeInboxTask with id = $sInboxTaskId is storing wrong Route Node Connection Id"
                        puts $sLogFile ">It should store one of the following Route Node connection Ids"
                        foreach sRN_Id $lRN_Id_Match {
                            puts $sLogFile ">$sRN_Id"
                        }
                    }
                }
            }
        }

        set sCmd {mql delete set "emxRouteSet"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
            puts $sLogFile "$outStr"
            mql abort transaction
            return -code 1
        }
        
        set sCmd {mql commit transaction}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion10-0-0-0CorrectRouteNodeId.tcl"
            puts $sLogFile "$outStr"
            return -code 1
        }
    }

    close $sLogFile
    
    return -code 0
}

