

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
   set sTypeReplace "dimension "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "dimension"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerDimensionData$sAppend.xls"
   set lsDimension [split [mql list dimension $sFilter] \n]
   set sFile "Name\tRegistry Name\tDescription\tHidden (boolean)\n"
   set sPath2 "$sSpinnerPath/Business/SpinnerDimensionUnitData$sAppend.xls"
   set sFile2 "Dimension Name\tUnit Name\tUnit Label\tUnit Description\tMultiplier (real)\tOffset (real)\tSetting Names (use \"|\" delim)\tSetting Values (use \"|\" delim)\tSystemName (use \"|\" delim)\tSystemUnit (use \"|\" delim)\tDefault (boolean)\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sDimension $lsDimension {
         set bPass TRUE
         set sModDate [mql print dimension $sDimension select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print dimension $sDimension select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print dimension $sDimension select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sDimension)} sMsg
            regsub -all " " $sDimension "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sDimension
            }
            set sDescription [mql print dimension $sDimension select description dump]
            set sHidden [mql print dimension $sDimension select hidden dump]
            set slsUnit [mql print dimension $sDimension select "unit" dump " | "]
            append sFile "$sName\t$sOrigName\t$sDescription\t$sHidden\n"
# Dimension Unit
            set lsUnit [split [mql print dimension $sDimension select unit dump |] |]
            foreach sUnit $lsUnit {
               set sName $sUnit
               set sLabel [mql print dimension $sDimension select unit\[$sUnit\].label dump]
               set sDescription [mql print dimension $sDimension select unit\[$sUnit\].description dump]
               set sMultiplier [mql print dimension $sDimension select unit\[$sUnit\].multiplier dump]
               set sOffset [mql print dimension $sDimension select unit\[$sUnit\].offset dump]
               set slsSettingName [mql print dimension $sDimension select unit\[$sUnit\].setting dump " | "]
               set slsSettingValue [mql print dimension $sDimension select unit\[$sUnit\].setting.value dump " | "]
			   
			   #KYB - Traverse thr each setting of a dimension unit, look for char '|' 
			   set slsSettingValue ""
			   set iCnt2 0
			   set sListUnitSettingName [split $slsSettingName |]  
			   set iCnt1 [ llength $sListUnitSettingName ]
			   foreach sUnitSettingName $sListUnitSettingName {
					set sUnitSettingName [string trim $sUnitSettingName]
					set slsUnitSettingValue [ mql print dimension $sDimension select unit\[$sUnit\].setting\[$sUnitSettingName\].value dump ]
					regsub -all "\134|" $slsUnitSettingValue "<PIPE:>" slsUnitSettingValue					
					append slsSettingValue $slsUnitSettingValue
					incr iCnt2
					if { $iCnt2 != $iCnt1 } { append slsSettingValue " | " }
			   }
			   
               set sDefault [mql print dimension $sDimension select unit\[$sUnit\].default dump]
               set lsSysName [list ]
               set lsSysUnit [list ]
               set lsPrint [split [mql print dimension "$sDimension"] \n]
               set bTrip "FALSE"
               foreach sPrint $lsPrint {
                  set sPrint [string trim $sPrint]
                  if {[string range $sPrint 0 3] == "unit" && [string first $sUnit $sPrint] > 3} {
                     set bTrip TRUE
                  } elseif {$bTrip && [string range $sPrint 0 3] == "unit"} {
                     break
                  } elseif {$bTrip} {
                     if {[string range $sPrint 0 5] == "system"} {
                        regsub "system" $sPrint "" sPrint
                        regsub " to unit " $sPrint "\|" sPrint
                        set lsSysNameUnit [split $sPrint "|"]
                        lappend lsSysName [string trim [lindex $lsSysNameUnit 0]]
                        lappend lsSysUnit [string trim [lindex $lsSysNameUnit 1]]
                     }
                  }
               }
               set slsSysName [join $lsSysName " | "]
               set slsSysUnit [join $lsSysUnit " | "]
               append sFile2 "$sDimension\t$sName\t$sLabel\t$sDescription\t$sMultiplier\t$sOffset\t$slsSettingName\t$slsSettingValue\t$slsSysName\t$slsSysUnit\t$sDefault\n"
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Dimension data loaded in file $sPath"
   set iFile [open $sPath2 w]
   puts $iFile $sFile2
   close $iFile
   puts "Dimension Unit data loaded in file $sPath2"
}
