var exec = require('cordova/exec'),
	cordova = require('cordova'),
	channel = require('cordova/channel');

function HandsRfid(){
	
}

HandsRfid.prototype.readConfig={
		POWER_GAIN:300,
		TYPE:"TID",
		LENGTH:6,
		COMPATIBLE:true,
		CONTINUOUS:false,
		KEYSTART:true,
		KEEPKEY:false
}

HandsRfid.prototype.connect = function (successCallback, errorCallback, addr){
	if (errorCallback == null) {
        errorCallback = function () {
        };
    }

    if (typeof errorCallback != "function") {
        console.log("RFIDOprate.connect failure: failure parameter not a function");
        return;
    }

    if (typeof successCallback != "function") {
        console.log("RFIDOprate.connect failure: success callback parameter must be a function");
        return;
    }
    
    exec(successCallback, errorCallback, 'RFIDOprate', 'connect', [addr]);
}

HandsRfid.prototype.disconnect = function (successCallback, errorCallback){

    if (errorCallback == null) {
        errorCallback = function () {
        };
    }

    if (typeof errorCallback != "function") {
        console.log("RFIDOprate.disconnect failure: failure parameter not a function");
        return;
    }

    if (typeof successCallback != "function") {
        console.log("RFIDOprate.disconnect failure: success callback parameter must be a function");
        return;
    }
    
    exec(successCallback, errorCallback, 'RFIDOprate', 'disconnect', []);
}

HandsRfid.prototype.getConnection = function (successCallback, errorCallback){

    if (errorCallback == null) {
        errorCallback = function () {
        };
    }

    if (typeof errorCallback != "function") {
        console.log("RFIDOprate.getConnection failure: failure parameter not a function");
        return;
    }

    if (typeof successCallback != "function") {
        console.log("RFIDOprate.getConnection failure: success callback parameter must be a function");
        return;
    }
    
    exec(successCallback, errorCallback, 'RFIDOprate', 'getConnection', []);
}

HandsRfid.prototype.readCard = function (successCallback, errorCallback, config){
	
	if(config){
		for(v in this.readConfig){
			if(!config[v]){
				config[v]=this.readConfig[v];
			}
		}
	}
	
	if(config instanceof Array) {
        // do nothing
    } else {
        if(typeof(config) === 'object') {
            config = [ config ];
        } else {
            config = [this.readConfig];
        }
    }

    if (errorCallback == null) {
        errorCallback = function () {
        };
    }

    if (typeof errorCallback != "function") {
        console.log("RFIDOprate.readCard failure: failure parameter not a function");
        return;
    }

    if (typeof successCallback != "function") {
        console.log("RFIDOprate.readCard failure: success callback parameter must be a function");
        return;
    }
    
    exec(successCallback, errorCallback, 'RFIDOprate', 'readCard', config);
}

HandsRfid.prototype.stopRead = function (successCallback, errorCallback, config){
	if(config instanceof Array) {
        // do nothing
    } else {
        if(typeof(config) === 'object') {
            config = [ config ];
        } else {
            config = [];
        }
    }

    if (errorCallback == null) {
        errorCallback = function () {
        };
    }

    if (typeof errorCallback != "function") {
        console.log("RFIDOprate.stopRead failure: failure parameter not a function");
        return;
    }

    if (typeof successCallback != "function") {
        console.log("RFIDOprate.stopRead failure: success callback parameter must be a function");
        return;
    }
    
    exec(successCallback, errorCallback, 'RFIDOprate', 'stopRead', config);
}

var handsRfid = new HandsRfid();

channel.createSticky('onCordovaConnectionReady');
channel.waitForInitialization('onCordovaConnectionReady');

module.exports = handsRfid;

exports = handsRfid;

/*exports.coolMethod = function(arg0, success, error) {
    exec(success, error, "hands-rfid-plugins", "coolMethod", [arg0]);
};*/
