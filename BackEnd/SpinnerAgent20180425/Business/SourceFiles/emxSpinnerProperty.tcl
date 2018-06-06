#########################################################################*2014x
#
# @progdoc      emxSpinnerProperty.tcl vM2013 (Build 6.10.19)
#
# @Description: Procedures for running in Properties
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

# Procedure to analyze properties
proc pAnalyzeProperty {} {
   global aCol aDat lsPropertyValueActual lsToTypeNameActual sSystem sPropertyValueActual
   if {$aCol(2) == ""} {
      mql notice "\nERROR: Spinner file '*PropertyData*.*' field 'Property Name' is blank for '$aCol(0) $aCol(1)'"
      return 1
   } elseif {( $aCol(4) == "" && $aCol(5) != "" ) || ( $aCol(4) != "" && $aCol(5) == "" ) } {
      mql notice "\nERROR: Spinner file '*PropertyData*.*' property '$aCol(2)' field 'To Type' or 'To Name' is blank.\nBoth fields must have a value or both fields must be blank"
      return 1
   }
   set sPropertyValueActual ""
   set lsPropertyValueActual [list ]
   set lsToTypeNameActual [list ]
   set aDat(4) ""
   set aDat(5) ""
   if {$aCol(0) != "association"} {
      set lsPropertyValueActual [split [pQuery "" "print $aCol(0) \042$aCol(1)\042 $sSystem select property\134\133$aCol(2)\134\135.value dump |"] |]
      set lsToTypeNameActual [split [pQuery "" "print $aCol(0) \042$aCol(1)\042 $sSystem select property\134\133$aCol(2)\134\135.to dump |"] |]
   } else {
      set lsPrint [split [pQuery "" "print $aCol(0) \042$aCol(1)\042"] \n]
      foreach sPrint $lsPrint {
         if {[string first "property" $sPrint] == 0 && [string first $aCol(2) $sPrint] == 9} {
            if {[string first " value " $sPrint] > -1} {
               regsub " value " $sPrint "|" sPrint
               lappend lsPropertyValueActual [lindex [split $sPrint |] 1]
               set sPrint [lindex [split $sPrint |] 0]
            }
            if {[string first " to " $sPrint] > -1} {
               regsub " to " $sPrint "|" sPrint
               lappend lsToTypeNameActual [lindex [split $sPrint |] 1]
            }
         }
      }
   }
   foreach sPropertyValueActual $lsPropertyValueActual sToTypeNameActual $lsToTypeNameActual {
      if {$aCol(4) != ""} {
         regsub " " $sToTypeNameActual "|" slsToTypeNameActual
         set aDat(4) [lindex [split $slsToTypeNameActual |] 0]
         set aDat(5) [lindex [split $slsToTypeNameActual |] 1]
         if {$aCol(4) == $aDat(4) && $aCol(5) == $aDat(5)} {
            break
         } else {
            set aDat(4) ""
            set aDat(5) ""
            set sPropertyValueActual ""
         }
      } elseif {$sToTypeNameActual == ""} {
         break
      } else {
         set sPropertyValueActual ""
      }
   }
   pSetAction "Modify $aCol(0) $aCol(1) property $aCol(2)"
   return 0
}                                       

# Procedure to process properties
proc pProcessProperty {} {
   global aCol aDat bUpdate sSystem lsPropertyValueActual lsToTypeNameActual sPropertyValueActual bOut
   set bUpdate FALSE
   set sAddModify FALSE
   set sToSystem ""
   
   #KYB Start - Replaced character [,] by \[,\] in property name
   regsub -all "\134\133" $aCol(2) "\134\134\133" aCol(2)
   regsub -all "\134\135" $aCol(2) "\134\134\135" aCol(2)
   
   if {$aCol(4) == "table"} {set sToSystem system}
   if {$aCol(3) == "" && $aCol(4) == "" && $aCol(5) == ""} {
      foreach sPropertyValueActual $lsPropertyValueActual sToTypeNameActual $lsToTypeNameActual {
         regsub " " $sToTypeNameActual "|" slsToTypeNameActual
         set aDat(4) [lindex [split $slsToTypeNameActual |] 0]
         set aDat(5) [lindex [split $slsToTypeNameActual |] 1]
         set sCommand "delete property \042$aCol(2)\042 on $aCol(0) \042$aCol(1)\042 $sSystem"
         if {$aDat(4) != ""} {append sCommand " to $aDat(4) \042$aDat(5)\042 $sToSystem"}
         pMqlCmd $sCommand
      }
   } elseif {$aCol(3) == $sPropertyValueActual && $aCol(4) == $aDat(4) && $aCol(5) == $aDat(5)} {
   } elseif {$aCol(2) == "SpinnerAgent"} {
   } elseif {$aCol(0) == "program" && $aCol(1) == "eServiceSchemaVariableMapping.tcl" && $aCol(4) != ""} {
      set sTypeReplace $aCol(4)
      if {$aCol(4) == "attribute"} {set sTypeReplace "att"}
      if {[catch {set sSymbolicTest $aSymbolic($sTypeReplace|$aCol(5))} sMsg] == 0} {
         if {$sSymbolicTest != $aCol(2)} {pWriteWarningMsg "\nWARNING: Schema element `$aCol(4) $aCol(5)` symbolic name `$sSymbolicTest` is being changed to `$aCol(2)`"}
         if {$aSymbolic($sTypeReplace|$aCol(5)) != ""} {pMqlCmd "delete property \042$aSymbolic($sTypeReplace|$aCol(5))\042 on $aCol(0) \042$aCol(1)\042 to $sTypeReplace \042$aCol(5)\042 $sToSystem"}
      }
      set sAddModify add
   } elseif {$sPropertyValueActual == "" && $aDat(4) == ""} {
      set sAddModify add
   } elseif {$aCol(3) != $sPropertyValueActual} {
      set sAddModify mod
   }
      
   if {$sAddModify != "FALSE"} {
      set aCol(3) [pRegSubEvalEscape $aCol(3)]
      if {$aCol(4) == ""} {
         pMqlCmd "$sAddModify property \042$aCol(2)\042 on $aCol(0) \042$aCol(1)\042 $sSystem value \042$aCol(3)\042"
      } elseif {$aCol(3) == ""} {
         pMqlCmd "$sAddModify property \042$aCol(2)\042 on $aCol(0) \042$aCol(1)\042 $sSystem to $aCol(4) \042$aCol(5)\042 $sToSystem"
      } else {
         pMqlCmd "$sAddModify property $aCol(2) on $aCol(0) \042$aCol(1)\042 $sSystem to $aCol(4) \042$aCol(5)\042 $sToSystem value \042$aCol(3)\042"
      }
   }
   #KYB End - Replaced character [,] by \[,\] in property name
   return 0
}                                          

