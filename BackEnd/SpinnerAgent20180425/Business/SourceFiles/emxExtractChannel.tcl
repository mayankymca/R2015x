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
#
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "channel "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "channel"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerChannelData$sAppend.xls"
   set lsChannel [split [mql list channel $sFilter] \n]
   set sFile "Name\tRegistry Name\tDescription\tLabel\tHref\tAlt\tSetting Name (use \"|\" delim)\tSetting Value (use \"|\" delim)\tCommands (use \"|\" delim)\tHeight\tHidden (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sChannel $lsChannel {
         set bPass TRUE
         set sModDate [mql print channel $sChannel select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print channel $sChannel select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print channel $sChannel select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sChannel)} sMsg
            regsub -all " " $sChannel "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sChannel
            }
            set sDescription [mql print channel $sChannel select description dump]
            set sLabel [mql print channel $sChannel select label dump]
            set sHref [mql print channel $sChannel select href dump]
            set sAlt [mql print channel $sChannel select alt dump]
            set sHidden [mql print channel $sChannel select hidden dump]
            set slsSettingName [mql print channel $sChannel select setting.name dump " | "]
            set slsSettingValue [mql print channel $sChannel select setting.value dump " | "]
			
			#KYB - Traverse thr each setting of a channel, look for char '|' 
		    set slsSettingValue ""
		    set iCnt2 0
		    set sListChannelSettingName [split $slsSettingName |]  
		    set iCnt1 [ llength $sListChannelSettingName ]
		    foreach sChannelSettingName $sListChannelSettingName {
				 set sChannelSettingName [string trim $sChannelSettingName]
				 set slsChannelSettingValue [ mql print channel $sChannel select setting\[$sChannelSettingName\].value dump ]
				 regsub -all "\134|" $slsChannelSettingValue "<PIPE:>" slsChannelSettingValue
				 regsub -all "<PIPE:><PIPE:>" $slsChannelSettingValue "||" slsChannelSettingValue				 
				 append slsSettingValue $slsChannelSettingValue
				 incr iCnt2
				 if { $iCnt2 != $iCnt1 } { append slsSettingValue " | " }
		    }
			
            set slsCommand [mql print channel $sChannel select command dump |]
            set sHeight [mql print channel $sChannel select height dump]
            append sFile "$sName\t$sOrigName\t$sDescription\t$sLabel\t$sHref\t$sAlt\t$slsSettingName\t$slsSettingValue\t$slsCommand\t$sHeight\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Channel data loaded in file $sPath"
}
