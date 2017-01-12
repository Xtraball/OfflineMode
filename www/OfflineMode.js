var exec = require('cordova/exec');

var OfflineMode = function() {
    var offline_mode = {};

    var callbacks = [];

    offline_mode.setCheckConnectionURL = function(checkConnectionURL, success_callback, error_callback) {
        exec(success_callback, error_callback, "OfflineMode", "setCheckConnectionURL", [checkConnectionURL]);
    };

    offline_mode.useCache = function(is_online, success_callback, error_callback) {
        exec(success_callback, error_callback, "OfflineMode", "useCache", [is_online]);
    };

    var internalCallbackName = "__internal_callback_"+(+(new Date()));
    offline_mode[internalCallbackName] = function (data) {
        for(var i = callbacks.length - 1; i >= 0; i--) {
            callbacks[i](data);
        }
    };

    exec(
        function() { console.log("[offline-mode] Inserted general callback"); },
        function() { console.log("[offline-mode] failed to insert general callback"); },
         "OfflineMode",
         "setInternalCallback",
        ["OfflineMode."+internalCallbackName]
    );

    offline_mode.registerCallback = function(callback) {
        if((typeof callback === "function")) {
            callbacks.push(callback);
            return offline_mode.unregisterCallback.bind(offline_mode, callback);
        }

        return false;
    };

    offline_mode.unregisterCallback = function(callback) {
        for(var i = callbacks.length - 1; i >= 0; i--) {
            if(callbacks[i] === callback) {
                callbacks.splice(i, 1);
            }
        }
    };

    return offline_mode;
};

module.exports = new OfflineMode();
