#************************************************************************
# @progdoc      eServiceUpdateEndItemCountOnRevise.tcl
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
set sEvent [ string tolower [ mql get env EVENT ] ]
set sClassifiedItemId [ mql get env OBJECTID ]

# Load the utilities and schema mapping program.
eval [ mql print prog eServicecommonUtil.tcl select code dump ]
set sRegProgName "eServiceSchemaVariableMapping.tcl"
eval [ utLoad $sRegProgName ]


set sClassifiedItem [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_ClassifiedItem]


  set sCmd {mql expand bus "$sClassifiedItemId" \
      to relationship "$sClassifiedItem" \
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
      set sClassesId [ lindex $temp 6 ]


  # Get Absolute Names
  set sAttCount [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_Count" ]
  pushShadowAgent



  # If business object is already locked , Dont try to lock it again
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

  # check for the event create
  if { $sEvent == "revision" } {
      incr sCount
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

      # check for the event create
      if { $sEvent == "revision" } {
		incr sCount
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

      popShadowAgent
   }
  }
  return $mqlret
}

