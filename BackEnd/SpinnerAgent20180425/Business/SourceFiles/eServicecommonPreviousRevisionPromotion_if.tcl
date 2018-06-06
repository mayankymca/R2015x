###############################################################################
# $RCSfile: eServicecommonPreviousRevisionPromotion_if.tcl.rca $ $Revision: 1.51 $
#
# @libdoc       eServicePreviousRevisionPromotion_if.tcl
#
# @Library:     Interface for Object Revision check trigger.
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
# @procdoc      eServicePreviousRevisionPromotion_if
#
# @Brief:
#
# @Description: check all earlied revisions of same object are at least at the
#                      specified state.
#
# @Parameters:  PSO -- a list of vertical-bar-delimited triples of the
#                      following form: "policy|state|operator"
#                      where operator is one of NE,LE,GE,EQ
#
# @Returns:     0 for success, 1 otherwise
#
# @Usage:       configure in policy as a check trigger on promote event.
#
# @Example:     configure mxTrigMgr thusly:
#
# @procdoc
#******************************************************************************
tcl;
eval {

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

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utload

###############################################################################

mql verbose off;

# Load MQL/Tcl utility procedures
eval  [utLoad eServicecommonPreviousRevisionPromotion.tcl]
eval  [utLoad eServiceSchemaVariableMapping.tcl]
eval  [utLoad eServicecommonShadowAgent.tcl]
eval  [utLoad eServicecommonDEBUG.tcl]
eval  [utLoad eServicecommonTranslation.tcl]

  # Set the standard Matrix environment variables.
  set sType      [ mql get env TYPE ]
  set sName      [ mql get env NAME ]
  set sRev       [ mql get env REVISION ]

  # get the input arguments
  set sState       [string trim [mql get env 1]]

  # set the program related variables
  set mqlret     0
  set outstr     ""
  set progname   "eServicecommonPreviousRevisionPromotion_if.tcl"

  # check for previous revision of current object if nothing exists, then exit
  if {$mqlret == 0} {

       set sCmd {mql print bus "$sType"  "$sName"  "$sRev"  select previous dump}
       set mqlret [catch {eval $sCmd} outstr]
       if {$mqlret == 0} {
             if {$outstr == "" } {
                  set mqlret 1
             } else {
                  set sPreRev $outstr
             }
        }


  }

  # get the policy on the previous revision of current object, if no policy
  # defined raise an error
  if {$mqlret == 0} {

       set sCmd {mql print bus "$sType" "$sName" "$sPreRev" select policy dump}
       set mqlret [catch {eval $sCmd} outstr]
       if {$mqlret == 0} {
            if {$outstr == "" } {
                set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicePreviousRevisionPromotion_if_NoPolicy" 4 \
                                        "Program" "$progname" \
                                        "Type" "$sType" \
                                        "Name" "$sName" \
                                        "Rev" "$sPreRev" \
                                        "" \
                                        ]
                set mqlret 1
            } else {
                set sPolicy $outstr
            }
       }
  }

  # if the state not defined then set it to default, else get the absolute name
  # from symbolic name
  if {$mqlret == 0} {
       if {[string compare $sState ""] == 0} {
            set sTempState "next"

       } elseif {[string match state_* $sState] == 1} {

            set sTempState [string trim [eServiceLookupStateName $sPolicy  $sState]]
            if {[string compare "$sTempState" ""] == 0 } {
                 set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicePreviousRevisionPromotion_if_InvalidState" 2 \
                                        "State" "$sState" \
                                        "Policy" "$sPolicy" \
                                        "" \
                                        ]
                 set mqlret 1
            }

       } elseif {([string compare [string tolower $sState] "next"] == 0) || ([string compare [string tolower $sState] "last"] == 0)} {
            set sTempState [string tolower $sState]
       } else {
            set outstr [mql execute program emxMailUtil -method getMessage \
                                        "emxFramework.ProgramObject.eServicePreviousRevisionPromotion_if_InvalidInput" 1 \
                                        "State" "$sState" \
                                        "" \
                                        ]
            set mqlret 1
       }
  }

  # call eServicePreviousRevisionPromotion
  if {$mqlret == 0} {
       set mqlret [eServicecommonPreviousRevisionPromotion  "$sType" "$sName" "$sRev" $sTempState]
  }

  if {($mqlret != 0) && ($outstr != "")} {
       mql notice $outstr
  }

  # the value returned to eServiceTriggerManager would not affect the outcome of
  # execution in any way
  return $mqlret

}
# end eServicePreviousRevisionPromotion_if

