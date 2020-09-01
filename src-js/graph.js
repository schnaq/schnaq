class SchnaqD3 {
  constructor(d3, parentId, data, width, height, nodeSize, textwrap) {
    this.d3 = d3;
    this.parentId = parentId;
    this.data = data;
    this.width = width;
    this.height = height;
    this.color = d3.scaleOrdinal(d3.schemeCategory10);
    this.adjlist = [];
    this.svg = this.resizeCanvas(width, height);
    this.graphForces = d3.forceSimulation();
    this.labelForces = d3.forceSimulation();
    this.d3.textwrap = textwrap;
    this.NODE_SIZE = nodeSize;

    this.initializeGraph(data, width, height, nodeSize);

    this.svg.call(
      d3.zoom()
        .scaleExtent([.1, 4])
        .on("zoom", () => {
          this.container.attr("transform", d3.event.transform);
        })
    );
  }

  neigh(a, b) {
    return a === b || this.adjlist[a + "-" + b];
  }

  ticked(that) {
    // Fix root node to center.
    let rootNode = that.data.nodes.find(node => node.type === "agenda");
    rootNode.fx = that.width / 2;
    rootNode.fy = that.height / 2;

    that.node.call(node => {
      that.updateNode(that, node)
    });
    that.link.call(link => {
      that.updateLink(that, link)
    });

    that.labelForces.alphaTarget(0.3).restart();
    that.labelNode.each(function (d, i) {
      if (i % 2 === 0) {
        d.x = d.node.x;
        d.y = d.node.y;
      } else {

        let x = d.node.x - that.NODE_SIZE/2;
        this.setAttribute("transform", "translate(" + x + "," + d.node.y + ")");
      }
    });
  }

  fixna(coord) {
    if (isFinite(coord)) return coord;
    return 0;
  }

  focus(that) {
    if (that.d3.event) {
      let index = that.d3.select(that.d3.event.target).datum().index;
      that.node.style("opacity", link => {
        return that.neigh(index, link.index) ? 1 : 0.1;
      });
      that.labelNode.attr("display", link => {
        return that.neigh(index, link.node.index) ? "block" : "none";
      });
      that.link.style("opacity", link => {
        return link.source.index === index || link.target.index === index ? 1 : 0.1;
      });
    }
  }

  unfocus(that) {
    that.labelNode.attr("display", "block");
    that.node.style("opacity", 1);
    that.link.style("opacity", 1);
  }

  updateLink(that, link) {
    link.attr("x1", link => {
      return that.fixna(link.source.x);
    })
      .attr("y1", link => {
        return that.fixna(link.source.y);
      })
      .attr("x2", link => {
        return that.fixna(link.target.x);
      })
      .attr("y2", link => {
        return that.fixna(link.target.y);
      });
  }

  updateNode(that, node) {
    node.attr("transform", node => {
      return "translate(" + that.fixna(node.x) + "," + that.fixna(node.y) + ")";
    });
  }

  dragstarted(that, d) {
    that.d3.event.sourceEvent.stopPropagation();
    if (!that.d3.event.active) that.graphForces.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  dragged(that, d) {
    d.fx = that.d3.event.x;
    d.fy = that.d3.event.y;
  }

  dragended(that, d) {
    if (!that.d3.event.active) that.graphForces.alphaTarget(0);
    d.fx = null;
    d.fy = null;
  }

  // Public Methods, not used as event-handlers

  resizeCanvas(width, height) {
    this.width = width;
    this.height = height;
    return this.d3.select(this.parentId).attr("width", width).attr("height", height);
  }

  centerForces(forceObject, width, height) {
    return forceObject
      .force("center", this.d3.forceCenter(width / 2, height / 2))
      .force("x", this.d3.forceX(width / 2).strength(1.5))
      .force("y", this.d3.forceY(height / 2).strength(1.5));
  }

  setLinkForces(forceObject) {
    return forceObject
      .force("charge", this.d3.forceManyBody().strength(-3000))
      .force("link", this.d3.forceLink(this.data.links).id(d => {
        return d.id;
      }).distance(75).strength(2))
      .on("tick", () => {
        this.ticked(this)
      });
  }

  setNodeForces(forceObject, nodes, width, height) {
    let forces = forceObject.nodes(nodes);
    forces = this.centerForces(forces, width, height);
    return this.setLinkForces(forces);
  }

  setLabelForces(force, labels) {
    return force
      .nodes(labels.nodes)
      .force("charge", this.d3.forceManyBody().strength(-50))
      .force("link", this.d3.forceLink(labels.links).distance(0).strength(2));
  }

  setSVG(node) {
    let svgPath;
    switch (node.type) {
      case "starting-argument":
        svgPath = "imgs/graph/bubble_light_blue_graph.svg";
        break;
      case "attack":
        svgPath = "imgs/graph/bubble_orange_graph.svg";
        break;
      case "undercut":
        svgPath = "imgs/graph/bubble_orange_graph.svg";
        break;
      case "support":
        svgPath = "imgs/graph/bubble_blue_graph.svg";
        break;
      default:
        svgPath = "imgs/graph/bubble_light_blue_graph.svg";
    }
    return svgPath;
  }

  fillNode(node) {
    let color;
    let blueLight = "#4cacf4";
    let blue = "#1292ee";
    let orange = "#ff772d";

    switch (node.type) {
      case "starting-argument":
        color = blueLight
        break;
      case "attack":
        color = orange;
        break;
      case "undercut":
        color = orange;
        break;
      case "support":
        color = blue;
        break;
      default:
        color = blueLight;
    }
    return color;
  }

  drawNodes(data, size) {
    let width = size + 10;
    let height = size * 0.75;
    this.node = this.container.append("g").attr("class", "nodes")
      .selectAll("g")
      .data(data.nodes)
      .enter()
      .append("rect")
      .attr("width", width)
      .attr("height", height)
      .attr("x", -width/ 2)
      .attr("y", -height / 2)
      .attr("rx", 4)
      .style("fill", node => {
        return this.fillNode(node);
      })
  }

  chooseColor(link) {
    let chosenColor;
    switch (link.type) {
      case "undercut":
        chosenColor = "#333";
        break;
      case "support":
        chosenColor = "#999";
        break;
      case "attack":
        chosenColor = "#444";
        break;
      case "starting":
        chosenColor = "#888";
        break;
      default:
        chosenColor = "#666";
    }
    return chosenColor;
  }

  drawLinks(data) {
    this.link = this.container.append("g").attr("class", "links")
      .selectAll("line")
      .data(data.links)
      .enter()
      .append("line")
      .attr("stroke", link => {
        return this.chooseColor(link);
      })
      .attr("stroke-width", "1px");
  }

  drawLabels(data) {

    this.labelNode = this.container.append("g").attr("class", "labelNodes")
      .selectAll("text")
      .data(data.nodes)
      .enter()
      .append("text")
      .text((node, index) => {
        return index % 2 === 0 ? "" : node.node.content;
      })
      .style("fill", "#FFF")
      .style("font-family", "Arial")
      .style("font-size", 6)
      .style("pointer-events", "none") // to prevent mouseover/drag capture
      .style("text-anchor", "start");
  }

  createLabels(data) {
    let labels = {
      "nodes": [],
      "links": []
    };
    data.nodes.forEach((node, index) => {
      labels.nodes.push({node: node});
      labels.nodes.push({node: node});
      labels.links.push({
        source: index * 2,
        target: index * 2 + 1
      });
    });
    return labels;
  }

  buildAdjacencyMatrix(data, adjlist) {
    data.links.forEach(link => {
      adjlist[link.source.index + "-" + link.target.index] = true;
      adjlist[link.target.index + "-" + link.source.index] = true;
    });
    return adjlist
  }

  setMouseOverEvents() {
    this.node.on("mouseover", () => {
      this.focus(this)
    }).on("mouseout", () => {
      this.unfocus(this)
    });
  }

  setDragEvents() {
    this.node.call(
      this.d3.drag()
        .on("start", d => {
          this.dragstarted(this, d)
        })
        .on("drag", d => {
          this.dragged(this, d)
        })
        .on("end", d => {
          this.dragended(this, d)
        })
    );
  }

  initializeGraph(data, width, height, nodeSize) {
    this.container = this.svg.append("g");
    this.drawLinks(data);
    this.drawNodes(data, nodeSize);

    let labels = this.createLabels(data);
    this.drawLabels(labels);

    this.graphForces = this.setNodeForces(this.graphForces, data.nodes, width, height);
    // Note: everything that ticked calls from `that` should be defined before.
    this.labelForces = this.setLabelForces(this.labelForces, labels);

    this.adjlist = this.buildAdjacencyMatrix(data, this.adjlist);

    this.setMouseOverEvents();
    this.setDragEvents();

    let wrap = this.d3.textwrap().bounds({height: nodeSize/2, width: nodeSize}).method('tspans');
    // select all text nodes
    let text = this.d3.selectAll('text');
    // run the text wrapping function on all text nodes
    text.call(wrap);
  }

  setSize(width, height) {
    this.resizeCanvas(width, height);
    this.graphForces = this.centerForces(this.graphForces, width, height);
    return this;
  }

  replaceData(data, width, height, nodeSize) {
    this.width = width;
    this.height = height;
    this.data = data;
    this.svg.selectAll("*").remove();
    this.initializeGraph(data, width, height, nodeSize);
    console.log("replace data");
  }

}

export {SchnaqD3};