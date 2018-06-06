#*******************************************************************************2014x
# @progdoc        emxSpinnerBusGrants.tcl vM2013 (Build 5.9.26)
#
# @Brief:         Add Bus Object Grants
#
# @Description:   Add Bus Object Grants
#                 Multiple headers for merging different types may be used using <HEADER> as last field.
#                 The file format has to be followed as shown below:
#
#                 Type(^t)Name(^t)Rev(^t)Grantor(^t)Grantee(^t)Read(^t)Modify(^t)Delete(^t)Checkout(^t)Checkin(^t)Schedule(^t)Lock(^t)Unlock(^t)Execute(^t)Freeze(^t)Thaw(^t)Create(^t)Revise(^t)Promote(^t)Demote(^t)Grant(^t)Enable(^t)Disable(^t)Override(^t)ChangeName(^t)ChangeType(^t)ChangeOwner(^t)ChangePolicy(^t)Revoke(^t)ChangeVault(^t)FromConnect(^t)ToConnect(^t)FromDisconnect(^t)ToDisconnect(^t)ViewForm(^t)Modifyform(^t)Show(^t)Signature(^t)Key
#                 Type(^t)Name(^t)Rev(^t)ChangeName(^t)ChangeRev(^t)Policy(^t)State(^t)Vault(^t)Owner(^t)description(^t)'Attribute 1'(^t)'Attribute N'(^t)[see note l](^t)[see note m](^t)[see note n](^t)[see note o]
#                    where (^t) is a tab
#
# @Parameters:    none
#
# @Returns:       Nothing   
#
# @Usage:         Can be used for legacy load into production
#
# @progdoc        Copyright (c) 2005, ENOVIA
#*******************************************************************************
# @Modifications:
#
# Matt Osterman 9/22/2005 - Initial Code
#
#*******************************************************************************

tcl;

eval {
   if {[info host] == "mostermant43" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
      set cmd "debugger_eval"
      set xxx [debugger_init]
   } else {
      set cmd "eval"
   }
}
$cmd {

   set sAllFiles [mql get env BUSGRANTLIST]

#  ********************** USER DEFINED VARIABLES*******************************
#  ****************************************************************************

   set sDate1 [clock format [clock seconds] -format %Y%m%d]
   set iStartTime  [clock seconds]
   set iErrTotal      0
   set bError     FALSE
   
# Scan Mode
   set bScan [mql get env SPINNERSCANMODE]
   if {$bScan != "TRUE"} {set bScan FALSE}
   if {$bScan} {set sDate1 "SCAN"}

   global iLogFileId 
   global iErrTot iAddTotal iDelTotal
   
##  this is to set the path
   if {$sAllFiles == ""} {set sAllFiles [ glob -nocomplain "./Objects/Grants/*.xls" ]}
   if {$sAllFiles == "./Objects/Grants/0"} {set sAllFiles ""}
   if {$sAllFiles == ""} {
      puts -nonewline "\n*** No data files of format 'grant_\[bus object name\].xls' found ***"
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
      global iErrTot iAddTotal iDelTotal sAllFiles sCurFile
      
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
      
      if {$iAddTotal > 0} {puts $iLogFileId "#     Number of Grants added:         $iAddTotal"}
      if {$iDelTotal > 0} {puts $iLogFileId "#     Number of Grants deleted:        $iDelTotal"}
      if {$iErrTot > 0} {puts $iLogFileId "#     Number of Errors:                  $iErrTot"}
      if {[expr $iAddTotal + $iDelTotal + $iErrTot] > 0} {puts $iLogFileId "#     Total time for load:               $iDay $iHour:$iMin:$iSec"}
      if {[lsearch $sAllFiles $sCurFile] == [expr [llength $sAllFiles] - 1]} {puts $iLogFileId ""}
   }

# Procedure to write screen cue
   proc pWriteCue {} {
      global iAddTotal iDelTotal iSkipTotal iErrTot iTenPercent iPrevAddModDel iPrevError
      set iAddModDel [expr $iAddTotal + $iDelTotal - $iPrevAddModDel]
      set iError [expr $iErrTot - $iPrevError]
      set iPrevAddModDel [expr $iAddTotal + $iDelTotal]
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
   mql verbose on
   mql trigger off
   file mkdir "./logs"
   if {[mql get env SPINNERLOGFILE] != ""} {
      set sLogFilePath [mql get env SPINNERLOGFILE]
      set sLogFileError [mql get env SPINNERERRORLOG]
      set bSpinnerAgent TRUE
   } else {    
      set sLogFilePath "./logs/BusGrants\.$sDate1.log"
      set bSpinnerAgent FALSE
   }
   set iLogFileId    [open $sLogFilePath a+]
   
   set lsAccessModes [ list read modify delete checkout checkin schedule lock \
        unlock execute freeze thaw create revise promote demote grant enable \
        disable override changename changetype changeowner changepolicy revoke \
        changevault fromconnect toconnect fromdisconnect todisconnect \
        viewform modifyform show ]
   set sHeader "Type\tName\tRevision\tGrantor\tGrantee\tRead\tModify\tDelete\tCheckout\tCheckin\tSchedule\tLock\tUnlock\tExecute\tFreeze\tThaw\tCreate\tRevise\tPromote\tDemote\tGrant\tEnable\tDisable\tOverride\tChangeName\tChangeType\tChangeOwner\tChangePolicy\tRevoke\tChangeVault\tFromConnect\tToConnect\tFromDisconnect\tToDisconnect\tViewForm\tModifyform\tShow\tSignature\tKey\n"

   foreach sCurFile $sAllFiles {
      set iFileId    [open $sCurFile r]
      puts $iLogFileId "\n# \[[clock format [clock seconds] -format %H:%M:%S]\] File: '[file tail $sCurFile]'"
      puts -nonewline "\nLoading bus objects from file '[file tail $sCurFile]'"
      
      set iErrTot        0
      set iAddTotal      0 
      set iDelTotal      0 
      set iHeader        0
      set iSkipTotal     0
      set iPrevAddModDel 0
      set iPrevError     0
      set bPercent   FALSE
      set iTenPercent    1

# READ FILES AND PROCESS RECORDS. 

      set lsFile [split [read $iFileId] \n]
      close $iFileId
      if {[llength $lsFile] > 50} {
         set iPercent [expr [llength $lsFile] / 10]
         set bPercent TRUE
      }
      foreach sLine $lsFile {
      	 set bAdd FALSE
      	 set bDel FALSE
      	 set bError FALSE
         set sLine [string trim $sLine]
         set lsLine [split $sLine \t]
         if {[string first "<HEADER>" $sLine] >= 0} {
            set iHeader 0
         }         
         if { $iHeader == 0 } {
            incr iHeader
         } elseif {$sLine != ""} {
            set sType           [ string trim [ lindex $lsLine 0 ] ]
            set sName           [ string trim [ lindex $lsLine 1 ] ]
            set sRev            [ string trim [ lindex $lsLine 2 ] ]
            set sGrantor        [ string trim [ lindex $lsLine 3 ] ]
            set sGrantee        [ string trim [ lindex $lsLine 4 ] ]
            set bSignature      [string toupper [ string trim [ lindex $lsLine 37 ] ] ]
            set sKey            [ string trim [ lindex $lsLine 38 ] ]
            
# flag for revoke if fields after T N R are blank and set variables
            set bRevoke TRUE
            set sAccess ""
            set lsAccess ""
            for {set i 3} {$i < 39} {incr i} {
               if {[lindex $lsLine $i] != ""} {
                  set bRevoke FALSE
               } 
               if {$i > 4 && $i < 38 && [string tolower [lindex $lsLine $i]] == "y"} {
               	  set j [expr $i - 5]
               	  lappend lsAccess [lindex $lsAccessModes $j]
               }
               set sAccess [join $lsAccess ,]
            }
            if {$bSignature != "TRUE"} {
               set bSignature "FALSE"
            }
# Check Existence of Bus Object
            if {$bScan != "TRUE"} {mql start transaction update}               
            set iErr  [ catch { mql print bus "$sType" "$sName" "$sRev" select exists dump } sResult ]   
            if {$iErr != 0 || $sResult != "TRUE"} {
               puts $iLogFileId "# ERROR: Bus Object \"$sType\" \"$sName\" \"$sRev\": does not exist"
               if {$bSpinnerAgent} {
                  set iLogFileErr [open $sLogFileError a+]
                  puts $iLogFileErr "ERROR: Bus Object \"$sType\" \"$sName\" \"$sRev\": does not exist"
                  close $iLogFileErr
               }
               set bError TRUE
            } else {
               if {[catch {
                  if {$bMatch("$sType|$sName|$sRev")} {
                     set bRevokeFirst FALSE
                  }
               } sMsg] != 0} {
                  set bMatch("$sType|$sName|$sRev") TRUE
                  set bRevokeFirst TRUE
               }
               if {$bRevoke || $bRevokeFirst} {
                  if {[mql print bus "$sType" "$sName" "$sRev" select granteeaccess dump |] != ""} {
                     set sCmd  "mql mod bus \"$sType\" \"$sName\" \"$sRev\" revoke all"
                     if {$bScan} {
                        puts $iLogFileId $sCmd
                     } elseif {[catch {eval $sCmd} sResult] != 0} {
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# ERROR: Bus Object \"$sType\" \"$sName\" \"$sRev\": $sResult"
                        if {$bSpinnerAgent} {
                           set iLogFileErr [open $sLogFileError a+]
                           puts $iLogFileErr "ERROR: Bus Object \"$sType\" \"$sName\" \"$sRev\": $sResult"
                           close $iLogFileErr
                        }
                        set bError TRUE
                     } else {
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult"
                        if {$bRevoke} {set bDel TRUE}
                     }
                  }
               }
               if {$bRevoke != "TRUE" && $bError != "TRUE"} {
# Check Grantor
# Changed the code fro list person to list user to fix the incident 339262 in line 239 and 250
                  if {[mql list user $sGrantor] == ""} {
                     puts $iLogFileId "# ERROR: Grantor '$sGrantor' on Bus Object \"$sType\" \"$sName\" \"$sRev\" not valid"
                     if {$bSpinnerAgent} {
                        set iLogFileErr [open $sLogFileError a+]
                        puts $iLogFileErr "ERROR: Grantor '$sGrantor' on Bus Object \"$sType\" \"$sName\" \"$sRev\" not valid"
                        close $iLogFileErr
                     }
                     set bError TRUE
                  }
# Check Grantee
                  if {$bError != "TRUE"} {
                     if {[mql list user $sGrantee] == ""} {
                        puts $iLogFileId "# ERROR: Grantee '$sGrantee' on Bus Object \"$sType\" \"$sName\" \"$sRev\" not valid"
                        if {$bSpinnerAgent} {
                           set iLogFileErr [open $sLogFileError a+]
                           puts $iLogFileErr "ERROR: Grantee '$sGrantee' on Bus Object \"$sType\" \"$sName\" \"$sRev\" not valid"
                           close $iLogFileErr
                        }
                        set bError TRUE
                     }
                  }
# Push Context to Grantor and Add Access
                  if {$bError != "TRUE"} {
                     mql push context user $sGrantor
                     set sCmd  "mql mod businessobject \"$sType\" \"$sName\" \"$sRev\" grant \"$sGrantee\" access \"$sAccess\" signature \"$bSignature\" key \"$sKey\""
                     if {$bScan} {
                        puts $iLogFileId $sCmd
                     } elseif {[catch {eval $sCmd} sResult] != 0} {
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# ERROR: \"$sType\" \"$sName\" \"$sRev\": $sResult"
                        if {$bSpinnerAgent} {
                           set iLogFileErr [open $sLogFileError a+]
                           puts $iLogFileErr "ERROR: \"$sType\" \"$sName\" \"$sRev\": $sResult"
                           close $iLogFileErr
                        }
                        set bError TRUE
                     } else {
                        set bAdd TRUE
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult"
                     }
                     mql pop context
                  }
               }
            }
# write cue
            if {$bError} {
               if {$bScan != "TRUE"} {mql abort transaction}
               if {$bPercent == "FALSE"} {puts -nonewline "!"}
               incr iErrTot
            } elseif {$bAdd} {
               if {$bScan != "TRUE"} {mql commit transaction}
               incr iAddTotal
               if {$bPercent == "FALSE"} {puts -nonewline "+"}
            } elseif {$bDel} {
               if {$bScan != "TRUE"} {mql commit transaction}
               if {$bPercent == "FALSE"} {puts -nonewline "-"}
               incr iDelTotal
            } else {
               if {$bScan != "TRUE"} {mql commit transaction}
               if {$bPercent == "FALSE"} {puts -nonewline "."}
               incr iSkipTotal
            }
            if {$bPercent && $iTenPercent < 10 && [expr $iAddTotal + $iDelTotal + $iSkipTotal + $iErrTot] > [expr $iTenPercent * $iPercent]} {
               pWriteCue
               incr iTenPercent
            }
         } 
         #  end of if
      } 
      ## end while for reading file
      if {$bPercent} {pWriteCue}
      pLogFile $iStartTime
      incr iErrTotal $iErrTot
   } 
   # end of forloop 
   close $iLogFileId
   mql set env BUSGRANTERROR $iErrTotal
   puts ""
} 
# end program

