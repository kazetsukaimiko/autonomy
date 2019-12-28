
function addTogglerEvent(toggleElement, event) {
  toggleElement.toggleOn = function() {
    var toggle = this.getAttribute("data-toggle");
    if (toggle != null) {
      var isToggledOn = getToggleState(toggle);
      if (typeof(togglerOnCallback) != "function") {
        togglerOnCallback = function(callback) { console.log("TogglerOnCallback"); callback(); };
      }
      if (!isToggledOn) {
        console.log("ON");
        return togglerOnCallback(function() {
          toggleOn(toggle);
        });
      }
    }
  };
  toggleElement.toggleOff = function() {
    var toggle = this.getAttribute("data-toggle");
    if (toggle != null) {
      var isToggledOn = getToggleState(toggle);
      if (typeof(togglerOffCallback) != "function") {
        togglerOffCallback = function(callback) { console.log("TogglerOffCallback");  callback(); };
      }
      if (isToggledOn) {
        console.log("OFF");
        return togglerOffCallback(function() {
          toggleOff(toggle);
        });
      }
    }
  };
  toggleElement.toggleControl = function() {
    var toggle = this.getAttribute("data-toggle");
    var togglerOn = this.getAttribute("data-toggleOn");
    var togglerOff = this.getAttribute("data-toggleOff");
    var activeOn = this.getAttribute("data-activeon");
    var togglerOnCallback = window[this.getAttribute("data-toggleOnCallback")];
    var togglerOffCallback = window[this.getAttribute("data-toggleOffCallback")];
    
    if (typeof(togglerOnCallback) != "function") {
      togglerOnCallback = function(callback) { console.log("TogglerOnCallback"); callback(); };
    }
    if (typeof(togglerOffCallback) != "function") {
      togglerOffCallback = function(callback) { console.log("TogglerOffCallback");  callback(); };
    }

    var isToggledOn = getToggleState(toggle);
  
    console.log("TogglerEvent click");
    if (togglerOff != null) {
      toggleOff(togglerOff);
    }
    if (togglerOn != null) {
      toggleOn(togglerOn);
    }
    if (toggle != null) {
      if (isToggledOn) {
        console.log("OFF");
        return togglerOffCallback(function() {
          toggleOff(toggle);
        });
        //return toggleOff(toggle);
      } else {
        console.log("ON");
        return togglerOnCallback(function() {
          toggleOn(toggle);
        });
        //return toggleOn(toggle);
      }
    } else if (activeOn != null) {
      if (getToggleState(activeOn) || getToggleState(togglerOn)) {
        togglerOnCallback();
      }
    }
  };

  toggleElement.addEventListener("click", function(evt) {
    this.toggleControl();
  });
  toggleElement.addEventListener("dragenter", function(evt) {
    var evt = evt || window.event;
    if (typeof(this.dragenterTimeout) != "undefined") {
      window.clearTimeout(this.dragenterTimeout);
      delete this.dragenterTimeout;
    }    
    var that = this; // Scope
    this.dragenterTimeout = window.setTimeout(function(evt) {
      if (that.getAttribute("data-toggleDragFiles") != "true") {
        for (var i=0; i< evt.dataTransfer.types.length; i++) {
          if (event.dataTransfer.types[i] == "Files") {
            return;
          } 
        }
      }    
      if (that.getAttribute("data-toggleDragover") == "true") {
        addClass(that, "dragoverToggle");
        that.toggleOn();
      } delete that.dragenterTimeout;
    }, 1000);
  });
  toggleElement.addEventListener("dragleave", function(evt) {
    var evt = evt || window.event;

    if (typeof(this.dragenterTimeout) != "undefined") {
      window.clearTimeout(this.dragenterTimeout);
      delete this.dragenterTimeout;
    }
    if (this.getAttribute("data-toggleDragFiles") != "true") {
      for (var i=0; i< evt.dataTransfer.types.length; i++) {
        if (event.dataTransfer.types[i] == "Files") {
          return;
        } 
      }
    }
    if (this.getAttribute("data-toggleDragleave") == "true") {
      removeClass(this, "dragoverToggle");
      this.toggleOff();
    }
  });
}

function toggleControlSync() {
  document.allElements(".toggleControl, [data-toggle]", function(control) {
    var toggleQuery = control.getAttribute("data-toggle");
    if (toggleQuery != null && typeof(toggleQuery) == "string") {
      if (getToggleState(toggleQuery)) {
        addClass(control, "targetActive");
      } else {
        removeClass(control, "targetActive");
      }
    }
    var activeQuery = control.getAttribute("data-activeon");
    if (activeQuery != null && typeof(activeQuery) == "string") {
      if (getToggleState(activeQuery)) {
        addClass(control, "targetActive");
      } else {
        removeClass(control, "targetActive");
      }
    }
  });
}

function toggleQuery(query, toggleClass) {
  if (typeof(toggleClass) != "string") {
    var toggleClass = "toggle";
  }
  if (typeof(query) == "string") {
    if (getToggleState(query, toggleClass)) {
      console.log("OFF");
      return toggleOff(query, toggleClass);
    } else {
      console.log("ON");
      return toggleOn(query, toggleClass);
    }
  } return false;
}

function toggle(query, toggleClass) {
  if (typeof(toggleClass) != "string") {
    var toggleClass = "toggle";
  }
  if (getToggleState(query, toggleClass)) {
    toggleOff(query, toggleClass);
  } else {
    toggleOn(query, toggleClass);
  }
}

function getToggleState(query, toggleClass) {
  if (typeof(toggleClass) != "string") {
    var toggleClass = "toggle";
  }
  if (typeof(query) == "string") {
    var hasAny = false;
    var allOn = true;
    document.allElements(query, function(element) {
      hasAny = true;
      if (!hasClass(element, "toggle")) {
        allOn = false;
      }
    }); return (hasAny && allOn);
  } return false;
}

function toggleOff(query, toggleClass) {
  if (typeof(toggleClass) != "string") {
    var toggleClass = "toggle";
  }
  if (window.toggleDebug === true) {
    console.trace(query, " / ", toggleClass);
  }
  if (typeof(query) == "string") {
    document.allElements(query, function(element) {
      var changed = hasClass(element, toggleClass);
      removeClass(element, toggleClass);
      if (hasClass(element, toggleClass) && typeof(element.ontoggle) == "function") {
        element.ontoggle();
      }
    }); toggleControlSync();
  } return false;
}

function toggleOn(query, toggleClass) {
  if (typeof(toggleClass) != "string") {
    var toggleClass = "toggle";
  }
  if (window.toggleDebug === true) {
    console.trace(query, " / ", toggleClass);
  }
  if (typeof(query) == "string") {
    document.allElements(query, function(element) {
      var changed = !hasClass(element, toggleClass);
      addClass(element, toggleClass);
      if (hasClass(element, toggleClass) && typeof(element.ontoggle) == "function") {
        element.ontoggle();
      }
    }); toggleControlSync();
  } return true;
}

window.addEventListener("load", function() {
  document.allElements(".toggleControl", function(elem) {
    addTogglerEvent(elem);
  });
});


