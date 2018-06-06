###############################################################################
#
# $RCSfile: eServicecommonSetAttribute.tcl.rca $ $Revision: 1.17 $ 
#
# Description:  This is a generic procedure that can be used to modify the contents of
#                    any object attribute.
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
# Load MQL/Tcl utility procedures
#
###############################################################################


###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
#
# Procedure:    appSetAttribute
#
# Description:  A method that will set an attribute (sAttrName) in a
#                    specific instance of an object (sType sName sREV)
#                    to a specified value (sNewValue)
#
# Parameters:   sType         These three parameters specify which
#                      sName        object to modify
#                      sRev
#                      sAttrName    Attribute in the specified object to modify
#                      sNewValue  Value to be placed into the specified attribute
#
# Returns:         mqlret        Return code (anything > 0 indicates an error)
#
#******************************************************************************

proc appSetAttribute { sType sName sRev sAttrName sNewValue } {

	#
	# Debugging trace - display message
	#
	mxDEBUGIN "appSetAttribute"
	
	set mqlret 0
	set lOutList {}
		
	#
	# Modify the specified object's attribute to sNewValue
	#
	DEBUG "Changing value of $sAttrName to $sNewValue"
	set szCommand "mql mod bus \"$sType\" \"$sName\" \"$sRev\" \"$sAttrName\" \"$sNewValue\" "
	set mqlret [ catch {eval  $szCommand} $lOutList ]

	#
	# Testing for error condition in the $szCommand execution
	#    
	if { $mqlret } { 
	DEBUG "Error: (appSetAttribute) Updating attributes: Command=$szCommand\n$lOutList" 
	}

	#
	# Debugging trace - note exit
	#
	mxDEBUGOUT "appSetAttribute"
    
	return $mqlret

}
#end setAttribute

#End of Module

