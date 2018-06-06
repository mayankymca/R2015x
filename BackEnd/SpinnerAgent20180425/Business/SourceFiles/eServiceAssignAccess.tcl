
###############################################################################
#
# $RCSfile: eServiceAssignAccess.tcl.rca $ $Revision: 1.6 $
#
# @progdoc     eServiceAssignAccess.tcl
#
# @Brief:      This program allows you to all a role to all the users in the 
#              database that match a pattern.
#
# @Description: This program will allow customer's the ability to quickly
#               assign a role to all the persons in the database
#
# @Parameters:  
#               param1: user pattern (* is all or Test* or "Test Everything,Test Buyer")
#               param2: role/group (admin type to assign role or group)
#               param3: Name (the name of the role or group to assign
#               param4: add|remove (action to execute add or remove) 
#               
#
# @Returns:    none
#
# @Usage:      run eServiceAssignAccess.tcl "<param1>" "<param2>" "<param3>" "<param4>";
#
# @Example:    Launch MQL and set context to creator or a business administrator
#              MQL<1> set context user creator password .....; 
#              MQL<2> tcl; 
#              % source eServiceAssignAccess.tcl
#              % eServiceAssignAccess "Test*" "role" "Exchange User" "add";
#               
#               
#
# @progdoc      Copyright (c) 2001, Matrix One, Inc. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
#
#
###############################################################################

mql verbose on;
mql trigger off;
mql quote off	;

proc eServiceAssignAccess { sPattern sType sName sAction }  { 

puts "executing procedure eServiceAssignAccess"
puts "Parameters passed: "
puts "paramter 1: $sPattern ";
puts "paramter 2: $sType ";
puts "paramter 3: $sName ";
puts "paramter 4: $sAction ";
puts "\n";



#-- Validation Section 
# set example
set sExample {eServiceAssignAccess "<param1>" "<param2>" "<param3>" "<param4>";}
set sExample2 { Parameters:   
 param1: user pattern (* is all or Test* or smith*)
 param2: role/group (admin type to assign role or group)
 param3: Name (the name of the role or group to assign
 param4: add|remove (action to execute add or remove) 
}

# Validate the parameters entered
if { "$sPattern" == "" || "$sType" == "" || "$sName" == "" || "$sAction" == "" } { 
  puts "all parameters must be entered";
  puts "\n$sExample\n$sExample2"
  return 1;
} 

#  validate the admin type - must be role or group
if {"$sType" != "role" || "$sName" == "group"  } { 
  puts "Parameter 2 must be 'role' or 'group' ";
  puts "\n$sExample\n$sExample2"
  return 1;
} 

#  validate action - must be add or remove
if {"$sAction" != "add" && "$sAction" != "remove"  } { 
  puts "Parameter 4 must be 'add' or 'remove' ";
  puts "\n$sExample\n$sExample2"
  return 1;
} 

# validate that the role or group entered exist in database 
set lAdmin [split [mql list $sType "$sName"] \n]
if { [lsearch $lAdmin "$sName"] == 1 } { 
  puts "Invalid $sType '$sName'. '$sName' does not exists in the database"
  return 1;
}

#- Set-up Section

#  validate action
if {"$sAction" == "add" } { 
  set sAction "assign";
} 
if {"$sAction" == "remove" } { 
  set sAction "remove assign";
} 


#
set lPersons [mql list person "$sPattern" ]
set lPersons [split $lPersons \n]
foreach  i $lPersons { 
 set sCmd "mql modify person \"$i\" $sAction $sType \"$sName\""
 puts $sCmd
 set mqlret [catch {eval $sCmd} MqlErr]
 if { $mqlret != 0 } {
   puts "Error $i\n$MqlErr" 
 } else { 
   puts "'$i' - $sAction $sType '$sName'"; 
 }  
}



# End of Eval 
} 

