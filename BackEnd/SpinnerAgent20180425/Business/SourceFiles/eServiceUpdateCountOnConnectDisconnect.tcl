#************************************************************************
# @progdoc      eServiceUpdateCountOnConnectDisconnect.tcl,v $ $Revision: 1.5 $
#
# @Brief:       This program update the count attribute by increment/decrement by 1
#               of Project Vault when ever a document object connected/disconnect
#
# @Description: This program sends iconmail to the Company Representatives
#               when an Employee is connected to a Company.
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

  mql verbose off;
  mql quote off;

  eval  [utLoad eServicecommonShadowAgent.tcl]

  # Get the Project Vault id.
  set sProjectVaultId   [ mql get env FROMOBJECTID ]
  set sEvent [ string tolower [ mql get env EVENT ] ]

  # Load the utilities and schema mapping program.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]

  # Get Absolute Names
  set sAttCount [ eServiceGetCurrentSchemaName "attribute" $sRegProgName "attribute_Count" ]

  pushShadowAgent

  # before processing starts, lock the business object
  set sCmd {mql lock bus "$sProjectVaultId"}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      mql notice "$outStr"
  }

  # Get the of Count Project Vault.
  set sCmd {mql print bus "$sProjectVaultId" \
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

  set sCmd {mql modify bus "$sProjectVaultId" \
                           "$sAttCount" "$sCount" \
  }

  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return $mqlret
  }

  # after processing ends, unlock the business object
  set sCmd {mql unlock bus "$sProjectVaultId"}
  set mqlret [catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      mql notice "$outStr"
  }

  popShadowAgent

  set sRelSubVaults [eServiceGetCurrentSchemaName relationship $sRegProgName relationship_SubVaults]
  set sCmd {mql expand bus "$sProjectVaultId" \
      to relationship "$sRelSubVaults" \
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
      set sCmd {mql lock bus "$sObjectId"}
      set mqlret [catch {eval $sCmd} outStr]
      if {$mqlret != 0} {
          mql notice "$outStr"
      }

      # Get the Count of Project Vault.
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
      } elseif  { $sEvent == "delete" } {
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
      set sCmd {mql unlock bus "$sObjectId"}
      set mqlret [catch {eval $sCmd} outStr]
      if {$mqlret != 0} {
          mql notice "$outStr"
      }

      popShadowAgent
  }

  return $mqlret
}

