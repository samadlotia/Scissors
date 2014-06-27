package scissors.internal;

import java.util.List;

import  java.awt.geom.Point2D;
import  java.awt.geom.Rectangle2D;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

class ForceDirectedLayout extends AbstractTask {
  final int iterations = 100;
  final CyNetworkView netView;
  final List<Partition> partitions;

  public ForceDirectedLayout(final CyNetworkView netView, final List<Partition> partitions) {
    this.netView = netView;
    this.partitions = partitions;
  }

  public void run(final TaskMonitor monitor) {
    final CyNetwork network = netView.getModel();
    final List<CyNode> nodes = network.getNodeList();
    final int n = nodes.size();
    final int[] membership = new int[n];
    for (int nodei = 0; nodei < n; nodei++) {
      final CyNode node = nodes.get(nodei);
      for (int parti = 0; parti < partitions.size(); parti++) {
        final Partition partition = partitions.get(parti);
        if (partition.getNodes().contains(node)) {
          membership[nodei] = parti;
          break;
        }
      }
    }

    final double[] xs = new double[n];
    final double[] ys = new double[n];
    for (int nodei = 0; nodei < n; nodei++) {
      final CyNode node = nodes.get(nodei);
      final View<CyNode> nodeView = netView.getNodeView(node);
      xs[nodei] = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
      ys[nodei] = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
    }

    final double[] x2s = new double[n];
    final double[] y2s = new double[n];

    for (int trgi = 0; trgi < n; trgi++) {
      final int trgMembership = membership[trgi];
      final double trgx = xs[trgi];
      final double trgy = ys[trgi];
      double trg2x = trgx;
      double trg2y = trgy;
      for (int srci = 0; srci < n; srci++) {
        final double srcx = xs[srci];
        final double srcy = ys[srci];
        final double deltax = trgx - srcx;
        final double deltay = trgy - srcy;
        final boolean isConnected = network.containsEdge(nodes.get(trgi), nodes.get(srci));
        if (isConnected) {
          trg2x -= 0.004 * deltax;
          trg2y -= 0.004 * deltay;
        } else {
          final boolean inSamePartition = trgMembership == membership[srci];
          if (inSamePartition) {
            trg2x -= 0.002 * deltax;
            trg2y -= 0.002 * deltay;
          } else {
            trg2x += 0.002 * deltax;
            trg2y += 0.002 * deltay;
          }
        }
      }
      x2s[trgi] = trg2x;
      y2s[trgi] = trg2y;
    }
    System.arraycopy(x2s, 0, xs, 0, n);
    System.arraycopy(y2s, 0, ys, 0, n);
    for (int nodei = 0; nodei < n; nodei++) {
      final CyNode node = nodes.get(nodei);
      final View<CyNode> nodeView = netView.getNodeView(node);
      nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, xs[nodei]);
      nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, ys[nodei]);
    }
    netView.updateView();
  }
}