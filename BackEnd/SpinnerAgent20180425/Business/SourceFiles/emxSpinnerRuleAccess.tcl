#####################################################################*V62015x
#
# @progdoc      emxSpinnerAccess.tcl vMV6R2013 (Build 11.10.1)
#
# @Description: This is schema spinner that adds or modifies schema
#               rule access.  Invoked from program
#               'emxSpinnerAgent.tcl' but may be run separately.
#
# @Parameters:  None
#
# @Usage:       Run this program for an MQL command window w/data files in directories:
#               . (current dir)         emxSpinnerAgent.tcl, emxSpinnerRuleAccess.tcl programs
#               ./Business/Rule         Rule access data files from Bus Doc Generator program        
#
# @progdoc      Copyright (c) ENOVIA Inc., June 26, 2002
#
# @Originator:  Greg Inglis
#
########################################################################################
#
# @Modifications: Venkatesh Harikrishan 04/03/2006 - Fix for Incident 317721
# @Modifications: Medha TAMBE 09/23/2014 - V6R2015x GA Compatibility for Rule Access
#
########################################################################################
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

proc pProcessMqlCmd { sAction sType sName sMql } {
    global sMsg_Log iAccessError bScan sLogFileError bSpinnerAgent bMQLExtract sMQLExtractFileDir
    append sMsg_Log "# ACTION: $sAction $sType $sName\n"
	append sMsg_Log "$sMql\n"
	
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
            append sMsg_Log "# $sAction $sType $sName Successful.\n"
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

proc pGetAssociationList {} {
	set lAssociationList [split [mql list association] \n]
	return $lAssociationList
}

proc pGetGroupList {} {
	set lGroup [split [mql list group] \n]
	return $lGroup
}

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

proc pRemoveUser { sRuleName sLoginVal sKeyVal sRevokeVal sOrgUserName } {
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
		
		set sCmd "mql mod rule \"$sRuleName\" remove "
		append sCmd $pQueryStart
		append sCmd " all "
		pProcessMqlCmd Mod rule $sRuleName $sCmd

	return 0;
}
proc pRuleDataWithLoginKey { sPol } {
    global sPolicyRule 

# Major/Minor Mod - ION - 10/1/2012
    
       set lAccessModes [ list read modify delete checkout checkin schedule lock \
           unlock execute freeze thaw create revise majorrevise promote demote grant \
           enable disable override changename changetype changeowner changepolicy revoke \
           changevault fromconnect toconnect fromdisconnect todisconnect \
           viewform modifyform show approve reject ignore reserve unreserve ]
	
	   set lAccessStr "read,modify,delete,checkout,checkin,schedule,lock,unlock,execute,freeze,thaw,create,revise,majorrevise,promote,demote,grant,enable,disable,override,changename,changetype,changeowner,changepolicy,revoke,changevault,fromconnect,toconnect,fromdisconnect,todisconnect,viewform,modifyform,show,approve,reject,ignore,reserve,unreserve"
	
	
    set lData {}
    set sStates [list 999999]

    foreach sSt $sStates {
        set sStateCmdOwner "owneraccess"
        set sStateCmdPublic "publicaccess"
        set sStateCmdAccess "access"
        set sStateCmdFilter "filter"
		
			set sOwner owner
			eval "set sAccess \[mql print $sPolicyRule \"$sPol\" select $sStateCmdOwner dump \]"
			set sRights [ split [ string trim $sAccess ] , ]
			if { $sRights == "all" } {
				set sRights $lAccessStr
			} elseif { $sRights == "none" } {
				set sRights ""
			}
			
			   #KYB retrieve organization and project for owner
				set sStateOwners [ mql print $sPolicyRule "$sPol" select owner\[*\].owner dump | ]			
				set sStateOwner [ split $sStateOwners | ]
				set iLen [ llength $sStateOwner ]
				set sFilters [ mql print $sPolicyRule "$sPol" select owner\[*\].filter dump : ]			
				set sFilter [ split $sFilters : ]
				set sOwnerAccess [ mql print $sPolicyRule "$sPol" select owner\[*\].access dump | ]
				set sAccess [ split $sOwnerAccess | ]
			
				set sOrganizations [mql print $sPolicyRule "$sPol" select owner\[*\].organization dump | ]
				set sOrganization [ split $sOrganizations | ]
				set sProjects [mql print $sPolicyRule "$sPol" select owner\[*\].project dump | ]
				set sProject [ split $sProjects | ]
				
				#KYB retrieve login and key for state owner
				set sLogins [mql print $sPolicyRule "$sPol" select owner\[*\].login dump | ]
				set sLogin [ split $sLogins | ]
				set sKeys [mql print $sPolicyRule "$sPol" select owner\[*\].key dump | ]
				set sKey [ split $sKeys | ]
				
				#KYB retrieve revoke for state for owner
				set sRevokes [mql print $sPolicyRule "$sPol" select owner\[*\].revoke dump | ]
				set sRevoke [ split $sRevokes | ]
				
				set sStateOwnerOwners [mql print $sPolicyRule "$sPol" select owner\[*\].owner dump | ]
				set sStateOwnerOwner [ split $sStateOwnerOwners | ]
				
				set sStateOwnerReserve [mql print $sPolicyRule "$sPol" select owner\[*\].reserve dump | ]
				set sStateOwnerReserveVal [ split $sStateOwnerReserve | ]
				set sStateOwnerMaturity [mql print $sPolicyRule "$sPol" select owner\[*\].maturity dump | ]
				set sStateOwnerMaturityVal [ split $sStateOwnerMaturity | ]
				set sStateOwnerCategory [mql print $sPolicyRule "$sPol" select owner\[*\].category dump | ]
				set sStateOwnerCategoryVal [ split $sStateOwnerCategory | ]
				set sLocalFilters [ mql print $sPolicyRule "$sPol" select owner\[*\].localfilter dump ]		
				set sLocalFilter [ split $sLocalFilters , ]
				
				for {set i 0} {$i < $iLen} {incr i} {					
					set sRight [ lindex $sAccess $i ]
					if { $sRight == "all" } { 
						set sRight $lAccessStr 
					}
					set sRights [ split $sRight , ]

					set sArrangedRights ""
					#KYB Start re-arranging access
					if { ($sRights == "all") || ($sRights == "none") } {
					} else {
						foreach iCnt2 $lAccessModes {
							set bFileAccess [ string tolower $iCnt2 ]
							foreach iCnt1 $sRights {
								if { $iCnt1 == $bFileAccess } {
									lappend sArrangedRights $iCnt2
								}
							}
						}
						set sRights $sArrangedRights					
					}				
					
					set sOrgProj [ lindex $sOrganization $i ]			
					append sOrgProj " "				
					set sPrj [ lindex $sProject $i ]
					append sOrgProj $sPrj
					
					set sFilterEach [ lindex $sFilter $i ]					
					set sLoginEach [ lindex $sLogin $i ]
					set sKeyEach [ lindex $sKey $i ]						
					set sRevokeEach [ lindex $sRevoke $i ]
					set sStateOwnerEach [ lindex $sStateOwnerOwner $i ]
					set sStateOwnerReserveEach [ lindex $sStateOwnerReserveVal $i ]
					set sStateOwnerMaturityEach [ lindex $sStateOwnerMaturityVal $i ]
					set sStateOwnerCategoryEach [ lindex $sStateOwnerCategoryVal $i ]
					set sLocalFilterEach [ lindex $sLocalFilter $i ]
					
					lappend lData [ list owner $sRights $sFilterEach $sOrgProj $sLoginEach $sKeyEach $sRevokeEach $sStateOwnerEach $sStateOwnerReserveEach $sStateOwnerMaturityEach $sStateOwnerCategoryEach $sLocalFilterEach ]
				}				
		
			
			set sOwner public
			eval "set sAccess \[mql print $sPolicyRule \"$sPol\" select $sStateCmdPublic dump \]"
			set sRights [ split [ string trim $sAccess ] , ]
			if { $sRights == "all" } {
				set sRights $lAccessStr
			} elseif { $sRights == "none" } {
				set sRights ""
			}
			
			#KYB retrieve organization and project for public
				set sStatePublic [ mql print $sPolicyRule "$sPol" select public\[*\].owner dump | ]			
				set sStateP [ split $sStatePublic | ]
				set iLen [ llength $sStateP ]
				set sFilters [ mql print $sPolicyRule "$sPol" select public\[*\].filter dump : ]			
				set sFilter [ split $sFilters : ]
				set sOwnerAccess [mql print $sPolicyRule "$sPol" select public\[*\].access dump | ]
				set sAccess [ split $sOwnerAccess | ]
			
				set sOrganizations [mql print $sPolicyRule "$sPol" select public\[*\].organization dump | ]
				set sOrganization [ split $sOrganizations | ]
				set sProjects [mql print $sPolicyRule "$sPol" select public\[*\].project dump | ]
				set sProject [ split $sProjects | ]
				
				#KYB retrieve login and key for state public
				set sLogins [mql print $sPolicyRule "$sPol" select public\[*\].login dump | ]
				set sLogin [ split $sLogins | ]
				set sKeys [mql print $sPolicyRule "$sPol" select public\[*\].key dump | ]
				set sKey [ split $sKeys | ]
				#KYB retrieve revoke for state for public
				set sRevokes [mql print $sPolicyRule "$sPol" select public\[*\].revoke dump | ]
				set sRevoke [ split $sRevokes | ]
				set sStatePublicOwners [mql print $sPolicyRule "$sPol" select public\[*\].owner dump | ]
				set sStatePublicOwner [ split $sStatePublicOwners | ]
				
				set sStatePublicReserve [mql print $sPolicyRule "$sPol" select public\[*\].reserve dump | ]
				set sStatePublicReserveVals [ split $sStatePublicReserve | ]
				set sStatePublicMaturity [mql print $sPolicyRule "$sPol" select public\[*\].maturity dump | ]
				set sStatePublicMaturityVals [ split $sStatePublicMaturity | ]
				set sStatePublicCategory [mql print $sPolicyRule "$sPol" select public\[*\].category dump | ]
				set sStatePublicCategoryVals [ split $sStatePublicCategory | ]
				set sLocalFilters [ mql print $sPolicyRule "$sPol" select public\[*\].localfilter dump : ]		
				set sLocalFilter [ split $sLocalFilters : ]
				
				for {set i 0} {$i < $iLen} {incr i} {					
					set sRight [ lindex $sAccess $i ]
					if { $sRight == "all" } {
						set sRight $lAccessStr 
					}
					set sRights [ split $sRight , ]						
					
					set sArrangedRights ""					
					#KYB Start re-arranging access
					if { ($sRights == "all") || ($sRights == "none") } {
					} else {
						foreach iCnt2 $lAccessModes {
							set bFileAccess [ string tolower $iCnt2 ]
							foreach iCnt1 $sRights {
								if { $iCnt1 == $bFileAccess } {
									lappend sArrangedRights $iCnt2
								}
							}
						}
						set sRights $sArrangedRights					
					}
					
					set sOrgProj [ lindex $sOrganization $i ]			
					append sOrgProj " "				
					set sPrj [ lindex $sProject $i ]
					append sOrgProj $sPrj
					
					set sFilterEach [ lindex $sFilter $i ]					
					set sLoginEach [ lindex $sLogin $i ]
					set sKeyEach [ lindex $sKey $i ]	
					set sRevokeEach [ lindex $sRevoke $i ]
					set sStatePublicOwnerEach [ lindex $sStatePublicOwner $i ]
					set sStatePublicReserveEach [ lindex $sStatePublicReserveVals $i ]
					set sStatePublicMaturityEach [ lindex $sStatePublicMaturityVals $i ]
					set sStatePublicCategoryEach [ lindex $sStatePublicCategoryVals $i ]
					set sLocalFilterEach [ lindex $sLocalFilter $i ]	
					
					lappend lData [ list public $sRights $sFilterEach $sOrgProj $sLoginEach $sKeyEach $sRevokeEach $sStatePublicOwnerEach $sStatePublicReserveEach $sStatePublicMaturityEach $sStatePublicCategoryEach $sLocalFilterEach ]
				}        
			
			#KYB Retrive users data for users
				set sUser [ mql print $sPolicyRule "$sPol" select user.user dump | ]
				set sUsers [ split $sUser | ]

				set sAccess [ mql print $sPolicyRule "$sPol" select user.access dump | ]
				set sAccess [ split $sAccess | ]
				
				set sFilter [ mql print $sPolicyRule "$sPol" select user.filter dump : ]
				set sFilter [ split $sFilter : ]
				
				set sOrg [ mql print $sPolicyRule "$sPol" select user.organization dump | ]
				set sOrg [ split $sOrg | ]
				
				set sPrj [ mql print $sPolicyRule "$sPol" select user.project dump | ]
				set sPrj [ split $sPrj | ]
				
				set sLogin [ mql print $sPolicyRule "$sPol" select user.login dump | ]
				set sLogin [ split $sLogin | ]
				
				set sKey [ mql print $sPolicyRule "$sPol" select user.key dump | ]
				set sKey [ split $sKey | ]
				
				set sRevokes [ mql print $sPolicyRule "$sPol" select user.revoke dump | ]
				set sRevoke [ split $sRevokes | ]
				
				set sStateUserOwnerValues [ mql print $sPolicyRule "$sPol" select user.owner dump | ]
				set sStateUserOwnerValue [ split $sStateUserOwnerValues | ]
				
				set sStateUserReserveValues [ mql print $sPolicyRule "$sPol" select user.reserve dump | ]
				set sStateUserReserveValue [ split $sStateUserReserveValues | ]
				
				set sStateUserMaturityValues [ mql print $sPolicyRule "$sPol" select user.maturity dump | ]
				set sStateUserMaturityValue [ split $sStateUserMaturityValues | ]
				
				set sStateUserCategoryValues [ mql print $sPolicyRule "$sPol" select user.category dump | ]
				set sStateUserCategoryValue [ split $sStateUserCategoryValues | ]
				
				set sLocalFilter [ mql print $sPolicyRule "$sPol" select user.localfilter dump : ]
				set sLocalFilter [ split $sLocalFilter : ]
				
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

					lappend lData [ list $sOwner $sUserRights $sUserFilter $sUserOrgProj $sUserLogin $sUserKey $sUserRevoke $sUserOwnerValue $sUserReserveValue $sUserMaturityValue $sUserCategoryValue $sUserLocalFilter ]
				}
    }
    return $lData
}
#End pRuleDataWithLoginKey

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

# Major/Minor Check
    set bMm "TRUE"
		
    set bScan [mql get env SPINNERSCANMODE]; #scan mode
    if {$bScan != "TRUE"} {set bScan FALSE}
    set bShowModOnly [mql get env SHOWMODONLY]
    set lFilesXLS [mql get env FILELIST]

    array set aPolicyRule [list rule Rule]
    set sSuffix [clock format [clock seconds] -format ".%m%d%y"]
    if {$bScan} {set sSuffix ".SCAN"}
    set sDelimit "\t"

    set sPolicyRuleAccess [mql get env ACCESSTYPE]
    if { $sPolicyRuleAccess == ""} {
       set lsPolicyRule [list rule]
    } else {
       regsub "access" $sPolicyRuleAccess "" sPolicyRule
       set lsPolicyRule [list $sPolicyRule]
    }
	
	set sSpinnerDirectory [mql get env SPINNERPATH]

	foreach sRule $lsPolicyRule {
		eval "set sListPolicy \[mql list \"$sRule\" * \]"
		set listPolicies [split $sListPolicy \n]
		
		set listFiles ""
		if {$lFilesXLS == ""} {
			set lFilesXLS [ glob -nocomplain "$sSpinnerDirectory/Business/$aPolicyRule($sRule)/*.xls" ]
		}
		foreach ruleFilename $lFilesXLS {
			set ruleName [file rootname [file tail $ruleFilename ]]
			lappend listFiles $ruleName
		}
		
		set listNames [ pCompareLists $listFiles $listPolicies ]
	
		set sRuleCommon [ lindex $listNames 1 ]
		
		foreach sRuleName $sRuleCommon {
		# IR Fix 317721
			set lRuleFileData [ split [ pfile_read "$sRuleName.xls" ] \n ]
			set nLineCount 0
			foreach sRuleLine $lRuleFileData {
				set sRuleLineData [ split $sRuleLine $sDelimit ]
				if { $sRuleLineData == "" } {
					continue
				}
				if { $nLineCount == 0 } {
					set sFileHeader [ string tolower $sRuleLineData ]						
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
			lappend lFiles $name
		}
		
		set lNames [ pCompareLists $lFiles $lPolicies ]

		set sExtra [ lindex $lNames 0 ]
		set sCommon [ lindex $lNames 1 ]
		
		foreach sName $sCommon {
			pfile_write $sOutFile $sMsg_Log
			set sMsg_Log ""
			set sRuleFile {}
			
			#Get all the users and their data from DB at once
			set sRuleDB [ pRuleDataWithLoginKey $sName ]
			# IR Fix 317721
			set lFileData [ split [ pfile_read "$sName.xls" ] \n ]
			set nCount 0
			
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
					set sOwner [ lindex $sLineData 1 ]						
					if { $sOwner == "Owner" || $sOwner == "owner" || $sOwner == "Public" || $sOwner == "public" } {
						set sOwner [ string tolower $sOwner ]
					}

					set nPos 0
					set sRights {}
					set sRuleNoRights {}
					set sFilter {}
					set sSecurityContext {}
					set sLogin {}
					set sKey {}	
					set sUserOwner {}
					set sUserReserve {}
					set sUserMaturity {}
					set sUserCategory {}
					set sUserRevoke {}
					set sLocalFilter {}
					foreach  i $sHeader j $sLineData {
						if { $nPos > 0 } {
							set bHasAccess [ string tolower $j ]
							if {$i == "filter"} {
								set sFilter $j
							} elseif { $bHasAccess == "y"} {
								lappend sRights $i
							} elseif { $bHasAccess == "" && $i != "key" } {
								lappend sRuleNoRights $i
							} elseif { $i == "organization(any|single|ancestor|descendant)" } {
								lappend sSecurityContext $bHasAccess
							} elseif { $i == "project(any|single|ancestor|descendant)" } {
								lappend sSecurityContext $bHasAccess
							} elseif { $i == "login(boolean)" } {
								set sLogin $j
							} elseif { $i == "key" } {
								set sKey $j
							} elseif { $i == "owner" } {
								set sUserOwner $j
							} elseif { $i == "reserve(context)" } {
								set sUserReserve $j
							} elseif { $i == "maturity" } {
								set sUserMaturity $j
							} elseif { $i == "category" } {
								set sUserCategory $j
							} elseif { $i == "revoke(boolean)" } {
								set sUserRevoke $j
							} elseif {$i == "localfilter"} {
								set sLocalFilter $j
							}
						}
						incr nPos
					}
					if { $sRights == "" } { set sRights "none" }
					
					lappend sRuleFile [ list $sOwner $sRights $sFilter $sSecurityContext $sLogin $sKey $sUserRevoke $sUserOwner $sUserReserve $sUserMaturity $sUserCategory $sRuleNoRights $sLocalFilter]
				}
				incr nCount
			}
			
			foreach sEachRule $sRuleFile {
				set sOwn [string trim [ lindex $sEachRule 0 ]]
				set sAcc [ lindex $sEachRule 1 ]
				set sFilter [ lindex $sEachRule 2 ]
				if { $sFilter != "" } { set sFilter [pRegSubEscape $sFilter] }
				set sRoleType [ lindex $sEachRule 3 ]				
				set sRuleLogin [ lindex $sEachRule 4 ]
				set sRuleKey [ lindex $sEachRule 5 ]
				set sRuleRevoke [ lindex $sEachRule 6 ]
				set sRuleOwner [ lindex $sEachRule 7 ]
				set sRuleReserve [ lindex $sEachRule 8 ]
				set sRuleMaturity [ lindex $sEachRule 9 ]
				set sRuleCategory [ lindex $sEachRule 10 ]
				set sRuleBlankAccess [ lindex $sEachRule 11 ]
				set sLocalFilter [ lindex $sEachRule 12 ]
				if { $sLocalFilter != "" } { set sLocalFilter [pRegSubEscape $sLocalFilter] }
				set sRuleOrganization [ lindex $sRoleType 0 ]
				set sRuleProject [ lindex $sRoleType 1 ]
				#Find actual user name
				set sUserStr $sOwn
				if { [string match "<<*>>" $sOwn] == 1 } {
					set startStr [split $sOwn <<]
					set endStr [lindex $startStr 2]
					set sUserStr [split $endStr >>]
					set sUserStr [lindex $sUserStr 0]
				}
				
				set isAssociationGroup [ pIsAssociationGroup $sOwn $lsAssociation $lsGroup ]
				
				#Build MQL query using revoke,login,key params
				set pQueryStart ""
				
				set sOwnerQuery " $sRuleOwner owner"
				set sReserveQuery " $sRuleReserve reserve"
				set sMaturityQuery " $sRuleMaturity maturity"
				set sCategoryQuery " $sRuleCategory category"
				
				if { $sRuleRevoke == "TRUE" } {append pQueryStart " revoke "}
				if { $sOwn == "Public" || $sOwn == "Owner" || $sOwn == "public" || $sOwn == "owner" } {
					if { $sRuleLogin == "TRUE" } {
						append pQueryStart " login $sOwn "
					} else {
						append pQueryStart " $sOwn "
					}								
				} else {
					if { $sRuleLogin == "TRUE" } {
						append pQueryStart " login \"$sOwn\" "
					} else {
						append pQueryStart " user \"$sOwn\" "
					}
				}						
				if { $sRuleKey != "" } {append pQueryStart " key $sRuleKey "}
				
				if { $sRuleLogin == "" } {
					set sRuleLogin "FALSE"
				}
				if { $sRuleRevoke == "" } {
					set sRuleRevoke "FALSE"
				}
				
				set nIndex [ lsearch -glob $sRuleDB [list $sUserStr [list *] * * $sRuleLogin $sRuleKey $sRuleRevoke * ] ]
				if { $nIndex == -1 } {
					set lAdd [ join $sAcc , ]
					set pQueryEnd ""
					#KYB Add user with parameters
					#Build MQL query using revoke,login,key params
					if {$sRuleOwner != ""} {append pQueryEnd $sOwnerQuery}
					if {$sRuleReserve != ""} {append pQueryEnd $sReserveQuery}
					if { $isAssociationGroup == "FALSE" } {
						if {$sRuleMaturity != ""} {append pQueryEnd $sMaturityQuery}
						if {$sRuleCategory != ""} {append pQueryEnd $sCategoryQuery}
					}
					
					if { ($sRuleOrganization == "" && $sRuleProject == "") || ($isAssociationGroup == "TRUE") } {
					} else {
						append pQueryEnd " $sRuleOrganization organization $sRuleProject project "
					}
					
					if {$sFilter == "<NULL>" } {set sFilter ""}
					if {$sFilter != ""} { append pQueryEnd " filter \"$sFilter\" " }
					if {$sLocalFilter == "<NULL>"} {set sLocalFilter ""}
					if {$sLocalFilter != ""} { append pQueryEnd " localfilter \"$sLocalFilter\" " }
					if { $lAdd == "" } { set lAdd "none" }
					set sCmd "mql mod rule \"$sName\" add "
					append sCmd $pQueryStart
					append sCmd $lAdd
					append sCmd $pQueryEnd
					
					pProcessMqlCmd Mod rule $sName $sCmd					
				} else {
					#KYB Check if user marked for deletion
					set iLenMinTwo [expr [string length $sOwn] -2]
					if {[string first "<<" $sOwn] >= 0 && [string first ">>" $sOwn] == $iLenMinTwo} {
						pRemoveUser $sName $sRuleLogin $sRuleKey $sRuleRevoke $sUserStr
					} else {
						#KYB Check if user needs to be modified
						set sEachDBRule [ lindex $sRuleDB $nIndex ]
						set sDBAcc [ lindex $sEachDBRule 1 ]
						set sDBFilter [ lindex $sEachDBRule 2 ]
						set sDBRuleRoleType [ lindex $sEachDBRule 3 ]
						set sDBRuleOrganization [ lindex $sDBRuleRoleType 0 ]
						set sDBRuleProject [ lindex $sDBRuleRoleType 1 ]
						
						set sDBRuleOwner [ lindex $sEachDBRule 7 ]
						set sDBRuleReserve [ lindex $sEachDBRule 8 ]
						set sDBRuleMaturity [ lindex $sEachDBRule 9 ]
						set sDBRuleCategory [ lindex $sEachDBRule 10 ]
						set sDBLocalFilter [ lindex $sEachDBRule 11 ]
						
						set lReqAcc [ pCompareLists $sAcc $sDBAcc ]
						set lAdd [ join [ lindex $lReqAcc 0 ] , ]
						set lDelPreList [ lindex $lReqAcc 2 ]
						#set lDel [ join [ lindex $lReqAcc 2 ] , ]
						
						set lNewList {}						
												
						foreach lEachDel $lDelPreList {
						    set bFlag "FALSE"
							foreach sRuleEachBlankAccess $sRuleBlankAccess {
								if {$lEachDel == $sRuleEachBlankAccess} {							
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
						
						if { ($sRuleOrganization == "" && $sRuleProject == "") || ($isAssociationGroup == "TRUE") } {
						} else {
							if {$sRuleOrganization != $sDBRuleOrganization} { append pQueryEnd " $sRuleOrganization organization " }
							if {$sRuleProject != $sDBRuleProject} { append pQueryEnd " $sRuleProject project " }
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

						
						if {$sRuleOwner != $sDBRuleOwner && $sRuleOwner != ""} {append pQueryEnd $sOwnerQuery}
						if {$sRuleReserve != $sDBRuleReserve && $sRuleReserve != ""} {append pQueryEnd $sReserveQuery}
						if { $isAssociationGroup == "FALSE" } {
							if {($sRuleMaturity != $sDBRuleMaturity && $sRuleMaturity != "")} {append pQueryEnd $sMaturityQuery}
							if {$sRuleCategory != $sDBRuleCategory && $sRuleCategory != ""} {append pQueryEnd $sCategoryQuery}
						}
						
						if {([ llength $lAdd ] != 0 && $lAdd != "none")|| $sFilter != $sDBFilter || $sLocalFilter != $sDBLocalFilter || $sRuleOrganization != $sDBRuleOrganization || $sRuleProject != $sDBRuleProject || $sRuleOwner != $sDBRuleOwner || $sRuleReserve != $sDBRuleReserve || $sRuleMaturity != $sDBRuleMaturity || $sRuleCategory != $sDBRuleCategory} {
							if { $lAdd == "" } { set lAdd "none" }
							set sCmd "mql mod rule \"$sName\" add "
							append sCmd $pQueryStart
							append sCmd $lAdd
							append sCmd $pQueryEnd
							pProcessMqlCmd Mod rule $sName $sCmd							
						} else {
							puts -nonewline "."
						}
						
						#QHV MQLExport \"$lDel"\ replace with $lDel
						if { [ llength $lDel ] != 0 && $lDel != "none" } {
							set bIsNoneUser [ pIsNoneUser $sDBAcc $lDel ]			
							
							set sCmd "mql mod rule \"$sName\" remove "
							append sCmd $pQueryStart
							append sCmd $lDel
							#append sCmd $pQueryEnd

							if { $bIsNoneUser == "TRUE" } {
								pProcessMqlCmd Mod rule $sName $sCmd
								set pQueryEnd ""
								
								if {$sRuleOwner != ""} {append pQueryEnd $sOwnerQuery}
								if {$sRuleReserve != ""} {append pQueryEnd $sReserveQuery}
								if { $isAssociationGroup == "FALSE" } {
									if {$sRuleMaturity != ""} {append pQueryEnd $sMaturityQuery}
									if {$sRuleCategory != ""} {append pQueryEnd $sCategoryQuery}
								}
								
								if { ($sRuleOrganization == "" && $sRuleProject == "") || ($isAssociationGroup == "TRUE") } {
								} else {
									append pQueryEnd " $sRuleOrganization organization $sRuleProject project "
								}
								
								if {$sFilter == "<NULL>" } {set sFilter ""}
								if {$sFilter != ""} { append pQueryEnd " filter \"$sFilter\" " }
								if {$sLocalFilter == "<NULL>"} {set sLocalFilter ""}
								if {$sLocalFilter != ""} { append pQueryEnd " localfilter \"$sLocalFilter\" " }
								
								set sCmd "mql mod rule \"$sName\" add "
								append sCmd $pQueryStart
								append sCmd " none "
								append sCmd $pQueryEnd
							}
							pProcessMqlCmd Mod rule $sName $sCmd
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

