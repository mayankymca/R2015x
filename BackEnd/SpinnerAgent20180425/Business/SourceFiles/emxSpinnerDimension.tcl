#########################################################################*2014x
#
# @progdoc      emxSpinnerDimension.tcl vM2013 (Build 7.4.1)
#
# @Description: Procedures for running in Dimensions w/Units
#
# @Parameters:  Returns 0 if successful, 1 if not
#
# @Usage:       Utilized by emxSpinnerAgent.tcl
#
# @progdoc      Copyright (c) ENOVIA ENOVIA 2007
#
#########################################################################
#
# @Modifications: FirstName LastName MM/DD/YYYY - Modification
#
#########################################################################

# Procedure to set setting names and values
   proc pSetSetting {sSchName slsStgName slsStgValue} {
      global lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgPlan aStgActual sRangeDelim aCol sLogFileError bOverlay bAdd lsDel
      set lsStgNamePlan [pTrimList $slsStgName]
      regsub -all "\134\174\134\174" $slsStgValue "<OR>" slsStgValue
      regsub -all "&&" $slsStgValue "<AND>" slsStgValue
      set lsStgValuePlanTemp [split $slsStgValue $sRangeDelim]
      set lsStgValuePlan ""
      foreach sStgValue $lsStgValuePlanTemp {
      	 regsub -all "<OR>" $sStgValue "\174\174" sStgValue
      	 regsub -all "<AND>" $sStgValue "\134\&\134\&" sStgValue
         regsub -all "<PIPE>" $sStgValue "|" sStgValue
		 regsub -all "<PIPE:>" $sStgValue "\134|" sStgValue
         lappend lsStgValuePlan [string trim $sStgValue]
      }
      set lsStgNameActual [split [pQuery "" "print dimension \042$sSchName\042 select unit\134\133$aCol(1)\134\135.setting.name dump |"] |]
      set lsStgValueActual [split [pQuery "" "print dimension \042$sSchName\042 select unit\134\133$aCol(1)\134\135.setting.value dump ^"] ^]
      foreach sStgNameActual $lsStgNameActual sStgValueActual $lsStgValueActual {array set aStgActual [list $sStgNameActual $sStgValueActual]}
      if {[llength $lsStgNamePlan] != [llength $lsStgValuePlan]} {
         set iLogFileErr [open $sLogFileError a+]
         puts $iLogFileErr "\nERROR: 'dimension' '$sSchName' unit '$aCol(1)' setting name and value lists are not the same length"
         close $iLogFileErr
         if {[llength $lsStgNamePlan] > [llength $lsStgValuePlan] && [string first "<OR>" $slsStgValue] > -1} {
            set iLogFileErr [open $sLogFileError a+]
            puts $iLogFileErr "Be sure to leave a space between '|'s if null values are intended vs. double '|'s"
            close $iLogFileErr
         }
         return 1
      }
      if {$bOverlay} {
      	 if {$lsStgNamePlan == "<NULL>"} {
      	    set lsStgNamePlan [list ]
      	    set lsStgValuePlan [list ]
      	 } elseif {$bAdd != "TRUE" && $lsStgNamePlan == ""} {
      	    set lsStgNamePlan $lsStgNameActual
      	    set lsStgValuePlan $lsStgValueActual
      	 } else {
      	    set lsTemp [pMergeList $lsStgNamePlan $lsStgValuePlan $lsStgNameActual $lsStgValueActual ""]
      	    set lsStgNamePlan [lindex $lsTemp 0]
      	    set lsStgValuePlan [lindex $lsTemp 1]
      	 }
      }
      foreach sStgNamePlan $lsStgNamePlan sStgValuePlan $lsStgValuePlan {array set aStgPlan [list $sStgNamePlan $sStgValuePlan]}
      return 0
   }

# Procedure to process setting names and values
   proc pSetting {sSchName sSchUnit} {
      global lsStgNameActual lsStgValueActual lsStgNamePlan lsStgValuePlan aStgActual aStgPlan
      foreach sStgNameA $lsStgNameActual sStgValueA $lsStgValueActual sStgNameP $lsStgNamePlan sStgValueP $lsStgValuePlan {
         if {$sStgNameA != ""} {
            if {[lsearch $lsStgNamePlan $sStgNameA] < 0} {
               pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 remove setting \042$sStgNameA\042"
            } elseif {$aStgPlan($sStgNameA) != $sStgValueA} {
               set sModSettingValue [pRegSubEvalEscape $aStgPlan($sStgNameA)]
               if {[string first "\047" $sModSettingValue] < 0 && [string first "javascript" [string tolower $sModSettingValue]] < 0} {
                  pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 setting \042$sStgNameA\042 '$sModSettingValue'"
               } else {
                  pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 setting \042$sStgNameA\042 \042$sModSettingValue\042"
               }
               array set aStgActual [list $sStgNameA $aStgPlan($sStgNameA)]
            }
         }
         if {$sStgNameP != ""} {
            if {[lsearch $lsStgNameActual $sStgNameP] < 0} {
               set sModSettingValue [pRegSubEvalEscape $sStgValueP]
               if {[string first "\047" $sModSettingValue] < 0 && [string first "javascript" [string tolower $sModSettingValue]] < 0} {
                  pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 setting \042$sStgNameP\042 '$sModSettingValue'"
               } else {
                  pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 setting \042$sStgNameP\042 \042$sModSettingValue\042"
               }
            } elseif {$aStgActual($sStgNameP) != $sStgValueP} {
               set sModSettingValue [pRegSubEvalEscape $sStgValueP]
               if {[string first "\047" $sModSettingValue] < 0 && [string first "javascript" [string tolower $sModSettingValue]] < 0} {
                  pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 setting \042$sStgNameP\042 '$sModSettingValue'"
               } else {
                  pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 setting \042$sStgNameP\042 \042$sModSettingValue\042"
               }
               array set aStgActual [list $sStgNameP $sStgValueP]
            }
         }
      }
   }

# Procedure to set system names and units
   proc pSetSysNameUnit {sSchName slsSysName slsSysUnit} {
      global lsSysNamePlan lsSysUnitPlan lsSysNameActual lsSysUnitActual aSysPlan aSysActual sRangeDelim aCol sLogFileError bOverlay bAdd lsDel
      set lsSysNamePlan [pTrimList $slsSysName]
      set lsSysUnitPlan [pTrimList $slsSysUnit]
      set lsSysNameActual [list ]
      set lsSysUnitActual [list ]
      set lsPrint [split [pQuery "" "print dimension \042$aCol(0)\042"] \n]
      set bTrip "FALSE"
      foreach sPrint $lsPrint {
         set sPrint [string trim $sPrint]
         if {[string range $sPrint 0 3] == "unit" && [string first $aCol(1) $sPrint] > 3} {
            set bTrip TRUE
         } elseif {$bTrip && [string range $sPrint 0 3] == "unit"} {
            break
         } elseif {$bTrip} {
            if {[string range $sPrint 0 5] == "system"} {
               regsub "system" $sPrint "" sPrint
               regsub " to unit " $sPrint "\|" sPrint
               set lsSysNameUnit [split $sPrint "|"]
               lappend lsSysNameActual [string trim [lindex $lsSysNameUnit 0]]
               lappend lsSysUnitActual [string trim [lindex $lsSysNameUnit 1]]
            }
         }
      }
      foreach sSysNameActual $lsSysNameActual sSysUnitActual $lsSysUnitActual {array set aSysActual [list $sSysNameActual $sSysUnitActual]}
      if {[llength $lsSysNamePlan] != [llength $lsSysUnitPlan]} {
         set iLogFileErr [open $sLogFileError a+]
         puts $iLogFileErr "\nERROR: 'dimension' '$sSchName' unit '$aCol(1)' SystemName and SystemUnit lists are not the same length"
         close $iLogFileErr
         return 1
      }
      if {$bOverlay} {
      	 if {$lsSysNamePlan == "<NULL>"} {
      	    set lsSysNamePlan [list ]
      	    set lsSysUnitPlan [list ]
      	 } elseif {$bAdd != "TRUE" && $lsSysNamePlan == ""} {
      	    set lsSysNamePlan $lsSysNameActual
      	    set lsSysUnitPlan $lsSysUnitActual
      	 } else {
      	    set lsTemp [pMergeList $lsSysNamePlan $lsSysUnitPlan $lsSysNameActual $lsSysUnitActual ""]
      	    set lsSysNamePlan [lindex $lsTemp 0]
      	    set lsSysUnitPlan [lindex $lsTemp 1]
      	 }
      }
      foreach sSysNamePlan $lsSysNamePlan sSysUnitPlan $lsSysUnitPlan {array set aSysPlan [list $sSysNamePlan $sSysUnitPlan]}
      return 0
   }

# Procedure to process system names and units
   proc pSysNameUnit {sSchName sSchUnit} {
      global lsSysNameActual lsSysUnitActual lsSysNamePlan lsSysUnitPlan aSysActual aSysPlan
      foreach sSysNameA $lsSysNameActual sSysUnitA $lsSysUnitActual sSysNameP $lsSysNamePlan sSysUnitP $lsSysUnitPlan {
         if {$sSysNameA != ""} {
            if {[lsearch $lsSysNamePlan $sSysNameA] < 0} {
               pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 remove system \042$sSysNameA\042 to unit \042$sSysUnitA\042"
            } elseif {$aSysPlan($sSysNameA) != $sSysUnitA} {
               pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 add system \042$sSysNameA\042 to unit \042$aSysPlan($sSysNameA)\042"
               array set aSysActual [list $sSysNameA $aSysPlan($sSysNameA)]
            }
         }
         if {$sSysNameP != ""} {
            if {[lsearch $lsSysNameActual $sSysNameP] < 0} {
               pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 add system \042$sSysNameP\042 to unit \042$sSysUnitP\042"
            } elseif {$aSysActual($sSysNameP) != $sSysUnitP} {
               pMqlCmd "escape mod dimension \042$sSchName\042 mod unit \042$sSchUnit\042 add system \042$sSysNameP\042 to unit \042$sSysUnitP\042"
               array set aSysActual [list $sSysNameP $sSysUnitP]
            }
         }
      }
   }

# Procedure to analyze dimensions
proc pAnalyzeDimension {} {
   global aCol aDat bOverlay bAdd sSchemaType lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgPlan aStgActual lsSysNamePlan lsSysUnitPlan lsSysNameActual lsSysUnitActual aSysPlan aSysActual sRangeDelim sLogFileError lsDel bRepeat sUnitNamePlan sUnitNameActual bDeleteFlag sUnitToDelete
   switch $sSchemaType {
      unit {
		set bDeleteFlag "FALSE"
		set sUnitToDelete ""
		set iLenMinTwo [expr [string length $aCol(1)] -2]
		if {[string first "<<" $aCol(1)] >= 0 && [string first ">>" $aCol(1)] == $iLenMinTwo} {
			#Find actual unit name
			set sUnitStr $aCol(1)
			if { [string match "<<*>>" $aCol(1)] == 1 } {
				set startStr [split $aCol(1) <<]
				set endStr [lindex $startStr 2]
				set sUnitStr [split $endStr >>]
				set sUnitToDelete [lindex $sUnitStr 0]
			}		
			set bDeleteFlag "TRUE"
		}	
		 
		 if {$bAdd != "TRUE"} {set sUnitNameActual [split [ mql print dimension "$aCol(0)" select unit.name dump | ] |]}
		 
		 if {$bDeleteFlag == "FALSE"} {
			 set bReturn [pSetSetting $aCol(0) $aCol(6) $aCol(7)]
			 if {$bReturn} {
				puts "\nError - Review log file '$sLogFileError', correct problem(s) and restart"
				return 1
			 }
			 set bReturn [pSetSysNameUnit $aCol(0) $aCol(8) $aCol(9)]
			 if {$bReturn} {
				puts "\nError - Review log file '$sLogFileError', correct problem(s) and restart"
				return 1
			 }
			 foreach iDat [list 2 3 4 5 10] sProperty [list label description multiplier offset "default"] {set aDat($iDat) [pQuery "" "print dimension \042$aCol(0)\042 select unit\134\133$aCol(1)\134\135.$sProperty dump"]}
			 set aCol(10) [pCompareAttr $aCol(10) notdefault "default" true true]
			 set aDat(10) [pCompareAttr $aDat(10) notdefault "default" true true]
			 if {$bOverlay} {pOverlay [list 2 3 4 5]}
			 pSetAction "Modify dimension $aCol(0) unit $aCol(1)"
		 }
      }
   }
   return 0
}

# Procedure to process dimensions
proc pProcessDimension {} {
   global aCol aDat bOverlay bAdd sHidden sHiddenActual sSchemaType bEscQuote bScan sNumberActual lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgActual aStgPlan lsSysNamePlan lsSysUnitPlan lsSysNameActual lsSysUnitActual aSysPlan aSysActual sSpinStamp sUnitNamePlan sUnitNameActual bDeleteFlag sUnitToDelete
   switch $sSchemaType {
      dimension {
         if {$bAdd} {
            pMqlCmd "add dimension \042$aCol(0)\042 $sHidden"
           
         } else {
           
            if {$sHidden != $sHiddenActual} {pMqlCmd "mod dimension \042$aCol(0)\042 $sHidden"}
           
         }
      } unit {
		 if {$bDeleteFlag == "TRUE"} {pMqlCmd "mod dimension \042$aCol(0)\042 remove unit \042$sUnitToDelete\042"}		 
	  
		 if {$bDeleteFlag == "FALSE"} {
			set bAddUnitFlag "TRUE"
			 foreach sEachUnit $sUnitNameActual {
				if {$aCol(1) == $sEachUnit} {
					set bAddUnitFlag "FALSE"
					break
				}
			 }
			 
			 if {$bAddUnitFlag == "TRUE"} {pMqlCmd "mod dimension \042$aCol(0)\042 add unit \042$aCol(1)\042"}
			 
			if {$aCol(10) != $aDat(10)} {
				if {$aCol(10) == "notdefault"} {pMqlCmd "mod dimension \042$aCol(0)\042 mod unit \042$aCol(1)\042 $aCol(10)"}
			 }
			 
			 if {$aCol(2) != $aDat(2)} {
				set aCol(2) [pRegSubEvalEscape $aCol(2)]
				pMqlCmd "mod dimension \042$aCol(0)\042 mod unit \042$aCol(1)\042 label \042$aCol(2)\042"
			 }
			 if {$aCol(3) != $aDat(3)} {
				set aCol(3) [pRegSubEvalEscape $aCol(3)]
				pMqlCmd "mod dimension \042$aCol(0)\042 mod unit \042$aCol(1)\042 unitdescription \042$aCol(3)\042"
			 }
			 if {$aCol(4) != $aDat(4)} {
				pMqlCmd "mod dimension \042$aCol(0)\042 mod unit \042$aCol(1)\042 multiplier \042$aCol(4)\042"
			 }
			 if {$aCol(5) != $aDat(5)} {
				pMqlCmd "mod dimension \042$aCol(0)\042 mod unit \042$aCol(1)\042 offset \042$aCol(5)\042"
			 }
			 
			 if {$aCol(10) != $aDat(10)} {
				if {$aCol(10) == "default"} {pMqlCmd "mod dimension \042$aCol(0)\042 mod unit \042$aCol(1)\042 $aCol(10)"}
			 }
			 
			 pSetting $aCol(0) $aCol(1)
			 pSysNameUnit $aCol(0) $aCol(1)
		 }
      }
   }
   return 0
}

