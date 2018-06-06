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

# KYB Start V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute
proc pConvertToValueType { sValType } {
   set sAttrValueType ""
	
   switch $sValType {
		singleval {
		 set sAttrValueType ""
		}
		multival {
		 set sAttrValueType "multivalue"
		}
		rangeval {
		 set sAttrValueType "rangevalue"
		}
	}
	
	return $sAttrValueType
}
# KYB End V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute 

$cmd {

#  Set up array for symbolic name mapping
#
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "att "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "attribute"} {
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
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix/Business";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix/Business";
      }
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerAttributeData$sAppend.xls"
   set lsAttribute [split [mql list attribute $sFilter] \n]
 
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }

   if {$sMxVersion >= 2010.1} {
      # KYB Start V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute
      #set sFile "Attribute Name\tRegistry Name\tType\tDescription\tDefault\tRanges (use \"|\" delim)\tMultiline (boolean)\tHidden (boolean)\tDimension\tMax Length (int)\tResetonClone (boolean)\tResetonRevision (boolean)\tIcon File\n"
	  #KYB Start - V6R2015x GA - Support Attribute Scope
	  set sFile "Attribute Name\tRegistry Name\tScope\tOwner\tType\tDescription\tDefault\tRanges (use \"|\" delim)\tMultiline (boolean)\tHidden (boolean)\tDimension\tMax Length (int)\tResetonClone (boolean)\tResetonRevision (boolean)\tIcon File\tValueType\n"
	  #KYB End - V6R2015x GA - Support Attribute Scope
	  # KYB End V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute
   } elseif {$sMxVersion >= 10.8} {  
	  # KYB Start V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute   
      #set sFile "Attribute Name\tRegistry Name\tType\tDescription\tDefault\tRanges (use \"|\" delim)\tMultiline (boolean)\tHidden (boolean)\tDimension\tIcon File\n"
	  set sFile "Attribute Name\tRegistry Name\tType\tDescription\tDefault\tRanges (use \"|\" delim)\tMultiline (boolean)\tHidden (boolean)\tDimension\tIcon File\tValueType\n"
	  # KYB End V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute
   } else {
      set sFile "Attribute Name\tRegistry Name\tType\tDescription\tDefault\tRanges (use \"|\" delim)\tMultiline (boolean)\tHidden (boolean)\tIcon File\n"
   }
   
   if {!$bTemplate} {
      foreach sAttribute $lsAttribute {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print attribute $sAttribute select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print attribute $sAttribute select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print attribute $sAttribute select name dump]			
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sAttribute)} sMsg
            regsub -all " " $sAttribute "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sAttribute
            }
            set sDescription [mql print attribute $sAttribute select description dump]
	    # Added for incident SR00034350 start
             regsub -all "\\\n" $sDescription "<NEWLINE>" sDescription
			 regsub -all "\\\t" $sDescription "<TAB>" sDescription
            # Added for incident SR00034350 End
            set sType [mql print attribute $sAttribute select type dump]
            set sDefault [mql print attribute $sAttribute select default dump]
			############ Start Added by SL Team During Upgradation of 2012 ################
             regsub -all "\\\n" $sDefault "<NEWLINE>" sDefault
			 ############ END ##############################################################
            if {$sDefault == "True" || $sDefault == "true" || $sDefault == "False" || $sDefault == "false"} {
               set sDefault "'$sDefault"

            }
            set bMultiline [mql print attribute $sAttribute select multiline dump]
            set bHidden [mql print attribute $sAttribute select hidden dump]
            set slsRange [mql print attribute $sAttribute select range dump " | "]
            if {$sMxVersion > 2010.1} {
               set sDimension [mql print attribute $sAttribute select dimension dump]
               set iMaxLength [mql print attribute $sAttribute select maxlength dump]
               set bResetOnClone [mql print attribute $sAttribute select resetonclone dump]
               set bResetOnRevision [mql print attribute $sAttribute select resetonrevision dump]
               # KYB Start V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute   
			   set sValueType [mql print attribute $sAttribute select valuetype dump]
			   set sConvertedValueType [ pConvertToValueType $sValueType ]
			   #KYB Start - V6R2015x GA - Support Attribute Scope
			   set sScope [mql print attribute $sAttribute select ownerkind dump]
			   set sScopeOwner [mql print attribute $sAttribute select owner dump]
			   if {$sScopeOwner != ""} {
					set tempName "" 
					append tempName $sScopeOwner "." $sName
					set sName $tempName
				}
			   #append sFile "$sName\t$sOrigName\t$sType\t$sDescription\t$sDefault\t $slsRange\t$bMultiline\t$bHidden\t$sDimension\t$iMaxLength\t$bResetOnClone\t$bResetOnRevision\n"			   
			   append sFile "$sName\t$sOrigName\t$sScope\t$sScopeOwner\t$sType\t$sDescription\t$sDefault\t $slsRange\t$bMultiline\t$bHidden\t$sDimension\t$iMaxLength\t$bResetOnClone\t$bResetOnRevision\t\t$sConvertedValueType\n"
			   #KYB End - V6R2015x GA - Support Attribute Scope
			   # KYB End V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute   
            } elseif {$sMxVersion >= 10.8} {
               set sDimension [mql print attribute $sAttribute select dimension dump]
			   # KYB Start V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute   
			   set sValueType [mql print attribute $sAttribute select valuetype dump]  
			   set sConvertedValueType [ pConvertToValueType $sValueType ]
               #append sFile "$sName\t$sOrigName\t$sType\t$sDescription\t$sDefault\t $slsRange\t$bMultiline\t$bHidden\t$sDimension\n"
			   append sFile "$sName\t$sOrigName\t$sType\t$sDescription\t$sDefault\t $slsRange\t$bMultiline\t$bHidden\t$sDimension\t\t$sConvertedValueType\n"
			   # KYB End V6R2013x SPN-MVATTR-010 Extract Value Type for an Attribute 
            } else {
               append sFile "$sName\t$sOrigName\t$sType\t$sDescription\t$sDefault\t $slsRange\t$bMultiline\t$bHidden\n"
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Attribute data loaded in file $sPath"
}
