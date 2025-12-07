// Create the chart
Highcharts.chart('container', {
    chart: {
        type: 'column'
    },
    title: {
        text: 'Browser market shares. January, 2018'
    },
    subtitle: {
        text: 'Click the columns to view versions. Source: <a href="http://statcounter.com" target="_blank">statcounter.com</a>'
    },
    xAxis: {
        type: 'category'
    },
    yAxis: {
        title: {
            text: 'Total percent market share'
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
                format: '{point.y:.1f}%'
            }
        }
    },

    tooltip: {
        headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
        pointFormat: '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.2f}%</b> of total<br/>'
    },

    "series": [
        {
            "name": "Browsers",
            "colorByPoint": true,
            "data": [

                {
                    "name": " December 2017 ",
                    "y": 2,
                    "drilldown": " December 2017 "
                },
                {
                    "name": " February 2018 ",
                    "y": 1,
                    "drilldown": " February 2018 "
                },
                {
                    "name": " March 2018 ",
                    "y": 1,
                    "drilldown": " March 2018 "
                },
                {
                    "name": " May 2018 ",
                    "y": 2,
                    "drilldown": " May 2018 "
                },
                {
                    "name": " June 2018 ",
                    "y": 1,
                    "drilldown": " June 2018 "
                },
                {
                    "name": " July 2018 ",
                    "y": 26,
                    "drilldown": " July 2018 "
                },
                {
                    "name": " August 2018 ",
                    "y": 83,
                    "drilldown": " August 2018 "
                },
                {
                    "name": " September 2018 ",
                    "y": 111,
                    "drilldown": " September 2018 "
                },
                {
                    "name": " October 2018 ",
                    "y": 252,
                    "drilldown": " October 2018 "
                },
                {
                    "name": " November 2018 ",
                    "y": 157,
                    "drilldown": " November 2018 "
                },
                {
                    "name": " December 2018 ",
                    "y": 81,
                    "drilldown": " December 2018 "
                },
                {
                    "name": " January 2019 ",
                    "y": 161,
                    "drilldown": " January 2019 "
                },
                {
                    "name": " February 2019 ",
                    "y": 101,
                    "drilldown": " February 2019 "
                }

            ],
            "drilldown": {
                "series": [

                    {
                        "name": " December 2017 ",
                        "id": " December 2017 ",
                        "data": [["Damaged Wagon",2]]
                    },
                    {
                        "name": " February 2018 ",
                        "id": " February 2018 ",
                        "data": [["Damaged Wagon",1]]
                    },
                    {
                        "name": " March 2018 ",
                        "id": " March 2018 ",
                        "data": [["Damaged Wagon",1]]
                    },
                    {
                        "name": " May 2018 ",
                        "id": " May 2018 ",
                        "data": [["Damaged Wagon",2]]
                    },
                    {
                        "name": " June 2018 ",
                        "id": " June 2018 ",
                        "data": [["Damaged Wagon",1]]
                    },
                    {
                        "name": " July 2018 ",
                        "id": " July 2018 ",
                        "data": [["Damaged Wagon",18], ["Vet Inspection",2]]
                    },
                    {
                        "name": " August 2018 ",
                        "id": " August 2018 ",
                        "data": [["Other",6], ["Damaged Wagon",28], ["Missing/ Damaged Seal",4], ["Vet Inspection",2]]
                    },
                    {
                        "name": " September 2018 ",
                        "id": " September 2018 ",
                        "data": [["Missing/ Damaged Seal",5], ["Damaged Wagon",38], ["Vet Inspection",2], ["Other",2]]
                    },
                    {
                        "name": " October 2018 ",
                        "id": " October 2018 ",
                        "data": [["Other",16], ["Missing/ Damaged Seal",18], ["Damaged Wagon",23]]
                    },
                    {
                        "name": " November 2018 ",
                        "id": " November 2018 ",
                        "data": [["Damaged Wagon",43], ["Other",6], ["Missing/ Damaged Seal",3]]
                    },
                    {
                        "name": " December 2018 ",
                        "id": " December 2018 ",
                        "data": [["Other",4], ["Damaged Wagon",30], ["Missing/ Damaged Seal",1], ["Customs Inspection",8], ["Vet Inspection",1]]
                    },
                    {
                        "name": " January 2019 ",
                        "id": " January 2019 ",
                        "data": [["Damaged Wagon",52], ["Weight Restriction",16], ["Customs Inspection",11]]
                    },
                    {
                        "name": " February 2019 ",
                        "id": " February 2019 ",
                        "data": [["Weight Restriction",12], ["Other",5], ["Damaged Wagon",37], ["Border Police Inspection",6], ["Customs Inspection",3]]
                    }

                ]
            }
        }
    ]
});