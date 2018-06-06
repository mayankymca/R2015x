#
# $RCSfile: eServicecommonWzrdValidateComboEntry.tcl.rca $ $Revision: 1.17 $ 
#
#************************************************************************
# @progdoc    eServicecommonWzrdValidateComboEntry.tcl
#
# @Brief:    Validate program for Combo widgets.
#                
# @Description:  Perform validate check for widget to make sure that entry
#                          is from the list of choices provided by the Load program.
#                          By default, also allow null entry, but with option to require
#                          that a non-empty selection be made.
#
# @Parameters:  RPE 1 - Name of this widget; used to get value entered.   
#                          RPE 2 - Error message if entry is invalid
#                          RPE 3 - If equal to the string REQUIRED, then a value 
#                                       must be entered for this widget; otherwise a 
#                                       null entry is okay. 
#                          RPE 4 - Error message is no entry is given when one is required.
#
# @Returns:       0   if entry is valid
#                         1   if entry is not valid (Do not advance to next frame.)
#
# @Usage:         While this program could be used for any widget in a wizard
#                         it is probably only useful to validate the entry for combo widgets.
#
#
# @progdoc        Copyright (c) 1998, MatrixOne
#************************************************************************

###############################################################################
#                                                                             #
#   Copyright (c) 1998-2008 Matrix One, Inc.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

tcl;
eval {

###############################################################################
#
# Procedure:    utLoad
#
# Description:  Procedure to load other tcl utilities procedures.
#
# Parameters:   sProgram                - Tcl file to load
#
# Returns:      sOutput                 - Filtered tcl file
#               glUtLoadProgs           - List of loaded programs
#
###############################################################################

proc utLoad { sProgram } {

    global glUtLoadProgs env

    if { ! [ info exists glUtLoadProgs ] } {
        set glUtLoadProgs {}
    }

    if { [ lsearch $glUtLoadProgs $sProgram ] < 0 } {
        lappend glUtLoadProgs $sProgram
    } else {
        return ""
    }

    if { [ catch {
        set sDir "$env(TCL_LIBRARY)/mxTclDev"
        set pFile [ open "$sDir/$sProgram" r ]
        set sOutput [ read $pFile ]
        close $pFile

    } ] == 0 } { return $sOutput }

    set  sSearch "  code \'"
    set  sOutput [ mql print program $sProgram ]
    set  iStart  [ string first $sSearch $sOutput ]
    incr iStart  [ string length $sSearch ]
    set  iEnd    [ expr [ string last "\'" $sOutput ] -1 ]
    set  sOutput [ string range $sOutput $iStart $iEnd ]

    return $sOutput
}
# end utload 


###############################################################################
#
# Procedure:    replaceQuotesByBraces
#
# Description:  Procedure to process input line, replacing single quoted items
#                      by left and right braces
#
# Parameters:   quotedInput - a string containing the quoted input (handles no quotes okay)
#
# Returns:      outStr                 - the modified string
#               
#  Note: should be extracted as general string utility addition once fully qualified
###############################################################################

proc replaceQuotesByBraces { quotedInput } {
    # Go through and turn single-quoted pairs into braces, so the
    # input arguments can be processed through tcl okay
    set singleQuote {'}
    set leftBrace "{"
    set rightBrace "}"
    set len [string length $quotedInput]
    set subChar $leftBrace
    set outStr $quotedInput
    for  {set ind 0} {$ind < $len} {incr ind} {
       set char [string index $outStr $ind]
       if { $char != $singleQuote } { 
             continue 
       }
       set beforeInd [string range $outStr 0 [expr $ind - 1]]
       # Note if $ind is at the end, an empty string will be returned here
       set afterInd [string range $outStr [expr $ind + 1] end]
       set outStr $beforeInd$subChar$afterInd
       if { $subChar == $leftBrace } { 
             set subChar $rightBrace 
       } else {
             set subChar $leftBrace
       }
   }                             
   if { $subChar == $rightBrace } {
   # If it gets here something's wrong; return null string
       set outStr {}
   }
   return $outStr
}

######################################################################
#
# LOAD MQL/Tcl TOOLKIT LIBRARIES.
#
######################################################################
# Always load DEBUG.tcl
   eval  [utLoad eServicecommonDEBUG.tcl]
   eval  [utLoad eServicecommonShadowAgent.tcl ]
######################################################################
   #
   # Debugging trace
     mxDEBUGIN "eServicecommonWzrdValidateComboEntry"

     set date [clock format [clock seconds] -format "%m-%d-%y: %I:%M" ]
     DEBUG "DATE/TIME $date"

   set status -1
   set singleQuote {'}
   set doubleQuote {"}

   # Name of this widget; used to get the selection value
   set widgetName [mql get env 1]
   set selection [mql get env $widgetName]
   set invalidMessage [mql get env 2]
   DEBUG "widgetName = $widgetName"
   if { [string length $invalidMessage] <= 0 } {
      set invalidMessage "Invalid entry."
   }
   DEBUG "invalidMessage = $invalidMessage"

   # Argument, which if = REQUIRED, disallows null entry
   set required [mql get env 3]
   set noEntryMessage [mql get env 4]
   if { [string length $noEntryMessage] <= 0} {
       set noEntryMessage "A required entry/selection is missing."
   }
   DEBUG "required = $required"
   DEBUG "noEntryMessage = $noEntryMessage"

   # If no selection was made check whether the REQUIRED option is set
   if { [string length $selection] == 0 } {
      if { $required == "REQUIRED" } {
         set status 1
         mql warning $noEntryMessage
      } else {
         set status 0
      }
   } 

   # If status not yet determined, check whether selection is from choices
   # generated by the load program. These are stored in a list which is the
   # value of the RPE variable that has the name of the load program.
   if { $status == -1 } {
      set status 1
      set wizName [string trim [ mql get env WIZARDNAME ]]
      set frameName [string trim [mql get env FRAMENAME]]
      # thisFrame/Widget will be set to 1 once the current frame/widget
      # are located in print wiz output
      set thisFrame 0
      set thisWidget 0
      set frameLen [string length "frame"]
      set nameLen [string length "name"]
      set widgeLen [string length "widget"]
      # Print out the wizard info
      set wizInfo [mql print wizard $wizName]

      # Search backwards to get the part of the string starting with this Frame
      while { [set frameInd [string last "frame " $wizInfo]] >= 0 } {
           set ind [expr $frameInd + $frameLen]
           set afterFrame [string range $wizInfo $ind end]
           set afterFrame [string trimleft $afterFrame]
           # Is this a reference to the current frame?
           set thisFrame [string match "$frameName*" $afterFrame]
           if { $thisFrame } {
               set wizInfo [string range $afterFrame [string length $frameName] end]
               break
           }
           set wizInfo [string range $wizInfo 0 [expr $frameInd - 1]]
      }
     if { $thisFrame } {
          while { [set nameInd [string last "name " $wizInfo]] >= 0 } {
                # Index of "widget" corresponding to this name
                set startWidget [string last "widget" $wizInfo]
                # Should find  "widget"; otherwise, something's amiss
                if { $startWidget < 0 } {
                    mql warning "Format error in print wizard $wizName"
                    break
                }
                set ind [expr $nameInd + $nameLen]
                set afterName [string range $wizInfo $ind end]
                set afterName [string trimleft $afterName]
                # Is this a reference to the current widget?
                set thisWidget [string match "$widgetName*" $afterName]
                if { $thisWidget } {
                     set wizInfo [string range $wizInfo $startWidget end]
                     break
                }   
                set wizInfo [string range $wizInfo 0 [expr $startWidget - 1]]
          }     
      }
      if { $thisWidget } {
          set mqlret 0
          set valueInd [string first "value" $wizInfo]
          set loadInd [string first "load" $wizInfo]

          # load has priority; if it is >= 0 then pick up its values     
          if { $loadInd >= 0 } {
              set ind [expr $loadInd + [string length "load"] ]                            
              set afterLoad [string range $wizInfo $ind end]
              set afterLoad [string trimleft $afterLoad]
              set wordTerm [string first " " $afterLoad]
              if { $wordTerm <= 0 } {
                  set wordTerm [string first "\t" $afterLoad]
              }
              if { $wordTerm <= 0 } {
                  set status 1
              }
              set loadProgName [string range $afterLoad 0 [expr $wordTerm - 1]]
              set afterProg [string range $afterLoad $wordTerm end]
              set afterProg [string trimleft $afterProg]
              set ind [string first "input" $afterProg]
              set inputArgs {}
              if {$ind >= 0} {
                  incr ind [string length "input"]
                  set afterInput [string range $afterProg $ind end]
                  set afterInput [string trimleft $afterInput]
                  set ind [string first  "\n" $afterInput]
                  if {$ind >= 0 } {
                      incr ind -1
                      set afterInput [string range $afterInput 0 $ind]
                      set afterInput [string trim $afterInput]
                      set ind [expr [string length $afterInput] - 2]
                      if { $ind >= 0 } {
                         # Remove outer quotes
                         set afterInput [string range $afterInput 1 $ind]

                         # Go through and turn single-quoted pairs into braces, so the
                         # input arguments can be processed through tcl okay
                           # If null return, unable to match up quotes;  treat as if catch had failed 
                           set inputArgs [replaceQuotesByBraces $afterInput]
                           if { [string length $inputArgs] <= 0 } {
                                set mqlret 1
                           }
                         }
                      }
                  }                                        
               set loadCmd "mql execute program \{$loadProgName\} \{$widgetName\}"
               set space { }
               foreach arg $inputArgs {
                    set loadCmd $loadCmd$space\{$arg\}
               }
               if { $mqlret == 0 } {
                    pushShadowAgent
                    DEBUG "loadCmd = $loadCmd"
                    mql unset env $widgetName
                    set mqlret [catch {eval $loadCmd} ]
                    set defaultSelection {}
                    set defaultSelection [mql get env $widgetName]
                    # Make sure that user selection is reset
                    mql set env $widgetName $selection
                    popShadowAgent
               }
               if { $mqlret != 0 } {
                   mql warning "Business Admin Error: Unable to validate field $widgetName"
                   # Issue warning, but allow advancement to next frame
                   set status 0
               } else {
                   set loadChoices {}
                   set loadChoices [mql get env $loadProgName]
                   DEBUG "loadChoices = $loadChoices, default = $defaultSelection, selection = $selection"
                   if { [lsearch $loadChoices $selection] >= 0 || $selection == $defaultSelection } {
                       set status 0              
                }
            }
          } else {
             if { $valueInd >= 0 } {
                set ind [expr $valueInd + [string length "value"] ]                            
                set afterValue [string range $wizInfo $ind end]
                set afterValue [string trimleft $afterValue]
                set values {}
                set ind [string first $singleQuote $afterValue]
                incr ind
                set afterValue [string range $afterValue $ind end]
                set ind [string first "\n" $afterValue]
                incr ind -1
                set afterValue [string range $afterValue 0 $ind]
                set ind [string last $singleQuote $afterValue]
                incr ind -1
                set afterValue [string range $afterValue 0 $ind]              
                if { [lsearch $afterValue $selection] >= 0 } {
                     set status 0
                }
             }
          }
          if { $status == 1 } {
             mql warning $invalidMessage
          }
      }
   }
   
   DEBUG "status = $status"

   # Debugging trace
      mxDEBUGOUT "appWzrdValidateComboEntry"

   exit $status
}


