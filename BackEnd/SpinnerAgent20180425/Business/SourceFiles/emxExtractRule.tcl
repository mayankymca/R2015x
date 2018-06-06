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
   set sTypeReplace "rule "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "rule"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerRuleData$sAppend.xls"
   if {$sMxVersion < 9.0} {
      set lsRule [split [mql list rule] \n]
   } else {
      set lsRule [split [mql list rule $sFilter] \n]
   }
   set sFile "Rule Name\tRegistry Name\tDescription\tPrograms (use \"|\" delim)\tAttributes (use \"|\" delim)\tRelationships (use \"|\" delim)\tForms (use \"|\" delim)\tHidden (boolean)\tEnforce Reserve Access (boolean)\tIcon File\n"
   
   if {!$bTemplate} {
      foreach sRule $lsRule {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print rule $sRule select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print rule $sRule select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print rule $sRule select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sRule)} sMsg
            regsub -all " " $sRule "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sRule
            }
            set sDescription [mql print rule $sRule select description dump]
            set bHidden [mql print rule $sRule select hidden dump]
            
            set lsAccess ""
            set slsAccess ""
            set lsAccessTemp [split [mql print rule $sRule select access] \n]
            foreach sAccessTemp $lsAccessTemp {
               set sAccessTemp [string trim $sAccessTemp]
               if {[string first "access\[" $sAccessTemp] > -1} {
                  set iFirst [expr [string first "access\[" $sAccessTemp] + 7]
                  set iSecond [expr [string first "\] =" $sAccessTemp] -1]
                  lappend lsAccess [string range $sAccessTemp $iFirst $iSecond]
               }
            }
            set slsAccess [join $lsAccess " | "]
            
            set slsProgram ""
            set slsAttribute ""
            set slsForm "" 
            set slsRelationship ""
            set lsPrint [split [mql print rule $sRule] \n]   

set sEnforceReserveAccess [string tolower [mql print rule $sRule select enforcereserveaccess dump]]

			
            foreach sPrint $lsPrint {
               set sPrint [string trim $sPrint]
               foreach sReference [list program attribute form "Relationship Type"] {
                  if {[string first $sReference $sPrint] == 0} {
                     regsub "$sReference\: " $sPrint "" slsReference
                     regsub -all ", " $slsReference " | " slsReference
                     switch $sReference {
                        program {
                           set slsProgram $slsReference
                        } attribute {
                           set slsAttribute $slsReference
                        } form {
                           set slsForm $slsReference
                        } "Relationship Type" {
                           set slsRelationship $slsReference
                        }
                     }
                  }
               }
            }
            
            append sFile "$sName\t$sOrigName\t$sDescription\t$slsProgram\t$slsAttribute\t$slsRelationship\t$slsForm\t$bHidden\t$sEnforceReserveAccess\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Rule data loaded in file $sPath"
}
