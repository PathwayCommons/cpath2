$(document).ready(function() {
	getHelp("help");
    getCommandsList("help/commands/");
    getCommandBodies("help/commands/");

    getCommandParameterDetails("help/formats", "output_parameter", "#command_header_additional_parameters_output");
    getCommandParameterDetails("help/kinds", "graph_parameter", "#command_header_additional_parameters_graph");
    getCommandParameterDetails("help/datasources", "datasource_parameter", "#command_header_additional_parameters_datasource");
    getCommandParameterDetails("help/organisms", "organism_parameter", "#command_header_additional_parameters_organism");
    getCommandParameterDetails("help/types", "biopax_parameter", "#command_header_additional_parameters_biopax");
});

//
// This function creates a list (<ul><li>)
// of web service commands.  Each <li> is an
// href to an anchor on the page.
// The list is stored in content element #command_list
//
function getCommandsList(helpWSPath) {

    // get some var values from jsp page
    var base = $('#web_service_url').text();
    var command_header_prefix = $('#command_header_prefix').text();
    var command_header_additional_parameters = $('#command_header_additional_parameters').text();
    
    // interate over results (as json) from web service call
    $.getJSON(base + "/" + helpWSPath, function(help) {
            $('#content .command_list_class').remove();
            var command_list_ui = $('<ul class="command_list_class"/>');
            $('#command_list').after(command_list_ui);
            // for each command, create an <li> w/a href
            $.each(help.members, function(idx, member) {
                    $(".command_list_class").append('<li><a href="#' + member.id + '">' + command_header_prefix + ' ' + member.title + '</a></li>');
            });
            // add additional parameters
            $(".command_list_class").append('<li><a href="#additional_parameters">' + command_header_additional_parameters + '</a></li>');
    });
}

//
// This function creates a body for each command supported by
// the web service. The command body will consist of a summary,
// parameters, output, and example query.  The bodies are stored 
// in the content element #command_bodies.
//
// This function should probably be combined with getHelpCommandsList
// to save an extra call to the web service, but the code is more
// readable if the two functions exist separately
//
function getCommandBodies(helpWSPath) {

    // get some var values from jsp page
    var base = $('#web_service_url').text();
    var command_header_prefix = $('#command_header_prefix').text();
    var command_header_summary = $('#command_header_summary').text();
    var command_header_parameters = $('#command_header_parameters').text();
    var command_header_output = $('#command_header_output').text();
    var command_header_query = $('#command_header_query').text();
    var command_header_query_footnote = $('#command_header_query_footnote').text();

    // interate over results (as json) from web service call
    $.getJSON(base + "/" + helpWSPath, function(help) {
            $('#content .command_bodies_class').remove();
            var command_bodies_ui = $('<div class="command_bodies_class">');
            $('#command_bodies').after(command_bodies_ui);
            // for each command, create command body
            $.each(help.members, function(idx, member) {
                    // render command
                    $(".command_bodies_class").append('<h2><a name="' + member.id + '"></a>' + command_header_prefix + ' ' + member.title + '</h2>');
                    // render command summary
                    $(".command_bodies_class").append('<p>');
                    $(".command_bodies_class").append('<h3>' + command_header_summary + '</h3>');
                    $(".command_bodies_class").append(member.info);
                    // parameters
                    $(".command_bodies_class").append('<h3>' + command_header_parameters + '</h3>');
                    $(".command_bodies_class").append('<ul style="margin-bottom: 1px;">');
                    // we need to call into web services agoin to get command specifics
                    $.each(member.members, function(idx2, member_member) {
                                $(".command_bodies_class").append('<li style="text-indent: 1em;">' + member_member.id + '&nbsp[' + member_member.info + ']</li>');
                    });
                    $(".command_bodies_class").append('</ul>');
                    // output
                    $(".command_bodies_class").append('<h3>' + command_header_output + '</h3>');
                    $(".command_bodies_class").append(member.output);
                    // example query
                    $(".command_bodies_class").append('<h3>' + command_header_query + '</h3>');
                    $(".command_bodies_class").append(command_header_query_footnote);
                    $(".command_bodies_class").append('<br>');
                    //$(".command_bodies_class").append('<a href="' + member.example + '"></a>');
                    $(".command_bodies_class").append('<a href="' + member.example + '">' + member.example + '</a>');
                    $(".command_bodies_class").append('</p>');
            });
            $(".command_bodies_class").append('</div>');
    });
}

//
// This function creates a body for the given command parameter output.
//
// @param helpWSPath - path to web service call
// @param clazz - the name of the class that will be created in the DOM
// @param header - the key into messages.properties to header string 
//
function getCommandParameterDetails(helpWSPath, clazz, header) {

    // get some var values from jsp page
    var base = $('#web_service_url').text();
    var command_header = $(header).text();
    var none_mesg = $('command_header_additional_parameters_none').text();
    var class_name = clazz + "_class"

    // setup content element and append header
    $('#content .' + class_name).remove();
    var ui = $('<div class="' + class_name + '">');
    $('#' + clazz).after(ui);
    $("." + class_name).append('<h3><a name="valid_' + clazz + '"></a>' + command_header + '</h3>');
    $("." + class_name).append('<ul style="margin-bottom: 1px;">');

    // interate over results (as json) from web service call
    $.getJSON(base + "/" + helpWSPath, function(help) {
            $.each(help.members, function(idx, member) {
                    var info = '';
                    if (helpWSPath.indexOf("organism") != -1) {
                            info = ' ' + member.info
                    }
                    else if (helpWSPath.indexOf("kind") != -1 || helpWSPath.indexOf("formats") != -1) {
                            info = ' [' + member.info + ']'
                    }
                    $("." + class_name).append('<li style="margin-left: 1em;">' + member.id + info + '</li>');
            });
            $("." + class_name).append('</ul>');
            $("." + class_name).append('</div>');
    });
}

//
// Example routine illustrating how to use cpath2 help web service
//
function getHelp(helpPath) {
    var base = $('#web_service_url').text();	
	//alert(base + "/" + helpPath);
	$.getJSON(base + "/" + helpPath, function(help) {
		$('#title').text(help.title);
		//$('#info').text(help.info);
		if(help.example) {
			$('#example').text(base + "/" + help.example);
			$('#example').attr('href', base + "/" + help.example);
		} else {
			$('#example').text("");
			$('#example').attr('href',"#");
		}
		// add children (next help steps)
		$('#content .members').remove();
		var newUl = $('<ul class="members"/>');
		$('#info').after(newUl);
		$.each(help.members, function(idx, v) {
			if(v.members.length > 0) {
				// add clickable links 
				$(".members").append('<li><a id="' 
						+ v.id + '" href="#">' 
					+ v.id + '</a>' 
					+ ((v.title && v.title != v.id) ? (' (' + v.title + ') ') : ' ')
					+ '</li>');
				$("#"+ v.id).bind(
						"click",
					function() {
						$('#prev').text(helpPath);
						$("#prev").bind(
								"click",
							function() {
								getHelp(helpPath);
							}
						);
						
						// refresh content
						getHelp(helpPath + "/" + v.id);
					}
				);
			} else {
				// add the description without a link
				$(".members").append(
					'<li id="' + v.id + '">' + v.id + '</li>');
				if(v.title && v.title != v.id) {
					$("#"+v.id).append(' (' + v.title + ')');
				}
				if(v.info) {
					$("#"+v.id).append('<pre>' + v.info + '</pre>');
				}
			} 
		});
	});
}

//
// start of framework to display url example results in a popup window
//
function showPopup() {		
	$('body').css('overflow','hidden');
	$('#popup').fadeIn('fast');
	$('#mask').fadeIn('fast');
}

function closePopup() {
	$('#popup').fadeOut('fast');
	$('#mask').fadeOut('fast');
	$('body').css('overflow','auto');
}
