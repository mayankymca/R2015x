#*******************************************************************************
# @progdoc        emxSpinnerPnOImpor.tcl
#
# @Brief:         Generates PnO Schema files.
#
# @Description:   Generates PnO schema 
#
# @Parameters:    User Name, Password, Context
#
# @Returns:       Nothing   
#
# @Usage:         Run in MQL:
#		  exec prog emxPnOImpor.tcl user password;
#
#*******************************************************************************

tcl;

eval {
           mql set env command ImportPnO;

		   set sPnOPath [pwd]
		   set PnOSettings "emxSpinnerPnOConfiguration.tcl"
		  
		   mql set env PnOPath $sPnOPath

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
		  
		  set lsSetting [list sAppilcationURL sSecurityContext bLicenseSetting sTransactionMode sTransactionSize]
		  set bSetErr FALSE
		  foreach sSetting $lsSetting {
			  if {[info exists "$sSetting"]} {
				  if { $sSetting == "sAppilcationURL" } { mql set env URL $sAppilcationURL }
				  if { $sSetting == "sSecurityContext" } { mql set env SecurityContext $sSecurityContext }
				  if { $sSetting == "bLicenseSetting" } { mql set env LicenseSetting $bLicenseSetting }
				  if { $sSetting == "sTransactionMode" } { mql set env TransactionMode $sTransactionMode }
				  if { $sSetting == "sTransactionSize" } { mql set env TransactionSize $sTransactionSize }
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

