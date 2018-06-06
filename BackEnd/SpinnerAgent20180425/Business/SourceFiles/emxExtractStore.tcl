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
   set sTypeReplace "store "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "store"} {
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
      file mkdir $sSpinnerPath/System
   }

   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   set sPath(captured) "$sSpinnerPath/System/store_captured.xls"
   set sPath(ingested) "$sSpinnerPath/System/store_ingested.xls"
   set sPath(tracked) "$sSpinnerPath/System/store_tracked.xls"
   ####### Added by SL team ######
   set sPath(designsync) "$sSpinnerPath/System/store_designsync.xls"
   ######### END #########

   set sFile(captured) "name\tRegistry Name\tdescription\ttype\tfilename\tpermission\tprotocol\tport\thost\tpath\tuser\tpassword\tlocation\tfcs\thidden\tmultipledirectories\tlock\ticon\n"
   set sFile(ingested) "name\tRegistry Name\tdescription\ttype\ttablespace\tindexspace\thidden\tlock\ticon\n"
   set sFile(tracked) "name\tRegistry Name\tdescription\ttype\thidden\tlock\ticon\n"
   ####### Added by SL team ######
   set sFile(designsync) "name\tRegistry Name\tdescription\ttype\tprotocol\tport\thost\tpath\tuser\tpassword\tfcs\ticon\n"
   ######### END #########

   set lsItem(captured) [list sDescription sFilename sMultiDir sLock sPermission sProtocol sUser sHost sPort sStorePath sSearch sFcs sHidden]
   set lsItem(ingested) [list sDescription sLock sHidden sTablespace sIndexspace]
   set lsItem(tracked) [list sDescription sLock sHidden]
   ####### Added by SL team ######                 
   set lsItem(designsync) [list sDescription sFilename sProtocol sPort sHost sStorePath sUser sPassword sFcs]
   ######### END #########

   set lsPrintItem(captured) [list description filename multipledirectories locked permission protocol user host port path "search url" "fcs url" hidden]
   set lsPrintItem(ingested) [list description locked hidden "data tablespace" "index tablespace"]
   set lsPrintItem(tracked) [list description locked hidden]
   ####### Added by SL team ######            
   set lsPrintItem(designsync) [list description filename protocol port host path user Password "fcs url"]
   ######### END #########

   set lsStore [split [mql list store $sFilter] \n]
   if {!$bTemplate} {
      foreach sStore $lsStore {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print store $sStore select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print store $sStore select property\[SpinnerAgent\] dump] != "")} {
		
            foreach sItem [list sDescription sFilename sLock sPermission sProtocol sUser sPassword sHost sPort sStorePath sSearch sFcs sMultiDir sHidden sTablespace sIndexspace] {
			 
               eval "set $sItem \"\""
            }
            
            set sName [mql print store $sStore select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sStore)} sMsg
            regsub -all " " $sStore "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sStore
            }
            set sType [mql print store $sStore select type dump]
            if {$sType == "captured"} {set slsLocation [mql print store $sStore select location dump " | "]}
            set lsPrint [split [mql print store $sStore] \n]

            foreach sPrintItem $lsPrintItem($sType) sItem $lsItem($sType) {	

               set iList [lsearch -regexp $lsPrint $sPrintItem]

               if {$iList >= 0} {
                  set sResult [string trim [lindex $lsPrint $iList]]
                  if {$sPrintItem != "hidden" && $sPrintItem != "multipledirectories" && $sPrintItem != "locked"} {
                     regsub $sPrintItem $sResult "" sResult
                     set sResult [string trim $sResult]
                  }

############## Added By SL Team #############
	if {[string first "history" $sResult] == 0} {
	set sResult ""
	}
############## END #############	
                  eval {set $sItem $sResult}
               }
            }
if {$sMxVersion > 2011.1} {set sMultiDir "N/A"}
            switch $sType {
               captured {
                  append sFile(captured) "$sName\t$sOrigName\t$sDescription\t$sType\t$sFilename\t$sPermission\t$sProtocol\t$sPort\t$sHost\t$sStorePath\t$sUser\t\t$slsLocation\t$sFcs\t$sHidden\t$sMultiDir\t$sLock\n"
               } ingested {
                  append sFile(ingested) "$sName\t$sOrigName\t$sDescription\t$sType\t$sTablespace\t$sIndexspace\t$sHidden\t$sLock\n"
               } tracked {
                  append sFile(tracked) "$sName\t$sOrigName\t$sDescription\t$sType\t$sHidden\t$sLock\n"
               } designsync {
                  append sFile(designsync) "$sName\t$sOrigName\t$sDescription\t$sType\t$sProtocol\t$sPort\t$sHost\t$sStorePath\t$sUser\t$sPassword\t$sFcs\n"
               } 
            }
         }
      }
   }
   foreach sType [list captured ingested tracked designsync] {
      set iFile [open $sPath($sType) w]
      puts $iFile $sFile($sType)
      close $iFile
   }
   puts "Store data loaded in files:\n   $sPath(captured)\n   $sPath(ingested)\n   $sPath(tracked)\n $sPath(designsync)"
}

