
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

   set lsSchemaType [mql get env 1]
   set sFilter "*"
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

   set sAppend "_ALL"   
   set sSpinnerPath [mql get env SPINNERPATH]
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
      }
      file mkdir "$sSpinnerPath/Business"
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
      
   set sPath "$sSpinnerPath/Business/SpinnerPropertyData$sAppend.xls"
   set sFile "Schema Type\tSchema Name\tProperty Name\tProperty Value\tTo Schema Type\tTo Schema Name\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   foreach sSchemaType $lsSchemaType {
      if {$sSchemaType == "webform"} {set sSchemaType "form"} 
      if {$sSchemaType == "association" || $sSchemaType == "site"} {
         set bAssoc TRUE
      } else {
         set bAssoc FALSE
      }
   
      if {$sSchemaType == "table"} {
         set lsName [split [mql list table system] \n]
      } else {
         set lsName [split [mql list $sSchemaType] \n]
      }
      foreach sName $lsName {
         set bPass TRUE
         if {!$bAssoc} {
            if {$sModDateMin != "" || $sModDateMax != ""} {
               if {$sSchemaType == "table"} {
                  set sModDate [mql print $sSchemaType $sName system select modified dump]
               } else {
                  set sModDate [mql print $sSchemaType $sName select modified dump]
               }
               set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
               if {$sModDateMin != "" && $sModDate < $sModDateMin} {
                  set bPass FALSE
               } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
                  set bPass FALSE
               }
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
			    if {[string first "history" $sPrint] > -1} {
			     break;
			      }
                  set sPropertyName [string trim $sPrint]
               }
               if {[string first "state_" $sPropertyName] != 0} {
                  append sFile "$sSchemaType\t$sName\t$sPropertyName\t$sPropertyValue\t$sToType\t$sToName\n"
               }
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Property data loaded in file $sPath"
}
