###############################################################################
#
# $RCSfile: eServicecommonPSUtilities.tcl.rca $  $Revision: 1.18 $ 
#
# Description:  Contains tcl procedures defining settings for
#               MatrixOne Application programs.
#
######################################################################

#############################################################
# The following procedures are taken from mxType.tcl
# called from eServicecommonObjectGenerator.tcl
#############################################################

proc mxTypeGetDfltPolicy sType {
    set sPolicies [ mxTypeGetPolicies $sType ]
    set sDfltPolicy [ lindex $sPolicies 0 ]
    return $sDfltPolicy 
}

proc mxTypeGetPolicies sType {
    set sPolId "policy "
    set sPolicies ""
    set sOutput [ string trim [ mql print type $sType ] ]
    set sOutput [ split $sOutput \n ]
    set cchPolicy [ string length $sPolId ]
    foreach sItem $sOutput {
        set sItem [ string trim $sItem ]
        set ichPolicy [ string first $sPolId $sItem ]
        if { $ichPolicy > -1 } {
            incr ichPolicy $cchPolicy
         
            set sPolicies [ string range $sItem $ichPolicy end ] 
            set sPolicies [ split $sPolicies "," ]
            break
        }
    }
    return $sPolicies
}

#####################################################################
# The following procedure is taken from mxPol.tcl
# called from eServicecommonObjectGenerator.tcl, eServicepomCreatePartWzrdExec.tcl
#####################################################################


proc mxPolGetInitRev { sPolicy } {
    set sInitRev ""
    set sRevSequence [ mql print policy "$sPolicy" select revision dump ]
    if { $sRevSequence != "" } {
        set cchRevSeq [ string length $sRevSequence ]
        set iRevSeq 0
        set ch [ string index $sRevSequence $iRevSeq ]
        if { ( $ch == "\[" ) || ( $ch == "(" ) } {
               
            incr iRevSeq
        }
        for { set iRevSeq } { $iRevSeq < $cchRevSeq } { incr iRevSeq } {
            set ch [ string index $sRevSequence $iRevSeq ]
            if { ( $ch == "," ) || ( $ch == "-" ) } {
                    
                break
            
            } else {
                set sInitRev $sInitRev$ch
            }
        }
        if { $sInitRev == "" } {
            set sInitRev [ lindex [ split $sRevSequence , ] 0 ]
        }
    }
    return $sInitRev
}

#####################################################################
# The following procedure is taken from utStr.tcl
# called from eServicecommonObjectGenerator.tcl
#####################################################################

proc utStrSequence { sString { fAddDigit 0 } { lSkip {} } } {
    set cchString [ string length $sString ]
    incr cchString -1
    set sNextString ""
    set fIncrementNextColumn 0
    for { set ich $cchString } { $ich >= 0 } { incr ich -1 } {
        set ch [ string index $sString $ich ]
        if { [ regexp {[0-9]} $ch ] == 1 } {
            if { $ch < 9 } {
                set fIncrementNextColumn 0
                incr ch
                while { [ lsearch $lSkip $ch ] >= 0 } { incr ch }
                set sNextString "$ch$sNextString" 
                break
            
            } else {
            
                set fIncrementNextColumn 1
                set sNextString "0$sNextString" 
            }
        } elseif { [ regexp {[A-Z]} $ch ] == 1 } {
            if { $ch != "Z" } {
                set fIncrementNextColumn 0
                set ch [ utStrIncrChar $ch ]
                while { [ lsearch $lSkip $ch ] >= 0 } {
                    set ch [ utStrIncrChar $ch ]
                }
                set sNextString "$ch$sNextString" 
                break
            } else {
                set fIncrementNextColumn 1
                set sNextString "A$sNextString" 
            }
        } elseif { [ regexp {[a-z]} $ch ] == 1 } {
            if { $ch != "z" } {
                set fIncrementNextColumn 0
                set ch [ utStrIncrChar $ch ]
                while { [ lsearch $lSkip $ch ] >= 0 } {
                    set ch [ utStrIncrChar $ch ]
                }
                set sNextString "$ch$sNextString" 
                break
                
            } else {
                set fIncrementNextColumn 1
                set sNextString "a$sNextString" 
            }
        } else {
            set sNextString "$ch$sNextString" 
        }
    }
    incr ich -1
    for { set ich } { $ich >= 0 } { incr ich -1 } {
    
        set ch [ string index $sString $ich ]
        set sNextString "$ch$sNextString" 
    }
    if { ( $fIncrementNextColumn == 1 ) || ( $sNextString == $sString ) } {
    
        if { $fAddDigit == 1 } {
            set ch [ string index $sString 0 ]
            if { [ regexp {[0-9]} $ch ] == 1 } {
                set sNextString "1$sNextString"
            } elseif { [ regexp {[A-Z]} $ch ] == 1 } {
                set sNextString "A$sNextString"
            } elseif { [ regexp {[a-z]} $ch ] == 1 } {
                set sNextString "a$sNextString"
            } else {
                set sNextString ""
            }
        } else {
            set sNextString ""
        }
    }
    return $sNextString
}

#############################################################
# The following procedure is taken from mxAttr.tcl
# called in eServicecommonWzrdListAttrRange.tcl
#############################################################


proc mxAttGetRanges { sAttr { sOperator {=} } } {
    set lValidOps [ list {=} {!=} {<} {>} {<=} {>=} ]
    if { [ lsearch $lValidOps $sOperator ] >= 0 } {
        set lReturn {}
        set lRanges [ mql print att "$sAttr" select range dump | ]
        set lRanges [ split $lRanges | ]
        foreach sRange $lRanges {
            if { [ string match "$sOperator *" $sRange ] } {
                set sValue [ string trim [ lrange $sRange 1 end ] ]
                lappend lReturn $sValue
            }
        }
        return $lReturn
    } else {
        return -1
    }
}

proc mxBusList { sType sName sRev { sLattice "*" } \
                 { sReturnMethod "spec" } } {
    mql quote on    
    set sOutput [ mql temporary query \
                   bus "$sType" "$sName" "$sRev" \
                   lattice $sLattice ]
    mql quote off
    set lOutput [ split $sOutput \n ]
    if { $sReturnMethod != "spec" } {
        set llObjs {}
        foreach sObj $lOutput {
            set lObj [ mxBusParseSpec $sObj ]
            lappend llObjs $lObj
        }
        return $llObjs
    }
    return $lOutput
}

proc mxBusParseSpec { sBus } {
  if { [ regexp {^("[^"]*"|'[^']*'|[^ ]+) +("[^"]*"|'[^']*'|[^ ]+) +("[^"]*"|'[^']*'|[^ ]+)$} $sBus dummy sType sName sRev ] == 1 } {
    set sType [ string trim $sType "\'\"" ]
    set sName [ string trim $sName "\'\"" ]
    set sRev  [ string trim $sRev "\'\"" ]
    set lBus [ list $sType $sName $sRev ]
  } else {
    set lBus {}
  }
  return $lBus
}

