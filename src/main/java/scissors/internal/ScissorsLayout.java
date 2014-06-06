package scissors.internal;

import java.util.List;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

class ScissorsLayout extends AbstractTask {
  static final double GROUP_WIDTH = 700.0;
  static final double NODE_X_SPACE = 10.0;
  static final double NODE_Y_SPACE = 10.0;
  static final double GROUP_X_SPACE = 150.0;
  static final double GROUP_Y_SPACE = 150.0;

  final CyNetworkView netView;
  final List<List<CyNode>> groups;

  public ScissorsLayout(final CyNetworkView netView, final List<List<CyNode>> groups) {
    this.netView = netView;
    this.groups = groups;
  }

  public void run(final TaskMonitor monitor) {
    final int ngroups = groups.size();
    final int cols = (int) Math.ceil(Math.sqrt(ngroups));
  }
}