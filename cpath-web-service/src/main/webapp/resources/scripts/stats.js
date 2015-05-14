/*
 * WARNING: Google Charts API and codes2names.js must be loaded before using methods in this file.
 *
 * Many of the functions here manipulate DataTable, a class provided by Google Charts.
 */

var AppStats = (function() {
  /*
   * Takes a google.visualization.DataTable 
   * and sets all null cells to the numerical value 0.
   * This assumes that columns with null values have the type 'number'.
   * This skips the first column.
   */
	function setNullCellsTo0(table) {
		var numCols = table.getNumberOfColumns();
		var numRows = table.getNumberOfRows();
		for (col = 1; col < numCols; col++) {
			for (row = 0; row < numRows; row++) {
				if (table.getValue(row, col) === null) {
					table.setValue(row, col, 0);
				}
			}
		}
	}
  
	/*
	 * Takes a google.visualization.DataTable 
	 * and sets all null cells to the value from earlier date.
	 * This assumes that columns with null values have the type 'number'.
	 * This skips the first column.
	 */	
	function setNullCellsToPrev(table) {
		var numCols = table.getNumberOfColumns(); //no. different log events (names)
		var numRows = table.getNumberOfRows(); //no. days (col=0 contains dates)
		for (col = 1; col < numCols; col++) {
			//set 0 in the first row if it was null
			if (table.getValue(0, col) === null) {
				table.setValue(0, col, 0);
			}
			//rows: for each day, from the second one in the past to present
			for (row = 1; row < numRows; row++) {
				if (table.getValue(row, col) === null) {
					table.setValue(row, col, table.getValue(row-1, col));
				}
			}
		}
	}


  /*
   * Puts in backslashes for all double-quotes.
   */
  function escapeQuotes(str) {
    return str.replace(/\"/g,'\\"');
  }

  /*
   * Takes a DataTable and returns a string containing
   * the CSV representation of the table.
   */ 
  function dataTableToCSV(table) {
    var csvData = '';
    var numCols = table.getNumberOfColumns();
    var numRows = table.getNumberOfRows();
    var types = {}; // a dictionary of column indices to their types

    // add the headers
    for (var col = 0; col < numCols; col++) {
      csvData += '"' + escapeQuotes(table.getColumnLabel(col)) + '"';
      if (col !== (numCols - 1))
        csvData += ",";
      types[col] = table.getColumnType(col);
    }
    csvData += "\n";

    // put in cell values
    for (var row = 0; row < numRows; row++) {
      for (col = 0; col < numCols; col++) {
        var type = types[col];
        var val = table.getValue(row, col);
        var valstr;
        if (val !== null) {
          if (type === 'date')
            valstr = toISODate(val);
          else if (type === 'number')
            valstr = val + '';
          else
            valstr = '"' + val + '"';
          csvData += valstr;
        }
        if (col !== (numCols - 1))
          csvData += ",";
      }
      csvData += "\n";
    }
    return csvData;
  }

  /*
   * Download CSV data as a file to the user's computer.
   */
  function downloadCSV(data) {
    location.href = 'data:application/csv;charset=utf-8,' + encodeURIComponent(data);
  }

  // TIMELINE

  /*
   * Takes a DataTable, where the first column has dates and the rest
   * are numeric. Adds a numeric column called "Total". For each row
   * in the table, the Total column contains the row's sum.
   */
  function addTotalColumn(table) {
    var numCols = table.getNumberOfColumns();
    if (numCols <= 2) // don't add a total column if there is only one column containing counts and a date column
      return;
    var numRows = table.getNumberOfRows();
    var totalCol = table.addColumn('number', 'Total');
    for (var row = 0; row < numRows; row++) {
      var sum = 0;
      for (col = 1; col < numCols; col++) {
        sum += table.getValue(row, col);
      }
      table.setValue(row, totalCol, sum);
    }
    return totalCol;
  }

  /*
   * Parses the ISO date string "yyyy-mm-dd" and
   * returns a Date object.
   */
  function parseISODate(str) {
    pieces = /(\d{4})-(\d{2})-(\d{2})/g.exec(str);
    if (pieces === null)
      return null;
    var year = parseInt(pieces[1], 10),
       month = parseInt(pieces[2], 10),
         day = parseInt(pieces[3], 10);
    return new Date(year, month - 1, day); // In ISO, months are 1-12; in JavaScript, months are 0-11.
  }

  /*
   * Takes a Date object and returns an ISO date string ("yyyy-mm-dd").
   */
  function toISODate(date) {
    return date.toISOString().split('T')[0];
  }

  /*
   * Takes a JS object containing timeline data 
   * and produces a google.visualization.DataTable.
   *
   * The timeline data has this format:
   * {
   *   'name1': [
   *      ['iso-date-1', count],
   *      ['iso-date-2', count],
   *      ...
   *    ],
   *   'name2': ...
   * }
   *
   * I.e., timeline data is a map of timeline entries;
   * each timeline entry is a key-value pair: 
   * key is timeline's name, and value is an array 
   * of [iso-date, integer] data points (arrays of size 2).
   *
   * The DataTable returned has this format:
   *  Date  | name1 | ... | nameN
   * -------+-----------+-----+-----------
   * date-1 | count     | ... | count
   * date-2 | count     | ... | count
   */
  function buildTimelineByDay(timelineData) {
    var timelineByDay = new google.visualization.DataTable();
    var dateCol = timelineByDay.addColumn('date', 'Date');
    var date2Row = {}; // dictionary of dates to row indices
    for (var timeline in timelineData) {
      var col = timelineByDay.addColumn('number', timeline);
      var dls = timelineData[timeline];
      for (var i in dls) {
        var dl = dls[i];
        var date = parseISODate(dl[0]);
        var row = date2Row[date]; // convert the date to a row index
        if (!(date in date2Row)) { // if there's no row for this date, created one
          row = timelineByDay.addRow();
          timelineByDay.setValue(row, dateCol, date);
          date2Row[date] = row;
        }

        var count = dl[1];
        timelineByDay.setValue(row, col, count);
      }
    }
    timelineByDay.sort([{'column': dateCol}]); // Charts requires the rows to be ordered by date
    return timelineByDay;
  }

  function setupTimelineSaveCSV(byDayTable, cumTable) {
    $('#timeline-csv').click(function() {
      // disable the button while saving
      var btn = $(this);
      if (btn.hasClass('disabled'))
        return;
      btn.addClass('disabled');

      var table;
      if ($('#timeline-by-day').hasClass('active'))
        table = byDayTable;
      else
        table = cumTable;

      downloadCSV(dataTableToCSV(table));

      // re-enable the button after saving is done
      btn.removeClass('disabled');
    });
  }

  function setupTimeline() {	  
	  var timelineChart = new google.visualization.AnnotatedTimeLine(document.getElementById('timeline-chart'));
	  var timelineByDay;
	  var timelineCum;
	    
	  //build the cumulative timeline ()
	  $.getJSON('timelinecum', function(data) {
		  // create the cumulative table
		  timelineCum = buildTimelineByDay(data);
		  setNullCellsToPrev(timelineCum);
		  // setup the button that switch to cumulative table view
		  $('#timeline-cumulative').click(function() {
			  $(this).parent().find('button').removeClass('active');
			  $(this).addClass('active');
			  timelineChart.draw(timelineCum, {});
		  });    	      
	  })
	  .done(function() {
		  $.getJSON('timeline', function(data) {
			  // create table
			  timelineByDay = buildTimelineByDay(data);
			  setNullCellsTo0(timelineByDay);
//		      addTotalColumn(timelineByDay);
			  // setup button that switch to by-day table view from the other view
			  $('#timeline-by-day').click(function() {
				  $(this).parent().find('button').removeClass('active');
				  $(this).addClass('active');
				  timelineChart.draw(timelineByDay, {});
			  }); 
			  // setup "save as csv" button
			  setupTimelineSaveCSV(timelineByDay, timelineCum);
			  // display the default (by day) visualization right now
			  timelineChart.draw(timelineByDay, {});
		  });
	  });     
  }

  // GEOGRAPHY

  /*
   * Takes a DataTable where the first column contains
   * two-letter country codes. This adds a new column
   * with the full country name. This uses the "CountryCodes"
   * object to do the conversion.
   */
  function addCountryNamesColumn(table) {
    var countryNameCol = table.addColumn('string', 'Country');
    for (var row = 0; row < table.getNumberOfRows(); row++) {
      var countryCode = table.getValue(row, 0);
      if (countryCode in CountryCodes) {
        var countryName = CountryCodes[countryCode];
        table.setValue(row, countryNameCol, countryName);
      }
    }
  }

  function setupWorldChart(worldTable) {
    var worldChart = new google.visualization.GeoChart(document.getElementById('geography-world-chart'));
    var formatter = new google.visualization.PatternFormat('{1}');
    formatter.format(worldTable, [0, 2]);
    var view = new google.visualization.DataView(worldTable);
    view.setColumns([0, 1]);
    worldChart.draw(view, {});
    return worldChart;
  }

  function showCountry(countryChart, countryCode) {
    var countryName = CountryCodes[countryCode];
    $('#geography-country-name').text(countryName);
    $('#country-loading').show();

    $.getJSON('geography/country/' + countryCode, function(countryData) {
      var countryTable = google.visualization.arrayToDataTable(countryData);
      var countryChartOptions = {
        'displayMode': 'markers',
        'resolution': 'provinces',
        'colorAxis': {'colors': ['blue', 'yellow']},
        'region': countryCode
      };
      countryChart.draw(countryTable, countryChartOptions);
    });
  }

  function setupCountryChart() {
    var countryChart = new google.visualization.GeoChart(document.getElementById('geography-country-chart'));
    google.visualization.events.addListener(countryChart, 'ready', function() {
      $('#country-loading').hide();
    });
    return countryChart;
  }

  function setupGeographySaveCSV() {
    $('#geography-csv').click(function() {
      var btn = $(this);
      if (btn.hasClass('disabled'))
        return;
      btn.addClass('disabled');

      $.getJSON('geography/all', function(geographyData) {
        var geographyTable = google.visualization.arrayToDataTable(geographyData);
        downloadCSV(dataTableToCSV(geographyTable));
        btn.removeClass('disabled');
      });
    });
  }

  function setupGeography() {
    $.getJSON('geography/world', function(worldData) {
      // set up the world table
      var worldTable = google.visualization.arrayToDataTable(worldData);
      addCountryNamesColumn(worldTable);

      // instantiate the world and country chart objects
      var worldChart = setupWorldChart(worldTable);
      var countryChart = setupCountryChart();

      //when the user clicks on a country in the world chart, load that country's city data in the country chart
      google.visualization.events.addListener(worldChart, 'select', function() {
        var row = worldChart.getSelection()[0].row;
        var countryCode = worldTable.getValue(row, 0);
        showCountry(countryChart, countryCode);
      });

      //when the world map is loaded, load the default country
      google.visualization.events.addListener(worldChart, 'ready', function() {
        showCountry(countryChart, 'US');
      });
    });

    setupGeographySaveCSV();
  }
  
  function setupSelectpicker() {		
	  //define onChage event handler before initializing the selectpicker
	  $('.selectpicker').on('change', function() {
		  var url = $('.selectpicker').val();
		  if (url) {
			  // app's root context path (defined on the JSP page)  
			  // depends on how this app was actually deployed.  		
			  //console.log("will go to: " + contextPath + url);
			  window.location = contextPath + url;
		  }
		  return false;
	  });	

	  $.getJSON('events', function(events) {
		  $('.selectpicker').html(''); //clear	  
		  $.each(events, function(i, ev){
			  if(ev[1])
				  $('.selectpicker')
				  .append('<option data-subtext="' + ev[0] 
				  + '" value="/log/' + ev[0] + '/' + ev[1] + '/stats">' 
				  + ev[1] + '</option>');
			  else
				  $('.selectpicker')
				  .append('<option data-subtext="(category)" value="/log/'
						  + ev[0] + '/stats">' + ev[0] + '</option>');
		  });
		  //init bootstrap-select selectpicker
		  $('.selectpicker').selectpicker();
		  $('.selectpicker').selectpicker('refresh');
	  });
  };
  
  
  return {
    'setupTimeline': setupTimeline,
    'setupGeography': setupGeography,
    'setupSelectpicker' : setupSelectpicker
  };

})();

