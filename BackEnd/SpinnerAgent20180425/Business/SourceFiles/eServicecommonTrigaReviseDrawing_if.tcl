############################################################################
# $RCSfile: eServicecommonTrigaReviseDrawing_if.tcl.rca $ $Revision: 1.46 $
#
# @libdoc       eServicecommonTrigaReviseDrawing_if.tcl
#
# @Brief:       Revise Drawing trigger program
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
    eval [utLoad eServicecommonDrawingPartLib.tcl]
    eval [utLoad eServicecommonShadowAgent.tcl]

    # Get env variables
    set sOid [ mql get env OBJECTID ]
    set sNewRev [ mql get env NEWREV ]

    # Get actual names from symbolic names
    set sRelationship [eServiceGetCurrentSchemaName \
                       attribute \
                       eServiceSchemaVariableMapping.tcl \
                       "relationship_PartSpecification" \
                      ]
    set sStateRelease [eServiceGetCurrentSchemaName \
                       state \
                       eServiceSchemaVariableMapping.tcl \
                       policy_ECPart \
                       state_Release \
                      ]
    set sStateObsolete [eServiceGetCurrentSchemaName \
                        state \
                        eServiceSchemaVariableMapping.tcl \
                        policy_ECPart \
                        state_Obsolete \
                       ]
    set sStateComplete [eServiceGetCurrentSchemaName \
                        state \
                        eServiceSchemaVariableMapping.tcl \
                        policy_DevelopmentPart \
                        state_Complete \
                       ]

    # Get All Parts connected through 'Part Specification' relationship and their information.
    set sCmd {mql expand bus $sOid \
                         to relationship "$sRelationship" \
                         select rel id \
                         select bus id state current \
                         where "next.from\[$sRelationship\].businessobject.id == $sOid or previous.from\[$sRelationship\].businessobject.id == $sOid" \
                         dump |}
    set mqlret [ catch {eval  $sCmd} outStr ]
    if {$mqlret != 0} {
        mql notice "$outStr"
        return $mqlret
    }
    set lInitialList [ split "$outStr" "\n" ]
    set lAllObjList ""

    set sCmd {mql expand bus $sOid \
                         to relationship "$sRelationship" \
                         select bus revisions \
                         where "next.from\[$sRelationship\].businessobject.id == $sOid or previous.from\[$sRelationship\].businessobject.id == $sOid" \
                         dump |}
    set mqlret [ catch {eval  $sCmd} outStr ]
    if {$mqlret != 0} {
        mql notice "$outStr"
        return $mqlret
    }
    set lAllRevList [ split "$outStr" "\n" ]

    set nCount 0
    foreach ele $lInitialList {
        set temp [ split $ele "|" ]
        set tempStateList ""
        set tempObjList ""
        for { set i 7 } { $i < [ expr [llength $temp]- 2 ] } { incr i }  {
            set tempStateList [ lappend tempStateList  [lindex $temp $i ] ]
        }
        set temp2 [  split [ lindex $lAllRevList $nCount ] "|" ]
        set tempRevList [ lrange $temp2 6 end ]
        set tempObjList [ list [ lindex $temp 6 ] $tempStateList [ lindex $temp [ expr [llength $temp]- 2 ]  ]   [ lindex $temp 3 ]  [ lindex $temp 4 ]  [ lindex $temp 5 ]  [ lindex $temp [ expr [llength $temp]- 1 ]  ] $tempRevList ]
        set lAllObjList [ lappend lAllObjList $tempObjList ]
        incr nCount
    }

    # Discard all the objects in Obsolete state within lAllObjList.
    set lObsoleteState [list "$sStateObsolete"]
    set mqlret [ catch {eval  ParseObjList {$lAllObjList} {$lObsoleteState} {LT}} lAllActiveObjList ]
    if {$mqlret != 0} {
        return $mqlret
    }

    # Keep objects that has multiple revisions within lAllActiveObjList.
    #set mqlret [ catch {eval  ParseMultiRevObj {$lAllActiveObjList}} lMultiRevObjList ]
    #if {$mqlret != 0} {
        #return $mqlret
    #}

    # Keep the later revision objects within lMultiRevObjList.
    set mqlret [ catch {eval  ParseLaterRevObj {$lAllActiveObjList}} lLaterRevObjList ]
    if {$mqlret != 0} {
        return $mqlret
    }

    # Discard all the objects in Release state within lAllObjList.
    set lReleaseState [list "$sStateRelease" "$sStateComplete"]
    set mqlret [ catch {eval  ParseObjList {$lLaterRevObjList} {$lReleaseState} {LT}} lFinalObjList ]
    if {$mqlret != 0} {
        return $mqlret
    }

    # Move connection to Later revision of Drawing.
    pushShadowAgent
    foreach lObjInfo $lFinalObjList {
        set sConnectId [lindex $lObjInfo 6]
        set sType [mql get env TYPE]
        set sName [mql get env NAME]

        set sCmd {mql modify connection "$sConnectId" to "$sType" "$sName" "$sNewRev"}
        set mqlret [ catch {eval  $sCmd} outStr ]

        if {$mqlret != 0} {
            mql notice "$outStr"
            popShadowAgent
            return $mqlret
        }
    }
    popShadowAgent
    return 0
}

