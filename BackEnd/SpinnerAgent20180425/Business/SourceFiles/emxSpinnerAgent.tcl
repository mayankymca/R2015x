tcl;
mql trigger off;
eval {
# Read arguments 
   set sSpinnerPath [mql get env 1]
   set sArg1 [string tolower $sSpinnerPath]   
   set sSpinnerSetting [mql get env 2]
   set sArg2 [string tolower $sSpinnerSetting]
   set sArg3 [string tolower [mql get env 3]]
   set sArg4 [string tolower [mql get env 4]]
   set bScan FALSE
   set bReset FALSE   
   set bMQLExtract FALSE
   
   if {$sArg3 == "scan"} {
      set bScan TRUE
   } elseif {$sArg3 == "reset" || $sArg3 == "force"} {
      set bReset TRUE
   } elseif {$sArg2 == "scan"} {
      set bScan TRUE
      set sSpinnerSetting ""
   } elseif {$sArg2 == "reset" || $sArg2 == "force"} {
      set bReset TRUE
      set sSpinnerSetting ""
   } elseif {$sArg1 == "scan"} {
      set bScan TRUE
      set sSpinnerPath ""
      set sSpinnerSetting ""
   } elseif {$sArg1 == "reset" || $sArg1 == "force"} {
      set bReset TRUE
      set sSpinnerPath ""
      set sSpinnerSetting ""
   }
   
   if { $sArg1 == "mql" || $sArg2 == "mql" || $sArg3 == "mql" || $sArg4 == "mql" } {
		mql set env VALIDATEMQLLIC TRUE
	}
   
	set sEscapeMode [mql print escape]
	if {[string match "Escape*on" $sEscapeMode] == 1 } {
		mql set escape off
	}
	
# Read settings
   if {$sSpinnerSetting == ""} {set sSpinnerSetting emxSpinnerSettings.tcl}
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

   set lsSetting [list sParentChild bUseAssignmentField bUseAccessField bRetainBusObject bImportOverwrite bBusObjOverwrite bBusRelOverwrite bTriggerAdd bTriggerMod bTriggerDel bTriggerChk sReplaceSymbolic sDelimiter sRangeDelim bShowModOnly bStreamLog bShowTransaction bOverlay bCompile lsSubDirSequence lsFileExtSkip rRefreshLog bAbbrCue iBusObjCommit bForeignVault bContinueOnError bChangeAttrType bPersonOverwrite bCDM bOut bJPOExtraction bCommandFile bResequenceStates sLogDir sEncodingSystem sEncofingUTF]
   set bSetErr FALSE   
   foreach sSetting $lsSetting {
      if {[info exists "$sSetting"]} {
		if { $sSetting == "sLogDir" } { mql set env LOGDIRPATH $sLogDir }
      } else {
         puts "ERROR:  Setting $sSetting is not defined.  Add parameter to $sSettingLoc - pull from latest emxSpinnerSettings.tcl program"
         set bSetErr TRUE
      }
   }
   if {$bSetErr} {
      exit 1
      return
   }

  #call jpo emxReadSpinnerAgent to set env
  set sAgent [mql execute prog emxReadSpinnerAgent]
  puts $sAgent
  
  #get env SpinnerAgent
  set sEvalAgent [mql get env SpinnerAgent]
  eval  $sEvalAgent
  set sEncodingSystem [encoding system $sEncodingSystem]
  
  if {[string match "Escape*on" $sEscapeMode] == 1 } {
		mql set escape on
	}
}
#KYB Fixed SCR-0007291 - After running the emxExtractSchema.tcl program triggers are left turned off.
mql trigger on;
