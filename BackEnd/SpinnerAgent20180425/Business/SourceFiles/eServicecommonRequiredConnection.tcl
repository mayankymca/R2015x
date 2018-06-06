###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonRequiredConnection.tcl.rca $ $Revision: 1.43 $
#
# @libdoc       eServicecommonRequiredConnection
#
# @Library:     Interface for required connection checks for triggers
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
# Define Global Variables
#
###############################################################################


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [ utLoad eServicecommonDEBUG.tcl ]
eval  [ utLoad eServicecommonTranslation.tcl ]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonRequiredConnection.tcl
#
# @Brief:       Check that at least one object is connected via the
#               indicated relatonship
#
# @Description: Check that at least one object is connected via the
#               indicated relatonship
#
# @Parameters:  direction    -- direction to traverse (to/from)
#               relationList -- relationships to traverse
#                               eg : $relationship_rel1,$relationship_rel2,...
#               typeList     -- types to traverse
#                               eg : $type_type1,$type_type2,..
#
# @Returns:     0 for no connection, 1 otherwise
#
# @Example:     Input to mxTrigManager:
#                 {{ eServicecommonRequiredConnection  {to/from/both} {$relationship_rel1,$relationship_rel2,..} {$type_type1,$type_type2,...} }}
#
# @procdoc
#******************************************************************************

proc eServicecommonRequiredConnection { sType sName sRev direction relationList typeList } {

  #
  # Debugging trace - note entry
  #
  set progname      "eServicecommonRequiredConnection.tcl"

  #
  # Error handling variables
  #
  set iReturn 0
  set outStr ""
  set mqlret 0
  set sCmd ""

  #
  # Expand business object to find required to/from relationship.
  #
  set lRels {}
  append sCmd "mql expand bus \"$sType\" \"$sName\" \"$sRev\""
  if {$direction != ""} {
      append sCmd " $direction"
  }
  if {$relationList != ""} {
      append sCmd " rel \"$relationList\""
  }
  if {$typeList != ""} {
      append sCmd " type \"$typeList\""
  }

  set mqlret [ catch {eval $sCmd} outStr]
  if { $mqlret != 0 } {
    set iReturn $mqlret
    set mqlret 0
  } else {
    set lRels $outStr
    set outStr {}
  }

  switch $direction {
      from    {set sDir "to"}
      to      {set sDir "from"}
      default {set sDir "from\\to"}
  }

  if { $lRels == {} } {
    set iReturn 1
    if { $relationList == "" } {
        if { $typeList == "" } {
            set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonRequiredConnection.AnyObj" 4 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                                  "Direction" "$sDir" \
                                "" \
                                ]
            mql notice "$sMsg"
        } else {
            set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonRequiredConnection.Type" 5 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                                  "Direction" "$sDir" \
                                  "Types" "$typeList" \
                                "" \
                                ]
            mql notice "$sMsg"
        }
    } else {
        if { $typeList == "" } {
            set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonRequiredConnection.Rel" 5 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                                  "Direction" "$sDir" \
                                  "Rels" "$relationList" \
                                "" \
                                ]
            mql notice "$sMsg"
        } else {
            set sMsg [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServicecommonRequiredConnection.TypeRel" 6 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                                  "Direction" "$sDir" \
                                  "Types" "$typeList" \
                                  "Rels" "$relationList" \
                                "" \
                                ]
            mql notice "$sMsg"
        }
    }
  }

  return -code $iReturn $outStr
}
# end eServicecommonRequiredConnection


# End of Module

