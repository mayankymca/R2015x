#########################################################################*2014x
#
# @progdoc      emxSpinnerPage.tcl vM2013 (Build 6.6.15)
#
# @Description: Procedures for running in Pages
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

# Procedure to analyze pages
proc pAnalyzePage {} {
   global aCol aDat bOverlay bAdd sContent sContentActual sContentTrim sMxVersion sSpinDir sPageFile sPageFilePlan
   regsub -all "/" $aCol(0) "SLASH" sPageFile
   set bFail FALSE
   if {[catch {set iPageFile [open "$sSpinDir/Business/PageFiles/$sPageFile" r]} sMsg] != 0} {
   	  set bFail TRUE
   	  set sErrorMsg "Page file '$sSpinDir/Business/PageFiles/$sPageFile' is not present"
   }
   if {$bFail} {
      pWriteErrorMsg "\nERROR: $sErrorMsg"
      exit 1
      return
   } else {
      set sContent [read $iPageFile]
      set sContentTrim [string trim $sContent]
      close $iPageFile
   }
   if {$bAdd != "TRUE"} {
      set aDat(3) [pPrintQuery "" mime "" ""]
      set sContentActual ""
      set sContentActual [pPrintQuery "" content "" ""]
   }
   if {$bOverlay} {pOverlay [list 3]}
}

# Procedure to process pages
proc pProcessPage {} {
   global aCol aDat bAdd sContent sContentActual sContentTrim sSpinDir sHidden sHiddenActual sMxVersion bJPO sPageFile sPageFilePlan bAEF
   set bModProg FALSE
   if {$bAdd} {
      pMqlCmd "add page \042$aCol(0)\042 mime \042$aCol(3)\042 $sHidden"
   } else {
      if {$aCol(3) != $aDat(3) || $sHidden != $sHiddenActual} {pMqlCmd "mod page \042$aCol(0)\042 mime \042$aCol(3)\042 $sHidden"}
      if {[string first $sContentActual $sContent] == -1 || $sContentTrim != $sContentActual} {set bModProg TRUE}
   }
   if {$bAdd || $bModProg} {
      pMqlCmd "mod page \042$aCol(0)\042 file \042$sSpinDir/Business/PageFiles/$sPageFile\042"
   }
   return 0
}
