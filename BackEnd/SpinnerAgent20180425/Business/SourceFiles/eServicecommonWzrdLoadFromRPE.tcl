###############################################################################
#
# $RCSfile: eServicecommonWzrdLoadFromRPE.tcl.rca $ $Revision: 1.17 $ 
#
# Description:  This file defines the code for a Wizard program to load the
#                       names of business objects from a given RPE variable.
#               
#
# Dependencies: 
#
###############################################################################

###############################################################################
#                                                                             #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                 #
#   This program contains proprietary and trade secret information of         #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not     #
#   evidence any actual or intended publication of such program.              #
#                                                                             #
###############################################################################

tcl;
eval {

######################################################################
#
# DEFINE UTLOAD
#
######################################################################

proc utLoad { Program } {

    set  sSearch "  code \'"
    set  sOutput [ mql print program $Program ]
    set  iStart  [ string first $sSearch $sOutput ]
    incr iStart  [ string length $sSearch ]
    set  iEnd    [ expr [ string last "\'" $sOutput ] -1 ]
    set  sOutput [ string range $sOutput $iStart $iEnd ]

    return $sOutput
}

######################################################################
#
# LOAD MQL/Tcl TOOLKIT LIBRARIES.
#
#####################################################################
#
# Debugging procedures
#
  eval [ utLoad eServicecommonDEBUG.tcl ]
#
######################################################################
#
# MAIN
#
#  Input: RPE 0 = the name of the variable (widget) into which this function will put the 
#                          list of found objects
#            RPE 2= the name of the RPE variable which contains the list to be loaded 
#            RPE 3= If defined, the index of the list to be shown as the selected item;
#                         this must be defined to show an item in a textbox widget, since the
#                         list itself is not accessible
#
#
######################################################################

    #
    # Debugging trace - note entry
    #
    mxDEBUGIN "appWzrdLoadFromRPE"


  # return found objects in progname
  set progname [mql get env 0]
  set widget [mql get env 1]
  set variableName {}
  set selectName {}
  set variableName [mql get env 2]
  set selectIndex [mql get env 3]
  set status 1
  set status [ catch {eval mql get env $variableName} list ] 
  if { $status == 0 } {
      mql set env $progname $list  
  } else {
      mql warning "Unable to set choices for widget $widget in program $progname"
  }
  if { $status == 0 && [llength $selectIndex] > 0 } {
      if { $selectIndex >= 0 && [llength $list] > $selectIndex } {
          mql set env $widget [lindex $list $selectIndex]
      }
  }
    #
    # Debugging trace - note exit
    #
    mxDEBUGOUT "appWzrdLoadFromRPE"

  exit
}

