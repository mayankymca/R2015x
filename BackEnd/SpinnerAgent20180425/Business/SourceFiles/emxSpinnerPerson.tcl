#####################################################################*2014x
#
# @progdoc      emxSpinnerPerson.tcl vM2013 (Build 7.1.23)
#
# @Description: This is schema spinner that adds Person schema.
#               Invoked from program
#               'emxSpinnerPerson.tcl'.
#
# @Usage:       Run this program for an MQL command window w/data files in directories:
#               . (current dir)         emxSpinnerAgent.tcl, emxSpinnerAccess.tcl programs
#
# @progdoc      Copyright (c) ENOVIA Inc., June 26, 2002
#
#########################################################################
#
# @Modifications: Greg Inglis   2004-02-09 - first version.
#                 Matt Osterman 2004-08-01 - added email, iconmail, password, hidden
#                 Tarun Gupta 11/28/2005 - Added code to set the Site of users to blank.
#                 Matt Osterman 2006-08-15 - incorporated overlay mode with emphasis on role/group assignments
#				  Santosh Sthul 2012-11-21 - Added code to incorporate Administration Access when type is Business Admin and System Admin 
#########################################################################
tcl;
eval {
   set sHost [info host]
   if { $sHost == "sn732plp" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
      set cmd "debugger_eval"
      set xxx [debugger_init]
   } else {
      set cmd "eval"
   }
}
$cmd {
	set sPersonFileType [mql get env AccessType]
   set bOverlay [mql get env OVERLAY]
   set bModIfExists [mql get env PERSONMODIFEXISTS]

#########################################################################
#                         USER DEFINED VARIABLES                        #
#########################################################################
   if {$bModIfExists == "" || $bModIfExists != "TRUE"} {
      set bModIfExists "FALSE" ;#TRUE or FALSE - modify person admin object if it exists
   }
   if {$bOverlay == "" || $bOverlay != "TRUE"} {
      set bOverlay "FALSE" ;#TRUE or FALSE - overlay person properties
   }
   set sChangeOwner "Test Everything" ;# person to change ownership of bus objects to if person is deleted.
#########################################################################


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



#************************************************************************
# Procedure:   pReadObjectAccess
#
# Description: Procedure to read Object access.
#
# Returns:     The Object Access file data
#************************************************************************

proc pReadObjectAccess { sSpinDir} {
	set sDelimit "\t"
	set sFileObject [glob -nocomplain "$sSpinDir/Business/SpinnerPersonAccessData*.*"]
	return $sFileObject
}
# End pReadObjectAccess


#************************************************************************
# Procedure:   pReadAdminType
#
# Description: Procedure to read Admin type.
#
# Returns:     The Admin file data
#************************************************************************

proc pReadAdminType { sSpinDir} {
	set sDelimit "\t"
	set sFileAdmin [glob -nocomplain "$sSpinDir/Business/SpinnerPersonAdminData*.*"]
	#set lFileDataAdmin [pProcessFile $sFileAdmin $sDelimit]
	return $sFileAdmin
}
# End pReadAdminType

#************************************************************************
# Procedure:   pPrpocessAdminType
#
# Description: Procedure to Analyze Admin access.
#
# Returns:     Person's Admin access
#************************************************************************

proc pPrpocessAdminType { pFileDataAdmin pPerson lsAdminAccessDB} {
	set lHeaderAdmin [lindex $pFileDataAdmin 0]
    set lDataAdmin [lrange $pFileDataAdmin 1 end]
	set lHeaderAdminColumn [split $lHeaderAdmin " "]
	
	set nHeaderAdminColumnCount [expr [llength $lHeaderAdminColumn] - 1]
	set sInd [lsearch $lsAdminAccessDB none]
	if {$sInd > -1} {set lsAdminAccessDB [lreplace $lsAdminAccessDB $sInd $sInd] }
	set sInd [lsearch $lsAdminAccessDB all]
	if {$sInd > -1} {
		set lsAdminAccessDB [lreplace $lHeaderAdminColumn 0 0]
		set sProcessIndex [lsearch $lsAdminAccessDB Process]
		set lsAdminAccessDB [lreplace $lsAdminAccessDB $sProcessIndex $sProcessIndex]
	}
	set pAdminAccess {}
	foreach lLineDataAdmin $lDataAdmin {
		set sPerson [lindex $lLineDataAdmin 0]

		if { $sPerson == $pPerson } {
			set pLineDataAdmin [split $lLineDataAdmin " "]
			set nlength 0
			#foreach  AdminAccess $pLineDataAdmin column $lHeaderAdminColumn 
			foreach  AdminAccess $lLineDataAdmin column $lHeaderAdminColumn {
				if { $column == "Process" } {
					incr nlength
				} else {
					set pHasAccess [ string tolower $AdminAccess ]
					set clength [string length $column]
					if { $pHasAccess == "y" } {
						set sIndex [lsearch -nocase $lsAdminAccessDB $column]
						if {$sIndex == -1} {
							lappend lsAdminAccessDB $column
						}
					} elseif {$pHasAccess == "-"} {
						set sIndex [lsearch -nocase  $lsAdminAccessDB $column]
						if {$sIndex > -1} {
							set lsAdminAccessDB [lreplace $lsAdminAccessDB $sIndex $sIndex]
						}
					}
				}
			}
			set index [llength $lsAdminAccessDB]
			if {$index > 0} {
				foreach  aAccess $lsAdminAccessDB {
					if { [string length $pAdminAccess] > 0 } { 
						append pAdminAccess "," $aAccess
					} else {
						append pAdminAccess $aAccess
					}
					
				}
			}		
			set nlength [expr $nlength + $index]
			if { $nHeaderAdminColumnCount == $nlength } {
				set pAdminAccess "all"
			}
	   }
	}
	if { $pAdminAccess == ""} { append pAdminAccess none }
	return $pAdminAccess
}
# End pPrpocessAdminType

#************************************************************************
# Procedure:   pProcessObjectAccess
#
# Description: Procedure to Analyze Object access.
#
# Returns:     Person's Object access
#************************************************************************

proc pProcessObjectAccess { pFileDataObject pPerson lsObjectAccessDB} {
	set lHeaderObject [lindex $pFileDataObject 0]
    set lDataObject [lrange $pFileDataObject 1 end]
	set lHeaderObjectColumn [split $lHeaderObject " "]
	set nHeaderObjectColumnCount [expr [llength $lHeaderObjectColumn] - 1]
	set sInd [lsearch $lsObjectAccessDB none]
	if {$sInd > -1} {set lsObjectAccessDB [lreplace $lsObjectAccessDB $sInd $sInd] }
	set sInd [lsearch $lsObjectAccessDB all]
	if {$sInd > -1} {
		set lsObjectAccessDB [lreplace $lHeaderObjectColumn 0 0]
	}
	set pObjectAccess {}
	foreach lLineDataObject $lDataObject {
		set sPerson [lindex $lLineDataObject 0]
		if { $sPerson == $pPerson } {
			 set pLineDataObject [split $lLineDataObject " "]
			 set nlength 0
			 #foreach ObjectAccess $pLineDataObject column $lHeaderObjectColumn 
			 foreach ObjectAccess $lLineDataObject column $lHeaderObjectColumn {
				set pHasAccess [ string tolower $ObjectAccess ]
				if { $pHasAccess == "y" } {
					set sIndex [lsearch -nocase $lsObjectAccessDB $column]
					if {$sIndex == -1} {
						lappend lsObjectAccessDB $column
					}
				} elseif {$pHasAccess == "-"} {
					set sIndex [lsearch -nocase  $lsObjectAccessDB $column]
					if {$sIndex > -1} {
						set lsObjectAccessDB [lreplace $lsObjectAccessDB $sIndex $sIndex]
					}
				}
			}
			set index [llength $lsObjectAccessDB]
			if {$index > 0} {
				foreach  aAccess $lsObjectAccessDB {
					if { [string length $pObjectAccess] > 0 } { 
						append pObjectAccess "," $aAccess
					} else {
						append pObjectAccess $aAccess
					}
				}
			}	
			set nlength [expr $nlength + $index]
			if { $nHeaderObjectColumnCount == $nlength } {
				set pObjectAccess "all"
			}
	   }
	}
	if { $pObjectAccess == ""} { append pObjectAccess none }
	return $pObjectAccess
}
# End pProcessObjectAccess

#************************************************************************
# Procedure:   pWriteCue
#
# Description: Procedure to write processing cue to display.
#
# Returns:     Nothing
#************************************************************************

   proc pWriteCue {} {
      global iAddCounter iSkipCounter iErrorCounter iTenPercent iPrevAddModDel iPrevError
      set iAddModDel [expr $iAddCounter - $iPrevAddModDel]
      set iError [expr $iErrorCounter - $iPrevError]
      set iPrevAddModDel $iAddCounter
      set iPrevError $iErrorCounter
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
# end pWriteCue

#main

    set bScan [mql get env SPINNERSCANMODE]; #scan mode and log suffix - SpinnerAgent
    if {$bScan != "TRUE"} {set bScan FALSE}
    set sSuffix [clock format [clock seconds] -format ".%m%d%y"]
    if {$bScan} {set sSuffix ".SCAN"}

    set sDelimit "\t"
    if {[mql get env SPINNERLOGFILE] != ""} {; # SpinnerAgent
       set sLogFile [mql get env SPINNERLOGFILE]
       set sLogFileError [mql get env SPINNERERRORLOG]
       set bSpinnerAgent TRUE
    } else {    
       set sLogFile [file join . logs Person$sSuffix.log]
       set bSpinnerAgent FALSE
    }
    set iSystemError 0; # SpinnerAgent
    set iAddCounter 0
    set iSkipCounter 0
    set iErrorCounter 0
    set iPrevAddModDel 0
    set iPrevError 0
    set iTenPercent 1

    set lRole [split [mql list role] \n]
    set lGroup [split [mql list group] \n]
    set lProduct [split [mql list product] \n]
    
    set sBusType person
    set sSpinDir ""
    if {$bSpinnerAgent} {
       set sSpinDir [mql get env SPINNERPATH]
    }
	# QHV SR00116155 read Administration Access file
	set lFileDataAdmin [pReadAdminType $sSpinDir]
	#KYB Spinner V6R2014x Rev 4 - Read Person Object Access file
	set lFileDataObject [pReadObjectAccess $sSpinDir]
    set lsDataFile [glob -nocomplain "$sSpinDir/Business/SpinnerPersonData*.*"]
    set bFirstRun TRUE
	if {$sPersonFileType == "personadmin"} {
		set lsDataFile $lFileDataAdmin
	} elseif {$sPersonFileType == "personaccess"} {
		set lsDataFile $lFileDataObject
	}
    foreach sBusFileName $lsDataFile {
       set sFile $sBusFileName
       set sBusFileName [file tail $sBusFileName]
       
       if {$bSpinnerAgent == "FALSE"} {
          puts "Process $sBusType\(s\) from file '$sBusFileName':"
       } else {
          pfile_write $sLogFile "# \[[clock format [clock seconds] -format %H:%M:%S]\] Person\(s\) from file '$sBusFileName':\n"
          if {$bFirstRun} {
             puts ""
             set bFirstRun FALSE
          }
       }
               
       set lFileData [pProcessFile $sFile $sDelimit]
       set lHeader [lindex $lFileData 0]
       set lData [lrange $lFileData 1 end]
   
       set sCmd "mql list $sBusType"
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
           pfile_write $sLogFile "# No Bus type '$sBusType for file '$sBusFileName' exist"
       } else {
           set lPersonNames [split $sRtn \n]
       }
   
       set bWriteCue FALSE
       if {[llength $lData] > 19} {
          set bWriteCue TRUE
          set iPercent [expr [llength $lData] / 10]
       }
       foreach lLineData $lData {
           set sName [lindex $lHeader 0]
           set sNameValue [string trim [lindex $lLineData 0] \"]
           set bDel FALSE
           if {[string first "<<" $sNameValue] == 0 && [string first ">>" $sNameValue] == [expr [string length $sNameValue] -2]} {
              set sNameValue [string range $sNameValue 2 [expr [string length $sNameValue] -3]]
              set bDel TRUE
           }
           set sAddMod "mod"
           set slsAssign ""
           set lsAssignActual ""
           set sExists [lsearch $lPersonNames $sNameValue]
           set lsProdCmd ""
           if {!$bDel} {
              if {$sExists == -1} {
                 set sAddMod "add"
              } elseif {$bOverlay} {
                 catch {set slsAssign [mql print person "$sNameValue" select assignment dump |]} sMsg
                 set lsAssignActual [split $slsAssign |]
              } else {
                 catch {mql mod person "$sNameValue" remove assign all} sMsg
              }
           }
           set sCmt "     $sNameValue"
           if {$bWriteCue == "FALSE"} {
              puts -nonewline $sCmt
              if {$bScan} {puts ""}
           }
           if { $sExists == -1 || $bModIfExists} {
               set sCmd "mql $sAddMod $sBusType "
               set bJ FALSE
               set bRoleGroup FALSE
               set bProduct FALSE
               set lsAssignRole ""
               set lsAssignGroup ""
               set lsAssignProduct ""
			   
			   if {!$bDel} {
					set bJ TRUE
				   if {$sPersonFileType == "personadmin"} {
						set sCmd "mql mod person "
						if {$sAddMod == "mod"} {
							set lsAdminAccessDB [split [mql print person "$sNameValue" select admin dump] ,]
						 }
						set lFileDataAdmin [pProcessFile $lsDataFile $sDelimit ]
						set sAdminAccess [pPrpocessAdminType $lFileDataAdmin $sNameValue $lsAdminAccessDB]
						set slAccess [split $sAdminAccess ,]
						append sCmd " " "\"$sNameValue\""
						if {$slAccess != $lsAdminAccessDB} {
								append sCmd " " "admin" " " "\"$sAdminAccess\""
						} else {set sCmd ""}
					}
					#KYB Spinner V6R2014x Rev 4 - Read Person Object Access file
					if {$sPersonFileType == "personaccess"} {
						set lsObjectAccessDB ""
						set sCmd "mql mod person "
						if {$sAddMod == "mod"} {
							set tempVar ""
							set lsPerson [split [mql print person "$sNameValue" !history] \n]
							 foreach sPerson $lsPerson {
								if {[string first "access " $sPerson]  > 0} {
									regsub "access " $sPerson "" sPerson
									set tempVar [string trim $sPerson]
								   break
								}
							 }
							set lsObjectAccessDB [split $tempVar ,]
						 }
						set lFileObjectAdmin [pProcessFile $lsDataFile $sDelimit]
						set sObjectAccess [pProcessObjectAccess $lFileObjectAdmin $sNameValue $lsObjectAccessDB]
						set slAccess [split $sObjectAccess ,]
						append sCmd " " "\"$sNameValue\""
						if {$slAccess != $lsObjectAccessDB} {								
							append sCmd " " "access" " " "\"$sObjectAccess\""
						} else {set sCmd ""}
					}
				}
			   if  {$sPersonFileType == "person"} {
			   
               foreach i $lHeader j $lLineData {
                   set j [string trim $j \"]
                   if {[string tolower $j] == "<null>"} {set j "<null>"}
                   # do Not process the registry name
				   if {$i == "Registry Name"} {
                       continue
                   }
                   if { $i == "name" } {
                      append sCmd " " "\"$sNameValue\""
                   } elseif {$j != "" && !$bDel} {
                       set bJ TRUE
                       if { $i == "assign_role"} {
                           set bRoleGroup TRUE
                           set jtest [string tolower $j]
                           if {$jtest == "all"} {
                              set lsAssignRole $lRole
                           } elseif {$jtest == "<null>" || $jtest == "<<all>>"} {
                              if {$bOverlay} {
                                 set lsAssignRole "<null>"
                              } else {
                                 set lsAssignRole ""
                              }
                           } else {
                              set lsAssignRole [split $j |]
                           }
                       } elseif { $i == "assign_group"} {
                           set bRoleGroup TRUE
                           set jtest [string tolower $j]
                           if {[string tolower $j] == "all"} {
                              set lsAssignGroup $lGroup
                           } elseif {$jtest == "<null>" || $jtest == "<<all>>"} {
                              if {$bOverlay} {
                                 set lsAssignGroup "<null>"
                              } else {
                                 set lsAssignGroup ""
                              }
                           } else {
                              set lsAssignGroup [split $j |]
                           }
                       } elseif { $i == "assign_product"} {
                           set bProduct TRUE
                           set jtest [string toupper $j]
                           if {[string toupper $j] == "ALL"} {
                              set lsAssignProduct $lProduct
                           } elseif {$jtest == "<NULL>" || $jtest == "<<ALL>>"} {
                              set lsAssignProduct "<null>"
                           } else {
                              set lsAssignProduct [split $j |]
                           }
                       } elseif { $i == "hidden"} {
                          if {$j != ""} {
                             if {[string tolower $j] != "true" && [string tolower $j] != "hidden"} {
                                set j nothidden
                             } else {
                                set j hidden
                             }
                             append sCmd " $j"
                          }
                       } elseif { $i == "password"} {
                          set bModPassword TRUE
                          if {$sAddMod == "mod"} {
                             set lsPerson [split [mql print person "$sNameValue"] \n]
                             foreach sPerson $lsPerson {
                                if {[string trim $sPerson] == "password <RESTRICTED>"} {
                                   set bModPassword FALSE
                                   break
                                }
                             }
                          }
                          if {$bModPassword} {                               
                             if {$j == "<BLANK>" || $j == "<null>"} {
                                set j ""
                             }
                             append sCmd " " "$i" " " "\"$j\""
                          }
                       } elseif { $i == "forcepassword"} {
                          if {$j != ""} {
                             if {[string tolower $j] == "<blank>" || [string tolower $j] == "<null>"} {
                                set j ""
                             }
                             append sCmd " " "password" " " "\"$j\""
                          }
                       } elseif { $i == "passwordexpired"} {
                          if {$j != ""} {
                             if {[string tolower $j] == "true" || [string tolower $j] == "yes" || [string tolower $j] == "passwordexpired"} {
                                append sCmd " passwordexpired"
                             }
                          }
                       } elseif { $i == "iconmail" || $i == "e_mail" } {
                          if {$j != ""} {
                             if {[string tolower $j] == "enable" || [string tolower $j] == "true"} {
                                set j enable
                             } else {
                                set j disable
                             }
                             regsub "_" $i "" i 
                             append sCmd " $j $i"
                          }
                       } elseif {$i == "type"} {
						# QHV 21-11-2012 SR00116155 "append Administration Access"
							set sTypeData ""
							set sDatType ""
							if {$sAddMod == "mod"} {
								set lsPerson [split [mql print person "$sNameValue" !history] \n]
								 foreach sPerson $lsPerson {
									if {[string first "type " $sPerson]  > 0} {
										regsub "type " $sPerson "" sPerson
										set sDatType [string trim $sPerson]
									   break
									}
								 }
							 }
							set lsTypeDataFile [split $j ","]
							set lsTypeDataDB [split $sDatType ","]
							
							foreach sTypeDataFile $lsTypeDataFile {
								if { [string first "<<" $sTypeDataFile] >= 0 && [string last ">>" $sTypeDataFile] >= 0 } {
									set sTypeDataFile [string trim $sTypeDataFile "<<"] 
									set sTypeDataFile [string trim $sTypeDataFile ">>"]
									set data [string trim $sTypeDataFile]
									 if {[lsearch $lsTypeDataDB $data] >= 0} {
										append sTypeData "not" $data
									}										
								} else { 
									append sTypeData $sTypeDataFile
								}
								append sTypeData ","
							}
							if { [string last , $sTypeData] >= 0 } {
								set sTypeData [string trimright $sTypeData ","]
							}
							append sCmd " " "$i" " " "\"$sTypeData\""
                       } else {
						  if {$j == "<null>"} {set j ""}
						  append sCmd " " "$i" " " "\"$j\""
					  }
                   }
				  }
               }
   # Process Roles and Groups            
               if {$bRoleGroup} {
                  if {$bOverlay} {
                     if {$lsAssignRole == "<null>" && $lsAssignGroup == "<null>"} {
                        append sCmd " remove assign all"
                     } elseif {$lsAssignRole == "<null>"} {
                        set slsRemoveRole ""
                        foreach sRole $lRole {
                           if {[lsearch $lsAssignActual $sRole] > -1} {
                              append sCmd " remove assign role \"$sRole\""
                           }
                        }
                     } elseif {$lsAssignGroup == "<null>"} {
                        foreach sGroup $lGroup {
                           if {[lsearch $lsAssignActual $sGroup] > -1} {
                              append sCmd " remove assign group \"$sGroup\""
                           }
                        }
                     }
                  }
                  if {$lsAssignRole != "<null>"} {
                     foreach sAssignRole $lsAssignRole {
                        set sAssignRole [string trim $sAssignRole]
                        if {$bOverlay && [string first "<<" $sAssignRole] >= 0 && [string first ">>" $sAssignRole] == [expr [string length $sAssignRole] -2]} {
                           set sAssignRole [string trim $sAssignRole "<<"]
                           set sAssignRole [string trim $sAssignRole ">>"]
                           set sAssignRole [string trim $sAssignRole]
                           if {[lsearch $lRole $sAssignRole] > -1 && [lsearch $lsAssignActual $sAssignRole] > -1} {
                              append sCmd " remove assign role \"$sAssignRole\""
                           }
                        } elseif {[lsearch $lRole $sAssignRole] > -1} {
                           if {$bOverlay} {
                              if {[lsearch $lsAssignActual $sAssignRole] < 0} {
                                 append sCmd " assign role \"$sAssignRole\""
                              }
                           } else {
                              append sCmd " assign role \"$sAssignRole\""
                           }
                        #START - Fix issue SR00116356(Spinning in a user with a non existant role does not throw an error) By SL Team on 21/11/2012
                        } else { 
							pfile_write $sLogFile "Warning :The Role '$sAssignRole' doesn't exist"
						} 
						# END
                     }
                  }
                  if {$lsAssignGroup != "<null>"} {
                     foreach sAssignGroup $lsAssignGroup {
                        set sAssignGroup [string trim $sAssignGroup]
                        if {$bOverlay && [string first "<<" $sAssignGroup] >= 0 && [string first ">>" $sAssignGroup] == [expr [string length $sAssignGroup] -2]} {
                           set sAssignGroup [string trim $sAssignGroup "<<"]
                           set sAssignGroup [string trim $sAssignGroup ">>"]
                           set sAssignGroup [string trim $sAssignGroup]
                           if {[lsearch $lGroup $sAssignGroup] > -1 && [lsearch $lsAssignActual $sAssignGroup] > -1} {
                              append sCmd " remove assign group \"$sAssignGroup\""
                           }
                        } elseif {[lsearch $lGroup $sAssignGroup] > -1} {
                           if {$bOverlay} {
                              if {[lsearch $lsAssignActual $sAssignGroup] < 0} {
                                 append sCmd " assign group \"$sAssignGroup\""
                              }
                           } else {
                              append sCmd " assign group \"$sAssignGroup\""
                           }
						#START - Fix issue SR00116356(Spinning in a user with a non existant role does not throw an error) By SL Team on 21/11/2012
                        } else { 
							pfile_write $sLogFile "Warning :The Group '$sAssignGroup' doesn't exist"
						}
						 #END
                     }
                  }
               }
               if {$bProduct && !$bDel && $lsAssignProduct != ""} {
                  set lsProductActual [split [mql list product * where "person == '$sNameValue'"] \n]
                  if {$lsAssignProduct == "<null>"} {
                     if {$lsProductActual != [list ]} {
                        set lsProdCmd [list ]
                        foreach sProductActual $lsProductActual {
                           lappend lsProdCmd "mql mod product $sProductActual remove person '$sNameValue'"
                        }
                     }
                  } else {
                     foreach sAssignProduct $lsAssignProduct {
                        set sAssignProduct [string trim $sAssignProduct]
                        if {[string first "<<" $sAssignProduct] >= 0 && [string first ">>" $sAssignProduct] == [expr [string length $sAssignProduct] -2]} {
                           set sAssignProduct [string trim $sAssignProduct "<<"]
                           set sAssignProduct [string trim $sAssignProduct ">>"]
                           set sAssignProduct [string trim $sAssignProduct]
                           if {[lsearch $lsProductActual $sAssignProduct] > -1} {
                              lappend lsProdCmd "mql mod product $sAssignProduct remove person '$sNameValue'"
                           }
                        } elseif {[lsearch $lProduct $sAssignProduct] > -1} {
                           if {[lsearch $lsProductActual $sAssignProduct] < 0} {
                              lappend lsProdCmd "mql mod product $sAssignProduct add person '$sNameValue'"
                           }
						#START - Fix issue SR00116356(Spinning in a user with a non existant role does not throw an error) By SL Team on 21/11/2012
                        } else { 
							pfile_write $sLogFile "Warning :The Product '$sAssignProduct' doesn't exist"
						}
					    #END
                     }
                  }
               }
               
               set bProcess TRUE
               if {$bJ == "FALSE"} {
                  if {$sExists == -1} {
                     set bProcess FALSE
                  } else {
                     regsub "mod" $sCmd "delete" sCmd
                     mql trigger off
                     if {[mql list person $sNameValue] != ""} {
                        puts -nonewline " (DELETE in progress - reassigning bus objects to '$sChangeOwner')"
                        if {[mql print bus Person $sNameValue - select exists dump]} {
                           mql delete bus Person $sNameValue -
                           pfile_write $sLogFile "mql delete bus Person \"$sNameValue\" -"
                        }  
                        set lsBusObj [split [mql temp query bus * * * owner "$sNameValue" select id dump |] \n]
                        foreach slsBusObj $lsBusObj {
                           set oId [lindex [split $slsBusObj |] 3]
                           mql mod bus $oId owner $sChangeOwner
                           pfile_write $sLogFile "mql mod bus $oId owner \"$sChangeOwner\""
                        }
                     }
                     mql trigger on
                  }
               }
               if {$bProcess} {
					if {$sCmd != ""} {
						pfile_write $sLogFile $sCmd
					}
                  if {$bScan != "TRUE"} {;#spinneragent
                      if {[catch {
                         eval $sCmd
                         foreach sProdCmd $lsProdCmd {
                            pfile_write $sLogFile $sProdCmd
                            eval $sProdCmd
                         }
                      } sMsg] == 0} {
                          if {$bWriteCue == "FALSE"} {
                             puts " (SUCCESS)"
                          } else {
                             incr iAddCounter
                          }
                          #pfile_write $sLogFile "# Command return is: SUCCESS"
                      } else {
                          if {$bWriteCue == "FALSE"} {
                             puts " (ERROR)"
                          } else {
                             incr iErrorCounter
                          }
                          pfile_write $sLogFile "Command return is: ERROR, message is:\n$sMsg"
                          if {$bSpinnerAgent} {
                             set iLogFileErr [open $sLogFileError a+]
                             puts $iLogFileErr "MQL command:\n$sCmd\nError message is:\n$sMsg"
                             close $iLogFileErr
                          }
                          incr iSystemError; #SpinnerAgent
                      }
                  }
               } else {
                  if {$bWriteCue == "FALSE"} {
                     puts " (SKIP)"
                  } else {
                     incr iSkipCounter
                  }
                  pfile_write $sLogFile "# $sBusType '$sNameValue' already deleted, SKIP"
               }
           } else {
               if {$bWriteCue == "FALSE"} {
                  puts " (SKIP)"
               } else {
                  incr iSkipCounter
               }
               pfile_write $sLogFile "# $sBusType '$sNameValue' already exists, SKIP"
           }
           if {$bWriteCue && $iTenPercent < 10 && [expr $iAddCounter + $iSkipCounter + $iErrorCounter] > [expr $iTenPercent * $iPercent]} {
              pWriteCue
              incr iTenPercent
           }
       }
       if {$bWriteCue} {
          pWriteCue
          puts ""
       }
       pfile_write $sLogFile "# End Process $sBusType\n"
       mql set env PERSONERROR $iSystemError; #SpinnerAgent
    }
}

