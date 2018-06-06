
tcl;

eval {
   if {[info host] == "mostermant61p" } {
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
   set sTypeReplace "format "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "format"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerFormatData$sAppend.xls"
   set lsFormat [split [mql list format $sFilter] \n]
   #KYB removed view,edit,print from writing to file, since it is not present in ENOVIA V6R2014x BPS code
   #set sFile "Format Name\tRegistry Name\tDescription\tVersion\tFile Suffix\tFile Creator\tFile Type\tView Command\tEdit Command\tPrint Command\tMime\tHidden (boolean)\tIcon File\n"
   set sFile "Format Name\tRegistry Name\tDescription\tVersion\tFile Suffix\tFile Creator\tFile Type\tMime\tHidden (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sFormat $lsFormat {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print format $sFormat select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print format $sFormat select property\[SpinnerAgent\] dump] != "")} {
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sFormat)} sMsg
            regsub -all " " $sFormat "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sFormat
            }
            set sDescription [mql print format $sFormat select description dump]
            set bHidden [mql print format $sFormat select hidden dump]
            set sVersion [mql print format $sFormat select version dump]
            set sFileSuffix [mql print format $sFormat select filesuffix dump]
			#KYB Commented MQL query to print 'view' for format, since it is not present in ENOVIA V6R2014x BPS code
            #set sViewCommand [mql print format $sFormat select view dump]
			#KYB Commented MQL query to print 'edit' for format, since it is not present in ENOVIA V6R2014x BPS code			
            #set sEditCommand [mql print format $sFormat select edit dump]
			#KYB Commented MQL query to print 'print' for format, since it is not present in ENOVIA V6R2014x BPS code						
            #set sPrintCommand [mql print format $sFormat select print dump]
      
            set sMime ""
            set sFileCreator ""
            set sFileType ""
            set lsPrint ""
            set lsPrint [split [mql print format $sFormat] \n]
            
            foreach sPrint $lsPrint {
               set sPrint [string trim $sPrint]
               
               if {[string first "type" $sPrint] == 0} {
                  regsub "type" $sPrint "" sFileType
                  set sFileType [string trim $sFileType]
                  set sFileCreator $sFileType
               } elseif {[string first "mime" $sPrint] == 0} {
                  regsub "mime" $sPrint "" sMime
                  set sMime [string trim $sMime]
               }
            }
			#KYB removed view,edit,print from writing to file, since it is not present in ENOVIA V6R2014x BPS code
            #append sFile "$sFormat\t$sOrigName\t$sDescription\t$sVersion\t$sFileSuffix\t$sFileCreator\t$sFileType\t$sViewCommand\t$sEditCommand\t$sPrintCommand\t$sMime\t$bHidden\n"
			append sFile "$sFormat\t$sOrigName\t$sDescription\t$sVersion\t$sFileSuffix\t$sFileCreator\t$sFileType\t$sMime\t$bHidden\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Format data loaded in file $sPath"
}
