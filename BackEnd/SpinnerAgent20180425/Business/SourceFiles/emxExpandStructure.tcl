#*******************************************************************************
# @progdoc        emxExpandStructure.tcl v 3.0
#
# @Brief:         Generates structure for business object(s) with connections in Spinner data file format.
#
# @Description:   Generates structure for business object with connections in Spinner data file format.
#                 The Use Case is expanding a structure such as a Project Space, Part (Assembly) or Specification and
#                 picking up the desired reference relationships and objects hanging on the structure for migration
#                 to another environment or reporting purposes. 
#
# @Parameters:    Business Object Name(s), Primary Relationship Name, Optional Secondary Relationship Names
#
# @Returns:       Nothing
#
# @Usage:         Run in MQL: exec prog emxExpandStructure.tcl "T|N|R" "P-REL|DIR|LVL" "S-REL(1)|BO,S-REL(N)|[BO] or *|[BO]"
#
# USAGE:  exec prog emxExpandStructure.tcl "[PARAM 1]" "[PARAM 2]" "[PARAM 3] (optional)"
#
#          where [PARAM 1] is " T | N | R " 
#                          Object Type, Name and Revision - pipe separated, wildcards allowed
#                [PARAM 2] is " P-REL | LVL | DIR "
#                          P-REL the "Primary Relationship Name" for the structure being expanded
#                          LVL is the number of levels to expand with values '1', '2',..., 'all' (default 'all')
#                          DIR is the direction with values 'from' or 'to' (default 'from' if blank)
#                [PARAM 3] is " S-REL(1) | BUSOBJ ,..., S-REL(N) | BUSOBJ"
#                          S-REL is the Secondary Rel Name(s) for one level on expanded objects (comma-separated list)
#                          BUSOBJ is 'bo' to add bus objects or 'file' to include bus obj files from secondary rels.
#          valid examples:
#                   exec prog emxExpandStructure.tcl "Part|Assembly One|1" "EBOM" "Alternate Part|bo, Reference Document|file"
#                   exec prog emxExpandStructure.tcl "Project Space|Project*|*" "SubTask" "Dependency,Project Access Key"
#                   exec prog emxExpandStructure.tcl "Part|Bolt, Lag, 3/4 inch|1" "EBOM|3|to"
#
# @progdoc        Copyright (c) 2005, ENOVIA
#*******************************************************************************
# @Modifications:
#
# Matt Osterman 01/25/2007 - Originated
# Matt Osterman 10/25/2007 - Update to workaround objects w/attribute groups (interfaces)
#                            Attributes from interfaces do not get pulled due to migration issues
# ION           07/16/2009 - Added grant support
#
#*******************************************************************************

tcl;

eval {
   set sHost [info host]

   if { $sHost == "sn732plp" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
      set cmd "debugger_eval"
      set xxx [debugger_init]
   } else {
      set cmd "eval"
   }
}
$cmd {

set sDirections(1) {
This script generates structure for business objects with connections in Spinner data file format.
The Use Case is expanding a structure such as a Project Space, Part (Assembly) or Specification and
picking up the desired reference relationships and objects hanging on the structure for migration
to another environment, pulling up where used scenerios or for reporting purposes.
}
set sDirections(2) {
USAGE:  exec prog emxExpandStructure.tcl "[PARAM 1]" "[PARAM 2]" "[PARAM 3] (optional)"
%
where [PARAM 1] is " T | N | R " 
                     Object Type, Name and Revision - pipe separated, wildcards allowed
           [PARAM 2] is " P-REL | LVL | DIR "
                     P-REL the "Primary Relationship Name" for the structure being expanded
                     LVL is the number of levels to expand with values '1', '2',..., 'all' (default 'all')
                     DIR is the direction with values 'from' or 'to' (default 'from' if blank)
           [PARAM 3] is " S-REL(1) | BUSOBJ ,..., S-REL(N) | BUSOBJ"
                     S-REL is the Secondary Rel Name(s) for one level on expanded objects (comma-separated list)
                     BUSOBJ is 'bo' to add bus objects or 'file' to include bus obj files from secondary rels. 
valid examples:
         exec prog emxExpandStructure.tcl "Part|Assembly One|1" "EBOM" "Alternate Part|bo, Reference Document|file"
         exec prog emxExpandStructure.tcl "Project Space|Project*|*" "SubTask" "Dependency,Project Access Key"
         exec prog emxExpandStructure.tcl "Part|Bolt, Lag, 3/4 inch|1" "EBOM|3|to"
}

#*******************************************************************************
# Procedure:   pSet_Filename
#
# Description: Replaces special characters for valid filename
#
# Returns:     None.
#*******************************************************************************
   proc pSet_FileName {sType sName sRev sRel sBO} {
      if {$sBO == "bo"} {
         set sFileName "bo_$sType\-$sName\-$sRev\_$sRel"
      } elseif {$sBO == "rel"} {
         set sFileName "rel_$sRel\_$sType\-$sName\-$sRev"
      } elseif {$sBO == "file1"} {
         set sFileName "file_$sType\-$sName\-$sRev\_$sRel"
      } elseif {$sBO == "file2"} {
         set sFileName "file_$sType\-$sName\-$sRev"
      } elseif {$sBO == "grant"} {
         set sFileName "grant_$sType\-$sName\-$sRev"
      } else {
         set sFileName "_$sType\-$sName\-$sRev"
      }
      regsub -all "\134\052" $sFileName "ALL" sFileName
      regsub -all "\134\174" $sFileName "-" sFileName
      regsub -all "/" $sFileName "-" sFileName
      regsub -all ":" $sFileName "-" sFileName
      regsub -all "<" $sFileName "-" sFileName
      regsub -all ">" $sFileName "-" sFileName
      regsub -all " " $sFileName "" sFileName
      return $sFileName
   }

#*******************************************************************************
# Procedure:   pSet_Grant
#
# Description: Adds grants if they exist on bus objects
#
# Code by B. Wilson for Grant support - adapted by ION
#
# Returns:     None.
#*******************************************************************************
   proc pSet_Grant {sGType sGName sGRev sRelPrimary} {
      global sDumpSchemaDirGrants
      set sFileName [pSet_FileName $sGType $sGName $sGRev $sRelPrimary "grant"]
      set p_grantname "$sDumpSchemaDirGrants/$sFileName\.xls"
      set p_grant [open $p_grantname w]
      puts $p_grant "Type\tName\tRevision\tGrantor\tGrantee\tRead\tModify\tDelete\tCheckout\tCheckin\tSchedule\tLock\tUnlock\tExecute\tFreeze\tThaw\tCreate\tRevise\tPromote\tDemote\tGrant\tEnable\tDisable\tOverride\tChangeName\tChangeType\tChangeOwner\tChangePolicy\tRevoke\tChangeVault\tFromConnect\tToConnect\tFromDisconnect\tToDisconnect\tViewForm\tModifyform\tShow\tSignature\tKey"

      set sGrants [mql print bus "$sGType" "$sGName" "$sGRev" select grantee grantor granteeaccess granteesignature dump |]
      set lsGrant [split $sGrants |]
      set iListCnt [llength $lsGrant]
      set sGrantTotal [expr $iListCnt / 4]
      set iCnt 1
      while {$iCnt <= $sGrantTotal} {
         set sGrantee [string trim [lindex $lsGrant [expr $iCnt - 1]]]
         set sGrantor [string trim [lindex $lsGrant [expr (($sGrantTotal * 1) + $iCnt) - 1]]]
         set sGAccess [string trim [lindex $lsGrant [expr (($sGrantTotal * 2) + $iCnt) - 1]]]
         set sGSign   [string trim [lindex $lsGrant [expr (($sGrantTotal * 3) + $iCnt) - 1]]]
         incr iCnt

         # Initialize variables
         set sRead "-"
         set sModify "-"
         set sDelete "-"
         set sCheckout "-"
         set sCheckin "-"
         set sSchedule "-"
         set sLock "-"
         set sUnlock "-"
         set sExecute "-"
         set sFreeze "-"
         set sThaw "-"
         set sCreate "-"
         set sRevise "-"
         set sPromote "-"
         set sDemote "-"
         set sGrant "-"
         set sEnable "-"
         set sDisable "-"
         set sOverride "-"
         set sChangeName "-"
         set sChangeType "-"
         set sChangeOwner "-"
         set sChangePolicy "-"
         set sRevoke "-"
         set sChangeVault "-"
         set sFromConnect "-"
         set sToConnect "-"
         set sFromDisconnect "-"
         set sToDisconnect "-"
         set sViewForm "-"
         set sModifyform "-"
         set sShow "-"
         set sKey ""

         if {[string first "read" $sGAccess] >= 0} {set sRead "Y"}
         if {[string first "modify" $sGAccess] >= 0} {set sModify "Y"}
         if {[string first "delete" $sGAccess] >= 0} {set sDelete "Y"}
         if {[string first "checkout" $sGAccess] >= 0} {set sCheckout "Y"}
         if {[string first "checkin" $sGAccess] >= 0} {set sCheckin "Y"}
         if {[string first "schedule" $sGAccess] >= 0} {set sSchedule "Y"}
         if {[string first "lock" $sGAccess] >= 0} {set sLock "Y"}
         if {[string first "unlock" $sGAccess] >= 0} {set sUnlock "Y"}
         if {[string first "execute" $sGAccess] >= 0} {set sExecute "Y"}
         if {[string first "freeze" $sGAccess] >= 0} {set sFreeze "Y"}
         if {[string first "thaw" $sGAccess] >= 0} {set sThaw "Y"}
         if {[string first "create" $sGAccess] >= 0} {set sCreate "Y"}
         if {[string first "revise" $sGAccess] >= 0} {set sRevise "Y"}
         if {[string first "promote" $sGAccess] >= 0} {set sPromote "Y"}
         if {[string first "demote" $sGAccess] >= 0} {set sDemote "Y"}
         if {[string first "grant" $sGAccess] >= 0} {set sGrant "Y"}
         if {[string first "enable" $sGAccess] >= 0} {set sEnable "Y"}
         if {[string first "disable" $sGAccess] >= 0} {set sDisable "Y"}
         if {[string first "override" $sGAccess] >= 0} {set sOverride "Y"}
         if {[string first "changename" $sGAccess] >= 0} {set sChangeName "Y"}
         if {[string first "changetype" $sGAccess] >= 0} {set sChangeType "Y"}
         if {[string first "changeowner" $sGAccess] >= 0} {set sChangeOwner "Y"}
         if {[string first "changepolicy" $sGAccess] >= 0} {set sChangePolicy "Y"}
         if {[string first "revoke" $sGAccess] >= 0} {set sRevoke "Y"}
         if {[string first "changevault" $sGAccess] >= 0} {set sChangeVault "Y"}
         if {[string first "fromconnect" $sGAccess] >= 0} {set sFromConnect "Y"}
         if {[string first "toconnect" $sGAccess] >= 0} {set sToConnect "Y"}
         if {[string first "fromdisconnect" $sGAccess] >= 0} {set sFromDisconnect "Y"}
         if {[string first "todisconnect" $sGAccess] >= 0} {set sToDisconnect "Y"}
         if {[string first "viewform" $sGAccess] >= 0} {set sViewForm "Y"}
         if {[string first "modifyform" $sGAccess] >= 0} {set sModifyform "Y"}
         if {[string first "show" $sGAccess] >= 0} {set sShow "Y"}

         puts $p_grant "$sGType\t$sGName\t$sGRev\t$sGrantor\t$sGrantee\t$sRead\t$sModify\t$sDelete\t$sCheckout\t$sCheckin\t$sSchedule\t$sLock\t$sUnlock\t$sExecute\t$sFreeze\t$sThaw\t$sCreate\t$sRevise\t$sPromote\t$sDemote\t$sGrant\t$sEnable\t$sDisable\t$sOverride\t$sChangeName\t$sChangeType\t$sChangeOwner\t$sChangePolicy\t$sRevoke\t$sChangeVault\t$sFromConnect\t$sToConnect\t$sFromDisconnect\t$sToDisconnect\t$sViewForm\t$sModifyform\t$sShow\t$sGSign\t$sKey"
      }
      close $p_grant
   }
   
# end of procedures

# main
   set sSpinnerPath [mql get env SPINNERPATHBO]
   set slsBusTNR [mql get env 1]
   set slsRelPrimary [mql get env 2]
   set slsRelSecondary [mql get env 3]
   set bChkErr FALSE
   
   if {$slsBusTNR == "" || [string tolower $slsBusTNR] == "help"} {
      puts "$sDirections(1)$sDirections(2)"
      exit 1
      return
   }

   set lsBusTNR [split $slsBusTNR |]
   set sBusType [string trim [lindex $lsBusTNR 0]]
   set sBusName [string trim [lindex $lsBusTNR 1]]
   set sBusRev [string trim [lindex $lsBusTNR 2]]

   # Modified by B. Wilson for comma in the name - START
   regsub -all "," $sBusName "?" sBusNameComma
   # Modified by B. Wilson for comma in the name - END

   set lsBus [split [mql temp query bus "$sBusType" "$sBusNameComma" "$sBusRev" select id dump |] \n]
   if {$lsBus == {}} {
      puts "ERROR: No Business Objects for \"$sBusType\" \"$sBusName\" \"$sBusRev\" found."
      exit 1
      return
   }
   
   set lsRelPrimary [split $slsRelPrimary |]
   set sRelPrimary [string trim [lindex $lsRelPrimary 0]]
   set sRelPrimLvl [string trim [lindex $lsRelPrimary 1]]
   set sRelPrimLvl [string tolower $sRelPrimLvl]
   set sRelPrimDir [string trim [lindex $lsRelPrimary 2]]
   set sRelPrimDir [string tolower $sRelPrimDir]
   set sErrorMsg ""
   if {$slsRelPrimary == ""} {
      set sErrorMsg "ERROR: No relationship parameters provided"
   } elseif {$sRelPrimLvl != "" && $sRelPrimLvl != "all" && [string is integer $sRelPrimLvl] != 1} {
      set sErrorMsg "ERROR: Relationship level parameter \042$sRelPrimLvl\042 not valid"
   } elseif {[lsearch [list "" "to" "from"] $sRelPrimDir] < 0} {
      set sErrorMsg "ERROR: Relationship direction parameter \042$sRelPrimDir\042 not valid"
   }
   if {$sErrorMsg != ""} {
      puts "$sDirections(2)\n$sErrorMsg - see directions above then re-execute"
      exit 1
      return
   } elseif {[mql list relationship "$sRelPrimary"] == ""} {
      puts "ERROR: Relationship \042$sRelPrimary\042 not found in database."
      exit 1
      return
   }
   
   if {$sRelPrimLvl == ""} {set sRelPrimLvl "all"}
   if {$sRelPrimDir == ""} {set sRelPrimDir "from"}
   
   set lsRelSecondary {}
   if {$slsRelSecondary != ""} {
      set lsRelSecTemp [split $slsRelSecondary ,]
      foreach slsRelSecTemp $lsRelSecTemp {
         set lsRelSecTemp [split $slsRelSecTemp |]
         set sRelSec [string trim [lindex $lsRelSecTemp 0]]
         set sRelSecOpt [string trim [lindex $lsRelSecTemp 1]]
         set sRelSecOpt [string tolower $sRelSecOpt]
         if {[lsearch [list "" "bo" "file"] $sRelSecOpt] < 0} {
            puts "$sDirections(2)\nERROR: Secondary Relationship bus object parameter \042$sRelSecOpt\042 not valid - see directions above then re-execute"
            exit 1
            return
         } elseif {$sRelSec == "*"} {
            puts "$sDirections(2)\nERROR: Secondary Relationship \042*\042 wildcard option not available - see directions above then re-execute"
            exit 1
            return
         } elseif {[mql list relationship "$sRelSec"] == ""} {
            puts "ERROR: Secondary Relationship \042$sRelSec\042 not found in database."
            exit 1
            return
         }
         lappend lsRelSecondary "$sRelSec"
         set sRelSecOption($sRelSec) $sRelSecOpt
      }
   }
   
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix1 [clock format [clock seconds] -format "%Y%m%d"]
      set sSuffix2 [pSet_FileName $sBusType $sBusName $sBusRev "" "path"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix1$sSuffix2";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix1$sSuffix2";
      }
   }

   set sDumpSchemaDirObjects [ file join $sSpinnerPath Objects ]
   file mkdir $sDumpSchemaDirObjects
   set sDumpSchemaDirFiles [ file join $sDumpSchemaDirObjects Files ]
   file mkdir $sDumpSchemaDirFiles
   # Added for Grants support - START
   set sDumpSchemaDirGrants [ file join $sDumpSchemaDirObjects Grants ]
   file mkdir $sDumpSchemaDirGrants
   # Added for Grants support - END
   set sDumpSchemaDirRelationships [ file join $sSpinnerPath Relationships ]
   file mkdir $sDumpSchemaDirRelationships
   set sDumpSchemaDirLogs [ file join $sSpinnerPath "logs" ]
   file mkdir $sDumpSchemaDirLogs
   set p_logfile "$sDumpSchemaDirLogs/CheckoutError.log"
   set p_errlog [open $p_logfile w]

# Primary Structure

   foreach slsBus $lsBus {
      set lslsBus [split $slsBus |]
      set sType [string trim [lindex $lslsBus 0]]
      set sName [string trim [lindex $lslsBus 1]]
      set sRev  [string trim [lindex $lslsBus 2]]
      puts "Start backup of Business Object \042$sType\042 \042$sName\042 \042$sRev\042 Relationship \042$sRelPrimary\042 Structure..."

      set sFileName [pSet_FileName $sType $sName $sRev $sRelPrimary "bo"]
      set p_filename "$sDumpSchemaDirObjects/$sFileName\.xls"
      set p_file [open $p_filename w]
      
      set sAttr($sType)  [ mql print type "$sType" select attribute dump \t ]
      puts $p_file "Type\tName\tRev\tChange Name\tChange Rev\tPolicy\tState\tVault\tOwner\tdescription\t$sAttr($sType)\t<HEADER>"
      # Modified by B. Wilson for comma in the name - START
      regsub -all "," $sName "?" sNameComma
      set sItemOne [mql temp query bus "$sType" "$sNameComma" "$sRev" select name revision policy current vault owner description attribute.value dump \t]
      # Modified by B. Wilson for comma in the name - END
      regsub -all "\n" $sItemOne {<NEWLINE>} sItemOne
      puts $p_file $sItemOne
      
      # Grant Support
      if {[mql print bus "$sType" "$sName" "$sRev" select grantee dump] != ""} {
         pSet_Grant "$sType" "$sName" "$sRev" "$sRelPrimary"
      }
      # End Grant Support - ION
      
      set sFileRel [pSet_FileName $sType $sName $sRev $sRelPrimary "rel"]
      set p_filerel "$sDumpSchemaDirRelationships/$sFileRel\.xls"
      set p_rel [open $p_filerel w]

      set sAttrRel($sRelPrimary)  [ mql print relationship "$sRelPrimary" select attribute dump \t ]
      puts $p_rel "FromType\tFromName\tFromRev\tToType\tToName\tToRev\tDirection\tRelationship\t$sAttrRel($sRelPrimary)"
      
      set slsStructure [mql expand bus "$sType" "$sName" "$sRev" rel "$sRelPrimary" $sRelPrimDir recurse to $sRelPrimLvl select bus name revision policy current vault owner description attribute.value select rel name $sRelPrimDir.type $sRelPrimDir.name $sRelPrimDir.revision attribute.value dump \t recordsep <NEWRECORD>]
      regsub -all "\n" $slsStructure {<NEWLINE>} slsStructure
      regsub -all "<NEWRECORD>" $slsStructure  "\n" slsStructure
      set lsStructure [split $slsStructure \n]
      set lsItem {}
      set lsItemRel {}
      foreach slsStructure $lsStructure {
         if {$slsStructure != ""} {
            set lslsStructure [split $slsStructure \t]
            set sToType [lindex $lslsStructure 3]
            set sToName [lindex $lslsStructure 4]
            set sToRev [lindex $lslsStructure 5]
            if {[catch {set iEnd [expr 12 + $iAttr($sToType)]} sMsg] != 0} {
               set iAttr($sToType) [llength [split [mql print type $sToType select attribute dump |] |] ]
               set iEnd [expr 12 + $iAttr($sToType)]
            }
            lappend lsItem [join [lrange $lslsStructure 3 $iEnd] \t]
            set iAttrRel [llength [split [mql print bus "$sToType" "$sToName" "$sToRev" select attribute dump |] |] ]
            set iEndRel [expr 12 + $iAttrRel]
            lappend lsItemRel [join [concat [lrange $lslsStructure [expr $iEndRel + 2] [expr $iEndRel + 4]] [lrange $lslsStructure 3 5] [lindex $lslsStructure 2] [list $sRelPrimary] [lrange $lslsStructure [expr $iEndRel + 5] end]] \t]
            
            # Grant Support
            if {[mql print bus "$sToType" "$sToName" "$sToRev" select grantee dump |] != ""} {
               pSet_Grant "$sToType" "$sToName" "$sToRev" "$sRelPrimary"
            }
            # End Grant Support - ION
            
         }
      }
      set lsItem [lsort -unique $lsItem]
      set sPreType $sType
      foreach sItem $lsItem {
         set sExpType [lindex [split $sItem \t] 0]
         if {[catch {set sTest $sAttr($sExpType)} sMsg] != 0} {
            set sAttr($sExpType) [ mql print type "$sExpType" select attribute dump \t ]
         }
         if {$sPreType == $sExpType} {
            puts $p_file $sItem
         } else {
           puts $p_file "Type\tName\tRev\t\t\tPolicy\tState\tVault\tOwner\tdescription\t$sAttr($sExpType)\t<HEADER>"
           puts $p_file $sItem
         }
         set sPreType $sExpType
      }
      close $p_file
      puts $p_rel [join $lsItemRel \n]
      close $p_rel

# Secondary Rels

      set lsItem [concat [list $sItemOne] $lsItem]
      foreach sRelSec $lsRelSecondary {
         puts "Start backup of Secondary Rel \042$sRelSec\042 for Bus Object \042$sType\042 \042$sName\042 \042$sRev\042..."
   
         set lsItemBus {}
         set lsItemRel {}
         foreach sItem $lsItem {
            set lslsItem [split $sItem \t]
            set sExpType [lindex $lslsItem 0]
            set sExpName [lindex $lslsItem 1]
            set sExpRev [lindex $lslsItem 2]
            
            set slsStructure [mql expand bus "$sExpType" "$sExpName" "$sExpRev" rel "$sRelSec" recurse to 1 select bus name revision policy current vault owner description attribute.value select rel attribute.value dump \t recordsep <NEWRECORD>]
            regsub -all "\n" $slsStructure {<NEWLINE>} slsStructure
            regsub -all "<NEWRECORD>" $slsStructure  "\n" slsStructure
            set lsStructure [split $slsStructure \n]
            foreach slsStructure $lsStructure {
               if {$slsStructure != ""} {
                  set lslsStructure [split $slsStructure \t]
                  set sToType [lindex $lslsStructure 3]
                  set sToName [lindex $lslsStructure 4]
                  set sToRev [lindex $lslsStructure 5]
                  if {[catch {set iEndBus [expr 12 + $iAttr($sToType)]} sMsg] != 0} {
                     set iAttr($sToType) [llength [split [mql print type $sToType select attribute dump |] |] ]
                     set iEndBus [expr 12 + $iAttr($sToType)]
                  }
                  if {$sRelSecOption($sRelSec) == "bo" || $sRelSecOption($sRelSec) == "file"} {
                     lappend lsItemBus [join [lrange $lslsStructure 3 $iEndBus] \t]
                  }
                  set iAttrRel [llength [split [mql print bus "$sToType" "$sToName" "$sToRev" select attribute dump |] |] ]
                  set iEndRel [expr 12 + $iAttrRel]
                  if {$sRelSec != "Classified Item"} {
                     lappend lsItemRel [join [concat [list $sExpType $sExpName $sExpRev] [lrange $lslsStructure 3 5] [lindex $lslsStructure 2] [list $sRelSec] [lrange $lslsStructure [expr $iEndRel + 1] end]] \t]
                  } else {
                     lappend lsItemRel [join [concat [list $sExpType $sExpName $sExpRev] [lrange $lslsStructure 3 5] [lindex $lslsStructure 2] [list $sRelSec] [lrange $lslsStructure [expr $iEndRel + 1] end] [list "ON"]] \t]
                  }
                                       
                  # Grant Support
                  if {[mql print bus "$sToType" "$sToName" "$sToRev" select grantee dump |] != ""} {
                     pSet_Grant "$sToType" "$sToName" "$sToRev" "$sRelSec"
                  }
                  # End Grant Support - ION
            
               }
            }
         }
            
         if {$lsItemRel != {}} {
            set sFileRel [pSet_FileName $sType $sName $sRev $sRelSec "rel"]
            set p_filerel "$sDumpSchemaDirRelationships/$sFileRel\.xls"
            set p_rel [open $p_filerel w]
            set sAttrRel($sRelSec)  [ mql print relationship "$sRelSec" select attribute dump \t ]
            puts $p_rel "FromType\tFromName\tFromRev\tToType\tToName\tToRev\tDirection\tRelationship\t$sAttrRel($sRelSec)"
            puts $p_rel [join $lsItemRel \n]
            close $p_rel
            
            if {$lsItemBus != {}} {
               set bWriteFile FALSE
               if {$sRelSecOption($sRelSec) == "file"} { 
                  set sFileFile [pSet_FileName $sType $sName $sRev $sRelSec "file1"]
                  set p_filefile "$sDumpSchemaDirFiles/$sFileFile\.xls"
                  set p_file [open $p_filefile w]
                  puts $p_file "Type\tName\tRevision\tFormat\tFile\tPath"
               }
               set sFileBus [pSet_FileName $sType $sName $sRev $sRelSec "bo"]
               set p_filebus "$sDumpSchemaDirObjects/$sFileBus\.xls"
               set p_bus [open $p_filebus w]
               set lsItemBus [lsort -unique $lsItemBus]
               set sPreType ""
               foreach sItem $lsItemBus {
                  set lslsItem [split $sItem \t]
                  set sExpType [lindex $lslsItem 0]
                  set sExpName [lindex $lslsItem 1]
                  set sExpRev [lindex $lslsItem 2]
                  if {[catch {set sTest $sAttr($sExpType)} sMsg] != 0} {
                     set sAttr($sExpType) [ mql print type "$sExpType" select attribute dump \t ]
                  }
                  if {$sPreType == $sExpType} {
                     puts $p_bus $sItem
                  } else {
                     puts $p_bus "Type\tName\tRev\tChange Name\tChange Rev\tPolicy\tState\tVault\tOwner\tdescription\t$sAttr($sExpType)\t<HEADER>"
                     puts $p_bus $sItem
                  }
                  set sPreType $sExpType
                  if {$sRelSecOption($sRelSec) == "file"} {
                     set lsFile [split [mql print bus "$sExpType" "$sExpName" "$sExpRev" select format.file.name dump |] |]
                     set lsFormat [split [mql print bus "$sExpType" "$sExpName" "$sExpRev" select format.file.format dump |] |]
                     if {$lsFile != {}} {
                        set sFileDir [pSet_FileName $sExpType $sExpName $sExpRev "" "file2"]
                        set sDumpSchemaDirBO [ file join $sDumpSchemaDirFiles $sFileDir ]
                        file mkdir $sDumpSchemaDirBO
                        set bCheckin FALSE
                        foreach sFile $lsFile sFormat $lsFormat {
                           if {[catch {
                              mql checkout bus "$sExpType" "$sExpName" "$sExpRev" server file $sFile "$sDumpSchemaDirBO"
                              set sFilePath "./Objects/Files/$sFileDir/$sFile"
                              puts $p_file "$sExpType\t$sExpName\t$sExpRev\t$sFormat\t$sFilePath"
                              set bWriteFile TRUE
                              set bCheckin TRUE
                           } sMsg ] != 0} {
                              puts "WARNING: Error w/Bus Object \"$sExpType\" \"$sExpName\" \"$sExpRev\":\n$sMsg"
                              puts $p_errlog "Error w/Bus Object \"$sExpType\" \"$sExpName\" \"$sExpRev\":\n$sMsg\n"
                              set bChkErr TRUE                              
                           }
                        }
                        if {!$bCheckin} {file delete -force $sDumpSchemaDirBO}
                     }
                  }            
               }
               close $p_bus
               if {$sRelSecOption($sRelSec) == "file"} {
                  close $p_file
                  if {!$bWriteFile} {
                     file delete -force $p_filefile
                  }
               } 
            }
         }
      }
   }
   puts "\nFiles loaded in directory: $sSpinnerPath"
   if {$bChkErr} {
      close $p_errlog
      puts "Checkout errors recorded in log file $p_logfile"
   }
}

