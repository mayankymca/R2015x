###############################################################################
# $RCSfile: eServicecommonPreviousRevisionPromotion.tcl.rca $ $Revision: 1.22 $
#
# @libdoc       eServicePreviousRevisionPromotion.tcl
#
# @Library:     Logic for Object Revision action trigger.
#
# @Brief:
#
# @Description: Check that all earlied revisions of same object are at least
#               at the specified state.
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
# @procdoc      eServicePreviousRevisionPromotion
#
# @Brief:
#
# @Description: check all earlied revisions of same object are at least at the
#                      specified state.
#
# @Parameters:  sType  sName sRev -- name of trigged object
#               PSO -- a list of vertical-bar-delimited triples of the
#                      following form: "policy|state|operator"
#                      where operator is one of NE,LE,GE,EQ
#
# @Returns:     0 for success, 1 otherwise
#
# @Usage:       supports trigger check
#
# @Example:     see eServicePreviousRevisionPromotion_if
#
# @procdoc
#************************************************************************

proc eServicecommonPreviousRevisionPromotion { sType sName sRev sState } {

    set  mqlret 0
    global outstr

    # get the previous revision of the current object
    if {$mqlret == 0} {
         set sCmd {mql print bus "$sType"  "$sName"  "$sRev"  select previous dump}
         set mqlret [catch {eval $sCmd} outstr]

         if {$mqlret == 0} {
              set sPreRev $outstr
         }
    }

    # get the states defined on the policy attached and set the state if input
    # state refers to next or last
    if {$mqlret == 0} {

          set sCmd {mql print bus "$sType" "$sName" "$sPreRev" select state dump}
          set mqlret [catch {eval $sCmd} outstr]

          if {$mqlret == 0} {

               set stateRow [split [string trim $outstr] ,]
               switch $sState {

                       next {

                            set sCmd {mql print bus "$sType" "$sName" "$sPreRev" select current dump}
                            set mqlret [catch {eval $sCmd} outstr]

                            if {$mqlret  == 0 } {

                                 set sState $outstr
                                 set index [ lsearch -exact $stateRow $sState]
                                 set sLength [llength $stateRow]

                                 #if the current state is the last state
                                 if {$index == [expr $sLength - 1]} {
                                      set sState ""
                                 } else {
                                      set sState  [lindex $stateRow [expr $index + 1] ]
                                 }
                            }

                       }

                       last {
                            set sLength [llength $stateRow]
                            set sState  [lindex $stateRow [expr $sLength - 1] ]
                       }

                       default {
                            set index  [lsearch -exact $stateRow $sState]
                            if {$index == -1 } {
                                 set outstr "The passed in state is not valid for the specified policy"
                                 set mqlret 1
                            }
                       }

               }
          }
    }

    # promote the objects to the specified state, recursively
    if {$mqlret == 0} {

        set indx 1
        pushShadowAgent
        while {$indx > 0 } {

               if {$sState != ""} {

                   set sCmd {mql temporary query bus "$sType" "$sName" "$sPreRev" select current dump |}
                   set mqlret [catch {eval $sCmd} outstr]

                   if {$mqlret != 0} {
                      set indx 0
                   } elseif {$outstr == ""} {
                      set indx 0
                   } else {
                      set sCurState [string trim [lindex [split $outstr |] 3] ]
                      if {$sCurState == "$sState"} {
                           set indx 0
                           break
                      }
                      set sCmd {mql promote bus "$sType" "$sName" "$sPreRev"}
                      set mqlret [catch {eval $sCmd} outstr]
                      if {$mqlret != 0} {
                           set indx 0
                      }
                   }
                } else {
                   set indx 0
                }
        }
        popShadowAgent
    }

    return $mqlret

}
# end eServicecommonPreviousRevisionPromotion

