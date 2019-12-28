/* FallingSnow AJAX Library
 * AJAX Made Easy
 */
  
function fs() {
}

fs.ajax = function() {
  return new fs._ajax();
}

fs.ajax.handlers = {};


fs._ajax = function() {
  this.target = null;
  this.headers = {};
  this.handlers = {};
  this.xhr = null;
  this.callMethod = null;
  this.acceptType = null;
  this.objectList = {};
  this.uploadProgressHandler = null;
  
  /*** BEGIN FLUENT API ***/

  this.GET = function(url) {
   this.callMethod = 'GET';
   this.target = url; return this;
  }

  this.POST = function(url) {
   this.callMethod = 'POST';
   this.target = url; return this;
  }

  this.DELETE = function(url) {
   this.callMethod = 'DELETE';
   this.target = url; return this;
  }
  
  this.accept = function(accept) {
   this.acceptType = accept; return this;
  }

  this.handle = function(code, callback) {
   if (typeof(code) == "number" && typeof(callback) == "function") {
    this.handlers[code] = callback;
   }; return this;
  }

  this.attach = function(objectName, objectValue) {
   if (typeof(objectName) == "string") {
    this.objectList[objectName] = objectValue;
   }; return this;
  }
  
  this.header = function(name, value) {
   this.headers[name] = value; return this;
  }
  
  this.uploadProgress = function(callable) {
   this.uploadProgressHandler = callable; return this;
  }

  /*** END FLUENT API ***/

  /*  
  this.open = function(url) {
   this.target = url; return this;
  }
  */

  this.handleOthers = function(callback) {
   if (typeof(callback) == "function") {
    this.otherCodesFunction = callback;
   } return this;
  }

  this.otherCodesFunction = function() {};


  
  this.getHandler = function() {
   var handler = this.otherCodesFunction;
   if (typeof(fs) == "function" && typeof(fs.ajax) == "function" && typeof(fs.ajax.handlers) == "object") {
    if (typeof(fs.ajax.handlers[this.xhr.status]) == "function") {
     handler = fs.ajax.handlers[this.xhr.status];
    }
   }
   if (typeof(this.handlers) == "object" && typeof(this.handlers[this.xhr.status]) == "function") {
    handler = this.handlers[this.xhr.status];
   }
   return handler;
  }
  
  this.makeXHR = function() {
   if (window.XMLHttpRequest) {
    this.xhr = new XMLHttpRequest();
    var that = this;
    this.xhr.onreadystatechange = function() {
     if (that.xhr.readyState == 4) {
      var handler = that.getHandler();
      if (handler != null) {
       handler(this, that);
      }
     }
    };
    if (typeof(this.acceptType) == "string") {
     this.headers['Accept'] = this.acceptType;
    }

    // REQUEST OPEN
    this.xhr.open(this.callMethod, this.target, true);
    for (var hi=0 in this.headers) {
     if (typeof(hi) == "string" && typeof(this.headers[hi]) == "string") {
      this.xhr.setRequestHeader(hi, this.headers[hi]);
     }
    }
    
    return this.xhr;
   } else {
    console.error("No XHR Support.");
   }
  }
    
  this.compileAttachments = function() {
   if (window.FormData) {
    var formData = new FormData();
    for(var objectName in this.objectList) {
     formData.append(objectName, JSON.stringify(objectList[objectName]));
    }
    return formData;
   } else {
    console.error("No FormData Support.");
   }
  }
  
  this.form = function(content) {
   var content = content || this.compileAttachments();
   this.makeXHR();
   this.xhr.send(content);
   return this;
  }
  
  this.raw = function(content) {
   this.makeXHR();
   if (this.callMethod != "GET") {
    if (this.uploadProgressHandler != null) {
     this.xhr.upload.onprogress = this.uploadProgressHandler;
    } this.xhr.send(content);
   } else {
    this.xhr.send();
   }; return this;
  }
  
  this.json = function(content) {
   if (this.callMethod != "GET") {
    var content = content || JSON.stringify(this.objectList);
    if (typeof(this.acceptType) != "string" && typeof(this.headers['Accept']) != "string") {
     this.headers['Accept'] = 'application/json';
    }
    this.headers['Content-Type'] = 'application/json';
    if (typeof(content) == "string") { 
     return this.raw(content);
    } else {
     return this.raw(JSON.stringify(content));
    }
   } else {
    return this.raw();
   }
  } 
  
  this.upload = function(content) {
   if (this.callMethod != "GET") {
    return this.raw(content);
   }
  }
}