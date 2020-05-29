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
    logger("Loading board " + uuid)
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
            setTimeout(function () {
                loadWorkspaces(uuid);
            }, 1000);
        })
        .handleOthers(function (xhr, request) {
            logger(xhr.status.toString() + " Failed to load board " + uuid + ": " + xhr.statusText + '\n' + xhr.responseText);
            setTimeout(function () {
                loadWorkspaces(uuid);
            }, 1000);
            // TODO: Recover the UI state somehow.
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

//function


window.onload = function() {
    console.log("Loading");
    loadAllBoards();
};
