#################################################################################
#
# $RCSfile: eServiceConversion10-0-0-2AttrTypeChange.tcl.rca $ $Revision: 1.4 $
#
# Description:
# DEPENDENCIES: This program has to lie in Schema & Common directories with the
#               same name and should be called from all builds
#################################################################################
#################################################################################
#                                                                               #
#   Copyright (c) 1998-2015 Dassault Systemes.  All Rights Reserved.                   #
#   This program contains proprietary and trade secret information of           #
#   Matrix One, Inc.  Copyright notice is precautionary only and does not       #
#   evidence any actual or intended publication of such program.                #
#                                                                               #
#################################################################################

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

    set  sOutput [ mql print program \"$sProgram\" select code dump ]

    return $sOutput
}
# end utload

    # Get environment variables
    set sRegProgName "eServiceSchemaVariableMapping.tcl"

    # Load utility files
    eval [utLoad $sRegProgName]

    # Get attribute symbolic name
    set sAttSymName [mql get env 1]
    # Get new attribute type
    set sNewAttType [mql get env 2]
    # Get attribute default
    set sDefault [mql get env 3]

    # Get admin names from symbolic names.
    set sAttName [eServiceGetCurrentSchemaName attribute $sRegProgName $sAttSymName]
    
    # Get attribute type in db
    set sCmd {mql print attribute "$sAttName" select type dump}
    set mqlret [ catch {eval $sCmd} outStr ]
    if {$mqlret != 0} {
        mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
        mql trace type INSTALL text ">$outStr"
        mql trace type INSTALL text ""
        return 1
    }
    set sAttType "$outStr"

    # If attribute type = input type
    if {"$sAttType" != "$sNewAttType"} {

        # Current date
        set sDate [clock format [clock seconds] -format %m-%d-%Y]

        # Create tempraray attribute of new type
        set sCmd {mql add attribute "$sAttSymName" \
                      type $sNewAttType \
                      default $sDefault \
                      property application value Framework \
                      property version value 10-0-0-2 \
                      property "original name" value "$sAttName" \
                      property installer value MatrixOneEngineering \
                      property "installed date" value "$sDate" \
                      }
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return 1
        }
        
        # For each type using it add the new attribute
        set sCmd {mql list type}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return 1
        }
        set lTypes [split "$outStr" "\n"]
        set sCmd {mql list type select immediateattribute\[$sAttName\] dump |}
        set mqlret [ catch {eval $sCmd} outStr ]
        if {$mqlret != 0} {
            mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
            mql trace type INSTALL text ">$outStr"
            mql trace type INSTALL text ""
            return 1
        }
        set lTypeUsage [split "$outStr" "\n"]

        foreach sTypeName $lTypes sTypeUsage $lTypeUsage {

            if {"$sTypeUsage" == "FALSE"} {
                continue
            }

            # add the new attribute
            set sCmd {mql modify type "$sTypeName" \
                          add attribute "$sAttSymName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }

            # Query entire type and their derived type's business objects.
            set sCmd {mql temp query bus "$sTypeName" * * \
                          select \
                          id \
                          attribute\[$sAttName\].value \
                          dump tcl \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }

            # For each object
            foreach lObj $outStr {

                set sId [lindex [lindex $lObj 3] 0]
                set sAttValue [lindex [lindex $lObj 4] 0]

                # Copy attribute value to new attribute
                set sCmd {mql modify bus "$sId" \
                              "$sAttSymName" "$sAttValue" \
                              }
                set mqlret [ catch {eval $sCmd} outStr ]
                if {$mqlret != 0} {
                    mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
                    mql trace type INSTALL text ">$outStr"
                    mql trace type INSTALL text ""
                    return 1
                }

            # End for
            }

            # Delete old attribute
            set sCmd {mql delete attribute "$sAttName"}
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }

            # Register property attribute_CageCode to point to attribute 'Cage Code New'
            set sCmd {mql add property $sAttSymName \
                          on program eServiceSchemaVariableMapping.tcl \
                          to attribute "$sAttSymName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }

            # Rename new attribute to old value
            set sCmd {mql modify attribute "$sAttSymName" \
                          name "$sAttName" \
                          }
            set mqlret [ catch {eval $sCmd} outStr ]
            if {$mqlret != 0} {
                mql trace type INSTALL text ">ERROR       : eServiceConversion10-0-0-2AttrTypeChange.tcl"
                mql trace type INSTALL text ">$outStr"
                mql trace type INSTALL text ""
                return 1
            }

        }
    }
    
    return -code 0
}

