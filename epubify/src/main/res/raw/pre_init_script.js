function setBodyPadding(topDp, bottomDp, rightDp, leftDp) {
    var body = document.getElementsByTagName('body')[0];
    body.style.paddingLeft = (window.devicePixelRatio * leftDp) + 'px';
    body.style.paddingRight = (window.devicePixelRatio * rightDp) + 'px';
    body.style.paddingTop = (window.devicePixelRatio * topDp) + 'px';
    body.style.paddingBottom = (window.devicePixelRatio * bottomDp) + 'px';
}

// Remove all padding and margin from the body

var body = document.getElementsByTagName('body')[0];
body.style.padding = '0px';
body.style.margin = '0px';