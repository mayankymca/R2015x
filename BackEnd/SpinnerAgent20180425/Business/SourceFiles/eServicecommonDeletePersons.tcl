###############################################################################
#
# $RCSfile: eServicecommonDeletePersons.tcl.rca $ $Revision: 1.45 $
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# Input : List of persons to be deleted.
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

    # Load the utilities and other libraries.
    eval  [ mql print prog eServicecommonUtil.tcl select code dump ]

    eval  [ utLoad eServicecommonPSUtilities.tcl ]
    eval  [ utLoad eServicecommonShadowAgent.tcl ]
    eval  [ utLoad eServicecommonDEBUG.tcl]
    eval  [ utLoad eServicecommonTranslation.tcl]

    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set sProgName     "eServicecommonDeletePersons.tcl"
    eval [utLoad $RegProgName]

    set lPersonId   [string trim [mql get env 1]]
    set outStr      ""
    set mqlret      0
    set bStartTransErr 0

    set mqlret [pushShadowAgent]
    if {$mqlret != 0 } {
	set outStr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonDeletePerson.Context" 0 \
                    "" \
                    ]
    }

    if {$mqlret == 0 } {
        set sCmd {utCheckStartTransaction}
        set mqlret [catch {eval $sCmd} outStr]
        set bStartTransErr $mqlret
    }

    if {$mqlret == 0 } {
        set bTranAlreadyStarted "$outStr"
        set sCmd {mql list person}
        set mqlret [catch {eval $sCmd} outStr]
    }

    if {$mqlret == 0 } {
        foreach sPersonId $lPersonId {
            set sCmd {mql print bus "$sPersonId" select name dump}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                break
            }
            set sPersonName $outStr
            set sCmd {mql delete businessobject "$sPersonId"}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                break
            }
            set sCmd {mql delete person "$sPersonName"}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                break
            }
        }
    }

    if {$mqlret == 0} {
        set sCmd {utCheckCommitTransaction $bTranAlreadyStarted}
        set mqlret [catch {eval $sCmd} outStr]
        popShadowAgent
        return "0|"
    } else {
        if {$bStartTransErr == 0} {
            set sCmd {utCheckAbortTransaction $bTranAlreadyStarted}
      	    set mqlret [catch {eval $sCmd} outStr]
        }
        popShadowAgent
        return "1|Error: - $outStr"
    }
}
##################################################################################

