################################################################################
#
#  Version V6R2014x
#  Modified by Medha TAMBE to enhance HTML doc generation
#  Version 10.8.0 (Build 8th Jan 09)
#  Modified by M.Osterman to add 10.8 features 
#  Spinner dumper routines
################################################################################



################################################################################
# Add_Value_Element_To_Array
#   Add value element to array
#
#   Parameters :
#       array_name
#       element
#       value
#   Return : none
#
proc Add_Value_Element_To_Array { array_tab element value } {
    upvar $array_tab $array_tab
    set array_name $array_tab

    if { [array exists $array_name] != 1 } {
        set ${array_name}($element) [list $value]
    } else {
        if { [lsearch -exact [array names $array_name] $element] != -1 } {
            lappend ${array_name}($element) $value
        } else {
            set ${array_name}($element) [list $value]
        }
    }
    return
}


################################################################################
# Replace_Space
#   Replace space characters by underscore
#
#   Parameters :
#       string
#   Return :
#       string
#
proc Replace_Space { string } {
    regsub -all -- " " $string "_" string
    return $string
}
#End Replace_Space


################################################################################
# pGenTrig
#   Processes Trigger data that contains { and } one char at a time.
#   Note: Make sure { and } match, even in comments!!!!
#
#   Parameters :
#       string
#   Return :
#       string
#

proc pGenTrig { sData } {

    set temp_data ""
    set sData [ string trim $sData ]
    set nLen [ string length $sData ]
    
    for {set x 8} {$x<$nLen} {incr x} {
    
        set i [string index $sData $x]
        
        if { $i == ":" } {
            append temp_data $sTag " "
            set sTag ""
        } elseif { $i == "(" } {
            set bInside TRUE
            append temp_data $sTag " "
            set sTag ""
        } elseif { $i == ")" } {
            set bInside FALSE
            append temp_data $sTag
            set sTag ""
        } elseif { $i == "," && $bInside =="FALSE" } {
            append temp_data "<BR>"
            set sTag ""
        } else {
            append sTag $i
        }
    }
    return $temp_data
}
# End pGenTrig


################################################################################
# pCheckHidden
#   Test to see if hidden should be enforced.
#
#   Parameters :
#       
#   Return :
#       list
#
proc pCheckHidden { lContent sType } {

    upvar aAdmin aAdmin
    
    set lOk [ list ]
    set lAllowed $aAdmin($sType)
    
    foreach sName $lContent {
        if { [lsearch $lAllowed $sName] != "-1" } {
        #add to list
            lappend lOk $sName
        }
    }
    return $lOk
}
#End pCheckHidden

################################################################################
# pSetSchemaStyle
#   Sets style of header for a schema HTML page.
#
#   Parameters :
#       
#   Return :
#       list
#
proc pSetSchemaStyle { objectName } {
    set HTML_Page_Content "
        <HTML>
        <HEAD>
        <TITLE>$objectName</TITLE>
		
        <style type=\"text/css\">		
		
        .body {
				font-family: \"Times New Roman\";
				font-size:12px;
				color:#666666;
				text-decoration:none;
			 }

        a:link {
			text-decoration: none;
			color: #FFAD85;
			}
		a:visited {
			text-decoration: none;
			color: #C28566;
			}
		a:hover {
			text-decoration: none;
			color: #2E0000;
			background-color:#FFFFFF;
			}
		a:active {
			text-decoration: none;
			color: #0000FF;
			}
        </style>
        </HEAD>
        <BODY>
    "
	
	return $HTML_Page_Content
}


################################################################################
# Generate_type
#   Generate HTML page for types
#
#   Parameters : none
#   Return : none
#
proc Generate_type {} {
    upvar  Attribute_Types Attribute_Types
    upvar aAdmin aAdmin
    global Out_Directory Out_Directory
    global Image_Directory Image_Directory
    global sDumpProperties
    global bDumpSchema
    global bSuppressHidden

    set lProp [list ]

    # Get definition instances
    set Object "type"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        # Get type icon
        set Icon_Filename "[Replace_Space $instance].gif"
        catch { mql icon type "$instance" file "$Icon_Filename" dir $Image_Directory }
        if { [file exists $Image_Directory/$Icon_Filename] == 0 } {
            set Icon_Filename matrix_type.gif
        }

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><IMG ALIGN=ABSBOTTOM SRC=Images/$Icon_Filename BORDER=0 HSPACE=15><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 VALIGN=BOTTOM><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {

            # Case 'inherited method'
            if { [string match "*inherited method*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 2 3]]
                set item_content [split [lrange $item 4 end] ,]
                set temp_item ""
                foreach program $item_content {
                    append temp_item "<A HREF=\"program.html#[Replace_Space $program]\">$program</A> "
                }
                set item_content $temp_item

            # Case 'inherited attribute'
            } elseif { [string match "*inherited attribute*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 2 3]]
                set item_content [split [lrange $item 4 end] ,]
                set temp_item ""
                if {$bSuppressHidden} {set item_content [ pCheckHidden $item_content attribute ]}
                foreach attribute $item_content {
                    append temp_item "<A HREF=\"attribute.html#[Replace_Space $attribute]\">$attribute</A>  "

                    # Update Attribute_Types array
                    Add_Value_Element_To_Array Attribute_Types $attribute $instance
                }
                set item_content $temp_item

            # Case 'inherited form'
            } elseif { [string match "*inherited form*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 2 3]]
                set item_content [split [lrange $item 4 end] ,]
                set temp_item ""
                foreach form $item_content {
                    append temp_item "<A HREF=\"form.html#[Replace_Space $form]\">$form</A>  "
                }
                set item_content $temp_item

            # Case 'inherited trigger'
            } elseif { [string match "*inherited trigger*" $item] == 1 } {
                set sTrigger $item
                set item [split $item]
                set item_name [join [lrange $item 2 3]]
                set item_content [split [lrange $item 4 end] ,]
                if { [ string match "*\{*\}*" $sTrigger ] == 0 } {
                    set item_content [Generate_TriggerLinks $item_content $Object $instance]
                } else {
                    set item_content [pGenTrig $sTrigger]
                }

            } else {
                set sTrigger $item
                set item [split $item]
                set item_name [lindex $item 2]

                # Case 'attribute'
                if { $item_name == "attribute" } {
                    set item_content [split [lrange $item 3 end] ,]
                    set temp_item ""
                    if {$bSuppressHidden} {set item_content [pCheckHidden $item_content attribute]}
                    foreach attribute $item_content {
                        append temp_item "<A HREF=\"attribute.html#[Replace_Space $attribute]\">$attribute</A>  "

                        # Update Attribute_Types arrays
                        Add_Value_Element_To_Array Attribute_Types $attribute $instance
                    }
                    set item_content $temp_item

                # Case 'method'
                } elseif { $item_name == "method" } {
                    set item_content [split [lrange $item 3 end] ,]
                    set temp_item ""
                    if {$bSuppressHidden} {set item_content [pCheckHidden $item_content program]}
                    foreach program $item_content {
                        append temp_item "<A HREF=\"program.html#[Replace_Space $program]\">$program</A>  "
                    }
                    set item_content $temp_item

                # Case 'form'
                } elseif { $item_name == "form" } {
                    set item_content [split [lrange $item 3 end] ,]
                    set temp_item ""
                    foreach form $item_content {
                        append temp_item "<A HREF=\"form.html#[Replace_Space $form]\">$form</A>  "
                    }
                    set item_content $temp_item

                # Case 'policy'
                } elseif { $item_name == "policy" } {
                    set item_content [split [lrange $item 3 end] ,]
                    set temp_item ""
                    foreach policy $item_content {
                        append temp_item "<A HREF=\"policy.html#[Replace_Space $policy]\">$policy</A>  "
                    }
                    set item_content $temp_item

                # Case 'trigger'
                } elseif { $item_name == "trigger" } {
                    set item_content [split [lrange $item 3 end] ,]
                    if { [ string match "*\{*\}*" $sTrigger ] == 0 } {
                        set item_content [Generate_TriggerLinks $item_content $Object $instance]
                    } else {
                        set item_content [pGenTrig $sTrigger]
                    }

                # Case 'derivative'
                } elseif { $item_name == "derivative" } {
                    set item_content [split [mql print type $instance select derivative dump |] |]
                    set temp_item ""
                    foreach type $item_content {
                        append temp_item "<A HREF=\"type.html#[Replace_Space $type]\">$type</A>  "
                    }
                    set item_content $temp_item

                # Case 'derived'
                } elseif { $item_name == "derived" } {
                    set item_content [join [lrange $item 3 end]]
                    set item_content "<A HREF=\"type.html#[Replace_Space $item_content]\">$item_content</A>"

                # Case 'property'
                # Extract property name and property value
                } elseif { $item_name == "property" } {
                    set property [lrange $item 3 end]
                    set value_index [lsearch -exact $property "value"]
                    if { $value_index != -1 } {
                        set item_name [join [lrange $property 0 [expr $value_index -1]]]
                        set item_content [join [lrange $property [expr $value_index +1] end]]
                    } else {
                        set item_content [join [lrange $item 3 end]]
                    }
                    lappend lProp "$item_name \t $item_content"

                # Default case
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
            }

            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=150>$item_name</TD>
                  <TD ALIGN=LEFT>$item_content</TD>
                </TR>"
        }

        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]

        set From_Rel [split [mql print type $instance select fromrel dump |] |]
        set temp ""
        foreach relation $From_Rel {
            append temp "<A HREF=\"relationship.html#[Replace_Space $relation]\">$relation</A>  "
        }
        set From_Rel $temp
        append Page_Content "<TR>
                          <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">from relation</TD>
                         <TD ALIGN=LEFT WIDTH=\"80%\">$From_Rel</TD>
                        </TR>"
        set To_Rel [split [mql print type $instance select torel dump |] |]
        set temp ""
        foreach relation $To_Rel {
            append temp "<A HREF=\"relationship.html#[Replace_Space $relation]\">$relation</A>  "
        }
        set To_Rel $temp
        append Page_Content "<TR>
              <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">to relation</TD>
              <TD ALIGN=LEFT WIDTH=\"80%\">$To_Rel</TD>
              </TR>"

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "


        if { $bDumpSchema } { 
           if {$Object != "index"} {
              pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content
           } else {
              pfile_write [ file join $Out_Directory index_.html ] $Page_Content
           }
        }   
}

################################################################################
# Generate_attribute
#   Generate HTML page for attributes
#
#   Parameters : none
#   Return : none
#
proc Generate_attribute {} {
    upvar Attribute_Types Attribute_Types
    upvar Attribute_Relationships Attribute_Relationships
    upvar aAdmin aAdmin
    
    global Out_Directory

    global sDumpProperties
    global bDumpSchema

    # Get definition instances
    set Object "attribute"
    set Instances $aAdmin($Object)
    
    set lProp [ list ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {
            set sTrigger $item
            set item [split $item]
            set item_name [lindex $item 2]

            # Case 'property'
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }

            lappend lProp "$item_name \t $item_content"

            # Case 'trigger'
            } elseif { $item_name == "trigger" } {
                set item_content [split [lrange $item 3 end] ,]
                if { [ string match "*\{*\}*" $sTrigger ] == 0 } {
                    set item_content [Generate_TriggerLinks $item_content $Object $instance]
                } else {
                    set item_content [pGenTrig $sTrigger]
                }

            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]

        # Types using current attribute
        set Parent_Types ""
        if { [lsearch -exact [array names Attribute_Types] $instance] != -1 } {
            foreach type $Attribute_Types($instance) {
                append Parent_Types "<A HREF=\"type.html#[Replace_Space $type]\">$type</A>  "
            }
            append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Used in type(s)</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$Parent_Types</TD>
                    </TR>"
        }

        # Relationships using current attribute
        set Parent_Relationship ""
        if { [lsearch -exact [array names Attribute_Relationships] $instance] != -1 } {
            foreach relation $Attribute_Relationships($instance) {
                append Parent_Relationship "<A HREF=\"relationship.html#[Replace_Space $relation]\">$relation</A>  "
            }
            append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Used in relationship(s)</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$Parent_Relationship</TD>
                    </TR>"
        }

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}



################################################################################
# Generate_relationship
#   Generate HTML page for relationship
#
#   Parameters : none
#   Return : none
#
proc Generate_relationship {} {
    upvar Attribute_Types Attribute_Types
    upvar Attribute_Relationships Attribute_Relationships
    upvar aAdmin aAdmin

    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    global bSuppressHidden

    set lProp [list ]

    # Get definition instances
    set Object "relationship"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {
            set sTrigger $item
            set item [split $item]
            set item_name [lindex $item 2]
            set skip_line FALSE

            # Case 'trigger'
            if { $item_name == "trigger" } {
                set item_content [split [lrange $item 3 end] ,]
                if { [ string match "*\{*\}*" $sTrigger ] == 0 } {
                    set item_content [Generate_TriggerLinks $item_content $Object $instance]
                } else {
                    set item_content [pGenTrig $sTrigger]
                }

            # Case 'attribute'
            } elseif { $item_name == "attribute" } {
                set item_content [split [lrange $item 3 end] ,]
                set temp_item ""
                if {$bSuppressHidden} {set item_content [pCheckHidden $item_content attribute]}
                foreach attribute $item_content {
                    append temp_item "<A HREF=\"attribute.html#[Replace_Space $attribute]\">$attribute</A>"

                    # Update Attribute_Relationships array
                    Add_Value_Element_To_Array Attribute_Relationships $attribute $instance
                }
                set item_content $temp_item

            # Case 'from' and 'to'
            } elseif { ($item_name == "from") || ($item_name == "to") } {
                set item_content [split [mql print relationship $instance select ${item_name}type dump |] |]
                set temp_item ""
                foreach type $item_content {
                    append temp_item "<A HREF=\"type.html#[Replace_Space $type]\">$type</A> "
                }
                set item_content $temp_item

            # Case 'type'
            } elseif { [string match "*type*" [join [lrange $item 3 end]]] == 1 } {
                set skip_line TRUE

            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
                    
            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }

            # Skip 'type' line
            if { $skip_line == "FALSE" } {
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"
            }
            if { [ llength $lProp ] > 0 } {
                set lProp [ join $lProp \n ]
            }
            set lProp [list ]
        }

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_policy
#   Generate HTML page for policy
#
#   Parameters : none
#   Return : none
#
proc Generate_policy {} {
    upvar Format_Policies Format_Policies
    upvar Store_Policy Store_Policy
    upvar aAdmin aAdmin

    global bExtendedPolicy
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    global bSVG
    
    set lProp [list ]

    # Get definition instances
    set Object "policy"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        set sNoSpace [Replace_Space $instance]

        append Page_Content "
            <A NAME=\"$sNoSpace\"
            <h1><B>$Object $instance</B></h1>
            </A>"


        if {$bSVG} {
            append Page_Content "
                <center>
                <object type=\"image/svg-xml\" width=\"700\" height=\"200\" data=\"Images/${sNoSpace}.svg\">
                Should not happen
                </object>
                </center>
            "
        #Get the lifecycle state names.
        set lPolicyStateName [split [mql print policy "$instance" select state dump |] |]
        set sText ""
        set x 10
        set y 10
        foreach sStateName $lPolicyStateName {

            append sText "<use x=\"$x\" y=\"$y\" xlink:href=\"#rect\"/>
                <text x=\"[expr $x + 5]\" y=\"[expr $y + 15]\"
                font-size=\"10\"
                font-family=\"Arial\"
                fill=\"black\"
                text-anchor=\"start\"
                dominant-baseline=\"mathematical\">$sStateName</text>"
                incr x 150
        }

        # Create lifecycle
            set sDataSVG "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>
<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20000303 Stylable//EN\"
\"http://www.w3.org/TR/2000/03/WD-SVG-20000303/DTD/svg-20000303-stylable.dtd\">
<svg xml:space=\"preserve\" width=\"5.0in\" height=\"2.5in\">
<defs>
    <rect id=\"rect\" width=\"90\" height=\"30\" fill=\"none\" stroke=\"blue\" stroke-width=\"2\"
/>
</defs>
$sText
</svg>
            "
            pfile_write [file join $Out_Directory Images ${sNoSpace}.svg] $sDataSVG
        }

        append Page_Content "
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
        "

        if { $bExtendedPolicy == "1" } {
            append Page_Content "
                <TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">State Access Info</TD>
                <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><A HREF=\"Policy/[Replace_Space $instance].html\">$instance</A></TD>
                </TR>"
        }

        set sCurrentState ""
        foreach item $Content {

            set sTrigger $item
            set item [split $item]
            set item_name [lindex $item 2]
            set sub_item_name [lindex $item 4]

            # Case 'type'
            if { $item_name == "type" } {
                set item_content [split [lrange $item 3 end] ,]
                set temp_item ""
                foreach type $item_content {
                    append temp_item "<A HREF=\"type.html#[Replace_Space $type]\">$type</A>  "
                }
                set item_content $temp_item

            # Case 'store'
            } elseif { $item_name == "store" } {
                set item_content [split [lrange $item 3 end] ,]
                Add_Value_Element_To_Array Store_Policy $item_content $instance
                set temp_item ""
                foreach store $item_content {
                    append temp_item "<A HREF=\"store.html#[Replace_Space $store]\">$store</A>  "
                }
                set item_content $temp_item

            # Case 'format'
            } elseif { $item_name == "format" } {
                set item_content [split [lrange $item 3 end] ,]
                set temp_item ""
                foreach format $item_content {
                    append temp_item "<A HREF=\"format.html#[Replace_Space $format]\">$format</A>  "

                    # Update Format_Policies
                    Add_Value_Element_To_Array Format_Policies $format $instance
                }
                set item_content $temp_item

            # Case 'defaultformat'
            } elseif { $item_name == "defaultformat" } {
                set item_content [join [lrange $item 3 end]]
                set item_content "<A HREF=\"format.html#[Replace_Space $item_content]\">$item_content</A>"

            # Case 'trigger'
            } elseif { $sub_item_name == "trigger" } {
                set item_content [split [lrange $item 5 end] ,]
                if { [ string match "*\{*\}*" $sTrigger ] == 0 } {
                    set item_content [Generate_TriggerLinks $item_content $Object $instance]
                } else {
                    set item_content [pGenTrig $sTrigger]
                }

            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
                    
            } elseif { $item_name == "state" } {
#                set sCurrentState $item_content
                set item_content [join [lrange $item 3 end]]
            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}



################################################################################
# Generate_program
#   Generate HTML page for program
#
#   Parameters : none
#   Return : none
#
proc Generate_program {} {
    upvar Out_Directory Out_Directory
    upvar aAdmin aAdmin
    global bExtendedProgram
    global sDumpProperties
    global bDumpSchema

    set lProp [list ]
    # Get definition instances
    set Object "program"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        set Selectables {description hidden ismqlprogram doesneedcontext code iswizardprogram doesuseinterface execute isamethod isafunction type downloadable property}

        foreach item_name $Selectables {

            # Case 'code'
            if { $item_name == "code" && $bExtendedProgram == "1" } {
                if {[catch {set item_content [mql print program $instance select $item_name dump]} sMsg] != 0} {continue}
                regsub -all -- \" $item_content \\\" item_content

                # Create a file containing the code
                regsub -all -- "/" $instance "_" program_filename
                regsub -all -- " " $program_filename "_" program_filename
                regsub -all -- ":" $program_filename "_" program_filename
                regsub -all -- "\134\174" $program_filename "_" program_filename
                regsub -all -- ">" $program_filename "_" program_filename
                regsub -all -- "<" $program_filename "_" program_filename
                set program_filename Programs/${program_filename}.txt
                set program_file [open $Out_Directory/$program_filename w+]
                puts $program_file $item_content
                close $program_file

                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\"><A HREF=\"${program_filename}\">See code</A></TD>
                    </TR>"
            # Case 'type'
            } elseif { $item_name == "type" } {
                set item_content [split [mql print program $instance select $item_name dump |] |]
                set temp_item ""
                foreach type $item_content {
                    append temp_item "<A HREF=\"type.html#[Replace_Space $type]\">$type</A>  "
                }
                set item_content $temp_item
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"


            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set propertydata [split [mql print program $instance select property dump |] | ]
foreach property $propertydata {
#                set property [lrange $item 3 end ]

				set sPrint $property
				if {[string first " value " $sPrint] > -1} {
                   regsub " value " $sPrint "|" slsPrint
                   set lslsPrint [split $slsPrint |]
                   set item_content [lindex $lslsPrint 1]
                   set sPrint [lindex $lslsPrint 0]
    			 
                }
                set item_name [string trim $sPrint]
				
                lappend lProp "$item_name \t $item_content"
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"
}


            # Default case
            } else {
                set item_content [mql print program $instance select $item_name dump]
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"
            }
            
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
            
        }

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_group
#   Generate HTML page for groups
#
#   Parameters : none
#   Return : none
#
proc Generate_group {} {

    global bExtendedPolicy
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin
    set lProp [list ]

    # Get definition instances
    set Object "group"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        if { $bExtendedPolicy == "1" } {
            append Page_Content "
                <TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Group Access Info</TD>
                <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><A HREF=\"Policy/[Replace_Space $instance].html\">$instance</A></TD>
                </TR>"
        }

        set Person_List FALSE
        foreach item $Content {
            set item [split $item]
            set item_name [lindex $item 2]

            # Case 'child' or 'parent'
            if { ($item_name == "child") || ($item_name == "parent") } {
                set item_content [join [lrange $item 3 end]]
                set item_content "<A HREF=\"group.html#[Replace_Space $item_content]\">$item_content</A>"

            # Case 'assign' or 'people'
            # Do it one time
            } elseif { ($item_name == "assign") || ($item_name == "people") } {
                if { $Person_List == "FALSE" } {
                    set persons [split [mql print group $instance select person dump |] |]
                    set item_content ""
                    foreach person $persons {
                        append item_content "<A HREF=\"person.html#[Replace_Space $person]\">$person</A> "
                    }
                    set Person_List TRUE
                } else {
                    continue
                }

            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_role
#   Generate HTML page for role
#
#   Parameters : none
#   Return : none
#
proc Generate_role {} {

    upvar aAdmin aAdmin
    global bExtendedPolicy
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    
    set lProp [list ]

    # Get definition instances
    set Object "role"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        if { $bExtendedPolicy == "1" } {
            append Page_Content "
                <TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Role Access Info</TD>
                <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><A HREF=\"Policy/[Replace_Space $instance].html\">$instance</A></TD>
                </TR>"
        }

        set Person_List FALSE
        foreach item $Content {		
			set childParentItem $item
		
            set item [split $item]
            set item_name [lindex $item 2]
			
            # Case 'child' or 'parent'
            if { ($item_name == "child") || ($item_name == "parent") } {			
				if {$item_name == "child"} {
					set strComp [string first "  child" $childParentItem]
					if { $strComp >= 0 } {
						set nLen [ string length $childParentItem ]
						set sChildParent [ string range $childParentItem 8 $nLen ] 

					}
				} elseif {$item_name == "parent"} {
					set strComp [string first "  parent" $childParentItem]
					if { $strComp >= 0 } {
						set nLen [ string length $childParentItem ]
						set sChildParent [ string range $childParentItem 9 $nLen ] 
					}
				}
				set sChildParent [ split $sChildParent , ]
				set sChildParent [ lsort -dictionary $sChildParent ]
				set item_content ""
				foreach sRoleChildParent $sChildParent {
					append item_content "<A HREF=\"role.html#[Replace_Space $sRoleChildParent]\">$sRoleChildParent</A> "
				}
            # Case 'assign' or 'people'
            # Do it one time
            } elseif { ($item_name == "assign") || ($item_name == "people") } {
                if { $Person_List == "FALSE" } {
                    set persons [split [mql print role $instance select person dump |] |]
                    set item_content ""
					set persons [lsort -dictionary $persons]
                    foreach person $persons {
                        append item_content "<A HREF=\"person.html#[Replace_Space $person]\">$person</A> "
                    }
                    set Person_List TRUE
                } else {
                    continue
                }

            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "
        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_person
#   Generate HTML page for person
#
#   Parameters : none
#   Return : none
#
proc Generate_person {} {

    upvar aAdmin aAdmin
    upvar aDirs aDirs
    
    global bExtendedPolicy
    global lExtendedPersonData

    global Out_Directory
    global sDumpProperties
    global bDumpSchema

    set sDelimit "\t"
    set lPerson [list ]
    set lProp [list ]
    set lPersonData [ list name fullname comment address phone fax email vault \
        site type assign_role assign_group ]
    lappend lPerson [join $lPersonData $sDelimit]

    # Get definition instances
    set Object "person"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        set aData(name) $instance
        
        if {[catch {set Content [mql print $Object $instance]} sMsg] != 0} {continue}

        regsub -all -- {\{} $Content { LEFTBRACE } Content
        regsub -all -- {\}} $Content { RIGHTBRACE } Content

        set Content [lrange [split $Content \n] 1 end]
        set lAssign_Role [list ]
        set lAssign_Group [list ]

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object<B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        if { $bExtendedPolicy == "1"  && [lsearch $lExtendedPersonData $instance] != "-1" } {
            append Page_Content "
                <TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Person Access Info</TD>
                <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><A HREF=\"Policy/[Replace_Space $instance].html\">$instance</A></TD>
                </TR>"
        }

        foreach item $Content {
            set item [string trimleft $item]
            set lItem [split $item]
            set item_name [lindex $lItem 0]
#            Change to allow for special char strings
            set nFirstWS [string first " " $item]
            set item_content [string range $item [expr $nFirstWS + 1] end]
            set item_content_html $item_content
            set aData($item_name) $item_content
            # Case assign
            if { $item_name == "assign" } {
                set user [lrange $item 2 end]
                set group_role [lindex $lItem 1]
                set item_content_html  "<A HREF=\"${group_role}.html#[Replace_Space $user]\">$user</A>"
                if {$group_role == "group"} {
                    lappend lAssign_Group $user
                } elseif {$group_role == "role"} {
                    lappend lAssign_Role $user
                }
            # Case lattice
            } elseif { $item_name == "lattice" } {
                set vault [lrange $item 1 end]
                set aData(vault) $vault
                set item_content_html  "<A HREF=\"vault.html#[Replace_Space $vault]\">$vault</A>"
            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                    set item_content_html $item_content
                } else {
                    set item_content [join [lrange $lItem 1 end]]
                    set item_content_html $item_content
                }
                lappend lProp "$item_name \t $item_content"
            }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content_html</TD>
                </TR>"
        }
        append Page_Content "\n</TABLE><BR><BR>"
        if {[llength $lAssign_Role] == 0} {
            set aData(assign_role) ""
        } else {
            set lAssign_Role [lsort -dictionary $lAssign_Role]
            set sAssign_Role [join $lAssign_Role |]
            regsub -all -- {\|} $sAssign_Role { | } sAssign_Role
            set aData(assign_role) $sAssign_Role
        }
        if {[llength $lAssign_Group] == 0} {
            set aData(assign_group) ""
        } else {
            set lAssign_Group [lsort -dictionary $lAssign_Group]
            set sAssign_Group [join $lAssign_Group |]
            regsub -all -- {\|} $sAssign_Group { | } sAssign_Group
            set aData(assign_group) $sAssign_Group
        }

        set lDataEach [list ]
        foreach sPersonData $lPersonData {
            if { [ info exists aData($sPersonData) ] == 1 } {
                lappend lDataEach $aData($sPersonData)
            } else {
                lappend lDataEach ""
            }
        }
        lappend lPerson [join $lDataEach $sDelimit]
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    if {$bDumpSchema} {pfile_write [file join $Out_Directory ${Object}.html] \
        $Page_Content}
    return 0
}
# End Generate_person

################################################################################
# Generate_format
#   Generate HTML page for format
#
#   Parameters :
#   Return : none
#
proc Generate_format {} {
    upvar Format_Policies Format_Policies
    upvar aAdmin aAdmin
    
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    
    set lProp [list ]

    # Get definition instances
    set Object "format"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {
            set item [split $item]
            set item_name [lindex $item 2]

            # Case 'view' 'edit' 'print'
            if { ($item_name == "view") || ($item_name == "edit") || ($item_name == "print") } {
                set item_content [join [lrange $item 3 end]]
                set item_content "<A HREF=\"program.html#[Replace_Space $item_content]\">$item_content</A> "

            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        # Used by policies
        set item_content ""
        if { [lsearch -exact [array names Format_Policies] $instance] != -1 } {
            foreach policy $Format_Policies($instance) {
                append item_content "<A HREF=policy.html#[Replace_Space $policy]\">$policy</A> "
            }
        } else {
            set item_content ""
        }
        append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Used by policies</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_association
#   Generate HTML page for association
#
#   Parameters : none
#   Return : none
#
proc Generate_association {} {

    upvar aAdmin aAdmin

    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    global bExtendedPolicy
    
    set lProp [list ]

    # Get definition instances
    set Object "association"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        if { $bExtendedPolicy == "1" } {
            append Page_Content "
                <TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">Association Access Info</TD>
                <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><A HREF=\"Policy/[Replace_Space $instance].html\">$instance</A></TD>
                </TR>"
        }

        set Person_List FALSE
        foreach item $Content {
            set item [split $item]
            set item_name [lindex $item 2]

            if { $Person_List != "TRUE" } {

                # Case List of persons :
                # line content is just : 'List of persons who belongs to association'
                if { $item_name == "List" } {
                    set Person_List TRUE
                    set item_name "List of persons"
                    set item_content ""

                # Case 'property'
                # Extract property name and property value
                } elseif { $item_name == "property" } {
                    set property [lrange $item 3 end]
                    set value_index [lsearch -exact $property "value"]
                    if { $value_index != -1 } {
                        set item_name [join [lrange $property 0 [expr $value_index -1]]]
                        set item_content [join [lrange $property [expr $value_index +1] end]]
                    } else {
                        set item_content [join [lrange $item 3 end]]
                    }
                    lappend lProp "$item_name \t $item_content"
                    
                # Default case
                } else {
                    set item_content [join [lrange $item 3 end]]
                }

            # Line content is just a name of person
            } else {
                set item_content $item_name
                set item_name ""
            }

            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_form
#   Generate HTML page for forms
#
#   Parameters : none
#   Return : none
#
proc Generate_form {} {

    upvar aAdmin aAdmin
    upvar aDirs aDirs
    global Out_Directory
    global sDumpProperties
    global bDumpSchema

    set lProp [list ]

    # Get definition instances
    set Object "form"
    set Instances $aAdmin($Object)
	
    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
    
        #KYB Modified for Spinner V6R2014x
		#if {[mql print $Object $instance select web dump] == "TRUE"}
		if {[mql print $Object $instance select web dump] == "FALSE"} {
            continue
        }
    
        set Content [lrange [split [mql print $Object $instance] \n] 1 end]

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {
            set item [split $item]
            set item_name [lindex $item 2]

            if { $item_name == "type" } {
                set item_content [string trimleft [join [lrange $item 3 end]]]
                set item_content "<A HREF=\"type.html#[Replace_Space ${item_content}]\">${item_content}</A>"

            # Case 'property'
            # Extract property name and property value
            } elseif { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            # Default case
            } else {
                set item_content [join [lrange $item 3 end]]
            }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        
        #export to file, change any spec char.
        set sInstanceFileName [pRemSpecChar $instance]
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
    return 0
}


################################################################################
# Generate_vault
#   Generate HTML page for vault
#   
#   Parameters :
#       category
#   Return : none
#
proc Generate_vault {  } {

    global sDumpSchemaDirSystem
    global Out_Directory
    global sDumpProperties
    global bDumpSchema

    upvar aAdmin aAdmin
    
    set sDelimit "\t"

    set lProp [list ]

    # Get definition instances
    set Object vault
    set Instances $aAdmin($Object)

    set lVaultLocal [list name "Registry Name" description indexspace tablespace]
    set lDumpLocal [ list [ join $lVaultLocal $sDelimit ] ]
    set lVaultRemote [list name "Registry Name" description server]
    set lDumpRemote [ list [ join $lVaultRemote $sDelimit ] ]
    set lVaultForeign [list name "Registry Name" description interface file]
    set lDumpForeign [ list [ join $lVaultForeign $sDelimit ] ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page   
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}
        
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        set aVault(name) $instance
        set sOriginalName [mql print $Object $instance select property\[original name\].value dump]
        array set aVault "\"Registry Name\" \"$sOriginalName\""
        set aVault(server) ""
        set aVault(interface) ""
        set aVault(map) ""

        foreach item $Content {
            set item [ string trim $item ]
            # Case 'data tablespace'
            if { [string match "*data tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]
                set aVault(tablespace) $item_content
            # Case 'index tablespace'
            } elseif { [string match "*index tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]
                set aVault(indexspace) $item_content
           # Case 'total number of business objects'
            } elseif { [string match "*total number of business objects*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 4]]
                set item_content [join [lrange $item 5 end] ]
           # Case 'total number of relationships'
            } elseif { [string match "*total number of relationships*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 3]]
                set item_content [join [lrange $item 4 end] ]
           # Case 'total number of dynamic relationships'
            } elseif { [string match "*total number of dynamic relationships*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 4]]
                set item_content [join [lrange $item 5 end] ]
            } else {
            set item [split $item]
            set item_name [lindex $item 0]
            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
            } elseif { $item_name == "description" } {
                set item_content [join [lrange $item 1 end]]
                set aVault(description) $item_content
            } elseif {$item_name == "map"} {
                set item_content [mql print vault $aVault(name) select map dump]
                set aVault(file) [file join . System $aVault(name).map]
                pfile_write [file join $sDumpSchemaDirSystem $aVault(name).map] $item_content
            } elseif {$item_name == "interface"} {
                set item_content [join [lrange $item 1 end]]
                set aVault(interface) $item_content
            } elseif {$item_name == "server"} {
                set item_content [join [lrange $item 1 end]]
                set aVault(server) $item_content
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }
        }
            append Page_Content "<TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"

        if {$aVault(server) != ""} {
            set sVaultType Remote
        } elseif {$aVault(map) != "" || $aVault(interface) != ""} {
            set sVaultType Foreign
        } else {
            set sVaultType Local
        }

        set sRefVault lVault${sVaultType}
        set lRefVault [set $sRefVault]
        set sInstanceData [ list ]
        foreach sDumpData $lRefVault {
            if { [ info exists aVault($sDumpData) ] == 1 } {
                lappend sInstanceData \"$aVault($sDumpData)\"
            }
            lappend sInstanceData $sDelimit
        }
        set sInstanceData [ join $sInstanceData "" ]
        lappend lDump${sVaultType} $sInstanceData
        unset aVault
    }
    append Page_Content "
        </BODY>
        </HTML>
    "

    if {[llength $lDumpLocal] > 1} {
        set lDumpLocal [ join $lDumpLocal \n ]
    }
    
    if {[llength $lDumpRemote] > 1} {
        set lDumpRemote [ join $lDumpRemote \n ]
    }
    
    if {[llength $lDumpForeign] > 1} {
        set lDumpForeign [ join $lDumpForeign \n ]
    }
    
    if {$bDumpSchema} {pfile_write [file join $Out_Directory ${Object}.html] $Page_Content }

    return 0
}


################################################################################
# Generate_store
#   Generate HTML page for store
#   
#   Parameters :
#       category
#   Return : none
#
proc Generate_store {  } {

    upvar Location_Store Location_Store
    upvar Store_Policy Store_Policy
    upvar aAdmin aAdmin

    global sDumpSchemaDirSystem
    global sDumpProperties
    global Out_Directory
    global bDumpSchema
    
    global bStatus
    
    set lProp [list ]

    set sDelimit "\t"
    set sSeperator " | "
    set sStoreType ""
    set bProcessLocations FALSE
    
    # Get definition instances
    set Object store
    set Instances $aAdmin($Object)

    set lStoreCaptured [ list name "Registry Name" description type filename permission \
        protocol port host path user password location ]
    
    set lDumpCaptured [ list [ join $lStoreCaptured $sDelimit ] ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page   
    foreach instance $Instances {
        if {$bStatus} {puts -nonewline "."}
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=150><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5><B>$instance</B></TD>
            </TR>
            </A>"

        if { [ info exists Store_Policy($instance) ] == 1 } {
            set sLinks ""
            set lStores $Store_Policy($instance)
            foreach sStore $lStores {
                append sLinks " " "<A HREF=\"policy.html#[Replace_Space $sStore]\">$sStore</A>"
            }
            append Page_Content "<TR>
                <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\"><B>Used in Policy</B></TD>
                <TD ALIGN=LEFT WIDTH=\"80%\">$sLinks</TD>
                </TR>"
        }
        set aStore(name) $instance
        set sOriginalName [mql print $Object $instance select property\[original name\].value dump]
        array set aStore "\"Registry Name\" \"$sOriginalName\""
        foreach item $Content {
            set item [ string trim $item ]
            if { $bProcessLocations == "TRUE" } {
                if { [ mql list location $item ] == "" } {
                    set bProcessLocations FALSE
                } else {
                    Add_Value_Element_To_Array Location_Store $item $instance
                    set aStore(locations) [ lappend aStore(locations) [Replace_Space $item] ]
                    append item_content " " "<A HREF=\"location.html#[Replace_Space ${item}] \
                        \">${item}</A>"
                    continue
                }
            # Case 'data tablespace'
            } elseif { [string match "*data tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]

            # Case 'index tablespace'
            } elseif { [string match "*index tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]

           # Case 'total number of business objects'
            } elseif { [string match "*total number of business objects*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 4]]
                set item_content [join [lrange $item 5 end] ]

            } else {
            set item [split $item]
            set item_name [lindex $item 0]
            
            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
            } elseif { $item_name == "type" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(type) $item_content
                set sStoreType $item_content
            } elseif { $item_name == "description" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(description) $item_content
            } elseif { $item_name == "filename" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(filename) $item_content
            } elseif { $item_name == "permission" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(permission) $item_content
            } elseif { $item_name == "path" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(path) $item_content
            } elseif { $item_name == "protocol" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(protocol) $item_content
            } elseif { $item_name == "host" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(host) $item_content
            } elseif { $item_name == "user" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(user) $item_content
            } elseif { $item_name == "password" } {
                set item_content [join [lrange $item 1 end]]
                set aStore(password) $item_content
            } elseif { $item_name == "locations:" } {
                set aStore(locations) [ list ]
                set bProcessLocations TRUE
                set item_content ""
                continue
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }
           }
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"

        set sInstanceData [ list ]
        #process each store type
        if { $sStoreType == "captured" } {
            foreach sDumpData $lStoreCaptured {
                if { [ info exists aStore($sDumpData) ] == 1 } {
                    if { $sDumpData == "locations" } {
                        set aStore(locations) [ join $aStore(locations) $sSeperator ]
                    }
                    lappend sInstanceData \"$aStore($sDumpData)\"
                }
                lappend sInstanceData $sDelimit
            }
            set sInstanceData [ join $sInstanceData "" ]
            lappend lDumpCaptured $sInstanceData
        } else {
            puts "Store type $sStoreType, not yet supported"
        }
        unset aStore
    }
    append Page_Content "
        </BODY>
        </HTML>
    "

    set lDumpCaptured [ join $lDumpCaptured \n ]
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
    return 0
}

################################################################################
# Generate_location
#   Generate HTML page for location
#   
#   Parameters :
#       category
#   Return : none
#
proc Generate_location {  } {
    upvar Out_Directory Out_Directory
	upvar aAdmin aAdmin
    global bDumpSchema
	
	set lProp [list ]
    # Get definition instances
    set Object "location"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]
	
	# Body of HTML page
    foreach instance $Instances {
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set Selectables {description hidden permission protocol port host path user fcsurl property history}	
		
		 foreach item_name $Selectables {
			# Case 'permission'
			if { $item_name == "permission" } {
				if {[catch {set item_content [mql print location $instance select $item_name dump]} sMsg] != 0} {continue}
				append Page_Content "<TR>
					  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
					  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
					</TR>"
			# Case 'property'
			} elseif { $item_name == "property" } {
				set propertydata [split [mql print location $instance select property dump |] | ]
				foreach property $propertydata {
					set value_index [lsearch -exact $property "value"]
					if { $value_index != -1 } {
						set item_name [join [lrange $property 0 [expr $value_index -1]]]
						set item_content [join [lrange $property [expr $value_index +1] end]]
					} else {
						set item_content [join $property]
					}
					lappend lProp "$item_name \t $item_content"
					append Page_Content "<TR>
						  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
						  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
						</TR>"
				}
			# Default case
			} else {
				set item_content [mql print location $instance select $item_name dump]
				append Page_Content "<TR>
					  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
					  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
					</TR>"
			}
			
		if { [ llength $lProp ] > 0 } {
			set lProp [ join $lProp \n ]
		}
		set lProp [list ]
		 }
	}
	
    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }	
	
    return 0
}


################################################################################
# Generate_site
#   Generate HTML page for site
#   
#   Parameters :
#       category
#   Return : none
#
proc Generate_site {  } {
    global sDumpSchemaDirSystem
    global sDumpProperties
    global Out_Directory
    global bDumpSchema
    set lProp [list ]
    upvar Location_Site Location_Site
    upvar aAdmin aAdmin
    set sDelimit "\t"
    set sSeperator " | "

    # Get definition instances
    set Object site
    set Instances $aAdmin($Object)
    set lSite [ list name {Registry Name} description {member location} ]
    set lDump [ list [ join $lSite $sDelimit ] ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page   
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"			
			
        set aData(name) $instance
        set sOriginalName [mql print $Object $instance select property\[original name\].value dump]
        array set aData "\"Registry Name\" \"$sOriginalName\""
        foreach item $Content {
            set item [ string trim $item ]
            # Case 'member location'
            if { [string match "member location*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]
                if { [ info exists aData(location) ] == 0 } {
                    set aData(location) $item_content
                } else {
                    set aData(location) [ append aData(location) $sSeperator $item_content ]
                }
                Add_Value_Element_To_Array Location_Site $item_content $instance
                set item_content "<A HREF=\"location.html#[Replace_Space ${item_content}] \
                    \">${item_content}</A>"
            } else {
                set item [split $item]
                set item_name [lindex $item 0]
                # Property case
                # Extract property name and property value
                if { $item_name == "property" } {
                    set property [lrange $item 1 end]
                    set value_index [lsearch -exact $property "value"]
                    if { $value_index != -1 } {
                        set item_name [join [lrange $property 0 [expr $value_index -1]]]
                        set item_content [join [lrange $property [expr $value_index +1] end]]
                    } else {
                        set item_content [join [lrange $item 1 end]]
                    }
                    lappend lProp "$item_name \t $item_content"
                } elseif { $item_name == "description" } {
                    set item_content [join [lrange $item 1 end]]
                    set aData(description) $item_content
                # Default case
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
            }
				
			append Page_Content "<TR>
				<TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
				<TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
				</TR>"				
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"

        set sInstanceData [ list ]
        foreach sDumpData $lSite {
            if { [ info exists aData($sDumpData) ] == 1 } {
                lappend sInstanceData \"$aData($sDumpData)\"
            }
            lappend sInstanceData $sDelimit
        }
        set sInstanceData [ join $sInstanceData "" ]
        lappend lDump $sInstanceData

        unset aData
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    set lDump [ join $lDump \n ]
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
    return 0
}


################################################################################
# Generate_server
#   Generate HTML page for server
#   
#   Parameters :
#       category
#   Return : none
#
proc Generate_server {  } {
    upvar Out_Directory Out_Directory
	upvar aAdmin aAdmin
    global bDumpSchema
	
	set lProp [list ]
    # Get definition instances
    set Object "server"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]
	
	# Body of HTML page
    foreach instance $Instances {
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set Selectables {description hidden user connect timezone property history}	
		
		 foreach item_name $Selectables {
			# Case 'content'
			if { $item_name == "permission" } {
				if {[catch {set item_content [mql print server $instance select $item_name dump]} sMsg] != 0} {continue}
				append Page_Content "<TR>
					  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
					  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
					</TR>"
			# Case 'property'
			} elseif { $item_name == "property" } {
				set propertydata [split [mql print server $instance select property dump |] | ]
				foreach property $propertydata {
					set value_index [lsearch -exact $property "value"]
					if { $value_index != -1 } {
						set item_name [join [lrange $property 0 [expr $value_index -1]]]
						set item_content [join [lrange $property [expr $value_index +1] end]]
					} else {
						set item_content [join $property]
					}
					lappend lProp "$item_name \t $item_content"
					append Page_Content "<TR>
						  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
						  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
						</TR>"
				}
			# Default case
			} else {
				set item_content [mql print server $instance select $item_name dump]
				append Page_Content "<TR>
					  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
					  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
					</TR>"
			}
			
		if { [ llength $lProp ] > 0 } {
			set lProp [ join $lProp \n ]
		}
		set lProp [list ]
		 }
	}
	
    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }	
	
	return 0;
}


################################################################################
# Generate_Table
#   Generate HTML page for simple category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_table { } {
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin

    set lProp [list ]

    # Get definition instances
    set Object table
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance system] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {
            set item [split [string trim $item]]
            set item_name [lindex $item 0]

            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"

            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }

            regsub -all -- "<" $item_content {\&#60;} item_content
            regsub -all -- ">" $item_content {\&#62;} item_content
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

        if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}


################################################################################
# Generate_command
#   Generate HTML
#   Generate MQL
#   Parameters :
#       category
#   Return : none
#
proc Generate_command {  } {

    global sDumpSchemaDirSystem
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    global bDumpMQL

    upvar aAdmin aAdmin
    
    set sDelimit "\t"

    set lProp [list ]

    # Get definition instances
    set Object command
    set Instances $aAdmin($Object)

    set lLabels [ list description label href alt setting user ]
    
    set lDump [ list [ join $lLabels $sDelimit ] ]
    set lMql [ list ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]
	
	set sUserFlag "FALSE"

    # Body of HTML page   
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set aData(name) $instance
		
		set sUserFlag "FALSE"
		
        foreach item $Content {

            set item [ string trim $item ]

            # Case 'data tablespace'
            if { [string match "*data tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]
                set aData(tablespace) $item_content

            } else {

            set item [split $item]
            set item_name [lindex $item 0]
            
            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            } elseif { $item_name == "description" } {
                set item_content [join [lrange $item 1 end]]
                set aData(description) $item_content
            } elseif { $item_name == "label" } {
                set item_content [join [lrange $item 1 end]]
                set aData(label) $item_content
            } elseif { $item_name == "href" } {
                set item_content [join [lrange $item 1 end]]
                set aData(href) $item_content
            } elseif { $item_name == "alt" } {
                set item_content [join [lrange $item 1 end]]
                set aData(alt) $item_content
            } elseif { $item_name == "setting" } {
				set item_content [join [lrange $item 1 end]]
                set nValue [ lsearch $item value ]
                set sFirstValue [ lrange $item 1 [expr $nValue - 1] ]
                set sSeconValue [ lrange $item [expr $nValue + 1] end ]
                if {[info exists aData(setting)] == 0} {
                    set aData(setting) [list "$sFirstValue $sSeconValue"]
                } else {
                    set aDate(setting) [lappend aData(setting) [list $sFirstValue $sSeconValue] ]
                }
            } elseif { $item_name == "user" } {
				if { $sUserFlag == "FALSE" } {
					set item_content [mql print command $instance select user dump ,]
					if {[info exists aData(user)] == 0} {
						set aData(user) [list $item_content]
					} else {
						set aData(user) [lappend aData(user) $item_content]
					}
					set sUserFlag "TRUE"
				} else {
					continue
				}
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }
           }
            
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"

        set sCode "\n\n"
        append sCode "puts stdout \"Add $Object ...\""
        append sCode "\n\nset bRegister 1\n\n"
        append sCode "set sMql \"mql add $Object \\\"$instance\\\"\"\n"
        append sCode "pProcessMqlCmd \$bRegister \$sMql\n\n"
        append sCode "puts stdout \"Mod $Object ...\"\n\n"
        append sCode "set bRegister 0\n\n"
        append sCode "set sMql \"mql mod $Object \\\"$instance\\\" \\\n"
        foreach sDumpData $lLabels {
            if { [ info exists aData($sDumpData) ] == 1 } {
                switch $sDumpData {
                    user {
                        foreach sUser $aData($sDumpData) {
                            append sCode "    add user " \\\"$sUser\\\" " \\\n"
                        }
                    }
                    setting {
                        foreach sSet $aData($sDumpData) {
                            append sCode "    add setting " \\\"[lindex $sSet 0]\\\" " " "\\\"[lindex $sSet 1]\\\" \\\n"
                        }
                    }
                    default {append sCode "    " $sDumpData " " \\\"$aData($sDumpData)\\\" " \\\n"}
                }
            }
        }
        append sCode "  \"\n\n"
        append sCode "pProcessMqlCmd \$bRegister \$sMql\n\n"
        lappend lMql $sCode
        unset aData
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    set lDump [ join $lDump \n ]
    set lMql [join $lMql \n]
    if {$bDumpMQL} {pfile_write [file join $sDumpSchemaDirSystem ${Object}.tcl] $lMql}
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
    return 0
}

################################################################################
# Generate_channel
#   Generate HTML
#   Generate MQL
#   Parameters :
#       category
#   Return : none
#
proc Generate_channel {  } {

    global sDumpSchemaDirSystem
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    global bDumpMQL

    upvar aAdmin aAdmin
    
    set sDelimit "\t"

    set lProp [list ]

    # Get definition instances
    set Object channel
    set Instances $aAdmin($Object)

    set lLabels [ list description label href alt setting command ]
    
    set lDump [ list [ join $lLabels $sDelimit ] ]
    set lMql [ list ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page   
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set aData(name) $instance

        foreach item $Content {

            set item [ string trim $item ]

            # Case 'data tablespace'
            if { [string match "*data tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]
                set aData(tablespace) $item_content

            } else {

            set item [split $item]
            set item_name [lindex $item 0]
            
            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            } elseif { $item_name == "description" } {
                set item_content [join [lrange $item 1 end]]
                set aData(description) $item_content
            } elseif { $item_name == "label" } {
                set item_content [join [lrange $item 1 end]]
                set aData(label) $item_content
            } elseif { $item_name == "href" } {
                set item_content [join [lrange $item 1 end]]
                set aData(href) $item_content
            } elseif { $item_name == "alt" } {
                set item_content [join [lrange $item 1 end]]
                set aData(alt) $item_content
            } elseif { $item_name == "setting" } {
				set item_content [join [lrange $item 1 end]]
                set nValue [ lsearch $item value ]				
                set sFirstValue [ lrange $item 1 [expr $nValue - 1] ]
                set sSeconValue [ lrange $item [expr $nValue + 1] end ]				
                if {[info exists aData(setting)] == 0} {
                    set aData(setting) [list "$sFirstValue $sSeconValue"]
                } else {
                    set aDate(setting) [lappend aData(setting) [list $sFirstValue $sSeconValue] ]
                }
            } elseif { $item_name == "command" } {
                set temp_item [join [lrange $item 1 end]]
				set temp_item [split $temp_item ,]
				set item_content ""
				foreach sChannelCommand $temp_item {
					append item_content "<A HREF=\"command.html#[Replace_Space $sChannelCommand]\">$sChannelCommand</A> "
				}
                if {[info exists aData(command)] == 0} {
                    set aData(command) [list $item_content]
                } else {
                    set aData(command) [lappend aData(command) $item_content]
                }
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }
           }
            
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"

        set sCode "\n\n"
        append sCode "puts stdout \"Add $Object ...\""
        append sCode "\n\nset bRegister 1\n\n"
        append sCode "set sMql \"mql add $Object \\\"$instance\\\"\"\n"
        append sCode "pProcessMqlCmd \$bRegister \$sMql\n\n"
        append sCode "puts stdout \"Mod $Object ...\"\n\n"
        append sCode "set bRegister 0\n\n"
        append sCode "set sMql \"mql mod $Object \\\"$instance\\\" \\\n"
        foreach sDumpData $lLabels {
            if { [ info exists aData($sDumpData) ] == 1 } {
                switch $sDumpData {
                    command {
                        foreach sCommand $aData($sDumpData) {
                            append sCode "    add command " \\\"$sCommand\\\" " \\\n"
                        }
                    }
                    setting {
                        foreach sSet $aData($sDumpData) {
                            append sCode "    add setting " \\\"[lindex $sSet 0]\\\" " " "\\\"[lindex $sSet 1]\\\" \\\n"
                        }
                    }
                    default {append sCode "    " $sDumpData " " \\\"$aData($sDumpData)\\\" " \\\n"}
                }
            }
        }
        append sCode "  \"\n\n"
        append sCode "pProcessMqlCmd \$bRegister \$sMql\n\n"
        lappend lMql $sCode
        unset aData
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    set lDump [ join $lDump \n ]
    set lMql [join $lMql \n]
    if {$bDumpMQL} {pfile_write [file join $sDumpSchemaDirSystem ${Object}.tcl] $lMql}
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
    return 0
}

################################################################################
# Generate_portal
#   Generate HTML
#   Generate MQL
#   Parameters :
#       category
#   Return : none
#
proc Generate_portal {  } {

    global sDumpSchemaDirSystem
    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    global bDumpMQL

    upvar aAdmin aAdmin
    
    set sDelimit "\t"

    set lProp [list ]

    # Get definition instances
    set Object portal
    set Instances $aAdmin($Object)

    set lLabels [ list description label href alt setting channel ]
    
    set lDump [ list [ join $lLabels $sDelimit ] ]
    set lMql [ list ]

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]
	
	set bChannelFlag "FALSE"
	
    # Body of HTML page   
    foreach instance $Instances {
        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set aData(name) $instance
		
		set bChannelFlag "FALSE"

        foreach item $Content {

            set item [ string trim $item ]

            # Case 'data tablespace'
            if { [string match "*data tablespace*" $item] == 1 } {
                set item [split $item]
                set item_name [join [lrange $item 0 1]]
                set item_content [join [lrange $item 2 end] ]
                set aData(tablespace) $item_content

            } else {

            set item [split $item]
            set item_name [lindex $item 0]
            
            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
                
            } elseif { $item_name == "description" } {
                set item_content [join [lrange $item 1 end]]
                set aData(description) $item_content
            } elseif { $item_name == "label" } {
                set item_content [join [lrange $item 1 end]]
                set aData(label) $item_content
            } elseif { $item_name == "href" } {
                set item_content [join [lrange $item 1 end]]
                set aData(href) $item_content
            } elseif { $item_name == "alt" } {
                set item_content [join [lrange $item 1 end]]
                set aData(alt) $item_content
            } elseif { $item_name == "setting" } {
                set nValue [ lsearch $item value ]
                set sFirstValue [ lrange $item 1 [expr $nValue - 1] ]
                set sSeconValue [ lrange $item [expr $nValue + 1] end ]
                if {[info exists aData(setting)] == 0} {
                    set aData(setting) [list "$sFirstValue $sSeconValue"]
                } else {
                    set aDate(setting) [lappend aData(setting) [list $sFirstValue $sSeconValue] ]
                }
            } elseif { $item_name == "channel" } {
				if { $bChannelFlag == "FALSE" } {
						set item_content [split [mql print portal $instance select channel dump |] |]
						set temp_item ""
						foreach sPortalChannel $item_content {
							append temp_item "<A HREF=\"channel.html#[Replace_Space $sPortalChannel]\">$sPortalChannel</A>  "
						}
						set item_content $temp_item
						if {[info exists aData(channel)] == 0} {
							set aData(channel) [list $item_content]
						} else {
							set aData(channel) [lappend aData(channel) $item_content]
						}
						set bChannelFlag "TRUE"
					} else {
						continue
					}
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }
           }
            
            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"

        set sCode "\n\n"
        append sCode "puts stdout \"Add $Object ...\""
        append sCode "\n\nset bRegister 1\n\n"
        append sCode "set sMql \"mql add $Object \\\"$instance\\\"\"\n"
        append sCode "pProcessMqlCmd \$bRegister \$sMql\n\n"
        append sCode "puts stdout \"Mod $Object ...\"\n\n"
        append sCode "set bRegister 0\n\n"
        append sCode "set sMql \"mql mod $Object \\\"$instance\\\" \\\n"
        foreach sDumpData $lLabels {
            if { [ info exists aData($sDumpData) ] == 1 } {
                switch $sDumpData {
                    user {
                        foreach sUser $aData($sDumpData) {
                            append sCode "    add channel " \\\"$sChannel\\\" " \\\n"
                        }
                    }
                    setting {
                        foreach sSet $aData($sDumpData) {
                            append sCode "    add setting " \\\"[lindex $sSet 0]\\\" " " "\\\"[lindex $sSet 1]\\\" \\\n"
                        }
                    }
                    default {append sCode "    " $sDumpData " " \\\"$aData($sDumpData)\\\" " \\\n"}
                }
            }
        }
        append sCode "  \"\n\n"
        append sCode "pProcessMqlCmd \$bRegister \$sMql\n\n"
        lappend lMql $sCode
        unset aData
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    set lDump [ join $lDump \n ]
    set lMql [join $lMql \n]
    if {$bDumpMQL} {pfile_write [file join $sDumpSchemaDirSystem ${Object}.tcl] $lMql}
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
    return 0
}

################################################################################
# Generate_Menu
#   Generate HTML page for Menu category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_menu { } {
	global Out_Directory
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin

    set lProp [list ]

    # Get definition instances
    set Object menu
    set Instances $aAdmin($Object)
	
	# Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]
	
	set bCommandFlag "FALSE"
	set bMenuFlag "FALSE"
	
	# Body of HTML page
    foreach instance $Instances {
		if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}
		
		append Page_Content "
		<A NAME=\"[Replace_Space $instance]\">
        <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
		<TR>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
		</TR>
		</A>"
		
		set bCommandFlag "FALSE"
		set bMenuFlag "FALSE"

		foreach item $Content {
			set item [split [string trim $item]]
            set item_name [lindex $item 0]

			if { $item_name == "menu" } {
				if { $bMenuFlag == "FALSE" } {
					set temp_item [split [mql print menu $instance select menu dump |] |]
					set item_content ""
					foreach sMenu $temp_item {
						append item_content "<A HREF=\"menu.html#[Replace_Space $sMenu]\">$sMenu</A> "
					}
					set bMenuFlag "TRUE"
				} else {
					continue
				}
			} elseif { $item_name == "command" } {
				if { $bCommandFlag == "FALSE" } {
					set temp_item [split [mql print menu $instance select command dump |] |]
					set item_content ""
					foreach sCommand $temp_item {
						append item_content "<A HREF=\"command.html#[Replace_Space $sCommand]\">$sCommand</A> "
					}
					set bCommandFlag "TRUE"
				} else {
					continue
				}
			# Property case
            # Extract property name and property value
			} elseif { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"

            # Default case
            } else {
				set item_content [join [lrange $item 1 end]]
            }

			if { ($item_name == "command") || ($item_name == "menu")} {
			} else {
				regsub -all -- "<" $item_content {\&#60;} item_content
				regsub -all -- ">" $item_content {\&#62;} item_content
			}

            append Page_Content "<TR>
            <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
			<TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
			</TR>"
		}
		
		if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
	}
	
    append Page_Content "
        </BODY>
        </HTML>
    "
	
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }	
}

################################################################################
# Generate_Inquiry
#   Generate HTML page for Inquiry category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_inquiry { } {
	global Out_Directory
	global bExtendedProgram
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin

    set lProp [list ]

    # Get definition instances
    set Object inquiry
    set Instances $aAdmin($Object)
	
	# Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

	# Body of HTML page
    foreach instance $Instances {
		if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}
		
		append Page_Content "
		<A NAME=\"[Replace_Space $instance]\">
		<TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
		<TR>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
		</TR>
		</A>"
		
		foreach item $Content {
			set item [split [string trim $item]]
            set item_name [lindex $item 0]

            # Case 'code'
            if { $item_name == "code" && $bExtendedProgram == "1" } {
                if {[catch {set item_content [mql print inquiry $instance select $item_name dump]} sMsg] != 0} {continue}
                regsub -all -- \" $item_content \\\" item_content

                # Create a file containing the code
                regsub -all -- "/" $instance "_" program_filename
                regsub -all -- " " $program_filename "_" program_filename
                regsub -all -- ":" $program_filename "_" program_filename
                regsub -all -- "\134\174" $program_filename "_" program_filename
                regsub -all -- ">" $program_filename "_" program_filename
                regsub -all -- "<" $program_filename "_" program_filename
                set program_filename Programs/${program_filename}.inq
                set program_file [open $Out_Directory/$program_filename w+]
                puts $program_file $item_content
                close $program_file

                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\"><A HREF=\"${program_filename}\">See code</A></TD>
                    </TR>"			
			} elseif { $item_name == "property" } {
			# Property case
            # Extract property name and property value
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
				
				regsub -all -- "<" $item_content {\&#60;} item_content
				regsub -all -- ">" $item_content {\&#62;} item_content
				append Page_Content "<TR>
				<TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
				<TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
				</TR>"
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
				regsub -all -- "<" $item_content {\&#60;} item_content
				regsub -all -- ">" $item_content {\&#62;} item_content
				append Page_Content "<TR>
				<TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
				<TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
				</TR>"				
            }
		}
		
		if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
	}
	
    append Page_Content "
        </BODY>
        </HTML>
    "
	
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }	
}

################################################################################
# Generate_Interface
#   Generate HTML page for Interface category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_interface { } {
	global Out_Directory
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin

    set lProp [list ]

    # Get definition instances
    set Object interface
    set Instances $aAdmin($Object)
	
	# Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

	# Body of HTML page
    foreach instance $Instances {
		if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}
		
		append Page_Content "
		<A NAME=\"[Replace_Space $instance]\">
        <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
		<TR>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
		</TR>
		</A>"
		
		foreach item $Content {
			set item [split [string trim $item]]
            set item_name [lindex $item 0]

            if { $item_name == "type" } {
				set item_content [split [mql print interface $instance select type dump |] |]
				set temp_item ""
				foreach sTypeInterface $item_content {
					append temp_item "<A HREF=\"type.html#[Replace_Space $sTypeInterface]\">$sTypeInterface</A>  "
				}
				set item_content $temp_item						
			} elseif { $item_name == "relationship" } {
				set item_content [split [mql print interface $instance select relationship dump |] |]
				set temp_item ""
				foreach sRelInterface $item_content {
					append temp_item "<A HREF=\"relationship.html#[Replace_Space $sRelInterface]\">$sRelInterface</A>  "
				}
				set item_content $temp_item			
			} elseif { $item_name == "attribute" } {
				set item_content [split [mql print interface $instance select attribute dump |] |]
				set temp_item ""
				foreach sAttrInterface $item_content {
					append temp_item "<A HREF=\"attribute.html#[Replace_Space $sAttrInterface]\">$sAttrInterface</A>  "
				}
				set item_content $temp_item     
			# Property case
            # Extract property name and property value
		   } elseif { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"
				regsub -all -- "<" $item_content {\&#60;} item_content
				regsub -all -- ">" $item_content {\&#62;} item_content
            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]				
				regsub -all -- "<" $item_content {\&#60;} item_content
				regsub -all -- ">" $item_content {\&#62;} item_content
            }

            append Page_Content "<TR>
            <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
			<TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
			</TR>"
		}
		
		if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
	}
	
    append Page_Content "
        </BODY>
        </HTML>
    "
	
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }	
}

################################################################################
# Generate_Index
#   Generate HTML page for Index category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_index { } {
	global Out_Directory
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin

    set lProp [list ]

    # Get definition instances
    set Object index
    set Instances $aAdmin($Object)
	
	# Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

	# Body of HTML page
    foreach instance $Instances {
		if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}
		
		append Page_Content "
		<A NAME=\"[Replace_Space $instance]\">
        <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
		<TR>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
		<TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
		</TR>
		</A>"
		
		foreach item $Content {
			set item [split [string trim $item]]
            set item_name [lindex $item 0]

            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 1 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 1 end]]
                }
                lappend lProp "$item_name \t $item_content"

            # Default case
            } else {
                set item_content [join [lrange $item 1 end]]
            }

            regsub -all -- "<" $item_content {\&#60;} item_content
            regsub -all -- ">" $item_content {\&#62;} item_content
            append Page_Content "<TR>
            <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
			<TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
			</TR>"
		}
		
		if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        append Page_Content "\n</TABLE><BR><BR>"
	}
	
    append Page_Content "
        </BODY>
        </HTML>
    "
	
    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }	
}

################################################################################
# Generate_Pgae
#   Generate HTML page for Page category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_page {} {
    upvar Out_Directory Out_Directory
    upvar aAdmin aAdmin
    global bExtendedProgram
    global sDumpProperties
    global bDumpSchema

    set lProp [list ]
    # Get definition instances
    set Object "page"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set Selectables {description hidden content mime property}

        foreach item_name $Selectables {
            # Case 'content'
            if { $item_name == "content" && $bExtendedProgram == "1" } {
                if {[catch {set item_content [mql print page $instance select $item_name dump]} sMsg] != 0} {continue}
                regsub -all -- \" $item_content \\\" item_content

                # Create a file containing the content
                regsub -all -- "/" $instance "_" program_filename
                regsub -all -- " " $program_filename "_" program_filename
                regsub -all -- ":" $program_filename "_" program_filename
                regsub -all -- "\134\174" $program_filename "_" program_filename
                regsub -all -- ">" $program_filename "_" program_filename
                regsub -all -- "<" $program_filename "_" program_filename
                set program_filename Programs/${program_filename}.txt
                set program_file [open $Out_Directory/$program_filename w+]
                puts $program_file $item_content
                close $program_file

                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\"><A HREF=\"${program_filename}\">See content</A></TD>
                    </TR>"
		    # Case 'property'
            } elseif { $item_name == "property" } {
                set propertydata [split [mql print page $instance select property dump |] | ]
				foreach property $propertydata {
					set value_index [lsearch -exact $property "value"]
					if { $value_index != -1 } {
						set item_name [join [lrange $property 0 [expr $value_index -1]]]
						set item_content [join [lrange $property [expr $value_index +1] end]]
					} else {
						set item_content [join $property]
					}
					lappend lProp "$item_name \t $item_content"
					append Page_Content "<TR>
						  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
						  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
						</TR>"
				}
            # Default case
            } else {
                set item_content [mql print page $instance select $item_name dump]
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"
            }
            
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
            
        }

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_expression
#   Generate HTML
#   Generate MQL
#   Parameters :
#       category
#   Return : none
#
proc Generate_expression {  } {

    upvar Out_Directory Out_Directory
    upvar aAdmin aAdmin
    global bExtendedProgram
    global sDumpProperties
    global bDumpSchema

    set lProp [list ]
    # Get definition instances
    set Object "expression"
    set Instances $aAdmin($Object)

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]

    # Body of HTML page
    foreach instance $Instances {
        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
            <TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        set Selectables {description value hidden property history}
		
        foreach item_name $Selectables {
            # Case 'content'
            if { $item_name == "content" && $bExtendedProgram == "1" } {
                if {[catch {set item_content [mql print expression $instance select $item_name dump]} sMsg] != 0} {continue}
                regsub -all -- \" $item_content \\\" item_content

                # Create a file containing the content
                regsub -all -- "/" $instance "_" program_filename
                regsub -all -- " " $program_filename "_" program_filename
                regsub -all -- ":" $program_filename "_" program_filename
                regsub -all -- "\134\174" $program_filename "_" program_filename
                regsub -all -- ">" $program_filename "_" program_filename
                regsub -all -- "<" $program_filename "_" program_filename
                set program_filename Programs/${program_filename}.txt
                set program_file [open $Out_Directory/$program_filename w+]
                puts $program_file $item_content
                close $program_file

                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\"><A HREF=\"${program_filename}\">See content</A></TD>
                    </TR>"
		    # Case 'property'
            } elseif { $item_name == "property" } {
                set propertydata [split [mql print expression $instance select property dump |] | ]
				foreach property $propertydata {
					set value_index [lsearch -exact $property "value"]
					if { $value_index != -1 } {
						set item_name [join [lrange $property 0 [expr $value_index -1]]]
						set item_content [join [lrange $property [expr $value_index +1] end]]
					} else {
						set item_content [join $property]
					}
					lappend lProp "$item_name \t $item_content"
					append Page_Content "<TR>
						  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
						  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
						</TR>"
				}
            # Default case
            } else {
                set item_content [mql print expression $instance select $item_name dump]
                append Page_Content "<TR>
                      <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                      <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                    </TR>"
            }
            
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
            
        }

        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content }
}

################################################################################
# Generate_Simple
#   Generate HTML page for simple category of business definitions
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_Simple { category } {

    global Out_Directory
    global sDumpProperties
    global bDumpSchema
    upvar aAdmin aAdmin
    upvar aDirs aDirs

    set lProp [list ]

    # Get definition instances
    set Object $category

	set ObjectName [ string tolower $category ]

    set Instances $aAdmin($Object)
	
    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $Object ]
	
    # Body of HTML page
    foreach instance $Instances {

        if {[catch {set Content [lrange [split [mql print $Object $instance] \n] 1 end]} sMsg] != 0} {continue}

        append Page_Content "
            <A NAME=\"[Replace_Space $instance]\">
			<TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">
            <TR>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"20%\"><B>$Object</B></TD>
            <TD ALIGN=LEFT BGCOLOR=#F5F5F5 WIDTH=\"80%\"><B>$instance</B></TD>
            </TR>
            </A>"

        foreach item $Content {			
			set item [split $item]
			
			set item_adminObj_rule $item
			
			set ruleRelTypeHeader "Relationship Type:"
			
			set ruleRelationshipType [string first $ruleRelTypeHeader $item_adminObj_rule]
						
            set item_name [lindex $item 2]			
			
            # Property case
            # Extract property name and property value
            if { $item_name == "property" } {
                set property [lrange $item 3 end]
                set value_index [lsearch -exact $property "value"]
                if { $value_index != -1 } {
                    set item_name [join [lrange $property 0 [expr $value_index -1]]]
                    set item_content [join [lrange $property [expr $value_index +1] end]]
                } else {
                    set item_content [join [lrange $item 3 end]]
                }
                lappend lProp "$item_name \t $item_content"
			# Case Rule
			} elseif { $item_adminObj_rule == "List of Administration objects that reference this Rule :" } {
				set item_name $item_adminObj_rule
				set item_content ""
			# Case Rule
			} elseif { $ruleRelationshipType >= 0 } {
				set item_name $ruleRelTypeHeader
				set item_content [ split [join [lrange $item 4 end]] , ]
				set temp_content ""
				foreach sRelRule $item_content {
					append temp_content "<A HREF=\"relationship.html#[Replace_Space $sRelRule]\">$sRelRule</A>  "
				}				
				set item_content $temp_content
			} elseif { $item_name == "program:" } {
			    set item_content [ split [join [lrange $item 3 end]] , ]
				set temp_content ""
				foreach sProgRule $item_content {
					append temp_content "<A HREF=\"program.html#[Replace_Space $sProgRule]\">$sProgRule</A>  "
				}				
				set item_content $temp_content				
			} elseif { $item_name == "form:" } {
			    set item_content [ split [join [lrange $item 3 end]] , ]
				set temp_content ""
				foreach sFormRule $item_content {
					append temp_content "<A HREF=\"form.html#[Replace_Space $sFormRule]\">$sFormRule</A>  "
				}				
				set item_content $temp_content
			} elseif { $item_name == "attribute:" } {
			    set item_content [ split [join [lrange $item 3 end]] , ]
				set temp_content ""
				foreach sAttrRule $item_content {
					append temp_content "<A HREF=\"attribute.html#[Replace_Space $sAttrRule]\">$sAttrRule</A>  "
				}				
				set item_content $temp_content
				# Default case
			} else {			
                set item_content [join [lrange $item 3 end]]
            }

            append Page_Content "<TR>
                  <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=\"20%\">$item_name</TD>
                  <TD ALIGN=LEFT WIDTH=\"80%\">$item_content</TD>
                </TR>"
        }
        if { [ llength $lProp ] > 0 } {
            set lProp [ join $lProp \n ]
        }
        set lProp [list ]
        if { $Object == "command" || $Object == "menu" || $Object == "channel" || $Object == "portal" || $Object == "page" || $Object == "interface" || $Object == "expression" || $Object == "index" || $Object == "dimension"} {
            # Do nothing
        } else {
        }
        append Page_Content "\n</TABLE><BR><BR>"
    }

    append Page_Content "
        </BODY>
        </HTML>
    "

    if { $bDumpSchema } { 
        if {$Object != "index"} {
             pfile_write [ file join $Out_Directory ${Object}.html ] $Page_Content
        } else {
             pfile_write [ file join $Out_Directory index_.html ] $Page_Content
        }
    }
         
    return 0
}



################################################################################
# Generate_Summary_Menu
#   Generate HTML page for a menu page (left frame)
#
#   Parameters :
#       category
#   Return : none
#
proc Generate_Summary_Menu { category } {
    upvar Category_Order Category_Order
    upvar Out_Directory Out_Directory
    upvar Statistic Statistic
    upvar aAdmin aAdmin
    global bDumpSchema
    global glsTriggerManagerObjects
	global glsNumberGeneratorObjects
	global glsObjectGeneratorObjects

    set Summary_Menu_Page "
        <HTML>		
        <HEAD>
        <TITLE>$category</TITLE>
		
        <style type=\"text/css\">
			.body {
					font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
					font-size:12px;
					color:#666666;
					text-decoration:none;
				  }

			 a:link {
					font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
					font-size:12px			 
					text-decoration: none;
					color: #333333;
				  }
			 a:visited {
					font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
					font-size:12px			 
					text-decoration: none;
					color: #CCCCCC;
				}
			a:hover {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:12px			
				text-decoration: none;
				color: #999999;
			}
			a:active {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:12px			
				text-decoration: none;
				color: #333333;
				}
        </style>		
		
        </HEAD>
        <BODY>
		<A HREF=general.html TARGET=Category><IMG SRC=Images/ematrix_logo.gif BORDER=0></A><BR><BR><BR>
    "

	foreach category_menu $Category_Order {
        # List administrative objects for category asked
        if { $category_menu == $category } {
			if { $category_menu == "form" } {
				append Summary_Menu_Page "<A HREF=summary.html TARGET=\"Summary\"><IMG SRC=Images/moins.gif BORDER=0 WIDTH=9 HEIGHT=9> webform</A><BR>"			
			} else {
				append Summary_Menu_Page "<A HREF=summary.html TARGET=\"Summary\"><IMG SRC=Images/moins.gif BORDER=0 WIDTH=9 HEIGHT=9> $category_menu</A><BR>"
			}
			
            if {$category == "Object Generator Objects"} {
                set Objects [lsort -dictionary $glsObjectGeneratorObjects]
				append Summary_Menu_Page "<TABLE BORDER=0 CELLSPACING=0>"
                foreach object $Objects {
                    append Summary_Menu_Page "
                            <TR><TD WIDTH=25>&nbsp;</TD>
                            <TD ALIGN=LEFT NOWRAP><A HREF=\"[Replace_Space $category].html#[Replace_Space $object]\" TARGET=\"Category\">$object</A><BR></TD>
                            </TR>
                    "
            }
            append Summary_Menu_Page "</TABLE>"						
			} elseif {$category == "Number Generator Objects"} {
                set Objects [lsort -dictionary $glsNumberGeneratorObjects]
				append Summary_Menu_Page "<TABLE BORDER=0 CELLSPACING=0>"
                foreach object $Objects {
                    append Summary_Menu_Page "
                            <TR><TD WIDTH=25>&nbsp;</TD>
                            <TD ALIGN=LEFT NOWRAP><A HREF=\"[Replace_Space $category].html#[Replace_Space $object]\" TARGET=\"Category\">$object</A><BR></TD>
                            </TR>
                    "
            }
            append Summary_Menu_Page "</TABLE>"			
			} elseif {$category == "Trigger Manager Objects"} {
                set Objects [lsort -dictionary $glsTriggerManagerObjects]
				append Summary_Menu_Page "<TABLE BORDER=0 CELLSPACING=0>"
                foreach object $Objects {
                    append Summary_Menu_Page "
                            <TR><TD WIDTH=25>&nbsp;</TD>
                            <TD ALIGN=LEFT NOWRAP><A HREF=\"[Replace_Space $category].html#[Replace_Space $object]\" TARGET=\"Category\">$object</A><BR></TD>
                            </TR>
                    "
            }
            append Summary_Menu_Page "</TABLE>"
            } else {
                set Objects $aAdmin($category)
				append Summary_Menu_Page "<TABLE BORDER=0 CELLSPACING=0>"
                foreach object $Objects {
					set sSubstitute [Replace_Space $category]
					#KYB Start Spinner V6R2014x Enhanced HTML Documentation
					#if {$sSubstitute == "index"} {set sSubstitute "index_"}
					#KYB End Spinner V6R2014x Enhanced HTML Documentation
					append Summary_Menu_Page "
								<TR><TD WIDTH=25>&nbsp;</TD>
								<TD ALIGN=LEFT NOWRAP><A HREF=\"$sSubstitute.html#[Replace_Space $object]\" TARGET=\"Category\">$object</A><BR></TD>
								</TR>
						"
                }
                append Summary_Menu_Page "</TABLE>"

            }
            # Update Statistic
            #puts "Add_Value_Element_To_Array Statistic $category [llength $Objects]"
            Add_Value_Element_To_Array Statistic $category [llength $Objects]

        # Display a link for other category
        } else {
            if {$category_menu == "index"} {
                append Summary_Menu_Page "<A HREF=\"index__menu.html\" TARGET=\"Summary\"><IMG SRC=Images/plus.gif BORDER=0 WIDTH=9 HEIGHT=9> $category_menu</A><BR>"
            } else {
               append Summary_Menu_Page "<A HREF=\"${category_menu}_menu.html\" TARGET=\"Summary\"><IMG SRC=Images/plus.gif BORDER=0 WIDTH=9 HEIGHT=9> $category_menu</A><BR>"
            }
        }
    }

    if { $bDumpSchema } { pfile_write [ file join $Out_Directory [Replace_Space $category]_menu.html ] $Summary_Menu_Page }
    return 0
}


#************************************************************************
# Procedure:   pfile_write
#
# Description: Procedure to write a variable to file.
#
# Parameters:  The filename to write to,
#              The data variable.
#
# Returns:     Nothing
#************************************************************************

proc pfile_write { filename data } {
  return  [catch {
    set fileid [open $filename "w+"]
    puts $fileid $data
    close $fileid
  }]
}
#End pfile_write


#************************************************************************
# Procedure:   pfile_read
#
# Description: Procedure to read a file.
#
# Parameters:  The filename to read from.
#
# Returns:     The file data
#************************************************************************

proc pfile_read { filename } {

  set data ""
  if { [file readable $filename] } {
    set fd [open $filename "r"]
    set data [read $fd]
    close $fd
  }
  return $data
}
#End file_read



proc pRemSpecChar {filename} {

    # Note, still need to add double quote, less than and greater than.
    #List elements are, {\\\" %22} {< %3C} {> %3E}
    set lChar [list {\\\\ %5C} {/ %2F} {: %3A} {\\\* %2A} {\\\? %3F} {\\\| %7C}]

    foreach i $lChar {

        set sLabel [lindex $i 0]
        set sValue [lindex $i 1]

        regsub -all -- "$sLabel" $filename "$sValue" filename

    }

    return $filename

}
#End pRemSpecChar



proc pFormat { lData sHead sType } {

    global lAccessModes
    global sPositive
    global sNegative


    set sFormat ""
    append sFormat "<html>\n"
    append sFormat {
        <STYLE type=text/css>
        TD.odd {
            BACKGROUND-COLOR: #EDEDED
        }
        TD.even {
            BACKGROUND-COLOR: #FFFFFF
        }
		
        .body {
				font-family: \"Times New Roman\";
				font-size:12px;
				color:#666666;
				text-decoration:none;
			 }

        a:link {
			text-decoration: none;
			color: #FFAD85;
			}
		a:visited {
			text-decoration: none;
			color: #C28566;
			}
		a:hover {
			text-decoration: none;
			color: #2E0000;
			background-color:#FFFFFF;
			}
		a:active {
			text-decoration: none;
			color: #0000FF;
			}		
		
        </STYLE>
    }
    append sFormat "<head>"
    append sFormat "<title>HTML document</title>"
    append sFormat "</head>\n"
    append sFormat "<body>"
    append sFormat "<h1><center>$sType - $sHead</center></h1>\n\n"

    set sFontC {#0000ff}

    if { [ llength $lData ] == 0 } {
        append sFormat "<h2><center>No Data</center></h2>\n"
        return $sFormat
    }

    append sFormat "<div style=\"width:80%\">\n"
    append sFormat "<table rows=\"1\" border=\"1\" cols=\"35\" align=\"Center\" border=\"1\" callpadding=\"1\" cellspacing=\"1\" width=\"100%\" ID=\"tblHeader\">\n"
    # construct the table header row
    append sFormat "<tr>\n"
    if { $sType == "Policy" } {
        append sFormat "<td CLASS=even VALIGN=BOTTOM ALIGN=LEFT><B>State</B></td>\n"
        append sFormat "<td CLASS=even VALIGN=BOTTOM ALIGN=LEFT><B>User</B></td>\n"
    } else {
        append sFormat "<td CLASS=even VALIGN=BOTTOM ALIGN=LEFT><B>Policy</B></td>\n"
        append sFormat "<td CLASS=even VALIGN=BOTTOM ALIGN=LEFT><B>State</B></td>\n"
    }
    # construct the access headers
    set lModes $lAccessModes
    lappend lModes Filter
    
    if {$sType == "Policy"} {
        foreach sMode $lModes {
            append sFormat "<td CLASS=even VALIGN=BOTTOM ALIGN=CENTER><IMG SRC=\"../Images/[string tolower $sMode].gif\" ALT=\"$sMode\"></TD>\n"
        }
    } else {
        foreach sMode $lModes {
            append sFormat "<td CLASS=even VALIGN=BOTTOM ALIGN=CENTER><IMG SRC=\"../../Images/[string tolower $sMode].gif\" ALT=\"$sMode\"></TD>\n"
        }
    }
    append sFormat "</tr>\n"
    append sFormat "</table>\n"

    append sFormat "<div style=\"height:400;overflow:auto;\">\n"
    append sFormat "<table cols=\"35\" border=\"1\" width=\"100%\" id=\"tblData\">\n"
    append sFormat "<tr height=\"0\">\n"

    for {set x 0} {$x < 35} {incr x} {
        append sFormat "<td></td>\n"
    }
    append sFormat "</tr>\n"

    set sData $lData
    set sLastPolicy ""
    set sLastState ""
    set sMajorRowClass "even"
    set sMinorRowClass "even"
    set sBlankRowClass "Spacer"
    set nAccessColumns [expr [llength $lAccessModes] + 3]
    set sSeparator "<TD COLSPAN=$nAccessColumns COLOR=\"#000000\" BGCOLOR=\"#000000\"><img src=\"../Images/utilspace.gif\" width=1 height=1></TD>\n"
    set sPositiveImage "Y"
    set sNegativeImage "&nbsp"

    set sTempData "@[join $sData @]"
    foreach line $sData {
        if { $line == "" } {
            continue
        }
        set sPolicyData [ lindex $line 1 ]
        set sLeft [ split [ lindex $line 0 ] , ]
        set sOwner [ lindex $sLeft 2 ]
        set sLeft [ split [ lindex $sLeft 0 ] | ]
        set sPolicy [ lindex $sLeft 0 ]
        set sState [ lindex $sLeft 2 ]
        set sRights [ lindex $sPolicyData 0 ]
        set sFilter [ lindex $sPolicyData 1 ]
        if { $sFilter == "" } {
            set sFilter "-"
        }
        append sFormat "<tr>"
        regsub -all {\(} $sPolicy {\\(} sTempPolicy
        regsub -all {\)} $sTempPolicy {\\)} sTempPolicy
        if { $sType == "Policy" } {
            # figure out how many rows there are with the same state
            # Make the state Name spans the correct number of rows
            if {$sState != $sLastState} {
                set sMatch "@\[\{\]?$sTempPolicy\\|\[0-9\]+\\|$sState\\," ;#check for \}
                set nNumUsersPerState [regsub -all $sMatch $sTempData {} sGarbage]
                append sFormat "$sSeparator</tr><tr>"
                append sFormat "<td CLASS=\"$sMajorRowClass\" rowspan=$nNumUsersPerState>$sState</td>"
                if {$sMajorRowClass == "odd"} {
                    set sMajorRowClass "even"
                } else {
                    set sMajorRowClass "odd"
                }
            }
            append sFormat "<td CLASS=\"$sMinorRowClass\"><A HREF=\"user/[Replace_Space $sOwner].html\">$sOwner</A></td>"
        } else {
            if {$sPolicy != $sLastPolicy} {
                set sMatch "@\[\{\]?$sTempPolicy\\|\[0-9\]+\\|\[^\\|\\,\]+\\,\[0-9\]+\\,$sOwner" ;#check for \}
                set nNumStatesPerPolicy [regsub -all $sMatch $sTempData {????} sGarbage]
                append sFormat "$sSeparator</tr><tr>\n"
                append sFormat "<td CLASS=\"$sMajorRowClass\" rowspan=$nNumStatesPerPolicy><A HREF=\"../[Replace_Space $sPolicy].html\">$sPolicy</A></td>"
                if {$sMajorRowClass == "odd"} {
                    set sMajorRowClass "even"
                } else {
                    set sMajorRowClass "odd"
                }
            }
            append sFormat "<td CLASS=\"$sMinorRowClass\">$sState</td>"
        }

        if { $sRights == "all" } {
            set sNegativeValue $sPositiveImage
        } else {
            set sNegativeValue $sNegativeImage
        }
        foreach sMode $lAccessModes {
            set sMode [string tolower $sMode]
            if { [ lsearch $sRights $sMode ] == -1 } {
                append sFormat "<td CLASS=\"$sMinorRowClass\">$sNegativeValue</td>"
            } else {
                append sFormat "<td CLASS=\"$sMinorRowClass\">$sPositiveImage</td>"
            }
        }
        append sFormat "<td CLASS=\"$sMinorRowClass\"> $sFilter</td>"
        append sFormat "</tr>\n\n"
        if {$sMinorRowClass == "odd"} {
            set sMinorRowClass "even"
        } else {
            set sMinorRowClass "odd"
        }
        set sLastState $sState
        set sLastPolicy $sPolicy
    }

    append sFormat "</td>\n"
    append sFormat "        </table>\n"
    append sFormat "</div>\n"
    append sFormat "</td> </tr> </table>\n"
    append sFormat "</div>\n"

    append sFormat "<script language=\"javascript\">

    doSyncTables();
    function doSyncTables(){
    var i;
    var nDtlCol = document.getElementById('tblData').rows\[0\].cells.length;
    var hdrColLength=\"\";
    var dltColLength=\"\";
    var dltColLength2=\"\";
    var colWidth = 0;

    for (i=0; i < nDtlCol; i++) {
        dltColLength2 = document.getElementById('tblHeader').cells\[i\].offsetWidth;
        dltColLength = document.getElementById('tblData').cells\[i\].offsetWidth;
        dltColLength2=parseInt(dltColLength2);
        dltColLength=parseInt(dltColLength);
     if (dltColLength<dltColLength2){
         dltColLength = dltColLength2;
     }
     if (dltColLength > 0){
         document.getElementById('tblData').cells\[i\].width = dltColLength;
         document.getElementById('tblHeader').cells\[i\].width = dltColLength;
     }     
     }     
     window.tblHeader.width  = window.tblData.offsetWidth;
     }
     </script>\n"

    append sFormat "    </body>\n"
    append sFormat "</html>\n"

    return $sFormat
}

proc Generate_ExtendedPolicy { } {

    global bDumpSchema
    global bStatus
    global bExtendedPolicy
    global lExtendedPersonData
    global Out_Directory
    global lAccessModes
    global nMxVer
    upvar aAdmin aAdmin

    set lAccessModes [ list Read Modify Delete Checkout Checkin Schedule Lock \
        Unlock Execute Freeze Thaw Create Revise Promote Demote Grant Enable \
        Disable Override ChangeName ChangeType ChangeOwner ChangePolicy Revoke \
        ChangeVault FromConnect ToConnect FromDisconnect ToDisconnect \
        ViewForm Modifyform Show ]
        
    set lSpecialUsers [ list Public Owner ]
        
    global sPositive
    global sNegative

    set sPositive Y
    set sNegative "-"

    set lPolicy $aAdmin(policy)
    set lRule $aAdmin(rule)
    set lPerson $aAdmin(person)
    set lRole $aAdmin(role)
    set lGroup $aAdmin(group)
    set lAssociation $aAdmin(association)

    if {$bStatus} {puts "Start Process Extended Policy ..."}

    foreach sPol $lPolicy {
        set sStates [ split [ mql print policy $sPol select state dump | ] | ]
        set bAllstate FALSE
        if {$nMxVer >= 10.8} {set bAllstate [ mql print policy $sPol select allstate dump ]}
        if {$sStates != [list ] && $bAllstate} {lappend sStates "allstate"}
        set sStOrder 0
        foreach sSt $sStates {
            if {$sSt == "allstate"} {
                set sOwner [ split [ string trim [ mql print policy $sPol select allstate.owneraccess dump | ] ] , ]
                set data($sPol|$sStOrder|$sSt,0,Owner) [ list $sOwner "" ]
                set sPublic [ split [ string trim [ mql print policy $sPol select allstate.publicaccess dump | ] ] , ]
                set data($sPol|$sStOrder|$sSt,0,Public) [ list $sPublic "" ]
                set sUsers [ split [ mql print policy $sPol select allstate.access ] \n ]
            } else {
                set sOwner [ split [ string trim [ mql print policy $sPol select state\[$sSt\].owneraccess dump | ] ] , ]
                set data($sPol|$sStOrder|$sSt,0,Owner) [ list $sOwner "" ]
                set sPublic [ split [ string trim [ mql print policy $sPol select state\[$sSt\].publicaccess dump | ] ] , ]
                set data($sPol|$sStOrder|$sSt,0,Public) [ list $sPublic "" ]
                set sUsers [ split [ mql print policy $sPol select state\[$sSt\].access ] \n ]
            }
            foreach i $sUsers {
                set i [ string trim $i ]
                if {[string first "policy" $i] == 0} {continue}
                if { $i != "" } {
                    #MODIFICATION by FIT -start
                    set sLine [ split $i "=" ]
		    #Modified to fix incident 365960 on 7th Jan 09 - Start
		    set sUs [string range [ string trim [lindex $sLine 0 ]] [ string first "." $sLine ] end ]
		    #Modified to fix incident 365960 on 7th Jan 09 - End
	    
                    #set sLine [ lindex [ split $i "." ] 1 ]
                    #set sLine [ split $sLine "=" ]
                    #MODIFICATION by FIT -end
                    set sRights [ split [ string trim [ lindex $sLine 1 ] ] , ]
                    if { $sRights == "all" } {
#                        set sRights $lAccessModes
                    } elseif { $sRights == "none" } {
                        set sRights ""
                    }
                    #MODIFICATION by FIT -start
                    #set sUs [string trim [ lindex $sLine 0 ] ]
                    #MODIFICATION by FIT -end
                    
                    if {[string first "access\[" $sUs] > -1} {
                        regsub "access\134\[" $sUs "|" sUs
                        set sUs [lindex [split $sUs |] 1]
                        regsub "\134\]" $sUs "" sOwner
                        if {$sSt == "allstate"} {
                            set sExpression [ mql print policy "$sPol" select allstate.filter\[$sOwner\] dump ]
                        } else {
                            set sExpression [ mql print policy "$sPol" select state\[$sSt\].filter\[$sOwner\] dump ]
                        }
                        set data($sPol|$sStOrder|$sSt,1,$sOwner) [ list $sRights $sExpression ]
                    }
                }
            }
            incr sStOrder
        }
    }
 
    set sSpin ""
    set bb ""
    if {$bStatus} {puts "Start Process Extended Policy by Policy Name ..."}
    foreach sP $lPolicy {
        set pu [ lsort -dictionary [ array name data "$sP|*|*,*,*" ] ]
        foreach i $pu {
            lappend sSpin [ list $i $data($i) ]
        }
        if { $bDumpSchema && $bExtendedPolicy } {
            set bb [ pFormat $sSpin $sP Policy ]
            set sFile [ Replace_Space $sP ]
            pfile_write $Out_Directory/Policy/$sFile.html $bb
        }
        set bb ""
        set sSpin ""
        set sPolicySpin ""
    }
     
    if {$bStatus} {puts "Start Process Extended Rule ..."}

    foreach sRul $lRule {
        set sOwner [ split [ string trim [ mql print rule $sRul select owneraccess dump | ] ] , ]
        set data($sRul|0|$sRul,0,Owner) [ list $sOwner "" ]
        set sPublic [ split [ string trim [ mql print rule $sRul select publicaccess dump | ] ] , ]
        set data($sRul|0|$sRul,0,Public) [ list $sPublic "" ]
        set sUsers [ split [ mql print rule $sRul select access ] \n ]
        foreach i $sUsers {
            set i [ string trim $i ]
            if {[string first "rule" $i] == 0} {continue}
            if { $i != "" } {
                set sLine [ split $i "=" ]
                set sRights [ split [ string trim [ lindex $sLine 1 ] ] , ]
                if { $sRights == "all" } {
#                    set sRights $lAccessModes
                } elseif { $sRights == "none" } {
                    set sRights ""
                }
                set sUs [string trim [ lindex $sLine 0 ] ]
                if {[string first "access\[" $sUs] > -1} {
                    regsub "access\134\[" $sUs "|" sUs
                    set sUs [lindex [split $sUs |] 1]
                    regsub "\134\]" $sUs "" sOwner
                    set sExpression [ mql print rule "$sRul" select filter\[$sOwner\] dump ]
                    set data($sRul|0|$sRul,1,$sOwner) [ list $sRights $sExpression ]
                }
            }
        }
    }
 
    set sSpin ""
    set bb ""
    if {$bStatus} {puts "Start Process Extended Rule by Rule Name ..."}
    foreach sR $lRule {
        set ru [ lsort -dictionary [ array name data "$sR|*|*,*,*" ] ]
        foreach i $ru {
            lappend sSpin [ list $i $data($i) ]
        }
        if { $bDumpSchema && $bExtendedPolicy } {
            set bb [ pFormat $sSpin $sR Rule ]
            set sFile [ Replace_Space $sR ]
            pfile_write $Out_Directory/Rule/$sFile.html $bb
        }
        set bb ""
        set sSpin ""
        set sRuleSpin ""
    }
     
    set sSpin ""
    set bb ""
    if { $bDumpSchema && $bExtendedPolicy} {
        if {$bStatus} {puts "Start Process Extended Policy Schema for Person ..."}
        foreach sP $lPerson {
            set pu [ lsort -dictionary [ array name data "*,*,$sP" ] ]
            if {$pu == ""} {continue}
            lappend lExtendedPersonData $sP
            foreach i $pu {
                lappend sSpin [ list $i $data($i) ]
            }
            set bb [ pFormat $sSpin $sP Person ]
            set sFile [ Replace_Space $sP ]
            pfile_write $Out_Directory/Policy/user/$sFile.html $bb
            set bb ""
            set sSpin ""
        }
    }


    set sSpin ""
    set bb ""
    if { $bDumpSchema && $bExtendedPolicy} {
        if {$bStatus} {puts "Start Process Extended Policy Schema for Role ..."}
        foreach sP $lRole {
            set pu [ lsort -dictionary [ array name data "*,*,$sP" ] ]
            foreach i $pu {
                lappend sSpin [ list $i $data($i) ]
            }
            set bb [ pFormat $sSpin $sP Role ]
            set sFile [ Replace_Space $sP ]        
            pfile_write $Out_Directory/Policy/user/$sFile.html $bb
            set bb ""
            set sSpin ""
        }
    }


    set sSpin ""
    set bb ""
    if { $bDumpSchema && $bExtendedPolicy} {
        if {$bStatus} {puts "Start Process Extended Policy Schema for Group ..."}
        foreach sP $lGroup {
            set pu [ lsort -dictionary [ array name data "*,*,$sP" ] ]
            foreach i $pu {
                lappend sSpin [ list $i $data($i) ]
            }
            set bb [ pFormat $sSpin $sP Group ]
            set sFile [ Replace_Space $sP ]        
            pfile_write $Out_Directory/Policy/user/$sFile.html $bb
            set bb ""
            set sSpin ""
        }
    }

    set sSpin ""
    set bb ""
    if { $bDumpSchema && $bExtendedPolicy} {
        if {$bStatus} {puts "Start Process Extended Policy Schema for Association ..."}
        foreach sP $lAssociation {
            set pu [ lsort -dictionary [ array name data "*,*,$sP" ] ]
            foreach i $pu {
                lappend sSpin [ list $i $data($i) ]
            }
            set bb [ pFormat $sSpin $sP Association ]
            set sFile [ Replace_Space $sP ]        
            pfile_write $Out_Directory/Policy/user/$sFile.html $bb
            set bb ""
            set sSpin ""
        }
    }


    set sSpin ""
    set bb ""
    if { $bDumpSchema && $bExtendedPolicy} {
        if {$bStatus} {puts "Start Process Extended Policy Schema for SpecialUsers ..."}
        foreach sP $lSpecialUsers {
            set pu [ lsort -dictionary [ array name data "*,*,$sP" ] ]
            foreach i $pu {
                lappend sSpin [ list $i $data($i) ]
            }
            set bb [ pFormat $sSpin $sP Special ]
            set sFile [ Replace_Space $sP ]        
            pfile_write $Out_Directory/Policy/user/$sFile.html $bb
            set bb ""
            set sSpin ""
        }
    }
}



################################################################################
# Generate_TriggerLinks
#
#   Parameters : TriggerData string
#   Return     : HTML formatted string containing Trigger information and hyperlinks
#
proc Generate_TriggerLinks {lsTriggerData sAdminType sAdminName {sState ""}} {
    upvar aTriggerXRef aTriggerXRef

    global glsPrograms
    global glsTriggerManagerObjects

    set sTempHTML "<TABLE BORDER=1 CELLSPACING=0 WIDTH=\"100%\">"
    foreach trigger $lsTriggerData {
        set trigger [split $trigger :]
        set trigger_event [lindex $trigger 0]
        set program_parameters [lindex $trigger 1]
        set bracket_index [string first ( $program_parameters]
        set program_name [string range $program_parameters 0 [expr $bracket_index -1]]
        set parameters [string range $program_parameters [expr $bracket_index +1] [expr [string length $program_parameters] -2]]

        # Add the trigger type,policy, state to the global list of "Where-used" triggers
        set lTriggerRef ""
        catch { set lTriggerRef $aTriggerXRef($program_name) }
        set sRefData "$sAdminType|$sAdminName|$sState|$trigger_event"
        if {[lsearch -exact $lTriggerRef $sRefData] == -1} {
            lappend lTriggerRef $sRefData
        }
        set aTriggerXRef($program_name) $lTriggerRef

        append sTempHTML "<TR><TD><B><FONT SIZE=\"2\" FACE=\"Times New Roman\">$trigger_event</B></FONT></TD><TD>"
        append sTempHTML "<B><FONT SIZE=\"2\" FACE=\"Times New Roman\"><A HREF=\"program.html#[Replace_Space $program_name]\">$program_name</B></FONT></A> "
        # Assume the parameters are in tcl list format
        # for each list element, see if it is corresponds to an
        # eServiceTrigger object or an actual program
        set sParamData ""
        foreach sParam $parameters {
            set bIsProgram [lsearch -exact $glsPrograms $sParam]
            set bIsTriggerObject [lsearch -exact $glsTriggerManagerObjects $sParam]
            if {$bIsProgram != -1  || $bIsTriggerObject != -1 } \
            {
                set lTriggerRef ""
                catch { set lTriggerRef $aTriggerXRef($sParam) }
                set sRefData "$sAdminType|$sAdminName|$sState|$trigger_event"
                if {[lsearch -exact $lTriggerRef $sRefData] == -1} {
                    lappend lTriggerRef $sRefData
                }
                set aTriggerXRef($sParam) $lTriggerRef

                # create the hyperlinks to the parameters
                if {$bIsProgram != -1} {
                    set sProgramLink "<A HREF=\"program.html#[Replace_Space $sParam]\"><FONT SIZE=\"2\" FACE=\"Times New Roman\" COLOR=\"#FFAD85\">$sParam</FONT></A>"
                } elseif {$bIsTriggerObject != -1} {
                    set sProgramLink "<A HREF=\"Trigger_Manager_Objects.html#[Replace_Space $sParam]\"><FONT SIZE=\"2\" FACE=\"Times New Roman\" COLOR=\"#FFAD85\">$sParam</FONT></A>"
                } else {
                    set sProgramLink "<FONT SIZE=\"2\" FACE=\"Times New Roman\">$sParam</FONT>"
                }
                append sParamData " $sProgramLink"
            }
        }
        if {$sParamData == ""} {
            set sParamData "&nbsp;"
        }

        append sTempHTML "$sParamData</TD></TR>\n"
    }
    append sTempHTML "</TABLE>"

    return $sTempHTML

}

################################################################################
# Generate_ObjectGeneratorObjects
#
#   Parameters : none
#   Return     : none
#
proc Generate_ObjectGeneratorObjects { } {
  global Out_Directory Out_Directory
  global bDumpSchema
  set sType "eService Object Generator"

  if { [ catch { mql print type $sType } sErr ] == 0 } {

    set lHeads [list "eService Program Name" "eService Sequence Number" ]
    set lAtt [ lsort -dictionary -index end [ split [ mql print type $sType select attribute dump | ] | ] ]

    set sCmd "mql temp query bus \"$sType\" * * select current description"
    foreach sAttr $lHeads {
        append sCmd " attribute\\\[$sAttr\\\].value"
    }
    foreach sAttr $lAtt {
        if {[lsearch -exact $lHeads $sAttr] == -1} {
            lappend lHeads $sAttr
            append sCmd " attribute\\\[$sAttr\\\].value"
        }
    }

    append sCmd " dump |"

    # Append the basic info to the list of information returned
    if { [ catch { eval $sCmd } sMsg ] == 0 } {
        set Object [ split $sMsg \n ]
    } else {
      puts "an error occurred\nthe message is:\n$sMsg"
    }

    set Object [lsort -dictionary $Object]
    set Page_Content [pFormatObjectObj_html $sType $lHeads $Object]
    if {$bDumpSchema} { pfile_write [file join $Out_Directory Object_Generator_Objects.html] $Page_Content }
    }
}

################################################################################
# Generate_NumberGeneratorObjects
#
#   Parameters : none
#   Return     : none
#
proc Generate_NumberGeneratorObjects { } {
  global Out_Directory Out_Directory
  global bDumpSchema
  set sType "eService Number Generator"

  if { [ catch { mql print type $sType } sErr ] == 0 } {

    set lHeads [list "eService Program Name" "eService Sequence Number" ]
    set lAtt [ lsort -dictionary -index end [ split [ mql print type $sType select attribute dump | ] | ] ]

    set sCmd "mql temp query bus \"$sType\" * * select current description"
    foreach sAttr $lHeads {
        append sCmd " attribute\\\[$sAttr\\\].value"
    }
    foreach sAttr $lAtt {
        if {[lsearch -exact $lHeads $sAttr] == -1} {
            lappend lHeads $sAttr
            append sCmd " attribute\\\[$sAttr\\\].value"
        }
    }

    append sCmd " dump |"

    # Append the basic info to the list of information returned
    if { [ catch { eval $sCmd } sMsg ] == 0 } {
        set Object [ split $sMsg \n ]
    } else {
      puts "an error occurred\nthe message is:\n$sMsg"
    }

    set Object [lsort -dictionary $Object]
    set Page_Content [pFormatNumberObj_html $sType $lHeads $Object]
    if {$bDumpSchema} { pfile_write [file join $Out_Directory Number_Generator_Objects.html] $Page_Content }
    }
}

################################################################################
# Generate_TriggerObjects
#
#   Parameters : none
#   Return     : none
#
proc Generate_TriggerObjects { } {
  global Out_Directory Out_Directory
  global bDumpSchema
  set sType "eService Trigger Program Parameters"

  if { [ catch { mql print type $sType } sErr ] == 0 } {

    set lHeads [list "eService Program Name" "eService Sequence Number" ]
    set lAtt [ lsort -dictionary -index end [ split [ mql print type $sType select attribute dump | ] | ] ]

    set sCmd "mql temp query bus \"$sType\" * * select current description"
    foreach sAttr $lHeads {
        append sCmd " attribute\\\[$sAttr\\\].value"
    }
    foreach sAttr $lAtt {
        if {[lsearch -exact $lHeads $sAttr] == -1} {
            lappend lHeads $sAttr
            append sCmd " attribute\\\[$sAttr\\\].value"
        }
    }
#    set sRecSep [format "%c" 127]
#    append sCmd " dump | recordsep $sRecSep"
    append sCmd " dump |"

    # Append the basic info to the list of information returned
    if { [ catch { eval $sCmd } sMsg ] == 0 } {
        set Object [ split $sMsg \n ]
    } else {
      puts "an error occurred\nthe message is:\n$sMsg"
    }

    set Object [lsort -dictionary $Object]
    set Page_Content [pFormatTriggerObj_html $sType $lHeads $Object]
    if {$bDumpSchema} { pfile_write [file join $Out_Directory Trigger_Manager_Objects.html] $Page_Content }
    }
}

################################################################################
# pFormatTriggerObj_html
#
#   Parameters : none
#   Return     : none
#
proc pFormatTriggerObj_html { sType lAtt data } {
    global glsTriggerManagerObjects

    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $sType ]

    regsub -all "\n" $data {<BR>} data
    set sLastTriggerObjName ""

    append Page_Content "<TABLE BORDER=1 CELLSPACING=0>\n"

    foreach linedata $data {
        set lineinfo [ split $linedata | ]
        set nCount 0
        set lsBasicInfo [lrange $lineinfo 0 4]
        set lineinfo [lrange $lineinfo 5 end]
        set sObjType  [lindex $lsBasicInfo 0]
        set sObjName  [lindex $lsBasicInfo 1]
        set sObjRev   [lindex $lsBasicInfo 2]
        set sObjState [lindex $lsBasicInfo 3]
        set sObjDesc  [lindex $lsBasicInfo 4]
        if {$sObjName != $sLastTriggerObjName} {
            lappend glsTriggerManagerObjects $sObjName
            append Page_Content "\n<TR><TD COLSPAN=2 ALIGN=LEFT BGCOLOR=#F5F5F5 VALIGN=BOTTOM>"
            append Page_Content "<A NAME=\"[Replace_Space $sObjName]\">"
            append Page_Content "<B>$sObjName</B>"
            append Page_Content "</A>"
            append Page_Content "</TD></TR>\n"
        }
        append Page_Content "<TR><TD ALIGN=LEFT BGCOLOR=#DCDCDC VALIGN=BOTTOM>"
        append Page_Content "Id"
        append Page_Content "</TD>\n"
        append Page_Content "<TD ALIGN=LEFT VALIGN=BOTTOM>"
        append Page_Content "$sObjRev"
        append Page_Content "</TD></TR>\n"
        if {$sObjState == "Active"} {
            set sColor "#009900"
        } else {
            set sColor "#FF0000"
        }
        append Page_Content "
        <TR>
        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>State</TD>
        <TD ALIGN=LEFT BGCOLOR=#FFFFFF WIDTH=300 VALIGN=BOTTOM>$sObjState</TD>
        </TR>
        <TR>
        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>Description</TD>
        <TD ALIGN=LEFT BGCOLOR=#FFFFFF WIDTH=300 VALIGN=BOTTOM>$sObjDesc</TD>
        </TR>"
        foreach sAttrName $lAtt sAttrValue $lineinfo {
            incr nCount
            if {$sAttrName == "eService Program Name"} {
                append Page_Content "<TR>
                    <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>$sAttrName</TD>
                    <TD ALIGN=LEFT WIDTH=300><A HREF=\"program.html#[Replace_Space $sAttrValue]\">$sAttrValue</A></TD>
                    </TR>\n"
            } else {
                if {$sAttrValue != ""} {
                    append Page_Content "<TR>
                        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>$sAttrName</TD>
                        <TD ALIGN=LEFT WIDTH=300>$sAttrValue</TD>
                        </TR>\n"
                }
            }
        }
        # End foreach
        append Page_Content "<TR><TD COLSPAN=2>&nbsp;</TD></TR>"
        set sLastTriggerObjName $sObjName
    }
    append Page_Content "</TABLE>\n<BR>"

    append Page_Content "
        </BODY>
        </HTML>"

    return $Page_Content

}

################################################################################
# pFormatNumberObj_html
#
#   Parameters : none
#   Return     : none
#
proc pFormatNumberObj_html { sType lAtt data } {
    global glsNumberGeneratorObjects
		
    # Head of HTML page
    set Page_Content [ pSetSchemaStyle $sType ]

    regsub -all "\n" $data {<BR>} data
    set sLastNumberObjName ""

    append Page_Content "<TABLE BORDER=1 CELLSPACING=0>\n"

    foreach linedata $data {
        set lineinfo [ split $linedata | ]
        set nCount 0
        set lsBasicInfo [lrange $lineinfo 0 4]
        set lineinfo [lrange $lineinfo 5 end]
        set sObjType  [lindex $lsBasicInfo 0]
        set sObjName  [lindex $lsBasicInfo 1]
        set sObjRev   [lindex $lsBasicInfo 2]
        set sObjState [lindex $lsBasicInfo 3]
        set sObjDesc  [lindex $lsBasicInfo 4]
        if {$sObjName != $sLastNumberObjName} {
            lappend glsNumberGeneratorObjects $sObjName
            append Page_Content "\n<TR><TD COLSPAN=2 ALIGN=LEFT BGCOLOR=#F5F5F5 VALIGN=BOTTOM>"
            append Page_Content "<A NAME=\"[Replace_Space $sObjName]\">"
            append Page_Content "<B>$sObjName</B>"
            append Page_Content "</A>"
            append Page_Content "</TD></TR>\n"
        }
        append Page_Content "<TR><TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=200 VALIGN=BOTTOM>"
        append Page_Content "Id"
        append Page_Content "</TD>\n"
        append Page_Content "<TD ALIGN=LEFT WIDTH=200 VALIGN=BOTTOM>"
        append Page_Content "$sObjRev"
        append Page_Content "</TD></TR>\n"
        if {$sObjState == "Active"} {
            set sColor "#009900"
        } else {
            set sColor "#FF0000"
        }
        append Page_Content "
        <TR>
        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=200>State</TD>
        <TD ALIGN=LEFT WIDTH=200 VALIGN=BOTTOM>$sObjState</TD>
        </TR>
        <TR>
        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=200>Description</TD>
        <TD ALIGN=LEFT WIDTH=200 VALIGN=BOTTOM>$sObjDesc</TD>
        </TR>"
        foreach sAttrName $lAtt sAttrValue $lineinfo {
            incr nCount
            if {$sAttrName == "eService Program Name"} {
                append Page_Content "<TR>
                    <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=200>$sAttrName</TD>
                    <TD ALIGN=LEFT WIDTH=200><A HREF=\"program.html#[Replace_Space $sAttrValue]\">$sAttrValue</A></TD>
                    </TR>\n"
            } else {
                if {$sAttrValue != ""} {
                    append Page_Content "<TR>
                        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=200>$sAttrName</TD>
                        <TD ALIGN=LEFT WIDTH=200>$sAttrValue</TD>
                        </TR>\n"
                }
            }
        }
        # End foreach
        append Page_Content "<TR><TD COLSPAN=2>&nbsp;</TD></TR>"
        set sLastNumberObjName $sObjName
    }
    append Page_Content "</TABLE>\n<BR>"

    append Page_Content "
        </BODY>
        </HTML>"

    return $Page_Content
}

################################################################################
# pFormatObjectObj_html
#
#   Parameters : none
#   Return     : none
#
proc pFormatObjectObj_html { sType lAtt data } {
    global glsObjectGeneratorObjects
    
	# Head of HTML page
    set Page_Content [ pSetSchemaStyle $sType ]

    regsub -all "\n" $data {<BR>} data
    set sLastObjectObjName ""

    append Page_Content "<TABLE BORDER=1 CELLSPACING=0>\n"

    foreach linedata $data {
        set lineinfo [ split $linedata | ]
        set nCount 0
        set lsBasicInfo [lrange $lineinfo 0 4]
        set lineinfo [lrange $lineinfo 5 end]
        set sObjType  [lindex $lsBasicInfo 0]
        set sObjName  [lindex $lsBasicInfo 1]
        set sObjRev   [lindex $lsBasicInfo 2]
        set sObjState [lindex $lsBasicInfo 3]
        set sObjDesc  [lindex $lsBasicInfo 4]
        if {$sObjName != $sLastObjectObjName} {
            lappend glsObjectGeneratorObjects $sObjName
            append Page_Content "\n<TR><TD COLSPAN=2 ALIGN=LEFT BGCOLOR=#F5F5F5 VALIGN=BOTTOM>"
            append Page_Content "<A NAME=\"[Replace_Space $sObjName]\">"
            append Page_Content "<B>$sObjName</B>"
            append Page_Content "</A>"
            append Page_Content "</TD></TR>\n"
        }
        append Page_Content "<TR><TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300 VALIGN=BOTTOM>"
        append Page_Content "Id"
        append Page_Content "</TD>\n"
        append Page_Content "<TD ALIGN=LEFT WIDTH=300 VALIGN=BOTTOM>"
        append Page_Content "$sObjRev"
        append Page_Content "</TD></TR>\n"
        if {$sObjState == "Active"} {
            set sColor "#009900"
        } else {
            set sColor "#FF0000"
        }
        append Page_Content "
        <TR>
        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>State</TD>
        <TD ALIGN=LEFT BGCOLOR=#FFFFFF WIDTH=300 VALIGN=BOTTOM>$sObjState</TD>
        </TR>
        <TR>
        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>Description</TD>
        <TD ALIGN=LEFT BGCOLOR=#FFFFFF WIDTH=300 VALIGN=BOTTOM>$sObjDesc</TD>
        </TR>"
        foreach sAttrName $lAtt sAttrValue $lineinfo {
            incr nCount
            if {$sAttrName == "eService Program Name"} {
                append Page_Content "<TR>
                    <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>$sAttrName</TD>
                    <TD ALIGN=LEFT WIDTH=300><A HREF=\"program.html#[Replace_Space $sAttrValue]\">$sAttrValue</A></TD>
                    </TR>\n"
            } else {
                if {$sAttrValue != ""} {
                    append Page_Content "<TR>
                        <TD ALIGN=LEFT BGCOLOR=#DCDCDC WIDTH=300>$sAttrName</TD>
                        <TD ALIGN=LEFT WIDTH=300>$sAttrValue</TD>
                        </TR>\n"
                }
            }
        }
        # End foreach
        append Page_Content "<TR><TD COLSPAN=2>&nbsp;</TD></TR>"
        set sLastObjectObjName $sObjName
    }
    append Page_Content "</TABLE>\n<BR>"

    append Page_Content "
        </BODY>
        </HTML>"

    return $Page_Content

}

################################################################################
# pRemoveElement
#
#   Parameters : list to clear
#   Return     : list with element removed
#
proc pRemoveElement { llist } {

    set bDone 0
    set sRemove "adm*"

    while { $bDone == 0 } {

        set nIndex [ lsearch $llist $sRemove ]
        if { $nIndex == -1 } {
            set bDone 1
        } else {
            set llist [ lreplace $llist $nIndex $nIndex ]
        }
    }

    return $llist
}


proc pProcessFile { lAdmin sFileName sDelimit } {

    global bStatus
    upvar aAdminTemp aAdminTemp

    set sFileData [ split [ pfile_read $sFileName ] \n ]
    set sFileLine1 [split [lindex $sFileData 0] $sDelimit ]
    set sFileMarker [ lindex $sFileLine1 0 ]
    set sInVersion [ lindex $sFileLine1 1 ]
    set sFileType [ lindex $sFileLine1 3 ]
    set nLineStart [ lindex $sFileLine1 5 ]
    if {$bStatus} {puts "Input File,\nVersion: $sInVersion\nData Type to process: $sFileType\nLine Start: $nLineStart"}

    switch $sFileType {
    filter {
        set sData [lrange $sFileData [ expr $nLineStart - 1] end]
        foreach sDataLine $sData {
            set sVal [ lindex [ split $sDataLine ] 2]
            if {$sVal == ""} {continue}

            set lLH [split [eval $sDataLine] \n]
            if { [ info exists aAdminTemp($sVal) ] == 0 } {
                set aAdminTemp($sVal) $lLH
            } else {
                set lRH $aAdminTemp($sVal)
                set aAdminTemp($sVal) [lindex [pCompareLists $lLH $lRH] 3]
            }
        }
    }
    data {
        set sHeaderRaw [ split [ lindex $sFileData [ expr $nLineStart - 1] ] "\t" ]
        set sHeader [ list ]
        foreach i $sHeaderRaw {
            lappend sHeader [ string trim $i ]
        }
        set sData [ lrange $sFileData $nLineStart end ]
        foreach sDataLine $sData {
            set sDataLine [ split $sDataLine $sDelimit ]
            foreach i $sHeader j $sDataLine {
                if { $j != "" } {
                    # Make sure the file header is in the admin list
                    if { [ lsearch $lAdmin ${i}* ] == -1 } {
                        if { $bStatus } { puts "Header error $i is not a recognized admin type, check developer file ..." }
                    }
                    # Check to see if name really exists
                    if {$i == "table" } {
                        set sRes [mql list $i $j system]
                    } else {
                        set sRes [mql list $i $j]
                    }
                    if {$sRes != ""} {
                        set aAdminTemp($i) [ lappend aAdminTemp($i) $j ]
                    } else {
                        if {$bStatus} {puts "Admin type $i, name $j, does not exist ..."}
                    }
                }
            }
        }
    }
    default {puts "unknown switch type, check input file"}
    } ;# End Switch
}
# End pIncludeFile



proc pMergeArray { sMode lAdminName } {

    upvar aAdmin aAdmin
    upvar aAdminTemp aAdminTemp

    foreach sType $lAdminName {
    
        if {[info exists aAdminTemp($sType)] == 0} {
            continue
        }

        set l1 $aAdmin($sType)
        set l2 $aAdminTemp($sType)

        switch $sMode {
        or {
            set lReturn [ pCompareLists $l1 $l2 ]
            set aAdmin($sType) [lindex $lReturn 3]
        }
        xor {
            set lReturn [ pCompareLists $l1 $l2 ]
            set aAdmin($sType) [lindex $lReturn 0] 
        }
        default {puts "unknown switch type from proc pMergeArray"}
        }
    }
}
# End pMergeArray



proc pCompareLists { lList1 lList2 } {

    set lCommon {}
    set lUnique1 {}
    set lOr $lList1
    foreach i1 $lList1 {
        set nFound [ lsearch $lList2 $i1 ]
        if { $nFound == -1 } {
            lappend lUnique1 $i1
        } else {
            lappend lCommon $i1
            set lList2 [ lreplace $lList2 $nFound $nFound ]
        }
    }
    foreach i2 $lList2 {
        set nFound [ lsearch $lOr $i2 ]
        if {$nFound == -1} {
            lappend lOr $i2
        }
    }
    set lResults [ list $lUnique1 $lCommon $lList2 $lOr ]
    return $lResults
}
# End pCompareLists


proc pCheckExists {sType sName} {

    set sCmd "mql list $sType $sName"
    if {[catch {eval $sCmd} sMsg] == 0} {
        set sExists $sMsg
    } else {
        puts "An error occurred with $sCmd, Error is: $sMsg"
        return "2|$sMsg"
    }
    set sExists $sMsg
    if {$sExists != ""} {
        return "0|"
    } else {
        return "1|"
    }
}
# End pCheckExists



################################################################################
# Generate
#
#   Parameters : none
#   Return     : none
#                This is the main processing routine.
#
proc Generate {sTypeFilter sNameFilter} {
    upvar Attribute_Types Attribute_Types
    upvar Attribute_Relationships Attribute_Relationships
    upvar Format_Policies Format_Policies
    upvar Statistic Statistic
    upvar Out_Directory Out_Directory
	#KYB Start
	upvar Main_Directory Main_Directory
	#KYB End
    upvar Image_Directory Image_Directory
    upvar aTriggerXRef aTriggerXRef
    upvar Location_Store Location_Store
    upvar Location_Site Location_Site
    upvar Store_Policy Store_Policy
    upvar aAdmin aAdmin

    global bStatus

    upvar aInclude aInclude
    upvar aExclude aExclude

    global bExtendedPolicy
    global lExtendedPersonData
    
    # A new array to hold all dirs, need to migrate them in!
    
    upvar aDirs aDirs

    global sDumpSchemaDir
    global sDumpSchemaDirSystem
    global sDumpSchemaDirBusiness
    global sDumpSchemaDirBusinessSource
    global sDumpSchemaDirBusinessPage
    global sDumpSchemaDirSystem
    global sDumpSchemaDirSystemMap
    global sDumpSchemaDirObjects
    global sDumpProperties


    global bDumpSchema
    global bDumpMQL
    global glsPrograms
    global glsTriggerManagerObjects
	global glsNumberGeneratorObjects
	global glsObjectGeneratorObjects
    set glsPrograms ""
    set glsTriggerManagerObjects ""
	set glsNumberGeneratorObjects ""
	set glsObjectGeneratorObjects ""

    global bStatus
    global nMxVer
    global sHeaderTitle
    
    global glsServer
    global bSuppressAdmReporting
    global bSuppressHidden

    set lExtendedPersonData [ list ]

	#KYB Fixed Spinner Version Issue for ENOVIA V6R2014x HFs
	#set sMqlVersion [mql version]
	set sMqlVersion "V6R2014x"

    if {[string first "V6" $sMqlVersion] >= 0} {
        # Modified below code while fixing 357785 on 12th Aug 08
        set nMxVer "10.9"
        # Modified above code while fixing 357785 on 12th Aug 08
    } else {
        set nMxVer [ join [lrange [split $sMqlVersion "."] 0 1] "."]
	  }
    array set aAdminTemp {}

# Check version, if < 9.5 stop.
    if { $nMxVer < 9.5 } {
        puts "version not supported"
        return
    }

    file mkdir $Out_Directory
    
    set sSchemaPolicy [ file join $Out_Directory Policy ]
    file mkdir $sSchemaPolicy
    set sSchemaPolicyUser [ file join $sSchemaPolicy user ]
    file mkdir $sSchemaPolicyUser
    set sSchemaRule [ file join $Out_Directory Rule ]
    file mkdir $sSchemaRule
    set Image_Directory [ file join $Out_Directory Images ]
    file mkdir $Image_Directory
    set sSchemaProgram [ file join $Out_Directory Programs ]
    file mkdir $sSchemaProgram
    
    if {$bDumpSchema} {
        if {$bStatus} {puts "Start Create Images ..."}
        Create_Images
    }

    # Generate main page
	#KYB Prefixed summary.html and general.html by Business folder path
    set Main_Page {
        <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 
            Frameset//EN""http://www.w3.org/TR/html4/loose.dtd">
        <HTML>
        <HEAD>
        <TITLE>Business Model Documentation</TITLE>
        </HEAD>
        <FRAMESET COLS="24%,*">
		  <FRAME NAME="Summary" SRC="Business/summary.html">
          <FRAME NAME="Category" SRC="Business/general.html">
        <NOFRAMES>
        <BODY>
        Not available
        </BODY>
        </NOFRAMES>
        </FRAMESET>
        </HTML>
    }
	
	#KYB Start Spinner V6R2014x Enhanced HTML Documentation
    #if {$bDumpSchema} { pfile_write [file join $Out_Directory index.html] $Main_Page }
	if {$bDumpSchema} { pfile_write [file join $Main_Directory index.html] $Main_Page }
	#KYB End Spinner V6R2014x Enhanced HTML Documentation

    # Generate summary page
    set Summary_Page "
        <HTML>
        <HEAD>
        <TITLE>Business Model Documentation</TITLE>
		
        <style type=\"text/css\">
			.body {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:16px;
				color:#666666;
				text-decoration:none;}

			a:link {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:16px;
				text-decoration: none;
				color: #333333;
			}
			a:visited {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:16px;			
				text-decoration: none;
				color: #CCCCCC;
			}
			a:hover {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:16px;
				text-decoration: none;
				color: #999999;
			}
			a:active {
				font-family: \"Times New Roman\", Arial, Helvetica, sans-serif;
				font-size:16px;			
				text-decoration: none;
				color: #333333;
			}
        </style>		
		
        </HEAD>
        <BODY>
        <A HREF=general.html TARGET=Category><IMG SRC=Images/ematrix_logo.gif BORDER=0></A><BR><BR><BR>
    "

    # This is the MASTER list of all admin types to process ANYWHERE.
    # Refer to readme for more details.  Version dependant.
	
	#KYB
	set lsSchema [list \
      program \
      role \
      group \
      person \
      association \
      attribute \
      type \
      relationship \
      format \
      policy \
      command \
      inquiry \
      menu \
      table \
      webform \
      channel \
      portal \
      rule \
      interface \
      expression \
      page \
      dimension \
      site \
      location \
      store \
      server \
      vault \
	  index \
   ]
   
# Short Name Array and Generate List Array
   foreach sSchema $lsSchema {
      set sShortSchema [string range $sSchema 0 3]
      set aSchema($sSchema) $sShortSchema
      set bSchema($sSchema) FALSE
   } 

   if {$sTypeFilter == "*"} {
		set lAdmin [ list {association complex} {attribute complex} {channel complex} {command complex}\
			{expression complex} {format complex} {group complex}\
			{index complex} {inquiry complex} {interface complex} {location complex} {menu complex}\
			{page complex} {person complex} {policy complex} {portal complex} {program complex}\
			{relationship complex} {role complex} {rule simple}\
			{site complex} {store complex} {server complex}\
			{table complex} {type complex} {vault complex} {form complex} ]      

		if {$nMxVer >= 10.7} {
			lappend lAdmin {dimension simple}
		}
   } else {
		  set lAdmin [ list ]
		  set lsType [split $sTypeFilter ,]
		  foreach sType $lsType {
			 set sType [string tolower [string trim $sType]]
			 set bFlag FALSE
			 foreach sSchema $lsSchema {
				if {[string range $sType 0 3] == $aSchema($sSchema)} {
				   set bFlag TRUE
				   if { $sSchema == "rule" || $sSchema == "dimension" } {
					   lappend lAdmin [ list $sSchema simple ]
				   } else {
					   if { $sSchema == "webform" } {
							lappend lAdmin [ list form complex ]
					   } else {					   
							lappend lAdmin [ list $sSchema complex ]
					   }
				   }
				   break
				} 
			 }
			 if {!$bFlag} {
				puts "ERROR: Schema type $sType is invalid.  Valid schema types are:\n [join $lsSchema , ]"
				exit 1
				return
			 }   
		  }
   }   

    # Initialize the aAdmin array elements and make empty.
    set lAdminName [ list ]
	
    foreach i $lsSchema {
        set sTypeName [lindex $i 0]
        set aAdmin($sTypeName) {}
    }
	
	foreach j $lAdmin {
        set sTypeSchemaName [lindex $j 0]
        lappend lAdminName $sTypeSchemaName
    }

    if { $aInclude(bMode) } {
        if { $bStatus } { puts "Build Data set from include file/s ..." }
        set lFile [ glob -nocomplain [ file join $aInclude(sDir) $aInclude(sMask) ] ]
        foreach sFile $lFile {
            unset aAdminTemp
            array set aAdminTemp {}
            pProcessFile $lAdmin $sFile $aInclude(sDelimit)
            pMergeArray or $lAdminName
        }
    } else {
        if { $bStatus } { puts "Build Data set from database ..." }

        foreach lAdminType $lAdmin {
            set sAdminType [ lindex $lAdminType 0 ]
			if { $sAdminType == "form" } {
				if { $sNameFilter == "*" } {
					set lValues [lsort -dictionary [split [mql list $sAdminType] \n]]
				} else {
					set lValues [lsort -dictionary [split [mql list $sAdminType $sNameFilter] \n]]
				}
				set lWebFormValues [ list ]
				
				foreach sValue $lValues {
					if {[mql print form $sValue select web dump] == "TRUE"} {
						#add to list
						lappend lWebFormValues $sValue
					}
				}				
				set lValues $lWebFormValues		 	
			} elseif { $sAdminType == "table" } {
				if { $sNameFilter == "*" } {
					set lValues [lsort -dictionary [split [mql list $sAdminType system] \n]]
				} else {
					set lValues [lsort -dictionary [split [mql list $sAdminType system $sNameFilter] \n]]
				}
            } else {
				if { $sNameFilter == "*" } {
					set lValues [lsort -dictionary [split [mql list $sAdminType] \n]]
				} else {
					set lValues [lsort -dictionary [split [mql list $sAdminType $sNameFilter] \n]]
				}
            }

            set aAdmin($sAdminType) $lValues
        }
    }


    if { $aExclude(bMode) } {
        if { $bStatus } { puts "Remove Data defined in exclude file/s ..." }
        set lFile [ glob -nocomplain [ file join $aExclude(sDir) $aExclude(sMask) ] ]
        foreach sFile $lFile {
            unset aAdminTemp
            array set aAdminTemp {}
            pProcessFile $lAdmin $sFile $aExclude(sDelimit)
            pMergeArray xor $lAdminName
        }
    }


    foreach i $lAdminName {
        puts "Process admin type \"$i\" ..."
        set aAdmin($i) [ lsort -dictionary $aAdmin($i) ]

        if {$bSuppressAdmReporting} {
            puts "Supress adm admin data ..."
            set aAdmin($i) [pRemoveElement $aAdmin($i)]
        }
    
        if {$bSuppressHidden} {
            puts "Supress Hidden admin data ..."
            foreach sValue $aAdmin($i) {
                set bHidden "FALSE"
                if {$i != "table"} {
                    set bHidden [mql print "$i" $sValue select hidden dump]
                } else {
                    set bHidden [mql print "$i" $sValue system select hidden dump]
                }
                if {$bHidden == "TRUE"} {
                    set nIndex [ lsearch $aAdmin($i) $sValue ]
                    set aAdmin($i) [ lreplace $aAdmin($i) $nIndex $nIndex ]
                }
            }
        }
    } ;# End foreach

    set Category_Order [ list ]
    foreach cat $lAdmin {
        lappend Category_Order [ lindex $cat 0 ]
    }
    # Temp only until the BO are fixed.
    lappend Category_Order "Trigger Manager Objects"
    set Category_Order [ lsort -dictionary $Category_Order ]

    # build a global list of programs for use by the trigger processing routine
    set glsPrograms [split [mql list program *] \n]
#    set glsPrograms $aAdmin(program)
    
    # glsTriggerManagerObjects is set when the trigger manager objects
    Generate_TriggerObjects

    lappend Category_Order "Number Generator Objects"
    set Category_Order [ lsort -dictionary $Category_Order ]	
	
    # glsNumberGeneratorObjects is set when the number generator objects	
    Generate_NumberGeneratorObjects

    lappend Category_Order "Object Generator Objects"
    set Category_Order [ lsort -dictionary $Category_Order ]	
	
    # glsObjectGeneratorObjects is set when the object generator objects	
    Generate_ObjectGeneratorObjects
	
    # Generate Extended data before creating users, will allow for null data.
    Generate_ExtendedPolicy

    foreach category $Category_Order {
        if { $bStatus } { puts "Build Menu $category ..." }
        Generate_Summary_Menu $category
		if { $category == "form" } {
			append Summary_Page "<A HREF=\"[Replace_Space $category]_menu.html\" TARGET=\"Summary\"><IMG SRC=Images/plus.gif BORDER=0 WIDTH=9 HEIGHT=9> webform</A><BR>"
		} else {
			append Summary_Page "<A HREF=\"[Replace_Space $category]_menu.html\" TARGET=\"Summary\"><IMG SRC=Images/plus.gif BORDER=0 WIDTH=9 HEIGHT=9> $category</A><BR>"
		}
    }

    # Generate the business system data.
    foreach lAdminType $lAdmin {
        set sAdminType [ lindex $lAdminType 0 ]
        set sGenerate [ lindex $lAdminType 1 ]
        if { [llength $aAdmin($sAdminType)] == 0 } {
            if { $bStatus } { puts "No Data for $sAdminType ..." }
        } else {
            if { $bStatus } { puts "Start Processing $sAdminType ..." }
            if { $sGenerate == "complex" } {
                set sRet [Generate_$sAdminType]
            } else {
                set sRet [Generate_Simple $sAdminType]
            }
        }
    }

    # Generate general page
    set General_Content "
        <HTML>
        <HEAD>
        <TITLE>Business Model Documentation</TITLE>
			<style type=\"text/css\">
				#customers
				tr:nth-child(odd) {background-color:white;} 
				tr:nth-child(even) {color:#000000;background-color:#EDEDED;} 

				#customers th 
				{
				font-size:1.1em;
				text-align:left;
				padding-top:5px;
				padding-bottom:4px;
				background-color:#C1C1C1;
				color:#ffffff;
				}
				
				#customers td, #customers th 
				{
				border:1px solid #C1C1C1;
				padding:3px 7px 2px 7px;
				}

				#customers
				{
				font-family:\"Times New Roman\", Arial, Helvetica, sans-serif;
				border-collapse:collapse;
				}
			</style>		
        </HEAD>
        <BODY>"

    append General_Content "
        <DIV ALIGN=center><BR>
        <FONT SIZE=\"3\"><B>$sHeaderTitle</FONT></B><BR><BR>
        <FONT SIZE=\"2\">Date-Time Generated<BR>(YYYY MM DD - HH MM SS)<BR>[clock format [clock seconds] -format "%Y %m %d% - %H %M %S"]</FONT><BR><BR>
        <TABLE id=\"customers\" BORDER=1 CELLSPACING=0>
        <TR>
        <TH ALIGN=CENTER><B>Administration Type</B></TH>
        <TH ALIGN=CENTER><B>Quantity</B></TH>
        </TR>"

    foreach type [lsort -dictionary [array names Statistic]] {
        if { $type != "" } {
            append General_Content "<TR class=\"alt\">
                  <TD ALIGN=CENTER>$type</TD>
                  <TD ALIGN=CENTER>$Statistic($type)</TD>
                </TR>"
        }
    }

    append General_Content "
        </TABLE>
        <BR>
        Use menu on the left frame to navigate through administrative objects.
        <BR><BR><BR><hr><FONT SIZE=\"1\">Schema Dumper, Version 2011 Build 2010.08.20<BR><BR>
        [mql version]<BR>
        © Copyright 2010 by ENOVIA Inc.<BR>
        All rights reserved.<BR><BR>
        <A HREF=http://www.3ds.com><IMG SRC=Images/matrixone_logo.gif BORDER=0><BR>
        <A HREF=http://www.3ds.com>www.3ds.com</a>
        </DIV>
        </FONT><BR><BR>
        </BODY>
        </HTML>"

    if {$bDumpSchema} { pfile_write [file join $Out_Directory general.html] $General_Content }


    # Summary page
    append Summary_Page "
        </BODY>
        </HTML>
    "
    if {$bDumpSchema} { pfile_write [file join $Out_Directory summary.html] $Summary_Page }
    
    return 0
}


################################################################################
# Create_Image_File
#   Create_Image_File
#
#   Parameters :
#       path
#       binary_data
#   Return     : none
#
proc Create_Image_File { path binary_data } {

    set Image_File [open $path w+]
    fconfigure $Image_File -translation binary
    foreach data $binary_data {
        if { $data == "00" } {
          set data_f \000
        } elseif { $data == "0A" } {
          set data_f \012
        } else {
          scan $data %x data_i
          set data_f [format %c $data_i]
        }

    puts -nonewline $Image_File $data_f
    }
    close $Image_File

    return
}

################################################################################
# Create_Images
#   Create Images
#
#   Parameters : none
#   Return     : none
#
proc Create_Images {} {
    upvar Image_Directory Image_Directory

    set ematrix_logo {ematrix_logo.gif {89 50 4e 47 0d 0a 1a 0a 00 00 00 0d 49 48 44 52 00 00 00 47 00 00 00 53 08 06 00 00 00 cd f7 1a 92 00 00 00 09 70 48 59 73 00 00 0b 13 00 00 0b 13 01 00 9a 9c 18 00 00 00 20 63 48 52 4d 00 00 7a 25 00 00 80 83 00 00 f9 ff 00 00 80 e9 00 00 75 30 00 00 ea 60 00 00 3a 98 00 00 17 6f 92 5f c5 46 00 00 2a 6c 49 44 41 54 78 da ec bc 79 94 5c d7 7d df f9 b9 cb 7b af d6 de bb 81 6e a0 b1 2f 24 41 10 24 c1 55 5c 44 51 02 29 6a 8d 25 59 8e 2c 4b ce 88 f6 f8 78 e2 c9 71 94 8c 9d f1 4c 32 76 1c c7 76 26 4e 6c 8f 32 1e c7 9e 28 5e a4 c4 92 ac 68 b3 2c 89 12 45 4a 14 c5 9d 20 48 2c 04 40 a0 b1 37 7a ef ae ae 7a f5 96 7b ef fc 71 5f 55 77 8b a2 36 4a 3a 9e 73 52 e7 3c 54 77 17 aa ea bd ef fb ad df df f7 5e e1 9c fb 08 3f b6 87 23 cf 32 d2 34 25 cd 32 16 17 16 98 9d 9d 27 6e 35 49 b2 9c dc 18 f2 2c 43 08 41 bd 56 67 68 68 90 9e 9e 1a d5 6a 85 4a a5 4a 10 46 fc 18 1f 2d e1 9c 73 3f 52 38 9c f5 60 a4 19 49 92 12 b7 33 96 e3 94 46 2b 65 a9 95 d1 6c a7 b4 92 8c 24 cd c9 f2 0c 63 41 38 8b 73 96 40 38 02 e9 88 b4 a0 12 29 fa 7b ca 8c 0c f5 31 32 3c 48 5f ff 00 20 7e a4 a7 ae 7f 54 9f 9c e7 b9 b7 92 2c a7 d1 4a 99 9a 8f b9 34 df e4 d2 cc 02 93 73 8b cc 2f 36 68 b4 62 da 69 4a a3 15 63 8c 25 50 8a 56 92 22 80 30 50 08 21 d0 52 11 85 01 95 28 a0 5e 0a e9 af 84 f4 97 35 23 fd 25 b6 6e 1c 66 fb b6 cd f4 f5 0f fe 28 2e 61 f1 87 6a 39 ce 39 f2 3c c3 59 47 92 59 a6 16 db 9c 9b 5e e2 c2 b4 07 a3 19 b7 89 93 94 24 49 69 a7 09 49 9a 91 64 39 3b 37 0e b3 7e a8 17 63 2c ce 39 0e 9f ba c0 e9 8b 33 68 25 31 c6 61 9d c5 18 83 75 16 a5 34 d5 28 a4 af 52 a6 af 1c 30 52 53 6c db d0 c7 75 7b 77 b1 79 cb b6 1f 26 38 0b 3f 14 70 3a a0 08 07 71 66 38 37 d3 e2 c2 cc 32 f3 8d 98 34 cd c9 f3 94 24 cb 68 b5 13 e2 38 21 6e a7 c4 49 9b 66 9c 50 2d 87 5c bf 7b 9c 38 49 69 a7 19 4a 49 7a 2a 25 be fa d4 51 a6 17 96 91 52 82 73 38 eb 70 80 c3 62 9d 25 cf 2d 42 0a ea e5 32 83 d5 12 43 65 c9 ae 0d 3d dc 76 e3 5e b6 6e df f1 77 03 1c 93 e7 38 6b 48 8c 63 62 aa c9 d9 a9 06 69 66 50 02 8c b1 24 59 4e 3b f5 80 24 69 42 3b c9 68 b7 db b4 92 94 38 49 69 2c 37 d9 32 3a c8 a6 d1 41 9a 71 42 18 68 4e 5d 98 e2 f8 99 49 84 94 b8 0e 30 ae 00 a7 73 ba ce e1 b0 5d cb 2a 87 21 83 d5 0a 43 15 b8 6e c7 30 07 ee ba 95 fe c1 91 57 05 8e 7e 35 99 27 4d 52 d2 cc 71 7e be c9 b1 f3 f3 b4 e2 9c 5a 29 20 54 12 e3 7c bc d4 4a 10 68 8d 09 0c 26 57 28 99 a3 94 40 0a 81 10 02 29 25 47 4e 5f a2 5c 0a 71 c0 62 23 e6 d9 63 67 e9 a9 96 10 42 60 9c 5b 89 bb ce 21 04 5d a0 70 02 29 1c 52 28 d2 2c e3 dc cc 2c 73 a5 88 99 96 e5 e8 e9 4f 73 cf 6d 7b b8 f5 d6 5b 7f e0 c0 2d 7f 30 5c 0c 4b 8d 26 67 2e 2f f3 95 e7 cf f3 f5 17 2e b0 dc 4a 89 42 85 b1 8e dc 3a 24 10 68 d0 5a a1 95 44 2b 85 94 12 21 04 ce f9 af 4d b3 9c bd 3b 36 f0 fa 9b ae 64 76 a9 c9 07 de 7a 3b 87 4f 5f e4 03 6f bb 83 e1 fe 3a b9 b1 08 21 fc b5 75 9e 8b 1b e3 2d c8 03 8c 87 0a ad 14 71 9a 71 6e 66 8e a3 53 09 1f f9 e2 73 fc f9 47 3f 4e 73 69 a1 fb be ef e7 f1 7d bb 95 31 39 93 97 17 39 71 69 89 17 27 fd 97 46 81 46 29 49 a0 34 91 96 28 a9 90 02 94 14 64 26 27 4d 0d ed 24 f5 01 b9 dd a6 d5 6e d3 8a 13 96 9a 2d 7a 6a 11 ff e4 bd 07 78 e8 e9 17 79 e4 e0 49 fe fe 81 1b a9 56 42 7e f3 4f 3f 4b 96 db 15 4b 71 0e 6b 1d ce d9 6e 9c eb 3c 77 7e b6 d6 16 3f fb 7a 2a 0a 02 86 7a ea ec 1e d2 bc ef 5d 07 d8 30 be 05 87 43 7c 6f 96 f4 fd c5 9c 34 4d 78 f1 d4 14 cf 9f 9d e5 d2 52 9b 72 a8 09 b4 42 0a 85 d6 82 50 2b 94 54 04 4a 11 28 89 14 02 63 2d 59 9e 13 27 a9 07 26 6e 13 b7 13 0f 50 3b 61 be d1 a4 af 56 e2 1f bf e7 00 8b cd 98 24 cd f9 ad 0f 7f 0e e1 40 4a 30 9d 78 e3 f0 07 16 1c 38 1c 38 5b fc dd ad 3d 0a 90 8c 31 38 67 18 e9 ef 67 73 af e4 67 de 7a 3b 7b f6 ee 5b 65 45 df 11 a4 85 ef d9 ad 5a ad 16 8f 1f 3c cd d7 0e 9f e3 ec dc 32 4a fa 3b 65 ac c1 3a 83 75 0e e3 2c 59 6e c8 ad c1 5a 7f d2 b2 88 2b 4a 7a d7 d2 5a a1 b5 ff 5d 49 41 ad 14 71 79 b6 c1 9f 7d fe 51 b6 8c 0e f2 ef 3f fa 00 52 40 50 d4 39 9d 03 e9 3d 0b c4 0a 50 ae 73 71 62 55 24 f4 c0 39 d7 b1 10 c9 a5 d9 69 8e cf a4 fc e9 27 1f e6 d9 a7 9f ea 38 e6 77 bd e6 ef 29 20 37 9b 2d 1e 7a e2 38 2f 5c 98 a7 ed a0 14 68 f2 dc 82 02 21 fd a9 99 dc 9f 99 12 8e 5c 0a b4 b4 08 27 50 52 a0 94 f0 c0 28 8d 92 12 29 a4 8f 3f 48 1c 10 86 9a 34 33 34 5b 09 02 87 52 0a eb 1c 76 d5 f9 0b c0 49 c0 14 00 14 00 09 04 ce 19 bc fd 3b 44 61 3d 42 58 9c f0 d6 a5 51 cc ce cf 62 4c 1f 7f f6 99 47 91 52 b0 ef ba fd af 3e 20 b7 e3 98 07 1e 3d cc 93 2f 4d b2 9c 1a 14 90 a6 39 d6 5a ac 35 e4 99 21 cf 0d b9 31 18 93 77 0b 36 63 0c c6 39 ac 0f 9b 28 25 51 4a 10 68 85 52 0a 59 80 a6 a4 40 08 08 b5 a2 5c 0a 51 4a e2 61 93 08 b1 ea f4 9c e8 a6 74 70 08 e1 90 c2 15 c0 ac c4 a2 4e d0 f5 60 09 ac b1 58 e7 d0 2a 60 7e 61 9e 89 b9 84 ff fc e9 47 78 e9 c4 8b df 35 48 7f 47 70 f2 2c e5 8b 8f 3c cf 93 27 2e d1 cc 2d d6 e4 8c 0d d4 a8 96 34 ed 34 25 33 16 d3 39 ac 7f ce 8d c1 58 43 66 0c c6 58 70 0e ad e8 5a 4f a0 35 81 ea b8 95 8f 4b 81 52 2c 34 5a 3c f8 d4 31 ac 73 48 25 ba 0e e2 b3 11 58 dc 8a 1b b9 ee d5 af ae 2c 30 d6 d7 3c 9d d8 93 a4 29 bd f5 32 57 ef 1c 27 cf 33 94 f4 16 34 31 9f f2 e1 bf fe 32 33 53 97 be 63 dc 91 df a9 8e f9 da 13 47 f8 e6 91 f3 34 73 8b 28 2c e2 f4 a5 59 d6 f5 55 29 07 8a 76 3b f5 9d b4 31 58 63 30 d6 62 ad 07 c8 3a 83 c3 e0 9c 45 ca 4e 4d e3 d3 b9 0e fc 33 42 20 a5 07 6d be d1 e2 af 1e 78 12 84 f0 b1 42 b0 f6 ce 3a 07 a2 f8 59 08 9c 13 1e 8c d5 7e 67 8b 8c 66 1d 69 9a 33 d0 53 65 d7 e6 31 0e 9f 3c 0b 02 ac 33 48 29 99 9e 9e e6 d8 e5 98 bf f8 c4 df 92 67 c9 f7 0f ce f3 47 4e f0 95 a7 4e b2 94 e5 80 25 cd 32 72 63 48 b3 8c 17 4e 4d b2 7e a0 4e ad 14 d0 4e 52 b2 dc 90 65 06 6b 3c 80 ce ba 2e 48 59 6e 30 c6 11 28 e5 2d 45 4a a4 d2 54 2a 15 6a b5 3a 51 b9 4a 10 86 44 61 44 4f b5 82 d6 01 2a 08 d1 51 85 20 2a 23 95 c6 15 60 89 6e d6 72 3e 26 15 40 98 e2 fb 7c 06 83 24 4b 19 ea ab b1 7b cb 18 8f 3f 7f 82 34 cb f1 e1 a7 88 47 52 70 79 7a 9a c7 4f cc f2 b9 2f 7c f9 fb 0b c8 b3 b3 33 fc cd d7 0f 73 79 39 46 07 01 79 96 23 a5 c4 e0 70 56 22 84 e5 85 53 17 b9 7a db 7a 66 17 61 b1 99 20 c2 10 29 05 52 28 2c 96 dc 1a c8 1c 12 49 66 2c 5a 09 ca 95 12 a5 4a 99 f6 a5 4b 9c 3f 79 84 e3 47 0e 71 f1 dc 69 16 e6 a6 49 93 18 6b 0d 08 49 10 44 54 7a 06 a8 0e ac a3 77 dd 26 ca fd eb 51 61 09 93 66 38 93 15 15 b2 2b 8c a5 48 e9 d6 bb 61 92 a6 ac 1f ec 63 db c6 11 1e 3b f8 22 ce 5a 94 a0 1b ab c0 22 70 e4 79 ce f4 fc 22 9f f9 da 0b ec bd 72 27 db 77 ec fa ee 45 a0 35 39 ff f5 b3 5f e3 c1 e7 26 30 4a a3 95 44 2a 89 96 0a 07 a8 e2 67 a5 7c d6 d9 b3 75 3d 0b cb 6d 1a 71 4a 25 0a 8a 54 ed eb 1f 9f a1 24 fd bd bd 54 cb 65 8e 1f 79 8e 07 fe e6 13 3c fb c4 a3 2c 2f 37 28 d7 7a e9 1b 18 a6 de d7 4f a9 54 41 6a 85 c9 73 da 71 8b e5 c5 39 96 e6 66 68 35 16 91 61 c4 d0 f8 2e d6 ef be 81 f2 c0 18 26 cf b0 79 5a 58 91 77 5d 67 fd 05 8f 0e f5 b1 79 fd 20 8f 1e 3c e6 53 7f 11 ac 3b b5 8f c3 82 75 60 1d 59 9e 32 38 38 c2 cd 3b 06 f9 df 7e f9 7e 94 0e bf 73 11 78 f0 f9 a3 fc e9 a7 1f 63 be 9d 12 06 1a 84 2c 80 28 62 46 01 8e d6 0a 21 7d 66 d9 b1 71 98 d9 a5 26 b2 f8 7b a0 7d e0 8d c2 88 9e de 1e 2e 9e 3a c6 a7 ff cb 9f 70 e8 d9 a7 e8 1b d9 c8 8e 3d d7 b2 7e c3 66 aa b5 3a 41 a0 7d c0 28 52 31 ab 6a 19 93 67 34 1a 8b 4c 9e 9f 60 e2 e8 73 cc 5f 3e 4f df d8 56 36 5d f7 06 4a 03 eb c9 db 2d 70 be 4b 77 d6 5b c4 ae 4d a3 1c 3e 79 a6 08 43 05 30 ce 42 11 0f 3b 96 b6 d2 e9 3b c6 46 46 f8 87 ef 78 0d af 7b dd 5d af 0c 4e ab b9 cc 7f f8 c8 17 79 ea e4 45 64 71 e7 85 94 1e 84 6e 31 e7 53 b1 92 12 a5 3c 10 52 4a 4a 61 d0 2d f2 c2 40 53 af d7 50 52 f0 a5 8f 7f 98 af 7e e1 bf 31 bc 69 17 7b 6f bc 93 fe a1 75 08 2c 26 f7 d5 eb 2b 74 35 dd 40 ec 6f 88 c6 39 98 9d ba c0 91 a7 be ce e5 b3 27 d8 78 f5 6b 18 dd 7b a7 4f d5 79 5a 80 60 c8 b2 dc bb 77 b7 52 ee 1c de 5a ac b3 88 e2 7b 9d 73 a4 69 4a bd b7 9f 6b b7 0c f3 9b ff e4 7e 2a b5 fa b7 07 e7 e1 47 9f e4 c3 9f 7b 92 c5 38 26 0c 02 9f 61 94 ee 76 cf b2 00 44 2a 81 12 1d 2b d1 48 a5 d0 52 52 0a 35 52 28 7a fb 7b 68 ce cf f1 f1 ff f8 db 4c 4d cf b0 ff f5 6f 63 dd d8 66 5c 9e 63 8d f1 c1 55 48 84 28 e2 80 10 fe 2e af aa 69 56 ee 7c 51 f0 09 81 92 12 a4 e6 c2 c4 71 9e fb da e7 88 aa bd 6c bf e3 1d a8 a8 82 49 db 50 04 65 67 57 2c a5 e3 46 ae 63 2d ce 14 cf ae fb 7f 8d b1 8c ad 1f e3 17 de 7e 33 f7 dd 7b e0 e5 ed 43 6b 79 89 47 9e 3d c9 fc 52 03 9b 67 64 59 8a c9 73 b2 34 21 cf 32 f2 2c c3 16 d4 67 9e fa df b3 34 23 49 12 92 24 21 cd 32 da ed 14 19 04 9c 3b 75 9c ff f8 db 1f 24 b6 92 bb de f9 73 f4 0f 8d 11 c7 2d d2 3c c3 76 7b 20 bb aa 0d f0 d5 70 71 fe dd f2 bf 08 0d 3e 33 19 43 9a e5 b4 db 31 23 e3 db b8 ed 27 ee 47 6b cd d1 bf fd 4f 64 8d 59 84 d2 5d f7 c2 14 cf 85 b5 38 6b 8a df 3d 30 d6 16 af 5b e3 41 34 19 4b cb cb 3c f8 c4 11 92 b8 f5 f2 54 fe dc d1 93 1c 3b 3b 4d d2 5e c6 9a 8c 2c 4d c9 b2 b4 0b 8c c9 73 f2 3c f3 47 e6 c9 f0 34 f7 53 84 2c 4d 49 93 04 ab 14 17 cf be c4 5f fd 87 df a4 be 7e 0b 37 dc fd f6 2e c1 de 49 bf ab 1b 44 5f 65 3b 9c 15 38 2b 8a d8 23 c0 fa 98 63 ed ea ff 4f f7 fd 49 3b 21 08 22 f6 1f 78 17 f5 e1 0d 1c 7b e0 2f c9 96 17 90 52 e3 0a 3a d5 a7 7a 7f f1 c2 59 c0 ac 02 a5 88 3f c6 3f 4b 24 4b 8b 73 9c 9a 5a e6 99 83 87 d6 82 63 f3 94 c7 9f 3f cd ec c2 02 b9 c9 c8 b3 14 93 27 64 69 9b 2c 4b 56 8e 34 25 4b 33 8c c9 3d 58 a9 b7 b0 24 4d c9 ad 65 61 76 9a bf fd 8b 3f a0 32 b0 9e bd 37 df 4d 9e 26 58 63 0b fe a5 53 e9 76 02 6e 61 d6 8e 6e 5c b0 dd a0 59 80 b6 aa 12 b6 05 45 da 49 e1 26 cf c0 59 f6 dd f1 26 c2 9e 01 5e 7a e4 af b1 79 ea 9b bd 55 f1 a6 f3 79 ce 3a ef 76 9d 38 64 4d b7 36 72 38 b2 2c 61 b1 d1 e2 1b cf 1e f5 26 dc 01 e7 dc 85 8b 1c 9b 98 a4 d9 98 c7 e4 19 69 da 26 cf 92 02 a4 14 93 25 98 02 a0 3c 4b bc 45 e5 29 59 9e 91 65 1e ac 34 37 3c fe f9 8f 90 e6 39 57 dd f8 3a 4c ee fb ac 4e 24 e9 b8 4a dc 6e b3 1c 27 58 b7 c2 ab bc 8c 72 e8 d6 24 fe 5f d3 a5 28 56 dc ce 73 4b 06 70 ec bd ed 8d b4 e3 16 17 0f 7e 05 94 2a 5a 08 6f 35 dd de ab 03 46 27 ad 17 ae e7 5d ce 20 85 a4 b1 34 cf 8b 67 e7 98 9c bc b4 02 ce a1 63 a7 99 9c 59 20 89 9b 98 2c c3 64 19 79 9a 92 a7 09 79 d2 5e b1 9a 24 c1 64 fe ef 59 9a 16 71 27 45 48 c5 c4 f3 df e4 e2 e9 63 6c dd 7b 33 61 18 61 4c be a6 ef 71 ce 91 e5 86 3b 6e d8 c3 1b 6e bd 06 29 05 4b cd 98 57 a6 93 bc 6b e5 5d 7a a1 b0 19 b7 42 ea 23 3c 87 5d ae d4 d9 7e ed ed 5c 3e 7d 84 e6 85 e3 48 21 a1 e0 72 9c 33 38 93 e3 ac f5 d4 8a 2d ea a2 4e 9a 2f 88 7b 09 c4 cd 06 d3 0b 4d 0e 1d 3e ee 2b e4 3c 4b 38 7a 6a 92 85 85 79 ef 2a 22 45 28 51 64 13 5f d3 08 a5 b0 d2 20 a5 c0 39 8d b2 0a 91 6b 84 54 04 61 48 63 61 96 97 9e 79 98 be d1 2d f4 0d ae c3 98 ac 30 ef 4e 09 e3 68 c5 29 3f f1 86 5b 78 eb 5d 37 f1 d2 b9 4b dc 73 c7 7e be f2 e8 b3 7c e5 9b cf 91 1b 43 a5 54 5a 65 2b ab 72 97 73 08 1c d6 ad 76 c5 e2 b5 82 d3 30 79 ca d0 e8 26 2e 0c 6f e0 e2 91 c7 d8 f6 9a 75 85 05 7a 6b c1 59 0f af e9 b4 18 6e 0d 83 e8 83 35 b8 dc b0 dc 6a 71 e4 d4 79 ee c1 a1 a7 a7 a6 99 98 9c 63 79 69 16 ac c1 e0 10 79 d1 14 2a 85 93 0a 69 24 a2 a8 8a ad 35 e4 42 21 95 ef 72 75 a0 39 7f e4 29 b2 34 63 dd c8 46 94 52 05 c9 b5 42 3d 75 ea 96 5b f6 ed e6 2f 3e f3 20 9f fb da 53 5c b7 7b 2b ef 7e d3 9d bc f6 a6 6b f8 d4 97 1f e5 b1 e7 5e 44 29 45 14 85 45 9c 71 5d 0b b2 6e 2d 1d ba 9a d6 ea c4 17 a5 02 86 36 ee e0 ec f3 8f b2 74 e1 38 f5 b1 dd 98 dc bb 0d 0e 9c 58 e9 d6 29 5a 0d 51 f4 67 be f1 72 48 09 cd c6 22 e7 2e 2f b1 b4 b8 80 3e 7b 71 8a e9 f9 05 92 56 83 20 d0 58 2b 91 52 01 02 65 14 4e 49 9c 90 20 25 4e 29 ff 9a f0 85 9f d3 9a a6 99 63 6a e2 30 b5 e1 51 c2 28 2a 08 f4 e2 24 dc ca 05 08 e1 b3 4d 9a e5 f4 56 ab 4c 5c 9c e6 b7 fe e8 af d8 bf 67 3b ef 7a e3 1d 1c b8 ed 7a 3e fe 85 af 73 f8 c4 59 4a 51 48 10 68 56 4d 61 d6 02 e3 d6 f2 30 4e 78 4e a8 52 ad 51 1d 58 c7 dc b9 63 d4 46 b6 40 91 99 9c b5 45 d7 6e bb e7 b4 9a 97 16 9d 1b 29 04 ad 66 83 d9 a5 98 8b 97 26 d1 67 2e 4e b1 b4 d8 20 4b 62 24 25 5f 6c 09 01 42 e1 a4 44 18 5f 1d 8b 02 1c 51 00 63 a5 42 88 32 4b 93 e7 c8 92 36 fd 3d 83 08 29 5f 46 5b 16 74 5d f7 e2 84 14 58 67 89 74 48 18 68 0e 1e 3b cd b3 47 4f 73 d7 cd d7 f0 81 77 de cb 85 cb b3 7c ec 0b 5f e7 cc c5 69 aa a5 08 a5 e4 1a 60 56 ea a0 55 b6 55 8c 6a 84 54 d4 07 47 b9 7c f2 20 c9 e2 65 74 6d 08 8a 18 e3 29 e7 a2 7d e8 b8 a6 c3 07 64 21 0a eb 11 64 59 46 a3 15 73 71 72 06 3d 39 b3 c8 f2 d2 02 36 cb c8 a5 44 48 81 13 9e 89 b3 9d d6 01 81 50 12 63 7c 1c 52 d2 b7 15 08 58 9c 3c 83 ae d4 09 a3 92 b7 0e 6b 91 d2 ad ba c1 9e b5 73 ab af 86 95 c9 41 14 06 00 7c e5 9b 07 f9 c6 33 87 79 d3 6b 6f e0 57 ee 7f 27 cf 1d 3b cd 27 bf f4 08 33 0b cb 54 2b 25 a4 10 6b a6 0e 7c 4b ec b0 45 6f 55 aa d6 50 61 99 c6 cc 79 fa ab fd 58 9b af 22 e8 bd e5 58 67 bb 0d 8a 40 60 8c 29 26 3f 02 6b 72 e2 38 e6 f2 cc 02 72 66 61 99 56 b3 81 b1 9e e6 34 45 06 ca b3 d4 a7 f3 34 f1 55 72 9a 60 d2 36 26 6d 93 a5 31 26 4b 48 96 17 89 1b 73 94 6a 7d 48 ad c1 d1 6d 0f 3a 3e bd c2 59 8a 2e 58 14 c1 75 f5 a3 5a f6 2e f9 89 2f 7c 83 7f f1 07 7f 81 75 8e 7f f9 8f de c7 bb ee b9 0d ac a5 19 b7 7d 70 2d 68 87 15 b0 57 b2 96 73 8e 40 87 44 b5 3e 5a 8b 33 d8 ac 8d 35 06 67 73 9c c9 a1 28 48 1b cb 4d b2 3c a7 56 29 f1 3b bf f6 4b 84 5a f9 59 bc f5 05 64 1c c7 cc 37 5a c8 66 9c d2 8e 1b be 92 4d 12 96 96 96 c8 d3 84 a5 c6 92 2f f0 e2 96 2f af 17 17 49 da 45 0a 2f c0 8a 97 e7 31 26 27 2a 55 3d ef 2b 24 79 96 76 ef 54 87 ce f4 19 c2 74 ef 76 87 ca b4 b6 73 d8 ee 85 d7 2a 25 e2 76 c2 9f fc d5 e7 f9 ed 3f fe 18 9b 37 ac e3 5f fc 4f ef e5 35 fb 76 93 66 29 69 9a ac c4 b3 ce f7 00 79 96 14 53 0a 41 a9 da 43 da 6e 61 d3 18 67 33 ac c9 b1 36 27 69 c7 6c 1b 1f e5 bd ef b8 8f b4 dd a6 56 29 f1 a9 2f 3c 48 b3 b9 8c 2c 58 4b 61 1d 59 d2 66 7e a9 89 6e b5 33 d2 76 4c 9a b4 a9 96 2b fc de ef fc 73 86 07 07 f9 ec 17 1f e4 a1 47 1e e3 8f 7f ff b7 48 93 94 e9 d9 79 7e f5 37 7e 97 3c cd 7c 97 6c 2c 49 b3 01 80 0a 43 3f 3a 51 02 6b 32 ac b1 48 ed 63 85 70 62 6d da ec 50 06 b8 97 93 db ae 18 b9 49 41 ad 5a 66 6a 6e 9e 7f fb ff 7e 8c 2b b7 6f e2 de db 6e e0 ea 9d 5b f9 ca 63 cf 30 71 61 d2 bb 76 97 83 32 98 3c f5 cd b1 90 04 51 09 6b 1d 79 d2 42 04 15 ac cd 01 81 c9 52 ca a5 80 8d eb 06 69 2c 37 78 df bb de cc 93 07 8f 10 c7 6d b4 ae 74 cf cb e4 39 ad 38 41 26 59 86 c9 53 92 24 61 cb f8 28 12 f8 bd 3f f8 10 ff f0 e7 de c7 c6 f5 23 8c 8e 0c f3 c1 5f fd 3f b8 f1 ba bd bc f6 d6 1b 58 5a 5c c0 a4 6d f2 bc 4d 9e b6 8b 00 ad 10 88 ee b4 20 4b db 6b 32 96 58 93 86 dd 0a 01 f5 b2 63 ed df 95 92 d4 2a 25 4e 9e bd c0 ff f5 91 4f f1 c8 d3 2f b0 ef ca dd ec bb 62 d7 9a ae 3b 4d e2 82 5a f6 35 b7 d2 81 2f 3a 93 18 6b 3d 13 60 6d 8e 00 e6 e6 e7 39 76 fc 14 03 bd 3d 6c db bc 91 27 9f 39 44 14 06 58 e3 ba 15 74 9e 27 e4 c6 21 8d 35 58 93 11 05 8a 67 9f 3f c2 af fd c6 ef f2 8b 3f ff 3f f0 cd c7 9f 64 7e 61 81 34 cb 38 f6 d2 69 4e 4d 9c 65 64 68 90 2c 4d 31 b9 af a2 6d 9e 21 94 ec 4e 08 7c f0 15 38 63 c8 b2 b4 c8 52 9e bb c5 ad 8c 4c ec b7 69 17 5e e9 b0 ce 15 c4 99 e6 c8 4b 13 7c e6 cb 0f 53 ad d5 e8 eb eb f1 d3 d4 34 c1 1a b3 72 0e 05 1d 02 02 9b 27 b8 2c f3 15 72 9e d3 6a 2d 73 cd 95 bb f8 1f 7f f6 a7 18 1b 19 62 76 6e c1 0f 27 8d ff 3f d6 78 c5 88 cd bd a5 69 07 5d 62 3c 28 8a bc 6f 3c f6 24 f7 1d b8 9b de 5a a5 e8 9a 8d 0f a7 d6 fa 9e c9 98 82 b7 cd 5f ee 19 45 dd 61 f2 9c 5c a6 e8 20 04 27 0b 72 7b 6d 96 f9 9e 07 fa 40 92 78 b7 b9 6a c7 56 94 14 34 9b 2d 4c 9e 92 17 17 d2 89 cf 6e 15 25 a4 6c 86 76 71 c1 f3 40 68 63 c8 5a 58 93 73 fe d4 51 7e e5 9f fd ef de 3a 95 02 b2 62 bc 93 53 72 65 94 14 48 25 24 0e 4b 73 b9 c1 9d 37 5f cf ef ff ee 6f f0 97 ff f5 e3 6c db b6 85 fe de 1e 84 10 8c 8f 0e b3 7d eb 16 2e 5d ba 84 10 d6 9b 6a 67 e4 db a9 23 56 a5 ee ce f8 c4 98 9c 3c 4d 8a 2a 75 55 cc b1 0e e9 40 3a 5f a3 c8 ee c1 9a 43 39 c8 92 94 56 2b 66 ef ae 6d dc ff ae 37 b3 69 6c 1d cf 1e 7a 81 a5 c6 12 ce ad b2 98 d5 37 c8 1a b0 0e 15 84 28 1d 22 74 80 54 da 13 77 9d 58 25 25 51 14 a2 0b 52 4f 74 15 1b 9e e0 0b b4 46 07 5a 23 10 04 81 e6 b9 17 8e 52 ab 56 f9 cb 3f f9 10 9f fc f4 e7 38 74 f8 08 ed 38 e6 3f fd d1 ef f3 f4 c1 43 7c e9 c1 87 a9 94 4a 6b ee bc 2d ac 69 c5 75 56 c5 9a 82 9a c8 f2 a4 b8 c3 ae db 84 ae 09 d2 6b 4b 5f 84 10 9e d8 4a 52 f6 ec dc cc 3b ee 7d 2d d6 58 3e f3 e0 23 1c 3e 7e 1a 29 1c 4a 0a ac 15 45 df e4 56 a6 e4 ce 15 8a 54 50 51 05 94 5e a1 5d a5 67 2d a3 28 f4 20 29 dd 6d 60 e9 68 7e 10 e8 20 a4 54 0a d1 a5 48 23 55 80 94 8a cb b3 b3 bc fb fd 3f cf e8 fa 11 26 ce 5e 40 07 9a 7b df fe 53 44 51 c4 dc e2 22 61 e0 79 e2 2e 19 a4 03 88 0d 26 4d 70 74 66 49 16 25 57 08 2a 21 1c 08 89 29 8a b1 ce e0 cf 14 71 a2 d3 6e 74 40 c9 8d a1 d9 8a d9 b4 61 1d 7f ff 2d af 67 f3 d8 08 9f 7e e0 11 be fa d8 33 58 67 09 b5 06 e1 8a 51 0c dd 1b e1 3a c4 99 73 a4 ed 26 52 08 74 54 29 5a 0b 09 d6 51 ae 56 79 e6 d0 51 7e e7 f7 ff c8 bb bb 90 05 2e de 54 05 02 27 72 74 50 a2 b7 56 41 d7 2a 25 c2 72 95 e6 d2 0c 41 10 91 3b c7 99 f3 93 84 51 88 90 82 76 9e 93 e4 86 28 2a f9 ea d9 47 3c cf 2b 07 11 02 41 d2 6e 51 5d 35 fe 58 7d 17 3b 83 4a 51 d0 0d 0e 8b b1 19 d6 aa 55 e2 23 7f 82 cb ad 98 81 be 1e 7e e6 3d 6f 65 ff d5 bb f8 ea 63 cf f0 a1 ff fc d7 34 db 31 d5 4a 04 4e 15 56 2b d6 54 c7 6e d5 04 c2 1a 43 7b 79 91 a8 5c 41 97 aa e4 79 06 c2 e2 a4 1f 2c ce cc 2f 72 f1 f2 34 e5 72 a5 4b c4 21 56 cd c1 80 72 b5 ce 40 7f 0f 7a b0 b7 46 b9 5c 67 ce 5a ef 77 d2 13 d9 42 38 04 0a ad fc b4 51 0a b9 46 12 22 a5 04 a5 51 61 89 a4 b1 80 19 19 c3 a9 82 71 eb a4 63 64 c1 c1 bc 5c f0 d1 ed d5 9d a3 d9 6a 11 86 21 ef 7e f3 eb b8 e7 f6 fd 3c 7b f8 04 ff eb bf f9 63 a6 e6 16 a8 55 cb d4 aa a5 e2 f3 56 d5 43 df 0a 4e 61 91 59 9a d2 5e 5e 64 64 74 1c 19 44 48 eb b0 c2 16 04 b5 44 21 29 eb d0 bb a2 f2 e2 04 c1 aa 6c 8a a0 5a ef 61 b0 b7 8e 1e 1d ea a3 d6 37 e0 ef 86 04 d1 11 c2 74 c1 90 ab 00 11 08 a1 fc eb 52 21 b5 26 a8 f5 d2 9a bd 44 da 6a 11 ea 00 5b b4 21 4a 48 4f 13 20 10 6a 15 b3 67 57 ca fe 56 d2 06 07 77 bf 66 3f ef 7c e3 9d 5c 9c 9a e1 5f 7d e8 cf 39 75 6e 92 4a 39 a2 a7 56 f1 77 d5 39 b0 fe 6b bb 24 55 b7 d2 b6 45 1d e3 2d 27 5e 5e c0 66 6d ea c3 1b 11 52 23 54 80 74 a6 4b 8f 8a 55 fa 15 ef 05 45 cc 12 45 36 95 01 83 03 03 0c 0d d4 d1 9b c6 06 e9 eb ef 47 29 8d c0 37 9e 62 35 30 52 76 89 2f 8a e1 bf 10 12 a1 14 52 29 c2 fa 20 f1 fc 65 9a 8b d3 94 6b 35 a4 31 be 59 95 16 a4 c5 ba c2 9d ac 43 4a 81 cd 73 e2 b8 4d 66 72 6e d8 7b 05 ef 7d fb 01 ac b5 fc 3f 1f f9 34 cf 1e 3d 41 a9 14 d1 53 2b 77 b9 bf 4e 37 5d 20 e3 e7 f0 8e c2 8d 0c 14 f4 a7 35 86 dc e4 2c cd 5c a2 d6 d3 4f 79 60 14 9b e7 08 15 82 cb 41 18 30 16 a1 0a cb 33 b6 f0 0e b1 42 80 e1 08 4b 15 86 87 06 19 19 ec 43 6f 1e 1b 61 dd f0 30 51 b5 8e 33 59 61 19 d2 1f 52 14 e0 14 d3 cd a2 3c 47 fa 74 27 a5 46 86 65 ca fd a3 b4 e6 2e 90 0c ae ef ce b7 8c ca c1 f8 ee de 09 2f 41 9b 9a 99 e5 ce 9b f7 71 79 76 81 f7 bc fd 00 c3 03 bd 7c e2 f3 0f f1 f0 e3 cf a1 b4 a2 a7 5e c5 ab da 0a 4b b3 ac 75 25 bb d2 81 77 46 2b 79 ee 27 23 c6 18 5a 8d 45 e2 b9 29 b6 5d 7f 07 2a 2a 63 5c 51 39 5b 81 90 0a 4b 8e 28 32 9c a0 98 63 61 8a f9 98 c3 64 19 03 83 c3 8c 0d 0f 30 30 d0 87 5c bf 6e 84 2d 63 c3 d4 fb 47 30 d6 a7 3b 51 50 12 be 2e d0 48 ed ad 44 48 85 d0 01 2a 88 90 3a 42 04 11 52 05 54 d7 6f 47 08 c9 e2 e5 f3 5e 9f 93 1b 4c d6 a9 38 fd 73 14 68 3e fc f1 cf b3 7e 78 90 5f fe c0 4f 72 e8 c8 09 3e f8 9b 1f e2 a1 c7 0f 52 2d 47 94 02 0d a6 88 0d 76 05 18 27 5c 57 8b ec 85 51 b6 db 65 1b e3 0b 52 63 bc 10 7c f6 c2 29 6a 7d fd 0c 6c b9 ca 67 53 e5 5d 5f e8 00 a4 f2 d7 a6 02 50 01 74 6a 9e e2 59 2a 85 31 96 d1 d1 8d 6c 18 e9 a3 5c a9 a1 83 a8 cc 55 db c6 18 1c 1d 67 6e fa bc 07 a1 23 4b 93 7e 3e ee 47 c2 05 38 52 23 0a ab 11 4a 23 b5 46 95 aa f4 6e ba 9a d9 93 4f 51 e9 1d 40 f5 0f 21 cc 4a 26 52 2a 40 2b cd e4 f4 2c bf fc 2f ff 10 29 05 69 9e 53 2d 97 09 82 d2 b7 f0 34 ac 49 ef 6b 2c a6 33 b5 34 06 63 32 4c 96 93 17 56 b3 30 75 91 f6 fc 14 3b ee 7e 07 41 a9 46 1a 37 51 5a 23 ac f4 b2 98 a2 6c 16 ce 78 8b 16 60 65 67 4e 26 bd fa 42 05 6c d9 b4 89 f1 d1 81 15 09 ca be 2b 36 33 be 61 13 67 8e 3f bf e2 3e b2 e3 5a 1d c4 0b 94 8b 67 29 15 42 05 08 ed 0b aa 9e cd 7b 68 cf 5f 66 f6 cc 8b 04 51 85 52 a5 d2 1d bd 74 b4 c2 4a e9 e2 77 49 39 8a 56 75 e8 85 fb ac 26 b4 d6 f4 57 76 d5 a4 d2 f7 82 26 37 7e f1 89 c9 59 5e 9c 63 7a e2 28 9b ae bc 8e 81 ad 7b 49 e3 26 52 07 be c6 c9 33 64 27 e6 29 e1 63 20 c6 97 11 ce 78 d6 40 f8 e5 02 bd 03 43 ec d8 34 c6 c6 0d eb 56 46 33 db 36 8f b3 67 c7 38 7d 43 63 9e 46 54 ba 38 8a 8b d7 fe 59 ea b0 28 c9 03 64 50 42 05 11 2a 08 11 2a 40 eb 90 d1 eb de 80 0a 4b 4c 9d 3e 4c da 6e 93 15 13 52 3f e7 ca b0 26 f3 b4 a5 35 6b e5 72 dd 41 9e ed 6a 0d 3b d3 48 ff b3 77 1d 67 3c 83 90 67 39 79 96 92 99 9c d6 f2 12 17 8f 3f 47 ff f0 7a 76 dc fe 36 9f 55 b5 5e 69 17 f4 8a 85 77 c2 03 4a e1 b5 78 3e 6c 08 a9 c9 32 c3 ae 1d 3b d9 ba 61 90 fe fe fe 15 70 82 a8 cc ad d7 6c 63 6c d3 36 9c 90 48 15 22 54 88 0c 8a be 24 08 91 41 e0 fd 57 85 48 1d 15 20 45 28 1d 11 04 11 5a 2b aa 3d 83 6c bf e3 5d b8 3c e7 e2 c9 43 24 71 ab 18 1f 77 c6 c8 a9 8f 13 b9 17 21 79 ee 27 f7 72 5d 63 3c 29 d5 89 23 b6 13 53 8a a3 f8 9c 2c f3 60 67 79 4e ab b1 c0 f9 a3 4f 53 2d 97 d9 ff d6 fb 09 2b 75 df 36 28 8d 0a 02 af 12 53 9d be ca 9f bf 14 aa 10 91 fb 91 93 2b 26 2c ba 54 e1 da 2b 77 b3 65 e3 70 77 4a de 9d 95 df 78 cd 2e f6 ec dc 46 ad 6f 08 27 24 42 fb a6 4d e9 c0 7f 99 0e bc e5 a8 00 a5 3d 70 3a 0c 09 c2 90 20 08 3d 40 d2 31 3a be 95 db de f9 8b 04 52 70 f6 c8 93 34 97 e6 fd 05 15 d4 eb 0a 05 9b 62 f3 6c cd 61 56 1d 36 4f b1 79 87 1e f1 d3 d6 b4 98 dd 67 59 c6 e2 ec 24 13 87 1e a3 5e ab 71 e7 7b 3f 48 7d 68 0c e1 4c 01 48 61 39 ba a8 73 64 50 a8 45 94 0f c6 b2 88 95 85 9c 26 cb 72 76 6d df ce ee cd eb d8 b8 71 ec e5 b2 b7 de be 01 ee be 71 37 cf bd f8 12 a7 4e 1e 21 d2 3e be 74 46 31 42 e9 02 18 7f 07 b4 0e d0 2a 40 16 27 13 85 01 95 72 89 6a a0 59 b7 fb 6a 76 fd f2 6f f1 f9 8f fc 21 27 0e 7e 93 75 5b af a0 6f 64 03 36 8a 50 d2 20 85 17 41 75 02 3f 05 b9 dd e9 0f bd 34 64 25 de 58 e3 3b 7f 63 2c 69 bb c5 cc 85 09 2e 9f 79 91 3d d7 dc c0 eb df f3 3f 93 07 65 16 16 e6 09 74 80 b5 c5 45 15 7d ae 56 5e e8 66 8c ff 5c 49 47 9b e3 7b 3b 6b 0d 42 29 6e df bf 8f ad 1b 07 88 a2 52 17 1c f5 eb bf fe eb bf de f9 65 74 b0 87 17 cf 4e 73 ee d2 94 6f 20 55 80 d0 01 3a 08 91 aa 10 32 06 be cd d7 da 5b 8e 90 8a fe 7a 8d 4a a9 44 b5 5c 66 a0 b7 4e 4f b5 c4 a6 4d 9b b8 fb be bf 47 a0 04 cf 3f fe 30 0b d3 97 7c 5d a4 82 42 0d ea ba 31 c5 14 45 9c 97 eb e6 d8 dc cb 75 3b 99 c8 e4 19 ed b8 c5 fc e5 f3 9c 3f 76 10 13 2f f1 93 ef ff 45 7e f2 17 fe 19 3a 2a 91 c4 b1 97 e8 0a af 11 4a d2 1c 25 c5 4a c7 2d 56 b2 a0 6f 0d 8b fa 0d 68 27 09 57 ef da cd bd af d9 c7 de ab b6 a3 83 a0 2b c1 5e 05 8e 23 2a 95 89 a4 e1 85 89 59 e6 17 e7 d1 61 84 d2 21 3a d0 2b 2a cf 20 28 dc 28 c4 22 19 5f 3f c8 de ed 9b 69 b4 12 fa ea 35 fa 7a 6a f4 f7 f5 50 af 94 e8 a9 57 79 cd 5d 07 d8 79 f5 7e 26 2f 9c e5 fc 89 e7 69 cc 4e 92 26 ed 42 75 ba 22 15 f1 20 f9 c0 9b e5 3e e0 26 71 93 e6 c2 2c b3 97 ce 30 35 71 94 78 71 96 7d 37 de c6 3f fd 8d df e3 c0 db de 45 9a 24 98 82 0a 71 16 32 63 d8 b5 69 1d d6 3a 16 1a 31 5a cb 2e 5d d1 4d e5 a2 03 92 28 94 ee 92 f7 bc f9 00 7b 77 ac 67 78 64 cd fa ac b6 5e cb b7 c1 ed 37 ec e5 f1 c3 67 98 5b 98 27 8e 9b 68 1d 20 95 f2 01 4e 69 94 d6 84 3a c4 22 d8 3a 36 cc ae cd a3 1c 3f 33 45 7f 4f 9d 9e 7a 85 9e 7a 95 9e 5a 85 4a 25 42 4a c1 e2 c2 3c eb c7 b7 f1 ae 9f fb 15 9e 78 ec 11 0e 3f f5 35 e6 2e 9c a2 39 73 11 a4 40 15 59 cf c7 04 e9 dd 27 cf c8 8b 31 10 58 ca d5 3e 76 5e 7b 1b 7b 6e 7e 1d fb 6f b8 85 e1 d1 11 1a 8b 4b 94 c2 80 5a b5 52 14 86 5e a0 72 fc ec 34 bb 37 8d 00 8e f3 97 67 d1 3a e8 aa d9 73 21 bc bb 09 81 14 86 e5 66 8b 7b 6f bf 85 ab b6 0c 31 be 69 c3 77 97 da ea 30 e2 a7 de 78 33 c7 26 26 79 fe c5 13 5e 1b a8 35 4a 69 82 20 44 6b 8d b5 b0 75 e3 3a 76 8c af e7 c5 89 cb f4 d6 ab f4 d6 2b f4 d4 aa d4 aa 15 ca e5 88 20 08 10 08 d2 34 63 76 66 96 99 99 19 ca 7d 23 6c da 7f 80 9e ed 73 24 4b 33 b4 17 a6 49 1a b3 64 71 d3 eb fa ac 17 20 84 a5 0a b5 81 75 44 f5 01 aa 83 eb e9 5f bf 99 e1 75 a3 d4 aa 15 2e 4f 4d 13 2a c1 e8 f0 20 81 d6 54 cb 51 57 28 9e 5b 47 6e 2c 47 cf 4c 72 c5 e6 11 84 90 9c bd 34 43 10 68 df 79 8b a2 c2 31 82 38 cb d9 b6 69 9c fb 6e bd 86 4d 1b 87 09 82 f0 7b d3 21 6f 19 df c8 7b ee bd 99 99 85 06 d3 f3 73 68 1d a2 b4 f6 01 cf c1 f6 f1 51 b6 6e 18 e1 d8 c4 24 7d 3d 35 6a 95 8a 17 5d 57 2b f4 d4 ca 94 c2 10 25 a5 5f 19 dc 8c 69 2c 37 69 34 5b 2c 2d 37 68 c5 cb 08 ad 29 0f 6e a0 34 b8 d1 c7 1e 93 77 f9 14 29 28 5a 97 a0 2b cc 36 d6 d1 58 5a 40 39 47 20 05 4b cb 4d 6a d5 0a 83 bd 75 42 ad a9 44 11 59 d9 90 e7 5e 18 2e 04 1c 9d 98 62 e7 c6 21 a4 10 4c 5c 9a f6 b1 24 f7 65 69 66 1d 3a 08 78 df 5b ee 66 7c 5d 9d a1 a1 e1 ef 6f d5 cc 81 db af e3 d4 85 19 fe db 57 9f 22 31 39 a1 0e 10 52 b1 7b d3 28 1b d7 0d 70 7c e2 32 fd 3d 35 6a d5 0a b5 4a 85 5a b9 4c b9 54 22 0c 43 a2 50 91 1b 47 2b 4e 58 5e 6e b2 b4 dc a4 d9 6a d2 4e 53 bf 7e aa 18 a9 ac 99 65 75 db 06 df 18 9a 3c e9 3a 7b 2e 05 79 9e 93 e4 7e 5d e8 52 b3 45 7d b9 49 bd 52 42 eb 80 20 d0 54 4a 51 b1 ac a0 a0 33 8c e5 f8 b9 69 76 6e 18 42 6b c1 c4 c5 19 82 c0 7f 47 a3 19 f3 fe b7 dc cd de 6d 23 8c 8d 0e 7f ff 4b 8a 84 54 bc ff ed 77 70 71 7a 91 27 8e bc 84 0e 03 c2 20 24 d0 9a 93 e7 a6 e9 eb ad 52 2d 95 a9 94 cb 54 2b 25 aa d5 12 51 14 a2 a4 c2 39 41 9a 65 24 6d 6f 35 cb ad 26 ad 42 54 69 8c ed 2a 70 3a 02 03 b1 c2 7c e1 84 1f 04 b2 8a 38 b7 d6 92 e7 b9 ff cc 2c a3 d9 8a 59 5a 6e 51 a9 54 e8 af 6b a2 40 77 97 14 74 75 86 c5 a2 92 97 2e cd 32 50 2f 53 0a 03 8c 95 2c 35 63 de fa da 9b 78 fd f5 3b 18 1c a8 13 84 d1 2b 2e 4b fb 8e ab 66 2a 95 2a bf f4 d3 07 b8 72 cb 06 02 a5 a8 55 ca 2c 2e fb c9 68 a5 54 a2 5a ab 50 2d 97 a8 96 4b 94 a2 88 52 14 10 68 49 9a 59 5a ad 84 e5 56 cc 72 ab 45 ab 15 13 b7 53 b2 cc 74 fb a6 46 d3 2f b4 cf 32 43 b3 ed 47 b9 ae 28 75 96 5b 6d 96 9b 2d 96 96 9b a4 59 e6 35 41 79 4e 96 7a fd 61 9c 24 34 9a 2d 9a cd 98 34 cb 50 42 10 05 c5 e2 fc 52 89 5a a5 44 ad 5c a2 5c 8a a8 94 4a cc 2e b5 08 82 90 56 3b e3 ae fd 7b f8 a9 37 dc 40 7f 5f 95 5a a1 39 16 3f d0 7a 2b e7 18 19 1a e0 83 ef 7f 23 1b 06 7b 71 d6 d2 d7 5b f7 14 43 a9 44 39 8a a8 55 cb 44 91 a7 1c 42 2d 0b ad 5e 46 ab bd 12 6b 5a ed c2 6a ec 0a 77 f2 4b ef 79 33 bd b5 0a 3b 36 8f f2 fe b7 bd 8e 66 3b c1 59 88 93 8c 9f 79 cb 5d fc bb ff e5 7e fe f5 3f 7a 1f 1b 47 06 69 27 19 79 91 e2 b3 2c 23 4d 33 9a ed 84 a5 66 93 e5 56 4c 9a 1b 90 92 30 0c a8 94 02 ca a5 90 52 a9 44 b5 73 8e e5 32 49 66 b8 7d df 15 fc 83 37 dd 4a ad ac e8 e9 e9 7d 75 8b d1 5c d1 4d 8f 8f ad e3 57 3e f0 16 fa aa 25 da 71 4c 6f 4f 9d 6a a5 4c 14 fa ec 15 45 9a 30 f0 a9 38 37 86 56 bb 4d bb 1d d3 8a 63 e2 76 9b 38 49 0a 0d b2 a7 24 db 69 c6 bd b7 5d cb 8e 4d a3 dc b2 77 17 37 ec d9 4e 9a 66 20 1c b9 31 bc fd 75 37 f1 c4 f3 c7 a9 96 4b fc dc 3b ef a1 d9 6a 75 5d 2b 2b 5c ab d5 f6 96 d9 68 c5 b4 da 09 02 41 14 68 c2 c0 bb 7f 39 0a 7d c5 5e 29 61 1d dc 75 fd 15 bc ff be 1b a9 96 14 7d 7d fd af 7e e9 b4 58 25 b5 df 34 b6 8e 5f fb f9 b7 51 8d 14 17 27 a7 a8 57 2b 54 2b 25 c2 20 40 0a 8d c5 97 e2 49 9a d1 8e 13 bf d0 35 6e d3 4e da 7e 39 52 6e 0a 71 a2 a0 9d a4 3c 75 f8 25 f6 6c 1f 67 74 b8 9f c7 0f 1d f7 6d 44 c1 5d b4 da 6d 7a 6b 65 a2 50 73 7c e2 82 67 16 ad 29 b4 d0 39 69 9e 93 66 29 cd 56 9b 66 2b 26 4e 12 b2 3c f7 e3 18 a5 08 43 4d a5 14 12 85 01 52 28 ee bb 75 0f ef 7e dd 35 54 4b 9a 9e de de ef 79 9d f9 f7 b1 ae dc 31 3a 3c c0 3f ff 85 9f 60 6c b0 cc 33 87 8e a2 a4 a4 5e 2d a3 b5 c4 5a 47 ab 9d 17 ab 7f db 34 9b 2d 9a 71 8b b8 ed 77 40 f1 2b 75 3d d3 af 95 e4 e0 b1 d3 5c b5 6d 23 a3 43 fd 1c 3d 75 1e 29 20 cf 0d 42 82 35 8e ab 77 6c 66 c7 f8 28 73 8b 0d 3f 32 29 56 01 a6 85 f5 64 59 46 3b 4d 69 c6 31 cd 56 9b 76 92 62 8c 41 6b 45 29 0c 50 52 51 ad 44 bc fd 8e ab 78 ed 35 9b 29 45 01 d5 95 75 0d 3f 4c 70 56 1a 94 9e 5a 85 5f bd ff ed dc b5 7f 3b 0f 3d fa 04 e7 2e 5e a6 5a 2e 21 25 24 49 c6 72 2b 66 b1 d1 a4 19 fb 20 dc 4e fd 62 35 d3 d1 1d 5b 88 b4 e6 f9 13 13 ec dd b9 85 91 81 5e 9e 3f 79 86 2b b7 6d 64 d7 96 51 96 9b 31 e5 52 c8 9f 7f fa 41 1e 78 f4 20 b7 5f 7f 15 59 9e 77 95 59 d6 14 c1 39 cf 69 67 29 cb ad 36 8d 56 4c b3 9d 90 f9 ee 92 dc c0 86 e1 5e de 7c d3 76 76 8e f5 51 2e 97 88 4a a5 97 4b f2 7e 18 ab 83 bf 45 a9 08 08 de fd c6 d7 70 f5 8e 71 fe ec 33 5f e7 d4 d9 0b 5c 77 f5 2e 10 7e 15 4a ab ed ef a6 b7 9a 9c cc e6 dd d9 95 40 a0 b5 e2 dc a5 19 9a ed 84 c9 99 05 ea 95 12 3f fb b6 bb 91 52 30 3b d7 60 66 b1 81 0e 03 96 e3 36 03 a6 5e 34 89 5e e8 64 8c 27 d5 d3 2c 43 a5 19 71 92 d0 8a 63 96 5b 11 42 48 46 07 fb d8 b7 6d 90 b1 81 a8 50 a7 96 7e e0 0d 29 5e f5 46 1f 59 9e f3 99 07 9f e2 c1 27 8f 52 ad d4 e8 ad 97 58 58 6a 30 33 b7 c0 dc c2 22 4b cd 26 71 92 78 49 2b 7e 09 a1 bf bb 86 fe 9e 2a b9 31 cc 2f 35 b9 e7 d6 6b 41 08 1e f8 e6 b3 0c f6 d6 89 db a9 97 9e 44 01 f3 8b cb 7e 66 a6 14 81 0e 08 c3 88 4a a5 42 b5 52 25 8a 42 2a a5 32 db c7 47 b9 75 ef 36 f6 6c 1e a4 1c 4a df 30 eb 57 b5 3d d0 c2 0f 6d ff 9c e9 d9 79 3e fb d0 d3 3c 79 f8 34 ad b6 df 1f 67 61 69 89 46 ab 45 5e 98 bb f8 16 65 72 96 fb 52 df af fd f4 4a 07 e3 fc 82 0d d9 d9 01 c5 81 56 12 8b 43 4a 45 10 ea 2e bb 17 45 65 b6 6d 1c e3 8e 6b 77 73 eb 9e 4d 0c f7 95 09 82 90 30 fa a1 6c 5f f5 c3 00 67 6d 7d 79 69 72 8a af 3e 71 98 27 5e 38 c5 d9 a9 79 66 17 97 68 c6 5e 4b d3 9d 9c 8a 55 15 b0 a3 4b 1f 78 a1 b4 5c f3 b9 1d 92 dd 58 43 6e 2c 3a 0c 18 1e 18 e4 ca ad 9b b9 79 cf 0e ae d9 31 ca 48 7f 95 6a a5 4c b9 52 59 f5 fe bf 23 9b 0b 7d db 4f 5e 98 e7 e0 91 97 78 e6 c8 04 c7 cf cf 70 79 be 41 a3 d5 22 c9 d2 82 78 f7 1a 9f 15 d1 a3 4f f3 ab 4f a7 b3 f8 ad 52 2a d3 57 af b1 61 78 88 ed 1b d7 b3 73 7c 84 4d 23 3d 0c 0f d4 e9 eb eb a1 5a ab 7f 5b 9d ce df 59 70 56 b6 4d c9 b8 30 39 c5 c9 89 f3 9c 38 73 89 73 97 17 98 5d 8a 59 6e fb 9d 96 72 bb 4a cb 5d cc e3 a3 20 20 0a fc f6 53 fd 3d 15 06 eb 65 46 fa 6b ac 1f ec 61 64 a8 97 e1 a1 7e fa fa fa bc 8c e4 47 f7 f8 31 80 f3 ad db 3f b4 96 99 5f 58 64 66 6e 81 f9 85 25 16 1b 4d da 49 4a 6e 3a 35 90 22 0c 7c 0b d0 db 53 a5 a7 5e a5 bf af 97 9e 9e 1e ca 95 ea 8f f3 54 7f fc e0 fc ff e8 b1 20 ff 3b 06 3f ec 6d a9 fe 3b 38 df 4b 0a 77 af e2 f5 1f a4 32 7f a5 2d 5d be dd eb ee 55 9f 87 fe 36 14 0e b9 cd 3d 1b 27 41 38 9f 41 bc ba 9b 42 64 20 ba dd 7a 3b cd 48 73 af 0e 2f 47 61 b1 38 43 77 5b 85 dc 18 94 94 18 6b 89 93 14 25 15 95 52 d8 3d f1 38 c9 c8 8d 4f 59 95 52 80 92 92 24 cd 49 32 2f 5b 89 c2 a0 2b e1 f5 19 cd eb 0a 5b ed 14 67 bd aa b4 52 8e 8a b9 d5 ca 79 75 b6 b0 ea 48 68 b3 3c 67 a1 d1 22 0c 34 3d d5 32 00 71 92 62 0a b9 5f a5 1c 15 0b e8 c4 2b 83 33 39 b7 c0 1f 7e ec cb 28 29 88 93 94 ad 63 c3 bc fb f5 37 f1 07 1f 7b c0 33 79 38 76 6f 5a cf 4f df 73 0b d6 3a 3e f1 d5 a7 18 19 e8 c1 18 cb 2d 57 6f e3 e2 f4 02 b9 b1 ec bf 62 0b 33 0b 0d 9e 79 f1 0c 07 6e de c3 5f 7f f5 29 2a a5 08 6b 1d 61 a8 b9 e7 a6 3d 4c cd 2d f1 c5 c7 5f 60 dd 40 2f d6 59 6e bb 66 27 a1 d6 fc 97 07 1e 63 6c a8 8f 56 3b 63 dd 60 0f b7 5f b3 93 b3 53 b3 9c 99 9c e5 8e 7d bb 78 f4 d0 09 ce 4c ce d2 5b ab 52 2f 47 dc 79 dd 6e 0e 9f 3a cf a1 97 ce f1 d3 f7 dc 82 40 70 e2 dc 24 ad 24 e1 da 9d 9b 39 37 35 cb 37 0e 9d a4 5e 29 13 28 c9 eb 6f bc 92 13 67 2f f3 c4 d1 09 86 fb 6a 68 ad b8 f3 da dd 44 81 fe ce 96 33 50 af f2 81 b7 dc c1 d3 c7 26 f8 dc 37 9e e3 ef dd 79 3d ce 39 8e 9f b9 cc 4f dc 75 1d bb 37 8f f2 7b 1f fd 22 42 08 de 71 d7 7e 46 fa 7b 78 d3 ad d7 74 df df 53 ad f0 89 07 9f 64 e3 48 3f 8f 1e 3a c9 2d 7b b7 77 f4 25 bc f5 f6 6b 01 f8 dc 37 0e 32 71 c9 6f af b9 7d e3 08 77 ec 5b d9 81 64 6a 7e 91 be 5a 99 fb 8a cf fc e8 97 be c9 f2 8e 71 72 63 69 34 bd 52 ab d1 6a 73 e7 75 bb d9 38 3c b0 52 4e 19 4b 9a e6 7c e1 9b cf 73 df ad d7 14 84 58 0a c0 d7 0f 1e e7 9e 9b ae 66 a8 6f 85 b2 58 6a b5 d9 bf 7b 13 7b b6 6d 7c c5 3d bc 5e 16 73 a2 30 20 0a 35 9f 7c e8 69 76 8e af 63 ff 15 5b 8b dd 4f 04 1b 86 fa d9 bf 7b 0b d7 ed 1a e7 f0 a9 0b 64 b9 61 7a 7e 89 a7 8f 4d 70 e8 e4 39 b2 dc bb c2 bd b7 5c cd 9f 7d fe 1b 6c df b8 8e d1 c1 3e 6f ba 85 c6 d8 58 af 85 09 8a 9d e1 ce 5c 9a e1 e9 63 13 1c 3e 7d a1 5b 15 5b eb 48 73 d3 bd 38 ad bd ea bc d3 1a 48 29 39 f8 e2 59 9e 3a 7a 9a f3 53 f3 7e 49 91 f5 96 37 d2 5f e7 81 27 0f 77 77 61 c9 f2 9c 50 eb 35 c0 00 28 a9 38 32 71 89 a7 8e 4d f0 d2 85 cb 85 96 c8 7d f7 80 5c 29 85 fc 83 b7 dc ce ec d2 32 ff e7 47 ff d6 93 50 c2 37 85 e0 65 b7 9d ad 30 95 92 94 a3 a0 a0 49 bd bf e6 b9 2d c6 2c b6 7b 31 0b 8d 26 9f fb c6 73 7c e6 eb cf 32 d8 57 63 7c dd 20 59 6e d0 4a 51 8e 82 ae 49 6b a5 b9 30 33 cf 67 bf fe 0c ff f7 27 1f 64 df 8e 4d c5 e4 c0 ad 61 97 a2 30 a0 1c 85 68 25 bb 7f 6b a7 29 fb af d8 4a 5f ad cc 03 4f 1c 41 2b 55 b4 6e e2 db f1 11 7e 9f b0 28 20 50 fa db 72 3d 2f 73 ab 76 9a 21 10 bc f1 e6 bd 1c 3e 75 81 53 17 a6 c8 32 53 d0 13 86 c9 d9 45 0e bd 74 9e 1b ae d8 42 18 68 fa ea 15 ae da ba 32 4a 8d 93 94 87 9f 3d c6 fd 6f bd 83 2f 3d 7e 98 d1 a1 3e 86 fb ea f4 d5 2b dc 77 eb 5e 74 b1 83 0a 40 6e fc 7e 37 ab df 9f 66 19 9b d6 0d f2 8e bb 6e e0 e8 c4 45 2e 4c cf b3 67 db 86 35 f3 ad dc 58 ae d9 31 ce 86 e1 fe b5 6b 3f 0b fc 6e bc 72 1b 97 e7 96 88 93 84 50 6b e2 c4 13 f2 d5 d2 4a b7 9e e7 86 ed 1b 46 d8 b3 75 c3 2b 25 2b a5 fd 72 91 55 01 79 76 91 7f fb d1 2f e0 9c 3f d1 9f 7d d3 6d f4 d5 2b 0c f6 54 f9 d4 c3 cf f0 c9 87 9e 66 eb d8 10 ef 39 70 33 3d d5 32 69 66 f8 ec 23 07 01 d8 b7 73 9c 33 93 33 5c b3 63 9c 81 9e 1a 77 5e b7 9b 47 0f 9d e4 2d b7 ef 23 d4 9a a0 d8 82 b3 f3 58 37 d0 cb 33 2f 9e e5 6f 1e 3d 84 73 70 f3 55 5b 09 b5 f6 12 7e e0 ca 2d 63 1c 9d b8 44 9c a4 04 c5 d6 33 00 db 37 8e f0 b5 83 2f d2 5b ab 10 06 9a d7 5d 7f 45 b1 8b dc ca 67 ef 1a 5f cf 62 11 a3 6e bc 6a 2b 9f 7a f8 19 fa eb 55 b4 92 bc f6 ba dd 6c 5a 3f c8 43 4f 1f e3 dc d4 1c 42 08 6e bb 66 07 f5 4a 79 8d 9d fc 7f 03 00 1b 55 9c 17 85 88 2a c1 00 00 00 00 49 45 4e 44 ae 42 60 82  }}
    set plus {plus.gif {47 49 46 38 39 61 09 00 09 00 F7 00 00 00 00 00 84 84 84 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 2C 00 00 00 00 09 00 09 00 00 08 26 00 03 08 1C 48 50 80 C1 83 02 04 1E 04 70 50 A1 41 86 06 15 02 98 38 31 61 80 85 0D 2F 3E CC 88 30 23 41 82 01 01 00 3B}}
    set moins {moins.gif {47 49 46 38 39 61 09 00 09 00 F7 00 00 00 00 00 84 84 84 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 2C 00 00 00 00 09 00 09 00 00 08 22 00 03 08 1C 48 50 80 C1 83 02 04 22 3C A8 70 61 C2 00 02 00 48 94 F8 D0 61 45 87 0D 17 12 DC 18 20 20 00 3B}}
    set matrix_type {matrix_type.gif {47 49 46 38 39 61 10 00 10 00 F7 00 00 00 00 00 7B 7B 7B BD BD BD FF FF 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 2C 00 00 00 00 10 00 10 00 00 08 67 00 09 08 1C 48 B0 A0 40 00 08 0D 1A 04 30 80 C0 00 84 10 21 12 04 E0 B0 62 C3 8B 14 07 32 B4 C8 31 E3 41 8B 01 42 8A 14 49 71 63 C3 00 0D 2B 3A 14 50 12 A4 42 96 04 4C 12 08 A0 32 E5 C3 98 2A 69 0E 0C 40 11 66 44 00 3A 1B 32 04 00 B3 20 4A 8D 0E 13 12 D4 79 50 A8 C7 9D 02 06 B0 94 A8 30 26 D5 AA 58 09 06 04 00 3B}}
    set matrixone_logo {matrixone_logo.gif {89 50 4e 47 0d 0a 1a 0a 00 00 00 0d 49 48 44 52 00 00 01 02 00 00 00 2b 08 02 00 00 00 1c f9 11 4d 00 00 00 01 73 52 47 42 00 ae ce 1c e9 00 00 00 04 67 41 4d 41 00 00 b1 8f 0b fc 61 05 00 00 00 09 70 48 59 73 00 00 0e c3 00 00 0e c3 01 c7 6f a8 64 00 00 1d 97 49 44 41 54 78 5e ed 9c e7 73 5c d7 75 c0 f5 a7 64 26 5f 9c 99 24 93 49 26 fe 60 4f 3c 1e 27 96 93 68 62 8f ec b1 6c 89 9d 00 01 50 a2 48 4a a4 58 45 12 bd 77 90 28 bb 58 f4 ba 8b de 7b 59 b4 45 5b f4 de 81 45 ef 8d 00 c1 a2 fc de 5e f0 71 b1 00 49 b0 8d 04 cf be 79 82 de bb ef de 73 ef 3d f7 f4 73 96 1f fd 70 88 6b 67 e7 f1 f2 ea da e4 f4 ec fc c2 c2 84 61 72 6a 7a 5a d7 a4 d7 b7 77 eb 3b fb 1b da ba 3b fb 47 f8 7a 08 30 96 2e 16 0c fc 44 31 f0 d1 6b d7 05 f1 2b e2 d3 f2 2a 9b 6a 5b fb b4 fa de d2 a6 ee 0a 7d 5f db e0 e4 f0 f4 f2 d4 d2 c3 e9 e5 ed ba ae f1 60 75 71 7a 71 cd 6b 41 59 3a 58 30 f0 d3 c4 c0 eb d9 e0 f1 e3 c7 fa 8e ee bc 52 6d 2e 77 51 45 bc 26 3b 24 2e d5 31 30 dc f6 b6 fb 1f 6c 6f 58 df f6 56 e7 69 f3 6a 3b 5d a2 f3 73 2b 75 3f cd 4d 5a 56 65 c1 c0 ab 31 f0 7a 36 78 c5 f8 a9 a9 a9 eb 1e 41 bf f8 b3 6d 7c 76 85 2a 47 e7 1e 9e 6c 41 b7 05 03 47 11 03 ef c4 06 6c 78 65 75 f5 df fe 70 46 95 5a 94 58 d2 12 9a 94 73 14 51 60 59 b3 05 03 ef ca 06 85 5a dd a9 5b de d5 ad fd aa ac 6a 5d 4b 97 05 a1 16 0c 1c 45 0c bc 13 1b e0 36 d8 da fb 97 34 74 75 8c cc 47 67 94 f0 7a 14 51 60 59 b3 05 03 ef c4 06 ae 61 09 69 e5 cd 8b eb 8f 32 4a eb 47 c6 0d 16 6c 5a 30 70 44 31 f0 f6 6c f0 20 21 33 a1 a8 ee f1 d3 1f 1a 3a 87 3b 7a 07 8f e8 fe 2d cb b6 60 00 0c bc 25 1b 3c 88 4f 4f af 68 66 7c 73 f7 70 57 df b0 05 95 16 0c 1c 69 0c bc 31 1b 6c 6e 6e 7a 29 13 ca 9a 7b 9f fe f0 43 43 5b df d0 98 c5 16 3a 7a 04 f0 ec d9 b3 a5 b5 8d a7 4f 39 c3 03 ae c7 4f 9e ae ac 3f 3c fc ae 9e 1c 04 67 73 eb 11 f7 e1 81 bc 45 cf 67 cf 7e 78 ca 7f ef e3 7a 33 36 e8 ed ed 75 0c 54 35 f7 4f 8d cd ad 6b 1b db 49 30 cb 6b 58 58 5c 1c 37 4c 0e 0c 8f 76 f7 0d e9 f4 1d 05 65 35 05 e5 35 b5 4d 6d c3 63 86 47 8f 0e 40 c7 e4 fc 72 db e0 44 db c0 b8 7c b7 0f 4e 8c 4d 2f 70 42 66 fb 9a 5d 5a 6d ee 1d 29 6f ee ae 6e eb eb 1f 9f 31 3d bc 95 f5 cd fe 89 d9 d6 81 b1 5d 20 46 80 5b 8f 76 04 84 89 d9 c5 8e 61 c3 0b f8 43 86 91 a9 17 0b a6 c3 f4 c2 8a be 6f ac 42 df 53 dd d6 3f 30 31 f3 e4 c9 0b b2 18 9d 9e ef 1c 99 5a db dc 32 5d cc 90 61 b6 eb 79 e3 e2 ea 06 cf 86 b9 25 b9 03 8b 69 1f 92 a6 eb 18 9a e8 94 6e e3 d4 83 d2 06 b7 9f 2f e9 d5 47 06 84 ec 9a d6 de b1 a9 cd ed 47 79 b5 ad d9 d5 2d 19 95 cd e9 95 cd 93 c6 59 f4 bd a3 39 35 ad ab 1b 7b 08 74 7c 66 31 bb ba b5 b5 7f ac a4 b1 53 f4 cf d0 ea e7 96 5f 53 db f2 70 fb 51 44 76 a5 19 99 42 52 3b 8f 9f 30 11 08 4f ab 68 3a 24 75 6d 6e 6d 27 16 d5 6d 3c dc 36 eb 5f d3 de af eb 1c 32 6d 7c f4 f8 f1 fe c3 3d e4 2c a6 dd e4 75 72 46 ea d2 fa b7 80 b0 7f c8 1b b0 41 76 7e d1 2d af e0 ea 2e 43 46 59 43 7d 6b b7 4c 8e 73 f3 0b e3 86 a9 a5 e5 15 01 7d 6a 61 a3 b6 6d 28 21 b7 da 39 54 6d 75 db ef d7 c7 2e fe e6 c4 65 4d 5e a9 d9 dc 79 b5 6d e7 7d e2 ce b9 a9 4e 38 84 1e b7 0f 91 6e 87 b0 53 2e 91 ce 51 d9 33 8b bb a0 40 ee 83 d4 92 93 4e aa 93 4e e1 5f dc 0b 39 e1 14 7e da 2d ca 31 22 63 71 75 1d 68 d0 d9 05 df f8 13 8e 8a 93 0e 61 02 c8 69 e7 f0 6f 03 e2 d7 1f 6e 23 9f 94 19 e5 a7 5c 54 c7 18 65 2f 7d e2 ef 59 b7 a8 b4 f2 46 b1 8c f5 87 5b f7 53 80 1c 7e c2 49 29 41 76 54 9e 71 8b 06 f2 f2 da 26 5f 61 24 1b 37 d5 19 b7 a8 fe f1 69 79 d9 9c f7 59 17 e5 29 97 08 98 81 46 c8 e8 7c 50 3a b3 c8 1d 92 8a eb ac dc 63 8e 33 9d 11 20 db 61 de 53 ce e1 97 fd e2 36 1e ee 61 a7 97 9d dc 80 61 c6 2e 28 2d 3a b7 0a 0c 9c 76 8d 3a ee a8 b0 72 51 59 b9 45 36 74 4b f4 14 97 5f 73 21 38 bb aa a5 97 67 f8 59 60 29 32 5b 6b e3 a7 49 2e d1 1d bb 1b 7c dc 51 69 e5 1a 4e ff 9e 91 c9 57 13 07 6c 10 96 5e b6 f1 70 8f 78 9a 5e 5c 41 1c 30 70 7a 61 35 ad fc b0 6c 40 ff d5 cd ad fd 0a 41 db d2 db dc 33 62 ba 0c 4d 69 fd a3 9d f7 10 4b 44 80 56 b5 f6 01 19 a6 35 93 53 6f cd 12 87 62 03 28 3e 3a 39 fd 9a 6f a4 77 6c ae 3a 5f 3b 3a f1 02 cb 28 84 c5 a5 65 31 7d 89 ae ed 8a eb fd 6f 5c 82 ae ba dd bf 1b 18 79 cd 33 c4 29 24 ce fe 7e cc f7 01 91 85 55 e6 5c 0b 46 90 61 dc 0b 2b eb 48 56 6e 54 81 7b 4c 8e 5d 60 9a 77 7c 9e 00 18 a4 2e e2 d5 37 21 1f 51 8d 12 e7 ec 1d 54 e9 e7 03 d3 52 8d d4 ac cc 2c e7 d9 ca 55 35 3c 39 0b e1 a2 c7 97 d6 36 21 56 3e 21 56 8f dd 0d b1 f6 4e bc af 29 7e b8 bd 03 3b 31 7c 69 75 43 96 f7 fe 49 05 62 a2 41 c3 2c 9f 90 2b 0e e1 e9 76 81 a9 e9 46 29 38 3c 39 07 87 58 bb 84 9b ca cb 81 89 d9 93 ce 2a 1b 77 95 38 4b 87 f0 34 1b 5f 75 59 d3 8b 54 09 47 c2 02 80 06 85 d9 f8 a9 99 9a 4d d1 b2 5f 52 be ec b4 86 26 67 cf 7a c6 21 5c 91 c7 a7 5d a3 d9 20 3d 91 a0 4f 9f 4a 1a b2 6d 60 cc da 27 29 a9 a8 0e 1e 86 e1 af 3f 48 7a f6 c3 33 96 71 c6 59 a9 ef 1b 3d ee a0 08 4d 2f 13 90 65 a1 db 3e 34 01 f1 15 d4 b5 6f ef 48 92 b8 aa ad 0f d4 a1 57 59 92 2a ab 02 32 82 7c 4b 1b 3b 51 3e 18 42 d1 79 55 8e 91 59 88 a7 b9 a5 55 30 9f a9 6d 46 6a a0 51 01 88 20 cf af 6b 03 94 78 e5 62 64 53 cf 08 c2 98 35 f7 19 85 c5 d6 f6 0e 13 a5 94 37 d4 76 0c 00 b6 be 6b 30 38 ad 14 20 85 ba 76 5e bb 47 26 af 06 25 45 e5 55 83 70 01 61 66 51 d2 39 dc dd c3 12 39 75 0c 4e ac 1b 75 2f 0a 7f 6a 61 99 87 ba 8e 01 66 64 d7 3c a3 f4 32 ab f4 2c 7e 6c 66 01 fc 38 a8 32 8a ea 3b 40 f8 d0 e4 dc 73 50 8d 59 55 7a 8e 98 d7 d1 a9 f9 4c ad d4 19 b4 f0 8a fe cc ad 69 4d 29 6b e8 1d 7b 21 d4 cc 8e e0 50 6c b0 b5 b5 e5 a1 4c 48 2c ac ed 1b 1e 37 1b af ef 94 84 13 57 9e b6 e1 f7 67 2e e9 f5 fa ed 6d 73 fd 78 e0 a9 c3 00 d6 9e 71 d7 1f ec a9 bf c0 f8 81 76 2f fa c6 32 04 cd 8e a4 bf e4 17 67 3a bc 40 d7 8e 0c 8e ce ad a6 11 e3 e1 4b ef 58 2b cf f8 7b ca f4 a6 5e 69 c3 f2 05 de 91 a9 67 3d 62 cf ba 45 c4 17 d6 2e ae 48 da 43 be 38 4b 14 cb 65 ff 78 d3 46 8c 10 5b 7f 0d 12 97 c6 8a e6 9e 73 be 49 4e 11 19 a6 1d b0 3a 20 6e f7 98 6c 1a 37 b6 1e 59 bb 86 23 f2 47 a6 a4 63 30 bb 5c a2 b2 ce f9 26 97 35 75 1f b8 f1 57 34 9a b2 01 3a d0 3d 36 a7 a5 7f 0c fb 0a 72 65 d4 f2 da c6 49 47 85 57 5c 2e f6 86 95 57 c2 17 77 83 b1 be ec 3c 22 bf 0f 4b c1 36 43 f9 dc 0c d1 70 fc 50 03 c2 5e cc 82 c6 40 a1 61 29 41 07 d8 54 70 26 02 a5 6f 6c 1a 46 8a cc d1 c2 18 d0 19 9c 0f 9b 81 31 70 1b 96 51 d1 3e 38 0e e6 1f a4 94 f0 00 21 22 83 a0 ef e4 52 1d f8 81 22 fd 93 0a 11 31 40 86 2e 5d a3 b2 38 2f 48 16 b6 87 a3 a2 72 ab 98 08 0b d0 23 26 1b 0c f3 29 26 bf a6 73 d8 80 92 cc a8 6c c2 fe 74 8f cd ab 6c ed 13 06 1b 14 ef 93 90 87 50 af ef 1c f4 88 c9 c1 ce 51 64 96 4f cc 2e f0 49 5d da 00 f9 32 1c 29 c0 52 03 d5 45 60 80 95 c3 75 5d c3 06 24 4b 71 63 67 58 66 25 cf 93 f3 4b c9 25 f5 48 40 df c4 fc 9a f6 01 8c 5b ba b1 12 14 75 79 53 37 2b a1 9d fd 72 a6 6c 0d 0d 89 70 79 19 f2 5f c3 06 64 c4 20 eb 03 8d 7b 01 b1 56 df 21 1e d4 85 da a0 e0 88 c3 1f 3c dc 89 f4 65 93 7b 69 b1 0d fa bb a3 48 a1 11 ad 0a 31 79 c4 e6 20 36 d8 30 f6 31 7f c1 1d 7c 52 d4 b0 3b e9 d4 fc f2 7d 4d 09 e4 7e ce 27 09 09 c1 ce 4d a1 81 e5 cb 01 09 b6 01 a9 67 5d 54 f1 08 d1 e7 86 3e e2 10 82 f6 94 20 af 3c 87 bc ec 19 97 8b ac 15 b4 1b 91 a3 b5 0d 48 89 2f dc 53 33 cb a9 d8 fa a7 24 14 d6 d2 a1 77 74 0a fb ed bc 47 a4 4c 70 f2 bc 98 64 68 0c 6c 2d 68 fa f0 d8 10 3d 4d d9 00 2a 3f 66 1f 86 9d 03 bf cd 1b 49 07 a7 e9 66 b0 1a ba 47 25 5e f0 89 45 59 81 1c a8 3f b6 a0 06 8d 7a d2 41 61 ed 16 f1 6d 60 d2 cd e0 e4 85 95 5d df 00 41 88 bb 52 d9 d2 07 d1 23 65 bd e2 73 e1 1c 40 19 69 45 0b f2 b1 39 e5 45 8e cf 2c 14 d6 4b 88 05 e1 19 95 7a d1 ce 40 84 ab 91 f7 06 40 af 7d 78 9a 10 e7 68 e6 b8 02 09 3f db 3b 3b 90 1d 63 43 d3 77 ed 5e 75 49 3d ea b4 a9 67 b8 d9 28 9b 16 56 d7 c3 8c a6 63 44 76 85 ac 5d 21 74 81 49 2e 45 46 b9 24 b6 f2 aa 38 0b 5e d3 2b 9b f0 12 f9 9a 5a de 84 8a 08 49 2f 87 87 d3 2a 1a d1 3c 42 1c 0c 4f cd 43 d6 c6 75 ae c0 f3 43 86 39 21 bc b8 c2 b3 2a 58 1e 8d 8f 76 24 ff 90 ed 20 32 20 03 78 4c 70 ef cb 2e 73 36 58 5d 5d 35 4c 4d e3 ef 1a a6 a6 16 17 17 d7 d6 d6 e0 81 f5 8d 8d b9 f9 f9 31 7e 6a 30 3d 3b 33 3b 67 ea e8 14 55 d5 63 af 00 7d 65 73 fb 96 4f e8 2b 66 32 fb 04 66 6d 7c 93 73 aa 5b 4c db c1 32 a4 96 58 5c 47 23 78 39 83 a9 7d 2f e4 b4 93 e2 94 63 d8 69 c7 b0 33 4e 8a 63 f7 42 39 78 59 b1 8a b1 1c 2d 94 71 c6 2d e6 94 73 44 69 e3 9e 82 0e c8 34 43 db 7c de 2b 06 e6 b9 76 3f 09 8b 45 1c 2d 9d 81 0c 40 01 99 29 8e d9 87 9e 70 50 08 c8 77 15 a9 d6 5e 09 ba 8e 3d c9 10 86 d3 c8 e9 0a fc c2 a2 6e d1 92 66 30 bb 10 3f 2c c3 ce 23 e2 d5 71 92 b5 cd 87 82 22 4d 2f 53 36 00 08 96 1b 62 15 e2 90 6d 39 08 0e 8f e5 b4 5b 74 6a 79 43 40 72 21 ac 88 0b a1 eb 1c 84 ee 4f bb 63 44 55 98 42 43 70 22 0e 8b 1a ba d2 2a 9a 83 8d e4 8e d9 09 4c ac 05 d8 c0 51 95 9e 58 ac 7b 90 52 dc 33 ba 6b e2 b2 f2 dc da 36 ba 61 26 a6 94 ed 3a 51 9c c2 e0 c4 0c d2 87 4f d8 4b c5 0d 9d 62 5f b0 01 fa 96 07 30 1c 95 ab c5 5f 52 65 57 8a d9 31 5a f8 0a a2 ea 8c 08 44 5d 88 4f 8a 8c b2 e5 e7 01 a8 9a b6 fe ec 2a 89 d3 08 1e 08 36 00 88 f0 76 50 29 e8 c0 a4 62 1d 92 0b 4b ac a0 be 53 b4 13 00 80 66 d0 7b 00 27 54 60 5c e7 2a f4 d3 33 3a 85 48 15 53 a3 f9 d1 12 b0 81 20 7a 34 98 56 df c3 03 d3 21 32 cc a4 a4 29 ae f6 b0 41 55 bd 3e a3 98 6d cf ce af 3e 34 cc 2f 43 13 68 a5 b1 99 25 5e 65 d7 06 fd 30 3a 6e 18 1e dd 95 bb d8 4b 0a f5 6e 45 5d 53 cf 50 72 4e f1 7e ca d8 df 82 2d 7a d1 37 86 23 04 be f8 ba b1 b5 8d 9f 87 50 b7 f3 8c 46 b6 d1 02 2d e2 b6 62 02 a2 ce b0 2c b9 71 e0 20 56 9c d7 fd 8e 11 87 81 f9 84 55 c3 e9 ee 9f 0e 7b 06 8a 39 e7 93 0c 7e f9 7a 27 2c 05 06 c3 66 95 21 23 33 8e db 87 d9 ba 47 08 e9 7e 4f 91 0a 95 23 90 84 e7 87 25 ad 29 6d 10 d6 9a 08 43 61 06 a0 64 b0 28 0e 9c 8b 5d 98 19 54 fb bb c1 90 d5 46 3f cf f4 1a 34 cc d8 f8 6b 62 f3 ab 67 96 56 99 0e ba 31 eb 80 86 a4 9d c5 8f 4c cf c3 f0 38 c7 b8 d1 d8 1b 20 04 df c0 c6 3d 92 b1 50 8f 30 d5 20 6b 21 ec b1 a6 42 d3 4a b1 ef 05 34 ef f8 5c 0c 9e 70 23 cf 20 b6 61 27 e1 7b 60 3e 61 90 f0 80 0f 8a b1 21 3a 43 a3 a8 dc d0 b4 32 ac 20 d3 c5 30 50 95 25 11 37 ce 18 d8 80 ec fc 93 0b 60 33 61 8e 8e 4e 2f e0 d6 0b 87 1b 36 16 da 20 38 b5 78 ce 28 31 b9 20 65 18 f2 f1 e3 27 38 2d 21 69 a5 08 56 04 b9 70 03 60 4e 0c 66 88 de 54 a2 89 73 a9 6d ef 47 84 8d cf 2e c2 24 bc c2 60 78 2f 30 83 6f 62 01 8c cd 31 f9 24 e4 2f af 6f a2 8e 44 a8 03 4e 43 f3 8b 8d 4f 2d ac 60 7d 99 e1 53 7e dd c3 06 04 37 d3 8b 2b e3 d2 f3 42 e3 34 be 8a 28 3f 65 8c c7 03 d5 2d f7 c0 73 d7 9c fe cf ea ca 67 97 1d 14 a9 85 98 c5 0c 6e ee e8 1d 9d d8 cd 18 d4 ea db 93 0b 25 63 9d 2b ab b2 a1 b1 ed f5 05 76 88 3d 44 1a c7 f9 95 57 34 81 14 28 98 20 0c af 77 c2 33 b0 f8 81 c3 36 30 09 10 cf a6 fe 25 dc fc e5 fd 4c 88 98 0e f0 cc 77 21 a9 8c 15 f7 79 cf 28 98 ea ac 5b 24 41 55 cc 92 9b 21 ea 6f 03 13 e5 af 27 1c c2 30 9c 6e 87 6a 80 06 c5 e0 72 c0 4e a6 90 5b fb 47 cf df cf b8 a7 4c 13 bb 80 40 89 08 9d f5 88 fb d2 2b fa 1b ff 78 d6 06 57 5c 09 4a 16 4c cb 49 c3 0f 98 55 67 5d a4 28 10 f7 37 01 09 97 fd e3 84 e9 c9 e9 5e 54 e4 63 a8 bc 0c e3 a2 1d 36 c0 f2 36 eb 33 31 b7 78 2d 24 35 b7 a6 05 41 7e 2d 58 23 0b 39 b9 1b 07 7f ed 81 1a 01 81 7e 40 51 5c 0f d6 38 47 64 8a 63 c6 59 b7 57 65 da 79 44 7d e5 13 db d0 2d a9 2c bc 5e a8 19 cb 27 50 5d 88 34 99 36 1a f1 88 7f 58 85 d9 b1 3d 84 af 9f 50 54 2b 5c 52 29 7a a6 29 86 ee d9 a6 ec d8 a4 94 37 62 59 21 10 fd 12 0b 82 53 4b e9 2c 04 01 52 19 b1 cd 03 af 9a 32 89 67 10 c3 c1 69 65 e1 59 95 df 87 69 58 5b d7 f0 44 6b bf 24 2b 71 06 d0 5d c6 15 76 b3 5a 11 e4 c1 c0 63 9b 81 c9 85 cc e8 1c 99 49 0b c2 97 29 e0 5b 5a b0 ca 10 8b ac 16 25 86 26 99 5f 5e c7 2e 0a 4e 29 f1 4f 2e 84 43 50 47 0c 84 8d e1 73 c1 2a 85 ba 0e 98 19 bf a5 d2 18 46 c3 53 17 82 12 a9 d1 3e 30 de d2 37 ca ae c1 43 a5 7e d7 8f dd 7f 34 87 72 91 19 86 75 94 94 9e fb b3 ff 3e 7e fa ae ff f0 c4 cc d4 fc ba 56 27 61 41 5c 2a 4d 76 65 ab c4 fa 6b 9b 8f 22 52 0b 06 47 cd d5 bd d9 c4 e0 3d b3 aa 25 4b ab d7 94 35 24 95 e8 b0 fc 4a 1a bb ba 47 a6 64 ed 0f 16 72 aa 5b d9 a4 10 54 e2 42 f3 e6 ea ba 50 82 3c 57 ea 7b 34 e5 4d 30 03 27 0d f7 63 a5 68 5b fb b0 50 f9 44 38 55 34 f2 d5 08 bc a9 bc b9 07 3b 55 9c 1f d2 0b 95 5a da d4 65 6a da a1 f7 81 dc 67 e4 40 71 0d 8c cf 10 c2 0f cb 28 43 56 c5 15 d4 62 19 cb 01 7b 2c 54 34 09 e2 0a eb c2 b8 00 69 0d 18 b2 c4 5b 04 ea 0b 1a ba 5e 1b b9 3f 90 0d 20 0e 8c 5a b6 cc b5 f3 f8 f1 81 b9 21 24 a8 1c aa 36 3e ef 49 b3 20 14 b9 4d b7 06 36 64 a7 08 02 12 9a 96 4b 1a 69 4c d1 f0 9f b0 b9 b9 78 40 00 89 05 88 16 e3 a3 f4 cc a7 85 d5 0d 58 45 b4 d3 24 2f 4f ee 4e 1f 3a 40 bb b0 0d 1d 76 e1 9b 24 b9 00 2e 67 75 00 82 0e 41 95 41 a0 82 21 d1 0c 2c cf 34 f6 ca e2 41 3b 70 80 4c 14 48 84 01 a5 c5 3c 36 ae f3 f9 da 68 a1 9b bc 4d b9 5d 2c 9d 3f a8 88 95 bd f9 96 e7 e7 bc fb ff 83 d9 00 53 a7 b5 b3 b7 77 d0 bc 4a 22 2e 2d e7 ef 7e 7b 2c b5 b8 a6 73 c0 50 ac dd 63 12 38 85 c4 d7 76 8e 4c cf 2c 36 75 0e 05 46 a7 2e 3c 4f 23 98 cd 67 79 15 18 38 90 0d 8e 34 72 b0 bf 15 99 15 68 03 a4 c3 93 bd cc f9 8a 7d c1 c8 28 bd f7 92 4f 78 17 ec 1d cc 06 a5 55 f5 7d e3 f3 dd 03 7b b2 80 4c 93 91 5f f2 f7 bf f9 2c 3e ab 2c bb b4 5e a7 97 bc 75 f9 c2 93 be e0 12 9c 5a d6 54 a2 6d 8c 4a 2b f5 8f 48 7e f2 5c c6 bc cb fa 0e 39 f6 ad d3 93 6f 3d f0 90 0b 7b 59 b7 1f 97 0d 90 8e fb 63 5c ef b8 23 74 08 60 45 0a e8 8d 40 bd f6 08 b0 11 e6 57 d6 4c b5 1e af 68 cb 37 9a e5 d5 9d 0f 60 83 ca ba c6 12 5d 57 4a 9e b9 7f 06 a0 bf d8 7d fb 89 d5 77 69 45 ba 80 08 f5 fa a6 79 04 6a 66 66 f6 8b 1b 5e 0f 12 f3 fd 94 c9 df 79 28 94 49 92 cd b7 ff 62 db 48 8e 00 75 31 c6 fa ad 10 b5 f1 d6 dc 0c 49 a1 ae 81 ce b1 f9 35 8e 51 38 f5 d2 b3 b8 70 09 5c e3 f2 b1 68 79 46 e3 13 1a f7 8c cb 7b 3e 50 1a 4b e0 1c aa c2 a0 bc a7 4a 27 a4 88 f3 e0 1c 99 81 93 ca b3 74 87 a4 a0 a3 89 d0 b9 44 e7 dc 51 a4 be 18 18 9a 42 00 84 c3 43 35 13 82 70 8c cc 26 bc 6d ba 5a fc 48 af 84 02 a7 c8 ec 86 2e 49 1c 70 ba c4 58 09 cb ca 10 8c c0 35 f8 ee 7c 25 8a e2 9d 50 f0 e2 13 3b 0a 56 8b 7c f3 81 d7 8f cb 06 08 6c 8c ec f7 48 46 46 2b 4e b2 ac 90 eb 44 27 c5 f3 fb ba b0 7f 48 f6 99 1a 4b 38 0c 22 ba fa be 2e 73 36 a0 2c e2 86 a7 32 2c 39 af a3 d7 dc 81 2b ac ac f9 c7 df 7d ee 12 1c ef 13 99 5e bc 2f 2b 2c 16 d4 da dd ff 3f e7 ef 7d e7 a1 3c 73 d5 f5 7f ad ae 17 54 ec c6 86 4d 97 4b a0 97 68 20 09 20 a8 8a e0 5a 54 8e 16 f7 3f be b0 0e bb 90 f0 19 41 4c 3e e1 6f c9 43 70 95 2e 84 e4 88 68 43 51 03 39 2c 0d d9 5c 65 46 19 d1 31 a2 6c 91 39 55 1c 2a e6 20 8e 41 64 6e 35 01 3e c2 05 38 c4 c4 dd 41 16 39 1d 20 63 91 92 30 26 e5 4c 40 16 76 62 46 e9 ce ab ce af 93 14 5a 63 cf b0 95 57 3c 37 3e 7a 55 eb ae 17 85 af 42 22 f9 ac 7b 0c 8b a9 37 b2 01 71 49 22 bc 67 9d 95 c0 94 ee 1c 2d 73 c5 e4 55 63 95 e2 78 10 c0 c1 e9 c7 bd 23 be 24 56 85 cf 80 87 f3 d3 64 03 1c 27 52 1f ef 8b 86 80 03 12 44 79 0f 69 04 39 99 fd be e0 73 7c 22 a8 2a 03 8c c8 a9 34 cc ed 89 5c bd e3 5c 7b d8 00 9e 3e 73 cb c3 31 3c 23 2a 35 d7 0c ee d0 e8 f8 af bf b8 70 c9 39 d8 31 44 ed fa 20 fa 15 8a 2c 26 2d f7 3f cf de f8 d4 ee d6 cf ff 64 fb f1 e9 6f d6 d6 cd 55 24 f9 70 02 35 90 5d 54 4e 15 a6 a1 e9 44 c4 28 20 3e 3b f7 08 d3 5a b4 3b 61 1a 12 5b 22 c2 40 76 10 82 23 07 0c 3f bc 6c e7 50 ad 31 9b 96 2e 77 40 ab 12 db 81 a6 89 b5 ed 1f 05 bd 5a 79 c6 11 5c 22 e0 68 ed 2e d5 11 c1 9c d6 de 09 04 8e 4e 50 14 e4 10 86 9e 61 54 72 b1 ce 2e 20 15 12 df 0f 81 18 39 8c 47 bd d3 e1 2b d2 70 fe 08 6a 99 82 e2 98 81 23 ac 64 92 af 42 5a e3 1a d2 c8 fa 09 b3 10 64 e9 32 d6 1d 88 8b 02 10 ac 6a aa 15 a0 12 82 21 b9 75 1d 52 d9 02 56 f9 b3 67 70 2c 61 1c a9 f4 60 5a 8a 19 88 0b f1 49 78 91 3a 3c aa 4e 18 45 f0 24 a7 b6 9d 78 22 fd 89 35 91 90 ca aa 6e 05 0e 3d d7 36 1e 12 7b c9 ad 6b 47 c5 09 db 89 10 16 d0 b8 c9 67 f1 8a 1f 4c 6c 9e d9 71 70 05 70 7a de 08 d1 d0 81 50 af 9f 31 35 41 85 1f 45 5f 7c 02 7e b9 be 27 b3 ba 0d ed 2a 3a 13 b6 16 86 13 92 8b 50 b5 dc 88 fb cc 66 b3 6b db c9 07 0b ef b6 b6 7d 00 94 c2 b1 d4 0d f0 ca bc d8 02 24 82 38 dc d7 06 21 f6 1f d3 2b 5a f6 b0 41 48 52 e6 cf 6d ee 5c f7 0d 9f 9c d9 53 23 30 34 3a 71 ec 3b d7 0b ce 21 76 4e a1 b6 77 7c e6 17 5e c3 88 9f 7d 79 e3 57 c7 bf f9 e7 4f 4e fd c7 a9 6b 19 25 bb b1 54 d3 45 60 66 48 f9 5d 3f 0d d5 10 24 f3 e5 20 0c 48 b1 f6 8a b7 f5 88 a4 ac 45 f4 27 0a 49 d5 d0 49 27 25 a7 ce 2b 36 0c 07 60 eb 19 73 ce 57 4d 8d 4a 45 4b af 59 a8 84 3e 28 01 34 06 02 5b 9e 91 f8 dd e7 77 82 c9 0c a0 7f a8 96 21 34 11 a0 29 21 d6 24 3a 90 3f 26 fa 49 34 a9 b1 7b 88 d2 0c 32 0c d0 34 15 2c 18 66 e4 aa ae 04 26 88 c8 0c f5 4e 84 50 af 04 24 04 69 a4 e8 5b a0 ba 98 c0 b9 1c f7 28 ae ef f8 ca 27 ce d6 4f fd 6d 40 62 49 43 e7 6b 7d 3e f8 5c ae 20 14 cb e0 d8 c3 d2 cb 7b 46 24 21 4d 84 5b 64 1e a8 99 61 23 65 cd dd 89 45 b5 94 c4 10 f2 47 88 d0 0e 17 d1 87 ea 1d 72 70 50 2a 58 a2 27 8c 5a 58 d7 be b8 b6 41 5e 0f 2e 82 19 08 23 8a f8 09 d1 61 0c 3f 8a 44 e8 4f bc 05 32 25 f4 de 35 32 89 a6 85 e6 60 1e ec 37 8a a6 88 f6 02 8a 60 17 79 06 8c 3d 34 1b b4 0e 75 62 3d 12 b3 6f e8 19 26 0a c4 d6 a2 f3 aa 09 4d 62 67 02 5f f0 09 73 39 47 65 42 c4 22 67 87 24 6a 1d 18 47 03 53 c6 52 d1 dc 4d f8 b8 77 7c 3a 20 b9 80 6c 03 9d 99 5d d4 62 b1 e0 2f bd a2 50 20 00 61 46 4c 65 10 4b 0a 99 1c 02 a9 1e 42 43 a0 9a 59 08 ce a2 7b 91 98 04 ee f2 6b db 30 92 29 69 31 c3 de 1b 11 fd fe ce 2f d8 60 68 6c e2 df 4f 5c b1 f5 08 57 25 ef b1 e9 75 ad 5d 27 6f 7a 9e 73 0a fe dd 05 a7 4f 2f da 1b a6 5f 5f 23 60 7b cd fe 57 c7 be f9 c5 e7 97 7e 7f c5 3b 54 bd 5b 27 67 36 37 32 2c a9 a4 9e 88 3e 21 79 4a 00 a8 21 a1 c3 c8 e4 1c c4 8a a2 a0 82 80 70 32 e6 b5 35 3c e0 ac ba e8 13 63 4a 58 e0 05 6a 3e e3 1e 4d 21 9a 63 44 a6 a0 8c e7 c4 f4 ec 5e 78 1a da 40 a6 72 da e1 2e 2c 16 b8 0b f4 b9 44 65 52 f3 e3 16 97 c7 61 f3 89 00 e5 d7 3e 31 08 72 11 87 85 38 ec 23 b3 45 d4 1c 39 64 17 90 82 d4 e4 99 7c df 05 ef 68 34 15 66 95 5b 74 16 10 5c 63 72 88 a5 9a d6 66 cf 2d af 92 c7 25 e1 40 b2 f9 56 68 8a 69 75 ea 21 0f 09 3a 20 e6 0d 89 63 6b c5 e6 55 23 0b 60 00 1a 21 3e 98 9f 84 14 1c 5b d7 29 a5 66 21 38 94 98 0c 96 b4 94 ae 6b 88 40 70 0c a9 b7 c5 15 fe 8a 4f b0 90 a8 e9 c0 c8 04 a6 dc 1f e2 16 fb 85 d5 85 31 83 c4 85 94 d9 11 a5 13 28 5e a1 a9 30 ca 21 59 ea f3 50 8f 62 ac 54 f5 3d bf cc d1 54 4b 75 d4 83 d4 e4 92 8e a0 1d ea 8f cc 91 b2 69 c8 05 7c 03 d1 99 64 05 5c 47 11 0a 32 05 c1 e7 93 98 5f 5c 2f e9 70 38 16 0f 10 ff 81 3d 02 19 15 44 a1 07 a9 2e 34 98 48 35 50 3c 42 be 7c 6c 7a 1e 25 c3 2b da 29 26 af 0a cd 26 a7 c9 d9 97 69 89 fb 21 d1 fb 8a 6e 2f d8 e0 86 3f 99 f9 48 9b db ee 93 93 2f ac c6 98 f4 02 6b fb 80 e3 0e f7 7f f6 c5 37 7f ba ea 6c 98 9a 91 61 ed ec ec 50 6e b4 be be 3e 39 39 69 30 18 ba bb fb ea 9a db 72 cb aa 9c 02 94 1f 9f ba fc e9 d7 8e 1f db de fd fc 5e 68 49 5d cb 2b a6 5f 59 db f0 88 93 ca 84 28 67 10 3a 81 d4 95 6b 74 0e 49 ab eb 0f d4 c8 06 a8 16 8b 45 14 b4 99 5d 88 96 8b c6 cc b1 a9 fd b3 be b9 6d e5 ac 44 7b 98 16 bd 21 d5 b0 67 40 e5 7e 20 20 97 fc eb 59 67 05 51 0e b3 af a4 df d1 12 a2 dc 03 68 58 62 14 11 bd 36 ac 01 95 dc 08 d6 50 7c 8a 1a 79 53 4f 91 e3 87 2c e0 04 dc 15 88 03 b2 c6 88 c7 65 42 48 e7 d6 b6 e3 ae 60 5d 88 32 81 c6 ee 61 b2 2e 62 c1 e8 3a dc a1 ea f6 01 4d 59 23 75 3e 30 0f be 96 f8 04 b9 90 99 e6 01 99 aa 2e 7d c1 36 b0 81 30 ba 60 27 48 8d 44 1e 6a 01 46 0a 48 2e 22 5b 0c 1b 08 69 8d e1 c1 f6 91 50 c2 d8 63 ef b0 01 a2 0a 41 80 36 a0 b0 02 75 24 f6 88 75 84 08 e7 61 0b b9 6e ac b4 e3 19 0d 89 11 85 77 4b 8e 88 14 10 8b 17 4a 1e d9 01 a3 a2 85 32 ab 9a 61 60 92 f1 ec 94 8c 3e 6c 20 7c 42 ce 82 ec 18 38 87 f9 79 65 cb 78 74 64 75 e2 0b 76 5d 4d 60 7e 10 df a0 b2 a1 f5 98 83 d2 c6 59 e1 1a a8 10 18 6c 6c 6e bb ee 15 72 c5 37 f2 94 8b e2 1f fe fc b5 63 68 9c 28 b0 1b 1c 9b 70 09 89 bf 17 14 73 cd 53 71 d5 4b 79 d1 e5 c1 b1 ab ce e7 be f7 3e 7d c3 ed f3 cb f6 5f 7c e7 f2 97 ab ae 7f bc ec f2 c9 45 d7 bf de 0d 71 0e 4b 34 2b cb 5b db d8 22 cf 85 05 82 82 06 11 24 f6 c8 89 42 ca 54 8c 99 96 48 c8 09 1d 74 e5 57 c1 d9 22 79 8e 00 a8 d0 f7 4a 03 9b 7b 38 2a 52 60 8c a2 5e da 34 49 8e f8 41 99 9c 73 0b 47 7e cb 64 4d ec 08 33 89 1c 24 a3 38 3f e3 dd 27 0a 2b 30 8e a9 a2 43 ed 98 f1 00 65 95 76 ee 91 68 1b 61 e0 a2 a3 d1 5a b0 01 3a 44 40 60 f1 dc 68 73 04 21 b6 99 58 95 b6 b5 17 4d 42 5e 19 ae 23 84 65 aa 2b cc e0 bf ec 15 8b 8e 70 16 46 30 b9 24 d4 1a 66 00 3d 21 6b 6c 12 31 44 e4 b5 10 01 18 2a 42 43 52 d2 2c ca 2e b0 1b 29 98 43 1b e0 50 8a ce 88 4f c1 06 90 29 5c 2d ca 81 a0 66 34 89 28 42 66 2c a4 46 96 57 d4 4a e1 33 90 9a ad 6c e9 81 82 79 05 94 e4 00 2c ae 50 67 4a fa 09 39 45 58 82 d2 0c d2 d2 72 59 84 98 08 f7 29 48 53 04 64 56 8e 25 23 d8 00 06 43 c9 a4 94 d5 cb 0e 80 bc 6b 6c 9b 4b 7e b1 23 46 d7 05 7d 8b 56 c1 a9 81 ff c5 0f 42 38 26 7e e6 ce bc e2 67 0f 70 05 2a 85 d9 e1 76 51 4a cd 5c 53 f3 4b 20 5f a4 2c df fd 92 b4 01 94 fa 95 9b e2 8c 7b ec 2f 3f ff fa 9e bb 5f 7e 51 d9 35 17 bf 73 b7 3d 9d a3 73 be b0 0f fd d3 55 b7 72 dd ae e0 a1 f3 fc d2 72 62 66 51 50 54 ca 5d 1f e5 85 3b de 76 b7 7d 4e 5f 75 fb ec a2 fd 27 36 37 7f f9 d7 af fe e5 0f 56 ff fa 47 db df 5a df b8 e2 ad ca ae d0 ed ff 27 5b c0 88 8d 67 b4 8d 7b 84 8d 5b 04 7f a9 2c ba 1a 94 e8 15 9f 8f 9d ba 7f 33 a0 15 6d 70 23 2c 43 04 25 39 03 3b af 38 69 a0 5b 04 f5 3f 97 7c 63 b1 3d b0 a7 4d cd 44 6c a1 cb 41 29 a6 8e 2c 6a fa ae 32 f5 92 7f fc 97 9e 51 8c da 1d ee 19 4d 2d 31 30 f1 4c ae 84 a4 9b 85 4a 69 47 10 f2 0b 1e 3c 01 21 7d 29 a3 bf 14 90 f4 b5 77 8c 0c 81 c5 63 26 51 b8 81 37 69 eb 15 f3 62 55 7e 71 37 82 d5 41 29 25 d4 d5 bc c5 f1 c0 66 b2 2c c7 8c 81 fd 04 11 43 2b 24 a7 f0 46 44 b5 0c 57 4e 4d 4b 48 3a e9 aa 8a b1 d9 05 4a 9e 94 59 95 ca 2c 2d 65 3c 10 8a 10 a2 5c a4 ba f1 6e c5 33 bf 2b a0 d8 41 95 53 05 79 31 8b 28 0c e9 1e 36 40 73 24 f5 51 bc 8a 8c 0a 04 39 91 5c 38 04 6d c3 57 9e 11 f9 82 2e 95 d9 da c8 dc 2a 9c 01 0e 05 67 37 40 5d 14 96 59 21 c5 e8 8c 8e 13 74 cf e9 28 b3 ab 70 e2 f9 65 90 88 ea a0 64 a8 1d 02 08 b6 96 22 93 a8 9d 56 94 36 72 c1 6c b8 10 22 0f 8d 96 13 56 1c 2c 44 6d 2f 1b 61 b3 b0 19 c9 01 c4 a5 68 a7 f4 83 07 94 e4 fd 94 52 78 3b 2c bd 94 19 d1 45 af a8 8f 78 23 e4 4b 6c 90 51 52 73 c2 49 45 11 cb 7f 59 5f fb a7 8f 3f fb e3 f9 9b 94 3a df 0c cf 3e f6 7d 60 64 46 11 3f 3e 3e 10 22 ed fc 0b d7 3d 03 c3 f5 fa f6 d2 ea fa b4 fc d2 d4 fc d2 92 ea 7a 7e 81 30 33 ff 52 0a 40 c8 e1 b4 89 1f aa 12 4f 24 09 72 e0 2f 59 c5 8c 20 89 6e 72 d4 08 82 c6 d5 13 63 c5 2f 45 f6 14 12 18 87 20 20 49 0d 99 2e 18 5c 4b 62 63 cb 38 50 7a 30 4e fd 70 5b 28 1c 60 9a f5 df 9d fa 99 f4 c9 6c 6a 23 1c f9 de 66 f9 74 a6 8f bc 23 be 1a 6b 1d f6 af eb 8d 0e e5 e0 ce 90 85 5c 2f 20 7a 20 20 85 74 e4 c2 76 12 de 2a a4 25 5b 6e c6 6a 83 17 8b a1 ac 95 fe a2 d6 40 b4 4a 3f e7 35 ae 16 aa 92 eb 2c 4c c7 c8 c3 e9 40 60 c7 58 d0 2f a9 20 5e 31 e1 4c 2b 23 68 44 31 1a 43 55 bb eb 97 eb 29 78 67 f1 87 fc a5 18 60 c5 a4 fc 95 61 c9 cb 80 66 e4 75 72 82 6f 6a 76 be ec 18 3e c2 c4 ff d2 4d e9 9d 5c 5a d8 d4 1b 95 5f 77 57 95 fd b5 bf da d6 2d 3c 3c ad 70 6a f6 80 df 94 bc 87 f3 b4 80 38 52 18 80 26 11 c6 f8 ac f0 80 08 5b ff ed 5d 1f d5 b6 74 c5 97 b7 d5 f6 4d c5 95 ea 2f f9 c6 dc 08 8c 4d 2b ae 5a 5a 5e fe db db aa 65 47 6f 8d 01 74 2c 92 fe f0 bf 26 7d eb 89 7e ac 81 1f c5 a6 e7 85 67 14 c7 66 97 e5 54 e8 46 0c ef 33 b3 f8 63 6d c9 32 af 05 03 6f 8a 81 c3 16 5a bf 29 5c 4b 7f 0b 06 8e 10 06 2c 6c 70 84 0e cb b2 d4 0f 85 01 0b 1b 7c 28 cc 5a e0 1e 21 0c 58 d8 e0 08 1d 96 65 a9 1f 0a 03 16 36 f8 50 98 b5 c0 3d 42 18 b0 b0 c1 11 3a 2c cb 52 3f 14 06 2c 6c f0 a1 30 6b 81 7b 84 30 60 61 83 23 74 58 96 a5 7e 28 0c 58 d8 e0 43 61 d6 02 f7 08 61 e0 ff 01 86 79 bd fa e5 e1 d3 ac 00 00 00 00 49 45 4e 44 ae 42 60 82}}
    set image_changename {changename.gif {47 49 46 38 39 61 10 00 50 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 50 00 40 08 c0 00 ff 09 1c 48 b0 a0 c1 83 08 ff 01 58 c8 30 21 c1 86 07 21 0a 04 e0 50 a1 c4 8a 13 1d 32 bc 68 90 22 46 8f 0f 35 2e cc a8 11 e3 c4 8d 20 0b a6 b4 58 71 a4 c2 92 2f 3d ae 3c b9 d1 24 47 93 38 73 b6 44 29 b2 66 42 94 33 43 0e 0c 3a 34 e6 4f 9d 27 5b 76 2c 49 d1 25 42 88 37 69 02 8d c8 f3 68 d1 a3 4d 77 fa 54 09 d4 a9 ca a5 4f ab 22 1d 4b 95 e4 d3 98 44 87 46 b5 1a 76 2b 55 b7 60 6d 8a fd d8 73 6d 46 b8 64 bb a6 55 bb d7 2e df b9 64 f9 9a 7d eb 77 2f d7 91 78 5f 1a 55 dc 11 a4 d7 87 80 bf e2 9c 3a b9 ee e3 af 89 cf fe 8c 6c b1 6b c4 cf 65 af 82 16 da 18 72 dd c0 64 03 02 00 3b}}
    set image_changeowner {changeowner.gif {47 49 46 38 39 61 10 00 57 00 f7 00 00 05 05 05 fb fb fb 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 00 00 00 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 57 00 40 08 cc 00 ff 09 1c 48 b0 a0 41 00 04 11 1a 1c 08 a0 a1 c3 85 0c 05 3e 84 48 71 a1 c3 89 14 15 56 64 78 51 a3 c5 8e 1f 3b 36 84 38 92 63 45 8f 1b 11 aa 44 59 50 e4 46 89 2c 23 b6 3c 59 32 a6 4c 95 12 43 62 7c 69 b3 64 ce 97 40 83 72 bc 98 52 a1 cd a1 47 7f 02 4d da d2 27 4d a6 ff 5c 92 3c 28 b4 28 d1 8c 0f af 1e d4 e8 34 61 55 af 19 a3 ee dc aa 15 ec 4c 8b 54 b1 76 fd ca b6 6d 54 b3 3a d7 ba 9d 98 54 ea d2 a7 63 9b 96 4d ab 34 a4 d0 bd 5b 97 82 0c 1b 14 70 42 8f 72 df 9e 0d 6c 52 2d 54 b1 7d a9 ae b4 ca 94 6e 62 a3 8a 63 8a cc cb 37 6e dd c1 53 65 92 04 8d 16 a7 e0 cf 86 45 3b 26 3d b7 33 dc c8 87 53 bb ad 18 10 00 3b}}
    set image_changepolicy {changepolicy.gif {47 49 46 38 39 61 10 00 52 00 f7 00 00 05 05 05 fb fb fb 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 00 00 00 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 52 00 40 08 c3 00 ff 09 1c 48 b0 a0 41 00 08 0b 02 20 98 10 a1 c3 85 06 19 3a 8c 28 f0 21 44 8a 03 17 6a bc 18 d1 e2 c3 8e 0c 31 fe f3 48 91 63 45 91 28 15 26 4c c9 d1 64 45 8f 2b 4b 5a 2c 79 50 e4 c4 91 31 55 7e c4 a8 71 a4 4c 98 3c 67 a6 1c 2a 31 27 d1 9f 2e 15 86 ac c9 94 66 c6 a3 41 49 da 84 98 b4 68 d5 a7 3e a9 b2 9c ba 13 a9 d1 9f 36 a1 0e 75 79 55 2c d4 b2 32 51 0a ad 9a b4 ed c5 b2 52 c7 76 dc d8 75 eb 41 98 68 8f c6 05 89 35 2a dc af 4d b5 ce dd 6b b6 e8 49 bf 80 fb b2 9c 58 17 6b 4f 9f 73 33 b2 15 ea 74 2c 65 bb 5e e1 e2 4c cc 37 f3 60 c2 87 95 46 56 2c 7a e9 5d 89 5c f3 16 a6 18 10 00 3b}}
    set image_changetype {changetype.gif {47 49 46 38 39 61 10 00 4d 00 f7 00 00 05 05 05 fb fb fb 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 00 00 00 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 4d 00 40 08 bb 00 ff 09 1c 48 b0 a0 41 00 08 13 1a 24 08 60 61 43 86 0f 17 4a 9c 78 50 61 c2 88 0c 29 16 6c 78 d1 a1 c7 89 1d 35 8a 84 18 52 63 49 92 17 31 56 3c e9 30 22 c2 91 2a 37 a6 94 18 33 e6 c8 9b 20 5f fe b3 89 13 e5 cc 95 30 75 0e 54 59 93 66 cf 9d 3a 79 0a 54 7a 70 e9 4e a7 2b 15 e6 64 29 93 2a c8 96 29 85 ca cc 48 13 a3 d6 a3 60 c3 22 85 6a 94 23 53 a4 5f b1 8a f4 4a d6 a7 d5 b3 2d 4d 86 e4 69 93 28 ce ac 69 db ea 1d 0a 77 eb d3 ab 66 e5 e6 25 b9 74 f0 43 8e 7b d1 66 5d 4b 71 f1 54 a9 46 87 36 fe 59 f6 ef da c1 85 ad 4a 16 ec 58 ec 5e ba 4d bb 6a f6 3c 31 20 00 3b}}
    set image_changevault {changevault.gif {47 49 46 38 39 61 10 00 52 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 52 00 40 08 bc 00 ff 09 1c 48 b0 20 41 00 08 13 22 34 78 50 21 00 86 03 1d 3e 84 d8 70 22 c4 87 18 2d 32 94 98 70 e3 41 8a ff 24 5e 34 a8 11 a4 c9 93 02 39 2e a4 28 d2 23 ca 90 2f 23 ae ec b8 d1 21 48 8c 30 6b 72 64 69 33 a6 4f 93 25 5d 16 0c 3a 32 26 d1 88 21 69 92 4c 7a 54 a6 52 a0 13 9b a6 7c ea 12 67 ce 9b 37 7b 5e d4 9a 55 aa d4 9f 29 97 82 1d 4b d6 a7 c6 a3 2a 8d ea 6c 89 d2 2b 57 92 6f 87 1a 8d fb d1 ea 49 aa 32 c7 ae f4 48 77 6a 59 a8 51 bb e2 fd f8 52 21 53 a2 81 03 d7 cc bb 76 af d0 bb 7d 1f c3 35 cc f2 b0 5a 9e 94 87 a6 15 2b 77 31 d2 a2 84 e1 36 14 fc f7 67 40 00 3b}}
    set image_checkin {checkin.gif {47 49 46 38 39 61 10 00 36 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 36 00 40 08 87 00 ff 09 1c 48 b0 a0 c1 7f 00 12 2a 3c 38 10 00 41 87 0c 23 32 74 48 11 62 41 85 18 13 46 cc 68 51 a2 c7 86 0f 3f 6a ec 78 10 a3 47 8e 13 0d 92 fc c8 b2 25 c8 8d 19 4b 56 34 b9 b1 e1 4a 84 1c 35 d6 7c 09 53 e0 cd 87 0b 25 fe 74 a9 b2 28 d1 a3 48 93 da a4 99 b2 25 4a a7 32 71 32 2d 19 52 28 ce 89 39 6f 6a 15 6a 71 a8 d4 a0 4a 81 42 f4 fa 94 25 d9 98 2a b3 52 35 9a b6 ea 5a b7 17 3b ea c4 3a 37 6c cb 80 00 3b}}
    set image_checkmark {checkmark.gif {47 49 46 38 39 61 09 00 09 00 91 00 00 00 00 00 ff ff ff ff ff ff 00 00 00 21 f9 04 01 00 00 02 00 2c 00 00 00 00 09 00 09 00 00 02 11 94 8f a9 07 a0 ed 44 93 d0 c0 6b 6f a4 15 75 53 00 00 3b}}
    set image_checkout {checkout.gif {47 49 46 38 39 61 10 00 40 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 40 00 40 08 95 00 ff 09 1c 48 b0 a0 c1 83 08 0d 02 48 28 70 e1 40 87 05 01 48 9c 38 91 e1 3f 89 0d 19 42 d4 a8 d0 a2 c7 87 1f 43 12 84 88 31 e1 c6 8d 07 29 8a 5c c9 b2 65 4a 8a 15 4d c2 d4 38 53 a6 4a 9a 25 2d 62 44 e9 f2 61 4c 93 17 2f f2 1c 59 33 22 cc 9f 11 3b 22 3c da b3 a9 d3 a7 49 41 be 64 1a 72 a8 cf 9b 4b 8b a6 5c 89 75 e9 48 9c 48 95 66 e4 98 d3 e8 d1 a1 68 69 4a cd da d5 69 4c ab 0d b5 ea 04 5b f6 2a d5 a8 78 8d 7e f5 9a 97 a8 5f 9b 70 a1 16 0c 08 00 3b}}
    set image_create {create.gif {47 49 46 38 39 61 10 00 2c 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 2c 00 40 08 71 00 ff 09 1c 48 b0 a0 41 00 ff 10 02 40 68 70 e0 c2 87 0c 1b 26 9c b8 50 a2 c5 8b 18 33 1e 84 f8 50 63 47 8b 11 27 36 0c 29 90 a4 46 90 0c 4d 16 84 88 51 a5 44 85 22 2f 56 8c f9 f2 23 4a 96 23 0f de b4 79 b2 a7 cf 9f 32 67 ba 2c c9 51 26 41 97 1c 79 b6 0c 8a f3 65 42 a5 40 1d 0a 65 0a f5 68 cf a4 46 1d d6 b4 ea 74 e5 56 a9 54 a3 8a 0d 08 00 3b}}
    set image_delete {delete.gif {47 49 46 38 39 61 10 00 31 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 31 00 40 08 78 00 ff 09 1c 48 b0 a0 c1 83 08 05 02 20 b8 30 21 80 87 10 13 fe 8b 38 11 61 43 86 12 33 6a 74 f8 50 e1 46 89 10 43 76 fc c8 51 e4 45 92 28 53 1e 34 69 d1 e4 49 83 14 5f 82 54 d9 32 64 49 8a 2b 61 d2 dc c9 b3 a7 4f 85 0d 65 62 5c 38 f2 e6 4d 9b 0e 51 8e 14 3a 90 e8 44 a6 4d 71 16 74 59 14 a3 4e 90 41 67 12 85 0a b4 ea 4a a9 30 4f 42 95 c9 94 aa c5 ab 2a 03 02 00 3b}}
    set image_demote {demote.gif {47 49 46 38 39 61 10 00 32 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 32 00 40 08 7e 00 ff 09 1c 48 b0 a0 c1 7f 00 12 02 38 48 70 e1 40 87 0c 11 46 34 98 50 20 c4 82 15 1f 4e dc 78 30 e3 45 8a 1c 43 52 54 98 31 22 49 93 24 15 8a e4 b8 b0 e5 c7 87 29 4b 32 3c 39 31 66 cd 94 37 69 ce 6c 58 53 a2 cc 91 3f 31 82 ec 38 74 a5 51 94 0e 5f 36 c4 69 92 67 c7 98 4a 8f 5a 64 ba 53 63 ce a8 55 67 52 7d aa 93 a8 48 9b 52 b9 5a bc 1a 34 2c 58 af 4e 8b 5a 05 ba 35 ac c8 80 00 3b}}
    set image_disable {disable.gif {47 49 46 38 39 61 10 00 31 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 31 00 40 08 82 00 ff 09 1c 48 b0 a0 c1 7f 00 12 2a 3c 38 50 61 42 86 0d 11 4a 84 68 10 00 43 8b 05 31 52 dc c8 91 a0 c3 8f 07 35 76 14 f8 f0 e1 48 8a 1f 17 9e 44 69 52 e4 46 97 2b 49 a6 34 79 11 24 47 95 17 11 5a 84 a9 53 a7 43 88 29 81 56 8c 49 b4 68 46 8c 3c 23 ee 4c da 10 e7 d1 99 21 a3 b2 64 1a b1 ea d3 a0 23 a9 62 cd f9 d3 68 c6 9e 54 c1 f2 2c 69 33 a7 d5 90 4e 2b 8a 64 0a 33 e9 4c b7 43 8b 06 04 00 3b}}
    set image_enable {enable.gif {47 49 46 38 39 61 10 00 31 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 31 00 40 08 77 00 ff 09 1c 48 b0 a0 c1 83 07 01 28 5c b8 30 21 c2 87 ff 18 2a 44 08 20 e2 c4 88 10 33 6a 7c 28 b1 22 47 86 19 2b 8a 0c 29 91 a2 41 8f 1b 53 6e 6c 88 31 25 4a 95 05 3b b2 a4 58 52 e3 4c 87 30 6d ca cc c9 b3 a7 cf 9c 17 5f 9e c4 78 f1 63 d1 98 32 85 0a 54 da d2 a8 4d 82 4c 93 32 35 69 f4 e6 50 90 2e 21 76 d4 5a 93 ea 52 9a 50 a7 7a 44 29 76 e7 cf 94 01 01 00 3b}}
    set image_execute {execute.gif {47 49 46 38 39 61 10 00 30 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 30 00 40 08 7c 00 ff 09 1c 48 b0 a0 41 00 08 13 1a 24 98 50 e1 42 81 0d 11 3e 84 c8 f0 21 80 7f 0e 2f 4e dc c8 30 a2 c4 89 1a 39 42 fc 18 72 21 c9 92 15 0b a2 14 09 92 e4 c6 92 2b 41 be 8c 38 73 60 4c 96 07 69 72 74 88 b3 a5 c7 9e 40 83 ee d4 78 93 e2 c5 8f 3e 8b 7a 44 6a b2 e7 4f 96 45 47 36 14 6a 31 65 52 91 2e 65 1e 8d 8a 91 a7 56 a6 26 c1 36 b5 69 11 e6 4d a2 64 c3 3e a5 0a 34 20 00 3b}}
    set image_freeze {freeze.gif {47 49 46 38 39 61 10 00 2e 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 2e 00 40 08 77 00 ff 09 1c 48 b0 a0 c1 7f 00 00 08 54 78 70 61 c2 87 0d 07 3e 64 18 91 22 c2 86 13 27 46 dc 88 11 22 c7 84 0b 37 2a 1c 69 91 a3 c9 93 28 53 16 1c 89 b0 e4 4a 89 2e 09 82 0c 59 f1 e2 cc 8a 0c 63 ca f4 88 53 e3 41 92 3a 25 1a 0c aa b2 a8 51 94 1e 89 66 24 aa 32 a9 48 87 4c 53 3a ed c9 f3 e8 ce 9c 22 33 3e 45 aa b5 a6 d0 9f 51 69 52 d4 b9 f4 a6 55 94 01 01 00 3b}}
    set image_fromconnect {fromconnect.gif {47 49 46 38 39 61 10 00 52 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 52 00 40 08 b3 00 ff 09 1c 48 b0 a0 c1 83 08 05 02 f8 b7 10 c0 c2 84 0e 23 3e 44 38 91 61 c2 8b 18 33 56 c4 58 71 63 c6 8f 1a 09 7a 2c 28 91 a3 c4 91 03 4f a2 54 28 92 a2 c1 95 0c 4f 82 0c 99 92 a2 4a 98 20 6f c2 ec 68 92 e5 c5 92 3f 67 92 fc 29 93 a8 43 93 40 5f 2a b5 99 f4 e3 ce a2 4c 23 1a 95 3a 15 29 55 a5 57 5d 0e 15 ca f5 e5 cd a0 39 27 3e 6d da 55 e4 51 8b 10 a1 d2 8c 7a 96 ed ce 96 69 c9 6e 75 aa b6 6c ca ba 73 d1 c6 5d d9 56 ef 52 b8 5e cd 56 b5 7b 50 25 c7 b0 72 cd 26 ae c9 78 ef e1 c6 85 f1 02 d6 28 19 32 ca be 60 c5 ba c5 4c f8 62 40 00 3b}}
    set image_fromdisconnect {fromdisconnect.gif {47 49 46 38 39 61 10 00 63 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 63 00 40 08 d7 00 ff 09 1c 48 b0 a0 c1 7f 00 0e 2a 04 c0 50 60 42 85 08 19 4a 84 18 51 e2 43 88 16 29 22 8c b8 11 e3 44 8c 06 2f 1e 14 a9 51 24 49 8d 28 53 aa 1c 68 b1 a1 ca 93 04 5b c2 64 d9 92 a2 cc 91 09 6f 8e 0c 69 b3 e6 4a 9b 31 17 ca 9c f9 73 28 51 8e 0e 7f f6 cc e8 91 e9 c2 a0 20 95 a6 34 d9 93 a5 54 8f 48 9f 5a c5 09 15 a7 ce a9 4b 3f 0a 75 ea 55 ec d8 a3 15 cd 06 55 cb b3 20 da ab 70 b5 26 2d 4b 36 6e c7 b9 4d df ba 3c 9a 33 eb d9 92 3e e5 a2 fc 3a 38 ac cb b3 87 77 6e cd 5b b2 2b dd b7 2f 1d 97 05 ec 77 67 43 c8 78 63 e6 0c 6c 97 b0 db b6 a0 17 bb 1d 6a 57 73 dd d0 86 f9 72 0e 8d f6 f4 67 d1 21 57 bf 9e 2a db f1 cc c4 40 ef de 26 5d 1a 65 40 00 3b}}
    set image_grant {grant.gif {47 49 46 38 39 61 10 00 2e 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 2e 00 40 08 6b 00 ff 09 1c 48 b0 a0 c1 83 08 0d 02 48 f8 0f 80 c3 87 0c 1f 42 3c e8 90 60 45 86 18 2d 6a 4c 28 f1 22 c6 89 0a 07 4a cc 48 b2 a4 c9 93 02 3b 82 a4 d8 31 e2 46 84 0b 63 7e 7c 89 92 66 ca 88 2b 6b ea dc c9 b3 67 4a 95 0b 39 9a 54 e9 b2 24 51 a3 38 47 0a 55 0a 73 68 4b a1 22 61 56 cc f9 32 68 d0 90 05 af 2a 7c ea 93 64 40 00 3b}}
    set image_lock {lock.gif {47 49 46 38 39 61 10 00 22 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 22 00 40 08 5c 00 ff 09 1c 48 b0 a0 c1 81 00 00 fc 53 78 10 61 42 86 0d 0b 26 8c 48 b1 a2 c5 8b 08 09 42 94 f8 b0 23 46 8b 10 27 1a ec 48 92 e2 c6 8d 07 3d 9a 2c 89 11 65 46 89 1f 63 56 3c 39 73 a1 4b 81 24 55 c2 1c b9 f2 65 c3 9c 32 23 02 15 aa 71 e6 c3 98 37 17 f2 4c 99 33 69 50 8a 01 01 00 3b}}
    set image_modify {modify.gif {47 49 46 38 39 61 11 00 30 00 f7 00 00 05 05 05 fb fb fb 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 00 00 00 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 11 00 30 00 40 08 83 00 ff 09 1c 48 b0 a0 41 81 00 12 16 04 b0 90 61 c2 87 0c 0f 0e 84 a8 50 e2 c4 87 16 09 52 cc f8 0f 23 47 8a 1e 39 4e fc b8 51 22 c8 8a 07 23 6a 14 c9 d2 20 c4 94 0e 5b 22 5c 99 51 65 47 93 27 6d ca b4 68 53 27 cd 9f 3c 7d ba 44 79 73 a7 d1 8f 47 3b 0a 6d e8 71 69 d1 85 50 9f 4a 65 5a b2 e5 52 85 20 93 0e 3d 69 72 a6 57 92 44 83 86 4d 09 14 e6 4b ad 54 cf e2 2c 2b b6 66 48 b1 55 d1 ca 0c 08 00 3b}}
    set image_modifyform {modifyform.gif {47 49 46 38 39 61 10 00 4d 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 4d 00 40 08 b6 00 ff 09 1c 48 b0 a0 c1 7f 00 12 2a 3c 38 50 e1 42 86 02 1d 42 8c 38 91 22 42 82 00 2a 6a c4 f8 f0 a0 43 89 1e 3f 26 9c 28 92 24 46 92 1f 19 66 bc 68 51 25 c8 95 15 61 9e 64 49 d3 25 c8 98 23 37 ea dc 29 13 e5 4d 8f 33 77 42 94 d9 b3 61 51 9b 29 35 e6 1c 0a 94 63 49 83 23 9f 16 14 d9 11 69 cc a0 42 a1 12 1d 5a 35 ab d7 ab 5c 7f 6a 15 3b 95 67 52 95 66 c9 62 3d ea 94 6d 43 ac 63 bb 7e cd 28 77 2a 55 b4 65 c3 0a 55 1b 71 69 4b 84 74 37 52 75 5b f3 68 d4 b3 4d ff 82 b5 7b 37 6c dd b9 88 a1 c2 8d cb 34 6f 5c be 46 75 46 36 ba 35 f1 db af 01 01 00 3b}}
    set image_override {override.gif {47 49 46 38 39 61 10 00 39 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 39 00 40 08 8e 00 ff 09 1c 48 b0 a0 c1 7f 00 12 2a 3c 48 50 21 00 86 03 1d 26 84 28 70 22 42 8a 0d 1f 1e d4 d8 10 a3 c7 8f 11 25 2e 84 c8 f1 22 49 89 18 1d 82 f4 28 72 25 4b 8b 25 5d 46 ac 38 73 e3 c3 96 24 3b e6 04 59 32 66 c1 9b 08 7d ea fc a8 92 22 ce 8d 06 85 ca 5c 2a 13 a6 d1 8b 16 4f 46 25 6a 53 a4 d2 8a 28 79 da 0c 5a 94 e1 d5 a4 59 bd 0e 45 5a d3 6b 58 a6 19 35 7e 3d 8a 36 e4 54 b3 5d c9 12 1d 69 36 a3 d8 9f 75 43 1a a5 cb 34 20 00 3b}}
    set image_promote {promote.gif {47 49 46 38 39 61 10 00 36 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 36 00 40 08 84 00 ff 09 1c 48 b0 a0 41 00 08 13 1a 24 98 50 e1 42 81 0d 11 3e 9c 38 11 00 c3 87 11 1d 52 dc f8 2f a3 44 8a 0d 37 5a e4 08 f1 22 c9 93 28 51 3a 1c 89 d1 64 ca 96 03 59 72 94 79 30 22 48 9b 15 71 e6 fc b8 53 63 41 9d 30 5d be 1c fa 73 24 cd 9f 1d 3b 1e 2d ca b3 66 46 90 29 43 42 8d 79 53 2a 51 a6 3e 17 2e d5 0a d4 69 56 a4 54 ab 6e 0d 5b b2 67 d3 83 2a bb 82 2d cb 75 2c 59 a3 18 3d 5e 7d 19 10 00 3b}}
    set image_read {read.gif {47 49 46 38 39 61 10 00 22 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 22 00 40 08 63 00 ff 09 1c 48 b0 a0 c1 7f 00 12 2a 3c c8 b0 21 42 00 03 13 3a 9c e8 10 62 43 85 0b 2f 62 b4 68 70 63 46 8a 1a 3d 72 a4 38 b2 e3 43 89 21 05 96 04 69 52 e5 4a 82 2b 5f b2 9c 19 51 e4 4b 8b 1c 65 c2 fc c8 d0 63 45 84 40 41 f2 3c 88 52 27 50 88 28 43 1a 6d d9 71 e1 d0 88 50 83 36 f5 49 b3 aa c0 80 00 3b}}
    set image_revise {revise.gif {47 49 46 38 39 61 10 00 2b 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 2b 00 40 08 75 00 ff 09 1c 48 b0 a0 41 81 00 12 02 38 f8 2f 61 43 85 0e 19 36 94 48 71 62 c5 8b 03 23 2e 94 b8 d0 e1 46 86 10 2f 76 7c 08 32 23 42 8c 18 47 46 04 d9 f1 63 4a 94 22 21 ba 3c a8 b1 62 48 96 1e 57 16 94 a9 10 a6 cf 9f 14 7b 5a c4 29 14 a8 c1 96 43 59 92 3c da f2 66 49 93 1c 8d ea 64 3a d5 28 41 a1 33 77 ca b4 aa 35 63 d6 93 08 bf 7a e5 f9 f4 aa d5 80 00 3b}}
    set image_revoke {revoke.gif {47 49 46 38 39 61 10 00 32 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 32 00 40 08 80 00 ff 09 1c 48 b0 a0 c1 7f 00 10 22 4c 68 10 80 c3 87 0f 0f 12 84 28 b1 62 45 87 03 31 5a dc c8 51 22 c3 85 16 21 6a e4 38 f2 a0 c8 8f 05 4f 96 ec 18 92 21 4a 93 11 49 26 7c c9 d2 e4 c4 90 35 29 6e 8c 49 b3 66 c6 8c 3d 7f a6 f4 49 b4 28 4c 97 2d 75 7a bc 89 73 25 50 95 36 1b ee 8c 79 91 a9 47 91 46 8f 06 9d 88 b5 6a 4e a4 55 67 6e 15 a8 b4 63 50 8a 54 87 92 5d ab b5 6c 56 8e 01 01 00 3b}}
    set image_schedule {schedule.gif {47 49 46 38 39 61 10 00 39 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 39 00 40 08 8c 00 ff 09 1c 48 b0 a0 c1 7f 00 12 0a 04 70 b0 21 c3 86 06 1f 3a 54 88 f0 60 c2 8b 18 21 6a dc c8 b1 a3 43 8d 18 29 4e cc e8 51 a2 45 89 26 09 92 ac e8 b1 25 c7 90 29 17 c2 14 79 72 a5 c5 97 33 63 ba 54 19 12 64 cf 88 37 21 c2 fc 88 d0 e6 ce a3 48 77 52 d4 39 90 a1 53 a6 4d 2f 8e 1c 0a b4 ea 54 9a 20 37 52 4d 5a 74 66 50 94 38 b1 9e 5c 98 f5 a9 56 a9 3e 8d 16 84 1a d5 2b 57 95 70 d3 42 5d 2a b6 a9 4c b2 23 bb 26 0d 08 00 3b}}
    set image_show {show.gif {47 49 46 38 39 61 10 00 26 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 26 00 40 08 65 00 ff 09 1c 48 b0 a0 c1 7f 00 12 1e 34 98 b0 21 80 85 02 1f 42 1c e8 90 e2 44 89 13 29 2a cc 18 b1 21 c7 8f 20 43 16 94 e8 91 e3 46 91 10 31 22 bc e8 50 a5 c6 96 27 47 76 2c 89 b2 a6 cd 99 21 2b b2 fc 08 f3 a2 48 9d 29 09 ba 1c d9 32 27 4b a0 42 61 c6 b4 c8 70 e1 46 9a 4d 11 92 4c a9 10 ea 4d 8e 01 01 00 3b}}
    set image_thaw {thaw.gif {47 49 46 38 39 61 10 00 26 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 26 00 40 08 61 00 ff 09 1c 48 b0 a0 c1 7f 00 12 1e 34 98 b0 21 80 83 0e 23 2e 7c 58 90 e2 c4 85 10 15 62 bc 08 71 a3 c7 8f 20 07 52 6c 08 52 63 48 8b 1d 1d 6e 8c 88 52 64 c7 8c 2c 27 b6 0c 49 f3 a4 c9 8f 2a 57 ce 84 49 12 e3 4e 99 39 2f 3e fc c9 f0 66 d1 a0 2f 75 b2 fc 69 d4 27 43 a0 12 93 d6 9c 7a 30 20 00 3b}}
    set image_toconnect {toconnect.gif {47 49 46 38 39 61 10 00 49 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 49 00 40 08 9f 00 ff 09 1c 48 b0 a0 c1 83 08 05 02 20 b8 30 e1 3f 00 10 1b 3a 64 38 31 a2 44 83 17 15 56 8c 98 30 e3 c3 89 20 43 32 b4 c8 d1 21 44 8d 1d 45 3e 24 69 92 25 42 92 1e 0b c2 7c a9 b2 a6 cd 8f 03 63 de 54 58 32 e5 c2 93 1d 5d be 14 7a 70 66 ca 9c 26 29 ee 54 ca 74 29 d2 a7 18 75 46 35 ea b4 aa d5 ab 20 61 02 a5 29 72 ab d4 95 16 b1 82 45 39 34 6c d7 96 66 cb f6 2c da 74 ea 5a 8c 2a a9 5a 95 0b 17 aa da a0 6d f3 ea dc ba 12 ed d7 9b 74 65 e6 95 f9 76 b0 da b4 82 13 47 15 1b 10 00 3b}}
    set image_todisconnect {todisconnect.gif {47 49 46 38 39 61 10 00 53 00 f7 00 00 00 00 00 00 00 55 00 00 aa 00 00 ff 00 24 00 00 24 55 00 24 aa 00 24 ff 00 49 00 00 49 55 00 49 aa 00 49 ff 00 6d 00 00 6d 55 00 6d aa 00 6d ff 00 92 00 00 92 55 00 92 aa 00 92 ff 00 b6 00 00 b6 55 00 b6 aa 00 b6 ff 00 db 00 00 db 55 00 db aa 00 db ff 00 ff 00 00 ff 55 00 ff aa 00 ff ff 24 00 00 24 00 55 24 00 aa 24 00 ff 24 24 00 24 24 55 24 24 aa 24 24 ff 24 49 00 24 49 55 24 49 aa 24 49 ff 24 6d 00 24 6d 55 24 6d aa 24 6d ff 24 92 00 24 92 55 24 92 aa 24 92 ff 24 b6 00 24 b6 55 24 b6 aa 24 b6 ff 24 db 00 24 db 55 24 db aa 24 db ff 24 ff 00 24 ff 55 24 ff aa 24 ff ff 49 00 00 49 00 55 49 00 aa 49 00 ff 49 24 00 49 24 55 49 24 aa 49 24 ff 49 49 00 49 49 55 49 49 aa 49 49 ff 49 6d 00 49 6d 55 49 6d aa 49 6d ff 49 92 00 49 92 55 49 92 aa 49 92 ff 49 b6 00 49 b6 55 49 b6 aa 49 b6 ff 49 db 00 49 db 55 49 db aa 49 db ff 49 ff 00 49 ff 55 49 ff aa 49 ff ff 6d 00 00 6d 00 55 6d 00 aa 6d 00 ff 6d 24 00 6d 24 55 6d 24 aa 6d 24 ff 6d 49 00 6d 49 55 6d 49 aa 6d 49 ff 6d 6d 00 6d 6d 55 6d 6d aa 6d 6d ff 6d 92 00 6d 92 55 6d 92 aa 6d 92 ff 6d b6 00 6d b6 55 6d b6 aa 6d b6 ff 6d db 00 6d db 55 6d db aa 6d db ff 6d ff 00 6d ff 55 6d ff aa 6d ff ff 92 00 00 92 00 55 92 00 aa 92 00 ff 92 24 00 92 24 55 92 24 aa 92 24 ff 92 49 00 92 49 55 92 49 aa 92 49 ff 92 6d 00 92 6d 55 92 6d aa 92 6d ff 92 92 00 92 92 55 92 92 aa 92 92 ff 92 b6 00 92 b6 55 92 b6 aa 92 b6 ff 92 db 00 92 db 55 92 db aa 92 db ff 92 ff 00 92 ff 55 92 ff aa 92 ff ff b6 00 00 b6 00 55 b6 00 aa b6 00 ff b6 24 00 b6 24 55 b6 24 aa b6 24 ff b6 49 00 b6 49 55 b6 49 aa b6 49 ff b6 6d 00 b6 6d 55 b6 6d aa b6 6d ff b6 92 00 b6 92 55 b6 92 aa b6 92 ff b6 b6 00 b6 b6 55 b6 b6 aa b6 b6 ff b6 db 00 b6 db 55 b6 db aa b6 db ff b6 ff 00 b6 ff 55 b6 ff aa b6 ff ff db 00 00 db 00 55 db 00 aa db 00 ff db 24 00 db 24 55 db 24 aa db 24 ff db 49 00 db 49 55 db 49 aa db 49 ff db 6d 00 db 6d 55 db 6d aa db 6d ff db 92 00 db 92 55 db 92 aa db 92 ff db b6 00 db b6 55 db b6 aa db b6 ff db db 00 db db 55 db db aa db db ff db ff 00 db ff 55 db ff aa db ff ff ff 00 00 ff 00 55 ff 00 aa ff 00 ff ff 24 00 ff 24 55 ff 24 aa ff 24 ff ff 49 00 ff 49 55 ff 49 aa ff 49 ff ff 6d 00 ff 6d 55 ff 6d aa ff 6d ff ff 92 00 ff 92 55 ff 92 aa ff 92 ff ff b6 00 ff b6 55 ff b6 aa ff b6 ff ff db 00 ff db 55 ff db aa ff db ff ff ff 00 ff ff 55 ff ff aa ff ff ff 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 53 00 40 08 bd 00 ff 09 1c 48 b0 a0 c1 7f 00 0e 2a 04 c0 50 60 42 85 08 19 4a 84 18 51 e2 43 88 16 29 22 8c b8 11 e3 44 8c 06 2f 16 6c a8 b1 a4 49 8d 16 49 9a 14 19 32 25 c5 94 2c 47 ba 6c 59 f1 63 c8 9b 20 07 c6 74 08 73 e7 49 9d 3d 5f 5e f4 f9 13 68 46 8f 47 17 12 24 da b1 e8 ca a5 42 75 3a 8d aa f2 a0 48 9f 55 89 f6 64 5a 13 e5 4c 9c 50 ad 4e 1d 4b 56 a9 54 ab 41 cb 86 75 e8 b5 ea c2 87 4c 13 ca e5 9a f4 6d 5d b0 25 61 fe 8c fb 15 ed dd 91 6b ed d2 0d dc f2 2f d9 ab 6d e9 ce 45 c9 91 2b 61 9e 35 dd 1e ee 0b b8 32 5e b6 7e 29 97 d5 9b 13 33 52 ad 7b d3 5e 6e 2a 53 2d c1 80 00 3b}}
    set image_unlock {unlock.gif {47 49 46 38 39 61 10 00 2f 00 f7 00 00 05 05 05 fb fb fb 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 00 00 00 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 2f 00 40 08 75 00 ff 09 1c 48 b0 a0 c1 81 00 00 fc 53 78 10 61 42 86 0d 0d 42 3c f8 f0 61 c3 89 02 31 46 dc c8 91 22 41 8d 11 41 2e ac 98 b0 a3 c9 93 1f 1d 52 24 69 31 64 ca 8d 15 39 b2 5c c9 52 64 46 92 30 5f 9a 14 59 13 a5 cf 9f 32 75 5e 1c 79 b1 26 c8 a3 30 21 da 1c 19 13 28 cd 96 43 11 ca 84 5a d0 28 52 89 49 9b 46 dd 89 d3 e3 49 8d 4b ab 16 9d e9 d5 a9 c1 80 00 3b}}
    set image_utilspace {utilspace.gif {47 49 46 38 39 61 03 00 03 00 f7 00 00 00 00 00 33 00 00 66 00 00 99 00 00 cc 00 00 ff 00 00 00 33 00 33 33 00 66 33 00 99 33 00 cc 33 00 ff 33 00 00 66 00 33 66 00 66 66 00 99 66 00 cc 66 00 ff 66 00 00 99 00 33 99 00 66 99 00 99 99 00 cc 99 00 ff 99 00 00 cc 00 33 cc 00 66 cc 00 99 cc 00 cc cc 00 ff cc 00 00 ff 00 33 ff 00 66 ff 00 99 ff 00 cc ff 00 ff ff 00 00 00 33 33 00 33 66 00 33 99 00 33 cc 00 33 ff 00 33 00 33 33 33 33 33 66 33 33 99 33 33 cc 33 33 ff 33 33 00 66 33 33 66 33 66 66 33 99 66 33 cc 66 33 ff 66 33 00 99 33 33 99 33 66 99 33 99 99 33 cc 99 33 ff 99 33 00 cc 33 33 cc 33 66 cc 33 99 cc 33 cc cc 33 ff cc 33 00 ff 33 33 ff 33 66 ff 33 99 ff 33 cc ff 33 ff ff 33 00 00 66 33 00 66 66 00 66 99 00 66 cc 00 66 ff 00 66 00 33 66 33 33 66 66 33 66 99 33 66 cc 33 66 ff 33 66 00 66 66 33 66 66 66 66 66 99 66 66 cc 66 66 ff 66 66 00 99 66 33 99 66 66 99 66 99 99 66 cc 99 66 ff 99 66 00 cc 66 33 cc 66 66 cc 66 99 cc 66 cc cc 66 ff cc 66 00 ff 66 33 ff 66 66 ff 66 99 ff 66 cc ff 66 ff ff 66 00 00 99 33 00 99 66 00 99 99 00 99 cc 00 99 ff 00 99 00 33 99 33 33 99 66 33 99 99 33 99 cc 33 99 ff 33 99 00 66 99 33 66 99 66 66 99 99 66 99 cc 66 99 ff 66 99 00 99 99 33 99 99 66 99 99 99 99 99 cc 99 99 ff 99 99 00 cc 99 33 cc 99 66 cc 99 99 cc 99 cc cc 99 ff cc 99 00 ff 99 33 ff 99 66 ff 99 99 ff 99 cc ff 99 ff ff 99 00 00 cc 33 00 cc 66 00 cc 99 00 cc cc 00 cc ff 00 cc 00 33 cc 33 33 cc 66 33 cc 99 33 cc cc 33 cc ff 33 cc 00 66 cc 33 66 cc 66 66 cc 99 66 cc cc 66 cc ff 66 cc 00 99 cc 33 99 cc 66 99 cc 99 99 cc cc 99 cc ff 99 cc 00 cc cc 33 cc cc 66 cc cc 99 cc cc cc cc cc ff cc cc 00 ff cc 33 ff cc 66 ff cc 99 ff cc cc ff cc ff ff cc 00 00 ff 33 00 ff 66 00 ff 99 00 ff cc 00 ff ff 00 ff 00 33 ff 33 33 ff 66 33 ff 99 33 ff cc 33 ff ff 33 ff 00 66 ff 33 66 ff 66 66 ff 99 66 ff cc 66 ff ff 66 ff 00 99 ff 33 99 ff 66 99 ff 99 99 ff cc 99 ff ff 99 ff 00 cc ff 33 cc ff 66 cc ff 99 cc ff cc cc ff ff cc ff 00 ff ff 33 ff ff 66 ff ff 99 ff ff cc ff ff ff ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 21 f9 04 01 00 00 d7 00 2c 00 00 00 00 03 00 03 00 40 08 07 00 af 09 1c 38 30 20 00 3b}}
    set image_viewform {viewform.gif {47 49 46 38 39 61 10 00 42 00 f7 00 00 05 05 05 fb fb fb 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 00 00 00 21 f9 04 01 00 00 ff 00 2c 00 00 00 00 10 00 42 00 40 08 9b 00 ff 09 1c 48 b0 a0 41 81 00 0e 1e 4c 38 90 a1 42 87 ff 20 12 04 40 b1 22 45 85 05 2f 62 8c 68 51 62 43 8e 1e 37 8a 1c 69 b0 63 48 92 13 4f 3e 44 a8 11 e5 c2 84 26 1f 5a 74 89 32 26 46 9b 2b 3f de ec b8 11 a7 cc 96 25 11 ea cc 49 b2 62 51 a0 3b 19 aa 0c ba 90 66 ca 93 4b 9d 4a bd d9 93 67 52 a3 54 6b 5a 25 3a d2 27 53 a1 55 91 7e 8d 18 16 eb d4 8f 66 7f 6e cd 38 b6 a4 d8 ab 22 df ba 9d 99 55 2b d8 95 30 a3 d2 75 0b 32 ed 58 a8 51 65 52 5d 9b 31 f0 d9 8d 01 01 00 3b}}
    set image_filter {filter.gif {47 49 46 38 39 61 12 00 27 00 f7 00 00 00 00 00 80 00 00 00 80 00 80 80 00 00 00 80 80 00 80 00 80 80 c0 c0 c0 c0 dc c0 a6 ca f0 00 00 00 ff ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ff fb f0 a0 a0 a4 80 80 80 ff 00 00 00 ff 00 ff ff 00 00 00 ff ff 00 ff 00 ff ff ff ff ff 21 f9 4 00 00 00 00 00 2c 00 00 00 00 12 00 27 00 87 00 00 00 80 00 00 00 80 00 80 80 00 00 00 80 80 00 80 00 80 80 c0 c0 c0 c0 dc c0 a6 ca f0 00 00 00 ff ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ff fb f0 a0 a0 a4 80 80 80 ff 00 00 00 ff 00 ff ff 00 00 00 ff ff 00 ff 00 ff ff ff ff ff 8 6b 00 17 8 1c 48 b0 a0 c1 83 8 13 2a 44 8 00 c0 c2 82 e 1f 12 8c 28 90 62 c2 86 18 2d 32 cc a8 f1 a0 c5 8e 12 1f 36 5c 00 d2 20 c6 8a 22 49 a2 5c c8 b1 24 c4 93 29 7 ba 24 d9 72 66 45 8e 31 43 9a bc 58 d3 65 4f 9f 1e 75 ae f4 e8 b0 25 4f 9a 19 73 aa 14 aa 70 a4 4e 9b 3b 91 e6 a4 e8 b3 27 cf 9a 17 83 32 dd ca b5 6b 40 00 3b 00}}

    set images [list $ematrix_logo $plus $moins $matrix_type $matrixone_logo]
    set lTempImageNames [info vars "image_*"]
    foreach sVarName $lTempImageNames {
        lappend images [subst $[subst $sVarName]]
    }

    foreach image $images {
        Create_Image_File $Image_Directory/[lindex $image 0] [lindex $image 1]
    }

}


