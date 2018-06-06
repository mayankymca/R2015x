###############################################################################
#
# $RCSfile: emxCommonAddSampleUsers.tcl.rca $ $Revision: 1.8 $
#
# Description:  This file creates Sample users for Roles.
#
# @Usage:       exec program emxCommonAddSampleUsers.tcl NoOfUsers
#               exec program emxCommonRemoveLanguageAliases.tcl
#
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

    # Verbose
    mql verbose on;
    mql trigger off

    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    eval [utLoad $RegProgName]

    # Setting the no of users to create from input argument.
    set usersPerRole [mql get env 1]
    if {$usersPerRole == ""} {
      set usersPerRole 3
    }

    # The variable iconmailEnableDisable is set by default to "disable" 
    # which means all the users created will have the icon mail disabled.
    # If the icon mail needs to be enabled, the variable has to be set to "enable".

    set iconmailEnableDisable "disable"
    
    set iconmailBO "FALSE"
    
    if { $iconmailEnableDisable == "enable" } {
    	set iconmailBO "TRUE"
    }

    set startNumber 1

    set sTypePerson [eServiceGetCurrentSchemaName type $RegProgName  type_Person]
    set sPolicyPerson [eServiceGetCurrentSchemaName policy $RegProgName  policy_Person]
    set sStateActive [eServiceGetCurrentSchemaName state $RegProgName  policy_Person state_Active]

    set relEmp [eServiceGetCurrentSchemaName relationship $RegProgName relationship_Employee]
    set relMem [eServiceGetCurrentSchemaName relationship $RegProgName relationship_Member]
    set sAttProjectRole [eServiceGetCurrentSchemaName attribute $RegProgName attribute_ProjectRole]
    set sTypeCompany [eServiceGetCurrentSchemaName type $RegProgName type_Company]
    set sRoleEmployee [eServiceGetCurrentSchemaName role $RegProgName role_Employee]
    set sHostCompany [eServiceGetCurrentSchemaName role $RegProgName role_CompanyName]
    set sVaultSample [eServiceGetCurrentSchemaName group $RegProgName vault_eServiceSample]

    # Getting Attribute Names for Person Object.
    set sAttrFirstName [eServiceGetCurrentSchemaName attribute $RegProgName attribute_FirstName]
    set sAttrLastName [eServiceGetCurrentSchemaName attribute $RegProgName attribute_LastName]
    set sAttrJTViewer [eServiceGetCurrentSchemaName attribute $RegProgName attribute_JTViewerType]
    set sAttrIconMail [eServiceGetCurrentSchemaName attribute $RegProgName attribute_IconMail]
    set sAttrLoginType [eServiceGetCurrentSchemaName attribute $RegProgName attribute_LoginType]
    set sAttrHostMeetings [eServiceGetCurrentSchemaName attribute $RegProgName attribute_HostMeetings]
    set sAttreMail [eServiceGetCurrentSchemaName attribute $RegProgName attribute_EmailAddress]
    set sAttrHomePhone [eServiceGetCurrentSchemaName attribute $RegProgName attribute_HomePhoneNumber]
    set sAttrWorkPhone [eServiceGetCurrentSchemaName attribute $RegProgName attribute_WorkPhoneNumber]

    # Check for Company Type in business
    set sCmd {mql print bus "$sTypeCompany" "$sHostCompany" - select exists dump |}
    set mqlret [catch {eval $sCmd} hostCompanyExists]
    if {"$mqlret" != 0} {
        puts "$hostCompanyExists"
        return -code 1
    }

    # Getting Roles list.
    set Roles [ split [ mql list role ] \n ]

    # Getting Child List for Employee Role.
    set childs [ split [mql print role "$sRoleEmployee" select child  dump | ] | ]

    # Creating Users for each Role.
    foreach role $Roles {
        puts "Creating User for Role $role";

        # Eliminating White spaces in role
        regsub -all { } $role {} roleName

        for {set suffixName $startNumber} {$suffixName <= $usersPerRole} {incr suffixName} {

            #Creating Person Admin Object
            if {[mql list person "Test$suffixName $roleName"] == ""} {
                set sCmd { mql add person "Test$suffixName $roleName" \
                               fullname "$roleName, Test$suffixName" \
                               vault "$sVaultSample" \
                               $iconmailEnableDisable iconmail \
                               disable email \
                               email "qenotify@matrixone.com" \
                               access all \
                               admin all \
                               type full \
                               assign role "$role" \
                         }
                set mqlret [ catch {eval $sCmd} outStr ]
                if {"$mqlret" != 0} {
                    puts "$outStr"
                    return -code 1
                }
            }

            # Check for the existence of Type Person
            if {"$sTypePerson" != "" } {

                if {[mql print bus "$sTypePerson" "Test$suffixName $roleName" "-" select exists dump |] == "FALSE"} {
                    #Creating Person Business Object
                    set sCmd {mql add bus "$sTypePerson" "Test$suffixName $roleName" "-" \
                                  description "Person Object for user Test$suffixName $roleName" \
                                  vault "$sVaultSample" \
                                  policy "$sPolicyPerson" \
                                  "$sAttrFirstName" "Test$suffixName" \
                                  "$sAttrLastName" "$roleName" \
                                  "$sAttrJTViewer" "None" \
                                  "$sAttrIconMail" "$iconmailBO" \
                                  "$sAttrLoginType" "Standard" \
                                  "$sAttrHostMeetings" "Yes" \
                                  "$sAttreMail" "qenotify@matrixone.com" \
                                  "$sAttrHomePhone" "203 333-8888" \
                                  "$sAttrWorkPhone" "203 999-0000" \
                             }
                    set mqlret [ catch {eval $sCmd} outStr ]
                    if {"$mqlret" != 0} {
                        puts "$outStr"
                        return -code 1
                    }
                }
                    
                # promote person admin object
                if {[mql print bus "$sTypePerson" "Test$suffixName $roleName" "-" select current dump |] != "$sStateActive"} {
                    set sCmd {mql promote bus "$sTypePerson" "Test$suffixName $roleName" "-"
                             }
                    set mqlret [ catch {eval $sCmd} outStr ]
                    if {"$mqlret" != 0} {
                        puts "$outStr"
                        return -code 1
                    }
                }

                # Check if role is derived from Employee Role
                if { ([lsearch $childs $role ] >= 0) } {

                    # Check if Company and Host Company exists
                    if {("$hostCompanyExists" == "TRUE")} {

                         # Check if Relationship Member exists
                         if {"$relMem" != "" && [mql print bus "$sTypePerson" "Test$suffixName $roleName" "-" select to\[$relMem\] dump |] != "True"} {

                             # get role symbolic name
                             set sRoleProp [lindex [split [mql list property to role "$role"]] 0]
                             #Connecting Person business object to Host Company with Member relationship
                             set sCmd { mql connect bus "$sTypeCompany" "$sHostCompany" "-" \
                                        relationship "$relMem" \
                                        to "$sTypePerson" "Test$suffixName $roleName" "-" \
                                        "$sAttProjectRole" "$sRoleProp" \
                                      }
                             set mqlret [ catch {eval $sCmd} outStr ]
                             if {"$mqlret" != 0} {
                                 puts "$outStr"
                                 return -code 1
                             }
                         }

                         # Check if Relationship Employee exists
                         if {"$relEmp" != "" && [mql print bus "$sTypePerson" "Test$suffixName $roleName" "-" select to\[$relEmp\] dump |] != "True"} {

                             # Set Project Role Attribute with Relationship name.
                             set sCmd { mql connect bus "$sTypeCompany" "$sHostCompany" "-" \
                                        relationship "$relEmp" \
                                        to "$sTypePerson" "Test$suffixName $roleName" "-"
                                      }
                             set mqlret [ catch {eval $sCmd} outStr ]
                             if {"$mqlret" != 0} {
                                 puts "$outStr"
                                 return -code 1
                             }
                         }
                    }
                }
            }
        }
    }
    
    mql trigger on
    return -code 0
}

