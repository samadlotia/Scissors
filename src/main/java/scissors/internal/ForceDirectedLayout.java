package scissors.internal;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import  java.awt.geom.Point2D;
import  java.awt.geom.Rectangle2D;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import prefuse.util.force.DragForce;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;

import org.cytoscape.work.Tunable;

public class ForceDirectedLayout extends AbstractTask {
  static final float DEFAULT_NODE_MASS = 3.0f;
  static final float DEFAULT_SPRING_COEFF = 1e-4f;
  static final float DEFAULT_SPRING_LENGTH = 50.0f;
  static final int DEFAULT_ITERS = 100;
  final CyNetworkView netView;
  final List<Partition> partitions;

  //@Tunable
  public double springCoeff = 1e-7;
  //@Tunable
  public double springLength = 300.0;

  public ForceDirectedLayout(final CyNetworkView netView, final List<Partition> partitions) {
    this.netView = netView;
    this.partitions = partitions;
  }

  public void run(final TaskMonitor monitor) {
    final ForceSimulator sim = new ForceSimulator();
    sim.addForce(new NBodyForce());
    sim.addForce(new SpringForce());
    sim.addForce(new DragForce());

    final CyNetwork network = netView.getModel();

    // create force items for each node
    final List<CyNode> nodes = network.getNodeList();
    final Map<CyNode,ForceItem> items = new HashMap<CyNode,ForceItem>();
    for (final CyNode node : nodes) {
      final ForceItem item = new ForceItem();
      item.mass = DEFAULT_NODE_MASS;
      item.location[0] = 0f;
      item.location[1] = 0f;
      sim.addItem(item);
      items.put(node, item);
    }

    // create springs for each edge
    for (final CyEdge edge : network.getEdgeList()) {
      final CyNode src = edge.getSource();
      final CyNode trg = edge.getTarget();
      final ForceItem srcItem = items.get(src);
      final ForceItem trgItem = items.get(trg);
      if (srcItem == null || trgItem == null) {
        continue;
      }
      sim.addSpring(srcItem, trgItem, DEFAULT_SPRING_COEFF, DEFAULT_SPRING_LENGTH);
    }

    // create springs between all intra-group nodes
    for (final Partition partition : partitions) {
      final Set<CyNode> partitionNodes = partition.getNodes();
      for (final CyNode src : partitionNodes) {
        final ForceItem srcItem = items.get(src);
        for (final CyNode trg : partitionNodes) {
          final ForceItem trgItem = items.get(trg);
          sim.addSpring(srcItem, trgItem, (float) springCoeff, (float) springLength);
        }
      }
    }

    // run force simulation
    final int iters = DEFAULT_ITERS;
    long timestep = 1000L;
    for (int i = 0; i < iters && !cancelled; i++) {
      timestep *= (1.0 - i/(double)iters);
      long step = timestep+50;
      sim.runSimulator(step);
      monitor.setProgress(i / ((double) iters));
    }

    // update node locations
    for (final CyNode node : nodes) {
      final ForceItem item = items.get(node);
      final double x = item.location[0];
      final double y = item.location[1];
      final View<CyNode> nodeView = netView.getNodeView(node);
      nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
      nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
    }

    netView.fitContent();
    netView.updateView();
  }
}