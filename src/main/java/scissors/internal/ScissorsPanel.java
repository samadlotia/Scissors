package scissors.internal;

import java.io.InputStream;
import java.io.File;

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
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import org.cytoscape.view.model.CyNetworkView;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CySwingApplication;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.FileChooserFilter;

class ScissorsPanel implements CytoPanelComponent, SetCurrentNetworkViewListener {
  final CyApplicationManager appMgr;
  final CySwingApplication swingApp;
  final TaskManager taskMgr;
  final FileUtil fileUtil;

  final IconCreator iconCreator;

  public ScissorsPanel(
      final CyApplicationManager appMgr,
      final CySwingApplication swingApp,
      final TaskManager taskMgr,
      final FileUtil fileUtil) {
    this.appMgr = appMgr;
    this.swingApp = swingApp;
    this.taskMgr = taskMgr;
    this.fileUtil = fileUtil;
    iconCreator = new IconCreator();
  }

  private boolean areThereNodesSelected(final CyNetwork network) {
    if (network == null) {
      return false;
    } else {
      return CyTableUtil.getNodesInState(network, "selected", true).size() > 0;
    }
  }

  private JButton newAddBtn(final NodeListsTableModel nodeListsModel) {
    final AbstractAction fileAction = new AbstractAction("From File", iconCreator.newIcon(15.0f, IconCode.FILE_TEXT_O)) {
      public void actionPerformed(ActionEvent e) {
        final CyNetwork network = appMgr.getCurrentNetwork();
        final File[] files = fileUtil.getFiles(swingApp.getJFrame(), "Choose Node Lists Files", FileUtil.LOAD, Arrays.asList(new FileChooserFilter("Text", ".txt")));
        for (final File file : files) {
          try {
            nodeListsModel.addNodeList(NodeList.newFromFile(file), network);
          } catch (Exception ex) {}
        }
      }
    };

    final AbstractAction selectionAction = new AbstractAction("From Node Selection", iconCreator.newIcon(15.0f, IconCode.CIRCLE_O_NOTCH)) {
      public void actionPerformed(ActionEvent e) {
        final CyNetwork network = appMgr.getCurrentNetwork();
        nodeListsModel.addNodeList(NodeList.newFromSelection(network), network);
      }
    };

    final JPopupMenu menu = new JPopupMenu();
    menu.add(fileAction);
    menu.add(selectionAction);

    final JButton addBtn = new JButton(iconCreator.newIcon(15.0f, IconCode.PLUS_SQUARE));
    addBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final boolean enableSelectionAction = areThereNodesSelected(appMgr.getCurrentNetwork());
        selectionAction.setEnabled(enableSelectionAction);
        menu.show(addBtn, 0, addBtn.getHeight());
      }
    });
    return addBtn;
  }

  public Component getComponent() {
    final NodeListsTableModel nodeListsModel = new NodeListsTableModel();
    final JTable nodeListsTable = new JTable(nodeListsModel);

    final JButton addBtn = newAddBtn(nodeListsModel);
    final JButton rmBtn = new JButton(iconCreator.newIcon(15.0f, IconCode.MINUS_SQUARE));

    final JButton runBtn = new JButton("Cut", iconCreator.newIcon(15.0f, IconCode.SCISSORS));

    final EasyGBC c = new EasyGBC();

    final JPanel nodeListsBtnsPanel = new JPanel(new FlowLayout());
    nodeListsBtnsPanel.add(addBtn);
    nodeListsBtnsPanel.add(rmBtn);

    final JPanel nodeListsPanel = new JPanel(new GridBagLayout());
    nodeListsPanel.add(new JLabel("Node Lists:"), c.reset().expandH());
    nodeListsPanel.add(nodeListsBtnsPanel, c.right());
    nodeListsPanel.add(new JScrollPane(nodeListsTable), c.down().spanH(2).expandHV());

    final JPanel btnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    btnsPanel.add(runBtn);

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.add(nodeListsPanel, c.reset().expandHV());
    mainPanel.add(btnsPanel, c.down().expandH());
    return mainPanel;
  }

  public CytoPanelName getCytoPanelName() {
    return CytoPanelName.WEST;
  }

  public Icon getIcon() {
    return iconCreator.newIcon(15.0f, IconCode.SCISSORS);
  }

  public String getTitle() {
    return "Scissors ";
  }

  public void handleEvent(SetCurrentNetworkViewEvent e) {
  }
}

class NodeListsTableModel extends AbstractTableModel {
  final List<NodeList> nodeLists = new ArrayList<NodeList>();
  final List<Integer> counts = new ArrayList<Integer>();

  public void addNodeList(final NodeList nodeList, final CyNetwork network) {
    final int row = nodeLists.size();
    nodeLists.add(nodeList);

    if (network == null) {
      counts.add(0);
    } else {
      final int count = nodeList.convertToNodes(network).size();
      counts.add(count);
    }

    super.fireTableRowsInserted(row, row);
  }

  public void removeNodeList(final int row) {
    nodeLists.remove(row);
    counts.remove(row);
    super.fireTableRowsDeleted(row, row);
  }

  public void updateCountsForNetwork(final CyNetwork network) {
    for (int i = 0; i < nodeLists.size(); i++) {
      if (network == null) {
        counts.set(i, 0);
      } else {
        final int count = nodeLists.get(i).convertToNodes(network).size();
        counts.set(i, count);
      }
      super.fireTableCellUpdated(i, 1);
    }
  }

  public String getColumnName(int col) {
    switch (col) {
    case 0:
      return "Name";
    case 1:
      return "Node Count";
    }
    return null;
  }

  public int getRowCount() {
    return nodeLists.size();
  }

  public int getColumnCount() {
    return 2;
  }

  public Object getValueAt(int row, int col) {
    switch (col) {
    case 0:
      return nodeLists.get(row).getName();
    case 1:
      return counts.get(row);
    }
    return null;
  }

  public boolean isCellEditable(int row, int col) {
    switch (col) {
    case 0:
      return true;
    case 1:
      return false;
    }
    return false;
  }

  public void setValueAt(Object val, int row, int col) {
    if (col != 0) {
      return;
    }
    nodeLists.get(row).setName(val.toString());
  }
}

enum IconCode {
  SCISSORS("\uf0c4"),
  PLUS_SQUARE("\uf0fe"),
  MINUS_SQUARE("\uf146"),
  FILE_TEXT_O("\uf0f6"),
  CIRCLE_O_NOTCH("\uf1ce");

  final String iconStr;
  IconCode(final String iconStr) {
    this.iconStr = iconStr;
  }

  public String getIconString() {
    return iconStr;
  }
}

class IconCreator {
  final Font font;
  final FontRenderContext context;


  public IconCreator() {
    font = loadFont();
    context = new FontRenderContext(null, true, true);
  }

  public ImageIcon newIcon(final float size, final IconCode iconCode) {
    if (font == null) {
      return null;
    }
    final String iconStr = iconCode.getIconString();
    final Font scaledFont = font.deriveFont(size);
    final Rectangle2D bounds = scaledFont.getStringBounds(iconStr, context);
    final int w = (int) Math.ceil(bounds.getWidth());
    final int h = (int) Math.ceil(bounds.getHeight());
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2d = img.createGraphics();
    g2d.setPaint(Color.BLACK);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setFont(scaledFont);
    final float y = (float) bounds.getHeight() - g2d.getFontMetrics(scaledFont).getLineMetrics(iconStr, g2d).getDescent();
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