#*******************************************************************************2014x
# @progdoc        emxSpinnerBusFiles.tcl vM2013 (Build 8.12.4)
#
# @Brief:         Checkin or delete bus object files
#
# @Description:   Create, modify or delete business objects
#                 The file format has to be followed as shown below:
#
#                 Type(^t)Name(^t)Rev(^t)Format(^t)File Path(^t)[see notes c & d](^t)[see note e]
#                    where (^t) is a tab
#
#  a.	Runs the spreadsheets in ./Objects/Files and creates a log file in ./logs
#  b.	Use one row per file path.
#  c.	This program will delete the file if <DELETE> is specified in column after File Path (6th column).
#  d.   The sixth field is set to 'ON' or 'OFF' to override global checkin trigger.
#  e.   The seventh field is used to replace files if set to 'REPLACE' (append is default).
#
# @Parameters:    none.
#
# @Returns:       Nothing   
#
# @Usage:         Can be used for legacy load into production.
#
# @progdoc        Copyright (c) 2003, ENOVIA
#*******************************************************************************
# @Modifications:
#
# FirstName LastName mm/dd/yyyy - Description
#
#*******************************************************************************

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

   set bTriggerChk [mql get env TRIGGERCHECKIN]
   set sAllFiles [mql get env FILELIST]

#  ********************** USER DEFINED VARIABLES*******************************
   if {$bTriggerChk == ""} {
      set bTriggerChk "OFF" ;# ON or OFF - turn trigger on
   }
#  ****************************************************************************

   set sDate1 [clock format [clock seconds] -format %Y%m%d]
   set iStartTime  [clock seconds]
   set iErrTotal      0
   set bError     FALSE
   
# Scan Mode
   set bScan [mql get env SPINNERSCANMODE]
   if {$bScan != "TRUE"} {set bScan FALSE}
   if {$bScan} {set sDate1 "SCAN"}

# CDM Awareness
   set bCDM [mql get env CDMAWARE]
   if {$bCDM != "TRUE"} {set bCDM FALSE}

   global iLogFileId 
   global iErrTot iModTotal iDelTotal iChgNameTotal iPolicyTotal iOwnerTotal iVaultTotal iPromoteTotal iDemoteTotal bCDM
   
##  this is to set the path
   if {$sAllFiles == ""} {set sAllFiles [ glob -nocomplain "./Objects/Files/*.xls" ]}
   if {$sAllFiles == "./Objects/Files/0"} {set sAllFiles ""}
   if {$sAllFiles == ""} {
      puts -nonewline "\n*** No data files found ***"
   }
   
#***************************************************************************
# Procedure:   pLogFile
# Description: Used after all records have been processed. Write time of execution and 
#              Totals of load to files.
# Parameters:  none.
# Returns:  none.
#***************************************************************************
   proc pLogFile { iStartTime } {
   
      global iLogFileId
      global iErrTot iDelTotal iModTotal iChgNameTotal iPolicyTotal iOwnerTotal iVaultTotal iPromoteTotal iDemoteTotal sAllFiles sCurFile
      
      set iEndTime  [clock seconds]
      incr iEndTime -$iStartTime
      set iMin  [expr $iEndTime / 60]
      set iSec  [expr $iEndTime - ($iMin * 60 )]
      set iHour [expr $iMin / 60 ]
      set iMin  [expr $iMin - ($iHour * 60 )]
      set iDay  [expr $iHour / 24 ]
      set iHour [expr $iHour - ($iDay * 24 )]
      set iSec  [format "%0.2i" $iSec]
      set iMin  [format "%0.2i" $iMin]
      
      if {$iModTotal > 0} {puts $iLogFileId "#     Number of Files checked-in:        $iModTotal"}
      if {$iDelTotal > 0} {puts $iLogFileId "#     Number of Files deleted:           $iDelTotal"}
      if {$iErrTot > 0} {puts $iLogFileId "#     Number of Errors:                  $iErrTot"}
      if {[expr $iDelTotal + $iModTotal + $iErrTot] > 0} {puts $iLogFileId "#     Total time for load:               $iDay $iHour:$iMin:$iSec"}
      
      if {[lsearch $sAllFiles $sCurFile] == [expr [llength $sAllFiles] - 1]} {puts $iLogFileId ""}
   }

# Procedure to write screen cue
   proc pWriteCue {} {
      global iDelTotal iModTotal iSkipTotal iErrTot iTenPercent iPrevAddModDel iPrevError
      set iAddModDel [expr $iDelTotal + $iModTotal - $iPrevAddModDel]
      set iError [expr $iErrTot - $iPrevError]
      set iPrevAddModDel [expr $iDelTotal + $iModTotal]
      set iPrevError $iErrTot
      set sWrite "..."
      if {$iAddModDel && $iError} {
         set sWrite "  \($iAddModDel\: $iError\!\)"
      } elseif {$iAddModDel} {
         set sWrite "  \($iAddModDel\:\)"
      } elseif {$iError} {
         set sWrite "  \($iError\!\)"
      }
      puts -nonewline "$sWrite[expr $iTenPercent * 10]%"
   }

#main

   if {$sAllFiles != ""} {
      puts "\n   global setting for triggers: checkin - $bTriggerChk"
      set sCDM "OFF"
      if {$bCDM} {set sCDM "ON"}
      puts "   global setting for CDM awareness - $sCDM"
   }
   mql verbose on
   mql trigger off
   file mkdir "./logs"
   if {[mql get env SPINNERLOGFILE] != ""} {
      set sLogFilePath [mql get env SPINNERLOGFILE]
      set sLogFileError [mql get env SPINNERERRORLOG]
      set bSpinnerAgent TRUE
   } else {    
      set sLogFilePath "./logs/BusFiles\.$sDate1.log"
      set bSpinnerAgent FALSE
   }
   set iLogFileId    [open $sLogFilePath a+]
   
   set lsCDMType {}
   if {[mql list relationship "Latest Version"] == ""} {
      set bCDM FALSE
   } elseif {$bCDM} {
      set lsFromType [split [mql print relationship "Latest Version" select fromtype dump |] |]
      foreach sFromType $lsFromType {
         set lslsFromType [split [mql print type $sFromType select derivative dump |] |]
         set lsCDMType [concat $lsCDMType $lslsFromType]
      }
      set lsCDMType [lsort -unique $lsCDMType]
   }
   
   foreach sCurFile $sAllFiles {
      set iFileId    [open $sCurFile r]
      puts $iLogFileId "\n# \[[clock format [clock seconds] -format %H:%M:%S]\] File: '[file tail $sCurFile]'"
      puts -nonewline "Checking in files specified in '[file tail $sCurFile]'"
      
      set iErrTot        0
      set iDelTotal      0 
      set iModTotal      0 
      set iHeader        0
      set iSkipTotal     0
      set iPrevAddModDel 0
      set iPrevError     0
      set bPercent   FALSE
      set iTenPercent    1
      set bTrigOver OFF
      set bTrigOn FALSE

# READ FILES AND PROCESS RECORDS. 

      set lsFile [split [read $iFileId] \n]
      close $iFileId
      if {[llength $lsFile] > 50} {
         set iPercent [expr [llength $lsFile] / 10]
         set bPercent TRUE
      }
      foreach sLine $lsFile {
         set sLine [string trim $sLine]
         set lsLine [split $sLine \t]
         if {[string first "<HEADER>" $sLine] >= 0} {
            set iHeader 0
            set lsLine [lrange $lsLine 0 [expr [llength $lsLine] -2]]
         }         
         if { $iHeader == 0 } {
            incr iHeader
         } elseif {$sLine != ""} {
            set sType     [ string trim [ lindex $lsLine 0 ] ]
            set sName     [ string trim [ lindex $lsLine 1 ] ]
            set sRev      [ string trim [ lindex $lsLine 2 ] ]
            set sFormat   [ string trim [ lindex $lsLine 3 ] ]
            set sChkPath  [ string trim [ lindex $lsLine 4 ] ]
            set bTrigOver [string toupper [ string trim [ lindex $lsLine 5 ] ] ]
            set sReplace  [string toupper [ string trim [ lindex $lsLine 6 ] ] ]
            set bCDMRow FALSE
            set sCDMAppend " - CDM awareness OFF"
            if {$bCDM} {
               if {[lsearch $lsCDMType $sType] >= 0} {
                  set bCDMRow TRUE
                  set sCDMAppend " - CDM awareness ON"
               }
            }
            regsub -all "\134\134" $sChkPath "/" sChkPath
            set bDelete FALSE
            if {$bTrigOver == "<DELETE>"} {set bDelete TRUE}
            set bReplace FALSE
            if {$sReplace == "REPLACE"} {set bReplace TRUE}
            set sTrig $bTriggerChk
            if {$bTrigOver == "ON" || $bTrigOver == "OFF"} {set sTrig $bTrigOver}
            
# basic checks
            if { [mql print businessobject "$sType" "$sName" "$sRev" select exists dump] == "FALSE" } {
               puts $iLogFileId "#Business Object \"$sType\" \"$sName\" \"$sRev\" does not exist"
               if {$bSpinnerAgent} {
                  set iLogFileErr [open $sLogFileError a+]
               	  puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" does not exist"
               	  close $iLogFileErr
               }
               if {$bPercent == "FALSE"} {puts -nonewline "!"}
               incr iErrTot
            } elseif {[file exists $sChkPath] == 0 && !$bDelete} {
               puts $iLogFileId "#File '$sChkPath' for BO \"$sType\" \"$sName\" \"$sRev\" does not exist or not accessible"
               if {$bSpinnerAgent} {
                  set iLogFileErr [open $sLogFileError a+]
               	  puts $iLogFileErr "File '$sChkPath' for BO \"$sType\" \"$sName\" \"$sRev\" does not exist or not accessible"
               	  close $iLogFileErr
               }
               if {$bPercent == "FALSE"} {puts -nonewline "!"}
               incr iErrTot
            } else {            	            	
               set bError FALSE
               set bMod FALSE
               set bDel FALSE
               set bExists FALSE
               set oID [mql print bus "$sType" "$sName" "$sRev" select id dump]
               set lsBOFile [split [mql print bus "$sType" "$sName" "$sRev" select format.file.name dump |] |]
               set sDirectory [file dirname $sChkPath]
# Begin Incident 361999 MJO 12/04/08
               if {[string last "/" $sDirectory] < [expr [string length $sDirectory] - 1]} {append sDirectory "/"}
# End Incident 361999
               set sFile [file tail $sChkPath]
               set sPolicy [mql print bus "$sType" "$sName" "$sRev" select policy dump]
               set sStore [mql print policy "$sPolicy" select store dump]
               if {[lsearch $lsBOFile $sFile] >= 0} {
               	  set bCheckedIn TRUE
               } else {
               	  set bCheckedIn FALSE
               }
               if {$bScan != "TRUE"} {mql start transaction update}
# delete bus object file
               if {$bDelete && $bCheckedIn} {
                  set sDelFormat [mql print bus "$sType" "$sName" "$sRev" select format.file\[$sFile\].format dump]
                  set sCmd  "mql delete bus \042$sType\042 \042$sName\042 \042$sRev\042 format \042$sDelFormat\042 file \042$sFile\042"
                  if {$bScan} {
                     puts $iLogFileId $sCmd
                  } elseif {[catch {eval $sCmd} sResult] != 0} {
                     puts $iLogFileId "$sCmd"
                     puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\" file \"$sFile\": $sResult"
                     if {$bSpinnerAgent} {
                        set iLogFileErr [open $sLogFileError a+]
                     	puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" file \"$sFile\": $sResult"
                     	close $iLogFileErr
                     }
                     set bError TRUE
                  } else {
                     set bDel TRUE
                     puts $iLogFileId "$sCmd"
                     puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\" file \"$sFile\": $sResult"
# CDM - Delete Version Objects
                     if {$bCDMRow} {
# Begin Incident 361999 MJO 12/04/08
                        set slsExpBus [lindex [split [mql expand bus "$sType" "$sName" "$sRev" rel "Latest Version" from select bus last where "attribute\[Title\] == '$sFile'" dump |] \n] 0]
# End Incident 361999
                        if {$slsExpBus != ""} {
                           set lsExpBus [split $slsExpBus |]
                           set sVerName [lindex $lsExpBus 4]
                           set iVersion [lindex $lsExpBus 6]
                           for {set i $iVersion} {$i >= 1} {incr i -1} {
                              if {[mql print bus "$sType" "$sVerName" $i select exists dump]} {
                                 set sCmd "mql delete bus \042$sType\042 \042$sVerName\042 \042$i\042"
                                 if {$bScan} {
                                    puts $iLogFileId $sCmd
                                 } elseif {[catch {eval $sCmd} sResult] != 0} {
                                    puts $iLogFileId "$sCmd"
                                    puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\" file \"$sFile\" - Delete Version Object: $sResult"
                                    if {$bSpinnerAgent} {
                                       set iLogFileErr [open $sLogFileError a+]
                                 	     puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" file \"$sFile\" - Delete Version Object: $sResult"
                                 	     close $iLogFileErr
                                    }
                                    set bError TRUE
                                 } else {
                                    set bDel TRUE
                                    puts $iLogFileId "$sCmd"
                                    puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\" file \"$sFile\" - Delete Version Object: $sResult"
                                 }
                              }
                           }
                        }
                     }
                  }
               } elseif {$bDelete != "TRUE"} {
# checkin file
                  if {$sFormat != ""} {
                     if {!$bCDMRow} {set sFormat " format '$sFormat'"}
                  } else {
                     if {$bCDMRow} {
                        set sFormat "generic"
                     } else {
                        set sFormat " format generic"
                     }
                  }
                  set sAppend " append"
                  if {$bReplace} {set sAppend ""}
# CDM Format 1/21/2007 MJO
                  if {$bCDMRow} {            
                     set sCmd "mql exec prog emxCommonDocument -method checkinBus $oID \042$sDirectory\042 \042$sFile\042 \042$sFormat\042 \042$sStore\042 false server 'File: $sFile checked into: $sType $sName $sRev'\;"
                  } else {
                     set sCmd "mql checkin bus \"$sType\" \"$sName\" \"$sRev\"$sFormat$sAppend \"$sChkPath\""
                  }
                  set bTrigOn FALSE
                  if {$bTrigOver == "ON" || ($bTriggerChk == "ON" && $bTrigOver != "OFF")} {
                  	mql trigger on
                  	set bTrigOn TRUE
                  }
                  if {$bScan} {
                     puts $iLogFileId $sCmd
                  } elseif {[catch {eval $sCmd} sResult] != 0} {
                     puts $iLogFileId "$sCmd"
                     puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" file \"$sChkPath\": $sResult - triggers $sTrig\$sCDMAppend"
                     if {$bSpinnerAgent} {
                        set iLogFileErr [open $sLogFileError a+]
			  puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" file \"$sChkPath\": $sResult - triggers $sTrig\$sCDMAppend"
                     	  close $iLogFileErr
                     }
                     set bError TRUE
                  } else {
                     puts $iLogFileId "$sCmd"
                     puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\" file '$sFile' checked in - triggers $sTrig\$sCDMAppend"
                     set bMod TRUE
                  }
                  if {$bTrigOn} {mql trigger off}
               }
# write cue
               if {$bError} {
               	  if {$bScan != "TRUE"} {mql abort transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline "!"}
                  incr iErrTot
               } elseif {$bMod} {
                  if {$bScan != "TRUE"} {mql commit transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline ":"}
                  incr iModTotal
               } elseif {$bDel} {
                  if {$bScan != "TRUE"} {mql commit transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline "-"}
                  incr iDelTotal
               } else {
                  if {$bScan != "TRUE"} {mql commit transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline "."}
                  incr iSkipTotal
               }
               if {$bPercent && $iTenPercent < 10 && [expr $iModTotal + $iDelTotal + $iSkipTotal + $iErrTot] > [expr $iTenPercent * $iPercent]} {
                  pWriteCue
                  incr iTenPercent
               } 
            }
         } 
      } 
      if {$bPercent} {pWriteCue}
      pLogFile $iStartTime
      incr iErrTotal $iErrTot
   } 
   close $iLogFileId
   mql set env BUSFILERROR $iErrTotal
   puts ""
} 

