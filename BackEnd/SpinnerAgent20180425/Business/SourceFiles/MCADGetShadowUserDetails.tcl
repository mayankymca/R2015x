###############################################################################
#                                                                             #
#   Copyright (c) 1998-2007 Dassault Systemes.  All Rights Reserved.          #
#   This program contains proprietary and trade secret information of         #
#   Dassault Systemes.  Copyright notice is precautionary only and does not   #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################



##################################################################
# MCADGetShadowUserDetails.tcl                                 
# 
# This program is to get the name and password
# for system administrator for a given site
#
# This is called to set the shadow user context
#
# Parameter - site name for which the system administrator's
#             name and password is to be found
#
# Returns - string containing name and password separated by |
# 
# In case input site name does not match any of the sites in the 
# program, it returns name and password of a default system 
# administrator
####################################################################


tcl;
eval {
 
           set suName ""
           set suPassword ""
           set retStr ""
           set siteName [mql get env 1]
           
	   # if site name matches, return sys admin's name and
	   # password
           if { $siteName == "TestSite" } {
                        set suName "creator"
                        set suPassword ""  
                        set retStr "${suName}|${suPassword}"
           } else {
	                # Return default system admin's name and passw
                        set suName "creator"
                        set suPassword ""  
                        set retStr "${suName}|${suPassword}" 
	   }
           
           return $retStr
}

