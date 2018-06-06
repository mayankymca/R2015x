###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonReplaceChildGroups.tcl.rca $ $Revision: 1.19 $
#
# @libdoc       eServicecommonReplaceChildGroups.tcl
#
# @Brief:       Adds child groups to parent group
#
# @Description:
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

  # Load the utilities and other libraries.
  eval  [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval  [ utLoad eServicecommonShadowAgent.tcl ]

  # Get input arguments
  set sParentGroup [string trim [mql get env 1]]
  set lChildGroups [string trim [mql get env 2]]

  # Form child pattern from child list
  set lGroupPattern {}
  foreach sGroup $lChildGroups {
      lappend lGroupPattern "'$sGroup'"
  }
  set sGroupPattern [join $lGroupPattern ,]

  pushShadowAgent

  #
  # start transaction
  #
  set sCmd {utCheckStartTransaction}
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return "1|$outStr"
  }
  set bTranAlreadyStarted "$outStr"

  set sCmd "mql modify group '$sParentGroup' remove child all"
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      utCheckAbortTransaction $bTranAlreadyStarted
      popShadowAgent
      return "1|$outStr"
  }

  #If no child groups specified then add them.
  if {"$sGroupPattern" != ""} {
      set sCmd "mql modify group '$sParentGroup' child $sGroupPattern"
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          mql notice "$outStr"
          utCheckAbortTransaction $bTranAlreadyStarted
          popShadowAgent
          return "1|$outStr"
      }
  }

  #
  # commit transaction
  #
  set sCmd {utCheckCommitTransaction $bTranAlreadyStarted}
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return "1|$outStr"
  }

  popShadowAgent
  return "0"
}
# end eServicecommonReplaceChildGroups.tcl

# End of Module

