###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonAutoRouteByNavigation.tcl.rca $ $Revision: 1.48 $
#
# @libdoc       eServicecommonAutoRouteByNavigation
#
# @Library:     Interface for required person checks for triggers
#
# @Brief:       Gets person objects through specified navigation path.
#
# @Description: see procedure description
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

###############################################################################
#
# Define Global Variables
#
###############################################################################


###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [ utLoad eServicecommonDEBUG.tcl ]
eval  [ utLoad eServicecommonShadowAgent.tcl ]
eval  [ utLoad eServicecommonTranslation.tcl ]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigcAutoRouteByNavigation_if.tcl
#
# @Brief:       Do different operation with the person object
#               connected through specified path
#
# @Description: Do different operation with the person object
#               connected through specified path
#
# @Parameters:  sSelect        -- Select statement which gets the user to be routed
#               sOperation     -- Operation to be performed with found person objects.
#                                 ROUTE  -- Assign the person to be owner of the object
#                                           from which trigger has generated and send
#                                           icon mail to that person.
#                                           If more then one person objects are found then
#                                           assign the first person to be owner and send
#                                           mail to all the persons found.
#                                 SEND   -- Send mail to all the persons found.
#                                 ASSIGN -- Same as ROUTE but don't send mail to anyone.
#               bErrNotFound   -- If set to TRUE then program will error out if user doesn't exist.
#               sSubject       -- If operation is ROUTE or SEND then this argument
#                                 specifies subject of the mail to be send.
#               sText          -- mail contents.
#               bPassObj       -- Wheather to pass selected object name into mail.
#                                 TRUE   -- Pass object name into mail
#                                 FALSE  -- Don't pass object name into mail.
#               bNotify        -- Set flag to give confirmation that operation was
#                                 successfully completed.  TRUE or null entry will turn flag
#                                 on, anything else turns flag off.
#               sBasePropFile  -- Base Name of the property file where subject and text 
#                                     should be looked up.
#                                     If this is not provided
#                                     by default it will look up in
#                                     property files with base name emxFrameworkStringResource.
#
# @Returns:     0 if no person objects found, 1 otherwise
#
# @Example:     Input to eServiceTriggerManager:
#                 {{ eServicecommonTrigcAutoRouteByNavigation_if  {from[relationship_ECR].businessobject.attribute[attribute_ChangeBoard]} {ROUTE} {subject} {contents} }}
#
# @procdoc
#******************************************************************************

proc eServicecommonAutoRouteByNavigation {sType sName sRev sSelect sOperation bErrNotFound sSubject sText bPassObj bNotify sBasePropFile} {

  #
  # Debugging trace - note entry
  #
  set progname      "eServicecommonAutoRouteByNavigation.tcl"

  #
  # Error handling variables
  #
  set iReturn 0
  set outStr ""
  set mqlret 0
  set sCmd ""

  set sCmd {mql print bus "$sType" "$sName" "$sRev" select "$sSelect" dump |}
  set mqlret [ catch {eval $sCmd} outStr]
  if {$mqlret != 0} {
      mql notice "$outStr"
      return -code 1 ""
  }

  if {"$outStr" == ""} {
      if {"$bErrNotFound" == "TRUE"} {
          set sMessageString [mql execute program emxMailUtil -method getMessage \
                                  "emxFramework.ProgramObject.eServicecommonAutoRouteByNavigation.RouteDestination" 0 \
                                  "" \
                             ]
          mql notice "$sMessageString"
          return -code 1 ""
      } else {
          return -code 0 ""
      }
  }

  set lPersons [split $outStr |]

  switch -exact $sOperation {

  "SEND" {
          set sPersonPattern [join $lPersons ","]

          if {"$bPassObj" == "TRUE"} {
              set sObjectId [mql get env OBJECTID]
              set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                    "$sPersonPattern" \
                                    "$sSubject" 0 \
                                    "$sText" 0 \
                                    "$sObjectId" \
                                    "" \
                                    "$sBasePropFile" \
                                    }
          } else {
              set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                    "$sPersonPattern" \
                                    "$sSubject" 0 \
                                    "$sText" 0 \
                                    "" \
                                    "" \
                                    "$sBasePropFile" \
                                    }
          }
          set mqlret [ catch {eval $sCmd} outStr]
          if { $mqlret != 0 } {
              mql notice "$outStr"
              return -code $mqlret ""
          }
      }
  "REASSIGN" {
          set sPerson [lindex $lPersons 0]
          pushShadowAgent
          set sCmd {mql modify bus "$sType" "$sName" "$sRev" owner "$sPerson"}
          set mqlret [ catch {eval $sCmd} outStr]
          if { $mqlret != 0 } {
              mql notice "$outStr"
              return -code $mqlret ""
          }
          popShadowAgent
      }
  "ROUTE" {
          set sPersonPattern [join $lPersons ","]
          if {"$bPassObj" == "TRUE"} {
              set sObjectId [mql get env OBJECTID]
              set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                    "$sPersonPattern" \
                                    "$sSubject" 0 \
                                    "$sText" 0 \
                                    "$sObjectId" \
                                    "" \
                                    "$sBasePropFile" \
                                    }
          } else {
              set sCmd {mql execute program emxMailUtil -method sendNotificationToUser \
                                    "$sPersonPattern" \
                                    "$sSubject" 0 \
                                    "$sText" 0 \
                                    "" \
                                    "" \
                                    "$sBasePropFile" \
                                    }
          }
          set mqlret [ catch {eval $sCmd} outStr]
          if { $mqlret != 0 } {
              mql notice "$outStr"
              return -code $mqlret ""
          }

          set sPerson [lindex $lPersons 0]
          pushShadowAgent
          set sCmd {mql modify bus "$sType" "$sName" "$sRev" owner "$sPerson"}
          set mqlret [ catch {eval $sCmd} outStr]
          if { $mqlret != 0 } {
              mql notice "$outStr"
              return -code $mqlret ""
          }
          popShadowAgent
      }
  default {
      }
  }

  # Notify completion.
  if {($mqlret == 0) && ("$bNotify" == "TRUE")} {
      if {"$sOperation" != "REASSIGN"} {
          set sMsg [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonAutoRouteByNavigation.CompleteNotify" 2 \
                                  "Operation" "$sOperation" \
                                  "Persons" "$sPersonPattern" \
                    "" \
                    ]
      } else {
          set sMsg [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonAutoRouteByNavigation.Complete" 1 \
                                  "Operation" "$sOperation" \
                    "" \
                    ]
      }
      mql notice $sMsg
  }

  return -code 0 ""

}
# end eServicecommonRequiredConnection


# End of Module

