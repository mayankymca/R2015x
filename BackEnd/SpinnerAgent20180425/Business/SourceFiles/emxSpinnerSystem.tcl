
#####################################################################*2014x
#
# @progdoc      emxSpinnerSystem.tcl vMV6R2013 (Build 11.10.2)
#
# @Description: This is schema spinner that adds system schema.
#               Invoked from program
#               'emxSpinnerSystem.tcl'.
#
# @Parameters:  None
#
# @Usage:       Run this program for an MQL command window w/data files in directories:
#               . (current dir)         emxSpinnerAgent.tcl, emxSpinnerAccess.tcl programs
#
# @progdoc      Copyright (c) ENOVIA Inc., June 26, 2002
#
#########################################################################
#
# @Modifications: Greg Inglis 2004-02-09 - first version.
# @Modifications: Matt Osterman 2007-01-18 - fully functional for vault, store,
#               location, site, server, index w/modify, delete and registration
# @Modifications: See SchemaAgent_ReadMe.htm
#
#########################################################################
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

#************************************************************************
# Procedure:   pfile_write
#
# Description: Procedure to write a variable to file.
#
# Parameters:  The filename to write to,
#              The data variable.
#
# Returns:     Nothing
#************************************************************************

proc pfile_write { filename data } {
  return  [catch {
    set fileid [open $filename "a+"]
    puts $fileid $data
    close $fileid
  }]
}
#End pfile_write


#************************************************************************
# Procedure:   pfile_read
#
# Description: Procedure to read a file.
#
# Parameters:  The filename to read from.
#
# Returns:     The file data
#************************************************************************

proc pfile_read { sFile } {

  set data ""
  if { [file readable $sFile] } {
    set fd [open $sFile r]
    set data [read $fd]
    close $fd
  }
  return $data
}
#End file_read


proc pProcessFile { sFileName sDelimit } {

    set lFileData [ list ]
    set sFileData [ split [ pfile_read $sFileName ] \n ]
    set sHeaderRaw [split [lindex $sFileData 0] $sDelimit ]

    set sHeader [ list ]
    foreach i $sHeaderRaw {
        lappend sHeader [ string trim $i ]
    }
    lappend lFileData $sHeader
    set sData [ lrange $sFileData 1 end ]
    foreach sDataLine $sData {
        set sDataLine [ split $sDataLine $sDelimit ]
        if {$sDataLine != ""} {
            lappend lFileData $sDataLine
        }
    }
    return $lFileData
}
# End pIncludeFile


#main

   #KYB Fixed Spinner Version Issue for ENOVIA V6R2014x HFs
   #set sMxVersion [mql version]
   set sMxVersion [mql get env MXVERSION]
   
    if {[string first "V6" $sMxVersion] >= 0} {
       set rAppend ""
	   if {[string range $sMxVersion 7 7] == "x"} {set rAppend ".1"}
       set sMxVersion [string range $sMxVersion 3 6]
	   if {$rAppend != ""} {append sMxVersion $rAppend}
    } else {
       set sMxVersion [join [lrange [split $sMxVersion .] 0 1] .]
    }
    set bScan [mql get env SPINNERSCANMODE]; #scan mode and log suffix - SpinnerAgent
    if {$bScan != "TRUE"} {set bScan FALSE}
    set sSuffix [clock format [clock seconds] -format ".%m%d%y"]
    if {$bScan} {set sSuffix ".SCAN"}

    set sDelimit "\t"
    set lSystemTypeNames [ list ]
    if {[mql get env SPINNERLOGFILE] != ""} {; # SpinnerAgent
       set sLogFile [mql get env SPINNERLOGFILE]
       set sLogFileError [mql get env SPINNERERRORLOG]
       set bSpinnerAgent TRUE
    } else {    
       set sLogFile [file join . logs System$sSuffix.log]
       set bSpinnerAgent FALSE
    }
    set iSystemError 0; # SpinnerAgent

    set lSystemInfo [ list {location location.xls} {site site.xls} \
        {store store_captured.xls} {store store_ingested.xls} {store store_tracked.xls} \
        {server server.xls} \
        {vault vault_local.xls} {vault vault_remote.xls} {vault vault_foreign.xls} \
        {index index.xls} ]
    if {[mql list program eServiceSchemaVariableMapping.tcl] == ""} {
       set bRegistry FALSE
    } else {
       set bRegistry TRUE
       set lsRegistry [split [mql print program eServiceSchemaVariableMapping.tcl select property dump |] |]
       array set aRegistry [list store store vault lattice location location site site server server index index]
    }
    puts ""
    
    foreach lSystem $lSystemInfo {
        set sSystemType [lindex $lSystem 0]
        set sSystemFileName [lindex $lSystem 1]
            
        set sSpinDir ""
        if {$bSpinnerAgent} {
           set sSpinDir [mql get env SPINNERPATH]
        }
        if {$sSpinDir != "" && $sSpinDir != "."} {
              set sFile [file join "$sSpinDir\/System" $sSystemFileName]
        } else {
              set sFile [file join System $sSystemFileName]
        }

        if {[file exists $sFile] == 1} {
           puts "Process $sSystemType\(s\) from file '$sSystemFileName':"
           pfile_write $sLogFile "# \[[clock format [clock seconds] -format %H:%M:%S]\] $sSystemType\(s\) from file '$sSystemFileName':\n"
           set lFileData [pProcessFile $sFile $sDelimit]
       
           set lHeader [lindex $lFileData 0]
           set lData [lrange $lFileData 1 end]
   
           set sCmd "mql list $sSystemType"
           if {[catch {eval $sCmd} sMsg] == 0} {
               set sRtn $sMsg
           } else {
               set sCmt "An error occurred. The mql cmd is\n$sCmd\nError message is\n$sMsg\n"
               puts $sCmt
               pfile_write $sLogFile $sCmt
               if {$bSpinnerAgent} {
                  set iLogFileErr [open $sLogFileError a+]
                  puts $iLogFileErr $sCmt
                  close $iLogFileErr
               }
               incr iSystemError; #SpinnerAgent
           }
           
           if {$sRtn == ""} {
               pfile_write $sLogFile "# No System type '$sSystemType for file '$sSystemFileName' exist"
           } else {
               set lSystemTypeNames [split $sRtn \n]
           }
   
           foreach lLineData $lData {
               set bAdd FALSE
               set bMod FALSE
               set bModAppend FALSE
               set bDel FALSE
               set sCmd ""
               set sName [lindex $lHeader 0]
               set sNameValue [string trim [lindex $lLineData 0] \"]
               if {$sNameValue == ""} {continue}
               if {[string first "<<" $sNameValue] == 0 && [string first ">>" $sNameValue] == [expr [string length $sNameValue] -2]} {
                   regsub "<<" $sNameValue "" sNameValue
                   regsub ">>" $sNameValue "" sNameValue
                   set sNameValue [string trim $sNameValue]
                   set bDel TRUE
               }
   # Analyze Registration 1/18/2007 MJO     
               set bReg FALSE
               set bUnReg FALSE
               set bRename FALSE
               if {!$bDel} {
                   set lUnRegProp {}
                   set lUnRegTo {}
                   set sRegName [string trim [lindex $lLineData 1] \"]
                   if {$sRegName != "" && $bRegistry} {
                       if {[string tolower $sRegName] == "<null>"} {set sRegName ""}
                       set sSymbolicFromName ""
                       set iReg [lsearch -regexp $lsRegistry "to $aRegistry($sSystemType) $sNameValue"]
                       if {$iReg >= 0} {
                           set sSymbolicFromName [lindex [split [lindex $lsRegistry $iReg] " "] 0]
                       }
        
                       if {$sRegName == ""} {
                          if {$sSymbolicFromName != ""} {
                              set bUnReg TRUE
                              lappend lUnRegProp $sSymbolicFromName
                              lappend lUnRegTo $sNameValue
                          }
                       } else {
                           set sSymbolicFromRegName "$sSystemType\_$sRegName"
                           regsub -all " " $sSymbolicFromRegName "" sSymbolicFromRegName
                           set sNameFromRegName [mql print program eServiceSchemaVariableMapping.tcl select property\[$sSymbolicFromRegName\].to dump]
                           if {$sNameFromRegName != ""} {
                               regsub $aRegistry($sSystemType) $sNameFromRegName "" sNameFromRegName
                               set sNameFromRegName [string trim $sNameFromRegName]
                           }
                           if {$sSymbolicFromName == "" && $sNameFromRegName != "" && $sNameValue != $sNameFromRegName} {
                               set bRename TRUE
                               set sNewName $sNameValue
                               set sNameValue $sNameFromRegName
                           } else {
                               if {$sSymbolicFromName == "" || $sNameFromRegName == "" || $sNameValue != $sNameFromRegName || $sSymbolicFromName != $sSymbolicFromRegName} {
                                   set bReg TRUE
                               }
                               if {$sNameFromRegName != "" && $sNameValue != $sNameFromRegName} {
                                   set bUnReg TRUE
                                   lappend lUnRegProp $sSymbolicFromRegName
                                   lappend lUnRegTo $sNameFromRegName
                               }
                               if {$sSymbolicFromName != "" && $sSymbolicFromName != $sSymbolicFromRegName} { 
                                   set bUnReg TRUE
                                   lappend lUnRegProp $sSymbolicFromName
                                   lappend lUnRegTo $sNameValue
                               }
                           }
                       }
                   }
               }
   # End Analyze Registration
               set sExists [lsearch $lSystemTypeNames $sNameValue]
               set sCmt "     $sNameValue"
               puts -nonewline $sCmt
               if {$sExists == -1 && !$bDel} {
                   set bAdd TRUE
                   set sCmd "mql add $sSystemType \042$sNameValue\042"
               } elseif {$bDel} {
                   set sCmd "mql delete $sSystemType \042$sNameValue\042"
               } else {
                   set bMod TRUE
                   set sCmd "mql mod $sSystemType \042$sNameValue\042"
                   if {$bRename} {append sCmd " name \042$sNewName\042"}
               }
               if {!$bDel} {
                   foreach i $lHeader j $lLineData {
                       set j [string trim $j \"]
                       if {$i == "path" || $i == "file"} {
                           # replace all double backslash with a forward slash, all singles with forward
                           regsub -all -- {\134\134} $j {/} j
                           regsub -all -- {\134} $j {/} j
                       }
                       if { $i == "name" || $i == "Registry Name"} {
                           # do not process the registry name (a future)
                           continue
                       } elseif {$bMod && $i == "type"} {
                           # skip as type is set on add only
                           pfile_write $sLogFile "# WARNING: Skipping modification to 'type' as not allowed."
                           continue
                       } elseif {$bMod && ($i == "indexspace" || $i == "tablespace" || $i == "server" || $i == "interface")} {
                           # skip as items are set on add only
                           pfile_write $sLogFile "# WARNING: Skipping modification to '$i' as not allowed."
                           continue
                       } elseif {$i == "location" && $j != ""} {
                           set lLoc [split $j |]
                           set sLocData ""
                           foreach sLoc $lLoc {
                               set sLoc [string trim $sLoc ]
                               if {[string first "<<" $sLoc] == 0 && [string first ">>" $sLoc] == [expr [string length $sLoc] -2]} {
                                   if {$bMod} {
                                       regsub "<<" $sLoc "" sLoc
                                       regsub ">>" $sLoc "" sLoc
                                       set sLoc [string trim $sLoc]
                                       if {[mql list location \042$sLoc\042] != ""} {append sLocData "remove location \042$sLoc\042 "}
                                    }
                               } else {
                                  append sLocData "add location \042$sLoc\042 "
                               }
                           }
                           if {$sLocData != ""} {
                               if {$bAdd} {
                                   set bModAppend TRUE
                                   set sModName $sNameValue
                                   if {$bRename} {set sModName $sNewName}
                                   set sModAppend "mql mod $sSystemType \042$sModName\042 $sLocData"
                               } elseif {$bMod} {
                                   append sCmd " $sLocData"
                               }
                           }
                       } elseif {$i == "attribute" && $j != ""} {
                           set lAttr [split $j |]
                           set lAttrData {}
                           set sAttrData ""
                           foreach sAttr $lAttr {
                               set sAttr [string trim $sAttr]
                               if {[string first "<<" $sAttr] == 0 && [string first ">>" $sAttr] == [expr [string length $sAttr] -2]} {
                                   if {$bMod} {
                                       regsub "<<" $sAttr "" sAttr
                                       regsub ">>" $sAttr "" sAttr
                                       set sAttr [string trim $sAttr]
                                       if {[mql list attribute \042$sAttr\042] != ""} {append sAttrData "remove attribute \042$sAttr\042 "}
                                    }
                               } else {
                                   if {$bAdd} {
				        #Commented below line to fix 355056 on 12 june 08
					#lappend lAttrData $sAttr
					#Added below to fix 355056  on 12 june 08 - start
					set tempAttrName '
					append tempAttrName $sAttr
					append tempAttrName '
                                        lappend lAttrData $tempAttrName
					#Added to fix 355056  on 12 june 08 - end
                                    } elseif {$bMod} {
                                       set lsAttr [split [mql print $sSystemType "$sNameValue" select attribute dump |] |]
                                       if {[lsearch $lsAttr $sAttr] < 0} {
                                           append sAttrData "add attribute \042$sAttr\042 "
                                       }
                                   }
                               }
                           }
                           if {$bAdd} {
                               if {[llength $lAttrData] > 0} {
                                   #Modified below to fix 355056 on 12 june 08 - start
				   append sCmd " " "$i" " " [join $lAttrData ,]
				   #Modified to fix 355056 on 12 june 08 - end
                                }
                           } elseif {$bMod} {
                               if {$sAttrData != ""} {
                                   append sCmd " $sAttrData"
                               }
                           }
                       } elseif {$i == "enable" && $j != ""} {
                           set bModAppend TRUE
                           set sModName $sNameValue
                           if {$bRename} {set sModName $sNewName}
						   if {[string tolower $j] != "true" && [string tolower $j] != "$i"} {
                              set sModAppend "mql disable index \042$sModName\042"
                           } else {
							 # KYB V6R2014 Start Checked whether index is already enabled or not
							  set sEnable [mql print index \042$sModName\042 select enabled dump]
							  if { $sEnable != "TRUE" } {
									set sModAppend "mql enable index \042$sModName\042"
								 } else {
									set sModAppend ""
									continue; 
								 }
							  # KYB V6R2014 End
                           }
                       } elseif { $i == "hidden" || $i == "multipledirectories" || $i == "unique" || $i == "foreign" || $i == "lock"} {
                           if {$i == "multipledirectories" && $sMxVersion > 2011.1} {
						      continue
						   } else {
                           if {$j != ""} {
                               if {[string tolower $j] != "true" && [string tolower $j] != "$i"} {
                                   if {$i == "lock"} {
                                       set j "unlock"
                                   } else {
                                       set j "not$i"
                                   }
                               } else {
                                   set j $i
                               }
                               append sCmd " $j"
                           }
						   }
                       } elseif { $j != "" } {
                           if {[string tolower $j] == "<null>"} {set j ""}
                           if {$i == "permission"} {
                               regsub -all -- "\134\174" $j "," j
                               regsub -all -- " " $j "" j
                           }
                           append sCmd " " "$i" " " "\"$j\""
                       }
                   }
               }
   # Process Command
               pfile_write $sLogFile $sCmd
               set bErr FALSE
               if {$bScan != "TRUE"} {;#spinneragent
                   if {$sCmd == ""} {
                       puts " (SKIP)"
                       pfile_write $sLogFile "# Command return is: SKIP"
                   } elseif {[catch {eval $sCmd} sMsg] == 0} {
                       if {!$bModAppend} {
                           puts " (SUCCESS)"
                           pfile_write $sLogFile "# Command return is: SUCCESS"
                       }
                   } else {
                       puts " (ERROR)"
                       pfile_write $sLogFile "Command return is: ERROR, message is:\n$sMsg"
                       if {$bSpinnerAgent} {
                          set iLogFileErr [open $sLogFileError a+]
                          puts $iLogFileErr "MQL command:\n$sCmd\nError message is:\n$sMsg"
                          close $iLogFileErr
                       }
                       incr iSystemError; #SpinnerAgent
                       set bErr TRUE
                   }
               }
   # Post Process Command
               if {$bModAppend} {
                   pfile_write $sLogFile "$sModAppend"
                   if {$bScan != "TRUE"} {; #Spinneragent
                       if {[catch {eval $sModAppend} sMsg] == 0} {
                           puts " (SUCCESS)"
                           pfile_write $sLogFile "# Command return is: SUCCESS"
                       } else {
                           if {!$bErr} {
                               puts " (ERROR)"
                               incr iSystemError; #SpinnerAgent
                           }
                           pfile_write $sLogFile "Command return is: ERROR, message is:\n$sMsg"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "MQL command:\n$sModAppend\nError message is:\n$sMsg"
                              close $iLogFileErr
                           }
                       }
                   }
               }
   # Process Registration 1/18/2007 MJO
               if {$bUnReg} {
                   foreach sUnRegProp $lUnRegProp sUnRegTo $lUnRegTo {
                       set sCmd "mql delete property $sUnRegProp on program eServiceSchemaVariableMapping.tcl to $aRegistry($sSystemType) \042$sUnRegTo\042"
                       pfile_write $sLogFile "$sCmd"
                       if {$bScan != "TRUE"} {; #Spinneragent
                           if {[catch {eval $sCmd} sMsg] == 0} {
                               puts "Unregistration of $aRegistry($sSystemType) \042$sUnRegTo\042 (SUCCESS)"
                               pfile_write $sLogFile "# Command return is: SUCCESS"
                               set lsRegistry [split [mql print program eServiceSchemaVariableMapping.tcl select property dump |] |]
                           } else {
                               if {!$bErr} {
                                   puts "Unregistration of $aRegistry($sSystemType) \042$sUnRegTo\042 (ERROR)"
                                   incr iSystemError; #SpinnerAgent
                               }
                               pfile_write $sLogFile "Command return is: ERROR, message is:\n$sMsg"
                               if {$bSpinnerAgent} {
                                   set iLogFileErr [open $sLogFileError a+]
                                   puts $iLogFileErr "MQL command:\n$sCmd\nError message is:\n$sMsg"
                                   close $iLogFileErr
                               }
                           }
                       }
                   }
               }
               if {$bReg} {
                   set sCmd "mql add property $sSymbolicFromRegName on program eServiceSchemaVariableMapping.tcl to $aRegistry($sSystemType) \042$sNameValue\042"
                   pfile_write $sLogFile "$sCmd"
                   if {$bScan != "TRUE"} {; #Spinneragent
                       if {[catch {eval $sCmd} sMsg] == 0} {
                           puts "Registration of $aRegistry($sSystemType) \042$sNameValue\042 (SUCCESS)"
                           pfile_write $sLogFile "# Command return is: SUCCESS"
                           set lsRegistry [split [mql print program eServiceSchemaVariableMapping.tcl select property dump |] |]
                       } else {
                           if {!$bErr} {
                               puts "Registration of $aRegistry($sSystemType) \042$sNameValue\042 (ERROR)"
                               incr iSystemError; #SpinnerAgent
                           }
                           pfile_write $sLogFile "Command return is: ERROR, message is:\n$sMsg"
                           if {$bSpinnerAgent} {
                               set iLogFileErr [open $sLogFileError a+]
                               puts $iLogFileErr "MQL command:\n$sCmd\nError message is:\n$sMsg"
                               close $iLogFileErr
                           }
                       }
                   }
               }
   # End Process Registration
           }		   
        pfile_write $sLogFile "# End Process $sSystemType\n"
        }		
    }
    mql set env SYSTEMERROR $iSystemError; #SpinnerAgent
}

