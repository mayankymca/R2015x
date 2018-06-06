#################################################################################
#
# $RCSfile: emxEBOMHistoryConversionUtility.tcl.rca $ $Revision: 1.29 $
#
# Description:
# DEPENDENCIES: This program has to lie in Schema & Common directories with the
#               same name and should be called from all builds
#################################################################################
#################################################################################
#                                                                               #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                   #
#   This program contains proprietary and trade secret information of           #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not       #
#   evidence any actual or intended publication of such program.                #
#                                                                               #
#################################################################################

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

proc parsePartListFile {sPartListFileName nLogFileId} {

    # Delete set emxEBOMHistoryConversionUtility if it already exists
    set sCmd {mql list set}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "ERROR:"
        puts stdout "$outStr"
        return -code 1
    }
    set lSets [split $outStr \n]
    if {[lsearch $lSets "emxEBOMHistoryConversionUtility"] >= 0} {
        set sCmd {mql delete set "emxEBOMHistoryConversionUtility"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts stdout "ERROR:"
            puts stdout "$outStr"
            return -code 1
        }
    }
    
    # Log message about processing file
    puts $nLogFileId "Parsing $sPartListFileName input file"
    puts $nLogFileId ""
    
    # Open file
    set nFileId [open "$sPartListFileName" r]
    
    # Get all the queries
    set sCmd {mql list query}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "ERROR:"
        puts stdout "$outStr"
        return -code 1
    }
    
    set lAllQueries [split $outStr \n]
    
    # Command to add set
    set sSetCmd {mql add set "emxEBOMHistoryConversionUtility"}
    set lQueries {}
    
    # Parse each line in the input file
    while {[gets $nFileId sLine] != -1} {
        set sLine [string trim $sLine]
        
        # continue if line is empty
        if {"$sLine" == ""} {
            continue
        }
        
        # Check if it is a Part list or query list
        set sTNR [split $sLine |]
        set bPartList "FALSE"
        
        # If it is a Part list then create a set with 
        # name emxEBOMHistoryConversionUtility and put all the parts
        # specified in the input file in it.
        # return set name.
        if {[llength $sTNR] == 3} {
            set bPartList "TRUE"
            set sType [lindex $sTNR 0]
            set sName [lindex $sTNR 1]
            set sRev [lindex $sTNR 2]
            set sCmd {mql print bus "$sType" "$sName" "$sRev" select exists dump |}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                puts stdout "ERROR:"
                puts stdout "$outStr"
                return -code 1
            }
            if {"$outStr" == "FALSE"} {
                puts $nLogFileId "WARNING:"
                puts $nLogFileId "'$sType' '$sName' '$sRev' does not exists"
                puts $nLogFileId ""
                continue
            }
            append sSetCmd " member bus \"$sType\" \"$sName\" \"$sRev\""
        # else return all the queries specified in the input file
        } else {
            if {[lsearch $lAllQueries "$sLine"] == -1} {
                puts $nLogFileId "WARNING:"
                puts $nLogFileId "Query $sLine does not exists"
                puts $nLogFileId ""
                continue
            }
            lappend lQueries "$sLine"
        }
    }
    if {"$bPartList" == "TRUE"} {
        set mqlret [catch {eval $sSetCmd} outStr]
        if {$mqlret != 0} {
            puts stdout "ERROR:"
            puts stdout "$outStr"
            return -code 1
        }
        set lQueries "SET"
    }
    
    close $nFileId
    
    return $lQueries
}

proc compareDates {sDate1 sDate2} {
    set sDate1ToSeconds [clock scan "$sDate1"]
    set sDate2ToSeconds [clock scan "$sDate2"]
    
    return [expr "$sDate1ToSeconds" - "$sDate2ToSeconds"]
}

proc addSubSecondsFromDate {sDate nSeconds sOperation} {
    set nDateToSeconds [clock scan "$sDate"]
    
    set nDateToSeconds [expr $nDateToSeconds $sOperation $nSeconds]
    
    return [clock format $nDateToSeconds -format "%m/%d/%Y %I:%M:%S %p"]
}

proc checkObjectState {sPolicy sCurrentState sStateToCompare} {

    uplevel {
        if {[info exists aPolicyStateMap] == 0} {
            array set aPolicyStateMap {}
        }
    }
    
    upvar aPolicyStateMap aLocalMap
    if {[array exists aLocalMap] == 0 || [array get aLocalMap $sPolicy] == ""} {
        
        # Get all the states of the policy
        set sCmd {mql print policy "$sPolicy" select state dump tcl}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            return ""
        }
        set lStates [lindex [lindex $outStr 0] 0]
        array set aLocalMap [list "$sPolicy" "$lStates"]
    } else {
        set lStates [lindex [array get aLocalMap $sPolicy] 1]
    }
    set nCurrentIndex [lsearch $lStates $sCurrentState]
    set nStateToCompareIndex [lsearch $lStates $sStateToCompare]
    if {"$nCurrentIndex" < 0 || "$nStateToCompareIndex" < 0} {
        return ""
    }
    
    return [expr $nCurrentIndex - $nStateToCompareIndex]
}

proc doEBOMHistoryConversion {sInputType nTransactionLimit nLogFileId bObsoletePartEndEffectivity sStartEffectivityDate} {

    set sRegProgName "eServiceSchemaVariableMapping.tcl"
    # Load utility files
    eval [utLoad $sRegProgName]
    
    # Get admin names from symbolic names.
    set sRelEBOM [eServiceGetCurrentSchemaName relationahip $sRegProgName relationship_EBOM]
    set sRelEBOMHistory [eServiceGetCurrentSchemaName relationahip $sRegProgName relationship_EBOMHistory]
    set sTypePart [eServiceGetCurrentSchemaName type $sRegProgName type_Part]
    set sAttStartEffectivityDate [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_StartEffectivityDate]
    set sAttEndEffectivityDate [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_EndEffectivityDate]
    set sTypePart [eServiceGetCurrentSchemaName type $sRegProgName type_Part]
    set sStateRelease [eServiceGetCurrentSchemaName state $sRegProgName policy_ECPart state_Release]
    set sStateObsolete [eServiceGetCurrentSchemaName state $sRegProgName policy_ECPart state_Obsolete]

    # Get all the common attributes on EBOM and EBOM History relationship
    # except Effectivity Date attributes.
    set sCmd {mql print relationship "$sRelEBOM" select attribute dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "ERROR:"
        puts stdout "$outStr"
        return -code 1
    }
    set lEBOMAttr [split $outStr |]
    
    set sCmd {mql print relationship "$sRelEBOMHistory" select attribute dump |}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "ERROR:"
        puts stdout "$outStr"
        return -code 1
    }
    set lEBOMHistoryAttr [split $outStr |]
    
    set lCommonAttr {}
    foreach sEBOMAttr $lEBOMAttr {
        if {[lsearch $lEBOMHistoryAttr "$sEBOMAttr"] >= 0 && \
            "$sEBOMAttr" != "$sAttStartEffectivityDate" &&
            "$sEBOMAttr" != "$sAttEndEffectivityDate"} {
            lappend lCommonAttr "$sEBOMAttr"
        }
    }
    
    # Get all the assembly and connected component parts detail 
    # on which conversion needs to be run
    if {[string toupper "$sInputType"] == "DATABASE"} {
        set sCmd {mql temp query bus "$sTypePart" * * \
                      select \
                      id \
                      current \
                      policy \
                      state\[$sStateRelease\].actual \
                      from\[$sRelEBOM\].id \
                      from\[$sRelEBOM\].businessobject.id \
                      from\[$sRelEBOM\].businessobject.state\[$sStateRelease\].actual \
                      from\[$sRelEBOM\].businessobject.state\[$sStateObsolete\].actual \
                      from\[$sRelEBOM\].businessobject.current \
                      from\[$sRelEBOM\].businessobject.policy \
                      from\[$sRelEBOMHistory\].businessobject.id \
                      }
    } else {
        set sCmd {mql print set "emxEBOMHistoryConversionUtility" \
                          select \
                          type \
                          name \
                          revision \
                          id \
                          current \
                          policy \
                          state\[$sStateRelease\].actual \
                          from\[$sRelEBOM\].id \
                          from\[$sRelEBOM\].businessobject.id \
                          from\[$sRelEBOM\].businessobject.state\[$sStateRelease\].actual \
                          from\[$sRelEBOM\].businessobject.state\[$sStateObsolete\].actual \
                          from\[$sRelEBOM\].businessobject.current \
                          from\[$sRelEBOM\].businessobject.policy \
                          from\[$sRelEBOMHistory\].businessobject.id \
                          }
    }
    
    foreach sCommonAttr $lCommonAttr {
        append sCmd " from\\\\\\[$sRelEBOM\\\\\\].attribute\\\\\\[$sCommonAttr\\\\\\].value"
    }

    foreach sCommonAttr $lCommonAttr {
        append sCmd " from\\\\\\[$sRelEBOMHistory\\\\\\].attribute\\\\\\[$sCommonAttr\\\\\\].value"
    }

    append sCmd " dump tcl"
    
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "ERROR:"
        puts stdout "$outStr"
        return -code 1
    }
    
    # for each assembly
    set nAssemblyCounter 0
    set nTranCounter 0
    set nTotalAssemblies [llength $outStr]
    if {$nTotalAssemblies < $nTransactionLimit} {
        set nTransactionLimit $nTotalAssemblies
    }
    foreach sPartDetail $outStr {
    
        # start transaction
        if {$nTransactionLimit > 0 && $nTranCounter == 0} {
            set sTranCmd {mql start transaction}
            set mqlret [catch {eval $sTranCmd} outStr]
            if {$mqlret != 0} {
                puts stdout "ERROR:"
                puts stdout "$outStr"
                return -code 1
            }
        }
        incr nAssemblyCounter
        incr nTranCounter
        
        # Get assebly details
        set sAsseblyId [lindex [lindex $sPartDetail 3] 0]
        set sAsseblyCurrent [lindex [lindex $sPartDetail 4] 0]
        set sAsseblyPolicy [lindex [lindex $sPartDetail 5] 0]
        set sAsseblyReleaseDate [lindex [lindex $sPartDetail 6] 0]
        
        # Get EBOM Ids
        set lEBOMId [lindex $sPartDetail 7]
        
        # continue if no component parts found
        if {[llength $lEBOMId] == 1 && [lindex $lEBOMId 0] == ""} {
            if {$nTransactionLimit > 0 && ($nTranCounter == $nTransactionLimit || $nAssemblyCounter == $nTotalAssemblies)} {
                set sTranCmd {mql commit transaction}
                set mqlret [catch {eval $sTranCmd} outStr]
                if {$mqlret != 0} {
                    puts stdout "ERROR:"
                    puts stdout "$outStr"
                    return -code 1
                }
                set nTranCounter 0
            }
            continue
        }
        
        # Get component details
        set lComponentIds [lindex $sPartDetail 8]
        set lComponentReleaseDates [lindex $sPartDetail 9]
        set lComponentObsoleteDates [lindex $sPartDetail 10]
        set lComponentCurrents [lindex $sPartDetail 11]
        set lComponentPolicies [lindex $sPartDetail 12]
        
        # Get EBOM History business object ids
        set lEBOMHistoryCompId [lindex $sPartDetail 13]
        
        # Get EBOM common attribute details
        for {set i 0} {$i < [llength $lCommonAttr]} {incr i} {
            set lCommonAttrValue [lindex $sPartDetail [expr 14 + $i]]
            foreach sEBOMId $lEBOMId \
                    sCommonAttrValue $lCommonAttrValue {
                array set "[lindex $lCommonAttr $i]" [list "$sEBOMId" "$sCommonAttrValue"]
            }
        }
        
        # Get EBOM History common attribute details if connection exists
        if {[llength $lEBOMHistoryCompId] != 1 || [lindex $lEBOMHistoryCompId 0] != ""} {
            for {set i 0} {$i < [llength $lCommonAttr]} {incr i} {
                set lCommonAttrValue [lindex $sPartDetail [expr 14 + [llength $lCommonAttr] + $i]]
                foreach sEBOMHistoryCompId $lEBOMHistoryCompId \
                        sCommonAttrValue $lCommonAttrValue {
                    array set "[lindex $lCommonAttr $i]" [list "$sEBOMHistoryCompId" "$sCommonAttrValue"]
                }
            }
        } else {
            set lEBOMHistoryCompId {}
        }
       
        # Check if assembly is released.
        if {[checkObjectState $sAsseblyPolicy $sAsseblyCurrent $sStateRelease] >= 0} {
            
            # For each component part
            foreach sComponentId $lComponentIds \
                    sComponentReleaseDate $lComponentReleaseDates \
                    sComponentCurrent $lComponentCurrents \
                    sComponentPolicy $lComponentPolicies \
                    sComponentObsoleteDate $lComponentObsoleteDates \
                    sEBOMId $lEBOMId {
            
                # check if component is released
                if {[checkObjectState $sComponentPolicy $sComponentCurrent $sStateRelease] >= 0} {
                
                    # If StartEffectivityDate input parameter is provided then
                    if {[string length "$sStartEffectivityDate"] != 0} {
                    
                        # Set Start Effectivity Attribute on EBOM connection 
                        # to StartEffectivityDate Input parameter
                        set sEBOMModCmd {mql modify connection "$sEBOMId" \
                                             "$sAttStartEffectivityDate" "$sStartEffectivityDate" \
                                             }
                        # If bObsoletePartEndEffectivity is set to TRUE and
                        # if component part is obsoleted then 
                        # set End Effectivity date 
                        # to StartEffectivityDate Input parameter + 1 day
                        set sStartEffectivityDatePlus1 [addSubSecondsFromDate "$sStartEffectivityDate" "86400" "+"]
                        if {"$bObsoletePartEndEffectivity" == "TRUE"} {
                            if {[checkObjectState $sComponentPolicy $sComponentCurrent $sStateObsolete] >= 0} {
                                append sEBOMModCmd " \"$sAttEndEffectivityDate\" \"$sStartEffectivityDatePlus1\""
                            }
                        }
                        set mqlret [catch {eval $sEBOMModCmd} outStr]
                        if {$mqlret != 0} {
                            puts stdout "ERROR:"
                            puts stdout "$outStr"
                            return -code 1
                        }
                        continue
                    }
                    
                    set sEBOMModCmd {mql modify connection "$sEBOMId"}
                    # If bObsoletePartEndEffectivity is set to TRUE and
                    # if component part is obsoleted then 
                    # set End Effectivity date to date at which
                    # part was obsoleted
                    if {"$bObsoletePartEndEffectivity" == "TRUE"} {
                        if {[checkObjectState $sComponentPolicy $sComponentCurrent $sStateObsolete] >= 0} {
                            append sEBOMModCmd " \"$sAttEndEffectivityDate\" \"$sComponentObsoleteDate\""
                        }
                    }
                    
                    # If component release date >= assembly release date then
                    # set Start Effectivity Date to component release date
                    if {[compareDates "$sComponentReleaseDate" "$sAsseblyReleaseDate"] >= 0} {
                        append sEBOMModCmd " \"$sAttStartEffectivityDate\" \"$sComponentReleaseDate\""
                        set mqlret [catch {eval $sEBOMModCmd} outStr]
                        if {$mqlret != 0} {
                            puts stdout "ERROR:"
                            puts stdout "$outStr"
                            return -code 1
                        }
                        
                        # Get previous revision details for component part
                        set sCmd {mql print bus "$sComponentId" \
                                      select \
                                      revisions.id \
                                      revisions.current \
                                      revisions.policy \
                                      revisions.state\[$sStateRelease\].actual \
                                      dump tcl \
                                      }
                        set mqlret [catch {eval $sCmd} outStr]
                        if {$mqlret != 0} {
                            puts stdout "ERROR:"
                            puts stdout "$outStr"
                            return -code 1
                        }
                        set lRevisionDetails [lindex $outStr 0]
                        set lRevisionIds [lindex $lRevisionDetails 0]
                        
                        set nPrevRevIndex 0
                        foreach sRevisionId $lRevisionIds {
                            if {"$sRevisionId" == "$sComponentId"} {
                                break
                            }
                            incr nPrevRevIndex
                        }
                        set nPrevRevIndex [expr $nPrevRevIndex - 1]
                        
                        set lRevisionIds [lrange [lindex $lRevisionDetails 0] 0 $nPrevRevIndex]
                        set lRevisionCurrents [lrange [lindex $lRevisionDetails 1] 0 $nPrevRevIndex]
                        set lRevisionPolicies [lrange [lindex $lRevisionDetails 2] 0 $nPrevRevIndex]
                        set lRevisionReleaseDates [lrange [lindex $lRevisionDetails 3] 0 $nPrevRevIndex]
                        
                        set nIndex 0
                        
                        # for each revision of component part
                        for {set nIndex [expr [llength $lRevisionIds] - 1]} {$nIndex >= 0} {set nIndex [expr $nIndex - 1]} {

                            set sRevisionId [lindex $lRevisionIds $nIndex]
                            set sRevisionCurrent [lindex $lRevisionCurrents $nIndex]
                            set sRevisionPolicy [lindex $lRevisionPolicies $nIndex]
                            set sRevisionReleaseDate [lindex $lRevisionReleaseDates $nIndex]
                        
                            # check if revision is released
                            if {[checkObjectState $sRevisionPolicy $sRevisionCurrent $sStateRelease] >= 0} {
                                
                                # Check if assembly and component are already connected
                                set sFirstCommonAttr [lindex $lCommonAttr 0]
                                if {[llength [array get $sFirstCommonAttr $sRevisionId]] != 0} {
                                    foreach sCommonAttr $lCommonAttr {
                                        set sEBOMAttrValue [lindex [array get $sCommonAttr $sEBOMId] 1]
                                        set sEBOMHistoryAttrValue [lindex [array get $sCommonAttr $sRevisionId] 1]
                                        if {"$sEBOMAttrValue" != "$sEBOMHistoryAttrValue"} {
                                            break
                                        }
                                    }
                                    # Warning
                                    set sTNRCmd {mql print bus $sRevisionId select type name revision dump tcl}
                                    set mqlret [catch {eval $sTNRCmd} outStr]
                                    if {$mqlret != 0} {
                                        puts stdout "ERROR:"
                                        puts stdout "$outStr"
                                        return -code 1
                                    }
                                    set lOutput [lindex $outStr 0]
                                    set sRevisionType [lindex [lindex $lOutput 0] 0]
                                    set sRevisionName [lindex [lindex $lOutput 1] 0]
                                    set sRevisionRev [lindex [lindex $lOutput 2] 0]
                                    
                                    set sTNRCmd {mql print bus $sAsseblyId select type name revision dump tcl}
                                    set mqlret [catch {eval $sTNRCmd} outStr]
                                    if {$mqlret != 0} {
                                        puts stdout "ERROR:"
                                        puts stdout "$outStr"
                                        return -code 1
                                    }
                                    set lOutput [lindex $outStr 0]
                                    set sAsseblyType [lindex [lindex $lOutput 0] 0]
                                    set sAsseblyName [lindex [lindex $lOutput 1] 0]
                                    set sAsseblyRev [lindex [lindex $lOutput 2] 0]
                                    puts $nLogFileId "WARNING:"
                                    puts $nLogFileId "$sAsseblyType $sAsseblyName $sAsseblyRev is already connected to $sRevisionType $sRevisionName $sRevisionRev with relationship $sRelEBOMHistory"
                                    puts $nLogFileId "Attributes on relationship are:"
                                    foreach sCommonAttr $lCommonAttr {
                                        set sEBOMAttrValue [lindex [array get $sCommonAttr $sEBOMId] 1]
                                        puts $nLogFileId "$sCommonAttr : $sEBOMAttrValue"
                                    }
                                    puts $nLogFileId ""
                                    continue
                                }
                                
                                # connect assembly to revision by EBOM History connection
                                set sCmd {mql connect bus $sAsseblyId to $sRevisionId \
                                              relationship "$sRelEBOMHistory" \
                                              }

                                # set all the common attributes to one stored above
                                foreach sCommonAttr $lCommonAttr {
                                    append sCmd " \"$sCommonAttr\" \"[lindex [array get $sCommonAttr $sEBOMId] 1]\""
                                }

                                # set end effectivity date to next revision release date + 1 sec
                                if {[expr $nIndex + 1] >= [llength $lRevisionIds]} {
                                    set sComponentReleaseDatePlus1 [addSubSecondsFromDate "$sComponentReleaseDate" "1" "-"]
                                    append sCmd " \"$sAttEndEffectivityDate\" \"$sComponentReleaseDatePlus1\""
                                } else {
                                    set sNextRevReleaseDate [lindex $lRevisionReleaseDates [expr $nIndex +1]]
                                    set sNextRevReleaseDatePlus1 [addSubSecondsFromDate "$sNextRevReleaseDate" "1" "-"]
                                    append sCmd " \"$sAttEndEffectivityDate\" \"$sNextRevReleaseDatePlus1\""
                                }

                                # if revision release date >= assembly release date
                                # set start effectivity date to revision release date
                                if {[compareDates "$sRevisionReleaseDate" "$sAsseblyReleaseDate"] >= 0} {
                                    append sCmd " \"$sAttStartEffectivityDate\" \"$sRevisionReleaseDate\""
                                    set mqlret [catch {eval $sCmd} outStr]
                                    if {$mqlret != 0} {
                                        puts stdout "ERROR:"
                                        puts stdout "$outStr"
                                        return -code 1
                                    }

                                # if revision release date < assembly release date
                                # set start effectivity date to assembly release date
                                # break and process next component
                                } else {
                                    append sCmd " \"$sAttStartEffectivityDate\" \"$sAsseblyReleaseDate\""
                                    set mqlret [catch {eval $sCmd} outStr]
                                    if {$mqlret != 0} {
                                        puts stdout "ERROR:"
                                        puts stdout "$outStr"
                                        return -code 1
                                    }
                                    break
                                }
                            } else {
                                # Warning
                                set sTNRCmd {mql print bus $sRevisionId select type name revision dump tcl}
                                set mqlret [catch {eval $sTNRCmd} outStr]
                                set lOutout [lindex $outStr 0]
                                set sRevisionType [lindex [lindex $lOutout 0] 0]
                                set sRevisionName [lindex [lindex $lOutout 1] 0]
                                set sRevisionRev [lindex [lindex $lOutout 2] 0]
                                if {$mqlret != 0} {
                                    puts stdout "ERROR:"
                                    puts stdout "$outStr"
                                    return -code 1
                                }
                                puts $nLogFileId "WARNING:"
                                puts $nLogFileId "$sRevisionType $sRevisionName $sRevisionRev is not in $sStateRelease state skipping it."
                                puts $nLogFileId ""
                                break
                            }
                        }
                        
                    # if component release date < assembly release date
                    # then set start effectivity date to assembly release date
                    # break and continue with next component part
                    } else {
                        append sEBOMModCmd " \"$sAttStartEffectivityDate\" \"$sAsseblyReleaseDate\""
                        set mqlret [catch {eval $sEBOMModCmd} outStr]
                        if {$mqlret != 0} {
                            puts stdout "ERROR:"
                            puts stdout "$outStr"
                            return -code 1
                        }
                        continue
                    }
                    
                } else {
                    # Warning
                    set sTNRCmd {mql print bus $sComponentId select type name revision dump tcl}
                    set mqlret [catch {eval $sTNRCmd} outStr]
                    set lOutput [lindex $outStr 0]
                    set sComponentType [lindex [lindex $lOutput 0] 0]
                    set sComponentName [lindex [lindex $lOutput 1] 0]
                    set sComponentRev [lindex [lindex $lOutput 2] 0]
                    if {$mqlret != 0} {
                        puts stdout "ERROR:"
                        puts stdout "$outStr"
                        return -code 1
                    }
                    puts $nLogFileId "WARNING:"
                    puts $nLogFileId "$sComponentType $sComponentName $sComponentRev is not in $sStateRelease state skipping it."
                    puts $nLogFileId ""
                }
            }
        } else {
            # Warning
            set sTNRCmd {mql print bus $sAsseblyId select type name revision dump tcl}
            set mqlret [catch {eval $sTNRCmd} outStr]
            set lOutput [lindex $outStr 0]
            set sAssemblyType [lindex [lindex $lOutput 0] 0]
            set sAssemblyName [lindex [lindex $lOutput 1] 0]
            set sAssemblyRev [lindex [lindex $lOutput 2] 0]
            if {$mqlret != 0} {
                puts stdout "ERROR:"
                puts stdout "$outStr"
                return -code 1
            }
            puts $nLogFileId "WARNING:"
            puts $nLogFileId "$sAssemblyType $sAssemblyName $sAssemblyRev is not in $sStateRelease state skipping it."
            puts $nLogFileId ""
        }

        if {$nTransactionLimit > 0 && ($nTranCounter == $nTransactionLimit || $nAssemblyCounter == $nTotalAssemblies)} {
            set sTranCmd {mql commit transaction}
            set mqlret [catch {eval $sTranCmd} outStr]
            if {$mqlret != 0} {
                puts stdout "ERROR:"
                puts stdout "$outStr"
                return -code 1
            }
            set nTranCounter 0
        }
    }
}

    # Parse input arguments.
    set sPartList [mql get env 1]
    set nTransactionLimit [mql get env 2]
    set sLogFile [mql get env 3]
    set bObsoletePartEndEffectivity [string toupper [mql get env 4]]
    set sStartEffectivityDate [mql get env 5]
    
    # trigger off
    set sCmd {mql trigger off}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "Error:"
        puts stdout "$outStr"
        return -code 1
    }
    
    # Input arguments error check
    if {"$sPartList" == "" || "$nTransactionLimit" == "" || "$sLogFile" == "" || "$bObsoletePartEndEffectivity" == ""} {
        puts "Error:"
        puts stdout "Example Usage: emxEBOMHistoryConversionUtility.tcl 'Database' 100 <PATH>/logfile TRUE"
        return -code 1
    }

    if {"$bObsoletePartEndEffectivity" != "TRUE" && "$bObsoletePartEndEffectivity" != "FALSE"} {
        puts "Error:"
        puts "Argument ObsoletePartEndEffectivity should be set to either TRUE or FALSE"
        return -code 1
    }

    # Open log file for writing log messages.
    set nLogFileId [open "$sLogFile" w 0666]
    
    # Log initial message about program name and input parameters.
    puts $nLogFileId "Executing conversion program [mql get env 0] ..."
    puts $nLogFileId ""
    if {[string toupper "$sPartList"] == "DATABASE"} {
        puts $nLogFileId "Part list is referenced by querying all the released parts in the database"
    } else {
        puts $nLogFileId "Part list is referenced from input file $sPartList"
    }
    
    if {"$nTransactionLimit" == "-1"} {
        puts $nLogFileId "Transaction boundaries are around whole conversion routine"
    } elseif {"$nTransactionLimit" == "0"} {
        puts $nLogFileId "There are no transaction boundaries"
    } else {
        puts $nLogFileId "Transaction committed after each $nTransactionLimit parts"
    }
    
    puts $nLogFileId "Log File : $sLogFile"
    puts $nLogFileId "Set Obsolete Part End Effectivity Date: $bObsoletePartEndEffectivity"
    
    puts $nLogFileId ""
    
    # start transaction
    if {$nTransactionLimit == -1} {
        set sCmd {mql start transaction}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts stdout "ERROR:"
            puts stdout "$outStr"
            return -code 1
        }
    }
    
    if {[string toupper "$sPartList"] == "DATABASE"} {
        set sCmd {doEBOMHistoryConversion "DATABASE" "$nTransactionLimit" "$nLogFileId" "$bObsoletePartEndEffectivity" "$sStartEffectivityDate"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            set sCheckTran [string  trim [mql print transaction]]
            if {"$sCheckTran" != "transaction inactive"} {
                mql abort transaction
            }
            return -code 1
        }
    } else {
        set sCmd {parsePartListFile $sPartList $nLogFileId}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            set sCheckTran [string  trim [mql print transaction]]
            if {"$sCheckTran" != "transaction inactive"} {
                mql abort transaction
            }
            return -code 1
        }
        
        if {"$outStr" == "SET"} {
            set sCmd {doEBOMHistoryConversion "SET" "$nTransactionLimit" "$nLogFileId" "$bObsoletePartEndEffectivity" "$sStartEffectivityDate"}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                set sCheckTran [string  trim [mql print transaction]]
                if {"$sCheckTran" != "transaction inactive"} {
                    mql abort transaction
                }

                return -code 1
            }
        } else {
            foreach sQuery $outStr {
                puts $nLogFileId "Executing query $sQuery..."
                puts $nLogFileId ""
                set sCmd {mql evaluate query "$sQuery" into set "emxEBOMHistoryConversionUtility"}
                set mqlret [catch {eval $sCmd} outStr]
                if {$mqlret != 0} {
                    puts stdout "ERROR:"
                    puts stdout "$outStr"
                    set sCheckTran [string  trim [mql print transaction]]
                    if {"$sCheckTran" != "transaction inactive"} {
                        mql abort transaction
                    }

                    return -code 1
                }

                set sCmd {doEBOMHistoryConversion "SET" "$nTransactionLimit" "$nLogFileId" "$bObsoletePartEndEffectivity" "$sStartEffectivityDate"}
                set mqlret [catch {eval $sCmd} outStr]
                if {$mqlret != 0} {
                    set sCheckTran [string  trim [mql print transaction]]
                    if {"$sCheckTran" != "transaction inactive"} {
                        mql abort transaction
                    }

                    puts stdout "ERROR:"
                    puts stdout "$outStr"
                    return -code 1
                }
            }
        }
    }
    
    if {$nTransactionLimit == -1} {
        set sCmd {mql commit transaction}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts stdout "ERROR:"
            puts stdout "$outStr"
            return -code 1
        }
    }
    close $nLogFileId
}

