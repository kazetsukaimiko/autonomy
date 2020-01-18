function logger(log) {
    console.log(log);
}

function loadWorkspaces() {
  var ajax = fs.ajax()
    //.POST("/rest/workspaces")
    .GET("http://localhost:8080/rest/workspaces")
    .accept("application/json")
    .handle(200, function(xhr, request) {
      //window.location.hash = query;
      var workspaces = JSON.parse(xhr.responseText);
      toggleOff(".content > .pane");
      populateWorkspacePane(document.getElementById("workspacePane"), workspaces);

      toggleOn(".content > #workspacePane"); // TODO : Activate Section.
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


function createWorkspaceDom(workspaceEntity) {
    var workspaceDom = document.createElement("div");
    workspaceDom.className = "entity workspace";
    workspaceDom.id = workspaceEntity.uuid;
    var view = document.createElement("div");

    if (workspaceEntity.active) {
        addClass(workspaceDom, "active");
    }

    var name = document.createElement("div");
    name.textContent = workspaceEntity.currentName;

    view.appendChild(name);
    workspaceDom.appendChild(view);

    // TODO: Concerns

    return workspaceDom;
}


function compareVersions(v1, v2) {
    return firstNon([
        nullCompare(v1, v2),
        compareIntegers(v1.major, v2.major),
        compareIntegers(v1.minor, v2.minor),
        compareIntegers(v1.micro, v2.micro)], 0);
}

function workspaceView(existingView, workspace) {
    if (existingView == null) {
        existingView = {};
    }

    if (existingView.currentName == null || existingView.currentVersion == null || compareVersions(workspace.version, existingView.currentVersion) > 0) {
        existingView.currentName = workspace.name;
        existingView.currentVersion = workspace.version;
    }
    return existingView;
}

function workspaceViewsByUUID(workspaces) {
    var workspaceViews = {};
    for( var i=0; i<workspaces.length; i++) {
        var uuid = workspaces[i].uuid;
        workspaceViews[uuid] = workspaceView(workspaceViews[uuid], workspaces[i]);
    }
    return workspaceViews;
}

function populateWorkspacePane(workspacePane, workspaces) {
    var title = document.createElement("div");
    title.textContent = "Workspaces";
    title.className = "title";
    var list = document.createElement("div");
    list.className = "list";

    var workspaceViews = workspaceViewsByUUID(workspaces);

    console.log(workspaces);
    console.log(workspaceViews);
    for( uuid in workspaceViews ) {
        list.appendChild(createWorkspaceDom(workspaceViews[uuid]));
    }

    workspacePane.empty();
    workspacePane.appendChild(title);
    workspacePane.appendChild(list);
    // workspacePane.appendChild(creator); // TODO : Creator
}


function init() {
  loadWorkspaces();
}

/*
function startApplication() {
  window.addEventListener("hashchange", function(evt) {
    hashEvent();
  });
  if (window.location.hash != "") {
    hashEvent();
  }
}

function hashEvent() {
  if (hashHistory(window.location.hash)) {
    document.allElements(".searchInput", function(elem) {
      elem.value = window.location.hash.substr(1);
    });
    runSearch(window.location.hash.substr(1));
  }
}

function hashHistory(newHash) {
  var hashHistoryId = "hashHistory";
  var hashHistoryDOM = document.getElementById(hashHistoryId);
  if (hashHistoryDOM == null) {
    hashHistoryDOM = makeDOM(hashHistoryId);
    hashHistoryDOM.dataset['value'] = "";
    document.getElementById("body").appendChild(hashHistoryDOM);
  }
  if (hashHistoryDOM.dataset['value'] == newHash) {
    return false;
  }  hashHistoryDOM.dataset['value'] = newHash; return true;
}

function populate() {
  logger("Populating DOM...");
  logger("Making Wiper...");
  document.getElementById("body").appendChild(makeWiper());
  logger("Making Logo...");
  document.getElementById("body").appendChild(makeLogo());
  logger("Making Sky...");
  document.getElementById("body").appendChild(makeSky());
  logger("Making Dimmer...");
  document.getElementById("body").appendChild(makeDimmer());
  logger("Making Top Controls...");
  document.getElementById("body").appendChild(makeTopControls());

  logger("Making Panels...");
  makePanels();
}


function intro() {
  toggleOn(".snowflakeLogo");
  setTimeout(function() {
    toggleOff(".wiper");
  }, 100);
  setTimeout(function() {
    toggleOn(".wiper");
    setTimeout(function() {
      toggleOff("#logHistory");
      //toggleOn("#dropdownMenu");
      toggleOff(".snowflakeLogo");
      toggleOn(".sky");
      toggleOn(".controls");
      toggleOff(".wiper");
      document.allElements(".searchInput", function(elem) {
        elem.focus();
        startApplication();
     });

    }, 1000);
  }, 3000);
}


function loadLogger() {
  var debugDOM = document.getElementById("logger");
  if (debugDOM == null) {
    var notifyDOM = makeDOM("DIV", "logger");
    notifyDOM.messages = [];
    document.getElementById("body").appendChild(notifyDOM);
  }
  var loggerDOM = document.getElementById("logHistory");
  if (loggerDOM == null) {
    var logHistoryDOM = makeDOM("DIV", "logHistory");
    addClass(logHistoryDOM, "apps");
    logHistoryDOM.addEventListener("dblclick", function() {
      removeClass(this, "toggle");
    });
    addClass(logHistoryDOM, "toggle");
    document.getElementById("body").appendChild(logHistoryDOM);
  }
}


function makeLogger() {
  return notifyDOM;
}

function logger(text) {
  loadLogger();
  if (typeof(text) == "string") {
    var notifyDOM = document.getElementById("logger");
    if (notifyDOM != null) {
      // Make all messages seen.
      notifyDOM.messages.push(text);
      cycleNotifications();
    }
    var loggerDOM = document.getElementById("logHistory");
    if (loggerDOM != null) {
      var logEntry = document.createElement("div");
      addClass(logEntry, "logEntry");
      logEntry.textContent = text;
      logEntry.addEventListener("click", function() {
          if (hasClass(this, "toggle")) {
            removeClass(this, "toggle");
          } else {
            if (this.scrollWidth > this.offsetWidth) {
              addClass(this, "toggle");
            }
          }
      });
      if (loggerDOM.childNodes.length > 0) {
        loggerDOM.insertBefore(logEntry, loggerDOM.childNodes[0]);
      } else {
        loggerDOM.appendChild(logEntry);
      }
    }
  }
}

function cycleNotifications() {
  loadLogger();
  var notifyDOM = document.getElementById("logger");
  if (typeof(notifyDOM.cycler) == "number") {
    notifyDOM.cycler = clearTimeout(notifyDOM.cycler);
  } 
  if (notifyDOM.messages.length > 0) {
    toggleOn("#logger");
    notifyDOM.cycler = setTimeout(function() { 
      notifyDOM.textContent = notifyDOM.messages.shift();
      cycleNotifications();
    }, 100);
  } else {
    notifyDOM.cycler = setTimeout(function() {
      toggleOff("#logger");
    }, 4000);
  }
}



// Class / Id Convention for DOM
function makeDOM(domType, name) {
  var domInstance = document.createElement(domType);
  addClass(domInstance, name);
  domInstance.id = name;
  return domInstance;
}

function rainElem(elem, dropNum, flashNum) {
  if (!(elem instanceof Element)) {
    var elem = document.body;
  }
  if (typeof(dropNum) != "number") {
    var dropNum = 800;
  }

  if (typeof(flashNum) != "number") {
    var flashNum = 0;
  }

  // Remove all Drops.
  elem.allElements(".drop", function(subElem) {
    elem.removeChild(subElem);
  });

  // Remove all Lightning flashes..
  elem.allElements(".flash", function(subElem) {
    elem.removeChild(subElem);
  });
  for( i=1;i<flashNum;i++) {
    var flash = makeDOM("div", "flash"+i);
    addClass(flash, "flash");
    elem.appendChild(addFlashFunction(flash));
  }

  for( i=1;i<dropNum;i++) {
    var dropLeft = randRange(0,1600);
    var dropTop = randRange(-1000,1400);

    //$('.rain').append('<div class="drop" id="drop'+i+'"></div>');
    var drop = makeDOM("div", "drop"+i);
    addClass(drop, "drop");
    drop.style.left = dropLeft+"px";
    drop.style.top = dropTop+"px";
    elem.appendChild(drop);
  }  

}

function flashModes() {
  return ['lit1', 'lit2', 'lit3'];
}

function addFlashFunction(flash) {
  flash.lightning = function() {
    //console.log("Lightning");

    var modes = flashModes();
    for (pos in modes) {
      removeClass(flash, modes[pos]);
    } 
    if (typeof(flash.lightningTimer) == "number") {
      flash.lightningTimer = clearTimeout(flash.lightningTimer);
    } 
    flash.lightningTimer = setTimeout(function() {
      var modes = flashModes();
      addClass(flash, randMember(modes));
      flash.lightingTimer = setTimeout(function() {
        flash.lightning();
      }, randRange(3000,18000));
    }, 100);
  }; flash.lightning();
  return flash;
}


function randomSky() {
  var items = skyModes();
  var item = items[Math.floor(Math.random()*items.length)];
  sky(item);
}

function skyModes() {
  return ['raining', 'stormy', 'day', 'sundown', 'sunset', 'yugure', 'night'];
}

function skydemo(modes) {
  if (typeof(modes) == "undefined") {
    var modes = skyModes();
  }
  if (modes.length>0) {
  
    var targetMode = modes.shift();
    sky(targetMode);
    setTimeout(function() {
      skydemo(modes);
    },5000);
  } else {
    sky();
  }
}

function sky(mode) {
  if (typeof(mode) != "string") {
    var mode = "day";
  }  
  logger("Sky: " + mode);

  var modes = skyModes();
  for (pos in modes) {
    toggleOff(".sky", modes[pos]);
  } toggleOn(".sky", mode);
}

function makeSearch() {
  var search = makeDOM("DIV", "search");
  var searchInput = makeDOM("input", "searchInput");
  searchInput.addEventListener("keyup", function(evt) {
    var evt = evt || window.event;
    var target = evt.target || evt.toElement || evt.relatedTarget;
    evt.stopPropagation();
    evt.preventDefault();
    if ((evt.keyCode == 13)) {
      runSearch(target.value);
      searchInput.blur();
    }
  });
  searchInput.setAttribute("type", "text");
  searchInput.setAttribute("autocomplete", "off");
  search.appendChild(searchInput);
  return search;
}

function createTorrentDOM(torrent) {
  var torrentDOM = document.createElement("div");
  addClass(torrentDOM, "torrent");

  // Id
  torrentDOM.id = torrent.id;

  // Source
  var sourceDOM = document.createElement("div");
  addClass(sourceDOM, "source");
  sourceDOM.textContent = torrent.source;

  // Title
  var titleDOM = document.createElement("div");
  addClass(titleDOM, "title");
  titleDOM.textContent = torrent.title;

  // Magnet
  var magnetDOM = document.createElement("a");
  addClass(magnetDOM, "magnet");
  magnetDOM.setAttribute("href", torrent.magnet);

  torrentDOM.appendChild(sourceDOM);
  torrentDOM.appendChild(titleDOM);
  torrentDOM.appendChild(magnetDOM);

  return torrentDOM;  
}

function loadTorrents(torrents) {
  
  document.getElementById("torrents").appendChild(createTorrentDOM({
    "id": "torrentId",
    "title": "Title",
    "source": "Source",
    "magnet": "Magnet"
  }));
  for (var i=0; i<torrents.length; i++) {
    var torrent = createTorrentDOM(torrents[i]);
    document.getElementById("torrents").appendChild(torrent);
  }
}

function runSearch(query) {
  document.allElements(".searchInput", function(elem) {
    addClass(elem, "searching");
  });
  toggleOff(".torrents");
  document.getElementById("torrents").empty();
  var ajax = fs.ajax()
    .POST("/rest/index/search")
    //.POST("http://localhost:8080/rest/index/search")
    .accept("application/json")
    .handle(200, function(xhr, request) {
      window.location.hash = query;
      var torrents = JSON.parse(xhr.responseText);
      document.allElements(".searchInput", function(elem) {
        removeClass(elem, "searching");
      });
      loadTorrents(torrents);
      toggleOn(".torrents");
    })
    .handleOthers(function(xhr, request) {
      logger(xhr.status.toString() + " Failed search: " + xhr.statusText + '\n' + xhr.responseText);
      document.allElements(".searchInput", function(elem) {
        removeClass(elem, "searching");
      });
    })
    .json(query);
}


function makeRain() {
  var rain = makeDOM("DIV", "rain");
  rain.appendChild(makeDOM("DIV", "rainCloud"));
  rainElem(rain, 800, 15);
  return rain;
}

function makeSky() {
  var sky = makeDOM("DIV", "sky");
  var blue = makeDOM("DIV", "blue");
  var cloud = makeDOM("DIV", "cloud");
  var night = makeDOM("DIV", "night");
  var nightfx = makeDOM("DIV", "nightfx");
  var stars = makeDOM("DIV", "stars");
  var twinkling = makeDOM("DIV", "twinkling");
  sky.appendChild(blue);
  sky.appendChild(cloud);
  sky.appendChild(makeRain());
  sky.appendChild(night);
  stars.appendChild(twinkling);
  nightfx.appendChild(stars);
  sky.appendChild(nightfx);
  return sky;
}

function setDimmer(mode) {
  if (typeof(mode) != number) {
    var opacity = 1;
  } else {
    var opacity = (100-mode)/100;
  } allElements("#dimmer", function(dimmer) { dimmer.style.opacity = opacity; });
}

function makeDimmer() {
  var dimmer = makeDOM("DIV", "dimmer");
  return dimmer;
}

function makeWiper() {
  var wiper = document.createElement("DIV");
  addClass(wiper, "wiper");
  addClass(wiper, "toggle");
  return wiper;
}

function makeLogo() {
  var snowflakeLogo = document.createElement("DIV");
  addClass(snowflakeLogo, "snowflakeLogo");
  addClass(snowflakeLogo, "toggle");
  var snowflakeTxt = document.createElement("DIV");
  addClass(snowflakeTxt, "snowflakeTxt");
  snowflakeTxt.textContent = "Snowflake";
  snowflakeLogo.appendChild(snowflakeTxt);
  return snowflakeLogo;
}


function makeTopControls() {
  var controlsContainer = makeDOM("DIV", "controlsContainer");
  var controls = makeDOM("DIV", "controls");
  var logo = makeDOM("DIV", "logo");
  logo.addEventListener("click", randomSky);
  var search = makeSearch();
  var menu = makeDOM("DIV", "menu");
  //var sideMenu = makeDropdownMenu();
  menu.dataset['toggle'] = '#dropdownMenu';
  menu.dataset['tooltip'] = 'Main Menu';
  addTogglerEvent(menu);
  controls.appendChild(logo);
  controls.appendChild(search);
  //controls.appendChild(menu);
  
  controlsContainer.appendChild(controls);
  //controlsContainer.appendChild(sideMenu);
  
  return controlsContainer;
}



function makeDropdownMenu() {
  var dropDown = makeDOM("div", "dropdownMenu");
 
  var designerIcon = makeDOM("div", "designerIcon");
  designerIcon.dataset['toggleoff'] = '.apps, .dropdownFader';
  designerIcon.dataset['toggle'] = '#appDesigner';
  designerIcon.dataset['tooltip'] = 'Designer';
  addTogglerEvent(designerIcon);

  var weatherIcon = makeDOM("div", "weatherIcon");
  weatherIcon.dataset['toggleoff'] = '.apps, .dropdownFader';
  weatherIcon.dataset['toggle'] = '#weatherControl';
  weatherIcon.dataset['tooltip'] = 'Weather Control';
  addTogglerEvent(weatherIcon);

  var cartIcon = makeDOM("div", "cartIcon");
  cartIcon.dataset['toggleoff'] = '.apps, .dropdownFader';
  cartIcon.dataset['toggle'] = '#cartPane';
  cartIcon.dataset['tooltip'] = 'Cart';
  var cartSub = document.createElement("div");
  cartSub.textContent = '0';
  addClass(cartSub, "subtitle");
  addClass(cartSub, "toggle");
  cartIcon.appendChild(cartSub);
  addTogglerEvent(cartIcon);

  var consoleIcon = makeDOM("div", "consoleIcon");
  consoleIcon.dataset['toggleoff'] = '.apps, .dropdownFader';
  consoleIcon.dataset['toggle'] = '#logHistory';
  consoleIcon.dataset['tooltip'] = 'Log History';
  addTogglerEvent(consoleIcon);

  dropDown.appendChild(designerIcon);
  dropDown.appendChild(weatherIcon);
  dropDown.appendChild(cartIcon);
  dropDown.appendChild(consoleIcon);
 
  return dropDown;
}

function makePanels() {
  document.getElementById("body").appendChild(makeDOM("div", "panels"));
  document.getElementById("panels").appendChild(makeDOM("div", "torrents"));
  //makeAppDesigner();
  //makeWeatherControl();
  
}

/*
function makeAppDesigner() {
  var appDesigner = makeDOM("div", "appDesigner");
  addClass(appDesigner, "apps");
  document.getElementById("panels").appendChild(appDesigner);
}
*/

/*
function makeWeatherControl() {
  var weatherControl = makeDOM("div", "weatherControl");
  addClass(weatherControl, "apps");
  
  var modes = skyModes();
  var selector = makeDOM("select", "skySelector");
    var defa = document.createElement("option");
    defa.value = '';
    defa.textContent = "Sky Mode";
    selector.appendChild(defa);

  for(pos in modes) {
    var opt = document.createElement("option");
    opt.value = modes[pos];
    opt.textContent = modes[pos];
    selector.appendChild(opt);
  } 
  selector.addEventListener("change", function() {
    sky(this.value);
  });
  weatherControl.appendChild(selector);
  
  
  document.getElementById("panels").appendChild(weatherControl);
}
*/

window.onload = function() {
  init();
};
