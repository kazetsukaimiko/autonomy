function logger(log) {
    console.log(log);
}

function loadWorkspaces(uuid) {
  var ajax = fs.ajax()
    //.POST("/rest/workspaces")
    .GET("http://localhost:18080/rest/simple/id/"+uuid)
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
    .json();
}

function setState(uuid, key, state) {
  var payload = {};
  payload[key] = state;
  var ajax = fs.ajax()
    //.POST("/rest/workspaces")
    .POST("http://localhost:18080/rest/simple/id/"+uuid)
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
    if (existing) {
        existing.checked = initialValue;
        return false;
    } else {
        var checkbox = document.createElement("input");
        checkbox.setAttribute("type", "checkbox");
        checkbox.checked = initialValue;
        checkbox.addEventListener("change", function(evt) {
            console.log(key, checkbox.checked);
            setState(uuid, key, checkbox.checked);
        });
        var text = document.createElement("span");
        text.textContent = key;

        var container = document.createElement("div");
        container.appendChild(text);
        container.appendChild(checkbox);
        return container;
    }
}

function populateToggles(controlsPane, uuid, state) {
    for(key in state) {
        button = createButton(uuid, key, state[key]);
        if (button) {
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
