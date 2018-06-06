
tcl;

eval {
   if {[info host] == "sn732plp" } {
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
   set sTypeReplace "menu "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "menu"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerMenuData$sAppend.xls"
   set lsMenu [split [mql list menu $sFilter] \n]
   set sFile "Name\tRegistry Name\tDescription\tLabel\tHref\tAlt\tSetting Name (use '|' delim)\tSetting Value (use '|' delim)\tCommand/Menu Names (in order-use '|' delim)\tHidden (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sMenu $lsMenu {
         set bPass TRUE
         set sModDate [mql print menu $sMenu select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print menu $sMenu select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print menu $sMenu select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sMenu)} sMsg
            regsub -all " " $sMenu "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sMenu
            }
            set sDescription [mql print menu $sMenu select description dump]
            set sLabel [mql print menu $sMenu select label dump]
            set sHref [mql print menu $sMenu select href dump]
            set sAlt [mql print menu $sMenu select alt dump]
            set sHidden [mql print menu $sMenu select hidden dump]
            set slsSettingName [mql print menu $sMenu select setting.name dump " | "]
            set slsSettingValue [mql print menu $sMenu select setting.value dump " | "]
			
			#KYB - Traverse thr each setting of a menu, look for char '|' 
		    set slsSettingValue ""
		    set iCnt2 0
		    set sListMenuSettingName [split $slsSettingName |]  
		    set iCnt1 [ llength $sListMenuSettingName ]
		    foreach sMenuSettingName $sListMenuSettingName {
				 set sMenuSettingName [string trim $sMenuSettingName]
				 set slsMenuSettingValue [ mql print menu $sMenu select setting\[$sMenuSettingName\].value dump ]
				 regsub -all "\134|" $slsMenuSettingValue "<PIPE:>" slsMenuSettingValue
				 regsub -all "<PIPE:><PIPE:>" $slsMenuSettingValue "||" slsMenuSettingValue				 
				 append slsSettingValue $slsMenuSettingValue
				 incr iCnt2
				 if { $iCnt2 != $iCnt1 } { append slsSettingValue " | " }
		    }
			
            set lsPrint [split [mql print menu $sMenu] \n]
            set slsCmdMenu [mql print menu $sMenu select child dump " | "]
            append sFile "$sName\t$sOrigName\t$sDescription\t$sLabel\t$sHref\t$sAlt\t$slsSettingName\t$slsSettingValue\t$slsCmdMenu\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Menu data loaded in file $sPath"
}
