###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonRequiredFormat.tcl.rca $ $Revision: 1.44 $
#
# @libdoc       eServicecommonRequiredFormat
#
# @Library:     Interface for required format checks for triggers
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
# @procdoc      eServicecommonRequiredFormat.tcl
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
#                 {{ eServicecommonRequiredFormat  {to/from/both} {$relationship_rel1,$relationship_rel2,..} {$type_type1,$type_type2,...} }}
#
# @procdoc
#******************************************************************************

proc eServicecommonRequiredFormat { sType sName sRev sSearchKey lFormatList bSizeCheck } {

  #
  # Debugging trace - note entry
  #
  set progname      "eServicecommonRequiredFormat"

  #
  # Error handling variables
  #
  set mqlret 0
  set outStr ""
  set iFound 0

  if { $sSearchKey == "default" } {
      #
      # Get default format for selected object
      #
      set sCmd "mql print bus '$sType' '$sName' '$sRev' select default dump"
      set mqlret [ catch {eval $sCmd} outStr]
      if { $mqlret != 0 } {
          return -code $mqlret $outStr
      }
      set lFormatList {}
      lappend lFormatList $outStr
  } else {
      #
      # Get list of formats supported by selected object
      #
      set sCmd "mql print bus '$sType' '$sName' '$sRev' select policy dump"
      set mqlret [ catch {eval $sCmd} outStr]
      if { $mqlret != 0 } {
          return -code $mqlret $outStr
      }
      set sCmd "mql print policy '$outStr' select format dump"
      set mqlret [ catch {eval $sCmd} outStr]
      if { $mqlret != 0 } {
          return -code $mqlret $outStr
      }
      if { $sSearchKey == "all" } {
          set lFormatList [split $outStr ","]
      } else {
          set lSupportedFormats [split $outStr ","]
      }
  }

  set iFound 0

  foreach sFormat $lFormatList {
      if { $sSearchKey == "list" && [lsearch $lSupportedFormats $sFormat] == -1} {
          continue;
      }

      set sCmd "mql print bus '$sType' '$sName' '$sRev' select format\\\[$sFormat\\\].hasfile format\\\[$sFormat\\\].file.size dump |"
      set mqlret [ catch {eval $sCmd} outStr]
      if { $mqlret != 0 } {
          return -code $mqlret $outStr
      }

      #
      # Check for any file checked in
      #
      set lTemp [split $outStr "|"]
      if {[lindex $lTemp 0] == "TRUE" && $bSizeCheck == "false" } {
          set iFound 1
          break
      } elseif {[lindex $lTemp 0] == "FALSE"} {
          continue
      }

      #
      # Check for checked in file size
      #
      if {$bSizeCheck == "true"} {
          for {set i 1} {$i <= [llength $lTemp]} {incr i} {
              if {[lindex $lTemp $i] > 0} {
                  set iFound 1
                  break
                  break
              }
          }
      }
  }

  set outStr ""
  if { $iFound == 0 } {
      set mqlret 1


      if { $sSearchKey == "default" } {
          set sMsg [mql execute program emxMailUtil -method getMessage \
                        "emxFramework.ProgramObject.eServicecommonRequiredFormat.Default" 3 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                        "" \
                        ]
          mql notice "$sMsg"
      } elseif { $sSearchKey == "all" } {
          set sMsg [mql execute program emxMailUtil -method getMessage \
                        "emxFramework.ProgramObject.eServicecommonRequiredFormat.Any" 3 \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                        "" \
                        ]
          mql notice "$sMsg"
      } else {
          set sTemp [join $lFormatList ","]
          set sMsg [mql execute program emxMailUtil -method getMessage \
                        "emxFramework.ProgramObject.eServicecommonRequiredFormat.Specific" 4 \
                                  "Format" "$sTemp" \
                                  "Type" "$sType" \
                                  "Name" "$sName" \
                                  "Rev" "$sRev" \
                        "" \
                        ]
          mql notice "$sMsg"
      }
  }

  return -code $mqlret $outStr
}
# end eServicecommonRequiredFormat


# End of Module

