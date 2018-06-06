#*******************************************************************************2015x
# @progdoc        emxSpinnerBusRels.tcl vM2013 (Build 9.7.24)
#
# @Description:   Add new relationships or modify/delete existing.  
#                 Multiple headers for merging different types may be used using <HEADER> as last field.
#                 The file format has to be followed as shown below:
#
#                 Rel Name(^t)Attr 1(^t)Attr N(^t)[see note f](^t)[see note g](^t)[see note h](^t)[see note i]
#                 b2b: FromType(^t)FromName(^t)FromRev(^t)ToType(^t)ToName(^t)ToRev(^t)
#                 b2r: FromType(^t)FromName(^t)FromRev(^t)RelName(^t)ToRel...(^t)ToType(^t)ToName(^t)ToRev(^t)
#                    where (^t) is a tab
#
#  a.	Runs the spreadsheets in ./Relationships named '*.xls'
#  b.	Creates a log file in ./logs  
#  c.	This will connect relationships and also modify or delete existing relationships.
#  d.	Attribute values may also be added or modified on the relationship.  Use <NULL> for null values.
#  e.   To delete rel's, enter <DELETE> in column 9 for Attr 1.  This will remove ALL rel's of specified rel name.
#  f.   The first field after attributes is set to 'TRUE' or 'FALSE' to override global setting to force modifications.
#  g.   The second field after attributes is set to 'ON' or 'OFF' to override global setting for create triggers.
#  h.   The third field after attributes is set to 'ON' or 'OFF' to override global setting for mod triggers.
#  i.   The fourth field after attributes is set to 'ON' or 'OFF' to override global setting for delete triggers.
#  j.   Relationship details are expected to appear after delete trigger column
#
# @Parameters:    none
#
# @Usage:         Intended for loading administrative data for AEF schema.
#
# @progdoc        Copyright (c) 2003, ENOVIA
#
#*******************************************************************************
# @Modifications:
#
# Charles Merinsky 4/22/2003 - initial code.
# Balaji Sutti 4/3/2006 - IR Fix 315205.
# Venkatesh Harikrishnan - Modified for the new line issue.
# Lohit Bijakal 07/10/2009 - Connections procedure (pGet_BOAdminRel) will use recursive procedures 
#                            (pGet_BOAdminConstructRels/pGet_BOAdminConstructTypes) to fix issues with complex b2r, r2b & r2r structures.
# ION 7/24/2009 - Simplified logic to reduce errant messages
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

   set bModIfExists [mql get env BUSRELMODIFEXISTS]
   set bTriggerAdd [mql get env TRIGGERCREATE]
   set bTriggerMod [mql get env TRIGGERMODIFY]
   set bTriggerDel [mql get env TRIGGERDELETE]
   set sAllFiles [mql get env RELFILELIST]

#  ********************** USER DEFINED VARIABLES*******************************
   if {$bModIfExists == ""} {
      set bModIfExists "TRUE" ;#TRUE or FALSE - modify bus object if it exists
   } elseif {$bModIfExists != "TRUE"} {
      set bModIfExists FALSE
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
   set iErrTot 0
   set iErrTotal 0

# Scan Mode
   set bScan [mql get env SPINNERSCANMODE]
   if {$bScan != "TRUE"} {set bScan FALSE}
   if {$bScan} {set sDate1 "SCAN"}

   global iLogFileId 
   global iErrTot iAddTotal iModTotal iDelTotal
   if {$sAllFiles == ""} {set sAllFiles [glob -nocomplain "./Relationships/*.xls"]}  
   if {$sAllFiles == "./Relationships/0"} {set sAllFiles ""}
   if {$sAllFiles == ""} {
      puts -nonewline "\n*** No data files of format '*.xls' found ***"
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
      global iErrTot iAddTotal iModTotal iDelTotal iChgNameTotal iPolicyTotal iOwnerTotal iVaultTotal sAllFiles sCurFile
      
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
      
      if {$iAddTotal > 0} {puts $iLogFileId "#     Number of Rels created:   $iAddTotal"}
      if {$iModTotal > 0} {puts $iLogFileId "#     Number of Rels modified:  $iModTotal"} 
      if {$iDelTotal > 0} {puts $iLogFileId "#     Number of Rels deleted:   $iDelTotal"} 
      if {$iErrTot > 0} {puts $iLogFileId   "#     Number of Errors:         $iErrTot"}
      if {[expr $iAddTotal + $iModTotal + $iDelTotal + $iErrTot] > 0} {puts $iLogFileId "#     Total time for load:               $iDay $iHour:$iMin:$iSec"}
      if {[lsearch $sAllFiles $sCurFile] == [expr [llength $sAllFiles] - 1]} {puts $iLogFileId ""}
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
# Procedure:   pCheckAttrHdr
# Description:   This procedure checks Attribute for a null value.  Only
#  valid Attribute names with attribute values will be returned.  
# Returns:  Attributes.
#***************************************************************************
   proc pCheckAttrHdr { llName llValue } {
      set pCombo ""
      if {$llName != "" && $llValue != "" } {
         foreach lName $llName lValue $llValue {
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
      return $pCombo
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

# Main
   if {$sAllFiles != ""} {
      puts -nonewline "\n   global setting to modify rels if they exist: $bModIfExists"
      puts -nonewline "\n   global setting for triggers: create - $bTriggerAdd; modify - $bTriggerMod; delete - $bTriggerDel"
   }
   mql verbose on
   mql trigger off
   file mkdir "./logs"
   if {[mql get env SPINNERLOGFILE] != ""} {
      set sLogFilePath [mql get env SPINNERLOGFILE]
      set sLogFileError [mql get env SPINNERERRORLOG]
      set iLogFileErr [open $sLogFileError a+]
      set bSpinnerAgent TRUE
   } else {    
      set sLogFilePath "./logs/BusRels\.$sDate1.log"
      set bSpinnerAgent FALSE
   }
   set iLogFileId    [open $sLogFilePath a+]

   foreach sCurFile $sAllFiles {
    # puts "-------------------'$sCurFile'-------------------"
    if {[string match "*eService Trigger Program Parameters*" $sCurFile] || [string match "*eService Object Generator*" $sCurFile] || [string match "*eService Number Generator*" $sCurFile]} {
   # puts "-------------------'$sCurFile'-------------------"
      set sRelType "rel"
      if {[string first "rel-b2b_" $sCurFile] >= 0} {
         set sRelType "b2b"
      } elseif {[string first "rel-b2r_" $sCurFile] >= 0} {
         set sRelType "b2r"
      } elseif {[string first "rel-r2b_" $sCurFile] >= 0} {
         set sRelType "r2b"
      } elseif {[string first "rel-r2r_" $sCurFile] >= 0} {
         set sRelType "r2r"
      }  
      set iFileId    [open $sCurFile r]
      puts $iLogFileId "\n# \[[clock format [clock seconds] -format %H:%M:%S]\] File '[file tail $sCurFile]'"
      puts -nonewline "\nConnecting relationships from file '[file tail $sCurFile]'"

      set iAddTotal      0 
      set iModTotal      0 
      set iDelTotal      0 
      set iErrTot        0
      set iSkipTotal     0
      set iHeader        0
      set iPrevAddModDel 0
      set iPrevError     0
      set bPercent   FALSE
      set iTenPercent    1
      set bTrigOver(1) OFF
      set bTrigOver(2) OFF
      set bTrigOver(3) OFF
      set bTrigOn FALSE

# READ FILES AND PROCESS RECORDS. 

      set lsFile [split [read $iFileId] \n]
      close $iFileId
      if {[llength $lsFile] > 50} {
         set iPercent [expr [llength $lsFile] / 10]
         set bPercent TRUE
      }
      set iAttrEnd end
      set iSettings end
      foreach sLine $lsFile {
         set bRepeat TRUE
         while {$bRepeat} {
            set bRepeat FALSE
            set bDeleteFirst FALSE
            set sLine [string trim $sLine]
            set lsLine [split $sLine \t]
            if {[string first "<HEADER>" $sLine] >= 0} {
               set iHeader 0
               set iAttrEnd [expr [llength $lsLine] -2]
               set lsLine [lrange $lsLine 0 $iAttrEnd]
            }
            if { $iHeader == 0 } {
               set iAttrIndex 1
               set iSettings [lsearch $lsLine "Override Settings"]
               switch $sRelType {
                  rel {
                     set iAttrIndex 8
                     if {$iAttrEnd == "end"} {set iAttrEnd [expr [llength $lsLine] -1]}
                  } b2b {
                     set iAttrEnd [expr [lsearch $lsLine "from.type"] - 1 ]
                  } b2r {
                     set iAttrEnd [expr [lsearch $lsLine "from.type"] - 1 ]
                     set lToRelExpr [ lrange $lsLine [ expr $iAttrEnd + 5 ] [expr $iSettings - 1 ] ]
                  } r2b {
                     set iAttrEnd [expr [lsearch $lsLine "fromrel"] - 1 ]
                     set iToType  [lsearch $lsLine "to.type"]
                     set lFromRelExpr [ lrange $lsLine [ expr $iAttrEnd + 2 ] [ expr $iToType - 1 ] ]
                  } r2r {
                     set iAttrEnd [expr [lsearch $lsLine "fromrel"] - 1 ]
                     set iToRelIndex  [lsearch $lsLine "torel"]
                     set lFromRelExpr [ lrange $lsLine [ expr $iAttrEnd + 2 ] [ expr $iToRelIndex - 1 ] ]
                     set lToRelExpr   [ lrange $lsLine [ expr $iToRelIndex + 1 ] [ expr $iSettings - 1 ] ]
                  }
               }
               set llName [ lrange  $lsLine $iAttrIndex $iAttrEnd ]
               incr iHeader
               set lsName ""
               foreach lName $llName {
                  if {$lName == ""} { continue }
                  set lName [string trim $lName]
                  if {[catch {
                     if {[mql print attribute $lName select type dump] == "timestamp"} {
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
               set bDelete FALSE
               #set sDelete [ string trim [ lindex $lsLine $iAttrIndex ] ]
			   set sDelete ""
			   set sRelName [ string trim [ lindex $lsLine [expr $iAttrIndex -1] ] ]
			   set iLenMinTwo [expr [string length $sRelName] -2]
			   if {[string first "<<" $sRelName] >= 0 && [string first ">>" $sRelName] == $iLenMinTwo} { 
					set sDelete "<DELETE>" 
					set sRelStr $sRelName
					if { [string match "<<*>>" $sRelName] == 1 } {
						set startStr [split $sRelName <<]
						set endStr [lindex $startStr 2]
						set sRelStr [split $endStr >>]
						set sRelName [lindex $sRelStr 0]
						set lsLine [lreplace $lsLine [expr $iAttrIndex -1] [expr $iAttrIndex -1] $sRelName]
					}
				}
               set sToDir "to"
               switch $sRelType {
                  rel {
                     set sFromType [ string trim [ lindex $lsLine 0 ] ]
                     set sFromName [ string trim [ lindex $lsLine 1 ] ]
                     set sFromRev  [ string trim [ lindex $lsLine 2 ] ]
                     set sToType   [ string trim [ lindex $lsLine 3 ] ]
                     set sToName   [ string trim [ lindex $lsLine 4 ] ]
                     set sToRev    [ string trim [ lindex $lsLine 5 ] ]
                     set sToDir    [ string tolower [ string trim [ lindex $lsLine 6 ] ] ]
                     set sRel      [ string trim [ lindex $lsLine 7 ] ]
                     set sFromLabel "bus object '$sFromType' '$sFromName' '$sFromRev'"
                     set sToLabel "bus object '$sToType' '$sToName' '$sToRev'"
                  } b2b {
                     set sRel      [ string trim [ lindex $lsLine 0 ] ]
                     set sFromType [ string trim [ lindex $lsLine [ expr $iAttrEnd + 1 ] ] ]
                     set sFromName [ string trim [ lindex $lsLine [ expr $iAttrEnd + 2 ] ] ]
                     set sFromRev  [ string trim [ lindex $lsLine [ expr $iAttrEnd + 3 ] ] ]
                     set sToType   [ string trim [ lindex $lsLine [ expr $iAttrEnd + 4 ] ] ]
                     set sToName   [ string trim [ lindex $lsLine [ expr $iAttrEnd + 5 ] ] ]
                     set sToRev    [ string trim [ lindex $lsLine [ expr $iAttrEnd + 6 ] ] ]
                     set sFromLabel "bus object '$sFromType' '$sFromName' '$sFromRev'"
                     set sToLabel "bus object '$sToType' '$sToName' '$sToRev'"
                  } b2r {
                     set sRel      [ string trim [ lindex $lsLine 0 ] ]
                     set sFromType [ string trim [ lindex $lsLine [ expr $iAttrEnd + 1 ] ] ]
                     set sFromName [ string trim [ lindex $lsLine [ expr $iAttrEnd + 2 ] ] ]
                     set sFromRev  [ string trim [ lindex $lsLine [ expr $iAttrEnd + 3 ] ] ]
                     set sToRel    [ string trim [ lindex $lsLine [ expr $iAttrEnd + 4 ] ] ]
                     set lToRel    [ lrange $lsLine [ expr $iAttrEnd + 5 ] [expr $iSettings - 1 ] ]
                     set sFromLabel "bus object '$sFromType' '$sFromName' '$sFromRev'"
                     set sToLabel "connection '$sToRel'"
                  } r2b {
                     set sRel     [ string trim [ lindex $lsLine 0 ] ]
                     set sFromRel [ string trim [ lindex $lsLine [ expr $iAttrEnd + 1 ] ] ]
                     set lFromRel [ lrange $lsLine [ expr $iAttrEnd + 2 ] [ expr $iToType - 1 ] ]
                     set sToType  [ string trim [ lindex $lsLine $iToType ] ]
                     set sToName  [ string trim [ lindex $lsLine [ expr $iToType + 1 ] ] ]
                     set sToRev   [ string trim [ lindex $lsLine [ expr $iToType + 2 ] ] ]
                     set sFromLabel "connection '$sFromRel'"
                     set sToLabel "bus object '$sToType' '$sToName' '$sToRev'"
                  } r2r {
                     set sRel     [ string trim [ lindex $lsLine 0 ] ]
                     set sFromRel [ string trim [ lindex $lsLine [ expr $iAttrEnd + 1 ] ] ]
                     set lFromRel [ lrange $lsLine [ expr $iAttrEnd + 2 ] [ expr $iToRelIndex - 1 ] ]
                     set sToRel   [ string trim [ lindex $lsLine $iToRelIndex ] ]
                     set lToRel   [ lrange $lsLine [ expr $iToRelIndex + 1 ] [expr $iSettings - 1 ] ]
                     set sFromLabel "connection '$sFromRel'"
                     set sToLabel "connection '$sToRel'"
                  }
               }
               if {$sDelete == "<DELETE>"} {
                  set bDelete TRUE
                  set llAttr ""
               } else {
                  set llValue [ lrange  $lsLine $iAttrIndex $iAttrEnd ]
                  set lsName ""
                  set lsValue ""
                  foreach lName $llName lValue $llValue {
                     if {$lName != ""} {
                        set lValue [string trim $lValue]
                        if {$lValue != ""} {
                           if {$aTimeStamp($lName)} {set lValue [lindex [split [string trim $lValue] " "] 0]}
                           lappend lsName $lName
                           lappend lsValue $lValue
                        }
                     }
                  }
                  set llAttr [pCheckAttrHdr "$lsName" "$lsValue"]
               }

               set bNonByPass   [string toupper [ string trim [ lindex $lsLine [expr $iSettings ] ] ] ]
               set bTrigOver(1) [string toupper [ string trim [ lindex $lsLine [expr $iSettings + 1 ] ] ] ]
               set bTrigOver(2) [string toupper [ string trim [ lindex $lsLine [expr $iSettings + 2 ] ] ] ]
               set bTrigOver(3) [string toupper [ string trim [ lindex $lsLine [expr $iSettings + 3 ] ] ] ]

               set sTrig(1) $bTriggerAdd
               set sTrig(2) $bTriggerMod
               set sTrig(3) $bTriggerDel
               for {set i 1} {$i < 4} {incr i} {
                  if {$bTrigOver($i) == "ON" || $bTrigOver($i) == "OFF"} {set sTrig($i) $bTrigOver($i)}
               }
               set sFromDir "from"
               if {$sToDir == "from"} {set sFromDir "to"}
               set sFromCard ""
               set sToCard ""
               set sFromCard [mql print rel $sRel select "$sFromDir\cardinality" dump]
               set sToCard [mql print rel $sRel select "$sToDir\cardinality" dump]
               set sSkip "n"
               set iErr1 0
               set iErr2 0
               set bFromMultiRel FALSE
               set bToMultiRel FALSE

               if {$sRel != ""} {
# Check From Type/Rel and To Type/Rel existence
                
                  if {$sRelType == "rel" || $sRelType == "b2b" || $sRelType == "b2r"} {
                     set iErr1  [ catch { mql print businessobject "$sFromType" "$sFromName" "$sFromRev" select id dump } sFromId ]
                  } else {
# Construct from side where clause
                     set sWhereExpr ""
                     foreach sFromRelExpr $lFromRelExpr sFromRelEach $lFromRel {
                        if {$sWhereExpr != ""} {append sWhereExpr "&& "}
                        append sWhereExpr [string range $sFromRelExpr 8 end] "=='$sFromRelEach' "
                     }
                     set lsFromRelId [split [mql query connection type "$sFromRel" where $sWhereExpr select id dump |] \n]
                     if {[llength $lsFromRelId] == 0 || [llength $lsFromRelId] > 1 || [string first "Warning" "[lindex $lsFromRelId 0]"] >= 0} {
                        set iErr1 1
                     } else {
                        set sFromId [lindex [split [lindex $lsFromRelId 0] |] 1]
                        if {$sFromId == ""} {set iErr1 1}
                     }
                  }
                  if {$sRelType == "rel" || $sRelType == "b2b" || $sRelType == "r2b"} {
                     set iErr2  [ catch { mql print businessobject "$sToType" "$sToName" "$sToRev" select id dump } sToId ]
                  } else {
# construct to side where clause
                     set sWhereExpr ""
                     foreach sToRelExpr $lToRelExpr sToRelEach $lToRel {
                        if {$sWhereExpr != ""} {append sWhereExpr "&& "}
                        append sWhereExpr [string range $sToRelExpr 6 end] "=='$sToRelEach' "
                     }
                     set lsToRelId [split [mql query connection type "$sToRel" where $sWhereExpr select id dump |] \n]
                     if {[llength $lsToRelId] == 0 || [llength $lsToRelId] > 1 || [string first "Warning" "[lindex $lsToRelId 0]"] >= 0} {
                        set iErr2 1
                     } else {
                        set sToId [lindex [split [lindex $lsToRelId 0] |] 1]
                        if {$sToId == ""} {set iErr2 1}
                     }
                  }

                  if { [expr $iErr1 + $iErr2] != 0} {
                     if {$bDelete} {
                        set sSkip "d"
                     } else {
                        if {$iErr1 != 0} {
                           puts $iLogFileId "\n$sFromLabel to $sToLabel: $sFromLabel does not exist or is duplicated"
                           if {$bSpinnerAgent} {puts $iLogFileErr "$sFromLabel to $sToLabel: $sFromLabel does not exist or is duplicated"}
                        }
                        if {$iErr2 != 0} {
                           puts $iLogFileId "\n$sFromLabel to $sToLabel: $sToLabel does not exist or is duplicated"
                           if {$bSpinnerAgent} {puts $iLogFileErr "$sFromLabel to $sToLabel: $sToLabel does not exist or is duplicated"}
                        }
                        incr iErrTot
                        set sSkip "y"
                     }
                  }

					# Check for duplicate connection
                  if {$sSkip == "d" || $sSkip == "y"} {
                  } elseif {[catch {
                     if {$bMatch("$sFromId|$sToId|$sRel")} {
                        if {$bDelete} {
                           set sSkip "d"
                        } else {
                           set sSkip "a"
                        }
                     }
                  } sMsg ] != 0 } {
					# Set Add, Mod or Del
                     set bMatch("$sFromId|$sToId|$sRel") TRUE
                     set aConnect("$sFromId|$sToId|$sRel") [list ]
                     set sSkip "a"
                     if {$sRelType == "rel" || $sRelType == "b2b" || $sRelType == "b2r"} {
                        set lsExpandFrom [split [mql print bus $sFromId select $sFromDir\[$sRel\].id dump |] |]
                     } else {
                        set lsExpandFrom [list ]
                        set lslsExpandFrom [split [mql query connection type "$sRel" where "fromrel.id == $sFromId" select id dump |] \n]
                        foreach slsExpandFrom $lslsExpandFrom {
                           lappend lsExpandFrom [lindex [split $slsExpandFrom |] 1]
                        }
                     }
   
                     if {$sRelType == "rel" || $sRelType == "b2b" || $sRelType == "r2b"} {
                        set lsExpandTo [split [mql print bus $sToId select $sToDir\[$sRel\].id dump |] |]
                     } else {
                        set lsExpandTo [list ]
                        set lslsExpandTo [split [mql query connection type "$sRel" where "torel.id == $sToId" select id dump |] \n]
                        foreach slsExpandTo $lslsExpandTo {
                           lappend lsExpandTo [lindex [split $slsExpandTo |] 1]
                        }
                     }
   
                     foreach sExpand $lsExpandFrom {
                        if {[lsearch $lsExpandTo $sExpand] >= 0} {
                           lappend aConnect("$sFromId|$sToId|$sRel") $sExpand
                        }
                     }
                     if {$bDelete && $aConnect("$sFromId|$sToId|$sRel") == [list ]} {
                        set sSkip "d"
                     } elseif {$bDelete} {
                        set bDeleteFirst TRUE
                     } elseif {[llength $aConnect("$sFromId|$sToId|$sRel")] > 1} {
                        set bDeleteFirst TRUE
                     } elseif {$aConnect("$sFromId|$sToId|$sRel") != ""} {
                        set sSkip "m"
                     }
                  }

					# Process Add, Mod or Skip
                  if {$sSkip == "d"} {
                     if {$bPercent == "FALSE"} {puts -nonewline "."}
                     incr iSkipTotal
                  } elseif {$sSkip != "y"} {
                     set bError FALSE
                     if {$bScan != "TRUE"} {mql start transaction update}
                     if {$sSkip == "a"} {
                        if {$bDeleteFirst} {
                           foreach sConnect $aConnect("$sFromId|$sToId|$sRel") {
                              if {$sConnect != ""} {
                                 set sCmd "mql disconnect connection $sConnect"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 } else {
                                 puts $iLogFileId "$sCmd: #Rel: '$sRel' $sFromLabel to $sToLabel"
                                 if {[catch {eval $sCmd} sResult]} {
                                    puts $iLogFileId "\nERROR: Cannot disconnect duplicate rel.  Error message: \n$sResult"
                                    if {$bSpinnerAgent} {puts $iLogFileErr "\nERROR: Cannot disconnect duplicate rel.  Error message: \n$sResult"}
                                    if {$bPercent == "FALSE"} {puts -nonewline "!"}
                                    incr iErrTot
                                    mql abort transaction
                                    set bError TRUE
                                    break
                                 }
								 }
                              }
                           }
                        }
                        if {$bDelete} {
                           if {$bPercent == "FALSE"} {puts -nonewline "-"}
                           incr iDelTotal
                           mql commit transaction
                        } else {
                        
							# Modified by Solution Library for the New Line Feeder error - Start
                           regsub -all {<NEWLINE>} $llAttr "\\\n" llAttr
							# Modified by Solution Library for the New Line Feeder error - End
                           switch $sRelType {
                              rel {
                                 set sCmd "mql connect bus $sFromId rel \"$sRel\" $sToDir $sToId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              } b2b {
                                 set sCmd "mql add connection \"$sRel\" from $sFromId to $sToId $llAttr"
								 set bMQLExtract [mql get env SPINNEEXTRACTMQL]
								 if { $bMQLExtract == "TRUE"} {
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
								 }
                              } b2r {
                                 set sCmd "mql add connection \"$sRel\" from $sFromId torel $sToId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              } r2b {
                                 set sCmd "mql add connection \"$sRel\" fromrel $sFromId to $sToId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              } r2r {
                                 set sCmd "mql add connection \"$sRel\" fromrel $sFromId torel $sToId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              }
                           }
      
                           puts $iLogFileId "$sCmd"
                           set bTrigOn FALSE
                           if {$bTrigOver(1) == "ON" || ($bTriggerAdd == "ON" && $bTrigOver(1) != "OFF")} {
                              mql trigger on
                              set bTrigOn TRUE
                           }
                           if {$bScan || $bError} {
                           } elseif {[catch {eval $sCmd} sResult]} {
                              puts $iLogFileId "\nRel: '$sRel' $sFromLabel to $sToLabel: $sResult - triggers $sTrig(1)"
                              if {$bSpinnerAgent} {puts $iLogFileErr "Rel: '$sRel' $sFromLabel to $sToLabel: $sResult - triggers $sTrig(1)"}
                              if {$bPercent == "FALSE"} {puts -nonewline "!"}
                              incr iErrTot
                              mql abort transaction
                           } else {
                              if {[catch {mql commit transaction} sResult]} {
                                 puts $iLogFileId "\nRel: '$sRel' $sFromLabel to $sToLabel: $sResult - triggers $sTrig(1)"
                                 if {$bSpinnerAgent} {puts $iLogFileErr "Rel: '$sRel' $sFromLabel to $sToLabel: $sResult - triggers $sTrig(1)"}
                                 if {$bPercent == "FALSE"} {puts -nonewline "!"}
                                 incr iErrTot
                              } else {
                                 puts $iLogFileId "# $sFromLabel to $sToLabel: $sResult - triggers $sTrig(1)"
                                 if {$bPercent == "FALSE"} {puts -nonewline "+"}
                                 incr iAddTotal
                              }
                           }
                        }
                        if {$bTrigOn} {mql trigger off}
                     } elseif {$sSkip == "m" && $llAttr != ""} {
                        set sModDel modify
                        set sConnect connection
                        if {$bNonByPass == "TRUE" || ($bModIfExists && $bNonByPass != "FALSE")} {
						# Modified by Solution Library for the New Line Feeder error - Start
                           regsub -all {<NEWLINE>} $llAttr "\\\n" llAttr
						# Modified by Solution Library for the New Line Feeder error - End
                           switch $sRelType {
                              rel - b2b {
                                 set sCmd "mql $sModDel $sConnect bus \"$sFromType\" \"$sFromName\" \"$sFromRev\" $sToDir \"$sToType\" \"$sToName\" \"$sToRev\" rel \"$sRel\" $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              } b2r {
                                 set sRelId [lindex [split [mql query connection type "$sRel" where "from.id == $sFromId && torel.id == $sToId" select id dump |] |] 1]
                                 set sCmd "mql $sModDel connection $sRelId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              } r2b {
                                 set sRelId [lindex [split [mql query connection type "$sRel" where "fromrel.id == $sFromId && to.id == $sToId" select id dump |] |] 1]
                                 set sCmd "mql $sModDel connection $sRelId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              } r2r {
                                 set sRelId [lindex [split [mql query connection type "$sRel" where "fromrel.id == $sFromId && torel.id == $sToId" select id dump |] |] 1]
                                 set sCmd "mql $sModDel connection $sRelId $llAttr"
								  set bMQLExtract [mql get env SPINNEEXTRACTMQL]
							if { $bMQLExtract == "TRUE"} {
								 pProcessMqlCmd Mod bus "" $sCmd
								 }
                              }
                           }
   
                           puts $iLogFileId "$sCmd"
                           set bTrigOn FALSE
                           if {$bNonByPass == "TRUE" || ($bModIfExists && $bNonByPass != "FALSE")} {
                              if {$bTrigOver(2) == "ON" || ($bTriggerMod == "ON" && $bTrigOver(2) != "OFF")} {
                                 mql trigger on
                                 set bTrigOn TRUE
                              }
                           }
                           if {$bScan} {
                           } elseif {[catch {eval $sCmd} sResult]} {
                              puts $iLogFileId "\n$sFromLabel to $sToLabel: $sResult - triggers $sTrig(2)"
                              if {$bSpinnerAgent} {puts $iLogFileErr "$sFromLabel to $sToLabel: $sResult - triggers $sTrig(2)"}
                              if {$bPercent == "FALSE"} {puts -nonewline "!"}
                              incr iErrTot
                              mql abort transaction
                           } else {
                              puts $iLogFileId "# $sFromLabel to $sToLabel: $sResult - triggers $sTrig(2)"
                              if {$bPercent == "FALSE"} {puts -nonewline ":"}
                              incr iModTotal
                              mql commit transaction
                           }
                           if {$bTrigOn} {mql trigger off}
                        } else {
                           if {$bScan != "TRUE"} {mql commit transaction}
                           if {$bPercent == "FALSE"} {puts -nonewline "."}
                           incr iSkipTotal
                        }
                     } else {
                        if {$bPercent == "FALSE"} {puts -nonewline "."}
                        incr iSkipTotal
                        if {$bScan != "TRUE"} {mql commit transaction} 
                     }
                  } else {
                     if {$bPercent == "FALSE"} {puts -nonewline "!"}
                     incr iErrTot
                  }
				  
               }
            }
			# Write cue
            if {$bPercent && $iTenPercent < 10 && [expr $iAddTotal + $iModTotal + $iDelTotal + $iSkipTotal + $iErrTot] > [expr $iTenPercent * $iPercent]} {
               pWriteCue
               incr iTenPercent
            }
         }
		# end while for reading file
      } 
      if {$bPercent} {pWriteCue}
      pLogFile $iStartTime
      incr iErrTotal $iErrTot
   } else {
		puts "Warning : '$sCurFile' File is restricted for import."
   }
  }
   # end of forloop
   close $iLogFileId
   mql set env BUSRELERROR $iErrTotal
   puts ""
}
# end program 
