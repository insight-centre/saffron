
var width = Math.min(1200,Math.max(document.getElementById('graph').offsetWidth,500)),
    height = Math.min(800,Math.max(document.getElementById('graph').offsetWidth * .6,500)),
    root;

var force = d3.layout.force()
    .linkDistance(40)
    .charge(-80)
    .gravity(.05)
    .size([width, height])
    .on("tick", tick);

var svg = d3.select("#graph").append("svg")
    .attr("width", width)
    .attr("height", height)
    .call(d3.behavior.zoom().on("zoom", function () {
    svg.attr("transform", "translate(" + d3.event.translate + ")" + " scale(" + d3.event.scale + ")")
  }));

var link = svg.selectAll(".link"),
    node = svg.selectAll(".node");

d3.json("/" + saffronDatasetName + "/taxonomy", function(error, json) {
  if (error) throw error;
  
  root = json;
  update();
});

function count(root) {
    var c = 1;
    if(root.children) {
        root.children.forEach(function(child) {
            c += count(child);
        });
    }
    return c;
}

function sumScore(root) {
    var s = root.score;
    if(root.children) {
        root.children.forEach(function(child) {
            s += sumScore(child);
        });
    }
    return s;
    
}

function maxRecurse(root) {
    var m = root.score;
    if(root.children) {
        root.children.forEach(function(child) {
            m = Math.max(m,maxRecurse(child));
        });
    }
    return m;
}
var maxScore = 1;
var rootLabel = "";

function update() {
  var nodes = flatten(root),
      links = d3.layout.tree().links(nodes);

  var ave = sumScore(root) / count(root);
  maxScore = maxRecurse(root);
  rootLabel = root.root;

  // Restart the force layout.
  force
      .nodes(nodes)
      .links(links)
      .start();

  // Update links.
  link = link.data(links, function(d) { return d.target.id; });

  link.exit().remove();

  link.enter().insert("line", ".node")
      .style("stroke-width", function(d) { if(d.target.linkScore && !isNaN(d.target.linkScore)) { return d.target.linkScore * 2 + 1; } else { return 1; } })
      .attr("class", "link");

  // Update nodes.
  node = node.data(nodes, function(d) { return d.id; });

  node.exit().remove();

  var nodeEnter = node.enter().append("g")
      .attr("class", nodeClass)
      .on("click", click)
      .call(force.drag);

  nodeEnter.append("circle")
      .attr("r", function(d) { return Math.max(Math.sqrt(d.score / ave * 15 || 4.5),5); });

  nodeEnter.append("text")
      .attr("dy", ".35em")
      .text(function(d) { return "\u00a0\u00a0\u00a0" + d.root; });

  node.select("circle")
       .style("fill", color);
      
}

function tick() {
  link.attr("x1", function(d) { return d.source.x; })
      .attr("y1", function(d) { return d.source.y; })
      .attr("x2", function(d) { return d.target.x; })
      .attr("y2", function(d) { return d.target.y; });

  node.attr("transform", function(d) { 
      if(d.root == rootLabel) {
          d.x = width/2;
          d.y = height/2;
      }
      return "translate(" + d.x + "," + d.y + ")"; 
  });
}
function componentToHex(c) {
    var hex = c.toString(16);
    return hex.length === 1 ? "0" + hex : hex;
}

function rgbToHex(r, g, b) {
    return "#" + componentToHex(r) + componentToHex(g) + componentToHex(b);
}

function color(d) {
    var amount = Math.round(200 * (d.score / maxScore));
    if(d.children) {
        if(d.children.length > 1) {
            var green = 200;
        } else {
            var green = 150;
        }
    } else {
        var green = 10;
    }
    var c = rgbToHex(amount+55, Math.round(green * (300 - amount) / 300), 256-amount);
    return c;
  //return d._children ? "#3182bd" // collapsed package
   //   : d.children ? "#c6dbef" // expanded package
    //  : "#fd8d3c"; // leaf node
}

function nodeClass(d) {
    if(d.score > maxScore/3) {
        return "node_important";
    } else {
        return "node";
    }
}

// Toggle children on click.
function click(d) {
  if (d3.event.defaultPrevented) return; // ignore drag
  if (d.children && d.children.length > 0) {
        d._children = d.children;
        d.children = null;
  } else {
        d.children = d._children;
        d._children = null;
  }
  update();
}

// Returns a list of all nodes under the root.
function flatten(root) {
  var nodes = [], i = 0;

  function recurse(node) {
    if (node.children) node.children.forEach(recurse);
    if (!node.id) node.id = ++i;
    nodes.push(node);
    //if(node.score < 5) {
    //    node._children = node.children;
    //    node.children = null;
    //}
  }

  recurse(root);
  return nodes;
}