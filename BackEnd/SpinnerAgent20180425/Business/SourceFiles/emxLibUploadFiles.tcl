tcl;
mql verb off
eval { 
	 set insProg [mql insert program .///Business///SourceFiles///prog]
	 set isValidLicense [mql execute prog emxPreInstallChecks -method checkLicenseKey]
	 set strTrue "True"
	 if { $isValidLicense == $strTrue } {
	  	return
	 } else { 
		exit 1
		return
	 }
}

