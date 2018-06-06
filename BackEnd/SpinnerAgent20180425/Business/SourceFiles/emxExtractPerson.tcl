
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

#Main

    set sFilter [mql get env 1]
    set bTemplate [mql get env 2]
    set bSpinnerAgentFilter [mql get env 3]
    set sGreaterThanEqualDate [mql get env 4]
    set sLessThanEqualDate [mql get env 5]

    set sAppend ""
    if {$sFilter != ""} {
       regsub -all "\134\052" $sFilter "ALL" sAppend
       regsub -all "\134\174" $sAppend "-" sAppend
       regsub -all "/" $sAppend "-" sAppend
       regsub -all ":" $sAppend "-" sAppend
       regsub -all "<" $sAppend "-" sAppend
       regsub -all ">" $sAppend "-" sAppend
       regsub -all " " $sAppend "" sAppend
       set sAppend "_$sAppend"
    }
   
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
   
    set sSpinnerPath [mql get env SPINNERPATH]
    if {$sSpinnerPath == ""} {
        set sOS [string tolower $tcl_platform(os)];
        set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
       
        if { [string tolower [string range $sOS 0 5]] == "window" } {
            set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
        } else {
          set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
        }
        file mkdir "$sSpinnerPath/Business"
    }
 
    set sDelimit "\t"
    set lPerson [list ]
    set lPersonData [ list name fullname comment address phone fax email vault site type assign_role assign_group assign_product e_mail iconmail password hidden ]
    lappend lPerson [join $lPersonData $sDelimit]
    
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
   
    if {!$bTemplate} {
        set lsPerson [split [mql list person $sFilter] \n]
    
        foreach sPerson $lsPerson {
            set bPass TRUE
            if {$sMxVersion > 8.9} {
               if {$sModDateMin != "" || $sModDateMax != ""} {
                  set sModDate [mql print person $sPerson select modified dump]
                  set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
                  if {$sModDateMin != "" && $sModDate < $sModDateMin} {
                     set bPass FALSE
                  } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
                     set bPass FALSE
                  }
               }
            }
            if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print person $sPerson select property\[SpinnerAgent\] dump] != "")} {
                set aData(name) $sPerson
                set Content [mql print person $sPerson]
        
                regsub -all -- {\{} $Content { LEFTBRACE } Content
                regsub -all -- {\}} $Content { RIGHTBRACE } Content
        
                set Content [lrange [split $Content \n] 1 end]
                set lAssign_Role [list ]
                set lAssign_Group [list ]
        
                foreach item $Content {
				
		#### ADDED BY SL TEAM ######
		regsub -all "\073" $item "\040" item
		regsub -all "\042" $item "\040" item
		#### END #######
                    set item [string trimleft $item]
                    set lItem [split $item]
                    set item_name [lindex $lItem 0]
					#puts "\n item_name     $item_name"
                    set item_content [lrange $item 1 end]
                    set aData($item_name) $item_content
                    # Case assign
                    if { $item_name == "assign" } {
                        set user [lrange $item 2 end]
                        set group_role [lindex $lItem 1]
                        if {$group_role == "group"} {
                            lappend lAssign_Group $user
                        } elseif {$group_role == "role"} {
                            lappend lAssign_Role $user
                        }
                    # Case lattice
                    } elseif { $item_name == "lattice" } {
					#puts "\n sPerson  $sPerson"
                        set vault [lrange $item 1 end]
                        set aData(vault) $vault
                    # Case mail
                    } elseif { $item_name == "enable" || $item_name == "disable" } {
                       set sMail [lindex $lItem 1]
                       regsub "email" $sMail "e_mail" sMail
                       set aData($sMail) $item_name
                    # Case hidden
                    } elseif { $item_name == "hidden" || $item_name == "nothidden" } {
                       set aData(hidden) $item_name
                    # Case password
                    } elseif { $item_name == "password" } {
                       set aData(password) ""
                     }
                }
                if {[llength $lAssign_Role] == 0} {
                    set aData(assign_role) ""
                } else {
                    set lAssign_Role [lsort -dictionary $lAssign_Role]
                    set sAssign_Role [join $lAssign_Role |]
                    regsub -all -- {\|} $sAssign_Role { | } sAssign_Role
                    set aData(assign_role) $sAssign_Role
                }
                if {[llength $lAssign_Group] == 0} {
                    set aData(assign_group) ""
                } else {
                    set lAssign_Group [lsort -dictionary $lAssign_Group]
                    set sAssign_Group [join $lAssign_Group |]
                    regsub -all -- {\|} $sAssign_Group { | } sAssign_Group
                    set aData(assign_group) $sAssign_Group
                }
                set aData(assign_product) [join [mql list product * where "person == '$sPerson'"] " | "]
        
                set lDataEach [list ]
                foreach sPersonData $lPersonData {
				#puts "\n sPersonData  $sPersonData"
                    if { [ info exists aData($sPersonData) ] == 1 } {
					#puts "\n aData($sPersonData)  $aData($sPersonData)"
                        lappend lDataEach $aData($sPersonData)
                    } else {
                        lappend lDataEach ""
                    }
                }
                lappend lPerson [join $lDataEach $sDelimit]
            }
			unset aData
        }
    }

    pfile_write "$sSpinnerPath/Business/SpinnerPersonData$sAppend.xls" [join $lPerson "\n"]
    puts "Person data loaded in file $sSpinnerPath/Business/SpinnerPersonData$sAppend.xls"
}
