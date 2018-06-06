###############################################################################
# $RCSfile: eServicecommonGetExchangeRates.tcl.rca $ $Revision: 1.17 $
#
# @progdoc      eServicecommonGetExchangeRates.tcl
#
# @Description: This program gets code of eServiceExchangeRates
#
# @Returns:     sucess or failure.
#
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
###############################################################################

tcl;

# Start eval to prevent echo to stdout.
eval {

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

  set sOutput [ mql print program '$sProgram' select code dump ]

  return $sOutput
}
# end utload


######################################################################
#
# LOAD MQL/Tcl TOOLKIT LIBRARIES.
#
#####################################################################

eval [ utLoad eServicecommonDEBUG.tcl ]
eval [ utLoad eServicecommonShadowAgent.tcl]

######################################################################
#
# MAIN
#
######################################################################

    # Return found objects in progname
    set sProgName [mql get env 0]

    #
    # Debugging trace - note entry
    #
    mxDEBUGIN "$sProgName"

    #
    # Error handling variables
    #
    set mqlret 0
    set outStr ""

    set sCmd "mql print program eServiceExchangeRates select code dump"
    pushShadowAgent
    set mqlret [ catch {eval $sCmd} outStr]
    popShadowAgent
    if {$mqlret == 0} {
        return "0|$outStr"
    } else {
        return "1|Error: $sProgName - $outStr"
    }
}

# End of Module

