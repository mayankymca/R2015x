#########################################################################*2014x
#
# @progdoc      emxSpinnerRule.tcl vM2013 (Build 5.1.12)
#
# @Description: Procedures for running in Rules
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

# Procedure to analyze rules
proc pAnalyzeRule {} {
   global aCol aDat bOverlay bAdd lsProgramPlan lsProgramActual lsAttributePlan lsAttributeActual lsRelationshipPlan lsRelationshipActual lsFormPlan lsFormActual lsAccessPlan lsAccessActual bUseAccessField lsSchemaType
   set lsProgramPlan [pTrimList $aCol(3)]
   set lsAttributePlan [pTrimList $aCol(4)]
   if {[lsearch $lsSchemaType attribute] >= 0} {set lsAttributePlan [pCheckNameChange $lsAttributePlan attribute]}
   set lsRelationshipPlan [pTrimList $aCol(5)]
   if {[lsearch $lsSchemaType relationship] >= 0} {set lsRelationshipPlan [pCheckNameChange $lsRelationshipPlan relationship]}
   set lsFormPlan [pTrimList $aCol(6)]
   set lsAccessPlan [pTrimList $aCol(7)]
   set lsProgramActual [list ]
   set lsAttributeActual [list ]
   set lsRelationshipActual [list ]
   set lsFormActual [list ]
   set lsAccessActual [list ]
   
   set aCol(8) [pCompareAttr $aCol(8) notenforcereserveaccess enforcereserveaccess true true]
   # set aCol(8) [pCompareAttr $aCol(8) notenforcereserveaccess notenforcereserveaccess true true]

   if {$bAdd != "TRUE"} {
      if {$bUseAccessField} {set lsAccessActual [pQueryAccess rule $aCol(0) access]}
	  
	  set bEnforceReserveAccessActual [pPrintQuery "" "state\134\133$aCol(1)\134\135.enforcereserveaccess" "" ""]
	  set aDat(8) [pCompareAttr $bEnforceReserveAccessActual notenforcereserveaccess enforcereserveaccess true false]
	   #set aDat(8) [pCompareAttr $bEnforceReserveAccessActual notenforcereserveaccess enforcereserveaccess true false]
	  
      set lsPrint [split [pQuery "" "print rule \042$aCol(0)\042"] \n]
      foreach sPrint $lsPrint {
         set sPrint [string trim $sPrint]
         foreach sReference [list program attribute "Relationship Type" form] {
            if {[string first $sReference $sPrint] == 0} {
               regsub "$sReference\: " $sPrint "" slsReference
               regsub -all ", " $slsReference "|" slsReference
               set lsReference [split $slsReference |]
               switch $sReference {
                  program {
                     set lsProgramActual $lsReference
                  } attribute {
                     set lsAttributeActual $lsReference
                  } "Relationship Type" {
                     set lsRelationshipActual $lsReference
                  } form {
                     set lsFormActual $lsReference
                  }
               }
            }
         }
      }
   }
   if {$bOverlay} {
      set lsProgramPlan [pOverlayList $lsProgramPlan $lsProgramActual]
      set lsAttributePlan [pOverlayList $lsAttributePlan $lsAttributeActual]
      set lsRelationshipPlan [pOverlayList $lsRelationshipPlan $lsRelationshipActual]
      set lsFormPlan [pOverlayList $lsFormPlan $lsFormActual]
      set lsAccessPlan [pOverlayList $lsAccessPlan $lsAccessActual]
   }
}                                       

# Procedure to process rules
proc pProcessRule {} {
   global aCol aDat bAdd lsSchemaType lsProgramPlan lsProgramActual lsAttributePlan lsAttributeActual lsRelationshipPlan lsRelationshipActual lsFormPlan lsFormActual lsAccessPlan lsAccessActual bUseAccessField sHidden sHiddenActual
   if {$bAdd} {
      pMqlCmd "add rule \042$aCol(0)\042 $sHidden"
      foreach sSchemaItem [list program attribute relationship form] lsRefPlan [list $lsProgramPlan $lsAttributePlan $lsRelationshipPlan $lsFormPlan] {
         foreach sRefPlan $lsRefPlan {pMqlCmd "mod $sSchemaItem \042$sRefPlan\042 add rule \042$aCol(0)\042"}
      }
      if {$bUseAccessField} {pPlanAdd $lsAccessPlan rule $aCol(0) "add user" ""}
	  
   } else {
      if {$sHidden != $sHiddenActual} {pMqlCmd "mod rule \042$aCol(0)\042 $sHidden"}
	  if { $aCol(8) != $aDat(8) } {pMqlCmd "mod rule \042$aCol(0)\042 $aCol(8)"}
      foreach sSchemaItem [list program attribute relationship form] lsRefPlan [list $lsProgramPlan $lsAttributePlan $lsRelationshipPlan $lsFormPlan] lsRefActual [list $lsProgramActual $lsAttributeActual $lsRelationshipActual $lsFormActual] {
         foreach sRefPlan $lsRefPlan sRefActual $lsRefActual {
            if {$sRefActual != "" && [lsearch $lsRefPlan $sRefActual] < 0} {pMqlCmd "mod $sSchemaItem \042$sRefActual\042 remove rule \042$aCol(0)\042"}
            if {$sRefPlan != "" && [lsearch $lsRefActual $sRefPlan] < 0} {pMqlCmd "mod $sSchemaItem \042$sRefPlan\042 add rule \042$aCol(0)\042"}
         }
      }
      if {$bUseAccessField} {pPlanActualAddDel $lsAccessActual "" $lsAccessPlan rule "" $aCol(1) "remove user" "add user" ""}
   }
   return 0
}                                          

