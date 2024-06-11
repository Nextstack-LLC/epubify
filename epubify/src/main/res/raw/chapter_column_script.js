var columnWidth;
var columnGap = 0;
var pageCount;
var currentPage = 0;

function initColumns() {
    // Get the body element
    var body = document.getElementsByTagName('body')[0];

    // Get the height and width of the window (WebView)
    var windowHeight = window.innerHeight;
    var windowWidth = window.innerWidth;

    // Get the full height of the body content
    var contentHeight = body.offsetHeight;

    // Calculate the initial number of pages based on the content height and window height
    var pageCount = Math.floor(contentHeight / windowHeight) + 1;

    // Calculate the new width of the body to accommodate all pages side by side
    var newBodyWidth = pageCount * windowWidth;

    // Set the height and width of the body
    body.style.height = windowHeight + 'px';
    body.style.width = newBodyWidth + 'px';

    // Set the number of columns to match the number of pages
    columnWidth = windowWidth - 0;
    body.style.webkitColumnWidth = columnWidth + 'px';
    body.style.webkitColumnHeight = windowHeight + 'px';
    body.style.webkitColumnGap = columnGap + 'px';
    body.style.padding = '0px';
    body.style.margin = '0px';
}

// Scroll to the element with the given id
function scrollToElement(elementId) {
    var element = document.getElementById(elementId);
    var offset = element.offsetLeft;
    var page = Math.floor(offset / columnWidth);

    window.WebViewBridge.setCurrentPage(page);
    scrollToPage(page);
}

// Call only after initialization
function calculateColumnCount() {
    // Select the span element
    const spanElement = document.getElementById('end-marker');

    // Get the left property of the span element
    const leftPosition = spanElement.getBoundingClientRect().left;

    // Calculate the adjusted column width (column width + column gap)
    const adjustedColumnWidth = columnWidth + columnGap;

    // Determine the column index by dividing the left position by the adjusted column width
    const columnIndex = Math.ceil(leftPosition / adjustedColumnWidth);

    // Real page count
    pageCount = columnIndex + 1

    window.WebViewBridge.setColumnCount(pageCount + currentPage)
}

// Scroll to the given page
function scrollToPage(page) {
    var position = page * (columnWidth);
    currentPage = page
    window.WebViewBridge.animateScrollToPosition(window.devicePixelRatio, position);
}

function setZoom(multiplier) {
    var body = document.getElementsByTagName('body')[0];
    body.style.fontSize = 100 + (12.5 * multiplier) + "%"
    calculateColumnCount()
}

initColumns();