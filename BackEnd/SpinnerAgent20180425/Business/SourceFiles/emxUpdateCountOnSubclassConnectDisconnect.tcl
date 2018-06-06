#************************************************************************
# @progdoc      eServiceUpdateEndItemCountOnSubclassConnectDisconnect.tcl
#
# @Brief:       This program update the count attribute by increment/decrement
#               of Part Family, Class , Library when ever Class object is connected/disconnect
#
# @Description: Updates the count.
#
# @Parameters:  None.
#
# @Returns:     Nothing.
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#************************************************************************

tcl;

eval {
#************************************************************************
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
#************************************************************************
# end utload

#************************************************************************
# Lock and unlock functions
# to enable lock and unlock only when object is not locked already

# lockBus function , returns true if object is locked in this method , false if object is locked previously
proc lockBus { busId } {
    set sCmd {mql print bus "$busId" \
                   select \
                   locked \
                   dump}

    set mqlret [catch {eval $sCmd} outStr]

    if {$mqlret != 0} {
        mql notice "$outStr"
    }

    if {$outStr == "TRUE"} {
        # If business object is already locked , Dont try to lock it again
        return "FALSE"  
        } else {
                set sCmd {mql lock bus "$busId"}
                set mqlret [catch {eval $sCmd} outStr]
                if {$mqlret != 0} {
                  mql notice "$outStr"
                }
                return "TRUE"
    }
}

# unlockBus function , unlock's business object if doUnlock is TRUE
proc unlockBus { busId doUnlock } {
    if {$doUnlock == "TRUE"} {
                set sCmd {mql unlock bus "$busId"}
                set mqlret [catch {eval $sCmd} outStr]
                if {$mqlret != 0} {
                  mql notice "$outStr"
                }
        }
}
# end lock and unlock functions
#************************************************************************


  mql verbose on;
  mql quote off;

  eval  [utLoad eServicecommonShadowAgent.tcl]

  # Get the classes id.
set sToClassesId   [ mql get env TOOBJECTID ]
set sClassesId   [ mql get env FROMOBJECTID ]

set sEvent [ string tolower [ mql get env EVENT ] ]

  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]

  # Get Absolute Names
  set sAttCount [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_Count" ]

  pushShadowAgent

  set sCmd {mql history off}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
	  mql notice "$outStr"
  }

  # If business object is already locked , Dont try to lock it again
  set lockStatus [ lockBus "$sToClassesId" ]

  # Get the Count of Classes in the attribute Count.
  set sCmd {mql print bus "$sToClassesId" \
                           select \
                           attribute\[$sAttCount\] \
                           dump | \
  }

  set mqlret [catch {eval $sCmd} outStr]

  if {$mqlret != 0} {
      mql notice "$outStr"
  }

  set lOutput [split $outStr |]
  set sClassCount [lindex $lOutput 0]

  # after processing ends, unlock the business object
  unlockBus "$sToClassesId" $lockStatus

  # Enable the history
  set sCmd {mql history on}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
	  mql notice "$outStr"
  }
  popShadowAgent

  set sSubClasses [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_Subclass]
  set sCmd {mql expand bus "$sClassesId" \
      to relationship "$sSubClasses" \
      type "Classification,Libraries" \
      recurse to all \
      select bus id \
      dump | \
  }

  set mqlret [ catch {eval  $sCmd} outStr ]

  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }

  set lInitialList [split $outStr \n]

  lappend lInitialList [list "||||||$sClassesId"]

  foreach ele $lInitialList {
      set temp [ split $ele "|" ]
      set sObjectId [ lindex $temp 6 ]

      pushShadowAgent

        set sCmd {mql history off}
	  set mqlret [catch {eval $sCmd} outStr]
	  if {$mqlret != 0} {
		  mql notice "$outStr"
	  }

      set lockStatus [ lockBus "$sObjectId" ]

      # Get the Count of Classes.
      set sCmd {mql print bus "$sObjectId" \
                      select \
                      attribute\[$sAttCount\] \
                      dump | \
      }

      set mqlret [catch {eval $sCmd} outStr]
      if {$mqlret != 0} {
          mql notice "$outStr"
      }

      set lOutput [split $outStr |]
      set sCount [lindex $lOutput 0]

      # check for the event create/delete
      if { $sEvent == "create" } {
	set sCount [expr $sCount + $sClassCount]
      } elseif {$sEvent == "delete"} {
	set sCount [expr $sCount - $sClassCount]
      }

      set sCmd {mql modify bus "$sObjectId" \
                           "$sAttCount" "$sCount" \
      }


      set mqlret [catch {eval $sCmd} outStr]
      if {$mqlret != 0} {
          mql notice "$outStr"
      }

      # after processing ends, unlock the business object
      unlockBus "$sObjectId" $lockStatus
          

	  # Enable the history
	  set sCmd {mql history on}
	  set mqlret [catch {eval $sCmd} outStr]
	  if {$mqlret != 0} {
		  mql notice "$outStr"
	  }
      popShadowAgent
  }

  return $mqlret
}

