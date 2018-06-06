############################################################################
# $RCSfile: eServicecommonTrigaRevisePart_if.tcl.rca $ $Revision: 1.44 $
#
# @libdoc       eServicecommonTrigaRevisePart_if.tcl
#
# @Brief:       Revise Part Trigger Program
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
    eval [utLoad eServicecommonShadowAgent.tcl]

    # Get env variables
    set sType [ mql get env TYPE]
    set sName [ mql get env NAME ]
    set sOid [ mql get env OBJECTID ]
    set sNextRev [ mql get env NEWREV ]
    set TBE_BOM_GO_TO_PRODUCTION [ mql get env TBE_BOM_GO_TO_PRODUCTION ]

    # Get actual names from symbolic names
    set sRelationship [eServiceGetCurrentSchemaName \
                       attribute \
                       eServiceSchemaVariableMapping.tcl \
                       "relationship_PartSpecification" \
                      ]

    # Find all Objects (Drawings) connected from this Object being revised.
    set sCmd {mql expand bus $sOid from relationship $sRelationship select bus id next select rel attribute attribute.value dump |}
    set mqlret [ catch {eval  $sCmd} outStr ]
    if {$mqlret != 0} {
        mql notice "$outStr"
        return $mqlret
    }

    set lInitialList [ split "$outStr" "\n" ]

    foreach ele $lInitialList {
        set temp [ split $ele "|" ]
        set sToChildNextRev [ lindex $temp 7 ]
        set sToChildRev [ lindex $temp 5 ]
        set sToChildType [ lindex $temp 3 ]
        set sToChildName [ lindex $temp 4 ]

        set sListLength [ expr [ llength $temp ] - 1 ]
        set lAttrInsertString {}
        if {$sListLength > 8} {
            set sAttrInfoLength [ expr $sListLength - 8 ]
            set lAttrNames [ lrange $temp  8  [expr 8 +  [expr $sAttrInfoLength / 2 ] ] ]
            set lAttrValues [ lrange $temp  [expr 1 + [expr 8 +  [expr $sAttrInfoLength / 2 ] ] ] end ]

            # Generate Relationship Attribute Insert String
            set listLength [ llength $temp ]

            for { set i 0 } { $i < [ llength $lAttrNames ] } { incr i } {
                lappend  lAttrInsertString "\"[lindex $lAttrNames $i ]\"" "\"[lindex $lAttrValues $i ]\""
            }
        }
        set sAttrInsertString [join $lAttrInsertString]


        # Determine the ID of object to connect to.
        if { ([ string compare $sToChildNextRev "" ] == 0) || ($TBE_BOM_GO_TO_PRODUCTION == "TRUE") } {
            pushShadowAgent
            set sCmd {mql connect bus "$sType" "$sName" "$sNextRev" relationship "$sRelationship" to "$sToChildType" "$sToChildName" "$sToChildRev"}
            append sCmd " $sAttrInsertString"
            set mqlret [ catch {eval  $sCmd} outStr ]
            popShadowAgent
            if {$mqlret != 0} {
                mql notice "$outStr"
                return $mqlret
            }
        } else {
            pushShadowAgent
            set sCmd {mql connect bus "$sType" "$sName" "$sNextRev" relationship "$sRelationship" to "$sToChildType" "$sToChildName" "$sToChildNextRev"}
            append sCmd " $sAttrInsertString"
            set mqlret [ catch {eval  $sCmd} outStr ]
            popShadowAgent
            if {$mqlret != 0} {
                mql notice "$outStr"
                return $mqlret
            }
        }
    }

    return 0
}

