jQuery.fn.highlight = function(start, end, css) {
    var skip = 0;
    var spannode = document.createElement('span');
    spannode.className = css;
    var middlebit = this[0].firstChild.splitText(start);
    var endbit = middlebit.splitText(end-start);
    var middleclone = middlebit.cloneNode(true);
    spannode.appendChild(middleclone);
    middlebit.parentNode.replaceChild(spannode, middlebit);
};

jQuery.fn.removeHighlight = function() {
    return this.find("span.highlight").each(function() {
            this.parentNode.firstChild.nodeName;
            with (this.parentNode) {
                replaceChild(this.firstChild, this);
                normalize();
            }
        }).end();
};