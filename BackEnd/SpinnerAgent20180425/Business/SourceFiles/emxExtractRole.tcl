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
   set sTypeReplace "role "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "role"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerRoleData$sAppend.xls"
   set lsRole [split [mql list role $sFilter] \n]
   set sFile "Role Name\tRegistry Name\tDescription\tParent Roles (use \"|\" delim)\tChild Roles (use \"|\" delim)\tAssignments (use \"|\" delim)\tSite\tHidden (boolean)\tRole Type (Project/Org)\tmaturity (none/public/protected/private)\tcategory (none/oem/goldpartner/partner/supplier/customer/contractor)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sRole $lsRole {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            if {$sModDateMin != "" || $sModDateMax != ""} {
               set sModDate [mql print role $sRole select modified dump]
               set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
               if {$sModDateMin != "" && $sModDate < $sModDateMin} {
                  set bPass FALSE
               } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
                  set bPass FALSE
               }
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print role $sRole select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print role $sRole select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sRole)} sMsg
            regsub -all " " $sRole "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sRole
            }
            set sDescription [mql print role $sRole select description dump]
            set slsParentRole [mql print role $sRole select parent dump " | "]
            set lsChildRole [pContinue [split [mql print role $sRole select child dump |] |] ]
            set iLast [llength $lsChildRole]
#            set lsAssignment [pContinue [split [mql print role $sRole select assignment dump |] |] ]
#            if {[llength $lsAssignment] > $iLast} {
#               set iLast [llength $lsAssignment]
#            }
            set bHidden [mql print role $sRole select hidden dump]
            set sSite ""
            set lsSiteTemp [split [mql print role $sRole] \n]
            foreach sSiteTemp $lsSiteTemp {
               set sSiteTemp [string trim $sSiteTemp]
               if {[string first "site" $sSiteTemp] == 0} {
                  regsub "site " $sSiteTemp "" sSite
                  break
               }
            }
			set sProjOrg ""
			if {$sMxVersion >= 10.9} {
			   set bIsAnOrg [mql print role $sRole select isanorg dump]
			   if {$bIsAnOrg} {
			      set sProjOrg "Org"
			   } else {
                  set bIsAProj [mql print role $sRole select isaproject dump]
			      if {$bIsAProj} {
				     set sProjOrg "Project"
				  }
			   }
			} 

			   set sMaturity [mql print role $sRole select maturity dump]
			   set sCategory [mql print role $sRole select category dump]

            set iCounter 1
            set sMultiline ""
            if {$iLast > 1} {
               set sMultiline " <MULTILINE.1.$iLast>"
            }
#            foreach sOnce [list 1] sChildRole $lsChildRole sAssignment $lsAssignment {
            foreach sOnce [list 1] sChildRole $lsChildRole {
               regsub -all "\\\(" $sName "\\\(" sTestName
               regsub -all "\\\)" $sTestName "\\\)" sTestName
#               regsub "$sTestName " $sAssignment "" sAssignment
#               regsub -all "\\| $sTestName " $sAssignment "\| " sAssignment
#               if {[string range $sAssignment 0 0] == "\"" && [string range $sAssignment end end] == "\""} {
#                  set sAssignment [string range $sAssignment 1 [expr [string length $sAssignment] - 2]]
#               }
               if {$iCounter == 1} {
#                  append sFile "$sName$sMultiline\t$sOrigName\t$sDescription\t$slsParentRole\t$sChildRole\t$sAssignment\t$sSite\t$bHidden\n"
                  append sFile "$sName$sMultiline\t$sOrigName\t$sDescription\t$slsParentRole\t$sChildRole\t\t$sSite\t$bHidden\t$sProjOrg\t$sMaturity\t$sCategory\n"
                  set bFirst FALSE
               } else {
                  set sMultiline " <MULTILINE.$iCounter.$iLast>"
#                  append sFile "$sName$sMultiline\t\t\t\t$sChildRole\t$sAssignment\n"
                  append sFile "$sName$sMultiline\t\t\t\t$sChildRole\n"
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
   puts "Role data loaded in file $sPath"
}
