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
   set sTypeReplace "program "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "program"} {
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
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix/Business";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix/Business";
      }
      file mkdir $sSpinnerPath
      file mkdir "$sSpinnerPath/SourceFiles"
   }

   set sPath "$sSpinnerPath/Business/SpinnerProgramData$sAppend.xls"
   set sSourceFileDir "$sSpinnerPath/Business/SourceFiles"
   set lsProgram [split [mql list program $sFilter] \n]
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion 2012
   }
   if {$sMxVersion > 2011} {
      set sFile "Program Name\tRegistry Name\tDescription\tType (mql<default>/java/ekl/external)\tExecute (immediate<default>/deferred)\tNeeds Bus Obj (boolean)\tDownloadable (boolean)\tPiped (boolean)\tPooled (boolean)\tHidden (boolean)\tUser\tIcon File\n"
   } else {
      set sFile "Program Name\tRegistry Name\tDescription\tType (mql<default>/java/external)\tExecute (immediate<default>/deferred)\tNeeds Bus Obj (boolean)\tDownloadable (boolean)\tPiped (boolean)\tPooled (boolean)\tHidden (boolean)\tUser\tIcon File\n"
   }         
   
   if {!$bTemplate} {
      foreach sProgram $lsProgram {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            if {$sModDateMin != "" || $sModDateMax != ""} {
               set sModDate [mql print program $sProgram select modified dump]
               set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
               if {$sModDateMin != "" && $sModDate < $sModDateMin} {
                  set bPass FALSE
               } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
                  set bPass FALSE
               }
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print program $sProgram select property\[SpinnerAgent\] dump] != "")} {
            if {[mql print program $sProgram select iswizardprogram dump] != "TRUE"} {
               set sName [mql print program $sProgram select name dump]
               set sOrigName ""
               catch {set sOrigName $aSymbolic($sProgram)} sMsg
               regsub -all " " $sProgram "" sOrigNameTest
               if {$sOrigNameTest == $sOrigName} {
                  set sOrigName $sProgram
               }
               set sDescription [mql print program $sProgram select description dump]
      
               if {$sMxVersion > 2011 && [mql print program $sProgram select iseklprogram dump] == "TRUE"} {
                  set sProgType ekl
               } elseif {$sMxVersion > 8.9 && [mql print program $sProgram select isjavaprogram dump] == "TRUE"} {
                  set sProgType java
               } elseif {[mql print program $sProgram select ismqlprogram dump] == "TRUE"} {
                  set sProgType mql
               } else {
                  set sProgType external
               }
               
               set sExecute [mql print program $sProgram select execute dump]
               set bNeedBusObj [mql print program $sProgram select doesneedcontext dump]
               set bDownload [mql print program $sProgram select downloadable dump]
               set sUser ""
               if {$sMxVersion >= 10.5} {
                  set sUser [mql print program $sProgram select user dump]
               }
               set lsProgram [split [mql print program $sProgram] \n]
      
               set bPooled FALSE
               set bPiped FALSE
      
               foreach sProg $lsProgram {
                  set sProg [string trim $sProg]
                  if {[string first "code" $sProg] == 0} {
                     break
                  } elseif {$sProg == "pooled"} {
                     set bPooled TRUE
                  } elseif {$sProg == "pipe"} {
                     set bPiped TRUE
                  }
               }
      
               set bHidden [mql print program $sProgram select hidden dump]
               append sFile "$sName\t$sOrigName\t$sDescription\t$sProgType\t$sExecute\t$bNeedBusObj\t$bDownload\t$bPiped\t$bPooled\t$bHidden\t$sUser\n"
               regsub -all "/" $sProgram "SLASH" sProgramFile 
               regsub -all ":" $sProgramFile "COLON" sProgramFile
               regsub -all "\134\174" $sProgramFile "PYPE" sProgramFile
               regsub -all ">" $sProgramFile "GTHAN" sProgramFile
               regsub -all "<" $sProgramFile "LTHAN" sProgramFile
   
			   #KYB Fixed SCR-0000963 of JPO code containing '\\' character
			   #KYB Start Fixed SCR-0001799 JPO Extraction Issue
			   set bJPOExtraction [mql get env JPOEXTRACTION]
			   if { $bJPOExtraction == "TRUE" && $sProgType == "java" } {
					#append sProgramFile "_mxJPO.java"
					set sCode [string trim [mql print program $sProgram select code dump]]
					if { $sCode != "" || [string length $sCode] > 0 } {
						mql extract program $sProgram source "$sSourceFileDir"
					} else {
					mql print program $sProgram select code dump output "$sSourceFileDir/$sProgramFile"
					}
				} else {
					mql print program $sProgram select code dump output "$sSourceFileDir/$sProgramFile"
			   }      
			   #KYB End Fixed SCR-0001799 JPO Extraction Issue			   
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Program data loaded in file $sPath\nSource files loaded in directory $sSourceFileDir"
}
