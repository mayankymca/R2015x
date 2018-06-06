###############################################################################
# $RCSfile: eServiceValidRevisionChange_if.tcl.rca $ $Revision: 1.49 $
#
# @libdoc       eServiceValidRevisionChange_if.tcl
#
# @Library:     Interface for Previous Existence check trigger
#
# @Brief:
#
# @Description: Check that at other revisions of this object do not exist.
#               If one does, fail with a message telling the user to use
#               "OBJECT | NEW | REVISION" instead of "OBJECT | NEW | ORIGINAL.
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
# @procdoc      eServiceValidRevisionChange_if
#
# @Brief:       Check for existence of '$TYPE' '$NAME' '*' ]
#
# @Description: Check that at other revisions of this object do not exist.
#               If one does, fail with a message telling the user to use
#               "OBJECT | NEW | REVISION" instead of "OBJECT | NEW | ORIGINAL.
#
# @Parameters:  None
#
# @Returns:     0   - Objects of '$TYPE' '$NAME' '*' do not exist
#               1   - Objects of '$TYPE' '$NAME' '*' exist
#
# @Example:     Input to eServiceTriggerManager:
#                {{ eServiceValidRevisionChange_if  {} }}
#
# @procdoc
#
###############################################################################
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

    set sOutput [ mql print program '$sProgram' select code dump ]

    return $sOutput
}
# end utload

  mql verbose off;
  eval  [utLoad eServiceSchemaVariableMapping.tcl]

  set progname "eServiceValidRevisionChange_if"

  set mqlret 0
  set outstr ""
  set mepstr ""

  set sType     [ mql get env TYPE ]
  set sName     [ mql get env NAME ]
  set sRev      [ mql get env REVISION ]
  set sPolicy   [mql get env POLICY]
  set bExpand   [ string toupper [ mql get env 1 ] ]
  set bRevision [mql execute program emxMailUtil -method getMessage \
                                "emxEngineeringCentral.MEP.allowCustomRevisions" 0 \
                                "" \
                                "emxEngineeringCentral" \
                           ]

  switch $bExpand {
      "EXPAND_FROM_SPECIFIC_LEVEL" {
          set sSpecificType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl [mql get env 2]]
          set sCmd {mql temp query bus "$sSpecificType" \"$sName\" "*" dump |}
      }
      "DO_NOT_EXPAND" {
          set sCmd {mql temp query !expand bus "$sType" \"$sName\" "*" dump |}
      }
      "EXPAND_FROM_SAME_LEVEL" {
          set sCmd {mql temp query bus "$sType" \"$sName\" "*" dump |}
      }
      default {
          set sCmd {mql temp query bus "$sType" \"$sName\" "*" dump |}
	  }
	}


	if {$sPolicy != "NULL" } {
		set sCommd {mql print policy "$sPolicy" select property\[PolicyClassification\].value dump |}
		set mqlret [catch {eval $sCommd} mepstr]
        if {$mqlret != 0} {
			return ""
		}
		if {"$mepstr" == "Equivalent" } {
			set sCmd {mql temp query bus "$sType" \"$sName\" "$sRev" dump |}
		} 
	}


  set mqlret [catch {eval $sCmd} outstr]


  # if outstr is populated with value,it indicates there are objects of same type
  # of different revisions are existing
  if {$mqlret == 0} {
     if {[string trim $outstr] != ""} {
       if {"$mepstr" == "Equivalent"} {
         set outstr [mql execute program emxMailUtil -method getMessage \
                              "emxEngineeringCentral.MEP.TNRExists" 0 \
                              "" \
                              "emxEngineeringCentralStringResource" \
                              ]

       } else {
            set outstr [mql execute program emxMailUtil -method getMessage \
                                "emxFramework.ProgramObject.eServiceValidRevisionChange_if.NoCreate" 3 \
                                  "Type" "$sType" \
                                  "Name" \"$sName\" \
                                  "Rev" "$sRev" \
                                "" \
                                ]
         }
        set mqlret 1
     }
  }
  
  # display the error and block the event
  if {$mqlret != 0} {
       mql warning "$outstr"
  }

  exit $mqlret
}
# end eServicecommonValidRevisionChange_if



