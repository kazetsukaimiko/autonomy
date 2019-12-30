var classes = ['bluesky', 'sunset', 'yugure', 'yugured', 'yuguredd', 'yoru'];
var current = null;

function updateClass() {
    if (current == null || current >= classes.length) {
        current = 0;
    }
    var selection = "#target > .background";
    var klazz = classes[current++];
    toggleOff(selection);
    toggleOn(selection + "." + klazz);

    document.allElements("#target > .label", function(e) { e.textContent = klazz; });
    window.setTimeout(updateClass, 3000);
}

window.onload = function() {
  updateClass();
};