class SchnaqD3 {
  constructor(d3, parentId, data) {
    let that = this;
    this.d3 = d3;
    this.parentId = parentId;
    this.data = data;
    this.width = 800;
    this.height = 600;
    this.color = d3.scaleOrdinal(d3.schemeCategory10);

    this.label = {
      'nodes': [],
      'links': []
    };

    this.adjlist = [];

    data.nodes.forEach((node, index) => {
      this.label.nodes.push({node: node});
      this.label.nodes.push({node: node});
      this.label.links.push({
        source: index * 2,
        target: index * 2 + 1
      });
    });

    this.labelLayout = d3.forceSimulation(this.label.nodes)
      .force("charge", d3.forceManyBody().strength(-50))
      .force("link", d3.forceLink(this.label.links).distance(0).strength(2));

    this.graphLayout = d3.forceSimulation(data.nodes)
      .force("charge", d3.forceManyBody().strength(-3000))
      .force("center", d3.forceCenter(this.width / 2, this.height / 2))
      .force("x", d3.forceX(this.width / 2).strength(1))
      .force("y", d3.forceY(this.height / 2).strength(1))
      .force("link", d3.forceLink(data.links).id(d => {
        return d.id;
      }).distance(50).strength(1))
      .on("tick", this.ticked);
    // TODO ticked unresolved

    data.links.forEach(link => {
      this.adjlist[link.source.index + "-" + link.target.index] = true;
      this.adjlist[link.target.index + "-" + link.source.index] = true;
    });

    this.svg = d3.select(parentId).attr("width", this.width).attr("height", this.height);
    this.container = this.svg.append("g");
    this.svg.call(
      d3.zoom()
        .scaleExtent([.1, 4])
        .on("zoom", function () {
          that.container.attr("transform", d3.event.transform);
        })
    );

    this.link = this.container.append("g").attr("class", "links")
      .selectAll("line")
      .data(data.links)
      .enter()
      .append("line")
      .attr("stroke", "#aaa")
      .attr("stroke-width", "1px");

    this.node = this.container.append("g").attr("class", "nodes")
      .selectAll("g")
      .data(data.nodes)
      .enter()
      .append("circle")
      .attr("r", 5)
      .attr("fill", node => {
        return color(node.group);
      })
    this.node.on("mouseover", focus).on("mouseout", this.unfocus);
    // TODO unfocus is unreverenced

    this.node.call(
      d3.drag()
        .on("start", this.dragstarted)
        .on("drag", this.dragged)
        .on("end", this.dragended)
      //TODO unreferenced drag-methods
    );

    this.labelNode = this.container.append("g").attr("class", "labelNodes")
      .selectAll("text")
      .data(this.label.nodes)
      .enter()
      .append("text")
      .text((node, index) => {
        return index % 2 === 0 ? "" : node.node.id;
      })
      .style("fill", "#555")
      .style("font-family", "Arial")
      .style("font-size", 12)
      .style("pointer-events", "none"); // to prevent mouseover/drag capture
  }

  neigh(a, b) {
    return a === b || this.adjlist[a + "-" + b];
  }

  ticked() {

    this.node.call(this.updateNode);
    this.link.call(this.updateLink);

    this.labelLayout.alphaTarget(0.3).restart();
    this.labelNode.each(function (d, i) {
      if (i % 2 === 0) {
        d.x = d.node.x;
        d.y = d.node.y;
      } else {
        let b = this.getBBox();

        let diffX = d.x - d.node.x;
        let diffY = d.y - d.node.y;

        let dist = Math.sqrt(diffX * diffX + diffY * diffY);

        let shiftX = b.width * (diffX - dist) / (dist * 2);
        shiftX = Math.max(-b.width, Math.min(0, shiftX));
        let shiftY = 16;
        this.setAttribute("transform", "translate(" + shiftX + "," + shiftY + ")");
      }
    });
    this.labelNode.call(this.updateNode);

  }

  fixna(coord) {
    if (isFinite(coord)) return coord;
    return 0;
  }

  focus() {
    let index = this.d3.select(this.d3.event.target).datum().index;
    this.node.style("opacity", link => {
      return this.neigh(index, link.index) ? 1 : 0.1;
    });
    this.labelNode.attr("display", link => {
      return this.neigh(index, link.node.index) ? "block" : "none";
    });
    link.style("opacity", link => {
      return link.source.index === index || link.target.index === index ? 1 : 0.1;
    });
  }

  unfocus() {
    this.labelNode.attr("display", "block");
    this.node.style("opacity", 1);
    this.link.style("opacity", 1);
  }

  updateLink(link) {
    link.attr("x1", link => {
      return this.fixna(link.source.x);
    })
      .attr("y1", link => {
        return this.fixna(link.source.y);
      })
      .attr("x2", link => {
        return this.fixna(link.target.x);
      })
      .attr("y2", link => {
        return this.fixna(link.target.y);
      });
  }

  updateNode(node) {
    node.attr("transform", node => {
      return "translate(" + this.fixna(node.x) + "," + this.fixna(node.y) + ")";
    });
  }

  dragstarted(d) {
    this.d3.event.sourceEvent.stopPropagation();
    if (!this.d3.event.active) this.graphLayout.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  dragged(d) {
    d.fx = this.d3.event.x;
    d.fy = this.d3.event.y;
  }

  dragended(d) {
    if (!this.d3.event.active) this.graphLayout.alphaTarget(0);
    d.fx = null;
    d.fy = null;
  }

}

function drawGraph(d3, parent_id, graph) {
  var width = 800;
  var height = 600;
  var color = d3.scaleOrdinal(d3.schemeCategory10);


  var label = {
    'nodes': [],
    'links': []
  };

  graph.nodes.forEach(function (d, i) {
    label.nodes.push({node: d});
    label.nodes.push({node: d});
    label.links.push({
      source: i * 2,
      target: i * 2 + 1
    });
  });

  var labelLayout = d3.forceSimulation(label.nodes)
    .force("charge", d3.forceManyBody().strength(-50))
    .force("link", d3.forceLink(label.links).distance(0).strength(2));

  var graphLayout = d3.forceSimulation(graph.nodes)
    .force("charge", d3.forceManyBody().strength(-3000))
    .force("center", d3.forceCenter(width / 2, height / 2))
    .force("x", d3.forceX(width / 2).strength(1))
    .force("y", d3.forceY(height / 2).strength(1))
    .force("link", d3.forceLink(graph.links).id(function (d) {
      return d.id;
    }).distance(50).strength(1))
    .on("tick", ticked);

  var adjlist = [];

  graph.links.forEach(function (d) {
    adjlist[d.source.index + "-" + d.target.index] = true;
    adjlist[d.target.index + "-" + d.source.index] = true;
  });

  function neigh(a, b) {
    return a == b || adjlist[a + "-" + b];
  }


  var svg = d3.select(parent_id).attr("width", width).attr("height", height);
  var container = svg.append("g");

  svg.call(
    d3.zoom()
      .scaleExtent([.1, 4])
      .on("zoom", function () {
        container.attr("transform", d3.event.transform);
      })
  );

  var link = container.append("g").attr("class", "links")
    .selectAll("line")
    .data(graph.links)
    .enter()
    .append("line")
    .attr("stroke", "#aaa")
    .attr("stroke-width", "1px");

  var node = container.append("g").attr("class", "nodes")
    .selectAll("g")
    .data(graph.nodes)
    .enter()
    .append("circle")
    .attr("r", 5)
    .attr("fill", function (d) {
      return color(d.group);
    })

  node.on("mouseover", focus).on("mouseout", unfocus);

  node.call(
    d3.drag()
      .on("start", dragstarted)
      .on("drag", dragged)
      .on("end", dragended)
  );

  var labelNode = container.append("g").attr("class", "labelNodes")
    .selectAll("text")
    .data(label.nodes)
    .enter()
    .append("text")
    .text(function (d, i) {
      return i % 2 == 0 ? "" : d.node.id;
    })
    .style("fill", "#555")
    .style("font-family", "Arial")
    .style("font-size", 12)
    .style("pointer-events", "none"); // to prevent mouseover/drag capture

  node.on("mouseover", focus).on("mouseout", unfocus);

  function ticked() {

    node.call(updateNode);
    link.call(updateLink);

    labelLayout.alphaTarget(0.3).restart();
    labelNode.each(function (d, i) {
      if (i % 2 == 0) {
        d.x = d.node.x;
        d.y = d.node.y;
      } else {
        var b = this.getBBox();

        var diffX = d.x - d.node.x;
        var diffY = d.y - d.node.y;

        var dist = Math.sqrt(diffX * diffX + diffY * diffY);

        var shiftX = b.width * (diffX - dist) / (dist * 2);
        shiftX = Math.max(-b.width, Math.min(0, shiftX));
        var shiftY = 16;
        this.setAttribute("transform", "translate(" + shiftX + "," + shiftY + ")");
      }
    });
    labelNode.call(updateNode);

  }

  function fixna(x) {
    if (isFinite(x)) return x;
    return 0;
  }

  function focus(d) {
    var index = d3.select(d3.event.target).datum().index;
    node.style("opacity", function (o) {
      return neigh(index, o.index) ? 1 : 0.1;
    });
    labelNode.attr("display", function (o) {
      return neigh(index, o.node.index) ? "block" : "none";
    });
    link.style("opacity", function (o) {
      return o.source.index == index || o.target.index == index ? 1 : 0.1;
    });
  }

  function unfocus() {
    labelNode.attr("display", "block");
    node.style("opacity", 1);
    link.style("opacity", 1);
  }

  function updateLink(link) {
    link.attr("x1", function (d) {
      return fixna(d.source.x);
    })
      .attr("y1", function (d) {
        return fixna(d.source.y);
      })
      .attr("x2", function (d) {
        return fixna(d.target.x);
      })
      .attr("y2", function (d) {
        return fixna(d.target.y);
      });
  }

  function updateNode(node) {
    node.attr("transform", function (d) {
      return "translate(" + fixna(d.x) + "," + fixna(d.y) + ")";
    });
  }

  function dragstarted(d) {
    d3.event.sourceEvent.stopPropagation();
    if (!d3.event.active) graphLayout.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  function dragged(d) {
    d.fx = d3.event.x;
    d.fy = d3.event.y;
  }

  function dragended(d) {
    if (!d3.event.active) graphLayout.alphaTarget(0);
    d.fx = null;
    d.fy = null;
  }

}

function setSize(d3, parent_id, width, height) {
  d3.select(parent_id).attr("width", width).attr("height", height);
}

export {drawGraph, setSize, SchnaqD3};