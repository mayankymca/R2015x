#################################################################################
#
# $RCSfile: eServiceConversion9-5-0-0ShowAccessToAllPolicies.tcl.rca $ $Revision: 1.7 $
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
    # return if conversion program is already run
    set sCmd {mql print program eServiceSystemInformation.tcl \
                  select property\[Conversion9-5-0-0ShowAccessToAllPolicies\].value \
                  dump | \
                  }
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-2-1AddEmployeeRole.tcl"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return -code 1
    }
    
    if {"$outStr" == "Executed"} {
        return -code 0
    }

    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Load utility files
    eval [utLoad $sRegProgName]

    # Get symbolic name
    set sProp [string trim [mql get env 1]]

    # Get name from property
    set sPolicy [eServiceGetCurrentSchemaName policy $sRegProgName $sProp]

    # Get all the states
    set sCmd {mql print policy "$sPolicy" select state dump |}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-0-0ShowAccessToAllPolicies.tcl"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return -code 1
    }
    set lStates [split $outStr |]

    # For each state get users defined and add show access to them
    # and public and owner
    set sPreCmd "mql modify policy \"$sPolicy\""
    set sPostCmd ""
    foreach sState $lStates {

        set lUsers {}
        # Get owner and public access
        set sCmd {mql print policy "$sPolicy" select state\[$sState\].publicaccess state\[$sState\].owneraccess dump |}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-0-0ShowAccessToAllPolicies.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }
        set lOutput [split $outStr |]
        set sPublicAccess [lindex $lOutput 0]
        set sOwnerAccess [lindex $lOutput 1]

        # Check if owner and public has read access
        if {"$sPublicAccess" == "all" || [string first "read" "$sPublicAccess"] >= 0} {
            lappend lUsers public
        }
        if {"$sOwnerAccess" == "all" || [string first "read" "$sOwnerAccess"] >= 0} {
            lappend lUsers owner
        }

        set sCmd {mql print policy "$sPolicy" select state\[$sState\].access}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-0-0ShowAccessToAllPolicies.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }
        set lOutput [lrange [split $outStr \n] 1 end]

        foreach sOutput $lOutput {
            if {[string first "read" [lindex [split "$sOutput" =] 1]] >= 0} {
                set nOpenBracketIndex [string last "\[" "$sOutput"]
                set nCloseBracketIndex [string last "\]" "$sOutput"]

                set sUserName [string range "$sOutput" [expr $nOpenBracketIndex + 1] [expr $nCloseBracketIndex - 1]]
                lappend lUsers "$sUserName"
            }
        }

        # For each user who has read access give show access
        if {[llength $lUsers] > 0} {
            append sPostCmd " state '$sState'"
            foreach sUser $lUsers {
                if {"$sUser" == "public" || "$sUser" == "owner"} {
                    append sPostCmd " add $sUser show"
                } else {
                    append sPostCmd " add user '$sUser' show"
                }
            }
        }
    }
    if {"$sPostCmd" != ""} {
        set sModCmd "${sPreCmd}${sPostCmd}"
        set mqlret [ catch {eval $sModCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion9-5-0-0ShowAccessToAllPolicies.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return -code 1
        }
    }
    
    return -code 0
}

