###############################################################################
#
# $RCSfile: eServicecommonWzrdListFromCommand.tcl.rca $ $Revision: 1.20 $
#
#************************************************************************
# @progdoc        eServicecommonWzrdListFromCommand.tcl
#
# @Brief:         Load widget or program attribute with any MQL command
#
# @Description:   Accept any MQL command that generates a delimited
#                 list and prepare that list to load either a Wizard widget
#                 or a program attribute
#
# @Parameters:    1 - Any mql command that produces a vertical-bar delimited
#                     list.
#		  2 - List of items to be added in output list.
#                 3 - The delimiter - default is end-of-line ("\n")
#                 4 - Optional selection from the list.  If this is not
#                     specified, the first item in the list will be selected.
#
# @Returns:       RPE settings for the list of names and the selected name.
#
# @Usage:         This program should be implemented as a load program
#                 for listboxes or comboboxes, or for program atributes,
#                 with input arguments as in the following examples.
#
#                 NOTE the use of {} as argument delimiters, thereby allowing
#                      a more MQL-like use of double quotes for named things.
#
#                 To get a list of types:
#                 input = {list type}
#
#                 To get a list of subtgroups of the Change Board group:
#                 input = {print group "Change Boards" select child dump |} {|}
#
#                 To get a list of members of the group Change Board 1:
#                 input = {print group "Sample Change Board 1" select person dump |} {|}
#
#                 To get a list of subtypes,
#                 input = {print type "Parent Type" select derivative dump |} {|}
#
#                 To get a list of ECO's with current state and an attribute
#                 value:
#                 input = {temp query bus  ECO * * select current attribute[Category of Change]  dump "     "}
#
#
# @progdoc        Copyright (c) 1998, MatrixOne
#************************************************************************

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

######################################################################
#
# LOAD MQL/Tcl TOOLKIT LIBRARIES.
#
######################################################################

    set RegProgName   "eServiceSchemaVariableMapping.tcl"

    # Load Utility function
    eval [utLoad $RegProgName]

    mql verbose off

    #set role_DesignEngineer [mql get env global role_DesignEngineer]
    mql quote off
    set sEvent [mql get env EVENT]

    # Program attributes arguments start at index 1
    if { $sEvent == "attribute choices" || $sEvent == "attribute check" } {
	set sProgname  [ mql get env 0 ]
	set sWidget ""
	set sCmd     [ mql get env 1 ]
	set sList    [ mql get env 2 ]
	set sDelim   [ mql get env 3 ]
	set sSelection ""
    #
    # Wizard load program arguments start at index 2
    } else {
	set sProgname  [ mql get env 0 ]
	set sWidget    [ mql get env 1 ]
	set sCmd       [ mql get env 2 ]
	set sList      [ mql get env 3 ]
	set sDelim     [ mql get env 4 ]
	set sSelection [ mql get env 5 ]
    }

    if { $sDelim == "" } {
	set sDelim "\n"
    }

    set lCmd [split $sCmd]
    foreach sWord $lCmd {
        if {[string index $sWord 0] == "\$"} {
            set sProp [string range $sWord 1 end]
            set "$sProp" [eServiceGetCurrentSchemaName type $RegProgName $sProp]
        }
    }

    regsub -all {\[} $sCmd {\\\[} sCmd
    regsub -all {\]} $sCmd {\\\]} sCmd
    set mqlret [catch \
	    {eval  mql $sCmd} sItems]
    if { $mqlret != 0 } {
	# Print group command failed - return nothing.
	set sItems ""
    }

    #
    # Sort them, then add one empty entry to allow for unsetting combos
    set lItems [ lsort -dictionary [ split $sItems $sDelim ] ]
    set lItems [ concat $lItems $sList ]

    # Program attributes: pass list back through a global env variable arg 0
    # There is no facility for specifying a default selection.
    #
    if { $sEvent == "attribute choices" || $sEvent == "attribute check" } {
        mql set env global $sProgname $lItems
    #
    # Wizard load programs: pass back to arg 0, but NOT global
    # Widgets DO allow a default.
    } else {
	mql set env $sProgname $lItems
	if { $sSelection == "" } {
	    mql set env $sWidget [lindex $lItems 0]
	} else {
	    mql set env $sWidget $sSelection
	}
    }

    # Only wizard widgets can set a selection
    #mql set env $sGroup $sSelection


# End eval
}

