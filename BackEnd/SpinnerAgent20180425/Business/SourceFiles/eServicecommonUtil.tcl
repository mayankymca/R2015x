
#************************************************************************
# @libdoc         eServicecommonUtil.tcl
#
# @Library:       eMatrix Common Utility Procedures
#
# @Brief:         Contains common utility procedures.
#
# @Description:   The eMatrix Common Utility library contains procedures
#                 for loading tcl libraries from Matrix program objects,
#                 catching errors, and aborting programs.
#
# @libdoc         Copyright (c) 2001, MatrixOne, Inc.
#************************************************************************

#************************************************************************
# @procdoc        utLoad
#
# @Brief:         Load a program object containing tcl code.
#
# @Description:   This procedure returns the code section of the specified
#                 program object.  Normally this is used along with the
#                 "eval" command to make the procedures defined in the
#                 library available to the currently running tcl session.
#
# @Parameters:    sProgram     - Name of the Matrix program library to load.
#                 bForceReload - (Optional) Force the program to be loaded
#                                even though it has been loaded before.
#                                (Default: 0)
#
# @Returns:       The contents of the code section of the program object.
#
# @Usage:         The following example loads the "mxPer.tcl" program
#                 object's code and makes it's procedures available:
#
# @Example:       % eval [ utLoad mxPer.tcl ]
#                 % info commands mxPer*
#                 mxPerGetContext mxPerGetSelectable mxPerGetBusAdmins ...
#
# @procdoc
#************************************************************************

proc utLoad { sProgram { bForceLoad 0 } } {

  # Access global variables.
  global glUtLoadProgs env

  # Make sure the global program list exists.
  if { ! [ info exists glUtLoadProgs ] } {
    set glUtLoadProgs {}
  }

  # If we are not forcing the program to be loaded,
  # then check if the program has already been loaded.
  if { ! $bForceLoad } {

    # If the specified program has already been loaded, then return nothing.
    # This avoids loading the same program more than once.
    if { [ lsearch $glUtLoadProgs $sProgram ] < 0 } {
      lappend glUtLoadProgs $sProgram
    } else {
      return ""
    }

  }

  # Try to read the program from a file, if it exists.
  if { [ catch {
    set sDir "$env(TCL_LIBRARY)/mxTclDev"
    set pFile [ open "$sDir/$sProgram" r ]
    set sOutput [ read $pFile ]
    close $pFile
  } ] == 0 } { return $sOutput }

  # If there was no file, then get the program contents from the database.
  return [ mql print program $sProgram select code dump ]

}
# End utLoad

#************************************************************************
# @procdoc        utCatch
#
# @Brief:         Catch errors in tcl code.
#
# @Description:   This procedure catches errors in the given tcl code
#                 and handles them properly.
#
# @Parameters:    sCmd        - The tcl command(s) to execute.
#                 bAbortTrans - (Optional) The abort transaction flag.
#                               If 1, abort the transaction when an
#                               error occurs.  Otherwise, do not abort
#                               the transaction when an error occurs.
#                               (Default: 0)
#                 sErrorMsg   - (Optional) The error message to display
#                               when an error occurs.  If "!!DEFAULT!!",
#                               then a default error message will be
#                               displayed that shows the program name
#                               and error message.  If a non-default
#                               error message is given, and it contains
#                               %s's, the first %s will be replaced with
#                               the program name and the second %s will
#                               be replaced with the error message.
#                               (Default: "!!DEFAULT!!")
#
# @Returns:       If no errors occur, then nothing is returned.
#                 If an error occurs, then the error code and
#                 error message are returned.
#
# @Usage:         The following example expands a business object and
#                 puts the results in a local variable.  If an error
#                 occurs, then the default message is displayed and
#                 the error code and error message is returned.
#
# @Example:       % utCatch { set sData [ mql expand bus $sOid from dump | ] }
#
# @procdoc
#************************************************************************

proc utCatch { sCmd { bAbortTrans 0 } { sErrorMsg "!!DEFAULT!!" } } {

  # Execute the command in the calling context and catch errors.
  set sErrorCode [ catch {uplevel $sCmd} sReturnMsg ]

  # If there was an error, then preform error processing.
  if { $sErrorCode != 0 } {

    # Display the specified error or the default.
    if { $sErrorMsg == "!!DEFAULT!!" } {
      mql notice "[ mql get env 0 ] :\n$sReturnMsg"
    } elseif { $sErrorMsg != "" } {
      mql notice [ format $sErrorMsg [ mql get env 0 ] $sReturnMsg ]
    }

    # Abort the transaction, if so instructed.
    if { $bAbortTrans } {
      mql abort trans
    }

    # Return the error code and error from the calling context.
    uplevel [ list return -code $sErrorCode $sReturnMsg ]

  }

  # If we got here, then return nothing.
  return

}
# End utCatch

#************************************************************************
# @procdoc        utAbort
#
# @Brief:         Abort and rollback the current transaction.
#
# @Description:   This procedure aborts and rolls back the current
#                 transaction.  It is done by trying to start a
#                 transaction and then aborting it.  This is done
#                 because enclosing the code in a transaction boundary
#                 is not possible, as there is no way to distinguish
#                 whether a transaction is started by matrix or another
#                 tcl program.  Hence starting a transaction, if already
#                 started would end in rolling back the whole transaction.
#                 If not started then the abort transaction would give the
#                 same effect.
#
# @Parameters:    None.
#
# @Returns:       Nothing.
#
# @Usage:         The following example aborts regardless of
#                 transaction boundaries:
#
# @Example:       % utAbort
#
# @procdoc
#************************************************************************

proc utAbort {} {

  # Try to start a transaction and disregard any errors.
  catch {mql start transaction}

  # If we are in a transaction, then abort.
  if { [ lindex [ mql print transaction ] 1 ] != "inactive" } {
    mql abort transaction
  }

  # Return nothing.
  return

}
# End utAbort

#************************************************************************
# @procdoc        utCheckStartTransaction
#
# @Brief:         Check for the existance of transaction and starts
#                 transaction only if not started.
#
# @Description:
#
# @Parameters:    sType -- type of transaction read/update.
#
# @Returns:       returns 1 and error message in case of failure
#                 returns 0 and 1 if transaction already started
#                               0 if transaction not started.
#
# @Usage:
#
# @Example:       % utCheckStartTransaction
#
# @procdoc
#************************************************************************

proc utCheckStartTransaction {{sType update}} {

  # Check if transaction has started.
  set sCmd {mql print transaction}
  set mqlret [ catch {eval  $sCmd} outStr ]
  if {$mqlret != 0} {
      return -code "$mqlret" "$outStr"
  }
  set sTransaction [lindex [split $outStr] 1]
  if {"$sTransaction" == "inactive"} {
      set bTranAlreadyStarted 0

      # start appropriate transaction as per input argument.
      if {"$sType" == "update"} {
          set sCmd {mql start transaction}
      } else {
          set sCmd {mql start transaction read}
      }
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          return -code "$mqlret" "$outStr"
      }
  } else {
      set bTranAlreadyStarted 1
  }

  return -code "$mqlret" "$bTranAlreadyStarted"
}
# End utCheckStartTransaction

#************************************************************************
# @procdoc        utCheckAbortTransaction
#
# @Brief:         Aborts transaction depending on input flag.
#
# @Description:
#
# @Parameters:    bTransaction -- 0 to abort transaction
#
# @Returns:       returns 1 and error message in case of failure
#                 returns 0 if successful
#
# @Usage:
#
# @Example:       % utCheckAbortTransaction
#
# @procdoc
#************************************************************************

proc utCheckAbortTransaction {{bTransaction}} {

  # Check if transaction has to be aborted.
  if {$bTransaction == 0} {
      set sCmd {mql abort transaction}
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          return -code "$mqlret" "$outStr"
      }
  }

  return -code 0 ""
}
# End utCheckAbortTransaction

#************************************************************************
# @procdoc        utCheckCommitTransaction
#
# @Brief:         Commits transaction depending on input flag.
#
# @Description:
#
# @Parameters:    bTransaction -- 0 to Commit transaction
#
# @Returns:       returns 1 and error message in case of failure
#                 returns 0 if successful
#
# @Usage:
#
# @Example:       % utCheckCommitTransaction
#
# @procdoc
#************************************************************************

proc utCheckCommitTransaction {{bTransaction}} {

  # Check if transaction has to be commited.
  if {$bTransaction == 0} {
      set sCmd {mql commit transaction}
      set mqlret [ catch {eval  $sCmd} outStr ]
      if {$mqlret != 0} {
          return -code "$mqlret" "$outStr"
      }
  }

  return -code 0 ""
}
# End utCheckCommitTransaction

#************************************************************************
# @procdoc        eServiceGetAssignments
#
# @Brief:         Gets assignments for a person.
#
# @Description:
#
# @Parameters:    sSpecificAssignment -- role/group (type of assignment)
#                                        defaults to role
#                 sUser               -- the user whose assignments are required
#                                        defaults to loged in user.
#
# @Returns:       returns 1 and error message in case of failure
#                 returns 0 and list of assignments.
#
# @Usage:
#
# @Example:       % eServiceGetAssignments
#
# @procdoc
#************************************************************************

proc eServiceGetAssignments {{sSpecificAssignment role} {sUser ""}} {

  if {$sUser == ""} {
      set sUser [mql get env USER]
  }

  # Get assignments.
  set sCmd {mql print person "$sUser" select assignment dump |}
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return -code 1 "$outStr"
  }
  set lAssignments [split $outStr |]

  # If Specific assignment requested then
  # get those from all assignments.
  if {"$sSpecificAssignment" != ""} {
      set sCmd {mql list "$sSpecificAssignment"}
      set mqlret [ catch {eval $sCmd} outStr ]
      if {$mqlret != 0} {
          return -code 1 "$outStr"
      }
      set lAllSpecificAdmins [split $outStr \n]
      set lSpecificAdmins {}
      foreach sAssignment $lAssignments {
          if {[lsearch $lAllSpecificAdmins "$sAssignment"] != -1} {
              lappend lSpecificAdmins "$sAssignment"
          }
      }
      return -code 0 "$lSpecificAdmins"
  } else {
      return -code 0 "$lAssignments"
  }
}

#************************************************************************
# @procdoc        eServiceGetProperty
#
# @Brief:         Gets property details on given admin object.
#
# @Description:
#
# @Parameters:    sAdminType    -- type of admin (type/attribute/program)
#                 sAdminName    -- name of admin object.
#                 sPropertyName -- name of the property for which details are required.
#                                  if not given then get details for all the properties.
#                 sDetail       -- which specific detail required. (to/value)
# @Returns:       returns 1 and error message in case of failure
#                 returns 0 and list of assignments.
#
# @Usage:
#
# @Example:       % eServiceGetProperty program eServiceRegistry
#
# @procdoc
#************************************************************************

proc eServiceGetProperty {sAdminType sAdminName {sPropertyName ""} {sDetail "value"}} {

  # if property name is not specified then get all property details.
  if {"$sPropertyName" == ""} {
      set sCmd {mql print "$sAdminType" "$sAdminName" select property dump |}
  } else {
      set sCmd {mql print "$sAdminType" "$sAdminName" select property\[${sPropertyName}\].${sDetail} dump |}
  }
  set mqlret [ catch {eval $sCmd} outStr ]
  if {$mqlret != 0} {
      return -code 1 "$outStr"
  }
  set lOutput [split $outStr |]
  if {"$sPropertyName" == "" || "$sDetail" == "value"} {
      return -code 0 "$lOutput"
  }
  set lNameList {}
  foreach sItem $lOutput {
      lappend lNameList [join [lrange [split $sItem] 1 end]]
  }
  return -code 0 "$lNameList"
}

# End of Module


