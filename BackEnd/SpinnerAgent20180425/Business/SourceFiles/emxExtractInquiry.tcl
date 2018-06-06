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
#
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "inquiry "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "inquiry"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerInquiryData$sAppend.xls"
   set cPath "$sSpinnerPath/Business/SourceFiles/"
   set CExtention ".inq"
   set lsInquiry [split [mql list inquiry $sFilter] \n]
   set sFile "Name\tRegistry Name\tDescription\tPattern\tFormat\tArgument Name (use \"|\" delim)\tArgument Value (use \"|\" delim)\tCode (use \"<ESC>\" & \"<NEWLINE>\")\tHidden (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sInquiry $lsInquiry {
         set bPass TRUE
         set sModDate [mql print inquiry $sInquiry select modified dump]
         set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
         if {$sModDateMin != "" && $sModDate < $sModDateMin} {
            set bPass FALSE
         } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
            set bPass FALSE
         }

         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print inquiry $sInquiry select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print inquiry $sInquiry select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sInquiry)} sMsg
            regsub -all " " $sInquiry "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sInquiry
            }
            set sDescription [mql print inquiry $sInquiry select description dump]
            set sHidden [mql print inquiry $sInquiry select hidden dump]
            set slsPattern [mql print inquiry $sInquiry select pattern dump]
            set slsFormat [mql print inquiry $sInquiry select format dump]
            set sCode [mql print inquiry $sInquiry select code dump]
            #regsub -all "\134\012" $sCode "<NEWLINE>" sCode
            #regsub -all "\134\011" $sCode "<TAB>" sCode            
            #regsub -all "\134\042" $sCode "<DQUOTE>" sCode
			set sCodeValue ""
			set slsSettingName [mql print inquiry $sInquiry select argument.name dump " | "]
            set slsSettingValue [mql print inquiry $sInquiry select argument.value dump " | "]
            append sFile "$sName\t$sOrigName\t$sDescription\t$slsPattern\t$slsFormat\t$slsSettingName\t$slsSettingValue\t$sCodeValue\t$sHidden\n"
			set cFilePath ""

			# START-Added By SL Team for conversion of files in xls to .inq format in the SouceFiles folder
			puts "sOrigName $sOrigName"
			append cFilePath $cPath $sOrigName $CExtention
			puts "$cFilePath"
			 set cFile [open $cFilePath w]
			 puts $cFile $sCode
			 close $cFile
			 # END
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Inquiry data loaded in file $sPath"
}

