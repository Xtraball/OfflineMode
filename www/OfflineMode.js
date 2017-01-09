var exec = require('cordova/exec');

var OfflineMode = function() {
    var exports = {};

    var callbacks = [];
    
    exports.setCheckConnectionURL = function(checkConnectionURL, success_callback, error_callback) {
        exec(success_callback, error_callback, "OfflineMode", "setCheckConnectionURL", [checkConnectionURL]);
    };
    
    exports.useCache = function(is_online, success_callback, error_callback) {
        exec(success_callback, error_callback, "OfflineMode", "useCache", [is_online]);
    };
    
    var internalCallbackName = "__internal_callback_"(+(new Date()));
    exports[internalCallbackName] = function (data) {
        for(var i = callbacks.length - 1; i >= 0; i--) {
            callbacks[i](data);
        }
    };

    exec(
         success_callback,
         error_callback,
         "OfflineMode",
         "setInternalCallback",
         ["OfflineMode."+internalCallbackName]);
    
    exports.registerCallback = function(callback) {
        if((typeof callback === "function")) {
            callbacks.push(callback)
            return exports.unregisterCallback.bind(exports, callback);
        }
        
        return false;
    };
    
    exports.unregisterCallback = function(callback) {
        for(var i = callbacks.length - 1; i >= 0; i--) {
            if(callbacks[i] === callback) {
                callbacks.splice(i, 1);
            }
        }
    };
    
    return exports;
};

module.exports = new OfflineMode();
