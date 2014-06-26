package scissors.internal;

import java.util.List;

import  java.awt.geom.Point2D;
import  java.awt.geom.Rectangle2D;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

class GridLayout extends AbstractTask {
  final CyNetworkView netView;
  final List<Partition> partitions;

  public GridLayout(final CyNetworkView netView, final List<Partition> partitions) {
    this.netView = netView;
    this.partitions = partitions;
  }

  static final double GROUP_X_SPACE = 150.0;
  static final double GROUP_Y_SPACE = 150.0;

  public void run(final TaskMonitor monitor) {
    final int cols = numOfCols(partitions.size());
    final Point2D.Double pos = new Point2D.Double(0.0, 0.0);
    final Rectangle2D.Double partitionSize = new Rectangle2D.Double();
    double maxH = 0.0;
    int col = 0;
    for (final Partition partition : partitions) {
      layoutPartition(partition, pos, partitionSize);

      col++;
      if (col >= cols) {
        pos.x = 0.0;
        pos.y += maxH + GROUP_Y_SPACE;
        maxH = 0.0;
        col = 0;
      } else {
        pos.x += GROUP_X_SPACE + partitionSize.width;
        maxH = Math.max(maxH, partitionSize.height);
      }
    }

    netView.fitContent();
    netView.updateView();
  }

  static final double NODE_X_SPACE = 10.0;
  static final double NODE_Y_SPACE = 10.0;

  private void layoutPartition(final Partition partition, final Point2D.Double startPt, final Rectangle2D.Double partitionSize) {
    double maxRowH = 0.0;
    double maxX = 0.0;
    double x = startPt.x;
    double y = startPt.y;
    final int cols = numOfCols(partition.getNodes().size());
    int col = 0;


    //System.out.println("---------------");
    for (final CyNode node : partition.getNodes()) {
      //System.out.println(String.format("%s: %d of %d - %f,%f", netView.getModel().getRow(node).get("name", String.class), col, cols, x, y));
      final View<CyNode> view = netView.getNodeView(node);
      view.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
      view.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);

      final double w = view.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
      final double h = view.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT);

      maxX = Math.max(maxX, x);
      col++;
      if (col >= cols) {
        y += maxRowH + NODE_Y_SPACE;
        maxRowH = 0.0;
        x = startPt.x;
        col = 0;
      } else {
        x += w + NODE_X_SPACE;
        maxRowH = Math.max(maxRowH, h);
      }
    }
    partitionSize.setRect(startPt.x, startPt.y, maxX - startPt.x, y - startPt.y);
  }

  static int numOfCols(final int n) {
    return (int) Math.ceil(Math.sqrt(n));
  }
}