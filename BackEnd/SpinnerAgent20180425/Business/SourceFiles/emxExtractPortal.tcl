
tcl;

eval {
   if {[info host] == "mostermant61p" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
   	  set cmd "debugger_eval"
   	  set xxx [debugger_init]
   } else {
   	  set cmd "eval"
   }
}

$cmd {
#  Set up array for symbolic name mapping
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "portal "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "portal"} {
         regsub $sTypeReplace $sPropertyTo "" sPropertyTo
         regsub "_" $sPropertyName "|" sSymbolicName
         set sSymbolicName [lindex [split $sSymbolicName |] 1]
         array set aSymbolic [list $sPropertyTo $sSymbolicName]
      }
   }

   set sFilter [mql get env 1]
   set bTemplate [mql get env 2]
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

   set sAppend ""
   if {$sFilter != ""} {
      regsub -all "\134\052" $sFilter "ALL" sAppend
      regsub -all "\134\174" $sAppend "-" sAppend
      regsub -all "/" $sAppend "-" sAppend
      regsub -all ":" $sAppend "-" sAppend
      regsub -all "<" $sAppend "-" sAppend
      regsub -all ">" $sAppend "-" sAppend
      regsub -all " " $sAppend "" sAppend
      set sAppend "_$sAppend"
   }
   
   if {$sGreaterThanEqualDate != ""} {
      set sModDateMin [clock scan $sGreaterThanEqualDate]
   } else {
      set sModDateMin ""
   }
   if {$sLessThanEqualDate != ""} {
      set sModDateMax [clock scan $sLessThanEqualDate]
   } else {
      set sModDateMax ""
   }
   
   set sSpinnerPath [mql get env SPINNERPATH]
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
      }
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerPortalData$sAppend.xls"
   set lsPortal [split [mql list portal $sFilter] \n]
   set sFile "Name\tRegistry Name\tDescription\tLabel\tHref\tAlt\tSetting Name (use \"|\" delim)\tSetting Value (use \"|\" delim)\tChannels (use \",\" w/ \"|\" delims)\tHidden (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sPortal $lsPortal {
         set bPass TRUE
         set sModDate [mql print portal $sPortal select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print portal $sPortal select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print portal $sPortal select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sPortal)} sMsg
            regsub -all " " $sPortal "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sPortal
            }
            set sDescription [mql print portal $sPortal select description dump]
            set sLabel [mql print portal $sPortal select label dump]
            set sHref [mql print portal $sPortal select href dump]
            set sAlt [mql print portal $sPortal select alt dump]
            set sHidden [mql print portal $sPortal select hidden dump]
            set slsSettingName [mql print portal $sPortal select setting.name dump " | "]
            set slsSettingValue [mql print portal $sPortal select setting.value dump " | "]
			
			#KYB - Traverse thr each setting of a channel, look for char '|' 
		    set slsSettingValue ""
		    set iCnt2 0
		    set sListPortalSettingName [split $slsSettingName |]  
		    set iCnt1 [ llength $sListPortalSettingName ]
		    foreach sPortalSettingName $sListPortalSettingName {
				 set sPortalSettingName [string trim $sPortalSettingName]
				 set slsPortalSettingValue [ mql print portal $sPortal select setting\[$sPortalSettingName\].value dump ]
				 regsub -all "\134|" $slsPortalSettingValue "<PIPE:>" slsPortalSettingValue
				 regsub -all "<PIPE:><PIPE:>" $slsPortalSettingValue "||" slsPortalSettingValue					 
				 append slsSettingValue $slsPortalSettingValue
				 incr iCnt2
				 if { $iCnt2 != $iCnt1 } { append slsSettingValue " | " }
		    }
			
            set slsChannel ""
            set lsPrint [split [mql print portal $sPortal] \n]
            set lsChannel ""
            foreach sPrint $lsPrint {
               set sPrint [string trim $sPrint]
               if {[string first "channel" $sPrint] == 0} {
                  regsub "channel " $sPrint "" sPrint
                  lappend lsChannel $sPrint
               }
            }
            set slsChannel [join $lsChannel " | "]
            append sFile "$sName\t$sOrigName\t$sDescription\t$sLabel\t$sHref\t$sAlt\t$slsSettingName\t$slsSettingValue\t$slsChannel\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Portal data loaded in file $sPath"
}
