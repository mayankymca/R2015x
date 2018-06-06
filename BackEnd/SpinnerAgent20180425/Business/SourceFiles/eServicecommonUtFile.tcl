###############################################################################
# NOTE: REMOVE THE FOLLOWING LINE BEFORE SUBMITTING TO PS DATABASE
# $RCSfile: eServicecommonUtFile.tcl.rca $ $Revision: 1.17 $
#
# @libdoc       eServicecommonUtFile.tcl
#
# @Library:     Tcl File Procedures
#
# @Brief:       Contains procedures for working with files
#
# @Description:
#               The Tcl File library contains procedures for working with files.
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

#************************************************************************
# @procdoc        utFileConvertBinary
#
# @Brief:         Converts characters from a binary file to ASCII
#
# @Description:   This procedure converts the specified number of
#                 characters in the given binary file to their ASCII
#                 equivalent.
#
# @Parameters:    pFile     - Pointer to opened file.
#                 cMaxChars - Maximum number of characters to read
#                             from file.
#                 chSub     - Character to replace all non-readable
#                             ASCII equivalent characters.
#
# @Returns:       String from file converted to ASCII equivalent
#
# @Usage:         The following code converts the first 50 characters
#                 in the file to their ASCII equivalent:
#
# @Example:       set pFile [ open "binary.dat" r ]
#                 set sAscii [utFileConvertBinary $pFile 50 ]
#
# @procdoc
#************************************************************************

proc utFileConvertBinary { pFile cMaxChars { chSub " " } } {

    fconfigure $pFile -translation binary

    set sRetStr ""
    set iChar 0

    while { [ eof $pFile ] == 0 && $iChar < $cMaxChars } {

        set iAscii 0
        scan [ read $pFile 1 ] %c iAscii

        if { ( $iAscii >= 32 && $iAscii <= 126 ) || \
             ( $iAscii >= 9  && $iAscii <= 11 ) || \
             $iAscii == 13 } {

            set sRetStr "$sRetStr[ format "%c" $iAscii ]"

        } else {

            set sRetStr "$sRetStr$chSub"
        }

        incr iChar
    }

    return $sRetStr
}
# end utFileConvertBinary

#************************************************************************
# @procdoc        utFileConvertName
#
# @Brief:         Convert a path name to work on a specified platform
#
# @Description:   Converts a file path to either a unix or windows
#                 file path.
#
# @Parameters:    sFileName - The name of the file path to convert
#                 sPlatform - (Optional) Defines which platform format
#                             to convert to.  Can be "current", "unix",
#                             or "windows".  If "current" is specified,
#                             then the file path is converted to the format
#                             needed by the current platform.
#                             (Default: current)
#
# @Returns:       The file path with converted to the appropriate platform
#
# @Usage:         The following calls to utFileConvertName show how to
#                 convert between unix and windows file paths:
#
# @Example:       utFileConvertName /temp/mxTmp.000
#                 ==> \temp\mxTmp.000
#                 utFileConvertName {\temp\mxTmp.000} unix
#                 ==> /temp/mxTmp.000
#
# @procdoc
#************************************************************************

proc utFileConvertName { sFileName { sPlatform "current" } } {

  if { "$sPlatform" == "current" } {
    if { [ utSysGetPlatform ] == "winnt" } {
      set sPlatform "windows"
    } else {
      set sPlatform "unix"
    }
  }

  switch $sPlatform {
    windows { regsub -all {/} $sFileName {\\} sNewFileName }
    unix    { regsub -all {\\} $sFileName {/} sNewFileName }
    default { error "utFileConvertName: Unsupported platform - '$sPlatform'" }
  }

  return $sNewFileName

}
# end utFileConvertName

#************************************************************************
# @procdoc        utFileCreateTmpDir
#
# @Brief:         Creates a temporary directory
#
# @Description:   Creates a temporary subdirectory under the temporary
#                 directory on the user's machine.  The name of the
#                 directory depends on the stlye specified.  If a "pattern"
#                 style is specified, then the name is based on the
#                 specified pattern (as defined in utFileGetUnique).
#                 If the style is "person", then the current person's
#                 name (with spaces replaced by underscores) is used
#                 as the subdirectory name.
#
# @Parameters:    sStyle   - (Optional) Defines the style of the temporary
#                            directory name.  Can be "pattern" or "person".
#                            (Default: pattern)
#                 sPattern - (Optional) Defines the pattern to use if the
#                            selected style is pattern.
#                            (Default: mxTmp.000)
#
# @Returns:       Path of the new temporary subdirectory
#
# @Usage:         The following calls to utFileCreateTmpDir show the
#                 different style of creating temporary directories:
#
# @Example:       utFileCreateTmpDir
#                 ==> /temp/mxTmp.000
#                 utFileCreateTmpDir pattern temp05
#                 ==> /temp/temp05
#                 utFileCreateTmpDir person
#                 ==> /temp/Joe_Engineer
#
# @procdoc
#************************************************************************

proc utFileCreateTmpDir { { sStyle "pattern" } { sPattern "mxTmp.000" } } {

  # Get the temporary directory name
  set sTmpDir [ utFileGetTmpDir ]

  # Create the directory name depending on selected style
  switch $sStyle {
    pattern {
      set sNewTmpDir $sTmpDir/[ utFileGetUnique $sTmpDir $sPattern ]
    }
    person {
      regsub -all { } [ lindex [ mxPerGetContext ] 1 ] _ sPerson
      set sNewTmpDir $sTmpDir/$sPerson
    }
    default {
      error "utFileCreateTmpDir: Unsupported style - '$sStyle'"
    }
  }

  # Create the temporary directory
  file mkdir $sNewTmpDir

  # Return the name of the newly created temporary directory
  return $sNewTmpDir

}
# end utFileCreateTmpDir

#************************************************************************
# @procdoc        utFileEnter
#
# @Brief:         Get a file path from the user
#
# @Description:   This procedure prompts the user to enter a file
#                 path.  Parameters exist for specifying error
#                 message.  If a parameter is set to empty quotes,
#                 its error check will not occur.
#
# @Parameters:    sPrompt  - Message asking the user to enter a file path
#                 sExists  - Message indicating that the entered
#                            file path does not exist
#                 sNoRead  - Message indicating that the current
#                            user can not read the file path
#                 sNoWrite - Message indicating that the current
#                            user can not write the file path
#                 sNotFile - Message indicating that the entered
#                            file path is not a file
#                 sNotDir  - Message indicating that the entered
#                            file path is not a directory
#                 args     - Optional characters used to exit from the prompt
#
# @Returns:       Returns a valid file path that was entered by the
#                 user.  Set to "", if the user exited the dialog.
#
# @Usage:         The following example prompts the user for a file path
#                 to save a file to:
#
# @Example:       utFileEnter "Enter Save File Path: " \
#                             ""                       \
#                             ""                       \
#                             "Can't write to file"    \
#                             ""                       \
#                             ""                       \
#                             "q"
#
# @procdoc
#************************************************************************

proc utFileEnter { sPrompt sExists sNoRead sNoWrite sNotFile \
                   sNotDir args } {

    set sPath ""

    if { $args == {} } {

        set lchExit {}

    } else {

        set lchExit $args
    }

    while { $sPath == "" } {

        puts $sPrompt
        gets stdin sPath
        set sPath [ string trim $sPath ]

        if { [ lsearch $lchExit $sPath ] >= 0 } {

            set sPath ""
            break

        } elseif { $sExists != "" && [ file exists $sPath ] == 0 } {

            puts $sExists
            set sPath ""

        } elseif { $sNoRead != "" && [ file readable $sPath ] == 0 } {

            puts $sNoRead
            set sPath ""

        } elseif { $sNoWrite != "" && [ file writable $sPath ] == 0 } {

            puts $sNoWrite
            set sPath ""

        } elseif { $sNotFile != "" && [ file isfile $sPath ] == 0 } {

            puts $sNotFile
            set sPath ""

        } elseif { $sNotDir != "" && [ file isdirectory $sPath ] == 0 } {

            puts $sNotDir
            set sPath ""
        }
    }

    return $sPath
}
# end utFileEnter

#************************************************************************
# @procdoc        utFileGetNextSection
#
# @Brief:         Get the next section in a file
#
# @Description:   This function returns the next "section" in the file.
#                 A section is defined as all lines between comment blocks.
#
# @Parameters:    file         - Pointer to the file.
#                 chComment    - Character in column one that indicates
#                                that the line is a comment.
#                 fIgnoreBlank - Pass "1" if blank lines in the section
#                                should be ignored; "0" otherwise.
#
# @Returns:       A list of all the lines in the section are returned.
#                 If there is only one item in the list it is returned
#                 as a string.
#
# @Usage:         The following code calls utFileGetNextSection to read
#                 the first 2 sections of the file and prints it to stdout:
#
# @Example:       set file [ open "sample.txt" r ]
#                 set chComment "#"
#
#                 puts "Section 1 contents:"
#                 set lSection [utFileGetNextSection $file $chComment "1" ]
#                 foreach sLine $lSection { puts $sLine }
#
#                 puts "Section 2 contents:"
#                 set lSection [utFileGetNextSection $file $chComment "1" ]
#                 foreach sLine $lSection { puts $sLine }
#
# @procdoc
#************************************************************************

proc utFileGetNextSection { file chComment fIgnoreBlank } {

    set lSection {}
    set fSectionTextFound 0

    while { [ gets $file sLine ] >= 0 } {

        if { [ string index $sLine 0 ] == $chComment } {

            if { $fSectionTextFound == 1 } {

                break
            }

        } else {

            set fSectionTextFound 1

            set sLine [ string trim $sLine ]

            if { $fIgnoreBlank == "1" } {

                if { [ string length $sLine ] > 0 } {

                    lappend lSection $sLine

                }

            } else {

                lappend lSection $sLine
            }
        }
    }

    if { [ llength $lSection ] == 1 } {

        return [ lindex $lSection 0 ]

    } else {

        return $lSection
    }
}
# end utFileGetNextSection

#************************************************************************
# @procdoc        utFileGetTmpDir
#
# @Brief:         Gets the temporary directory
#
# @Description:   This function will return the temporary directory path.
#                 It will first check for a possible directory by
#                 searching for the TMP, TEMP, TMPDIR, and HOME environment
#                 variables.  If none of these are set it or none are
#                 defined as a valid directoty, it will check if "c:\temp"
#                 exists on windows machines or if "/tmp" exists on
#                 unix machines.  If neither of these default directories
#                 exist, "c:\" will be returned for the windows platform
#                 and "/" will be returned for the unix platform.
#
# @Parameters:    None
#
# @Returns:       The path of the temporary directory
#
# @Usage:         The following code creates a file in the temporary
#                 directory called "file.txt":
#
# @Example:       set fFile [ open "[ utFileGetTmpDir ]/file.txt" w ]
#
# @procdoc
#************************************************************************

proc utFileGetTmpDir {} {

    global env tcl_platform

    set sTmpDir ""

    #
    # Create a list of all possible temp dir variables in lower-case so
    # case insensitive searches can be made.
    #

    set lTmpDirVars [ list tmpdir tmp temp ]

    #
    # Search for a temporary directory setting in the environment.
    #

    set lEnvIndices [ array names env ]

    foreach sVar $lTmpDirVars {

        set iMatch [ lsearch [ string tolower $lEnvIndices ] $sVar ]

        if { $iMatch >= 0 } {

            set sVal $env([ lindex $lEnvIndices $iMatch ])

            if { [ file isdirectory $sVal ] == 1 } {

                set sTmpDir $sVal
                break
            }
        }
    }

    #
    # If the previous tests failed, then if this is a windows platform,
    # the temporary directory is set to either "c:\temp" or "c:\".  If
    # it is Unix machine the temporary directory is set to "/tmp" or "/".
    #

    if { $sTmpDir == "" } {

        if { [ string match "*win*" \
               [ string tolower $tcl_platform(os) ] ] == 1 } {

            if { [ file isdirectory "c:\\temp" ] == 1 } {

                set sTmpDir "c:\\temp"

            } else {

                set sTmpDir "c:\\"
            }

        } else {

            if { [ file isdirectory "/tmp" ] == 1 } {

                set sTmpDir "/tmp"

            } else {

                set sTmpDir "/"
            }
        }
    }

    return $sTmpDir
}
# end utFileGetTmpDir

#************************************************************************
# @procdoc        utFileGetUnique
#
# @Brief:         Return a unique filename in the specified directory
#
# @Description:   This function returns a unique file name in the
#                 given directory.
#
# @Parameters:    sDir        - Directory location of file.
#                 sPattern    - Pattern used to generate unique file
#                               name.  The pattern uses the same
#                               logic described in utStrSequence.
#                 sExtension  - Optional extension for file name.
#
# @Returns:       Name of the unique file
#
# @Usage:         The following code calls utFileGetUnique to create a
#                 unique file name following the pattern "000". If files
#                 named "000", "001", and "002" already exist in the
#                 given directory, the value returned from the function
#                 will be "003".
#
# @Example:       set pFile [ open "/tmp/[ utFileGetUnique "/tmp" "000" ]" w ]
#
# @procdoc
#************************************************************************

proc utFileGetUnique { sDir sPattern { sExtension "" } } {

    set sFile $sPattern

    while { ( [ file isdirectory $sDir/$sFile$sExtension ] == "1" ) || \
            ( [ file isfile $sDir/$sFile$sExtension ] == "1" ) } {

        set sFile [ utStrSequence $sFile ]
    }

    return $sFile$sExtension
}
# end utFileGetUnique

#************************************************************************
# @procdoc        utFileReadToList
#
# @Brief:         Read data from the given file into a list
#
# @Description:   This function reads data from the given file into a list.
#
# @Parameters:    sFileName       - The name of the file to read
#                 fRemoveComments - An optional boolean flag to specify
#                                   whether comments should be removed
#                                   from the data or not.  Default is false.
#                 cCommentChar    - An optional parameter to specify the
#                                   comment character.  Default is "#".
#                 chDelim         - An optional parameter to specify
#                                   a character to split each line of
#                                   the file.
#
# @Returns:       List of strings containing the data in the file
#
# @Usage:         The following code calls utFileReadToList and ignores
#                 all lines that begin with the comment character of "!":
#
# @Example:       utFileReadToList "data.txt" 1 "!"
#
# @procdoc
#************************************************************************

proc utFileReadToList { sFileName { fRemoveComments 0 } \
                        { cCommentChar # } { chDelim "" } } {

    # Read the entire file into a string
    set sFileContents [ utFileReadToString $sFileName 0 ]

    # Split the string into list
    set lFileContents [ split $sFileContents \n ]

    # Remove comments from the list, if requested
    if { $fRemoveComments } {

        # Set lLines to empty list just in case
        set lLine {}

        foreach sLine $lFileContents {

            set sLine [ utStrRemoveComments $sLine $cCommentChar ]

            if { [ string length $sLine ] > 0 } {

                if { $chDelim != "" } {

                    set sLine [ split $sLine $chDelim ]
                }

                lappend lLine $sLine
            }
        }

    } elseif { $chDelim != "" } {

        foreach sLine $lFileContents {

            set sLine [ split $sLine $chDelim ]
            lappend lLine $sLine
        }

    } else {

        set lLine $lFileContents
    }

    # Return the list
    return $lLine
}
# end utFileReadToList

#************************************************************************
# @procdoc        utFileReadToString
#
# @Brief:         Read data from the given file into a string
#
# @Description:   This function reads data from the given file into a string.
#
# @Parameters:    sFileName    - The name of the file to read
#                 fKeepNewline - Flag indicating whether to keep a final
#                                newline characher if it exists.  Set to
#                                1 to keep the newline, 0 to remove it.
#                                (Default: 1)
#
# @Returns:       String containing the data in the file
#
# @Usage:         The following code calls utFileReadToString to
#                 retrieve the contents of "data.txt":
#
# @Example:       utFileReadToString "data.txt"
#
# @procdoc
#************************************************************************

proc utFileReadToString { sFileName { fKeepNewline 1 } } {

    # Open the file for reading
    set fInFile [ open "$sFileName" r ]

    # Read entire file into string, keeping the final newline, if desired.
    if { $fKeepNewline } {
        set sInFileContents [ read $fInFile ]
    } else {
        set sInFileContents [ read -nonewline $fInFile ]
    }

    # Close the file
    close $fInFile

    # Return the string
    return $sInFileContents
}
# end utFileReadToString


#************************************************************************
# @procdoc        utFileWriteFromList
#
# @Brief:         Write data to a file from a list
#
# @Description:   This function writes data to a file from a list.
#
# @Parameters:    sFileName    - The name of the file to write to.
#                 lLine        - The list of strings to write to the file.
#                 fTranslation - (Optional) The end-of-line tranlsation
#                                mode as described in the Tcl help file.
#                                (default: auto)
#
# @Returns:       Nothing
#
# @Usage:         The following code writes some letters of the alphabet
#                 to their own line in the file, letters.txt:
#
# @Example:       utFileWriteFromList "letters.txt" {A B C D E}
#
# @procdoc
#************************************************************************

proc utFileWriteFromList { sFileName lLine { fTranslation auto } } {

  # Join the list elements together and call utFileWriteFromString
  utFileWriteFromString $sFileName [ join $lLine \n ] $fTranslation

}
# end utFileWriteFromList


#************************************************************************
# @procdoc        utFileWriteFromString
#
# @Brief:         Write data to a file from a string
#
# @Description:   This function writes data to a file from a string.
#
# @Parameters:    sFileName     - The name of the file to write to.
#                 sFileContents - The string to write to the file.
#                 fTranslation  - (Optional) The end-of-line tranlsation
#                                 mode as described in the Tcl help file.
#                                 (default: auto)
#
# @Returns:       Nothing
#
# @Usage:         The following code writes the history of business
#                 object "Part" "A" "1" to file "revs.txt":
#
# @Example:       set sHistory [ mxBusGetBasic "Part" "A" "1" "history" ]
#                 utFileWriteFromString "revs.txt" $sHistory
#
# @procdoc
#************************************************************************

proc utFileWriteFromString { sFileName sFileContents { fTranslation auto } } {

  # Open the file for writing
  set fOutFile [ open "$sFileName" w ]

  # Set the translation mode on the file
  fconfigure $fOutFile -translation $fTranslation

  # Write string into file
  puts -nonewline $fOutFile $sFileContents

  # Close the file
  close $fOutFile
}
# end utFileWriteFromString


# end of Module

