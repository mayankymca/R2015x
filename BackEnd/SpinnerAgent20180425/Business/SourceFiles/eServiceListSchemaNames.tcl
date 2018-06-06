###############################################################################
#
# $RCSfile: eServiceListSchemaNames.tcl.rca $ $Revision: 1.8.1.1 $ 
#
# Description:  This file returns a all the property names and their values
#	        as a |(pipe) seperated list
# DEPENDENCIES: 
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

     set sProgramName "eServiceSchemaVariableMapping.tcl"
     set lProgram [split [mql list program] "\n"]
     set lFinalList {}
     set DUMPDELIMITER |
         
     if { $sProgramName == ""} {
	puts stdout "eServiceListSchemaNames : Program Name cannot be null"
	exit
     } elseif {[lsearch -exact $lProgram $sProgramName] == -1} {
	puts stdout "eServiceListSchemaNames : $sProgramName does not exist"
	exit
     } else {
	mql quote on;
        set lList [split [mql list property on program $sProgramName] "\n"]
        foreach lElement $lList {
          
           set lElement [split $lElement]                     
           set sPropertyName  [ lindex $lElement 0]
                     
           # count tells us the number of admin objects or their names
           # having space in them
           set count [regsub -all \' $lElement {:} lElement]
             
	       if {$count == 0} {            
	                
	           set sPropertyValue [ lindex $lElement 6]                
	                
	       } elseif {$count == 2} {
	             
	           # only one occurance of space between words
	           # it could be admin object or its name           
	                   	                   
	           set lElement [split $lElement ":"]
	           set lElementLength [llength $lElement]
	           if {$lElementLength == 3} {
	               set sPropertyValue [ lindex $lElement 2]
	               if {$sPropertyValue == ""} {
	                   set sPropertyValue [ lindex $lElement 1]
	               }
	           }
	               
                   #added for Bug No 339017  Begin
                    if {$lElementLength >= 4} {
                           set colon ":";
                           set lElement2 [ lindex $lElement 1]
                           for { set i 2} { $i < $lElementLength-1} { incr i } {
                                 set lElement1 [ lindex $lElement $i]
                                 set lElement2 $lElement2$colon$lElement1
                                }
                            set sPropertyValue $lElement2
                            if {$sPropertyValue == ""} {
                            set sPropertyValue [ lindex $lElement 1]
                       }
                   }
                   #added for Bug No 339017  End
	       } elseif {$count == 4} {
	             
	           # both admin object & its name are having space
	           # separated words for names          
	                  
	           set lElement [split $lElement ":"]
	           set sPropertyValue [ lindex $lElement 3]	               
	               
	       }
	    
	    if {($sPropertyName != "") && ($sPropertyValue != "")} {
	    
	       append lFinalList "$sPropertyName$DUMPDELIMITER$sPropertyValue$DUMPDELIMITER"
	       	            
	    } 
#end of foreach loop	       
        } 
         mql quote off;
         return $lFinalList
# end of if elseif
     }
}

################################################################################

