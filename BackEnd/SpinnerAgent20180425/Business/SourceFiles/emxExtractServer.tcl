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
   set sTypeReplace "server "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "server"} {
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

   set sPath "$sSpinnerPath/System/server.xls"
   set lsServer [split [mql list server $sFilter] \n]
   set sFile "name\tRegistry Name\tdescription\tuser\tpassword\tconnect\ttimezone\tforeign\thidden\ticon\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sServer $lsServer {
         if {[catch {set sName [mql print server $sServer select name dump]} sMsg] != 0} {
            puts "ERROR: Problem with retrieving info on server '$sServer' - Error Msg:\n$sMsg"
            continue
         }
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print server $sServer select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print server $sServer select property\[SpinnerAgent\] dump] != "")} {
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sServer)} sMsg
            regsub -all " " $sServer "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sServer
            }
                     
            set sDescription [mql print server $sServer select description dump]
            set sUser [mql print server $sServer select user dump]
            set sConnect [mql print server $sServer select connect dump]
            set sTimeZone [mql print server $sServer select timezone dump]
            set sForeign [mql print server $sServer select foreign dump]
            set sHidden [mql print server $sServer select hidden dump]
            append sFile "$sName\t$sOrigName\t$sDescription\t$sUser\t\t$sConnect\t$sTimeZone\t$sForeign\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Server data loaded in file $sPath"
}
