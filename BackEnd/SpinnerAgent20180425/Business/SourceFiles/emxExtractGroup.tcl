
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
   set sTypeReplace "group "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "group"} {
         regsub $sTypeReplace $sPropertyTo "" sPropertyTo
         regsub "_" $sPropertyName "|" sSymbolicName
         set sSymbolicName [lindex [split $sSymbolicName |] 1]
         array set aSymbolic [list $sPropertyTo $sSymbolicName]
      }
   }

   proc pContinue {lsList} {
      set bFirst TRUE
      set slsList ""
      set lslsList ""
      foreach sList $lsList {
         if {$bFirst} {
            set slsList $sList
            set bFirst FALSE
         } else {
            append slsList " | $sList"
            if {[string length $slsList] > 6400} {
               lappend lslsList $slsList
               set slsList ""
               set bFirst TRUE
            }
         }
      }
      if {$slsList != ""} {
         lappend lslsList $slsList
      }
      return $lslsList
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
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix/Business";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix/Business";
      }
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerGroupData$sAppend.xls"
   set lsGroup [split [mql list group $sFilter] \n]
   set sFile "Group Name\tRegistry Name\tDescription\tParent Groups (use \"|\" delim)\tChild Groups (use \"|\" delim)\tAssignments (use \"|\" delim)\tSite\tHidden (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sGroup $lsGroup {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            if {$sModDateMin != "" || $sModDateMax != ""} {
               set sModDate [mql print group $sGroup select modified dump]
               set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
               if {$sModDateMin != "" && $sModDate < $sModDateMin} {
                  set bPass FALSE
               } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
                  set bPass FALSE
               }
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print group $sGroup select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print group $sGroup select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sGroup)} sMsg
            regsub -all " " $sGroup "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sGroup
            }
            set sDescription [mql print group $sGroup select description dump]
            set slsParentGroup [mql print group $sGroup select parent dump " | "]
            set lsChildGroup [pContinue [split [mql print group $sGroup select child dump |] |] ]
            set iLast [llength $lsChildGroup]
#            set lsAssignment [pContinue [split [mql print group $sGroup select assignment dump |] |] ]
#            if {[llength $lsAssignment] > $iLast} {
#               set iLast [llength $lsAssignment]
#            }
            set bHidden [mql print group $sGroup select hidden dump]
            set sSite ""
            set lsSiteTemp [split [mql print group $sGroup] \n]
            foreach sSiteTemp $lsSiteTemp {
               set sSiteTemp [string trim $sSiteTemp]
               if {[string first "site" $sSiteTemp] == 0} {
                  regsub "site " $sSiteTemp "" sSite
                  break
               }
            }
            set iCounter 1
            set sMultiline ""
            if {$iLast > 1} {
               set sMultiline " <MULTILINE.1.$iLast>"
            }
#            foreach sOnce [list 1] sChildGroup $lsChildGroup sAssignment $lsAssignment {
            foreach sOnce [list 1] sChildGroup $lsChildGroup {
               regsub -all "\\\(" $sName "\\\(" sTestName
               regsub -all "\\\)" $sTestName "\\\)" sTestName
#               regsub "$sTestName " $sAssignment "" sAssignment
#               regsub -all "\\| $sTestName " $sAssignment "\| " sAssignment
#               if {[string range $sAssignment 0 0] == "\"" && [string range $sAssignment end end] == "\""} {
#                  set sAssignment [string range $sAssignment 1 [expr [string length $sAssignment] - 2]]
#               }
               if {$iCounter == 1} {
#                  append sFile "$sName$sMultiline\t$sOrigName\t$sDescription\t$slsParentGroup\t$sChildGroup\t$sAssignment\t$sSite\t$bHidden\n"
                  append sFile "$sName$sMultiline\t$sOrigName\t$sDescription\t$slsParentGroup\t$sChildGroup\t\t$sSite\t$bHidden\n"
                  set bFirst FALSE
               } else {
                  set sMultiline " <MULTILINE.$iCounter.$iLast>"
#                  append sFile "$sName$sMultiline\t\t\t\t$sChildGroup\t$sAssignment\n"
                  append sFile "$sName$sMultiline\t\t\t\t$sChildGroup\n"
               }
               incr iCounter
            }
#            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Group data loaded in file $sPath"
}
