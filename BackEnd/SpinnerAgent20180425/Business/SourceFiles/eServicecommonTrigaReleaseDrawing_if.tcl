############################################################################
# $RCSfile: eServicecommonTrigaReleaseDrawing_if.tcl.rca $ $Revision: 1.46 $
#
# @libdoc       eServicecommonTrigaReleaseDrawing_if.tcl
#
# @Brief:       Release Drawing trigger program
#
# @libdoc       Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# The following sample code is provided for your reference purposes in
# connection with your use of the Matrix System (TM) software product
# which you have licensed from MatrixOne, Inc. ("MatrixOne").
# The sample code is provided to you without any warranty of any kind
# whatsoever and you agree to be responsible for the use and/or incorporation
# of the sample code into any software product you develop. You agree to fully
# and completely indemnify and hold MatrixOne harmless from any and all loss,
# claim, liability or damages with respect to your use of the Sample Code.
#
# Subject to the foregoing, you are permitted to copy, modify, and distribute
# the sample code for any purpose and without fee, provided that (i) a
# copyright notice in the in the form of "Copyright 1995 - 1998 MatrixOne Inc.,
# Two Executive Drive, Chelmsford, MA  01824. All Rights Reserved" appears
# in all copies, (ii) both the copyright notice and this permission notice
# appear in supporting documentation and (iii) you are a valid licensee of
# the Matrix System software.
#
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

    # include files
    eval [utLoad eServiceSchemaVariableMapping.tcl]
    eval [utLoad eServicecommonDrawingPartLib.tcl ]
    eval [utLoad eServicecommonShadowAgent.tcl]

    # Get env variables
    set sOid [ mql get env OBJECTID ]
    set nWarningLimit [ mql get env 1 ]
    if {"$nWarningLimit" == ""} {
        set nWarningLimit 500
    }

    # Get actual names from symbolic names
    set sRelationship [eServiceGetCurrentSchemaName \
                       attribute \
                       eServiceSchemaVariableMapping.tcl \
                       "relationship_PartSpecification" \
                      ]
    set sObsolete [eServiceGetCurrentSchemaName \
                       state \
                       eServiceSchemaVariableMapping.tcl \
                       "policy_ECPart" \
                       "state_Obsolete" \
                      ]

    # Get Previous Document ID
    set sCmd {mql print bus $sOid select previous.id dump | }
    set mqlret [ catch {eval  $sCmd} outStr ]
    if {$mqlret != 0} {
        mql notice "$outStr"
        return $mqlret
    }

    # If No Previos Drawing Revision is found, do nothing!
    set sPreviousDrawingID $outStr
    if { [ string compare $sPreviousDrawingID "" ] != 0 } {


        # Get list of objects related to previous drawing revision
        set sCmd {mql expand bus $sPreviousDrawingID \
                             to relationship "$sRelationship" \
                             select rel id \
                             select bus id state current \
                             where "!(next != '')" \
                             dump |}
        set mqlret [ catch {eval  $sCmd} outStr ]
        if {$mqlret != 0} {
            mql notice "$outStr"
            return $mqlret
        }
        set lInitialList [ split "$outStr" "\n" ]
        set lAllObjList ""

        set nCount 0
        foreach ele $lInitialList {
            set temp [ split $ele "|" ]
            set tempStateList ""
            set tempObjList ""
            for { set i 7 } { $i < [ expr [llength $temp]- 2 ] } { incr i }  {
                set tempStateList [ lappend tempStateList  [lindex $temp $i ] ]
            }
            set tempObjList [ list [ lindex $temp 6 ] $tempStateList [ lindex $temp [ expr [llength $temp]- 2 ]  ]   [ lindex $temp 3 ]  [ lindex $temp 4 ]  [ lindex $temp 5 ]  [ lindex $temp [ expr [llength $temp]- 1 ]  ] ]
            set lAllObjList [ lappend lAllObjList $tempObjList ]
            incr nCount
        }

        # lAllObjList contains all the parts connected to previous drawing, formatted in Lists of Lists

        # Remove all Obsolete Parts within lAllObjList List.
        set lAllActiveObjList [ ParseObjList $lAllObjList [list $sObsolete] "LT" ]

        # If connections to be changed are more than warning limit
        # then popup mql notice about process delay.
        if {[llength $lAllActiveObjList] > "$nWarningLimit"} {
            mql notice "The process will take a while!!!"
        }

        # Should connect only objects within lLatestRelObjList list that do no have Next Revisions.
        pushShadowAgent
        foreach ele $lAllActiveObjList {
            set sCmd {mql modify connection [ lindex $ele 6 ] to $sOid }
            set mqlret [ catch {eval  $sCmd} outStr ]
            if {$mqlret != 0} {
                mql notice "$outStr"
                popShadowAgent
                return $mqlret
            }
        }
        popShadowAgent
   }

   return 0
}

