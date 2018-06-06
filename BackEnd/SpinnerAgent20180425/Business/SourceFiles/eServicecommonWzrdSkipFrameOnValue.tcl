#************************************************************************
# $RCSfile: eServicecommonWzrdSkipFrameOnValue.tcl.rca $ $Revision: 1.18 $
# @progdoc        mxWzrdSkipFrameOnValue.tcl
#
# @Brief:         Skip a Frame Due to a Value in the RPE
#
# @Description:   This procedure will skip a frame by comparing a value
#                 in the RPE with the given value and operator.
#
# @Parameters:    1 - Name of the RPE variable
#                 2 - Operator to use; valid values are \"==\", \"!=\", \">\",
#                     and \"<\"
#                 3 - Value to compare to the RPE variable's value
#
# @Returns:       0 if the comparison passes; frame is entered
#                 1 if the comparison fails; frame is skipped
#
# @Usage:         This program should be implemented as a frame prologue.
#
#                 The following parameter input string will make sure
#                 the \"Part Size\" widget does not have a value of
#                 \"TBD\"
#
#                 {Part Size} != TBD
#
# @progdoc        Copyright (c) 1998, MatrixOne
#************************************************************************

tcl;

eval {

    set sRpeValue [ mql get env [ mql get env 1 ] ]
    set sOperator [ mql get env 2 ]
    set sChkValue [ mql get env 3 ]

    set iExit 1

    switch $sOperator {
        =  -
        == { if { $sRpeValue == $sChkValue } { set iExit 0 } }
        != { if { $sRpeValue != $sChkValue } { set iExit 0 } }
        >  { if { $sRpeValue >  $sChkValue } { set iExit 0 } }
        <  { if { $sRpeValue <  $sChkValue } { set iExit 0 } }
    }

    exit $iExit

# End eval
}

