###############################################################################
#
# $RCSfile: emxIniFileReader.tcl.rca $ $Revision: 1.4 $
#
# Description: The program has functions to read INI file
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
# Procedure:    emxIniGetKeys
#
# Description:  Procedure to get all the keys in specified section.
#
# Parameters:   sFile           - Name of the ini file
#               sSection        - Name of the section
#
# Returns:      lKeys           - List of all the keys
#
###############################################################################

proc emxIniGetKeys { sFile sSection } {

    set lKeys {}

    # Open the ini file
    set fileId [open $sFile r]

    set bSectionStart "FALSE"

    # Read each line.
    while {[gets $fileId sLine] != -1} {

        set sLine [string trim $sLine]
        # Look for comments and blank lines
        if {[string length "$sLine"] == 0 || [string range "$sLine" 0 0] == "#"} {
            continue
        }

        # check if section definition line
        set nStartIndex [string first "\[" "$sLine"]
        set nEndIndex [string first "\]" "$sLine"]
        if {"$nStartIndex" >= 0 && "$nEndIndex" > 0} {
            if {"$bSectionStart" == "FALSE"} {
                set sCurrSection [string range "$sLine" [expr $nStartIndex + 1] [expr $nEndIndex - 1]]
                set sCurrSection [string trim "$sCurrSection"]

                if {"$sSection" == "$sCurrSection"} {
                    set bSectionStart "TRUE"
                    continue
                }
            } else {
                break
            }
        }
        
        if {"$bSectionStart" == "TRUE"} {
            set lKeyValue [split "$sLine" "="]
            lappend lKeys [lindex $lKeyValue 0]
        }
    }

    # Close file
    close $fileId
    
    return $lKeys
}

    # Get AppInfo.rul
    set sAppInfoFile [mql get env 1]
    # Get section
    set sSection [mql get env 2]
    
    # Get keys
    set lKeys [emxIniGetKeys "$sAppInfoFile" "$sSection"]
    
    return -code 0 "$lKeys"
}

