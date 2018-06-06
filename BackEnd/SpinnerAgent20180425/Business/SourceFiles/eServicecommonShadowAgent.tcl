###############################################################################
#
# $RCSfile: eServicecommonShadowAgent.tcl.rca $ $Revision: 1.46 $
#
# Description:  This file contains procedures that change the session
#               context to/from a shadow agent who has all permissions in all
#               states of all policies and is a business administrator.
#
#               This users password is hard coded in this procedure and can
#               be changed as long as the same change is made to the shadow
#               agents password in the system also.
#
#               The username is: "User Agent"
#               The password is: "shadowsecret"
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
# Define Global Variables
#
###############################################################################

###############################################################################
#
# Define Procedures
#
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


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################

eval  [ utLoad eServiceSchemaVariableMapping.tcl ]

#******************************************************************************
#
# Procedure:    pushShadowAgent
#
# Description:  This procedure changes the session context to User Agent and
#               stores the name of the user who is currently logged in to an
#               environment variable.
#
# Parameters:   none
#
# Returns:      0 if no errors
#               non-zero if error occurs
#
#******************************************************************************

proc pushShadowAgent {{userAgent "person_UserAgent"}} {

    #
    # Debugging trace - note entry
    #
    set progname "pushShadowAgent"
    #mxDEBUGIN "$progname"

    #
    # Error handling variables
    #
    set mqlret 0
    set pushResult 1
    set outStr ""

    # If property name of agent is specified then get name from property.
    if {[string first "person_" "$userAgent"] >= 0} {
        set userAgent [eServiceGetCurrentSchemaName person "eServiceSchemaVariableMapping.tcl" "$userAgent"]
    }

    # Define the user agent login and password
    set agentPassword [mql execute program emxcommonPushPopShadowAgent -method getShadowAgentPassword]

    # Save user for later - set APPREALUSER environment variable
    set sUser [mql get env USER]
    if { $mqlret == 0 } {
      set mqlret [catch {eval mql set env APPREALUSER \"$sUser\" } outStr ]
    }

    # Set context to a shadow agent
    if { $mqlret == 0 } {
      set mqlret [catch {eval mql push context person \"$userAgent\" password \"$agentPassword\" } outStr ]
      set pushResult $mqlret
    }

    # Set USER environment variable to shadow agent
    if { $mqlret == 0 } {
      set mqlret [catch {eval mql set env USER \"$userAgent\" } outStr ]
    }

    # Handle errors
    if { $mqlret != 0 } {
      if {$pushResult == 0} {
        mql pop context
      }
      mql set env APPREALUSER ""
      #DEBUG "Error ($progname): $outStr"
    }

    #
    # Debugging trace - note exit
    #
    #mxDEBUGOUT "$progname"
    set sCmd  {mql exec program "emxContextUtil" -method "pushBeanContext" "$userAgent"}
    set mqlret [catch {eval $sCmd} outStr ]
    return $mqlret
}
# end <pushShadowAgent>

#******************************************************************************
#
# Procedure:    popShadowAgent
#
# Description:  This procedure changes the session context back to the orginal
#               User. The original user is determined by accessing an
#               environment variable where the pushShadowAgent procedure put
#               the name of the orginal user.
#
# Parameters:   none
#
# Returns:      0 if no errors
#               non-zero if error occurs
#
#******************************************************************************

proc popShadowAgent {} {

    #
    # Debugging trace - note entry
    #
    set progname "popShadowAgent"
    #mxDEBUGIN "$progname"

    #
    # Error handling variables
    #
    set mqlret 0
    set outStr ""

    # Retreive user from environment
    set sUser [mql get env APPREALUSER]

    # Set context to original user
    if { $mqlret == 0 } {
      set mqlret [catch {eval mql pop context} outStr ]
    }

    # Set USER environment variable to original user
    if { $mqlret == 0 } {
      set mqlret [catch {eval mql set env USER \"$sUser\" } outStr ]
    }

    # Unset APPREALUSER environment variable
    if { $mqlret == 0 } {
      set mqlret [catch {eval mql set env APPREALUSER \"\" } outStr ]
    }

    # Handle errors
    if { $mqlret != 0 } {
      #DEBUG "Error ($progname): $outStr"
    }

    #
    # Debugging trace - note exit
    #
    #mxDEBUGOUT "$progname"

    set sCmd {mql exec program "emxContextUtil" -method popBeanContext}
    set mqlret [catch {eval $sCmd} outStr ]
    return $mqlret
}
# end <popShadowAgent>

# End of Module

