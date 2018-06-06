#########################################################################*2014x
#
# @progdoc      emxSpinnerExpression.tcl vM2013 (Build 7.1.15)
#
# @Description: Procedures for running in Expressions
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

# Procedure to pass tcl-type variables in mql commands
   proc pRegSubMqlEscape {sEscape} {
      regsub -all "\134$" $sEscape "\134\134\$" sEscape
      regsub -all "\134{" $sEscape "\134\173" sEscape
      regsub -all "\134}" $sEscape "\134\175" sEscape
      regsub -all "\134\133" $sEscape "\134\133" sEscape
      regsub -all "\134\135" $sEscape "\134\135" sEscape
      regsub -all "\042" $sEscape "\134\042" sEscape
      regsub -all "\047" $sEscape "\134\047" sEscape
      return $sEscape
   }

# Procedure to analyze expressions
proc pAnalyzeExpression {} {
   global aCol aDat bOverlay bAdd
   if {$bAdd != "TRUE"} {
      set aDat(3) [pPrintQuery "" value "" ""]
   }
   if {$bOverlay} {
      pOverlay [list 3]
   }
}

# Procedure to process expressions
proc pProcessExpression {} {
   global aCol aDat bAdd sHidden sHiddenActual bUpdate bReg bScan resultappend
   set iExit 0
   set aCol(3) [pRegSubMqlEscape $aCol(3)]
   if {$bAdd} {
      pMqlCmd "add expression \042$aCol(0)\042 value \042$aCol(3)\042 $sHidden"
   } else {
      if {$aCol(3) != $aDat(3) || $sHidden != $sHiddenActual} {
		pMqlCmd "mod expression \042$aCol(0)\042 value \042$aCol(3)\042 $sHidden"		
	  }
   }
   return $iExit
}

