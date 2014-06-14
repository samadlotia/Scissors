package scissors.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import javax.swing.SwingUtilities;

public class ImportNodeListTaskFactory implements TaskFactory {
  final CyApplicationManager appMgr;
  final CySwingApplication swingApp;
  final CyNetworkManager netMgr;

  public ImportNodeListTaskFactory(
      final CyApplicationManager appMgr,
      final CySwingApplication swingApp,
      final CyNetworkManager netMgr
      ) {
    this.appMgr = appMgr;
    this.swingApp = swingApp;
    this.netMgr = netMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new AbstractTask() {
      public void run(TaskMonitor monitor) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            new ImportNodeListDialog(swingApp.getJFrame(), netMgr, appMgr);
          }
        });
      }
    });
  }

  public boolean isReady() {
    return appMgr.getCurrentNetwork() != null;
  }
}