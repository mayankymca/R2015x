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
    set fileid [open $filename "w+"]
    puts $fileid $data
    close $fileid
  }]
}
#End pfile_write

#************************************************************************
# Procedure:   pFormatSpinnerWithLoginKey
#
# Description: Procedure to format data for spinner file.
#
# Parameters:  The data to format.
#
# Returns:     Nothing
#************************************************************************
    proc pFormatSpinnerWithLoginKey { lData sHead sType } {    
        global lAccessModes
        global sPositive
        global sNegative
        global bTemplate
     
        set sDelimit "\t"
        set sFormat ""
    
        if { [ llength $lData ] == 0 && !$bTemplate } {
            append sFormat "No Data"
            return $sFormat
        }
    
        append sFormat "State"
        append sFormat "${sDelimit}User"		
		
		append sFormat "${sDelimit}Login(boolean)"
		append sFormat "${sDelimit}Key"
		append sFormat "${sDelimit}Revoke(boolean)"
    
        # construct the access headers
        foreach sMode $lAccessModes {
            append sFormat "$sDelimit$sMode"
        }
        append sFormat "${sDelimit}Filter"
		append sFormat "${sDelimit}LocalFilter"
		append sFormat "${sDelimit}Branch State"
	
        append sFormat "\n"
    
        foreach line $lData {
            if { $line == "" } {
                continue
            }			
		
            set sPolicyDetails [ lindex $line 0 ]
            set sPolicyData [ lindex $line 1 ]
            set sFilter [ lindex $sPolicyData 1 ]

			# KYB Start V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy
			set sPolicySecurityContext [ lindex $sPolicyData 2 ]			
			set sSecurityContext [ split [ lindex $sPolicySecurityContext 0 ] , ]
			set sPolicyOrg [ lindex $sSecurityContext 0 ]
			set sPolicyProject [ lindex $sSecurityContext 1 ]
			# KYB End V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy
			
            set sLeft [ split [ lindex $line 0 ] , ]
            set sOwner [ lindex $sLeft 2 ]
            set sLeft [ split [ lindex $sLeft 0 ] | ]
            set sPolicy [ lindex $sLeft 0 ]
            set sState [ lindex $sLeft 2 ]
            set sRights [ lindex $sPolicyData 0 ]
			
			#KYB for login and key
			set sLogin [ lindex $sPolicyData 3 ] 
			set sKey [ lindex $sPolicyData 4 ] 
			
			#KYB for V6R2015x
			set sOwnerVal [ lindex $sPolicyData 5 ]
			set sReserveVal [ lindex $sPolicyData 6 ]
			set sMaturityVal [ lindex $sPolicyData 7 ]
			set sCategoryVal [ lindex $sPolicyData 8 ]
			set sRevoke [ lindex $sPolicyData 9 ]
			set sBranch [ lindex $sPolicyData 10 ]
			set sLocalFilter [ lindex $sPolicyData 11 ]			
            append sFormat "$sState"
            append sFormat "$sDelimit$sOwner"
						
			append sFormat "$sDelimit$sLogin"
			append sFormat "$sDelimit$sKey"
			append sFormat "$sDelimit$sRevoke"
			
			if { $sRights == "all" } {
                set sNegativeValue $sPositive
            } else {
                set sNegativeValue $sNegative
            }

            foreach sMode $lAccessModes {
                set sMode [string tolower $sMode]
				
				# KYB Start V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy
				if { $sMode != "organization(any|single|ancestor|descendant)" && $sMode != "project(any|single|ancestor|descendant)" && $sMode != "owner" && $sMode != "reserve(context)" && $sMode != "maturity" && $sMode != "category" } {
					if { [ lsearch $sRights $sMode ] == -1 } {
						append sFormat "$sDelimit$sNegativeValue"
					} else {
						append sFormat "$sDelimit$sPositive"
					}
				   }
				
				if { $sMode == "organization(any|single|ancestor|descendant)" } {					
					append sFormat "$sDelimit$sPolicyOrg"
				} elseif {$sMode == "project(any|single|ancestor|descendant)"} {
					append sFormat "$sDelimit$sPolicyProject"
				} elseif {$sMode == "owner"} {
					append sFormat "$sDelimit$sOwnerVal"
				} elseif {$sMode == "reserve(context)"} {
					append sFormat "$sDelimit$sReserveVal"
				} elseif {$sMode == "maturity"} {
					append sFormat "$sDelimit$sMaturityVal"
				} elseif {$sMode == "category"} {
					append sFormat "$sDelimit$sCategoryVal"
				}
				# KYB End V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy
            }
            append sFormat "$sDelimit$sFilter"
			append sFormat "$sDelimit$sLocalFilter"
			append sFormat "$sDelimit$sBranch"
			append sFormat "\n"
        }    

        return $sFormat
    }
#End pFormatSpinnerWithLoginKey

# Main
   set sFilter [mql get env 1]
   set bTemplate [mql get env 2]
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

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

	set sMxVersion [mql get env MXVERSION]
   if {[string first "V6" $sMxVersion] >= 0} {
      set rAppend ""
	  if {[string range $sMxVersion 7 7] == "x"} {set rAppend ".1"}
      set sMxVersion [string range $sMxVersion 3 6]
	  if {$rAppend != ""} {append sMxVersion $rAppend}
   } else {
      set sMxVersion [join [lrange [split $sMxVersion .] 0 1] .]
   }
# Major/Minor Check - ION - 10/1/2011
   set bMm "TRUE"
   
    set sSpinnerPath [mql get env SPINNERPATH]
    if {$sSpinnerPath == ""} {
       set sOS [string tolower $tcl_platform(os)];
       set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
       
       if { [string tolower [string range $sOS 0 5]] == "window" } {
          set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
       } else {
          set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
       }
       file mkdir "$sSpinnerPath/Business/Policy"
    }
	 
   set lAccessModes [ list Read Modify Delete Checkout Checkin Schedule Lock \
   Unlock Execute Freeze Thaw Create Revise MajorRevise Promote Demote Grant \
   Enable Disable Override ChangeName ChangeType ChangeOwner ChangePolicy Revoke \
   ChangeVault FromConnect ToConnect FromDisconnect ToDisconnect \
   ViewForm Modifyform Show Approve Reject Ignore Reserve Unreserve Organization(any|single|ancestor|descendant) Project(any|single|ancestor|descendant) Owner Reserve(Context) Maturity Category ]

    set sPositive Y
    set sNegative "-"

    set lPolicy [split [mql list policy $sFilter] \n]

		foreach sPol $lPolicy {
		   set bPass TRUE
		   if {$sMxVersion > 8.9} {
			  set sModDate [mql print policy $sPol select modified dump]
			  set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
			  if {$sModDateMin != "" && $sModDate < $sModDateMin} {
				 set bPass FALSE
			  } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
				 set bPass FALSE
			  }
		   }
			
		   if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print policy $sPol select property\[SpinnerAgent\] dump] != "")} {
			  set sStates [ split [ mql print policy $sPol select state dump | ] | ]
			  set bAllstate FALSE
			  set bAllstate [ mql print policy $sPol select allstate dump ]
			  if {$bAllstate && $sStates != [list ]} {lappend sStates "allstate"}
			  set sStOrder 0
			  if {!$bTemplate} {
				 foreach sSt $sStates {
					 if {$sSt == "allstate"} {
						# Extract data for allstate for owner
						set sOwners [ split [ string trim [ mql print policy $sPol select allstate.owner\[*\].owner dump | ] ] | ]
						set sAccess [ split [ string trim [ mql print policy $sPol select allstate.owner\[*\].access dump | ] ] | ]
						set sOrg [ split [ mql print policy $sPol select allstate.owner\[*\].organization dump ] , ]
						set sProject [ split [ mql print policy $sPol select allstate.owner\[*\].project dump ] , ]
						append sOrgProj $sProject
						set sFilter [ split [ mql print policy $sPol select allstate.owner\[*\].filter dump : ] : ]
						set sLocalFilter [ split [ mql print policy $sPol select allstate.owner\[*\].localfilter dump : ] : ]
						set sLogin [ split [ mql print policy $sPol select allstate.owner\[*\].login dump ] , ]
						set sKey [ split [ mql print policy $sPol select allstate.owner\[*\].key dump ] , ]
						set sOwnerRevoke [ split [ mql print policy $sPol select allstate.owner\[*\].revoke dump ] , ]
						set sOwnerOwner [ split [ mql print policy $sPol select allstate.owner\[*\].owner dump ] , ]
						set sOwnerReserve [ split [ mql print policy $sPol select allstate.owner\[*\].reserve dump ] , ]
						set sOwnerMaturity [ split [ mql print policy $sPol select allstate.owner\[*\].maturity dump ] , ]
						set sOwnerCategory [ split [ mql print policy $sPol select allstate.owner\[*\].category dump ] , ]

						set iLen [ llength $sOwners ]
						for {set i 0} {$i < $iLen} {incr i} {
							set sAllStateOwnerAccess [ lindex $sAccess $i ]
							set sAllStateOwnerAccess [ split $sAllStateOwnerAccess , ]
							
							set sAllStateOwnerFilter [ lindex $sFilter $i ]
							set sAllStateOwnerOrgProj [ lindex $sOrg $i ]
							append sAllStateOwnerOrgProj ","	
							set sAllStateOwnerProj [ lindex $sProject $i ]
							append sAllStateOwnerOrgProj $sAllStateOwnerProj
							
							set sAllStateOwnerLogin [ lindex $sLogin $i ]
							set sAllStateOwnerKey [ lindex $sKey $i ]
							
							set sAllStateOwnerOwner [ lindex $sOwnerOwner $i ]
							set sAllStateOwnerReserve [ lindex $sOwnerReserve $i ]
							set sAllStateOwnerMaturity [ lindex $sOwnerMaturity $i ]
							set sAllStateOwnerCategory [ lindex $sOwnerCategory $i ]
							
							set sAllStateOwnerRevoke [ lindex $sOwnerRevoke $i ]
							set sAllStateOwnerlocalFilter [ lindex $sLocalFilter $i ]
							
							set data($sPol|$sStOrder|$sSt|0,$i,Owner) [ list $sAllStateOwnerAccess 	$sAllStateOwnerFilter $sAllStateOwnerOrgProj $sAllStateOwnerLogin $sAllStateOwnerKey $sAllStateOwnerOwner $sAllStateOwnerReserve $sAllStateOwnerMaturity $sAllStateOwnerCategory $sAllStateOwnerRevoke "" $sAllStateOwnerlocalFilter ]
						}
						
						# Extract data for allstate for public				
						set sOwners [ split [ string trim [ mql print policy $sPol select allstate.public\[*\].owner dump | ] ] | ]				
						set sAccess [ split [ string trim [ mql print policy $sPol select allstate.public\[*\].access dump | ] ] | ]				
						set sOrg [ split [ mql print policy $sPol select allstate.public\[*\].organization dump ] , ]	
						set sProject [ split [ mql print policy $sPol select allstate.public\[*\].project dump ] , ]
						append sOrgProj $sProject
						set sFilter [ split [ mql print policy $sPol select allstate.public\[*\].filter dump : ] : ]
						set sLocalFilter [ split [ mql print policy $sPol select allstate.public\[*\].localfilter dump : ] : ]
						set sLogin [ split [ mql print policy $sPol select allstate.public\[*\].login dump ] , ]
						set sKey [ split [ mql print policy $sPol select allstate.public\[*\].key dump ] , ]
						set sPublicRevoke [ split [ mql print policy $sPol select allstate\[*\].public.revoke dump ] , ]
						set sPublicOwner [ split [ mql print policy $sPol select allstate.public\[*\].owner dump ] , ]
						set sPublicReserve [ split [ mql print policy $sPol select allstate.public\[*\].reserve dump ] , ]
						set sPublicMaturity [ split [ mql print policy $sPol select allstate.public\[*\].maturity dump ] , ]
					    set sPublicCategory [ split [ mql print policy $sPol select allstate.public\[*\].category dump ] , ]
						set iLen [ llength $sOwners ]
						for {set i 0} {$i < $iLen} {incr i} {
							set sAllStatePublicAccess [ lindex $sAccess $i ]
							set sAllStatePublicAccess [ split $sAllStatePublicAccess , ]
							set sAllStatePublicFilter [ lindex $sFilter $i ]
							set sAllStatePublicOrgProj [ lindex $sOrg $i ]
							append sAllStatePublicOrgProj ","	
							set sAllStatePublicProj [ lindex $sProject $i ]
							append sAllStatePublicOrgProj $sAllStatePublicProj						
							
							set sAllStatePublicLogin [ lindex $sLogin $i ]
							set sAllStatePublicKey [ lindex $sKey $i ]
							
							set sAllStatePublicOwner [ lindex $sPublicOwner $i ]
							set sAllStatePublicReserve [ lindex $sPublicReserve $i ]
							set sAllStatePublicMaturity [ lindex $sPublicMaturity $i ]
							set sAllStatePublicCategory [ lindex $sPublicCategory $i ]
							
							set sAllStatePublicRevoke [ lindex $sPublicRevoke $i ]
							set sAllStatePublicLocalFilter [ lindex $sLocalFilter $i ]
							
							set data($sPol|$sStOrder|$sSt|0,$i,Public) [ list $sAllStatePublicAccess $sAllStatePublicFilter $sAllStatePublicOrgProj $sAllStatePublicLogin $sAllStatePublicKey $sAllStatePublicOwner $sAllStatePublicReserve $sAllStatePublicMaturity $sAllStatePublicCategory $sAllStatePublicRevoke "" $sAllStatePublicLocalFilter ]
						}
						
						# Extract data for allstate for user
						set sAllStateUsers [ split [ mql print policy $sPol select allstate.user.user dump ] , ]
						set sAllStateAccess [ split [ mql print policy $sPol select allstate.user.access dump | ] | ]
						set sAllStateOrganization [ split [ mql print policy $sPol select allstate.user.organization dump ] , ]
						set sAllStateProject [ split [ mql print policy $sPol select allstate.user.project dump ] , ]
						
						set sAllStateFilter [ split [ mql print policy $sPol select allstate.user.filter dump : ] : ]
						set sAllStateLocalFilter [ split [ mql print policy $sPol select allstate.user.localfilter dump : ] : ]
						set sAllStateLogin [ split [ mql print policy $sPol select allstate.user.login dump ] , ]
						set sAllStateKey [ split [ mql print policy $sPol select allstate.user.key dump ] , ]
						set sAllStateRevoke [ split [ mql print policy $sPol select allstate.user.revoke dump ] , ]
						set sAllStateUserOwner [ split [ mql print policy $sPol select allstate.user.owner dump ] , ]
						set sAllStateUserReserveVals [ split [ mql print policy $sPol select allstate.user.reserve dump ] , ]
						set sAllStateUserMaturityVals [ split [ mql print policy $sPol select allstate.user.maturity dump ] , ]
						set sAllStateUserCategoryVals [ split [ mql print policy $sPol select allstate.user.category dump ] , ]						
						set iUserCnt [llength $sAllStateUsers]					
						
						for {set i 0} {$i < $iUserCnt} {incr i} {
							set sAllStateUser [ lindex $sAllStateUsers $i ]
							set sAllStateOwner [string trim $sAllStateUser]

							set sAllStateUserRights [ lindex $sAllStateAccess $i ]
							set sAllStateUserRights [ split $sAllStateUserRights , ]

							set sAllStateUserOrgProj [ lindex $sAllStateOrganization $i ]			
							append sAllStateUserOrgProj ","				
							set sAllStateUserPrj [ lindex $sAllStateProject $i ]
							append sAllStateUserOrgProj $sAllStateUserPrj				

							set sAllStateUserFilter [ lindex $sAllStateFilter $i ]

							set sAllStateUserLogin [ lindex $sAllStateLogin $i ]
							set sAllStateUserKey [ lindex $sAllStateKey $i ]
							
							set sAllStateUserOwnerVal [ lindex $sAllStateUserOwner $i ]
							set sAllStateUserReserveValue [ lindex $sAllStateUserReserveVals $i ]
							set sAllStateUserMaturityValue [ lindex $sAllStateUserMaturityVals $i ]
							set sAllStateUserCategoryValue [ lindex $sAllStateUserCategoryVals $i ]
							set sAllStateUserRevokeValue [ lindex $sAllStateRevoke $i ]
							set sAllStateUserLocalFilter [ lindex $sAllStateLocalFilter $i ]

							# KYB Start V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy						 
							set data($sPol|$sStOrder|$sSt|$i,1,$sAllStateUser) [ list $sAllStateUserRights $sAllStateUserFilter $sAllStateUserOrgProj $sAllStateUserLogin $sAllStateUserKey $sAllStateUserOwnerVal $sAllStateUserReserveValue $sAllStateUserMaturityValue $sAllStateUserCategoryValue $sAllStateUserRevokeValue "" $sAllStateUserLocalFilter ]
							# KYB End V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy
						}					
					 } else {					
						# KYB - extract all parameters for owner
						set sOwners [ split [ string trim [ mql print policy $sPol select state\[$sSt\].owner\[*\].owner dump | ] ] | ]					
						set sOwnerAccess [ split [ string trim [ mql print policy $sPol select state\[$sSt\].owner\[*\].access dump | ] ] | ]				
						set sOwnerOrg [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].organization dump ] , ]
						set sOwnerProject [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].project dump ] , ]
						set sOwnerFilter [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].filter dump : ] : ]
						set sOwnerLocalFilter [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].localfilter dump : ] : ]
						set sOwnerLogins [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].login dump ] , ]
						set sOwnerKeys [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].key dump ] , ]
						
						set sOwnerRevokes [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].revoke dump ] , ]
						
						set sOwnerOwners [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].owner dump ] , ]
						set sOwnerReserveVal [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].reserve dump ] , ]
						set sOwnerMaturityVal [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].maturity dump ] , ]
						set sOwnerCategoryVal [ split [ mql print policy $sPol select state\[$sSt\].owner\[*\].category dump ] , ]
						set iOwnerLen [ llength $sOwners ]
						
						for {set i 0} {$i < $iOwnerLen} {incr i} {
							set sOAccess [ lindex $sOwnerAccess $i ]
							set sOAccess [ split $sOAccess , ]
							
							set sOwnerFilterExpr [ lindex $sOwnerFilter $i ]
							set sOwnerOrgPrj [ lindex $sOwnerOrg $i ]
							append sOwnerOrgPrj ","	
							set sOwnerPrj [ lindex $sOwnerProject $i ]
							append sOwnerOrgPrj $sOwnerPrj
							
							set sOwnerLogin [ lindex $sOwnerLogins $i ]
							set sOwnerKey [ lindex $sOwnerKeys $i ]							
							
							set sStateOwnerOwner [ lindex $sOwnerOwners $i ]
							set sStateOwnerReserve [ lindex $sOwnerReserveVal $i ]
							set sStateOwnerMaturity [ lindex $sOwnerMaturityVal $i ]
							set sStateOwnerCategory [ lindex $sOwnerCategoryVal $i ]
							
							set sStateOwnerRevoke [ lindex $sOwnerRevokes $i ]
							set sOwnerLocalFilter [ lindex $sOwnerLocalFilter $i ]
							
							set data($sPol|$sStOrder|$sSt|0,$i,Owner) [ list $sOAccess $sOwnerFilterExpr $sOwnerOrgPrj $sOwnerLogin $sOwnerKey $sStateOwnerOwner $sStateOwnerReserve $sStateOwnerMaturity $sStateOwnerCategory $sStateOwnerRevoke "" $sOwnerLocalFilter ]
						}					
						
						# KYB - extract all parameters for public
						set sPublic [ split [ string trim [ mql print policy $sPol select state\[$sSt\].public\[*\].owner dump | ] ] | ]				
						set sPublicAccess [ split [ string trim [ mql print policy $sPol select state\[$sSt\].public\[*\].access dump | ] ] | ]				
						set sPublicOrg [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].organization dump ] , ]
						set sPublicProject [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].project dump ] , ]
						set sPublicFilter [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].filter dump : ] : ]
						set sPublicLocalFilter [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].localfilter dump : ] : ]
						set sPublicLogins [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].login dump ] , ]					
						set sPublicKeys [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].key dump ] , ]
						
						set sPublicRevokes [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].revoke dump ] , ]
						
						set sPublicOwners [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].owner dump ] , ]
						set sPublicReserveValues [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].reserve dump ] , ]
						set sPublicMaturityValues [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].maturity dump ] , ]
						set sPublicCategoryValues [ split [ mql print policy $sPol select state\[$sSt\].public\[*\].category dump ] , ]
						set iPublicLen [ llength $sPublic ]
						for {set i 0} {$i < $iPublicLen} {incr i} {
							set sEachAccess [ lindex $sPublicAccess $i ]
							set sEachAccess [ split $sEachAccess , ]
							set sPublicFilterExpr [ lindex $sPublicFilter $i ]
							set sPublicOrgProj [ lindex $sPublicOrg $i ]
							append sPublicOrgProj ","	
							set sPublicPrj [ lindex $sPublicProject $i ]
							append sPublicOrgProj $sPublicPrj
							
							set sPublicLogin [ lindex $sPublicLogins $i ]
							set sPublicKey [ lindex $sPublicKeys $i ]							
							
							set sPublicOwnerVal [ lindex $sPublicOwners $i ]
							set sPublicReserveVals [ lindex $sPublicReserveValues $i ]
							set sPublicMaturityVals [ lindex $sPublicMaturityValues $i ]
							set sPublicCategoryVals [ lindex $sPublicCategoryValues $i ]
							
							set sPublicRevokeVals [ lindex $sPublicRevokes $i ]
							set sPublicLocalFilter [ lindex $sPublicLocalFilter $i ]
							
							set data($sPol|$sStOrder|$sSt|0,$i,Public) [ list $sEachAccess $sPublicFilterExpr $sPublicOrgProj $sPublicLogin $sPublicKey $sPublicOwnerVal $sPublicReserveVals $sPublicMaturityVals $sPublicCategoryVals $sPublicRevokeVals "" $sPublicLocalFilter ]
							}
						
						 set sUsers [ split [ mql print policy $sPol select state\[$sSt\].access ] \n ]
					 }
					 
					 # Extract data for state for user
						set sUsers [ split [ mql print policy $sPol select state\[$sSt\].user.user dump ] , ]
						#set sAccess [ split [ mql print policy $sPol select state\[$sSt\].user.access dump | ] | ]
						set sAccess [ split [ string trim [ mql print policy $sPol select state\[$sSt\].user.access dump | ] ] | ]
						set sOrganization [ split [ mql print policy $sPol select state\[$sSt\].user.organization dump ] , ]
						set sProject [ split [ mql print policy $sPol select state\[$sSt\].user.project dump ] , ]
						
						set sFilter [ split [ mql print policy $sPol select state\[$sSt\].user.filter dump : ] : ]
						set sLocalFilter [ split [ mql print policy $sPol select state\[$sSt\].user.localfilter dump : ] : ]
						set sLogin [ split [ mql print policy $sPol select state\[$sSt\].user.login dump ] , ]
						set sKey [ split [ mql print policy $sPol select state\[$sSt\].user.key dump ] , ]	
						set sRevoke [ split [ mql print policy $sPol select state\[$sSt\].user.revoke dump ] , ]	
						
						set sOwnerVals [ split [ mql print policy $sPol select state\[$sSt\].user.owner dump ] , ]	
						set sReserveVals [ split [ mql print policy $sPol select state\[$sSt\].user.reserve dump ] , ]	
						set sMaturityVals [ split [ mql print policy $sPol select state\[$sSt\].user.maturity dump ] , ]
						set sCategoryVals [ split [ mql print policy $sPol select state\[$sSt\].user.category dump ] , ]
						set iUserCnt [llength $sUsers]

						for {set i 0} {$i < $iUserCnt} {incr i} {
							set sUser [ lindex $sUsers $i ]
							set sOwner [string trim $sUser]
							
							set sUserRights [ lindex $sAccess $i ]
							set sUserRights [ split $sUserRights , ]						
							
							set sUserOrgProj [ lindex $sOrganization $i ]			
							append sUserOrgProj ","				
							set sUserPrj [ lindex $sProject $i ]
							append sUserOrgProj $sUserPrj		

							set sUserFilter [ lindex $sFilter $i ]
							
							set sUserLogin [ lindex $sLogin $i ]
							set sUserKey [ lindex $sKey $i ]
							
							set sUserOwner [ lindex $sOwnerVals $i ]
							set sUserReserve [ lindex $sReserveVals $i ]
							set sUserMaturity [ lindex $sMaturityVals $i ]
							set sUserCategory [ lindex $sCategoryVals $i ]
							set sUserRevoke [ lindex $sRevoke $i ]
							set sUserLocalFilter [ lindex $sLocalFilter $i ]
							
							set sBranch ""
							if { $sUserKey != "" } {set sBranch [ mql print policy $sPol select state\[$sSt\].signature\[$sUserKey\].branch dump ]}
							
							# KYB Start V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy						 						
							set data($sPol|$sStOrder|$sSt|$i,1,$sOwner) [ list $sUserRights $sUserFilter $sUserOrgProj $sUserLogin $sUserKey $sUserOwner $sUserReserve $sUserMaturity $sUserCategory $sUserRevoke $sBranch $sUserLocalFilter ]
							# KYB End V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user of a state of a policy
						}
					 incr sStOrder
				 }
			  }
		   } 
		}
	 
		set sSpin ""
		foreach sP $lPolicy {
			#set pu [ lsort -dictionary [ array name data "$sP|*|*,*,*" ] ]
			set pu [ lsort -dictionary [ array name data "$sP|*|*|*,*,*" ] ]
			foreach i $pu {
				lappend sSpin [ list $i $data($i) ]
			}
			set sPolicySpin [ pFormatSpinnerWithLoginKey $sSpin $sP Policy ]
			pfile_write "$sSpinnerPath/Business/Policy/$sP.xls" $sPolicySpin
			set sSpin ""
		}
	
    puts "Policy State Access data loaded in directory: $sSpinnerPath/Business/Policy"
}


