
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
   set sTypeReplace "index "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "index"} {
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
      file mkdir $sSpinnerPath/System
   }

   set sPath "$sSpinnerPath/System/index.xls"
   set lsIndex [split [mql list index $sFilter] \n]
   set sFile "name\tRegistry Name\tdescription\tattribute\tenable\tunique\thidden\ticon\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sIndex $lsIndex {
         if {[catch {set sName [mql print index $sIndex select name dump]} sMsg] != 0} {
            puts "ERROR: Problem with retrieving info on index '$sIndex' - Error Msg:\n$sMsg"
            continue
         }
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print index $sIndex select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print index $sIndex select property\[SpinnerAgent\] dump] != "")} {
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sIndex)} sMsg
            regsub -all " " $sIndex "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sIndex
            }
                     
            set sDescription [mql print index $sIndex select description dump]
            set slsAttribute [mql print index $sIndex select attribute dump " | "]
            set sEnable [mql print index $sIndex select enabled dump]
            set sUnique [mql print index $sIndex select unique dump]
            set sHidden [mql print index $sIndex select hidden dump]
            append sFile "$sName\t$sOrigName\t$sDescription\t$slsAttribute\t$sEnable\t$sUnique\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Index data loaded in file $sPath"
}
