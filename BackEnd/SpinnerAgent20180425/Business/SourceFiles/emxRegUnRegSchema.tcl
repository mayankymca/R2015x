#************************************************************************10.9
# Procedure:   pRegisterSchema
#
# Description: Procedure to register schema element in AEF
#
# Parameters:  Schema Type, Schema Name, Symbolic Name
#
# Returns:     Nothing
#************************************************************************

#  Registration Procedure
   proc pRegisterSchema {sProcSchema_Type sProcType_Name sProcOrigName sProcSymbolic} {
      global sFile iErrCtr iRegisterCounter bScan sAction bModFlag bShowModOnly bUpdate bAppend bPercent sLogFileError bOut

      array set aName [list 1 version 2 application 3 "original name" 4 installer 5 "installed date"]
            
      set aValue(1) [pQuery "" "print program eServiceSystemInformation.tcl select property\\\[version\\\].value dump"]
      set aValue(2) Framework;#<=========================== USER MODIFIABLE VARIABLE ***
      set aValue(3) $sProcOrigName
      set aValue(4) "Dassault Systemes Services";#<=== USER MODIFIABLE VARIABLE ***
      set aValue(5) [clock format [clock seconds] -format "%m-%d-%y"]
      
      set sSystem ""
      if {$sProcSchema_Type == "table"} {
         set sSystem "system"
      }
      set bUpdate FALSE
      set sAction "Registration of $sProcSchema_Type $sProcType_Name"
      set bAppend TRUE
      if {$bScan != "TRUE"} {mql start transaction update}
   
      if { [ catch {
         if {$sProcSchema_Type == "association"} {
            array set aNameActual [list 1 "" 2 "" 3 "" 4 "" 5 ""]
            array set aValueActual [list 1 "" 2 "" 3 "" 4 "" 5 ""]
            set lsPrintAssoc [split [pQuery "" "print association \"$sProcType_Name\""] \n]
            foreach sPrintAssoc $lsPrintAssoc {
               for {set i 1} {$i < 6} {incr i} {
                  if {[string first $aName($i) $sPrintAssoc] > -1} {
                     set aNameActual($i) $aName($i)
                     regsub "value" $sPrintAssoc "|" sPrintAssoc
                     set aValueActual($i) [string trim [lindex [split $sPrintAssoc |] 1] ]
                  }
               }
            }
         } else {
            for {set i 1} {$i < 6} {incr i} {
               set aNameActual($i) [pQuery "" "print $sProcSchema_Type \"$sProcType_Name\" $sSystem select property\\\[$aName($i)\\\].name dump"]
               set aValueActual($i) [pQuery "" "print $sProcSchema_Type \"$sProcType_Name\" $sSystem select property\\\[$aName($i)\\\].value dump"]
            }
         }
         
         if {$aNameActual(3) == "" || $aValueActual(3) == ""} {
            for {set i 1} {$i < 6} {incr i} {
               set sAddMod ""
               if {$aNameActual($i) == ""} {
                  set sAddMod add
               } elseif {$aValueActual($i) != $aValue($i)} {
                  set sAddMod mod
               }
               if {$sAddMod != ""} {
			   if { $i != 3 } {
                  pMqlCmd "$sAddMod property \"$aName($i)\" on $sProcSchema_Type \"$sProcType_Name\" $sSystem value \"$aValue($i)\""
				  } else {
				   pMqlCmd "$sAddMod property \"$aName($i)\" on $sProcSchema_Type \"$sProcType_Name\" $sSystem value \"$sProcType_Name\"" 
				   }
               }
            }
         }

         set lsTo [split [pQuery "" "print program \"eServiceSchemaVariableMapping.tcl\" select property\\\[$sProcSymbolic\\\].to dump |"] |]
         if {$lsTo == ""} {
            pMqlCmd "add property \"$sProcSymbolic\" on program \"eServiceSchemaVariableMapping.tcl\" to $sProcSchema_Type \"$sProcType_Name\" $sSystem"
         } else {
            set sProcSchema_Test $sProcSchema_Type
            if {$sProcSchema_Type == "attribute"} {
               set sProcSchema_Test att
            }
            set sProcTest "$sProcSchema_Test $sProcType_Name"
            set bPass FALSE
            foreach sTo $lsTo {
               if {$sTo == $sProcTest} {
                  set bPass TRUE
                  break
               }
            }
            if {$bPass != "TRUE"} {
               pMqlCmd "add property \"$sProcSymbolic\" on program \"eServiceSchemaVariableMapping.tcl\" to $sProcSchema_Type \"$sProcType_Name\" $sSystem"
            }
         }
      } result ] != 0 } {
          if {$bScan != "TRUE"} {mql abort transaction}
          if {$sProcSchema_Type == "page" || $sProcSchema_Type == "pageobject"} {
             pAppend "REGISTRATION NOT SUCCESSFUL!  $result\n" FALSE
             set bOut FALSE
             pWriteWarningMsg "WARNING: Page registration is not currently supported by MQL! (Page '$sProcType_Name' symbolic name '$sProcSymbolic')\n"
             set bOut TRUE
          } else {
             set iLogFileErr [open $sLogFileError a+]
             puts $iLogFileErr "$sAction\n$result\n"
             close $iLogFileErr
             pAppend "REGISTRATION NOT SUCCESSFUL!  $result\n" FALSE
             incr iErrCtr
          }
          if {$bPercent != "TRUE"} {puts -nonewline "!"}
      } elseif {$bUpdate} {
         if {$bScan != "TRUE"} {pAppend "# Registration successful." FALSE}
         append sFile "\n"
         set bModFlag TRUE
         if {$bScan != "TRUE"} {mql commit transaction}
         incr iRegisterCounter
      } else {
         if {$bScan != "TRUE" && $bShowModOnly != "TRUE"} {pAppend " - not required." TRUE}
         if {$bScan != "TRUE"} {mql commit transaction}
      }
   }
#End pRegisterSchema

#************************************************************************
# Procedure:   pUnRegisterSchema
#
# Description: Procedure to unregister schema element from AEF
#
# Parameters:  Schema Type, Schema Name, Symbolic Name
#
# Returns:     Nothing
#************************************************************************
   proc pUnRegisterSchema {sProcSchema_Type sProcType_Name sProcSymbolic} {
      global sFile iErrCtr iRegisterCounter bScan sAction bModFlag bShowModOnly bUpdate bAppend bPercent sLogFileError

      set bUpdate FALSE
      if {$bScan != "TRUE"} {mql start transaction update}
      set sAction "Unregister $sProcSchema_Type $sProcType_Name"
      set bAppend TRUE
   
      set sSystem ""
      if {$sProcSchema_Type == "table"} {
         set sSystem "system"
      }

      if { [ catch {
          if {$bScan != "TRUE"} {
             if {[pQuery "" "print program \"eServiceSchemaVariableMapping.tcl\" select property\\\[$sProcSymbolic\\\] dump"] != ""} {
                set sProcSchema_Test "$sProcSchema_Type"
                if {$sProcSchema_Type == "attribute"} {
                   set sProcSchema_Test "att"
                }
				
				if {$sProcSchema_Type == "pageobject"} {
                   set sProcSchema_Test "process"
                }
                
                set sProcTest "$sProcSchema_Test $sProcType_Name"
                set lsTo [split [pQuery "" "print program \"eServiceSchemaVariableMapping.tcl\" select property\\\[$sProcSymbolic\\\].to dump |"] |]
                foreach sTo $lsTo {
                   if {$sTo == $sProcTest} {
                      pMqlCmd "delete property \"$sProcSymbolic\" on program \"eServiceSchemaVariableMapping.tcl\" to $sProcSchema_Type \"$sProcType_Name\" $sSystem"
                      break
                   }
                }
             }
          }
      } result ] != 0 } {
          set iLogFileErr [open $sLogFileError a+]
          puts $iLogFileErr "$sAction\n$result\n"
          close $iLogFileErr
          pAppend "UNREGISTRATION NOT SUCCESSFUL!  $result\n" FALSE
          if {$bScan != "TRUE"} {mql abort transaction}
          incr iErrCtr
          if {$bPercent != "TRUE"} {puts -nonewline "!"}
      } elseif {$bUpdate} {
         if {$bScan != "TRUE"} {pAppend "# Unregistration successful." FALSE}
         append sFile "\n"
         set bModFlag TRUE
         if {$bScan != "TRUE"} {mql commit transaction}
         incr iRegisterCounter -1
      } else {
         if {$bScan != "TRUE" && $bShowModOnly != "TRUE"} {pAppend " - not required." TRUE}
         if {$bScan != "TRUE"} {mql commit transaction}
      }
   }

#End pUnRegisterSchema

