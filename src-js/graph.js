class SchnaqD3 {
  constructor(d3, parentId, data) {
    this.d3 = d3;
    this.parentId = parentId;
    this.data = data;
    let INITIAL_WIDTH = 800;
    let INITIAL_HEIGHT = 600;
    this.color = d3.scaleOrdinal(d3.schemeCategory10);
    this.adjlist = [];
    this.svg = this.resizeCanvas(INITIAL_WIDTH, INITIAL_HEIGHT);
    this.graphLayout = d3.forceSimulation();
    this.labelLayout = d3.forceSimulation();

    this.initializeGraph(data, INITIAL_WIDTH, INITIAL_HEIGHT);

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
    that.node.call(node => {
      that.updateNode(that, node)
    });
    that.link.call(link => {
      that.updateLink(that, link)
    });

    that.labelLayout.alphaTarget(0.3).restart();
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
    if (!that.d3.event.active) that.graphLayout.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  dragged(that, d) {
    d.fx = that.d3.event.x;
    d.fy = that.d3.event.y;
  }

  dragended(that, d) {
    if (!that.d3.event.active) that.graphLayout.alphaTarget(0);
    d.fx = null;
    d.fy = null;
  }

  // Public Methods, not used as event-handlers

  resizeCanvas(width, height) {
    return this.d3.select(this.parentId).attr("width", width).attr("height", height);
  }

  centerForces(forceObject, width, height) {
    return forceObject
      .force("center", this.d3.forceCenter(width / 2, height / 2))
      .force("x", this.d3.forceX(width / 2).strength(1))
      .force("y", this.d3.forceY(height / 2).strength(1));
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

  initializeGraph(data, width, height) {
    this.container = this.svg.append("g");
    this.node = this.container.append("g").attr("class", "nodes")
      .selectAll("g")
      .data(data.nodes)
      .enter()
      .append("circle")
      .attr("r", 5)
      .attr("fill", node => {
        return this.color(node.group);
      });

    this.link = this.container.append("g").attr("class", "links")
      .selectAll("line")
      .data(data.links)
      .enter()
      .append("line")
      .attr("stroke", "#aaa")
      .attr("stroke-width", "1px");

    this.label = {
      "nodes": [],
      "links": []
    };
    data.nodes.forEach((node, index) => {
      this.label.nodes.push({node: node});
      this.label.nodes.push({node: node});
      this.label.links.push({
        source: index * 2,
        target: index * 2 + 1
      });
    });

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

    this.graphLayout = this.setNodeForces(this.graphLayout, this.data.nodes, width, height);
    // Note: everything that ticked calls from `that` should be defined before.

    this.labelLayout = this.labelLayout
      .nodes(this.label.nodes)
      .force("charge", this.d3.forceManyBody().strength(-50))
      .force("link", this.d3.forceLink(this.label.links).distance(0).strength(2));

    data.links.forEach(link => {
      this.adjlist[link.source.index + "-" + link.target.index] = true;
      this.adjlist[link.target.index + "-" + link.source.index] = true;
    });

    this.node.on("mouseover", () => {
      this.focus(this)
    }).on("mouseout", () => {
      this.unfocus(this)
    });

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

  setSize(width, height) {
    this.resizeCanvas(width, height);
    this.graphLayout = this.centerForces(this.graphLayout, width, height);
    return this;
  }

  replaceData(data, width, height) {
    this.data = data;
    this.svg.selectAll("*").remove();
    this.initializeGraph(data, width, height);
  }

}

export {SchnaqD3};