###############################################################################
# $RCSfile: eServicecommonRevisionGeneratorParameters.tcl.rca $ $Revision: 1.20 $
#
# @progdoc      eServicecommonRevisionGeneratorParameter.tcl
#
# @Brief:       The revision generator parameters
#
# @Description: This program is loaded at the begining of revision generator
#               It defines some of the parameter on which revision generator
#               depends.
#
# @progdoc      Copyright (c) 1993-2015, Dassault Systemes. All Rights Reserved.
#               This program contains proprietary and trade secret
#               information of Matrix One, Inc.  Copyright notice is
#               precautionary only and does not evidence any actual or
#               intended publication of such program.
###############################################################################

# Constant Parameters used by Revision Generator Program

# Default Vault if vault selected while Revision Generator is getting executed is "ADMINISTRATOR"
set sDefaultVault "vault_eServiceSample"

# Maximum Amount of time in seconds for which name generator should try to create an object.
set sMaxTryTimeLimit 600

# Maximum Amount of time in seconds for which name generator should wait for locked object.
set sMaxLockTimeLimit 60

# Maximum Amount of time in miliseconds to wait after each iteration.
set sIterDelay 10

# Number Generator Object Name
set sNumberGenName "Revision Generator"

# Number Generator Object Rev
set sNumberGenRev ""

