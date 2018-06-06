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
# Procedure:   pFormatSpinner
#
# Description: Procedure to format data for spinner file.
#
# Parameters:  The data to format.
#
# Returns:     Nothing
#************************************************************************
proc pFormatSpinner { lData sHead sType } {    
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
    
        append sFormat "Rule"
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
        append sFormat "\n"
	
        foreach line $lData {
            if { $line == "" } {
                continue
            }
			
            set sRuleDetails [ lindex $line 0 ]
            set sRuleData [ lindex $line 1 ]
            set sFilter [ lindex $sRuleData 1 ]
            set sLeft [ split [ lindex $line 0 ] , ]
            set sOwner [ lindex $sLeft 2 ]
            set sLeft [ split [ lindex $sLeft 0 ] | ]
            set sRule [ lindex $sLeft 0 ]
            set sRights [ lindex $sRuleData 0 ]
    
			# KYB Start V6R2013x Feature Extracting Organization and Project columns for a user for a rule
			set sRuleSecurityContext [ lindex $sRuleData 2 ]
			set sSecurityContext [ split [ lindex $sRuleSecurityContext 0 ] , ]
			set sRuleOrg [ lindex $sSecurityContext 0 ]
			set sRuleProject [ lindex $sSecurityContext 1 ]
			# KYB End V6R2013x Feature Extracting Organization and Project columns for a user for a rule
    
            append sFormat "$sRule"
            append sFormat "$sDelimit$sOwner"
			
			set sLogin [ lindex $sRuleData 3 ]
			set sKey [ lindex $sRuleData 4 ]
			set sRevoke [ lindex $sRuleData 9 ]
			set sOwnerVal [ lindex $sRuleData 5 ]
			set sReserveVal [ lindex $sRuleData 6 ]
			set sMaturityVal [ lindex $sRuleData 7 ]
			set sCategoryVal [ lindex $sRuleData 8 ]
			set sLocalFilter [ lindex $sRuleData 10 ]

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
				
				# KYB Start V6R2013x Feature Extracting Organization and Project columns for a user for a rule
				if { $sMode != "organization(any|single|ancestor|descendant)" && $sMode != "project(any|single|ancestor|descendant)" && $sMode != "owner" && $sMode != "reserve(context)" && $sMode != "maturity" && $sMode != "category" } {
					if { [ lsearch $sRights $sMode ] == -1 } {
						append sFormat "$sDelimit$sNegativeValue"
					} else {
						append sFormat "$sDelimit$sPositive"
					}
                }

				if { $sMode == "organization(any|single|ancestor|descendant)" } {
					append sFormat "$sDelimit$sRuleOrg"
				} elseif {$sMode == "project(any|single|ancestor|descendant)"} {
					append sFormat "$sDelimit$sRuleProject"
				} elseif {$sMode == "owner"} {
					append sFormat "$sDelimit$sOwnerVal"
				} elseif {$sMode == "reserve(context)"} {
					append sFormat "$sDelimit$sReserveVal"
				} elseif {$sMode == "maturity"} {
					append sFormat "$sDelimit$sMaturityVal"
				} elseif {$sMode == "category"} {
					append sFormat "$sDelimit$sCategoryVal"
				}
				# KYB End V6R2013x Feature Extracting Organization and Project columns for a user for a rule
            }
            append sFormat "$sDelimit$sFilter"
			append sFormat "$sDelimit$sLocalFilter"
            append sFormat "\n"
    
        }
        return $sFormat
    }
#End pFormatSpinner

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
   
   #KYB Fixed Spinner Version Issue for ENOVIA V6R2014x HFs
   #set sMxVersion [mql version]
   #set sMxVersion "V6R2014x"
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
   #KYB Start - Commented code for checking major/minor enabled or not
   #set bMm [mql get env MAJORMINOREXTRACTION]
   #KYB End - Commented code for checking major/minor enabled or not
	
    set sSpinnerPath [mql get env SPINNERPATH]
    if {$sSpinnerPath == ""} {
       set sOS [string tolower $tcl_platform(os)];
       set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
       
       if { [string tolower [string range $sOS 0 5]] == "window" } {
          set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
       } else {
          set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
       }
       file mkdir "$sSpinnerPath/Business/Rule"
    }
 
# Major/Minor Mod - ION - 10/1/2012
# KYB Start V6R2013x Feature Extracting Organization and Project columns for a user for a rule
   
       set lAccessModes [ list Read Modify Delete Checkout Checkin Schedule Lock \
           Unlock Execute Freeze Thaw Create Revise MajorRevise Promote Demote Grant \
           Enable Disable Override ChangeName ChangeType ChangeOwner ChangePolicy Revoke \
           ChangeVault FromConnect ToConnect FromDisconnect ToDisconnect \
           ViewForm Modifyform Show Approve Reject Ignore Reserve Unreserve Organization(any|single|ancestor|descendant) Project(any|single|ancestor|descendant) Owner Reserve(Context) Maturity Category ]
   
# KYB End V6R2013x Feature CTX-SPN-030 Extracting Organization and Project columns for a user for a rule
  	
    set sPositive Y
    set sNegative "-"

    set lRule [split [mql list Rule $sFilter] \n]

    foreach sRule $lRule {
       set bPass TRUE
       if {$sMxVersion > 8.9} {
          set sModDate [mql print rule $sRule select modified dump]
          set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
          if {$sModDateMin != "" && $sModDate < $sModDateMin} {
             set bPass FALSE
          } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
             set bPass FALSE
          }
       }
        
       if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print rule $sRule select property\[SpinnerAgent\] dump] != "")} {
          if {!$bTemplate} {
             #KYB V6R2015x GA - Extract info for 'owner'
			 set sOwners [ split [ string trim [ mql print rule $sRule select owner\[*\].owner dump | ] ] | ]
			 set sOwnerAccess [ split [ string trim [ mql print rule $sRule select owner\[*\].access dump | ] ] | ]
			 set sOwnerOrg [ split [ mql print rule $sRule select owner\[*\].organization dump ] , ]
			 set sOwnerProject [ split [ mql print rule $sRule select owner\[*\].project dump ] , ]
			 set sOwnerFilter [ split [ mql print rule $sRule select owner\[*\].filter dump : ] : ]
			 set sOwnerLogins [ split [ mql print rule $sRule select owner\[*\].login dump ] , ]
			 set sOwnerKeys [ split [ mql print rule $sRule select owner\[*\].key dump ] , ]
			 
			 set sLocalFilter [ split [ mql print rule $sRule select owner\[*\].localfilter dump : ] : ]
			 
			 set sOwnerRevokes [ split [ mql print rule $sRule select owner\[*\].revoke dump ] , ]			
			 set sOwnerOwners [ split [ mql print rule $sRule select owner\[*\].owner dump ] , ]
			 set sOwnerReserveVal [ split [ mql print rule $sRule select owner\[*\].reserve dump ] , ]
			 set sOwnerMaturityVal [ split [ mql print rule $sRule select owner\[*\].maturity dump ] , ]
			 set sOwnerCategoryVal [ split [ mql print rule $sRule select owner\[*\].category dump ] , ]
			 set iOwnerLen [ llength $sOwners ]
			 
			 for {set i 0} {$i < $iOwnerLen} {incr i} {
				set sAllAccess [ lindex $sOwnerAccess $i ]
				set sEachAccess [ split $sAllAccess , ]				
				set sEachFilter [ lindex $sOwnerFilter $i ]
				set sEachOrgPrj [ lindex $sOwnerOrg $i ]
				append sEachOrgPrj ","	
				set sEachPrj [ lindex $sOwnerProject $i ]
				append sEachOrgPrj $sEachPrj				
				set sEachLogin [ lindex $sOwnerLogins $i ]
				set sEachKey [ lindex $sOwnerKeys $i ]				
				set sEachOwner [ lindex $sOwnerOwners $i ]
				set sEachReserve [ lindex $sOwnerReserveVal $i ]
				set sEachMaturity [ lindex $sOwnerMaturityVal $i ]
				set sEachCategory [ lindex $sOwnerCategoryVal $i ]				
				set sEachRevoke [ lindex $sOwnerRevokes $i ]				
				
				set sEachlocalFilter [ lindex $sLocalFilter $i ]
				
				set data($sRule|$i,1,Owner) [ list $sEachAccess $sEachFilter $sEachOrgPrj $sEachLogin $sEachKey $sEachOwner $sEachReserve $sEachMaturity $sEachCategory $sEachRevoke $sEachlocalFilter]
			 }
			 
			 #KYB V6R2015x GA - Extract info for 'public'
			 set sPublicUsers [ split [ string trim [ mql print rule $sRule select public\[*\].owner dump | ] ] | ]
			 set sPublicAccess [ split [ string trim [ mql print rule $sRule select public\[*\].access dump | ] ] | ]
			 set sPublicOrg [ split [ mql print rule $sRule select public\[*\].organization dump ] , ]
			 set sPublicProject [ split [ mql print rule $sRule select public\[*\].project dump ] , ]
			 set sPublicFilter [ split [ mql print rule $sRule select public\[*\].filter dump : ] : ]
			 
			 set sPublicLocalFilter [ split [ mql print rule $sRule select public\[*\].localfilter dump : ] : ]
			 
			 set sPublicLogins [ split [ mql print rule $sRule select public\[*\].login dump ] , ]
			 set sPublicKeys [ split [ mql print rule $sRule select public\[*\].key dump ] , ]
			 set sPublicRevokes [ split [ mql print rule $sRule select public\[*\].revoke dump ] , ]			
			 set sPublicOwners [ split [ mql print rule $sRule select public\[*\].owner dump ] , ]
			 set sPublicReserveVal [ split [ mql print rule $sRule select public\[*\].reserve dump ] , ]
			 set sPublicMaturityVal [ split [ mql print rule $sRule select public\[*\].maturity dump ] , ]
			 set sPublicCategoryVal [ split [ mql print rule $sRule select public\[*\].category dump ] , ]
			 set iPublicLen [ llength $sPublicUsers ]
			 
			 for {set i 0} {$i < $iPublicLen} {incr i} {
				set sAllAccess [ lindex $sPublicAccess $i ]
				set sEachAccess [ split $sAllAccess , ]
				set sEachFilter [ lindex $sPublicFilter $i ]
				set sEachOrgPrj [ lindex $sPublicOrg $i ]
				append sEachOrgPrj ","
				set sEachPrj [ lindex $sPublicProject $i ]
				append sEachOrgPrj $sEachPrj
				set sEachLogin [ lindex $sPublicLogins $i ]
				set sEachKey [ lindex $sPublicKeys $i ]
				set sEachOwner [ lindex $sPublicOwners $i ]
				set sEachReserve [ lindex $sPublicReserveVal $i ]
				set sEachMaturity [ lindex $sPublicMaturityVal $i ]
				set sEachCategory [ lindex $sPublicCategoryVal $i ]
				set sEachRevoke [ lindex $sPublicRevokes $i ]
				
				set sEachLocalFilter [ lindex $sPublicLocalFilter $i ]
				
				set data($sRule|$i,1,public) [ list $sEachAccess $sEachFilter $sEachOrgPrj $sEachLogin $sEachKey $sEachOwner $sEachReserve $sEachMaturity $sEachCategory $sEachRevoke $sEachLocalFilter ]
			 }
			 
			 #KYB V6R2015x GA - Extract info for 'user'
			 set sUsers [ split [ mql print rule $sRule select user.user dump ] , ]
			 set sUserAccess [ split [ string trim [ mql print rule $sRule select user.access dump | ] ] | ]
			 set sUserOrganization [ split [ mql print rule $sRule select user.organization dump ] , ]
			 set sUserProject [ split [ mql print rule $sRule select user.project dump ] , ]			
			 set sUserFilter [ split [ mql print rule $sRule select user.filter dump : ] : ]
			 
			 set sUserLocalFilter [ split [ mql print rule $sRule select user.localfilter dump : ] : ]
			 
			 set sUserLogin [ split [ mql print rule $sRule select user.login dump ] , ]
			 set sUserKey [ split [ mql print rule $sRule select user.key dump ] , ]	
			 set sUserRevoke [ split [ mql print rule $sRule select user.revoke dump ] , ]				
			 set sUserOwnerVals [ split [ mql print rule $sRule select user.owner dump ] , ]	
			 set sUserReserveVals [ split [ mql print rule $sRule select user.reserve dump ] , ]	
			 set sUserMaturityVals [ split [ mql print rule $sRule select user.maturity dump ] , ]
			 set sUserCategoryVals [ split [ mql print rule $sRule select user.category dump ] , ]
			 set iUserCnt [llength $sUsers]
			 
			 for {set i 0} {$i < $iUserCnt} {incr i} {
				set sUser [ lindex $sUsers $i ]
				set sOwner [string trim $sUser]				
				set sRights [ lindex $sUserAccess $i ]
				set sUserRights [ split $sRights , ]				
				set sUserOrgProj [ lindex $sUserOrganization $i ]			
				append sUserOrgProj ","				
				set sUserPrj [ lindex $sUserProject $i ]
				append sUserOrgProj $sUserPrj
				set sFilter [ lindex $sUserFilter $i ]				
				set sLogin [ lindex $sUserLogin $i ]
				set sKey [ lindex $sUserKey $i ]				
				set sUserOwner [ lindex $sUserOwnerVals $i ]
				set sUserReserve [ lindex $sUserReserveVals $i ]
				set sUserMaturity [ lindex $sUserMaturityVals $i ]
				set sUserCategory [ lindex $sUserCategoryVals $i ]
				set sRevoke [ lindex $sUserRevoke $i ]				
				
				set sLocalFilter [ lindex $sUserLocalFilter $i ]

				set data($sRule|$i,1,$sOwner) [ list $sUserRights $sFilter $sUserOrgProj $sLogin $sKey $sUserOwner $sUserReserve $sUserMaturity $sUserCategory $sRevoke $sLocalFilter ]
			}
          }
       }
    
       set sSpin ""
       foreach sRule $lRule {
           set pu [ lsort -dictionary [ array name data "$sRule|*,*,*" ] ]
           foreach i $pu {
               lappend sSpin [ list $i $data($i) ]
           }
           set sRuleeSpin [ pFormatSpinner $sSpin $sRule Rule ]
           pfile_write "$sSpinnerPath/Business/Rule/$sRule.xls" $sRuleeSpin
           set sSpin ""
       }
    }
    puts "Rule Access data loaded in directory: $sSpinnerPath/Business/Rule"
}
