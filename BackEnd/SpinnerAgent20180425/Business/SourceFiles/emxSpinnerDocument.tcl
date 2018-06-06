tcl;


eval {

set sHost [info host]

if { $sHost == "mostermant43" } {
        source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
	set cmd "debugger_eval"
	set xxx [debugger_init]

} else {
	puts "Host not registered for TclDebugger"
	set cmd "eval"
}

}

$cmd {

#main

# General Settings.
# Set the dir for the root level, if set to "" system temp is used.
# Use the / in path settings, tcl will resolve to the platform.
   #KYB V6R2014x - Fixed folder path for OS other than windows
   set sOS [string tolower $tcl_platform(os)];
   if { [string tolower [string range $sOS 0 5]] == "window" } {
      set sRootDir "c:/temp";
   } else {
      set sRootDir "/tmp";
   }

    #set sRootDir "c:/temp"

# Set output dir is created under to the root dir.
    set sOutputDir "Business_Model"


# Set to 0 to create a new unique dir, 1 to overwrite the existing dir.
    set bOverWrite 0


# Output Status Messages, 0 Off, 1 On.
    set bStatus 1


# To suppress hidden information, 0 Off (not checked), 1 On.
    set bSuppressHidden 0


# To suppress adm information, 0 Off (not checked), 1 On.
    set bSuppressAdmReporting 0


# Settings for Schema Data, ie the html
# To generate schema, 0 Off, 1 On.
    set bDumpSchema 1

# Text to place on the title page.
    set sHeaderTitle "Schema Agent"

# Under Development.
# Generate SVG images, 0 Off, 1 On.
    set bSVG 0

# To generate Program code listing, 0 Off, 1 On.
    set bExtendedProgram 1

# To generate detailed Policy Tables, 0 Off, 1 On.
    set bExtendedPolicy 1


# Settings for the Spinner Data output
# To generate spreadsheet data, 0 Off, 1 On.
    set bDumpSpinner 1


# Under Development
# Settings for the Dumping MQL, 0 Off, 1 On.
    set bDumpMQL 0


# Developers Mode, allows data filters to be applied to limit ouptut data.
# When set to 1 will use file input to set data to report on.
# When set to 0 will use the database and report all schema.

	set aInclude(bMode) 0

    set aInclude(sDir) "c:/temp/developer/include"
    set aInclude(sMask) "*.xls"
    set aInclude(sDelimit) "\t"

# The exclude list will remove objects from the output after the list are built.


    set aExclude(bMode) 0

    set aExclude(sDir) "c:/temp/developer/exclude"
    set aExclude(sMask) "*.xls"
    set aExclude(sDelimit) "\t"

	# KYB Start - Add arguments to filter HTML files
   set sSchemaType [mql get env 1]
   set sSchemaName [mql get env 2]
   
   if {$sSchemaType == "" || $sSchemaName == ""} {
      puts "\nInvoke program in this manner: \
            \n  exec prog emxSpinnerDocument.tcl \"SchemaType 1,...,SchemaType N\" \"SchemaName\" \"param3\" \"param4\" \"param5\" \
            \n\n   Note: wildcards are allowed for SchemaName - use * (asterisk) for all schema. \
            \n    Valid Examples: \
            \n       exec prog emxSpinnerDocument.tcl * * (generate HTML all schema)\
            \n       exec prog emxSpinnerDocument.tcl type Part* (all types beginning with Part)"
			
	  exit 1
      return
   }   
   	# KYB End - Add arguments to filter HTML files

    ###########################################################################
    # Do Not edit below this line
    #

    # create OutPut Directory
    if { $sRootDir == "" } {
        if { $tcl_platform(platform) == "windows" } {
            set Temp_Directory $env(TEMP)
        } elseif { $tcl_platform(platform) == "unix" } {
            set Temp_Directory $env(TMPDIR)
        }
    } else {
        set Temp_Directory $sRootDir
    }

    if { $bOverWrite } {
        set Out_Directory [file nativename "$Temp_Directory/${sOutputDir}"]
        if { [ file isdirectory $Out_Directory ] == 1 } {
            if {$bStatus} {puts "Delete existing directories ..."}
            set lDirDelete [ list ]
            set lDel [ list Images Policy Programs Spinner ]
            foreach i $lDel {
                lappend lDirDelete [ file join $Out_Directory $i ]
            }
            set lFiles [ glob -nocomplain [ file join $Out_Directory *.html ] ]
            if { [llength $lFiles] != 0 } {
                set lDirDelete [concat $lDirDelete $lFiles ]
            }
            foreach sDirDelete $lDirDelete {
                if { [ catch { file delete -force $sDirDelete } sMsg] } {
                    puts stderr "Unable to delete $sDirDelete, Message:\n$sMsg"
                    exit 1
                } else {
                    if {$bStatus} {puts "Directory $sDirDelete Deleted."}
                }
            }
        }
    } else {
        #KYB Start Spinner V6R2014x Enhanced HTML Documentation
		#set Out_Directory [file nativename "$Temp_Directory/${sOutputDir}_[clock format [clock seconds] -format "%Y%m%d-%H%M%S"]"]
		set Out_Directory [file nativename "$Temp_Directory/${sOutputDir}_[clock format [clock seconds] -format "%Y%m%d-%H%M%S"]"]
		set Main_Directory $Out_Directory
		append Out_Directory "/Business"
		#KYB End Spinner V6R2014x Enhanced HTML Documentation
    }

    if { $bStatus } { puts "OutPut dir set to $Out_Directory" }

    eval [ mql print program emxSchema_Dumper.tcl select code dump ]

    mql verbose off
    
    # Initialize arrays
    array set Attribute_Types {}
    array set Attribute_Relationships {}
    array set Format_Policies {}
    array set Location_Store {}
    array set Location_Site {}
    array set Store_Policy {}
    array set Statistic {}
    array set aAdmin {}
    array set aDirs {}

    Generate $sSchemaType $sSchemaName
    
    if {$bDumpSchema} {
       puts "\nSchema HTML output located in directory: $Out_Directory"
    }
    if {$bDumpSpinner} {
       puts "\nNOTICE:  To generate Spinner data files, execute the following command:\nexec prog emxExtractSchema.tcl * *;\n"
    }
    
    exit 0

}

