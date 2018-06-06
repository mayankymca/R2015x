tcl;

eval {
   if {[info host] == "MOSTERMAN2K" } {
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

# Main
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
   
   #KYB Fixed Spinner Version Issue for ENOVIA V6R2014x HFs
   #set sMxVersion [mql version]
   #set sMxVersion "V6R2014x"
   set sMxVersion [mql get env MXVERSION]
   if {[string first "V6" $sMxVersion] >= 0} {
      set sMxVersion "10.9"
   } else {
      set sMxVersion [join [lrange [split $sMxVersion .] 0 1] .]
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
 
    set lAccessModes(1) [ list Read Modify Delete Checkout Checkin Schedule Lock \
        Unlock Execute Freeze Thaw Create Revise Promote Demote Grant Enable \
        Disable Override ChangeName ChangeType ChangeOwner ChangePolicy Revoke \
        ChangeVault FromConnect ToConnect FromDisconnect ToDisconnect \
        ViewForm Modifyform Show ]
        
    set lAccessModes(2) [ list Attribute Type Relationship "Format" Person Group Role \
        Association Policy Program Wizard Report Form Rule Property Site Store Vault \
        server Location Process "Menu" Inquiry Table Portal Expression ]

    set sPositive Y
    set sNegative "-"
    #KYB V6R2014x GA - Renamed column 'Person' to 'Name'
	#set sPersonAccAdm(1) [list "Person\t[join $lAccessModes(1) \t]"]
    #set sPersonAccAdm(2) [list "Person\t[join $lAccessModes(2) \t]"]
	set sPersonAccAdm(1) [list "Name\t[join $lAccessModes(1) \t]"]
    set sPersonAccAdm(2) [list "Name\t[join $lAccessModes(2) \t]"]
    set lsPersonAccAdm(1) [list ]
    set lsPersonAccAdm(2) [list ]
    set sFile(1) "SpinnerPersonAccessData$sAppend.xls"
    set sFile(2) "SpinnerPersonAdminData$sAppend.xls"

    set lsPerson [split [mql list person $sFilter] \n]

    if {!$bTemplate} {
       foreach sPerson $lsPerson {
       	set lsPrint [split [mql print person $sPerson] \n]
           foreach i $lsPrint {
               set i [ string trim $i ]
               regsub " " $i ":" i
               set lsi [split $i :] 
               if { [lindex $lsi 0] == "access"} {
                   set lsRights(1) [split [lindex $lsi 1] ,]
                   set lsRights(2) [split [mql print person $sPerson select admin dump] ,]
                   for {set j 1} {$j < 3} {incr j} {
                       if { [lindex $lsRights($j) 0] == "all" } {
                           set sNegativeValue $sPositive
                       } else {
                           set sNegativeValue $sNegative
                       }
                       set sFormat ""
                       foreach sMode $lAccessModes($j) {
                           set sMode [string tolower $sMode]
                           if { [ lsearch $lsRights($j) $sMode ] == -1 } {
                               append sFormat "\t$sNegativeValue"
                           } else {
                               append sFormat "\t$sPositive"
                           }
                       }
                       lappend lsPersonAccAdm($j) "$sPerson$sFormat"
                   }
                   break
               }
           }
       }
    }
    set lsPersonAccAdm(1) [concat $sPersonAccAdm(1) [lsort $lsPersonAccAdm(1)]]
    set lsPersonAccAdm(2) [concat $sPersonAccAdm(2) [lsort $lsPersonAccAdm(2)]]
    pfile_write "$sSpinnerPath/Business/$sFile(1)" [join $lsPersonAccAdm(1) \n]
    pfile_write "$sSpinnerPath/Business/$sFile(2)" [join $lsPersonAccAdm(2) \n]
    puts "Person Access data loaded in files: $sSpinnerPath/Business/$sFile(1) and\n$sSpinnerPath/Business/$sFile(2)"
}
