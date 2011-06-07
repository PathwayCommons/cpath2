;(function($){
	
	$.fn.dialogpaper = function(opts){
		var defaults = {
			resizable: false
	    };
		options = $.extend(defaults, opts);
		
		$(this).html( '<div class="scrollbarpaper-wrapper">' + $(this).html() + '</div>' );
		$(this).dialog(options);
		$(this).scrollbarPaper();
		
		// press last button on enter---not the first one
		if( options.modal ){
			var last_button = $(this).parents(".ui-dialog").find(".ui-dialog-buttonset").find("button:last");
			var enter_listener = function(evt){
				if( evt.which == 13 ){
					last_button.click();
				}
			}
			$("body").bind("keydown", enter_listener);	
		}
		
		$(this).bind("dialogclose", function(){
			$(this).dialog("destroy");
		});
		
	};
	
})(jQuery); 