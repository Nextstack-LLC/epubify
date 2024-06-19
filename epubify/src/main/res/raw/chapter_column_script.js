const columnWidth = window.innerWidth;
let pageCount = 0;
let currentPage = 0;
const columnGap = 10; // Adjust this value to set the desired gap between columns

function initColumns() {
  const body = document.body;
  const windowHeight = window.innerHeight;

  const contentHeight = body.offsetHeight;
  pageCount = Math.floor(contentHeight / windowHeight) + 1;

  const newBodyWidth = (pageCount * columnWidth) + ((pageCount - 1) * columnGap);

  body.style.height = `${windowHeight}px`;
  body.style.width = `${newBodyWidth}px`;

  body.style.columnWidth = `${columnWidth}px`;
  body.style.columnHeight = `${windowHeight}px`;
  body.style.columnGap = `${columnGap}px`;
}

function scrollToElement(elementId) {
  const element = document.getElementById(elementId);
  const offset = element.offsetLeft;
  const page = Math.floor(offset / (columnWidth + columnGap));

  window.WebViewBridge.setCurrentPage(page);
  scrollToPage(page);
}

function calculateColumnCount() {
  const spanElement = document.getElementById('end-marker');
  const scrollOffset = window.scrollX;
  const leftPosition = scrollOffset + spanElement.getBoundingClientRect().left;

  const adjustedColumnWidth = columnWidth + columnGap;
  const columnIndex = Math.ceil(leftPosition / adjustedColumnWidth);
  pageCount = columnIndex + 1;

  window.WebViewBridge.setColumnCount(pageCount);
}

function scrollToPage(page, animated = true) {
  const position = page * (columnWidth + columnGap) - columnGap / 2;

  currentPage = page;
  window.WebViewBridge.scrollToPosition(window.devicePixelRatio, position, animated);
}

function setZoom(multiplier) {
  document.body.style.fontSize = `${100 + (12.5 * multiplier)}%`;
  calculateColumnCount();
}

initColumns();
calculateColumnCount();