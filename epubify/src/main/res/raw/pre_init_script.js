function setBodyPadding(topDp, bottomDp, rightDp, leftDp) {
    var body = document.getElementsByTagName('body')[0];
    body.style.marginLeft = window.devicePixelRatio * leftDp + 'px';
    body.style.marginRight = window.devicePixelRatio * rightDp + 'px';
    body.style.marginTop = window.devicePixelRatio * topDp + 'px';
    body.style.marginBottom = window.devicePixelRatio * bottomDp + 'px';
}

// Remove all padding and margin from the body

var body = document.getElementsByTagName('body')[0];
body.style.padding = '0px';
body.style.margin = '0px';