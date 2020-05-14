//https://stackoverflow.com/questions/847185/convert-a-unix-timestamp-to-time-in-javascript
function formatTimestampMs(UNIX_timestamp_ms){
  var a = new Date(UNIX_timestamp_ms);
  var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  var year = a.getFullYear();
  var month = months[a.getMonth()];
  var date = a.getDate();
  var hour = a.getHours();
  var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
  var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
  var time = hour + ':' + min + ':' + sec ;
  return time;
}

//https://stackoverflow.com/questions/32589197/capitalize-first-letter-of-each-word-in-a-string-javascript/32589256
function capitalizeEachWord(str) {
   var splitStr = str.toLowerCase().split(' ');
   for (var i = 0; i < splitStr.length; i++) {
       // You do not need to check if i is larger than splitStr length, as your for does that for you
       // Assign it back to the array
       splitStr[i] = splitStr[i].charAt(0).toUpperCase() + splitStr[i].substring(1);
   }
   // Directly return the joined string
   return splitStr.join(' ');
}

function formatFlattenedObjectKey(key_s){
    key_s = key_s.replace(".", " - ");
    key_s = key_s.replace(/_/g, " ");

    // special cases

    // end special cases

    key_s = capitalizeEachWord(key_s);
    return key_s;
}

//https://stackoverflow.com/questions/847185/convert-a-unix-timestamp-to-time-in-javascript
function formatTimestampMsFull(UNIX_timestamp_ms){
  var a = new Date(UNIX_timestamp_ms);
  var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  var year = a.getFullYear();
  var month = months[a.getMonth()];
  var date = a.getDate();
  var hour = a.getHours();
  var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
  var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
  var time = hour + ':' + min + ':' + sec + ' - ' + date + '/' + month + '/' + year;
  return time;
}

function formatTimestampMsNotAsFull(UNIX_timestamp_ms){
  var a = new Date(UNIX_timestamp_ms);
  var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  var year = a.getFullYear();
  var month = months[a.getMonth()];
  var date = a.getDate();
  var hour = a.getHours();
  var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
  var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
  var time = a.getMonth()+1 + '/' + date + "/" + year + " " + hour + ':' + min;
  return time;
}

//https://stackoverflow.com/questions/8528382/javascript-show-milliseconds-as-dayshoursmins-without-seconds/8528531
function dhm(t){
    var cd = 24 * 60 * 60 * 1000,
        ch = 60 * 60 * 1000,
        d = Math.floor(t / cd),
        h = Math.floor( (t - d * cd) / ch),
        m = Math.round( (t - d * cd - h * ch) / 60000),
        pad = function(n){ return n < 10 ? '0' + n : n; };
  if( m === 60 ){
    h++;
    m = 0;
  }
  if( h === 24 ){
    d++;
    h = 0;
  }

  if(d != 0){
    return d + " days " + pad(h) + " hours " + pad(m) + " min";
  }

  if(h != 0){
    return pad(h) + " hours " + pad(m) + " min";
  }

  return pad(m) + " min";
}

function convertNagiosHostStatusCodeToEnglish(nagiosHostStatus){
//#define HOST_PENDING			1
//#define SD_HOST_UP				2
//#define SD_HOST_DOWN			4
//#define SD_HOST_UNREACHABLE		8
//https://github.com/NagiosEnterprises/nagioscore/blob/37ff339c8e13547e51fde2f736216ba6876b7742/include/statusdata.h#L156
    var states = {
        1: "Pending",
        2: "Up",
        4: "Down",
        8: "Unreachable"
    };

    return states[nagiosHostStatus];
}

function getNagiosHostBadgeFromStatus(status){
    var badge = "badge badge-danger";
    if(status == 2){ // everything is up
        badge = "badge badge-success";
    }else if( status == 8){ // nothing is down, but not everything is up (unreachable/pending)
        badge = "badge badge-warning";
    }else if(status == 1){
        badge = "badge badge-info";
    }
    return badge;
}

function convertNagiosServiceStatusCodeToEnglish(nagiosServiceStatus){
//#define SERVICE_PENDING			1
//#define SERVICE_OK			2
//#define SERVICE_WARNING			4
//#define SERVICE_UNKNOWN			8
//#define SERVICE_CRITICAL		16
//https://github.com/NagiosEnterprises/nagioscore/blob/37ff339c8e13547e51fde2f736216ba6876b7742/include/statusdata.h#L156
    var states = {
        1: "Pending",
        2: "Ok",
        4: "Warning",
        8: "Unknown",
        16: "Critical"
    };

    return states[nagiosServiceStatus];
}

function getNagiosServiceBadgeFromStatus(status){
    var badge = "badge badge-danger";
    if(status == 2){
        badge = "badge badge-success";
    }else if(status == 4 || status == 8){
        badge = "badge badge-warning";
    }else if(status == 1){
        badge = "badge badge-info";
    }
    return badge;
}

function enableBlur(msg="LOADING"){
    document.getElementById("blurtext").innerHTML = msg;
    document.getElementById("loading").style.display = "";
}

function updateBlurText(txt){
    document.getElementById("blurtext").innerHTML = txt;
}

function disableBlur(){
    document.getElementById("loading").style.display = "none";
}

// this function requires jquery
function custom_show_message(title, message){
    document.getElementById("modal-msg-title").innerHTML = title;
    document.getElementById("modal-msg-content").innerHTML = message;

    $("#modal-secondary").modal('show');
}

function custom_show_message_redirect(title, message, location){
    document.getElementById("modal-msg-title").innerHTML = title;
    document.getElementById("modal-msg-content").innerHTML = message;

    $("#modal-secondary").modal('show');

    $('#modal-secondary').on('hidden.bs.modal', function () {
      window.location.replace("/dashboard");
    });

}

function fireToast(type, message){
Toast.fire({
        type: type,
        title: message
  })
}
//https://stackoverflow.com/questions/3710204/how-to-check-if-a-string-is-a-valid-json-string-in-javascript-without-using-try
function IsJsonString(str) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}

function apiAjaxCallCustomError(destination, type, payload, callback, errorfunction, contentType="application/x-www-form-urlencoded; charset=UTF-8"){
    apiAjaxCallFull(destination, type, payload, callback, "application/x-www-form-urlencoded; charset=UTF-8", false, errorfunction);
}

function apiAjaxCall(destination, type, payload, callback, contentType="application/x-www-form-urlencoded; charset=UTF-8"){
    apiAjaxCallFull(destination, type, payload, callback, "application/x-www-form-urlencoded; charset=UTF-8", false, ajaxErrorFunction);
}

function apiAjaxCallTransparent(destination, type, payload, callback, contentType="application/x-www-form-urlencoded; charset=UTF-8"){
    apiAjaxCallFull(destination, type, payload, callback, "application/x-www-form-urlencoded; charset=UTF-8", true, ajaxErrorFunction);
}

function apiAjaxCallFull(destination, type, payload, callback, contentType="application/x-www-form-urlencoded; charset=UTF-8", transparent=false, errorCallback){
    if(!transparent){
        enableBlur();
    }
    $.ajax({
      type: type,
      url: destination,
      xhrFields: { withCredentials: true },
      crossDomain: true,
      data: payload,
      contentType: contentType,
      success: function(data){
        disableBlur();
        callback(data)
      },
      error: function(xhr, status, error){
          disableBlur();
          errorCallback(xhr, status, error);
        }
    });
}

//https://stackoverflow.com/questions/15900485/correct-way-to-convert-size-in-bytes-to-kb-mb-gb-in-javascript
function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function ajaxErrorFunction(xhr, status, error, transparent){
    disableBlur();

    var errorMessage = "Unknown Error";
    try{
        var resText = JSON.parse(xhr.responseText);
        if(resText.hasOwnProperty("msg")){
            errorMessage = resText.msg;
        }else{
            errorMessage = xhr.status + ': ' + xhr.statusText + " -- " + xhr.state();
        }
    }catch(err){
        errorMessage = xhr.status + ': ' + xhr.statusText + " -- " + xhr.state();
    }
    custom_show_message("Error", errorMessage);
}

function ajaxErrorLog(xhr, status, error){
    disableBlur();
    console.log(xhr.status + ': ' + xhr.statusText + " -- " + xhr.state());
}

function numberWithCommas(x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function indexOfMax(arr) {
    if (arr.length === 0) {
        return -1;
    }

    var max = arr[0];
    var maxIndex = 0;

    for (var i = 1; i < arr.length; i++) {
        if (arr[i] > max) {
            maxIndex = i;
            max = arr[i];
        }
    }

    return maxIndex;
}

function apiAjaxCallFullSimple(destination, type, payload, callback, customErrorFunctionDashboard, async=true, contenttype="application/json; charset=UTF-8"){
    $.ajax({
      type: type,
      async: async,
      url: destination,
      crossDomain: true,
      data: payload,
      headers: {
          "Accept": "application/json"
        },
      contentType: contenttype,
      success: function(data){
        disableBlur();
        callback(data)
      },
      error: customErrorFunctionDashboard
    });
}



