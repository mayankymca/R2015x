#************************************************************************
# @progdoc        emxSpinnerSettings.tcl
#
# @Brief:         User definable settings for emxSpinnerAgent.tcl
#
# @Usage:         Modify parameters as commented below.  
#
# @progdoc        Copyright (c) 2005, ENOVIA Inc.
#************************************************************************
# @Modifications:
#
# @FirstName LastName --/--/--  - Description
#  
#************************************************************************

#  ********************** USER DEFINED VARIABLES*********************************
#  WARNING - DO NOT CHANGE SETTINGS UNTIL YOU ARE FAMILIAR WITH DEFAULT BEHAVIOR!
#  ******************************************************************************
   set sParentChild "child"        ;# parent or child - use parent or child fields in roles and groups - cannot use both!
   set bUseAssignmentField "FALSE" ;# TRUE or FALSE - process person assignments set in role/group data
#                                   Note: recommend using group and role assignments in person data file instead
   set bUseAccessField "FALSE"     ;# TRUE or FALSE - process user access assignments in rules and policy states
#                                   Note: strongly recommended to use policy/rule access files instead
   set bRetainBusObject "TRUE"     ;# TRUE or FALSE - retain bus objects current states when resequencing policy states
#                                   Note: For large data sets, set to FALSE and use data migration methods instead
   set bImportOverwrite "FALSE"    ;# TRUE or FALSE - overwrite import if it exists
   set bBusObjOverwrite "FALSE"    ;# TRUE or FALSE - overwrite bus objects in emxSpinnerBusObjects.tcl if exists
   set bBusRelOverwrite "FALSE"    ;# TRUE or FALSE - overwrite bus rels in emxSpinnerBusRels.tcl if exists
   set bTriggerAdd "OFF"           ;# ON or OFF - turns global create triggers on or off for bus objects and connections
   set bTriggerMod "OFF"           ;# ON or OFF - turns global modify triggers on or off for bus objects and connections
   set bTriggerDel "OFF"           ;# ON or OFF - turns global delete triggers on or off for bus objects and connections
   set bTriggerChk "OFF"           ;# ON or OFF - turns global checkin triggers on or off for bus object files
   set sReplaceSymbolic "&?%$ ()"  ;# characters to be trimmed when creating symbolic names
   set sDelimiter "tab"            ;# data delimiter: select tab, pipe, comma, carot, tilde or newline
   set sRangeDelim "pipe"          ;# attr range delimiter (use different one than data delimiter!)
   set bShowModOnly "TRUE"         ;# TRUE or FALSE - only modifications are logged when set to TRUE
   set bStreamLog "FALSE"          ;# TRUE or FALSE - output log to screen if TRUE
   set bShowTransaction "FALSE"    ;# TRUE or FALSE - show transaction boundaries during logging
   set bCompile "FALSE"             ;# TRUE or FALSE - compile JPO's added or modified if TRUE
   set bOverlay "TRUE"             ;# TRUE or FALSE - skip blank entries, append/merge list elements
#                                   Notes: - Overlay not applicable to access, system, bus object or rel data files
#                                          - Use <NULL> to force null values
#                                          - Use double tags to delete list items (e.g. <<Delete Element>> )
   set lsSubDirSequence [list ""]  ;# list of subdirectories for sequenced builds (e.g. [list Spinner1 Spinner2 ""]
#                                   Note: "" is current working directory.  In example, ./Spinner/Spinner1 is run first,
#                                         ./Spinner/Spinner2 second, ./Spinner last.  Read Schema_Agent.htm for usage 
   set lsFileExtSkip [list ".bak" ".scc" ".tmp"] ;# list of file extensions to skip when reading data files
   set rRefreshLog 1               ;# time interval in days to reset SpinDone.log (forces processing of all data files)
   set bAbbrCue "FALSE"            ;# TRUE or FALSE - abbreviate cues to screen if TRUE
   set iBusObjCommit 1000          ;# number of bus objects to process before committing when renaming states
   set bForeignVault "FALSE"       ;# TRUE or FALSE - use foreign vaults in bus object queries
   set bContinueOnError "FALSE"    ;# TRUE or FALSE - flag, but do not halt on errors
   set bChangeAttrType "FALSE"     ;# TRUE or FALSE - allow attribute type to be changed (e.g. string to real)
#                                   WARNING: Attribute must be deleted then added to change type so may cause loss of data
   set bPersonOverwrite "TRUE"     ;# TRUE or FALSE - overwrite person objects in emxSpinnerPerson.tcl if exists
   set bCDM "TRUE"                 ;# TRUE or FALSE - checkin files using CDM format
   set bOut "TRUE"                 ;# TRUE or FALSE - display warning messages to console
   set bJPOExtraction "TRUE"	   ;# TRUE or FALSE - extract JPO with _mxJPO.java extension, extract JPO with no extension
   set bCommandFile "FALSE"  ;# TRUE or FALSE - extract/import of code of command in a file if set to TRUE, else code of command will be in Spinner file.
   set lsSkipSchemaExtraction [list ""] ;# list of schema elements to be skipped during full spinner export
   set bResequenceStates "FALSE" ;# TRUE or FALSE - if TRUE then state will be resequenced, if FALSE policy states will not be resequenced
   set sLogDir ""      ;#Specify Log folder path to generate logs during extract/import.

   #set bJava "TRUE"

   set bJavaExtraction "TRUE"      ;# TRUE or FALSE - extract files using java (high performance / fast extraction), extract files using tcl logic
   set sEncodingSystem [encoding system]  ;#For UTF-8 Setting
   set sEncofingUTF [encoding system utf-8] ;#To display japanese characters

   set sGlobalConfigType [list ""]   ;#list of business object to update  Global Config objects through spinner.
#  ****************************************************************************

