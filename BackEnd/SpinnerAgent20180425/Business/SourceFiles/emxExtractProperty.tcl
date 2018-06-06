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

   set sSchemaType [mql get env 1]
   if {$sSchemaType == "association" || $sSchemaType == "site"} {
      set bAssoc TRUE
      set sFilter "*"
   } else {
      set sFilter [mql get env 2]
      set bAssoc FALSE
   }
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

   set bAppend FALSE
   set sAppend ""
   if {$sSchemaType == "table"} {
      set sFilter "system"
      set sAppend "_table_ALL"
   } else {
      if {$sFilter != ""} {
         regsub -all "\134\052" $sFilter "ALL" sAppend
         regsub -all "\134\174" $sAppend "-" sAppend
         regsub -all "/" $sAppend "-" sAppend
         regsub -all ":" $sAppend "-" sAppend
         regsub -all "<" $sAppend "-" sAppend
         regsub -all ">" $sAppend "-" sAppend
         regsub -all " " $sAppend "" sAppend
         set sAppend "_$sSchemaType\_$sAppend"
      }
   }
   
   if {$sGreaterThanEqualDate != "" && !$bAssoc} {
      set sModDateMin [clock scan $sGreaterThanEqualDate]
   } else {
      set sModDateMin ""
   }
   if {$sLessThanEqualDate != "" && !$bAssoc} {
      set sModDateMax [clock scan $sLessThanEqualDate]
   } else {
      set sModDateMax ""
   }
   
   set sSpinnerPath [mql get env SPINNERPATH]
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix/Business";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix/Business";
      }
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerPropertyData$sAppend.xls"
   set sFile "Schema Type\tSchema Name\tProperty Name\tProperty Value\tTo Schema Type\tTo Schema Name\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {$bAssoc} {
      set lsName [split [mql list $sSchemaType] \n]
   } else {
      set lsName [split [mql list $sSchemaType $sFilter] \n]
   }

   foreach sName $lsName {
      set bPass TRUE
      if {$sModDateMin != "" || $sModDateMax != ""} {
         set sModDate [mql print $sSchemaType $sName select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }
      }
      if {$bPass && $sSchemaType == "form"} {
         if {[mql print form $sName select web dump] != "TRUE"} {
            set bPass FALSE
         }
      }
      set sSpinnerTest ""
      if {$bSpinnerAgentFilter} {
         if {$sSchemaType == "table"} {
            set sSpinnerTest [mql print $sSchemaType $sName system select property\[SpinnerAgent\] dump]
         } else {
            set sSpinnerTest [mql print $sSchemaType $sName select property\[SpinnerAgent\] dump]
         }
      }
                  
      if {($bPass == "TRUE") && ($bAssoc || $bSpinnerAgentFilter != "TRUE" || $sSpinnerTest != "")} {
         if {$bAssoc} {
            set lsPrint [split [mql print $sSchemaType "$sName"] \n]
         } elseif {$sSchemaType == "table"} {
            set lsPrint [split [mql print $sSchemaType "$sName" system select property dump |] |]
         } else {
            set lsPrint [split [mql print $sSchemaType "$sName" select property dump |] |]
         }
         
         foreach sPrint $lsPrint {
            set sToType ""
            set sToName ""
            set sPropertyValue ""
            if {$bAssoc && [string first "property" $sPrint] < 0} {continue}
            if {[string first " value " $sPrint] > -1} {
               regsub " value " $sPrint "|" slsPrint
               set lslsPrint [split $slsPrint |]
               set sPropertyValue [lindex $lslsPrint 1]
               set sPrint [lindex $lslsPrint 0]
			 
            }
            if {[string first " to " $sPrint] > -1} {
               regsub " to " $sPrint "|" slsPrint
               set lslsPrint [split $slsPrint |]
               set sPropertyName [lindex $lslsPrint 0]
               set slsToTypeName [lindex $lslsPrint 1]
               regsub " " $slsToTypeName "|" slsToTypeName
               set lslsToTypeName [split $slsToTypeName |]
               set sToType [lindex $lslsToTypeName 0]
               set sToName [lindex $lslsToTypeName 1]
            } else {
               set sPropertyName [string trim $sPrint]
            }
            if {[string first "state_" $sPropertyName] != 0} {
               append sFile "$sSchemaType\t$sName\t$sPropertyName\t$sPropertyValue\t$sToType\t$sToName\n"
               set bAppend TRUE
            }
         }
      }
   }
   if {$bAppend} {
      set iFile [open $sPath w]
      puts $iFile $sFile
      close $iFile
      puts "Property data loaded in file $sPath"
   }
}
