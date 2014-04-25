// Extend JS String with repeat method
//String.prototype.repeat = function(num) {
//  return new Array(num + 1).join(this);
//};

$(function() {		
    // Disable link clicks to prevent page scrolling
    $(document).on('click', 'a[href="#fakelink"]', function (e) {
      e.preventDefault();
    });
	    	    
    // Switch
    $("[data-toggle='switch']").bootstrapSwitch();
	    	    	    
});
