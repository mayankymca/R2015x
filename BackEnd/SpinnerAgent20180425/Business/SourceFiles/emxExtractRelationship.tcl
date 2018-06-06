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

#  Major/Minor Flag
   #KYB Start - Commented code for checking major/minor enabled or not
   #set bMm [mql get env MAJORMINOR]
   #if {$bMm != "TRUE"} {
      #set bMm FALSE
   #}
   #KYB End - Commented code for checking major/minor enabled or not

#  Set up array for symbolic name mapping
#
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "relationship "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "relationship"} {
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

   set sPath "$sSpinnerPath/Business/SpinnerRelationshipData$sAppend.xls"
   set lsRelationship [split [mql list relationship $sFilter] \n]
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
# Major/Minor Check - ION - 10/1/2011
   #KYB Start - Commented code for checking major/minor enabled or not   
   #mql verbose on
   #if {[catch {
    #  set sTestMm [mql validate upgrade revisions]
	  #KYB fix SCR-0002369 for J&J for V6R2013x
	 # set sTest [string trim $sTestMm]
      #if {$sTestMm == "" || [string tolower $sTestMm] == "validation of upgrade complete"}
	  #if {$sTest == "" || [string last "Validation of upgrade complete" $sTest] >= 0} {
       #  set bMm TRUE
   #} else {
	#     set bMm FALSE
	 # }
   #} sMsg] != 0} {
    #  set bMm FALSE
   #}

   #mql verbose off
   #set bMm [mql get env MAJORMINOREXTRACTION]
   #KYB End - Commented code for checking major/minor enabled or not  
   
   if {$sMxVersion >= 10.8} {
   
         # KYB
         #set sFile "Name\tRegistry Name\tDescription\tAttributes (use \"|\" delim)\tSparse (boolean)\tHidden (boolean)\tPreventDuplicates (boolean)\tFrom Types (use \"|\" delim)\tFrom Rels (use \"|\" delim)\tFrom Revision (none or \"\"/ float / replicate)\tFrom Clone (none or \"\" / float / replicate)\tFrom Cardinality (one / many or \"\")\tFrom Propagate Modify (boolean)\tTo Types (use \"|\" delim)\tTo Rels (use \"|\" delim)\tTo Revision (none or \"\" / float / replicate)\tTo Clone (none or \"\" / float / replicate)\tTo Cardinality (one / many or \"\")\tTo Propagate Modify (boolean)\tFrom Propagate Connect (boolean)\tTo Propagate Connect (boolean)\tFrom Meaning\tTo Meaning\tDynamic (boolean)\tIcon File\n"
          set sFile "Name\tRegistry Name\tDescription\tAttributes (use \"|\" delim)\tSparse (boolean)\tHidden (boolean)\tPreventDuplicates (boolean)\tFrom Types (use \"|\" delim)\tFrom Rels (use \"|\" delim)\tFrom Revision (none or \"\"/ float / replicate)\tFrom Clone (none or \"\" / float / replicate)\tFrom Cardinality (one / many or \"\")\tFrom Propagate Modify (boolean)\tTo Types (use \"|\" delim)\tTo Rels (use \"|\" delim)\tTo Revision (none or \"\" / float / replicate)\tTo Clone (none or \"\" / float / replicate)\tTo Cardinality (one / many or \"\")\tTo Propagate Modify (boolean)\tFrom Propagate Connect (boolean)\tTo Propagate Connect (boolean)\tFrom Meaning\tTo Meaning\tDynamic (boolean)\tIcon File\tParent Rel\tAbstract(boolean)\n"
      
   } else {
      # KYB
      #set sFile "Name\tRegistry Name\tDescription\tAttributes (use \"|\" delim)\tSparse (boolean)\tHidden (boolean)\tPreventDuplicates (boolean)\tFrom Types (use \"|\" delim)\tFrom Meaning\tFrom Revision (none or \"\"/ float / replicate)\tFrom Clone (none or \"\" / float / replicate)\tFrom Cardinality (one / many or \"\")\tFrom Propagate Modify (boolean)\tTo Types (use \"|\" delim)\tTo Meaning\tTo Revision (none or \"\" / float / replicate)\tTo Clone (none or \"\" / float / replicate)\tTo Cardinality (one / many or \"\")\tTo Propagate Modify (boolean)\tFrom Propagate Connect (boolean)\tTo Propagate Connect (boolean)\tIcon File\n"
      set sFile "Name\tRegistry Name\tDescription\tAttributes (use \"|\" delim)\tSparse (boolean)\tHidden (boolean)\tPreventDuplicates (boolean)\tFrom Types (use \"|\" delim)\tFrom Meaning\tFrom Revision (none or \"\"/ float / replicate)\tFrom Clone (none or \"\" / float / replicate)\tFrom Cardinality (one / many or \"\")\tFrom Propagate Modify (boolean)\tTo Types (use \"|\" delim)\tTo Meaning\tTo Revision (none or \"\" / float / replicate)\tTo Clone (none or \"\" / float / replicate)\tTo Cardinality (one / many or \"\")\tTo Propagate Modify (boolean)\tFrom Propagate Connect (boolean)\tTo Propagate Connect (boolean)\tIcon File\tParent Rel\tAbstract(boolean)\n"
   }
   
   if {!$bTemplate} {
      foreach sRelationship $lsRelationship {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print relationship $sRelationship select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print relationship $sRelationship select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print relationship $sRelationship select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sRelationship)} sMsg
            regsub -all " " $sRelationship "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sRelationship
            }
           
            # KYB Start
	        set sParentType [mql print relationship $sRelationship select derived dump]
	        set sAbstractType [mql print relationship $sRelationship select abstract dump]
			# KYB End
            set sDescription [mql print relationship $sRelationship select description dump]
            set bSparse [mql print relationship $sRelationship select sparse dump]
            set bHidden [mql print relationship $sRelationship select hidden dump]
            set bPreventDuplicate [mql print relationship $sRelationship select preventduplicates dump]
            set sFromMeaning [mql print relationship $sRelationship select frommeaning dump]
            set sFromRevision [mql print relationship $sRelationship select fromreviseaction dump]
            set sFromClone [mql print relationship $sRelationship select fromcloneaction dump]
            set sFromCardinality [mql print relationship $sRelationship select fromcardinality dump]
            set sToMeaning [mql print relationship $sRelationship select tomeaning dump]
            set sToRevision [mql print relationship $sRelationship select toreviseaction dump]
            set sToClone [mql print relationship $sRelationship select tocloneaction dump]
            set sToCardinality [mql print relationship $sRelationship select tocardinality dump]
            set slsFromType [mql print relationship $sRelationship select fromtype dump " | "]
            set slsToType [mql print relationship $sRelationship select totype dump " | "]
            if {$sMxVersion >= 10.8} {
               set slsFromRel [mql print relationship $sRelationship select fromrel dump " | "]
               set slsToRel [mql print relationship $sRelationship select torel dump " | "]
            }
			set bDynamic ""
			
			   set bDynamic [mql print relationship $sRelationship select dynamic dump]
            
   
            if {$sMxVersion < 10.5} {
               set bFromPropConnect ""
               set bToPropConnect ""
               if {$slsFromType == ""} {
                  set lsPrint [split [mql print relationship $sRelationship] \n]
                  set bTrip FALSE
                  foreach sPrint $lsPrint {
                     set sPrint [string trim $sPrint]
                     if {$bTrip} {
                        if {$sPrint == "to"} {
                           break
                        } elseif {$sPrint == "type all"} {
                           set slsFromType all
                        }
                     } elseif {$sPrint == "from"} {
                        set bTrip TRUE
                     }
                  }
               }
               if {$slsToType == ""} {
                  set lsPrint [split [mql print relationship $sRelationship] \n]
                  set bTrip FALSE
                  foreach sPrint $lsPrint {
                     set sPrint [string trim $sPrint]
                     if {$bTrip} {
                        if {$sPrint == "type all"} {
                           set slsToType all
                        }
                     } elseif {$sPrint == "to"} {
                        set bTrip TRUE
                     }
                  }
               }
         
               set lsRel [split [mql print rel $sRelationship] \n]
               set iCounter 0
               foreach sRel $lsRel {
                  set sRel [string trim $sRel]
                  if {[string first "propagate modify" $sRel] == 0} {
                     incr iCounter
                     set lslsRel [split $sRel " "]
                     if {$iCounter == 1} {
                        set bFromPropModify [lindex $lslsRel 2]
                     } else {
                        set bToPropModify [lindex $lslsRel 2]
                     }
                  }
                  if {$iCounter > 1} {
                     break
                  }
               }
            } else {
               set bFromPropModify [mql print relationship $sRelationship select frompropagatemodify dump]
               set bToPropModify [mql print relationship $sRelationship select topropagatemodify dump]
               set bFromPropConnect [mql print relationship $sRelationship select frompropagateconnection dump]
               set bToPropConnect [mql print relationship $sRelationship select topropagateconnection dump]
            }
   
            set slsAttribute [mql print relationship $sRelationship select attribute dump " | "]
            if {$sMxVersion >= 10.8} {
               # KYB
               #append sFile "$sName\t$sOrigName\t$sDescription\t$slsAttribute\t$bSparse\t$bHidden\t$bPreventDuplicate\t$slsFromType\t$slsFromRel\t$sFromRevision\t$sFromClone\t$sFromCardinality\t$bFromPropModify\t$slsToType\t$slsToRel\t$sToRevision\t$sToClone\t$sToCardinality\t$bToPropModify\t$bFromPropConnect\t$bToPropConnect\t$sFromMeaning\t$sToMeaning\t$bDynamic\n"
			 
               append sFile "$sName\t$sOrigName\t$sDescription\t$slsAttribute\t$bSparse\t$bHidden\t$bPreventDuplicate\t$slsFromType\t$slsFromRel\t$sFromRevision\t$sFromClone\t$sFromCardinality\t$bFromPropModify\t$slsToType\t$slsToRel\t$sToRevision\t$sToClone\t$sToCardinality\t$bToPropModify\t$bFromPropConnect\t$bToPropConnect\t$sFromMeaning\t$sToMeaning\t$bDynamic\t\t$sParentType\t$sAbstractType\n"
            
            } else {
               # KYB
               #append sFile "$sName\t$sOrigName\t$sDescription\t$slsAttribute\t$bSparse\t$bHidden\t$bPreventDuplicate\t$slsFromType\t$sFromMeaning\t$sFromRevision\t$sFromClone\t$sFromCardinality\t$bFromPropModify\t$slsToType\t$sToMeaning\t$sToRevision\t$sToClone\t$sToCardinality\t$bToPropModify\t$bFromPropConnect\t$bToPropConnect\n"
               append sFile "$sName\t$sOrigName\t$sDescription\t$slsAttribute\t$bSparse\t$bHidden\t$bPreventDuplicate\t$slsFromType\t$sFromMeaning\t$sFromRevision\t$sFromClone\t$sFromCardinality\t$bFromPropModify\t$slsToType\t$sToMeaning\t$sToRevision\t$sToClone\t$sToCardinality\t$bToPropModify\t$bFromPropConnect\t$bToPropConnect\t\t$sParentType\t$sAbstractType\n"
            }
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Relationship data loaded in file $sPath"
}
