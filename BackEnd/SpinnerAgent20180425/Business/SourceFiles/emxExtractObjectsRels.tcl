#*******************************************************************************
# @progdoc        emxExtractObjectsRels.tcl v6R2014x
#
# @Brief:         Generates business object and/or connection spinner files.
#
# @Description:   Generates business object and/or connection spinner files.
#
# @Parameters:    Business Object Name, Relationship Name
#
# @Returns:       Nothing   
#
# @Usage:         Run in MQL:
#		  exec prog emxExtractObjectsRels.tcl "Type 1,Type N" "Rel 1,Rel N"
#
#    Valid Examples:
#    a) Administrative lists	exec prog emxExtractObjectsRels.tcl
#    b) one type  - one rels	exec prog emxExtractObjectsRels.tcl "ECO" "New Part / Part Revision"
#    c) type list - no rels  	exec prog emxExtractObjectsRels.tcl "Part,ECO,ECR" ""
#    d) no types  - rel list	exec prog emxExtractObjectsRels.tcl "" "Ref*"
#    e) type list - rel list	exec prog emxExtractObjectsRels.tcl "EC*" "Ref*,EBOM"
#    f) bus objects by TNR    exec prog emxExtractObjectsRels.tcl "EC*|ECR-123*|*"
#
#    To generate templates only, add 'template' as the third parameter:
#	                        exec prog emxExtractObjectsRels.tcl "*" "*" template
#    To include revision chains, add 'revision' as the third parameter:
#	                        exec prog emxExtractObjectsRels.tcl "Part|Bolt|*" "" revision
#    To not include sub-types in queries, specify 'exact' as the fourth parameter:
#                         exec prog emxExtractObjectsRels.tcl "Part|Bolt|*" "" revision exact
#    To limit vaults, add vaults in comma delimited list as the fifth parameter:
#                         exec prog emxExtractObjectsRels.tcl "Part|Bolt|*" "" revision "" "eService Production,eService Admin" 
#    To checkout files, add 'file' as the sixth parameter:
#                         exec prog emxExtractObjectsRels.tcl "Part|Bolt|*" "" revision "" "" file
#    To include file data only (no checkouts), add 'filedata' as the sixth parameter:
#                         exec prog emxExtractObjectsRels.tcl "Part|Bolt|*" "" revision "" "" filedata
#
# @progdoc        Copyright (c) 2009, ENOVIA
#*******************************************************************************
# @Modifications:
#
# Matt Osterman 01/04/2005 - Originated
# Venkatesh Harikrishnan 04/04/2006 - Modified for the New Line Issue
# Matt Osterman 10/25/2006 - Added TNR capability for bus objects
# MJO 10/16/2008 - Connections procedure (pGet_BOAdminRel) rewritten for V6 enhancements
# TQV 2009-07-03 - Modification allowing column data to be correctly aligned with column header.
# YUE 07/10/2009 - Connections procedure (pGet_BOAdminRel) will use recursive procedures 
#                  (pGet_BOAdminConstructRels/pGet_BOAdminConstructTypes) to fix issues with complex b2r, r2b & r2r structures.
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

################################################################################
#                   Business Objects
################################################################################
#*******************************************************************************
# Procedure:   pGet_BOAdmin
#
# Description: print details about the admin type and create spreadsheets
#
# Returns:     None.
#*******************************************************************************
   proc pGet_BOAdmin { sList } {
      global sDumpSchemaDirObjects bTemplate bRevision bSubType slsVault bFileData bExternal sExtFileName
      set sSettings "\tOverride Settings\tCreate Trigger\tModify Trigger\tDelete Trigger"
	  #KYB Start - Modified for Overlay mode
	  set seServiceAttributeList ""
	  set seServiceTriggerProgramParamAttributeList "eService Sequence Number\teService Target States\teService Program Name\teService Method Name\teService Constructor Arguments\teService Program Argument 1\teService Program Argument 2\teService Program Argument 3\teService Program Argument 4\teService Program Argument 5\teService Program Argument 6\teService Program Argument 7\teService Program Argument 8\teService Program Argument 9\teService Program Argument 10\teService Program Argument 11\teService Program Argument 12\teService Program Argument 13\teService Program Argument 14\teService Program Argument 15\teService Program Argument Desc 1\teService Program Argument Desc 2\teService Program Argument Desc 3\teService Program Argument Desc 4\teService Program Argument Desc 5\teService Program Argument Desc 6\teService Program Argument Desc 7\teService Program Argument Desc 8\teService Program Argument Desc 9\teService Program Argument Desc 10\teService Program Argument Desc 11\teService Program Argument Desc 12\teService Program Argument Desc 13\teService Program Argument Desc 14\teService Program Argument Desc 15\teService Error Type"
	  
	  set seServiceObjectGeneratorAttributeList "eService Name Prefix\teService Name Suffix\teService Retry Delay\teService Retry Count\teService Processing Time Limit\teService Safety Policy\teService Safety Vault"
	  
	  set seServiceNumberGeneratorAttributeList "eService Next Number"
	  #KYB End - Modified for Overlay mode

      foreach sList1 $sList {
         set lsTNR [split $sList1 |]
         set sType [string trim [lindex $lsTNR 0]]
       	 set lsType [split [mql list type $sType] \n]
         set sName [string trim [lindex $lsTNR 1]]
         set sRev  [string trim [lindex $lsTNR 2]]
		 set sPrevType  [ string trim [ lindex $lsTNR 3 ] ]
         set sPrevName  [ string trim [ lindex $lsTNR 4 ] ]
		 set beServiceObj FALSE
				
         if {$sName == ""} {set sName "*"}
         if {$sRev == ""} {set sRev "*"}
         set bSkip FALSE 
         if {[string first "|" $sList1] > 0 && !$bExternal} {
            puts "Start backup of Business Object $sList1 ..."
            set bSkip TRUE
         }
         foreach sType $lsType {        
		 
			if {$sType == "eService Trigger Program Parameters"} {
				set seServiceAttributeList $seServiceTriggerProgramParamAttributeList
				set beServiceObj TRUE
			} elseif {$sType == "eService Object Generator"} {
				set seServiceAttributeList $seServiceObjectGeneratorAttributeList
				set beServiceObj TRUE
			} elseif {$sType == "eService Number Generator"} {
				set seServiceAttributeList $seServiceNumberGeneratorAttributeList
				set beServiceObj TRUE
			}
		 
# skip if abstract
            set sAbst [mql print type "$sType"]
            set i [ regsub "abstract true" $sAbst "abstract true" sCheck ]
            if {$i > 0} {
               set bFlag FALSE
            } else {
               set bFlag TRUE
            }
            if {$bFlag} {
               if {!$bSkip && !$bExternal} {
                  puts "Start backup of business object type $sType ..."
               }

               if {$bTemplate != "TRUE"} {               
                  set attributeList [ split [ mql print type "$sType" select attribute dump \n] \n ]
                  set sFormattedAttributeValueList ""
                  set sFormattedAttributeList ""
				  
				  if {$beServiceObj} { set attributeList [ split $seServiceAttributeList \t] }
   
                  foreach sAttribute $attributeList {
                  	 set sFormattedAttributeValueList [concat $sFormattedAttributeValueList {attribute\[} ]
                  	 append sFormattedAttributeValueList $sAttribute
                  	 append sFormattedAttributeValueList {\].value}
                  }         
                  
                  foreach sAttribute $attributeList {
                  	 append sFormattedAttributeList \t$sAttribute
                  }				  
				  
				  if {$beServiceObj} {
					set sFormattedAttributeList \t$seServiceAttributeList
				  }
# 2009-07-03 : TQV : Modification allowing fixed column output
# 361393 - Revision Chains - MJO - 10/09/08
                  if {$bRevision} {
                     if {$bSubType} {
                        set sCmd " mql temp query bus \"$sType\" \"$sName\" \"$sRev\" vault \"$slsVault\" select previous name revision policy current vault owner description $sFormattedAttributeValueList dump \\\t recordsep <NEWRECORD>"
                     } else {
                        set sCmd " mql temp query bus \"$sType\" \"$sName\" \"$sRev\" vault \"$slsVault\" where \"type == '$sType'\" select previous name revision policy current vault owner description $sFormattedAttributeValueList dump \\\t recordsep <NEWRECORD>"
                     }                   
                  } else {
                     if {$bSubType} {
                        set sCmd " mql temp query bus \"$sType\" \"$sName\" \"$sRev\" vault \"$slsVault\" select name revision policy current vault owner description $sFormattedAttributeValueList dump \\\t recordsep <NEWRECORD>"
                     } else {
                        set sCmd " mql temp query bus \"$sType\" \"$sName\" \"$sRev\" vault \"$slsVault\" where \"type == '$sType'\" select name revision policy current vault owner description $sFormattedAttributeValueList dump \\\t recordsep <NEWRECORD>"
                     }
                  }
# End 361393
# 2009-07-03 : TQV : END : Modification allowing fixed column output

# START-For Revision chain                
				if { "$sPrevType" == "" || "$sPrevName" == ""} {
                  } elseif { "$sRev" == "$sPrevRev" && "$sType" == "$sPrevType" && "$sName" == "$sPrevName" } {
                  } elseif { [mql print bus "$sType" "$sName" "$sRev" select previous dump] != "" } {
                  } elseif {[mql print bus "$sPrevType" "$sPrevName" "$sPrevRev" select exists dump] != "TRUE"} {
                  } else {
                     set sCurId [mql print businessobject "$sType" "$sName" "$sRev" select id dump]
                     set lsPrev [split [mql print businessobject "$sPrevType" "$sPrevName" "$sPrevRev" select id next dump |] |]   
                     set sPrevId       [ string trim [ lindex "$lsPrev" 0 ] ]
                     set sPrevNextRev  [ string trim [ lindex "$lsPrev" 1 ] ]
					 
                     if {$sPrevNextRev != ""} {
                     } else {
                        set sCmd  "mql revise bus $sPrevId bus $sCurId"
                        if {$bScan} {
                           puts $iLogFileId $sCmd
                        } elseif {[catch {eval $sCmd} sResult] != 0} {
                           puts $iLogFileId "$sCmd"
						   puts $iLogFileId "\n\"$sType\" \"$sName\" \"$sRev\" - error in connecting revision chain: $sResult - triggers off"
                           if {$bSpinnerAgent} {
                              set iLogFileErr [open $sLogFileError a+]
                              puts $iLogFileErr "\"$sType\" \"$sName\" \"$sRev\" - error in connecting revision chain : $sResult - triggers off"
                              close $iLogFileErr
                           }
                           set bError TRUE
                        } else {
                           puts $iLogFileId "$sCmd"
                           puts $iLogFileId "# \"$sType\" \"$sName\" \"$sRev\": revision chain connected - triggers off"
                           set bMod TRUE
                           incr iRevChainTotal
                        }
                     }
                  }  
# END

                  if { [ catch { eval $sCmd } sOutstr ] == 0 } {
                     regsub -all "\n" $sOutstr {<NEWLINE>} sOutstr
                     regsub -all "<NEWRECORD>" $sOutstr  "\n" sOutstr
                     regsub -all "\134\134" $sOutstr "<BACKSLASH>" sOutstr
                     if {$sOutstr != ""} {
                        if {!$bExternal} {
                           set sFileName "bo_$sType\_$sName\_$sRev"
                           regsub -all "\134\174" $sFileName "_" sFileName
                           regsub -all "\134\052" $sFileName "ALL" sFileName
                           regsub -all "/" $sFileName "SLASH" sFileName
                           regsub -all ":" $sFileName "COLON" sFileName
                           regsub -all "<" $sFileName "LTHAN" sFileName
                           regsub -all ">" $sFileName "GTHAN" sFileName
                           set p_filename "$sDumpSchemaDirObjects/$sFileName\.xls"
                           set p_file [open $p_filename w]
                           if {$bRevision} {
							  set lBos "Type\tName\tRev\tPrevious Rev\tNew Name\tNew Rev \tPolicy\tState\tVault\tOwner\tdescription$sFormattedAttributeList$sSettings"
                           } else {      
							  set lBos "Type\tName\tRev\tNew Name\tNew Rev \tPolicy\tState\tVault\tOwner\tdescription$sFormattedAttributeList$sSettings"
                           }
						   puts $p_file "$lBos"
                           puts $p_file $sOutstr
                           close $p_file
                        } else {
                           append lBos "$sOutstr"
                        }
                     }
                  }
               } else {
                  set sFileName "bo_$sType"
                  regsub -all "\134\174" $sFileName "PYPE" sFileName
                  regsub -all "/" $sFileName "SLASH" sFileName
                  regsub -all ":" $sFileName "COLON" sFileName
                  regsub -all "<" $sFileName "LTHAN" sFileName
                  regsub -all ">" $sFileName "GTHAN" sFileName
                  set p_filename "$sDumpSchemaDirObjects/$sFileName\.xls"
                  set p_file [open $p_filename w]
                  set sAttr1  [ mql print type "$sType" select attribute dump \t ]
                  set lBos "Type\tName\tRev\tNew Name\tNew Rev\tPolicy\tState\tVault\tOwner\tdescription\t$sAttr1"
                  puts $p_file "$lBos"
                  close $p_file
               }
            }
         }
         if {$bExternal} {
            set p_filename "$sDumpSchemaDirObjects/$sExtFileName"
            set p_file [open $p_filename w]
            puts $p_file "Type\tName\tRev\tNew Name\tNew Rev\tPolicy\tState\tVault\tOwner\tdescription$sFormattedAttributeList$sSettings"
            puts $p_file "$lBos"
            close $p_file
         }
      }
   }

################################################################################
#                   Connections
################################################################################
#*******************************************************************************
# Procedure:   pGet_BOAdminRel
#
# Description: print details about the admin type and create spreadsheets
#
# Returns:     None.
#*******************************************************************************
   proc pGet_BOAdminRel { sList } {
      global sDumpSchemaDirRelationships bTemplate slsVault

      foreach sRel $sList {
         set sFromType [ mql print relationship $sRel select fromtype dump |]
         set sToType [ mql print relationship $sRel select totype dump |]
         if {[catch {
         set sFromRel [ mql print relationship $sRel select fromrel dump |]
         set sToRel [ mql print relationship $sRel select torel dump |]
         } sMsg ] != 0} {
            set sFromRel ""
            set sToRel ""
         }

         puts "Start backup of Relationship $sRel..."
         set sAttr1  [ mql print relationship "$sRel" select attribute dump \t ]
         set iAtt [regsub "\t" $sAttr1 "" sTest] 
         if {$iAtt > 0} {set iAtt [expr $iAtt + 1]}
         set iRel [expr $iAtt + 6]

         set sFormattedAttributeValueList ""
         set sFormattedAttributeList ""
         set attributeList [split $sAttr1 \t]
         foreach sAttribute $attributeList {
           set sFormattedAttributeValueList [concat $sFormattedAttributeValueList {attribute\[} ]
           append sFormattedAttributeValueList $sAttribute
           append sFormattedAttributeValueList {\].value}
           append sFormattedAttributeList \t$sAttribute
         }

         set sSettings "\tOverride Settings\tCreate Trigger\tModify Trigger\tDelete Trigger"
         if {$sFromType != "" && $sToType != ""} {
            set fname "rel-b2b_$sRel"
            regsub -all "/" $fname "_FWDSLASH_" fname 
            set p_filename "$sDumpSchemaDirRelationships/$fname\.xls"
            set lBos "Rel Name$sFormattedAttributeList\tfrom.type\tfrom.name\tfrom.revision\tto.type\tto.name\tto.revision$sSettings"
            set sQuery ""
            if {!$bTemplate} {  
               set sQuery "mql query connection type \"$sRel\" vault \"$slsVault\" select $sFormattedAttributeValueList from.type from.name from.revision to.type to.name to.revision dump \\\t recordsep \"<NEWRECORD>\""
               catch { eval $sQuery } sQuery
               regsub -all "\n" $sQuery {<NEWLINE>} sQuery
               set iRet [regsub -all "<NEWRECORD>" $sQuery  "\n" sQuery]
               if {$iRet > 0} {
                  set iTab [regsub -all "\t" $sQuery  "" sTest]
                  if {[expr $iTab / $iRet] < $iRel} {set sQuery ""}
               }
               append lBos "\n\$sQuery"
            } else {
               append lBos "\n\$sRel"
            }
            if {$bTemplate || $sQuery != ""} {
               set p_file [open $p_filename w]
               puts $p_file "$lBos"
               close $p_file
            }
         }

         if {$sFromRel != "" && $sToType != ""} {
            set fname "rel-r2b_$sRel"
            regsub -all "/" $fname "_FWDSLASH_" fname 
            set p_filename "$sDumpSchemaDirRelationships/$fname\.xls"

            set sSubHeader ""
            set sSubQuery  ""

            set sFromWhere ""
            if {$sFromRel != "all"} { 

              set lRecursive [ split [ pGet_BOAdminConstructRels $sRel "" "" ] \n ]
              set sSubHeader [ lindex $lRecursive 0 ]
              set sSubQuery  [ lindex $lRecursive 1 ]

              set lFromRels [ split $sFromRel | ]
              foreach sFromRel $lFromRels {
                if {$sFromWhere != ""} { append sFromWhere " || " }
                append sFromWhere "fromrel=='$sFromRel'"
              }
              if {$sFromWhere != ""} { set sFromWhere "where \"$sFromWhere\"" }
            }

            set sQuery "mql query connection type \"$sRel\" vault \"$slsVault\" $sFromWhere select $sFormattedAttributeValueList "
            set lBos "Rel Name$sFormattedAttributeList$sSubHeader$sSettings"
            if {!$bTemplate} {  
                append sQuery "$sSubQuery dump \\\t recordsep \"<NEWRECORD>\" "
                catch { eval $sQuery } sResult 
                regsub -all "\n" $sResult {<NEWLINE>} sResult
                set iRet [regsub -all "<NEWRECORD>" $sResult  "\n" sResult]
                if {$iRet > 0} {
                  set iTab [regsub -all "\t" $sResult  "" sTest]
                }
                append lBos "\n\$sResult"
            } else {
                append lBos "\n\$sRel"
            }
            if {$bTemplate || $sResult != ""} {
                set p_file [open $p_filename w]
                puts $p_file "$lBos"
                close $p_file
            }
         }

         if {$sFromType != "" && $sToRel != ""} {
            set fname "rel-b2r_$sRel"
            regsub -all "/" $fname "_FWDSLASH_" fname 
            set p_filename "$sDumpSchemaDirRelationships/$fname\.xls"

            set sSubHeader "\tfrom.type\tfrom.name\tfrom.revision"
            set sSubQuery  "from.type from.name from.revision"

            set sToWhere ""
            if {$sToRel != "all"} { 

              set lRecursive [ split [ pGet_BOAdminConstructRels $sRel "" "b2r" ] \n ]
              append sSubHeader [ lindex $lRecursive 0 ]
              append sSubQuery  " "  [ lindex $lRecursive 1 ]

              set lToRels [ split $sToRel | ]
              foreach sToRel $lToRels {
                if {$sToWhere != ""} { append sToWhere " || " }
                append sToWhere "torel=='$sToRel'"
              }
              if {$sToWhere != ""} { set sToWhere "where \"$sToWhere\"" }
            }

            set sQuery "mql query connection type \"$sRel\" vault \"$slsVault\" $sToWhere select $sFormattedAttributeValueList "

            set lBos "Rel Name$sFormattedAttributeList$sSubHeader$sSettings"
            if {!$bTemplate} {  
                append sQuery "$sSubQuery dump \\\t recordsep \"<NEWRECORD>\" "
                catch { eval $sQuery } sResult 
                regsub -all "\n" $sResult {<NEWLINE>} sResult
                set iRet [regsub -all "<NEWRECORD>" $sResult  "\n" sResult]
                if {$iRet > 0} {
                  set iTab [regsub -all "\t" $sResult  "" sTest]
                }
                append lBos "\n\$sResult"
            } else {
                append lBos "\n\$sRel"
            }
            if {$bTemplate || $sResult != ""} {
                set p_file [open $p_filename w]
                puts $p_file "$lBos"
                close $p_file
            }
         }

         if {$sFromRel != "" && $sToRel != ""} {
            set fname "rel-r2r_$sRel"
            regsub -all "/" $fname "_FWDSLASH_" fname 
            set p_filename "$sDumpSchemaDirRelationships/$fname\.xls"

            set sFromWhere ""
            if {$sFromRel != "all"} { 
              set lFromRels [ split $sFromRel | ]
              foreach sFromRel $lFromRels {
                if {$sFromWhere != ""} { append sFromWhere " || " }
                append sFromWhere "fromrel=='$sFromRel'"
              }
            }

            set sToWhere ""
            if {$sToRel != "all"} { 
              set lToRels [ split $sToRel | ]
              foreach sToRel $lToRels {
                if {$sToWhere != ""} { append sToWhere " || " }
                append sToWhere "torel=='$sToRel'"
              }
            }
            set sWhereClause "where \""
            if {$sFromWhere != ""} { append sWhereClause $sFromWhere }
            if {$sFromWhere != "" && $sToWhere != ""} { append sWhereClause " && " }
            if {$sToWhere   != ""} { append sWhereClause $sToWhere }
            append sWhereClause "\""
            if {$sWhereClause == "where \"\""} { set sWhereClause "" }

            set sQuery "mql query connection type \"$sRel\" vault \"$slsVault\" $sWhereClause select $sFormattedAttributeValueList "
            set lRecursive [ split [ pGet_BOAdminConstructRels $sRel "" "" ] \n ]
            set sSubHeader [ lindex $lRecursive 0 ]
            set sSubQuery  [ lindex $lRecursive 1 ]

            set lBos "Rel Name$sFormattedAttributeList$sSubHeader$sSettings"
            if {!$bTemplate} {  
                append sQuery "$sSubQuery dump \\\t recordsep \"<NEWRECORD>\" "
                catch { eval $sQuery } sResult 
                regsub -all "\n" $sResult {<NEWLINE>} sResult
                set iRet [regsub -all "<NEWRECORD>" $sResult  "\n" sResult]
                if {$iRet > 0} {
                  set iTab [regsub -all "\t" $sResult  "" sTest]
                }
                append lBos "\n\$sResult"
            } else {
                append lBos "\n\$sRel"
            }
            if {$bTemplate || $sResult != ""} {
                set p_file [open $p_filename w]
                puts $p_file "$lBos"
                close $p_file
            }
         }
      }
   }


################################################################################
#                 Recursive Connections
################################################################################
#*******************************************************************************
# Procedure:   pGet_BOAdminConstructRels
#
# Description: construct query recursively for multilevel connections
#
# Returns:     string Header + Query with \n seperator.
#*******************************************************************************
   proc pGet_BOAdminConstructRels { sRelName strRel strRelType } {
      global strFromRel strToRel
      set fromrel "fromrel."
      set torel "torel."

      set sHeader ""
      set sQuery  ""

      # check for multiple relationships
      # Example: fromrel of "EBOM Substitute" has EBOM|EBOM History
      set lRel [ split $sRelName | ]
      foreach sRelName $lRel {
         if {[catch {
           set sFromRel [ mql print relationship "$sRelName" select fromrel dump |]
           set sToRel   [ mql print relationship "$sRelName" select torel   dump |]
         } sMsg ] != 0} {
           set sFromRel ""
           set sToRel   ""
         }

         if {$sFromRel != "" && $sFromRel != "all"} {
           set lRecursive [ split [ pGet_BOAdminConstructRels $sFromRel "$strRel$fromrel" "" ] \n ]
           append sHeader "\t$strRel" "fromrel" [ lindex $lRecursive 0 ]
           append sQuery  " $strRel$fromrel" "name "  [ lindex $lRecursive 1 ]
         }
         ######### START Added by SL Team for spinner V6R2012 validation ##########
         if {$sToRel != "" && $sToRel != "all"} {
         ######### END ##########
           set lRecursive [ split [ pGet_BOAdminConstructRels $sToRel "$strRel$torel" "" ] \n ]
           append sHeader "\t$strRel" "torel" [ lindex $lRecursive 0 ]
           append sQuery  " $strRel$torel" "name "  [ lindex $lRecursive 1 ]
         }
      }

      if {$strRelType != "b2r"} {
         set lBus [ split [ pGet_BOAdminConstructTypes $sRelName $strRel ] \n ]
         append sHeader [ lindex $lBus 0 ]
         append sQuery  [ lindex $lBus 1 ]
      }

      return $sHeader\n$sQuery
   }

################################################################################
#                 Recursive Connections
################################################################################
#*******************************************************************************
# Procedure:   pGet_BOAdminConstructTypes
#
# Description: construct query recursively for multilevel connections
#
# Returns:     string Header + Query with \n seperator.
#*******************************************************************************
   proc pGet_BOAdminConstructTypes { sRelName strRel } {
      set sHeader ""
      set sQuery  ""

      set sFromType [ mql print relationship "$sRelName" select fromtype dump |]
      set sToType   [ mql print relationship "$sRelName" select totype   dump |]

      if {$sFromType != ""} {
        append sHeader "\t$strRel" "from.type\t$strRel" "from.name\t$strRel" "from.revision"
        append sQuery $strRel "from.type " $strRel "from.name " $strRel "from.revision "
      }

      if {$sToType != ""} {
        append sHeader "\t$strRel" "to.type\t$strRel" "to.name\t$strRel" "to.revision"
        append sQuery $strRel "to.type " $strRel "to.name " $strRel "to.revision "
      }

      return $sHeader\n$sQuery
   }
# end of procedures

   #KYB Start - Unable to specify a directory to create the spinner files on extract
   #set sSpinnerPath [mql get env SPINNERPATHBO]
   set sSpinnerPath [mql get env SPINNERPATH]
   #KYB End - Unable to specify a directory to create the spinner files on extract
   set sBusType [mql get env 1]
   set sRelType [mql get env 2]
   set sTemplate [string tolower [string trim [mql get env 3]]]
   set bTemplate FALSE
# 361393 - Revision Chains - MJO - 10/09/08
   set bRevision FALSE
   set bExternal FALSE
   if {$sTemplate == "template"} {
      set bTemplate TRUE
   } elseif {$sTemplate == "revision"} {
      set bRevision TRUE
   } elseif {$sTemplate == "external"} {
      set bExternal TRUE
   }
# End 361393
   set bSubType TRUE
   set sSubType [mql get env 4]
   if {[string tolower $sSubType] == "exact"} {set bSubType FALSE}
   set slsVault [mql get env 5]
   if {$slsVault == ""} {set slsVault "*"}
   set sFileData [string tolower [string trim [mql get env 6]]]
   set bFileData FALSE
   set bCheckout FALSE
   if {$sFileData == "file" || $sFileData == "filedata"} {
      set bFileData TRUE
   }
   if {$sFileData == "file"} {
      set bCheckout TRUE
   }
   set sExtFileName ""
   if {$bExternal} {
      set sFilter [mql get env 7]
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
      set sExtFileName "bo_eServiceTrigProgParams$sAppend.xls"
   }
   
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix";
      }
   }

   set sDumpSchemaDirObjects [ file join $sSpinnerPath Objects ]
   file mkdir $sDumpSchemaDirObjects
   if {$bFileData} {
      set sDumpSchemaDirFiles [ file join $sSpinnerPath "Objects/Files" ]
      file mkdir $sDumpSchemaDirFiles
   }
   set sDumpSchemaDirRelationships [ file join $sSpinnerPath Relationships ]
   file mkdir $sDumpSchemaDirRelationships
   
   if {$sBusType == "" && $sRelType == ""} {
      set sBusType [join [split [mql list type eService*] \n] ,]
      set sRelType [join [split [mql list relationship eService*] \n] ,]
      if {[mql get env SPINNERPATHBO] == ""} {puts "\nAdministrative business objects & connections being extracted.\nFor specific business objects or connections, invoke program in this manner:\n  exec prog emxExtractObjectsRels.tcl \"Type 1,...,Type N\" \"Rel 1,...,Rel N\"\nNotes: Wildcards are allowed. For TNR, separate with '|'s (e.g. EC*|ECR-101*|* )\n  For templates, specify 'template' as 3rd parameter.\n  For revision chains, specify 'revision' as 3rd parameter.\n"}
   }
   
   if {$sBusType == ""} {
   } else {
      set lsType [split $sBusType ,]
      pGet_BOAdmin $lsType
   }

   if {$sRelType != ""} {
      if {$sRelType == "*"} {
         set lsRelType [split [mql list relationship $sRelType] \n]
      } else {
         set lsRelType [split $sRelType ,]
      }
      set thelist [list ]
      foreach sRelType $lsRelType {
         set lsList [split [mql list relationship $sRelType] \n]
         set thelist [concat $thelist $lsList]
      }
      pGet_BOAdminRel $thelist
   }

   if {$bExternal} {
      puts "Trigger Program Parameters loaded in file: $sSpinnerPath/Objects/$sExtFileName"
   } else { 
      puts "\nFiles loaded in directory: $sSpinnerPath"
   }
}
