"use strict";

$(function() {	
	// make code pretty
	window.prettyPrint && prettyPrint();
	
	$(function() {       
	    $('.hider').on('click', function(){
	        var $hider = $(this);
	        var hideeid = $hider.attr('hide-id');
	        var $hidee = $('#' + hideeid);
	        $hidee.toggleClass('hidden');
	        return false; /*prevent following the fake link*/
	    });    
	});
});

$(window).on('resize load', function() {
    
	$('body').css({
        "padding-top": $("#header_navbar").height() + 10 + "px"
    }, {
        "padding-bottom": $("#footer_navbar").height() + 10 + "px"
    });
    
    $('.nav-target').css({
        "padding-top": $("#header_navbar").height() + 10 + "px"
    }, {
        "margin-top": $("#header_navbar").height() + 10 + "px"
    });
    
});
