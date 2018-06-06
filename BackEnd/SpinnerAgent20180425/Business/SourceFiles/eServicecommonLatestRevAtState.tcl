###############################################################################
# $RCSfile: eServicecommonLatestRevAtState.tcl.rca $ $Revision: 1.20 $
#
# @libdoc       eServicecommonLatestRevAtState.tcl
#
#
# @Brief:       Check this objects children prior to promotion of parent.
#
# @Description: This program gets the latest revision for any given object
#               that is at an indicated state
#
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
# @procdoc      eServicecommonLatestRevAtState
#
# @Brief:       Get the latest revision that is at a specified state
#
# @Description: The procedure obtains the latest revision of an object, that is
#                          in a specified state. The type, name and revision of
#                          the object whose revision is to be found out are passed
#                          in as the input parameters.
#
#                          The intent of this program is to provide a procedure
#                          which takes a state and gives the latest revision of
#                          that is in that state.
#
# @Parameters:
#               sType          - The TYPE name whose revisions are to be checked for.
#               sName          - Name of the object whose revisions are to be checked for
#               sRev           - The revision of the object whose revisions are to be looked for.
#               sState         - The state to look for.
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
#
# @procdoc
#******************************************************************************

proc eServicecommonLatestRevAtState  { sType sName sRevision sState} {

  # Debugging trace - note entry
  set progname "eServicecommonLatestRevAtState.tcl"
  mxDEBUGIN "$progname"

  set retval          -1
  set mqlret          0
  set sLatestRev      ""
  set sPrevRevision   ""
  set sRelRev         [string trim "$sRevision"]

  global outstr

  while {1} {

        # get the previous revision of the object
        if {$mqlret == 0} {
             set sCmd   {mql print bus "$sType" "$sName" "$sRelRev" select previous dump}
             set mqlret [catch {eval $sCmd} outstr]
        }

        if {$mqlret != 0} {
             break
        } elseif {[string trim $outstr] == "" } {
             break
        }

        if {$mqlret == 0} {
             set sPrevRevision $outstr
             set sRelRev $sPrevRevision

             # get the policy based on previous revision
             set sCmd {mql print bus "$sType" "$sName" "$sPrevRevision" select policy dump}
             set mqlret [catch {eval $sCmd} outstr]

             if {$mqlret == 0} {
                  set sPrevPolicy $outstr

                  # get the states on the policy
                  set sCmd {mql print policy $sPrevPolicy select state dump}
                  set mqlret [catch {eval $sCmd} outstr]

                  if {$mqlret == 0} {
                       set stateRow $outstr

                       # convert the result of the sCmd into a list by splitting on ,
                       # get its position in the list of states
                       set stateRow [split $stateRow ,]
                       set iIndx 0
                       set indexTargetState ""
                       foreach lstateRowElement $stateRow {
                               set lstateRowElement [string trim "$lstateRowElement"]
                               if {[string compare "$lstateRowElement" "$sState"] == 0} {
                                    set indexTargetState $iIndx
                                    break
                               }
                               incr iIndx
                       }

                       # get the current state of the object and its position
                       # in the list of states
                       set sCmd {mql print bus "$sType" "$sName" "$sPrevRevision" select current dump}
                       set mqlret [catch {eval $sCmd} outstr]

                       if {$mqlret == 0} {
                            set sCurrentState $outstr
                            set iIndx 0
                            set indexCurrentState ""
                            foreach lstateRowElement $stateRow {
                                    set lstateRowElement [string trim "$lstateRowElement"]
                                    if {[string compare "$lstateRowElement" "$sCurrentState"] == 0} {
                                         set indexCurrentState $iIndx
                                         break
                                    }
                                    incr iIndx
                            }
                       }

                       if {($indexTargetState != "") && ($indexCurrentState != "")} {
                             if {$indexCurrentState >= $indexTargetState} {
                                  set sLatestRev "$sPrevRevision"
                                  break
                             }
                       } else {
                             set outstr "Error : $progname - specified state undefined in $sPrevPolicy policy"
                             set mqlret 1
                       }
                  }
             }
        }
  }

  # Debugging trace - note exit
  mxDEBUGOUT "$progname $mqlret"

  if {$mqlret == 0} {
     return $sLatestRev
  } else {
     return $retval
  }

}
# end eServicecommonLatestRevAtState

