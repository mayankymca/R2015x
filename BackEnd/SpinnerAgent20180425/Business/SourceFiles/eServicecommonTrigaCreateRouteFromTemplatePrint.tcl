###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonTrigaCreateRouteFromTemplatePrint.tcl.rca $ $Revision: 1.46 $
#
# @libdoc       eServicecommonTrigaCreateRouteFromTemplatePrint
#
# @Library:     Interface to create a Route from a Template Route
#
# @Brief:       Create a Route based off a Templeate Route Definition allowing
#               for the use of a Print statement to navigate to related
#               information, if desired.
#
# @Description: see procedure description
#
# @libdoc       Copyright (c) 2001, Matrix One, Inc. All Rights Reserved.
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

###############################################################################
#
# Procedure:    pCheckValue
#
# Description:  Procedure to compare print statement value with check value.
#
# Parameters:   sValue - Value from print statement
#               sCheckValue - Value to check against print statement value.
#               sOperator - The Comparison Operator to check state with. Valid
#                           values are  LT, GT, EQ, LE, GE, and NE.
#
# Returns:      O - Create Route
#               1 - Do not create Route
#
###############################################################################

proc pCheckValue {sValue sCheckValue sOperator} {
    set iReturn 0

    switch $sOperator {
        LT {
            if {$sValue >= $sCheckValue} {
                set iReturn 1
            }
        }

        GT {
            if {$sValue <= $sCheckValue} {
                set iReturn 1
            }
        }

        EQ {
            if {$sValue != $sCheckValue} {
                set iReturn 1
            }
        }

        LE {
            if {$sValue > $sCheckValue} {
                set iReturn 1
            }
        }

        GE {
            if {$sValue < $sCheckValue} {
                set iReturn 1
            }
        }

        NE {
            if {$sValue == $sCheckValue} {
                set iReturn 1
            }
        }
    }

    return $iReturn
}
# end pCheckValue

###############################################################################
#
# Load MQL/Tcl utility procedures
#
###############################################################################
eval  [utLoad eServicecommonCreateRouteFromTemplate.tcl]
eval  [utLoad eServicecommonTranslation.tcl]
###############################################################################
#
# Define Procedures
#
###############################################################################

#******************************************************************************
# @procdoc      eServicecommonTrigaCreateRouteFromTemplatePrint
#
# @Brief:       Create a Route based off a Templete Route Definition passing
#               in a Print statement to navigate to related information to
#               determine whether to create Route or not.
#
# @Description: This program will create a Route object and connect it to an
#               object.  Route will be associated to state passed in and the
#               Policy passed in.
#               Logic can be passed in through a mql print statement to determine
#               if a Route should be created.  The Print statement is an argument
#               passed into this program, if one is not supplied there is no
#               check for any related information and a Route will automatically
#               be created.  Another argument to be passed in will determine
#               whether Route should be automatically started (Promoted to In
#               Process) or if an email should be sent to the owner of the route
#               instructing him/her to manually Promote the Route.
#
# @Parameters:     sSelect - Print Select Statement to use.
#                sObjectId - Object Id to execute print statement against.
#                   sValue - Value to compare print statement results against.
#          sResultOperator - Result Operator.  All values returned from select statement are
#                            compared against passed in value (sValue).  The operator passed
#                            in here will determine the logic used on print statement values.
#                            The results of this logic will determine whether Route should
#                            be created.
#                            Valid Inputs:
#                                 All (default) - All values must pass comparison in order
#                                                 to create Route.
#                                 SUMMATION - If the product of all values pass the comparison
#                                             create Route.
#                                 NONE - If none of the values pass the comparison create Route.
#                                 ANY - If any of the values pass the comparions create Route.
#      sComparisonOperator - The Comparison Operator to check print statement results against.
#                            Valid Inpute:
#                                 LT - Less Than
#                                 GT - Greater Than
#                                 EQ - Equal
#                                 LE - Less or Equal
#                                 GE - Greater or Equal
#                                 NE - Not Equal
#                    sType - Objects type that is being associated to Route
#                    sName - Name of Object that is being associated to Route
#                     sRev - Revision of Object being associated to Route
#            sTemplateType - Templates type to be copied
#            sTemplateName - Name of Template to be copied
#             sTemplateRev - Revision of Template to be copied
#              sRouteOwner - User to assign route to
#             sRouteAction - Action for route when route is complete
#                            Valid inputs:
#                                 Promote = Promote Connected Object
#                                 Notify = Notify Route Owner
#              sStartRoute - Manner in which to initiate route.  If set to Manual
#                            notification will be given to owner to start route
#                            if set to Automatic, route will start immediately.
#                            Valid inputs:
#                                 Manual
#                                 Automatic
#                             Note that if a blank route is created i.e. route template did
#                             not exist and sRouteCreateFlag (see below) is set, the Automatic setting
#                             will have no effect and manual notification will be given to owner
#                             to add members and tasks etc. and then to start route
#          sRouteBaseState - State to associate route to
#              sRouteVault - Vault to put route
#         sRouteBasePolicy - Policy to associate route to
#         sRouteCreateFlag - (optional) Flag as to whether to create a blank route if route template object
#                            specified does not exist.
#                             Valid Inputs:
#                                  CREATE - Create the route object even if the route template
#                                           object does not exist.
#                                  null value - error out if route template object does not exist.
#                             Default is null value.
#
# @Returns:     0 if route was successfully created
#               1 if there was an error
#
# @progdoc
#******************************************************************************
proc eServicecommonCreateRouteFromTemplatePrint {sSelect sObjectId sValue sResultOperator \
            sComparisonOperator sType sName sRev sTemplateType sTemplateName sTemplateRev \
            sRouteOwner sRouteAction sStartRoute sRouteBaseState sRouteVault sRouteBasePolicy {sRouteCreateFlag ""}} {
    #
    # Get Program names
    #
    set progname "eServicecommonTrigaCreateRouteFromTemplatePrint.tcl"
    #
    # Initialize variables
    #
    set bCreateFlag 0
    set mqlret 0

    #
    # Create list of valid inputs for sComparisonOperator
    #
    set lOperators [list LT GT EQ LE GE NE]

    #
    # Check if operator passed in is valid
    #
    if {[lsearch $lOperators $sComparisonOperator] == -1} {
        set mqlret 1
        set outStr [mql execute program emxMailUtil -method getMessage \
                    "emxFramework.ProgramObject.eServicecommonTrigaCreateRouteFromTemplatePrint.InvalidOperation" 2 \
                                  "Operation" "$sComparisonOperator" \
                                  "Operations" "$lOperators" \
                    "" \
                    ]
    }

    if {$mqlret == 0 && "$sSelect" != ""} {
        #
        # Remove all slashes from select statement if any
        #
        regsub -all {\\} $sSelect "" sSelect

        #
        # Add slashes to command to escape brackets
        #
        regsub -all {\[} $sSelect {\[} sSelect
        regsub -all {\]} $sSelect {\]} sSelect

        #
        # Build Command String
        #
        set sCmd {mql print bus $sObjectId select}
        append sCmd " $sSelect"
        append sCmd " dump |"

        #
        # Execute Command
        #
        set mqlret [catch {eval $sCmd} outStr]

        #
        # Error out if no value found
        #
        if {$outStr == ""} {
            set bCreateFlag 1
        }

        #
        # If no errors and print statement returned a value check value
        #
        if {$mqlret == 0 && $bCreateFlag == 0} {
            #
            # Create a list of values from all objects
            #
            set lData [split $outStr |]

            switch $sResultOperator {
                SUMMATION {
                    set iAnswer 0
                    foreach iDataValue $lData {
                        if {[catch {eval expr $iAnswer + $iDataValue} iAnswer]} {
                            set mqlret 1
                            set outStr $iAnswer
                            break
                        }
                    }

                    #
                    # Check summation against value using comparison operator
                    #
                    set bCreateFlag [pCheckValue $iAnswer $sValue $sComparisonOperator]
                }

                ANY {
                    set bCreateFlag 1
                    foreach sDataValue $lData {
                        #
                        # Check each value against value using comparison operator
                        #
                        set bCheckFlag [pCheckValue $sDataValue $sValue $sComparisonOperator]

                        if {$bCheckFlag == 0} {
                            set bCreateFlag 0
                            break
                        }
                    }
                }

                NONE {
                    foreach sDataValue $lData {
                        #
                        # Check each value against value using comparison operator
                        #
                        set bCheckFlag [pCheckValue $sDataValue $sValue $sComparisonOperator]

                        if {$bCheckFlag == 0} {
                            set bCreateFlag 1
                            break
                        }
                    }
                }

                default {
                    foreach sDataValue $lData {
                        #
                        # Check each value against value using comparison operator
                        #
                        set bCheckFlag [pCheckValue $sDataValue $sValue $sComparisonOperator]

                        if {$bCheckFlag == 1} {
                            set bCreateFlag 1
                            break
                        }
                    }
                }
            }
        }
    }
     
     if {$mqlret == 1} {
        set sErrMsg "$progname :\n"
        mql notice "$sErrMsg $outStr"
    } elseif {$bCreateFlag == 0} {
        set mqlret [eServicecommonCreateRouteFromTemplate $sType $sName $sRev $sTemplateType \
                $sTemplateName $sTemplateRev $sRouteOwner $sRouteAction $sStartRoute $sRouteBaseState $sRouteVault $sRouteBasePolicy $sRouteCreateFlag]
    }
    return $mqlret
}
# end eServicecommonTrigaCreateRouteFromTemplatePrint

}
# End of Module

