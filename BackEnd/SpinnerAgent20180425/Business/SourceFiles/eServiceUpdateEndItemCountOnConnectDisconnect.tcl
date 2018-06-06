#************************************************************************
# @progdoc      eServiceUpdateEndItemCountOnConnectDisconnect.tcl,v $ $Revision: 1.5 $
#
# @Brief:       This program update the count attribute by increment/decrement by 1
#               of Part Family, Part, Class when ever an End Item object is connected/disconnect
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
  mql verbose off;
  mql quote off;

  eval  [utLoad eServicecommonShadowAgent.tcl]

  # Get the classes id.
  set sClassesId   [ mql get env FROMOBJECTID ]
  set sEvent [ string tolower [ mql get env EVENT ] ]
  #set sClassesId   "8330.46483.12064.60170"
  #set sEvent "create"
  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]

  # Get Absolute Names
  set sAttCount [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_Count" ]

  pushShadowAgent

# Added for bug 375552 to avoid history getting registered while updating the count value
  set sCmd {mql history off}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
	  mql notice "$outStr"
  }

  # before processing starts, lock the business object
  set lockStatus [ lockBus "$sClassesId" ]

  # Get the Count of Classes in the attribute Count.
  set sCmd {mql print bus "$sClassesId" \
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
      incr sCount
  } elseif {$sEvent == "delete"} {
      set sCount [expr $sCount - 1]
  }

  set sCmd {mql modify bus "$sClassesId" \
                           "$sAttCount" "$sCount" \
  }

  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }

  # after processing ends, unlock the business object
     unlockBus "$sClassesId" $lockStatus

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

  foreach ele $lInitialList {
      set temp [ split $ele "|" ]
      set sObjectId [ lindex $temp 6 ]

      pushShadowAgent
# Added for bug 375552 to avoid history getting registered while updating the count value
      set sCmd {mql history off}
      set mqlret [catch {eval $sCmd} outStr]
      if {$mqlret != 0} {
          mql notice "$outStr"
      }

      # after processing ends, unlock the business object
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
	incr sCount
      } elseif {$sEvent == "delete"} {
	set sCount [expr $sCount - 1]
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

      set sCmd {mql history on}
      set mqlret [catch {eval $sCmd} outStr]
      if {$mqlret != 0} {
          mql notice "$outStr"
      }

	  popShadowAgent
  }

  return $mqlret
}

