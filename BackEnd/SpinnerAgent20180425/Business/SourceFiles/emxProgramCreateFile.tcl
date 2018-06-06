###############################################################################
#
# $RCSfile: emxProgramCreateFile.tcl.rca $ $Revision: 1.11 $
#
# @progdoc     emxProgramCreateFile.tcl
#
# @Brief:      This program will create a file with a Project objects child information.
#
# @Description: This program will be run as a method on a Project object.  It will be passed a filename, filepath, and
#                         the file delimiter.  The Project objects subtask information will be written to the filepath/filename
#                         provided.
#
# @Parameters:  
#               param1: filename to output data to
#               param2: pathname where to create the file
#               param3: the file delimiter to use
#               param4: print column headers
#               
#
# @Returns:     none
#
# @Usage:      
#
# @Example:     
#               
#               
#
# @progdoc      Copyright (c) 2001, Matrix One, Inc. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
#
###############################################################################


tcl;

#--------------------
# Procedure Section
#--------------------
proc pFormatDate { sDate } { 
 if {$sDate != "" } {
   set sDate  [clock format [clock scan $sDate ] -format "%m/%d/%y"]
 } 
 return $sDate
}  

proc debug { str } { # puts $str } 
#--------------------
#  Main Program
#--------------------
eval {

  mql verbose off;
  mql quote off;

  # Load in the utLoad procedure and other libraries.
  eval [ mql print prog eServicecommonUtil.tcl select code dump ]
  eval [ utLoad eServicecommonShadowAgent.tcl ]
  eval [ utLoad eServicecommonDEBUG.tcl ]
  
  # Load the schema mapping program.
  set sRegProgName "eServiceSchemaVariableMapping.tcl"
  eval [ utLoad $sRegProgName ]
  
  
  # Load all property definitions in the global RPE 
  LoadSchemaGlobalEnv  "eServiceSchemaVariableMapping.tcl"; 

  #-- must push and pop shawdow agent

  # Obtain property values
  set sAttrTaskEstStartDate [mql get env attribute_TaskEstimatedStartDate]
  set sAttrTaskEstFinishDate [mql get env attribute_TaskEstimatedFinishDate]
  set sAttrTaskEstDuration [mql get env attribute_TaskEstimatedDuration]
  set sAttrTaskActualStartDate [mql get env attribute_TaskActualStartDate]
  set sAttrTaskActualFinishDate [mql get env attribute_TaskActualFinishDate]
  set sAttrTaskActualDuration [mql get env attribute_TaskActualDuration]

  set sAttrFirstName [mql get env attribute_FirstName]
  set sAttrMiddleName [mql get env attribute_LastName]
  set sAttrLastName [mql get env attribute_MiddleName]
  
  set sAttrDependecyType [mql get env attribute_DependencyType]  
  set sRelSubtask [mql get env relationship_Subtask]
  set sRelDependency [mql get env relationship_Dependency]
  set sRelAssignee [mql get env relationship_AssignedTasks]
  
  # Get the object.
  set sProjectName  "\"[ mql get env NAME ]\""
  set sObjectId  [ mql get env OBJECTID ]

  # Get Parameters
  # set sFilename [mql get env 1]
  # set sPathname [mql get env 2]
  # set sDelimiter  [mql get env 3]
  # set sHeader [string tolower [string trim [mql get env 4]]]


  # New way - page is return the fields in one parameter delimited by | 

  set param [split [mql get env 1] |]

  set sFilename [lindex $param 0]
  set sPathname [lindex $param 1]
  set sDelimiter  [lindex $param 2]
  set sHeader [string tolower [string trim [lindex $param 3]]]
  set bStandard [string tolower [string trim [lindex $param 4]]]
  set lAttributes [string tolower [string trim [lindex $param 5]]]


  
  debug " sFilename: $sFilename "
  debug "  sPathname: $sPathname "
  debug "  sDelimiter: $sDelimiter  "
  debug "  sHeader: $sHeader "
  debug "  bStandard: $bStandard "
  debug "  lAttributes: $lAttributes "


  # Set default date format - this does not work
  # mql set env global MX_NORMAL_DATE_FORMAT moy/dom/yr4

  # Validate parameters
  if { "$sFilename" == "" && "$sPathname" == "" && "$sDelimiter" == "" && "$sHeader" == "" } { 
   mql notice "Invalid parameters passed to program"
   return 1
   exit 1
  }

  if { "$sFilename" == "" } { 
   set sFilename "temp.csv"
  }
  
  if { "$sPathname" == "" || [file exists $sPathname] == 0} { 
   mql notice "Invalid Path name passed to this program"
   return 1
   exit 1
     set sPathname "c:/temp"
  } 


  set sDelimiter [string tolower [string trim $sDelimiter] ]

  if {  "$sDelimiter" == "comma" } {
    set sDelimiter ","
  } 
  if {  "$sDelimiter" == "tab" } {
    set sDelimiter "\t"
  } 

  if { "$sDelimiter" == "" } { 
     set sDelimiter ","
  } elseif { "$sDelimiter" != ","  && "$sDelimiter" != "\t" } {
     set sDelimiter "\t"
  } 

  if { "$sHeader" == ""  } { 
     set sHeader "yes"
  } 

  if { "$bStandard" == ""  && "$lAttributes" == ""   } { 
     set bStandard "true";
  }  
  
 if { "$bStandard" == "true"   } { 
  # Header Row
  set sColumnHeader "WBS"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Name"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Type"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Est Duration"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Est Start"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Est End"       
  set sColumnHeader "${sColumnHeader}${sDelimiter}Dependecy"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Assignee"
 }  else { 
  # Header Row
  set sColumnHeader "ID${sDelimiter}WBS${sDelimiter}Name"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Est Duration"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Est Start"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Est End"       
  set sColumnHeader "${sColumnHeader}${sDelimiter}Act Duration"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Act Start"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Act End"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Dependecy"
  set sColumnHeader "${sColumnHeader}${sDelimiter}Assignee"


  } 
  
  # Get the Project objects information
  set cDump "^"
      
  set sCmd {mql print bus "$sObjectId" select \
   attribute\[$sAttrTaskEstDuration\] \
   attribute\[$sAttrTaskEstStartDate\] \
   attribute\[$sAttrTaskEstFinishDate\] \
   attribute\[$sAttrTaskActualDuration\] \
   attribute\[$sAttrTaskActualStartDate\] \
   attribute\[$sAttrTaskActualFinishDate\] \
   type  owner\
   dump $cDump}
  set mqlret [catch {eval $sCmd} outStr] 
  


  
  if { $mqlret != 0 } {
      set sErrMsg $outStr
  } else {
      set iCnt 0
       catch { unset aWBS } 
      set aWBS(0)  "0"
      # Open the file for writing
      set sTempFile [open "$sPathname/$sFilename" w]
      # set sTempFile [open "c:/temp/$sFilename" w]


      if { "$sHeader" == "yes"  || "$sHeader" == "y" } { 
        puts $sTempFile "$sColumnHeader"
        flush $sTempFile
      } 
      # Write the Project information to the file
      set lPrjInfo [split $outStr $cDump]


debug "Project Info: $outStr" 


       # Reformat the dates
       
       set sEstDuration "[lindex $lPrjInfo 0] days"
       set sEstStart [pFormatDate [lindex $lPrjInfo 1] ]
       set sEstEnd   [pFormatDate [lindex $lPrjInfo 2] ]
       set sActDuration [lindex $lPrjInfo 3] 
       set sActStart   [pFormatDate [lindex $lPrjInfo 4] ]
       set sActEnd  [pFormatDate [lindex $lPrjInfo 5] ]
       set sProjectType ""
       
       set sOutputStr ""
      if { "$bStandard" == "true"   } { 
       append sOutputStr "${iCnt}${sDelimiter}${sProjectName}"
       append sOutputStr "${sDelimiter}${sProjectType}"
       append sOutputStr "${sDelimiter}${sEstDuration}"
       append sOutputStr "${sDelimiter}${sEstStart}"
       append sOutputStr "${sDelimiter}${sEstEnd}"       
      } else { 
       append sOutputStr "${iCnt}${sDelimiter}0${sDelimiter}${sProjectName}"
       append sOutputStr "${sDelimiter}${sEstDuration}"
       append sOutputStr "${sDelimiter}${sEstStart}"
       append sOutputStr "${sDelimiter}${sEstEnd}"       
       append sOutputStr "${sDelimiter}${sActDuration}"
       append sOutputStr "${sDelimiter}${sActStart}"
       append sOutputStr "${sDelimiter}${sActEnd}"
     } 
       puts $sTempFile "${sOutputStr}"

      # Expand the Project object to get its child subtask information
  
      set sCmd {mql expand bus "$sObjectId" from rel "$sRelSubtask" terse recurse to all select bus \
       name \
       attribute\[$sAttrTaskEstDuration\] \
       attribute\[$sAttrTaskEstStartDate\] \
       attribute\[$sAttrTaskEstFinishDate\] \
       attribute\[$sAttrTaskActualDuration\] \
       attribute\[$sAttrTaskActualStartDate\] \
       attribute\[$sAttrTaskActualFinishDate\] \
          type dump $cDump}
      set mqlret [catch {eval $sCmd} lChildrenTasks]

      if { $mqlret != 0 || [llength $lChildrenTasks] == 0 } {
          set sErrMsg $lChildrenTasks
      } else {

          set lChildrenTasks [split $lChildrenTasks \n]

	  # the first loop sets all the children id into an array for later
	  # array aWBS will be used a reference for Dependencies
	  set iCnt 1;
	  foreach sSubTaskInfo $lChildrenTasks {
               # Get the current subtasks info
               set lSubTask [split $sSubTaskInfo "$cDump"]
	       set sChildID [lindex $lSubTask 3] 
	       set aWBS($sChildID) $iCnt
	       incr iCnt
	  } 
	       

          # Go through child subtasks and write the information to the file
          set iCnt 1
          set iID 1
          set sPrevLevel "" 
          set iLevel1Cnt 0
          foreach sSubTaskInfo $lChildrenTasks {
               # Get the current subtasks info
               set lSubTask [split $sSubTaskInfo "$cDump"]
               set sLevel [lindex $lSubTask 0]
               set sChildID [lindex $lSubTask 3]
               set sName "\"[lindex $lSubTask 4]\""
               set sEstDuration "[lindex $lSubTask 5] days"
               set sEstStart [pFormatDate [lindex $lSubTask 6] ]
               set sEstEnd   [pFormatDate [lindex $lSubTask 7] ]
               set sActDuration [lindex $lSubTask 8]
               set sActStart   [pFormatDate [lindex $lSubTask 9] ]
               set sActEnd  [pFormatDate [lindex $lSubTask 10] ]
               set sType   [lindex $lSubTask 11] 
  
debug "Child $sLevel  Info: \n $lSubTask" 
debug "sLevel: $sLevel sChildID: $sChildID  sName: $sName"
	       
               # get Dependencies 
               set sCmd {mql expand bus "$sChildID" from rel "$sRelDependency" terse \
	       select rel attribute\[$sAttrDependecyType\] select bus name dump $cDump } 
	       set mqlret [catch {eval $sCmd} lDependencies ]
	       if { $mqlret == 1 } { 
	         set lDependencies "" 
	       } 
	       
	       set lDependencies [split $lDependencies \n]
	       # format dependencies using task ID
debug "lDependencies: $lDependencies"	       
	       set sStrDep ""
	       set sChar ""
	       foreach lDep $lDependencies {  
		   # Get the current subtasks info
                   set lDep [split $lDep "$cDump"]
	           set sDepID [lindex $lDep 3] 
		   set sDepType [lindex $lDep 5] 
		   set sID [lindex [array get aWBS $sDepID] 1]
                   if { "$sID" != "" } { 
		     append sStrDep "${sChar}${sID}${sDepType}"
		     set sChar ","
		   } 
	        }
		
		# required for comma delimited files
                if {"$sDelimiter" == "," && "$sStrDep" != "" } { 
	         set sStrDep "\"$sStrDep\""
	        } 	

               # get Assignee 
	       set sChar ","
               set sCmd {mql print bus "$sChildID" select to\[$sRelAssignee\].from.name dump $sChar } 
	       set mqlret [catch {eval $sCmd} sAssignee ]

	       # required for comma delimited files
	       if {"$sDelimiter" == "," && "$sAssignee" != "" && $mqlret == 0 } { 
	         set sAssignee "\"$sAssignee\""
	       } elseif { $mqlret == 1 } { 
	         set sAssignee "" 
	       }               

  

             # Calc WBS
             incr iID
             set sWBS [array names aWBS $sLevel]
             set sParent [array names aWBS [expr $sLevel -1]] 

              if { $sPrevLevel != $sLevel } { 
                 # Have to parse for last number of wbs to be able to incr it by one
                 if { ($sPrevLevel > $sLevel) && ($aWBS($sParent) != 0) } {
                     set list [split $aWBS($sWBS) "."]
                     set iID [lindex $list end]
                     incr iID
                     set sPrevLevel $sLevel
                 } else {
                     set sPrevLevel $sLevel
                     set iID 1; 
                 }
              }  

debug "\t sLevel: $sLevel " 
debug "\t sPrevLevel: $sPrevLevel " 
debug "\t iID: $iID " 
debug "\t Parent WBS: $sParent" 
debug "\t Current WBS: $sWBS " 


             if { $sLevel == 1 } { 
                   incr iLevel1Cnt
                   set aWBS($sLevel) "$iLevel1Cnt" 
                   set sWBS $iLevel1Cnt
              } 
              if { $sLevel > 1 } {  
                  set sParent "$aWBS($sParent)";
                  set sWBS "${sParent}.${iID}";
                  set aWBS($sLevel) "$sWBS" 
              }  

debug "\t iLevel1Cnt: $iLevel1Cnt " 
debug "\t NEW WBS: $sWBS " 
# debug "[parray aWBS]"

             # Change of WBS
              # if { $sPrevLevel != $sLevel } { 
              #   set sPrevLevel $sLevel
               #   set iID 1
               # }  
               
               # Format output 
              if { "$bStandard" == "true"   } { 
               set sOutputStr "${sWBS}${sDelimiter}${sName}"
               append sOutputStr "${sDelimiter}${sType}"
               append sOutputStr "${sDelimiter}${sEstDuration}"
               append sOutputStr "${sDelimiter}${sEstStart}"
               append sOutputStr "${sDelimiter}${sEstEnd}"        
               append sOutputStr "${sDelimiter}${sStrDep}"
  	         append sOutputStr "${sDelimiter}${sAssignee}"
            } else { 
             set sOutputStr "${iCnt}${sDelimiter}$sLevel${sDelimiter}${sName}"
               append sOutputStr "${sDelimiter}${sEstDuration}"
               append sOutputStr "${sDelimiter}${sEstStart}"
               append sOutputStr "${sDelimiter}${sEstEnd}"       
               append sOutputStr "${sDelimiter}${sActDuration}"
               append sOutputStr "${sDelimiter}${sActStart}"
               append sOutputStr "${sDelimiter}${sActEnd}"
               append sOutputStr "${sDelimiter}${sStrDep}"
  	         append sOutputStr "${sDelimiter}${sAssignee}"
            } 



               # Write the SubTask information to the file
               puts $sTempFile "${sOutputStr}"
               flush $sTempFile; 
  	         incr iCnt


          }; # foreach   

	  
      }; # mqlret != 0

      # Close the file
      close $sTempFile

  }; # mqlret != 0

  if { $mqlret != 0 } {
       regsub -all {\"} $sErrMsg {'} sErrMsg
       mql notice "$sErrMsg"
  }

  return $mqlret
}

