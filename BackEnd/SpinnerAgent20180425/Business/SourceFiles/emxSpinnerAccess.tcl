#####################################################################*2014x
#
# @progdoc      emxSpinnerAccess.tcl vMV6R2013 (Build 11.10.1)
#
# @Description: This is schema spinner that adds or modifies schema
#               policy access.  Invoked from program
#               'emxSpinnerAgent.tcl' but may be run separately.
#
# @Parameters:  None
#
# @Usage:       Run this program for an MQL command window w/data files in directories:
#               . (current dir)         emxSpinnerAgent.tcl, emxSpinnerAccess.tcl programs
#               ./Business/Policy       Policy access data files from Bus Doc Generator program
#
# @progdoc      Copyright (c) ENOVIA Inc., June 26, 2002
#
# @Originator:  Greg Inglis
#
#########################################################################
#
# @Modifications: Venkatesh Harikrishan 04/03/2006 - Fix for Incident 317721
# @Modifications: Medha TAMBE 11/12/2014 - Support For Revoke column
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
proc pfile_read { filename } {
  global sPolicyRule aPolicyRule
  set data ""
  # IR Fix 317721
  set sSpinnerDir [mql get env SPINNERPATH]
  set sFile "$sSpinnerDir/Business/$aPolicyRule($sPolicyRule)/$filename"

  if { [file readable $sFile] } {
    set fd [open $sFile r]
    set data [read $fd]
    close $fd
  } else { # IR Fix 317721
  	puts "File Not Found"
  }
  return $data
}
#End file_read

################################################################################
# Replace_Space
#   Replace space characters by underscore
#   
#   Parameters :
#       string
#   Return :
#       string 
#********************************************************************************
proc Replace_Space { string } {
    regsub -all -- " " $string "_" string
	
    return $string
}
proc Replace_backslash { string } {
    
	regsub -all "_bslash_" $string "/" string
    return $string
}
proc Replace_bslash { string } {
    
	regsub -all "/" $string "_bslash_" string
    return $string
}
proc pProcessMqlCmd { sAction sType sName sMql } {
    global sMsg_Log iAccessError bScan sLogFileError bSpinnerAgent bMQLExtract sMQLExtractFileDir
    append sMsg_Log "# ACTION: $sAction $sType $sName\n"
	append sMsg_Log "# Process query:$sMql\n"
	
	#QHV Start MQLExtract
	set bMQLExtract [mql get env SPINNEEXTRACTMQL]
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
    	mql start transaction update
        if { [ catch { eval $sMql } sMsg ] != 0 } {
            set sErrMsg "$sAction $sType $sName NOT successful.\nCommand: $sMql\nError Reason is $sMsg\n"
            append sMsg_Log $sErrMsg
            mql abort transaction
            puts -nonewline "!"
            if {$bSpinnerAgent} {
               set iLogFileErr [open $sLogFileError a+]
               puts $iLogFileErr $sErrMsg
               close $iLogFileErr
            }
            incr iAccessError
        } else {
            append sMsg_Log "# $sAction $sType $sName Successful."
            puts -nonewline ":"
            mql commit transaction
        }
    }
    return $sMsg
}
#End pProcessMqlCmd

# Procedure to pass tcl-type variables in tcl eval commands
   proc pRegSubEscape {sEscape} {
      regsub -all "\134$" $sEscape "\134\134\$" sEscape
      regsub -all "\134{" $sEscape "\134\134\173" sEscape
      regsub -all "\134}" $sEscape "\134\134\175" sEscape
      regsub -all "\134\133" $sEscape "\134\134\133" sEscape
      regsub -all "\134\135" $sEscape "\134\134\135" sEscape
      if {[string range $sEscape 0 0] == "\042" && [string range $sEscape end end] == "\042" && [string length $sEscape] > 2} {
         set iLast [expr [string length $sEscape] -2]
   	 set sEscape [string range $sEscape 1 $iLast]
      }
      regsub -all "\042\042" $sEscape "\042" sEscape
      regsub -all "\042" $sEscape "\134\042" sEscape
      return $sEscape
   }
#End pRegSubEscape

proc pIsAssociationGroup { sUser lAssociationList lGroupList } {
	foreach sAssociation $lAssociationList {
		if { $sUser == $sAssociation } {
			return "TRUE"
		}
	}	
	foreach sGroup $lGroupList {
		if { $sUser == $sGroup } {
			return "TRUE"
		}
	}	
	return "FALSE"
}

proc pGetAssociationList {} {
	set lAssociationList [split [mql list association] \n]
	return $lAssociationList
}

proc pGetGroupList {} {
	set lGroup [split [mql list group] \n]
	return $lGroup
}

proc pCompareLists { lList1 lList2 } {
    set lCommon {}
    set lUnique1 {}
    foreach i1 $lList1 {
        set nFound [ lsearch $lList2 $i1 ]
        if { $nFound == -1 } {
            lappend lUnique1 $i1
        } else {
            lappend lCommon $i1
            set lList2 [ lreplace $lList2 $nFound $nFound ]
        }
    }
    set lResults [ list $lUnique1 $lCommon $lList2 ]
    return $lResults
}

proc pPolicyDataWithLoginKey { sPol } {
    global sPolicyRule bAllState

   set lAccessModes [ list read modify delete checkout checkin schedule lock \
	   unlock execute freeze thaw create revise majorrevise promote demote grant \
	   enable disable override changename changetype changeowner changepolicy revoke \
	   changevault fromconnect toconnect fromdisconnect todisconnect \
	   viewform modifyform show approve reject ignore reserve unreserve ]
	
	   set lAccessStr "read,modify,delete,checkout,checkin,schedule,lock,unlock,execute,freeze,thaw,create,revise,majorrevise,promote,demote,grant,enable,disable,override,changename,changetype,changeowner,changepolicy,revoke,changevault,fromconnect,toconnect,fromdisconnect,todisconnect,viewform,modifyform,show,approve,reject,ignore,reserve,unreserve"

    set lData {}
    set sStates [list 999999]
    if {$sPolicyRule == "policy"} {
        set sStates [ split [ mql print policy $sPol select state dump | ] | ]
        if {$bAllState && $sStates != [list ]} {lappend sStates "allstate"}
    }
    foreach sSt $sStates {
	
		set sState "allstate"
		if {$sSt != "allstate"} {
			set sState "state\[$sSt\]"
		}
		set slUserTypes [ list "owner" "public" "user" ]
		
		foreach sUserType $slUserTypes {
			set sUser "user"
			set sUserName $sUserType
			if {$sUserType == "owner" || $sUserType == "public"} {
				set sUser "owner"
				append sUserName "\[*\]"
			}
			
			set sUser [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.$sUser dump | ]
			set sUsers [ split $sUser | ]

			set sAccess [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.access dump | ]
			set sAccess [ split $sAccess | ]
			
			set sFilter [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.filter dump : ]
			set sFilter [ split $sFilter : ]
			
			set sLocalFilter [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.localfilter dump : ]
			set sLocalFilter [ split $sLocalFilter : ]
			
			set sOrg [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.organization dump | ]
			set sOrg [ split $sOrg | ]
			
			set sPrj [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.project dump | ]
			set sPrj [ split $sPrj | ]
			
			set sLogin [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.login dump | ]
			set sLogin [ split $sLogin | ]
			
			set sKey [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.key dump | ]
			set sKey [ split $sKey | ]
			
			set sRevokes [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.revoke dump | ]
			set sRevoke [ split $sRevokes | ]
			
			set sStateUserOwnerValues [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.owner dump | ]
			set sStateUserOwnerValue [ split $sStateUserOwnerValues | ]
			
			set sStateUserReserveValues [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.reserve dump | ]
			set sStateUserReserveValue [ split $sStateUserReserveValues | ]
			
			set sStateUserMaturityValues [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.maturity dump | ]
			set sStateUserMaturityValue [ split $sStateUserMaturityValues | ]
			
			set sStateUserCategoryValues [ mql print $sPolicyRule "$sPol" select $sState.$sUserName.category dump | ]
			set sStateUserCategoryValue [ split $sStateUserCategoryValues | ]
			
			set iUserCnt [llength $sUsers]
			
			for {set i 0} {$i < $iUserCnt} {incr i} {
				set sUser [ lindex $sUsers $i ]
				set sOwner [string trim $sUser]
				
				set sUserRights [ lindex $sAccess $i ]
				if { $sUserRights == "all" } { 
					set sUserRights $lAccessStr 
				}
				
				set sUserRights [ split $sUserRights , ]
									
				set sArrangedRights ""					
				#KYB Start re-arranging access
				if { ($sUserRights == "all") || ($sUserRights == "none") } {
				} else {
					foreach iCnt2 $lAccessModes {
						set bFileAccess [ string tolower $iCnt2 ]
						foreach iCnt1 $sUserRights {
							if { $iCnt1 == $bFileAccess } {
								lappend sArrangedRights $iCnt2
							}
						}
					}
					set sUserRights $sArrangedRights					
				}
									
				set sUserOrgProj [ lindex $sOrg $i ]
				append sUserOrgProj " "
				set sUserPrj [ lindex $sPrj $i ]
				append sUserOrgProj $sUserPrj
				
				set sUserFilter [ lindex $sFilter $i ]
				set sUserLogin [ lindex $sLogin $i ]
				set sUserKey [ lindex $sKey $i ]
				set sUserRevoke [ lindex $sRevoke $i ]
				set sUserOwnerValue [ lindex $sStateUserOwnerValue $i ]
				set sUserReserveValue [ lindex $sStateUserReserveValue $i ]
				set sUserMaturityValue [ lindex $sStateUserMaturityValue $i ]
				set sUserCategoryValue [ lindex $sStateUserCategoryValue $i ]
				set sUserLocalFilter [ lindex $sLocalFilter $i ]

				set sBranch ""
				if { $sUserKey != ""  && $sState != "allstate" } {set sBranch [ mql print policy "$sPol" select $sState.signature\[$sUserKey\].branch dump ]}
				if {$sUserType == "owner" } {	
					set sOwner "Owner"
				} elseif { $sUserType == "public" } {
					set sOwner "Public"
				}
				lappend lData [ list $sSt $sOwner $sUserRights $sUserFilter $sUserOrgProj $sUserLogin $sUserKey $sUserRevoke $sUserOwnerValue $sUserReserveValue $sUserMaturityValue $sUserCategoryValue $sBranch $sUserLocalFilter]
			}
		}
    }
	#puts "$lData"
    return $lData
}
#End pPolicyDataWithLoginKey

proc pIsNoneUser { sDBAcc lDel } {
	set sSpaceChar " "
	set sCurrentAccess [ split $sDBAcc $sSpaceChar ]
	set lCurrentAccess [ llength $sCurrentAccess ]
	
	set sDelAccess [ split $lDel , ]
	set lDelAccess [ llength $sDelAccess ]
	
	if { $lDelAccess == $lCurrentAccess } {
		return "TRUE"
	} else {
		return "FALSE"
	}
}

proc pRemoveUser { sPolicyName sStateName sLoginVal sKeyVal sRevokeVal sOrgUserName } {
	global bAllState
		if {$sStateName == "allstate"} {
			if {$bAllState} {
				set sCmdInsert "allstate"
			} else {
				continue
			}
		} else {
			set sCmdInsert "state \134\"$sStateName\134\""
		}
	
		#Build MQL query using username,revoke,login,owner
		set pQueryStart ""

		if { $sRevokeVal == "TRUE" } {append pQueryStart " revoke "}
		if { $sOrgUserName == "Public" || $sOrgUserName == "Owner" || $sOrgUserName == "public" || $sOrgUserName == "owner" } {
			if { $sLoginVal == "TRUE" } {
				append pQueryStart " login $sOrgUserName "
			} else {
				append pQueryStart " $sOrgUserName "
			}
		} else {
			if { $sLoginVal == "TRUE" } {
				append pQueryStart " login \"$sOrgUserName\" "
			} else {
				append pQueryStart " user \"$sOrgUserName\" "
			}
		}
		if { $sKeyVal != "" } {append pQueryStart " key \"$sKeyVal\" "}
		
		set sCmd "mql mod policy \"$sPolicyName\" $sCmdInsert remove "
		append sCmd $pQueryStart
		append sCmd " all "
		pProcessMqlCmd Mod policy $sPolicyName $sCmd

	return 0;
}

#main
	set sMxVersion [mql get env MXVERSION]
    if {[string first "V6" $sMxVersion] >= 0} {
       set rAppend ""
	   if {[string range $sMxVersion 7 7] == "x"} {set rAppend ".1"}
       set sMxVersion [string range $sMxVersion 3 6]
	   if {$rAppend != ""} {append sMxVersion $rAppend}
    } else {
       set sMxVersion [join [lrange [split $sMxVersion .] 0 1] .]
    }

    set bScan [mql get env SPINNERSCANMODE]; #scan mode
    if {$bScan != "TRUE"} {set bScan FALSE}
    set bShowModOnly [mql get env SHOWMODONLY]
    set lFilesXLS [mql get env FILELIST]

    array set aPolicyRule [list policy Policy]
    set sSuffix [clock format [clock seconds] -format ".%m%d%y"]
    if {$bScan} {set sSuffix ".SCAN"}
    set sDelimit "\t"

    set sPolicyRuleAccess [mql get env ACCESSTYPE]
    if { $sPolicyRuleAccess == ""} {
       set lsPolicyRule [list policy]
    } else {
       regsub "access" $sPolicyRuleAccess "" sPolicyRule
       set lsPolicyRule [list $sPolicyRule]
    }
	
	set sSpinnerDirectory [mql get env SPINNERPATH]
	set bRevoke "FALSE"

		foreach sPolicy $lsPolicyRule {
			eval "set sListPolicy \[mql list \"$sPolicy\" * \]"
			set listPolicies [split $sListPolicy \n]
			
			set listFiles ""
			if {$lFilesXLS == ""} {
				set lFilesXLS [ glob -nocomplain "$sSpinnerDirectory/Business/$aPolicyRule($sPolicy)/*.xls" ]
			}
		
			foreach policyFilename $lFilesXLS {
			 
				set policyName [file rootname [file tail $policyFilename ]]
			    set policyName [Replace_backslash $policyName] 
				
				lappend listFiles $policyName
				
			}
			
			set listNames [ pCompareLists $listFiles $listPolicies ]
		
			set sPolicyExtra [ lindex $listNames 0 ]
			set sPolicyCommon [ lindex $listNames 1 ]
			
			foreach sPolicyName $sPolicyCommon {
			
			
			 set sNewPolicyName [Replace_bslash $sPolicyName] 	
			# IR Fix 317721
				set lPolicyFileData [ split [ pfile_read "$sNewPolicyName.xls" ] \n ]
				set nLineCount 0
				foreach sPolicyLine $lPolicyFileData {
					set sPolicyLineData [ split $sPolicyLine $sDelimit ]
					if { $sPolicyLineData == "" } {
						continue
					}
					if { $nLineCount == 0 } {
						set sFileHeader [ string tolower $sPolicyLineData ]						
						break
					}
					incr nLineCount
				}
			}
		}	
	
	#KYB Start - Handle association and group users	
	set lsAssociation [ pGetAssociationList ]
	set lsGroup [ pGetGroupList ]
		
		foreach sPolicyRule $lsPolicyRule {
			set iAccessError 0
			set sMsg_Log ""
			eval "set slPolicy \[mql list \"$sPolicyRule\" * \]"
			set lPolicies [split $slPolicy \n]
		   
			if {[mql get env SPINNERLOGFILE] != ""} {; # SpinnerAgent Hook
			   set bSpinnerAgent TRUE
			   set sOutFile "[mql get env SPINNERLOGFILE]"
			   set sLogFileError [mql get env SPINNERERRORLOG]
			} else {
			   set bSpinnerAgent FALSE    
			   set sOutFile "./logs/$aPolicyRule($sPolicyRule)Access$sSuffix.log"
			   file delete $sOutFile
			}
			
			set lFiles ""
			if {$lFilesXLS == ""} {set lFilesXLS [ glob -nocomplain "./Business/$aPolicyRule($sPolicyRule)/*.xls" ]}
			
			foreach filename $lFilesXLS {

			set name [file rootname [file tail $filename ]]
			 set newname [Replace_backslash $name] 	
				lappend lFiles $newname
				
			}
			
			set lNames [ pCompareLists $lFiles $lPolicies ]

			set sExtra [ lindex $lNames 0 ]
			set sCommon [ lindex $lNames 1 ]
						
			foreach sName $sCommon {
			
				set bAllState FALSE
				catch {set bAllState [mql print policy $sName select allstate dump]} sMsg
				pfile_write $sOutFile $sMsg_Log
				set sMsg_Log ""
				set sPolicyFile {}
				set sPolicyDB [ pPolicyDataWithLoginKey $sName ]
			# IR Fix 317721
			 set sNewName [Replace_bslash $sName] 	
				set lFileData [ split [ pfile_read "$sNewName.xls" ] \n ]
				puts $sName
				set nCount 0
			
			#Read all the data from policy file at once
			foreach sLine $lFileData {
				set sLineData [ split $sLine $sDelimit ]
				set nL [ llength $sLineData ]
				if { $sLineData == "" } {
					continue
				}
				if { $nCount == 0 } {
					set sHeader [ string tolower $sLineData ]
				} else {
					# process the data line!!
					set sState [ lindex $sLineData 0 ]
					set sOwner [ lindex $sLineData 1 ]

					set nPos 0
					set sRights {}
					set sPolicyNoRights {}
					set sFilter {}
					set sLocalFilter {}
					set sSecurityContext {}
					set sLogin {}
					set sKey {}
					set sStateUserOwner {}
					set sStateUserReserve {}
					set sStateUserMaturity {}
					set sStateUserCategory {}
					set sStateUserRevoke {}
					set sSigBranch {}
					foreach  i $sHeader j $sLineData {
						if { $nPos > 1 } {
							set bHasAccess [ string tolower $j ]
							if {$i == "filter"} {
								set sFilter $j
							} elseif { $bHasAccess == "y" } {
								lappend sRights $i
							} elseif { $bHasAccess == "" && $i != "key" } {
								lappend sPolicyNoRights $i
							} elseif { $i == "organization(any|single|ancestor|descendant)" } {
								lappend sSecurityContext $bHasAccess
							} elseif { $i == "project(any|single|ancestor|descendant)" } {
								lappend sSecurityContext $bHasAccess
							} elseif { $i == "login(boolean)" } {
								set sLogin $j
							} elseif { $i == "key" } {
								set sKey $j
							} elseif { $i == "owner" } {
								set sStateUserOwner $j
							} elseif { $i == "reserve(context)" } {
								set sStateUserReserve $j
							} elseif { $i == "maturity" } {
								set sStateUserMaturity $j
							} elseif { $i == "category" } {
								set sStateUserCategory $j
							} elseif { $i == "revoke(boolean)" } {
								set sStateUserRevoke $j
							} elseif { $i == "branch state" } {
								set sSigBranch $j
							} elseif {$i == "localfilter"} {
								set sLocalFilter $j
							}
						}
						incr nPos
					}
										
					if { $sRights == "" } { set sRights "none" }
					lappend sPolicyFile [ list $sState $sOwner $sRights $sFilter $sSecurityContext $sLogin $sKey $sStateUserOwner $sStateUserReserve $sStateUserMaturity $sStateUserCategory $sStateUserRevoke $sSigBranch $sPolicyNoRights $sLocalFilter ]
				}
				incr nCount
			}
			
			append sMsg_Log "\n# \[[clock format [clock seconds] -format %H:%M:%S]\] $aPolicyRule($sPolicyRule)Access '$sName':"
			puts -nonewline "\n$sPolicyRule $sName"
			
			foreach sEachPolicy $sPolicyFile {
				set sSt [ lindex $sEachPolicy 0 ]
				
				if {$sSt == "allstate"} {
						if {$bAllState} {
							set sInsert " allstate"
							set sCmdInsert "allstate"
						} else {
							continue
						}
				} else {
					set sInsert " state $sSt"
					set sCmdInsert "state \134\"$sSt\134\""
				}
				
				set sOwn [string trim [ lindex $sEachPolicy 1 ]]
				set sAcc [ lindex $sEachPolicy 2 ]
				set sFilter [ lindex $sEachPolicy 3 ]
				set sPolicyRoleType [ lindex $sEachPolicy 4 ]				
				set sPolicyLogin [ lindex $sEachPolicy 5 ]
				set sPolicyKey [ lindex $sEachPolicy 6 ]
				set sPolicyOwner [ lindex $sEachPolicy 7 ]
				set sPolicyReserve [ lindex $sEachPolicy 8 ]
				set sPolicyMaturity [ lindex $sEachPolicy 9 ]
				set sPolicyCategory [ lindex $sEachPolicy 10 ]
				set sPolicyRevoke [ lindex $sEachPolicy 11 ]
				set sPolicySigBranch [ lindex $sEachPolicy 12 ]
				set sPolicyBlankAccess [ lindex $sEachPolicy 13 ]
				if { $sFilter != "" } { set sFilter [pRegSubEscape $sFilter] }
				set sPolicyOrganization [ lindex $sPolicyRoleType 0 ]
				set sPolicyProject [ lindex $sPolicyRoleType 1 ]
				set sLocalFilter [ lindex $sEachPolicy 14 ]
				if { $sLocalFilter != "" } { set sLocalFilter [pRegSubEscape $sLocalFilter] }
				
				#Find actual user name
				set sUserStr $sOwn
				if { [string match "<<*>>" $sOwn] == 1 } {
					set startStr [split $sOwn <<]
					set endStr [lindex $startStr 2]
					set sUserStr [split $endStr >>]
					set sUserStr [lindex $sUserStr 0]
				}
				
				set isAssociationGroup [ pIsAssociationGroup $sOwn $lsAssociation $lsGroup ]
				
				#KYB - Build Start Query
				set pQueryStart ""
				if { $sPolicyRevoke == "TRUE" } {append pQueryStart " revoke "}
					
				if { $sOwn == "Public" || $sOwn == "Owner" || $sOwn == "public" || $sOwn == "owner" } {
					if { $sPolicyLogin == "TRUE" } {
						append pQueryStart " login $sOwn "
					} else {
						append pQueryStart " $sOwn "
					}
				} else {
					if { $sPolicyLogin == "TRUE" } {
						append pQueryStart " login \"$sOwn\" "
					} else {
						append pQueryStart " user \"$sOwn\" "
					}
				}
				
				set sOwnerQuery " $sPolicyOwner owner"
				set sReserveQuery " $sPolicyReserve reserve"
				set sMaturityQuery " $sPolicyMaturity maturity"
				set sCategoryQuery " $sPolicyCategory category"
					
				if { $sPolicyKey != "" } {append pQueryStart " key \"$sPolicyKey\" "}
				
				if { $sPolicyLogin == "" } {
					set sPolicyLogin "FALSE"
				}
				if { $sPolicyRevoke == "" } {
					set sPolicyRevoke "FALSE"
				}
				set nIndex [ lsearch -glob $sPolicyDB [list $sSt $sUserStr [list *] * * $sPolicyLogin $sPolicyKey $sPolicyRevoke * ] ]
				#puts "nIndex!!!!!!!!!!!!!!!!!!!!!!!!!!$nIndex"
				if { $nIndex == -1 } {
					if {$sUserStr == $sOwn} {
						set lAdd [ join $sAcc , ]
						
						#KYB Add user with parameters
						#Build MQL query using revoke,login,key params						
						set pQueryEnd ""
						
						if { ($sPolicyOrganization == "" && $sPolicyProject == "") || ($isAssociationGroup == "TRUE") } {
						} else {
							append pQueryEnd " $sPolicyOrganization organization $sPolicyProject project "
						}
						
						if {$sPolicyOwner != ""} {append pQueryEnd $sOwnerQuery}
							if {$sPolicyReserve != ""} {append pQueryEnd $sReserveQuery}
							if { $isAssociationGroup == "FALSE" } {
								if {$sPolicyMaturity != ""} {append pQueryEnd $sMaturityQuery}
								if {$sPolicyCategory != ""} {append pQueryEnd $sCategoryQuery}
							}
						if {$sFilter == "<NULL>" } {set sFilter ""}
						if {$sFilter != ""} { append pQueryEnd " filter \"$sFilter\" " }
						if {$sLocalFilter == "<NULL>"} {set sLocalFilter ""}
						if {$sLocalFilter != ""} { append pQueryEnd " localfilter \"$sLocalFilter\" " }

						if { $lAdd == "" } { set lAdd "none" }
						set sCmd "mql mod policy \"$sName\" $sCmdInsert add "
						append sCmd $pQueryStart
						append sCmd $lAdd
						append sCmd $pQueryEnd
						
						pProcessMqlCmd Mod policy $sName $sCmd
						
						#Update signature with branch
						if {$sPolicySigBranch != ""} {
							set sTemp [mql print policy $sName select state\[$sSt\].signature\[$sPolicyKey\] dump]
							if {$sTemp != ""} {
								set sCmd "mql mod policy \"$sName\" $sCmdInsert signature \"$sPolicyKey\" branch '$sPolicySigBranch'"
								pProcessMqlCmd Mod policy $sName $sCmd
							}
						}
					}
				} else {
					#KYB Check if user marked for deletion
					set iLenMinTwo [expr [string length $sOwn] -2]
					if {[string first "<<" $sOwn] >= 0 && [string first ">>" $sOwn] == $iLenMinTwo} {
						pRemoveUser $sName $sSt $sPolicyLogin $sPolicyKey $sPolicyRevoke $sUserStr
					} else {
						#KYB Check if user needs to be modified
						set sEachDBPolicy [ lindex $sPolicyDB $nIndex ]
						set sDBAcc [ lindex $sEachDBPolicy 2 ]
						set sDBFilter [ lindex $sEachDBPolicy 3 ]
						set sDBPolicyRoleType [ lindex $sEachDBPolicy 4 ]
						set sDBPolicyOrganization [ lindex $sDBPolicyRoleType 0 ]
						set sDBPolicyProject [ lindex $sDBPolicyRoleType 1 ]
						set sDBPolicySigBranch [ lindex $sEachDBPolicy 12 ]
						set sDBPolicyOwner [ lindex $sEachDBPolicy 8 ]
						set sDBPolicyReserve [ lindex $sEachDBPolicy 9 ]
						set sDBPolicyMaturity [ lindex $sEachDBPolicy 10 ]
						set sDBPolicyCategory [ lindex $sEachDBPolicy 11 ]
						set sDBLocalFilter [ lindex $sEachDBPolicy 13 ]
						
						set lReqAcc [ pCompareLists $sAcc $sDBAcc ]
						set lAdd [ join [ lindex $lReqAcc 0 ] , ]
						set lDelPreList [ lindex $lReqAcc 2 ]
						#set lDel [ join [ lindex $lReqAcc 2 ] , ]
						
						set lNewList {}						
												
						foreach lEachDel $lDelPreList {
						    set bFlag "FALSE"
							foreach sPolicyEachBlankAccess $sPolicyBlankAccess {
								if {$lEachDel == $sPolicyEachBlankAccess} {							
									set bFlag "TRUE"
									break
								}
							}
							
							if { $bFlag == "FALSE" } {
								lappend lNewList $lEachDel
							}							
						}
						
						set lDel [ join $lNewList , ]
						
						if { $sDBFilter != "" } { set sDBFilter [pRegSubEscape $sDBFilter] }
						if { $sDBLocalFilter != "" } { set sDBLocalFilter [pRegSubEscape $sDBLocalFilter] }
						
						#Build MQL query using revoke,login,key params
						set pQueryEnd ""
						
						if { ($sPolicyOrganization == "" && $sPolicyProject == "") || ($isAssociationGroup == "TRUE") } {
						} else {
							if {$sPolicyOrganization != $sDBPolicyOrganization} { append pQueryEnd " $sPolicyOrganization organization " }
							if {$sPolicyProject != $sDBPolicyProject} { append pQueryEnd " $sPolicyProject project " }
						}
						
						if {$sFilter == "<NULL>" && $sDBFilter != "" && $sFilter != $sDBFilter} {
							append pQueryEnd " remove filter "
						} elseif {$sFilter == "<NULL>" } {set sFilter ""}
						
						if {$sFilter != "" && $sFilter != "<NULL>" && $sFilter != $sDBFilter} {
							append pQueryEnd " filter \"$sFilter\" "
						} elseif {$sFilter == ""} {set sFilter $sDBFilter}
						
						if {$sLocalFilter == "<NULL>" && $sDBLocalFilter != "" && $sLocalFilter != $sDBLocalFilter} {
							append pQueryEnd " remove localfilter "
						} elseif {$sLocalFilter == "<NULL>"} {set sLocalFilter ""}
						
						if {$sLocalFilter != "" && $sLocalFilter != "<NULL>" && $sLocalFilter != $sDBLocalFilter} {
							append pQueryEnd " localfilter \"$sLocalFilter\" "
						} elseif {$sLocalFilter == "" } {set sLocalFilter $sDBLocalFilter}

						
						if {$sPolicyOwner != $sDBPolicyOwner && $sPolicyOwner != ""} {append pQueryEnd $sOwnerQuery}
						if {$sPolicyReserve != $sDBPolicyReserve && $sPolicyReserve != ""} {append pQueryEnd $sReserveQuery}
						if { $isAssociationGroup == "FALSE" } {
							if {($sPolicyMaturity != $sDBPolicyMaturity && $sPolicyMaturity != "")} {append pQueryEnd $sMaturityQuery}
							if {$sPolicyCategory != $sDBPolicyCategory && $sPolicyCategory != ""} {append pQueryEnd $sCategoryQuery}
						}
						
						if {([ llength $lAdd ] != 0 && $lAdd != "none") || $sFilter != $sDBFilter || $sLocalFilter != $sDBLocalFilter || $sPolicyOrganization != $sDBPolicyOrganization || $sPolicyProject != $sDBPolicyProject || ($sPolicyOwner != $sDBPolicyOwner && $sPolicyOwner != "") || ($sPolicyReserve != $sDBPolicyReserve && $sPolicyReserve != "") || ($sPolicyMaturity != $sDBPolicyMaturity) || ($sPolicyCategory != $sDBPolicyCategory )} {
							if { $lAdd == "" } { set lAdd "none" }
							set sCmd "mql mod policy \"$sName\" $sCmdInsert add "
							append sCmd $pQueryStart
							append sCmd $lAdd
							append sCmd $pQueryEnd
							pProcessMqlCmd Mod policy $sName $sCmd
						} else {
							puts -nonewline "."
						}
						
						#QHV MQLExport \"$lDel"\ replace with $lDel
						if { [ llength $lDel ] != 0 && $lDel != "none" } {
							set bIsNoneUser [ pIsNoneUser $sDBAcc $lDel ]			
							
							set sCmd "mql mod policy \"$sName\" $sCmdInsert remove "
							append sCmd $pQueryStart
							append sCmd $lDel
							#append sCmd $pQueryEnd

							if { $bIsNoneUser == "TRUE" } {
								pProcessMqlCmd Mod policy $sName $sCmd
								
								set pQueryEnd ""
								if { ($sPolicyOrganization == "" && $sPolicyProject == "") || ($isAssociationGroup == "TRUE") } {
								} else {
									append pQueryEnd " $sPolicyOrganization organization $sPolicyProject project "
								}
								
								if {$sPolicyOwner != ""} {append pQueryEnd $sOwnerQuery}
									if {$sPolicyReserve != ""} {append pQueryEnd $sReserveQuery}
									if { $isAssociationGroup == "FALSE" } {
										if {$sPolicyMaturity != ""} {append pQueryEnd $sMaturityQuery}
										if {$sPolicyCategory != ""} {append pQueryEnd $sCategoryQuery}
									}
								if {$sFilter == "<NULL>" } {set sFilter ""}
								if {$sFilter != ""} { append pQueryEnd " filter \"$sFilter\" " }
								if {$sLocalFilter == "<NULL>"} {set sLocalFilter ""}
								if {$sLocalFilter != ""} { append pQueryEnd " localfilter \"$sLocalFilter\" " }
								
								set sCmd "mql mod policy \"$sName\" $sCmdInsert add "
								append sCmd $pQueryStart
								append sCmd " none "
								append sCmd $pQueryEnd
							}
							pProcessMqlCmd Mod policy $sName $sCmd
						}
						
						#Update signature with branch
						if {$sPolicySigBranch != $sDBPolicySigBranch } {
							if {$sPolicySigBranch != ""} {
								set sTemp [mql print policy $sName select state\[$sSt\].signature\[$sPolicyKey\] dump]
								if {$sTemp != ""} {
									if {$sPolicySigBranch == "<NULL>"} {
										set sCmd "mql mod policy \"$sName\" $sCmdInsert signature \"$sPolicyKey\" remove branch"
										} else {
											set sCmd "mql mod policy \"$sName\" $sCmdInsert signature \"$sPolicyKey\" branch $sPolicySigBranch"
										}
									pProcessMqlCmd Mod policy $sName $sCmd
								}
							}
						}
					}
				}
			}
		}
	}
		
	pfile_write $sOutFile $sMsg_Log
	if {$bSpinnerAgent} {pfile_write $sOutFile ""}
	puts ""
	mql set env ACCESSERROR $iAccessError
}


