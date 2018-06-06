###############################################################################
# $RCSfile: emxCommonRemoveLanguageAliases.tcl.rca $ $Revision: 1.12 $
#
# @libdoc       emxCommonRemoveLanguageAliases.tcl
#
# @Library:     Program to clean up language aliases.
#
# @Brief:       Program to clean up language aliases.
#
# @Description: This program accepts list of languages to be cleaned up
#               There is only one input to this program
#               It can be list of languages whose aliases needs to be removed
#               Or
#               reserved word all. In this case all the language aliases will
#               removed.
#
# @Usage:       exec program emxCommonRemoveLanguageAliases.tcl {en de fr}
#               exec program emxCommonRemoveLanguageAliases.tcl all
#
# @libdoc       Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
# The following sample code is provided for your reference purposes in
# connection with your use of the Matrix System (TM) software product
# which you have licensed from MatrixOne, Inc. ("MatrixOne").
# The sample code is provided to you without any warranty of any kind
# whatsoever and you agree to be responsible for the use and/or incorporation
# of the sample code into any software product you develop. You agree to fully
# and completely indemnify and hold MatrixOne harmless from any and all loss,
# claim, liability or damages with respect to your use of the Sample Code.
#
# Subject to the foregoing, you are permitted to copy, modify, and distribute
# the sample code for any purpose and without fee, provided that (i) a
# copyright notice in the in the form of "Copyright 1995 - 1998 MatrixOne Inc.,
# Two Executive Drive, Chelmsford, MA  01824. All Rights Reserved" appears
# in all copies, (ii) both the copyright notice and this permission notice
# appear in supporting documentation and (iii) you are a valid licensee of
# the Matrix System software.
#
###############################################################################

tcl;
eval {

    # Get languages to be removed
    set lLanguages [mql get env 1]
    
    if {"$lLanguages" == "all"} {
        set sCmd {mql list language}
        set mqlret [catch {eval $sCmd} outStr]
         if {$mqlret != 0} {
            puts stdout "$outStr"
            return -code 1
        }
        
        set lLanguages [split $outStr \n]
    }

    # Trigger off
    set sCmd {mql trigger off}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "$outStr"
        return -code 1
    }

    # start transaction
    set sCmd {mql start transaction}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "$outStr"
        return -code 1
    }

    # For each language remove aliases.
    foreach sLanguage $lLanguages {
        # Get all the alias names.
        set sCmd {mql list alias aliasname "$sLanguage"}
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            puts stdout "$outStr"
            mql abort transaction
            return -code 1
        }

        set lOutput [split $outStr \n]

        foreach sLine $lOutput {
            set lLine [split $sLine ']

            # Get admin type
            set sAdmin [string trim [lindex $lLine 0]]
            if {"$sAdmin" == "att"} {
                set sAdmin "attribute"
            } elseif {"$sAdmin" == "lattice"} {
                set sAdmin "vault"
            }

            # Get admin name
            set sAdminName [lindex $lLine 1]

            # Get alias name
            set sAliasName [lindex $lLine 3]

            # Delete alias on admin.
            set sCmd {mql delete alias "$sAliasName" from $sAdmin "$sAdminName" aliasname "$sLanguage"}
            set mqlret [catch {eval $sCmd} outStr]
            if {$mqlret != 0} {
                puts stdout "$outStr"
                mql abort transaction
                return -code 1
            }
        }
    }

    # Trigger on
    set sCmd {mql trigger on}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "$outStr"
        return -code 1
    }

    # commit transaction
    set sCmd {mql commit transaction}
    set mqlret [catch {eval $sCmd} outStr]
    if {$mqlret != 0} {
        puts stdout "$outStr"
        return -code 1
    }

}

