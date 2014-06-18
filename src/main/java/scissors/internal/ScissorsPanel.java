package scissors.internal;

import java.io.InputStream;

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyNode;

import org.cytoscape.view.model.CyNetworkView;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

class ScissorsPanel implements CytoPanelComponent, SetCurrentNetworkViewListener {
  final CyApplicationManager appMgr;
  final TaskManager taskMgr;
  final ImageIcon icon;

  public ScissorsPanel(final CyApplicationManager appMgr, final TaskManager taskMgr) {
    this.appMgr = appMgr;
    this.taskMgr = taskMgr;
    final IconCreator iconCreater = new IconCreator();
    icon = iconCreater.newIcon(15.0f, "\uf0c4");
  }

  public Component getComponent() {
    final EasyGBC c = new EasyGBC();

    final JPanel panel = new JPanel(new GridBagLayout());
    return panel;
  }

  public CytoPanelName getCytoPanelName() {
    return CytoPanelName.WEST;
  }

  public Icon getIcon() {
    return icon;
  }

  public String getTitle() {
    return "Scissors ";
  }

  public void handleEvent(SetCurrentNetworkViewEvent e) {
  }
}

class IconCreator {
  final Font font;
  final FontRenderContext context;

  public IconCreator() {
    font = loadFont();
    context = new FontRenderContext(null, true, true);
  }

  public ImageIcon newIcon(final float size, final String iconStr) {
    if (font == null) {
      return null;
    }
    final Font scaledFont = font.deriveFont(size);
    final Rectangle2D bounds = scaledFont.getStringBounds(iconStr, context);
    final int w = (int) Math.ceil(bounds.getWidth());
    final int h = (int) Math.ceil(bounds.getHeight());
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2d = img.createGraphics();
    g2d.setPaint(Color.BLACK);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setFont(scaledFont);
    final float y = h - g2d.getFontMetrics(scaledFont).getLineMetrics(iconStr, g2d).getDescent();
    g2d.drawString(iconStr, 0.0f, y);
    return new ImageIcon(img);
  }

  static Font loadFont() {
    InputStream contents = null;
    try {
      contents = IconCreator.class.getResourceAsStream("/scissors/fontawesome-webfont.ttf");
      return Font.createFont(Font.TRUETYPE_FONT, contents);
    } catch (Exception e) {
      return null;
    } finally {
      try {
        contents.close();
      } catch (Exception e) {}
    }
  }
}