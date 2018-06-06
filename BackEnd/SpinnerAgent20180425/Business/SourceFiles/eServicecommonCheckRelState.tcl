###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonCheckRelState.tcl.rca $ $Revision: 1.55 $
#
# @libdoc       eServiceCheckRelState.tcl
#
# @Library:     Interface for checking related objects' state
#
# @Brief:       Check this objects children prior to promotion of parent.
#
# @Description: This program checks if the children objects related
#                      to the parent with the specified relationships have
#                      reached the state given for their type.
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
# Load MQL/Tcl Toolkit Libraries.
#
###############################################################################
eval  [utLoad eServicecommonTranslation.tcl]
eval  [utLoad  eServiceSchemaVariableMapping.tcl]

###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServiceCheckRelState
#
# @Brief:       Check state of related objects
#
# @Description: Check the incoming parameters for validity,
#                          obtain their "lookup" names in case of customer
#                          modification and pass on the correctly formatted
#                          parameters to the professional services methods.
#
#                          The intent of this program is to provide a function
#                          which checks the state of all objects of
#                          a named object type related to a parent object.
#                          The returned value will inform the parent if all the
#                          requested related objects are at a given state so
#                          that the parent can be promoted to the next state.
#
# @Parameters:  sRelationship        - Relationship to expand from, mutltiple relationships
#                                      can be entered as a string delimited with commas.
#                                      Ex. Part,Drawing Specification
#                                      Passing in one of the following will expand on all
#                                      relationships:  * or "" (NULL).
#               sDirection           - The direction to expand.  Valid entries are
#                                      from and to.  A NULL string or any other value
#                                      will expand in both directions.
#               sTargetObject        - Object to expand on, multiple objects can be entered
#                                      as a string, delimited with commas.
#                                      Ex. Part,Drawing Print,ECO
#                                      Passing in one of the following will expand on all
#                                      objects:  * or "" (NULL).
#               sTargetState         - The state being checked for.
#               sComparisonOperator  - Operator to check state with. Valid entries are
#                                      LT, GT, EQ, LE, GE, and NE.
#               sObjectRequirement   - Set flag if an object should be connected.  If expand
#                                      command returns no objects an error will be displayed.
#               sStateRequirement    - Set flag if target state should be present. If expand
#                                      command returns object with no target state present in it.
#
#               Input via RPE:
#                   sParentType      - Parent object's type
#                   sParentName      - Parent object's name
#                   sParentRev       - Parent object's revision number
#
# @Returns:     0 if all children are in a valid state.
#               1 if any child is in an invalid state.
#
# @Usage:       For use as trigger check on promotion
#
# @Example:     eServiceCheckRelState Type Name Rev "Part,Specification"
#                     to  "Part,Drawing Print" Release GE
#
# @procdoc
#************************************************************************

proc eServicecommonCheckRelState  { sParentType sParentName sParentRev sRelationship sDirection sTargetObject sTargetStateProp sComparisonOperator sObjectRequirement sStateRequirement} {
    # Set program name
    set progname "eServiceCheckRelState.tcl"
    # Initialize variables
    set mqlret 0
    set retCode 0
    set sMsgs ""
    set dMsgs ""
    set aMsgs ""
    set lObjs {}
    set bParentState 0
    # If no Target state is defined use current state of object
    if {[string compare $sTargetStateProp ""] == 0} {
        set sCmd {mql print bus "$sParentType" "$sParentName" "$sParentRev" select current dump}
        set mqlret [catch {eval $sCmd} outstr]
       
        # Set Target State
        set sTargetState $outstr
        set bParentState 1
    }
    if {$mqlret == 0} {
        # Create expand command
        set sCmd {mql expand bus "$sParentType" "$sParentName" "$sParentRev"}

        # Set expand direction if entered
        set sDirection [string tolower [string trim $sDirection]]
        if {"$sDirection" == "to" || "$sDirection" == "from"} {
            append sCmd { "$sDirection"}
        }

        if {[string compare $sRelationship ""] == 0} {
            set sRelationship "*"
        }
        append sCmd { relationship "$sRelationship"}

        if {[string compare $sTargetObject ""] == 0} {
            set sTargetObject "*"
        }
        append sCmd { type "$sTargetObject"}

        append sCmd { select bus current policy dump |}

        # Execute expand command
        set mqlret [catch {eval $sCmd} outstr]

        # If required flag was set, check if an object was found, if not error out
        if {[string compare $outstr ""] == 0 && [string tolower $sObjectRequirement] == "required"} {
            set outstr [mql execute program emxMailUtil -method getMessage \
                            "emxFramework.ProgramObject.eServicecommonCheckRelState.NoObject" 2 \
                                  "Rel" "$sRelationship" \
                                  "Object" "$sTargetObject" \
                            "" \
                       ]
            set mqlret 1
        }

        if {$mqlret == 0} {


            set sStateChange 0
            set sDevPart 0
            set sEcPart 0
            set sState $sTargetStateProp


            # Create a list of all matching objects and check their state
            set lObjs [split $outstr \n]
            foreach i $lObjs {
                set sChildType    [lindex [split $i |] 3]
                set sChildName    [lindex [split $i |] 4]
                set sChildRev     [lindex [split $i |] 5]
                set sChildState   [lindex [split $i |] 6]
                set sChildPolicy  [lindex [split $i |] 7]

             # If a dvlp part, then we need to equate "Approved" (EC Part State) to 
             # "Complete" (Dvlp part state) and "Review" (EC Part State) 
             # to "Peer Review" (Dvlp Part State)

                set sRegProgName  "eServiceSchemaVariableMapping.tcl"
                set sDvlpPartPolicy  [eServiceGetCurrentSchemaName policy $sRegProgName "policy_DevelopmentPart"]


                if {$sStateChange == 1} {
                    set sTargetStateProp $sState
                    set sStateChange 0
                }


   # Commented and added the below code For the bug 307577
                 if {[string compare "$sChildPolicy"  "$sDvlpPartPolicy"] == 0} {
                    if {[string compare $sTargetStateProp "state_Approved"] == 0}  {
                       set sTargetStateProp "state_Complete"
                    }  elseif {[string compare "$sTargetStateProp" "state_Review"] == 0} {
                        set sTargetStateProp "state_PeerReview"
                     }

                  set sStateChange 1
                }

                 if {[string compare "$sChildPolicy"  "$sDvlpPartPolicy"] == 0} {
                       if {$sDevPart == 0} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidObject" 3 \
                                                    "Type" "$sParentType" \
                                                    "Name" "$sParentName" \
                                                    "Rev" "$sParentRev" \
                                                "" \
                                           ]
                                set dMsgs $outstr
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.DevDescription" 3 \
                                                    "Type" "$sParentType" \
                                                    "Name" "$sParentName" \
                                                    "Rev" "$sParentRev" \
                                                "" \
                                           ]
                                append dMsgs $outstr
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.DevObject" 3 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                "" \
                                           ]
                                set retCode 1
                                set sDevPart 1
                                append dMsgs $outstr
                       } else {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.DevObject" 3 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                "" \
                                           ]
                                set retCode 1
                                append dMsgs $outstr
                       }
                  }

   # till here For bug 307577

                if {$bParentState == 0} {
                    set sTargetState [eServiceLookupStateName "$sChildPolicy" "$sTargetStateProp" ]
                    if {[string compare $sTargetState ""] == 0} {
                        if {[string tolower $sStateRequirement] == "required"} {
     	                    # Error out if not registered
                            set outstr [mql execute program emxMailUtil -method getMessage \
                                            "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidState" 2 \
                                                "State" "$sTargetStateProp" \
                                                "Policy" "$sChildPolicy" \
                                            "" \
                                       ]
     	                    set mqlret 1
     	                } else {
     	                    continue
     	                }
     	            }
                }

              if {[string compare "$sChildPolicy"  "$sDvlpPartPolicy"] != 0} {
                # Get all states for object
                set sCmd {mql print bus "$sChildType" "$sChildName" "$sChildRev" select state dump}
                set mqlret [catch {eval $sCmd} outstr]
                if {$mqlret == 0} {
                    set stateRow [split $outstr ,]

                    # Get index location of Target State
                    set indexTargetState [lsearch $stateRow $sTargetState]
                    
                    # Check if state is in objects policy
                    if {$indexTargetState < 0} {
                        if {[string tolower $sStateRequirement] == "required"} {
                            set retCode 1
                            set outstr [mql execute program emxMailUtil -method getMessage \
                                            "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidTargetState" 5 \
                                                "Type" "$sChildType" \
                                                "Name" "$sChildName" \
                                                "Rev" "$sChildRev" \
                                                "Policy" "$sChildPolicy" \
                                                "State" "$sTargetState" \
                                            "" \
                                       ]
                            append sMsgs $outstr
                            continue
                        } else {
                            continue
                        }
                    }

                    # Get index location for object
                    set index [lsearch $stateRow $sChildState]

                    # Check Target State index with object index location
                    switch $sComparisonOperator {
                        LT {
                            if {$index >= $indexTargetState} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrAfter" 5 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                    "State" "$sChildState" \
                                                    "TargetState" "$sTargetState" \
                                                "" \
                                           ]
                                set retCode 1
                                append sMsgs $outstr
                            }
                        }

                        GT {
                            if {$index <= $indexTargetState} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrBefore" 5 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                    "State" "$sChildState" \
                                                    "TargetState" "$sTargetState" \
                                                "" \
                                           ]
                                set retCode 1
                                append sMsgs $outstr
                            }
                        }

                        EQ {
                            if {$index != $indexTargetState} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.NotIn" 4 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                    "State" "$sTargetState" \
                                                "" \
                                           ]
                                set retCode 1
                                append sMsgs $outstr
                            }
                        }

                        LE {
                            if {$index > $indexTargetState} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.After" 5 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                    "State" "$sChildState" \
                                                    "TargetState" "$sTargetState" \
                                                "" \
                                           ]
                                set retCode 1
                                append sMsgs $outstr
                            }
                        }

                        GE {

                            if {$index < $indexTargetState} {
                           if {$sEcPart == 0} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.ECDescription" 3 \
                                                    "Type" "$sParentType" \
                                                    "Name" "$sParentName" \
                                                    "Rev" "$sParentRev" \
                                                "" \
                                           ]
                                append sMsgs $outstr
                                set sEcPart 1
                           }
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.Before" 5 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                    "State" "$sChildState" \
                                                    "TargetState" "$sTargetState" \
                                                "" \
                                           ]
                                set retCode 1
                                append sMsgs $outstr
                            }
                        }

                        NE {
                            if {$index == $indexTargetState} {
                                set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.Equal" 4 \
                                                    "Type" "$sChildType" \
                                                    "Name" "$sChildName" \
                                                    "Rev" "$sChildRev" \
                                                    "State" "$sChildState" \
                                                "" \
                                           ]
                                set retCode 1
                                append sMsgs $outstr
                            }
                        }

                        default {
                            set outstr [mql execute program emxMailUtil -method getMessage \
                                                "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidOperator" 1 \
                                                    "Operation" "$sComparisonOperator" \
                                                "" \
                                           ]
                            set mqlret 1
                            break
                        }

                    }
                } else {
                    set mqlret 1
                    break
                }
            }
        }
    }
    }
    if {$retCode == 1} {
        append aMsgs $dMsgs
        append aMsgs $sMsgs
        mql notice $aMsgs
        set mqlret 1
    } elseif {$mqlret == 1} {
        mql notice "$outstr"
    }

    return $mqlret
}

# end eServiceCheckRelState

# End of Module

