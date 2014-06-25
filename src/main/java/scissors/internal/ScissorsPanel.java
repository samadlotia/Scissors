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
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

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
import javax.swing.JFileChooser;
import javax.swing.table.AbstractTableModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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

class ScissorsPanel implements CytoPanelComponent, SetCurrentNetworkViewListener {
  final CyApplicationManager appMgr;
  final CySwingApplication swingApp;
  final TaskManager taskMgr;

  final IconCreator iconCreator;
  final NodeListsTableModel nodeListsModel;
  final PartitionsTableModel partitionsModel;

  public ScissorsPanel(
      final CyApplicationManager appMgr,
      final CySwingApplication swingApp,
      final TaskManager taskMgr
    ) {
    this.appMgr = appMgr;
    this.swingApp = swingApp;
    this.taskMgr = taskMgr;
    iconCreator = new IconCreator();
    nodeListsModel = new NodeListsTableModel();
    partitionsModel = new PartitionsTableModel();
  }

  private boolean areThereNodesSelected() {
    final CyNetwork network = appMgr.getCurrentNetwork();
    if (network == null) {
      return false;
    } else {
      return CyTableUtil.getNodesInState(network, "selected", true).size() > 0;
    }
  }

  private boolean isThereANetwork() {
    return appMgr.getCurrentNetwork() != null;
  }

  private void updatePartitionsTable(final CyNetwork network) {
    partitionsModel.setPartitions(Partition.generateFromNodeLists(nodeListsModel.getNodeLists(), network));
  }

  private JButton newAddBtn() {
    final AbstractAction tableAction = new AbstractAction("From Discrete Node Table Columns", iconCreator.newIcon(17.0f, IconCode.TABLE)) {
      public void actionPerformed(ActionEvent e) {
        final CyNetwork network = appMgr.getCurrentNetwork();
        new ColumnsDialog(swingApp.getJFrame(), network.getDefaultNodeTable(), new ColumnsDialog.OkListener() {
          public void invoked(final List<NodeList> nodeLists) {
            for (final NodeList nodeList : nodeLists) {
              nodeListsModel.addNodeList(nodeList, network);
            }
            updatePartitionsTable(network);
          }
        });
      }
    };

    final AbstractAction selectionAction = new AbstractAction("From Node Selection", iconCreator.newIcon(17.0f, IconCode.CIRCLE_O_NOTCH)) {
      public void actionPerformed(ActionEvent e) {
        final CyNetwork network = appMgr.getCurrentNetwork();
        nodeListsModel.addNodeList(NodeList.fromSelection(network), network);
        updatePartitionsTable(network);
      }
    };

    final AbstractAction fileAction = new AbstractAction("From File", iconCreator.newIcon(17.0f, IconCode.FILE_TEXT_O)) {
      final JFileChooser chooser = new JFileChooser();
      public void actionPerformed(ActionEvent e) {
        final CyNetwork network = appMgr.getCurrentNetwork();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Choose Node Lists Files");
        final int choice = chooser.showOpenDialog(swingApp.getJFrame());
        if (choice != JFileChooser.APPROVE_OPTION) {
          return;
        }
        final File[] files = chooser.getSelectedFiles();
        for (final File file : files) {
          try {
            nodeListsModel.addNodeList(NodeList.fromFile(file), network);
          } catch (Exception ex) {}
        }
        updatePartitionsTable(network);
      }
    };

    final JPopupMenu menu = new JPopupMenu();
    menu.add(tableAction);
    menu.add(selectionAction);
    menu.add(fileAction);

    final JButton addBtn = new JButton(iconCreator.newIcon(15.0f, IconCode.PLUS));
    addBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectionAction.setEnabled(areThereNodesSelected());
        tableAction.setEnabled(isThereANetwork());
        menu.show(addBtn, 0, addBtn.getHeight());
      }
    });
    return addBtn;
  }

  public Component getComponent() {
    final JTable nodeListsTable = new JTable(nodeListsModel);
    final JTable partitionsTable = new JTable(partitionsModel);

    final JButton addBtn = newAddBtn();
    final JButton rmBtn = new JButton(iconCreator.newIcon(15.0f, IconCode.MINUS));
    rmBtn.setEnabled(false);
    rmBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final int[] rows = nodeListsTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
          nodeListsModel.removeNodeList(rows[i]);
        }
        updatePartitionsTable(appMgr.getCurrentNetwork());
      }
    });

    nodeListsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        final boolean areRowsSelected = nodeListsTable.getSelectedRowCount() > 0;
        rmBtn.setEnabled(areRowsSelected);
        if (!areRowsSelected) {
          return;
        }
        final CyNetwork network = appMgr.getCurrentNetwork();
        final CyTable tbl = network.getDefaultNodeTable();
        final Set<CyNode> nodesToSelect = new HashSet<CyNode>();
        for (final int index : nodeListsTable.getSelectedRows()) {
          nodesToSelect.addAll(nodeListsModel.getNodeListAt(index).getNodes(network));
        }
        for (final CyNode node : network.getNodeList()) {
          final boolean doSelect = nodesToSelect.contains(node);
          tbl.getRow(node.getSUID()).set(CyNetwork.SELECTED, doSelect);
        }
        appMgr.getCurrentNetworkView().updateView();
      }
    });

    partitionsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        final boolean areRowsSelected = partitionsTable.getSelectedRowCount() > 0;
        if (!areRowsSelected) {
          return;
        }
        final CyNetwork network = appMgr.getCurrentNetwork();
        final CyTable tbl = network.getDefaultNodeTable();
        final Set<CyNode> nodesToSelect = new HashSet<CyNode>();
        for (final int index : partitionsTable.getSelectedRows()) {
          nodesToSelect.addAll(partitionsModel.getPartitionAt(index).getNodes());
        }
        for (final CyNode node : network.getNodeList()) {
          final boolean doSelect = nodesToSelect.contains(node);
          tbl.getRow(node.getSUID()).set(CyNetwork.SELECTED, doSelect);
        }
        appMgr.getCurrentNetworkView().updateView();
      }
    });

    final JButton runBtn = new JButton("Cut", iconCreator.newIcon(15.0f, IconCode.SCISSORS));
    runBtn.addActionListener(new RunLayout());

    final EasyGBC c = new EasyGBC();

    final JPanel nodeListsBtnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    nodeListsBtnsPanel.add(addBtn);
    nodeListsBtnsPanel.add(rmBtn);

    final JPanel nodeListsPanel = new JPanel(new GridBagLayout());
    nodeListsPanel.add(new JLabel("Node Lists:"), c.reset().insets(0, 5, 0, 0));
    nodeListsPanel.add(nodeListsBtnsPanel, c.expandH().right().noInsets());
    nodeListsPanel.add(new JScrollPane(nodeListsTable), c.down().spanH(2).expandHV());

    final JPanel partitionsPanel = new JPanel(new GridBagLayout());
    partitionsPanel.add(new JLabel("Partitions:"), c.reset().expandH().insets(15, 5, 0, 0));
    partitionsPanel.add(new JScrollPane(partitionsTable), c.down().expandHV().noInsets());

    final JPanel btnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    btnsPanel.add(runBtn);

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.add(nodeListsPanel, c.reset().expandHV());
    mainPanel.add(partitionsPanel, c.down());
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
    final CyNetwork network = appMgr.getCurrentNetwork();
    nodeListsModel.updateCountsForNetwork(network);
    updatePartitionsTable(network);
  }

  class RunLayout implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final CyNetworkView view = appMgr.getCurrentNetworkView();
      final List<Partition> partitions = partitionsModel.getPartitions();
      taskMgr.execute(new TaskIterator(new ScissorsLayout(view, partitions)));
    }
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
      final int count = nodeList.getNodes(network).size();
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
        final int count = nodeLists.get(i).getNodes(network).size();
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

  public int size() {
    return nodeLists.size();
  }

  public NodeList getNodeListAt(final int index) {
    return nodeLists.get(index);
  }

  public List<NodeList> getNodeLists() {
    return nodeLists;
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

class PartitionsTableModel extends AbstractTableModel {
  List<Partition> partitions = Collections.emptyList();

  public List<Partition> getPartitions() {
    return partitions;
  }

  public void setPartitions(final List<Partition> partitions) {
    this.partitions = partitions;
    super.fireTableDataChanged();
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
    return partitions.size();
  }

  public Partition getPartitionAt(final int index) {
    return partitions.get(index);
  }

  public int getColumnCount() {
    return 2;
  }

  public Object getValueAt(int row, int col) {
    switch (col) {
    case 0:
      return partitions.get(row).getName();
    case 1:
      return partitions.get(row).getNodes().size();
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
    partitions.get(row).setName(val.toString());
  }
}

enum IconCode {
  SCISSORS("\uf0c4"),
  PLUS("\uf067"),
  MINUS("\uf068"),
  TABLE("\uf0ce"),
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