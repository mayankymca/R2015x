###############################################################################
# @progdoc      eServicecommonDEBUGset.tcl
#
# @Brief:       Turn on debugging
#
# @Description: Sets global variable MXDEBUGFLAG to 1
#               and tells user through 'puts' that it had done so
#
# @Parameters:  None
#
# @Returns:     global MXDEBUGFLAG is set to 1
#
# @Usage:       Usually as a toolbar button
#
# @progdoc      Copyright (c) 1998, Matrix One, Inc. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# Notes: By defining variables in the 'Define Global Variables' section
#        and utLoading *.tcl files in the 'Load MQL/Tcl Utility Procedures'
#        section, those variables are automatically made global and procedures
#        will only need to be loaded once.
#
###############################################################################

tcl;
eval {

######################################################################
#
# MAIN
#
######################################################################

#
# Set debugging global
#
puts stdout "\n>>>>>>>>>>>>>> Setting MXDEBUGFLAG to 1\n"
mql set env global MXDEBUGFLAG 1
}

