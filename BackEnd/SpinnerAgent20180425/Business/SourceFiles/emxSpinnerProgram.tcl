
#########################################################################*2014x
#
# @progdoc      emxSpinnerProgram.tcl vMV6R2013 (Build 11.10.2)
#
# @Description: Procedures for running in Programs
#
# @Parameters:  Returns 0 if successful, 1 if not
#
# @Usage:       Utilized by emxSpinnerAgent.tcl
#
# @progdoc      Copyright (c) ENOVIA Inc. 2005
#
#########################################################################
#
# @Modifications: See SchemaAgent_ReadMe.htm
#
#########################################################################

# Procedure to analyze programs
proc pAnalyzeProgram {} {
   global aCol aDat bOverlay bAdd sCode sCodeActual sCodeTrim sMxVersion sSpinDir bJPO sProgramFile sProgramFilePlan bOut
   if {$bAdd != "TRUE" && $bOverlay && $aCol(3) == ""} {
   } elseif {[string tolower $aCol(3)] != "java" && [string tolower $aCol(3)] != "ekl" && [string tolower $aCol(3)] != "external"} {
      set aCol(3) mql
   } elseif {[string tolower $aCol(3)] != "ekl" && [string tolower $aCol(3)] != "external"} {
      set aCol(3) java
   } elseif {[string tolower $aCol(3)] != "ekl" || $sMxVersion < 2011.1} {
      set aCol(3) external
   }
   set aCol(4) [pCompareAttr $aCol(4) immediate deferred deferred true]
   set aCol(5) [pCompareAttr $aCol(5) !needsbusinessobject needsbusinessobject true true]
   set aCol(6) [pCompareAttr $aCol(6) !downloadable downloadable true true]
   set aCol(7) [pCompareAttr $aCol(7) !pipe pipe true true]
   set aCol(8) [pCompareAttr $aCol(8) !pooled pooled true true]
   #TDQ: fix for packaged jpos: in package directories, the / is necessary: it is hence removed as a special character
   set lsSpecChar [list ":" "<" ">" "\134\174"]
   #array set aSpecChar [list "/" SLASH ":" COLON "<" LTHAN ">" GTHAN "\134\174" PYPE]
   array set aSpecChar [list ":" COLON "<" LTHAN ">" GTHAN "\134\174" PYPE]
   set sProgramFile $aCol(0)
   foreach sSpecChar $lsSpecChar {
      regsub -all "$sSpecChar" $sProgramFile "$aSpecChar($sSpecChar)" sProgramFile
   }
   set bFail FALSE
   set bJPO FALSE
   if {[catch {set iSourceFile [open "$sSpinDir/Business/SourceFiles/$sProgramFile" r]} sMsg] != 0} {
   	set bFail TRUE
   	set sErrorMsg "Source file '$sSpinDir/Business/SourceFiles/$sProgramFile' is not present"
   	if {$aCol(3) == "java"} {
   	   set sProgramFilePlan [glob -nocomplain "$sSpinDir/Business/SourceFiles/$sProgramFile\_mxJPO*.java"]
	   
	   #KYB Start Fixed SCR-0001799 JPO Extraction Issue
	   if {$sProgramFilePlan == ""} {
	        set sFullDirPath "$sSpinDir/Business/SourceFiles"
			set lsProgramName [ split $sProgramFile . ]
			set iCnt [ llength $lsProgramName ]
			incr iCnt -1
			set sProgramName [ lindex $lsProgramName $iCnt ]
			for {set i 0} {$i < $iCnt} {incr i} {
				append sFullDirPath "/"
				append sFullDirPath [ lindex $lsProgramName $i ]
            }
			set sProgramFilePlan [glob -nocomplain "$sFullDirPath/$sProgramName\_mxJPO*.java"]
		}
	   #KYB End Fixed SCR-0001799 JPO Extraction Issue
	   
           if {$sProgramFilePlan == ""} {
              set sErrorMsg "Source file '$sProgramFile' or JPO '$sProgramFile\_mxJPO\133*\135.java' in directory '$sSpinDir/Business/SourceFiles' is not present"
           } elseif {[llength $sProgramFilePlan] > 1} {
              set sErrorMsg "Too many versions of source file '$sSpinDir/Business/SourceFiles/$sProgramFile\_mxJPO\133*\135.java' are present"
           } else {
              set bFail FALSE
              set bJPO TRUE
              set sProgramFilePlan [ lindex $sProgramFilePlan 0 ]
              set iSourceFile [open $sProgramFilePlan r]
           }
       }
   } elseif {$aCol(3) == "java" && [glob -nocomplain "$sSpinDir/Business/SourceFiles/$sProgramFile\_mxJPO*.java"] != ""} {
   	set sWarningMsg "WARNING: Both files '$sProgramFile' and '$sProgramFile\_mxJPO\133*\135.java' are present: '$sProgramFile\_mxJPO\133*\135.java' will be ignored."
        pWriteWarningMsg "\n$sWarningMsg"
   	set bJPO FALSE
   }
   if {$bFail} {
      pWriteErrorMsg "\nERROR: $sErrorMsg"
      exit 1
      return
   } else {
      set sCode [read $iSourceFile]
      if {$bJPO} {regsub -all "_mxJPO(\133a-z\135|\133A-Z\135|\1330-9\135)*" $sCode "_mxJPO" sCode}
      set sCodeTrim [string trim $sCode]
      close $iSourceFile
   }
   if {$bAdd != "TRUE"} {
      set aDat(3) ""
      set bProgType ""
      if {[catch {set bProgType [mql print program $aCol(0) select isjavaprogram dump]} sMsg] == 0} {
         if {$bProgType} {
            set aDat(3) java
         } elseif {[mql print program $aCol(0) select ismqlprogram dump] == "TRUE"} {
            set aDat(3) mql
         } elseif {$sMxVersion > 2011 && [mql print program $aCol(0) select iseklprogram dump] == "TRUE"} {
            set aDat(3) ekl
         } else {
            set aDat(3) external
         }
      }
      set aDat(4) [pPrintQuery "" execute "" ""]
      set bNeedBusObj [pPrintQuery "" doesneedcontext "" ""]
      set aDat(5) [pCompareAttr $bNeedBusObj !needsbusinessobject needsbusinessobject true false]
      set bDownload [pPrintQuery "" downloadable "" ""]
      set aDat(6) [pCompareAttr $bDownload !downloadable downloadable true false]
      set aDat(7) "!pipe"
      set aDat(8) "!pooled"
      set lsPrint [split [pQuery "" "print program \042$aCol(0)\042"] \n]
      foreach sPipePool $lsPrint {
         set sPipePool [string trim $sPipePool]
         if {[string first "code" $sPipePool] == 0} {
            break
         } elseif {$sPipePool == "pooled"} {
            set aDat(8) pooled
         } elseif {$sPipePool == "pipe"} {
            set aDat(7) pipe
         }
      }
      if {$sMxVersion >= 10.5} {set aDat(10) [pPrintQuery "" user "" ""]}
      set sCodeActual ""
      if {$bJPO} {
         if {[catch {set sJPOPath "$env(MATRIXHOME)\/java/custom"} sMsg] == 0} {
            regsub -all "\134\134" $sJPOPath "/" sJPOPath
            set sProgTime [clock seconds]
            if {[catch {mql extract program $aCol(0)} sMsg] == 0} {
               set lsProgramFileActual [glob -nocomplain "$sJPOPath/$sProgramFile\_mxJPO*.java"]
               foreach sProgramFileActual $lsProgramFileActual {if {[file mtime $sProgramFileActual] >= $sProgTime} {break}}
               if {$sProgramFileActual != ""} {
                  set iCodeActual [open $sProgramFileActual r]
                  set sCodeActual [string trim [read $iCodeActual]]
                  regsub -all "_mxJPO(\133a-z\135|\133A-Z\135|\1330-9\135)*" $sCodeActual "_mxJPO" sCodeActual
                  close $iCodeActual
                  catch {file delete $sProgramFileActual -force} sMsg
               }
            }
         }
      } else {
         set sCodeActual [pPrintQuery "" code "" ""]
      }
   }
   if {$bOverlay} {pOverlay [list 3 4 5 6 7 8]}
   if {$bOverlay && $sMxVersion >= 10.5} {pOverlay [list 10]}
}

# Procedure to process programs
proc pProcessProgram {} {
   global aCol aDat bAdd sCode sCodeActual sCodeTrim sSpinDir sHidden sHiddenActual sMxVersion bJPO sProgramFile sProgramFilePlan bAEF
   set bModProg FALSE
   #TDQ: fix for packaged jpos: only add/mod program if not mxJPO with / (otherwise add/mod program commands consider "path/path/progname" instead of "path.path.progname")
   if {![regexp "mxJPO(.)*.java" sProgramFilePlan] && [string first "/" $aCol(0)] < 0} {
   if {$bAdd} {
      pMqlCmd "add program \042$aCol(0)\042 $aCol(3) execute $aCol(4) $aCol(5) $aCol(6) $aCol(7) $aCol(8) $sHidden"
      if {$aCol(0) == "eServiceSchemaVariableMapping.tcl"} {set bAEF TRUE}
      if {$sMxVersion >= 10.5} {pMqlCmd "mod program \042$aCol(0)\042 execute user \042$aCol(10)\042"}
   } else {
      if {$aCol(3) != $aDat(3) || $aCol(4) != $aDat(4) || $aCol(5) != $aDat(5) || $aCol(6) != $aDat(6) || $aCol(7) != $aDat(7) || $aCol(8) != $aDat(8) || $sHidden != $sHiddenActual} {pMqlCmd "mod program \042$aCol(0)\042 $aCol(3) execute $aCol(4) $aCol(5) $aCol(6) $aCol(7) $aCol(8) $sHidden"}
      if {$sMxVersion >= 10.5 && $aCol(10) != $aDat(10)} {pMqlCmd "mod program \042$aCol(0)\042 execute user \042$aCol(10)\042"}
      if {[string first $sCodeActual $sCode] == -1 && $sCodeTrim != $sCodeActual} {set bModProg TRUE}
   }
   } else {
      #TDQ: make sure the element is skipped so that Spinner doesn't try to update the program description
      uplevel {set bSkipElement TRUE}
   }
   if {$bAdd || $bModProg} {
      if {$bJPO} {
         pMqlCmd "insert prog \042$sProgramFilePlan\042"
      } else {
         pMqlCmd "mod program \042$aCol(0)\042 file \042$sSpinDir/Business/SourceFiles/$sProgramFile\042"
      }
   }
   return 0
}
