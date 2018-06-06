###############################################################################
#
# $RCSfile: eServicecommonGetViewers.tcl.rca $ $Revision: 1.20 $
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# Input : List of persons to be deleted.
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
#
# Load MQL/Tcl utility procedures
#
###############################################################################

    set sProgName     "eServicecommonGetViewers.tcl"

    set sFormat     [string trim [mql get env 1]]
    set outStr      ""
    set mqlret      0
    
    mql verbose off

    set sCmd {mql print format "$sFormat" select property\[supportedViewer\].to dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        return "1|Error: - $outStr"
    }

    set lOutput [split $outStr |]
    set lViewers {}
    foreach sItem $lOutput {
        set lItem [split $sItem]
        set sViewer [lindex $lItem 1]
        lappend lViewers $sViewer
        set sCmd {mql print program "$sViewer" select property\[viewerTip\].value dump}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            return "1|Error: - $outStr"
        }
        lappend lViewers "$outStr"
    }
    set sViewer [join $lViewers |]

    return "0|$sViewer"
}
##################################################################################

