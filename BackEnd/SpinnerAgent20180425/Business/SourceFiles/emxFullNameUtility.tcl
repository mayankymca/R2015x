#################################################################################
#
# $RCSfile: emxFullNameUtility.tcl.rca $ $Revision: 1.14 $
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

    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Sub keys
    set LAST_NAME "<Last Name>"
    set FIRST_NAME "<First Name>"
    set USER_NAME "<User Name>"
    
    # Load utility files
    eval [utLoad $sRegProgName]

    # Get input parameters
    set sLogFileName [mql get env 1]
    set sFullNameFormat [mql get env 2]
    
    # If log file not specified then default to stdout
    if {[string length $sLogFileName] == 0} {
        set nLogFileId "stdout"
    # Open log file for writing log messages.
    } else {
        set nLogFileId [open "$sLogFileName" w 666]
    }

    # If full name format not specified then default to 
    # '<Last Name>, <First Name>' format
    if {[string length $sFullNameFormat] == 0} {
        set sFullNameFormat "<Last Name>, <First Name>"
    }

    # Log initial message about program name and input parameters.
    puts $nLogFileId "Executing conversion program [mql get env 0] ..."
    puts $nLogFileId ""
    
    if {[string length $sLogFileName] == 0} {
        puts $nLogFileId "Log File : stdout"
    } else {
        puts $nLogFileId "Log File : $sLogFileName"
    }
    puts $nLogFileId "Full Name Format : $sFullNameFormat"
    puts $nLogFileId ""

    # Get admin names from properties
    set sTypePerson [eServiceGetCurrentSchemaName type $sRegProgName type_Person]
    set sAttLastName [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_LastName]
    set sAttFirstName [eServiceGetCurrentSchemaName attribute $sRegProgName attribute_FirstName]

    # Trigger Off
    set sCmd {mql trigger off}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxFullNameUtility.tcl"
        puts stdout ">$outStr"
        return -code 1
    }

    # Start transaction
    set sCmd {mql start transaction}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxFullNameUtility.tcl"
        puts stdout ">$outStr"
        mql trigger on
        return -code 1
    }

    # Get all the person admin objects.
    set sCmd {mql list person}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxFullNameUtility.tcl"
        puts stdout ">$outStr"
        mql trigger on
        mql abort transaction
        return -code 1
    }
    set lAllPersons [split "$outStr" \n]

    # Get sub keys used in the format (<Last Name>, <First Name>, <User Name>)
    set bLastNameFound 0
    set bFirstNameFound 0
    set bUserNameFound 0
    if {[string first "$LAST_NAME" "$sFullNameFormat"] >= 0} {
        set bLastNameFound 1
    }
    if {[string first "$FIRST_NAME" "$sFullNameFormat"] >= 0} {
        set bFirstNameFound 1
    }
    if {[string first "$USER_NAME" "$sFullNameFormat"] >= 0} {
        set bUserNameFound 1
    }

    # Get list of all the Person business objects
    # and their attributes first name and last name
    set sCmd {mql temp query bus "$sTypePerson" * * \
                  select \
                  name \
                  attribute\[$sAttLastName\].value \
                  attribute\[$sAttFirstName\].value \
                  dump tcl \
                  }
    set mqlret [ catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxFullNameUtility.tcl"
        puts stdout ">$outStr"
        mql trigger on
        mql abort transaction
        return -code 1
    }

    # For each Person
    foreach lPersonInfo $outStr {
        # Get business object name
        set sPersonName [lindex [lindex $lPersonInfo 3] 0]
        
        # If business object name does not exists in list of admin person names then
        # Log a warning message about person not found
        # Continue to next person.
        if {[lsearch $lAllPersons "$sPersonName"] == -1} {
            puts $nLogFileId "WARNING:"
            puts $nLogFileId "Could not find Person '$sPersonName'"
            puts $nLogFileId ""
            continue
        }

        # Get last name
        set sLastName [lindex [lindex $lPersonInfo 4] 0]

        # Get first name
        set sFirstName [lindex [lindex $lPersonInfo 5] 0]
        
        # If any sub keys in format input parameters contains 
        # <First Name> and <Last Name> and 
        # if attributes 'First Name' and 'Last Name' are blank then
        # Log a message about blank values.
        # Continue
        if {$bLastNameFound == 1 && ([string length "$sLastName"] == 0 || "$sLastName" == "Unknown") && \
            $bFirstNameFound == 1 && ([string length "$sFirstName"] == 0 || "$sFirstName" == "Unknown")} {
            puts $nLogFileId "WARNING:"
            puts $nLogFileId "Attribute $sAttLastName and $sAttFirstName are not set for Person '$sPersonName'"
            puts $nLogFileId ""
            continue
        }
        if {$bLastNameFound == 1 && ([string length "$sLastName"] == 0 || "$sLastName" == "Unknown")} {
            puts $nLogFileId "WARNING:"
            puts $nLogFileId "Attribute $sAttLastName is not set for Person '$sPersonName'"
            puts $nLogFileId ""
            continue
        }
        if {$bFirstNameFound == 1 && ([string length "$sFirstName"] == 0 || "$sFirstName" == "Unknown")} {
            puts $nLogFileId "WARNING:"
            puts $nLogFileId "Attribute $sAttFirstName is not set for Person '$sPersonName'"
            puts $nLogFileId ""
            continue
        }

        # Form full name out of the format specified by replacing sub keys as follows
        # <First Name> -> attribute value 'First Name' on person business object
        # <Last Name> -> attribute value 'Last Name' on person business object
        # <User Name> -> name of person business object
        set sFullName "$sFullNameFormat"
        regsub -all "$LAST_NAME" "$sFullName" "$sLastName" sFullName
        regsub -all "$FIRST_NAME" "$sFullName" "$sFirstName" sFullName
        regsub -all "$USER_NAME" "$sFullName" "$sPersonName" sFullName
        
        set sCmd {mql modify person "$sPersonName" \
                      fullname "$sFullName" \
                      }
        set mqlret [ catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts stdout ">ERROR       : emxFullNameUtility.tcl"
            puts stdout ">$outStr"
            mql trigger on
            mql abort transaction
            return -code 1
        }
    }

    # commit transaction
    set sCmd {mql commit transaction}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxFullNameUtility.tcl"
        puts stdout ">$outStr"
        mql trigger on
        return -code 1
    }

    # Trigger Off
    set sCmd {mql trigger off}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        puts stdout ">ERROR       : emxFullNameUtility.tcl"
        puts stdout ">$outStr"
        return -code 1
    }
}

