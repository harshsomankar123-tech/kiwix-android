function() {
    var savedRange = null;

    var HighlightEngine = {
        saveSelection: function() {
            try {
                var selection = window.getSelection();
                if (selection && selection.rangeCount > 0) {
                    savedRange = selection.getRangeAt(0).cloneRange();
                    return true;
                }
            } catch (e) {
                console.error("HighlightEngine.saveSelection error:", e);
            }
            return false;
        },

        createHighlight: function(color) {
            try {
                var range = savedRange;
                if (!range) {
                    var selection = window.getSelection();
                    if (selection && selection.rangeCount > 0) {
                        range = selection.getRangeAt(0);
                    }
                }
                
                if (!range || range.collapsed) return null;
                
                var safeRange = this.getSafeRange(range);
                
                if (safeRange) {
                    var highlightText = range.toString();
                    var rangeJSON = JSON.stringify(safeRange);
                    this.applyHighlightToRange(range, color, rangeJSON);
                    if (window.HighlightInterface) {
                        window.HighlightInterface.onHighlightCreated(highlightText, rangeJSON, color);
                    }
                    window.getSelection().removeAllRanges();
                    savedRange = null;
                    return rangeJSON;
                }
            } catch (e) {
                console.error("HighlightEngine.createHighlight error:", e);
            }
            savedRange = null;
            return null;
        },

        getSafeRange: function(range) {
            var startContainer = range.startContainer;
            var endContainer = range.endContainer;
            
            if (!startContainer || !endContainer) return null;

            return {
                startOffset: range.startOffset,
                endOffset: range.endOffset,
                startPath: this.getPathTo(startContainer),
                endPath: this.getPathTo(endContainer),
                text: range.toString()
            };
        },

        getPathTo: function(node) {
            if (!node) return "";
            if (node.nodeType === 3) { // Text node
                var parent = node.parentNode;
                if (!parent) return "";
                var siblings = parent.childNodes;
                var index = 0;
                for (var i = 0; i < siblings.length; i++) {
                    var sibling = siblings[i];
                    if (sibling === node) {
                        return this.getPathTo(parent) + "/text()[" + (index + 1) + "]";
                    }
                    if (sibling.nodeType === 3) index++;
                }
            }
            if (node === document.body) return "/html/body";
            if (node === document.documentElement) return "/html";
            if (!node.parentNode) return "";

            var index = 0;
            var siblings = node.parentNode.childNodes;
            for (var i = 0; i < siblings.length; i++) {
                var sibling = siblings[i];
                if (sibling === node) {
                    return this.getPathTo(node.parentNode) + "/" + node.tagName.toLowerCase() + "[" + (index + 1) + "]";
                }
                if (sibling.nodeType === 1 && sibling.tagName === node.tagName) {
                    index++;
                }
            }
            return "";
        },

        applyHighlightToRange: function(range, color, rangeJSON) {
            try {
                var startContainer = range.startContainer;
                var endContainer = range.endContainer;
                
                var nodes = [];
                var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
                var node;
                var started = false;
                
                if (startContainer === endContainer && startContainer.nodeType === 3) {
                    nodes.push(startContainer);
                } else {
                    while (node = walker.nextNode()) {
                        if (node === startContainer) started = true;
                        if (started) nodes.push(node);
                        if (node === endContainer) break;
                    }
                }
                
                nodes.forEach(node => {
                    var nodeRange = document.createRange();
                    var start = (node === startContainer) ? range.startOffset : 0;
                    var end = (node === endContainer) ? range.endOffset : node.length;
                    
                    nodeRange.setStart(node, start);
                    nodeRange.setEnd(node, end);
                    
                    var span = document.createElement('span');
                    span.style.backgroundColor = color || 'yellow';
                    span.className = 'kiwix-highlight';
                    if (rangeJSON) span.dataset.range = rangeJSON;
                    
                    try {
                        nodeRange.surroundContents(span);
                    } catch (e) {
                        var content = nodeRange.extractContents();
                        span.appendChild(content);
                        nodeRange.insertNode(span);
                    }
                });
            } catch (e) {
                console.error("HighlightEngine.applyHighlightToRange error:", e);
            }
        },

        restoreHighlight: function(rangeJSON, color) {
            try {
                var info = JSON.parse(rangeJSON);
                var range = document.createRange();
                
                var startNode = this.getNodeByPath(info.startPath);
                var endNode = this.getNodeByPath(info.endPath);
                
                if (startNode && endNode) {
                    range.setStart(startNode, info.startOffset);
                    range.setEnd(endNode, info.endOffset);
                    this.applyHighlightToRange(range, color, rangeJSON);
                }
            } catch (e) {
                console.error("HighlightEngine.restoreHighlight error:", e);
            }
        },

        removeHighlight: function() {
            try {
                var selection = window.getSelection();
                if (!selection || !selection.rangeCount) return;
                
                var range = selection.getRangeAt(0);
                var container = range.commonAncestorContainer;
                if (container.nodeType === 3) container = container.parentNode;
                
                var span = container.closest('.kiwix-highlight');
                if (span) {
                    var rangeJSON = span.dataset.range;
                    // Find all spans with the same rangeJSON in case it's multi-node
                    var highlights = document.querySelectorAll('.kiwix-highlight[data-range="' + rangeJSON.replace(/"/g, '\\"') + '"]');
                    if (highlights.length === 0) highlights = [span]; // Fallback
                    
                    highlights.forEach(h => {
                        var parent = h.parentNode;
                        if (parent) {
                            while (h.firstChild) parent.insertBefore(h.firstChild, h);
                            parent.removeChild(h);
                            parent.normalize();
                        }
                    });
                    
                    if (window.HighlightInterface && rangeJSON) {
                        window.HighlightInterface.onHighlightDeleted(rangeJSON);
                    }
                }
            } catch (e) {
                console.error("HighlightEngine.removeHighlight error:", e);
            }
        },

        isSelectionHighlighted: function() {
            try {
                var selection = window.getSelection();
                if (!selection || !selection.rangeCount) return false;
                
                var range = selection.getRangeAt(0);
                var container = range.commonAncestorContainer;
                if (container.nodeType === 3) container = container.parentNode;
                
                return container.closest('.kiwix-highlight') !== null;
            } catch (e) {
                return false;
            }
        },

        getNodeByPath: function(path) {
            try {
                var result = document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                return result.singleNodeValue;
            } catch (e) {
                console.error("HighlightEngine.getNodeByPath error for path " + path + ":", e);
                return null;
            }
        },

        clearAll: function() {
            var highlights = document.querySelectorAll('.kiwix-highlight');
            highlights.forEach(function(h) {
                var parent = h.parentNode;
                if (parent) {
                    while(h.firstChild) parent.insertBefore(h.firstChild, h);
                    parent.removeChild(h);
                }
            });
        }
    };
    window.HighlightEngine = HighlightEngine;
}
