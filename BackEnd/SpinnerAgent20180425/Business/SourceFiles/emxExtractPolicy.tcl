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
   set sMxVersion [mql get env MXVERSION]
   
#  Set up array for symbolic name mapping
   set lsPropertyName [split [mql print program eServiceSchemaVariableMapping.tcl select property.name dump |] |]
   set lsPropertyTo [split [mql print program eServiceSchemaVariableMapping.tcl select property.to dump |] |]
   set sTypeReplace "policy "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "policy"} {
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
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerPolicyData$sAppend.xls"
   set lsPolicy [split [mql list policy $sFilter] \n]
   
   set sPath2 "$sSpinnerPath/Business/SpinnerPolicyStateData$sAppend.xls"
   
   ######## Added By SL Team for Policy issue (Policy xls sheet name change)####### 
   set sPath3 "$sSpinnerPath/Business/SpinnerPolicyStateSignatureData$sAppend.xls"
    ######## END ####### 
	set sFile "Name\tRegistry Name\tDescription\tStore\tHidden (boolean)\tTypes (use \"|\" delim)\tFormats (use \"|\" delim)\tDefault Format\tLocking (boolean)\tAllstate (boolean)\tMinor Rev Seq (use 'continue' for '...')\tMajor Rev Seq (use 'continue' for '...')\tDelimiter\tIcon File\n"
			 
	set sFile2 "Policy Name\tState Name\tState Registry Name\tVersion (boolean)\tPromote (boolean)\tCheckout History (boolean)\tNotify Users (use \"|\" delim)\tNotify Message\tRoute User\tRoute Message\tMinor Revision (boolean)\tMajor Revision (boolean)\tPublished (boolean)\tEnforce Reserve Access (boolean)\tBefore State Name\tIcon File\n"
	
   set sFile3 "Policy Name\tState Name\tSignature Name\tBranch State\tFilter\n"

   if {!$bTemplate} {
      foreach sPolicy $lsPolicy {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print policy $sPolicy select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print policy $sPolicy select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print policy $sPolicy select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sPolicy)} sMsg
            regsub -all " " $sPolicy "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sPolicy
            }
			set sMajorRevSeq ""
			set sMinorRevSeq ""
			set sDelimiter ""
			
		   set sMinorRevSeq [mql print policy $sPolicy select minorsequence dump]
		   regsub -all "\\\056\\\056\\\056" $sMinorRevSeq "continue" sMinorRevSeq
		   set sMajorRevSeq [mql print policy $sPolicy select majorsequence dump]
		   regsub -all "\\\056\\\056\\\056" $sMajorRevSeq "continue" sMajorRevSeq
		   set sDelimiter [mql print policy $sPolicy select delimiter dump]
			
            set bHidden [mql print policy $sPolicy select hidden dump]
            set bLocking [mql print policy $sPolicy select islockingenforced dump]
            
            set slsType [mql print policy $sPolicy select type dump " | "]
            set slsFormat [mql print policy $sPolicy select format dump " | "]
            set slsState [mql print policy $sPolicy select state dump " | "]
			set bAllstate [mql print policy $sPolicy select allstate dump " | "]
            
            set lsState [mql print policy $sPolicy select state dump ]
            foreach sState $lsState {
            	  array set aStateOrig [list $sState ""]
            } 
            set lsStateProp [split [mql print policy $sPolicy select property dump |] |]
            foreach sStateProp $lsStateProp {
               if {[string first "state_" $sStateProp] == 0} {
                  regsub "state_" $sStateProp "" sStateProp
                  regsub "value " $sStateProp "" sStateProp
                  regsub " " $sStateProp "|" sStateProp
                  set lsStateName [split $sStateProp |]
                  set sStateOrig [lindex $lsStateName 0]
                  set sStateName [lindex $lsStateName 1]
                  array set aStateOrig [list $sStateName $sStateOrig]
               }
            }
      
            set lsState [split $slsState |]
            set slsStateOrig ""
            set bFirstFlag TRUE
            foreach sState $lsState {
               set sState [string trim $sState]
               set sStateOrig ""
               catch {set sStateOrig $aStateOrig($sState)} sMsg
               regsub -all " " $sState "" sStateTest
               if {$sStateTest == $sStateOrig} {
                  set sStateOrig $sState
               }
               if {$bFirstFlag == "TRUE"} {
                  set slsStateOrig $sStateOrig
                  set bFirstFlag FALSE
               } else {
                  append slsStateOrig " | $sStateOrig"
               }
            }
      
            set sStore [mql print policy $sPolicy select store dump]
            set sDefaultFormat [mql print policy $sPolicy select defaultformat dump]
            set sDescription [mql print policy $sPolicy select description dump]
            if {$sMxVersion >= 10.8} {set bAllstate [mql print policy $sPolicy select allstate dump]}
			
			append sFile "$sName\t$sOrigName\t$sDescription\t$sStore\t$bHidden\t$slsType\t$slsFormat\t$sDefaultFormat\t$bLocking\t$bAllstate\t$sMinorRevSeq\t$sMajorRevSeq\t$sDelimiter\n"
# Policy State
            set lsState [split [mql print policy $sPolicy select state dump |] |]
			set count 1
            foreach sState $lsState {
			  set sMinorRev [string tolower [mql print policy $sPolicy select state\[$sState\].minorrevisionable dump]]
			  set sMajorRev [string tolower [mql print policy $sPolicy select state\[$sState\].majorrevisionable dump]]
			  set sPublish [string tolower [mql print policy $sPolicy select state\[$sState\].published dump]]
			  set sEnforceReserveAccess [string tolower [mql print policy $sPolicy select state\[$sState\].enforcereserveaccess dump]]
			   
               set sVersion [string tolower [mql print policy $sPolicy select state\[$sState\].versionable dump]]
               set sPromote [string tolower [mql print policy $sPolicy select state\[$sState\].autopromote dump]]
               set sCheckout [string tolower [mql print policy $sPolicy select state\[$sState\].checkouthistory dump]]
               set sNotifyMsg [mql print policy $sPolicy select state\[$sState\].notify dump]
               set sRouteMsg [mql print policy $sPolicy select state\[$sState\].route dump]
               set sBeforeState [lindex $lsState $count]
               set slsNotify ""
               set lsNotifyTemp [split [mql print policy $sPolicy] \n]
               set bTrip "FALSE"
               foreach sNotifyTemp $lsNotifyTemp {
                  set sNotifyTemp [string trim $sNotifyTemp]
                  if {$sNotifyTemp == "state $sState"} {
                     set bTrip TRUE
                  } elseif {$bTrip == "TRUE" && [string range $sNotifyTemp 0 4] == "state"} {
                     break
                  } elseif {$bTrip == "TRUE"} {
                     if {[string range $sNotifyTemp 0 5] == "notify"} {
                        regsub "notify " $sNotifyTemp "" sNotifyTemp
                        regsub -all "'" $sNotifyTemp "" sNotifyTemp
                        if {$sNotifyMsg != "" } {regsub " $sNotifyMsg" $sNotifyTemp "" sNotifyTemp}
                        set sNotifyTemp [string trim $sNotifyTemp]
                        regsub -all "," $sNotifyTemp " | " slsNotify
                        break
                     }
                  } 
               }
               
               set sRoute ""
               set lsRouteTemp [split [mql print policy $sPolicy] \n]
               set bTrip "FALSE"
               foreach sRouteTemp $lsRouteTemp {
                  set sRouteTemp [string trim $sRouteTemp]
                  if {$sRouteTemp == "state $sState"} {
                     set bTrip TRUE
                  } elseif {$bTrip == "TRUE" && [string range $sRouteTemp 0 4] == "state"} {
                     break
                  } elseif {$bTrip == "TRUE"} {
                     if {[string range $sRouteTemp 0 4] == "route"} {
                        regsub "route " $sRouteTemp "" sRouteTemp
                        regsub -all "'" $sRouteTemp "" sRouteTemp
                        if {$sRouteMsg != ""} {regsub " $sRouteMsg" $sRouteTemp "" sRouteTemp}
                        set sRoute [string trim $sRouteTemp]
                        break
                     }
                  }
               }
			   catch {set sStateOrig $aStateOrig($sState)} sMsg
			   
				append sFile2 "$sPolicy\t$sState\t$sStateOrig\t$sVersion\t$sPromote\t$sCheckout\t$slsNotify\t$sNotifyMsg\t$sRoute\t$sRouteMsg\t$sMinorRev\t$sMajorRev\t$sPublish\t$sEnforceReserveAccess\t$sBeforeState\t\n"
			   # Policy State Signature
               set lsSignature [split [mql print policy $sPolicy select state\[$sState\].signature dump |] |]
               foreach sSignature $lsSignature {
					set slsApprove [mql print policy $sPolicy select state\[$sState\].signature\[$sSignature\].approve dump " | "]
					set slsReject [mql print policy $sPolicy select state\[$sState\].signature\[$sSignature\].reject dump " | "]
					set slsIgnore [mql print policy $sPolicy select state\[$sState\].signature\[$sSignature\].ignore dump " | "]
					
					set sStateBranch [mql print policy $sPolicy select state\[$sState\].signature\[$sSignature\].branch dump " | "]
					set sStateSigFilter [mql print policy $sPolicy select state\[$sState\].signature\[$sSignature\].filter dump " | "]
					if {$slsApprove == "" && $slsReject == "" && $slsIgnore == ""} {
						append sFile3 "$sPolicy\t$sState\t$sSignature\t$sStateBranch\t$sStateSigFilter\n"
					}
               }
			   
			   incr count
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Policy data loaded in file $sPath"
   set iFile [open $sPath2 w]
   puts $iFile $sFile2
   close $iFile
   puts "Policy State data loaded in file $sPath2"
   set iFile [open $sPath3 w]
   puts $iFile $sFile3
   close $iFile
   puts "Policy State Signature data loaded in file $sPath3"

}
