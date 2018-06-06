###############################################################################
#
# $RCSfile: eServiceSchemaVariableMapping.tcl.rca $ $Revision: 1.115 $
#
# Description:  This file contains all the procedures required for adding the
#                objects to the database in the modified format, wherein
#                collisions and version mismatch are taken care of.
# DEPENDENCIES: an environment variable MXAPPBUILD must be set which points to
#               a directory which has a specified directory structure and set
#               of files below it.
#
###############################################################################
###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################
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

proc LoadSchema { sProgramName } {

# This script is to set global variables, whose name is same as
# the property on the program passed to it as input argument.

    set sProgramName [string trim "$sProgramName"]
    set lAdmins [list store vault attribute type relationship format group role person association policy wizard index]

    if { $sProgramName == ""} {
       puts stdout "LoadSchema : Program Name cannot be null"
       return
    }
    set sCmd {mql print program "$sProgramName" select property dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "LoadSchema : $outStr"
        return
    }

    set lOutput [split $outStr |]
    foreach lElement $lOutput {
        set lItem [split $lElement]
        set sPropertyName [lindex $lItem 0]
        set sPropertyValue [lrange $lItem 3 end]

        if {[lsearch $lAdmins [lindex [split $sPropertyName _] 0]] >= 0} {
            catch {global "$sPropertyName"}
            catch {set "$sPropertyName" "$sPropertyValue"}
        }
    }
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceIsInstalled { sProgramName sProperty } {

# This script is used to check whether the property sent as input argument
# is connected to the program sent as the other input argument.

        set sProgramName [string trim "$sProgramName"]
        set sProperty    [string trim "$sProperty"]

        set lProgram [split [mql list program] "\n"]

        if { $sProgramName == ""} {
           puts stdout "eServiceIsInstalled : Program Name cannot be null"
           exit
        } elseif {[lsearch -exact $lProgram $sProgramName] == -1} {
           puts stdout "eServiceIsInstalled : $sProgramName does not exist"
           exit
        } elseif {$sProperty == ""} {
           return 0
        } else {
          set lList [split [mql list property on program $sProgramName] "\n"]
          foreach lElement $lList {

                  set lElement [split $lElement]
                  if {[lsearch -exact $lElement $sProperty] >= 0} {
                     return 1
                  }
#end of foreach loop
          }
# end of if elseif
        }
        return 0
# end of eServiceIsInstalled procedure
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceGetName { sProgramName sProperty } {

# This script is used to get the name of admin object attached to the property
# sent as input argument which connected to the program sent as the other input argument.

        set sProgramName [string trim "$sProgramName"]
        set sProperty    [string trim "$sProperty"]

        set lProgram [split [mql list program] "\n"]

        if { $sProgramName == ""} {
           puts stdout "eServiceGetName : Program Name cannot be null"
           exit
        } elseif {[lsearch -exact $lProgram $sProgramName] == -1} {
           puts stdout "eServiceGetName : $sProgramName does not exist"
           exit
        } else {
          mql quote on;
          set lList [split [mql list property on program $sProgramName] "\n"]
          foreach lElement $lList {

                  set lElement [split $lElement]
                  if {[lsearch -exact $lElement $sProperty] >= 0} {

                     # count tells us the number of admin objects or their names
                     # having space in them
                     set count [regsub -all \' $lElement {:} lElement]

                     if {$count == 0} {

                        set sPropertyValue [ lindex $lElement 6]
                        return $sPropertyValue

                     } elseif {$count == 2} {

                       # only one occurance of space between words
                       # it could be admin object or its name

                       set lElement [split $lElement ":"]
                       set lElementLength [llength $lElement]
                       if {$lElementLength == 3} {
                           set sPropertyValue [ lindex $lElement 2]
                           if {$sPropertyValue == ""} {
                              set sPropertyValue [ lindex $lElement 1]
                           }
                           return $sPropertyValue
                       }

                     } elseif {$count == 4} {

                       # both admin object & its name are having space
                       # separated words for names

                       set lElement [split $lElement ":"]
                       set sPropertyValue [ lindex $lElement 3]
                       return $sPropertyValue

                     }
                  }
#end of foreach loop
          }
          mql quote off;
          return ""
# end of if elseif
        }
# end of eServiceGetName procedure
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceGetVersion { sProgramName sProperty } {

# This script is used to get the version of the admin object attached to the property
# sent as input argument which connected to the program sent as the other input argument.

        set sProgramName   [string trim "$sProgramName"]
        set sProperty      [string trim "$sProperty"]
        set sItemType      [string trim [lindex [split $sProperty "_"] 0]]

        set lProgram [split [mql list program] "\n"]

        if { $sProgramName == ""} {
           puts stdout "eServiceGetVersion : Program Name cannot be null"
           exit
        } elseif {[lsearch -exact $lProgram $sProgramName] == -1} {
           puts stdout "eServiceGetVersion : $sProgramName does not exist"
           exit
        } else {
          mql quote on;
          set lList [split [mql list property on program $sProgramName] "\n"]
          foreach lElement $lList {

                  set lElement [split $lElement]
                  if {[lsearch -exact $lElement $sProperty] >= 0} {

                     # count tells us the number of admin objects or their names
                     # having space in them
                     set count [regsub -all \' $lElement {:} lElement]

                     if {$count == 0} {

                        set sPropertyValue [ lindex $lElement 6]

                     } elseif {$count == 2} {

                       # only one occurance of space between words
                       # it could be admin object or its name

                       set lElement [split $lElement ":"]
                       set lElementLength [llength $lElement]
                       if {$lElementLength == 3} {
                           set sPropertyValue [ lindex $lElement 2]
                           if {$sPropertyValue == ""} {
                              set sPropertyValue [ lindex $lElement 1]
                           }
                       }

                     } elseif {$count == 4} {

                       # both admin object & its name are having space
                       # separated words for names

                       set lElement [split $lElement ":"]
                       set sPropertyValue [ lindex $lElement 3]

                     }
                  }
#end of foreach loop
          }
          mql quote off;

          set lList                [split [mql list property on "$sItemType" "$sPropertyValue"] "\n"]
          set sPropertyValue       [split $sPropertyValue]
          set sPropertyValueLength [llength $sPropertyValue]
          # this variable tells us whether a multi word name for admin object
          # lies in the list starting with version as the first element
          set found 0

          foreach lElement $lList {

                  set lElement [split $lElement]
                  if {[lsearch -exact $lElement "version"] >= 0} {

                        foreach lItem $sPropertyValue {
                                if {[lsearch -exact $lElement $lItem ] >= 0} {
                                   incr found
                                }
                        }

                        # the value of found if equals the length of the admin
                        # object name it means the admin object name lies in the list
                        # because variable found is incremented everytime a word of
                        # multiword admin object name lies in the lElement list
                        if {$found == $sPropertyValueLength} {
                           set lElementLength [llength $lElement]
                           set sVersion [lindex $lElement [expr $lElementLength - 1]]
                           return $sVersion
                        } else {
                           set found 0
                        }
                  }
#end of foreach loop
          }
# end of if elseif
        }
# end of eServiceGetVersion procedure
}

################################################################################

proc eServiceIsModified { sProgramName sProperty } {
    return 0
}

################################################################################

proc eServiceInstallAdmin { lCmdList ItemType } {


    # set directory
    set sFullDir [mql get env MXAPPBUILD]
    set sPixDir  [mql get env MXPIXMAPDIR]

    set sVersion      [mql get env MXVERSION]
    set sApplication  [mql get env MXAPPLICATION]
    set prefixName    [mql get env MXPREFIXNAME]
    set RegProgName   [mql get env REGISTRATIONOBJECT]
    set sInstallOrg   [mql get env MXORGNAME]
    set sFont         [mql get env MXFONT]
    set sFixedFont    [mql get env MXFIXEDFONT]
    set sLanguage     [mql get env MXLANGUAGE]
    set sProdData     [mql get env MXPRODDATATABLE]
    set sProdIndex    [mql get env MXPRODINDEXTABLE]
    set sMode         [mql get env MXMODE]
    set sDate         [clock format [clock seconds] -format %m-%d-%Y]
    set mqlall        0

    if {"$sMode" != ""} {
        set sSchemaFileName [file join "$sFullDir" ".." ".." "Common" "UnInstall" "DB" "SchemaChanges.mql"]
        set sFileId [open "$sSchemaFileName" a 0666]
    }

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # Get list for existence check
    set lAdminUserTypes [list "person" "role" "group" "association"]
    if {[lsearch $lAdminUserTypes $ItemType] >= 0} {
        set lItem [split [mql list user] "\n"]
    } else {
        set lItem [split [mql list \"$ItemType\"] "\n"]
    }

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {
        set lGlobalElement [split $lGlobalElement]
        set sPropertyName  [ lindex $lGlobalElement 0]
        catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

        # check for correct format of input list being processed
        if {[llength $lElement] < 4} {

            puts stdout ">ERROR       : eServiceInstallAdmin"
            puts stdout ">Incorrect format"
            puts stdout ">$lElement"
            set mqlall 1
            continue
        }

        set mqlret 0
        set ItemProperty  [ string trim [lindex $lElement 1 ]]
        set ItemName      [ string trim [lindex $lElement 0]]

        set AdminType         [ string trim [lindex [split $ItemProperty "_"] 0]]
        set ItemNameOriginal  $ItemName

        set sCmd {mql print program "$RegProgName" select property\[$ItemProperty\].to dump}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : eServiceInstallAdmin"
            puts stdout ">$outStr"
            set mqlall 1
            continue
        }

        if {"$outStr" != ""} {
            set bItemInstalled 1
            set ItemName [lrange [split $outStr] 1 end]
  #BEGIN OF CHANGE
            if {$AdminType == "role" || $AdminType == "group" || $AdminType == "association"} {
                set sCmd {mql print user "$ItemName" select property\[version\].value property\[application\].value dump |}
            } else {
                set sCmd {mql print "$AdminType" "$ItemName" select property\[version\].value property\[application\].value dump |}
            }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallAdmin"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }
            set ItemVersion [lindex [split "$outStr" |] 0]
            set ItemApplication [lindex [split "$outStr" |] 1]
  #END OF CHANGE
            set bItemModified     [ eServiceIsModified $RegProgName $ItemProperty  ]
        } else {
            set bItemInstalled 0
            # the if statement below looks for name collision of BO name
            # in the database
            if { [lsearch -exact $lItem "$ItemName"] >= 0} {
                puts stdout [format ">Renaming    : %-13s%-54s" $AdminType  $ItemName]
                set ItemName "$prefixName$ItemName"
                set $ItemProperty $ItemName
            } else {
                set $ItemProperty $ItemName
            }
        }

        if { $bItemInstalled } {
            if {"$sMode" == "HOT_FIX"} {
                set sCmdVersion    [ lindex $lElement 2]
                set ItemExec       [ lindex $lElement 3]

                set lItemExec [split "$ItemExec"]
                set lNewItemExec {}
                foreach sItemExec $lItemExec {
                    if {"$sItemExec" != ""} {
                        lappend lNewItemExec "$sItemExec"
                    }
                }

                # if admin object is already present and mql command is
                # trying to add it then skip the command
                if {[string tolower [lindex $lNewItemExec 1]] == "add"} {
                    puts stdout [format ">Exists      : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                    regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                    regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                    regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                    regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                    set sCmd "puts stdout \"[format ">Skipping    : %-13s%-54s" "Command"  "$ItemExec"]\""
                    eval $sCmd
                    continue
                }
                if {"$ItemType" == "policy"} {
                    set sCmd {mql print policy "$ItemName" select property.name property.value dump tcl}
                    set mqlret [ catch {eval $sCmd} outStr ]
                    if { $mqlret == 0} {
                        set lPolicyProp [lindex [lindex $outStr 0] 0]
                        set lPolicyPropVal [lindex [lindex $outStr 0] 1]
                        foreach sPolicyProp $lPolicyProp sPolicyPropVal $lPolicyPropVal {
                            if {[string first "state_" "$sPolicyProp"] == 0} {
                                set $sPolicyProp "$sPolicyPropVal"
                            }
                        }
                    }
                }
                if { $mqlret == 0} {
                    puts stdout [format ">Modifying   : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                    set mqlret [ catch {eval $ItemExec} outStr ]
                    if {$mqlret == 0} {
                        if {"$sMode" != ""} {
                            regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                            regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                            regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                            regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                            set sCmd "puts $sFileId \"$ItemExec\""
                            set mqlret [ catch {eval $sCmd} outStr ]
                        }
                    }
                }
                if { ($mqlret == 0) && ($ItemType == "policy")} {
                    eServiceAddPropertyPolicy $ItemName
                }
                
                continue
            }

            # check for same version & modified flag, do necessary processing
            if { ($ItemVersion == $sVersion) && ($bItemModified == 1) } {

                puts stdout [format ">EXCEPTION   : %-13s%-54s: Modified Not Implemented" $AdminType  $ItemName]
                continue

            } elseif { ($ItemVersion == $sVersion) && ($bItemModified == 0) && ($ItemApplication != "FrameworkFuture")} {

                puts stdout [format ">Same Version: %-13s%-54s%-15s"  $AdminType  $ItemName  $ItemVersion]
                continue

            } elseif { ($ItemVersion != $sVersion) && ($bItemModified == 1) } {

                puts stdout [format ">EXCEPTION   : %-13s%-54s: Modified Not Implemented" $AdminType  $ItemName]
                continue

            } elseif { ($ItemVersion != $sVersion) && ($bItemModified == 0) || ($ItemApplication == "FrameworkFuture")} {

                set sCmdVersion    [ lindex $lElement 2]
                set ItemExec       [ lindex $lElement 3]

                # if application property on admin object is set to FrameworkFuture
                # then compare version on admin object with that passed in as part of command
                if {"$ItemApplication" == "FrameworkFuture"} {
                    if {[eServiceVersionCompare $ItemVersion $sCmdVersion] == 1} {
                        puts stdout [format ">Skipping    : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                        regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                        regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                        regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                        regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                        set sCmd "puts stdout \"[format ">Skipping    : %-13s%-54s" "Command"  "$ItemExec"]\""
                        eval $sCmd
                        continue
                    }
                    if {"$ItemVersion" == "$sCmdVersion"} {
                        puts stdout [format ">Syncing     : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                        regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                        regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                        regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                        regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                        set sCmd "puts stdout \"[format ">Skipping    : %-13s%-54s" "Command"  "$ItemExec"]\""
                        eval $sCmd
                        set sCmd {mql modify property application \
                                      on "$ItemType" "$ItemName" \
                                      value "$sApplication" \
                                 }
                        set mqlret [ catch {eval $sCmd} outStr ]
                        continue
                    }
                }

                set lItemExec [split "$ItemExec"]
                set lNewItemExec {}
                foreach sItemExec $lItemExec {
                    if {"$sItemExec" != ""} {
                        lappend lNewItemExec "$sItemExec"
                    }
                }

                # if admin object is already present and mql command is
                # trying to add it then skip the command
                if {[string tolower [lindex $lNewItemExec 1]] == "add"} {
                    puts stdout [format ">Exists      : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                    regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                    regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                    regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                    regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                    set sCmd "puts stdout \"[format ">Skipping    : %-13s%-54s" "Command"  "$ItemExec"]\""
                    eval $sCmd
                    continue
                }

                # modify admin object only if admin object version is less then AEF
                # version getting installed.
                if {[eServiceVersionCompare "$ItemVersion" "$sCmdVersion"] != 1 && "$ItemVersion" != "$sCmdVersion"} {
                    if { $mqlret == 0} {
                        if {"$ItemType" == "policy"} {
                            set sCmd {mql print policy "$ItemName" select property.name property.value dump tcl}
                            set mqlret [ catch {eval $sCmd} outStr ]
                            if { $mqlret == 0} {
                                set lPolicyProp [lindex [lindex $outStr 0] 0]
                                set lPolicyPropVal [lindex [lindex $outStr 0] 1]
                                foreach sPolicyProp $lPolicyProp sPolicyPropVal $lPolicyPropVal {
                                    if {[string first "state_" "$sPolicyProp"] == 0} {
                                        set $sPolicyProp "$sPolicyPropVal"
                                    }
                                }
                            }
                        }
                        if { $mqlret == 0} {
                            puts stdout [format ">Modifying   : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                            set mqlret [ catch {eval $ItemExec} outStr ]
                            if {$mqlret == 0} {
                                if {"$sMode" != ""} {
                                    regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                                    regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                                    regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                                    regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                                    set sCmd "puts $sFileId \"$ItemExec\""
                                    set mqlret [ catch {eval $sCmd} outStr ]
                                }
                            }
                        }
                    }
                }

                if { $mqlret == 0} {
                    set sCmd {mql modify property version \
                                  on "$ItemType" "$ItemName" \
                                  value "$sCmdVersion" \
                                  }
                    set mqlret [ catch {eval $sCmd} outStr ]
                }
                if { $mqlret == 0} {
                    set sCmd {mql modify property "installed date" \
                                  on "$ItemType" "$ItemName" \
                                  value "$sDate" \
                                  }
                    set mqlret [ catch {eval $sCmd} outStr ]
                }
                if { ($mqlret == 0) && ($ItemType == "policy")} {
                    eServiceAddPropertyPolicy $ItemName
                }
            }

        } else {

            set sCmdVersion    [ lindex $lElement 2 ]
            set ItemExec       [ lindex $lElement 3]
            if { $mqlret == 0} {
                puts stdout [format ">Adding      : %-13s%-54s%-15s" $AdminType  $ItemName  $sCmdVersion]
                set mqlret [ catch {eval $ItemExec} outStr ]
                if { $mqlret == 0} {
                    if {"$sMode" != ""} {
                        regsub -all "\"" "$ItemExec" "\\\"" ItemExec
                        regsub -all "print role \\$" "$ItemExec" "print role \\\\$" ItemExec
                        regsub -all "print group \\$" "$ItemExec" "print group \\\\$" ItemExec
                        regsub -all "print group \{\\$" "$ItemExec" "print group \{\\\\$" ItemExec
                        set sCmd "puts $sFileId \"$ItemExec\""
                        set mqlret [ catch {eval $sCmd} outStr ]
                    }
                }
            }

            if { $mqlret == 0} {
                set sCmd {mql add property "$ItemProperty" \
                              on program "$RegProgName" \
                              to "$ItemType" "$ItemName" \
                         }
                set mqlret [ catch {eval $sCmd} outStr ]
            }
            if { $mqlret == 0} {
                set sCmd {mql add property application \
                                  on "$ItemType" "$ItemName" \
                                  value "$sApplication" \
                         }
                set mqlret [ catch {eval $sCmd} outStr ]
            }
            if { $mqlret == 0} {
                set sCmd {mql add property version \
                                  on "$ItemType" "$ItemName" \
                                  value "$sCmdVersion" \
                         }
                set mqlret [ catch {eval $sCmd} outStr ]
            }
            if { $mqlret == 0} {
                set sCmd {mql add property 'original name' \
                                  on "$ItemType" "$ItemName" \
                                  value "$ItemNameOriginal" \
                         }
                set mqlret [ catch {eval $sCmd} outStr ]
            }
            if { $mqlret == 0} {
                set sCmd {mql add property installer \
                              on "$ItemType" "$ItemName" \
                              value "$sInstallOrg" \
                              }
                set mqlret [ catch {eval $sCmd} outStr ]
            }
            if { $mqlret == 0} {
                set sCmd {mql add property 'installed date' \
                              on "$ItemType" "$ItemName" \
                              value "$sDate" \
                              }
                set mqlret [ catch {eval $sCmd} outStr ]
            }
            if { ($mqlret == 0) && ($ItemType == "policy")} {
                eServiceAddPropertyPolicy $ItemName
            }
        }

        if { $mqlret != 0 } {

            puts stdout ">ERROR       : eServiceInstallAdmin"
            puts stdout ">$outStr"
            set mqlall $mqlret
        }
    }

    if {"$sMode" != ""} {
        close $sFileId
    }

    return $mqlall
}

################################################################################

proc eServiceAddPropertyPolicy { sPolicyName } {

     set mqlret 0
     set sPolicyName [string trim "$sPolicyName"]

     # get the names of all the states on the policy
     if { $mqlret == 0} {
         set sCmd {mql print policy "$sPolicyName" \
                             select state;
                   }
         set mqlret [ catch {eval $sCmd} outStr ]
     }

     # the first line gives the admin type & name, so pick from line 2
     if { $mqlret == 0} {
        set lStateList [split $outStr \n]
        set lStateList [lrange $lStateList 1 end]
     }

     # get all the properties on the policy given by sPolicyName
     if { $mqlret == 0} {
         set sCmd {mql list property on policy "$sPolicyName";}
         set mqlret [ catch {eval $sCmd} outStr ]
     }

     if { $mqlret == 0} {
          set lPropertyList [split $outStr \n]
     }

     foreach lStateElement $lStateList {

        # if this variable is set then the property is on the policy
        set PropertyFound 0
        set lStateName [string trim [lindex [ split $lStateElement "="] 1]]
        regsub -all " " $lStateName {} StateProperty
        set StateProperty "state_${StateProperty}"

        # check whether this property is already on the policy
        foreach lPropertyElement $lPropertyList {
            if {[lsearch -exact $lPropertyElement $StateProperty] >= 0} {
               set PropertyFound 1
               break
            }
        }

        if {$PropertyFound == 0} {
            if {$mqlret == 0} {
               set sCmd {mql add property "$StateProperty" \
                                on policy "$sPolicyName" \
                                       value "$lStateName" \
                        }
               set mqlret [ catch {eval $sCmd} outStr ]
            }
        }
     }

     if { $mqlret != 0 } {
             puts stdout "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
             puts stdout ""
             puts stdout "$outStr"
             puts stdout ""
             puts stdout "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
     }
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceRefresh { sProgramName } {

    # this program is used to dynamically refresh the global variables created
    # for properties attached to eServiceSchemaVaribaleMapping.tcl. This is
    # called only during the addition to the database rather than modifying an
    # existing object.

    set sProgramName [string trim "$sProgramName" ]
    LoadSchema  $sProgramName
    return

}

################################################################################

proc emxGetCurrentSchemaName { sItemName sProgramName sPropertyName {sStateName ""}} {

    mql verbose off;

    # get the input argument
    set sInputType      [string trim $sItemName]
    set sPropProgName   [string trim $sProgramName]
    set sSymbolicName   [string trim $sPropertyName]

    # set program related variables
    set sProgName     "emxGetCurrentSchemaName"
    set mqlret        0
    set outstr        ""

    # check for null input argument
    if {($sSymbolicName == "") || ($sInputType == "") || ($sPropProgName == "")} {
         puts stdout ">ERROR       : $sProgName"
         puts stdout ">Error : Incomplete argument list"
         return ""
    }

    # if type is state look for state symbolic name
    if {($sInputType == "state") && ($sStateName == "")} {
         puts stdout ">ERROR       : $sProgName"
         puts stdout ">Error : symbolic state name not specified"
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
         puts stdout ">ERROR       : $sProgName"
         puts stdout ">Error : Incomplete argument list"
         return ""
    }

    # if type is state look for state symbolic name
    if {($sInputType == "state") && ($sStateName == "")} {
         puts stdout ">ERROR       : $sProgName"
         puts stdout ">Error : symbolic state name not specified"
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
                             set outstr "Error : $sProgName - $sSymStateName not property on policy $sReturnStr"
                             mql notice $outstr
                             return ""
                        }
                   }
              }
         } else {
              set outstr "Error : $sProgName - $sSymbolicName not property on $sPropProgName"
              mql notice $outstr
              return ""
         }
    }

    # if error, then display message
    if {$mqlret != 0} {
         mql notice $outstr
         return ""
    }

}

# end of eServiceGetCurrentSchemaName proc

################################################################################

proc LoadSchemaGlobalEnv { sProgramName } {

# This script is to set global environmental variables, whose name is same as
# the property on the program passed to it as input argument.

        set sProgramName [string trim "$sProgramName"]
        set lProgram [split [mql list program] "\n"]

        if { $sProgramName == ""} {
           puts stdout "LoadSchemaGlobalEnv : Program Name cannot be null"
           exit
        } elseif {[lsearch -exact $lProgram $sProgramName] == -1} {
           puts stdout "LoadSchemaGlobalEnv : $sProgramName does not exist"
           exit
        } elseif {[string trim [mql get env global eServiceLoadSchemaGlobalEnvSet]] == ""} {
          mql quote on;
          set lList [split [mql list property on program $sProgramName] "\n"]
          foreach lElement $lList {

                     set lElement [split $lElement]
                     set sPropertyName  [ lindex $lElement 0]

                     # count tells us the number of admin objects or their names
                     # having space in them
                     set count [regsub -all \' $lElement {:} lElement]


                     if {$count == 0} {

                        set sPropertyValue [ lindex $lElement 6]

                     } elseif {$count == 2} {

                       # only one occurance of space between words
                       # it could be admin object or its name

                       set lElement [split $lElement ":"]
                       set lElementLength [llength $lElement]
                       if {$lElementLength == 3} {
                           set sPropertyValue [ lindex $lElement 2]
                           if {$sPropertyValue == ""} {
                              set sPropertyValue [ lindex $lElement 1]
                           }
                       }

                     } elseif {$count == 4} {

                       # both admin object & its name are having space
                       # separated words for names

                       set lElement [split $lElement ":"]
                       set sPropertyValue [ lindex $lElement 3]

                     }

                     set  sPropertyValue [string trim "$sPropertyValue"]
                     mql set env global "$sPropertyName" "$sPropertyValue"

#end of foreach loop
         }
         mql set env global eServiceLoadSchemaGlobalEnvSet 1
# end of if elseif
       }
       mql quote off;
# end of LoadSchemaGlobalEnv procedure
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceInstallProgram { lCmdList ItemType } {

    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]

    set sNewObjName ""
    set sErrorMsg         ""
    set sOrgName          ""
    set lExistProgram     [split [mql list "$ItemType"] "\n"]

    foreach lElement $lCmdList {

             # check for correct format of input list being processed
             if {[llength $lElement] != 2} {

                puts stdout ">ERROR       : eServiceInstallProgram"
                    puts stdout ">$lElement - Incorrect format"
                 continue
             }

             set iProgramFound   0
             set mqlret          0
             set sObjectName     [ string trim [lindex $lElement 0 ]]
             set sMqlCommand     [ string trim [lindex $lElement 1 ]]

             set sMqlCommand [split $sMqlCommand]
             set sIndex [lsearch -exact $sMqlCommand "OBJNAME"]
             if {$sIndex == -1} {
                 append sErrorMsg "Incorrect Format: $lElement\n"
                 continue
             } else {
                 set sMqlCommandPart [lrange $sMqlCommand [expr $sIndex + 1] end]
             }

             foreach lSubItem $lExistProgram {
                     set lSubItem [string trim $lSubItem]
                     if {[string compare "$sObjectName" "$lSubItem"] == 0} {
                         set iProgramFound 1
                         break
                     }
             }

             if {$iProgramFound == 0} {

                  set sCmd {mql add program "$sObjectName"}
                  append  sCmd " " [join $sMqlCommandPart]
                  
                  if {[lsearch $sMqlCommandPart "file"] >= 0} {
                      set sFileName [lindex $sMqlCommandPart [expr [lsearch $sMqlCommandPart "file"] + 1]]
                      regsub -all "\"" "$sFileName" "" sFileName
                      regsub -all "\\\\" "$sFileName" "" sFileName
                      set sFileCmd "file exists $sFileName"
                      set mqlret [ catch {eval $sFileCmd} outStr ]
                      if {$outStr == 0} {
                          continue
                      }
                  }
                  
                  puts stdout [format ">Adding      : %-13s%-54s%-15s" $ItemType  $sObjectName  $sInstallVersion]
                  set mqlret [ catch {eval $sCmd} outStr ]

                  if { $mqlret == 0} {
                       set sCmd {mql add property application \
                                     on "$ItemType" "$sObjectName" \
                                     value "$sApplication" \
                                     }
                       set mqlret [ catch {eval $sCmd} outStr ]
                  }
                  if { $mqlret == 0} {
                       set sCmd {mql add property version \
                                     on "$ItemType" "$sObjectName" \
                                     value "$sInstallVersion" \
                                     }
                       set mqlret [ catch {eval $sCmd} outStr ]
                  }
                  if { $mqlret == 0} {
                       set sCmd {mql add property 'original name' \
                                     on "$ItemType" "$sObjectName" \
                                     value "$sObjectName" \
                                     }
                       set mqlret [ catch {eval $sCmd} outStr ]
                 }
                 if { $mqlret == 0} {
                      set sCmd {mql add property installer \
                                    on "$ItemType" "$sObjectName" \
                                    value "$sInstallOrg" \
                                    }
                      set mqlret [ catch {eval $sCmd} outStr ]
                 }
                 if { $mqlret == 0} {
                      set sCmd {mql add property 'installed date' \
                                    on "$ItemType" "$sObjectName" \
                                    value "$sDate" \
                                    }
                      set mqlret [ catch {eval $sCmd} outStr ]
                }
                if { $mqlret != 0} {
                    puts stdout ">ERROR       : eServiceInstallProgram"
                        puts stdout ">$sCmd"
                        puts stdout ">$outStr"
                }

             } else {

                    set lProgPropList   [split [mql list property on $ItemType $sObjectName] "\n"]
                    foreach lProgElement $lProgPropList {

                            set lProgElement [split $lProgElement]
                            set sIndex [lsearch -exact $lProgElement "installer"]
                            if {$sIndex == 0} {
                               set lProgElementLength [llength $lProgElement]
                               set sOrgName [string trim [lindex $lProgElement [expr $lProgElementLength - 1]]]
                               break
                            }

                   }
                   if {$sOrgName == ""} {

                        append sErrorMsg "$sObjectName already exists, but not owned by Matrix-One\n"

                   } elseif {[string compare "$sOrgName" "MatrixOneEngineering"] == 0} {

                          foreach lProgElement $lProgPropList {

                                  set lProgElement [split $lProgElement]
                                  set sIndex [lsearch -exact $lProgElement "version"]
                                  if {$sIndex == 0} {
                                     set lProgElementLength [llength $lProgElement]
                                     set sProgramVersion [string trim [lindex $lProgElement [expr $lProgElementLength - 1]]]
                                     break
                                  }

                          }

                          if {[eServiceVersionCompare "$sProgramVersion" "$sInstallVersion"] == 0} {

                               set sCmd {mql modify program "$sObjectName"}
                               append  sCmd " " [join $sMqlCommandPart]

                               if {[lsearch $sMqlCommandPart "file"] >= 0} {
                                   set sFileName [lindex $sMqlCommandPart [expr [lsearch $sMqlCommandPart "file"] + 1]]
                                   regsub -all "\"" "$sFileName" "" sFileName
                                   regsub -all "\\\\" "$sFileName" "" sFileName
                                   set sFileCmd "file exists $sFileName"
                                   set mqlret [ catch {eval $sFileCmd} outStr ]
                                   if {$outStr == 0} {
                                       continue
                                   }
                               }

                               puts stdout [format ">Modifying   : %-13s%-54s%-15s" $ItemType  $sObjectName  $sInstallVersion]
                               set mqlret [ catch {eval $sCmd} outStr ]
                               if { $mqlret == 0} {

                                    set sCmd {mql modify property version \
                                                  on "$ItemType" "$sObjectName" \
                                                  value "$sInstallVersion" \
                                                  }
                                    set mqlret [ catch {eval $sCmd} outStr ]
                               }
                               if { $mqlret == 0} {
                                    set sCmd {mql modify property "installed date" \
                                                  on "$ItemType" "$sObjectName" \
                                                  value "$sDate" \
                                                  }
                                    set mqlret [ catch {eval $sCmd} outStr ]
                               }
                               if { $mqlret != 0} {
                                    puts stdout ">ERROR       : eServiceInstallProgram"
                                        puts stdout ">$sCmd"
                                        puts stdout ">$outStr"
                               }

                         } elseif {[eServiceVersionCompare "$sProgramVersion" "$sInstallVersion"] == 1} {

                               puts stdout [format ">Exists      : %-13s%-54s%-15s" $ItemType  $sObjectName  $sProgramVersion]

                         }
                 }
          }

          if {$mqlret != 0} {
              append sErrorMsg "$outStr\n"
          }
    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {

              return 1
    }

    return 0
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceInstallCustomProgram { lCmdList ItemType } {

    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]

    set lExistProgram     [split [mql list "$ItemType"] "\n"]
    set mqlall 0
    
    foreach lElement $lCmdList {

        # check for correct format of input list being processed
        if {[llength $lElement] != 2} {
            puts stdout ">ERROR       : eServiceInstallCustomProgram"
            puts stdout ">$lElement - Incorrect format"
            set mqlall 1
            continue
        }

        set mqlret          0
        set sObjectName     [ string trim [lindex $lElement 0 ]]
        set sMqlCommand     [ string trim [lindex $lElement 1 ]]

        set sMqlCommand [split $sMqlCommand]
        set sIndex [lsearch -exact $sMqlCommand "OBJNAME"]
        if {$sIndex == -1} {
            puts stdout ">ERROR       : eServiceInstallCustomProgram"
            puts stdout ">$lElement - Incorrect format"
            set mqlall 1
            continue
        } else {
             set sMqlCommandPart [lrange $sMqlCommand [expr $sIndex + 1] end]
        }

        if {[lsearch $lExistProgram "$sObjectName"] == "-1"} {
            set sCmd {mql add program "$sObjectName"}
            append  sCmd " " [join $sMqlCommandPart]

            if {[lsearch $sMqlCommandPart "file"] >= 0} {
                set sFileName [lindex $sMqlCommandPart [expr [lsearch $sMqlCommandPart "file"] + 1]]
                regsub -all "\"" "$sFileName" "" sFileName
                regsub -all "\\\\" "$sFileName" "" sFileName
                set sFileCmd "file exists $sFileName"
                set mqlret [ catch {eval $sFileCmd} outStr ]
                if {$outStr == 0} {
                    continue
                }
            }

            # Add program
            puts stdout [format ">Adding      : %-13s%-54s%-15s" $ItemType  $sObjectName  $sInstallVersion]
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallCustomProgram"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }

            # add application property
            set sCmd {mql add property application \
                         on "$ItemType" "$sObjectName" \
                         value "$sApplication" \
                         }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallCustomProgram"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }

            # add version property
            set sCmd {mql add property version \
                         on "$ItemType" "$sObjectName" \
                         value "$sInstallVersion" \
                         }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallCustomProgram"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }

            # add original name
            set sCmd {mql add property 'original name' \
                         on "$ItemType" "$sObjectName" \
                         value "$sObjectName" \
                         }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallCustomProgram"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }

            # add installer
            set sCmd {mql add property installer \
                        on "$ItemType" "$sObjectName" \
                        value "$sInstallOrg" \
                        }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallCustomProgram"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }

            # add installed date
            set sCmd {mql add property 'installed date' \
                        on "$ItemType" "$sObjectName" \
                        value "$sDate" \
                        }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceInstallCustomProgram"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }
        } else {
            puts stdout [format ">Exists      : %-13s%-54s" $ItemType  $sObjectName]
        }
    }

    return $mqlall
}

################################################################################

proc eServiceVersionCompare {sProgramVersion sInstallVersion} {

    if {($sProgramVersion == "") || ($sInstallVersion == "")} {
       puts stdout "eServiceVersionCompare: Error - incomplete list of arguments"
       return 9999
    }

    set lVersion [mql get env MXVERSIONLIST]
    if {"$lVersion" != ""} {
        set nProgramVersionIndex [lsearch $lVersion $sProgramVersion]
        if {$nProgramVersionIndex == -1} {
            return 1
        }

        set nInstallVersionIndex [lsearch $lVersion $sInstallVersion]

        if { $nProgramVersionIndex < $nInstallVersionIndex } {
            return 0
        } elseif { $nProgramVersionIndex > $nInstallVersionIndex } {
            return 1
        }
    } else {
        set lProgramVersion       [split $sProgramVersion -]
        set lInstallVersion       [split $sInstallVersion -]
        set nProgramVersionDigits [llength $lProgramVersion ]
        set nInstallVersionDigits [llength $lInstallVersion ]

        if { $nProgramVersionDigits < $nInstallVersionDigits } {
            return 0
        } elseif { $nProgramVersionDigits > $nInstallVersionDigits } {
            return 1
        }

        for { set nDigit 0 } { $nDigit < $nProgramVersionDigits } { incr nDigit } {
            set sProgramDigit [lindex $lProgramVersion $nDigit]
            set sInstallDigit [lindex $lInstallVersion $nDigit]
            if { $sProgramDigit < $sInstallDigit } {
                return 0
            } elseif { $sProgramDigit > $sInstallDigit } {
                return 1
            }
        }
    }

    return 0
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceRegisterFeatures { lCmdList ItemType } {

    set sProgName "eServiceRegisterFeatures"

    set sErrorMsg       ""
    set sPropName       ""

    foreach lElement $lCmdList {

             set sExistProp     [split [mql list property on program "$ItemType"] "\n"]
             set lExistProgs    [split [mql list program] "\n"]
             set iFileFound     0
             # check for correct format of input list being processed
             if {[llength $lElement] < 3} {
                puts stdout ">ERROR       : eServiceRegisterFeatures"
                puts stdout ">$lElement - Incorrect format"
                continue
             }

             set mqlret 0
             set sOperation      [ string trim [lindex $lElement 0 ]]
             set sFeatureName    [ string trim [lindex $lElement 1 ]]
             set sMenuName       [ string trim [lindex $lElement 2 ]]
             set sLangFileList   [ lrange $lElement 3 end]

             foreach lSubItem $sExistProp {
                     set lSubItem [split [string trim $lSubItem]]
                     set sIndex [lsearch -exact $lSubItem $sFeatureName]
                     if {$sIndex > 0} {
                        set iFileFound  1
                        break
                     }
             }

             if {[string compare $sOperation "ADD"] == 0} {

                    # Feature placeholder already exists in database
                    if {$iFileFound == 1} {
                       puts stdout ">Exists      : placeholder  $sFeatureName"
                       set lPropOnFeatHold   [split [mql list property on program "$sFeatureName"] "\n"]
                       foreach {sLangType sLangFileName} $sLangFileList {
                             set iPageAttach    0
                             set sLangType      [string trim $sLangType]
                             set sLangFileName  [string trim $sLangFileName]

                             # check for the existence of page object in database
                             if {($sLangFileName != "") && ([lsearch -exact $lExistProgs $sLangFileName] != -1)} {
                                set sPropName "eServicePage${sLangType}"

                                # check if the Page object is already connected to feature place holder
                                # through the eServicePage property
                                foreach lFeatElement $lPropOnFeatHold {
                                   set lFeatElement [split [string trim $lFeatElement]]
                                   set iFileIndex   [lsearch -exact $lFeatElement $sLangFileName]
                                   if {$iFileIndex > 0} {
                                       set iPageAttach 1
                                       break
                                   }
                                }

                                # if already not connected to feature placeholder,
                                # connect it
                                if {$iPageAttach == 0} {
                                   if {$mqlret == 0} {
                                      set sCmd {mql add property "$sPropName" \
                                                        on program "$sFeatureName" \
                                                        to program "$sLangFileName" \
                                                        }
                                      set mqlret [ catch {eval $sCmd} outStr ]
                                   }
                                }
                             }
                       }
                    } else {
                       if {$mqlret == 0} {
                          puts stdout ">Adding      : placeholder  $sFeatureName"
                          set sCmd {mql add program "$sFeatureName" \
                                        mql \
                                        hidden \
                                        }
                          set mqlret [ catch {eval $sCmd} outStr ]
                       }
                       if {$mqlret == 0} {
                          set sCmd {mql add property "eServiceFeature" \
                                            on program "$ItemType" \
                                            to program "$sFeatureName" \
                                            value "$sMenuName" \
                                            }
                          set mqlret [ catch {eval $sCmd} outStr ]
                       }
                       if {$mqlret == 0} {
                          foreach {sLangType sLangFileName} $sLangFileList {
                             set sLangType      [string trim $sLangType]
                             set sLangFileName  [string trim $sLangFileName]
                             if {($sLangFileName != "") && ([lsearch -exact $lExistProgs $sLangFileName] != -1)} {
                                set sPropName "eServicePage${sLangType}"
                                if {$mqlret == 0} {
                                   set sCmd {mql add property "$sPropName" \
                                                     on program "$sFeatureName" \
                                                     to program "$sLangFileName" \
                                                     }
                                   set mqlret [ catch {eval $sCmd} outStr ]
                                }
                             }
                          }
                       }
                    }
             } elseif {[string compare $sOperation "DEL"] == 0} {
                if {$iFileFound == 0} {
                     continue
                } else {
                     set sCmd {mql delete property "eServiceFeature" \
                                          on program "$ItemType" \
                                          to program "$sFeatureName" \
                                          }
                     set mqlret [ catch {eval $sCmd} outStr ]
                }
             }

             if {$mqlret != 0} {
                 puts stdout ">ERROR       : eServiceRegisterFeatures"
                 puts stdout ">$sCmd"
                 puts stdout ">$outStr"
                 append sErrorMsg "$outStr\n"
             }
    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {
          return 1
    }

    return 0
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceRegisterPages { lCmdList ItemType } {

    set sProgName "eServiceRegisterPages"

    set iPropertyFound  0
    set sErrorMsg       ""
    set sExistRoles     [split [mql list "$ItemType"] "\n"]
    set RegProgName     [mql get env REGISTRATIONOBJECT]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {


             # check for correct format of input list being processed
             if {[llength $lElement] < 3} {
                puts stdout ">ERROR       : eServiceRegisterPages"
                puts stdout ">Incorrect format"
                puts stdout ">$lElement"
                 continue
             }

             set sOperation      [ string trim [lindex $lElement 0 ]]
             set sFileName       [ string trim [lindex $lElement 1 ]]
             set lRoleList       [lrange $lElement 2 end]

       foreach sRoleName $lRoleList {

             set sRoleName       [ string trim [ set [ string trim $sRoleName]]]
             set iRoleFound      0
             set mqlret          0
             set iPropertyFound  0

             foreach lSubItem $sExistRoles {
                     set lSubItem [string trim $lSubItem]
                     if {[string compare "$sRoleName" "$lSubItem"] == 0} {
                         set iRoleFound 1
                         break
                     }
             }

             if {$iRoleFound == 0} {
                 puts stdout ">ERROR       : eServiceRegisterPages"
                 puts stdout ">$sRoleName - role not found"
                 append sErrorMsg "$sRoleName - role not found\n"
                 continue
             }

             set lRolePropList   [split [mql list property on role $sRoleName] "\n"]
             foreach lRoleElement $lRolePropList {

                     set lRoleElement [split $lRoleElement]
                     set iPropIndex   [lsearch -exact $lRoleElement "eServiceFeatureAccess"]
                     if {$iPropIndex == 0} {
                        set sIndex    [lsearch -exact $lRoleElement "$sFileName"]
                        if {$sIndex > 0} {
                           set iPropertyFound 1
                           break
                        }
                     }
             }
             if {[string compare $sOperation "ADD"] == 0} {

                    if {$iPropertyFound == 1} {
                       puts stdout [format ">Exists      : %-13s%-54s%-50s" property  $sRoleName $sFileName]
                    } else {
                       puts stdout [format ">Adding      : %-13s%-54s%-50s" property  $sRoleName $sFileName]
                       set sCmd {mql add property "eServiceFeatureAccess" \
                                         on role "$sRoleName" \
                                         to program "$sFileName" \
                                         }
                       set mqlret [ catch {eval $sCmd} outStr ]

                    }

             } elseif {[string compare $sOperation "DEL"] == 0} {

                if {$iPropertyFound == 1} {

                     puts stdout [format ">Deleting    : %-13s%-54s%-50s" property  $sRoleName $sFileName]
                     set sCmd {mql delete property "eServiceFeatureAccess" \
                                          on role "$sRoleName" \
                                          to program "$sFileName" \
                                          }
                     set mqlret [ catch {eval $sCmd} outStr ]

                }

             }

             if {$mqlret != 0} {
                 puts stdout ">ERROR       : eServiceRegisterPages"
                 puts stdout ">$outStr"
                 append sErrorMsg "$outStr\n"
             }
        }
    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {
              return 1
    }

    return 0
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceModifyMethod { lCmdList ItemType } {

    set sProgName "eServiceModifyMethod"

    set sErrorMsg       ""
    set DUMPDELIMITER   "|"
    set RegProgName     [mql get env REGISTRATIONOBJECT]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

             # check for correct format of input list being processed
             if {[llength $lElement] < 3} {
                puts stdout ">ERROR       : eServiceModifyMethod"
                    puts stdout ">$lElement Incorrect format"
                 continue
             }

             set mqlret          0
             set iMethodFound    ""
             set sOperation      [ string trim [lindex $lElement 0 ]]
             set sTypeName       [ string trim [ set [ string trim [lindex $lElement 1 ]]]]
             set sMethodName     [ string trim [lindex $lElement 2 ]]

             set sPrefixMethod   [ string trim [lindex [split $sMethodName "_"] 0]]

             if {([string compare $sPrefixMethod "wizard"] == 0) || ([string compare $sPrefixMethod "program"] == 0)} {
                set sMethodName  [ string trim [ set [ string trim "$sMethodName"]]]
             }
             set sCmd {mql print type "$sTypeName" select method dump $DUMPDELIMITER}
             set mqlret [ catch {eval $sCmd} outStr ]

             if {$mqlret == 0} {
                 if {$outStr != ""} {
                    set lMethodList [split $outStr $DUMPDELIMITER]
                 } else {
                    set lMethodList ""
                 }
             }

             if {$mqlret == 0} {
                set iMethodFound [lsearch -exact $lMethodList "$sMethodName"]

                if {[string compare $sOperation "ADD"] == 0} {

                    if {$iMethodFound >= 0} {
                       puts stdout [format ">Exists      : %-13s%-54s%-15s" "Method" $sTypeName  $sMethodName]
                       set iMethodFound ""
                       continue
                    } else {

                       puts stdout [format ">Adding      : %-13s%-54s%-15s" "Method"  $sTypeName  $sMethodName]
                       set sCmd {mql modify type "$sTypeName"  add \
                                            method "$sMethodName" \
                                            }
                       set mqlret [ catch {eval $sCmd} outStr ]
                    }

               } elseif {[string compare $sOperation "DEL"] == 0} {

                    if {$iMethodFound == -1} {
                        set iMethodFound ""
                        continue
                    } else {
                        puts stdout [format ">Removing    : %-13s%-54s%-15s" "Method"  $sTypeName  $sMethodName]
                        set sCmd {mql modify type "$sTypeName"  remove \
                                             method "$sMethodName" \
                                          }
                        set mqlret [ catch {eval $sCmd} outStr ]
                    }
               }
             }

             if {$mqlret != 0} {
                 puts stdout ">ERROR       : eServiceModifyMethod"
                     puts stdout ">$outStr"
                 append sErrorMsg "$outStr\n"
             }

    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {

              return 1
    }

    return 0
}

################################################################################

proc eServiceLookupStateName {sAbsPolicyName sSymbolicStateName} {

      set sAbsPolicyName      [string trim "$sAbsPolicyName"]
      set sSymbolicStateName  [string trim "$sSymbolicStateName"]

      set sPrefixPolicy       [string trim [lindex [split $sAbsPolicyName "_"] 0]]
      set sPrefixState        [string trim [lindex [split $sSymbolicStateName "_"] 0]]

      if {[string compare "$sPrefixPolicy" "policy"] == 0} {
         puts stdout "eServiceLookupStateName: policy name cannot be a symbolic name"
         return ""
      }
      if {[string compare "$sPrefixState" "state"] != 0} {
         puts stdout "eServiceLookupStateName: state name cannot be an absolute name"
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
                               puts stdout "eServiceLookupStateName: Incorrect format for properties on policy"
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
         puts stdout "eServiceLookupStateName: specified policy name does not exist"
         return ""
      }
      return ""
}
# end of eServiceGetCurrentSchemaName proc

################################################################################

proc eServiceInstallPerson { lCmdList ItemType } {

    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]

    set sErrorMsg            ""

    set RegProgName          [mql get env REGISTRATIONOBJECT]
    set sStateActv           [eServiceGetCurrentSchemaName state $RegProgName policy_Person state_Active]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

             # check for correct format of input list being processed
             if {[llength $lElement] != 2} {

                puts stdout ">ERROR       : eServiceInstallPerson"
                    puts stdout ">$lElement - Incorrect format"
                 continue
             }

             set lExistPerson    [split [mql list "$ItemType"] "\n"]
             set mqlret          0
             set sPersonName     [ string trim [lindex $lElement 0 ]]
             set sMqlCommand     [ string trim [lindex $lElement 1 ]]

             set iPersonIndx     [lsearch -exact $lExistPerson "$sPersonName"]

             if {$iPersonIndx >= 0} {
                puts stdout [format ">Exists      : %-13s%-54s" $ItemType  "$sPersonName"]
                set bPersonExist 1

             } else {
                puts stdout [format ">Adding      : %-13s%-54s" $ItemType  "$sPersonName"]
                set bPersonExist 0
             }

             set sCmd {mql print bus "$type_Person" "$sPersonName" "-" select exists dump }
             set mqlret [ catch {eval $sCmd} outStr ]
             if {$mqlret != 0} {
                 puts stdout ">ERROR       : eServiceInstallPerson"
                 puts stdout ">$sCmd"
                     puts stdout ">$outStr"
                 append sErrorMsg "$outStr\n"
                 continue
             }

             if {$outStr == "TRUE"} {
                 set bPersonBusExist 1
                 puts stdout [format ">Exists      : %-13s%-40s%-14s%-s" $type_Person  "$sPersonName" "-" "Business Object"]
                 set sCmd {mql print bus "$type_Person" "$sPersonName" "-" select current dump }
                 set mqlret [ catch {eval $sCmd} outStr ]
                 if {$mqlret != 0} {
                     puts stdout ">ERROR       : eServiceInstallPerson"
                     puts stdout ">$sCmd"
                         puts stdout ">$outStr"
                     append sErrorMsg "$outStr\n"
                     continue
                 }
                 if {"$outStr" == "$sStateActv"} {
                     set bPersonBusPromoted 1
                 } else {
                     set bPersonBusPromoted 0
                 }

                 set sCmd {mql expand businessobject "$type_Person" "$sPersonName" "-" \
                                      relationship "$relationship_Employee" \
                                      select bus dump |
                          }
                 set mqlret [ catch {eval $sCmd} outStr ]
                 if {$mqlret != 0} {
                     puts stdout ">ERROR       : eServiceInstallPerson"
                     puts stdout ">$sCmd"
                         puts stdout ">$outStr"
                     append sErrorMsg "$outStr\n"
                     continue
                 }
                 set lObjs [split $outStr \n]
                 set bPersonBusConnected 0
                 foreach sObj $lObjs {
                     set lItems [split $sObj |]
                     set sType  [lindex $lItems 3]
                     set sName  [lindex $lItems 4]
                     set sRev   [lindex $lItems 5]
                     if {"$sType" == "$type_Company" && "$sName" == "$role_CompanyName" && "$sRev" == "-"} {
                         set bPersonBusConnected 1
                         break
                     }
                 }
             } else {
                 set bPersonBusExist 0
                 set bPersonBusConnected 0
                 set bPersonBusPromoted 0
                 puts stdout [format ">Adding      : %-13s%-40s%-14s%-s" $type_Person  "$sPersonName" "-" "Business Object"]
             }

             if {$bPersonExist == 0} {
                 set mqlret [ catch {eval $sMqlCommand} outStr ]
                 if {$mqlret != 0} {
                    puts stdout ">ERROR       : eServiceInstallPerson"
                    puts stdout ">$sMqlCommand"
                        puts stdout ">$outStr"
                    append sErrorMsg "$outStr\n"
                    continue
                 }
             }

             if {$bPersonBusExist == 0} {
                 set lName [split $sPersonName " "]
                 set sFirstName ""
                 set sLastName ""
                 set sMiddleName ""
                 set nCount [llength $lName]
                 switch -exact $nCount {

                     1 {
                         set sFirstName [lindex $lName 0]
                     }
                     2 {
                         set sFirstName [lindex $lName 0]
                         set sLastName [lindex $lName 1]
                     }
                     default {
                         set sFirstName [lindex $lName 0]
                         set sMiddleName [lindex $lName 1]
                         set sLastName [lindex $lName 2]
                     }
                 }
                 set lPersonName [split "$sPersonName"]
                 set sEmailName [join "$lPersonName" ""]
                 set lCompanyName [split "$role_CompanyName"]
                 set sWebName [join "$lCompanyName" ""]
                 set sCmd {mql add businessobject "$type_Person" "$sPersonName" "-" \
                                   description "Person Object for user $sPersonName" \
                                   vault "$vault_eServiceSample" \
                                   owner "$sPersonName" \
                                   policy "$policy_Person" \
                                   "$attribute_LastName" "$sLastName" \
                                   "$attribute_FirstName" "$sFirstName" \
                                   "$attribute_MiddleName" "$sMiddleName" \
                                   "$attribute_WorkPhoneNumber" "(123)322-2004" \
                                   "$attribute_HomePhoneNumber" "(123)456-7890" \
                                   "$attribute_EmailAddress" "${sEmailName}@${sWebName}.com" \
                                   "$attribute_WebSite" "http://www.$sWebName.com" \
                                   "$attribute_HostMeetings" "Yes" \
                          }
                 set mqlret [ catch {eval $sCmd} outStr ]
                 if {$mqlret != 0} {
                     puts stdout ">ERROR       : eServiceInstallPerson"
                     puts stdout ">$sCmd"
                         puts stdout ">$outStr"
                     append sErrorMsg "$outStr\n"
                     continue
                 }
             }

             if {$bPersonBusPromoted == 0} {
                 puts stdout [format ">Promoting   : %-13s%-40s%-14s%-s" $type_Person  "$sPersonName" "-" "Business Object to Active"]
                 set sCmd {mql promote businessobject "$type_Person" "$sPersonName" "-"}
                 set mqlret [ catch {eval $sCmd} outStr ]
                 if {$mqlret != 0} {
                     puts stdout ">ERROR       : eServiceInstallPerson"
                     puts stdout ">$sCmd"
                         puts stdout ">$outStr"
                     append sErrorMsg "$outStr\n"
                     continue
                 }
             }

             if {$bPersonBusConnected == 0} {
                 set sCmd {mql print person "$sPersonName" select assignment dump |}
                 set mqlret [ catch {eval $sCmd} outStr ]
                 if {$mqlret != 0} {
                     puts stdout ">ERROR       : eServiceInstallPerson"
                     puts stdout ">$sCmd"
                         puts stdout ">$outStr"
                     append sErrorMsg "$outStr\n"
                     continue
                 }
                 set lAssignments [split $outStr |]
                 if {[lsearch -exact $lAssignments "$role_Supplier"] >= 0 || \
                     [lsearch -exact $lAssignments "$role_Customer"] >= 0 || \
                     [lsearch -exact $lAssignments "$role_SupplierRepresentative"] >= 0 || \
                     [lsearch -exact $lAssignments "$role_CustomerRepresentative"] >= 0 || \
                     [lsearch -exact $lAssignments "$role_SupplierEngineer"] >= 0} {

                     continue
                 }

                 puts stdout [format ">Adding      : %-13s%-40s%-14s%-s" $type_Person  "$sPersonName" "-" "Business Object Connection"]
                 set sCmd {mql connect businessobject "$type_Company" "$role_CompanyName" "-" \
                                   relationship "$relationship_Employee" \
                                   to "$type_Person" "$sPersonName" "-" \
                          }
                 set mqlret [ catch {eval $sCmd} outStr ]
                 if {$mqlret != 0} {
                     puts stdout ">ERROR       : eServiceInstallPerson"
                     puts stdout ">$sCmd"
                         puts stdout ">$outStr"
                     append sErrorMsg "$outStr\n"
                     continue
                 }
             } else {
                 puts stdout [format ">Exists      : %-13s%-40s%-14s%-s" $type_Person  "$sPersonName" "-" "Business Object Connection"]
             }
    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {
              return 1
    }

    return 0
}

################################################################################

proc eServiceApplicationGetVersion { sProgramName sPropertyName } {

    set sProgName "eServiceApplicationGetVersion"
    set lVersions [mql get env MXVERSIONLIST]
    set sProgramName [string trim $sProgramName]
    set sPropertyName [string trim $sPropertyName]

    set sAppPropNamePrefixed "appVersion${sPropertyName}"
    set sFeaturePropNamePrefixed "featureVersion${sPropertyName}"

    if {$sProgramName == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - Program Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return ""
    }

    if {$sPropertyName == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - property Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return ""
    }

    # Check if program exists and if it doesn't then return blank
    set sCmd {mql list program "$sProgramName"}
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0 || "$outstr" != "$sProgramName"} {
        return ""
    }

    # Get code , all the properties and their values on the program
    set sCmd {mql print program "$sProgramName" select \
                        code \
                        property.name \
                        property.value \
                        dump | \
                        }
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        return ""
    }

    set lOutput [split $outstr |]
    set sCode [lindex $lOutput 0]
    set lPropInfo [lrange $lOutput 1 end]
    set lPropNames [lrange $lPropInfo 0 [expr [llength $lPropInfo] / 2 - 1]]
    set lPropValues [lrange $lPropInfo [expr [llength $lPropInfo] / 2] end]
    set nCounter 0
    set sPropValue ""

    # check if version conversion executed
    # if not then no need to append application name by appVersion.
    if {[lsearch $lPropNames "Conversion8-0-1-0ApplicationVersion"] == -1} {
        set sAppPropNamePrefixed "$sPropertyName"
        set sFeaturePropNamePrefixed "$sPropertyName"
    }

    foreach sPropName $lPropNames {
        if {"$sPropName" == "$sAppPropNamePrefixed" || "$sPropName" == "$sFeaturePropNamePrefixed"} {

            # Get all the versions that are installed.
            set lAppVersions {}
            set lCode [split $sCode \n]
            foreach sLine $lCode {
                set sLine [string trim "$sLine"]
                if {[string length "$sLine"] == 0 || [string range "$sLine" 0 0] == "#"} {
                    continue
                }

                if {[lindex "$sLine" 0] == "$sPropertyName"} {
                    lappend lAppVersions [lindex "$sLine" 1]
                }
            }

            set sPropValue [lindex $lPropValues $nCounter]

            # Check if current build has information about installed build
            # If no then get version from list of versions in log
            # and return highest possible version for which current build has information.
            if {[llength $lAppVersions] != 0 && [lsearch $lVersions "$sPropValue"] == -1} {
                for {set i [expr [llength $lAppVersions] - 1]} {$i >= 0} {set i [expr $i - 1]} {
                    set sAppVersion [lindex $lAppVersions $i]
                    if {[lsearch $lVersions "$sAppVersion"] >= 0} {
                        set sPropValue "$sAppVersion"
                        break
                    }
                }
            }
            if {"$sCode" == ""} {
                set sCode "# Application Install Log\n"
                append sCode "# The Log should not be changed manually\n"
                append sCode "# It shows history of applications installed\n"
                append sCode "# The log consists of three fields separated by spaces as shown\n"
                append sCode "# ApplicationName Version DateOfInstallation\n"
            }
            if {[llength $lAppVersions] == 0} {
                append sCode "\n$sPropertyName $sPropValue UNKNOWN"
                set sCmd {mql modify program "$sProgramName" \
                                     code "$sCode" \
                                     }
                set mqlret [catch {eval $sCmd} outstr]
                if {$mqlret != 0} {
                    break
                }
            }
            break
        }
        incr nCounter
    }

    return "$sPropValue"
}

################################################################################

proc eServiceApplicationSetVersion { sProgramName sPropertyName sVersion } {

    set sProgName "eServiceApplicationSetVersion"

    set sProgramName         [string trim $sProgramName]
    set sPropertyName        [string trim $sPropertyName]
    set sVersion             [string trim $sVersion]
    set sMode                [mql get env MXMODE]
    set sInstallDir          [mql get env MXAPPBUILD]
    set sModuleType          [mql get env MX_MODULE_TYPE]
    set sNightly             [mql get env MXNIGHTLY]

    if {"$sModuleType" == "" || "$sModuleType" == "APPLICATION"} {
        set sPropNamePrefixed "appVersion${sPropertyName}"
        set sPropNameInstallType "appInstallType${sPropertyName}"
        set sPropNameNightly "appNightly${sPropertyName}"
    } else {
        set sPropNamePrefixed "featureVersion${sPropertyName}"
        set sPropNameInstallType "featureInstallType${sPropertyName}"
        set sPropNameNightly "featureNightly${sPropertyName}"
    }
    set sDate [clock format [clock seconds] -format "%m-%d-%Y %I:%M %p"]

    if {$sProgramName == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - Program Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return 1
    }

    if {$sPropertyName == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - property Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return 1
    }

    if {$sVersion == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - version Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return 1
    }

    set mqlret               0
    set outstr               ""
    set iPropertyFound       0

    if {"$sMode" == "HOT_FIX"} {
        set sCmd {mql modify program "$sProgramName" \
                      property "appHF${sPropertyName}_${sVersion}" value "installed" \
                      }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
           puts stdout ">ERROR       : eServiceApplicationSetVersion"
           puts stdout ">$sCmd"
           puts stdout ">$outstr"
           return 1
        }

    } else {

        # Get code of system information program
        set sCmd {mql print program "$sProgramName" select code dump |}
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret != 0} {
           puts stdout ">ERROR       : eServiceApplicationSetVersion"
           puts stdout ">$sCmd"
           puts stdout ">$outstr"
           return 1
        }

        set sCode "$outstr"
        # append version log in code
        if {"$sCode" == ""} {
            set sCode "# Application Install Log\n"
            append sCode "# The Log should not be changed manually\n"
            append sCode "# It shows history of applications installed\n"
            append sCode "# The log consists of three fields separated by spaces as shown\n"
            append sCode "# ApplicationName Version DateOfInstallation TypeOfInstall\n"
        }
        if {"$sMode" == "SERVICE_PACK"} {
            set sCode "$sCode\n$sPropertyName $sVersion $sDate SERVICE_PACK"
            set sInstallType "SERVICE_PACK"
        } else {
            set sCode "$sCode\n$sPropertyName $sVersion $sDate FULL_BUILD"
            set sInstallType "FULL_BUILD"
        }

        # Conversion from feature to application
        # application to feature
        set sRemoveCmd ""
        if {"$sModuleType" == "" || "$sModuleType" == "APPLICATION"} {
            set sRemovePropVersion "featureVersion${sPropertyName}"
            set sRemovePropInstallType "featureInstallType${sPropertyName}"
            set sRemovePropNightly "featureNightly${sPropertyName}"
        } else {
            set sRemovePropVersion "appVersion${sPropertyName}"
            set sRemovePropInstallType "appInstallType${sPropertyName}"
            set sRemovePropNightly "appNightly${sPropertyName}"
        }
        set sCmd {mql print program "$sProgramName" \
                            select property\[$sRemovePropVersion\].value dump |
                 }
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret != 0} {
           puts stdout ">ERROR       : eServiceApplicationSetVersion"
           puts stdout ">$sCmd"
           puts stdout ">$outstr"
           return 1
        }
        if {"$outstr" != ""} {
            set sRemoveCmd " remove property \"$sRemovePropVersion\" remove property \"$sRemovePropInstallType\" remove property \"$sRemovePropNightly\""
        }
        
        if {"$sNightly" == ""} {
            set sCmd {mql modify program "$sProgramName" \
                          property "$sPropNamePrefixed" value "$sVersion" \
                          property "$sPropNameInstallType" value "$sInstallType" \
                          remove property "$sPropNameNightly" \
                          code "$sCode" \
                     }
        } else {
            set sCmd {mql modify program "$sProgramName" \
                          property "$sPropNamePrefixed" value "$sVersion" \
                          property "$sPropNameInstallType" value "$sInstallType" \
                          property "$sPropNameNightly" value "$sNightly" \
                          code "$sCode" \
                     }
        }
        append sCmd "$sRemoveCmd"
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret != 0} {
           puts stdout ">ERROR       : eServiceApplicationSetVersion"
           puts stdout ">$sCmd"
           puts stdout ">$outstr"
           return 1
        }
        
        set sCmd {mql print context select tenant dump}
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret != 0} {
           puts stdout ">ERROR       : eServiceApplicationSetVersion"
           puts stdout ">$sCmd"
           puts stdout ">$outstr"
           return 1
        }
        
        if {"$outstr" == ""} {
            setSTAGINGVersion "$sInstallDir" "$sPropNamePrefixed" "$sVersion"
        }

    }

    return 0
}

proc setSTAGINGVersion {installDir propName version} {
    set fileName [file join "$installDir" ".." ".." ".." ".." ".." "STAGING" "ematrix" "installAppVersionsList.txt"]
    if {[file exists "$fileName"] == 0} {
        set file [open "$fileName" w+]
        puts $file "    property\[${propName}\].value = ${version}"
        close $file
    } else {
        set temp ""
        #saves each line to an arg in a temp list
        set file [open $fileName]
        foreach {line} [split [read $file] \n] {
            if {[string trim $line] != ""} {
                lappend temp $line
            }
        }
        close $file

        #rewrites your file
        set file [open ${fileName}.temp w+]
        set found "false"
        foreach {line} $temp {
            set key [string trim [lindex [split [string trim "$line"] "="] 0]]
            set value [string trim [lindex [split [string trim "$line"] "="] 1]]

            if {[string trim "$key"] == "property\[${propName}\].value"} {
                set line [string map [list "$value" "$version"] "$line"]
                set found "true"
            }
            
            puts $file "$line"
        }
        if {"$found" == "false"} {
            puts $file "    property\[${propName}\].value = ${version}"
        }
        close $file
        file delete -force "$fileName"
        file rename -force "${fileName}.temp"  "$fileName"
    }
}

##################################################################################

proc eServiceAddSampleBusObject { lCmdList } {

    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]
    set sAdminUser           [mql get env global eServiceAdmin]
    set sAdminPwd            [mql get env global eServiceAdminPwd]
    set sCreatorPwd          [mql get env global eServiceCreatorPwd]
    set sErrorMsg            ""

    set RegProgName          [mql get env REGISTRATIONOBJECT]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

            # check for correct format of input list being processed
             if {[llength $lElement] < 2} {

                puts stdout ">ERROR       : eServiceAddSampleBusObject"
                puts stdout ">Incorrect format"
                puts stdout ">$lElement"
                 continue
             }

            set sElementDesc   [lindex $lElement 0]
            set lExecElement   [lrange $lElement 1 end]
            puts stdout [format ">Adding      : %-67s" $sElementDesc]

            foreach lSubElement $lExecElement {
                    set mqlret [ catch {eval $lSubElement} outStr ]
                    if {$mqlret != 0} {
                       puts stdout ">ERROR       : eServiceAddSampleBusObject"
                       puts stdout ">$lSubElement"
                           puts stdout ">$outStr"
                       append sErrorMsg "$outStr\n"
                    }
            }
    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {

              return 1
    }

    return 0
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceAddAdministrationBusObject { lCmdList sAppName } {


    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]
    set sAdminUser           [mql get env global eServiceAdmin]
    set sAdminPwd            [mql get env global eServiceAdminPwd]
    set sCreatorPwd          [mql get env global eServiceCreatorPwd]
    set sErrorMsg            ""

    set RegProgName          [mql get env REGISTRATIONOBJECT]
    set sVerProgName         [string trim [mql get env global VERSIONREGISTRATIONOBJECT]]

    set sAppVersion          [string trim [eServiceApplicationGetVersion $sVerProgName "$sAppName"]]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

             # check for correct format of input list being processed
             if {[llength $lElement] < 3} {

                puts stdout ">ERROR       : eServiceAddAdministrationBusObject"
                    puts stdout ">$lElement - Incorrect format"
                 continue
             }

             set lIndx          0
             set sCmdVersion    [ lindex $lElement $lIndx]
             set ItemDesc       [ lindex $lElement [incr lIndx 1]]
              set ItemExec       [ lindex $lElement [incr lIndx 1]]

             while {$ItemExec != ""} {
                   if { $sAppVersion == "" || [eServiceVersionCompare "$sAppVersion" "$sCmdVersion"] == 0} {

                      puts stdout [format ">Updating    : %-67s%-15s" $ItemDesc  $sCmdVersion]

                      foreach lSubElement $ItemExec {
                              set mqlret [ catch {eval $lSubElement} outStr]
                              if {$mqlret != 0} {
                                  puts stdout ">ERROR       : eServiceAddAdministrationBusObject"
                                  puts stdout ">$lSubElement"
                                      puts stdout ">$outStr"
                                  append sErrorMsg "$outStr\n"
                              }
                      }
                      incr lIndx
                      set sCmdVersion    [ lindex $lElement $lIndx ]
                      incr lIndx
                      set ItemDesc       [ lindex $lElement $lIndx ]
                      incr lIndx
                      set ItemExec       [ lindex $lElement $lIndx ]
                   }
             }

    }

    if { $sErrorMsg != "" } {

              return 1
    }
    return 0
}

################################################################################

proc eServiceInstallWorkspace { lCmdList sAppName } {


    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]
    set sAdminUser           [mql get env global eServiceAdmin]
    set sAdminPwd            [mql get env global eServiceAdminPwd]
    set sCreatorPwd          [mql get env global eServiceCreatorPwd]
    set sErrorMsg            ""

    set RegProgName          [mql get env REGISTRATIONOBJECT]
    set sVerProgName         [string trim [mql get env global VERSIONREGISTRATIONOBJECT]]

    set sAppVersion          [string trim [eServiceApplicationGetVersion $sVerProgName "$sAppName"]]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

             # check for correct format of input list being processed
             if {[llength $lElement] < 3} {

                puts stdout ">ERROR       : eServiceInstallWorkspace"
                    puts stdout ">$lElement - Incorrect format"
                 continue
             }

             set lIndx          0
             set sCmdVersion    [ lindex $lElement $lIndx]
             set ItemDesc       [ lindex $lElement [incr lIndx 1]]
              set ItemExec       [ lindex $lElement [incr lIndx 1]]

             while {$ItemExec != ""} {
                   if { $sAppVersion == "" || [eServiceVersionCompare "$sAppVersion" "$sCmdVersion"] == 0} {
                      puts stdout [format ">Updating    : %-13s%-54s%-15s" "workspace" $ItemDesc $sCmdVersion]
                      foreach lSubElement $ItemExec {
                              set mqlret [ catch {eval $lSubElement} outStr]
                              if {$mqlret != 0} {
                                  puts stdout ">ERROR       : eServiceInstallWorkspace"
                                  puts stdout ">$lSubElement"
                                      puts stdout ">$outStr"
                                  append sErrorMsg "$outStr\n"
                              }
                      }
                      incr lIndx
                      set sCmdVersion    [ lindex $lElement $lIndx ]
                      incr lIndx
                      set ItemDesc       [ lindex $lElement $lIndx ]
                      incr lIndx
                      set ItemExec       [ lindex $lElement $lIndx ]
                   }
             }
    }

    if { $sErrorMsg != "" } {

              return 1
    }
    return 0
}

################################################################################

proc eServiceRunInstallPrograms { lPrograms sApplication } {

    set progname "eServiceRunInstallPrograms"
    set sModuleType [mql get env MX_MODULE_TYPE]
    set mqlall 0
    mql set env MXERRORFLAG "FALSE"
    set sFullDir [mql get env MXAPPBUILD]
    set bStatus [mql get env MXSTATUS]
    set nCount [llength $lPrograms]
    if { $bStatus == "TRUE"} {
        file delete -force "$sFullDir/SetupAbort=status"
        set mqlret [catch {open "$sFullDir/NoOfFiles=$nCount=$sApplication=status" {WRONLY CREAT TRUNC} 0666} fieldId]
        if { $mqlret != 0 } {
            puts stdout "Error ($progname): Could not update status"
            return 1
        }
        close $fieldId
    }

    set nCount 0
    foreach sProgram $lPrograms {
        if {$bStatus == "TRUE" && [file exists "$sFullDir/SetupAbort=status"] == 1} {
            set mqlall 1
            break;
        }
        set lTemp [split $sProgram "/"]
        set nTemp [expr {[llength $lTemp] - 1}]
        set sTemp [lindex $lTemp $nTemp]
        incr nCount
        if { $bStatus == "TRUE"} {
            set mqlret [catch {open "$sFullDir/$nCount=$sTemp=$sApplication=status" {WRONLY CREAT TRUNC} 0666} fieldId]
            if { $mqlret != 0 } {
                puts stdout "Error ($progname): Could not update status"
                return 1
            }
            close $fieldId
        }
        puts stdout ""
        puts stdout "Running $sTemp"

        set mqlret [catch {mql run \"$sProgram\" } outStr ]
        if { $mqlret != 0 } {
            set mqlall $mqlret
            mql set env MXERRORFLAG "TRUE"
            if {$outStr != ""} {
                puts stdout "Error ($progname): $outStr"
            }
        }
    }

    if { $bStatus == "TRUE"} {
        if { $mqlall == 0 } {
            set mqlret [catch {open "$sFullDir/Result=Success=$sApplication=status" {WRONLY CREAT TRUNC} 0666} fieldId]
        } else {
            set mqlret [catch {open "$sFullDir/Result=Failure=$sApplication=status" {WRONLY CREAT TRUNC} 0666} fieldId]
        }
        if { $mqlret != 0 } {
            puts stdout "Error ($progname): Could not update status"
            return 1
        }
        close $fieldId
    }

    return $mqlall
}

################################################################################


proc eServiceInstallGroup { lCmdList ItemType } {

    set sFullDir             [mql get env MXAPPBUILD]
    set sInstallVersion      [mql get env MXVERSION]
    set sApplication         [mql get env MXAPPLICATION]
    set sInstallOrg          [mql get env MXORGNAME]
    set sDate                [clock format [clock seconds] -format %m-%d-%Y]

    set sErrorMsg            ""

    set RegProgName          [mql get env REGISTRATIONOBJECT]

    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $RegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $RegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {

                     set lGlobalElement [split $lGlobalElement]
                     set sPropertyName  [ lindex $lGlobalElement 0]
                     catch {global "$sPropertyName"}
    }

    foreach lElement $lCmdList {

             # check for correct format of input list being processed
             if {[llength $lElement] != 2} {

                puts stdout ">ERROR       : eServiceInstallGroup"
                    puts stdout ">$lElement - Incorrect format"
                 continue
             }

             set lExistGroup     [split [mql list "$ItemType"] "\n"]
             set mqlret          0
             set sGroupName      [ string trim [lindex $lElement 0 ]]
             set sMqlCommand     [ string trim [lindex $lElement 1 ]]

             set iPersonIndx     [lsearch -exact $lExistGroup "$sGroupName"]

             if {$iPersonIndx >= 0} {
                puts stdout [format ">Exists      : %-13s%-54s" $ItemType  "$sGroupName"]

             } else {
                puts stdout [format ">Adding      : %-13s%-54s" $ItemType  "$sGroupName"]
                set mqlret [ catch {eval $sMqlCommand} outStr ]
                 if {$mqlret != 0} {
                    puts stdout ">ERROR       : eServiceInstallGroup"
                    puts stdout ">$sMqlCommand"
                        puts stdout ">$outStr"
                    append sErrorMsg "$outStr\n"
                    continue
                 }
             }
    }
# end of foreach of lCmdList

    if { $sErrorMsg != "" } {
              return 1
    }

    return 0
}

################################################################################

################################################################################
# depricated method
################################################################################
proc eServiceApplicationSetLanguage { sProgramName sBaseLanguage {sAliasLanguages ""}} {

    set sProgName "eServiceApplicationSetLanguage"

    set sRegProgName          [mql get env REGISTRATIONOBJECT]
    set sProgramName          [string trim $sProgramName]
    set sBaseLanguage         [string trim $sBaseLanguage]
    set sAliasLanguages       [string trim $sAliasLanguages]
    set mqlret                 0
    set outstr                 ""
    set sTranslationFilePrefix "eServicecommonSetTranslation"
    set lAdmins [list store vault attribute type relationship format group role association person policy wizard]

    puts stdout ""
    puts stdout "Updating Language Aliases"

    if {$sProgramName == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - Program Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return 1
    }

    if {$sBaseLanguage == ""} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sBaseLanguage: Error - Program Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return 1
    }

    eval  [utLoad eServicecommonTranslation.tcl]

    set lAlias [split $sAliasLanguages ,]
    #get all the programs in the database
    set sExistProgram        [split [mql list program] "\n"]

    set iProgramIndx         [lsearch -exact $sExistProgram "$sProgramName"]

    #if the place holder for properties eServiceSystemInformation.tcl does not exist return FAILURE
    if {$iProgramIndx == -1} {
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       puts stdout ""
       puts stdout "$sProgName: Error - Program Name cannot be null"
       puts stdout ""
       puts stdout "++++++++++++++++++++++++++++++++++++++++++++++"
       return 1
    }

    # Get if Base Language is set
    set sCmd {mql print program "$sProgramName" select property\[BaseLanguage\].to dump |}
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceApplicationSetLanguage"
        puts stdout ">$sCmd"
        puts stdout ">$outstr"
        return 1
    }

    # Delete previous base language
    if {"$outstr" != ""} {
        set sCurrentBaseLang [lindex [split "$outstr"] 1]
        set sCmd {mql delete property BaseLanguage \
                          on program "$sProgramName" \
                          to program "$sCurrentBaseLang" \
                          }
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : eServiceApplicationSetLanguage"
            puts stdout ">$sCmd"
            puts stdout ">$outstr"
            return 1
        }
    }

    set sLangProgramName "${sTranslationFilePrefix}${sBaseLanguage}.tcl"

    # Add new Base language
    set sCmd {mql add property BaseLanguage \
                      on program "$sProgramName" \
                      to program "$sLangProgramName" \
                      }
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceApplicationSetLanguage"
        puts stdout ">$sCmd"
        puts stdout ">$outstr"
        return 1
    }

    # get all the available languages
    eval  [utLoad eServicecommonLanguageMap.tcl]
    set lLanguages [array names LanguageMap]

    set sCmd {mql print program "$sProgramName" select property\[Language\].to dump |}
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceApplicationSetLanguage"
        puts stdout ">$sCmd"
        puts stdout ">$outstr"
        return 1
    }

    set lOutput [split $outstr |]

    set sCmd {mql list property on program "$sRegProgName"}
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceApplicationSetLanguage"
        puts stdout ">$sCmd"
        puts stdout ">$outstr"
        return 1
    }

    set lPropList [split $outstr \n]

    foreach sLanguage $lLanguages {

        if {[lsearch $lAlias $sLanguage] >= 0} {
            set mqlret [catch {eval eServiceTranslation::eServiceTranslationLocale {$sLanguage}} outStr]
            if { $mqlret != 0 } {
                puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                puts stdout ">$outStr"
                set mqlall $mqlret
            }

            set mqlret [catch {eval eServiceTranslation::eServiceTranslationLoad} outStr]
            if { $mqlret != 0 } {
                puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                puts stdout ">$outStr"
                set mqlall $mqlret
            }

            foreach sItem $lPropList {
                set sProp [lindex [split $sItem] 0]
                set sCmd {mql print program "$sRegProgName" select property\[$sProp\].to dump |}
                set mqlret [catch {eval $sCmd} outstr]
                if {$mqlret != 0} {
                        puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                        puts stdout ">$sCmd"
                        puts stdout ">$outstr"
                        return 1
                }
                set sAbsName [lrange [split $outstr] 1 end]
                set sAdmin [lindex [split $sProp _] 0]
                if {[lsearch $lAdmins $sAdmin] >= 0} {
                    set sCmd {mql print alias on "$sAdmin" "$sAbsName" language "$sLanguage"}
                    set mqlret [catch {eval $sCmd} outstr]
                    if {$mqlret != 0} {
                        puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                        puts stdout ">$sCmd"
                        puts stdout ">$outstr"
                        return 1
                    }
                    if {$outstr == ""} {

                        set mqlret [catch {eval eServiceTranslation::eServiceTranslationGetString "$sProp"} outstr]
                        if { $mqlret != 0 } {
                            puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                                puts stdout ">$outstr"
                            set mqlall $mqlret
                        }

                        if {$outstr != ""} {
                            puts stdout [format ">Adding Alias for language $sLanguage on: %-13s%-54s%-15s" $sAdmin  $sAbsName  $outstr]
                            set sCmd {mql add alias "$outstr" to "$sAdmin" "$sAbsName" language "$sLanguage"}
                            set mqlret [catch {eval $sCmd} outstr]
                            if {$mqlret != 0} {
                                puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                                puts stdout ">$sCmd"
                                puts stdout ">$outstr"
                                return 1
                            }
                        }
                    } else {
                        puts stdout [format ">Exist Alias for language $sLanguage on: %-13s%-54s" $sAdmin  $sAbsName]
                    }
                }
            }
        }
        set sLangProgramName "${sTranslationFilePrefix}${sLanguage}.tcl"
        if {[lsearch $lOutput "program $sLangProgramName"] == -1} {
            if {[lsearch $lAlias $sLanguage] >= 0} {
                set sValue "Yes"
            } else {
                set sValue "No"
            }
            set sCmd {mql add property Language \
                              on program "$sProgramName" \
                              to program "$sLangProgramName" \
                              value "$sValue" \
                              }
            set mqlret [catch {eval $sCmd} outstr]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceApplicationSetLanguage"
                puts stdout ">$sCmd"
                puts stdout ">$outstr"
                return 1
            }
        }
    }
    return 0
}

##################################################################################

proc eServiceAddAdminObj {sOperation} {

    # Get required fields from input arguments.
    set sComment  [lindex $sOperation 0]
    set sType     [lindex $sOperation 2]
    set sTempName [lindex $sOperation 3]
    set sCmd      "set sName \"$sTempName\""
    uplevel       $sCmd
    upvar         sName sName
    upvar         sMode sMode
    set sRev      [lindex $sOperation 4]
    set sMQLCmd   [lindex $sOperation 5]

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Check if object already exists
    set sCmd {mql print bus "$sAbsType" "$sName" "$sRev" select exists dump}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceAddAdminObj"
        puts stdout ">$sCmd"
        puts stdout ">$outStr"
        return 1
    }

    # If it is not present then execute lCmds to add it.
    if {"$outStr" == "TRUE"} {
        puts stdout [format ">Exists      : %-13s%-54s%-15s" $sAbsType $sName $sRev]
    } else {
        puts stdout "$sComment"

        if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
            puts "$sMQLCmd"
        }

        set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
        if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceAddAdminObj"
                puts stdout ">$sMQLCmd"
                puts stdout ">$outStr"
                return 1
        }
    }

    return 0
}

##################################################################################

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
    set sRev      [lindex $sOperation 4]
    set sMQLCmd   [lindex $sOperation 5]

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Check if object already exists
    set sCmd {mql print bus "$sAbsType" "$sName" "$sRev" select exists dump}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceDeleteAdminObj"
        puts stdout ">$sCmd"
        puts stdout ">$outStr"
        return 1
    }

    # If it is not present then execute lCmds to add it.
    if {"$outStr" == "FALSE"} {
        puts stdout [format ">Deleted      :%-13s%-54s%-15s" $sAbsType $sName $sRev]
    } else {
        puts stdout "$sComment"
        if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
            puts "$sMQLCmd"
        }

        set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
        if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceDeleteAdminObj"
                puts stdout ">$sMQLCmd"
                puts stdout ">$outStr"
                return 1
        }
    }

    return 0
}

##################################################################################

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
    set sRev      [lindex $sOperation 4]
    set sMQLCmd   [lindex $sOperation 5]

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Execute all the commands to delete object.
    puts stdout "$sComment"
    if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
        puts "$sMQLCmd"
    }

    set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
    if {$mqlret != 0} {
            puts stdout ">ERROR       : eServiceModifyAdminObj"
            puts stdout ">$sMQLCmd"
            puts stdout ">$outStr"
            return 1
    }

    return 0
}

##################################################################################

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
        puts stdout ">ERROR       : eServiceConnectAdminObj"
        puts stdout ">$sCmd"
        puts stdout ">$outStr"
        return 1
    }
    set lOutput [split $outStr "\n"]

    foreach sItem $lOutput {
        set lItem [split $sItem |]
        if {"$sAbsFromType" == [lindex $lItem 3] && "$sFromName" == [lindex $lItem 4] && "$sFromRev" == [lindex $lItem 5]} {
            puts stdout [format ">Connection   :%-13s%-54s%-15s" $sAbsToType $sToName $sToRev]
            puts stdout [format ">to           :%-13s%-54s%-15s" $sAbsFromType $sFromName $sFromRev]
            puts stdout "already exists"
            return 0
        }
    }

    # Execute all the commands to connect objects.
    puts stdout "$sComment"
    if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
        puts "$sMQLCmd"
    }

    set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
    if {$mqlret != 0} {
            puts stdout ">ERROR       : eServiceConnectAdminObj"
            puts stdout ">$sMQLCmd"
            puts stdout ">$outStr"
            return 1
    }

    return 0
}

##################################################################################

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
        puts stdout ">ERROR       : eServiceDisconnectAdminObj"
        puts stdout ">$sCmd"
        puts stdout ">$outStr"
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
        puts stdout [format ">Connection   :%-13s%-54s%-15s" $sAbsToType $sToName $sToRev]
        puts stdout [format ">to           :%-13s%-54s%-15s" $sAbsFromType $sFromName $sFromRev]
        puts stdout "does not exists"
    } else {
        # Execute all the commands to disconnect objects.
        puts stdout "$sComment"
        if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
            puts "$sMQLCmd"
        }

        set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : eServiceDisconnectAdminObj"
            puts stdout ">$sMQLCmd"
            puts stdout ">$outStr"
            return 1
        }
    }

    return 0
}

##################################################################################

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

    set sAbsToType       [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sToType]
    set sAbsFromType     [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sFromType]
    set sAbsRelationship [eServiceGetCurrentSchemaName relationship eServiceSchemaVariableMapping.tcl $sRelationship]

    # Execute all the commands to modify connection between objects.
    puts stdout "$sComment"
    if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
        puts "$sMQLCmd"
    }

    set mqlret [catch {eval {uplevel $sMQLCmd}} outStr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceModifyAdminObjConnection"
        puts stdout ">$sMQLCmd"
        puts stdout ">$outStr"
        return 1
    }

    return 0
}

##################################################################################

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

    set sAbsType [eServiceGetCurrentSchemaName type eServiceSchemaVariableMapping.tcl $sType]

    # Get current state , list of states supported and policy of an object
    set sCmd {mql print bus "$sAbsType" "$sName" "$sRev" select policy current state dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : eServiceChangeAdminObjState"
        puts stdout ">$sCmd"
        puts stdout ">$outStr"
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
        puts stdout [format ">Object       :%-13s%-54s%-15s" $sAbsType $sName $sRev]
        puts stdout [format ">is in state  :%-13s" $sTargetState]
    } else {
        puts stdout [format ">Moving object:%-13s%-54s%-15s" $sAbsType $sName $sRev]
        puts stdout [format ">to state     :%-13s" $sTargetState]
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
            if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
                puts "mql override bus \"$sAbsType\" \"$sName\" \"$sRev\""
            }

            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceChangeAdminObjState"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                return 1
            }
            set sCmd {mql $sAction bus "$sAbsType" "$sName" "$sRev"}
            if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
                puts "mql $sAction bus \"$sAbsType\" \"$sName\" \"$sRev\""
            }

            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : eServiceChangeAdminObjState"
                puts stdout ">$sCmd"
                puts stdout ">$outStr"
                return 1
            }
        }
    }

    return 0
}

##################################################################################

proc eServiceInstallAdminBusObjs {lOperations} {

    set sInstallDir [mql get env MXAPPBUILD]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sMode [mql get env MXMODE]
    set mqlret 0
    set mqlall 0


    # Load Environment variables having property names
    # and names of admin objects as values
    LoadSchema $sRegProgName

    # get all the properties attached to the eServiceSchemaVariableMapping.tcl
    # tell this procedure as they have global scope
    set lListGlobalVariables [split [mql list property on program $sRegProgName] "\n"]

    foreach lGlobalElement $lListGlobalVariables {
        set lGlobalElement [split $lGlobalElement]
        set sPropertyName  [ lindex $lGlobalElement 0]
        catch {global "$sPropertyName"}
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
                    puts stdout ">ERROR       : eServiceInstallAdminBusObjs"
                    puts stdout ">Invalid Operation"
                    set mqlret 1
                }
        }

        if {$mqlret != 0} {
            set mqlall $mqlret
        }
    }

    return $mqlall
}

#############################################################################
# Procedure Name : emxGetValueFromKey
# Brief Description : This procedure accepts a list of key and value pair
#                     in the format specified in input argument and
#                     returns value of the specified key.
# Input : 1. List of keys and values
#         Example :
#                   {
#                    {
#                     {name} {label} {icon} {href} {command} {menu}
#                    }
#                    {
#                     {MENUNAME}
#                     {LABELNAME}
#                     {FILENAME}
#                     {"STRING"}
#                     {
#                      {COMMANDNAME1} {COMMANDNAME2} {COMMANDNAME3} {COMMANDNAME4}
#                     }
#                     {
#                      {MENUNAME1} {MENUNAME2}
#                     }
#                    }
#                   }
#          2. Key :
#             Example : command
# Outout : Returns Value of the specified key
#          Example : For key command
#                     {
#                      {COMMANDNAME1} {COMMANDNAME2} {COMMANDNAME3} {COMMANDNAME4}
#                     }
#
#############################################################################

proc emxGetValueFromKey {lKeyValues sKey} {

    # Get key list
    set lKeys [lindex $lKeyValues 0]

    # Get value list
    set lValues [lindex $lKeyValues 1]

    # Search for the key
    set nIndex [lsearch $lKeys "$sKey"]
    if {$nIndex == -1} {
        return ""
    }

    # Get value at same index
    return [lindex $lValues $nIndex]
}

#############################################################################
# Procedure Name : emxGetKeyValuePairs
# Brief Description : This procedure accepts a list of key and value pair
#                     in the format specified in input argument and
#                     returns list of keys and values
# Input : List of keys and values
#         Example :
#                  {ADD}
#                  {name=MENUNAME}
#                  {label=LABELNAME}
#                  {icon=FILENAME}
#                  {href="STRING"}
#                  {command=COMMANDNAME1}
#                  {command=COMMANDNAME2}
#                  {command=COMMANDNAME3}
#                  {command=COMMANDNAME4}
#                  {menu=MENUNAME1}
#                  {menu=MENUNAME2}
# Outout : Returns list of Keys and Values
#          Example :
#                   {
#                    {
#                     {name} {label} {icon} {href} {command} {menu}
#                    }
#                    {
#                     {MENUNAME}
#                     {LABELNAME}
#                     {FILENAME}
#                     {"STRING"}
#                     {
#                      {COMMANDNAME1} {COMMANDNAME2} {COMMANDNAME3} {COMMANDNAME4}
#                     }
#                     {
#                      {MENUNAME1} {MENUNAME2}
#                     }
#                    }
#                   }
#############################################################################

proc emxGetKeyValuePairs {lAdminInfo} {

    # Return list of keys and values
    set lKeys {}
    set lValues {}

    # For each key value pair separate out keys and values
    set nCommandMenuCounter 0
    foreach sItem $lAdminInfo {

        set sItem [string trim "$sItem"]
        set nCurlIndex [string first "\{" "$sItem"]
        if {$nCurlIndex >= 0 && $nCurlIndex == 0} {
            set sItem1 [lindex $sItem 0]
            set sItem2 [lindex $sItem 1]
            set sKey [lindex [split $sItem1 =] 0]
            set nIndex [lsearch $lKeys "$sKey"]
            if {$nIndex >= 0} {
                set lPrevValue [lindex $lValues $nIndex]
                set lItem1 [lindex $lPrevValue 0]
                set lItem2 [lindex $lPrevValue 1]
                lappend lItem1 [join [lrange [split $sItem1 =] 1 end] "="]
                lappend lItem2 [join [lrange [split $sItem2 =] 1 end] "="]
                set lValues [lreplace $lValues $nIndex $nIndex [list $lItem1 $lItem2] ]
            } else {
                lappend lKeys "$sKey"
                lappend lValues [list [list [join [lrange [split $sItem1 =] 1 end] "="]] [list [join [lrange [split $sItem2 =] 1 end] "="]]]
            }

        } else {
            set lKeyValue [split $sItem =]
            set sKey [lindex $lKeyValue 0]
            if {"$sKey" == "command" || "$sKey" == "menu"} {
                set sKey "command_menu"
            }
            if {"$sKey" == "add command" || "$sKey" == "add menu"} {
                set sKey "add command_menu"
            }
            set sValue [join [lrange $lKeyValue 1 end] "="]
            set nIndex [lsearch $lKeys "$sKey"]
            if {$nIndex >= 0} {
                set lPrevValues [lindex $lValues $nIndex]
                set lValues [lreplace $lValues $nIndex $nIndex [lappend lPrevValues $sValue] ]
            } else {
                lappend lKeys "$sKey"
                if {"$sKey" == "argument" || "$sKey" == "add argument" || "$sKey" == "remove argument"} {
                    lappend lValues [list "$sValue"]
                } else {
                    lappend lValues "$sValue"
                }
            }
        }
    }

    # Return list of keys and values
    return [list $lKeys $lValues]
}

#############################################################################
# Procedure Name : emxGetAdminNamesFromProps
# Brief Description : This procedure returns list od admin names
#                     from given list of properties
# Input : List of property names
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of admin names
#############################################################################

proc emxGetAdminNamesFromProps {lProps} {

    # Registration Program Name
    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Create MQL command to get all the property to end
    set sCmd "mql print program '$sRegProgName' select"
    foreach sProp $lProps {
        append sCmd " property\\\[$sProp\\\].to"
    }
    append sCmd " dump |"
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        return -code $mqlret "$outStr"
    }

    # Parse the output to return list of admin names.
    set lOutput [split "$outStr" |]
    set lAdmins {}
    foreach sOutput $lOutput {
        set sAdminName [join [lrange [split "$sOutput"] 1 end] " "]
        lappend lAdmins "$sAdminName"
    }

    return -code 0 $lAdmins
}

#############################################################################
# Procedure Name : emxCreateMQLForMenu
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on menuInfo provided,
#                     name of menu and whether it is already installed
# Input : 1. lMenuInfo
#            Example :
#                   {
#                    {
#                     {name} {label} {icon} {href} {command} {menu}
#                    }
#                    {
#                     {MENUNAME}
#                     {LABELNAME}
#                     {FILENAME}
#                     {"STRING"}
#                     {
#                      {COMMANDNAME1} {COMMANDNAME2} {COMMANDNAME3} {COMMANDNAME4}
#                     }
#                     {
#                      {MENUNAME1} {MENUNAME2}
#                     }
#                    }
#                   }
#          2. sMenuName - Name of menu
#          3. bMenuInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install menu
#############################################################################

proc emxCreateMQLForMenu {lMenuInfo sMenuName bMenuInstalled sOperation} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided
    set lKeys [lindex $lMenuInfo 0]
    set lValues [lindex $lMenuInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lMenuInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lMenuInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Menu already installed
    if {$bMenuInstalled == 1} {

        # Check operation
        switch $sOperation {
            "ADD MENU" {
                # If operation is Add and Menu already exists
                # then modify Menu to add specified commands
                # and specified menues
                puts stdout [format ">Menu %s Exists" $sMenuName]
                set lCommandMenuProps [emxGetValueFromKey $lMenuInfo command_menu]
                if {[llength $lCommandMenuProps] != 0} {
                    set sCmd "mql modify menu '$sMenuName'"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$lCommandMenuProps}} lCommandMenus ]
                    if { $mqlret != 0 } {
                        return -code $mqlret $lCommandMenus
                    }
                    set i 0
                    foreach sCommandMenu $lCommandMenus {
                        set sProp [lindex $lCommandMenuProps $i]
                        puts stdout [format ">Adding %s [lindex [split $sProp _] 0] To Menu %s" $sCommandMenu $sMenuName]
                        append sCmd " add [lindex [split $sProp _] 0] '$sCommandMenu'"
                        incr i
                    }
                    lappend lCmd $sCmd
                }
            }

            "MODIFY MENU" {
                # If operation is modify then update specified information
                puts stdout [format ">Modifying Menu %s" $sMenuName]
                set sCmd "mql modify menu '$sMenuName'"
                set nCount 0
                set lOrderedMenus {}
                set lOrderedCommands {}
                set lMenuOrders {}
                set lCommandOrders {}
                set sCmd2 "mql modify menu '$sMenuName'"
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        href -
                        alt {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " add setting '$sSettingName' '$sValue'"
                        }
                        "^remove setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " remove setting '$sSettingName'"
                        }
                        "add command_menu" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lCommandMenus ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommandMenus
                            }
                            set i 0
                            foreach sCommandMenu $lCommandMenus {
                                set sProp [lindex $sValue $i]
                                append sCmd " add [lindex [split $sProp _] 0] '$sCommandMenu'"
                                incr i
                            }
                        }
                        "remove command" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lCommands ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommands
                            }
                            foreach sCommand $lCommands {
                                append sCmd " remove command '$sCommand'"
                            }
                        }
                        "remove menu" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lMenus ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lMenus
                            }
                            foreach sMenu $lMenus {
                                append sCmd " remove menu '$sMenu'"
                            }
                        }
                        "command" {
                            set lCommandNameProp [lindex $sValue 0]
                            set lOrders [lindex $sValue 1]
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$lCommandNameProp}} lCommands ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommands
                            }
                            set i 0
                            foreach sCommand $lCommands {
                                append sCmd2 " order command '$sCommand' [lindex $lOrders $i]"
                                incr i
                            }
                        }
                        "menu" {
                            set lMenuNameProp [lindex $sValue 0]
                            set lOrders [lindex $sValue 1]
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$lMenuNameProp}} lMenus ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lMenus
                            }
                            set i 0
                            foreach sMenu $lMenus {
                                append sCmd2 " order menu '$sMenu' [lindex $lOrders $i]"
                                incr i
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                lappend lCmd $sCmd
                if {"$sCmd2" != "mql modify menu '$sMenuName'"} {
                    lappend lCmd $sCmd2
                }

                if {[llength $lOrderedCommands] > 0 || [llength $lOrderedMenus] > 0} {
                    set sCmd "mql modify '$sMenuName'"
                    set nCount1 0
                    foreach sCommand $lOrderedCommands {
                        set sOrder [lindex $lCommandOrders $nCount1]
                        append sCmd " order command '$sCommand' $sOrder"
                        incr nCount1
                    }
                    set nCount1 0
                    foreach sMenu $lOrderedMenus {
                        set sOrder [lindex $lMenuOrders $nCount1]
                        append sCmd " order menu '$sMenu' $sOrder"
                        incr nCount1
                    }

                    lappend lCmd $sCmd
                }
            }

            "DELETE MENU" {
                # If Operation is Delete the delete specified Command
                puts stdout [format ">Deleting Menu %s" $sMenuName]
                lappend lCmd "mql delete menu '$sMenuName'"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD MENU" {
                # If operation is ADD then add the specified menu
                # Rename the menu if menu with same name already exists
                set lAllMenus [split [mql list menu] \n]
                set sMenuOrigName "$sMenuName"
                if {[lsearch $lAllMenus "$sMenuName"] >= 0} {
                    puts stdout [format ">Renaming Menu %s to %s" "$sMenuName" "$sPrefixName$sMenuName"]
                    set sMenuName "$sPrefixName$sMenuName"
                }
                puts stdout [format ">Adding Menu %s" "$sMenuName"]
                set sCmd "mql add menu '$sMenuName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        href -
                        alt {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " setting '$sSettingName' '$sValue'"
                        }
                        command_menu {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lCommandMenus ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommandMenus
                            }
                            set i 0
                            foreach sComandMenu $lCommandMenus {
                                set sProp [lindex $sValue $i]
                                append sCmd " [lindex [split $sProp _] 0] '$sComandMenu'"
                                incr i
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sMenuOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to menu '$sMenuName'"
            }

            "MODIFY MENU" {
                    return -code 1 "Menu $sMenuName does not exists"
            }

            "DELETE MENU" {
                # If operation is Delete then do nothing as Menu doesnot exists.
                puts stdout [format ">Menu %s Already Removed" "$sMenuName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}

#############################################################################
# Procedure Name : emxCreateMQLForPortal
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on portalInfo provided,
#                     name of portal and whether it is already installed
# Input : 1. lPortalInfo
#            Example :
#                   {
#                    {
#                     {name} {label} {icon} {href} {channel}
#                    }
#                    {
#                     {MENUNAME}
#                     {LABELNAME}
#                     {FILENAME}
#                     {"STRING"}
#                     {
#                      {CHANNELNAME1} {CHANNELNAME2} {CHANNELNAME3} {CHANNELNAME4}
#                     }
#                    }
#                   }
#          2. sPortalName - Name of portal
#          3. bPortalInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install portal
#############################################################################

proc emxCreateMQLForPortal {lPortalInfo sPortalName bPortalInstalled sOperation} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided
    set lKeys [lindex $lPortalInfo 0]
    set lValues [lindex $lPortalInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lPortalInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lPortalInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Portal already installed
    if {$bPortalInstalled == 1} {
        # Check operation
        switch $sOperation {
            "ADD PORTAL" {
                # If operation is Add and Portal already exists
                # then modify Portal to add specified commands
                # and specified portals
                puts stdout [format ">Portal %s Exists" $sPortalName]
                set lChannelPortalProps [emxGetValueFromKey $lPortalInfo channel]
                if {[llength $lChannelPortalProps] != 0} {
                    set sCmd "mql modify portal '$sPortalName'"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    foreach sChannelPortalProp $lChannelPortalProps {
                        set lChannelProps [split $sChannelPortalProp ","]
                        set mqlret [ catch {eval emxGetAdminNamesFromProps {$lChannelProps}} lChannels ]
                        if { $mqlret != 0 } {
                            return -code $mqlret $lChannels
                        }
                        append sCmd " place '[lindex $lChannels 0]' newrow after ''"
                        set lChannels [lrange $lChannels 1 end]
                        foreach sChannel $lChannels {
                            append sCmd " place '$sChannel' after ''"
                        }
                    }
                    lappend lCmd $sCmd
                }
            }

            "MODIFY PORTAL" {
                # Get command information.
                set sCmd {mql print portal "$sPortalName" select channel place dump tcl}
                set mqlret [catch {eval $sCmd} outStr]
                if { $mqlret != 0 } {
                    return -code $mqlret $outStr
                }
                set lExistingChannels [lindex [lindex $outStr 0] 0]
                set lPlaces [lindex [lindex $outStr 0] 1]

                # If operation is modify then update specified information
                puts stdout [format ">Modifying Portal %s" $sPortalName]
                set sCmd "mql modify portal '$sPortalName'"
                set nCount 0
                set sCmd2 "mql modify portal '$sPortalName'"
				append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        description -
                        alt -
                        href {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " add setting '$sSettingName' '$sValue'"
                        }
                        "^remove setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " remove setting '$sSettingName'"
                        }
                        "remove channel" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lChannels ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lChannels
                            }
							append sCmd " remove channel "                          
                            foreach sChannel $lChannels {
                               append sCmd " '$sChannel' "
                            }
                        }
                        "add channel" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lChannels ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lChannels
                            }
                            foreach sChannel $lChannels {
                                if {[lsearch $lExistingChannels "$sChannel"] >=0 } {
                                    continue
                                }
                                append sCmd " place '$sChannel' after ''"
                                lappend lExistingChannels "$sChannel"
                                set sLastColumnPlace [lindex [split [lindex $lPlaces [expr [llength $lPlaces] -1]] ,] 1]
                                set sLastRowPlace [lindex [split [lindex $lPlaces [expr [llength $lPlaces] -1]] ,] 0]
                                lappend lPlaces "$sLastRowPlace,[expr $sLastColumnPlace + 1]"
                            }
                        }
                        "channel" {
                            set lChannelNameProp [lindex $sValue 0]
                            set lOrders [lindex $sValue 1]
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$lChannelNameProp}} lChannels ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lChannels
                            }
                            foreach sChannel $lChannels sOrder $lOrders {
                                set sRowIndex [lindex [split $sOrder ,] 0]
                                set sColumnIndex [lindex [split $sOrder ,] 1]
                                
                                if {"$sRowIndex" == "last"} {
                                    if {"$sColumnIndex" == "insert"} {
                                        append sCmd2 " place '$sChannel' newrow after ''"
                                    } elseif {"$sColumnIndex" == "last"} {
                                        append sCmd2 " place '$sChannel' after '[lindex $lExistingChannels [expr [llength $lExistingChannels] - 1]]'"
                                    } else {
                                        set sRowPlaceIndex [lindex [split [lindex $lPlaces [expr [llength $lPlaces] - 1]] ,] 0]
                                        set sColumnPlaceIndex [lindex [split [lindex $lPlaces [expr [llength $lPlaces] - 1]] ,] 1]
                                        if {[lsearch $lPlaces "$sRowPlaceIndex,$sColumnIndex"] < 0} {
                                            append sCmd2 " place '$sChannel' after '[lindex $lExistingChannels [expr [llength $lExistingChannels] - 1]]'"
                                        } else {
                                            append sCmd2 " place '$sChannel' before '[lindex $lExistingChannels [lsearch $lPlaces "$sRowPlaceIndex,$sColumnIndex"]]'"
                                        }
                                    }
                                } else {
                                    if {"$sColumnIndex" == "insert"} {
                                        if {[lsearch $lPlaces "$sRowIndex,1"] < 0} {
                                            append sCmd2 " place '$sChannel' newrow after ''"
                                        } else {
                                            set sIndexChannel [lindex $lExistingChannels [lsearch $lPlaces "$sRowIndex,1"]]
                                            append sCmd2 " place '$sChannel' newrow before '$sIndexChannel'"
                                        }
                                    } elseif {"$sColumnIndex" == "last"} {
                                        set sIndexChannel ""
                                        set sPlaceIndex "-1"
                                        foreach sPlace $lPlaces sExistingChannel $lExistingChannels {
                                            if {[lindex [split $sPlace ,] 0] == "$sRowIndex"} {
                                                set sIndexChannel "$sExistingChannel"
                                            }
                                        }
                                        
                                        append sCmd2 " place '$sChannel' after '$sIndexChannel'"
                                    } else {
                                        if {[lsearch $lPlaces "$sOrder"] < 0} {
                                            set sIndexChannel ""
                                            foreach sPlace $lPlaces sExistingChannel $lExistingChannels {
                                                if {[lindex [split $sPlace ,] 0] == "$sRowIndex"} {
                                                    set sIndexChannel "$sExistingChannel"
                                                }
                                            }

                                            append sCmd2 " place '$sChannel' after '$sIndexChannel'"
                                        } else {
                                            append sCmd2 " place '$sChannel' before '[lindex $lExistingChannels [lsearch $lPlaces "$sOrder"]]'"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    incr nCount
                }
                lappend lCmd $sCmd
                if {"$sCmd2" != "mql modify portal '$sPortalName'"} {
                    lappend lCmd $sCmd2
                }
            }

            "DELETE PORTAL" {
                # If Operation is Delete the delete specified Command
                puts stdout [format ">Deleting Portal %s" $sPortalName]
                lappend lCmd "mql delete portal '$sPortalName'"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD PORTAL" {
                # If operation is ADD then add the specified portal
                # Rename the portal if portal with same name already exists
                set lAllPortals [split [mql list portal] \n]
                set sPortalOrigName "$sPortalName"
                if {[lsearch $lAllPortals "$sPortalName"] >= 0} {
                    puts stdout [format ">Renaming Portal %s to %s" "$sPortalName" "$sPrefixName$sPortalName"]
                    set sPortalName "$sPrefixName$sPortalName"
                }
                puts stdout [format ">Adding Portal %s" "$sPortalName"]
                set sCmd "mql add portal '$sPortalName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        description -
                        alt -
                        height -
                        href {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " setting '$sSettingName' '$sValue'"
                        }
                        channel {
                            foreach sChannelProps $sValue {
                                set lChannelProps [split $sChannelProps ,]

                                set mqlret [ catch {eval emxGetAdminNamesFromProps {$lChannelProps}} lChannels ]
                                if { $mqlret != 0 } {
                                    return -code $mqlret $lChannels
                                }
                                
                                set lChannelsWithQuotes {}
                                foreach sChannel $lChannels {
                                    lappend lChannelsWithQuotes "'$sChannel'"
                                }
                                append sCmd " channel [join $lChannelsWithQuotes ,]"
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sPortalOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to portal '$sPortalName'"
            }

            "MODIFY PORTAL" {
                    return -code 1 "Portal $sPortalName does not exists"
            }

            "DELETE PORTAL" {
                # If operation is Delete then do nothing as Portal doesnot exists.
                puts stdout [format ">Portal %s Already Removed" "$sPortalName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}


#############################################################################
# Procedure Name : emxCreateMQLForChannel
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on channelInfo provided,
#                     name of channel and whether it is already installed
# Input : 1. lChannelInfo
#            Example :
#                   {
#                    {
#                     {name} {label} {icon} {href} {command}
#                    }
#                    {
#                     {MENUNAME}
#                     {LABELNAME}
#                     {FILENAME}
#                     {"STRING"}
#                     {
#                      {COMMANDNAME1} {COMMANDNAME2} {COMMANDNAME3} {COMMANDNAME4}
#                     }
#                    }
#                   }
#          2. sChannelName - Name of channel
#          3. bChannelInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install channel
#############################################################################

proc emxCreateMQLForChannel {lChannelInfo sChannelName bChannelInstalled sOperation} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided
    set lKeys [lindex $lChannelInfo 0]
    set lValues [lindex $lChannelInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lChannelInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lChannelInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Channel already installed
    if {$bChannelInstalled == 1} {
        # Check operation
        switch $sOperation {
            "ADD CHANNEL" {
                # If operation is Add and Channel already exists
                # then modify Channel to add specified commands
                # and specified channels
                puts stdout [format ">Channel %s Exists" $sChannelName]
                set lCommandChannelProps [emxGetValueFromKey $lChannelInfo command_menu]
                if {[llength $lCommandChannelProps] != 0} {
                    set sCmd "mql modify channel '$sChannelName'"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$lCommandChannelProps}} lCommandChannels ]
                    if { $mqlret != 0 } {
                        return -code $mqlret $lCommandChannels
                    }
                    set i 0
                    foreach sCommandChannel $lCommandChannels {
                        set sProp [lindex $lCommandChannelProps $i]
                        puts stdout [format ">Adding %s [lindex [split $sProp _] 0] To Channel %s" $sCommandChannel $sChannelName]
                        append sCmd " place '$sCommandChannel' after ''"
                        incr i
                    }
                    lappend lCmd $sCmd
                }
            }

            "MODIFY CHANNEL" {
                # Get command information.
                set sCmd {mql print channel "$sChannelName" select command dump tcl}
                set mqlret [catch {eval $sCmd} outStr]
                if { $mqlret != 0 } {
                    return -code $mqlret $outStr
                }
                set lExistingCmd [lindex [lindex $outStr 0] 0]

                # If operation is modify then update specified information
                puts stdout [format ">Modifying Channel %s" $sChannelName]
                set sCmd "mql modify channel '$sChannelName'"
                set nCount 0
                set sCmd2 "mql modify channel '$sChannelName'"
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        description -
                        alt -
                        height -
                        href {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " add setting '$sSettingName' '$sValue'"
                        }
                        "^remove setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " remove setting '$sSettingName'"
                        }
                        "add command_menu" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lCommandChannels ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommandChannels
                            }
                            set i 0
                            foreach sCommandChannel $lCommandChannels {
                                set sProp [lindex $sValue $i]
                                append sCmd " place '$sCommandChannel' after ''"
                                lappend lExistingCmd "$sCommandChannel"
                                incr i
                            }
                        }
                        "remove command" {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lCommands ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommands
                            }
                            foreach sCommand $lCommands {
                                append sCmd " remove command '$sCommand'"
                            }
                        }
                        "command" {
                            set lCommandNameProp [lindex $sValue 0]
                            set lOrders [lindex $sValue 1]
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$lCommandNameProp}} lCommands ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommands
                            }
                            foreach sCommand $lCommands sOrder $lOrders {
                                if {[llength $lExistingCmd] < $sOrder} {
                                    set sOrder [llength $lExistingCmd]
                                }

                                append sCmd2 " place '$sCommand' before '[lindex $lExistingCmd [expr $sOrder - 1]]'"
                                set lExistingCmd [linsert $lExistingCmd [expr $sOrder - 1] "$sCommand"]
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                lappend lCmd $sCmd
                if {"$sCmd2" != "mql modify channel '$sChannelName'"} {
                    lappend lCmd $sCmd2
                }
            }

            "DELETE CHANNEL" {
                # If Operation is Delete the delete specified Command
                puts stdout [format ">Deleting Channel %s" $sChannelName]
                lappend lCmd "mql delete channel '$sChannelName'"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD CHANNEL" {
                # If operation is ADD then add the specified channel
                # Rename the channel if channel with same name already exists
                set lAllChannels [split [mql list channel] \n]
                set sChannelOrigName "$sChannelName"
                if {[lsearch $lAllChannels "$sChannelName"] >= 0} {
                    puts stdout [format ">Renaming Channel %s to %s" "$sChannelName" "$sPrefixName$sChannelName"]
                    set sChannelName "$sPrefixName$sChannelName"
                }
                puts stdout [format ">Adding Channel %s" "$sChannelName"]
                set sCmd "mql add channel '$sChannelName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        description -
                        alt -
                        height -
                        href {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " setting '$sSettingName' '$sValue'"
                        }
                        command_menu {
                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lCommandChannels ]
                            if { $mqlret != 0 } {
                                return -code $mqlret $lCommandChannels
                            }
                            set i 0
                            foreach sComandChannel $lCommandChannels {
                                set sProp [lindex $sValue $i]
                                append sCmd " [lindex [split $sProp _] 0] '$sComandChannel'"
                                incr i
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sChannelOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to channel '$sChannelName'"
            }

            "MODIFY CHANNEL" {
                    return -code 1 "Channel $sChannelName does not exists"
            }

            "DELETE CHANNEL" {
                # If operation is Delete then do nothing as Channel doesnot exists.
                puts stdout [format ">Channel %s Already Removed" "$sChannelName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}

#############################################################################
# Procedure Name : emxCreateMQLForCommand
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on commandInfo provided,
#                     name of command and whether it is already installed
# Input : 1. lCommnadInfo
#            Example :
#                   {
#                    {
#                     {name} {label} {icon} {href} {user}
#                    }
#                    {
#                     {MENUNAME}
#                     {LABELNAME}
#                     {FILENAME}
#                     {STRING}
#                     {
#                      {USER1} {USER2} {USER3} {USER4}
#                     }
#                    }
#                   }
#          2. sCommandName - Name of command
#          3. bCommnadInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install command
#############################################################################

proc emxCreateMQLForCommand {lCommandInfo sCommandName bCommandInstalled sOperation} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided
    set lKeys [lindex $lCommandInfo 0]
    set lValues [lindex $lCommandInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lCommandInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lCommandInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sInstallDir [mql get env MXAPPBUILD]
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Command already installed
    if {$bCommandInstalled == 1} {

        # Check operation
        switch $sOperation {
            "ADD COMMAND" {
                # If operation is Add and Command already exists
                # then modify Command to add specified users
                puts stdout [format ">Command %s Exists" $sCommandName]
                set lUserProps [emxGetValueFromKey $lCommandInfo user]
                if {[llength $lUserProps] != 0} {
                    set sCmd "mql modify command '$sCommandName'"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"

                    if {[lsearch $lUserProps "all"] == -1} {
                        set mqlret [ catch {eval emxGetAdminNamesFromProps {$lUserProps}} lUsers ]
                        if { $mqlret != 0 } {
                            return -code $mqlret $lUsers
                        }
                        foreach sUser $lUsers {
                            append sCmd " add user '$sUser'"
                        }
                    } else {
                        set lUsers [list "all"]
                        append sCmd " add user all"
                    }

                    puts stdout [format ">Adding %s Users To Command %s" [join $lUsers ,] $sCommandName]
                    lappend lCmd $sCmd
                }
            }

            "MODIFY COMMAND" {
                # If operation is modify then update specified information
                puts stdout [format ">Modifying Command %s" $sCommandName]
                set sCmd "mql modify command '$sCommandName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        href -
                        alt -
                        file -
                        description {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " add setting '$sSettingName' '$sValue'"
                        }
                        "^remove setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " remove setting '$sSettingName'"
                        }
                        "add user" {
                            if {[lsearch $sValue "all"] == -1} {
                                set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lUsers ]
                                if { $mqlret != 0 } {
                                    return -code $mqlret $lUsers
                                }
                                foreach sUser $lUsers {
                                    append sCmd " add user '$sUser'"
                                }
                            } else {
                                append sCmd " add user all"
                            }
                        }
                        "remove user" {
                            if {[lsearch $sValue "all"] == -1} {
                                set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lUsers ]
                                if { $mqlret != 0 } {
                                    return -code $mqlret $lUsers
                                }
                                foreach sUser $lUsers {
                                    append sCmd " remove user '$sUser'"
                                }
                            } else {
                                append sCmd " remove user all"
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                lappend lCmd $sCmd
            }

            "DELETE COMMAND" {
                # If Operation is Delete then delete specified Command
                puts stdout [format ">Deleting Command %s" $sCommandName]
                lappend lCmd "mql delete command '$sCommandName'"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD COMMAND" {
                # If operation is ADD then add the specified command
                # Rename the command if command with same name already exists
                set lAllCommands [split [mql list command] \n]
                set sCommandOrigName "$sCommandName"
                if {[lsearch $lAllCommands "$sCommandName"] >= 0} {
                    puts stdout [format ">Renaming Command %s to %s" "$sCommandName" "$sPrefixName$sCommandName"]
                    set sCommandName "$sPrefixName$sCommandName"
                }
                puts stdout [format ">Adding Command %s" "$sCommandName"]
                set sCmd "mql add command '$sCommandName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch -regexp $sKey {
                        icon -
                        label -
                        href -
                        alt -
                        file -
                        description {
                            append sCmd " $sKey '$sValue'"
                        }
                        "^add setting*" -
                        "^setting*" {
                            set nOpenIndex [string first "\[" "$sKey"]
                            set nCloseIndex [string first "\]" "$sKey"]
                            set sSettingName [string range "$sKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                            append sCmd " setting '$sSettingName' '$sValue'"
                        }
                        "user" {
                            if {[lsearch $sValue "all"] == -1} {
                                set mqlret [ catch {eval emxGetAdminNamesFromProps {$sValue}} lUsers ]
                                if { $mqlret != 0 } {
                                    return -code $mqlret $lUsers
                                }
                                foreach sUser $lUsers {
                                    append sCmd " user '$sUser'"
                                }
                            } else {
                                append sCmd " user all"
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sCommandOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to command '$sCommandName'"
            }

            "MODIFY COMMAND" {
                    return -code 1 "Command $sCommandName does not exists"
            }

            "DELETE COMMAND" {
                # If operation is Delete then do nothing as Command doesnot exists.
                puts stdout [format ">Command %s Already Removed" "$sCommandName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}

#############################################################################
# Procedure Name : emxCreateMQLForInquiry
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on inquiryInfo provided,
#                     name of inquiry and whether it is already installed
# Input : 1. lInquiryInfo
#            Example :
#                   {
#                    {
#                     {name} {pattern} {format} {file} {argument}
#                    }
#                    {
#                     {INQUIRYNAME}
#                     {*|*|*|${ID}}
#                     {ID = ${ID}}
#                     {FILENAME}
#                     {
#                      {NAME VALUE} {NAME VALUE} {NAME VALUE} {NAME VALUE}
#                     }
#                    }
#                   }
#          2. sInquiryName - Name of Inquiry
#          3. bInquiryInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install Inquiry
#############################################################################

proc emxCreateMQLForInquiry {lInquiryInfo sInquiryName bInquiryInstalled sOperation} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided
    set lKeys [lindex $lInquiryInfo 0]
    set lValues [lindex $lInquiryInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lInquiryInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lInquiryInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Inquiry already installed
    if {$bInquiryInstalled == 1} {

        # Check operation
        switch $sOperation {
            "ADD INQUIRY" {
                # If operation is Add and Inquiry already exists
                # then do nothing
                puts stdout [format ">Inquiry %s Exists" $sInquiryName]
            }

            "MODIFY INQUIRY" {
                # If operation is modify then update specified information
                puts stdout [format ">Modifying Inquiry %s" $sInquiryName]
                set sCmd "mql modify inquiry '$sInquiryName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch $sKey {
                        icon -
                        description -
                        pattern -
                        format -
                        code -
                        file {
                            append sCmd " $sKey '$sValue'"
                        }
                        argument -
                        "add argument" -
                        "remove argument" {
                            foreach sNameValue $sValue {
                                append sCmd " $sKey $sNameValue"
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                lappend lCmd $sCmd
            }

            "DELETE INQUIRY" {
                # If Operation is Delete then delete specified Inquiry
                puts stdout [format ">Deleting Inquiry %s" $sInquiryName]
                lappend lCmd "mql delete inquiry '$sInquiryName'"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD INQUIRY" {
                # If operation is ADD then add the specified inquiry
                # Rename the inquiry if inquiry with same name already exists
                set lAllInquirys [split [mql list inquiry] \n]
                set sInquiryOrigName "$sInquiryName"
                if {[lsearch $lAllInquirys "$sInquiryName"] >= 0} {
                    puts stdout [format ">Renaming Inquiry %s to %s" "$sInquiryName" "$sPrefixName$sInquiryName"]
                    set sInquiryName "$sPrefixName$sInquiryName"
                }
                puts stdout [format ">Adding Inquiry %s" "$sInquiryName"]
                set sCmd "mql add inquiry '$sInquiryName'"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch $sKey {
                        icon -
                        description -
                        pattern -
                        format -
                        code -
                        file {
                            append sCmd " $sKey '$sValue'"
                        }
                        argument {
                            foreach sNameValue $sValue {
                                append sCmd " argument $sNameValue"
                            }
                        }
                    }
                    incr nCount
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sInquiryOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to inquiry '$sInquiryName'"
            }

            "MODIFY INQUIRY" {
                    return -code 1 "Inquiry $sInquiryName does not exists"
            }

            "DELETE INQUIRY" {
                # If operation is Delete then do nothing as Inquiry doesnot exists.
                puts stdout [format ">Inquiry %s Already Removed" "$sInquiryName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}

#############################################################################
# Procedure Name : emxCreateMQLForTable
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on tableInfo provided,
#                     name of table and whether it is already installed
# Input : 1. lTableInfo
#            Example :
#                   {
#                    {
#                     {name} {pattern} {format} {file} {argument}
#                    }
#                    {
#                     {TABLENAME}
#                     {*|*|*|${ID}}
#                     {ID = ${ID}}
#                     {FILENAME}
#                     {
#                      {NAME VALUE} {NAME VALUE} {NAME VALUE} {NAME VALUE}
#                     }
#                    }
#                   }
#          2. sTableName - Name of Table
#          3. bTableInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install Table
#############################################################################

proc emxCreateMQLForTable {lTableInfo sTableName bTableInstalled sOperation lColumnOperations lColumnInfos} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided About Tables
    set lKeys [lindex $lTableInfo 0]
    set lValues [lindex $lTableInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lTableInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lTableInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Table already installed
    if {$bTableInstalled == 1} {

        # Get the coulmns already defined in the tables.
        set sCmd {mql print table "$sTableName" system select column.name dump |}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            return -code 1 $outStr
        }
        set lDefinedColumns [split $outStr |]

        # Check operation
        switch $sOperation {
            "ADD TABLE" {
                # If operation is Add and Table already exists
                # then try to add new columns.
                puts stdout [format ">Table %s Exists" $sTableName]

                set nCounter 0
                set sPostCmd ""
                set sPreCmd "mql modify table '$sTableName' system"

                # check for all the columns information
                foreach sColumnOperation $lColumnOperations {

                    # only ADD COLUMN operation should be supported
                    if {"$sColumnOperation" == "ADD COLUMN"} {

                        # Get column info
                        set lColumnInfo [lindex $lColumnInfos $nCounter]
                        set lColumnKeys [lindex $lColumnInfo 0]
                        set lColumnValues [lindex $lColumnInfo 1]
                        set nIndex [lsearch $lColumnKeys "name"]

                        # Error out if column name not specified.
                        if {$nIndex == -1} {
                            return -code 1 "Column name not specified"
                        }
                        set sColumnName [lindex $lColumnValues $nIndex]

                        # Check if column with same name already present
                        # If not then add column
                        if {[lsearch $lDefinedColumns $sColumnName] == -1} {

                            puts stdout [format ">Adding Column %s To Table %s" $sColumnName $sTableName]
                            append sPostCmd " column name '$sColumnName'"

                            # Get type and expression defination first
                            set nIndex1 [lsearch $lColumnKeys type]
                            set nIndex2 [lsearch $lColumnKeys expression]
                            if {("$nIndex1" == -1 && "$nIndex2" != -1) || ("$nIndex1" != -1 && "$nIndex2" == -1)} {
                                return -code 1 "type or expression missing for column $sColumnName"
                            }
                            if {"$nIndex1" != -1 && "$nIndex2" != -1} {
                                append sPostCmd " [lindex $lColumnValues $nIndex1] [lindex $lColumnValues $nIndex2]"
                            }

                            # Get rest of the column information
                            set nCounter2 0
                            foreach sColumnKey $lColumnKeys {
                                set sColumnValue [lindex $lColumnValues $nCounter2]
                                switch -regexp $sColumnKey {
                                    minsize -
                                    scale -
                                    href -
                                    alt -
                                    range -
                                    update -
                                    order -
                                    sorttype -
                                    autoheight -
                                    autowidth -
                                    label -
                                    edit -
                                    size -
                                    hidden {
                                        append sPostCmd " $sColumnKey '$sColumnValue'"
                                    }
                                    "^add setting*" -
                                    "^setting*" {
                                        set nOpenIndex [string first "\[" "$sColumnKey"]
                                        set nCloseIndex [string first "\]" "$sColumnKey"]
                                        set sSettingName [string range "$sColumnKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                        append sPostCmd " setting '$sSettingName' '$sColumnValue'"
                                    }
                                    user {
                                        if {[lsearch $sColumnValue "all"] == -1} {
                                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sColumnValue}} lUsers ]
                                            if { $mqlret != 0 } {
                                                return -code $mqlret $lUsers
                                            }
                                            foreach sUser $lUsers {
                                                append sPostCmd " user '$sUser'"
                                            }
                                        } else {
                                            append sPostCmd " user all"
                                        }
                                    }
                                    sortprogram {
                                        append sPostCmd " program '$sColumnValue'"
                                    }
                                }
                                incr nCounter2
                            }
                        } else {
                            puts stdout [format ">Column %s Already Exists In Table %s" $sColumnName $sTableName]
                            if {[lsearch $lColumnKeys "user"] != -1} {
                                set sColumnValue [lindex $lColumnValues [lsearch $lColumnKeys "user"]]
                                append sPostCmd " column modify name '$sColumnName'"
                                if {[lsearch $sColumnValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sColumnValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " add user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " add user all"
                                    set lUsers [list "all"]
                                }
                                puts stdout [format ">Adding %s Users To Column %s Of Table %s" [join $lUsers ,] $sColumnName $sTableName]
                            }
                        }
                    } else {
                        return -code 1 "Unsupported column operation $sColumnOperation"
                    }
                    incr nCounter
                }

                if {"$sPostCmd" != ""} {
                    set sCmd "${sPreCmd}${sPostCmd}"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    lappend lCmd $sCmd
                }
            }

            "MODIFY TABLE" {
                # If operation is modify then update specified information
                set nCounter 0
                set sPostCmd ""
                set sPreCmd "mql modify table '$sTableName' system"
                set nCount 0

                # update table information
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch $sKey {
                        icon -
                        description {
                            append sPostCmd " $sKey '$sValue'"
                        }
                        hidden {
                            if {"$sValue" == "false"} {
                                append sPostCmd " nothidden"
                            } else {
                                append sPostCmd " hidden"
                            }
                        }
                    }
                    incr nCount
                }

                # update column information
                foreach sColumnOperation $lColumnOperations {

                    # Get each column info
                    set lColumnInfo [lindex $lColumnInfos $nCounter]
                    set lColumnKeys [lindex $lColumnInfo 0]
                    set lColumnValues [lindex $lColumnInfo 1]

                    # Get column name
                    set nIndex [lsearch $lColumnKeys "name"]
                    if {$nIndex == -1} {
                        return -code 1 "Column name not specified"
                    }
                    set sColumnName [lindex $lColumnValues $nIndex]

                    set lColumnSettings {}
                    # Either add , delete or modify column
                    switch $sColumnOperation {
                        "ADD COLUMN" {
                            if {[lsearch $lDefinedColumns $sColumnName] != -1} {
                                puts stdout [format ">Column %s Already Exists In Table %s" $sColumnName $sTableName]
                                if {[lsearch $lColumnKeys "user"] != -1} {
                                    set sColumnValue [lindex $lColumnValues [lsearch $lColumnKeys "user"]]
                                    append sPostCmd " column modify name '$sColumnName'"
                                    if {[lsearch $sColumnValue "all"] == -1} {
                                        set mqlret [ catch {eval emxGetAdminNamesFromProps {$sColumnValue}} lUsers ]
                                        if { $mqlret != 0 } {
                                            return -code $mqlret $lUsers
                                        }
                                        foreach sUser $lUsers {
                                            append sPostCmd " add user '$sUser'"
                                        }
                                    } else {
                                        append sPostCmd " add user all"
                                        set lUsers [list "all"]
                                    }
                                    puts stdout [format ">Adding %s Users To Column %s Of Table %s" [join $lUsers ,] $sColumnName $sTableName]
                                }
                                incr nCounter
                                continue
                            } else {
                                puts stdout [format ">Adding Column %s To Table %s" $sColumnName $sTableName]
                                append sPostCmd " column name '$sColumnName'"
                            }
                        }
                        "MODIFY COLUMN" {
                            if {[lsearch $lDefinedColumns $sColumnName] != -1} {
                                # Get settings on the defined columns
                                set sCmd {mql print table "$sTableName" system select column\[$sColumnName\].setting dump |}
                                set mqlret [ catch {eval $sCmd} outStr ]
                                if {"$mqlret" != 0} {
                                    return -code 1 $outStr
                                }
                                set lColumnSettings [split $outStr |]
                                puts stdout [format ">Modifying Column %s In Table %s" $sColumnName $sTableName]
                                append sPostCmd " column modify name '$sColumnName'"
                            } else {
                                return -code 1 "Column $sColumnName Does Not Exists In Table $sTableName"
                            }
                        }
                        "DELETE COLUMN" {
                            if {[lsearch $lDefinedColumns $sColumnName] != -1} {
                                puts stdout [format ">Deleteing Column %s From Table %s" $sColumnName $sTableName]
                                append sPostCmd " column delete name '$sColumnName'"
                            } else {
                                puts stdout [format ">Column %s Does Not Exists In Table %s" $sColumnName $sTableName]
                            }
                            incr nCounter
                            continue
                        }
                        default {
                            return -code 1 "Unsupported column operation $sColumnOperation"
                        }
                    }

                    # Get type and expression info first
                    set nIndex1 [lsearch $lColumnKeys type]
                    set nIndex2 [lsearch $lColumnKeys expression]
                    if {("$nIndex1" == -1 && "$nIndex2" != -1) || ("$nIndex1" != -1 && "$nIndex2" == -1)} {
                        return -code 1 "type or expression missing for column $sColumnName"
                    }
                    if {"$nIndex1" != -1 && "$nIndex2" != -1} {
                        append sPostCmd " [lindex $lColumnValues $nIndex1] [lindex $lColumnValues $nIndex2]"
                    }

                    # Get rest of the table info
                    set nCounter2 0
                    foreach sColumnKey $lColumnKeys {
                        set sColumnValue [lindex $lColumnValues $nCounter2]
                        switch -regexp $sColumnKey {
                            minsize -
                            size -
                            scale -
                            href -
                            alt -
                            range -
                            update -
                            order -
                            sorttype -
                            autoheight -
                            autowidth -
                            edit -
                            label -
                            hidden {
                                append sPostCmd " $sColumnKey '$sColumnValue'"
                            }
                            "^add setting*" -
                            "^setting*" {
                                set nOpenIndex [string first "\[" "$sColumnKey"]
                                set nCloseIndex [string first "\]" "$sColumnKey"]
                                set sSettingName [string range "$sColumnKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                if {[lsearch $lColumnSettings "$sSettingName"] == -1} {
									if {[string first ";" $sColumnValue]>=0} {
										append sPostCmd " setting '$sSettingName' \"$sColumnValue\""
									} else {
										append sPostCmd " setting '$sSettingName' '$sColumnValue'"
									}
                                } else {
									if {[string first ";" $sColumnValue]>=0} {
										append sPostCmd " remove setting '$sSettingName' add setting '$sSettingName' \"$sColumnValue\""
									} else {
										append sPostCmd " remove setting '$sSettingName' add setting '$sSettingName' '$sColumnValue'"
									}
                                }
                            }
                            "^remove setting*" {
                                set nOpenIndex [string first "\[" "$sColumnKey"]
                                set nCloseIndex [string first "\]" "$sColumnKey"]
                                set sSettingName [string range "$sColumnKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                if {[lsearch $lColumnSettings "$sSettingName"] >= 0} {
                                    append sPostCmd " remove setting '$sSettingName'"
                                }
                            }
                            "add user" {
                                if {[lsearch $sColumnValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sColumnValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " add user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " add user all"
                                }
                            }
                            "remove user" {
                                if {[lsearch $sColumnValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sColumnValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " remove user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " remove user all"
                                }
                            }
                            sortprogram {
                                append sPostCmd " program '$sColumnValue'"
                            }
                        }
                        incr nCounter2
                    }

                    incr nCounter
                }

                if {"$sPostCmd" != ""} {
                    puts stdout [format ">Modifying Table %s" $sTableName]
                    set sCmd "${sPreCmd}${sPostCmd}"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    lappend lCmd $sCmd
                }
            }

            "DELETE TABLE" {
                # If Operation is Delete then delete specified Table
                puts stdout [format ">Deleting Table %s" $sTableName]
                lappend lCmd "mql delete table '$sTableName' system"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD TABLE" {
                # If operation is ADD then add the specified table
                # Rename the table if table with same name already exists
                set lAllTables [split [mql list table system] \n]
                set sTableOrigName "$sTableName"
                if {[lsearch $lAllTables "$sTableName"] >= 0} {
                    puts stdout [format ">Renaming Table %s to %s" "$sTableName" "$sPrefixName$sTableName"]
                    set sTableName "$sPrefixName$sTableName"
                }
                puts stdout [format ">Adding Table %s" "$sTableName"]

                # Add From Table Info
                set sCmd "mql add table '$sTableName' system"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch $sKey {
                        icon -
                        description {
                            append sCmd " $sKey '$sValue'"
                        }
                        hidden {
                            if {"$sValue" == "false"} {
                                append sCmd " nothidden"
                            } else {
                                append sCmd " hidden"
                            }
                        }
                    }
                    incr nCount
                }

                # Add from column info
                set nCounter 0
                foreach sColumnOperation $lColumnOperations {
                    if {"$sColumnOperation" == "ADD COLUMN"} {
                        set lColumnInfo [lindex $lColumnInfos $nCounter]
                        set lColumnKeys [lindex $lColumnInfo 0]
                        set lColumnValues [lindex $lColumnInfo 1]
                        set nIndex [lsearch $lColumnKeys "name"]
                        if {$nIndex == -1} {
                            return -code 1 "Column name not specified"
                        }

                        set sColumnName [lindex $lColumnValues $nIndex]
                        puts stdout [format ">Adding Column %s To Table %s" $sColumnName $sTableName]
                        append sCmd " column name '$sColumnName'"

                        # Get type and expression info first
                        set nIndex1 [lsearch $lColumnKeys type]
                        set nIndex2 [lsearch $lColumnKeys expression]
                        if {("$nIndex1" == -1 && "$nIndex2" != -1) || ("$nIndex1" != -1 && "$nIndex2" == -1)} {
                            return -code 1 "type or expression missing for column $sColumnName"
                        }
                        if {"$nIndex1" != -1 && "$nIndex2" != -1} {
                            append sCmd " [lindex $lColumnValues $nIndex1] [lindex $lColumnValues $nIndex2]"
                        }

                        set nCounter2 0
                        foreach sColumnKey $lColumnKeys {
                            set sColumnValue [lindex $lColumnValues $nCounter2]
                            switch -regexp $sColumnKey {
                                minsize -
                                size -
                                scale -
                                href -
                                alt -
                                range -
                                update -
                                order -
                                sorttype -
                                autoheight -
                                autowidth -
                                edit -
                                label -
                                hidden {
                                    append sCmd " $sColumnKey '$sColumnValue'"
                                }
                                "^add setting*" -
                                "^setting*" {
                                    set nOpenIndex [string first "\[" "$sColumnKey"]
                                    set nCloseIndex [string first "\]" "$sColumnKey"]
                                    set sSettingName [string range "$sColumnKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
									if {[string first ";" $sColumnValue]>=0} {
                                        append sCmd " setting '$sSettingName' \"$sColumnValue\""
									} else {
                                        append sCmd " setting '$sSettingName' '$sColumnValue'"
									}
                                }
                                "user" {
                                    if {[lsearch $sColumnValue "all"] == -1} {
                                        set mqlret [ catch {eval emxGetAdminNamesFromProps {$sColumnValue}} lUsers ]
                                        if { $mqlret != 0 } {
                                            return -code $mqlret $lUsers
                                        }
                                        foreach sUser $lUsers {
                                            append sCmd " user '$sUser'"
                                        }
                                    } else {
                                        append sCmd " user all"
                                    }
                                }
                                sortprogram {
                                    append sCmd " program '$sColumnValue'"
                                }
                            }
                            incr nCounter2
                        }
                    }
                    incr nCounter
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sTableOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to table '$sTableName' system"
            }

            "MODIFY TABLE" {
                    return -code 1 "Table $sTableName does not exists"
            }

            "DELETE TABLE" {
                # If operation is Delete then do nothing as Table doesnot exists.
                puts stdout [format ">Table %s Does Not Exists" "$sTableName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}

#############################################################################
# Procedure Name : emxCreateMQLForForm
# Brief Description : This procedure returns a list of MQL
#                     commands to install based on formInfo provided,
#                     name of form and whether it is already installed
# Input : 1. lFormInfo
#            Example :
#                   {
#                    {
#                     {name} {pattern} {format} {file} {argument}
#                    }
#                    {
#                     {FORMNAME}
#                     {*|*|*|${ID}}
#                     {ID = ${ID}}
#                     {FILENAME}
#                     {
#                      {NAME VALUE} {NAME VALUE} {NAME VALUE} {NAME VALUE}
#                     }
#                    }
#                   }
#          2. sFormName - Name of Form
#          3. bFormInstalled - 1 or 0
# Outout : On Failure : returns 1 and Error Message
#          On Success : returns 0 and list of MQL commands to execute to install Form
#############################################################################

proc emxCreateMQLForForm {lFormInfo sFormName bFormInstalled sOperation lFieldOperations lFieldInfos} {

    # return MQL Command List
    set lCmd {}

    # Get Information Provided About Forms
    set lKeys [lindex $lFormInfo 0]
    set lValues [lindex $lFormInfo 1]

    # Get Version
    set sVersion [emxGetValueFromKey $lFormInfo version]
    if {"$sVersion" == ""} {
        return -code 1 "Version not specified"
    }

    # Get Property
    set sProperty [emxGetValueFromKey $lFormInfo property]
    if {"$sProperty" == ""} {
        return -code 1 "Property name not specified"
    }

    # Get Environment Variables
    set sApplication [mql get env MXAPPLICATION]
    set sInstallOrg [mql get env MXORGNAME]
    set sRegProgName [mql get env REGISTRATIONOBJECT]
    set sPrefixName [mql get env MXPREFIXNAME]
    set sPixDir [mql get env MXPIXMAPDIR]

    # Get Time Stamp
    set sDate [clock format [clock seconds] -format %m-%d-%Y]

    # Return Value
    set nRet 0

    # Check if Form already installed
    if {$bFormInstalled == 1} {

        # Get the fields already defined in the forms.
        set sCmd {mql print form "$sFormName" select field.name dump |}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            return -code 1 $outStr
        }
        set lDefinedFields [split $outStr |]

        # Check operation
        switch $sOperation {
            "ADD FORM" {
                # If operation is Add and Form already exists
                # then try to add new fields.
                puts stdout [format ">Form %s Exists" $sFormName]

                set nCounter 0
                set sPostCmd ""
                set sPreCmd "mql modify form '$sFormName' web"

                # check for all the fields information
                foreach sFieldOperation $lFieldOperations {

                    # only ADD FIELD operation should be supported
                    if {"$sFieldOperation" == "ADD FIELD"} {

                        # Get field info
                        set lFieldInfo [lindex $lFieldInfos $nCounter]
                        set lFieldKeys [lindex $lFieldInfo 0]
                        set lFieldValues [lindex $lFieldInfo 1]
                        set nIndex [lsearch $lFieldKeys "name"]

                        # Error out if field name not specified.
                        if {$nIndex == -1} {
                            return -code 1 "Field name not specified"
                        }
                        set sFieldName [lindex $lFieldValues $nIndex]

                        # Check if field with same name already present
                        # If not then add field
                        if {[lsearch $lDefinedFields $sFieldName] == -1} {

                            puts stdout [format ">Adding Field %s To Form %s" $sFieldName $sFormName]
                            append sPostCmd " field"

                            # Get type and expression defination first
                            set nIndex1 [lsearch $lFieldKeys type]
                            set nIndex2 [lsearch $lFieldKeys expression]
                            if {"$nIndex2" == -1} {
                                append sPostCmd " select ''"
                            } else {
                                if {$nIndex1 == -1} {
                                    return -code 1 "type missing for field $sFieldName"
                                }
                                append sPostCmd " [lindex $lFieldValues $nIndex1] [lindex $lFieldValues $nIndex2]"
                            }

                            # Get rest of the field information
                            set nCounter2 0
                            foreach sFieldKey $lFieldKeys {
                                set sFieldValue [lindex $lFieldValues $nCounter2]
                                switch -regexp $sFieldKey {
                                    name -
                                    href -
                                    alt -
                                    range -
                                    order -
                                    label {
                                        append sPostCmd " $sFieldKey '$sFieldValue'"
                                    }
                                    "^add setting*" -
                                    "^setting*" {
                                        set nOpenIndex [string first "\[" "$sFieldKey"]
                                        set nCloseIndex [string first "\]" "$sFieldKey"]
                                        set sSettingName [string range "$sFieldKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                        append sPostCmd " setting '$sSettingName' '$sFieldValue'"
                                    }
                                    user {
                                        if {[lsearch $sFieldValue "all"] == -1} {
                                            set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                            if { $mqlret != 0 } {
                                                return -code $mqlret $lUsers
                                            }
                                            foreach sUser $lUsers {
                                                append sPostCmd " user '$sUser'"
                                            }
                                        } else {
                                            append sPostCmd " user all"
                                        }
                                    }
                                }
                                incr nCounter2
                            }
                        } else {
                            puts stdout [format ">Field %s Already Exists In Form %s" $sFieldName $sFormName]
                            if {[lsearch $lFieldKeys "user"] != -1} {
                                set sFieldValue [lindex $lFieldValues [lsearch $lFieldKeys "user"]]
                                append sPostCmd " field modify name '$sFieldName'"
                                if {[lsearch $sFieldValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " add user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " add user all"
                                    set lUsers [list "all"]
                                }
                                puts stdout [format ">Adding %s Users To Field %s Of Form %s" [join $lUsers ,] $sFieldName $sFormName]
                            }
                        }
                    } else {
                        return -code 1 "Unsupported field operation $sFieldOperation"
                    }
                    incr nCounter
                }

                if {"$sPostCmd" != ""} {
                    set sCmd "${sPreCmd}${sPostCmd}"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    lappend lCmd $sCmd
                }
            }

            "MODIFY FORM" {
                # If operation is modify then update specified information
                set nCounter 0
                set sPostCmd ""
                set sPreCmd "mql modify form '$sFormName' web"
                set nCount 0

                # update form information
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch $sKey {
                        web {
                            if {"$sValue" == "false"} {
                                append sPostCmd " notweb"
                            } else {
                                append sPostCmd " web"
                            }
                        }
                        hidden {
                            if {"$sValue" == "false"} {
                                append sPostCmd " nothidden"
                            } else {
                                append sPostCmd " hidden"
                            }
                        }
                    }
                    incr nCount
                }

                # update field information
                foreach sFieldOperation $lFieldOperations {

                    # Get each field info
                    set lFieldInfo [lindex $lFieldInfos $nCounter]
                    set lFieldKeys [lindex $lFieldInfo 0]
                    set lFieldValues [lindex $lFieldInfo 1]

                    # Get field name
                    set nIndex [lsearch $lFieldKeys "name"]
                    if {$nIndex == -1} {
                        return -code 1 "Field name not specified"
                    }
                    set sFieldName [lindex $lFieldValues $nIndex]

                    set lFieldSettings {}
                    # Either add , delete or modify field
                    switch $sFieldOperation {
                        "ADD FIELD" {
                            if {[lsearch $lDefinedFields $sFieldName] != -1} {
                                puts stdout [format ">Field %s Already Exists In Form %s" $sFieldName $sFormName]
                                if {[lsearch $lFieldKeys "user"] != -1} {
                                    set sFieldValue [lindex $lFieldValues [lsearch $lFieldKeys "user"]]
                                    append sPostCmd " field modify name '$sFieldName'"
                                    if {[lsearch $sFieldValue "all"] == -1} {
                                        set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                        if { $mqlret != 0 } {
                                            return -code $mqlret $lUsers
                                        }
                                        foreach sUser $lUsers {
                                            append sPostCmd " add user '$sUser'"
                                        }
                                    } else {
                                        append sPostCmd " add user all"
                                        set lUsers [list "all"]
                                    }
                                    puts stdout [format ">Adding %s Users To Field %s Of Form %s" [join $lUsers ,] $sFieldName $sFormName]
                                }
                                incr nCounter
                                continue
                            } else {
                                puts stdout [format ">Adding Field %s To Form %s" $sFieldName $sFormName]

                                append sPostCmd " field"
                                # Get type and expression defination first
                                set nIndex1 [lsearch $lFieldKeys type]
                                set nIndex2 [lsearch $lFieldKeys expression]
                                if {"$nIndex2" == -1} {
                                    append sPostCmd " select ''"
                                } else {
                                    if {$nIndex1 == -1} {
                                        return -code 1 "type missing for field $sFieldName"
                                    }
                                    append sPostCmd " [lindex $lFieldValues $nIndex1] [lindex $lFieldValues $nIndex2]"
                                }
                            }
                        }
                        "MODIFY FIELD" {
                            if {[lsearch $lDefinedFields $sFieldName] != -1} {
                                # Get settings on the defined fields
                                set sCmd {mql print form "$sFormName" select field\[$sFieldName\].setting dump |}
                                set mqlret [ catch {eval $sCmd} outStr ]
                                if {"$mqlret" != 0} {
                                    return -code 1 $outStr
                                }
                                set lFieldSettings [split $outStr |]
                                puts stdout [format ">Modifying Field %s In Form %s" $sFieldName $sFormName]
                                append sPostCmd " field modify name '$sFieldName'"

                                # Get type and expression info first
                                set nIndex1 [lsearch $lFieldKeys type]
                                set nIndex2 [lsearch $lFieldKeys expression]
                                if {("$nIndex1" == -1 && "$nIndex2" != -1) || ("$nIndex1" != -1 && "$nIndex2" == -1)} {
                                    return -code 1 "type or expression missing for field $sFieldName"
                                }
                                if {"$nIndex1" != -1 && "$nIndex2" != -1} {
                                    append sPostCmd " [lindex $lFieldValues $nIndex1] [lindex $lFieldValues $nIndex2]"
                                }
                            } else {
                                return -code 1 "Field $sFieldName Does Not Exists In Form $sFormName"
                            }
                        }
                        "DELETE FIELD" {
                            if {[lsearch $lDefinedFields $sFieldName] != -1} {
                                puts stdout [format ">Deleteing Field %s From Form %s" $sFieldName $sFormName]
                                append sPostCmd " field delete name '$sFieldName'"
                            } else {
                                puts stdout [format ">Field %s Does Not Exists In Form %s" $sFieldName $sFormName]
                            }
                            incr nCounter
                            continue
                        }
                        default {
                            return -code 1 "Unsupported field operation $sFieldOperation"
                        }
                    }

                    # Get rest of the form info
                    set nCounter2 0
                    foreach sFieldKey $lFieldKeys {
                        set sFieldValue [lindex $lFieldValues $nCounter2]
                        switch -regexp $sFieldKey {
                            name -
                            href -
                            alt -
                            range -
                            order -
                            label {
                                append sPostCmd " $sFieldKey '$sFieldValue'"
                            }
                            "^add setting*" -
                            "^setting*" {
                                set nOpenIndex [string first "\[" "$sFieldKey"]
                                set nCloseIndex [string first "\]" "$sFieldKey"]
                                set sSettingName [string range "$sFieldKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                if {[lsearch $lFieldSettings "$sSettingName"] == -1} {
                                    append sPostCmd " setting '$sSettingName' '$sFieldValue'"
                                } else {
                                    append sPostCmd " remove setting '$sSettingName' add setting '$sSettingName' '$sFieldValue'"
                                }
                            }
                            "^remove setting*" {
                                set nOpenIndex [string first "\[" "$sFieldKey"]
                                set nCloseIndex [string first "\]" "$sFieldKey"]
                                set sSettingName [string range "$sFieldKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                if {[lsearch $lFieldSettings "$sSettingName"] >= 0} {
                                    append sPostCmd " remove setting '$sSettingName'"
                                }
                            }
                            "add user" {
                                if {[lsearch $sFieldValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " add user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " add user all"
                                    #append sPostCmd " user all"
                                }
                            }
                            #Added a case when setting user is provided
                            "user" {
                                if {[lsearch $sFieldValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " user all"
                                }
                            }
                            "remove user" {
                                if {[lsearch $sFieldValue "all"] == -1} {
                                    set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                    if { $mqlret != 0 } {
                                        return -code $mqlret $lUsers
                                    }
                                    foreach sUser $lUsers {
                                        append sPostCmd " remove user '$sUser'"
                                    }
                                } else {
                                    append sPostCmd " remove user all"
                                }
                            }
                        }
                        incr nCounter2
                    }

                    incr nCounter
                }

                if {"$sPostCmd" != ""} {
                    puts stdout [format ">Modifying Form %s" $sFormName]
                    set sCmd "${sPreCmd}${sPostCmd}"
                    append sCmd " property application value '$sApplication'"
                    append sCmd " property version value '$sVersion'"
                    lappend lCmd $sCmd
                }
            }

            "DELETE FORM" {
                # If Operation is Delete then delete specified Form
                puts stdout [format ">Deleting Form %s" $sFormName]
                lappend lCmd "mql delete form '$sFormName' web"
            }
            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    } else {
        switch $sOperation {
            "ADD FORM" {
                # If operation is ADD then add the specified form
                # Rename the form if form with same name already exists
                set lAllForms [split [mql list form] \n]
                set sFormOrigName "$sFormName"
                if {[lsearch $lAllForms "$sFormName"] >= 0} {
                    puts stdout [format ">Renaming Form %s to %s" "$sFormName" "$sPrefixName$sFormName"]
                    set sFormName "$sPrefixName$sFormName"
                }
                puts stdout [format ">Adding Form %s" "$sFormName"]

                # Add From Form Info
                set sCmd "mql add form '$sFormName' web"
                set nCount 0
                foreach sKey $lKeys {
                    set sValue [lindex $lValues $nCount]
                    switch $sKey {
                        hidden {
                            if {"$sValue" == "false"} {
                                append sCmd " nothidden"
                            } else {
                                append sCmd " hidden"
                            }
                        }
                    }
                    incr nCount
                }

                # Add from field info
                set nCounter 0
                foreach sFieldOperation $lFieldOperations {
                    if {"$sFieldOperation" == "ADD FIELD"} {
                        set lFieldInfo [lindex $lFieldInfos $nCounter]
                        set lFieldKeys [lindex $lFieldInfo 0]
                        set lFieldValues [lindex $lFieldInfo 1]
                        set nIndex [lsearch $lFieldKeys "name"]
                        if {$nIndex == -1} {
                            return -code 1 "Field name not specified"
                        }

                        set sFieldName [lindex $lFieldValues $nIndex]
                        puts stdout [format ">Adding Field %s To Form %s" $sFieldName $sFormName]

                        # Get type and expression defination first
                        set nIndex1 [lsearch $lFieldKeys type]
                        set nIndex2 [lsearch $lFieldKeys expression]
                        if {"$nIndex2" == -1} {
                            append sCmd " field select ''"
                        } else {
                            if {$nIndex1 == -1} {
                                return -code 1 "type missing for field $sFieldName"
                            }
                            append sCmd " field [lindex $lFieldValues $nIndex1] '[lindex $lFieldValues $nIndex2]'"
                        }

                        set nCounter2 0
                        foreach sFieldKey $lFieldKeys {
                            set sFieldValue [lindex $lFieldValues $nCounter2]
                            switch -regexp $sFieldKey {
                                name -
                                href -
                                alt -
                                range -
                                order -
                                label {
                                    append sCmd " $sFieldKey '$sFieldValue'"
                                }
                                "^add setting*" -
                                "^setting*" {
                                    set nOpenIndex [string first "\[" "$sFieldKey"]
                                    set nCloseIndex [string first "\]" "$sFieldKey"]
                                    set sSettingName [string range "$sFieldKey" [expr $nOpenIndex + 1] [expr $nCloseIndex - 1]]
                                    append sCmd " setting '$sSettingName' '$sFieldValue'"
                                }
                                "user" {
                                    if {[lsearch $sFieldValue "all"] == -1} {
                                        set mqlret [ catch {eval emxGetAdminNamesFromProps {$sFieldValue}} lUsers ]
                                        if { $mqlret != 0 } {
                                            return -code $mqlret $lUsers
                                        }
                                        foreach sUser $lUsers {
                                            append sCmd " user '$sUser'"
                                        }
                                    } else {
                                        append sCmd " user all"
                                    }
                                }
                            }
                            incr nCounter2
                        }
                    }
                    incr nCounter
                }
                append sCmd " property application value '$sApplication'"
                append sCmd " property version value '$sVersion'"
                append sCmd " property installer value '$sInstallOrg'"
                append sCmd " property 'installed date' value '$sDate'"
                append sCmd " property 'original name' value '$sFormOrigName'"
                lappend lCmd $sCmd

                lappend lCmd "mql add property '$sProperty' on program '$sRegProgName' to form '$sFormName'"
            }

            "MODIFY FORM" {
                    return -code 1 "Form $sFormName does not exists"
            }

            "DELETE FORM" {
                # If operation is Delete then do nothing as Form doesnot exists.
                puts stdout [format ">Form %s Does Not Exists" "$sFormName"]
            }

            default {
                return -code 1 "Unsupported operation $sOperation"
            }
        }
    }

    return -code $nRet $lCmd
}


#############################################################################
# Procedure Name : emxInstallProgram
# Brief Description : This procedure installs programs
#
# Input : 1. lProgramList
#         Example :
#                  {Program1}
#                  {access=READ_ONLY} \
#                  {file=$sInstallDir/Common/Program1.java}
#                  {description=Description1}
#                  {hidden=True}
#                  {type=JAVA}
# Outout : On Failure : returns 1
#          On Success : returns 0
#############################################################################

proc emxInstallProgram {lProgramList} {

    # env variables
    set sInstallDir [mql get env MXAPPBUILD]
    set sRunMode [mql get env MXRUNMODE]
    set sVersion [mql get env MXVERSION]
    set sApplication [mql get env MXAPPLICATION]
    set sInstaller [mql get env MXORGNAME]
    set sMode [mql get env MXMODE]
    set sDate [clock format [clock seconds] -format %m-%d-%Y]
    set lInitialProgs [mql get env MXINITIALPROGS]
    set sKernelType [string trim [lindex [split [mql print system inivar MX_DEFAULT_KERNEL] "="] 1]]

    if {"$lInitialProgs" == ""} {
        set sCmd {mql list program}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxInstallProgram"
            puts stdout ">$outStr"
            return 1
        }
        
        mql set env MXINITIALPROGS [split "$outStr" \n]
        set lInitialProgs [split "$outStr" \n]
    }

    # Return Variable
    set mqlall 0

    # For each program listed in the script
    foreach lProgramInfo $lProgramList {
        set sProgramName [lindex $lProgramInfo 0]

        set lKeyValues [emxGetKeyValuePairs $lProgramInfo]
        set lKeys [lindex $lKeyValues 0]
        set lValues [lindex $lKeyValues 1]

        set nBaseIndex [lsearch $lKeys "base"]
        set nFileIndex [lsearch $lKeys "file"]

        set sProgramOrPage "program"

        # If neither base or file key values are defined, try to default them
        # based on the program type
        #     type=java:
        #         base=$sInstallDir/../../JPOSrc
        #     type=external, mql:
        #         file=$sInstallDir/AppInstall/Programs/$sProgramName
        if {$nBaseIndex == -1 && $nFileIndex == -1} {
            set nTypeIndex [lsearch $lKeys "type"]
            if {$nTypeIndex > -1} {
                switch [lindex $lValues $nTypeIndex] {
                    java {
                        lappend lKeys base
                        if { "$sRunMode" == "MKRTV" } {
                            lappend lValues "$sInstallDir/../../code/commands"
                        } else {
                            lappend lValues "$sInstallDir/../../JPOSrc"
                        }
                        set nBaseIndex [lsearch $lKeys "base"]
                    }

                    external -
                    mql {
                        lappend lKeys file
                        lappend lValues "$sInstallDir/AppInstall/Programs/$sProgramName"
                        set nFileIndex [lsearch $lKeys "file"]
                    }

                    page {
                        lappend lKeys file
                        lappend lValues "$sInstallDir/AppInstall/Pages/$sProgramName"
                        set nFileIndex [lsearch $lKeys "file"]
                        set sProgramOrPage "page"
                    }

                    default {
                        puts stdout [format ">Checking    : %-13s%-54s" program "$sProgramName"]
                        puts stdout ">ERROR       : emxInstallProgram"
                        puts stdout ">Could not determine default location of file for [lindex $lValues $nTypeIndex] program type"
                        set mqlall 1
                        continue
                    }
                }
            } else {
                puts stdout [format ">Checking    : %-13s%-54s" program "$sProgramName"]
                puts stdout ">ERROR       : emxInstallProgram"
                puts stdout ">No type defined for program object to set default location"
                set mqlall 1
                continue
            }
        }

        # Check whether path to program object exists, based on how it was 
        # specified in its definition. If program file does not exist and
        # a full install is being done, raise an error.
        if {"$nFileIndex" >= 0} {
            set sFileValue [lindex $lValues "$nFileIndex"]
            if {"$sFileValue" != "NONE"} {
                set bFileExists [eval "file exists $sFileValue"]
                if {$bFileExists == 0} {
                    if {$sMode == "FULL_MODE"} {
                        puts stdout [format ">Checking    : %-13s%-54s" program "$sProgramName"]
                        puts stdout ">ERROR       : emxInstallProgram"
                        puts stdout ">File, $sFileValue, not found"
                        set mqlall 1
                    }
                    continue
                }
            }
        }

        if {"$nBaseIndex" >= 0} {
            set sBaseValue [lindex $lValues "$nBaseIndex"]
            regsub -all "\\." "$sProgramName" "/" sProgramNameNew
            set bFileExists 0
            if {"$sKernelType" == "ematrix.net"} {
                set sFileValue "${sBaseValue}/${sProgramNameNew}_mxJPO.jsl"
                set bFileExists [file exists $sFileValue]
            }
            if {$bFileExists == 0} {
                set sFileValue "${sBaseValue}/${sProgramNameNew}_mxJPO.java"
                set bFileExists [file exists $sFileValue]
            }

            if {$bFileExists == 0} {
                if {$sMode == "FULL_MODE"} {
                    puts stdout [format ">Checking    : %-13s%-54s" program "$sProgramName"]
                    puts stdout ">ERROR       : emxInstallProgram"
                    puts stdout ">File, ${sProgramName}_mxJPO.java, not found in base directory, $sBaseValue"
                    set mqlall 1
                }
                continue
            }
        }

        set sCmd {mql list $sProgramOrPage "$sProgramName"}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxInstallProgram"
            puts stdout ">$outStr"
            set mqlall 1
            continue
        }

        # If program already exists in the database then
        if {"$sProgramName" == "$outStr"} {

            set sCmd {mql print $sProgramOrPage "$sProgramName" \
                          select \
                          property\[access\].value \
                          dump | \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : emxInstallProgram"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }
            
            set sProgAcess "$outStr"

            # If force install is set
            set nForceIndex [lsearch $lKeys "force"]
            set sAccessVal [lindex $lValues [lsearch $lKeys "access"]]
            if {"$nForceIndex" >= 0 && "$sAccessVal" != "$sProgAcess"} {

                puts stdout [format ">Setting     : %-13s%-54s%-15s" $sProgramOrPage "$sProgramName" "$sAccessVal"]

                set sCmd {mql modify $sProgramOrPage "$sProgramName" \
                              property access value "$sAccessVal" \
                              property version value "$sVersion" \
                              property application value "$sApplication" \
                              property "installed date" value "$sDate" \
                              property "installer" value "$sInstaller" \
                              }
                if {"$sAccessVal" == "READ_WRITE"} {
                    puts stdout [format ">Modifying   : %-13s%-54s%-15s" $sProgramOrPage "$sProgramName" "$sVersion"]

                    foreach sKey $lKeys sValue $lValues {
                        switch $sKey {
                            description {
                                append sCmd " $sKey '$sValue'"
                            }
                            file {
                                if {"$sValue" != "NONE"} {
                                    append sCmd " $sKey '$sValue'"
                                }
                            }
                            "access" {
                                append sCmd " property 'access' value '$sValue'"
                            }
                            type {
                                if {"$sValue" != "page"} {
                                append sCmd " $sValue"
                                }
                            }
                            hidden -
                            pooled {
                                if {[string toupper "$sValue"] == "TRUE"} {
                                    append sCmd " $sKey"
                                } else {
                                    append sCmd " not$sKey"
                                }
                            }
                            base {
                                regsub -all "\\." "$sProgramName" "/" sFileName
                                if {"$sKernelType" == "ematrix.net"} {
                                    set sCheckFileName "$sValue/${sFileName}_mxJPO.jsl"
                                    set bJSLFileExists [file exists $sCheckFileName]
                                    if {$bJSLFileExists == 0} {
                                        set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                                    }
                                } else {
                                    set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                                }
                                append sCmd " file '$sCheckFileName'"
                            }
                        }
                    }
                }
                set mqlret [ catch {eval $sCmd} outStr ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallProgram"
                    puts stdout ">$outStr"
                    set mqlall 1
                    continue
                }
            } else {

                # If property Access doesn't exists on the program then
                if {"$sProgAcess" == ""} {
                    # If Base JPO program exists for current program then
                    if {[lsearch $lInitialProgs "${sProgramName}Base"] < 0} {

                        # If Base JPO program exists for current program then
                        puts stdout [format ">Modifying   : %-13s%-54s%-15s" $sProgramOrPage "$sProgramName" $sVersion]

                        set sCmd {mql modify $sProgramOrPage "$sProgramName" \
                                      property version value "$sVersion" \
                                      property application value "$sApplication" \
                                      property "installed date" value "$sDate" \
                                      property "installer" value "$sInstaller" \
                                      }

                        foreach sKey $lKeys sValue $lValues {
                            switch $sKey {
                                description {
                                    append sCmd " $sKey '$sValue'"
                                }
                                file {
                                    if {"$sValue" != "NONE"} {
                                        append sCmd " $sKey '$sValue'"
                                    }
                                }
                                "access" {
                                    append sCmd " property 'access' value '$sValue'"
                                }
                                type {
                                    if {"$sValue" != "page"} {
                                    append sCmd " $sValue"
                                }
                                }
                                hidden -
                                pooled {
                                    if {[string toupper "$sValue"] == "TRUE"} {
                                        append sCmd " $sKey"
                                    } else {
                                        append sCmd " not$sKey"
                                    }
                                }
                                base {
                                    regsub -all "\\." "$sProgramName" "/" sFileName
                                    if {"$sKernelType" == "ematrix.net"} {
                                        set sCheckFileName "$sValue/${sFileName}_mxJPO.jsl"
                                        set bJSLFileExists [file exists $sCheckFileName]
                                        if {$bJSLFileExists == 0} {
                                            set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                                        }
                                    } else {
                                        set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                                    }
                                    append sCmd " file '$sCheckFileName'"
                                }
                            }
                        }
                        set mqlret [ catch {eval $sCmd} outStr ]
                        if {$mqlret != 0} {
                            puts stdout ">ERROR       : emxInstallProgram"
                            puts stdout ">$outStr"
                            set mqlall 1
                            continue
                        }
                    } else {
                        puts stdout [format ">Exists      : %-13s%-54s" "$sProgramOrPage" "$sProgramName"]
                        set sAccess [lindex $lValues [lsearch $lKeys "access"]]
                        set sCmd {mql modify $sProgramOrPage "$sProgramName" \
                                      property access value "$sAccess" \
                                      }
                        set mqlret [ catch {eval $sCmd} outStr ]
                        if {$mqlret != 0} {
                            puts stdout ">ERROR       : emxInstallProgram"
                            puts stdout ">$outStr"
                            set mqlall 1
                            continue
                        }
                    }
                # If Access = READ_ONLY then
                } elseif {"$sProgAcess" == "READ_ONLY"} {
                    puts stdout [format ">Exists      : %-13s%-54s" "$sProgramOrPage" "$sProgramName"]
                # Else if Access = READ_WRITE then
                } else {
                    puts stdout [format ">Modifying   : %-13s%-54s%-15s" $sProgramOrPage "$sProgramName" $sVersion]

                    set sCmd {mql modify $sProgramOrPage "$sProgramName" \
                                  property version value "$sVersion" \
                                  property application value "$sApplication" \
                                  property "installed date" value "$sDate" \
                                  property "installer" value "$sInstaller" \
                                  }

                    foreach sKey $lKeys sValue $lValues {
                        switch $sKey {
                            description {
                                append sCmd " $sKey '$sValue'"
                            }
                            file {
                                if {"$sValue" != "NONE"} {
                                    append sCmd " $sKey '$sValue'"
                                }
                            }
                            type {
                                if {"$sValue" != "page"} {
                                append sCmd " $sValue"
                                }
                            }
                            hidden -
                            pooled {
                                if {[string toupper "$sValue"] == "TRUE"} {
                                    append sCmd " $sKey"
                                } else {
                                    append sCmd " not$sKey"
                                }
                            }
                            base {
                                regsub -all "\\." "$sProgramName" "/" sFileName
                                if {"$sKernelType" == "ematrix.net"} {
                                    set sCheckFileName "$sValue/${sFileName}_mxJPO.jsl"
                                    set bJSLFileExists [file exists $sCheckFileName]
                                    if {$bJSLFileExists == 0} {
                                        set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                                    }
                                } else {
                                    set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                                }
                                append sCmd " file '$sCheckFileName'"
                            }
                        }
                    }
                    set mqlret [ catch {eval $sCmd} outStr ]
                    if {$mqlret != 0} {
                        puts stdout ">ERROR       : emxInstallProgram"
                        puts stdout ">$outStr"
                        set mqlall 1
                        continue
                    }
                }
            }
        # Else if program doesn't exists then
        } else {
            puts stdout [format ">Adding      : %-13s%-54s%-15s" $sProgramOrPage "$sProgramName" $sVersion]
            set sCmd {mql add $sProgramOrPage "$sProgramName" \
                          property version value "$sVersion" \
                          property application value "$sApplication" \
                          property "installed date" value "$sDate" \
                          property "installer" value "$sInstaller" \
                          property "original name" value "$sProgramName" \
                          }

            foreach sKey $lKeys sValue $lValues {
                switch $sKey {
                    description {
                        append sCmd " $sKey '$sValue'"
                    }
                    file {
                        if {"$sValue" != "NONE"} {
                            append sCmd " $sKey '$sValue'"
                        }
                    }
                    "access" {
                        append sCmd " property 'access' value '$sValue'"
                    }
                    type {
                        if {"$sValue" != "page"} {
                        append sCmd " $sValue"
                        }
                    }
                    hidden -
                    pooled {
                        if {[string toupper "$sValue"] == "TRUE"} {
                            append sCmd " $sKey"
                        } else {
                            append sCmd " not$sKey"
                        }
                    }
                    base {
                        regsub -all "\\." "$sProgramName" "/" sFileName
                        if {"$sKernelType" == "ematrix.net"} {
                            set sCheckFileName "$sValue/${sFileName}_mxJPO.jsl"
                            set bJSLFileExists [file exists $sCheckFileName]
                            if {$bJSLFileExists == 0} {
                                set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                            }
                        } else {
                            set sCheckFileName "$sValue/${sFileName}_mxJPO.java"
                        }
                        append sCmd " file '$sCheckFileName'"
                    }
                }
            }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : emxInstallProgram"
                puts stdout ">$outStr"
                set mqlall 1
                continue
            }
        }
    }

    return $mqlall
}

#############################################################################
# Procedure Name : emxInstallAdmin
# Brief Description : This procedure installs UI Component
#
# Input : 1. lAdminList
#         Example :
#                  {ADD}
#                  {name=MENUNAME}
#                  {label=LABELNAME}
#                  {icon=FILENAME}
#                  {href="STRING"}
#                  {command=COMMANDNAME1}
#                  {command=COMMANDNAME2}
#                  {command=COMMANDNAME3}
#                  {command=COMMANDNAME4}
#                  {menu=MENUNAME1}
#                  {menu=MENUNAME2}
#          2. sAdminCategory - Admin type
# Outout : On Failure : returns 1
#          On Success : returns 0
#############################################################################

proc emxInstallAdmin {lAdminList sAdminCategory} {


    # Get MQL environment variables
    set RegProgName [mql get env REGISTRATIONOBJECT]
    set sLanguage [mql get env MXLANGUAGE]
    set sPixDir [mql get env MXPIXMAPDIR]
    set sInstallDir [mql get env MXAPPBUILD]
    set lVersions [mql get env MXVERSIONLISTTOINSTALL]
    set sMode [mql get env MXMODE]
    set sPrefName [mql get env MXPREFIXNAME]
    set sVersion [lindex $lVersions 0]

    # Return Variable
    set mqlall 0

    foreach lAdminInfo $lAdminList {

        set mqlret 0
        set sOperation [lindex $lAdminInfo 0]
        set nCounter 0
        if {"$sAdminCategory" == "table" || "$sAdminCategory" == "form"} {
            foreach sItem $lAdminInfo {
                if {"$sItem" == "ADD COLUMN" || \
                    "$sItem" == "MODIFY COLUMN" || \
                    "$sItem" == "DELETE COLUMN" || \
                    "$sItem" == "ADD FIELD" || \
                    "$sItem" == "MODIFY FIELD" || \
                    "$sItem" == "DELETE FIELD" \
                    } {
                    
                    break
                }
                incr nCounter
            }
            set lTableAdminInfo [lrange $lAdminInfo 0 [expr $nCounter - 1]]
            set lKeyValues [emxGetKeyValuePairs $lTableAdminInfo]

            set lColumnOperations {}
            set lColumnAdminInfos {}
            set lColumnAdminInfo [lrange $lAdminInfo $nCounter end]
            set lTempList {}
            foreach sItem $lColumnAdminInfo {
                if {"$sItem" == "ADD COLUMN" || \
                    "$sItem" == "MODIFY COLUMN" || \
                    "$sItem" == "DELETE COLUMN" || \
                    "$sItem" == "ADD FIELD" || \
                    "$sItem" == "MODIFY FIELD" || \
                    "$sItem" == "DELETE FIELD" \
                    } {
                    lappend lColumnOperations "$sItem"
                    if {[llength $lTempList] != 0} {
                        set lTempKeyValues [emxGetKeyValuePairs $lTempList]
                        lappend lColumnAdminInfos $lTempKeyValues
                        set lTempList {}
                    }
                } else {
                    lappend lTempList "$sItem"
                }
            }
            set lTempKeyValues [emxGetKeyValuePairs $lTempList]
            lappend lColumnAdminInfos $lTempKeyValues
        } else {
            set lKeyValues [emxGetKeyValuePairs $lAdminInfo]
        }
        set sAdminProp [emxGetValueFromKey $lKeyValues property]
        if {"$sAdminProp" == ""} {
            puts stdout ">ERROR       : emxInstallAdmin"
            puts stdout ">Property name not specified"
            set mqlall 1
            continue
        }

        set sAdminName [emxGetValueFromKey $lKeyValues name]

        # Store Admin Name in Menulist
        set sAdminNameOriginal $sAdminName

        # Check if Admin object is already installed in the database
        set sCmd {mql print program "$RegProgName" select property\[$sAdminProp\].to dump}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxInstallAdmin"
            puts stdout ">$outStr"
            set mqlall 1
            continue
        }
        if {"$outStr" != ""} {
            set bAdminInstalled 1
            set sAdminName [lrange [split $outStr] 1 end]
        } else {
            set bAdminInstalled 0
        }

        # Form MQL commands from Admin Info to install it
        # and execute them.
        switch "$sAdminCategory" {

            command {
                set mqlret [ catch {eval emxCreateMQLForCommand {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            menu {
                set mqlret [ catch {eval emxCreateMQLForMenu {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            inquiry {
                set mqlret [ catch {eval emxCreateMQLForInquiry {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            table {
                set mqlret [ catch {eval emxCreateMQLForTable {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation} {$lColumnOperations} {$lColumnAdminInfos}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            form {
                set mqlret [ catch {eval emxCreateMQLForForm {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation} {$lColumnOperations} {$lColumnAdminInfos}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            channel {
                set mqlret [ catch {eval emxCreateMQLForChannel {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            portal {
                set mqlret [ catch {eval emxCreateMQLForPortal {$lKeyValues} {$sAdminName} {$bAdminInstalled} {$sOperation}} lCmd ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$lCmd"
                    set mqlall 1
                    continue
                }
            }

            default {
                puts stdout ">ERROR       : emxInstallAdmin"
                puts stdout ">Unsupported Admin Type $sAdminCategory"
                set mqlall 1
                continue
            }
        }

        if {[llength $lCmd] > 0} {
            foreach sCmd $lCmd {
                if {[info exists ::env(LOG_MQL)] && "$::env(LOG_MQL)" == "TRUE"} {
                    puts "$sCmd"
                }
                set mqlret [ catch {eval $sCmd} outStr ]
                if {$mqlret != 0} {
                    puts stdout ">ERROR       : emxInstallAdmin"
                    puts stdout ">$outStr"
                    set mqlall 1
                    break
                }
            }
        }
    }

    return $mqlall
}


#############################################################################
# Procedure Name : emxGetPropertyFromAdminName
# Brief Description : This procedure accepts a AdminType, registration program
#                     name and Admin name and returns property pointing to
#                     specified admin.
# Input : 1. sAdminType
#            Ex: type,attribute,policy
#         2. sRegProg
#            Ex: eServiceSchemaVariableMapping.tcl
#         3. sAdmiName
#            Ex: Inbox Task, Route
# Outout : Returns error or property name.
#############################################################################

proc emxGetPropertyFromAdminName {sAdminType sRegProg sAdminName} {

    # get to properties to get property names.
    set sCmd {mql list property to $sAdminType "$sAdminName"}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        return -code 1 $outStr
    }
    set lPropInfo [split $outStr \n]

    # foreach property check from side = sRegProg
    foreach sPropInfo $lPropInfo {
        set sFromAdminType [lindex [split $sPropInfo] 2]
        set sFromAdminName [lindex [split $sPropInfo] 3]

        if {"$sFromAdminType" == "program" && "$sFromAdminName" == "$sRegProg"} {
            return -code 0 [lindex [split $sPropInfo] 0]
        }
    }

    # return unregistered admin error
    return -code 1 "$sAdminType $sAdminName not registered"
}

#############################################################################
# Procedure Name : emxGetPropertyFromStateName
# Brief Description : This procedure accepts policy and statename and
#                     returns registered state property.
# Input : 1. sPolicyName
#         2. sStateName
# Outout : Returns error or property name.
#############################################################################

proc emxGetPropertyFromStateName {sPolicyName sStateName} {

    # Get all the properties on specified policy
    set sCmd {mql print policy "$sPolicyName" select property dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        return -code 1 $outStr
    }

    # Search for right property and return.
    set lOutput [split $outStr |]
    foreach sPropInfo $lOutput {
        if {[string first "state_" "$sPropInfo"] == 0} {
            set lPropInfo [split $sPropInfo]
            set sStateNameFromProp [join [lrange $lPropInfo 2 end]]
            if {"$sStateNameFromProp" == "$sStateName"} {
                set sProperty [lindex $lPropInfo 0]
                return -code 0 $sProperty
            }
        }
    }

    # return unregistered state error
    return -code 1 "state $sStateName not registered"
}

#############################################################################
# Procedure Name : emxCheckFutureSchema
# Brief Description : This procedure check if any Future Schema exists afte
#                     completing whole install
#############################################################################

proc emxCheckFutureSchema {} {

    # list of all the admin objects
    set lAdmins [list attribute \
                      type \
                      relationship \
                      policy \
                      user \
                      format \
                      vault \
                      store \
                      wizard \
                      ]
        
    set bItemFound 0
    # for each admin
    foreach sAdmin $lAdmins {
        # Get all the admin item installed
        set sCmd {mql list $sAdmin}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            return -code 1 $outStr
        }
        set lAdminNames [split $outStr \n]
        # for each admin item
        foreach sAdminName $lAdminNames {
            # Get application and version property
            set sCmd {mql print $sAdmin "$sAdminName" \
                          select \
                          property\[application\].value \
                          property\[version\].value \
                          dump tcl \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                return -code 1 $outStr
            }
            set sApplication [lindex [lindex [lindex $outStr 0] 0] 0]
            set sVersion [lindex [lindex [lindex $outStr 0] 1] 0]
            
            # If application property is set to FrameworkFuture
            # then log message.
            if {"$sApplication" == "FrameworkFuture"} {
                if {"$bItemFound" == 0} {
                    puts stdout ""
                    puts stdout ">Found       : FrameworkFuture On Following Items"
                    set bItemFound 1
                }
                puts stdout [format ">%-12s: %-13s%61s" $sAdmin $sAdminName $sVersion]
            }
        }
    }

    # If any future item found then
    # give reference to AEF guide
    if {$bItemFound == 1} {
        puts stdout ">Refer       : 'Configuring Schema as Future Framework Schema' in the AEF Guide"
    }
    
    return -code 0
}

#############################################################################
# Procedure Name : emxSetVersion
# Brief Description : This procedure sets application version.
#############################################################################
proc emxSetVersion {sName sVersion lDependentMods sModuleType} {

    set sAppPropCmd ""
    set sFeaturePropCmd ""
    foreach sDependentMod $lDependentMods {
        append sAppPropCmd " property\\\[appVersion${sDependentMod}\\\].value"
        append sFeaturePropCmd " property\\\[featureVersion${sDependentMod}\\\].value"
    }

    set sCmd "mql print program eServiceSystemInformation.tcl select $sAppPropCmd dump tcl"
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        return -code 1 $outStr
    }
    set lAppOutput [lindex $outStr 0]

    set sCmd "mql print program eServiceSystemInformation.tcl select $sFeaturePropCmd dump tcl"
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        return -code 1 $outStr
    }
    set lFeatureOutput [lindex $outStr 0]

    set bDependentsFound "TRUE"
    foreach sAppOutput $lAppOutput sFeatureOutput $lFeatureOutput {
        if {[lindex $sAppOutput 0] != "$sVersion" && [lindex $sFeatureOutput 0] != "$sVersion"} {
            set bDependentsFound "FALSE"
            break
        }
    }

    if {"$bDependentsFound" == "TRUE"} {
        set sOldModuleType [mql get env MX_MODULE_TYPE]
        if {"$sModuleType" == "" || "$sModuleType" == "APPLICATION"} {
            mql set env MX_MODULE_TYPE "APPLICATION"
            set mqlret  [eServiceApplicationSetVersion "eServiceSystemInformation.tcl" "$sName" "$sVersion"]
            if { $mqlret != 0 } {
                return -code 1
            }
        } else {
            mql set env MX_MODULE_TYPE "FEATURE"
            set mqlret  [eServiceApplicationSetVersion "eServiceSystemInformation.tcl" "$sName" "$sVersion"]
            if { $mqlret != 0 } {
                return -code 1
            }
        }
        mql set env MX_MODULE_TYPE "$sOldModuleType"
    }
}

###############################################################################
#
# Procedure:    emxUpdateHelpAbout
#
# Description:  Procedure to update help about properties
#
# Parameters:   lOldApp - Old application settings to be removed
#               sNewApp - New application setting to be added
#
# Returns:
###############################################################################

proc emxUpdateHelpAbout { lOldApp sNewApp} {

    # get registartion object.
    set sRegObj [mql get env global VERSIONREGISTRATIONOBJECT]

    # get all the properties on it.
    set sCmd {mql print program "$sRegObj" select property.name property.value dump tcl}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxUpdateHelpAbout"
        puts stdout ">$outStr"
        return 1
    }
    set lProperty [lindex [lindex $outStr 0] 0]
    set lValue [lindex [lindex $outStr 0] 1]
    
    set sOldAppFound 0

    # foreach old application
    foreach sOldApp $lOldApp {
        set sOldAppProp "appVersion${sOldApp}"
        # if old app already exists
        if {[lsearch $lProperty "$sOldAppProp"] >= 0} {

            set sOldAppFound 1
            # get old application value
            set sNewAppVersion [lindex $lValue [lsearch $lProperty "$sOldAppProp"]]

            # delete old application setting.
            set sCmd {mql delete property "$sOldAppProp" on program "$sRegObj"}
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                puts stdout ">ERROR       : emxUpdateHelpAbout"
                puts stdout ">$outStr"
                return 1
            }
            break
        }
    }

    # if old application found then
    # add new application property
    if {"$sOldAppFound" == "1"} {
        set sNewAppProp "appVersion${sNewApp}"
        set sCmd {mql modify program "$sRegObj" property "$sNewAppProp" value "$sNewAppVersion"}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxUpdateHelpAbout"
            puts stdout ">$outStr"
            return 1
        }
    }
    
    # Modify code of eServiceSystemInformation.tcl
    set sCmd {mql print program "$sRegObj" select code dump}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxUpdateHelpAbout"
        puts stdout ">$outStr"
        return 1
    }
    set sCode "$$outStr"
    set bCodeChanged "FALSE"
    # foreach old application
    foreach sOldApp $lOldApp {
         set nReplaceCount [regsub -all "${sOldApp} " "$sCode" "${sNewApp} " sCode]
         if {$nReplaceCount > 0} {
             set bCodeChanged "TRUE"
         }
    }
    
    if {$bCodeChanged == "TRUE"} {
        set sCmd {mql modify program "$sRegObj" code "$sCode"}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxUpdateHelpAbout"
            puts stdout ">$outStr"
            return 1
        }
    }
}

###############################################################################
#
# Procedure:    emxTurnOnIndex
#
# Description:  Procedure to update help about properties
#
# Parameters:   lApps - application list to be activated.
#
# Returns:
###############################################################################

proc emxTurnOnIndex {lApps} {
    set sPodName [string trim [lindex [split [mql print system iniVar PodDefinitionName] "="] 1]]
    if {"$sPodName" != ""} {
        return
    }
    set sInstallDir [mql get env MXAPPBUILD]
    set sVersion [file tail [file normalize "$sInstallDir/../.."]]
    set sConfigFile "$sInstallDir/../../../../BusinessProcessServices/$sVersion/Modules/ENOFramework/AppInstall/Programs/config.xml"
    if {[file exists "$sConfigFile"]} {

        set temp ""
        #saves each line to an arg in a temp list
        set file [open "$sConfigFile"]
        foreach {line} [split [read $file] \n] {
            lappend temp $line
        }
        close $file
        #rewrites your file
        set file [open ${sConfigFile}.temp w+]

        foreach {line} $temp {
            if {[string first "<APPLICATION" $line] >= 0} {
                set lLine [split [string trim "$line"] " "]
                set sApp [lindex [split [lindex $lLine 1] "="] 1]
                set sApp [string range "$sApp" 1 [expr [string length "$sApp"] - 2]]
                if {[string first "=\"false\"" "$line"] >= 0 && [lsearch $lApps "$sApp"] >= 0} {
                    set line [string map {false true} "$line"]
                }
            }
            puts $file "$line"
        }
        close $file
        file delete -force "$sConfigFile"
        file rename -force "${sConfigFile}.temp"  "$sConfigFile"
        
        set MQLCmd "mql exec program emxIndexUtil -method setActiveApps"
        foreach app $lApps {
            append MQLCmd " $app"
        }
        set mqlret [ catch {eval $MQLCmd} outStr ]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxTurnOnIndex"
            puts stdout ">$outStr"
            return 1
        }
    }
}


