###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.          #
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

    set RegProgName "eServiceSchemaVariableMapping.tcl"  
    # Load Utility function
    eval [utLoad $RegProgName]
    
    # Get Base path for all the stores
    set sBasePath [mql get env 1]

    # All the stores installed by ENOVIA applications
    # Append to this list any new store installed by ENOVIA in future.
    set lEnoviaStores [list \
                          store_ImageStore \
                          store_STORE \
                      ]

    foreach sEnoviaStore $lEnoviaStores {
        set sStoreName [eServiceGetCurrentSchemaName store $RegProgName "$sEnoviaStore"]
        if {"$sStoreName" == ""} {
            continue
        } else {
            set sStorePath [file join "$sBasePath" "$sStoreName"]
            set sCmd {mql modify store "$sStoreName" path "$sStorePath"}
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts ">ERROR       : emxCommonSetENOVIAStorePath.tcl"
                puts ">$outStr"
                puts ""
                return -code 1
            }
        }
    }
    
    return -code 0
}

