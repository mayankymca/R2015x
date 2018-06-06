
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
   set sTypeReplace "interface "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "interface"} {
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

   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   set sPath "$sSpinnerPath/Business/SpinnerInterfaceData$sAppend.xls"
   set lsInterface [split [mql list interface $sFilter] \n]
   if {$sMxVersion >= 10.8} { 
      set sFile "Name\tRegistry Name\tParents (use \"|\" delim)\tAbstract (boolean)\tDescription\tAttributes (use \"|\" delim)\tTypes (use \"|\" delim)\tHidden (boolean)\tRels (use \"|\" delim)\tIcon File\n"
   } else {
      set sFile "Name\tRegistry Name\tParents (use \"|\" delim)\tAbstract (boolean)\tDescription\tAttributes (use \"|\" delim)\tTypes (use \"|\" delim)\tHidden (boolean)\tIcon File\n"
   }
      
   if {!$bTemplate} {
      foreach sInterface $lsInterface {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print interface $sInterface select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print interface $sInterface select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print interface $sInterface select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sInterface)} sMsg
            regsub -all " " $sInterface "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sInterface
            }
            set sDescription [mql print interface $sInterface select description dump]
            set sHidden [mql print interface $sInterface select hidden dump]
            set slsAttribute [mql print interface $sInterface select attribute dump " | "]
            set slsType [mql print interface $sInterface select type dump " | "]
            set slsDerived [mql print interface $sInterface select derived dump " | "]
            set bAbstract [mql print interface $sInterface select abstract dump]
            if {$sMxVersion >= 10.8} {
               set slsRel [mql print interface $sInterface select relationship dump " | "]
               append sFile "$sName\t$sOrigName\t$slsDerived\t$bAbstract\t$sDescription\t$slsAttribute\t$slsType\t$sHidden\t$slsRel\n"
            } else {
               append sFile "$sName\t$sOrigName\t$slsDerived\t$bAbstract\t$sDescription\t$slsAttribute\t$slsType\t\t$sHidden\n"
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Interface data loaded in file $sPath"
}
