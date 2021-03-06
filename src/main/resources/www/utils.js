/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Function that will do long polling on one type of object
 * @param {type} url
 * @param {type} onSuccess
 * @returns {LongPoller}
 */
function Poll(url, onSuccess) {
    this.seq = 0;
    this.url = url;
    this.onSuccess = onSuccess;

    this.pollerSuccess = function (event) {
        console.log("long polling to " + this.url + "/" + this.seq + " succedded, next url is  " + this.url + "/" + event.seq);
        this.seq = event.seq;
        this.onSuccess(event.data);
    };

    this.start = function () {
        $.ajax({
            context: this,
            url: this.url + "/" + this.seq,
            dataType: "json",
            complete: this.start,
            timeout: 30000,
            success: this.pollerSuccess
        });
    };

    this.dispose = function () {
        this.onSuccess = function () {
        };
        this.start = function () {
        };
        this.pollerSuccess = function () {
        };
    };
}

function createButtonSet(id) {
    $(id).buttonset().addClass("ui-buttonset-vertical")                             // Adds our custom CSS class which changes the orientation.
            .find("label").removeClass("ui-corner-left ui-corner-right")            // Remove the corner classes that don't make sense with the new layout.
            .on("mouseenter", function (e) {                                         // Hack needed to adjust the top border on the next label during hover.
                $(this).next().next().addClass("ui-transparent-border-top");
            })
            .on("mouseleave", function (e) {
                $(this).next().next().removeClass("ui-transparent-border-top");     // Hack needed to adjust the top border on the next label during hover.
            })
            .filter(":first").addClass("ui-corner-top")                             // Apply proper corner styles.
            .end()
            .filter(":last").addClass("ui-corner-bottom");
}


function postMessage(servlet, clazz, msg) {
    postMessage(servlet, clazz, msg, function () {
    });
}

function postMessage(servlet, clazz, msg, onSuccess) {
    $.ajax({
        url: servlet + '/' + clazz,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(msg),
        success: onSuccess,
        dataType: 'json'
    });
}

function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

function domClean(myNode) {
    while (myNode.firstChild) {
        myNode.removeChild(myNode.firstChild);
    }
}

function bin2String(array) {
    var result = "";
    for (var i = 0; i < array.length; i++) {
        result += String.fromCharCode(parseInt(array[i], 10));
    }
    return result;
}

function postDownload(path, params, method) {
    method = method || "post"; // Set method to post by default if not specified.

    // The rest of this code assumes you are not using a library.
    // It can be made less wordy if you use one.
    var form = document.createElement("form");
    form.setAttribute("method", method);
    form.setAttribute("action", path);

    for(var key in params) {
        if(params.hasOwnProperty(key)) {
            var hiddenField = document.createElement("input");
            hiddenField.setAttribute("type", "hidden");
            hiddenField.setAttribute("name", key);
            hiddenField.setAttribute("value", params[key]);

            form.appendChild(hiddenField);
         }
    }

    document.body.appendChild(form);
    form.submit();
}
