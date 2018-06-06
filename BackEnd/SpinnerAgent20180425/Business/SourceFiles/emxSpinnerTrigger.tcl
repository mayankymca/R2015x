#########################################################################*2014x
#
# @progdoc      emxSpinnerTrigger.tcl vM2013 (Build 5.1.12)
#
# @Description: Procedures for running in Triggers
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

# Procedure to analyze triggers
proc pAnalyzeTrigger {} {
   global aCol aDat bOut bOverlay

   set sTrigTypePlan [string tolower "$aCol(3)$aCol(4)"]
   if {$sTrigTypePlan == "check" || $sTrigTypePlan == "action" || $sTrigTypePlan == "eventcheck" || $sTrigTypePlan == "eventaction" } {
      if {[string first "event" $sTrigTypePlan] == 0 && $aCol(5) == "" && $aCol(6) != ""} {
         pWriteWarningMsg "\nWARNING: Field 'Program' is blank while field 'Input' is not blank for '$aCol(0) $aCol(1)' trigger '$sTrigTypePlan'.\nThis trigger will be removed if one is in place"
      }
      set lsCatchTest [list action check]
      regsub "event" $sTrigTypePlan "" sTrigTypePlan
   } else {
      set lsCatchTest trigger
   }
   if {[string tolower $aCol(0)] == "policy"} {
      set sCatchStringOne [string tolower "state $aCol(2)"]
      set bPass false
   } else {
      set sCatchStringOne ""
      set bPass true
   }
   set sCatchStringTwo ""
   set aDat(5) ""
   set aDat(6) ""
   set lsPrint [split [pQuery "" "print $aCol(0) \042$aCol(1)\042"] \n]
   foreach sPrint $lsPrint {
      set sPrint [string trim $sPrint]
      if {$sCatchStringOne != ""} {
         if {[string first $sCatchStringOne [string tolower $sPrint]] == 0} {
            set bPass true
            set sCatchStringOne ""
            set sCatchStringTwo "state"
         }
      } elseif {$sCatchStringTwo != ""} {
         if {[string first $sCatchStringTwo $sPrint] == 0} {break}
      }
      if {$bPass} {
         set bCatchTest false
         foreach sCatchTest $lsCatchTest {
            if {[string first "$sCatchTest " $sPrint] == 0} {
               if {$sTrigTypePlan == "action" || $sTrigTypePlan == "check"} {
                  if {$sCatchTest == $sTrigTypePlan} {
                     set bCatchTest true
                     break
                  }
               } else {
                  set bCatchTest true
                  break
               }
            }
         }
         if {$bCatchTest} {
            regsub "$sCatchTest " $sPrint "" sPrint
            if {$sCatchTest == "trigger"} {
               set lsTrig [split $sPrint ","]
               foreach sTrig $lsTrig {
                  regsub ":" $sTrig "|" sTrig
                  set lslsTrig [split $sTrig |]
                  set sTrigTypeActual [string tolower [lindex $lslsTrig 0]]
                  if {$sTrigTypePlan == $sTrigTypeActual} {
                     set slsTrigProg [lindex $lslsTrig 1]
                     regsub "\134(" $slsTrigProg "|" slsTrigProg
                     set lslsTrigProg [split $slsTrigProg |]
                     set aDat(5) [lindex $lslsTrigProg 0]
                     set aDat(6) [lindex $lslsTrigProg 1]
                     regsub "\134)" $aDat(6) "" aDat(6)
                     break
                  }
               }
            } else {
               set sTrigTypeActual $sCatchTest
               if {$sTrigTypePlan == $sTrigTypeActual} {
                  regsub " input " $sPrint "|" sPrint
                  set lsTrig [split $sPrint |]
                  set aDat(5) [string trim [lindex $lsTrig 0]]
                  regsub -all "'" $aDat(5) "" aDat(5)
                  set aDat(6) [string trim [lindex $lsTrig 1]]
                  regsub -all "'" $aDat(6) "" aDat(6)
               }
            }
            break
         }
      }
   }
   pSetAction "Modify $aCol(0) $aCol(1) $aCol(2) trigger $aCol(3)$aCol(4)"
    if {$bOverlay} {
      pOverlay [list 5 6]
   }
}

# Procedure to process triggers
proc pProcessTrigger {} {
   global aCol aDat
   if {$aCol(2) != "" && ( [string tolower $aCol(3)] == "" || [string tolower $aCol(3)] == "event" ) } {
      if {$aCol(5) != $aDat(5) || $aCol(6) != $aDat(6)} {
         pMqlCmd "escape mod $aCol(0) \042$aCol(1)\042 state \042$aCol(2)\042 $aCol(4) \042$aCol(5)\042 input '$aCol(6)'"
      }
   } else {
      set sStateName ""
      if {$aCol(2) != ""} {set sStateName "state \134\042$aCol(2)\134\042"}
      if {$aCol(5) != ""} {
         if {$aCol(5) != $aDat(5) || $aCol(6) != $aDat(6)} {
            set aCol(6) [pRegSubEvalEscape $aCol(6)]
            pMqlCmd "escape mod $aCol(0) \042$aCol(1)\042 $sStateName add trigger $aCol(3) $aCol(4) \042$aCol(5)\042 input '$aCol(6)'"
         }
      } elseif {$aDat(5) != ""} {
         pMqlCmd "mod $aCol(0) \042$aCol(1)\042 $sStateName remove trigger $aCol(3) $aCol(4)"
      }
   }
   return 0
}                                          

