var width = $("#graph").width(),
    height = Math.min(800,Math.max(document.getElementById('graph').offsetWidth * .6,800)),
    root;

var force = d3.layout.force()
    .linkDistance(80)
    .charge(-80)
    .gravity(.08)
    .alpha(0)
    .size([width, height])
    .on("tick", tick);

var zoom = d3.behavior.zoom().scaleExtent([1, 10]).on("zoom", zoomed);

var svg = d3.select("#graph").append("svg")
    .attr("width", width)
    .attr("height", height)
    .call(zoom)
    .append("g");

var link = svg.selectAll(".link"),
    node = svg.selectAll(".node");


d3.json(apiUrl + saffronDatasetName, function(error, json) {
  if (error) throw error;
  root = json;
  if (root.root === "HEAD_TERM") {
    root.root = "Root";
  }
  var links = graph.links;
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
      .attr("class", "link");

  // Update nodes.
  node = node.data(nodes, function(d) { return d.id; });

  node.exit().remove();
  var displayThreshold = 30;

  var nodeEnter = node.enter().append("g")
      .attr("class", nodeClass)
      .on("click", function (d) {
          if (!location.href.endsWith("/edit")) {
            location.href = "term/" + d.root;  
          }
          
      })
      .style("cursor","pointer");

  nodeEnter.append("a")
      .attr("href", function(d) { 
        if (location.href.endsWith("/edit")) {
            return location.href;  
          } else {
            return "term/" + d.root;  
          }
       })
      .append("text")
      .each(function(d) {
          if(d.score>maxScore/3){
            d.scaleThreshold = 10;
          } else {
            d.scaleThreshold = Math.sqrt(displayThreshold / 10);
            d.opacityScale = d3.scale.linear() 
            .domain([d.scaleThreshold, d.scaleThreshold * 1.3])
            .range([0, 1]);
          }
      })
      .text(function(d) { return d.name })
      .attr('opacity', function(d) {
        if (d.scaleThreshold < 1  || d.scaleThreshold===10) {
          return 1;
        }
        return 0;
      })
      .attr("dy", function(d) { return d.orient === "left" || d.orient === "right" ? ".35em" : d.orient === "bottom" ? ".71em" : null; })
      .attr("x", function(d) { return d.orient === "right" ? 6 : d.orient === "left" ? -6 : null; })
      .attr("y", function(d) { return d.orient === "bottom" ? 6 : d.orient === "top" ? -6 : null; })
      .text(function(d, i) { return i * 1000; })
      .attr("dy", ".35em")
      .text(function(d) { return "\u00a0\u00a0\u00a0" + d.root; });

    nodeEnter.append("circle")
      .on("mouseover", fade(.2))
      .on("mouseout", fade2(1))
      .attr("r", function(d) { return Math.max(Math.sqrt(d.score / ave * 10 || 4.5),5); })
      .text(function(d) { return d.name; })

    node.select("circle")
        .style("fill", color);

}

function isConnected(a, b) {
  var linkedByIndex = {};

  force.nodes().forEach(function(node) {
    linkedByIndex[node.index] = linkedByIndex[node.index] || [];
  })

  force.links().forEach(function(link) {
    linkedByIndex[link.source.index + "," + link.target.index] = 1;
  })
  return linkedByIndex[a.index + "," + b.index] || linkedByIndex[b.index + "," + a.index] || a.index == b.index;
}

function fade(opacity) {
  return function(d) {
    node.style("stroke-opacity", function(o) {
      thisOpacity = isConnected(d, o) ? 1 : opacity;
      return thisOpacity;
    });
    node.style("fill-opacity", function(o) {
      thisOpacity = isConnected(d, o) ? 1 : opacity;
      return thisOpacity;
    });
    node.select('text')
      .attr('opacity', function(d) {
        return 1;
    })
    link.style("stroke-opacity", function(o) {
      return o.source === d || o.target === d ? 1 : opacity;
    });
  };
}

function fade2(opacity) {
  return function(d) {
    node.style("stroke-opacity", function(o) {
      thisOpacity = isConnected(d, o) ? 1 : opacity;
      return thisOpacity;
    });
    node.style("fill-opacity", function(o) {
      thisOpacity = isConnected(d, o) ? 1 : opacity;
      return thisOpacity;
    });
    node.select('text')
    .attr('opacity', function(d) {
      if(d.scaleThreshold===10) {
        return 1;
      }
      if (zoom.scale() > d.scaleThreshold) {
        return d.opacityScale(zoom.scale());
      } 
      return 0;
    })
    link.style("stroke-opacity", function(o) {
      return o[0] === d || o[2] === d ? 1 : opacity;
    });
  };
}

function tick() {
  var linkNode = svg.selectAll(".link-node");
  linkNode.attr("cx", function(d) { return d.x = (d.source.x + d.target.x) * 0.8; })
          .attr("cy", function(d) { return d.y = (d.source.y + d.target.y) * 0.8; });

  link.attr("x1", function(d) { return d.source.x; })
      .attr("y1", function(d) { return d.source.y; })
      .attr("x2", function(d) { return d.target.x; })
      .attr("y2", function(d) { return d.target.y; });

  // Calming down initial tick of a force layout
  // if the tick is needed, toggle comments the following lines
  // node.attr("transform", function(d) { 
  //     if(d.root == rootLabel) {
  //         d.x = width/2;
  //         d.y = height/2;
  //     }
  //     return "translate(" + d.x + "," + d.y + ")"; 
  // });
  var k = 0;
  while ((force.alpha() > 1e-2) && (k < 150)) {
    force.tick();
    k = k + 1;
  }
  node.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
  // end calming down initial tick of a force layout
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
}

function nodeClass(d) {
    if(d.score > maxScore/3) {
        return "node_important";
    } else {
        return "node";
    }
}

// Returns a list of all nodes under the root.
function flatten(root) {
  var nodes = [], i = 0;

  function recurse(node) {
    if (node.children) node.children.forEach(recurse);
    if (!node.id) node.id = ++i;
    nodes.push(node);
  }

  recurse(root);
  return nodes;
}

function zoomed() {
  svg.attr("transform",
      "translate(" + zoom.translate() + ")" +
      "scale(" + zoom.scale() + ")"
  );

  var link = svg.selectAll(".link"),
  node = svg.selectAll(".node");


  node.select('text')
    .attr('opacity', function(d) {
      if(d.scaleThreshold===10) {
        return 1;
      }
      if (zoom.scale() > d.scaleThreshold) {
        return d.opacityScale(zoom.scale());
      } 
      return 0;
    });
}

function interpolateZoom (translate, scale) {
  var self = this;
  return d3.transition().duration(350).tween("zoom", function () {
  var iTranslate = d3.interpolate(zoom.translate(), translate),
    iScale = d3.interpolate(zoom.scale(), scale);
    return function (t) {
      zoom
        .scale(iScale(t))
        .translate(iTranslate(t));
      zoomed();
    };
  });
}

function zoomClick() {
  var clicked = d3.event.target,
    direction = 1,
    factor = 0.2,
    target_zoom = 1,
    center = [width / 2, height / 2],
    extent = zoom.scaleExtent(),
    translate = zoom.translate(),
    translate0 = [],
    l = [],
    view = {x: translate[0], y: translate[1], k: zoom.scale()};

    d3.event.preventDefault();
    direction = (this.id === 'zoom_in') ? 1 : -1;
    target_zoom = zoom.scale() * (1 + factor * direction);

    if (target_zoom < extent[0] || target_zoom > extent[1]) { return false; }

    translate0 = [(center[0] - view.x) / view.k, (center[1] - view.y) / view.k];
    view.k = target_zoom;
    l = [translate0[0] * view.k + view.x, translate0[1] * view.k + view.y];

    view.x += center[0] - l[0];
    view.y += center[1] - l[1];

    interpolateZoom([view.x-10, view.y-10], view.k);
}

d3.selectAll('button').on('click', zoomClick);
