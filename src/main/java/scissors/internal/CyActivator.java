package scissors.internal;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.ServiceProperties;

public class CyActivator extends AbstractCyActivator {
  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
      props.put(vals[i], vals[i + 1]);
    return props;
  }

  public void start(BundleContext bc) {
    super.registerAllServices(bc, new ScissorsPanel(), ezProps());
  }
}
