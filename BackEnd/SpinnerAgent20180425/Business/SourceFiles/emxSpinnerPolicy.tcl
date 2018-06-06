#########################################################################*2014x
#
# @progdoc      emxSpinnerPolicy.tcl vM2012 (Build 11.10.1)
#
# @Description: Procedures for running in Policies
#
# @Parameters:  Returns 0 if successful, 1 if not
#
# @Usage:       Utilized by emxSpinnerAgent.tcl
#
# @progdoc      Copyright (c) ENOVIA Inc. 2005
#
#########################################################################
#
# @Modifications: See SchemaAgent_ReadMe.htm
#
#########################################################################

# Procedure to analyze policies
proc pAnalyzePolicy {} {
   global aCol aDat bOverlay bAdd lsTypePlan lsTypeActual lsTypeRetain slsTypeRetain lsFormatPlan lsFormatActual lsSchemaType bRetainBusObject bScan sMxVersion 
   regsub -all "continue" $aCol(10) "..." aCol(10)
   regsub -all "continue" $aCol(11) "..." aCol(11)
   set lsTypePlan [pTrimList $aCol(5)]
   if {[lsearch $lsSchemaType type] >= 0} {set lsTypePlan [pCheckNameChange $lsTypePlan type]}
   set lsFormatPlan [pTrimList $aCol(6)]
   if {[lsearch $lsSchemaType format] >= 0} {set lsFormatPlan [pCheckNameChange $lsFormatPlan format]}
   if {$aCol(7) != ""} {set aCol(7) [pCheckNameChange $aCol(7) format]}
   set aCol(8) [pCompareAttr $aCol(8) notenforce enforce true true]
   
   foreach sValue [list lsTypeActual lsFormatActual lsTypeRetain] {
      set "$sValue" [list ]
   }
   
   if {$bAdd != "TRUE"} {
      foreach iDat [list 3 7 10 11 12] sProperty [list store defaultformat minorsequence majorsequence delimiter] {set aDat($iDat) [pPrintQuery "" $sProperty "" ""]}
      set bEnforceActual [pPrintQuery "" islockingenforced "" ""]
      set aDat(8) [pCompareAttr $bEnforceActual notenforce enforce true false]
      set lsTypeActual [pPrintQuery "" type | spl]
      set lsFormatActual [pPrintQuery "" format | spl]
  
      set aDat(9) [pPrintQuery "FALSE" allstate "" ""]
   }
   if {$bOverlay} {
      pOverlay [list 3 7 8 9 10 11 12]
      set lsTypePlan [pOverlayList $lsTypePlan $lsTypeActual]
      set lsFormatPlan [pOverlayList $lsFormatPlan $lsFormatActual]
   }
   if {$bAdd != "TRUE" && $bRetainBusObject && $bScan != "TRUE"} {
      foreach sTypeActual $lsTypeActual {if {[lsearch $lsTypePlan $sTypeActual] > -1} {lappend lsTypeRetain $sTypeActual}}
      set slsTypeRetain [join $lsTypeRetain ,]
   }
}                                    

# Procedure to process policies
proc pProcessPolicy {} {
   global aCol aDat bAdd lsTypePlan lsTypeActual lsTypeRetain slsTypeRetain lsFormatPlan lsFormatActual sHidden sHiddenActual lsSchemaType sSpinDir aSchemaTitle bRetainBusObject iBusObjCommit bScan slsVault sLogFileDir bOut sMxVersion resultappend
   if {$bAdd} {
	 set aCol(10) [pRegSubEvalEscape $aCol(10)]
	 set aCol(11) [pRegSubEvalEscape $aCol(11)]
	 if {$aCol(10) != "" && $aCol(11) != ""} {
		pMqlCmd "escape add policy \042$aCol(0)\042 minorsequence '$aCol(10)' majorsequence '$aCol(11)' delimiter $aCol(12) $sHidden $aCol(8)"
	 } elseif {$aCol(11) == ""} {
		pMqlCmd "escape add policy \042$aCol(0)\042 minorsequence '$aCol(10)' $sHidden $aCol(8)"
	 } else {
		pMqlCmd "escape add policy \042$aCol(0)\042 majorsequence '$aCol(11)' $sHidden $aCol(8)"
	 }
      
      pPlanAdd $lsTypePlan policy $aCol(0) "add type" ""
      pPlanAdd $lsFormatPlan policy $aCol(0) "add format" ""

      if {$aCol(7) != ""} {pMqlCmd "mod policy \042$aCol(0)\042 defaultformat \042$aCol(7)\042"}
      if {$aCol(3) != ""} {pMqlCmd "mod policy \042$aCol(0)\042 store \042$aCol(3)\042"}
      if {$aCol(9) == "TRUE"} {pMqlCmd "mod policy \042$aCol(0)\042 add allstate"}
   } else {
	     if {$aCol(10) != $aDat(10) || $aCol(11) != $aDat(11) || $sHidden != $sHiddenActual || $aCol(8) != $aDat(8)} {
            if {$aCol(10) != ""} {set aCol(10) [pRegSubEvalEscape $aCol(10)]}
            if {$aCol(11) != ""} {set aCol(11) [pRegSubEvalEscape $aCol(11)]}
            pMqlCmd "escape mod policy \042$aCol(0)\042 minorsequence '$aCol(10)' majorsequence '$aCol(11)' $sHidden $aCol(8)"
	     }
		 if {$aCol(12) != $aDat(12)} {
			pMqlCmd "escape mod policy \042$aCol(0)\042 delimiter $aCol(12)"
		 }
      if {$aCol(3) == "" && $aDat(3) != ""} {
         set resultappend "ERROR: Policy '$aCol(0)' store '$aDat(4)' may not be changed back to <null> once set!"
         return 1
      } elseif {$aCol(3) != $aDat(3)} {
         pMqlCmd "mod policy \042$aCol(0)\042 store \042$aCol(3)\042"
      }
      pPlanActualAddDel $lsTypeActual "" $lsTypePlan policy "" $aCol(0) "remove type" "add type" ""
      pPlanActualAddDel $lsFormatActual "" $lsFormatPlan policy "" $aCol(0) "remove format" "add format" ""
      if {$aCol(7) == "" && $aDat(7) != ""} {
         set resultappend "ERROR: Policy '$aCol(0)' default format '$aDat(7)' may not be set to <null> once set!"
         return 1
      } elseif {$aCol(7) != $aDat(7)} {
         pMqlCmd "mod policy \042$aCol(0)\042 defaultformat \042$aCol(7)\042"
      }
      if {$aCol(9) != "" && $aCol(9) != $aDat(9)} {
         if {$aCol(9) == "TRUE"} {
            pMqlCmd "mod policy \042$aCol(0)\042 add allstate"
         } else {
			if { $aDat(9) == "TRUE" || $aDat(9) == "true" } {
				pMqlCmd "mod policy \042$aCol(0)\042 remove allstate"
			}
         }
      }
   }
   return 0
}

# Procedure to analyze policy states
proc pAnalyzeState {} {
   global aCol aDat bOverlay bAdd lsNotifyPlan lsNotifyActual bUseAccessField sSymbolicActual sSymbolicPlan bAddState sStateToDelete bDeleteFlag lsPlanOrder sPrevPolicy bResequenceStates
   
   #KYB Start - Overlay mode
   set bAddState "TRUE"
   set bDeleteFlag "FALSE"
	set iLenMinTwo [expr [string length $aCol(1)] -2]
	if {[string first "<<" $aCol(1)] >= 0 && [string first ">>" $aCol(1)] == $iLenMinTwo} {
		#Find actual user name
		set sStateStr $aCol(1)
		if { [string match "<<*>>" $aCol(1)] == 1 } {
			set startStr [split $aCol(1) <<]
			set endStr [lindex $startStr 2]
			set sStateStr [split $endStr >>]
			set sStateToDelete [lindex $sStateStr 0]
		}		
		set bDeleteFlag "TRUE"
		set bAddState "FALSE"
	}

   set lsStateActual [pPrintQuery "" state | spl]
   foreach sStateActual $lsStateActual {
		if { [string match "$aCol(1)" "$sStateActual"] == 1 } {
			set bAddState "FALSE"
			break
		}
   }
   
   set lsStateRef [pPrintQuery "" property.value | spl]
   set lsSymbolicRef [pPrintQuery "" property.name | spl]
   set sSymbolicActual ""
   
   foreach sStateRef $lsStateRef sSymbolicRef $lsSymbolicRef {
         if {[string range $sSymbolicRef 0 5] == "state_"} {    
			 if { $sStateRef == "$aCol(1)" } {
				set sSymbolicActual $sSymbolicRef
			 }
      	  }
   }
   
   set sSymbolicPlan "state_"
   if { $aCol(2) == "" } {
		set sSymbolicRef $aCol(1)
		regsub -all " " $sSymbolicRef "" sSymbolicRef
		append sSymbolicPlan $aCol(1)
   } else {
	    set sSymbolicRef $aCol(2)
		regsub -all " " $sSymbolicRef "" sSymbolicRef
		append sSymbolicPlan $sSymbolicRef
   }
   #KYB End - Overlay mode
   
    foreach iCol [list 3 4 5 10 12] {set aCol($iCol) [pStateCompareAttr $aCol($iCol) false true true true]}
    foreach iCol [list 5 11] {set aCol($iCol) [pStateCompareAttr $aCol($iCol) true false false true]}
	set aCol(13) [pCompareAttr $aCol(13) notenforcereserveaccess enforcereserveaccess true true]
	set lsNotifyPlan [pTrimList $aCol(6)]

   set lsNotifyActual [list ]
	# Major/Minor Mod - ION - 10/1/2012
	if {$bAddState != "TRUE"} {
		foreach iDat [list 3 4 5 10 11 12] sProperty [list versionable autopromote checkouthistory minorrevisionable majorrevisionable published enforcereserveaccess] {set aDat($iDat) [pPrintQuery "" "state\134\133$aCol(1)\134\135.$sProperty" "" str]}
		set bEnforceReserveAccessActual [pPrintQuery "" "state\134\133$aCol(1)\134\135.enforcereserveaccess" "" ""]
		set aDat(13) [pCompareAttr $bEnforceReserveAccessActual notenforcereserveaccess enforcereserveaccess true false]
		set aDat(7) [pPrintQuery "" "state\134\133$aCol(1)\134\135.notify" "" ""]
		set aDat(9) [pPrintQuery "" "state\134\133$aCol(1)\134\135.route" "" ""]
		set aDat(8) ""
		
	   set lsPrint [split [pQuery "" "print policy \042$aCol(0)\042"] \n]
	   set bTrip "FALSE"
	   foreach sPrint $lsPrint {
		  set sPrint [string trim $sPrint]
		  if {$sPrint == "state $aCol(1)"} {
			 set bTrip TRUE
		  } elseif {$bTrip && [string range $sPrint 0 4] == "state"} {
			 break
		  } elseif {$bTrip} {
			 if {[string range $sPrint 0 5] == "notify"} {
				regsub "notify " $sPrint "" sPrint
				regsub -all "'" $sPrint "" sPrint
				if {$aDat(7) != ""} {regsub " $aDat(7)" $sPrint "" sPrint}
				set sPrint [string trim $sPrint]
				set lsNotifyActual [split $sPrint ","]
			 } elseif {[string range $sPrint 0 4] == "route"} {
				regsub "route " $sPrint "" sPrint
				regsub -all "'" $sPrint "" sPrint
				if {$aDat(9) != ""} {regsub " $aDat(9)" $sPrint "" sPrint}
				set aDat(8) [string trim $sPrint]
			 }
		  }
	   }
	}
   pSetAction "Modify policy $aCol(0) state $aCol(1)"
   if {$bOverlay} {
         pStateOverlay [list 3 4 5 7 8 9 10 11 12 13]
		set lsNotifyPlan [pOverlayList $lsNotifyPlan $lsNotifyActual]
   }
   
	if {$sPrevPolicy ==  ""} {set sPrevPolicy $aCol(0)}
	if {$bDeleteFlag != "TRUE"} {
		if {$sPrevPolicy ==  $aCol(0)} {
			if {[llength $lsPlanOrder] > 1} {
				lappend lsPlanOrder [list $aCol(1) $aCol(14)]
			} else { 
				lappend lsPlanOrder [list $aCol(1) $aCol(14)]
			}
		}
	}
}
                                   
#******* Added By SL Team for Policy issue (Policy having branches is screwed up with User Agent being populated etc in Signature approver fields) *******# 
proc pAnalyzeDataSignature {} {
	global lsFileExtSkip aCol lsMasterSignatureList
	set varSigDataFilePath "[pwd]/Business/*PolicyStateSignature*.*"
	set sDelimitSiganture "\t"
	set lsDataFileSignature [glob -nocomplain $varSigDataFilePath]

	set slsDataFileSignature ""
	set lsMasterSignatureList ""
         foreach sDataFileSignature $lsDataFileSignature {
                     if {[lsearch $lsFileExtSkip [file ext $sDataFileSignature]] < 0} {
                      set iFile [open $sDataFileSignature r]
                      append slsDataFileSignature "[read $iFile]"
                      close $iFile
                     } 
				  }
					  set linesSignature [split $slsDataFileSignature \n]
					  foreach signature $linesSignature {
						  set sDataLineSignature [ split $signature $sDelimitSiganture ]
						  set sDataLineSignatureList [list $sDataLineSignature]
						
						  foreach signatureList $sDataLineSignatureList {
							  set sDataLinesSignatureAtZe  [ lindex "$signatureList" 0] 
							  set sDataLinesSignatureAtOne  [lindex "$signatureList" 1] 
							  set sDataLinesSignatureName [lindex "$signatureList" 2] 
							  set sDataLinesApproveSignature  [lindex "$signatureList" 3] 
							  set sDataLinesRejectSignature  [lindex "$signatureList" 4]
							  set sDataLinesIgnoreSignature  [lindex "$signatureList" 5]
							  set sDataLinesBranchSignature  [lindex "$signatureList" 6]
								########  Added by SL team for issue IR-140370 START  #############
							  set sApproveUser ""
							  set sRejectUser ""
							  set sIgnoreUser ""
							  
							  if {[regexp -all {\|} $sDataLinesApproveSignature match]} {
									set lsSignature [pTrimList $sDataLinesApproveSignature]
									set sApproveUser [lindex "$lsSignature" 0]
								} else {
								   set sApproveUser $sDataLinesApproveSignature
							   }
							   
							   if {[regexp -all {\|} $sDataLinesRejectSignature match]} {
									set lsSignature [pTrimList $sDataLinesRejectSignature]
									set sRejectUser [lindex "$lsSignature" 0]
								} else {
								   set sRejectUser $sDataLinesRejectSignature
							    }
								
								if {[regexp -all {\|} $sDataLinesIgnoreSignature match]} {
									set lsSignature [pTrimList $sDataLinesIgnoreSignature]
									set sIgnoreUser [lindex "$lsSignature" 0]
								} else {
								   set sIgnoreUser $sDataLinesIgnoreSignature
							    }
								
								lappend lsMasterSignatureList [list $sDataLinesSignatureAtZe $sDataLinesSignatureAtOne $sApproveUser $sDataLinesSignatureName $sRejectUser $sIgnoreUser $sDataLinesBranchSignature]
								############### END #################
						 }
				     }
	return $lsMasterSignatureList
}
#*************** END **************#

proc pProcessToOrder {lsPlanOrder} {
		global lsFinalPlanOrder counter sNumber lsNotProcessedPlanOrder
		set lsNotProcessedPlanOrder [list ]
		
		foreach lsPOrder $lsPlanOrder {
			set sState [lindex $lsPOrder 0]
			set sBeforeState [lindex $lsPOrder 1]
			
			if {$lsFinalPlanOrder > 1} {
				set stateIndex [lsearch $lsFinalPlanOrder $sState]
				if {$stateIndex > -1} {
					set beforeStateIndex [lsearch $lsFinalPlanOrder $sBeforeState]
					if {$beforeStateIndex == -1 } {
						set lsFinalPlanOrder [linsert $lsFinalPlanOrder [expr $stateIndex +1] $sBeforeState]
					} else {
						if {[expr $stateIndex +1] != $beforeStateIndex} {
							set lsFinalPlanOrder [lreplace $lsFinalPlanOrder $beforeStateIndex $beforeStateIndex ]
							set lsFinalPlanOrder [linsert $lsFinalPlanOrder $stateIndex $sBeforeState]
						}
					}
				} else {
					set beforeStateIndex [lsearch $lsFinalPlanOrder $sBeforeState]
					if {$sBeforeState == ""} {
						lappend lsFinalPlanOrder $sState
					} elseif {$beforeStateIndex > -1} {
						set lsFinalPlanOrder [linsert $lsFinalPlanOrder [expr $beforeStateIndex] $sState]
					} else {
						lappend lsNotProcessedPlanOrder $lsPOrder
					}
				}		
			} else { 
				lappend lsFinalPlanOrder $sState
				if {$sBeforeState != ""} { lappend lsFinalPlanOrder $sBeforeState }
			}
		}
		incr counter
		if {$counter < $sNumber && $lsNotProcessedPlanOrder > 1} {
				pProcessToOrder $lsNotProcessedPlanOrder
		}
	}
	
proc pQueryBusinessObjects {sStateP} {
	global lsStateReorder bQuery slsTypeRetain slsVault sLogFileDir sSchemaElement iBOCounter aBusObject
	set aBusObject($sStateP) ""
	set lsTypeToRetain ""
	set lsTypeActual [split [pQuery "" "print policy \042$sSchemaElement\042 select type dump |"] |]
	foreach sTypeActual $lsTypeActual {lappend lsTypeToRetain $sTypeActual}
    set slsTypeRetain [join $lsTypeToRetain ,]
	if {$bQuery} {
		puts -nonewline "\nRetain bus object current state option activated: querying database..."
		mql temp query bus "$slsTypeRetain" * * vault "$slsVault" select id current policy dump | output "$sLogFileDir/BusObjectQuery.txt"
		set bQuery FALSE
	}
	set iBOQuery [open "$sLogFileDir/BusObjectQuery.txt" r]
	set slsBusObject [gets $iBOQuery]
	set iBOCounter 0
	while {$slsBusObject != ""} {
		incr iBOCounter
		set lslsBusObject [split $slsBusObject |]
		if {[lindex $lslsBusObject 4] == $sStateP && [lindex $lslsBusObject 5] == $sSchemaElement} {lappend aBusObject($sStateP) [lindex $lslsBusObject 3]}
		set slsBusObject [gets $iBOQuery]
	}
	close $iBOQuery
	if {$aBusObject($sStateP) != ""} {lappend lsStateReorder $sStateP}
}

# Procedure to process policy states
proc pProcessState {lsMasterSignatureList} {
	global aCol sfinaldata aDat lsNotifyPlan lsNotifyActual bUseAccessField sIcon bScan bAdd bRetainBusObject sLogFileDir iBusObjCommit lsSchemaType sSpinDir aSchemaTitle sSymbolicActual sSymbolicPlan bAddState sStateToDelete bDeleteFlag lsPlanOrder sPrevPolicy sLastElement lsFinalPlanOrder sSchemaType sNumber counter lsNotProcessedPlanOrder lsStateReorder bQuery sSchemaElement iBOCounter aBusObject bStateContinue bContinue bResequenceStates
	
	set sfinaldata ""
	set sDelimitSiganture "\t"
	set sModMasterlist ""
	set lsEmptySigList ""
	set lsNonEmptySigList ""

	if {$bDeleteFlag == "TRUE"} {
		set sTemp [mql print policy $aCol(0) select state\[$sStateToDelete\].name dump]
		if {$sTemp != ""} {
		pMqlCmd "mod policy \042$aCol(0)\042 remove state \042$sStateToDelete\042"
		}
	}
	
	if {$bAddState == "TRUE"} {
		set lsPolicyStates [split [pQuery "" "print policy \042$aCol(0)\042 select state dump |"] |]
		if {$lsPolicyStates != ""} {
			if {$aCol(14) != ""} {
				pMqlCmd "mod policy \042$aCol(0)\042 add state \042$aCol(1)\042 before \042$aCol(14)\042"
			} else {
				pMqlCmd "mod policy \042$aCol(0)\042 add state \042$aCol(1)\042"
			}
		}
	}
	
	if {($sPrevPolicy != $aCol(0) || $sLastElement == "TRUE")} {
		set bExecuteStateData FALSE
		set lsStateReorder ""
		set bQuery TRUE
		set sSchemaElement $sPrevPolicy
		set sPrevPolicy $aCol(0)
		set lsFinalPlanOrder [list ]
		set counter 0
		set sNumber [llength $lsPlanOrder]
		pProcessToOrder $lsPlanOrder
		if {$bResequenceStates} {
		
			foreach lsPOrder $lsPlanOrder {
				set lsStateActual [split [pQuery "" "print policy \042$sSchemaElement\042 select state dump |"] |]
				
				set sState [lindex $lsPOrder 0]
				set sBeforeState [lindex $lsPOrder 1]
				set stateIndexActual [lsearch $lsStateActual $sState]
				set beforeStateIndexActual [lsearch $lsStateActual $sBeforeState]
				if {$sBeforeState != ""} {
					if {[expr $beforeStateIndexActual - $stateIndexActual] != 1} {
						set sStateBeforeState [lindex $lsStateActual [expr $beforeStateIndexActual -1]]
						if {$sStateBeforeState != "" || $sStateBeforeState  == "" } {
							set stateIndex [lsearch $lsPlanOrder [list $sStateBeforeState $sState]]
							if {$stateIndex >= -1} {
								if {$bRetainBusObject && $bScan != "TRUE"} {
									pQueryBusinessObjects $sState
								}
								pMqlCmd "mod policy \042$sSchemaElement\042 remove state \042$sState\042"
								pMqlCmd "mod policy \042$sSchemaElement\042 add state \042$sState\042 before \042$sBeforeState\042"
								set bExecuteStateData TRUE
							} else {
								set sStateBeforeState [lindex $lsStateActual [expr $beforeStateIndexActual -2]]
								if {$sStateBeforeState != "" } {
									set statebBeforeIndex [lsearch $lsPlanOrder [list $sStateBeforeState $sState]]
									if {$statebBeforeIndex >= 0} {
										if {$bRetainBusObject && $bScan != "TRUE"} {
											pQueryBusinessObjects $sState
										}
										pMqlCmd "mod policy \042$sSchemaElement\042 remove state \042$sState\042"
										pMqlCmd "mod policy \042$sSchemaElement\042 add state \042$sState\042 before \042$sBeforeState\042"
										set bExecuteStateData TRUE
									}
								}
							}
						}
					}
				}
			}
		}
		set sPolicyName [split [pQuery "" "print policy \042$sSchemaElement\042 select name dump |"] |]
		set lsPolicyStates [split [pQuery "" "print policy \042$sSchemaElement\042 select state dump |"] |]
		if {$sPolicyName != "" && $lsPolicyStates == ""} {
			foreach sState $lsFinalPlanOrder {
			if { $sState != ""} {
				pMqlCmd "mod policy \042$sSchemaElement\042 add state \042$sState\042"
				set bExecuteStateData TRUE
				}
			}
		}
		
		if {($sSchemaElement != $aCol(0) && $sLastElement == "TRUE")} {
			set sPolicyName [split [pQuery "" "print policy \042$aCol(0)\042 select name dump |"] |]
			set lsPolicyStates [split [pQuery "" "print policy \042$aCol(0)\042 select state dump |"] |]
			if {$sPolicyName != "" && $lsPolicyStates == ""} {
				pMqlCmd "mod policy \042$aCol(0)\042 add state \042$aCol(1)\042"
			}
		}
		
		set lsPlanOrder [list ]
		if {$bDeleteFlag != "TRUE"} {
			lappend lsPlanOrder [list "$aCol(1)" "$aCol(14)"]
		}
		
		if {$bQuery == "FALSE"} {
		 puts "$iBOCounter bus object(s) found"
		file delete -force "$sLogFileDir/BusObjectQuery.txt"
	 }
# Reset Bus Object states if reordered - ION 10/26/09 (removed promote logic - added change state logic)
	 foreach sStateReorder $lsStateReorder {
		mql trigger off
		set iCommit 0
		set bReset FALSE
		foreach oID $aBusObject($sStateReorder) {
		   if {!$bReset} {
			  if {[pQuery "" "print bus $oID select current dump"] != "$sStateReorder"} {
				 puts "Resetting [llength $aBusObject($sStateReorder)] bus object(s) back to state '$sStateReorder'"
				 pAppend "# Promote [llength $aBusObject($sStateReorder)] business objects back to state: $sStateReorder" FALSE
				 set bReset TRUE
			  } else {
				 break
			  }
		   }
		   if {[catch {
			  mql mod bus $oID current $sStateReorder
		   } sMsg] != 0} {
			  pWriteWarningMsg "\nWARNING: Bus Object [pQuery "$oID" "print bus $oID select type name revision dump \042 \042"] reset state error:\n$sMsg"
			  break
		   } else {
			  incr iCommit
			  if {$iCommit > $iBusObjCommit} {
				 mql commit transaction
				 mql start transaction update
				 pAppend "# Committed $iCommit business object state resets" FALSE
				 set iCommit 0
			  }
		   }
		}
		mql trigger on
	 }
	 
	 # For state resequence, run state, signature, trigger and policyaccess files
		if {$bExecuteStateData} {
			set bStateContinue TRUE
			set bContinue TRUE
		}
	# Sync state properties - ION 10/26/09 (changed logic as states were not properly fixed)
	}
	set lsPolicySts [split [pQuery "" "print policy \042$aCol(0)\042 select state dump |"] |]
	if {$bDeleteFlag == "FALSE" && $lsPolicySts != ""} {
		if { $sSymbolicActual != $sSymbolicPlan } {
			if {$sSymbolicActual != ""} {
				pMqlCmd "delete property \042$sSymbolicActual\042 on policy \042$aCol(0)\042"
			}
			pMqlCmd "add property \042$sSymbolicPlan\042 on policy \042$aCol(0)\042 value \042$aCol(1)\042"
		}
		
		if {$aCol(3) != $aDat(3) || $aCol(4) != $aDat(4) || $aCol(5) != $aDat(5) || $aCol(7) != $aDat(7) || $aCol(9) != $aDat(9) || $aCol(10) != $aDat(10) || $aCol(11) != $aDat(11) || $aCol(12) != $aDat(12) || $aCol(13) != $aDat(13)} {
			pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 version $aCol(3) promote $aCol(4) checkouthistory $aCol(5) notify message \042$aCol(7)\042 route message \042$aCol(9)\042 minorrevision $aCol(10) majorrevision $aCol(11) published $aCol(12) $aCol(13)"
		}
		
		pPlanActualAddDel $lsNotifyActual "" $lsNotifyPlan policy "\042$aCol(0)\042 state" $aCol(1) "remove notify" "add notify" ""
		if {$aDat(8) != "" && $aDat(8) != $aCol(8)} {pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 remove route"}
		if {$aCol(8) != "" && $aCol(8) != $aDat(8)} {pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 add route \042$aCol(8)\042"}
		if {$sIcon != "" && $bScan != "TRUE"} {mql mod policy $aCol(0) state $aCol(1) icon "$sSpinDir/Pix/$sIcon"}
	}
	
	return 	$lsMasterSignatureList	
}
#***************** END ******************#    

# Procedure to analyze policy state signatures
proc pAnalyzeSignature {} {
   global aCol aDat bOverlay bAdd bRemoveSignature sSigExist
   set aDat(3) ""
   set aDat(4) ""
   
   set bRemoveSignature "FALSE"
   set sSigExist "TRUE"
   set iLenMinTwo [expr [string length $aCol(2)] -2]
	 if {[string first "<<" $aCol(2)] >= 0 && [string first ">>" $aCol(2)] == $iLenMinTwo} {
		set aCol(2) [string range $aCol(2) 2 [expr [string length $aCol(2)] -3]]
		set bRemoveSignature "TRUE"
		set sTemp [mql print policy $aCol(0) select state\[$aCol(1)\].signature\[$aCol(2)\] dump]
		if {$sTemp == ""} {set sSigExist "FALSE" }
	 }
	if {$bRemoveSignature == "FALSE"} {
	
	   set aDat(3) [mql print policy $aCol(0) select state\[$aCol(1)\].signature\[$aCol(2)\].branch dump]
	   set aDat(4) [mql print policy $aCol(0) select state\[$aCol(1)\].signature\[$aCol(2)\].filter dump]

	   pSetAction "Modify policy $aCol(0) state $aCol(1) signature $aCol(2)"
	   
	   if {$bOverlay} {
		  pOverlay [list 3 4 ]
	   }
	} elseif {$sSigExist == "TRUE"} { pSetAction "Modify policy $aCol(0) state $aCol(1) remove signature $aCol(2)" }
}

# Procedure to process policy state signatures
proc pProcessSignature {} {
   global aCol aDat bRemoveSignature sSigExist
   if {$bRemoveSignature == "FALSE"} {
	   if {$aCol(3) != $aDat(3)} {
		  if {$aCol(3) == "" && $aDat(3) != ""} {
			 pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 signature \042$aCol(2)\042 remove branch"
		  } else {
			 pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 signature \042$aCol(2)\042 branch \042$aCol(3)\042"
		  }
	   }
	   
	   if {$aCol(4) != $aDat(4)} {
		  if {$aCol(4) == "" && $aDat(4) != ""} {
			 pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 signature \042$aCol(2)\042 remove filter"
		  } else {
			set sSigFilter [pRegSubEvalEscape $aCol(4)]
			 pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 signature \042$aCol(2)\042 filter \042$sSigFilter\042"
		  }
	   }
	} elseif {$sSigExist == "TRUE"} {
		pMqlCmd "mod policy \042$aCol(0)\042 state \042$aCol(1)\042 remove signature \042$aCol(2)\042"
	}
   return 0
}

# Procedure to add list elements
proc pPlanActualAdd {lsActual lsActualForPlan lsPlan sSchemaType sMidCommand sSchemaName sActionDel sActionAdd sActionAppend} {
      global sSystem aCmdMenuPlan aCmdMenuActual
      if {$lsActualForPlan == ""} {set lsActualForPlan $lsActual}
      set sAppend ""
      foreach sPlan $lsPlan sActual $lsActual {
         if {$sPlan != "" && [lsearch $lsActualForPlan $sPlan] < 0} {
            if {$sSchemaType == "menu"} {set sActionAdd "add $aCmdMenuPlan($sPlan)"}
            append sAppend " $sActionAdd \042$sPlan\042$sActionAppend"
            if {$sSchemaType == "type" && $sActionAdd == "add method"} {pDeleteMethod $sSchemaName $sPlan}
         }
      }
      if {$sAppend != ""} {
			pMqlCmd "mod $sSchemaType $sMidCommand \042$sSchemaName\042 $sSystem$sAppend"
	  }
}
   
# Procedure to remove list elements
proc pPlanActualDel {lsActual lsActualForPlan lsPlan sSchemaType sMidCommand sSchemaName sActionDel sActionAdd sActionAppend} {
      global sSystem aCmdMenuPlan aCmdMenuActual
      if {$lsActualForPlan == ""} {set lsActualForPlan $lsActual}
      set sAppend ""
      foreach sPlan $lsPlan sActual $lsActual {
         if {$sActual != "" && [lsearch $lsPlan $sActual] < 0} {
            if {$sSchemaType == "menu"} {set sActionDel	"remove $aCmdMenuActual($sActual)"}
            append sAppend " $sActionDel \042$sActual\042"
         }
      }
      if {$sAppend != ""} {
		pMqlCmd "mod $sSchemaType $sMidCommand \042$sSchemaName\042 $sSystem$sAppend"
	  }
}
# Procedure to skip blanks if bOverlay switch is set
   proc pStateOverlay {lsNumber} {
      global aCol aDat lsMasterSignatureList bAddState
      foreach sNumber $lsNumber {
        if {$aCol($sNumber) == "<NULL>"} {
           set aCol($sNumber) ""
        } elseif {$bAddState != "TRUE" && $aCol($sNumber) == ""} {
           set aCol($sNumber) $aDat($sNumber)
      	} elseif {$bAddState == "TRUE"} {
      	   set aDat($sNumber) ""
      	}
      }
   }
   
   # Procedure to check and set attribute values
   proc pStateCompareAttr {sAttrCurrent sAttrDefault sAttrAlternate bTrueFalse bCol} {
      global bOverlay bAddState
      if {$bCol && $bAddState != "TRUE" && $bOverlay && $sAttrCurrent == ""} {
         return ""
      } elseif {[string tolower $sAttrCurrent] != $sAttrAlternate && [string tolower $sAttrCurrent] != $bTrueFalse} {
         return $sAttrDefault
      } else {
         return $sAttrAlternate
      }
   }
