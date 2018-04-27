var exec = require("cordova/exec");

function Track() { }

Track.prototype.startTrack = function (tid, uid, dbname, onSuccess, onError) {
    exec(onSuccess, onError, "Track", "startTrack", [tid, uid, dbname]);
};

Track.prototype.stopTrack = function (tid, onSuccess, onError) {
    exec(onSuccess, onError, "Track", "stopTrack", [tid]);
};

Track.prototype.startLocation = function (onSuccess, onError) {
    exec(onSuccess, onError, "Track", "startLocation", []);
};

Track.prototype.stopLocation = function (onSuccess, onError) {
    exec(onSuccess, onError, "Track", "stopLocation", []);
};

Track.prototype.geocodeSeach = function (lng, lat, onSuccess, onError) {
    exec(onSuccess, onError, "Track", "geocodeSeach", [lng, lat]);
};

Track.prototype.saveUser = function (url, userId, onSuccess, onError) {
    exec(onSuccess, onError, "Track", "saveUser", [url, userId]);
};

module.exports = new Track();
