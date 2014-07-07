/*
 * Functions here manipulate DataTable, a class provided by Google Charts.
 */

var AppIps = (function() {
  /*
   * Takes a DataTable and sets all null cells to the numerical value 0.
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
   * Takes a DataTable containing the timeline data by day and
   * returns a DataTable containing cumulative timeline data.
   * The cumulative table has the same format as the by-day table.
   */
  function buildTimelineCumulative(timelineByDay) {
    var timelineCum = timelineByDay.clone();
    var numCols = timelineCum.getNumberOfColumns();
    var numRows = timelineCum.getNumberOfRows();
    for (var row = 1; row < numRows; row++) {
      for (var col = 1; col < numCols; col++) {
        timelineCum.setValue(row, col, timelineCum.getValue(row, col) + timelineCum.getValue(row - 1, col));
      }
    }
    return timelineCum;
  }

  /*
   * Takes a JS object containing timeline data and produces a
   * DataTable.
   *
   * The timeline data has this format:
   * {
   *   'name1': [
   *      ['iso-date-1', count], // <-- The number of downloads that occurred on this date for this version
   *      ['iso-date-2', count],
   *      ...
   *    ],
   *   'name2': ...
   * }
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
    for (var version in timelineData) {
      var col = timelineByDay.addColumn('number', version);
      var dls = timelineData[version];
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
    setNullCellsTo0(timelineByDay);
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
    $.getJSON('iptimeline', function(timelineData) {
      // create by-day and cumulative tables
      var timelineByDay = buildTimelineByDay(timelineData);
      var timelineCum = buildTimelineCumulative(timelineByDay);
//      addTotalColumn(timelineByDay);
//      addTotalColumn(timelineCum);

      // create timeline visualization
      var timelineChart = new google.visualization.AnnotatedTimeLine(document.getElementById('timeline-chart'));
      timelineChart.draw(timelineCum, {});

      // setup buttons that switch between by-day and cumulative timelines
      $('#timeline-by-day').click(function() {
        $(this).parent().find('button').removeClass('active');
        $(this).addClass('active');
        timelineChart.draw(timelineByDay, {});
      });
      $('#timeline-cumulative').click(function() {
        $(this).parent().find('button').removeClass('active');
        $(this).addClass('active');
        timelineChart.draw(timelineCum, {});
      });

      // setup "save as csv" button
      setupTimelineSaveCSV(timelineByDay, timelineCum);
    });
  }


  function setupSelectpicker() {		
	  //define onChage event handler before initializing the selectpicker
	  $('.selectpicker').on('change', function() {
		  var url = $('.selectpicker').val();
		  if (url) {
			  // app's root context path (defined on the JSP page)  
			  // depends on how this app was actually deployed.  		
//			  console.log("will go to: " + contextPath + url);
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
				  + '" value="/log/' + ev[0] + '/' + ev[1] + '/ips">' 
				  + ev[1] + '</option>');
			  else
				  $('.selectpicker')
				  .append('<option data-subtext="(category)" value="/log/'
						  + ev[0] + '/ips">' + ev[0] + '</option>');
		  });
		  //init bootstrap-select selectpicker
		  $('.selectpicker').selectpicker();
		  $('.selectpicker').selectpicker('refresh');
	  });
  };
  
  
  return {
    'setupTimeline': setupTimeline,
    'setupSelectpicker' : setupSelectpicker
  };

})();

