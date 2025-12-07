/*
 * Author: Tsotsolas George (tsotzolas@gmail.com)
 * Αρχείο το οποίο έχει όλα τα Highcharts αρχεία τα οποία χρησιμοποιούνται για τα τα στατιστικά της εφαρμογής.
 *
 * Για να μπορέσει να δούλέψει έχουμε βάλει στο xhtml της εφαρμογής κάποια hidden πεδία απο τα οποία πάμε και πέρνουμε
 * τα δεδομενα τα οποία χρειάζεται για να παίξουν τα διαγράμματα.
 */
/********************WAGONS STATISTICS**********************/

/****                                                  ****/

/**
 * Wagons Statistics
 * Wagon Per Type Bars
 */
function showWagonPerType() {

    var wagonTypes = (document.getElementById('valuesForm:wagonTypes')).value.split(',');
    var dateParousiasisArithmos = JSON.parse(document.getElementById('valuesForm:dateParousiasisArithmos').value);
    console.log(document.getElementById('valuesForm:dateParousiasisArithmos').value);

    var wagonPerType = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            zoomType: 'xy'
        },
        title: {
            text: 'Καταταχθέντες ανά ημέρα'
        },
        subtitle: {
            text: 'Source: Pearl'
        },
        xAxis: [{
            categories: wagonTypes,
            crosshair: true
        }],
        yAxis: [{ // Primary yAxis
            labels: {
                format: '{value}',
                style: {
                    color: Highcharts.getOptions().colors[1]
                }
            },
            title: {
                text: 'Number Of Wagons',
                style: {
                    color: Highcharts.getOptions().colors[1]
                }
            }
        }, { // Secondary yAxis

        }],
        tooltip: {
            shared: true
        },
        legend: {
            layout: 'vertical',
            align: 'left',
            x: 120,
            verticalAlign: 'top',
            y: 100,
            floating: true,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || 'rgba(255,255,255,0.25)'
        },
        series: [{
            name: 'Wagons',
            type: 'column',
            yAxis: 1,
            data: dateParousiasisArithmos,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });
}


/**
 * Wagons Statistics
 * Wagon Per Type Pie
 */
function showWagonsPerTypePie() {

    var wagonTypesPie = document.getElementById('valuesForm:wagonDataPie').value;
    console.log("Test");
    console.log(wagonTypesPie);

    var wagonPerTypePie = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'pie',
            options3d: {
                enabled: false,
                alpha: 45,
                beta: 0
            }
        },
        title: {
            text: 'Wagons per Type Percent (%) Value '
        },
        subtitle: {
            text: 'Source: Pearl'
        },
        tooltip: {
            pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                depth: 35,
                dataLabels: {
                    enabled: true,
                    format: '{point.name}'
                }
            }
        },
        series: [{
            type: 'pie',
            name: 'Type share',
            data: JSON.parse(wagonTypesPie)
        }]
    });
}


/**
 * Wagons Statistics
 * Wagon Per Status Bars
 */
function showWagonPerStatusChart() {

    var wagonStatuses = (document.getElementById('valuesForm:wagonStatuses')).value.split(',');
    var wagonStatusData = JSON.parse(document.getElementById('valuesForm:wagonStatusData').value);
    console.log(document.getElementById('valuesForm:dateParousiasisArithmos').value);

    var wagonPerStatus = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            zoomType: 'xy'
        },
        title: {
            text: 'Wagons per Status'
        },
        subtitle: {
            text: 'Source: Pearl'
        },
        xAxis: [{
            categories: wagonStatuses,
            crosshair: true
        }],
        yAxis: [{ // Primary yAxis
            labels: {
                format: '{value}',
                style: {
                    color: Highcharts.getOptions().colors[1]
                }
            },
            title: {
                text: 'Number Of Wagons',
                style: {
                    color: Highcharts.getOptions().colors[1]
                }
            }
        }, { // Secondary yAxis

        }],
        tooltip: {
            shared: true
        },
        legend: {
            layout: 'vertical',
            align: 'left',
            x: 120,
            verticalAlign: 'top',
            y: 100,
            floating: true,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || 'rgba(255,255,255,0.25)'
        },
        series: [{
            name: 'Wagons',
            type: 'column',
            yAxis: 1,
            data: wagonStatusData,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });
}


/**
 * Wagons Statistics
 * Wagon Per Status Pie
 */
function showWagonPerStatusPieChart() {

    var wagonStatusPie = document.getElementById('valuesForm:wagonStatusDataPie').value;
    alert("Tsotzolas");
    alert(wagonStatusPie);
    var wagonPerTypePie = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'pie',
            options3d: {
                enabled: false,
                alpha: 45,
                beta: 0
            }
        },
        title: {
            text: 'Wagons per Status Percent (%) Value'
        },
        subtitle: {
            text: 'Source: Pearl'
        },
        tooltip: {
            pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                depth: 35,
                dataLabels: {
                    enabled: true,
                    format: '{point.name}'
                }
            }
        },
        series: [{
            type: 'pie',
            name: 'Status share',
            data: JSON.parse(wagonStatusPie)
        }]
    });
}


function showChartLessors() {

    var wagonLessors = (document.getElementById('valuesForm:wagonLessors')).value.split(',');
    var wagonLessorsData = JSON.parse(document.getElementById('valuesForm:wagonLessorsData').value);

    var wagonPerStatus = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            zoomType: 'xy'
        },
        title: {
            text: 'Wagons per Lessors'
        },
        subtitle: {
            text: 'Source: Pearl'
        },
        xAxis: [{
            categories: wagonLessors,
            crosshair: true
        }],
        yAxis: [{ // Primary yAxis
            labels: {
                format: '{value}',
                style: {
                    color: Highcharts.getOptions().colors[1]
                }
            },
            title: {
                text: 'Number Of Lessors',

                style: {
                    color: Highcharts.getOptions().colors[1]
                }
            }
        }, { // Secondary yAxis

        }],
        tooltip: {
            shared: true
        },
        legend: {
            layout: 'vertical',
            align: 'left',
            x: 120,
            verticalAlign: 'top',
            y: 100,
            floating: true,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || 'rgba(255,255,255,0.25)'
        },
        series: [{
            name: 'Lessors',
            type: 'column',
            yAxis: 1,
            data: wagonLessorsData,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });

}


/**
 * Wagons Statistics
 * Wagon Per Lessors Pie
 */
function showChartLessorsPie() {

    var wagonLessorPie = (document.getElementById('valuesForm:wagonLessorPie').value);

    console.log(JSON.parse(wagonLessorPie));

    var lessorsPie = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'pie',
            options3d: {
                enabled: false,
                alpha: 45,
                beta: 0
            }
        },
        title: {
            text: 'Wagons per Lessors Percent (%) Value'
        },
        subtitle: {
            text: 'Source: Pearl'
        },
        tooltip: {
            pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                depth: 35,
                dataLabels: {
                    enabled: true,
                    format: '{point.name}'
                }
            }
        },
        series: [{
            type: 'pie',
            name: 'Lessor share',
            data: JSON.parse(wagonLessorPie)
        }]
    });
}

function wagonsPerDate() {

    var k = (document.getElementById('valuesForm:wagonsPerDate').value);


    var temp = [];
    temp = k.split("|");

    var temp1 = [];
    for (j in temp) {
        var k = [];
        k = temp[j].split(",");
        temp1.push((k));

    }

    /********************************************/
    var timestampData = [];
    var timestampData1 = [];
    var timestampData2 = [];

    for (i in temp1) {
        timestampData.push([new Date(temp1[i][0].replace("[", "")).getTime(), parseInt(temp1[i][1].replace("]", ""))]);
        timestampData1.push([new Date(temp1[i][0].replace("[", "")).getTime(), parseInt(temp1[i][2].replace("]", ""))]);
        timestampData2.push([new Date(temp1[i][0].replace("[", "")).getTime(), parseInt(temp1[i][3].replace("]", ""))]);
    }

    console.log(timestampData);

    // create the chart
    var wagonsPerDate = new Highcharts.stockChart({
        chart: {
            renderTo: 'container',
            height: 600
        },
        rangeSelector: {
            selected: 1
        },

        title: {
            text: 'Wagons Per Date'
        },
        legend: {
            layout: 'vertical',
            align: 'right',
            verticalAlign: 'middle'
        },

        series: [{
            name: 'Attached Wagons (All)',
            data: timestampData,
            type: 'areaspline',
            threshold: null,
            tooltip: {
                valueDecimals: 0
            },
            fillColor: {
                linearGradient: {
                    x1: 0,
                    y1: 0,
                    x2: 0,
                    y2: 1
                },
                stops: [
                    [0, Highcharts.getOptions().colors[2]],
                    [1, Highcharts.Color(Highcharts.getOptions().colors[2]).setOpacity(0).get('rgba')]
                ]
            }
        },{
            name: 'Attached Private Wagons',
            data: timestampData2,
            type: 'areaspline',
            threshold: null,
            tooltip: {
                valueDecimals: 0
            },
            fillColor: {
                linearGradient: {
                    x1: 0,
                    y1: 0,
                    x2: 0,
                    y2: 1
                },
                stops: [
                    [0, Highcharts.getOptions().colors[5]],
                    [1, Highcharts.Color(Highcharts.getOptions().colors[5]).setOpacity(0).get('rgba')]
                ]
            }
        }, {
            name: 'Private Wagons',
            data: timestampData1,
            type: 'areaspline',
            threshold: null,
            tooltip: {
                valueDecimals: 0
            },
            fillColor: {
                linearGradient: {
                    x1: 0,
                    y1: 0,
                    x2: 0,
                    y2: 1
                },
                stops: [
                    [0, Highcharts.getOptions().colors[0]],
                    [1, Highcharts.Color(Highcharts.getOptions().colors[3]).setOpacity(0).get('rgba')]
                ]
            }
        }
        ]
    });
}

/****                                                   ****/
/********************WAGONS STATISTICS**********************/


function damagesPerDate() {

    var k = (document.getElementById('valuesForm:damagesPerDate').value);


    var temp = [];
    temp = k.split("|");

    var temp1 = [];
    for (j in temp) {
        var k = [];
        k = temp[j].split(",");
        temp1.push((k));

    }


    /********************************************/
    var timestampData = [];

    for (i in temp1) {
        timestampData.push([new Date(temp1[i][0].replace("[", "")).getTime(), parseInt(temp1[i][1].replace("]", ""))]);
    }

    console.log(timestampData);

    // create the chart
    var damagePerDate = new Highcharts.stockChart({
        chart: {
            renderTo: 'container',
            height: 600
        },
        rangeSelector: {
            selected: 1
        },

        title: {
            text: 'Damages Per Date'
        },

        series: [{
            name: 'Damages',
            data: timestampData,
            type: 'areaspline',
            threshold: null,
            tooltip: {
                valueDecimals: 0
            },
            fillColor: {
                linearGradient: {
                    x1: 0,
                    y1: 0,
                    x2: 0,
                    y2: 1
                },
                stops: [
                    [0, Highcharts.getOptions().colors[0]],
                    [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                ]
            }
        }]
    });
}


function missionPerMonth() {

    var missionPerMonthCategories = (document.getElementById('valuesForm:missionPerMonth').value);
    var missionPerMonthNorthbound = (document.getElementById('valuesForm:missionsPerMonthNorthBounds').value);
    var missionPerMonthSouthbound = (document.getElementById('valuesForm:missionsPerMonthSouthBounds').value);
    var missionPerMonthEastbound = (document.getElementById('valuesForm:missionsPerMonthEastBounds').value);
    var missionPerMonthWestbound = (document.getElementById('valuesForm:missionsPerMonthWestBounds').value);

    //Για να μετατρέχουμε τα String σε Integers
    var tempNorthBound = missionPerMonthNorthbound.split(",");
    var finalDataNorthBound = [];
    for (j in tempNorthBound) {
        finalDataNorthBound.push((parseInt(tempNorthBound[j])));
    }

    //Για να μετατρέχουμε τα String σε Integers
    // var tempSouthBound = [];
    var tempSouthBound = missionPerMonthSouthbound.split(",");
    var finalDataSouthBound = [];
    for (j in tempSouthBound) {
        finalDataSouthBound.push((parseInt(tempSouthBound[j])));
    }

    //Για να μετατρέχουμε τα String σε Integers
    // var tempEastBound = [];
    var tempEastBound = missionPerMonthEastbound.split(",");
    var finalDataEastBound = [];
    for (j in tempEastBound) {
        finalDataEastBound.push((parseInt(tempEastBound[j])));
    }

    //Για να μετατρέχουμε τα String σε Integers
    // var tempWestBound = [];
    var tempWestBound = missionPerMonthWestbound.split(",");
    var finalDataWestBound = [];
    for (j in tempWestBound) {
        finalDataWestBound.push((parseInt(tempWestBound[j])));
    }


    var missionPerMonth1 = new Highcharts.Chart({

        chart: {
            type: 'column',
            renderTo: 'container'
        },

        title: {
            text: 'Missions per month'
        },

        xAxis: {
            categories: missionPerMonthCategories.split(",")
        },

        yAxis: {
            allowDecimals: false,
            min: 0,
            title: {
                text: 'Number of missions'
            },
            stackLabels: {
                enabled: true,
                style: {
                    fontWeight: 'bold',
                    color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
                }
            }
        },

        tooltip: {
            formatter: function () {
                return '<b>' + this.x + '</b><br/>' +
                    this.series.name + ': ' + this.y + '<br/>' +
                    'Total: ' + this.point.stackTotal;
            }
        },

        plotOptions: {
            column: {
                stacking: 'normal',
                dataLabels: {
                    enabled: true,
                    color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white'
                }
            }
        },

        series: [{
            name: 'Northbound',
            data: finalDataNorthBound,
            color: '#2C8AF2'
        }, {
            name: 'Southbound',
            data: finalDataSouthBound,
            color: '#008000'
        }, {
            name: 'Eastbound',
            data: finalDataEastBound,
            color: '#ffd644'
        }, {
            name: 'Westhbound',
            data: finalDataWestBound,
            color: '#f28a8b'
        }]
    });
}


function detachmentsPerMonth() {

    // var data1 = [
    //     ["August 2018","4"],
    //     ["September 2018","31"],
    //     ["October 2018","34"],
    //     ["November 2018","44"],
    //     ["December 2018","58"],
    //     ["January 2019","55"],
    //     ["February 2019","9"]
    // ];


    var k = (document.getElementById('valuesForm:detachmentsPerMonth').value);

    console.log(k);

    var temp = [];
    temp = k.split("|");

    var finalData = [];
    for (j in temp) {
        var k = [];
        k = temp[j].split(",");
        k[1] = parseInt(k[1]);
        finalData.push((k));
    }


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'column'
        },
        title: {
            text: 'Detachments per Month'
        },
        subtitle: {
            text: 'Source: <a href="https://pearl-rail.com/">Pearl</a>'
        },
        xAxis: {
            type: 'category',
            labels: {
                rotation: -45,
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Detachment'
            }
        },
        legend: {
            enabled: false
        },
        tooltip: {
            pointFormat: 'Detachment in month: <b>{point.y:.0f} </b>'
        },
        series: [{
            name: 'Detachment per Month',
            data: finalData,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });


}


function detachmentsReasons() {

    var k = (document.getElementById('valuesForm:detachmentsReason').value);

    var temp = [];
    temp = k.split("|");

    var finalData = [];
    for (j in temp) {
        var k = [];
        k = temp[j].split(",");
        k[1] = parseInt(k[1]);
        finalData.push((k));
    }


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'column'
        },
        title: {
            text: 'Detachments Reason'
        },
        subtitle: {
            text: 'Source: <a href="https://pearl-rail.com/">Pearl</a>'
        },
        xAxis: {
            type: 'category',
            labels: {
                rotation: -45,
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Detachment'
            }
        },
        legend: {
            enabled: false
        },
        tooltip: {
            pointFormat: 'Detachment in month: <b>{point.y:.0f} </b>'
        },
        series: [{
            name: 'Detachment',
            data: finalData,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });
}


function detachmentsPerMonthReason() {

    var reasonPerMonthReason1 = (document.getElementById('valuesForm:reasonPerMonthReason1').value);
    var reasonPerMonthReasonDrillDown = (document.getElementById('valuesForm:reasonPerMonthReason2').value);

    console.log(JSON.parse(reasonPerMonthReason1));
    console.log(JSON.parse(reasonPerMonthReasonDrillDown));


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            type: 'column',
            renderTo: 'container',

        },
        title: {
            text: 'Detachments per Month / Reasons'
        },
        subtitle: {
            text: 'Click the columns to view reason. Source: <a href="https://pearl-rail.com/" target="_blank">pearl-rail.com</a>'
        },
        xAxis: {
            type: 'category'
        },
        yAxis: {
            title: {
                text: 'Total Detachments'
            }

        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                borderWidth: 0,
                dataLabels: {
                    enabled: true,
                    format: '{point.y:.0f}'
                }
            }
        },

        tooltip: {
            headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
            pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.0f}</b> Detachments<br/>'
        },

        "series": [
            {
                "name": "Months",
                "colorByPoint": true,
                "data": JSON.parse(reasonPerMonthReason1)
            }
        ],
        "drilldown": {
            "series": JSON.parse(reasonPerMonthReasonDrillDown)
        }
    });


}



function transferdContainerPerMonthFrom() {

    var reasonPerMonthReason1 = (document.getElementById('valuesForm:reasonPerMonthReason1').value);
    var reasonPerMonthReasonDrillDown = (document.getElementById('valuesForm:reasonPerMonthReason2').value);

    console.log(JSON.parse(reasonPerMonthReason1));
    console.log(JSON.parse(reasonPerMonthReasonDrillDown));


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            type: 'column',
            renderTo: 'container',

        },
        title: {
            text: 'Transferred Container per Month / Station From'
        },
        subtitle: {
            text: 'Click the columns to view reason. Source: <a href="https://pearl-rail.com/" target="_blank">pearl-rail.com</a>'
        },
        xAxis: {
            type: 'category'
        },
        yAxis: {
            title: {
                text: 'Total Containers'
            }

        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                borderWidth: 0,
                dataLabels: {
                    enabled: true,
                    format: '{point.y:.0f}'
                }
            }
        },

        tooltip: {
            headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
            pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.0f}</b> Containers<br/>'
        },

        "series": [
            {
                "name": "Months",
                "colorByPoint": true,
                "data": JSON.parse(reasonPerMonthReason1)
            }
        ],
        "drilldown": {
            "series": JSON.parse(reasonPerMonthReasonDrillDown)
        }
    });


}


    function transferdContainerPerMonthTo() {

    var reasonPerMonthReason1 = (document.getElementById('valuesForm:reasonPerMonthReason1').value);
    var reasonPerMonthReasonDrillDown = (document.getElementById('valuesForm:reasonPerMonthReason2').value);

    console.log(JSON.parse(reasonPerMonthReason1));
    console.log(JSON.parse(reasonPerMonthReasonDrillDown));


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            type: 'column',
            renderTo: 'container',

        },
        title: {
            text: 'Transferred Container per Month / Station To'
        },
        subtitle: {
            text: 'Click the columns to view reason. Source: <a href="https://pearl-rail.com/" target="_blank">pearl-rail.com</a>'
        },
        xAxis: {
            type: 'category'
        },
        yAxis: {
            title: {
                text: 'Total Containers'
            }

        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                borderWidth: 0,
                dataLabels: {
                    enabled: true,
                    format: '{point.y:.0f}'
                }
            }
        },

        tooltip: {
            headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
            pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.0f}</b> Containers<br/>'
        },

        "series": [
            {
                "name": "Months",
                "colorByPoint": true,
                "data": JSON.parse(reasonPerMonthReason1)
            }
        ],
        "drilldown": {
            "series": JSON.parse(reasonPerMonthReasonDrillDown)
        }
    });


}




function transferdTEUPerMonthFrom() {

    var reasonPerMonthReason1 = (document.getElementById('valuesForm:reasonPerMonthReason1').value);
    var reasonPerMonthReasonDrillDown = (document.getElementById('valuesForm:reasonPerMonthReason2').value);

    console.log(JSON.parse(reasonPerMonthReason1));
    console.log(JSON.parse(reasonPerMonthReasonDrillDown));


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            type: 'column',
            renderTo: 'container',

        },
        title: {
            text: 'Transferred TEU per Month / Station From'
        },
        subtitle: {
            text: 'Click the columns to view reason. Source: <a href="https://pearl-rail.com/" target="_blank">pearl-rail.com</a>'
        },
        xAxis: {
            type: 'category'
        },
        yAxis: {
            title: {
                text: 'Total TEU'
            }

        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                borderWidth: 0,
                dataLabels: {
                    enabled: true,
                    format: '{point.y:.0f}'
                }
            }
        },

        tooltip: {
            headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
            pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.0f}</b> TEU<br/>'
        },

        "series": [
            {
                "name": "Months",
                "colorByPoint": true,
                "data": JSON.parse(reasonPerMonthReason1)
            }
        ],
        "drilldown": {
            "series": JSON.parse(reasonPerMonthReasonDrillDown)
        }
    });


}


function transferdTEUPerMonthTo() {

    var reasonPerMonthReason1 = (document.getElementById('valuesForm:reasonPerMonthReason1').value);
    var reasonPerMonthReasonDrillDown = (document.getElementById('valuesForm:reasonPerMonthReason2').value);

    console.log(JSON.parse(reasonPerMonthReason1));
    console.log(JSON.parse(reasonPerMonthReasonDrillDown));


    var missionPerMonth = new Highcharts.Chart({
        chart: {
            type: 'column',
            renderTo: 'container',

        },
        title: {
            text: 'Transferred TEU per Month / Station To'
        },
        subtitle: {
            text: 'Click the columns to view reason. Source: <a href="https://pearl-rail.com/" target="_blank">pearl-rail.com</a>'
        },
        xAxis: {
            type: 'category'
        },
        yAxis: {
            title: {
                text: 'Total TEU'
            }

        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                borderWidth: 0,
                dataLabels: {
                    enabled: true,
                    format: '{point.y:.0f}'
                }
            }
        },

        tooltip: {
            headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
            pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.0f}</b> TEU<br/>'
        },

        "series": [
            {
                "name": "Months",
                "colorByPoint": true,
                "data": JSON.parse(reasonPerMonthReason1)
            }
        ],
        "drilldown": {
            "series": JSON.parse(reasonPerMonthReasonDrillDown)
        }
    });


}

function wagonKMPerMonth() {

    var k = (document.getElementById('valuesForm:wagonKMPerMonth').value);

    console.log(k);

    var temp = [];
    temp = k.split("|");

    var finalData = [];
    for (j in temp) {
        var k = [];
        k = temp[j].split(",");
        k[1] = parseInt(k[1]);
        finalData.push((k));
    }


    var kmPerMonth = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'column'
        },
        title: {
            text: 'Total Wagon Km per Month'
        },
        subtitle: {
            text: 'Source: <a href="https://pearl-rail.com/">Pearl</a>'
        },
        xAxis: {
            type: 'category',
            labels: {
                rotation: -45,
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Kilometers'
            }
        },
        legend: {
            enabled: false
        },
        tooltip: {
            pointFormat: 'Km in month: <b>{point.y:.0f} </b>'
        },
        series: [{
            name: 'Total Wagon Km per Month',
            data: finalData,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });
}

function missionKMPerMonth() {

    var k = (document.getElementById('valuesForm:missionKMPerMonth').value);

    console.log(k);

    var temp = [];
    temp = k.split("|");

    var finalData = [];
    for (j in temp) {
        var k = [];
        k = temp[j].split(",");
        k[1] = parseInt(k[1]);
        finalData.push((k));
    }


    var miskmPerMonth = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
            type: 'column'
        },
        title: {
            text: 'Total Mission Km per Month'
        },
        subtitle: {
            text: 'Source: <a href="https://pearl-rail.com/">Pearl</a>'
        },
        xAxis: {
            type: 'category',
            labels: {
                rotation: -45,
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Kilometers'
            }
        },
        legend: {
            enabled: false
        },
        tooltip: {
            pointFormat: 'Km in month: <b>{point.y:.0f} </b>'
        },
        series: [{
            name: 'Total Mission Km per Month',
            data: finalData,
            dataLabels: {
                enabled: true,
                rotation: -90,
                color: '#FFFFFF',
                align: 'right',
                format: '{point.y:.0f}', // one decimal
                y: 10, // 10 pixels down from the top
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                }
            }
        }]
    });
}

function payloadPerMonth() {

    var payloadPerMonthCategories = (document.getElementById('valuesForm:payloadPerMonth').value);
    var payloadPerMonthGW = (document.getElementById('valuesForm:payloadPerMonthGW').value);
    var payloadPerMonthTare = (document.getElementById('valuesForm:payloadPerMonthTare').value);

    //Για να μετατρέχουμε τα String σε Integers
    var tempGW = payloadPerMonthGW.split(",");
    var finalGW = [];
    for (j in tempGW) {
        finalGW.push((parseInt(tempGW[j])));
    }

    //Για να μετατρέχουμε τα String σε Integers
    // var tempSouthBound = [];
    var tempTare = payloadPerMonthTare.split(",");
    var finalTare = [];
    for (j in tempTare) {
        finalTare.push((parseInt(tempTare[j])));
    }

    var payloadPerMonth1 = new Highcharts.Chart({

        chart: {
            type: 'column',
            renderTo: 'container'
        },

        title: {
            text: 'Payload per month'
        },

        xAxis: {
            categories: payloadPerMonthCategories.split(",")
        },

        yAxis: {
            allowDecimals: false,
            min: 0,
            title: {
                text: 'Payload in Tons'
            },
            stackLabels: {
                enabled: true,
                style: {
                    fontWeight: 'bold',
                    color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
                }
            }
        },

        tooltip: {
            formatter: function () {
                return '<b>' + this.x + '</b><br/>' +
                    this.series.name + ': ' + this.y + '<br/>' +
                    'Total: ' + this.point.stackTotal;
            }
        },

        plotOptions: {
            column: {
                stacking: 'normal',
                dataLabels: {
                    enabled: true,
                    color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white'
                }
            }
        },

        series: [{
            name: 'Gross Weight',
            data: finalGW,
            color: '#2C8AF2'
        }, {
            name: 'Tare Weight',
            data: finalTare,
            color: '#008000'
        }]
    });
}


//Histock dummy data for testing and for
// var data1 = [
//     ["07 December 2017 00:00","2"],
//     ["23 February 2018 00:00","1"],
//     ["11 March 2018 00:00","1"],
//     ["11 May 2018 00:00","1"],
//     ["24 May 2018 00:00","1"],
//     ["17 June 2018 00:00","1"],
//     ["01 July 2018 00:00","1"],
//     ["09 July 2018 00:00","1"],
//     ["10 July 2018 00:00","1"],
//     ["11 July 2018 00:00","1"],
//     ["13 July 2018 00:00","2"],
//     ["16 July 2018 00:00","1"],
//     ["17 July 2018 00:00","1"],
//     ["19 July 2018 00:00","1"],
//     ["20 July 2018 00:00","1"],
//     ["22 July 2018 00:00","1"],
//     ["26 July 2018 00:00","5"],
//     ["27 July 2018 00:00","1"],
//     ["30 July 2018 00:00","2"],
//     ["03 August 2018 00:00","1"],
//     ["06 August 2018 00:00","3"],
//     ["09 August 2018 00:00","2"],
//     ["11 August 2018 00:00","1"],
//     ["13 August 2018 00:00","2"],
//     ["15 August 2018 00:00","1"],
//     ["16 August 2018 00:00","2"],
//     ["17 August 2018 00:00","4"],
//     ["20 August 2018 00:00","1"],
//     ["21 August 2018 00:00","3"],
//     ["22 August 2018 00:00","2"],
//     ["23 August 2018 00:00","3"],
//     ["26 August 2018 00:00","1"],
//     ["29 August 2018 00:00","1"],
//     ["31 August 2018 00:00","1"],
//     ["01 September 2018 00:00","1"],
//     ["03 September 2018 00:00","4"],
//     ["06 September 2018 00:00","6"],
//     ["09 September 2018 00:00","6"],
//     ["11 September 2018 00:00","4"],
//     ["13 September 2018 00:00","1"],
//     ["14 September 2018 00:00","3"],
//     ["15 September 2018 00:00","1"],
//     ["16 September 2018 00:00","2"],
//     ["20 September 2018 00:00","1"],
//     ["21 September 2018 00:00","1"],
//     ["22 September 2018 00:00","2"],
//     ["23 September 2018 00:00","1"],
//     ["24 September 2018 00:00","2"],
//     ["26 September 2018 00:00","3"],
//     ["27 September 2018 00:00","2"],
//     ["02 October 2018 00:00","1"],
//     ["03 October 2018 00:00","1"],
//     ["12 October 2018 00:00","2"],
//     ["13 October 2018 00:00","1"],
//     ["14 October 2018 00:00","2"],
//     ["15 October 2018 00:00","1"],
//     ["17 October 2018 00:00","1"],
//     ["18 October 2018 00:00","4"],
//     ["22 October 2018 00:00","3"],
//     ["25 October 2018 00:00","3"],
//     ["30 October 2018 00:00","1"],
//     ["31 October 2018 00:00","2"],
//     ["01 November 2018 00:00","1"],
//     ["03 November 2018 00:00","4"],
//     ["04 November 2018 00:00","1"],
//     ["05 November 2018 00:00","2"],
//     ["07 November 2018 00:00","1"],
//     ["08 November 2018 00:00","1"],
//     ["10 November 2018 00:00","1"],
//     ["11 November 2018 00:00","1"],
//     ["14 November 2018 00:00","8"],
//     ["18 November 2018 00:00","3"],
//     ["19 November 2018 00:00","2"],
//     ["20 November 2018 00:00","1"],
//     ["21 November 2018 00:00","4"],
//     ["24 November 2018 00:00","1"],
//     ["25 November 2018 00:00","3"],
//     ["27 November 2018 00:00","4"],
//     ["03 December 2018 00:00","2"],
//     ["04 December 2018 00:00","1"],
//     ["05 December 2018 00:00","3"],
//     ["07 December 2018 00:00","1"],
//     ["13 December 2018 00:00","1"],
//     ["14 December 2018 00:00","1"],
//     ["17 December 2018 00:00","2"],
//     ["18 December 2018 00:00","4"],
//     ["19 December 2018 00:00","3"],
//     ["20 December 2018 00:00","2"],
//     ["21 December 2018 00:00","2"],
//     ["22 December 2018 00:00","1"],
//     ["28 December 2018 00:00","7"],
//     ["02 January 2019 00:00","1"],
//     ["06 January 2019 00:00","1"],
//     ["09 January 2019 00:00","2"],
//     ["10 January 2019 00:00","1"],
//     ["11 January 2019 00:00","2"],
//     ["12 January 2019 00:00","2"],
//     ["14 January 2019 00:00","7"],
//     ["15 January 2019 00:00","1"],
//     ["16 January 2019 00:00","1"],
//     ["17 January 2019 00:00","4"],
//     ["18 January 2019 00:00","2"],
//     ["20 January 2019 00:00","2"],
//     ["21 January 2019 00:00","10"],
//     ["22 January 2019 00:00","1"],
//     ["24 January 2019 00:00","5"],
//     ["25 January 2019 00:00","1"],
//     ["26 January 2019 00:00","4"],
//     ["28 January 2019 00:00","2"],
//     ["29 January 2019 00:00","2"],
//     ["02 February 2019 00:00","1"],
//     ["03 February 2019 00:00","5"],
//     ["04 February 2019 00:00","2"],
//     ["05 February 2019 00:00","4"],
//     ["06 February 2019 00:00","3"],
//     ["08 February 2019 00:00","2"],
//     ["09 February 2019 00:00","3"],
//     ["10 February 2019 00:00","4"],
//     ["11 February 2019 00:00","4"],
//     ["12 February 2019 00:00","3"],
//     ["19 February 2019 00:00","2"]
// ];




