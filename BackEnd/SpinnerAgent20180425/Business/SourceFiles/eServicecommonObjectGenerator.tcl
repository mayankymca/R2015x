###############################################################################
# $RCSfile: eServicecommonObjectGenerator.tcl.rca $ $Revision: 1.66 $
#
# @progdoc      eServicecommonObjectGenerator.tcl
#
# @Brief:       The number generator procedure
#
# @Description: This program is used to automatically generate number sequences
#               for different admin objects. The procedure also creates the
#               objects for the user. Before creating the object it checks for
#               the uniqueness of the object being created.
#
#
# @Parameters:  ObjectGeneratorName      - Name of AutoName Business Object where
#                                          creation parameters are to be found
#
#               ObjectGeneratorRevision  - Revision of AutoName Business Object
#               CreateAdditional         - A parameter specifying whether to
#                                          create additional objects. This is a
#                                          flag having values Null & Additional.
#                                          A Null means no additional objects
#                                          to be created.
#                                          Default is Null.
#               ObjectVault              - Vault in which to create object
#                                          Default is context vault.
#               CustomRevisionLevel      - A parameter specifying custom revision
#                                          level if user specified.
#                                          Default is null value i.e. no custom revision.
#
# @Returns:     A list of lists. Each item in the list holds the following
#               The id, type, name and revision of the object created.
#               If there is an error it returns a list with the first item as null
#               and the sencond item the error message.
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
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

proc eServiceNumberGenerator { ObjectGeneratorName ObjectGeneratorRevision ObjectPolicy {CreateAdditional Null} {ObjectVault ""} {CustomRevisionLevel ""} {ObjectType ""} {UniqueNameOnly "No"} {UseSuperUser "Yes"}} {

    eval  [utLoad eServiceSchemaVariableMapping.tcl]
    eval  [utLoad eServicecommonShadowAgent.tcl]
    eval  [utLoad eServicecommonPSUtilities.tcl]
    eval  [utLoad eServicecommonDEBUG.tcl]

    # trim the input values of spaces on either side, if any
    set ObjectGeneratorName      [string trim $ObjectGeneratorName]
    set ObjectGeneratorRevision  [string trim $ObjectGeneratorRevision]
    set ObjectPolicy             [string trim $ObjectPolicy]
    set CreateAdditional         [string trim $CreateAdditional]
    set ObjectVault              [string trim $ObjectVault]
    set CustomRevisionLevel      [string trim $CustomRevisionLevel]
    set ObjectType               [string trim $ObjectType]

    # set program related variables
    set progname      "eServicecommonObjectGenerator.tcl"
    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set mqlret        0
    set outstr        ""
    set sNextNumber   ""

    # program related List varibales
    set lConSolidateSet       {}
    set lErrorSet             {}
    set lAdditionalObjectList {}

    # matrix env variables required by program
    set sRealUser   [mql get env USER]

    # get the absolute names from symbolic names
    set sTypeeServiceObjectGenerator      [eServiceGetCurrentSchemaName  type $RegProgName          type_eServiceObjectGenerator ]
    set sTypeeServiceNumberGenerator      [eServiceGetCurrentSchemaName  type $RegProgName          type_eServiceNumberGenerator ]
    set sAttreServiceNamePrefix           [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceNamePrefix ]
    set sAttreServiceNameSuffix           [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceNameSuffix ]
    set sAttreServiceRetryDelay           [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceRetryDelay ]
    set sAttreServiceRetryCount           [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceRetryCount ]
    set sAttreServiceSafetyVault          [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceSafetyVault ]
    set sAttreServiceProcessingTimeLimit  [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceProcessingTimeLimit ]
    set sAttreServiceSafetyPolicy         [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceSafetyPolicy ]
    set sAttreServiceNextNumber           [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceNextNumber ]
    set sAttreServiceConnectRelation      [eServiceGetCurrentSchemaName  attribute $RegProgName     attribute_eServiceConnectRelation ]
    set sReleServiceNumberGenerator       [eServiceGetCurrentSchemaName  relationship $RegProgName  relationship_eServiceNumberGenerator ]
    set sReleServiceAdditionalObject      [eServiceGetCurrentSchemaName  relationship $RegProgName  relationship_eServiceAdditionalObject ]
    set sPolicyeServiceObjectGenerator    [eServiceGetCurrentSchemaName  policy $RegProgName        policy_eServiceObjectGenerator ]

    # check if the user has passed in the name of the Object Generator Object,
    # if not raise an error
    if {[string compare $ObjectGeneratorName ""] == 0 } {
         set lErrorSet [list "" "The passed in parameters are not valid"]
         set mqlret 1
    } else {
         if {"$ObjectType" == ""} {
             set sPrimaryObjectType [eServiceGetCurrentSchemaName  type $RegProgName $ObjectGeneratorName]
         } else {
             set sPrimaryObjectType [eServiceGetCurrentSchemaName  type $RegProgName $ObjectType]
         }
    }

    # Get the vault through context object.
    if {"$ObjectVault" == ""} {
        set sCmd {mql print context}
        set mqlret [catch {eval $sCmd} outstr]
        if {$mqlret == 0} {
            set lOutput [split "$outstr"]
            set nIndex [lsearch $lOutput "person"]
            set sVault [join [lrange $lOutput 2 [expr $nIndex - 1]]]
        }
    } else {
        if {[string first "vault_" "$ObjectVault"] == 0} {
            set sVault [eServiceGetCurrentSchemaName vault $RegProgName $ObjectVault]
        } else {
            set sVault "$ObjectVault"
        }
    }

    # check if the revision is passed in, if not set revision as the first
    # revision in the sequence
    if {$mqlret == 0} {
         if {[string compare $ObjectGeneratorRevision ""] == 0} {
              set ObjectGeneratorRevision     [mxPolGetInitRev $sPolicyeServiceObjectGenerator]
         }
    }

    # Check if the object generator object exists
    if {$mqlret == 0} {
         set sCmd {mql print bus "$sTypeeServiceObjectGenerator" "$ObjectGeneratorName" "$ObjectGeneratorRevision" select exists dump }
         set mqlret [catch {eval $sCmd} outstr]
    }

    # If not then return with an error message
    if {$mqlret == 0} {
         if {$outstr == "FALSE"} {
              set lErrorSet [list "" "The Object Generator object $sTypeeServiceObjectGenerator $ObjectGeneratorName $ObjectGeneratorRevision doesn't exist"]
              set mqlret 1
         }
    }

    # If the Object Generator object exists, then get the attribute values
    if {$mqlret == 0} {
         set sCmd {mql print bus "$sTypeeServiceObjectGenerator" "$ObjectGeneratorName" "$ObjectGeneratorRevision" \
                             select \
                                    attribute\[$sAttreServiceNamePrefix\].value \
                                    attribute\[$sAttreServiceNameSuffix\].value \
                                    attribute\[$sAttreServiceRetryDelay\].value \
                                    attribute\[$sAttreServiceRetryCount\].value \
                                    attribute\[$sAttreServiceSafetyVault\].value \
                                    attribute\[$sAttreServiceProcessingTimeLimit\].value \
                                    attribute\[$sAttreServiceSafetyPolicy\].value \
                             dump |}
         set mqlret [catch {eval $sCmd} outstr]
    }

    if {$mqlret == 0} {

         set lResult $outstr

         set sNamePrefix        [string trim [lindex [split $lResult |] 0] ]
         set sNameSuffix        [string trim [lindex [split $lResult |] 1] ]
         set sRetryDelay        [string trim [lindex [split $lResult |] 2] ]
         set sRetryCount        [string trim [lindex [split $lResult |] 3] ]
         set sSafetyVault       [string trim [lindex [split $lResult |] 4] ]
         set sProcessTimeLimit  [string trim [lindex [split $lResult |] 5] ]
         set sSafetyPolicy      [string trim [lindex [split $lResult |] 6] ]
    }

    # Check if the policy by which the primary object is governed by,
    # is passed in, if its not passed in, set the policy as the default one.
    if {$mqlret == 0} {
         if {[string compare $ObjectPolicy ""] == 0} {
              set ObjectPolicy  [eServiceGetCurrentSchemaName  policy $RegProgName  $sSafetyPolicy ]
         } else {
              set ObjectPolicy  [eServiceGetCurrentSchemaName  policy $RegProgName  $ObjectPolicy ]
         }
    }

    # check if the vault associated with the USER env variable is ADMINISTRATION,
    # if yes, then get the absolute value of the safety policy
    if {($mqlret == 0) && ([string compare "$sVault" "ADMINISTRATION"] == 0)} {
         set sSafetyVault [eServiceGetCurrentSchemaName vault $RegProgName $sSafetyVault]
    }


    # Check that the nummer generator object is connected to the Object generator
    if { $mqlret == 0 } {
        set sCmd {mql expand bus "$sTypeeServiceObjectGenerator" "$ObjectGeneratorName" "$ObjectGeneratorRevision" \
                                     from relationship "$sReleServiceNumberGenerator" \
                                     select bus \
                                            id \
                                     dump |}                                   
        set mqlret [catch {eval $sCmd} outstr]
    }
    
    if {$mqlret == 0} {

        set sNumberGeneratorType          [string trim [lindex [split $outstr |] 3] ]
        set sNumberGeneratorName          [string trim [lindex [split $outstr |] 4] ]   
        set sNumberGeneratorRev           [string trim [lindex [split $outstr |] 5] ]
        set sNumberGeneratorId            [string trim [lindex [split $outstr |] 6] ]
        
        set sNumberGeneratorObject "$sNumberGeneratorType, $sNumberGeneratorName, $sNumberGeneratorRev"

        # Check if the Object Generator object is connected to a Number generator object
        # by the relation "eService Number Generator", if not return with an error message
        if {$sNumberGeneratorId == ""} {
              set lErrorSet [list "" "The Object Generator Object is not configured properly, Please contact the Administrator"]
              set mqlret 1
        }
    }
    
    
    # Try to create an object for maximum retry time limit
    if {$mqlret == 0} {

        # set the timer which determines how long this program will try to
        # generate a autoname
        set sCurrentTime [clock seconds]
        set sMaxProcessTime [expr {$sCurrentTime + $sProcessTimeLimit}]

        # start of autoname generation loop
        while {1} {

            # push to Shadow agent to have access to the number generator object
            pushShadowAgent
       
            set mqlret [catch {mql start thread} outstr]
            if {$mqlret != 0} {
                set lErrorSet [list "" "mql start thread failed: $outstr"]
                return $lErrorSet
            }

            set mqlret [catch {mql start transaction} outstr]
            if {$mqlret != 0} {
                set lErrorSet [list "" "mql start transaction failed: $outstr"]
                catch {mql kill thread} outstr
                return $lErrorSet
            }

            set i $sRetryCount

            # start retry loop, try to lock until number of retry limit reached              
            while {$i > 0} {

                # Try to lock the Name Generator Object. When sucessfully locked we
                # prevent other from modify the number generator at the same time
                set sCmd {mql lock bus "$sNumberGeneratorId"}
                set mqlret [catch {eval $sCmd} outstr]
                
                # Lock sucessfull
                if {$mqlret == 0} {
                    break
                
                # Lock failed
                } else {
                
                     # If not locked and we was not able to lock, something 
                     # else is wrong, return from this proc with the error
                     if {[string first "locked" "$outstr"] == -1} {
                         popShadowAgent
                         return [list "" "The Number Generator ($sNumberGeneratorObject) could not generate the name, contact the Administrator. $outstr"]
                     }

                     # wait for sRetryDelay ms
                     after $sRetryDelay

                     # Decrease the counter
                     incr i -1
                }
            }
            # End of while loop for the retry


            # If errors, the object is not accessible after sRetryCount, warn
            # the user and stop any further processing
            if {$mqlret != 0} {
                popShadowAgent
                set lErrorSet [list "" "The Number Generator Object ($sNumberGeneratorObject) is busy, Please try later, If the problem persists contact the Administrator\n$outstr"]
                break
            } 

            # no errors were encountered and number generator is now locked.
            # It is now safe to get the next number attribute
            if {$mqlret == 0 } {
                # get the value of the next number attribute
                set sCmd {mql print bus "$sNumberGeneratorId" \
                                     select \
                                            attribute\[$sAttreServiceNextNumber\].value \
                                     dump |}
                set mqlret [catch {eval $sCmd} outstr]
                if {$mqlret != 0} {
                    set sCmd {mql unlock bus "$sNumberGeneratorId"}
                    set mqlret1 [catch {eval $sCmd} outstr1]
                    break
                }
            }

            if {$mqlret == 0} {
                 set lResult $outstr

                 set sNextNumber    [string trim [lindex [split $lResult |] 0] ]
                 set sModNextNumber [utStrSequence $sNextNumber 1]

                 # modify the new number attribute on number generator object
                 # with the new value
                 set sCmd {mql mod bus "$sNumberGeneratorId" "$sAttreServiceNextNumber" "$sModNextNumber"}
                 set mqlret [catch {eval $sCmd} outstr]
                 if {$mqlret != 0} {
                     set sCmd {mql unlock bus "$sNumberGeneratorId"}
                     set mqlret1 [catch {eval $sCmd} outstr1]
                     break
                 }
            }

            # end of processing of the number generator, unlock the object
            if {$mqlret == 0} {
                 set sCmd {mql unlock bus "$sNumberGeneratorId"}
                 set mqlret [catch {eval $sCmd} outstr]
            }

            set mqlret [catch {mql commit transaction} outstr]
            if {$mqlret != 0} {
                set lErrorSet [list "" "mql commit transaction failed: $outstr"]
                catch {mql kill thread} outstr
                return $lErrorSet
            }

            set mqlret [catch {mql kill thread} outstr]
            if {$mqlret != 0} {
                set lErrorSet [list "" "mql kill thread failed: $outstr"]
                return $lErrorSet
            }

            popShadowAgent

            # Compose the new name and verify its uniqueness
            if {$mqlret == 0} {

                # Generate the new name for the primary object
                set sNewName "$sNamePrefix$sNextNumber$sNameSuffix"

                # Get the initial revision for the primary object
                if { $CustomRevisionLevel == "" } {                    
                    set sRev     [mxPolGetInitRev $ObjectPolicy]
                } else {
                    # Use the custom revision level since user specified it
                    set sRev "$CustomRevisionLevel"
                }

                if {$mqlret == 0} {

                    # Check to see if the new object name generated exists
                    set sCmd {mql temp query bus $sPrimaryObjectType $sNewName * select exists dump |}
                    set mqlret [catch {eval $sCmd} outstr]

                    if {$mqlret == 0} {
                        #added for bug 329108
                        set tempstr [string trim [lindex [split $outstr \n] 0]]
                        set outstr [string trim [lindex [split $tempstr |] 3]]

                        # Object exists, 
                        # if the processing time limit is exceeded then raise
                        # an error, otherwise try the next number in the sequence
                        # by not break out from the autoname generation loop
                        if {$outstr == "TRUE"} {
                            set sCurrent [clock seconds]
                            if {$sCurrent >= $sMaxProcessTime} {
                                set lErrorSet [list "" "Object $sNewName already exist, the max process time of $sProcessTimeLimit seconds have been reached, Please try later, If the problem persists contact the Administrator"]
                                return $lErrorSet
                            }
                        # object does not exist, 
                        # leave the autoname generation loop                            
                        } else {
                            break
                        }
                    }
                }
            }
        }
        # End of autoname generation loop

        # Enter this block if no errors encountered till this point
        # adding autoname object to database and changing the owner and construction
        # of return string is done here
        if {$mqlret == 0} {
            # if only name required then return name
            if {"$UniqueNameOnly" == "Yes"} {
                lappend lConSolidateSet [ list "$sNewName"]
            } else {

                # push to Shadow agent to have access to create and modify the new object
                if {"$UseSuperUser" != "No"} {
                    pushShadowAgent
                }

                # Add object
                if {$sVault == "ADMINISTRATION"} {
                    set sCmd {mql add bus $sPrimaryObjectType $sNewName $sRev vault "$sSafetyVault" policy "$ObjectPolicy" select id dump}
                    set sReturnVault $sSafetyVault
                } else {                                              
                    set sCmd {mql add bus $sPrimaryObjectType $sNewName $sRev vault "$sVault" policy "$ObjectPolicy" select id dump}
                    set sReturnVault $sVault
                }
                set mqlret [catch {eval $sCmd} outstr]

                 if {$mqlret == 0} {
                    set sNewObjectID $outstr
                 }
                # change owner to real user
                if {$mqlret == 0 && "$UseSuperUser" != "No"} {
                    set sCmd {mql mod bus "$sNewObjectID" owner "$sRealUser" }
                    set mqlret [catch {eval $sCmd} outstr]
                }

                #if {$mqlret == 0} {
                 #   set sCmd {mql print bus $sPrimaryObjectType $sNewName $sRev select id dump}
                 #   set mqlret [catch {eval $sCmd} outstr]
              #  }

                if {$mqlret == 0} {                   
                    lappend lConSolidateSet [ list $sNewObjectID $sPrimaryObjectType $sNewName $sRev $sReturnVault]
                }

                if {($mqlret == 0) && ($CreateAdditional == "Additional")} {

                    set sCmd {mql expand bus "$sTypeeServiceObjectGenerator" "$ObjectGeneratorName" "$ObjectGeneratorRevision" \
                                           from relationship $sReleServiceAdditionalObject  \
                                           select bus \
                                                  id \
                                                  attribute\[$sAttreServiceNamePrefix\].value \
                                                  attribute\[$sAttreServiceNameSuffix\].value \
                                           select relationship \
                                                  attribute\[$sAttreServiceConnectRelation\].value \
                                           dump |}
                    set mqlret [catch {eval $sCmd} outstr]

                    if {$mqlret == 0} {

                        set outstr [split $outstr \n]

                        foreach i $outstr {

                            set sAdditonalObject          [string trim [lindex [split $i |] 4 ] ]
                            set sAdditonalPrefix          [string trim [lindex [split $i |] 7 ] ]
                            set sAdditonalSuffix          [string trim [lindex [split $i |] 8 ] ]
                            set sAdditonalConnectRel      [string trim [lindex [split $i |] 9 ] ]

                            if {[string first "type_" "$sAdditonalObject"] == 0} {
                                set sAdditonalObject [eServiceGetCurrentSchemaName  type "$RegProgName" "$sAdditonalObject"]
                            }
                            if {[string first "relationship_" "$sAdditonalConnectRel"] == 0} {
                                set sAdditonalConnectRel [eServiceGetCurrentSchemaName  type "$RegProgName" "$sAdditonalConnectRel"]
                            }
                            set sAddPolicy                [mxTypeGetDfltPolicy $sAdditonalObject]
                            set sAddRev                   [mxPolGetInitRev $sAddPolicy]

                            set sAddNewName "$sAdditonalPrefix$sNextNumber$sAdditonalSuffix"

                            set sCmd {mql temp query bus "$sAdditonalObject" "$sAddNewName" "sAddRev" select exists id dump | }
                            set mqlret [catch {eval $sCmd} outstr]

                            if {$mqlret == 0} {

                                set sExistence [string trim [lindex [split $outstr |] 3]]
                                set sID        [string trim [lindex [split $outstr |] 4]]

                                if {$sExistence != "TRUE"} {
                                    if {$sVault == "ADMINISTRATION"} {
                                        set sCmd {mql add bus $sAdditonalObject $sAddNewName $sAddRev vault "$sSafetyVault" policy "$sAddPolicy" select id dump | }
                                        set sReturnVault $sSafetyVault
                                    } else {
                                        set sCmd {mql add bus $sAdditonalObject $sAddNewName $sAddRev policy "$sAddPolicy" select id dump | }
                                        set sReturnVault $sSafetyVault
                                    }
                                    set mqlret [catch {eval $sCmd} outstr]

                                   # if {$mqlret == 0} {
                                   #     set sCmd {mql print bus $sAdditonalObject $sAddNewName $sAddRev select id dump}
                                   #     set mqlret [catch {eval $sCmd} outstr]
                                   # }

                                    if {$mqlret == 0} {
                                        set sNewObjectID $outstr
                                        lappend lConSolidateSet [ list $sNewObjectID $sAdditonalObject $sAddNewName $sAddRev $sReturnVault]

                                    }
                                }

                                if {$mqlret == 0 && "$UseSuperUser" != "No"} {
                                    set sCmd {mql mod bus "$sNewObjectID" owner "$sRealUser" }
                                    set mqlret [catch {eval $sCmd} outstr]
                                }

                                if {$mqlret == 0} {
                                    set sCmd {mql connect bus "$sPrimaryObjectType" "$sNewName" "$sRev" \
                                                                        rel "$sAdditonalConnectRel" to "$sNewObjectID"}
                                    set mqlret [catch {eval $sCmd} outstr]
                                }
                            }
                        }
                    }
                }
                if {"$UseSuperUser" != "No"} {
                    popShadowAgent
                }
            }
        } 
        # end of adding autoname object to database block
    }
    # end of create an object for maximum retry time limit block

    if {$mqlret == 0} {
        return $lConSolidateSet
    } else {
        if {[lindex $lErrorSet 1] == ""} {
            set lErrorSet [list "" "$outstr"]
        }
        return $lErrorSet
    }
}

