class SchnaqD3 {
  constructor(d3, parentId, data, width, height) {
    this.d3 = d3;
    this.parentId = parentId;
    this.data = data;
    this.width = width;
    this.height = height;
    let INITIAL_NODE_SIZE = 5;
    this.color = d3.scaleOrdinal(d3.schemeCategory10);
    this.adjlist = [];
    this.svg = this.resizeCanvas(width, height);
    this.graphForces = d3.forceSimulation();
    this.labelForces = d3.forceSimulation();

    this.initializeGraph(data, width, height, INITIAL_NODE_SIZE);

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
    that.labelNode.call(node => {
      that.updateNode(that, node)
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
      }).distance(50).strength(1))
      .on("tick", () => {
        this.ticked(this)
      });
  }

  setNodeForces(forceObject, nodes, width, height) {
    let forces = forceObject.nodes(nodes);
    forces = this.centerForces(forces, width, height);
    return this.setLinkForces(forces);
  }

  drawNodes(data, size) {
    this.node = this.container.append("g").attr("class", "nodes")
      .selectAll("g")
      .data(data.nodes)
      .enter()
      .append("circle")
      .attr("r", size)
      .attr("fill", node => {
        return this.color(node.type);
      });
  }

  chooseColor(link) {
    let chosenColor;
    switch (link.type) {
      case "undercut":
        chosenColor = "#990000";
        break;
      case "support":
        chosenColor = "#009933";
        break;
      case "attack":
        chosenColor = "#ff0000";
        break;
      case "starting":
        chosenColor = "#0033cc";
        break;
      default:
        chosenColor = "#aaa";
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

  drawLabels(labels) {
    this.labelNode = this.container.append("g").attr("class", "labelNodes")
      .selectAll("text")
      .data(labels.nodes)
      .enter()
      .append("text")
      .text((node, index) => {
        return index % 2 === 0 ? "" : node.node.content;
      })
      .style("fill", "#555")
      .style("font-family", "Arial")
      .style("font-size", 12)
      .style("pointer-events", "none"); // to prevent mouseover/drag capture
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

  setLabelForces(force, labels) {
    return force
      .nodes(labels.nodes)
      .force("charge", this.d3.forceManyBody().strength(-50))
      .force("link", this.d3.forceLink(labels.links).distance(0).strength(2));
  }

  initializeGraph(data, width, height, nodeSize) {
    this.container = this.svg.append("g");
    this.drawNodes(data, nodeSize);
    this.drawLinks(data);

    let labels = this.createLabels(data);
    this.drawLabels(labels);

    this.graphForces = this.setNodeForces(this.graphForces, data.nodes, width, height);
    // Note: everything that ticked calls from `that` should be defined before.
    this.labelForces = this.setLabelForces(this.labelForces, labels);

    this.adjlist = this.buildAdjacencyMatrix(data, this.adjlist);

    this.setMouseOverEvents();
    this.setDragEvents();
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
  }

}

export {SchnaqD3};