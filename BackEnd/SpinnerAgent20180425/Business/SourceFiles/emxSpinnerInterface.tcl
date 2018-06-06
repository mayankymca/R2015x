#########################################################################*2014x
#
# @progdoc      emxSpinnerInterface.tcl vM2013 (Build 8.3.31)
#
# @Description: Procedures for running in Interfaces
#
# @Parameters:  Returns 0 if successful, 1 if not
#
# @Usage:       Utilized by emxSpinnerAgent.tcl
#
# @progdoc      Copyright (c) ENOVIA ENOVIA 2006
#
#########################################################################
#
# @Modifications: FirstName LastName MM/DD/YYYY - Modification
#
#########################################################################

# Procedure to check for and remove same types on derivative interfaces
   proc pDeleteTypeRel {sSchName sTypeRelName sTypeRel} {
      set lsDerived [pPrintQuery "" derived | spl]
      foreach sDerived $lsDerived {
         if {$sDerived != "" && [lsearch [split [pQuery "" "print interface \042$sDerived\042 select $sTypeRel dump |"] |] $sTypeRelName] > -1} {
            pMqlCmd "mod interface \042$sSchName\042 remove $sTypeRel \042$sTypeRelName\042"
         } else {
            set lsDerivative [split [pQuery "" "print interface \042$sSchName\042 select derivative dump |"] |]
            foreach sDerivative $lsDerivative {
               set slsTypeTest [pQuery "" "print interface \042$sDerivative\042 select $sTypeRel dump |"]
               if {[regsub -all "$sTypeRelName" $slsTypeTest "" slsTypeTest] > 1} {pMqlCmd "mod interface \042$sDerivative\042 remove $sTypeRel \042$sTypeRelName\042"}
            }
         }
      }
   }

# Procedure to analyze interfaces
proc pAnalyzeInterface {} {
   global aCol aDat lsAttributePlan lsAttributeActual lsAttrImmediate lsTypePlan lsTypeActual lsTypeDerivative lsRelPlan lsRelActual lsRelDerivative lsDerivedPlan lsDerivedActual bOverlay bAdd sMxVersion
   set lsDerivedPlan [pTrimList $aCol(2)]
   set aCol(3) [pCompareAttr $aCol(3) false true abstract true]
   set lsAttributePlan [pTrimList $aCol(5)]
   set lsTypePlan [pTrimList $aCol(6)]
   if {$sMxVersion >= 10.8} {set lsRelPlan [pTrimList $aCol(8)]}
   set lsDerivedActual [list ]
   set lsAttributeActual [list ]
   set lsTypeActual [list ]
   set lsRelActual [list ]
   if {$bAdd != "TRUE"} {
      set aDat(3) [pPrintQuery "false" abstract "" "str"]
      set lsDerivedActual [pPrintQuery "" derived | spl]
      set lsAttributeActual [pPrintQuery "" attribute | spl]
      set lsAttrImmediate [pPrintQuery "" immediateattribute | spl]
      set lsTypeActual [pPrintQuery "" type | spl]
      if {$sMxVersion >= 10.8} {set lsRelActual [pPrintQuery "" relationship | spl]} 
      if {[llength $lsDerivedActual] > 0} {
         set lsTypeDerivative [list ]
         set lsRelDerivative [list ]
         foreach sDerivedActual $lsDerivedActual {
            set lsTypeDerived [split [pQuery "" "print interface \042$sDerivedActual\042 select type dump |"] |]
            foreach sType $lsTypeActual {if {[lsearch $lsTypeDerived $sType] < 0 && [lsearch $lsTypeDerivative $sType] < 0} {lappend lsTypeDerivative $sType}}
            if {$sMxVersion >= 10.8} {
               set lsRelDerived [split [pQuery "" "print interface \042$sDerivedActual\042 select relationship dump |"] |]
               foreach sRel $lsRelActual {if {[lsearch $lsRelDerived $sRel] < 0 && [lsearch $lsRelDerivative $sRel] < 0} {lappend lsRelDerivative $sRel}}
            }
         }
      } else {
         set lsTypeDerivative $lsTypeActual
         set lsRelDerivative $lsRelActual
      }
   }
   if {$bOverlay} {
      pOverlay [list 3]
      set lsDerivedPlan [pOverlayList $lsDerivedPlan $lsDerivedActual]
      set lsAttributePlan [pOverlayList $lsAttributePlan $lsAttributeActual]
      set lsTypePlan [pOverlayList $lsTypePlan $lsTypeActual]
      if {$sMxVersion >= 10.8} {set lsRelPlan [pOverlayList $lsRelPlan $lsRelActual]}
   }
}

# Procedure to process interfaces
proc pProcessInterface {} {
   global aCol aDat bAdd lsDerivedPlan lsDerivedActual lsAttributePlan lsAttributeActual lsAttrImmediate lsTypePlan lsTypeActual lsTypeDerivative lsRelPlan lsRelActual lsRelDerivative resultappend sHidden sHiddenActual bUpdate bReg bScan sMxVersion
   set iExit 0
   if {$bAdd} {
      pMqlCmd "add interface \042$aCol(0)\042 abstract $aCol(3) $sHidden"
      if {[llength $lsDerivedPlan] > 0} {
         set slsDerivedPlan [join $lsDerivedPlan "','"]
         pMqlCmd "mod interface \042$aCol(0)\042 derived '$slsDerivedPlan'"
      }
      set sAppend ""
      foreach sAttr $lsAttributePlan {if {[pPrintQuery FALSE "attribute\133$sAttr\135" "" ""] == "FALSE"} {append sAppend " add attribute \042$sAttr\042"}}
      if {$sAppend != ""} {pMqlCmd "mod interface \042$aCol(0)\042$sAppend"}
      pPlanAdd $lsTypePlan interface $aCol(0) "add type" ""
      foreach sTypeP $lsTypePlan {pDeleteTypeRel $aCol(0) $sTypeP "type"}
      if {$sMxVersion >= 10.8} {
         pPlanAdd $lsRelPlan interface $aCol(0) "add relationship" ""
         foreach sRelP $lsRelPlan {pDeleteTypeRel $aCol(0) $sRelP "relationship"}
      }
   } else {
      if {$aCol(3) != $aDat(3) || $sHidden != $sHiddenActual} {pMqlCmd "escape mod interface \042$aCol(0)\042 abstract $aCol(3) $sHidden"}
      if {[llength $lsDerivedPlan] > 0} {
         if {[lsort -unique $lsDerivedPlan] != [lsort -unique $lsDerivedActual]} {
            set slsDerivedPlan [join $lsDerivedPlan "','"]
            pMqlCmd "mod interface \042$aCol(0)\042 derived '$slsDerivedPlan'"
         }
      } elseif {[llength $lsDerivedActual] > 0} {
         pMqlCmd "mod interface \042$aCol(0)\042 remove derived"
      }
      pPlanActualAddDel $lsAttrImmediate $lsAttributeActual $lsAttributePlan interface "" $aCol(0) "remove attribute" "add attribute" ""
      pPlanActualAddDel $lsTypeDerivative $lsTypeActual $lsTypePlan interface "" $aCol(0) "remove type" "add type" ""
      if {$sMxVersion >= 10.8} {pPlanActualAddDel $lsRelDerivative $lsRelActual $lsRelPlan interface "" $aCol(0) "remove relationship" "add relationship" ""}
   }
   return $iExit
}

