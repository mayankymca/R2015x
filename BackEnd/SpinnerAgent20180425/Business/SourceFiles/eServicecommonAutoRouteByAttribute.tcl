###############################################################################
# $RCSfile: eServicecommonAutoRouteByAttribute.tcl.rca $ $Revision: 1.45 $
#
# @libdoc       eServicecommonAutoRouteByAttribute.tcl
#
# @Library:     Procedure for simple auto route action
#
# @Brief:       Routes or notifies person named in specified attribute.
#
# @Description: This program automatically routes, sends, or reassigns an
#                    object to the user who is indicated in the specified attribute.
#                    This user can be a person, group, or role.
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

#******************************************************************************
# @procdoc      eServicecommonTrigcAutoRouteByAttibute_if
#
# @Brief:       Routes or notifies person named in specified attribute.
#
# @Description: This procedure is passed in the name of an
#               attribute to read.  This attribute contains the
#               target to which the operation relates.  The attribute
#               can contain a user, role or group identifier.
#
#               The sendoption method instructs this procedure as
#               to the operation to perform on behalf of the target
#               object:
#
#               send - send a message and object copy to target
#               assign - assign target as new owner of this object
#               route - perform both operations
#
#               If the sendoption is assign or route, this object will be
#               owned by the target (person / group / role) specified.
#
# @Parameters:  sAbsAttName    -- attribute used to get 'user name'
#               sSendoption    -- action to be performed (route/send/reassign)
#               sSubject       -- the subject text of the message if sendoption
#                                 is route or send (opitonal)
#               sText          -- mail contents.
#               sPassObj       -- Wheather to pass selected object name into mail.
#                                 TRUE   -- Pass object name into mail
#                                 FALSE  -- Don't pass object name into mail.
#               sNotify        -- Set flag to give confirmation that operation was
#                                 successfully completed.  TRUE or null entry will turn flag
#                                 on, anything else turns flag off.
#               sBasePropFile  -- Base Name of the property file where subject and text 
#                                     should be looked up.
#
# @Returns:     0 for success; 1 for failure
#
# @Usage:
#
# @procdoc
#*******************************************************************************

###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [ utLoad eServicecommonShadowAgent.tcl ]
eval  [ utLoad eServicecommonTranslation.tcl ]

proc eServicecommonAutoRouteByAttribute { sType sName sRev sAbsAttName sSendoption sSubject sText sPassObj sNotify sBasePropFile} {

    mql verbose off

    # Load MQL/Tcl utility procedures
    eval  [utLoad eServicecommonShadowAgent.tcl]

    # set program related variables
    set progname    "eServicecommonAutoRouteByAttribute.tcl"
    set mqlret      0

    set szCommand {mql print bus "$sType" "$sName" "$sRev" select attribute\[$sAbsAttName\] dump}
    if {[catch {eval $szCommand} objAttVal] == 0} {
        set sNewOwner $objAttVal
    } else {
        mql notice "$progname - $objAttVal"
        set mqlret 1
    }

    # Check to see if ownership of object is to be changed
    if {$mqlret == 0} {

        if {($sSendoption == "assign") || ($sSendoption == "route")} {
             set mqlret [pushShadowAgent]
             if {$mqlret != 0} {
                 set outStr [mql execute program emxMailUtil -method getMessage \
                                 "emxFramework.ProgramObject.eServicecommonAutoRouteByAttribute.ErrorNoRight" 5 \
                                               "Program" "$progname" \
                                               "Type" "$sType" \
                                               "Name" "$sName" \
                                               "Rev" "$sRev" \
                                               "Owner" "$sNewOwner" \
                                 "" \
                            ]
                 mql notice "$outStr"
             } else {
                 set sCmd {mql modify bus "$sType" "$sName" "$sRev" owner "$sNewOwner"}
                 set mqlret [catch {eval $sCmd} outStr]
                 if {$mqlret != 0} {
                     mql notice "$progname - $outStr"
                 }
                 popShadowAgent
             }
        }
    }

    # If sSendoption was send or route, send out a copy of the object and
    # a text message to the new object owner
    if {(($sSendoption == "send")||($sSendoption == "route")) && ($mqlret == 0)}  {
        if {"$sPassObj" == "TRUE"} {
            set sObjectId [mql get env OBJECTID]
            set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                  "$sNewOwner" \
                                  "$sSubject" 0 \
                                  "$sText" 0 \
                                  "$sObjectId" \
                                  "" \
                                  "$sBasePropFile" \
                                  }
        } else {
            set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                  "$sNewOwner" \
                                  "$sSubject" 0 \
                                  "$sText" 0 \
                                  "" \
                                  "" \
                                  "$sBasePropFile" \
                                  }
        }
        set mqlret [catch {eval $sCmd} outStr]
        if {$mqlret != 0} {
            mql notice "$progname - $sCmd"
        }
    }

    # Notify completion.
    if {($mqlret == 0) && ("$sNotify" == "TRUE" || "$sNotify" == "")} {
        set sMsg [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonAutoRouteByAttribute.Notification" 2 \
                                  "Operation" "$sSendoption" \
                                  "Owner" "$sNewOwner" \
                    "" \
                    ]
        mql notice $sMsg
    }

    return -code $mqlret ""
}
# end eServicecommonAutoRouteByAttribute

