#*******************************************************************************
# @progdoc        emxSpinnerPnOExport.tcl
#
# @Brief:         Generates PnO Schema files.
#
# @Description:   Generates PnO schema 
#
# @Parameters:    Command, User Name, Password, PnOType, ObjectName
#
# @Returns:       Nothing   
#
# @Usage:         Run in MQL:
#		  exec prog emxPnOExport.tcl user password PnOType ObjectName;
#
#*******************************************************************************

tcl;

eval {
           mql set env command ExportPnO;
		   
		   set PnOSettings "emxSpinnerPnOConfiguration.tcl"
		   set userExtractionPath [ mql get env PNOEXTRACTIONPATH ]
		   
		   if { $userExtractionPath != "" } {
  				mql set env PnOPath $userExtractionPath
		   } else {
	         puts "\nNOTICE: Extraction path not set, Starting PnO extraction at default location. To specify extraction path execute command \"set env PNOEXTRACTIONPATH path.\"\n"
           } 
		   
		   if {[catch {
		   if {[mql list program $PnOSettings] != ""} {
		      eval [mql print program $PnOSettings select code dump]
		      set sSettingLoc "database program $PnOSettings"
	       } else {
		     puts "ERROR:  Spinner Settings file missing.  Load emxSpinnerPnOConfiguration.tcl in database or place in Spinner path"
		     exit 1
		     return
	       }
		   } sMsg] != 0} {
			   puts "\nERROR: Problem with settings file $PnOSettings\n$sMsg"
			   exit 1
			   return
		  }		  
		  
		  set lsSetting [list sAppilcationURL sSecurityContext bLicenseSetting]
		  set bSetErr FALSE
		  foreach sSetting $lsSetting {
			  if {[info exists "$sSetting"]} {
				  if { $sSetting == "sAppilcationURL" } { mql set env URL $sAppilcationURL }
				  if { $sSetting == "sSecurityContext" } { mql set env SecurityContext $sSecurityContext }
				  if { $sSetting == "bLicenseSetting" } { mql set env LicenseSetting $bLicenseSetting }
			  } else {
				 puts "ERROR:  Setting $sSetting is not defined.  Add parameter to $sSettingLoc - pull from latest emxSpinnerPnOConfiguration.tcl program"
				 set bSetErr TRUE
			 }
	     }
		  if {$bSetErr} {
             exit 1
             return
         }
		   mql execute prog emxSpinnerPnOPlugin;
     }

