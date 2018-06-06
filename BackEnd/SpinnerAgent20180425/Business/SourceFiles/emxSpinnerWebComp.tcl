#########################################################################*2014x
#
# @progdoc      emxSpinnerWebComp.tcl vM2013 (Build 9.9.23)
#
# @Description: Procedures for running in Web Components
#               (Command, Menu, Channel, Portal, Inquiry, Table, WebForm)
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
#                 Medha TAMBE 07/02/2014 - Fixed command setting value issue of single and double quotes
#                 Medha TAMBE 07/02/2014 - Fixed for command code, import code from command.json file
#
#########################################################################
# Procedure to set setting names and values
   proc pSetSetting {sSchType sSchName slsStgName slsStgValue sMidCommand sStgArgument} {
      global lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgPlan aStgActual sRangeDelim sColumnField aCol sLogFileError bOverlay bAdd lsDel
      set lsStgNamePlan [pTrimList $slsStgName]
      regsub -all "\134\174\134\174" $slsStgValue "<OR>" slsStgValue
      regsub -all "&&" $slsStgValue "<AND>" slsStgValue
      if {$sSchType == "form"} {
         regsub -all " \134\174 " $slsStgValue "<SPLIT>" slsStgValue
         regsub -all "\134\174" $slsStgValue "<PIPE>" slsStgValue
         regsub -all "<SPLIT>" $slsStgValue "|" slsStgValue
      }	  	  
	  
      set lsStgValuePlanTemp [split $slsStgValue $sRangeDelim]	  
      set lsStgValuePlan ""
      foreach sStgValue $lsStgValuePlanTemp {
      	 regsub -all "<OR>" $sStgValue "\174\174" sStgValue
      	 regsub -all "<AND>" $sStgValue "\134\&\134\&" sStgValue
         regsub -all "<PIPE>" $sStgValue "|" sStgValue
         regsub -all "<NULL>" $sStgValue "" sStgValue
		 regsub -all "<PIPE:>" $sStgValue "\134|" sStgValue
         lappend lsStgValuePlan [string trim $sStgValue]
      }
      set lsStgNameActual [split [pQuery "" "print $sSchType \042$sSchName\042 $sMidCommand$sStgArgument.name dump |"] |]
      set lsStgValueActual [split [pQuery "" "print $sSchType \042$sSchName\042 $sMidCommand$sStgArgument.value dump ^"] ^]
      foreach sStgNameActual $lsStgNameActual sStgValueActual $lsStgValueActual {array set aStgActual [list $sStgNameActual $sStgValueActual]}
      if {[llength $lsStgNamePlan] != [llength $lsStgValuePlan]} {
         set sAppend ""
         if {$sSchType == "form" || $sSchType == "table"} {set sAppend " $sColumnField '$aCol(1)'"}
         set iLogFileErr [open $sLogFileError a+]
         puts $iLogFileErr "\nERROR: '$sSchType' '$sSchName'$sAppend setting name and value lists are not the same length"
         close $iLogFileErr
         if {[llength $lsStgNamePlan] > [llength $lsStgValuePlan] && [string first "<OR>" $slsStgValue] > -1} {
            set iLogFileErr [open $sLogFileError a+]
            puts $iLogFileErr "Be sure to leave a space between '|'s if null values are intended vs. double '|'s"
            close $iLogFileErr
         }
         return 1
      }
      if {$bOverlay} {
      	 if {$lsStgNamePlan == "<NULL>"} {
      	    set lsStgNamePlan [list ]
      	    set lsStgValuePlan [list ]
      	 } elseif {$bAdd != "TRUE" && $lsStgNamePlan == ""} {
      	    set lsStgNamePlan $lsStgNameActual
      	    set lsStgValuePlan $lsStgValueActual
      	 } else {
      	    set lsTemp [pMergeList $lsStgNamePlan $lsStgValuePlan $lsStgNameActual $lsStgValueActual ""]
      	    set lsStgNamePlan [lindex $lsTemp 0]
      	    set lsStgValuePlan [lindex $lsTemp 1]
      	 }
      }
      foreach sStgNamePlan $lsStgNamePlan sStgValuePlan $lsStgValuePlan {array set aStgPlan [list $sStgNamePlan $sStgValuePlan]}
      return 0
   }
   
   # Procedure to set setting names and values
   proc pSetFormSetting {sSchType sSchName slsStgName slsStgValue sMidCommand sStgArgument} {
      global lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgPlan aStgActual sRangeDelim sColumnField aCol sLogFileError bOverlay bAdd lsDel
      set lsStgNamePlan [pTrimList $slsStgName]
      regsub -all "\134\174\134\174" $slsStgValue "<OR>" slsStgValue
      regsub -all "&&" $slsStgValue "<AND>" slsStgValue
      if {$sSchType == "form"} {
         regsub -all " \134\174 " $slsStgValue "<SPLIT>" slsStgValue
         regsub -all "\134\174" $slsStgValue "<PIPE>" slsStgValue
         regsub -all "<SPLIT>" $slsStgValue "|" slsStgValue
      }	  	  
	  
      set lsStgValuePlanTemp [split $slsStgValue $sRangeDelim]	  
      set lsStgValuePlan ""
      foreach sStgValue $lsStgValuePlanTemp {
      	 regsub -all "<OR>" $sStgValue "\174\174" sStgValue
      	 regsub -all "<AND>" $sStgValue "\134\&\134\&" sStgValue
         regsub -all "<PIPE>" $sStgValue "|" sStgValue
         regsub -all "<NULL>" $sStgValue "" sStgValue
		 regsub -all "<PIPE:>" $sStgValue "\134|" sStgValue
         lappend lsStgValuePlan [string trim $sStgValue]
      }
      #set lsStgNameActual [split [pQuery "" "print $sSchType \042$sSchName\042 $sMidCommand$sStgArgument.name dump |"] |]
      #set lsStgValueActual [split [pQuery "" "print $sSchType \042$sSchName\042 $sMidCommand$sStgArgument.value dump ^"] ^]
      foreach sStgNameActual $lsStgNameActual sStgValueActual $lsStgValueActual {array set aStgActual [list $sStgNameActual $sStgValueActual]}
      if {[llength $lsStgNamePlan] != [llength $lsStgValuePlan]} {
         set sAppend ""
         if {$sSchType == "form" || $sSchType == "table"} {set sAppend " $sColumnField '$aCol(1)'"}
         set iLogFileErr [open $sLogFileError a+]
         puts $iLogFileErr "\nERROR: '$sSchType' '$sSchName'$sAppend setting name and value lists are not the same length"
         close $iLogFileErr
         if {[llength $lsStgNamePlan] > [llength $lsStgValuePlan] && [string first "<OR>" $slsStgValue] > -1} {
            set iLogFileErr [open $sLogFileError a+]
            puts $iLogFileErr "Be sure to leave a space between '|'s if null values are intended vs. double '|'s"
            close $iLogFileErr
         }
         return 1
      }
      if {$bOverlay} {
      	 if {$lsStgNamePlan == "<NULL>"} {
      	    set lsStgNamePlan [list ]
      	    set lsStgValuePlan [list ]
      	 } elseif {$bAdd != "TRUE" && $lsStgNamePlan == ""} {
      	    set lsStgNamePlan $lsStgNameActual
      	    set lsStgValuePlan $lsStgValueActual
      	 } else {
      	    set lsTemp [pMergeList $lsStgNamePlan $lsStgValuePlan $lsStgNameActual $lsStgValueActual ""]
      	    set lsStgNamePlan [lindex $lsTemp 0]
      	    set lsStgValuePlan [lindex $lsTemp 1]
      	 }
      }
      foreach sStgNamePlan $lsStgNamePlan sStgValuePlan $lsStgValuePlan {array set aStgPlan [list $sStgNamePlan $sStgValuePlan]}
      return 0
   }

# Procedure to process setting names and values
   proc pSetting {sSchType sSchName sMidCommand sStgArgument} {
      global lsStgNameActual lsStgValueActual lsStgNamePlan lsStgValuePlan aStgActual aStgPlan
	  set sCommand "escape mod $sSchType \042$sSchName\042 $sMidCommand"
	  set sAppendCommand ""
      foreach sStgNameA $lsStgNameActual sStgValueA $lsStgValueActual sStgNameP $lsStgNamePlan sStgValueP $lsStgValuePlan {
         if {$sStgNameA != ""} {
		 ######### START Added by SL Team for spinner V6R2012 validation ##########
		 if { $sStgNameP  == "" || $sStgNameP  == "%"} {
		  pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand remove $sStgArgument \042$sStgNameP\042"
		 }

		 ######### END ##########
            if {[lsearch $lsStgNamePlan $sStgNameA] < 0} {
               pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand remove $sStgArgument \042$sStgNameA\042"
            } elseif {$aStgPlan($sStgNameA) != $sStgValueA} {
               set sModSettingValue [pRegSubEvalEscape $aStgPlan($sStgNameA)]
               if {[string first "\047" $sModSettingValue] < 0 && [string first "javascript" [string tolower $sModSettingValue]] < 0} {
				  #KYB Fixed SCR-0001804 Single quote issue for Bell
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameA\042 '$sModSettingValue'"
				  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameA\042 \042$sModSettingValue\042"
				  #KYB Fixed SCR-0007292 - Spinner errors when trying to import commands with “hidden”==”true”
				   set checkIfDoubleQuote [string match "*\"*" $sModSettingValue]
				   if { $checkIfDoubleQuote == 1 } { 
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameA\042 '$sModSettingValue'"
				  append sAppendCommand " add $sStgArgument \042$sStgNameA\042 '$sModSettingValue'"
               } else {
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameA\042 \042$sModSettingValue\042"
				  append sAppendCommand " add $sStgArgument \042$sStgNameA\042 \042$sModSettingValue\042"
               }
               } else {
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameA\042 \042$sModSettingValue\042"
				  append sAppendCommand " add $sStgArgument \042$sStgNameA\042 \042$sModSettingValue\042"
               }
               array set aStgActual [list $sStgNameA $aStgPlan($sStgNameA)]
            }
         }
         if {$sStgNameP != ""} {
            if {[lsearch $lsStgNameActual $sStgNameP] < 0} {
               set sModSettingValue [pRegSubEvalEscape $sStgValueP]
               if {[string first "\047" $sModSettingValue] < 0 && [string first "javascript" [string tolower $sModSettingValue]] < 0} {
			      ######### START Added by SL Team for spinner V6R2012 validation ##########
			       if { $sStgNameP  == "" || $sStgNameP  == "%"} {
				  
				  pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand remove $sStgArgument \042$sStgNameP\042"
				  ######### END ##########
				   } else {
				  #KYB Fixed SCR-0001804 Single quote issue for Bell
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 '$sModSettingValue'"
				  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
				  #KYB Fixed SCR-0007292 - Spinner errors when trying to import commands with “hidden”==”true”
				  set checkIfDoubleQuote [string match "*\"*" $sModSettingValue]
					  if { $checkIfDoubleQuote == 1 } { 
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 '$sModSettingValue'"
				  append sAppendCommand " add $sStgArgument \042$sStgNameP\042 '$sModSettingValue'"
					   } else {
							#pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
							append sAppendCommand " add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
					   }
				  }
               } else {
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
				  append sAppendCommand " add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
               }
            } elseif {$aStgActual($sStgNameP) != $sStgValueP} {
               set sModSettingValue [pRegSubEvalEscape $sStgValueP]
               if {[string first "\047" $sModSettingValue] < 0 && [string first "javascript" [string tolower $sModSettingValue]] < 0} {
				  #KYB Fixed SCR-0001804 Single quote issue for Bell
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 '$sModSettingValue'"
				  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
				  #KYB Fixed SCR-0007292 - Spinner errors when trying to import commands with “hidden”==”true”
				   set checkIfDoubleQuote [string match "*\"*" $sModSettingValue]
				   if { $checkIfDoubleQuote == 1 } { 
						#pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 '$sModSettingValue'"
						append sAppendCommand " add $sStgArgument \042$sStgNameP\042 '$sModSettingValue'"
					} else {
						#pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
						append sAppendCommand " add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
					}
               } else {
                  #pMqlCmd "escape mod $sSchType \042$sSchName\042 $sMidCommand add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
				  append sAppendCommand " add $sStgArgument \042$sStgNameP\042 \042$sModSettingValue\042"
               }
               array set aStgActual [list $sStgNameP $sStgValueP]
            }
         }
      }
	if {$sAppendCommand != ""} {
		append sCommand $sAppendCommand
		pMqlCmd $sCommand
	 }
   }

# Procedure to set up portal channel append string
   proc pPortalChannel {lsChannel} {
      set sAppend ""
      foreach sChannel $lsChannel {
         set lsChannelItem [split $sChannel ,]
         set lsChannelRow ""
         foreach sChannelItem $lsChannelItem {lappend lsChannelRow "'[string trim $sChannelItem]'"}
         append sAppend " channel [join $lsChannelRow ,]"
      }
      return $sAppend
   }

# Procedure to analyze web components
proc pAnalyzeWebComp {} {
   global aCol aDat bOverlay bAdd bUserAll sSchemaType lsSchemaType aSchemaElement lsCommandPlan lsCommandActual lsChannelPlan lsChannelActual lsUserPlan lsUserActual lsCmdMenuPlan lsCmdMenuActual aCmdMenuPlan aCmdMenuActual lsColumnNamePlan lsColumnNameActual lsTypePlan lsTypeActual sColumnField sSystem sMxVersion sNumberActual lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgPlan aStgActual sRangeDelim sLogFileError lsDel bRepeat lsPlanOrder sPlanUnordered sPrevForm isFieldDuplicate aHiddenIndex
   
   set fExt  ".inq"
   set sSpinnerPath [mql get env SPINNERPATH]
   set sInquiryFileDir "$sSpinnerPath/Business/SourceFiles/"
   set isFieldDuplicate "FALSE"
	
   switch $sSchemaType {
      command - menu - channel - portal {
         set bReturn [pSetSetting $sSchemaType $aCol(0) $aCol(6) $aCol(7) "select " setting]
         if {$bReturn} {
            puts "\nError - Review log file '$sLogFileError', correct problem(s) and restart"
            return 1
         }
         set lsCommandPlan [pTrimList $aCol(8)]
         if {$bAdd != "TRUE"} {
            set aDat(3) [pPrintQuery "" label "" ""]
            set aDat(4) [pPrintQuery "" href "" ""]
            set aDat(5) [pPrintQuery "" alt "" ""]
         }
         if {$bOverlay} {pOverlay [list 3 4 5]}
         switch $sSchemaType {
            command {
               set lsUserPlan $lsCommandPlan
               set lsUserActual ""
               if {$bAdd != "TRUE"} {
                  set lsUserActual [pPrintQuery "" user | spl]
                  set aDat(10) [pPrintQuery "" code "" ""]
                  set bUserAll [pPrintQuery "" property\134\133UserAll\134\135.value "" ""]
               }
               regsub -all "<NEWLINE>" $aCol(10) "\012" aCol(10)
               regsub -all "<TAB>" $aCol(10) "\011" aCol(10)
               regsub -all "<DQUOTE>" $aCol(10) "\042" aCol(10)
			   
			   #KYB Start
			   set bCommandFile [mql get env COMMMANDCODE]
			   if { $bCommandFile == "TRUE" } {
					set filePath ""
					append filePath $sInquiryFileDir $aCol(0) ".json"
					
					if {[file exists $filePath] == 1} {
						set iFile [open $filePath r]
						set slsDataFile [read $iFile]
						close $iFile
						if {$aCol(10) != "<NULL>"} {
							set aCol(10) [string trim $slsDataFile]
							if { $aCol(10) != "" } {
							puts "Command Code File Path: $filePath"
							}
						}
					}
				}
				# KYB END
				if {$bOverlay} {
                  pOverlay [list 10]
                  set lsUserPlan [pOverlayList $lsUserPlan $lsUserActual]
               }
            } menu {
               set lsCmdMenuPlan $lsCommandPlan
               set lsCmdMenuActual ""
               if {$bAdd != "TRUE"} {
                  set lsCmdMenuActual [pPrintQuery "" child | spl]
                  foreach sCmdMenuActual $lsCmdMenuActual {set aCmdMenuActual($sCmdMenuActual) [pPrintQuery "menu" "child\134\133$sCmdMenuActual\134\135.type" "" ""]}
               }
               if {$bOverlay} {set lsCmdMenuPlan [pOverlayList $lsCmdMenuPlan $lsCmdMenuActual]}
               if {!$bRepeat && [llength [lsort -unique $lsCmdMenuPlan]] != [llength $lsCmdMenuPlan]} {
                  set lsTest [lsort $lsCmdMenuPlan]
                  set sPrevTest ""
                  foreach sTest $lsTest {
                     if {$sTest == $sPrevTest} {
                        puts "\nERROR: Duplicate item '$sTest' in command/menu list for menu '$aCol(0)'"
                        break
                     } else {
                        set sPrevTest $sTest
                     }
                  }
               }
               foreach sCmdMenuPlan $lsCmdMenuPlan {
                  if {[pQuery "" "list command \042$sCmdMenuPlan\042"] == ""} {
                     set aCmdMenuPlan($sCmdMenuPlan) menu
                  } else {
                     set aCmdMenuPlan($sCmdMenuPlan) command
                  }
               }
            } channel {
               if {[lsearch $lsSchemaType command] >= 0} {set lsCommandPlan [pCheckNameChange $lsCommandPlan command]}
               if {$aCol(9) == "" && ($bOverlay != "TRUE" || $bAdd)} {set aCol(9) 0}
               set lsCommandActual ""
               if {$bAdd != "TRUE"} {
                  set aDat(9) [pPrintQuery "0" height "" ""]
                  set lsCommandActual [pPrintQuery "" command | spl]
               }
               if {$bOverlay} {
                  pOverlay [list 9]
				  if {$aCol(9) == ""} {set aCol(9) 0}
                  set lsCommandPlan [pOverlayList $lsCommandPlan $lsCommandActual]
               }
            } portal {
               set lsChannelPlan $lsCommandPlan
               set lsChannelActual ""
      	       if {$bAdd != "TRUE"} {
                  set lsPrint [split [pQuery "" "print portal \042$aCol(0)\042"] \n]
                  foreach sPrint $lsPrint {
                     set sPrint [string trim $sPrint]
                     if {[string first "channel" $sPrint] == 0} {
                        regsub "channel " $sPrint "" sPrint
                        lappend lsChannelActual $sPrint
                     }
                  }
      	       }
               #if {$bOverlay} {set lsChannelPlan [pOverlayList $lsChannelPlan $lsChannelActual]}
            }
         }
      } inquiry {
         set bReturn [pSetSetting inquiry $aCol(0) $aCol(5) $aCol(6) "select " argument]
         if {$bReturn} {
            puts "\nError - Review log file '$sLogFileError', correct problem(s) and restart"
            return 1
         }

        # START-Added By SL Team for Inquiry Issue      
		 set filePath ""
			    append filePath $sInquiryFileDir $aCol(0) $fExt
				puts "File Path $filePath"
								
				# KYB Start V6R2013x Fixed adding a new inquiry without a .inq file
				if {[file exists $filePath] == 1} {
				} else {
					set cFile [open $filePath w]
					puts $cFile $aCol(7)
					close $cFile
				}
				# KYB End V6R2013x Fixed adding a new inquiry without a .inq file
			if {$aCol(7) != "<NULL>"} {
				set iFile [open $filePath r]
				set slsDataFile [read $iFile]
				puts "File Content $slsDataFile"
				close $iFile
				set aCol(7) [string trim $slsDataFile]
			}
		# END	
				
         if {$bAdd != "TRUE"} {
            set aDat(3) [pPrintQuery "" pattern "" ""]
            set aDat(4) [pPrintQuery "" format "" ""]
            set aDat(7) [pPrintQuery "" code "" ""]
         }
         if {$bOverlay} {pOverlay [list 3 4 7]}
      } table - webform {
         if {$sSchemaType == "webform"} {
            set lsTypePlan [pTrimList $aCol(4)]
            set lsTypeActual [split [pQuery "" "print form \042$aCol(0)\042 select type dump |"] |]
            if {$bOverlay} {set lsTypePlan [pOverlayList $lsTypePlan $lsTypeActual]}
         }
      } column - field {
		 #KYB Start
		 set bDeleteFlag "FALSE" 
	     set lsColumnNamePlan [pTrimList $aCol(1)] 
		 
		 if {$bOverlay} {
			set iLenMinTwo [expr [string length $aCol(1)] -2]
			if {[string first "<<" $aCol(1)] >= 0 && [string first ">>" $aCol(1)] == $iLenMinTwo} {
				set bDeleteFlag "TRUE"
			}
		} else {
			set bDelField TRUE
			for {set i 2} {$i < [expr $aHiddenIndex($sSchemaType)]} {incr i} {
				if {$aCol($i) != ""} {
					set bDelField FALSE
					break
				}
			}
			if {$bDelField == "TRUE"} {
				set bDeleteFlag "TRUE"
				set sColName "<<"
				set lsColumnNamePlan [pTrimList [append sColName $aCol(1) ">>"]] 
			}
		}
		
		 set lsColumnNameActual [split [pQuery "" "print $aSchemaElement($sSchemaType) \042$aCol(0)\042 $sSystem select $sColumnField.name dump |"] |]
		 #if {$bOverlay} {
            if {$sSchemaType == "webform" && [lsort $lsColumnNamePlan] != [lsort -unique $lsColumnNamePlan]} {
               if {$bAdd != "TRUE"} {
                  pWriteWarningMsg "\nWARNING: '$sSchemaType' '$aCol(0)' references duplicate field names - these cannot be processed safely.\nAll field additions, removals or re-ordering actions will be skipped."
                  set lsColumnNamePlan $lsColumnNameActual
               }
            } else {
               set lsColumnNamePlan [pOverlayList $lsColumnNamePlan $lsColumnNameActual]
            }
         #}		 
		 #KYB End 
	  
		if {$bDeleteFlag == "FALSE"} {
			 if {$aCol(4) != "" && [string range [string tolower $aCol(4)] 0 2] != "bus"} {
				if {[string range [string tolower $aCol(4)] 0 2] != "rel"} {
				   set aCol(4) set
				} else {
				   set aCol(4) rel
				}
			 } elseif {$bOverlay && $aCol(4) == ""} {
			 } else {
				set aCol(4) bus
			 }
			 regsub -all "\042" $aCol(6) "" aCol(6)
			 
			 set lsUserPlan [pTrimList $aCol(9)]
			 if {$sSchemaType == "column"} {
				
				set bReturn [pSetSetting $aSchemaElement($sSchemaType) $aCol(0) $aCol(7) $aCol(8) "$sSystem select $sColumnField\134\133$aCol(1)\134\135." setting]
				 if {$bReturn} {
					puts "\nError - Review log file '$sLogFileError', correct problem(s) and restart"
					return 1
				 }
			 
				foreach iDat [list 2 3 4 6 5] sProperty [list label description expressiontype href expression] {set aDat($iDat) ""}
				#set aDat(5) [pPrintQuery "" "column\134\133$aCol(1)\134\135.expression" "" ""]
				#set aDat(4) [string range $aDat(4) 0 2]
				set sNumberActual [pQuery "" "print $aSchemaElement($sSchemaType) \042$aCol(0)\042 $sSystem select $sColumnField\134\133$aCol(1)\134\135.number dump"]
				if {$sMxVersion > 9.5} {
				   #set lsUserActual [split [pQuery "" "print $aSchemaElement($sSchemaType) \042$aCol(0)\042 $sSystem select $sColumnField\134\133$aCol(1)\134\135.user dump |"] |]
				   set lsUserActual [list ]
				   foreach iDat [list 10 11 12] sProperty [list alt range update] {set aDat($iDat) ""}
				   set sSortType [string range [string tolower $aCol(13)] 0 2]
				   switch $sSortType {
					  alp {
						 set aCol(13) alpha
					  } num {
						 set aCol(13) numeric
					  } oth {
						 set aCol(13) other
					  } default {
						 if {$bOverlay && $aCol(13) == ""} {
						 } else {
							set aCol(13) none
						 }
					  }
				   }
				   set aDat(13) "none"
				   set lsPrint [split [pQuery "" "print table \042$aCol(0)\042 system"] \n]
				   set bTrip "FALSE"
				   foreach sPrint $lsPrint {
					  set sPrint [string trim $sPrint]
					  if {$bTrip && [string index $sPrint 0] == "#" && [string first " column" $sPrint] > 0} {
							break
					 } elseif {[string index $sPrint 0] == "#" && [string first " column" $sPrint] > 0} {
						 #set bTrip TRUE
						 set aDat(4) ""
						 set aDat(5) ""
						 set aDat(2) ""
						 if {[string first "set" $sPrint] > 9} {
							set aDat(4) "set"
						 }
					  } elseif {[string first "businessobject" $sPrint] == 0} {
							regsub "businessobject" $sPrint "" sPrint
							 set aDat(4) "businessobject"
							 set aDat(5) [string trim $sPrint]
					 } elseif {[string first "relationship" $sPrint] == 0} {
							regsub "relationship" $sPrint "" sPrint
							 set aDat(4) "relationship"
							 set aDat(5) [string trim $sPrint]
					} elseif {$sPrint == "set"} {
							set aDat(4) "set"
					} elseif {[string first "label" $sPrint] == 0} {
							 regsub "label" $sPrint "" sPrint
							 set aDat(2) [string trim $sPrint]
					} elseif {[string range $sPrint 0 3] == "name" && [string first $aCol(1) $sPrint] > 3} {
							regsub "name" $sPrint "" sPrint
							set sPrint [string trim $sPrint]
							if {$sPrint == $aCol(1)} {
								set bTrip TRUE
							}
					} elseif {$bTrip} {
						if {[string first "href" $sPrint] == 0} {
								 regsub "href" $sPrint "" sPrint
								 set aDat(6) [string trim $sPrint]
							  } elseif {[string first "alt" $sPrint] == 0} {
								 regsub "alt" $sPrint "" sPrint
								 set aDat(10) [string trim $sPrint]
							  } elseif {[string first "range" $sPrint] == 0} {
								 regsub "range" $sPrint "" sPrint
								 set aDat(11) [string trim $sPrint]
							  } elseif {[string first "update" $sPrint] == 0} {
								 regsub "update" $sPrint "" sPrint
								 set aDat(12) [string trim $sPrint]
							  } elseif {[string first "user" $sPrint] == 0} {
								 regsub "user" $sPrint "" sPrint
								 lappend lsUserActual [string trim $sPrint]
							  } elseif {[string range $sPrint 0 7] == "sorttype"} {
								regsub "sorttype" $sPrint "" sPrint
								set aDat(13) [string trim $sPrint]
							 }						 
						}
				   }
				    if {$bTrip != "TRUE"} {
						set aDat(4) ""
						set aDat(5) ""
						set aDat(2) ""
				    }
				   
				   set aDat(4) [string range $aDat(4) 0 2]
				   if {$aCol(5) != ""} { regsub -all "<PIPE:>" $aCol(5) "|" aCol(5) }
				   if {$bOverlay} {pOverlay [list 2 3 4 5 6]}
				
				   if {$bOverlay} {
					  pOverlay [list 10 11 12]
					  if {$sSchemaType == "column"} {pOverlay [list 13]}
					  set lsUserPlan [pOverlayList $lsUserPlan $lsUserActual]
				   }
				   if {$sPrevForm ==  ""} {set sPrevForm $aCol(0)}
					if {$sPrevForm ==  $aCol(0)} {
						if {[llength $lsPlanOrder] > 1 && $aCol(14) != "" } {
							lappend lsPlanOrder [list $aCol(1) $aCol(14)]
						} elseif {$aCol(13) != ""} { lappend lsPlanOrder [list $aCol(1) $aCol(14)] }
						
						if { [string length $sPlanUnordered] > 1 && $aCol(14) == "" } {
							append sPlanUnordered "|" $aCol(1)
						} elseif {$aCol(14) == ""} { set sPlanUnordered $aCol(1) }
					}
				}
			 } else {
				set lsFieldActual [pPrintQuery "" "field.name" | spl]
				set lsFieldNumber [pPrintQuery "" "field.number" | spl]
				set sFieldTest "|[join [lsort $lsFieldActual ] |]|"
				if {[string first "|$aCol(1)|$aCol(1)|" $sFieldTest] >= 0 } {
				   set isFieldDuplicate "TRUE"
				   if {$aCol(13) == ""} {
					  pWriteErrorMsg "\nERROR: WebForm '$aCol(0)' field name '$aCol(1)' is duplicated in the database and needs the field order specified."
					  return 1
				   } elseif {[lindex $lsFieldActual [expr $aCol(13) - 1]] != $aCol(1)} {
					  pWriteErrorMsg "\nERROR: WebForm '$aCol(0)' field name '$aCol(1)' order number '$aCol(13)' does not match the database.\nThis field uses a duplicated name cannot be processed without the correct field order specified."
					  return 1
				   } else {
					  set iLindex [expr $aCol(13) - 1]
					  set sNumberActual [lindex $lsFieldNumber $iLindex]
				   }
				} else {
				   #set iLindex [lsearch [split [pQuery "" "print form \042$aCol(0)\042 select field.name dump |"] |] $aCol(1) ]
				   #set sNumberActual [lindex [split [pQuery "" "print form \042$aCol(0)\042 select field.number dump |"] |] $iLindex]
				   set iLindex [ lsearch $lsFieldActual $aCol(1) ]
				   set sNumberActual [lindex $lsFieldNumber $iLindex ]
				}
				foreach iDat [list 2 3 4 5 6 10 11 12 13] sProperty [list label description expressiontype expression href alt range update number] {set aDat($iDat) ""}
				#set aDat(5) [pPrintQuery "" "field\134\133$aCol(1)\134\135.expression" "" ""]
				#set aDat(4) [string range $aDat(4) 0 2]
				set lsStgNameActual [list ]
				set lsStgValueActual [list ]
				set lsUserActual [list ]
				set lsPrint [split [pQuery "" "print form \042$aCol(0)\042"] \n]
				set bFoundField FALSE
				if {$sNumberActual != ""} {
					foreach sPrint $lsPrint {
					   set sPrint [string trim $sPrint]
					   if {[string first "field# $sNumberActual" $sPrint] == 0} {
							set sTemp ""
							set sTemp [string range $sPrint [expr [string first "select" $sPrint] +6] end]
							set aDat(5) [string trim $sTemp]
						   set bFoundField TRUE
						} elseif {$bFoundField} {
						if {[string first "expressiontype" $sPrint] == 0} {
							 regsub "expressiontype" $sPrint "" sPrint
							 set aDat(4) [string trim $sPrint]
							 set aDat(4) [string range $aDat(4) 0 2]
						  } elseif {[string first "label" $sPrint] == 0} {
							 regsub "label" $sPrint "" sPrint
							 set aDat(2) [string trim $sPrint]
						  } elseif {[string first "href" $sPrint] == 0} {
							 regsub "href" $sPrint "" sPrint
							 set aDat(6) [string trim $sPrint]
						  } elseif {[string first "alt" $sPrint] == 0} {
							 regsub "alt" $sPrint "" sPrint
							 set aDat(10) [string trim $sPrint]
						  } elseif {[string first "range" $sPrint] == 0} {
							 regsub "range" $sPrint "" sPrint
							 set aDat(11) [string trim $sPrint]
						  } elseif {[string first "update" $sPrint] == 0} {
							 regsub "update" $sPrint "" sPrint
							 set aDat(12) [string trim $sPrint]
						  } elseif {[string first "setting" $sPrint] == 0} {
							 regsub "setting" $sPrint "" sPrint
							 set sPrint [string trim $sPrint]
							 regsub " value " $sPrint "^" lsPrint
							 lappend lsStgNameActual [lindex [split $lsPrint ^] 0]
							 lappend lsStgValueActual [lindex [split $lsPrint ^] 1]
						  } elseif {[string first "user" $sPrint] == 0} {
							 regsub "user" $sPrint "" sPrint
							 lappend lsUserActual [string trim $sPrint]
						  } elseif {[string first "field" $sPrint] == 0} {
							 break
						  }
					   }
					}
				}
				
				set bReturn [pSetFormSetting $aSchemaElement($sSchemaType) $aCol(0) $aCol(7) $aCol(8) "$sSystem select $sColumnField\134\133$aCol(1)\134\135." setting]
				 if {$bReturn} {
					puts "\nError - Review log file '$sLogFileError', correct problem(s) and restart"
					return 1
				 }
				
				foreach sStgNameActual $lsStgNameActual sStgValueActual $lsStgValueActual {array set aStgActual [list $sStgNameActual $sStgValueActual]}
			   if {$aCol(5) != ""} { regsub -all "<PIPE:>" $aCol(5) "|" aCol(5) }
				if {$bOverlay} {
				   if {$aCol(5) == "" && $aDat(5) == "dummy"} {set aCol(5) "<NULL>"}
				   pOverlay [list 2 3 4 5 6 10 11 12]
				   set lsUserPlan [pOverlayList $lsUserPlan $lsUserActual]
				}
				if {$sPrevForm ==  ""} {set sPrevForm $aCol(0)}
				if {$sPrevForm ==  $aCol(0)} {
					if {[llength $lsPlanOrder] > 1 && $aCol(13) != "" } {
						lappend lsPlanOrder [list $aCol(1) $aCol(13)]
					} elseif {$aCol(13) != ""} { lappend lsPlanOrder [list $aCol(1) $aCol(13)] }
					
					if { [string length $sPlanUnordered] > 1 && $aCol(13) == "" } {
						append sPlanUnordered "|" $aCol(1)
					} elseif {$aCol(13) == ""} { set sPlanUnordered $aCol(1) }
				}
			 }
		 }
		 
         pSetAction "Modify $aSchemaElement($sSchemaType) $aCol(0) $sColumnField $aCol(1)"
      }
   }
   return 0
}

# Procedure to process web components
proc pProcessWebComp {} {
   global aCol aDat bOverlay bAdd bUserAll sHidden sHiddenActual sSchemaType lsSchemaType aSchemaElement lsCommandPlan lsCommandActual lsChannelPlan lsChannelActual lsUserPlan lsUserActual lsCmdMenuPlan lsCmdMenuActual aCmdMenuPlan aCmdMenuActual lsColumnNamePlan lsColumnNameActual lsTypePlan lsTypeActual sColumnField bEscQuote bScan sMxVersion sNumberActual lsStgNamePlan lsStgValuePlan lsStgNameActual lsStgValueActual aStgActual aStgPlan sSpinStamp lsPlanOrder sPlanUnordered sPrevForm sSystem sLastElement aHiddenIndex isFieldDuplicate
   # Spinner Path
   
   set fExt  ".inq"
   set sSpinnerPath [mql get env SPINNERPATH]
   set sInquiryFileDir "$sSpinnerPath/Business/SourceFiles/"
   set filePath ""
   append filePath $sInquiryFileDir $aCol(0) $fExt
   
   switch $sSchemaType {
      command - menu - channel - portal {
         if {$bAdd} {
            foreach iCol [list 3 4 5] {set aCol($iCol) [pRegSubEvalEscape $aCol($iCol)]}
            set sAppend ""
            if {$sSchemaType == "portal"} {set sAppend [pPortalChannel $lsChannelPlan]}
            if {$bEscQuote} {
               pMqlCmd "escape add $sSchemaType \042$aCol(0)\042 label '$aCol(3)' Alt '$aCol(5)' $sHidden $sAppend"
            } else {
               pMqlCmd "add $sSchemaType \042$aCol(0)\042 label \042$aCol(3)\042 Alt \042$aCol(5)\042 href \"$aCol(4)\" $sHidden $sAppend"
            }
            foreach sStgName $lsStgNamePlan sStgValue $lsStgValuePlan {
               set sStgValue [pRegSubEvalEscape $sStgValue]
			   #KYB Fixed SCR-0001804 Single quote issue for Bell
               #pMqlCmd "escape mod $sSchemaType \042$aCol(0)\042 add setting \042$sStgName\042 '$sStgValue'"
			   #pMqlCmd "escape mod $sSchemaType \042$aCol(0)\042 add setting \042$sStgName\042 \042$sStgValue\042"
			   #KYB Fixed SCR-0007292 - Spinner errors when trying to import commands with “hidden”==”true”
			   set checkIfDoubleQuote [string match "*\"*" $sStgValue]
			   if { $checkIfDoubleQuote == 1 } { 
               pMqlCmd "escape mod $sSchemaType \042$aCol(0)\042 add setting \042$sStgName\042 '$sStgValue'"
			   } else {
					pMqlCmd "escape mod $sSchemaType \042$aCol(0)\042 add setting \042$sStgName\042 \042$sStgValue\042"
			   }
            }
            switch $sSchemaType {
               command {
                  set bCommandFile [mql get env COMMMANDCODE]
				  	set filePath ""
					append filePath $sInquiryFileDir $aCol(0) ".json"
				   if { $bCommandFile == "TRUE" && [file exists $filePath] == 1 && $aCol(10) != "" } {
						set cmdCodeFile $aCol(0)
						append cmdCodeFile ".json"
						pMqlCmd "mod command \042$aCol(0)\042 file \042$sInquiryFileDir/$cmdCodeFile\042"  
					} elseif {$aCol(10) != ""} {
						pMqlCmd "mod command \042$aCol(0)\042 code {$aCol(10)}"
					}
                  if {$lsUserPlan != "" && [string tolower $lsUserPlan] != "all"} {
                     set lsUserActual "all"
                     pMqlCmd "add property UserAll on command \042$aCol(0)\042 value TRUE"
                     pPlanActualAddDel $lsUserActual "" $lsUserPlan command "" $aCol(0) "remove user" "add user" ""
                  }
               } menu {
                  pPlanAdd $lsCmdMenuPlan menu $aCol(0) "add" ""
               } channel {
                  pMqlCmd "mod channel \042$aCol(0)\042 height $aCol(9)"
                  pPlanAdd $lsCommandPlan channel $aCol(0) "place" "after \042\042"
               }
            }
         } else {
            switch $sSchemaType {
               command - menu - channel {
                  if {$sHidden != $sHiddenActual || $aCol(3) != $aDat(3) || $aCol(5) != $aDat(5) || $aCol(4) != $aDat(4)} {
                     foreach iCol [list 3 4 5] {set aCol($iCol) [pRegSubEvalEscape $aCol($iCol)]}
                     if {$bEscQuote} {
                        pMqlCmd "escape mod $sSchemaType \042$aCol(0)\042 label '$aCol(3)' Alt '$aCol(5)' $sHidden"
                        if {$aCol(4) == $aDat(4)} {set bEscQuote FALSE}
                     } else {
                        pMqlCmd "mod $sSchemaType \042$aCol(0)\042 label \042$aCol(3)\042 Alt \042$aCol(5)\042 href \"$aCol(4)\" $sHidden"
                        set bEscQuote FALSE
                     }
                  }
                  pSetting $sSchemaType $aCol(0) "" setting
                  switch $sSchemaType {
                     command {
                        if {$aCol(10) != $aDat(10)} {
                           #KYB Start
						   set bCommandFile [mql get env COMMMANDCODE]
						   set filePath ""
							append filePath $sInquiryFileDir $aCol(0) ".json"
							if { $bCommandFile == "TRUE" && [file exists $filePath] == 1 && $aCol(10) != "" } {
								set cmdCodeFile $aCol(0)
								append cmdCodeFile ".json"
								pMqlCmd "mod command \042$aCol(0)\042 file \042$sInquiryFileDir/$cmdCodeFile\042"  
						   } else {
                           pMqlCmd "mod command \042$aCol(0)\042 code {$aCol(10)}"
                        }
						   #KYB End
						   #pMqlCmd "mod command \042$aCol(0)\042 code {$aCol(10)}"
                        }
                        if {($lsUserPlan == "" || [string tolower $lsUserPlan] == "all") && $bUserAll != ""} {
                           pMqlCmd "delete property UserAll on command \042$aCol(0)\042"
                        } elseif {$lsUserPlan != "" && $bUserAll == ""} {
                           set lsUserActual "all"
                           pMqlCmd "add property UserAll on command \042$aCol(0)\042 value TRUE"
                        }
                        pPlanActualAddDel $lsUserActual "" $lsUserPlan command "" $aCol(0) "remove user" "add user" ""
                     } menu {
                        pPlanActualAddDel $lsCmdMenuActual "" $lsCmdMenuPlan menu "" $aCol(0) "remove" "add" ""
                        if {$aCol(0) != "Tree" && $lsCmdMenuPlan != $lsCmdMenuActual} {
                           set iIndex 1
                           foreach sCmdMenuPlan $lsCmdMenuPlan {
                              pMqlCmd "mod menu \042$aCol(0)\042 order $aCmdMenuPlan($sCmdMenuPlan) \042$sCmdMenuPlan\042 $iIndex"
                              incr iIndex
                           }
                        }
                     } channel {
                        if {$aCol(9) != $aDat(9)} {pMqlCmd "mod channel \042$aCol(0)\042 height $aCol(9)"}
                        if {$lsCommandPlan != $lsCommandActual} {
                           pPlanAdd $lsCommandActual channel $aCol(0) "remove command" ""
                           pPlanAdd $lsCommandPlan channel $aCol(0) "place" "after \042\042"
                        }
                     }
                  }
               } portal {
                  if {$lsChannelPlan != $lsChannelActual} {
					set lsFinalPlan ""
					set sAppendChannelRemove " remove channel"
					
					foreach sChannelPlan $lsChannelPlan {
						set lsChannelRow [split $sChannelPlan ","]
						set iCtrTemp 0
						foreach sChannel $lsChannelRow {
							if {[string first "<<" $sChannel] == -1} {
								if {$iCtrTemp > 0} {
									append lsFinalPlan " place " $sChannel " after \042\042"
								} else {
									append lsFinalPlan " place " $sChannel " newrow after \042\042"
								}
								incr iCtrTemp
							}
						}
					}
					
					if { $lsChannelActual!= ""} {
					foreach sChannelActual $lsChannelActual {
						set lsChannelAct [split $sChannelActual ","]
						foreach sChannelAct $lsChannelAct {
							append sAppendChannelRemove " " $sChannelAct
						}
					}
					} else {
					set sAppendChannelRemove ""
					}
					if { [string length $sAppendChannelRemove] > 0} {
						pMqlCmd "escape mod portal \042$aCol(0)\042$sAppendChannelRemove"
					}
					if { [string length $lsFinalPlan] > 0} {
						pMqlCmd "escape mod portal \042$aCol(0)\042$lsFinalPlan"
					}
					
                    #set sAppend [pPortalChannel $lsChannelPlan]
                  } 
				  if {$sHidden != $sHiddenActual || $aCol(3) != $aDat(3) || $aCol(5) != $aDat(5) || $aCol(4) != $aDat(4)} {
                     foreach iCol [list 3 4 5] {set aCol($iCol) [pRegSubEvalEscape $aCol($iCol)]}
                     pMqlCmd "escape mod portal \042$aCol(0)\042 label '$aCol(3)' Alt '$aCol(5)' href '$aCol(4)' $sHidden"
                  }
                  pSetting portal $aCol(0) "" setting
               }
            }
         }
         if {$bEscQuote && $bScan != "TRUE"} {
            pAppend "mql mod $sSchemaType $aCol(0) href \"$aCol(4)\"" FALSE
            mql mod $sSchemaType $aCol(0) href "$aCol(4)"
         }
      } inquiry {
         if {$bAdd} {
            foreach iCol [list 3 4] {set aCol($iCol) [pRegSubEvalEscape $aCol($iCol)]}
            pMqlCmd "escape add inquiry \042$aCol(0)\042 pattern '$aCol(3)' format '$aCol(4)' $sHidden"
			
            pMqlCmd "mod inquiry \042$aCol(0)\042 file $filePath"
            if {$bScan != "TRUE"} {mql mod inquiry $aCol(0) file $filePath}
            foreach sStgName $lsStgNamePlan sStgValue $lsStgValuePlan {
               set sStgValue [pRegSubEvalEscape $sStgValue]
               pMqlCmd "escape mod inquiry \042$aCol(0)\042 add argument \042$sStgName\042 '$sStgValue'"
            }
         } else {
            if {$sHidden != $sHiddenActual || $aCol(3) != $aDat(3) || $aCol(4) != $aDat(4) || $aCol(7) != $aDat(7)} {
               foreach iCol [list 3 4] {set aCol($iCol) [pRegSubEvalEscape $aCol($iCol)]}
               pMqlCmd "escape mod inquiry \042$aCol(0)\042 pattern '$aCol(3)' format '$aCol(4)' $sHidden"
            }
			if {$aCol(7) != $aDat(7)} {
				if {$aCol(7) == ""} {
					pMqlCmd "mod inquiry \042$aCol(0)\042 code \042$aCol(7)\042"
				} else {
					pMqlCmd "mod inquiry \042$aCol(0)\042 file $filePath"
				}
               set bUpdate TRUE
			  }
            pSetting inquiry $aCol(0) "" argument
         }
      } table {
         if {$bAdd} {
            pMqlCmd "add table \042$aCol(0)\042 system $sHidden"
         } else {
            if {$sHidden != $sHiddenActual} {pMqlCmd "escape mod table \042$aCol(0)\042 system $sHidden"}
         }
      } column {
			#KYB Start
			
			set bDeleteFlag "FALSE" 
			
			if {$bOverlay} { 
				set iLenMinTwo [expr [string length $aCol(1)] -2]
				if {[string first "<<" $aCol(1)] >= 0 && [string first ">>" $aCol(1)] == $iLenMinTwo} {
					set bDeleteFlag "TRUE"
				}
			} else {
				set bDeleteFlag "TRUE" 
				for {set i 2} {$i < [expr $aHiddenIndex($sSchemaType)]} {incr i} {
					if {$aCol($i) != ""} {
						set bDeleteFlag FALSE
						break
					}
				}
			}
			
			pPlanActualAddDel $lsColumnNameActual "" $lsColumnNamePlan "table" "" $aCol(0) "column delete name" "column name" ""
			
			if {$bDeleteFlag == "FALSE"} {
			
				if {$sPrevForm != $aCol(0) || $sLastElement == "TRUE"} {
					set sSingleLast FALSE
					if {$sPrevForm != $aCol(0) && $sLastElement == "TRUE"} {
						set sSingleLast TRUE
					}
					set sSchemaElement $sPrevForm
					set sPrevForm $aCol(0)
					
					set lsFinalPlanOrder [list ]
					set lsPlanOrder [lsort -integer -index 1 $lsPlanOrder]
					foreach lsPOrder $lsPlanOrder {
						foreach sTemp $lsPOrder {
							lappend lsFinalPlanOrder $sTemp
							break;
						}
					}
					
					if {[string length $sPlanUnordered] > 1} { lappend lsFinalPlanOrder [split $sPlanUnordered "|"] }
					set sQuery ""
					set lsFieldActual [split [pQuery "" "print $aSchemaElement($sSchemaType) \042$sSchemaElement\042 $sSystem select column.name dump |"] |]
					
					if {$lsFieldActual != $lsFinalPlanOrder} {
						foreach sColumnNameP $lsFinalPlanOrder {
							set lsFieldActual [split [pQuery "" "print $aSchemaElement($sSchemaType) \042$sSchemaElement\042 $sSystem select column.name dump |"] |]
							set sActulOrder [expr [lsearch $lsFieldActual $sColumnNameP] +1]
							set sPlanOrder [lindex [lindex $lsPlanOrder [lsearch $lsFinalPlanOrder $sColumnNameP] ] 1 ]
							#if { $sActulOrder != $sPlanOrder && $sPlanOrder != "" } {
							
								if {$sQuery == ""} {
									set sQuery "mod table \042$sSchemaElement\042 system"
								}
								append sQuery " column mod name \042$sColumnNameP\042 order $sPlanOrder"
							#}
						}
					  
						if {$sQuery != ""} {
							pMqlCmd $sQuery
						}
					}
					 
					 if {$sSingleLast == "TRUE"} {
						set sFieldOrder [pQuery "" "print table \042$aCol(0)\042 $sSystem select column\134\133$aCol(1)\134\135.number dump"]
						if {$sFieldOrder != $aCol(14)} {
							set stempQuery "mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 order $aCol(14)"
							pMqlCmd $stempQuery
						}
					 }
					set lsPlanOrder [list ]
					if {$aCol(14) != ""} {
						lappend lsPlanOrder [list $aCol(1) $aCol(14)]
					} elseif {$aCol(14) == ""} {
						set sPlanUnordered $aCol(1)
					}
				}
			if {$aCol(1) != ""} {
				#if {$sHidden != $sHiddenActual} {pMqlCmd "escape mod table \042$aCol(0)\042 system $sHidden"}
				set lsColumnNameActual [pPrintQuery "" "column.name" | spl]
				#KYB End
				
				set sCommand "mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042"
				set sAppendCommand ""

				 if {$sHidden != $sHiddenActual || $aCol(2) != $aDat(2)} {
					set aCol(2) [pRegSubEvalEscape $aCol(2)]
					#pMqlCmd "mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 label \042$aCol(2)\042 $sHidden"
					append sAppendCommand " label \042$aCol(2)\042 $sHidden"
				 }
				 				 
				 if {$aCol(4) != $aDat(4) || $aCol(5) != $aDat(5)} {
					set aCol(5) [pRegSubEvalEscape $aCol(5)]
					if {$aCol(4) == "bus" && $aCol(5) == ""} {set aCol(5) "dummy"}
				#Modified below for 359584 to handle single quote issue - start
					#pMqlCmd "escape mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 $aCol(4) \134$aCol(5)\134"
					append sAppendCommand " $aCol(4)  \042$aCol(5)\042"
				#Modified below for 359584 to handle single quote issue - end
				 }
				 
				 if {$aCol(3) != $aDat(3)} {
					#pMqlCmd "mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 description \042$aCol(3)\042"
					append sAppendCommand " description \042$aCol(3)\042"
				 }
				 
				 if {$sAppendCommand != ""} {
					append sCommand $sAppendCommand
					pMqlCmd $sCommand
				}
				set sCommand "mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042"
				set sAppendCommand ""
				
				 if {$aCol(6) != $aDat(6)} {
					set aCol(6) [pRegSubEvalEscape $aCol(6)]
					#pMqlCmd "mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 href '$aCol(6)'"
					append sAppendCommand " href '$aCol(6)'"
				 }

				 pSetting table $aCol(0) "system column mod name \134\042$aCol(1)\134\042" setting
				 if {$sMxVersion > 9.5} {
					pPlanActualAddDel $lsUserActual "" $lsUserPlan table "" $aCol(0) "column mod name \042$aCol(1)\042 remove user" "column mod name \042$aCol(1)\042 add user" ""
					foreach sFld [list 10 11 12 13] sLink [list alt range update sorttype] {
					   if {$aCol($sFld) != $aDat($sFld)} {
						  set aCol($sFld) [pRegSubEvalEscape $aCol($sFld)]
						  if {[string first "\047" $aCol($sFld)] < 0 && [string first "javascript" [string tolower $aCol($sFld)]] < 0} {
							 #pMqlCmd "escape mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 $sLink '$aCol($sFld)'"
							 append sAppendCommand " $sLink '$aCol($sFld)'"
						  } else {
							 #pMqlCmd "escape mod table \042$aCol(0)\042 system column mod name \042$aCol(1)\042 $sLink \042$aCol($sFld)\042"
							 append sAppendCommand " $sLink \042$aCol($sFld)\042"
						  }
					   }
					}
				 } else {
					pPlanAdd "all" table $aCol(0) "column mod name \042$aCol(1)\042 remove user" ""
					pPlanAdd $lsUserPlan table $aCol(0) "column mod name \042$aCol(1)\042 add user" ""
				 }
				 
				 if {$sAppendCommand != ""} {
					append sCommand $sAppendCommand
					pMqlCmd $sCommand
				}
			}
		 }
      } webform {
         if {$bAdd} {
            pMqlCmd "add form \042$aCol(0)\042 web $sHidden"
            pPlanAdd $lsTypePlan form $aCol(0) "type" ""
         } else {
            if {$sHidden != $sHiddenActual} {pMqlCmd "mod form \042$aCol(0)\042 $sHidden"}
            pPlanActualAddDel $lsTypeActual "" $lsTypePlan form "" $aCol(0) "type delete" "type" ""
         }
      } field {
			
			set bDeleteFlag "FALSE"
			
			if {$bOverlay} {
				set iLenMinTwo [expr [string length $aCol(1)] -2]
				if {[string first "<<" $aCol(1)] >= 0 && [string first ">>" $aCol(1)] == $iLenMinTwo} {
					set bDeleteFlag "TRUE"
				}
			} else {
				set bDeleteFlag "TRUE"
				for {set i 2} {$i < [expr $aHiddenIndex($sSchemaType)]} {incr i} {
					if {$aCol($i) != ""} {
						set bDeleteFlag FALSE
						break
					}
				}
			}

			  #KYB Start
			if {$lsColumnNamePlan != $lsColumnNameActual} {
				pPlanActualAddDel $lsColumnNameActual "" $lsColumnNamePlan form "" $aCol(0) "field delete name" "field name" ""
		  	}			
			#KYB End
	  
			if {$bDeleteFlag == "FALSE"} {
			
				if {$sPrevForm != $aCol(0) || $sLastElement == "TRUE"} {
					set sSingleLast FALSE
					if {$sPrevForm != $aCol(0) && $sLastElement == "TRUE"} {
						set sSingleLast TRUE
					}
					
					set sSchemaElement $sPrevForm
					set sPrevForm $aCol(0)
					
					set lsFinalPlanOrder [list ]
					set lsPlanOrder [lsort -integer -index 1 $lsPlanOrder]
					foreach lsPOrder $lsPlanOrder {
						foreach sTemp $lsPOrder {
							lappend lsFinalPlanOrder $sTemp
							break;
						}
					}
					
					if {[string length $sPlanUnordered] > 1} { lappend lsFinalPlanOrder [split $sPlanUnordered "|"] }
					
					set lsFieldActual [split [pQuery "" "print $aSchemaElement($sSchemaType) \042$sSchemaElement\042 $sSystem select field.name dump |"] |]
					set sQuery ""
					
					 if {$lsFieldActual != $lsFinalPlanOrder} {
						 foreach sColumnNameP $lsFinalPlanOrder {
						 set lsFieldActual [split [pQuery "" "print $aSchemaElement($sSchemaType) \042$sSchemaElement\042 $sSystem select field.name dump |"] |]
							set sActulOrder [expr [lsearch $lsFieldActual $sColumnNameP] +1]
							set sPlanOrder [lindex [lindex $lsPlanOrder [lsearch $lsFinalPlanOrder $sColumnNameP] ] 1 ]
							if { $sActulOrder != $sPlanOrder && $sPlanOrder != "" } {
								if {$sQuery == ""} {
									set sQuery "mod form \042$sSchemaElement\042"
								}
								append sQuery " field mod name \042$sColumnNameP\042 order $sPlanOrder"
							}
						 }
						 if {$sQuery != ""} {
						 	pMqlCmd $sQuery
						 }
					  }
					  
					  if {$sSingleLast == "TRUE"} {
						set sFieldOrder [pQuery "" "print $aSchemaElement($sSchemaType) \042$aCol(0)\042 $sSystem select field\134\133$aCol(1)\134\135.number dump" ]
						if {$sFieldOrder != $aCol(13)} {
							set stempQuery "mod form \042$aCol(0)\042 field mod name \042$aCol(1)\042 order $aCol(13)"
							pMqlCmd $stempQuery
						}
					 }
					  
					set lsPlanOrder [list ]
					if {$aCol(13) != ""} {
						lappend lsPlanOrder [list $aCol(1) $aCol(13)]
					} elseif {$aCol(13) == ""} {
						set sPlanUnordered $aCol(1)
					}
				}
				if {$isFieldDuplicate == "TRUE"} {
					set sModifier "$sNumberActual name \042$aCol(1)\042"
				} else {
					set sModifier "name \042$aCol(1)\042"
				}
				
				set sCommand "escape mod form \042$aCol(0)\042 field mod $sModifier"
				set sAppendCommand ""
				if {$sHidden != $sHiddenActual || $aCol(4) != $aDat(4) || $aCol(5) != $aDat(5)} {
					set aCol(5) [pRegSubEvalEscape $aCol(5)]
					 ######### START Added by SL Team for spinner V6R2012x validation ##########
					if {[regexp -all \134\134\134\134\134\042 $aCol(5)] } {
						regsub -all "\134\134\134\042" $aCol(5) "\136" aCol(5)
					}
					if {[regexp -all \136 $aCol(5) match5]} {
						regsub -all "\134\136" $aCol(5) "\042" aCol(5)
					} 
					if {[regexp -all \134\134\134\042 $aCol(5)]} {
						regsub -all "\134\134\134\042" $aCol(5) "\134\134\134\134\134\042" aCol(5)
					}
					 ########### END ##############
					#Modified below for 359584 to handle single quote issue - start
					append sAppendCommand " $aCol(4) \042$aCol(5)\042 $sHidden"
					# "escape mod form \042$aCol(0)\042 field mod $sModifier $aCol(4) \042$aCol(5)\042 $sHidden"
					#Modified below for 359584 to handle single quote issue - end
				}
			 
				if {$aCol(2) != $aDat(2)} {append sAppendCommand " label \042$aCol(2)\042"}
				foreach sFld [list 6 10 11 12] sLink [list href alt range update] {
					if {$aCol($sFld) != $aDat($sFld)} {
					   set aCol($sFld) [pRegSubEvalEscape $aCol($sFld)]
					   if {[string first "\047" $aCol($sFld)] < 0 && [string first "javascript" [string tolower $aCol($sFld)]] < 0} {
						  #pMqlCmd "escape mod form \042$aCol(0)\042 field mod $sModifier $sLink '$aCol($sFld)'"
						  append sAppendCommand " $sLink '$aCol($sFld)'"
					   } else {
						  #pMqlCmd "escape mod form \042$aCol(0)\042 field mod $sModifier $sLink \042$aCol($sFld)\042"
						  append sAppendCommand " $sLink \042$aCol($sFld)\042"
					   }
					}
				}
			if {$sAppendCommand != ""} {
				append sCommand $sAppendCommand
				pMqlCmd $sCommand
			}
			 pSetting form $aCol(0) "field mod $sModifier" setting
			 pPlanActualAddDel $lsUserActual "" $lsUserPlan form "" $aCol(0) "field mod $sModifier remove user" "field mod $sModifier add user" ""
			 #KYB Added
			 #pMqlCmd "mod form \042$aCol(0)\042 field mod $sModifier order \042$aCol(13)\042"
		 }
      }
   }
   return 0
}

