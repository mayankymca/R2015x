
#************************************************************************
# @progdoc        eServicecommonTrigcUnfloatRels_if.tcl
#
# @Brief:         Restore floated relationships when the object is deleted.
#
# @Description:   This program restores floated relationships that are
#                 deleted when the last business object revision is deleted.
#                 The relationships matching the given pattern are modified
#                 to connect to/from the previous revision of the object.
#
# @Parameters:    sRels - The relationship pattern
#
# @Returns:       Nothing
#
# @Usage:         This program must be implemented as a delete override
#                 trigger on a relationship type.
#
# @progdoc        Copyright (c) 2000, MatrixOne, Inc.
#************************************************************************

tcl;

# Start eval to prevent echo to stdout.
eval {

# Load necessary Mql/Tcl libraries.
eval [ mql print program eServicecommonUtil.tcl select code dump ]

# Get the relationship pattern from the input parameters.
set sRels [ mql get env 1 ]

# If the relationship pattern is blank, then default to an asterisk.
if { $sRels == "" } {
  set sRels "*"
}

# Get object ids of the current and previous revisions.
utCatch {
  set sId [ mql get env OBJECTID ]
  set sPrevRevId [ mql print bus $sId select previous.id dump ]
}

# If there is a previous revision, then unfloat the matching relationships.
if { $sPrevRevId != "" } {

  # Find the ids of all the "from" relationships.
  utCatch {
    set sExpand [ mql expand bus $sId from rel $sRels select rel id dump | ]
  }

  # Loop through the relationships and replace the object
  # on the "from" end with the previous revision.
  utCatch {
    foreach {x x x x x x sRelId} [ split $sExpand \n| ] {
      mql mod connection $sRelId from $sPrevRevId
    }
  }

  # Find the ids of all the "to" relationships.
  utCatch {
    set sExpand [ mql expand bus $sId to rel $sRels select rel id dump | ]
  }

  # Loop through the relationships and replace the object
  # on the "to" end with the previous revision.
  utCatch {
    foreach {x x x x x x sRelId} [ split $sExpand \n| ] {
      mql mod connection $sRelId to $sPrevRevId
    }
  }

}

# Exit with success.
exit 0

# End eval to prevent echo to stdout.
}



# End of Module


