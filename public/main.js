
function getUser(userData) {

    if (userData.length == 0) {
        $("#login").show();
        $("#upload").hide();
    }  else {
        $("#logout").show();
        $("#upload").show();
    }
}

$.get("/user", getUser);

function getPhotos(photosData) {
    for (var i in photosData) {
        var photo = photosData[i];
        var elem = $("<img>");
        elem.attr("src", "/photons/" + photo.filename);
        elem.attr("class", "col-xs-6")
        elem.attr("style", "margin-top: 5px;")
        $("#photos").append(elem);
    }
}

$.get("/photos", getPhotos);

