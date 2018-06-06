############################################################################
# $RCSfile: eServicecommonDrawingPartLib.tcl.rca $ $Revision: 1.47 $
#
# @libdoc       eServicecommonDrawingPartLib.tcl
#
# @Library:     Common functions used by Drawing and Part revision
#               and release trigger programs
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

###############################################################################
# Function Name: ParseMultiRevObj
# Input        : lObjsInfo
#                - It is a list of object information.
#                - Example:
#                          {
#                            {busId}
#                            {{state1} {state2} {state3}}
#                            {current state}
#                            {type}
#                            {name}
#                            {revision}
#                            {connectionId}
#                            {{rev1} {rev2} {rev3}}
#                          }
# Return       : lRetObjsInfo
#                - Same as input list.
# Description  : This function is used to return a list
#                which will only contain multiple revision objects
#                from input object list
###############################################################################

proc ParseMultiRevObj {lObjsInfo} {

    # Object list to be returned.
    set lRetObjsInfo {}

    set i 0
    # For each object.
    foreach lObjInfo1 $lObjsInfo {

        # Get Type
        set sType1 [lindex $lObjInfo1 3]
        # Get Name
        set sName1 [lindex $lObjInfo1 4]

        set j 0
        # Search for any revision in the list.
        foreach lObjInfo2 $lObjsInfo {

            # Get Type
            set sType2 [lindex $lObjInfo2 3]
            # Get Name
            set sName2 [lindex $lObjInfo2 4]

            # Compare Type and Name
            if {$i != $j && $sType1 == $sType2 && $sName1 == $sName2} {
                lappend lRetObjsInfo $lObjInfo1
                break
            }

            incr j
        }

        incr i
    }

    return -code 0 $lRetObjsInfo
}

###############################################################################
# Function Name: ParseLaterRevObj
# Input        : lObjsInfo
#                - It is a list of object information.
#                - Example:
#                          {
#                            {busId}
#                            {{state1} {state2} {state3}}
#                            {current state}
#                            {type}
#                            {name}
#                            {revision}
#                            {connectionId}
#                            {{rev1} {rev2} {rev3}}
#                          }
# Return       : lRetObjsInfo
#                - Same as input list.
# Description  : This function is used to return a list
#                which will only contain later revision objects
#                from input object list
###############################################################################

proc ParseLaterRevObj {lObjsInfo} {

    # Object list to be returned.
    set lRetObjsInfo {}

    # For each object.
    foreach lObjInfo1 $lObjsInfo {

        # Get Object Id
        set sObjId [lindex $lObjInfo1 0]
        set sType1 [lindex $lObjInfo1 3]
        set sName1 [lindex $lObjInfo1 4]
        set sRev1 [lindex $lObjInfo1 5]

        # Get all the revisions of an object.
        set lRev [lindex $lObjInfo1 7]

        # Get index of current object revision.
        set nRevIndex [expr [lsearch $lRev $sRev1] + 1]

        set bLaterRevFound 0
        foreach lObjInfo2 $lObjsInfo {

            # Get TNR of object to be compared
            set sType2 [lindex $lObjInfo2 3]
            set sName2 [lindex $lObjInfo2 4]
            set sRev2 [lindex $lObjInfo2 5]

            # If only revision is different
            if {$sType2 == $sType1 && $sName2 == $sName1 && $sRev2 != $sRev1} {
                # See if object to be compared is later revision.
                for {set i $nRevIndex} {$i < [llength $lRev]} {incr i} {
                    if {$sRev2 == [lindex $lRev $i]} {
                        set bLaterRevFound 1
                        break
                    }
                }
            }
        }

        # if object itself is a latest revision then add it to return list.
        if {$bLaterRevFound == 0} {
            lappend lRetObjsInfo $lObjInfo1
        }

    }

    return -code 0 $lRetObjsInfo
}

###############################################################################
# Function Name: ParseObjList
# Input        : lObjsInfo
#                - It is a list of object information.
#                - Example:
#                          {
#                            {busId}
#                            {{state1} {state2} {state3}}
#                            {current state}
#                            {type}
#                            {name}
#                            {revision}
#                            {connectionId}
#                            {{rev1} {rev2} {rev3}}
#                          }
#                lTargetState
#                - states of object to be compared
#                - Example:
#                          {
#                            {Release}
#                            {Complete}
#                          }
#                sOperator
#                - comparision operator
#                - Example:
#                  EQ NE LT LE GT GE
# Return       : lRetObjsInfo
#                - Same as input list.
# Description  : This function is used to return a list
#                which will only contain objects which has
#                current state which satisfies sState and
#                comparision operator equation.
###############################################################################

proc ParseObjList {lObjsInfo lTargetState sOperator} {

    # incude file
    eval  [ utLoad eServicecommonTranslation.tcl]

    # Object list to be returned.
    set lRetObjsInfo {}

    # For each object.
    foreach lObjInfo $lObjsInfo {

        # Get all the states of an object.
        set lStates [lindex $lObjInfo 1]

        # Get current state of an object.
        set sCurrent [lindex $lObjInfo 2]

        # Get position of state to be compared.
        foreach sState $lTargetState {
            set sCompareIndex [lsearch $lStates $sState]
            if {$sCompareIndex >= 0} {
                break
            }
        }

        # State not found
        if {$sCompareIndex == -1} {
            lappend lRetObjsInfo $lObjInfo
            continue
        }

        # Get position of the object.
        set sCurrentIndex [lsearch $lStates $sCurrent]

        # Compare according to operator.
        switch $sOperator {
            EQ {
                if {$sCurrentIndex == $sCompareIndex} {
                    lappend lRetObjsInfo $lObjInfo
                }
            }
            NQ {
                if {$sCurrentIndex != $sCompareIndex} {
                    lappend lRetObjsInfo $lObjInfo
                }
            }
            LT {
                if {$sCurrentIndex < $sCompareIndex} {
                    lappend lRetObjsInfo $lObjInfo
                }
            }
            LE {
                if {$sCurrentIndex <= $sCompareIndex} {
                    lappend lRetObjsInfo $lObjInfo
                }
            }
            GT {
                if {$sCurrentIndex > $sCompareIndex} {
                    lappend lRetObjsInfo $lObjInfo
                }
            }
            GE {
                if {$sCurrentIndex >= $sCompareIndex} {
                    lappend lRetObjsInfo $lObjInfo
                }
            }
            default {
                set outStr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonDrawingPartLib.InvalidOperation" 1 \
                                "Operation" "$sOperator" \
                                "" \
                                ]
                mql notice "$outStr"
                return -code 1 ""
            }
        }

    }

    # return objects which satisfies condition.
    return -code 0 $lRetObjsInfo
}

