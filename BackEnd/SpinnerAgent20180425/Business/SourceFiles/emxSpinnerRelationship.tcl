#########################################################################*2014x
#
# @progdoc      emxSpinnerRelationship.tcl vMV6R2013 (Build 11.10.1)
#
# @Description: Procedures for running in Relationships
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

# Procedure to trim a string for markup characters at start and end (<<xyz>>)
proc processValForDelete { sValue } {
	set bChkErr FALSE
	
	if { [string first "<<" $sValue] >= 0 && [string last ">>" $sValue] >= 0 } {
		set sValue [string trim $sValue "<<"] 
		set sValue [string trim $sValue ">>"]
		set data [string trim $sValue]
		set bChkErr TRUE                              
	}
	
	return $bChkErr
}

# Procedure to analyze relationships
proc pAnalyzeRelationship {} {
   global aCol aDat bOverlay bAdd sMxVersion lsAttributePlan lsAttributeActual lsFromTypePlan lsFromTypeActual lsFromRelPlan lsFromRelActual lsToTypePlan lsToTypeActual lsToRelPlan lsToRelActual lsSchemaType 
   set lsAttributePlan [pTrimList $aCol(3)]
   if {[lsearch $lsSchemaType attribute] >= 0} {set lsAttributePlan [pCheckNameChange $lsAttributePlan attribute]}
   set aCol(4) [pCompareAttr $aCol(4) false true sparse true]
   set aCol(6) [pCompareAttr $aCol(6) !preventduplicates preventduplicates true true]
   set aCol(26) [pCompareAttr $aCol(26) false true abstract true]
# Major/Minor Mod - ION - 10/1/2012
   set aCol(23) [pCompareAttr $aCol(23) false true true true]
   foreach iFromTo [list 9 10 15 16] {
   	if {$bOverlay && $aCol($iFromTo) == ""} {
		if {$bAdd == "TRUE"} {set aCol($iFromTo) none}
      } elseif {[string tolower $aCol($iFromTo)] == "none" || ( [string tolower $aCol($iFromTo)] != "float" && [string tolower $aCol($iFromTo)] != "replicate" ) } {
         set aCol($iFromTo) none
      } elseif {[string tolower $aCol($iFromTo)] == "float"} {
         set aCol($iFromTo) float
      } else {
         set aCol($iFromTo) replicate
      }
   }
   foreach iCol [list 11 17] {set aCol($iCol) [pCompareAttr $aCol($iCol) n 1 one true]}
   foreach iCol [list 12 18] {set aCol($iCol) [pCompareAttr $aCol($iCol) notpropagatemodify propagatemodify true true]}
   set lsFromTypePlan [pTrimList $aCol(7)]
   set lsToTypePlan [pTrimList $aCol(13)]
   if {$sMxVersion >= 10.8} { 
      set lsFromRelPlan [pTrimList $aCol(8)]
      set lsToRelPlan [pTrimList $aCol(14)]
   }
   if {[lsearch $lsSchemaType type] >= 0} {
      set lsFromTypePlan [pCheckNameChange $lsFromTypePlan type]
      set lsToTypePlan [pCheckNameChange $lsToTypePlan type]
   }
   if {$sMxVersion >= 10.8} {
      set lsFromRelPlan [pCheckNameChange $lsFromRelPlan relationship]
      set lsToRelPlan [pCheckNameChange $lsToRelPlan relationship]
   }   
   if {$sMxVersion >= 10.5} {foreach iCol [list 19 20] {set aCol($iCol) [pCompareAttr $aCol($iCol) propagateconnection notpropagateconnection false true]}}
   set lsFromTypeActual [list ]
   set lsToTypeActual [list ]
   set lsFromRelActual [list ]
   set lsToRelActual [list ]
   set lsAttributeActual [list ]
   if {$bAdd != "TRUE"} {
      set aDat(4) [pPrintQuery false sparse "" str]
      set aDat(6) [pPrintQuery false preventduplicates "" str]
      set aDat(6) [pCompareAttr $aDat(6) !preventduplicates preventduplicates true false]
# Major/Minor Mod - ION - 10/1/2012
     set aDat(23) [pPrintQuery false dynamic "" str]
	  # KYB
	  
		set aDat(25) [pPrintQuery "" derived "" str]
		set aDat(26) [pPrintQuery false abstract "" str]
	   
	  
      if {$sMxVersion >= 10.8} {
         foreach sDat [list 21 9 10 11 22 15 16 17] sPQ1 [list "" none none n none none n] sPQ2 [list frommeaning fromreviseaction fromcloneaction fromcardinality tomeaning toreviseaction tocloneaction tocardinality] {
		 set aDat($sDat) [pPrintQuery $sPQ1 $sPQ2 "" ""]
		 }
      } else {
         foreach sDat [list 8 9 10 11 14 15 16 17] sPQ1 [list "" none none n "" none none n] sPQ2 [list frommeaning fromreviseaction fromcloneaction fromcardinality tomeaning toreviseaction tocloneaction tocardinality] {set aDat($sDat) [pPrintQuery $sPQ1 $sPQ2 "" ""]}
      }
      foreach iDat [list 11 17] {set aDat($iDat) [pCompareAttr $aDat($iDat) n 1 one false]}
      set bFromPropModActual false
      set bToPropModActual] false
      if {[catch {set lsRel [split [mql print rel $aCol(0)] \n]} sMsg] == 0} {
         set iCounter1 0
         foreach sRel $lsRel {
            set sRel [string trim $sRel]
            if {[string first "propagate modify" $sRel] == 0} {
               incr iCounter1
               set lslsRel [split $sRel " "]
               if {$iCounter1 == 1} {
                  set bFromPropModActual [lindex $lslsRel 2]
               } else {
                  set bToPropModActual [lindex $lslsRel 2]
               }
            }
            if {$iCounter1 > 1} {break}
         }
      }
      foreach iDat [list 12 18] {set aDat($iDat) [pCompareAttr $bFromPropModActual notpropagatemodify propagatemodify true false]}
      if {$sMxVersion >= 10.5} {
         foreach iCol [list 19 20] {set aCol($iCol) [pCompareAttr $aCol($iCol) propagateconnection notpropagateconnection false true]}
         set aDat(19) [pPrintQuery true frompropagateconnection "" str]
         set aDat(20) [pPrintQuery true topropagateconnection "" str]
         foreach iDat [list 19 20] {set aDat($iDat) [pCompareAttr $aDat($iDat) propagateconnection notpropagateconnection false false]}
      }
      set lsAttributeActual [pPrintQuery "" attribute | spl]
      set lsFromTypeActual [pPrintQuery "" fromtype | spl]
      set lsToTypeActual [pPrintQuery "" totype | spl]
      if {$sMxVersion >= 10.8} {
         set lsFromRelActual [pPrintQuery "" fromrel | spl]
         set lsToRelActual [pPrintQuery "" torel | spl]
      }
   }
   if {$bOverlay} {
      if {$sMxVersion >= 10.8} {
         pOverlay [list 4 6 9 10 11 12 15 16 17 18 21 22 26]
         set lsFromRelPlan [pOverlayList $lsFromRelPlan $lsFromRelActual]
         set lsToRelPlan [pOverlayList $lsToRelPlan $lsToRelActual]
      } else {
         pOverlay [list 4 6 8 9 10 11 12 14 15 16 17 18]
      }
      set lsFromTypePlan [pOverlayList $lsFromTypePlan $lsFromTypeActual]
      set lsToTypePlan [pOverlayList $lsToTypePlan $lsToTypeActual]
      set lsAttributePlan [pOverlayList $lsAttributePlan $lsAttributeActual]
      if {$sMxVersion >= 10.5} {pOverlay [list 19 20]}
# Major/Minor Mod - ION - 10/1/2012
	  pOverlay [list 23]
   }
}

# Procedure to process relationships
proc pProcessRelationship {} {
   global aCol aDat bAdd lsAttributePlan lsAttributeActual lsFromTypePlan lsFromTypeActual lsToTypePlan lsToTypeActual lsFromRelPlan lsFromRelActual lsToRelPlan lsToRelActual sHidden sHiddenActual sMxVersion 
   if {$bAdd} {
# Major/Minor Mod - ION - 10/1/2012
   
# JD le 13/01/12 : suppression de dynamic --> marche pas sur les rel CBP --> uniquement VPLM !!!
#         pMqlCmd "add relationship \042$aCol(0)\042 $sHidden $aCol(6) sparse $aCol(4) dynamic $aCol(23) from cardinality $aCol(11) revision $aCol(9) clone $aCol(10) $aCol(12) to cardinality $aCol(17) revision $aCol(15) clone $aCol(16) $aCol(18)"
         pMqlCmd "add relationship \042$aCol(0)\042 $sHidden $aCol(6) sparse $aCol(4) from cardinality $aCol(11) revision $aCol(9) clone $aCol(10) $aCol(12) to cardinality $aCol(17) revision $aCol(15) clone $aCol(16) $aCol(18)"
	 
      if {$sMxVersion < 10.8} {
	     pMqlCmd "mod relationship \042$aCol(0)\042 from meaning \042$aCol(8)\042 to meaning \042$aCol(14)\042"
	  } else {
	     pMqlCmd "mod relationship \042$aCol(0)\042 from meaning \042$aCol(21)\042 to meaning \042$aCol(22)\042"
	  }
      if {$sMxVersion >= 10.5} {pMqlCmd "mod relationship \042$aCol(0)\042 from $aCol(19) to $aCol(20)"}
      pPlanAdd $lsAttributePlan relationship $aCol(0) "add attribute" ""
      pPlanAdd $lsFromTypePlan relationship $aCol(0) "from add type" ""
      pPlanAdd $lsToTypePlan relationship $aCol(0) "to add type" ""
      if {$sMxVersion >= 10.8} { 
         pPlanAdd $lsFromRelPlan relationship $aCol(0) "from add rel" ""
         pPlanAdd $lsToRelPlan relationship $aCol(0) "to add rel" ""
      }
	  
	  # KYB
	  if {$sMxVersion >= 10.5} {
	
			if { $aCol(25) != "" } {pMqlCmd "mod relationship \042$aCol(0)\042 derived \042$aCol(25)\042"}
			if { $aCol(26) != "" } {pMqlCmd "mod relationship \042$aCol(0)\042 abstract $aCol(26)"}
		
	  }
   } else {
# Major/Minor Mod - ION - 10/1/2012
     
# JD le 13/01/12 : suppression de dynamic --> marche pas sur les rel CBP --> uniquement VPLM !!!
#         if {$sHidden != $sHiddenActual || $aCol(6) != $aDat(6) || $aCol(4) != $aDat(4) || $aCol(11) != $aDat(11) || $aCol(9) != $aDat(9) || $aCol(10) != $aDat(10) || $aCol(12) != $aDat(12) || $aCol(17) != $aDat(17) || $aCol(15) != $aDat(15) || $aCol(16) != $aDat(16) || $aCol(18) != $aDat(18) || $aCol(23) != $aDat(23)} {pMqlCmd "mod relationship \042$aCol(0)\042 $sHidden $aCol(6) sparse $aCol(4) dynamic $aCol(23) from cardinality $aCol(11) revision $aCol(9) clone $aCol(10) $aCol(12) to cardinality $aCol(17) revision $aCol(15) clone $aCol(16) $aCol(18)"}
         if {$sHidden != $sHiddenActual || $aCol(6) != $aDat(6) || $aCol(4) != $aDat(4) || $aCol(11) != $aDat(11) || $aCol(9) != $aDat(9) || $aCol(10) != $aDat(10) || $aCol(12) != $aDat(12) || $aCol(17) != $aDat(17) || $aCol(15) != $aDat(15) || $aCol(16) != $aDat(16) || $aCol(18) != $aDat(18) || $aCol(23) != $aDat(23)} {pMqlCmd "mod relationship \042$aCol(0)\042 $sHidden $aCol(6) sparse $aCol(4) from cardinality $aCol(11) revision $aCol(9) clone $aCol(10) $aCol(12) to cardinality $aCol(17) revision $aCol(15) clone $aCol(16) $aCol(18)"}
   
      if {$sMxVersion < 10.8} {
	     if {$aCol(8) != $aDat(8) || $aCol(14) != $aDat(14)} {pMqlCmd "mod relationship \042$aCol(0)\042 from meaning \042$aCol(8)\042 to meaning \042$aCol(14)\042"}
	  } else {
	     if {$aCol(21) != $aDat(21) || $aCol(22) != $aDat(22)} {pMqlCmd "mod relationship \042$aCol(0)\042 from meaning \042$aCol(21)\042 to meaning \042$aCol(22)\042"}
	  }
      if {$sMxVersion >= 10.5 && ($aCol(19) != $aDat(19) || $aCol(20) != $aDat(20))} {pMqlCmd "mod relationship \042$aCol(0)\042 from $aCol(19) to $aCol(20)"}
      pPlanActualAddDel $lsAttributeActual "" $lsAttributePlan relationship "" $aCol(0) "remove attribute" "add attribute" ""
      pPlanActualAddDel $lsFromTypeActual "" $lsFromTypePlan relationship "" $aCol(0) "from remove type" "from add type" ""
      pPlanActualAddDel $lsToTypeActual "" $lsToTypePlan relationship "" $aCol(0) "to remove type" "to add type" ""
      if {$sMxVersion >= 10.8} { 
         pPlanActualAddDel $lsFromRelActual "" $lsFromRelPlan relationship "" $aCol(0) "from remove rel" "from add rel" ""
         pPlanActualAddDel $lsToRelActual "" $lsToRelPlan relationship "" $aCol(0) "to remove rel" "to add rel" ""
      }
	  
	  # KYB
	  if {$sMxVersion >= 10.5} {
		
		set bRemoveParentRel [ processValForDelete $aCol(25) ]
		
		if { $bRemoveParentRel } {
			pMqlCmd "mod relationship \042$aCol(0)\042 remove derived"
		   } else {
			if { [string tolower  $aCol(25)] != $aDat(25) && ($aCol(25) != "") } {
				pMqlCmd "mod relationship \042$aCol(0)\042 derived \042$aCol(25)\042"}
		   }

			if { [string tolower  $aCol(26)] != $aDat(26) && ($aCol(26) != "")  } {
				pMqlCmd "mod relationship \042$aCol(0)\042 abstract $aCol(26)"}
		 
	  }
   }
   return 0
}

