###############################################################################
#
# $RCSfile: eServicecommonTranslation.tcl.rca $ $Revision: 1.39 $
#
# Description:  This file has all the functions to work with different languages
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

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utload

namespace eval eServiceTranslation {
    namespace export eServiceTranslationGetString eServiceTranslationSetString eServiceTranslationLocale eServiceTranslationGetPreferences eServiceTranslationUnknown

    # Records the current locale as passed to eServiceTranslationLocale
    variable eServiceLocale ""

    # Records the list of locales to search
    variable eServiceLoclist {}

    # Records the mapping between source strings and translated strings.  The
    # array key is of the form "<locale>,<namespace>,<src>" and the value is
    # the translated string.
    array set eServiceTranslationStrings {}
}

# eServiceTranslation::eServiceTranslationGetString --
#
# Find the translation for the given string based on the current
# locale setting.
#
# Arguments:
# src The string to translate.
#
# Results:
# Returns the translatd string.

proc eServiceTranslation::eServiceTranslationGetString {src} {
    set ns [uplevel {namespace current}]
    foreach loc $::eServiceTranslation::eServiceLoclist {
  if {[info exists ::eServiceTranslation::eServiceTranslationStrings($loc,$ns,$src)]} {
      return $::eServiceTranslation::eServiceTranslationStrings($loc,$ns,$src)
  }
    }
    # we have not found the translation
    return [uplevel 1 [list [namespace origin eServiceTranslationUnknown] \
      $::eServiceTranslation::eServiceLocale $src]]
}

# eServiceTranslation::eServiceTranslationLocale --
#
# Query or set the current locale.
#
# Arguments:
# newLocale (Optional) The new locale string. Locale strings
#     should be composed of one or more sublocale parts
#     separated by underscores (e.g. en_US).
#
# Results:
# Returns the current locale.

proc eServiceTranslation::eServiceTranslationLocale {args} {
    set len [llength $args]

    if {$len > 1} {
  error {wrong # args: should be "eServiceTranslationLocale ?newLocale?"}
    }

    set args [string tolower $args]
    if {$len == 1} {
  set ::eServiceTranslation::eServiceLocale $args
  set ::eServiceTranslation::eServiceLoclist {}
  set word ""
  foreach part [split $args _] {
      set word [string trimleft "${word}_${part}" _]
      set ::eServiceTranslation::eServiceLoclist \
                    [linsert $::eServiceTranslation::eServiceLoclist 0 $word]
  }
    }
    return $::eServiceTranslation::eServiceLocale
}

# eServiceTranslation::eServiceTranslationGetPreferences --
#
# Fetch the list of locales used to look up strings, ordered from
# most preferred to least preferred.
#
# Arguments:
# None.
#
# Results:
# Returns an ordered list of the locales preferred by the user.

proc eServiceTranslation::eServiceTranslationGetPreferences {} {
    return $::eServiceTranslation::eServiceLoclist
}

# eServiceTranslation::eServiceTranslationLoad --
#
# Attempt to load message catalogs for each locale in the
# preference list from the specified directory.
#
# Arguments:
# langdir   The directory to search.
#
# Results:
# Returns the number of message catalogs that were loaded.

proc eServiceTranslation::eServiceTranslationLoad {} {
    set x 0
    set lPrograms [split [mql list program] \n]
    foreach p [::eServiceTranslation::eServiceTranslationGetPreferences] {
  set langfile "eServicecommonSetTranslation${p}.tcl"
  if {[lsearch $lPrograms $langfile] >= 0} {
      incr x
      uplevel [list eval [utLoad $langfile]]
  }
    }
    return $x
}

# eServiceTranslation::eServiceTranslationLoadMessages --
#
#       Gets language from database and sets it.
# Attempt to load message catalogs for each locale in the
# preference list from the specified directory.
#
# Arguments:
#
# Results:
# Returns the number of message catalogs that were loaded.

proc eServiceTranslation::eServiceTranslationLoadMessages {} {
    set x 0
    # Get language from database
    set sCmd {mql print language}
    set mqlret [ catch {eval  $sCmd} outStr ]
    if {$mqlret != 0 || "$outStr" == ""} {
        ::eServiceTranslation::eServiceTranslationLocale en
    } else {
        set sLangLine [string trim [lindex [split "$outStr" \n] 0]]
        set sAllLang [string range [lindex [split $sLangLine] 1] 1 [expr [string length [lindex [split $sLangLine] 1]] - 2]]
        set sContLang [lindex [split $sAllLang ,] 0]
        set sLang [lindex [split $sContLang -] 0]
        ::eServiceTranslation::eServiceTranslationLocale "$sLang"
    }

    # Get all the customized and original messages.
    set lPrograms [split [mql list program] \n]
    foreach p [::eServiceTranslation::eServiceTranslationGetPreferences] {
  set langfile "emxcommonMessages_${p}.tcl"
  if {[lsearch $lPrograms $langfile] >= 0} {
      incr x
      uplevel [list eval [utLoad $langfile]]
  }
    }

    foreach p [::eServiceTranslation::eServiceTranslationGetPreferences] {
  set langfile "emxcommonCustomMessages_${p}.tcl"
  if {[lsearch $lPrograms $langfile] >= 0} {
      incr x
      uplevel [list eval [utLoad $langfile]]
  }
    }

    if {$x == 0} {
        incr x
        ::eServiceTranslation::eServiceTranslationLocale en
        uplevel [list eval [utLoad emxcommonMessages_en.tcl]]
    }
    return $x
}

# eServiceTranslation::eServiceTranslationSetString --
#
# Set the translation for a given string in a specified locale.
#
# Arguments:
# locale    The locale to use.
# src   The source string.
# dest    (Optional) The translated string.  If omitted,
#     the source string is used.
#
# Results:
# Returns the new locale.

proc eServiceTranslation::eServiceTranslationSetString {locale src {dest ""}} {
    if {$dest == ""} {
  set dest $src
    }

    set ns [uplevel {namespace current}]

    set ::eServiceTranslation::eServiceTranslationStrings([string tolower $locale],$ns,$src) $dest
    return $dest
}

# eServiceTranslation::eServiceTranslationUnknown --
#
# This routine is called by eServiceTranslation::eServiceTranslationGetString if a translation cannot
# be found for a string.  This routine is intended to be replaced
# by an application specific routine for error reporting
# purposes.  The default behavior is to return the source string.
#
# Arguments:
# locale    The current locale.
# src   The string to be translated.
#
# Results:
# Returns the translated value.

proc eServiceTranslation::eServiceTranslationUnknown {locale src} {
    return ""
}

# Initialize the default locale

namespace eval eServiceTranslation {
    # set default locale, try to get from environment
    if {[info exists ::env(LANG)]} {
        eServiceTranslationLocale $::env(LANG)
    } else {
        eServiceTranslationLocale "C"
    }
}

}

