###############################################################################
#
# $RCSfile: eServicecommonCreatePerson.tcl.rca $ $Revision: 1.58 $
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
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
eval  {

  # Load the utilities and other libraries.
  eval  [ mql print prog eServicecommonUtil.tcl select code dump ]

  eval  [ utLoad eServicecommonPSUtilities.tcl ]
  eval  [ utLoad eServicecommonShadowAgent.tcl ]
  eval  [ utLoad eServicecommonDEBUG.tcl]
  eval  [ utLoad eServicecommonTranslation.tcl]
###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################

    set RegProgName   "eServiceSchemaVariableMapping.tcl"
    set sProgName     "eServicecommonCreatePerson.tcl"

    eval [utLoad $RegProgName]

    mql verbose off
    mql quote off

    set sName               [string trim [mql get env 1]]
    set sPassword             [string trim [mql get env 2]]
    set sRoleList             [string trim [mql get env 3]]
    set sLimit                [string trim [mql get env 4]]
    set sDefaultVault         [string trim [mql get env 5]]
    set sEmailAdd             [string trim [mql get env 6]]
    set sPromoteObject          [string toupper [string trim [mql get env 7]]]
    set sGroupList              [string trim [ mql get env 8 ]]
    set sTypePerson             [eServiceGetCurrentSchemaName  type      $RegProgName  type_Person ]
    set sRealUser             [ mql get env USER]
    set sResultList ""
    set ssCmd ""
    set sTempName $sName
    set sPersonID ""
    set mqlret  0
    set i 1

    set sVault          [ mql get env VAULT]

    if {[string compare [string trim $sEmailAdd] ""] == 0} {
       set sEmailFlag "disable"
    } else {
       set sEmailFlag "enable"
    }

    # default promote object to FALSE.
    if {"$sPromoteObject" == ""} {
        set sPromoteObject "FALSE"
    }

    if {$mqlret == 0} {

        #Create Role Addition List for CMD
        if {[llength $sRoleList] > 0} {
            foreach j $sRoleList {
              set sRole         [string trim [lindex $j 0]]
              set sRole  [eServiceGetCurrentSchemaName  role $RegProgName  $sRole ]
              lappend sResultList  "assign role \"$sRole\""
            }
        }

       #Create Group Addition List for CMD
        if { [ llength $sGroupList ] > 0 } {
            foreach j $sGroupList {
                set sGroup [ string trim [ lindex $j 0]]
                set sGroup  [eServiceGetCurrentSchemaName  group $RegProgName  $sGroup ]
                lappend sResultList "assign group \"$sGroup\""
            }
        }

     #Test if Person object already exists
       while {1} {
        set sCmd {mql temp query bus $sTypePerson $sTempName * select exists dump |}
    set mqlret [catch {eval $sCmd} outstr]
    if {$mqlret == 0} {
       set outstr  [string trim [lindex [split $outstr |] 3]]
       set lPerson [split [mql list person] "\n"]
       if { $outstr == "TRUE" || [lsearch -exact $lPerson "$sTempName"] >= 0 } {
        incr i
        set sTempName $sName$i
          if {[string compare [string trim $sLimit] ""] != 0 } {
             if {$sLimit <= $i} {
                set outstr [mql execute program emxMailUtil -method getMessage \
                                      "emxFramework.ProgramObject.eServicecommonCreatePerson.AlreadyInUse" 0 \
                                      "" \
                                 ]
                set mqlret  1
                break
             } else {
                continue
                   }
          }
       } else {
          set sPolicy       [mxTypeGetDfltPolicy $sTypePerson]
                set sRevision     [mxPolGetInitRev $sPolicy]
                pushShadowAgent
                #
                # start transaction
                #
                set sCmd {utCheckStartTransaction}
                set mqlret [ catch {eval  $sCmd} outstr ]
                set bStartTransErr $mqlret

                if {$mqlret == 0} {
                   set bTranAlreadyStarted "$outstr"
                   set sResultList  [join $sResultList]
                   if {$sPassword == ""} {
                        set sCmd {mql add person "$sTempName" \
                                      fullname "$sTempName" \
                                      enable iconmail \
                                      "$sEmailFlag" email \
                                      vault "$sDefaultVault" \
                                      access all \
                                      admin all \
                                      type full \
                                      email "$sEmailAdd" \
                                 }
                   } else {
                        set sCmd {mql add person "$sTempName" \
                                      fullname "$sTempName" \
                                      enable iconmail \
                                      "$sEmailFlag" email \
                                      vault "$sDefaultVault" \
                                      access all \
                                      admin all \
                                      type full \
                                      password "$sPassword" \
                                      email "$sEmailAdd" \
                                 }
                   }
                   append  ssCmd "$sCmd" " " "$sResultList"
                   set mqlret [catch {eval $ssCmd} outstr]
                }
                if {$mqlret == 0} {
             set sCmd {mql add bus "$sTypePerson" "$sTempName" "$sRevision" vault "$sDefaultVault" policy "$sPolicy"  select id name dump | }             
             set mqlret [catch {eval $sCmd} outstr]
          }
          if {$mqlret == 0} {
	      
	      set lnewPerson [split $outstr | ]
              set sPersonID [ lindex $lnewPerson 0 ]
	      set sTempName [lindex $lnewPerson 1 ]

              set sCmd {mql mod bus "$sPersonID" owner "$sTempName" }
                   set mqlret [catch {eval $sCmd} outstr]
                }
               
                # commit or abort transaction
                if {$mqlret == 0} {
                   set sCmd {utCheckCommitTransaction $bTranAlreadyStarted}
                   set mqlret [catch {eval $sCmd} outstr]
                } else {
                   if {$bStartTransErr == 0} {
                       utCheckAbortTransaction $bTranAlreadyStarted
                   }
                }
                popShadowAgent
                if {$mqlret == 0 && "$sPromoteObject" == "TRUE"} {
                    set sCmd {mql promote bus "$sPersonID" }
                    set mqlret [catch {eval $sCmd} outstr]
                }
                break
       }
    }
       }
    }

    if {$mqlret != 0} {
          return "1|Error: - $outstr"
    } else {
       return "0|$sPersonID"
    }
}
##################################################################################

