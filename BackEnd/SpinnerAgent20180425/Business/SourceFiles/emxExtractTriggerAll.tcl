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

   set lsSchemaType [mql get env 1]
   set sFilter [mql get env 2]
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

   set sAppend "_ALL"

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

   set sPath "$sSpinnerPath/Business/SpinnerTriggerData$sAppend.xls"
   set sFile "Schema Type\tSchema Name\tState (for Policy)\tTrigger Event\tTrigger Type (check / override / action)\tProgram\tInput\n"
   
	#KYB Fixed Spinner Version Issue for ENOVIA V6R2014x HFs
	#set sMxVersion [mql version]
	#set sMxVersion "V6R2014x"
	set sMxVersion [mql get env MXVERSION]
   if {[string first "V6" $sMxVersion] >= 0} {
      set sMxVersion "10.9"
   } else {
      set sMxVersion [join [lrange [split $sMxVersion .] 0 1] .]
   }
   
   foreach sSchemaType $lsSchemaType {
      set lsName [split [mql list $sSchemaType $sFilter] \n]
      foreach sName $lsName {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print $sSchemaType $sName select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print $sSchemaType $sName select property\[SpinnerAgent\] dump] != "")} {
   
            if {$sSchemaType == "policy"} {
               set lsState [split [mql print policy $sName select state dump |] |]
               set lsState [lsort -decreasing $lsState]
               set lsCatchTest [list action check trigger]
            } else {
               set lsCatchTest trigger
            }
      
            set sStateName ""
            set sProgram ""
            set sInput ""
            set lsPrint [split [mql print $sSchemaType $sName] \n]
      
            foreach sPrint $lsPrint {                         
               set sPrint [string trim $sPrint]
      
               if {$sSchemaType == "policy"} {
                  if {[string first "state" $sPrint] == 0} {
                     foreach sState $lsState {
                        if {[string first "state $sState" $sPrint] == 0} {
                           set sStateName $sState
                           break
                        }
                     }
                  }
               }
                           
               set bCatchTest false
               
               foreach sCatchTest $lsCatchTest {
                  if {[string first "$sCatchTest " $sPrint] == 0} {
                     set bCatchTest true
                     break
                  }
               }
               
               if {$bCatchTest == "true"} {
                  regsub "$sCatchTest " $sPrint "" sPrint
                  
                  if {$sCatchTest == "trigger"} {
   
                     regsub -all "\134\051," $sPrint "|" sPrint
                     set lsTrig [split $sPrint "|"]
   
                     foreach sTrig $lsTrig {
                        regsub ":" $sTrig "|" sTrig
                        set lslsTrig [split $sTrig |]
                        set sTrigEventType [string tolower [lindex $lslsTrig 0]]
                        
                        if {$sTrigEventType == "checkincheck"} {
                           set sTrigEvent checkin
                           set sTrigType check
                        } elseif {$sTrigEventType == "checkinaction"} {
                           set sTrigEvent checkin
                           set sTrigType action
                        } elseif {$sTrigEventType == "checkinoverride"} {
                           set sTrigEvent checkin
                           set sTrigType override
                        } elseif {$sTrigEventType == "checkoutcheck"} {
                           set sTrigEvent checkout
                           set sTrigType check
                        } elseif {$sTrigEventType == "checkoutaction"} {
                           set sTrigEvent checkout
                           set sTrigType action
                        } elseif {$sTrigEventType == "checkoutoverride"} {
                           set sTrigEvent checkout
                           set sTrigType override
                        } elseif {[regsub "check" $sTrigEventType "" sTrigEvent] == 1} {
                           set sTrigType "check"
                        } elseif {[regsub "action" $sTrigEventType "" sTrigEvent] == 1} {
                           set sTrigType "action"
                        } elseif {[regsub "override" $sTrigEventType "" sTrigEvent] == 1} {
                           set sTrigType "override"
                        }
                        
                        set slsTrigProg [lindex $lslsTrig 1]
                        regsub "\134\050" $slsTrigProg "|" slsTrigProg
                        set lslsTrigProg [split $slsTrigProg |]
                        set sProgram [lindex $lslsTrigProg 0]
                        set sInput [lindex $lslsTrigProg 1]
                        regsub "\134\051" $sInput "" sInput
                        append sFile "$sSchemaType\t$sName\t$sStateName\t$sTrigEvent\t$sTrigType\t$sProgram\t$sInput\n"
                     }
                  } else {
                     set sTrigType $sCatchTest
                     regsub " input " $sPrint "|" sPrint
                     set lsTrig [split $sPrint |]
                     set sProgram [string trim [lindex $lsTrig 0]]
                     regsub -all "'" $sProgram "" sProgram
                     set sInput [string trim [lindex $lsTrig 1]]
                     regsub -all "'" $sInput "" sInput
                     append sFile "$sSchemaType\t$sName\t$sStateName\tevent\t$sTrigType\t$sProgram\t$sInput\n"
                  }
               }
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Trigger data loaded in file $sPath"
}
