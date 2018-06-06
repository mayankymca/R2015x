###############################################################################
# $RCSfile: eServicecommonRelativeFloatAction.tcl.rca $ $Revision: 1.50 $
#
# @libdoc       eServicecommonRelativeFloatAction.tcl
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
# @procdoc      eServicecommonRelativeFloatAction
#
# @Brief:       Get the latest revision that is at a specified state
#
# @Description:
#               The program eServicecommonRelativeFloatAction_if.tcl is a promote action trigger.
#               When an object is promoted, the program gets the previous revision of the object and
#               floats all the specified relationships in the specified direction.
#
# @Parameters:
#               sType          - The TYPE name whose revisions are to be checked for.
#               sName          - Name of the object whose revisions are to be checked for
#               sRevision      - The revision of the object whose revisions are to be looked for.
#               sRel           - Relationship to expand on
#               sDirection     - Direction to expand rel (to or from)
#               sOperation     - Determines which objects to check relationships on
#                                (PREVIOUS or LATEST).
#               sState         - The state to look for.
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
#
# @procdoc
#******************************************************************************

proc eServicecommonRelativeFloatAction  {sType sName sRevision sRel sDirection sOperation sState} {
  set progname "eServicecommonRelativeFloatAction.tcl"

  set mqlret 0
  set sPrevRevision ""

  # sOperation is set to PREVIOUS then get the previous revision,if available
  # else return to calling program,if sOperation is LATEST then get the latest
  # revision by calling eServicecommonLatestRevAtState proc
  if {"$sOperation" == "PREVIOUS"} {
        # modified for bug 320466r0
        set sCmd {mql print bus "$sType" "$sName" "$sRevision" select previous previous.type dump |}
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret == 0} {
             if {$outstr == "" } {
                  return $mqlret
             } else {
           # modified for bug 320466r0
                 set lData [split $outstr |]
                 set sPrevRevision [lindex $lData 0]
                 set sType [lindex $lData 1]
                 #set sPrevRevision $outstr
             }
        } else {
            # Give error message if an error
            mql notice "$progname :\n$outstr"
        }
  } elseif {"$sOperation" == "LATEST"} {
        set sPrevRevision [eServicecommonLatestRevAtState $sType $sName $sRevision $sState]
        if {$sPrevRevision == -1} {
             set mqlret 1
        }
  }

  # expand the object based on the revision obtained, from input relationship
  # and direction sent in as input arguments.  If all's well modify connection
  if {($mqlret == 0) && ($sPrevRevision != "")} {
       if {[string trim $sDirection] != "both"} {
            set sCmd {mql expand bus "$sType" "$sName" "$sPrevRevision" \
                                     "$sDirection" relationship "$sRel" \
                                     select bus \
                                     policy \
                                     select relationship \
                                     id \
                                     dump |}
       } else {
            set sCmd {mql expand bus "$sType" "$sName" "$sPrevRevision" \
                                      relationship  "$sRel" \
                                      select bus \
                                      policy \
                                      select relationship \
                                      id \
                                      dump |}
       }

       set mqlret [catch {eval $sCmd} outstr]

       if {$mqlret == 0} {
            set lConnectedObjects [split $outstr \n]
            set lObjects2Move {}

            foreach sObjectData $lConnectedObjects {
                set lObjectData [split $sObjectData |]
                set sParentType [lindex $lObjectData 3]
                set sParentName [lindex $lObjectData 4]
                set sParentRev [lindex $lObjectData 5]
                set sParentPol [lindex $lObjectData 6]
                set sParentRelId [lindex $lObjectData 7]

                # Check Classification
                set sCmd {mql print policy "$sParentPol" select property\[PolicyClassification\].value dump |}
                set mqlret [catch {eval $sCmd} outstr]
                if {$mqlret != 0} {
                    mql notice "$outstr"
                    return 1
                }
                if {$outstr != "Production"} {
                    continue
                }

                # Check if revision is the last revision in specified state
                if {[lsearch [array names aLastObjectRevs] "$sParentType $sParentName"] == -1} {
                    # Get last revision of object that is in the specified state
                    set sParentLastRev [eServicecommonLastRevInState $sParentType $sParentName $sState 0 $sParentPol EQ]

                    if {"$sParentLastRev" == "Error"} {
                        # Error out if an error
                        set mqlret 1
                        break
                    } elseif {"$sParentLastRev" != ""} {
                        # Add object to array, won't have to check other revisions of this object
                        set aLastObjectRevs([list $sParentType $sParentName]) $sParentLastRev
                    }
                } else {
                    # Retrieve last revision in specified state for this Type and Name
                    set sParentLastRev $aLastObjectRevs($sParentType $sParentName)
                }

                if {"$sParentRev" == "$sParentLastRev"} {
                    # If last revision in specified state add to list of rels to move
                    lappend lObjects2Move $sParentRelId
                } else {
                    # Check if revision is in prior state than target state
                    # Procedure eServicecommonCheckStatePosition is in eServicecommonCheckStatePosition.tcl program
                    set iNotRel [eServicecommonCheckStatePosition $sParentType $sParentName $sParentRev $sState LT]

                    if {$iNotRel == 0} {
                        # If revision is in a prior state add to list of rels to move
                        lappend lObjects2Move $sParentRelId
                    } elseif {$iNotRel == 1} {
                        # Error out if an error
                        set mqlret 1
                        break
                    }
                }
            }

            if {$mqlret == 0} {
                pushShadowAgent
                foreach sObjId $lObjects2Move {
                    set sCmd {mql modify connection $sObjId "$sDirection" "$sType" "$sName" "$sRevision"}
                    set mqlret [catch {eval $sCmd} outstr]

                    if {$mqlret != 0} {
                        # If an error, give message and break out of loop
                        mql notice "$progname :\n$outstr"
                        break
                    }
                }
                popShadowAgent
            }
       } else {
           # Give an error message if an error
           mql notice "$progname :\n$outstr"
       }
  }

  return $mqlret

}
# end eServicecommonRelativeFloatAction

