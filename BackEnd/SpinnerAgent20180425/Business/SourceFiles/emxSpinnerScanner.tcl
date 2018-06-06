#########################################################################2014x
#
# @progdoc      emxSpinnerScanner.tcl vM2013
#
# @Description: Sets program emxSpinnerAgent.tcl into scan mode.
#
# @Parameters:  None
#
# @Usage:       Run this program for an MQL command window w/data files in directories:
#               . (current dir)         emxSpinnerAgent.tcl, emxSpinnerAccess.tcl programs
#               ./Business              Spinner[SCHEMA]Data.xls data files
#               ./Business/SourceFiles  Database program files  
#               ./Business/Policy       Policy access data files from Bus Doc Generator program
#               ./Business/Rule         Rule access data files from Bus Doc Generator program        
#               ./Export/[SCHEMA TYPE]  Export files from Bus Doc Generator program
#
# @progdoc      Copyright (c) ENOVIA Inc., October 7, 2003
#
#########################################################################
#
# @Modifications: Matt Osterman 03/04/2005 - Setup for executable version
#
#########################################################################
tcl;

eval {
   if {[info host] == "mostermant43" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
      set cmd "debugger_eval"
      set xxx [debugger_init]
   } else {
      set cmd "eval"
   }
}
$cmd {

   set bExecute TRUE
   if {[mql get env 0] == ""} {set bExecute FALSE}

   set sLocation ""
   if {$bExecute && [mql list program emxSpinnerAgent.tcl] != ""} {
      set sLocation database
   } elseif {$bExecute != "TRUE" && [file exists "./emxSpinnerAgent.tcl"] == 1} {
      set sLocation filesystem
   } else {
      if {$bExecute} {
         puts "ERROR: Program execution halted. Program object 'emxSpinnerAgent.tcl' is missing from the database."
      } else {
         puts "ERROR: Program execution halted. Program 'emxSpinnerAgent.tcl' is missing from directory '[pwd]'."
      }      	
      exit 1
      return
   } 
   mql set env SPINNERSCANMODE TRUE
   if {$sLocation == "filesystem"} {
      mql run emxSpinnerAgent.tcl
   } else {
      mql exec prog emxSpinnerAgent.tcl
   }
}


