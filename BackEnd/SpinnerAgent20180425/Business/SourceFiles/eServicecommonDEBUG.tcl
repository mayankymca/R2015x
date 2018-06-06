###############################################################################
# @libdoc       eServicecommonDEBUG.tcl
#
# @Library:     Tcl Procedures for printing debugging messages
#
# @Brief:       Tcl procedures to output debugging messages
#
# @Description: Procedures mxDEBUGIN, mxDEBUGOUT, and DEBUG
#
# @libdoc       Copyright (c) 1998, Matrix One, Inc. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
###############################################################################

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      mxDEBUGIN
#
# @Brief:       For printing debug message upon code entry
#
# @Description: This function should be called on entry to each procedure
#               in the MatrixOne Application libraries/programs.  Prints
#               indented ENTER messages with level numbers.
#
#               Reads the global environment variable MXDEBUGFLAG.
#               If not less than zero, outputs debugging trace message.
#
# @Parameters:  sMsg - the name of the procedure being entered
#
# @Returns:     Nothing
#
# @Usage:       mxDEBUGIN <procedure being entered>
#
# @Example:     set procname "mxIssueNlib.tcl"
#               mxDEBUGIN "$procname"
#
# @procdoc
#******************************************************************************

proc mxDEBUGIN { sMsg } {
    set lclDEBUG [mql get env global MXDEBUGFLAG]

    # If debugging, print message and increment global
    if { $lclDEBUG > 0 } {
        eval  [utLoad utStr.tcl]
        puts stdout \
             "[utStrRepeat " " [expr 2 * $lclDEBUG]]$lclDEBUG: ENTER $sMsg"
        mql set env global MXDEBUGFLAG [expr $lclDEBUG + 1]
    }
}
# end mxDEBUGIN


#******************************************************************************
# @procdoc      mxDEBUGOUT
#
# @Brief:       For printing debug message upon code exit
#
# @Description: This function should be called on exit from each procedure
#               in the MatrixOne Application libraries/programs.  Prints
#               indented EXIT messages with level numbers.
#
#               Reads the global environment variable MXDEBUGFLAG.
#               If not less than zero, outputs debugging trace message.
#
# @Parameters:  sMsg - the name of the procedure being exited
#
# @Returns:     Nothing
#
# @Usage:       mxDEBUGOUT <procedure being exited>
#
# @Example:     set procname "mxIssueNlib.tcl"
#               mxDEBUGOUT "$procname"
#
# @procdoc
#******************************************************************************

proc mxDEBUGOUT { sMsg } {
    set lclDEBUG [mql get env global MXDEBUGFLAG]

    # If debugging,
    # print message with level-1 and decrement global (but not below 1)
    if { $lclDEBUG > 0 } {
        eval  [utLoad utStr.tcl]
        set lclDEBUG [expr $lclDEBUG - 1]
        puts stdout "[utStrRepeat " " [expr 2 * $lclDEBUG]]$lclDEBUG: EXIT $sMsg"
        if { $lclDEBUG >= 1 } {
            mql set env global MXDEBUGFLAG $lclDEBUG
        }
    }
}
# end mxDEBUGOUT


#******************************************************************************
# @procdoc      DEBUG
#
# @Brief:       For printing debug message
#
# @Description: This function can be called anywhere to print debugging
#               messages, as passed by the calling procedure.  Prints
#               indented messages with level numbers.
#
#               Reads the global environment variable MXDEBUGFLAG.
#               If not less than zero, outputs debugging trace message.
#
# @Parameters:  sMsg - Message to print if debugging is turned on.
#
# @Returns:     Nothing
#
# @Usage:       DEBUG <message to print>
#
# @Example:     DEBUG "lList = $lList"
#
# @procdoc
#******************************************************************************

proc DEBUG { sMsg } {
    set lclDEBUG [mql get env global MXDEBUGFLAG]

    # If debugging, print message with level-1
    if { $lclDEBUG > 0 } {
        eval  [utLoad utStr.tcl]
        set lclDEBUG [expr $lclDEBUG - 1]
        puts stdout "[utStrRepeat " " [expr 2 * $lclDEBUG]]$lclDEBUG: $sMsg"
    }
}
# end DEBUG

# End of Module

