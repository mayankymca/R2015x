############################################################################
# $RCSfile: eServicecommonTrigaDeleteDrawing_if.tcl.rca $ $Revision: 1.36 $
#
# @libdoc       eServicecommonTrigaDeleteDrawing_if.tcl
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
    eval [utLoad eServicecommonShadowAgent.tcl]

    # Get env variables
    set sOid [ mql get env OBJECTID ]

    # Get actual names from symbolic names
    set sRelationship [eServiceGetCurrentSchemaName \
                       attribute \
                       eServiceSchemaVariableMapping.tcl \
                       "relationship_PartSpecification" \
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
        set sCmd {mql expand bus $sOid to relationship $sRelationship select rel id dump |}
        set mqlret [ catch {eval  $sCmd} outStr ]
        if {$mqlret != 0} {
            mql notice "$outStr"
            return $mqlret
        }
        set lInitialList [ split "$outStr" "\n" ]

        # Should connect all objects within lInitialList list to prev revision.
        pushShadowAgent
        foreach ele $lInitialList {

            set temp [ split $ele "|" ]
            set sRelId [ lindex $temp 6 ]

            set sCmd {mql modify connection $sRelId to $sPreviousDrawingID }
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

