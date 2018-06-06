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
   set sTypeReplace "association "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "association"} {
         regsub $sTypeReplace $sPropertyTo "" sPropertyTo
         regsub "_" $sPropertyName "|" sSymbolicName
         set sSymbolicName [lindex [split $sSymbolicName |] 1]
         array set aSymbolic [list $sPropertyTo $sSymbolicName]
      }
   }

   set sFilter "*"
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
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix/Business";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix/Business";
      }
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerAssociationData$sAppend.xls"
   set sFile "Name\tRegistry Name\tDescription\tDefinition (use format \"<USER> | and / or | <USER>\" ...)\tHidden (boolean)\tIcon File\n"
   set lsAssociation [split [mql list association] \n]
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sAssociation $lsAssociation {
         set sDescription ""
         set sDefinition ""
         set sModified ""
         set sHidden ""
         set sOrigName ""
         set sSpinnerAgent ""
         set lsPrint [split [mql print association $sAssociation] \n]
   
         foreach sPrint $lsPrint {
            set sPrint [string trim $sPrint]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sAssociation)} sMsg
            regsub -all " " $sAssociation "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sAssociation
            }
            if {[string first "description" $sPrint] == 0} {
               regsub "description" $sPrint "" sDescription
               set sDescription [string trim $sDescription]
            } elseif {[string first "modified" $sPrint] == 0} {
               regsub "modified" $sPrint "" sModified
               set sModified [string trim $sModified]
            } elseif {[string first "property SpinnerAgent" $sPrint] == 0} {
               regsub "property SpinnerAgent" $sPrint "" sSpinnerAgent
               set sSpinnerAgent [string trim $sSpinnerAgent]
            } elseif {[string first "definition" $sPrint] == 0} {
               regsub "definition" $sPrint "" sDefinition
               regsub -all "\"" $sDefinition "" sDefinition
               regsub -all "&&" $sDefinition "| and |" sDefinition
               regsub -all "\\\|\\\|" $sDefinition "| or |" sDefinition
               set sDefinition [string trim $sDefinition]
            } elseif {$sPrint == "hidden"} {
               set sHidden true
            } elseif {$sPrint == "nothidden"} {
               set sHidden false
            }
         }
   
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [clock scan [clock format [clock scan $sModified] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || $sSpinnerAgent != "")} {
            append sFile "$sAssociation\t$sOrigName\t$sDescription\t$sDefinition\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Association data loaded in file $sPath"
}
