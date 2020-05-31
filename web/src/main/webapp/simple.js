function logger(log) {
    console.log(log);
}


function loadAllBoards() {
    const ajax = fs.ajax()
        //.POST("/rest/workspaces")
        //.GET("/rest/simple/id/"+uuid)
        .GET("http://hakobune.local:8080/rest/simple/")
        .accept("application/json")
        .handle(200, function (xhr, request) {
            const boards = JSON.parse(xhr.responseText);
            for (let i = 0; i < boards.length; i++) {
                loadBoard(boards[i]);
            }
            setTimeout(function () {
                loadAllBoards();
            }, 1000);
        })
        .handleOthers(function (xhr, request) {
            logger(xhr.status.toString() + " Failed to fetch boards: " + xhr.statusText + '\n' + xhr.responseText);
            setTimeout(function () {
                loadAllBoards();
            }, 1000);
            // TODO: Recover the UI state somehow.
        })
        .json();
}


function loadBoard(uuid) {
    //logger("Loading board " + uuid)
    const ajax = fs.ajax()
        //.POST("/rest/workspaces")
        //.GET("/rest/simple/id/"+uuid)
        .GET("http://hakobune.local:8080/rest/simple/id/" + uuid)
        .accept("application/json")
        .handle(200, function (xhr, request) {
            //window.location.hash = query;
            const aliasView = JSON.parse(xhr.responseText);
            //toggleOff("#controls");
            populateToggles(getControls(uuid), uuid, aliasView);
            toggleOn("#" + getControlsId(uuid)); // TODO : Activate Section.
        })
        .handleOthers(function (xhr, request) {
            logger(xhr.status.toString() + " Failed to load board " + uuid + ": " + xhr.statusText + '\n' + xhr.responseText);
            // TODO: Recover the UI state somehow.
        })
        .json();
}

function loadVEDirectDevices() {
    logger("Loading devices...")
    const ajax = fs.ajax()
        //.POST("/rest/workspaces")
        //.GET("/rest/vedirect/device/")
        .GET("http://hakobune.local:8080/rest/vedirect/device/")
        .accept("application/json")
        .handle(200, function (xhr, request) {
            const devices = JSON.parse(xhr.responseText);
            for (let i = 0; i < devices.length; i++) {
                loadVEDirectDevice(devices[i]['serialNumber'], devices[i]['type']);
            }
            setTimeout(function () {
                loadVEDirectDevices();
            }, 1000);
        })
        .handleOthers(function (xhr, request) {
            logger(xhr.status.toString() + " Failed to load VEDirectDevices: " + xhr.statusText + '\n' + xhr.responseText);
            setTimeout(function () {
                loadVEDirectDevices();
            }, 1000);
        })
        .json();
}

function loadVEDirectDevice(serialNumber, type) {
    logger("Loading VEDirect Device Serial: " + serialNumber);
    const ajax = fs.ajax()
        //.POST("/rest/workspaces")
        //.GET("/rest/vedirect/device/" + serialNumber)
        .GET("http://hakobune.local:8080/rest/vedirect/device/" + serialNumber)
        .accept("application/json")
        .handle(200, function (xhr, request) {
            const controllerView = JSON.parse(xhr.responseText);
            loadControllerView(serialNumber, controllerView);
        })
        .handleOthers(function (xhr, request) {
            logger(xhr.status.toString() + " Failed to load VEDirectDevice " + serialNumber + ": "
                + xhr.statusText + '\n' + xhr.responseText);
        })
        .json();
}


function setState(uuid, key, state) {
    const payload = {};
    payload[key] = state;
    const ajax = fs.ajax()
        .POST("http://hakobune.local:8080/rest/simple/id/" + uuid)
        //.POST("/rest/simple/id/"+uuid)
        .accept("application/json")
        .handle(200, function (xhr, request) {
            //window.location.hash = query;
            const state = JSON.parse(xhr.responseText);
            toggleOff("#controls");
            populateToggles(document.getElementById("controls"), uuid, state);

            toggleOn(".controls"); // TODO : Activate Section.
        })
        .handleOthers(function (xhr, request) {
            logger(xhr.status.toString() + " Failed to fetch woirkspaces: " + xhr.statusText + '\n' + xhr.responseText);
            /*
            document.allElements(".searchInput", function(elem) {
              removeClass(elem, "searching");
            });
            */
            // TODO: Recover the UI state somehow.
        })
        .json(payload);
}

function getControlsId(uuid) {
    return "controls-" + uuid;
}

function getControls(uuid) {
    let controlsDiv = document.getElementById(getControlsId(uuid));
    if (controlsDiv == null) {
        controlsDiv = document.createElement("div");
        getTabsContainer().appendChild(controlsDiv);
    }
    return controlsDiv;
}

function getTabsContainer() {
    return document.getElementById("tabs");
}

function createButton(uuid, key, initialValue) {
    const elementId = uuid + "_" + key;
    let existing = document.getElementById(elementId);
    if (existing === null) {
        console.log("Creating control for " + key);
        const checkbox = document.createElement("input");
        checkbox.setAttribute("type", "checkbox");
        checkbox.checked = initialValue;
        checkbox.id = elementId;
        const text = document.createElement("strong");
        text.textContent = key;
        text.setAttribute("style", "color:white");

        const container = document.createElement("label");
        addClass(container, "switch-light switch-holo");

        const span = document.createElement("span");
        const on = document.createElement("span");
        on.textContent = "On";
        const off = document.createElement("span");
        off.textContent = "Off";
        span.appendChild(off);
        span.appendChild(on);

        span.appendChild(document.createElement("a"));

        container.setAttribute("style", "display:inline-block;margin: 10px;width: 200px;");

        container.appendChild(checkbox);
        container.appendChild(text);
        container.appendChild(span);

        container.addEventListener("click", function (evt) {
            //console.log(key, checkbox.checked);
            evt.preventDefault();
            evt.stopPropagation();
            setState(uuid, key, !document.getElementById(elementId).checked);
            return false;
        });
        return container;
    } else {
        if (existing.checked != initialValue) {
            console.log("Setting existing control " + key + " to " + initialValue);
            existing.checked = initialValue;
        }
        return null;
    }
}

function getGroup(controlsPane, key) {
    let groupName = key;
    const parts = key.split("_");
    if (parts.length === 2) {
        groupName = key[0];
    }
    groupName = "group_" + groupName;

    let group = document.getElementById(groupName);
    if (group == null) {
        group = document.createElement("div");
        group.id = groupName;
        group.setAttribute("class", "controlGroup");
        controlsPane.appendChild(group);
    }

    return group;
}


function populateToggles(controlsPane, uuid, aliasView) {
    const state = aliasView['applianceStates'];
    let button;
    for (let key in state) {
        button = createButton(uuid, key, state[key]);
        if (button !== null) {
            getGroup(controlsPane, key).appendChild(button);
        }
    }
}

function getCharts() {
    return document.getElementById("charts");
}



function loadControllerView(serialNumber, controllerView) {
    const controllerTime = controllerView['controllerTime'];
    const controllerState = controllerView['controllerState'];
    const controllerHistory = controllerView['controllerHistory'];
    //loadControllerTimeRadar(serialNumber, controllerTime);
    loadControllerTimeDoughnut(serialNumber, "controller-time", "Controller Time", controllerTime['data']);

    loadControllerGauge(serialNumber, controllerState['mainVoltage'], controllerHistory['maxMainVoltage'],
        "main-voltage", "Main Voltage", "V");

    loadControllerGauge(serialNumber, controllerState['panelVoltage'], controllerHistory['maxPanelVoltage'],
        "pv-voltage", "PV Voltage", "V");
    loadControllerGauge(serialNumber, controllerState['panelPower'], controllerHistory['maxPanelPower'],
        "pv-power", "PV Power", "W");

}

/*
function loadControllerTimeRadar(serialNumber, controllerTime) {
    const canvasId = "controllerTime-radar-"+serialNumber;
    let canvas = document.getElementById(canvasId);
    if (canvas == null) {
        canvas = document.createElement("canvas");
        canvas.id = canvasId;
        getCharts().appendChild(canvas);
    }

    const color = Chart.helpers.color;
    console.log(controllerTime);
    const data  =  controllerTime['data'];
    const total = Object.values(data).reduce((a, b) => a + b, 0);
    const secondsPerUnit = total / controllerTime['secondsPerUnit'];
    const unit  = controllerTime['unit'];
    const scaledValues = Object.values(data).map((a) => a / secondsPerUnit);

    const config = {
        type: 'radar',
        data: {
            labels: Object.keys(controllerTime['data']),
            datasets: [{
                label: serialNumber,
                backgroundColor: color(window.chartColors.red).alpha(0.2).rgbString(),
                borderColor: window.chartColors.red,
                pointBackgroundColor: window.chartColors.red,
                data: scaledValues
            }]
        },
        options: {
            legend: {
                position: 'top',
            },
            title: {
                display: true,
                text: 'Controller Time, ' + unit
            },
            scale: {
                ticks: {
                    beginAtZero: true
                }
            }
        }
    };
    console.log(config);

    if (typeof(canvas.dataset['chart']) == "undefined") {
        logger("Creating new canvas");
        canvas.dataset['chart'] = new Chart(canvas, config);
    } else {
        logger("Updating existing canvas");
        canvas.dataset['chart'].options.data = config.data;
        canvas.dataset['chart'].update();
    }
}
*/


function loadControllerTimeDoughnut(serialNumber, name, title, cmap) {
    const divId = "controller-"+name+"-plotly-"+serialNumber;
    let div = document.getElementById(divId);
    if (div == null) {
        div = document.createElement("div");
        div.id = divId;
        getCharts().appendChild(div);
    }

    var data = [{
        values: Object.values(cmap),
        labels: Object.keys(cmap),
        //domain: {column: 0},
        name: serialNumber,
        hoverinfo: 'label+percent+name',
        hole: .7,
        type: 'pie'
    }];

    var layout = {
        title: {
            text: title,
            font: {
                size: 20,
                color: "#FFF"
            }
        },
        height: 300,
        width: 300,
        showlegend: true,
        legend: {
            font: {
                color: "#FFF"
            },
            x: 1.5,
            xanchor: 'right',
            y: -.5
        },


        plot_bgcolor: "rgba(0,0,0,0)",
        paper_bgcolor: "rgba(0,0,0,0)",
        bgcolor: "rgba(0,0,0,0)"
        //grid: {rows: 1, columns: 2}
    };

    const config = {
        responsive: true,
        displayModeBar: false
    };

    if (typeof(div.dataset['chart']) == "undefined") {
        div.dataset['chart'] = true;
        Plotly.newPlot(div.id, data, layout, config);
    } else {
        Plotly.react(div.id, data, layout, config);
    }
}


function loadControllerGauge(serialNumber, currentValue, maxValue, name, title, suffix) {
    const divId = "controller-"+name+"-plotly-"+serialNumber;
    let div = document.getElementById(divId);
    if (div == null) {
        div = document.createElement("div");
        div.id = divId;
        getCharts().appendChild(div);
    }

    const data = [
        {
            domain: { x: [0, 1], y: [0, 1] },
            value: currentValue,
            title: {
                text: title,
                font: {
                    size: 20,
                    color: "#FFF"
                }
            },
            type: "indicator",
            mode: "gauge+number+delta",
            number: {
                font: {
                    size: 18,
                    color: "#FFF"
                },
                suffix: suffix
            },
            delta: {
                reference: maxValue, // TODO Previous known state
                font: {
                    size: 14
                },
                suffix:  suffix
            },
            gauge: {
                bordercolor: "#FFF",
                bar: {
                    color: "rgba(255,255,255,0.8)",
                    line: {
                        color: "#FFF",
                        width: 1
                    }
                },
                axis:
                    {
                        range: [null, maxValue]
                    }
            }
        }
    ];

    const layout = {
        width: 300,
        height: 300,

        plot_bgcolor: "rgba(0,0,0,0)",
        paper_bgcolor: "rgba(0,0,0,0)",
        bgcolor: "rgba(0,0,0,0)"
    };

    const config = {
        responsive: true,
        displayModeBar: false
    };

    if (typeof(div.dataset['chart']) == "undefined") {
        div.dataset['chart'] = true;
        Plotly.newPlot(div.id, data, layout, config);
    } else {
        Plotly.react(div.id, data, layout, config);
    }
}


var randomScalingFactor = function() {
    return Math.round(Math.random() * 100);
};


window.chartColors = {
    red: 'rgb(255, 99, 132)',
    orange: 'rgb(255, 159, 64)',
    yellow: 'rgb(255, 205, 86)',
    green: 'rgb(75, 192, 192)',
    blue: 'rgb(54, 162, 235)',
    purple: 'rgb(153, 102, 255)',
    grey: 'rgb(201, 203, 207)'
};

window.onload = function() {
    console.log("Loading");
    loadAllBoards();
    loadVEDirectDevices();
};
