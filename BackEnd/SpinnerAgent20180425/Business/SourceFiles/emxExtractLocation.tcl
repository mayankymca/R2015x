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

#  Set up array for symbolic name mapping
#
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "location "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "location"} {
         regsub $sTypeReplace $sPropertyTo "" sPropertyTo
         regsub "_" $sPropertyName "|" sSymbolicName
         set sSymbolicName [lindex [split $sSymbolicName |] 1]
         array set aSymbolic [list $sPropertyTo $sSymbolicName]
      }
   }

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
   
   set sSpinnerPath [mql get env SPINNERPATH]
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
      }
      file mkdir $sSpinnerPath/System
   }

   set sPath "$sSpinnerPath/System/location.xls"
   set lsLocation [split [mql list location $sFilter] \n]
   ###### Modify by SL Team for issue "multipledirectories" ########
   set sFile "name\tRegistry Name\tdescription\tpermission\tprotocol\tport\thost\tpath\tuser\tpassword\turl\tfcs\thidden\ticon\n"
   ###### END #########
	
	#KYB Fixed Spinner Version Issue for ENOVIA V6R2014x HFs
	#set sMxVersion [mql version]
    #set sMxVersion "V6R2014x"
	set sMxVersion [mql get env MXVERSION]
   if {[string first "V6" $sMxVersion] >= 0} {
      set sMxVersion "10.9"
   } else {
      set sMxVersion [join [lrange [split $sMxVersion .] 0 1] .]
   }
   
   if {!$bTemplate} {
      foreach sLocation $lsLocation {
         if {[catch {set sName [mql print location $sLocation select name dump]} sMsg] != 0} {
            puts "ERROR: Problem with retrieving info on location '$sLocation' - Error Msg:\n$sMsg"
            continue
         }
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print location $sLocation select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print location $sLocation select property\[SpinnerAgent\] dump] != "")} {
		     ###### Modify by SL Team for issue "multipledirectories" ########
            foreach sItem [list sDescription sPermission sProtocol sPort sHost sLocPath sUser sSearchURL sFcsURL sHidden] {
			 ###### END ########
               eval "set $sItem \"\""
            }
            
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sLocation)} sMsg
            regsub -all " " $sLocation "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sLocation
            }
			
            ###### Modify by SL Team for issue "multipledirectories" ########
			set lsPrint [split [mql print location $sLocation select description protocol host path user fcsurl hidden port] \n]
			###### END ########
			
			 ###### Modify by SL Team for issue "multipledirectories" ########
            foreach sItem [list description protocol host port path user "fcs url" "search url" hidden] sVar [list sDescription sProtocol sHost sPort sLocPath sUser sFcsURL sSearchURL sHidden] {
			###### END ########
               set iList [lsearch -regexp $lsPrint $sItem]
               if {$iList >= 0} {
                  set sResult [string trim [lindex $lsPrint $iList]]
				   ###### Modify by SL Team for issue "multipledirectories" ########
                  if {$sItem != "hidden"} {
				  ###### END ########
                     regsub $sItem $sResult "" sResult
                     set sResult [string trim $sResult]
					 ###### Added by SL Team for issue "multipledirectories"########
					 regsub -all "\075" $sResult "" sResult
					 set sResult [string trim $sResult]
                     ###### END ##########
			      } 
                   ###### Added by SL Team for issue "multipledirectories" ########
				   regsub "hidden" $sResult "" sResult
                   regsub -all "\075" $sResult "" sResult
				   set sResult [string trim $sResult]
				   ###### END ##########
                  eval {set $sVar $sResult}
               }
            }
            set sPermission [mql print location $sLocation select permission dump]
            append sFile "$sName\t$sOrigName\t$sDescription\t$sPermission\t$sProtocol\t$sPort\t$sHost\t$sLocPath\t$sUser\t\t$sSearchURL\t$sFcsURL\t$sHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Location data loaded in file $sPath"
}

