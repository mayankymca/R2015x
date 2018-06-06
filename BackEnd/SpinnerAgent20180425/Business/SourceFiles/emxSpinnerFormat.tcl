#########################################################################*2014x
#
# @progdoc      emxSpinnerFormat.tcl vM2013 (Build 5.1.12)
#
# @Description: Procedures for running in Formats
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

# Procedure to analyze formats
proc pAnalyzeFormat {} {
   global aCol aDat bOverlay bAdd
   if {$bAdd != "TRUE"} {
      foreach sDat [list 3 4] sPQ2 [list version filesuffix] {set aDat($sDat) [pPrintQuery "" $sPQ2 "" ""]}
      set aDat(5) ""
      set aDat(6) ""
      set aDat(7) ""
      set lsPrint [split [pQuery "" "print format \042$aCol(0)\042"] \n]
      foreach sPrint $lsPrint {
         set sPrint [string trim $sPrint]
         if {[string first "type" $sPrint] == 0} {
            regsub "type" $sPrint "" aDat(6)
            set aDat(6) [string trim $aDat(6)]
            set aDat(5) $aDat(6)
         } elseif {[string first "mime" $sPrint] == 0} {
            regsub "mime" $sPrint "" aDat(7)
            set aDat(7) [string trim $aDat(7)]
         }
      }
   }
   if {$bOverlay} {pOverlay [list 3 4 5 6 7]}
}

# Procedure to process formats
proc pProcessFormat {} {
   global aCol aDat bAdd sSchemaType sHidden sHiddenActual
   if {$bAdd} {
	  #KYB removed view,edit,print from writing to file, since it is not present in ENOVIA V6R2014x BPS code
	  pMqlCmd "add format \042$aCol(0)\042 version \042$aCol(3)\042 suffix \042$aCol(4)\042 creator \042$aCol(5)\042 type \042$aCol(6)\042 mime \042$aCol(7)\042 $sHidden"
   } else {
		if {$sHidden != $sHiddenActual} {pMqlCmd "escape mod format \042$aCol(0)\042 $sHidden"}
		if {$aCol(3) != $aDat(3) || $aCol(4) != $aDat(4) || $aCol(5) != $aDat(5) || $aCol(6) != $aDat(6) || $aCol(7) != $aDat(7)} {
			#KYB removed view,edit,print from writing to file, since it is not present in ENOVIA V6R2014x BPS code   
			pMqlCmd "mod format \042$aCol(0)\042 version \042$aCol(3)\042 suffix \042$aCol(4)\042 creator \042$aCol(5)\042 type \042$aCol(6)\042 mime \042$aCol(7)\042"	 
		}		
   }
   return 0
}                                          
                                      

