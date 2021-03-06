package scissors.internal;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;

public class CyActivator extends AbstractCyActivator {
  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
      props.put(vals[i], vals[i + 1]);
    return props;
  }

  public void start(BundleContext bc) {
    final CyNetworkManager netMgr = getService(bc, CyNetworkManager.class);
    final CyApplicationManager appMgr = getService(bc, CyApplicationManager.class);
    final CySwingApplication swingApp = getService(bc, CySwingApplication.class);
    final TaskManager taskMgr = getService(bc, DialogTaskManager.class);

    super.registerAllServices(bc, new ScissorsPanel(appMgr, swingApp, taskMgr), ezProps());
  }
}
