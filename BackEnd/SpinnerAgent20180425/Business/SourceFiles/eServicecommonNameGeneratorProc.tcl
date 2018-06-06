###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
#
# $RCSfile: eServicecommonNameGeneratorProc.tcl.rca $ $Revision: 1.40 $
#
# @progdoc      eServicecommonNameGeneratorProc.tcl
#
# @Brief:       The name generator procedure
#
# @Description: This program is used to automatically generate unique names for objects
#
# @Parameters:  sObjectType               - Type of an object for which to generate name
#
#               sObjectRev                - Revision of the object for which to generate name (Optional)
#
#               sObjectPolicy             - Policy supported by the type specified (Optional)
#
# @Returns:     A pipe seperated list of 0, ObjectId, ObjectType, ObjectName, ObjectRev, ObjectVault in sucess
#               A pipe seperated list of 1,Error message in failure
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
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
# end utLoad
###############################################################################

proc eServiceNameGeneratorProc { sObjectType {sObjectRev ""} {sObjectPolicy ""} {sObjectVault ""} {sTypeSearchPattern ""} {bExpand "TRUE"} {sVaultSearchPattern "*"} {bUniqueNameOnly "FALSE"} {bUseSuperUser "TRUE"}} {

    eval  [utLoad eServiceSchemaVariableMapping.tcl]
    eval  [utLoad eServicecommonDEBUG.tcl]
    eval  [utLoad eServicecommonShadowAgent.tcl]
    eval  [utLoad eServicecommonPSUtilities.tcl]

    # set program related variables
    set progname      "eServicecommonNameGeneratorProc.tcl"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set nPassed       0

    # Name Generator Parameter
    eval  [mql print program eServicecommonNameGeneratorParameters.tcl select code dump]

    # trim the input values of spaces on either side, if any
    set sObjectType         [string trim "$sObjectType"]
    set sObjectRev          [string trim "$sObjectRev"]
    set sObjectPolicy       [string trim "$sObjectPolicy"]
    set sObjectVault        [string trim "$sObjectVault"]
    set sTypeSearchPattern  [string trim "$sTypeSearchPattern"]
    set bExpand             [string toupper [string trim "$bExpand"]]
    set bUseSuperUser       [string toupper [string trim "$bUseSuperUser"]]
    set bUniqueNameOnly     [string toupper [string trim "$bUniqueNameOnly"]]
    set sVaultSearchPattern [string trim "$sVaultSearchPattern"]
    if {"$sTypeSearchPattern" == ""} {
        set sTypeSearchPattern "$sObjectType"
    }

    # get absolute names from symbolic names
    if {[lindex [split $sObjectType _] 0] == "type"} {
        set sAbsObjectType [eServiceGetCurrentSchemaName type "$RegProgName" "$sObjectType"]
        if {$sAbsObjectType == ""} {
            return "Error: Property $sObjectType does not exists"
        }
    } else {
        set sAbsObjectType "$sObjectType"
    }

    if {[lindex [split $sObjectPolicy _] 0] == "policy"} {
        set sAbsObjectPolicy [eServiceGetCurrentSchemaName policy "$RegProgName" "$sObjectPolicy"]
        if {$sAbsObjectPolicy == ""} {
            return "Error: Property $sObjectPolicy does not exists"
        }
    } else {
        set sAbsObjectPolicy "$sObjectPolicy"
    }

    if {[lindex [split $sObjectVault _] 0] == "vault"} {
        set sAbsObjectVault [eServiceGetCurrentSchemaName vault "$RegProgName" "$sObjectVault"]
        if {$sAbsObjectVault == ""} {
            return "Error: Property $sObjectVault does not exists"
        }
    } else {
        set sAbsObjectVault "$sObjectVault"
    }

    set lTypesSearchPattern [split "$sTypeSearchPattern" ,]
    set lAbsTypesSearchPattern {}
    foreach sTypeInPattern $lTypesSearchPattern {
        if {[lindex [split $sTypeInPattern _] 0] == "type"} {
            set sAbsTypeInPattern [eServiceGetCurrentSchemaName type "$RegProgName" "$sTypeInPattern"]
            if {$sAbsTypeInPattern == ""} {
                return "Error: Property $sAbsTypeInPattern does not exists"
            }
            lappend lAbsTypesSearchPattern "$sAbsTypeInPattern"
        } else {
            lappend lAbsTypesSearchPattern "$sTypeInPattern"
        }
    }
    set sAbsTypesSearchPattern [join $lAbsTypesSearchPattern ,]

    set lVaultsSearchPattern [split "$sVaultSearchPattern" ,]
    set lAbsVaultsSearchPattern {}
    foreach sVaultInPattern $lVaultsSearchPattern {
        if {[lindex [split $sVaultInPattern _] 0] == "vault"} {
            set sAbsVaultInPattern [eServiceGetCurrentSchemaName vault "$RegProgName" "$sVaultInPattern"]
            if {$sAbsVaultInPattern == ""} {
                return "Error: Property $sAbsVaultInPattern does not exists"
            }
            lappend lAbsVaultsSearchPattern "$sAbsVaultInPattern"
        } else {
            lappend lAbsVaultsSearchPattern "$sVaultInPattern"
        }
    }
    set sAbsVaultsSearchPattern [join $lAbsVaultsSearchPattern ,]

    # matrix env variables required by program
    if {"$sAbsObjectVault" == ""} {
        set sAbsObjectVault      [mql get env VAULT]
    }
    set sUser       [mql get env USER]

    # check to see if ObjectPolicy argument is passed
    # if not passed then get policy from sObjectType argument
    # else use provided policy.
    if {"$sAbsObjectPolicy" == ""} {
        set sCmd {mql print type "$sAbsObjectType" select policy dump |}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            return "$outStr"
        }
        set sAbsObjectPolicy [lindex [split $outStr |] 0]
        if {$sAbsObjectPolicy == ""} {
            return "Error : Unable to find governing policy for type $sAbsObjectType"
        }
    }

    # check to see if ObjectRevision argument is passed
    # if not passed then get first sequence
    # else use provided revision.
    if {"$sObjectRev" == ""} {
        set sObjectRev [mxPolGetInitRev "$sAbsObjectPolicy"]
    }

    # Try to create an object for maximum retry time limit
    pushShadowAgent
    set sMaxTryTime [expr [clock seconds] + $sMaxTryTimeLimit]
    set sLastModified ""
    set sMaxLockTime [expr [clock seconds] + $sMaxLockTimeLimit]
    while {[clock seconds] < $sMaxTryTime } {

        # wait each iteration.
        after $sIterDelay

        # Try to lock Name Generator Object.
        set sAbsNumberGenerator [eServiceGetCurrentSchemaName type "$RegProgName" "type_eServiceNumberGenerator"]
        set sCmd {mql lock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            if {[string first "locked" "$outStr"] == -1} {
                popShadowAgent
                return "$outStr"
            }

            # if unsuccessful in locking then check when it was last modified.
            set sCmd {mql print bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev" select modified dump}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                popShadowAgent
                return "$outStr"
            }

            # if last modified is changed then someone is trying to use name generator program.
            # hence update last modified.
            if {"$outStr" != "$sLastModified"} {
                set sLastModified "$outStr"
                set sMaxLockTime [expr [clock seconds] + $sMaxLockTimeLimit]
            # if last modified is not changing from last sMaxLockTimeLimit seconds
            # then unlock object assuming something is wrong
            } elseif {[clock seconds] > $sMaxLockTime} {
                set sCmd {mql unlock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"}
                set mqlret [catch {eval $sCmd} outStr]
                if {$mqlret != 0} {
                    popShadowAgent
                    return "$outStr"
                }
            }
            continue
        }

        # Get Next Number
        set sAbsNextNumber [eServiceGetCurrentSchemaName attribute "$RegProgName" "attribute_eServiceNextNumber"]
        set sCmd {mql print bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev" select attribute\[$sAbsNextNumber\] dump}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            mql unlock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"
            popShadowAgent
            return "$outStr"
        }
        set sNextNumber $outStr

        # check if object already present with currenly selected name.
        # If yes then increament name by one and retry till successful.
        if {"$bExpand" == "TRUE"} {
            set sCmd {mql temp query bus "$sAbsTypesSearchPattern" "$sNextNumber" "$sObjectRev" vault "$sAbsVaultsSearchPattern" limit 1 expandtype}
        } else {
            set sCmd {mql temp query bus "$sAbsTypesSearchPattern" "$sNextNumber" "$sObjectRev" vault "$sAbsVaultsSearchPattern" limit 1 notexpandtype}
        }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            mql unlock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"
            popShadowAgent
            return "$outStr"
        }
        if {"$outStr" == ""} {
            if {"$bUniqueNameOnly" == "FALSE"} {
                if {"$sAbsObjectVault" == "ADMINISTRATION"} {
                    set sAbsObjectVault [eServiceGetCurrentSchemaName vault "$RegProgName" "$sDefaultVault"]
                }

                set sAbsOriginator [eServiceGetCurrentSchemaName attribute "$RegProgName" "attribute_Originator"]
            
                if {"$bUseSuperUser" == "TRUE"} {
                    #Add the bus object
                    set sCmd {mql add bus "$sAbsObjectType" "$sNextNumber" "$sObjectRev" \
                                      policy "$sAbsObjectPolicy" \
                                      vault "$sAbsObjectVault" \
                                      owner "$sUser" \
                                      "$sAbsOriginator" "$sUser" \
                                      }
                } else {
                    popShadowAgent
                    #Add the bus object
                    set sCmd {mql add bus "$sAbsObjectType" "$sNextNumber" "$sObjectRev" \
                                      policy "$sAbsObjectPolicy" \
                                      vault "$sAbsObjectVault" \
                                      "$sAbsOriginator" "$sUser" \
                                      }
                }
                set mqlret [catch {eval $sCmd} outStr]
                if {$mqlret != 0} {
                    if {[string first "unique" "$outStr"] == -1} {
                        mql unlock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"
                        if {"$bUseSuperUser" == "TRUE"} {
                            popShadowAgent
                        }
                        return "$outStr"
                    }
                } else {
                    set nPassed 1
                }
                if {"$bUseSuperUser" != "TRUE"} {
                    pushShadowAgent
                }
            } else {
                set nPassed 1
            }
        }

        # if name found then increament number and try next time.
        set sOldNumber $sNextNumber
        set sNextNumber [string trimleft "$sNextNumber" 0]
        incr sNextNumber
        set sNextNumber [format "%010d" "$sNextNumber"]
        set sCmd {mql modify bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev" \
                             "$sAbsNextNumber" "$sNextNumber" \
                             }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            mql unlock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"
            popShadowAgent
            return "$outStr"
        }

        # unlock the number generator object
        set sCmd {mql unlock bus "$sAbsNumberGenerator" "$sNumberGenName" "$sNumberGenRev"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            popShadowAgent
            return "$outStr"
        }

        if {$nPassed == 1} {
            break
        }
    }
    popShadowAgent

    if {$nPassed == 0} {
        return "Error : Unable to create an Object, Please try later as database is busy"
    }

    if {"$bUniqueNameOnly" == "FALSE"} {

        # Get Object ID
        set sCmd {mql print bus "$sAbsObjectType" "$sOldNumber" "$sObjectRev" select id dump}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            return "$outStr"
        } else {
            set lReturnList [list "$outStr" "$sAbsObjectType" "$sOldNumber" "$sObjectRev" "$sAbsObjectVault"]
        }
    } else {
        set lReturnList [list "$sOldNumber"]
    }
    
    return "$lReturnList"
}

