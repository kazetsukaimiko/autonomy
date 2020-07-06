/**
 *
 */















/* Frostbite AJAX Library
 */

function frostbite() {
}

frostbite.ajax = function() {
    return new frostbite._ajax();
}
frostbite.ajax.handlers = {};

frostbite.FETCHES = "fetch";
frostbite.LISTENERS = "listener";

frostbite._ajax = function() {
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
        }
        return this;
    }

    this.attach = function(objectName, objectValue) {
        if (typeof(objectName) == "string") {
            this.objectList[objectName] = objectValue;
        }
        return this;
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
        let handler = this.otherCodesFunction;
        if (typeof(frostbite) == "function" && typeof(frostbite.ajax) == "function" && typeof(frostbite.ajax.handlers) == "object") {
            if (typeof(frostbite.ajax.handlers[this.xhr.status]) == "function") {
                handler = frostbite.ajax.handlers[this.xhr.status];
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
            const that = this;
            this.xhr.onreadystatechange = function() {
                if (that.xhr.readyState === 4) {
                    const handler = that.getHandler();
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
            for (const hi in this.headers) {
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

    this.form = function(input) {
        const content = input || this.compileAttachments();
        this.makeXHR();
        this.xhr.send(content);
        return this;
    }

    this.raw = function(content) {
        this.makeXHR();
        if (this.callMethod !== "GET") {
            if (this.uploadProgressHandler != null) {
                this.xhr.upload.onprogress = this.uploadProgressHandler;
            } this.xhr.send(content);
        } else {
            this.xhr.send();
        }
        return this;
    }

    this.json = function(input) {
        if (this.callMethod !== "GET") {
            const content = input || JSON.stringify(this.objectList);
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
        if (this.callMethod !== "GET") {
            return this.raw(content);
        }
    }
}

window.onload = function() {
    console.log('Starting up Frostbite.');
    window[frostbite.FETCHES] = {};
    window[frostbite.LISTENERS] = {};
};

frostbite.queryList = function(queryString, initializer) {
    const selection = document.querySelectorAll(queryString);
    let list = [];
    selection.forEach(function(val, idx, obj) {
        const elem = obj[idx];

        if (typeof(initializer) === "function") {
            initializer(elem);
        }
        list.push(elem);
    })
    return list;
}

frostbite.initializeFetch = function(fetchElem) {
     if (typeof(fetchElem) !== "undefined") {

     }
}

frostbite.findFetches = function() {
    return frostbite.queryList(frostbite.FETCHES + "[id]", function() {

    });
}

frostbite.findFetches = function() {
    return frostbite.queryList(frostbite.FETCHES + "[id]");
}


