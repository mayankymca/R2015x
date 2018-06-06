tcl;
mql trigger off;
eval {
    set sSpinnerSetting "emxSpinnerSettings.tcl"
   
	if {[catch {
		   if {[mql list program $sSpinnerSetting] != ""} {	   
			 eval [mql print program $sSpinnerSetting select code dump]
			 set sSettingLoc "database program $sSpinnerSetting"
		   } else {
			 puts "ERROR:  Spinner Settings file missing.  Load emxSpinnerSettings.tcl in database or place in Spinner path"
			 exit 1
			 return
		   }
		  } sMsg] != 0} {
			   puts "\nERROR: Problem with settings file $sSpinnerSetting\n$sMsg"
			   exit 1
			   return
		  }
		  
		  set lsSetting [list sParentChild bUseAssignmentField bUseAccessField bRetainBusObject bImportOverwrite bBusObjOverwrite bBusRelOverwrite bTriggerAdd bTriggerMod bTriggerDel bTriggerChk sReplaceSymbolic sDelimiter sRangeDelim bShowModOnly bStreamLog bShowTransaction bOverlay bCompile lsSubDirSequence lsFileExtSkip rRefreshLog bAbbrCue iBusObjCommit bForeignVault bContinueOnError bChangeAttrType bPersonOverwrite bCDM bOut bJPOExtraction bCommandFile bResequenceStates sLogDir]
   set bSetErr FALSE	 
	
	foreach sSetting $lsSetting {
      if {[info exists "$sSetting"]} {
			if { $sSetting == "sLogDir" } { mql set env LOGDIRPATH $sLogDir }
      } else {
         puts "ERROR:  Setting $sSetting is not defined.  Add parameter to $sSettingLoc - pull from latest emxSpinnerSettings.tcl program"
         set bSetErr TRUE
      }
   }
  
  
  #call jpo emxRebuildLicense to rehost the license
  set sRehost [mql execute prog emxRebuildLicense]  
}
#KYB Fixed SCR-0007291 - After running the emxExtractSchema.tcl program triggers are left turned off.
mql trigger on;

