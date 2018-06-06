###############################################################################
# $RCSfile: eServicecommonTrigcRelAttribute.tcl.rca $ $Revision: 1.43 $
#
# @libdoc       eServicecommonTrigcRelAttribute.tcl
#
# @Library:     Logic for Validate Attribute on Relation
#
# @Brief:
#
# @Description: Compares an attribute value on a relation to a default value
#               or specified value.  Also, checks for unique values.
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
# @procdoc      eServicecommonTrigcRelAttribute
#
# @Brief:
#
# @Description: Compares an attribute value on a relation to a default value
#               or specified value.  Also, checks for unique values.
#
# @Parameters:  sType sName sRev -- TNR of current object
#               sRelName         -- The relationship to traverse
#               sDirection       -- Direction to traverse, must be to or from.
#                                   from is the default.
#               sAttrName        -- Name of the attribute to check on rel
#               iAttrFlag        -- Attribute flag
#                                   0 = Don't check attribute value (Default)
#                                   1 = Check attribute value against default
#                                       value for attribute.
#                                   2 = Check attribute value against value
#                                       passed in to $sAttrVal
#               sAttrVal         -- Value to check against if $iAttrFlag is 2
#               iUniqueFlag      -- Unique flag
#                                   0 = Don't check that values are unique (Default)
#                                   1 = Check that values are unique
#
# @Returns:     return 0 if connection's attribute value(s) do not satisfy
#                         attribute value check(s).
#                      1 otherwise
#
#******************************************************************************
proc  eServicecommonTrigcRelAttribute {sType sName sRev sRelName sDirection sAttrName iAttrFlag sAttrVal iUniqueFlag} {

    eval  [utLoad eServicecommonTranslation.tcl]

    set mqlret 0

    # Set flag defaults
    if {$iAttrFlag != 1 && $iAttrFlag != 2 } {
        set iAttrFlag 0
    }

    if {$iUniqueFlag != 1} {
        set iUniqueFlag 0
    }

    # Check for valid direction value
    if {$sDirection != "to"} {
        set sDirection "from"
    }

    # Get the default value if iAttrFlag is 1
    if {$iAttrFlag == 1} {
        set sCommand {mql print attribute "$sAttrName" select default dump}
        if {[catch {eval $sCommand} sDefaultVal] == 0} {
            set sAttrVal $sDefaultVal
            set mqlret 0
        } else {
            set mqlret 1
            set outstr "$sDefaultVal"
            mql notice "$outstr"
        }
    }

    if {$mqlret == 0} {
        # Get attribute values from rel
        set sCommand {mql expand bus "$sType" "$sName" "$sRev" rel "$sRelName" $sDirection select rel attribute\[$sAttrName\] dump |}

        if {[catch {eval $sCommand} sConnectObjs] == 0} {
           set lConnectObjs [split $sConnectObjs \n]
           set lAttrVals {}
           foreach sConnectElement $lConnectObjs {
               set lConnectElement [split $sConnectElement |]
               set sObjType [lindex $lConnectElement 3]
               set sObjName [lindex $lConnectElement 4]
               set sObjRev [lindex $lConnectElement 5]
               set sObjAttrVal [lindex $lConnectElement 6]

               # Check attribute values if flag is set
               if {"$sObjAttrVal" == "$sAttrVal" && $iAttrFlag != 0} {
                   set mqlret 1

                   if {$iAttrFlag == 1} {
                       set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicecommonTrigcRelAttribute.DefaultInvalid" 6 \
                                        "Type" "$sObjType" \
                                        "Name" "$sObjName" \
                                        "Rev" "$sObjRev" \
                                        "Attribute" "$sAttrName" \
                                        "Rel" "$sRelName" \
                                        "Value" "$sAttrVal" \
                                        "" \
                                        ]
                   } else {
                       set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicecommonTrigcRelAttribute.InvalidValue" 6 \
                                        "Type" "$sObjType" \
                                        "Name" "$sObjName" \
                                        "Rev" "$sObjRev" \
                                        "Attribute" "$sAttrName" \
                                        "Rel" "$sRelName" \
                                        "Value" "$sAttrVal" \
                                        "" \
                                        ]
                   }
                   mql notice "$outstr"
                   break
               }

               # Check if values are unique if flag is set
               if {$iUniqueFlag == 1} {
                   if {[lsearch $lAttrVals "$sObjAttrVal"] == -1} {
                       lappend lAttrVals "$sObjAttrVal"
                   } else {
                       set mqlret 1
                       set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicecommonTrigcRelAttribute.NotUnique" 3 \
                                        "Attribute" "$sAttrName" \
                                        "Rel" "$sRelName" \
                                        "OriAtt" "$sObjAttrVal" \
                                        "" \
                                        ]
                       mql notice "$outstr"
                       break
                   }
               }
           }
        } else {
            set mqlret 1
            set outstr "$sConnectObjs"
            mql notice "$outstr"
        }
    }

    return $mqlret
}
# end  eServicecommonTrigcRelAttribute

