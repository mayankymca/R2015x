###############################################################################
#
# $RCSfile: emxAdminObjUtil.tcl.rca $ $Revision: 1.4 $
#
# Description:  TCL routines used by installations
###############################################################################
###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

proc emxGetUniqueExpName {} {

    set sUniqueNumber [mql get env MX_UNIQUE_NUMBER]
    set sAppName [mql get env MXAPPLICATION]
    if {"$sUniqueNumber" == ""} {
        set sUniqueNumber 1
    } else {
        incr sUniqueNumber
    }
    
    mql set env MX_UNIQUE_NUMBER "$sUniqueNumber"
    
    return "${sAppName}_${sUniqueNumber}.exp"
}


proc LoadSchema { sProgramName } {

# This script is to set global variables, whose name is same as
# the property on the program passed to it as input argument.

    set sProgramName [string trim "$sProgramName"]
    set lAdmins [list store vault attribute type relationship format group role person association policy wizard index]

    if { $sProgramName == ""} {
       mql trace type INSTALL text "LoadSchema : Program Name cannot be null"
       mql trace type INSTALL text ""
       return
    }
    set sCmd {mql print program "$sProgramName" select property dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text "LoadSchema : $outStr"
        mql trace type INSTALL text ""
        return
    }

    set lOutput [split $outStr |]
    foreach lElement $lOutput {
        set lItem [split $lElement]
        set sPropertyName [lindex $lItem 0]
        set sPropertyValue [lrange $lItem 3 end]

        if {[lsearch $lAdmins [lindex [split $sPropertyName _] 0]] >= 0} {
            global "$sPropertyName"
            set "$sPropertyName" "$sPropertyValue"
        }
    }
}

proc eServiceLookupStateName {sAbsPolicyName sSymbolicStateName} {

      set sAbsPolicyName      [string trim "$sAbsPolicyName"]
      set sSymbolicStateName  [string trim "$sSymbolicStateName"]

      set sPrefixPolicy       [string trim [lindex [split $sAbsPolicyName "_"] 0]]
      set sPrefixState        [string trim [lindex [split $sSymbolicStateName "_"] 0]]

      if {[string compare "$sPrefixPolicy" "policy"] == 0} {
         mql trace type INSTALL text "eServiceLookupStateName: policy name cannot be a symbolic name"
         mql trace type INSTALL text ""
         return ""
      }
      if {[string compare "$sPrefixState" "state"] != 0} {
         mql trace type INSTALL text "eServiceLookupStateName: state name cannot be an absolute name"
         mql trace type INSTALL text ""
         return ""
      }

      set lPolicy    [split [mql list policy] "\n"]

      set iPolicyIndx  [lsearch -exact $lPolicy "$sAbsPolicyName"]

      if {$iPolicyIndx >= 0} {

         set lStateList [split [mql list property on policy "$sAbsPolicyName"] "\n"]

         foreach lStateElement $lStateList {

                    set lStateElement       [split [string trim $lStateElement]]
                    set sStatePropertyName  [string trim [lindex $lStateElement 0]]
                    set sStateArrName       [string trim [lindex [split $sStatePropertyName "_"] 0]]

                    if { $sStateArrName == "state"} {

                        set iIndx           [lsearch -exact $lStateElement "$sSymbolicStateName"]
                        if {$iIndx == 0} {
                            set iValIndx    [lsearch -exact $lStateElement "value"]
                            if {$iValIndx == -1} {
                               mql trace type INSTALL text "eServiceLookupStateName: Incorrect format for properties on policy"
                               mql trace type INSTALL text ""
                               return ""
                            } else {
                               set sAbsStateName  [string trim [lrange $lStateElement [expr $iValIndx + 1] end]]
                               regsub \{ $sAbsStateName "" sAbsStateName
                               regsub \} $sAbsStateName "" sAbsStateName
                               return "$sAbsStateName"
                            }
                        }
                    }
         }
      } else {
         mql trace type INSTALL text "eServiceLookupStateName: specified policy name does not exist"
         mql trace type INSTALL text ""
         return ""
      }
      return ""
}


proc eServiceGetCurrentSchemaName { sItemName sProgramName sPropertyName {sStateName ""}} {

    mql verbose off;

    # get the input argument
    set sInputType      [string trim $sItemName]
    set sPropProgName   [string trim $sProgramName]
    set sSymbolicName   [string trim $sPropertyName]

    # set program related variables
    set sProgName     "eServiceGetCurrentSchemaName"
    set mqlret        0
    set outstr        ""

    # check for null input argument
    if {($sSymbolicName == "") || ($sInputType == "") || ($sPropProgName == "")} {
         mql trace type INSTALL text ">ERROR       : $sProgName"
         mql trace type INSTALL text ">Error : Incomplete argument list"
         mql trace type INSTALL text ""
         return ""
    }

    # if type is state look for state symbolic name
    if {($sInputType == "state") && ($sStateName == "")} {
         mql trace type INSTALL text ">ERROR       : $sProgName"
         mql trace type INSTALL text ">Error : symbolic state name not specified"
         mql trace type INSTALL text ""
         return ""
    }

    set sSymStateName [string trim "$sStateName" ]

    # get the absolute name from symbolic name
    if {$mqlret == 0} {
         set sCmd {mql print program "$sPropProgName" \
                             select \
                                    property\[$sSymbolicName\].to \
                             dump \
                             }
         set mqlret [ catch {eval $sCmd} outstr ]
    }

    # return the value to calling program
    if {$mqlret == 0} {
         if {[string trim $outstr] != ""} {
              set sReturnStr [string trim [lrange [split $outstr] 1 end]]
              if {[string compare "$sInputType" "state"] != 0} {
                   return "$sReturnStr"
              } else {
                   set sCmd {mql print policy "$sReturnStr" \
                                       select \
                                              property\[$sSymStateName\].value \
                                       dump \
                                       }
                   set mqlret [ catch {eval $sCmd} outstr ]
                   if {$mqlret == 0} {
                        if {[string trim $outstr] != ""} {
                             return [string trim "$outstr"]
                        } else {
                             return ""
                        }
                   }
              }
         } else {
              return ""
         }
    }

    # if error, then display message
    if {$mqlret != 0} {
         return ""
    }

}


proc eServiceAddAdminObj {sOperation} {

    # Get required fields from input arguments.
    set sComment  [lindex $sOperation 0]
    set sType     [lindex $sOperation 2]
    set sTempName [lindex $sOperation 3]
    set sCmd      "set sName \"$sTempName\""
    uplevel       $sCmd
    upvar         sName sName
    upvar         sMode sMode
    if {"$sMode" != ""} {
        upvar         sFileId sFileId
    }
    set sRev      [lindex $sOperation 4]
    set sMQLCmd   [lindex $sOperation 5]

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Check if object already exists
    set sCmd {mql print bus "$sAbsType" "$sName" "$sRev" select exists dump}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceAddAdminObj"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }

    # If it is not present then execute lCmds to add it.
    if {"$outStr" == "TRUE"} {
        mql trace type INSTALL text [format ">Exists      : %-13s%-54s%-15s" $sAbsType $sName $sRev]
        mql trace type INSTALL text ""
    } else {
        mql trace type INSTALL text "$sComment"
        mql trace type INSTALL text ""
        if {"$sMode" != ""} {
            puts $sFileId "mql delete bus \"$sAbsType\" \"$sName\" \"$sRev\""
        }
        set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
        if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceAddAdminObj"
                mql trace type INSTALL text ">$sMQLCmd"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
        }
    }

    return 0
}

proc eServiceModifyAdminObj {sOperation} {

    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sVersion [lindex $lVersions 0]

    # Get required fields from input arguments.
    set sComment  [lindex $sOperation 0]
    set sType     [lindex $sOperation 2]
    set sTempName [lindex $sOperation 3]

    set sCmd      "set sName \"$sTempName\""
    uplevel       $sCmd
    upvar         sName sName
    upvar         sMode sMode
    if {"$sMode" != ""} {
        upvar         sFileId sFileId
    }
    set sRev      [lindex $sOperation 4]
    set sMQLCmd   [lindex $sOperation 5]

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Execute all the commands to delete object.
    mql trace type INSTALL text "$sComment"
    mql trace type INSTALL text ""
    if {"$sMode" != ""} {
        set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
        set sCmd {mql export bus "$sAbsType" "$sName" "$sRev" \
                      into file "$sExpFileName" \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceModifyAdminObj"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            set mqlall 1
            continue
        }
        puts $sFileId "mql import bus \"$sAbsType\" \"$sName\" \"$sRev\" overwrite from file \"$sExpFileName\""
    }
    set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
    if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceModifyAdminObj"
            mql trace type INSTALL text ">$sMQLCmd"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return 1
    }

    return 0
}

proc eServiceDeleteAdminObj {sOperation} {

    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sVersion [lindex $lVersions 0]

    # Get required fields from input arguments.
    set sComment  [lindex $sOperation 0]
    set sType     [lindex $sOperation 2]
    set sTempName [lindex $sOperation 3]
    set sCmd      "set sName \"$sTempName\""
    uplevel       $sCmd
    upvar         sName sName
    upvar         sMode sMode
    if {"$sMode" != ""} {
        upvar         sFileId sFileId
    }
    set sRev      [lindex $sOperation 4]
    set sMQLCmd   [lindex $sOperation 5]

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Check if object already exists
    set sCmd {mql print bus "$sAbsType" "$sName" "$sRev" select exists dump}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceDeleteAdminObj"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }

    # If it is not present then execute lCmds to add it.
    if {"$outStr" == "FALSE"} {
        mql trace type INSTALL text [format ">Deleted      :%-13s%-54s%-15s" $sAbsType $sName $sRev]
        mql trace type INSTALL text ""
    } else {
        mql trace type INSTALL text "$sComment"
        mql trace type INSTALL text ""
        if {"$sMode" != ""} {
            set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
            set sCmd {mql export bus "$sAbsType" "$sName" "$sRev" \
                          into file "$sExpFileName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceDeleteAdminObj"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                set mqlall 1
                continue
            }
            puts $sFileId "mql import bus \"$sAbsType\" \"$sName\" \"$sRev\" overwrite from file \"$sExpFileName\""
        }

        set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
        if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceDeleteAdminObj"
                mql trace type INSTALL text ">$sMQLCmd"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
        }
    }

    return 0
}

proc eServiceConnectAdminObj {sOperation} {
    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sVersion [lindex $lVersions 0]

    # Get required fields from input arguments.
    set sComment      [lindex $sOperation 0]
    set sToType       [lindex $sOperation 2]
    set sTempName     [lindex $sOperation 3]
    set sCmd          "set sToName \"$sTempName\""
    uplevel           $sCmd
    upvar             sToName sToName
    set sToRev        [lindex $sOperation 4]
    set sFromType     [lindex $sOperation 5]
    set sTempName     [lindex $sOperation 6]
    set sCmd          "set sFromName \"$sTempName\""
    uplevel           $sCmd
    upvar             sFromName sFromName
    set sFromRev      [lindex $sOperation 7]
    set sRelationship [lindex $sOperation 8]
    set sMQLCmd       [lindex $sOperation 9]
    upvar             sMode sMode
    if {"$sMode" != ""} {
        upvar             sFileId sFileId
    }

    set sAbsToType       [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sToType]
    set sAbsFromType     [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sFromType]
    set sAbsRelationship [eServiceGetCurrentSchemaName relationship eServiceSchemaVariableMapping.tcl $sRelationship]

    # Check if connection exists.
    set sCmd {mql expand bus "$sAbsToType" "$sToName" "$sToRev" \
                             from relationship "$sAbsRelationship" \
                             type "$sAbsFromType" \
                             dump | \
                             }
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConnectAdminObj"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }
    set lOutput [split $outStr "\n"]

    foreach sItem $lOutput {
        set lItem [split $sItem |]
        if {"$sAbsFromType" == [lindex $lItem 3] && "$sFromName" == [lindex $lItem 4] && "$sFromRev" == [lindex $lItem 5]} {
            mql trace type INSTALL text [format ">Connection   :%-13s%-54s%-15s" $sAbsToType $sToName $sToRev]
            mql trace type INSTALL text [format ">to           :%-13s%-54s%-15s" $sAbsFromType $sFromName $sFromRev]
            mql trace type INSTALL text "already exists"
            mql trace type INSTALL text ""
            return 0
        }
    }

    # Execute all the commands to connect objects.
    mql trace type INSTALL text "$sComment"
    mql trace type INSTALL text ""
    if {"$sMode" != ""} {
        set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
        set sCmd {mql export bus "$sAbsToType" "$sToName" "$sToRev" \
                      into file "$sExpFileName" \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConnectAdminObj"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            set mqlall 1
            continue
        }
        puts $sFileId "mql import bus \"$sAbsToType\" \"$sToName\" \"$sToRev\" overwrite from file \"$sExpFileName\""

        set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
        set sCmd {mql export bus "$sAbsFromType" "$sFromName" "$sFromRev" \
                      into file "$sExpFileName" \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConnectAdminObj"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            set mqlall 1
            continue
        }
        puts $sFileId "mql import bus \"$sAbsFromType\" \"$sFromName\" \"$sFromRev\" overwrite from file \"$sExpFileName\""
    }
    set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
    if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConnectAdminObj"
            mql trace type INSTALL text ">$sMQLCmd"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return 1
    }

    return 0
}


proc eServiceDisconnectAdminObj {sOperation} {

    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sVersion [lindex $lVersions 0]

    # Get required fields from input arguments.
    set sComment      [lindex $sOperation 0]
    set sToType       [lindex $sOperation 2]
    set sTempName     [lindex $sOperation 3]
    set sCmd          "set sToName \"$sTempName\""
    uplevel           $sCmd
    upvar             sToName sToName
    set sToRev        [lindex $sOperation 4]
    set sFromType     [lindex $sOperation 5]
    set sTempName     [lindex $sOperation 6]
    set sCmd          "set sFromName \"$sTempName\""
    uplevel           $sCmd
    upvar             sFromName sFromName
    set sFromRev      [lindex $sOperation 7]
    set sRelationship [lindex $sOperation 8]
    set sMQLCmd       [lindex $sOperation 9]
    upvar             sMode sMode
    if {"$sMode" != ""} {
        upvar             sFileId sFileId
    }


    set sAbsToType       [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sToType]
    set sAbsFromType     [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sFromType]
    set sAbsRelationship [eServiceGetCurrentSchemaName relationship eServiceSchemaVariableMapping.tcl $sRelationship]

    # Check if connection exists.
    set sCmd {mql expand bus "$sAbsToType" "$sToName" "$sToRev" \
                             from relationship "$sAbsRelationship" \
                             type "$sAbsFromType" \
                             dump | \
                             }
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceDisconnectAdminObj"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }
    set lOutput [split $outStr "\n"]

    set nObjectFound 0
    foreach sItem $lOutput {
        set lItem [split $sItem |]
        if {"$sAbsFromType" == [lindex $lItem 3] && "$sFromName" == [lindex $lItem 4] && "$sFromRev" == [lindex $lItem 5]} {
            set nObjectFound 1
            break
        }
    }

    if {$nObjectFound == 0} {
        mql trace type INSTALL text [format ">Connection   :%-13s%-54s%-15s" $sAbsToType $sToName $sToRev]
        mql trace type INSTALL text [format ">to           :%-13s%-54s%-15s" $sAbsFromType $sFromName $sFromRev]
        mql trace type INSTALL text "does not exists"
        mql trace type INSTALL text ""
    } else {
        # Execute all the commands to disconnect objects.
        mql trace type INSTALL text "$sComment"
        mql trace type INSTALL text ""
        if {"$sMode" != ""} {
            set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
            set sCmd {mql export bus "$sAbsToType" "$sToName" "$sToRev" \
                          into file "$sExpFileName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceDisconnectAdminObj"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                set mqlall 1
                continue
            }
            puts $sFileId "mql import bus \"$sAbsToType\" \"$sToName\" \"$sToRev\" overwrite from file \"$sExpFileName\""

            set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
            set sCmd {mql export bus "$sAbsFromType" "$sFromName" "$sFromRev" \
                          into file "$sExpFileName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceDisconnectAdminObj"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                set mqlall 1
                continue
            }
            puts $sFileId "mql import bus \"$sAbsFromType\" \"$sFromName\" \"$sFromRev\" overwrite from file \"$sExpFileName\""
        }
        set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceDisconnectAdminObj"
            mql trace type INSTALL text ">$sMQLCmd"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return 1
        }
    }

    return 0
}


proc eServiceModifyAdminObjConnection {sOperation} {

    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sVersion [lindex $lVersions 0]

    # Get required fields from input arguments.
    set sComment      [lindex $sOperation 0]
    set sToType       [lindex $sOperation 2]
    set sTempName     [lindex $sOperation 3]
    set sCmd          "set sToName \"$sTempName\""
    uplevel           $sCmd
    upvar             sToName sToName
    set sToRev        [lindex $sOperation 4]
    set sFromType     [lindex $sOperation 5]
    set sTempName     [lindex $sOperation 6]
    set sCmd          "set sFromName \"$sTempName\""
    uplevel           $sCmd
    upvar             sFromName sFromName
    set sFromRev      [lindex $sOperation 7]
    set sRelationship [lindex $sOperation 8]
    set sMQLCmd       [lindex $sOperation 9]
    upvar             sMode sMode
    if {"$sMode" != ""} {
        upvar             sFileId sFileId
    }

    set sAbsToType       [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sToType]
    set sAbsFromType     [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sFromType]
    set sAbsRelationship [eServiceGetCurrentSchemaName relationship eServiceSchemaVariableMapping.tcl $sRelationship]

    # Execute all the commands to modify connection between objects.
    mql trace type INSTALL text "$sComment"
    mql trace type INSTALL text ""
    if {"$sMode" != ""} {
        set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
        set sCmd {mql export bus "$sAbsToType" "$sToName" "$sToRev" \
                      into file "$sExpFileName" \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceModifyAdminObjConnection"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            set mqlall 1
            continue
        }
        puts $sFileId "mql import bus \"$sAbsToType\" \"$sToName\" \"$sToRev\" overwrite from file \"$sExpFileName\""

        set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
        set sCmd {mql export bus "$sAbsFromType" "$sFromName" "$sFromRev" \
                      into file "$sExpFileName" \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceModifyAdminObjConnection"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            set mqlall 1
            continue
        }
        puts $sFileId "mql import bus \"$sAbsFromType\" \"$sFromName\" \"$sFromRev\" overwrite from file \"$sExpFileName\""
    }
    set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceModifyAdminObjConnection"
        mql trace type INSTALL text ">$sMQLCmd"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }

    return 0
}


proc eServiceChangeAdminObjState {sOperation} {

    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sVersion [lindex $lVersions 0]

    # Get required fields from input arguments.
    set sComment  [lindex $sOperation 0]
    set sType     [lindex $sOperation 2]
    set sTempName [lindex $sOperation 3]
    set sCmd      "set sName \"$sTempName\""
    uplevel       $sCmd
    upvar         sName sName
    set sRev      [lindex $sOperation 4]
    set sState    [lindex $sOperation 5]
    upvar         sMode sMode
    if {"$sMode" != ""} {
        upvar         sFileId sFileId
    }

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Get current state , list of states supported and policy of an object
    set sCmd {mql print bus "$sAbsType" "$sName" "$sRev" select policy current state dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceChangeAdminObjState"
        mql trace type INSTALL text ">$sCmd"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }
    set lOutput       [split $outStr "|"]
    set sPolicy       [lindex $lOutput 0]
    set sCurrentState [lindex $lOutput 1]
    set lStates       [lrange $lOutput 2 end]
    set sTargetState  [eServiceLookupStateName "$sPolicy" "$sState"]

    # If object is in the same state then don't do anything
    # else promote or demote the object to bring it in right state.
    if {"$sCurrentState" == "$sTargetState"} {
        mql trace type INSTALL text [format ">Object       :%-13s%-54s%-15s" $sAbsType $sName $sRev]
        mql trace type INSTALL text [format ">is in state  :%-13s" $sTargetState]
        mql trace type INSTALL text ""
    } else {
        mql trace type INSTALL text [format ">Moving object:%-13s%-54s%-15s" $sAbsType $sName $sRev]
        mql trace type INSTALL text [format ">to state     :%-13s" $sTargetState]
        mql trace type INSTALL text ""
        if {"$sMode" != ""} {
            set sExpFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "[emxGetUniqueExpName]"]
            set sCmd {mql export bus "$sAbsType" "$sName" "$sRev" \
                          into file "$sExpFileName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceChangeAdminObjState"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                set mqlall 1
                continue
            }
            puts $sFileId "mql import bus \"$sAbsType\" \"$sName\" \"$sRev\" overwrite from file \"$sExpFileName\""
        }
        set sCurrentIndex [lsearch $lStates $sCurrentState]
        set sTargetIndex  [lsearch $lStates $sTargetState]

        if {"$sTargetIndex" > "$sCurrentIndex"} {
            set sDiff [expr $sTargetIndex - $sCurrentIndex]
            set sAction "promote"
        } else {
            set sDiff [expr $sCurrentIndex - $sTargetIndex]
            set sAction "demote"
        }

        for {set i 0} {$i < $sDiff} {incr i} {
            set sCmd {mql override bus "$sAbsType" "$sName" "$sRev"}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceChangeAdminObjState"
                mql trace type INSTALL text ">$sCmd"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }
            set sCmd {mql $sAction bus "$sAbsType" "$sName" "$sRev"}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceChangeAdminObjState"
                mql trace type INSTALL text ">$sCmd"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }
        }
    }

    return 0
}


proc eServiceInstallAdminBusObjs {lOperations} {

    set sInstallDir [mql get env MXAPPBUILD]
    set sRegProgName "eServiceSchemaVariableMapping.tcl"
    set sMode [mql get env MXMODE]
    set mqlret 0
    set mqlall 0

    if {"$sMode" != ""} {
        set sUnInstallFileName [file join "$sInstallDir" ".." ".." "Common" "UnInstall" "DB" "UnInstall.mql"]
        set sFileId [open "$sUnInstallFileName" a 0666]
    }

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $sRegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $sRegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {
        set lGlobalElement [split $lGlobalElement]
        set sPropertyName  [ lindex $lGlobalElement 0]
        global $sPropertyName
    }

    # execute each operation.
    foreach sOperation $lOperations {

        set nCounter 0
        set sComment [lindex $sOperation $nCounter]
        set sFunction [lindex $sOperation [incr nCounter]]

        switch $sFunction {
            "ADD_OBJECT"
                {
                    set mqlret [eServiceAddAdminObj "$sOperation"]
                }
            "MODIFY_OBJECT"
                {
                    set mqlret [eServiceModifyAdminObj "$sOperation"]
                }
            "DELETE_OBJECT"
                {
                    set mqlret [eServiceDeleteAdminObj "$sOperation"]
                }
            "CONNECT_OBJECTS"
                {
                    set mqlret [eServiceConnectAdminObj "$sOperation"]
                }
            "DISCONNECT_OBJECTS"
                {
                    set mqlret [eServiceDisconnectAdminObj "$sOperation"]
                }
            "MODIFY_CONNECTION"
                {
                    set mqlret [eServiceModifyAdminObjConnection "$sOperation"]
                }
            "CHANGE_STATE"
                {
                    set mqlret [eServiceChangeAdminObjState "$sOperation"]
                }
            default
                {
                    mql trace type INSTALL text ">ERROR       : eServiceInstallAdminBusObjs"
                    mql trace type INSTALL text ">Invalid Operation"
                    mql trace type INSTALL text ""
                    set mqlret 1
                }
        }

        if {$mqlret != 0} {
            set mqlall $mqlret
        }
    }

    if {"$sMode" != ""} {
        close $sFileId
    }

    return $mqlall
}


