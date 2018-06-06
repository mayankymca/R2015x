###############################################################################
# $RCSfile: eServicecommonTrigcAttribute.tcl.rca $ $Revision: 1.44 $
#
# @libdoc       eServicecommonTrigcAttribute.tcl
#
# @Library:     Logic to support attribute checks for triggers
#
# @Brief:       Compare specified attribute to its default value
#
# @Description: see procedure description
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

    set sOutput [ mql print program '$sProgram' select code dump ]

    return $sOutput
}
# end utload

#******************************************************************************
# @procdoc      eServicecommonTrigcAttribute
#
# @Brief:       Logic to support attribute checks for triggers
#
# @Description: compares an attribute value to a default value
#
# @Parameters:  sType  sName sRev -- name of object
#               attName -- attribute name
#               attValue -- attribute value
#
# @Returns:     0 if attribute differs from default
#               1 otherwise
#
# @Usage:       Supports check attribute trigger
#
# @Example:     none
#
# @procdoc
#******************************************************************************

proc  eServicecommonTrigcAttribute {  sType sName sRev attName attVal } {

    eval  [utLoad eServicecommonTranslation.tcl]

    # call getAttributeValue
    set szCommand {mql print bus "$sType" "$sName" "$sRev" select attribute\[$attName\] dump}
    if {[catch {eval $szCommand} objAttVal] == 0} {
        if {[string compare "$attVal" "$objAttVal"] == 0} {
             set sI18NTypeKey "emxFramework.Type.[join [split $sType] "_"]"
             set sI18NType [mql execute program emxMailUtil -method getMessage \
                                "$sI18NTypeKey" 0 \
                                "" \
                                ]
             set sI18NAttKey "emxFramework.Attribute.[join [split $attName] "_"]"
             set sI18NAtt [mql execute program emxMailUtil -method getMessage \
                                "$sI18NAttKey" 0 \
                                "" \
                                ]
             set sI18NAttDefKey "emxFramework.Default.[join [split $attName] "_"]"
             set sI18NAttDef [mql execute program emxMailUtil -method getMessage \
                                "$sI18NAttDefKey" 0 \
                                "" \
                                ]

             set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcAttribute.CheckFailed" 5 \
                                  "AttName" "$sI18NAtt" \
                                  "Type" "$sI18NType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                                  "Value" "$sI18NAttDef" \
                                "" \
                                ]
             mql notice "$sMsg"
             return 1
        } else {
             return 0
        }
    } else {
        set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonTrigcAttribute.UnableToGet" 2 \
                                  "AttName" "$attName" \
                                  "Name" "$sName" \
                                "" \
                                ]
        mql notice "$sMsg"
        return 1
    }
}
# end  eServicecommonTrigcAttribute

