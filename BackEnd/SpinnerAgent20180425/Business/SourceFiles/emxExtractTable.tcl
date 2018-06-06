tcl;

eval {
   if {[info host] == "sn732plp" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
   	  set cmd "debugger_eval"
   	  set xxx [debugger_init]
   } else {
   	  set cmd "eval"
   }
}

$cmd {
#  Set up array for symbolic name mapping
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "table "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "table"} {
         regsub $sTypeReplace $sPropertyTo "" sPropertyTo
         regsub "_" $sPropertyName "|" sSymbolicName
         set sSymbolicName [lindex [split $sSymbolicName |] 1]
         array set aSymbolic [list $sPropertyTo $sSymbolicName]
      }
   }

   set sFilter [mql get env 1]
   set bTemplate [mql get env 2]
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

   set sAppend ""
   if {$sFilter != ""} {
      regsub -all "\134\052" $sFilter "ALL" sAppend
      regsub -all "\134\174" $sAppend "-" sAppend
      regsub -all "/" $sAppend "-" sAppend
      regsub -all ":" $sAppend "-" sAppend
      regsub -all "<" $sAppend "-" sAppend
      regsub -all ">" $sAppend "-" sAppend
      regsub -all " " $sAppend "" sAppend
      set sAppend "_$sAppend"
   }

   if {$sGreaterThanEqualDate != ""} {
      set sModDateMin [clock scan $sGreaterThanEqualDate]
   } else {
      set sModDateMin ""
   }
   if {$sLessThanEqualDate != ""} {
      set sModDateMax [clock scan $sLessThanEqualDate]
   } else {
      set sModDateMax ""
   }
   
   set sSpinnerPath [mql get env SPINNERPATH]
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
      }
      file mkdir $sSpinnerPath
   }

   #KYB - Fixed SCR-0002238 - Modified to add filter for table extraction
   #set sPath "$sSpinnerPath/Business/SpinnerTableData\_ALL.xls"
   set sPath "$sSpinnerPath/Business/SpinnerTableData$sAppend.xls"   
   #set lsTable [split [mql list table system] \n]
   set lsTable [split [mql list table system "$sFilter"] \n]
   set sFile "Name\tRegistry Name\tDescription\tHidden (boolean)\n"
   # START--- Added by Solution Library Team for Incidents SR00040360 and SR00039673
   #set sPath2 "$sSpinnerPath/Business/SpinnerTableColumnData\_ALL.xls"
   set sPath2 "$sSpinnerPath/Business/SpinnerTableColumnData$sAppend.xls"
    # END-----      
   set sFile2 "Table Name\tColumn Name\tColumn Label\tCol Description\tExpression Type (bus or \"\" / rel)\tExpression\tHref\tSetting Names (use \"|\" delim)\tSetting Values (use \"|\" delim)\tUsers (use \"|\" delim)\tAlt\tRange\tUpdate\tSortType (alpha / numeric / other / none or \"\")\tColumn Order\tHidden\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   proc generateSettingsList {sTableName} {
		global sSettingFinalNames sSettingFinalValues
		set slsSettings [mql print table $sTableName system select column.setting.value ]
		set slSettings [split $slsSettings \n]
		set index 0
	   
		set sSettingNames ""
		set sSettingValues ""
		set sPrevField ""
		set sSettingFinalNames ""
		set sSettingFinalValues ""
		foreach sSetting $slSettings {
			if {$index > 0} {
				set sSetting [string trim $sSetting]
				set sFirst [string first "=" $sSetting]
				set sTempName [string trim [string range $sSetting 0 [expr $sFirst -1]]]
				set sTempValue [string trim [string range $sSetting [expr $sFirst +1 ] [string length $sSetting]]]
				set sTempInd1 [string first ".setting\[" $sTempName]
				set sTempInd2 [string first "\].value" $sTempName]
				set sFieldN [string trim [string range $sTempName 0 [expr $sTempInd1 -1]]]
				set sName [string trim [string range $sTempName [expr $sTempInd1 +9] [expr $sTempInd2-1]]]
				
				if {$sPrevField == ""} {set sPrevField $sFieldN}
				if {$sPrevField != $sFieldN || ($sFieldN == "field" && [string first $sName $sSettingNames] >=0)} { 
					lappend sSettingFinalNames $sSettingNames 
					lappend sSettingFinalValues $sSettingValues
					set sPrevField $sFieldN
					set sSettingNames ""
					set sSettingValues ""						
				}
				
				if {$sSettingNames == ""} {
					set sSettingNames $sName
				} else {
					append sSettingNames "|" $sName
				}
				
				if {$sSettingValues == ""} {
					set sSettingValues $sTempValue
				} else {
					append sSettingValues "|" $sTempValue
				}
			
				if {[llength $slSettings] == [ expr $index +1]} {
					lappend sSettingFinalNames $sSettingNames 
					lappend sSettingFinalValues $sSettingValues
				}
			}
			incr index
	   }
	}
   
   if {!$bTemplate} {
      foreach sTable $lsTable {
         set bPass TRUE
         set sModDate [mql print table $sTable system select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print table $sTable system select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print table $sTable system select name dump]
			
            for {set i 0} {$i < [string length $sName]} {incr i} {
               if {[string range $sName $i $i] == " "} {
                  regsub " " $sName " " sName
				  
               } else {
                  break
               }
            }
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sTable)} sMsg
            regsub -all " " $sTable "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sTable
            }
            set sDescription [mql print table $sTable system select description dump]
            set sHidden [mql print table $sTable system select hidden dump]

            append sFile "$sName\t$sOrigName\t$sDescription\t$sHidden\n"
			
# Table Column
            set lsColumn [split [mql print table $sTable system select column dump |] |]
		    set colvalue " "
			set count 0;
			set sSettingFinalNames ""
			set sSettingFinalValues ""
            foreach sColumn $lsColumn {
				   incr count
				   set sName $sColumn
				  
				   for {set i 0} {$i < [string length $sName]} {incr i} {				   
					  if {[string range $sName $i $i] == " "} {
						 regsub " " $sName "<SPACE>" sName
					  } else {
						 break
					  }			  			
				   }
										  
				   set sLabel [mql print table $sTable system select column\[$sColumn\].label dump]
				   set sDescription [mql print table $sTable system select column\[$sColumn\].description dump]
				   set sExpressionType [mql print table $sTable system select column\[$sColumn\].expressiontype dump]
				   set sExpression [mql print table $sTable system select column\[$sColumn\].expression dump]
				   regsub -all "\134|" $sExpression "<PIPE:>" sExpression
				   regsub -all "<PIPE:><PIPE:>" $sExpression "||" sExpression
				   set sHref [mql print table $sTable system select column\[$sColumn\].href dump]
				   set sAlt [mql print table $sTable system select column\[$sColumn\].alt dump]
				   set sRange [mql print table $sTable system select column\[$sColumn\].range dump]
				   set sUpdate [mql print table $sTable system select column\[$sColumn\].update dump]				   
				   set slsSettingName [mql print table $sTable system select column\[$sColumn\].setting dump " | "]
				   #set slsSettingValue [mql print table $sTable system select column\[$sColumn\].setting.value dump " | "]
				   #AFR2 Start Modified to fetch number and add it in column order column
				   set ColumnOrder  [mql print table $sTable system select column\[$sColumn\].number dump]	
				   #KYB - Traverse thr each setting of a column, look for char '|' 
				   set slsSettingValue ""
				   set iCnt2 0
				   set sListColSettingName [split $slsSettingName |]   
				   set iCnt1 [ llength $sListColSettingName ]
				   foreach sColSettingName $sListColSettingName {
						set sColSettingName [string trim $sColSettingName]
						set slsColSettingValue [ mql print table $sTable system select column\[$sColumn\].setting\[$sColSettingName\].value dump ]
						regsub -all "\134|" $slsColSettingValue "<PIPE:>" slsColSettingValue
						regsub -all "<PIPE:><PIPE:>" $slsColSettingValue "||" slsColSettingValue						
						append slsSettingValue $slsColSettingValue
						incr iCnt2
						if { $iCnt2 != $iCnt1 } { append slsSettingValue " | " }
				   }
				   
				   if {$sMxVersion >= 9.6} {
					  set slsUser [mql print table $sTable system select column\[$sColumn\].user dump " | "]
					  set sAlt [mql print table $sTable system select column\[$sColumn\].alt dump]
					  set sRange [mql print table $sTable system select column\[$sColumn\].range dump]
					  set sUpdate [mql print table $sTable system select column\[$sColumn\].update dump]
					  set sSortType "none"
					  set lsPrint [split [mql print table $sTable system] \n]
					  set bTrip "FALSE"
					  foreach sPrint $lsPrint {
						 set sPrint [string trim $sPrint]
						 if {[string range $sPrint 0 3] == "name" && [string first $sColumn $sPrint] > 3} {
							set bTrip TRUE
						 } elseif {$bTrip && [string range $sPrint 0 3] == "name"} {
							break
						 } elseif {$bTrip} {
							if {[string range $sPrint 0 7] == "sorttype"} {
							   regsub "sorttype" $sPrint "" sPrint
							   set sSortType [string trim $sPrint]
							   break
							}
						 } 
					  }
					  set sHidden [mql print table $sTable system select column\[$sColumn\].hidden dump]
				   } else {
					  set slsUser ""
					  set sAlt ""
					  set sRange ""
					  set sUpdate ""
					  set sSortType ""
					  set sHidden ""
				   }
				   set sTableName $sTable
				   for {set i 0} {$i < [string length $sTableName]} {incr i} {
					  if {[string range $sTableName $i $i] == " "} {
						 regsub " " $sTableName " " sTableName
					  } else {
						 break
					  }
				   }
				   
				   #KYB Start V6R2013x Fixed issue of blank column name, Temporary fix because of OOTB issue
				   if { $sColumn == "" } {
					   set lsColumnLabel [split $sLabel ,]
					   set lsColumnDescription [split $sDescription ,]
					   set lsColumnExpressionType [split $sExpressionType ,]
					   set lsColumnExpression [split $sExpression ,]
					   set lsColumnHref [split $sHref ,]
					   set lsColumnAlt [split $sAlt ,]
					   set lsColumnRange [split $sRange ,]
					   set lsColumnUpdate [split $sUpdate ,]
					   set lsColumnHidden [split $sHidden ,]
					   set lsColumnOrder [split $ColumnOrder ,]
					   #KYB Start Fixed incorrect extraction of Setting name and Setting value for a column
					   set lsColumnSettingName [split $slsSettingName |]
					   set lsColumnSettingValue [split $slsSettingValue |]
					   #KYB End
					   if {$sSettingFinalNames == "" && $sSettingFinalValues == ""} { generateSettingsList $sTable }
						
					   set iCount 0   
					   if {$sMxVersion >= 9.6} {
							set lsColumnSlsUser [split $slsUser |]
								foreach sColumnSlsUser $lsColumnSlsUser {
									incr iCount
									if { $count == $iCount } {
										set slsUser $sColumnSlsUser
									}
							}
						}
					   
						set sLabel [lindex $lsColumnLabel [expr $count -1]]
						set sDescription [lindex $lsColumnDescription [expr $count -1]]
						set sExpressionType [lindex $lsColumnExpressionType [expr $count -1]]
						set sExpression [lindex $lsColumnExpression [expr $count -1]]
						set sHref [lindex $lsColumnHref [expr $count -1]]
						set sAlt [lindex $lsColumnAlt [expr $count -1]]
						set sRange [lindex $lsColumnRange [expr $count -1]]
						set sUpdate [lindex $lsColumnUpdate [expr $count -1]]
						set sHidden [lindex $lsColumnHidden [expr $count -1]]
						set ColumnOrder [lindex $lsColumnOrder [expr $count -1]]
						set slsSettingName [lindex $sSettingFinalNames [expr $count -1]]
						set slsSettingValue [lindex $sSettingFinalValues [expr $count -1]]

				   }			   	   
				   #KYB End V6R2013x Fixed issue of blank column name, Temporary fix because of OOTB issue
				   
               append sFile2 "$sTableName\t$sName\t$sLabel\t$sDescription\t$sExpressionType\t$sExpression\t$sHref\t$slsSettingName\t$slsSettingValue\t$slsUser\t$sAlt\t$sRange\t$sUpdate\t$sSortType\t$ColumnOrder\t$sHidden\n"
   		       #AFR2 End Modified to fetch number and add it in column order column
			}
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Table data loaded in file $sPath"
   set iFile [open $sPath2 w]
   puts $iFile $sFile2
   close $iFile   
   puts "Table Column data loaded in file $sPath2"
}
