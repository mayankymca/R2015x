tcl;
mql trigger off;
eval {
   #KYB Start - User can specify his/her own location for Logs to be generated during execution
   set sOS [string tolower $tcl_platform(os)];
   set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
   set userExtractionPath [ mql get env SPINNEREXTRACTIONPATH ]
   
   if { $userExtractionPath != "" } {
      if {[file exists $userExtractionPath] == 0} {
			#puts "NOTICE - Extraction path is not valid, extraction will take place at default location."
			if { [string tolower [string range $sOS 0 5]] == "window" } {
				set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
            } else {
				set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
		    }
		} else {
			set sSpinnerPath $userExtractionPath
			append	sSpinnerPath "/SpinnerAgent$sSuffix";
		}
   } else {
	   #puts "NOTICE - Extraction path is not set, extraction will take place at default location."
	   if { [string tolower [string range $sOS 0 5]] == "window" } {
		  set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
	   } else {
		  set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
	   }
   }
   
   set sEscapeMode [mql print escape]
	if {[string match "Escape*on" $sEscapeMode] == 1 } {
		mql set escape off
	}
	
   set sSpinnerSetting "emxSpinnerSettings.tcl"
   #KYB End - User can specify his/her own location for Logs to be generated during execution

	if {[catch {
		   if {[file exists "$sSpinnerPath/$sSpinnerSetting"] == 1} {	      
			 set iFileSet [open "$sSpinnerPath/$sSpinnerSetting" r]
			 eval [read $iFileSet]
			 close $iFileSet
			 set sSettingLoc "file $sSpinnerPath/$sSpinnerSetting"
		   } elseif {[mql list program $sSpinnerSetting] != ""} {	   
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

  #call jpo emxReadExtractSchema to set env
  set sExtractSchema [mql execute prog emxReadExtractSchema]
  puts $sExtractSchema
  
  #get env ExtractSchema
  set sEvalExtractSchema [mql get env ExtractSchema]
  eval  $sEvalExtractSchema
  
  if {[string match "Escape*on" $sEscapeMode] == 1 } {
		mql set escape on
	}
}
#KYB Fixed SCR-0007291 - After running the emxExtractSchema.tcl program triggers are left turned off.
mql trigger on;

