###############################################################################
#
# $RCSfile: emxPartSubscriptionMenu.tcl.rca $ $Revision: 1.1.1.3 $
#
# Description:This program adds all Menus common for all the applications
# DEPENDENCIES: an environment variable MXSPCBUILD must be set which points to
#               a directory which has a specified directory structure and set
#               of files below it.
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

    set Item "menu"
    set RegProgName   [mql get env REGISTRATIONOBJECT]
    # Load Utility function
    eval [utLoad $RegProgName]

    set lList_APPPartEvents \
    [ list {ADD MENU} \
           {version=10-7-SP1} \
           {property=menu_APPPartEvents} \
           {name=APPPartEvents} \
    ]

    set lCmd [ list $lList_APPPartEvents \
             ]

    set mqlret [emxInstallAdmin $lCmd $Item]

    mql verbose off;

    return -code $mqlret ""
}

