#################################################################################
#
# $RCSfile: emxCommonConversion9-5-1-0CopyDispositionAtt.tcl.rca $ $Revision: 1.7 $
#
# Description:
# DEPENDENCIES: This program has to lie in Schema & Common directories with the
#               same name and should be called from all builds
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

###############################################################################
#
# Procedure:    escapeSpecialChars
#
# Description:  Procedure to escape special chars from string
#
# Parameters:   sString                 - String contening special chars
#
# Returns:      sOutput                 - String with escaped special chars.
#
###############################################################################

proc escapeSpecialChars { sString } {
    regsub -all {#} "$sString" {\#} sString
    regsub -all {\\\$} "$sString" {\\\\$} sString
    regsub -all {\[} "$sString" {\\[} sString
    regsub -all {\]} "$sString" {\\]} sString
    regsub -all {"} "$sString" {\"} sString
    regsub -all {'} "$sString" {\'} sString
    
    return "$sString"
}
# end escapeSpecialChars

    # Get environment variables
    set sDir [string trim [mql get env 1]]
    set sLogFile [string trim [mql get env 2]]
    set sPartFileIndex [string trim [mql get env 3]]
    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    if {"$sPartFileIndex" == ""} {
        set nPartStartIndex 1
        set nPartEndIndex 100000
    } else {
        set nPartStartIndex [lindex [split $sPartFileIndex ,] 0]
        set nPartEndIndex [lindex [split $sPartFileIndex ,] 1]
    }
    set sLogFile [open "$sLogFile" a 0666]

    # Load utility files
    eval [utLoad $sRegProgName]

    # Check if conversion routine is already executed.
    set sCmd {mql print program "eServiceSystemInformation.tcl" \
                  select property\[Conversion9-5-1-0CopyDispositionAtt\].value \
                  dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
        puts $sLogFile "$outStr"
        return -code 1
    }
    if {"$outStr" == "Executed"} {
        puts stdout "Conversion9-5-1-0CopyDispositionAtt already executed"
        puts stdout "No need to run it agian"
        close $sLogFile
        return -code 0
    }

    # Get admin names from symbolic names.
    set sTypePart [eServiceGetCurrentSchemaName type $sRegProgName type_Part]
    set sRelRequestPartObsolescence [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_RequestPartObsolescence]
    set sRelRequestPartRevision [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_RequestPartRevision]
    set sRelNewPartPartRev [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_NewPartPartRevision]
    set sRelMakeObsolete [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_MakeObsolete]

    set sBaseFileName "[join [split $sTypePart] ""]List"
    # Get all the attributes on Request Part Revision relationship
    set sCmd {mql print relationship "$sRelRequestPartRevision" select attribute dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
        puts $sLogFile ">$outStr"
        return -code 1
    }
    set lRPRAtt [split $outStr |]

    # Get all the attributes on New Part Part Rev relationship
    set sCmd {mql print relationship "$sRelNewPartPartRev" select attribute dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
        puts $sLogFile ">$outStr"
        return -code 1
    }
    set lNPNRAtt [split $outStr |]

    # Get only attributes common between New Part New Revision and Request Part Revision relationships
    set lNewPartCommonAtt {}
    foreach sAtt $lNPNRAtt {
        if {[lsearch $lRPRAtt "$sAtt"] != -1} {
            lappend lNewPartCommonAtt "$sAtt"
        }
    }

    # Get all the attributes on Request Part Obsolescence relationship
    set sCmd {mql print relationship "$sRelRequestPartObsolescence" select attribute dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
        puts $sLogFile ">$outStr"
        return -code 1
    }
    set lRPOAtt [split $outStr |]

    # Get all the attributes on Make Obsolete relationship
    set sCmd {mql print relationship "$sRelMakeObsolete" select attribute dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
        puts $sLogFile ">$outStr"
        return -code 1
    }
    set sMOAtt [split $outStr |]

    # Get only attributes common between Make Obsolete and Request Part Obsolescence relationships
    set lObsoleteCommonAtt {}
    foreach sAtt $sMOAtt {
        if {[lsearch $lRPOAtt "$sAtt"] != -1} {
            lappend lObsoleteCommonAtt "$sAtt"
        }
    }

    # For each Part List File
    for {set nFileCount $nPartStartIndex} {$nFileCount <= $nPartEndIndex} {incr nFileCount} {

        if {[file exists "$sDir/$sBaseFileName$nFileCount"] != 1} {
            break
        }
        puts $sLogFile "*****************************************************"
        puts $sLogFile "*Processing Parts In $sDir/$sBaseFileName$nFileCount File..."
        puts $sLogFile "*****************************************************"

        # Start Transaction
        set sCmd {mql start transaction}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
            puts $sLogFile "$outStr"
            return -code 1
        }

        # Read all the entries in the file and form a set
        set sFileId [open "$sDir/$sBaseFileName$nFileCount" r]
        set sCmd {mql add set "emxPartSet"}
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
            puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
            puts $sLogFile "$outStr"
            mql abort transaction
            return -code 1
        }

        if {[llength $lNewPartCommonAtt] != 0} {
            # Get all the Parts and all the attributes on Request Part Revision rel connected from it
            # also get relationship New Part Part Rev relationship id connected to it.
            set sCmd "mql print set \"emxPartSet\" select type name revision id to\\\\\\[$sRelRequestPartRevision\\\\\\].id next.to\\\\\\[$sRelNewPartPartRev\\\\\\].id"
            foreach sAtt $lNewPartCommonAtt {
                append sCmd " to\\\\\\[$sRelRequestPartRevision\\\\\\].attribute\\\\\\[$sAtt\\\\\\].value"
            }
            append sCmd " dump tcl"
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                puts $sLogFile ">$outStr"
                mql abort transaction
                return -code 1
            }

            # Copy all the attributes values to New Part New revision relationship
            set lOutput "$outStr"
            foreach lLine $lOutput {
                set sType [lindex $lLine 0]
                set sName [lindex $lLine 1]
                set sRev [lindex $lLine 2]
                set sPartId [lindex [lindex $lLine 3] 0]

                set sRequestPartRevisionId [lindex [lindex $lLine 4] 0]
                set sNewPartPartRevId [lindex [lindex $lLine 5] 0]

                if {"$sNewPartPartRevId" != "" && "$sRequestPartRevisionId" != ""} {

                    set sCmd "mql modify connection $sNewPartPartRevId"
                    set nCounter 0
                    foreach sAtt $lNewPartCommonAtt {
                        set sAttValue "[lindex [lindex $lLine [expr 6 + $nCounter]] 0]"
                        set sAttValue [escapeSpecialChars "$sAttValue"]
                        append sCmd " \"$sAtt\" \"$sAttValue\""
                        incr nCounter
                    }

                    # set escape on                
                    set sEscapeCmd {mql set escape on}
                    set mqlret [ catch {eval $sEscapeCmd} outStr ]
                    if {$mqlret != 0} {
                        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                        puts $sLogFile ">$outStr"
                        mql abort transaction
                        return -code 1
                    }

                    set mqlret [ catch {eval $sCmd} outStr ]
                    if {$mqlret != 0} {
                        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                        puts $sLogFile ">$outStr"
                        mql set escape off
                        mql abort transaction
                        return -code 1
                    }

                    # set escape off
                    set sEscapeCmd {mql set escape off}
                    set mqlret [ catch {eval $sEscapeCmd} outStr ]
                    if {$mqlret != 0} {
                        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                        puts $sLogFile ">$outStr"
                        mql abort transaction
                        return -code 1
                    }
                }
            }
        }

        if {[llength $lObsoleteCommonAtt] != 0} {
            # Get all the Parts and all the attributes on Request Part Obsolescence rel connected from it
            # also get relationship Make Obsolete relationship id connected to it.
            set sCmd "mql print set \"emxPartSet\" select type name revision id to\\\\\\[$sRelRequestPartObsolescence\\\\\\].id to\\\\\\[$sRelMakeObsolete\\\\\\].id"
            foreach sAtt $lObsoleteCommonAtt {
                append sCmd " to\\\\\\[$sRelRequestPartObsolescence\\\\\\].attribute\\\\\\[$sAtt\\\\\\].value"
            }
            append sCmd " dump tcl"
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                puts $sLogFile ">$outStr"
                mql abort transaction
                return -code 1
            }

            # Copy all the attributes values to Make Obsolete relationship
            set lOutput "$outStr"
            foreach lLine $lOutput {
                set sType [lindex $lLine 0]
                set sName [lindex $lLine 1]
                set sRev [lindex $lLine 2]

                set sPartId [lindex [lindex $lLine 3] 0]
                set sRequestPartObsolescenceId [lindex [lindex $lLine 4] 0]
                set sNewMakeObsoleteId [lindex [lindex $lLine 5] 0]

                if {"$sRequestPartObsolescenceId" != "" && "$sNewMakeObsoleteId" != ""} {

                    set sCmd "mql modify connection $sNewMakeObsoleteId"
                    set nCounter 0
                    foreach sAtt $lObsoleteCommonAtt {
                        set sAttValue "[lindex [lindex $lLine [expr 6 + $nCounter]] 0]"
                        set sAttValue [escapeSpecialChars "$sAttValue"]
                        append sCmd " \"$sAtt\" \"$sAttValue\""
                        incr nCounter
                    }

                    # set escape on                
                    set sEscapeCmd {mql set escape on}
                    set mqlret [ catch {eval $sEscapeCmd} outStr ]
                    if {$mqlret != 0} {
                        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                        puts $sLogFile ">$outStr"
                        mql abort transaction
                        return -code 1
                    }

                    set mqlret [ catch {eval $sCmd} outStr ]
                    if {$mqlret != 0} {
                        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                        puts $sLogFile ">$outStr"
                        mql abort transaction
                        mql set escape off
                        return -code 1
                    }

                    # set escape off
                    set sEscapeCmd {mql set escape off}
                    set mqlret [ catch {eval $sEscapeCmd} outStr ]
                    if {$mqlret != 0} {
                        puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
                        puts $sLogFile ">$outStr"
                        mql abort transaction
                        return -code 1
                    }
                }
            }
        }

        set sCmd {mql delete set "emxPartSet"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
            puts $sLogFile "$outStr"
            mql abort transaction
            return -code 1
        }

        set sCmd {mql commit transaction}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts $sLogFile ">ERROR       : emxCommonConversion9-5-1-0CopyDispositionAtt.tcl"
            puts $sLogFile "$outStr"
            return -code 1
        }
    }

    close $sLogFile

    return -code 0
}

