
#************************************************************************
# @progdoc      eServicecommonUtilMqlCmdWrapper.tcl
#
# @Brief:       Mql Command Wrapper
#
# @Description: This program is used to execute mql commands and catch
#               any errors.  It is called from the eServicecommonUtilMqlCmd
#               java method.
#
# @Parameters:  sMqlCmd - The mql command to execute.
#
# @Returns:     If there are no errors, then the results of the mql command
#               prefixed with "0|" is returned.  If there is an error, then
#               an error message prefixed with "1|" is returned.  The calling
#               java program must know how to deal with these responses.
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#************************************************************************

tcl;

eval {

    # Make sure quoting and verbose mode are off.
    mql quote off
    mql verbose off

    # Get the mql command parameter.
    set sMqlCmd [ mql get env 1 ]

    # Execute the command and return the results.
    return "[ catch $sMqlCmd sResult ]|$sResult"

}


