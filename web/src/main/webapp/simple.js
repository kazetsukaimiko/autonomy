function logger(log) {
    console.log(log);
}

function loadWorkspaces(uuid) {
  var ajax = fs.ajax()
    //.POST("/rest/workspaces")
    .GET("/rest/simple/id/"+uuid+"/setup")
    .accept("application/json")
    .handle(200, function(xhr, request) {
      //window.location.hash = query;
      var aliasView = JSON.parse(xhr.responseText);
      toggleOff("#controls");
      populateToggles(document.getElementById("controls"), uuid, aliasView);

      toggleOn(".controls"); // TODO : Activate Section.
    })
    .handleOthers(function(xhr, request) {
      logger(xhr.status.toString() + " Failed to fetch woirkspaces: " + xhr.statusText + '\n' + xhr.responseText);
      /*
      document.allElements(".searchInput", function(elem) {
        removeClass(elem, "searching");
      });
      */
      // TODO: Recover the UI state somehow.
    })
    .json();
}

function setState(uuid, key, state) {
  var payload = {};
  payload[key] = state;
  var ajax = fs.ajax()
    //.POST("/rest/workspaces")
    .POST("/rest/simple/id/"+uuid)
    .accept("application/json")
    .handle(200, function(xhr, request) {
      //window.location.hash = query;
      var state = JSON.parse(xhr.responseText);
      toggleOff("#controls");
      populateToggles(document.getElementById("controls"), uuid, state);

      toggleOn(".controls"); // TODO : Activate Section.
    })
    .handleOthers(function(xhr, request) {
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


function createButton(uuid, key, initialValue) {
    var elementId = uuid + "_" + key;
    var existing = document.getElementById(elementId);
    if (existing === null) {
        console.log("Creating control for " + key);
        var checkbox = document.createElement("input");
        checkbox.setAttribute("type", "checkbox");
        checkbox.checked = initialValue;
        checkbox.id = elementId;
        var text = document.createElement("strong");
        text.textContent = key;
        text.setAttribute("style", "color:white");

        var container = document.createElement("label");
        addClass(container, "switch-light switch-holo");

        var span = document.createElement("span");
        var on = document.createElement("span");
        on.textContent = "On";
        var off = document.createElement("span");
        off.textContent = "Off";
        span.appendChild(off);
        span.appendChild(on);

        span.appendChild(document.createElement("a"));

        container.setAttribute("style", "display:inline-block;margin: 10px;width: 200px;");

        container.appendChild(checkbox);
        container.appendChild(text);
        container.appendChild(span);

        container.addEventListener("click", function(evt) {
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

function populateToggles(controlsPane, uuid, aliasView) {
    var state = aliasView.applianceStates;
    for(key in state) {
        button = createButton(uuid, key, state[key]);
        if (button !== null) {
            controlsPane.appendChild(button);
        }
    }
}

window.onhashchange = function() {
    console.log("HashChange");
    document.getElementById("controls").empty();
    loadWorkspaces(window.location.hash.substr(1));
}

window.onload = function() {
    var uuid = "0f2829e1-1804-4993-ae33-c5dd21840646";
    console.log("Loading");
    if (window.location.hash.substr(1) != uuid) {
        console.log(window.location.hash);
        window.location.hash = uuid;
    } else {
        window.onhashchange();
    }
};
