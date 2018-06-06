#*******************************************************************************V6R2015x
# @progdoc        emxSpinnerBusObjects.tcl 4/20/2012 after vM10.10 (Build 15 Jan 10)
#
# @Brief:         Create, modify or delete bus objects
#
# @Description:   Create, modify or delete business objects
#                 Multiple headers for merging different types may be used using <HEADER> as last field.
#                 The file format has to be followed as shown below:
#
#                 Type(^t)Name(^t)Rev(^t)ChangeName(^t)ChangeRev(^t)Policy(^t)State(^t)Vault(^t)Owner(^t)description(^t)'Attribute 1'(^t)'Attribute N'(^t)[see note l](^t)[see note m](^t)[see note n](^t)[see note o]
#                    where (^t) is a tab
#
#                 For revisions chains, the 'Previous Type', 'Previous Name' and 'Previous Rev' columns are added:
#                 Type(^t)Name(^t)Rev(^t)Previous Type(^t)Previous Name(^t)Previous Rev(^t)ChangeName(^t)ChangeRev(^t)Policy(^t)State(^t)Vault(^t)Owner(^t)description(^t)'Attribute 1'(^t)'Attribute N'(^t)[see note l](^t)[see note m](^t)[see note n](^t)[see note o]
#                    where (^t) is a tab
#
#  a.	Runs the spreadsheets in ./Objects and creates a log file in ./logs
#  b.	This program will delete objects if only the T N R is specified or <DELETE> tag specified in Change Name field.
#  c.	Adds a new business object if one does not exist.
#  d.	Change name with the new name in the 4th column.
#  e.	Change revision with the revision value in the 5th column.
#  f.	Change policy with the policy name in the 6th column.
#  g.	Change state with the state name in the 7th column.  Objects can be promoted or demoted.
#  h.	Change vault with the vault name in the 8th column. Vault must exist.
#  i.	Change owner with the new person in the 9th column.
#  j.	Description is input in the 10th column.
#  k.	Attribute names are on the header line and the values are input in the columns below.  Use <NULL> for null values.
#  l.   The first field after attributes is set to 'TRUE' or 'FALSE' to override global setting to force modifications.
#  m.   The second field after attributes is set to 'ON' or 'OFF' to override global setting for create triggers.
#  n.   The third field after attributes is set to 'ON' or 'OFF' to override global setting for mod triggers.
#  o.   The fourth field after attributes is set to 'ON' or 'OFF' to override global setting for delete triggers.
#  p.   Use the '<NEWLINE>' tag to replace linefeeds in multiline attributes.
#
# @Parameters:    none
#
# @Returns:       Nothing   
#
# @Usage:         Can be used for legacy load into production
#
# @progdoc        Copyright (c) 2003, ENOVIA
#*******************************************************************************
# @Modifications:
#
# Charles Merinsky 4/22/2003 - Initial code.
# Venkatesh Harikrishnan - Modified for the new line issue.
# Matt Osterman 1/12/2007 - Added <DELETE> tag capability for delete.
# Pritam Mahajan 2/25/2009 - Fix for 370077.
# Mihai ILIE 6/02/2009 - Fix for 375907
# Thomas Ducray 4/20/2012 - Fix for previous revision: allow specifying Previous Type and Previous Name
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
   set bModIfExists [mql get env BUSOBJMODIFEXISTS]
   set bTriggerAdd [mql get env TRIGGERCREATE]
   set bTriggerMod [mql get env TRIGGERMODIFY]
   set bTriggerDel [mql get env TRIGGERDELETE]
   set sAllFiles [mql get env BUSFILELIST]
   set sGlobalConfigType [mql get env GLOBALCONFIGTYPE]
#  ********************** USER DEFINED VARIABLES*******************************
   if {$bModIfExists == ""} {
      set bModIfExists "FALSE" ;#TRUE or FALSE - modify bus object if it exists
   }
   if {$bTriggerAdd == ""} {
      set bTriggerAdd "OFF" ;# ON or OFF - turn trigger on
   }
   if {$bTriggerMod == ""} {
      set bTriggerMod "OFF" ;# ON or OFF - turn trigger on
   }
   if {$bTriggerDel == ""} {
      set bTriggerDel "OFF" ;# ON or OFF - turn trigger on
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

   global iLogFileId 
   global iErrTot iAddTotal iModTotal iDelTotal iChgNameTotal iPolicyTotal iOwnerTotal iVaultTotal iPromoteTotal iDemoteTotal iRevChainTotal
   
##  this is to set the path
   if {$sAllFiles == ""} {set sAllFiles [ glob -nocomplain "./Objects/*.xls" ]}
   if {$sAllFiles == "./Objects/0"} {set sAllFiles ""}
   if {$sAllFiles == ""} {
      puts -nonewline "\n*** No data files of format '\[bus object name\].xls' found ***"
   }
   
   
  proc pProcessMqlCmd { sAction sType sName sMql } {
    global sMsg_Log iAccessError bScan sLogFileError bSpinnerAgent bMQLExtract sMQLExtractFileDir bUpdateDBForMQLExtraction
    append sMsg_Log "# ACTION: $sAction $sType $sName\n"
	append sMsg_Log "$sMql\n"
	
	#QHV Start MQLExtract
	set bMQLExtract [mql get env SPINNEEXTRACTMQL]
	if { $bMQLExtract != "TRUE"} {
		set bMQLExtract "FALSE"
	}
	
	set bUpdateDBForMQLExtraction [mql get env UPDATEDBFORMQL]
	if {$bMQLExtract == "TRUE"} {
		set sMQLQueryPrint "$sMql"
			
		set sMQLQueryPrint [string map {\\\\ "" mql ""} $sMql]

		set sSpinnerDir [mql get env SPINNERPATH]
		
		set sMQLFile "$sSpinnerDir/MQLExtract/MQLQueryExtract.mql"
		if {![file exists $sMQLFile]} {
			set iLogFileQuery [open $sMQLFile w]
		} else { 
			set iLogFileQuery [open $sMQLFile a+]
		}
		set finalQuery ""
		append finalQuery "$sMQLQueryPrint ; \n"

		puts $iLogFileQuery $finalQuery
		close $iLogFileQuery
		set sMsg ""
	#QHV End MQLExtract
	} elseif {$bScan} {
        append sMsg_Log "$sMql\n"
        puts -nonewline ":"
        set sMsg ""
    } else {
	if {($bMQLExtract == "FALSE") || ($bMQLExtract == "TRUE" && $bUpdateDBForMQLExtraction == "TRUE")} {
    	#mql start transaction update
        if { [ catch { eval $sMql } sMsg ] != 0 } {
            set sErrMsg "$sAction $sType $sName NOT successful.\nCommand: $sMql\nError Reason is $sMsg\n"
            append sMsg_Log $sErrMsg
            #mql abort transaction
            puts -nonewline "!"
            if {$bSpinnerAgent} {
               set iLogFileErr [open $sLogFileError a+]
               puts $iLogFileErr $sErrMsg
               close $iLogFileErr
            }
            incr iAccessError
        } else {
            append sMsg_Log "# $sAction $sType $sName Successful.\n"
            puts -nonewline ":"
            #mql commit transaction
        }
	}
    }
    return $sMsg
}
#End pProcessMqlCmd
   
   
#***************************************************************************
# Procedure:   pLogFile
# Description: Used after all records have been processed. Write time of execution and 
#              Totals of load to files.
# Parameters:  none.
# Returns:  none.
#***************************************************************************
   proc pLogFile { iStartTime } {   
      global iLogFileId
      global iErrTot iAddTotal iModTotal iDelTotal iChgNameTotal iPolicyTotal iOwnerTotal iVaultTotal iPromoteTotal iDemoteTotal iRevChainTotal sAllFiles sCurFile
      
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
      
      if {$iAddTotal > 0} {puts $iLogFileId "#     Number of Objects created:         $iAddTotal"}
      if {$iModTotal > 0} {puts $iLogFileId "#     Number of Objects modified:        $iModTotal"}
      if {$iDelTotal > 0} {puts $iLogFileId "#     Number of Objects deleted:        $iDelTotal"}
      if {$iChgNameTotal > 0} {puts $iLogFileId "#     Number of Objects change name:     $iChgNameTotal"} 
      if {$iOwnerTotal > 0} {puts $iLogFileId "#     Number of Objects change owner:    $iOwnerTotal"}
      if {$iPolicyTotal > 0} {puts $iLogFileId "#     Number of Objects change policy:   $iPolicyTotal"}
      if {$iVaultTotal > 0} {puts $iLogFileId "#     Number of Objects change vault:    $iVaultTotal"}
      if {$iPromoteTotal > 0} {puts $iLogFileId "#     Number of Objects promoted:        $iPromoteTotal"} 
      if {$iDemoteTotal > 0} {puts $iLogFileId "#     Number of Objects demoted:         $iDemoteTotal"}
      if {$iRevChainTotal > 0} {puts $iLogFileId "#     Number of Objects revision chained:         $iRevChainTotal"}
      if {$iErrTot > 0} {puts $iLogFileId "#     Number of Errors:                  $iErrTot"}
      if {[expr $iAddTotal + $iModTotal + $iDelTotal + $iErrTot] > 0} {puts $iLogFileId "#     Total time for load:               $iDay $iHour:$iMin:$iSec"}
      
      if {[lsearch $sAllFiles $sCurFile] == [expr [llength $sAllFiles] - 1]} {puts $iLogFileId ""}
   }
   
#***************************************************************************
# Procedure:   pCheckAttrHdr
# Description:   This procedure checks Attribute for a null value.  Only
#  valid Attribute names with attribute values will be returned.  
# Returns:  Attributes.
#***************************************************************************
   proc pCheckAttrHdr { llName llValue } {
      set pCombo ""
      if {$llName != "" && $llValue != "" } {
         foreach lName $llName lValue $llValue {
		
			if {[string tolower $lName] != "override settings" && [string tolower $lName] != "create trigger" && [string tolower $lName] != "modify trigger" && [string tolower $lName] != "delete trigger"} {
				if {[string range $lValue 0 0] == "\042" && [string range $lValue end end] == "\042" && [string length $lValue] > 2} {
					 if {[string first "," $lValue] > -1 || [string first "\042\042" $lValue] > -1} {
						set iLast [expr [string length $lValue] -2]
						set lValue [string range $lValue 1 $iLast]
						regsub -all "\042\042" $lValue "\042" lValue
					 }
				}
				regsub -all "\134$" $lValue "\134\134\$" lValue
				regsub -all "\134{" $lValue "\134\134\173" lValue
				regsub -all "\134}" $lValue "\134\134\175" lValue
				regsub -all "\134\133" $lValue "\134\134\133" lValue
				regsub -all "\134\135" $lValue "\134\134\135" lValue
				regsub -all "\042" $lValue "\134\042" lValue
				regsub -all "'" $lValue "\047" lValue
				regsub -all "<ESC>" $lValue "\134" lValue
				regsub -all "<SPACE>" $lValue " " lValue
				regsub -all "<NULL>" $lValue "" lValue
				append pCombo " \"$lName\" \"$lValue\""
			}
         }
      }
      return $pCombo
   }
   
#***************************************************************************
# @procdoc        pGetBasic
# @Brief:         Copy from mxBus to get policy states.
# @Description:   Copy from mxBus to get policy states.
# @Returns:       
#**************************************************************************
   proc pGetBasic { sTypeOrOid sName sRev args } {
      if { $args == "" } { return "" }
      if { $sName == "" } {
         set sOid $sTypeOrOid
      } else {
         set sOid [ list $sTypeOrOid $sName $sRev ]
      }
      set sValues ""
      if { [ llength $args ] == 1 } {
         set sValues [ eval mql print bus $sOid select $args dump ]
      } else {
         set sValues [ eval mql print bus $sOid select $args dump ^ ]
      }
      if { [ llength $args ] == 1 } {
         return $sValues
      } else {
         return [ split $sValues ^ ]
      }
   }
   
#***************************************************************************
# @procdoc        pPromoteToTargetState
# @Brief:         Promote to a target state. Stolen from mxBus.
# @Description:   Promote to a target state. Stolen from mxBus.
# @Returns:       0 == good  1 == error
#**************************************************************************
   proc pPromoteToTargetState {lBus sTargetSt} {   
      global iLogFileId sLogFileError iPromoteTotal iDemoteTotal iErrTot bError bSpinnerAgent iUniqueID lsTempPolicy
      
      set iRet 0
      set lData [ eval pGetBasic $lBus policy current type id ]
      set sBusPol [ lindex $lData 0 ]
      set sBusSt  [ lindex $lData 1 ]
      set sBusType [ lindex $lData 2 ]
      set sBusOid [ lindex $lData 3 ]
      
      if {[catch {set lPolStates [ split [ mql print pol "$sBusPol" select state.name dump | ] | ]} sMsg] != 0} {
         puts $iLogFileId "\n$lBus: error - cannot promote business object as it does not exist"
         if {$bSpinnerAgent} {
            set iLogFileErr [open $sLogFileError a+]
            puts $iLogFileErr "$lBus: error - cannot promote business object as it does not exist"
            close $iLogFileErr
         }
         set bError TRUE
         incr iErrTot
         set iRet 1
      } else {
         set iBusSt  [ lsearch $lPolStates $sBusSt ]
		 #KYB Start Modified for Overlay Mode
		 if {$sTargetSt == ""} {set sTargetSt $sBusSt}
		 #KYB End Modified for Overlay Mode
         set iSt [ lsearch $lPolStates $sTargetSt ]
   
         if { $iSt == -1 } {
            puts $iLogFileId "\n$lBus: error State \"$sTargetSt\" does not exist"
            if {$bSpinnerAgent} {
               set iLogFileErr [open $sLogFileError a+]
               puts $iLogFileErr "$lBus: error State \"$sTargetSt\" does not exist"
               close $iLogFileErr
            }
            set bError TRUE
            incr iErrTot
            set iRet 1
         } elseif {$iSt != $iBusSt} {
# 12/21/2006 - Set state with policy change instead of promote/demote - MJO
            if {[catch {
               set sTempPol "$sBusType\_$sTargetSt\_$iUniqueID"
               if {[lsearch $lsTempPolicy $sTempPol] < 0} {
                  set sCmd "mql add policy \"$sTempPol\" state \"$sTargetSt\" type \"$sBusType\" hidden"
				  
				  #pProcessMqlCmd Mod businessobject "" $sCmd
				  
# 08/07/2008 - Fix for 357699 - Multiple formats for CAD Objects
                  set slsFormat [mql print policy "$sBusPol" select format dump |]
                  if {$slsFormat != ""} {
                     set lsFormat [split $slsFormat |]
                     foreach sFormat $lsFormat {
                        append sCmd " format \"$sFormat\""
                     }
                  }
# End Fix 357699
                  eval $sCmd
                  lappend lsTempPolicy $sTempPol
               }
               mql mod bus $sBusOid policy "$sTempPol"
			   #set sCmd "mql mod bus $sBusOid policy \"$sTempPol\""
			   #pProcessMqlCmd Mod bus $sBusOid $sCmd
			   
               mql mod bus $sBusOid policy "$sBusPol"
			   #set sCmd "mql mod bus $sBusOid policy \"$sBusPol\""
			   #pProcessMqlCmd Mod bus $sBusOid $sCmd
			   
			   
            } sResult] != 0} {
               puts $iLogFileId "\n$lBus - error in changing bus object state: $sResult"
               if {$bSpinnerAgent} {
                  set iLogFileErr [open $sLogFileError a+]
                  puts $iLogFileErr "$lBus - error in changing bus object state: $sResult"
                  close $iLogFileErr
               }
               set bError TRUE
               incr iErrTot
               set iRet 1
            } else {
               puts $iLogFileId "# $lBus: changing bus object state - successful"
               if { [ expr $iSt - $iBusSt ] > 0 } {
                  incr iPromoteTotal
               } else {
                  incr iDemoteTotal
               }
               set iRet 0
            }
         }
      }
      return $iRet
   }
   
# Procedure to write screen cue
   proc pWriteCue {} {
      global iAddTotal iModTotal iDelTotal iSkipTotal iErrTot iTenPercent iPrevAddModDel iPrevError
      set iAddModDel [expr $iAddTotal + $iModTotal + $iDelTotal - $iPrevAddModDel]
      set iError [expr $iErrTot - $iPrevError]
      set iPrevAddModDel [expr $iAddTotal + $iModTotal + $iDelTotal]
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
      puts "\n   global setting to modify bus objects if they exist: $bModIfExists"
      puts -nonewline "   global setting for triggers: create - $bTriggerAdd; modify - $bTriggerMod; delete - $bTriggerDel"
   }
   mql verbose on
   mql trigger off
   file mkdir "./logs"
   if {[mql get env SPINNERLOGFILE] != ""} {
      set sLogFilePath [mql get env SPINNERLOGFILE]
      set sLogFileError [mql get env SPINNERERRORLOG]
      set bSpinnerAgent TRUE
   } else {    
      set sLogFilePath "./logs/BusObjects\.$sDate1.log"
      set bSpinnerAgent FALSE
   }
   set iLogFileId    [open $sLogFilePath a+]
   set iUniqueID [clock seconds]
   set lsTempPolicy {}
   
   foreach sCurFile $sAllFiles {
		if { [llength $sGlobalConfigType] > 0} {
			set GlobalConfigType ""
			foreach sConfigType $sGlobalConfigType {
				if {[string match "*$sConfigType*" $sCurFile]} {
					set GlobalConfigType $sConfigType
					break
				}
			}
			
		} else {set GlobalConfigType ""}
		
				
		if {[string match "*bo_eService Number Generator*" $sCurFile] || [string match "*bo_eService Object Generator*" $sCurFile] || [string match "*bo_eService Trigger Program Parameters*" $sCurFile] || [string match "*$GlobalConfigType*" $sCurFile]} {
		  
		set rowCount 1
		if {$GlobalConfigType != "" && [string match "*$GlobalConfigType*" $sCurFile]} {
			set fp [open $sCurFile r]
			set file_data [read $fp]
			close $fp
			#  Process data file
			set data [split $file_data "\n"]
				foreach line $data {
					incr rowCount
				}
		}
	  
		if {$rowCount >= 103 && $GlobalConfigType != "" } {
			puts "ERROR : More than 100 Objects are not allowed to Import on Type $GlobalConfigType"
			exit 1
			return
		}
	  
      set iFileId    [open $sCurFile r]
      puts $iLogFileId "\n# \[[clock format [clock seconds] -format %H:%M:%S]\] File: '[file tail $sCurFile]'"
      puts -nonewline "\nLoading bus objects from file '[file tail $sCurFile]'"
      
      set iErrTot        0
      set iAddTotal      0 
      set iModTotal      0 
      set iDelTotal      0 
      set iChgNameTotal  0
      set iPolicyTotal   0 
      set iOwnerTotal    0
      set iVaultTotal    0
      set iPromoteTotal  0
      set iDemoteTotal   0
      set iRevChainTotal 0
      set iHeader        0
      set iSkipTotal     0
      set iPrevAddModDel 0
      set iPrevError     0
      set bPercent   FALSE
      set iTenPercent    1
      set bTrigOver(1) OFF
      set bTrigOver(2) OFF
      set bTrigOver(3) OFF
      set bTrigOn FALSE
      set bRevision FALSE

# READ FILES AND PROCESS RECORDS.
      set lsFile [split [read $iFileId] \n]
      close $iFileId
      if {[llength $lsFile] > 50} {
         set iPercent [expr [llength $lsFile] / 10]
         set bPercent TRUE
      }
      set iAttrEnd end
      foreach sLine $lsFile {
         set sLine [string trim $sLine]
         set lsLine [split $sLine \t]
         if {[string first "<HEADER>" $sLine] >= 0} {
            set iHeader 0
            set iAttrEnd [expr [llength $lsLine] -2]
            set lsLine [lrange $lsLine 0 $iAttrEnd]
            set bRevision FALSE
         }         
         if { $iHeader == 0 } {
# 361693 (Revision Chain) - MJO - 10/9/2008            
			if {[string first "Previous Rev" $sLine] == 14} {
               set bRevision TRUE
            } else {set bRevision FALSE}
			
            if {$iAttrEnd == "end"} {set iAttrEnd [expr [llength $lsLine] -1]}
            if {$bRevision} {
               set llName     [ lrange  $lsLine 12 $iAttrEnd ]
            } else {
               set llName     [ lrange  $lsLine 9 $iAttrEnd ]
            }            
# End 361693
            incr iHeader
            set lsName ""
            foreach lName $llName {
               set lName [string trim $lName]
               if {[catch {
                  if {[string tolower $lName] != "description" && [string tolower $lName] != "override settings" && [string tolower $lName] != "create trigger" && [string tolower $lName] != "modify trigger" && [string tolower $lName] != "delete trigger" && [mql print attribute $lName select type dump] == "timestamp"} {
                     set aTimeStamp($lName) TRUE
                  } else {
                     set aTimeStamp($lName) FALSE
                  }
               } sMsg] != 0} {
               	  puts "\nERROR: Attribute name '$lName' is not a valid attribute or field order is incorrect."
                  close $iLogFileId
               	  exit 1
               	  return
               }
               lappend lsName $lName
            }
            set llName $lsName
         } elseif {$sLine != ""} {
			set sType       [ string trim [ lindex $lsLine 0 ] ]
			if {[string match "eService Number Generator" $sType] || [string match "eService Object Generator" $sType] || [string match "eService Trigger Program Parameters" $sType] || [string match "<<eService Number Generator>>" $sType] || [string match "<<eService Object Generator>>" $sType] || [string match "<<eService Trigger Program Parameters>>" $sType] || [string match $GlobalConfigType $sType]} {
			
            set sName       [ string trim [ lindex $lsLine 1 ] ]
            set sRev        [ string trim [ lindex $lsLine 2 ] ]
# 361693 (Revision Chain) - MJO - 10/9/2008
            if {$bRevision} {
               set sPrevType    [ string trim [ lindex $lsLine 3 ] ]
			   set sPrevName    [ string trim [ lindex $lsLine 4 ] ]
               set sPrevRev    [ string trim [ lindex $lsLine 5 ] ]
               set sChangeName [ string trim [ lindex $lsLine 6 ] ]
               set sChangeRev  [ string trim [ lindex $lsLine 7 ] ]
               set sPolicy     [ string trim [ lindex $lsLine 8 ] ]
               set sState      [ string trim [ lindex $lsLine 9 ] ]
               set sVault      [ string trim [ lindex $lsLine 10 ] ]
               set sOwner      [ string trim [ lindex $lsLine 11 ] ]
               set llValue     [ lrange  $lsLine 12 $iAttrEnd ]
            } else {
               set sChangeName [ string trim [ lindex $lsLine 3 ] ]
               set sChangeRev  [ string trim [ lindex $lsLine 4 ] ]
               set sPolicy     [ string trim [ lindex $lsLine 5 ] ]
               set sState      [ string trim [ lindex $lsLine 6 ] ]
               set sVault      [ string trim [ lindex $lsLine 7 ] ]
               set sOwner      [ string trim [ lindex $lsLine 8 ] ]
               set llValue     [ lrange  $lsLine 9 $iAttrEnd ]
            }
# End 361693
            set lsName ""
            set lsValue ""
            foreach lName $llName lValue $llValue {
               if {$lName != ""} {
                  set lValue [string trim $lValue]
                  if {$lValue != ""} {
                     lappend lsName $lName
                     lappend lsValue $lValue
                  }
               }
            }
            set llAttr [pCheckAttrHdr "$lsName" "$lsValue"]
            set bNonByPass [string toupper [ string trim [ lindex $lsLine [expr $iAttrEnd - 3] ] ] ]
            set bTrigOver(1) [string toupper [ string trim [ lindex $lsLine [expr $iAttrEnd - 2 ] ] ] ]
            set bTrigOver(2) [string toupper [ string trim [ lindex $lsLine [expr $iAttrEnd - 1] ] ] ]
            set bTrigOver(3) [string toupper [ string trim [ lindex $lsLine $iAttrEnd ] ] ]
            set sTrig(1) $bTriggerAdd
            set sTrig(2) $bTriggerMod
            set sTrig(3) $bTriggerDel
            for {set i 1} {$i < 4} {incr i} {
               if {$bTrigOver($i) == "ON" || $bTrigOver($i) == "OFF"} {set sTrig($i) $bTrigOver($i)}
            }
            
# flag for delete if fields after T N R are blank or Change Name field is <DELETE>
#KYB Start - Modified for Overlay mode
            set bDelete TRUE
            set bChgNR TRUE
            #if {[lindex $lsLine 3] == "<DELETE>"} {
             #  set bChgNR FALSE
            #} else {
             #  for {set i 3} {$i < [llength $lsLine]} {incr i} {
              #    if {[lindex $lsLine $i] != ""} {
               #      set bDelete FALSE
                #     break
                 # }
               #}
            #}
			
			set bTypeDelete FALSE			
			set iLenMinTwo [expr [string length $sType] -2]
			if {[string first "<<" $sType] >= 0 && [string first ">>" $sType] == $iLenMinTwo} {
				set bTypeDelete TRUE
				
				#Find actual Type name
				set sTypeStr $sType
				if { [string match "<<*>>" $sType] == 1 } {
					set startStr [split $sType <<]
					set endStr [lindex $startStr 2]
					set sTypeStr [split $endStr >>]
					set sType [lindex $sTypeStr 0]
				}
			}
			
			set bNameDelete FALSE			
			set iLenMinTwo [expr [string length $sName] -2]
			if {[string first "<<" $sName] >= 0 && [string first ">>" $sName] == $iLenMinTwo} {
				set bNameDelete TRUE
				
				#Find actual Object name
				set sObjStr $sName
				if { [string match "<<*>>" $sName] == 1 } {
					set startStr [split $sName <<]
					set endStr [lindex $startStr 2]
					set sObjStr [split $endStr >>]
					set sName [lindex $sObjStr 0]
				}
			}
			
			set bRevDelete FALSE			
			set iLenMinTwo [expr [string length $sRev] -2]
			if {[string first "<<" $sRev] >= 0 && [string first ">>" $sRev] == $iLenMinTwo} {
				set bRevDelete TRUE
				
				#Find actual Revision
				set sRevStr $sRev
				if { [string match "<<*>>" $sRev] == 1 } {
					set startStr [split $sRev <<]
					set endStr [lindex $startStr 2]
					set sRevStr [split $endStr >>]
					set sRev [lindex $sRevStr 0]
				}
			}
			
			if {$bTypeDelete == "TRUE" && $bNameDelete == "TRUE" && $bRevDelete == "TRUE"} {
				set bChgNR FALSE
			} else {
				set bDelete FALSE
			}
			
#KYB End - Modified for Overlay mode
			# puts "---------Befor mod Type sType :------'$sType'----------"
# set mql command for adding or modifying object
            set iErr  [ catch { mql print businessobject "$sType" "$sName" "$sRev" select exists dump } result1 ]   
            
            if { "$sName" != ""} {
               set bError FALSE
               set bMod FALSE
               set bAdd FALSE
               set bDel FALSE
               set bExists FALSE
               if {$bScan != "TRUE"} {mql start transaction update}
               if {$result1 == "FALSE" && $bDelete == "FALSE"} {
                  if {$sName != $sChangeName && $sChangeName != ""} {set sName $sChangeName}
                  if {$sRev != $sChangeRev && $sChangeRev != ""} {set sRev $sChangeRev}
                  set iErr2  [ catch { mql print businessobject "$sType" "$sName" "$sRev" select exists dump } result2 ]
                  if {$result2 == "FALSE"} {  
	# Modified by Solution Library for the New Line Feeder error - Start
                     regsub -all "<LINEFEED>" $llAttr "\\\n" llAttr
                     regsub -all "<RETURN>" $llAttr "\\\n" llAttr
					 regsub -all "<NEWLINE>" $llAttr "\\\n" llAttr
	# Modified by Solution Library for the New Line Feeder error - End
	
	             # Modified below for fixing 370077 on 25 Feb 09 . Added word escape. - start
                     set sCmd  "mql escape add businessobject \"$sType\" \"$sName\" \"$sRev\" policy \"$sPolicy\" vault \"$sVault\" owner \"$sOwner\" $llAttr"
     	             # Modified above for fixing 370077 on 25 Feb 09 . Added word escape. - end
					 
					 set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
									pProcessMqlCmd Mod businessobject "" $sCmd
							} else { 
					 
					 
                     set bTrigOn FALSE
                     if {$bTrigOver(1) == "ON" || ($bTriggerAdd == "ON" && $bTrigOver(1) != "OFF")} {
                        mql trigger on
                        set bTrigOn TRUE
                     }
                     if {$bScan} {
                        puts $iLogFileId $sCmd
                     } elseif {[catch {eval $sCmd} sResult] != 0} {
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult - triggers $sTrig(1)"
                        if {$bSpinnerAgent} {
                           set iLogFileErr [open $sLogFileError a+]
                           puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\": $sResult - triggers $sTrig(1)"
                           close $iLogFileErr
                        }
                        set bError TRUE
                     }  else {
                        set bAdd TRUE
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult - triggers $sTrig(1)"
                        set lBus "\"$sType\" \"$sName\" \"$sRev\""
#start IMB:: June 02 2009 - Incident 375907: Replaced the line above with the next two lines
#the eval it's needed for the name containing spaces like:SAL-EN3767-030\[008+010\] UD  TP
#for names without spaces SAL-EN3767-030\[008+010\] works either with eval or without eval
#set iErr  [ catch { mql print businessobject "$sType" "$sName" "$sRev" select current dump } sCurState ] 
                        set sPrintCmd "mql escape print businessobject \"$sType\" \"$sName\" \"$sRev\" select current dump"
                  			set iErr  [ catch { eval $sPrintCmd} sCurState ]
#If there is an error for the print command we need to put it in the log file
  			                if { $iErr != 0 } {
  				                 puts $iLogFileId "$sPrintCmd"
  				                 puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sCurState - triggers $sTrig(1)"
  				                 set bError TRUE
  			                } else {
				                   if { "$sCurState" == "$sState"} {
				                   } else {	
				                      set lBus "\"$sType\" \"$sName\" \"$sRev\""
				                      if {$bScan} {
				                         puts $iLogFileId "Promote '$lBus' to state '$sState'"
				                      } else {
				                         pPromoteToTargetState "$lBus" "$sState"
				                      }
				                   }
			                  }
#end IMB:: June 02 2009 - Incident 375907 
                     }
                     if {$bTrigOn} {mql trigger off}
					 }
                  } else {
                     set bExists TRUE
                  }
               } elseif {$result1} {
                  set bExists TRUE
               }
               
# add revision chain (361693 - MJO - 10/9/2008)
# allow defining previous type and previous name as well - previous name needs to be updated if the previous object's name is changed
               if {$bRevision && !$bError} {
                  if { "$sPrevType" == "" || "$sPrevName" == ""} {
                  } elseif { "$sRev" == "$sPrevRev" && "$sType" == "$sPrevType" && "$sName" == "$sPrevName" } {
                  } elseif { [mql print bus "$sType" "$sName" "$sRev" select previous dump] != "" } {
                  } elseif {[mql print bus "$sPrevType" "$sPrevName" "$sPrevRev" select exists dump] != "TRUE"} {
                  } else {
                     set sCurId [mql print businessobject "$sType" "$sName" "$sRev" select id dump]
                     set lsPrev [split [mql print businessobject "$sPrevType" "$sPrevName" "$sPrevRev" select id next dump |] |]   
                     set sPrevId       [ string trim [ lindex "$lsPrev" 0 ] ]
                     set sPrevNextRev  [ string trim [ lindex "$lsPrev" 1 ] ]
                     if {$sPrevNextRev != ""} {
                     } else {
                        set sCmd  "mql revise bus $sPrevId bus $sCurId"
						set bMQLExtract [mql get env SPINNEEXTRACTMQL]
						if { $bMQLExtract == "TRUE"} {
						pProcessMqlCmd Mod bus "" $sCmd
						} else {
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" - error in connecting revision chain: $sResult - triggers off"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" - error in connecting revision chain : $sResult - triggers off"
                              close $iLogFileErr
                           }
                           set bError TRUE
                        }  else {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": revision chain connected - triggers off"
                           set bMod TRUE
                           incr iRevChainTotal
                        }
						}
                     }
                  }
               }
# End 361693
               if {$bExists} {
                  set bChgName FALSE
                  set bChgRev FALSE
# Start 383175 : 01/15/2010 : ION : Changed logic for name and/or rev change
# modify name/rev
                  if {$bChgNR && ($sName != $sChangeName && $sChangeName != "") || ($sRev != $sChangeRev && $sChangeRev != "")} {
                     if {$sChangeName == ""} {set sChangeName $sName}
                     if {$sChangeRev == ""} {set sChangeRev $sRev}
                     set iErr3  [ catch { mql print businessobject "$sType" "$sChangeName" "$sChangeRev" select exists dump } result3 ]
                     if {$result3 == "FALSE"} {
                        set sCmd  "mql mod businessobject \"$sType\" \"$sName\" \"$sRev\" name \"$sChangeName\" revision \"$sChangeRev\""
						set bMQLExtract [mql get env SPINNEEXTRACTMQL]
						if { $bMQLExtract == "TRUE"} {
						pProcessMqlCmd Mod businessobject "" $sCmd
						} else {
                        set bTrigOn FALSE
                        if {$bTrigOver(2) == "ON" || ($bTriggerMod == "ON" && $bTrigOver(2) != "OFF")} {
                           mql trigger on
                           set bTrigOn TRUE
                        }
                        set bChgName TRUE
                        set bChgRev TRUE
                        set bNonByPass TRUE
						}
                     } else {
                        set bDelete TRUE
                     }
# End 383175
                     if {$bChgName || $bChgRev} {
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\": $sResult"
                              close $iLogFileErr
                           }
                           set bError TRUE
                        } else {
                           set bMod TRUE
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult"
                        }
                        if {$bTrigOn} {mql trigger off}
                     }
                  }
# delete bus object
                  if {$bDelete} {
                     set sCmd  "mql delete businessobject \"$sType\" \"$sName\" \"$sRev\""
					 set bMQLExtract [mql get env SPINNEEXTRACTMQL]
					 if { $bMQLExtract == "TRUE"} {
					 pProcessMqlCmd Mod businessobject "" $sCmd
					 } else {
                     set bTrigOn FALSE
                     if {$bTrigOver(3) == "ON" || ($bTriggerDel == "ON" && $bTrigOver(3) != "OFF")} {
                     	mql trigger on
                     	set bTrigOn TRUE
                     }
                     if {$bScan} {
                        puts $iLogFileId $sCmd
                     } elseif {[catch {eval $sCmd} sResult] != 0} {
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult - triggers $sTrig(3)"
                        if {$bSpinnerAgent} {
                           set iLogFileErr [open $sLogFileError a+]
                           puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\": $sResult - triggers $sTrig(3)"
                           close $iLogFileErr
                        }
                        set bError TRUE
                     } else {
                        set bDel TRUE
                        puts $iLogFileId "$sCmd"
                        puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": $sResult - triggers $sTrig(3)"
                     }
                     if {$bTrigOn} {mql trigger off}
					 }
                  }
                  
                  if {$bChgName} {
                     set sName $sChangeName
                  } elseif {$bChgNR && $bDelete && $sName != $sChangeName && $sChangeName != ""} {
                     set sName $sChangeName
                  }
                  if {$bChgRev} {
                     set sRev $sChangeRev
                  } elseif {$bChgNR && $bDelete && $sRev != $sChangeRev && $sChangeRev != ""} {
                     set sRev $sChangeRev
                  }
# modify other elements if setting allows                     
                  if {($bDel != "TRUE" ) && (($bModIfExists && $bNonByPass != "FALSE") || $bNonByPass == "TRUE")} {
                     set iErr  [ catch { mql print businessobject "$sType" "$sName" "$sRev" select policy vault owner id current dump "," } sChgMod ]   
                     set sChgMod1     [split "$sChgMod" "," ] 
                     set sCurPolicy   [ string trim [ lindex "$sChgMod1" 0 ] ]
                     set sCurVault    [ string trim [ lindex "$sChgMod1" 1 ] ]
                     set sCurOwner    [ string trim [ lindex "$sChgMod1" 2 ] ]
                     set sCurId       [ string trim [ lindex "$sChgMod1" 3 ] ]
                     set sCurState    [ string trim [ lindex "$sChgMod1" 4 ] ]
                     set bTrigOn FALSE
                     if {$bTrigOver(2) == "ON" || ($bTriggerMod == "ON" && $bTrigOver(2) != "OFF")} {
                     	  mql trigger on
                     	  set bTrigOn TRUE
                     }
# modify policy
                     if { "$sPolicy" == ""} {
                     } elseif { "$sPolicy" == "$sCurPolicy" } {
                     } else {
                        set sCmd  "mql modify businessobject $sCurId policy \"$sPolicy\""
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" - error in modifying policy: $sResult - triggers $sTrig(2)"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" - error in modifying policy: $sResult - triggers $sTrig(2)"
                              close $iLogFileErr
                           }
                           set bError TRUE
                        } else {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": policy changed - triggers $sTrig(2)"
                           set bMod TRUE
                           incr iPolicyTotal
                           set sCurState [mql print businessobject "$sType" "$sName" "$sRev" select current dump]
                        }
                     }
# modify vault
                     if { "$sVault" == ""} {
                     } elseif { "$sVault" == "$sCurVault" } {
                     } else {
                        set sCmd  "mql modify businessobject $sCurId vault \"$sVault\""
						set bMQLExtract [mql get env SPINNEEXTRACTMQL]
						if { $bMQLExtract == "TRUE"} {
						pProcessMqlCmd Mod businessobject "" $sCmd
						} else {
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" - error in change vault: $sResult - triggers $sTrig(2)"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" - error in change vault: $sResult - triggers $sTrig(2)"
                              close $iLogFileErr
                           }
                           set bError TRUE
                        } else {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": vault changed - triggers $sTrig(2)"
                           set bMod TRUE
                           incr iVaultTotal
                        }
					 }
                     }
# modify owner
                     if { "$sOwner" == ""} {
                     } elseif { "$sOwner" ==  "$sCurOwner" } {
                     } else {
                        set sCmd  "mql modify businessobject $sCurId owner \"$sOwner\""
						set bMQLExtract [mql get env SPINNEEXTRACTMQL]
						if { $bMQLExtract == "TRUE"} {
						pProcessMqlCmd Mod businessobject "" $sCmd
						} else {
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" - error in owner change: $sResult - triggers $sTrig(2)"
                           if {$bSpinnerAgent} {
                               set iLogFileErr [open $sLogFileError a+]
                               puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" - error in owner change: $sResult - triggers $sTrig(2)"
                               close $iLogFileErr
                           }
                           set bError TRUE
                        } else {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": owner changed - triggers $sTrig(2)"
                           set bMod TRUE
                           incr iOwnerTotal
                        }
						}
                     }
# modify attributes
                     if { "$llAttr" == ""} {
                     } elseif { $bDelete != TRUE } {
                     	regsub -all "<NEWLINE>" $llAttr "\\\n" llAttr
                     	regsub -all "<LINEFEED>" $llAttr "\\\n" llAttr
                     	regsub -all "<RETURN>" $llAttr "\\\n" llAttr
                     	regsub -all "<BACKSLASH>" $llAttr "\\\\\\\\\\\\" llAttr
# Modified below for fixing 370077 on 25 Feb 09 . Added word escape. - start
                     	set sCmd  "mql escape modify businessobject $sCurId $llAttr"
# Modified below for fixing 370077 on 25 Feb 09 . Added word escape. - end
						set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								pProcessMqlCmd Mod businessobject "" $sCmd
							} else {
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" - error in attributes modify: $sResult - triggers $sTrig(2)"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" - error in attributes modify: $sResult - triggers $sTrig(2)"
                              close $iLogFileErr
                           }
                           set bError TRUE
                        } else {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": attributes modified - triggers $sTrig(2)"
                           set bMod TRUE
                        }
						}
                     }
                     if {$bTrigOn} {mql trigger off}
# modify states
                     if { "$sCurState" == "$sState"} {
                     } else {	
                        set lBus "\"$sType\" \"$sName\" \"$sRev\""
                        if {$bScan} {
                           puts $iLogFileId "Change '$lBus' to state '$sState'"
                        } else {
                           pPromoteToTargetState "$lBus" "$sState"
                           set bMod TRUE
                        }
                     }
                  }
               }

# write cue
               if {$bError} {
               	  if {$bScan != "TRUE"} {mql abort transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline "!"}
                  incr iErrTot
               } elseif {$bMod || $bAdd} {
                  if {$bScan != "TRUE"} {mql commit transaction}
                  if {$bMod} {
                     incr iModTotal
                     if {$bPercent == "FALSE"} {puts -nonewline ":"}
                  } else {
                     incr iAddTotal
                     if {$bPercent == "FALSE"} {puts -nonewline "+"}
                  }
               } elseif {$bDel} {
                  if {$bScan != "TRUE"} {mql commit transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline "-"}
                  incr iDelTotal
               } else {
                  if {$bScan != "TRUE"} {mql commit transaction}
                  if {$bPercent == "FALSE"} {puts -nonewline "."}
                  incr iSkipTotal
               }
               if {$bPercent && $iTenPercent < 10 && [expr $iAddTotal + $iModTotal + $iDelTotal + $iSkipTotal + $iErrTot] > [expr $iTenPercent * $iPercent]} {
                  pWriteCue
                  incr iTenPercent
               }
            } 
            # name is blank
         } else {
			set bPercent "FALSE"
			puts "\nWarning : '$sType' Type is restricted for import."
			break
		}
		}
         #  end of if
      } 
      ## end while for reading file
      if {$bPercent} {pWriteCue}
      pLogFile $iStartTime
      incr iErrTotal $iErrTot
	} else {
		puts "Warning : '$sCurFile' File is restricted for import."
		}
   } 
   # end of forloop 
   close $iLogFileId
   mql set env BUSOBJERROR $iErrTotal
# Delete temporary policies for changing states
   foreach sTempPol $lsTempPolicy {
      catch {mql delete policy "$sTempPol"} sMsg
   }
   puts ""

} 
# end program

