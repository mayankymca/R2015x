#*******************************************************************************
# @progdoc        emxSpinnerPnOLegacyLicenseMapping.tcl
#
# @Brief:         Upgrade license mapping.
#
# @Description:   Upgrade lower level License to higher level
#
#
# @Returns:       Nothing   
#
# @Usage:         Run in MQL:
#		  exec prog emxSpinnerPnOLegacyLicenseMapping.tcl ;
#
#*******************************************************************************

tcl;

eval {
		   mql execute prog emxSpinnerPnOLegacyLicense;
     }

