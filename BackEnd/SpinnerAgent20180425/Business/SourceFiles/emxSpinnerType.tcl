#########################################################################*2014x
#
# @progdoc      emxSpinnerType.tcl vM2013 (Build 5.1.12)
#
# @Description: Procedures for running in Types
#
# @Parameters:  Returns 0 if successful, 1 if not
#
# @Usage:       Utilized by emxSpinnerAgent.tcl
#
# @progdoc      Copyright (c) ENOVIA Inc. 2005
#
#########################################################################
#
# @Modifications: FirstName LastName MM/DD/YYYY - Modification
#
#########################################################################

# Procedure to check for and remove same methods on derivative types
   proc pDeleteMethod {sSchName sMethod} {
      set sDerived [pQuery "" "print type \042$sSchName\042 select derived dump"]
      if {$sDerived != "" && [lsearch [split [pQuery "" "print type \042$sDerived\042 select method dump |"] |] $sMethod] > -1} {
         pMqlCmd "mod type \042$sSchName\042 remove method \042$sMethod\042"
      } else {
         set lsDerivative [split [pQuery "" "print type \042$sSchName\042 select derivative dump |"] |]
         foreach sDerivative $lsDerivative {
            set slsMethodTest [pQuery "" "print type \042$sDerivative\042 select method dump |"]
            if {[regsub -all "$sMethod" $slsMethodTest "" slsMethodTest] > 1} {pMqlCmd "mod type \042$sDerivative\042 remove method \042$sMethod\042"}
         }
      }
   }

# Procedure to analyze types
proc pAnalyzeType {} {
   global aCol aDat lsSchemaType lsAttributePlan lsAttributeActual lsAttrImmediate lsMethodPlan lsMethodActual lsMethodDerivative bOverlay bAdd
   if {$aCol(2) != ""} {set aCol(2) [pCheckNameChange $aCol(2) type]}
   set aCol(3) [pCompareAttr $aCol(3) false true abstract true]
   set lsAttributePlan [pTrimList $aCol(5)]
   if {[lsearch $lsSchemaType attribute] >= 0} {set lsAttributePlan [pCheckNameChange $lsAttributePlan attribute]}
   set lsMethodPlan [pTrimList $aCol(6)]
   set aCol(8) [pCompareAttr $aCol(8) false true sparse true]
   set lsMethodActual [list ]
   set lsAttributeActual [list ]
   if {$bAdd != "TRUE"} {
      set aDat(8) [pPrintQuery "" sparse "" str]
      set aDat(8) [pCompareAttr $aDat(8) false true true false]
      set lsPrint [split [pQuery "" "print type \042$aCol(0)\042"] \n]
      set aDat(3) false
      foreach sPrint $lsPrint {
         set sPrint [string trim $sPrint]
         if {[string range $sPrint 0 7] == "abstract"} {
            regsub "abstract " $sPrint "" aDat(3)
            set aDat(3) [string tolower $aDat(3)]
            break
         }
      }
      set aDat(2) [pPrintQuery "" derived "" ""]
      set lsAttributeActual [pPrintQuery "" attribute | spl]
# Begin fix to check for change of derived type retaining attributes from previous type  - MJO 1/30/2007 
      if {$aCol(2) != "" && $aCol(2) != $aDat(2) && [string tolower $aCol(2)] != "<null>"} {
         if {$lsAttributePlan != {}} {
            foreach sAttributePlan $lsAttributePlan {
               if {[string first "<<" $sAttributePlan] < 0 && [string tolower $sAttributePlan] != "<null>"} {
                  if {[lsearch $lsAttributeActual $sAttributePlan] < 0} {
                     continue
                  } else {
                     if {[mql list type $aCol(2)] != ""} {
                        set lsAttributeActual [split [mql print type "$aCol(2)" select attribute dump |] |]
                     }
                     break
                  }
               }
            }
         }
      }
# End fix
      set lsAttrImmediate [pPrintQuery "" immediateattribute | spl]
      set lsMethodActual [pPrintQuery "" method | spl]
      if {$aDat(2) != ""} {
         set lsMethodDerived [split [pQuery "" "print type \042$aDat(2)\042 select method dump |"] |]
         set lsMethodDerivative [list ]
         foreach sMethod $lsMethodActual {if {[lsearch $lsMethodDerived $sMethod] < 0} {lappend lsMethodDerivative $sMethod}}
      } else {
         set lsMethodDerivative $lsMethodActual
      }
   }
   if {$bOverlay} {
      pOverlay [list 2 3 8]
      set lsMethodPlan [pOverlayList $lsMethodPlan $lsMethodActual]
      set lsAttributePlan [pOverlayList $lsAttributePlan $lsAttributeActual]
   }
}

# Procedure to process types
proc pProcessType {} {
   global aCol aDat bAdd lsAttributePlan lsAttributeActual lsAttrImmediate lsMethodPlan lsMethodActual lsMethodDerivative bSkipElement resultappend sHidden sHiddenActual bUpdate bSkipElement bReg
   set iExit 0
   if {$bAdd} {
      regsub -all " " $aCol(2) "" sDeleteTest
      if {[string tolower $sDeleteTest] != "deletethistype"} {
         pMqlCmd "add type \042$aCol(0)\042 abstract $aCol(3) $sHidden sparse $aCol(8)"
         if {$aCol(2) != ""} {pMqlCmd "mod type \042$aCol(0)\042 derived \042$aCol(2)\042"}
         set sAppend ""
         foreach sAttr $lsAttributePlan {if {[pPrintQuery FALSE "attribute\133$sAttr\135" "" ""] == "FALSE"} {append sAppend " add attribute \042$sAttr\042"}}
         if {$sAppend != ""} {pMqlCmd "mod type \042$aCol(0)\042$sAppend"}
         pPlanAdd $lsMethodPlan type $aCol(0) "add method" ""
         foreach sMethodP $lsMethodPlan {pDeleteMethod $aCol(0) $sMethodP}
      } else {
         pAppend "# add type \042$aCol(0)\042 skipped as the parent is \042$aCol(2)\042" FALSE
         set bUpdate FALSE
         set bReg FALSE
         set bSkipElement TRUE
      }
   } else {
      if {$aCol(3) != $aDat(3) || $sHidden != $sHiddenActual || $aCol(8) != $aDat(8)} {pMqlCmd "escape mod type \042$aCol(0)\042 abstract $aCol(3) $sHidden sparse $aCol(8)"}
      regsub -all " " $aCol(2) "" sTopLevelTest
      if {[string tolower $sTopLevelTest] == "toplevelplaceholder" && $aDat(2) == ""} {
         set bReg FALSE
         set bSkipElement TRUE
      } elseif {$aCol(2) != "" && $aCol(2) != $aDat(2)} {
         pMqlCmd "mod type \042$aCol(0)\042 derived \042$aCol(2)\042"
      } elseif {$aCol(2) == "" && $aDat(2) != ""} {
         ##Modification started for Overlay mode changes to fix issue - not able to remove parent
		 #set resultappend "ERROR: Type '$aCol(0)' derived type '$aDat(2)' may not be set back to <null> !\nTo disassociate '$aCol(0)' from '$aDat(2)', set derived type to 'Top Level Placeholder'"
         #set iExit 1
         #return $iExit
		 pMqlCmd "mod type \042$aCol(0)\042 remove derived"
		 ##Modifications ends for Overlay Mode Changes##
		       }
      pPlanActualAddDel $lsAttrImmediate $lsAttributeActual $lsAttributePlan type "" $aCol(0) "remove attribute" "add attribute" ""
      pPlanActualAddDel $lsMethodDerivative $lsMethodActual $lsMethodPlan type "" $aCol(0) "remove method" "add method" ""
   }
   return $iExit
}
