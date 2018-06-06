###############################################################################
# $RCSfile: eServicecommonTrigcRecursiveConnection.tcl.rca $ $Revision: 1.43 $
#
# @Library:     Logic for Recursive Connection check
#
# @Description: check that objects with the same Type and Name as the
#               current object are not connected to the current structure via the
#               specified relationship in the specified direction.
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

#******************************************************************************
# @procdoc      eServicecommonTrigcRecursiveConnection
# @Description: check that objects with the same Type and Name as the
#               current object are not connected to the current structure via
#               the specified relationship in the specified direction.
#
# @Parameters:  sType sName sRev -- name of current object
#               sRelationName -- relationship to traverse
#               sDirection    -- direction to traverse
#               iSubTypeFlag  -- check subtype flag
#                                 1 = Check for objects with same type and name.
#                                 0 = Check for objects with same name.
#               iRecursiveFlag -- Recurse to all flag.
#                                 1 = recurse to all levels
#                                 0 = one level
#
# @Returns:     0 for no recursive connection , 1 otherwise
#*******************************************************************************
proc eServicecommonTrigcRecursiveConnection {sType sName sRev sRelationName sDirection iSubTypeFlag iRecursiveFlag} {

    eval  [utLoad eServicecommonTranslation.tcl]

    set iReturnVal 0
    set DUMPDELIMITER "|"

    if {$iRecursiveFlag == 0} {
        set sCommand {mql expand bus "$sType" "$sName" "$sRev" rel "$sRelationName" "$sDirection" type "$sType" dump $DUMPDELIMITER}
    } else {
        set sCommand {mql expand bus "$sType" "$sName" "$sRev" rel "$sRelationName" "$sDirection" recurse to all dump $DUMPDELIMITER}
    }

    if {[catch {eval $sCommand} lObjs] != 0} {
        set iReturnVal 1
        set lObjs ""
    }

    if {$lObjs != ""} {
        set lObjs [split $lObjs \n]
        foreach sObjData $lObjs {
                set lObjData [split $sObjData $DUMPDELIMITER]
                if {[string compare [lindex $lObjData 4] "$sName"] == 0} {
                    if {$iSubTypeFlag == 0} {
                        if {[string compare [lindex $lObjData 3] "$sType"] == 0} {
                            set iReturnVal 1
                            set sObjType $sType
                            set sObjRev [lindex $lObjData 5]
                            break
                        }
                    } else {
                           set iReturnVal 1
                           set sObjType [lindex $lObjData 3]
                           set sObjRev [lindex $lObjData 5]
                           break
                    }
                }
        }
    }

    if {($iReturnVal == 1) && ($lObjs != "")} {
        set sMsg [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigcRecursiveConnection.RecursiveCheck" 3 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                    "" \
                    ]
        mql notice $sMsg
    }

    return $iReturnVal
}
# end eServicecommonTrigcRecursiveConnection

