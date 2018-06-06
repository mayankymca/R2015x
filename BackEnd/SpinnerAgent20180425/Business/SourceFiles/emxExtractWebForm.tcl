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
   set sTypeReplace "form "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "form"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerWebFormData$sAppend.xls"
   set lsWebForm [split [mql list form $sFilter] \n]
   set sFile "Name\tRegistry Name\tDescription\tHidden (boolean)\tTypes (use \"|\" delim)\n"
   set sPath2 "$sSpinnerPath/Business/SpinnerWebFormFieldData$sAppend.xls"
   set sFile2 "WebForm Name\tField Name\tField Label\tField Description\tExpression Type (bus or \"\" / rel)\tExpression\tHref\tSetting Names (use \"|\" delim)\tSetting Values (use \"|\" delim)\tUsers (use \"|\" delim)\tAlt\tRange\tUpdate\tField Order\tHidden\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
	proc generateSettingsList {sWebFormName} {
		global sSettingFinalNames sSettingFinalValues
		set slsSettings [mql print form $sWebFormName select field.setting.value ]
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
      foreach sWebForm $lsWebForm {
         if {[mql print form $sWebForm select web dump] == "TRUE"} {
# Web Form
            set bPass TRUE
            set sModDate [mql print form $sWebForm select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
            
            if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print form $sWebForm select property\[SpinnerAgent\] dump] != "")} {
               set sName [mql print form $sWebForm select name dump]
			   set sFormName $sName
			   
               for {set i 0} {$i < [string length $sName]} {incr i} {
                  if {[string range $sName $i $i] == " "} {
                     regsub " " $sName "<SPACE>" sName
                  } else {
                     break
                  }
               }
               set sOrigName ""
               catch {set sOrigName $aSymbolic($sWebForm)} sMsg
               regsub -all " " $sWebForm "" sOrigNameTest
               if {$sOrigNameTest == $sOrigName} {
                  set sOrigName $sWebForm
               }
               set sDescription [mql print form $sWebForm select description dump]
               set slsType [mql print form $sWebForm select type dump " | "]
               set sHidden [mql print form $sWebForm select hidden dump]

               append sFile "$sName\t$sOrigName\t$sDescription\t$sHidden\t$slsType\n"
# Web Form Field
			   set lsField [split [mql print form $sWebForm select field dump |] |]
               set bField FALSE
               set iCounter 1
			   set count 0
			   set sSettingFinalNames ""
			   set sSettingFinalValues ""
               foreach sField $lsField {
				incr count
                  set sField [string trim $sField]
                                 set sName $sField
				  set iFieldOrder $iCounter
				  incr iCounter
				  
				  for {set i 0} {$i < [string length $sName]} {incr i} {				   
					  if {[string range $sName $i $i] == " "} {
						 regsub " " $sName "<SPACE>" sName
					  } else {
						 break
					  }			  			
				   }
				   
				   set sLabel [mql print form $sWebForm select field\[$sField\].label dump]
				   set sDescription [mql print form $sWebForm select field\[$sField\].description dump]
				   set sExpressionType [mql print form $sWebForm select field\[$sField\].expressiontype dump]
				   set sExpression [mql print form $sWebForm select field\[$sField\].expression dump]
				   regsub -all "\134|" $sExpression "<PIPE:>" sExpression
				   regsub -all "<PIPE:><PIPE:>" $sExpression "||" sExpression
				   set sHref [mql print form $sWebForm select field\[$sField\].href dump]
				   set sAlt [mql print form $sWebForm select field\[$sField\].alt dump]
				   set sRange [mql print form $sWebForm select field\[$sField\].range dump]
				   set sUpdate [mql print form $sWebForm select field\[$sField\].update dump]
				   set slsSettingName [mql print form $sWebForm select field\[$sField\].setting dump " | "]
				   #KYB - Traverse thr each setting of a field, look for char '|' 
				   set slsSettingValue ""
				   set iCnt2 0
				   set sListFieldSettingName [split $slsSettingName |]   
				   set iCnt1 [ llength $sListFieldSettingName ]
				   foreach sFieldSettingName $sListFieldSettingName {
						set sFieldSettingName [string trim $sFieldSettingName]
						set slsFieldSettingValue [ mql print form $sWebForm select field\[$sField\].setting\[$sFieldSettingName\].value dump ]
						regsub -all "\134|" $slsFieldSettingValue "<PIPE:>" slsFieldSettingValue
						regsub -all "<PIPE:><PIPE:>" $slsFieldSettingValue "||" slsFieldSettingValue						
						append slsSettingValue $slsFieldSettingValue
						incr iCnt2
						if { $iCnt2 != $iCnt1 } { append slsSettingValue " | " }
				   }
				   
				   set slsUser [mql print form $sWebForm select field\[$sField\].user dump " | "]
				   set bHidden [mql print form $sWebForm select field\[$sField\].hidden dump]
				   
				   if { $sField == "" } {
					   set lsFieldLabel [split $sLabel ,]
					   set lsFieldDescription [split $sDescription ,]
					   set lsFieldExpressionType [split $sExpressionType ,]
					   set lsFieldExpression [split $sExpression ,]
					   set lsFieldHref [split $sHref ,]
					   set lsFieldAlt [split $sAlt ,]
					   set lsFieldRange [split $sRange ,]
					   set lsFieldUpdate [split $sUpdate ,]
					   set lsFieldHidden [split $bHidden ,]
					   #KYB Start Fixed incorrect extraction of Setting name and Setting value for a column
					   set lsFieldSettingName [split $slsSettingName |]
					   set lsFieldSettingValue [split $slsSettingValue |]
					   #KYB End
					   if {$sSettingFinalNames == "" && $sSettingFinalValues == ""} { generateSettingsList $sWebForm }
					   set iCount 0   
					   if {$sMxVersion >= 9.6} {
							set lsFieldSlsUser [split $slsUser |]
								foreach sFieldSlsUser $lsFieldSlsUser {
									incr iCount
									if { $count == $iCount } {
										set slsUser $sFieldSlsUser
									}
							}
						}
					   
						set sLabel [lindex $lsFieldLabel [expr $count -1]]
						set sDescription [lindex $lsFieldDescription [expr $count -1]]
						set sExpressionType [lindex $lsFieldExpressionType [expr $count -1]]
						set sExpression [lindex $lsFieldExpression [expr $count -1]]
						set sHref [lindex $lsFieldHref [expr $count -1]]
						set sAlt [lindex $lsFieldAlt [expr $count -1]]
						set sRange [lindex $lsFieldRange [expr $count -1]]
						set sUpdate [lindex $lsFieldUpdate [expr $count -1]]
						set bHidden [lindex $lsFieldHidden [expr $count -1]]
						set slsSettingName [lindex $sSettingFinalNames [expr $count -1]]
						set slsSettingValue [lindex $sSettingFinalValues [expr $count -1]]
				   }
				   
				   append sFile2 "$sFormName\t$sName\t$sLabel\t$sDescription\t$sExpressionType\t$sExpression\t$sHref\t$slsSettingName\t$slsSettingValue\t$slsUser\t$sAlt\t$sRange\t$sUpdate\t$iFieldOrder\t$bHidden\n"
               }
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Web Form data loaded in file $sPath"
   set iFile [open $sPath2 w]
   puts $iFile $sFile2
   close $iFile
   puts "Web Form Field data loaded in file $sPath2"
}
