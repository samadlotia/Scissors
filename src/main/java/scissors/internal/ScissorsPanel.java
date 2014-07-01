package scissors.internal;

import java.io.InputStream;
import java.io.File;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
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
  enum Layout {
    GRID,
    FORCE_DIRECTED
  }

  final CyApplicationManager appMgr;
  final CySwingApplication swingApp;
  final TaskManager taskMgr;

  final IconCreator iconCreator;
  final NodeListsTableModel nodeListsModel;
  final PartitionsTableModel partitionsModel;

  Layout selectedLayout = Layout.GRID;

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
    final AbstractAction tableAction = new AddNodeListsFromDiscreteValuesAction();
    final AbstractAction selectionAction = new AddNodeListFromSelectionAction();
    final AbstractAction fileAction = new AddNodeListFromFileAction();

    final JPopupMenu menu = new JPopupMenu();
    menu.add(new AddNodeListsFromDiscreteValuesAction());
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

  private JButton newRunButton() {
    final Icon checkedIcon = iconCreator.newIcon(15.0f, IconCode.CHECK);
    final ActionListener runLayoutAction = new RunLayoutAction();
    final JMenuItem gridLayoutMenuItem = new JMenuItem("Grid Layout", (selectedLayout == Layout.GRID ? checkedIcon : null));
    final JMenuItem forceDirectedLayoutMenuItem = new JMenuItem("Force Directed Layout");
    gridLayoutMenuItem.addActionListener(new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        gridLayoutMenuItem.setIcon(checkedIcon);
        forceDirectedLayoutMenuItem.setIcon(null);
        selectedLayout = Layout.GRID;
        runLayoutAction.actionPerformed(e);
      }
    });
    forceDirectedLayoutMenuItem.addActionListener(new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        gridLayoutMenuItem.setIcon(null);
        forceDirectedLayoutMenuItem.setIcon(checkedIcon);
        selectedLayout = Layout.FORCE_DIRECTED;
        runLayoutAction.actionPerformed(e);
      }
    });
    //final JMenuItem layoutOptionsMenuItem = new JMenuItem("Options...");
    final JPopupMenu layoutsMenu = new JPopupMenu();
    layoutsMenu.add(gridLayoutMenuItem);
    layoutsMenu.add(forceDirectedLayoutMenuItem);
    //layoutsMenu.addSeparator();
    //layoutsMenu.add(layoutOptionsMenuItem);

    //final JButton runBtn = new JButton(new RunLayoutAction());
    final SplitButton runBtn = new SplitButton("Cut", iconCreator.newIcon(15.0f, IconCode.SCISSORS), iconCreator.newIcon(10.0f, IconCode.CHEVRON_DOWN));
    runBtn.addActionListener(new RunLayoutAction());
    runBtn.setMenu(layoutsMenu);
    return runBtn;
  }

  public Component getComponent() {
    final JTable nodeListsTable = new JTable(nodeListsModel);
    final JTable partitionsTable = new JTable(partitionsModel);

    final JButton addBtn = newAddBtn();
    final JButton rmBtn = new JButton(new RemoveNodeListsAction(nodeListsTable));
    rmBtn.setEnabled(false);

    nodeListsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        final boolean areRowsSelected = nodeListsTable.getSelectedRowCount() > 0;
        rmBtn.setEnabled(areRowsSelected);
        if (areRowsSelected) {
          nodeListsModel.selectNodesInNetwork(nodeListsTable.getSelectedRows(), appMgr.getCurrentNetworkView());
        }
      }
    });

    partitionsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        final boolean areRowsSelected = partitionsTable.getSelectedRowCount() > 0;
        if (areRowsSelected) {
          partitionsModel.selectNodesInNetwork(partitionsTable.getSelectedRows(), appMgr.getCurrentNetworkView());
        }
      }
    });

    final JButton runBtn = newRunButton();
    final JButton storeToColumnBtn = new JButton(new StoreToColumnAction());
    final JButton refreshBtn = new JButton(new RefreshAction());

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

    final JPanel secondaryBtnsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    secondaryBtnsPanel.add(refreshBtn);

    final JPanel mainBtnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    mainBtnsPanel.add(storeToColumnBtn);
    mainBtnsPanel.add(runBtn);

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.add(nodeListsPanel, c.reset().expandHV().spanH(2));
    mainPanel.add(partitionsPanel, c.down().spanH(2));
    mainPanel.add(secondaryBtnsPanel, c.noSpan().down().expandH());
    mainPanel.add(mainBtnsPanel, c.right().expandH());
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
    refresh();
  }

  private void refresh() {
    final CyNetwork network = appMgr.getCurrentNetwork();
    nodeListsModel.updateCountsForNetwork(network);
    updatePartitionsTable(network);
  }

  class AddNodeListsFromDiscreteValuesAction extends AbstractAction {
    public AddNodeListsFromDiscreteValuesAction() {
      super("From Discrete Node Table Columns", iconCreator.newIcon(17.0f, IconCode.TABLE));
    }

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
  }

  class AddNodeListFromSelectionAction extends AbstractAction {
    public AddNodeListFromSelectionAction() {
      super("From Node Selection", iconCreator.newIcon(17.0f, IconCode.CIRCLE_O_NOTCH));
    }

    public void actionPerformed(ActionEvent e) {
      final CyNetwork network = appMgr.getCurrentNetwork();
      nodeListsModel.addNodeList(NodeList.fromSelection(network), network);
      updatePartitionsTable(network);
    }
  }

  class AddNodeListFromFileAction extends AbstractAction {
    final JFileChooser chooser = new JFileChooser();
    public AddNodeListFromFileAction() {
      super("From File", iconCreator.newIcon(17.0f, IconCode.FILE_TEXT_O));
    }

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
  }

  class RemoveNodeListsAction extends AbstractAction {
    final JTable nodeListsTable;
    public RemoveNodeListsAction(final JTable nodeListsTable) {
      super(null, iconCreator.newIcon(15.0f, IconCode.MINUS));
      this.nodeListsTable = nodeListsTable;
    }

    public void actionPerformed(ActionEvent e) {
      final int[] rows = nodeListsTable.getSelectedRows();
      for (int i = rows.length - 1; i >= 0; i--) {
        nodeListsModel.removeNodeList(rows[i]);
      }
      updatePartitionsTable(appMgr.getCurrentNetwork());
    }
  }

  class RefreshAction extends AbstractAction {
    public RefreshAction() {
      super("Refresh", iconCreator.newIcon(15.0f, IconCode.REFRESH));
    }

    public void actionPerformed(ActionEvent e) {
      refresh();
    }
  }

  class RunLayoutAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      final CyNetworkView view = appMgr.getCurrentNetworkView();
      final List<Partition> partitions = partitionsModel.getPartitions();
      switch (selectedLayout) {
      case GRID:
        taskMgr.execute(new TaskIterator(new GridLayout(view, partitions)));
        break;
      case FORCE_DIRECTED:
        taskMgr.execute(new TaskIterator(new ForceDirectedLayout(view, partitions)));
        break;
      }
    }
  }

  class StoreToColumnAction extends AbstractAction {
    public StoreToColumnAction() {
      super("Store to column", iconCreator.newIcon(15.0f, IconCode.SIGN_IN));
    }

    public void actionPerformed(ActionEvent e) {
      final CyNetwork network = appMgr.getCurrentNetwork();
      final List<Partition> partitions = partitionsModel.getPartitions();
      Partition.storePartitionsInNodeColumn(partitions, network, "Partition");
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

  public void selectNodesInNetwork(final int[] rows, final CyNetworkView view) {
    final CyNetwork network = view.getModel();
    final CyTable tbl = network.getDefaultNodeTable();
    final Set<CyNode> nodesToSelect = new HashSet<CyNode>();
    for (final int index : rows) {
      nodesToSelect.addAll(nodeLists.get(index).getNodes(network));
    }
    for (final CyNode node : network.getNodeList()) {
      final boolean doSelect = nodesToSelect.contains(node);
      tbl.getRow(node.getSUID()).set(CyNetwork.SELECTED, doSelect);
    }
    view.updateView();
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

  public void selectNodesInNetwork(final int[] rows, final CyNetworkView view) {
    final CyNetwork network = view.getModel();
    final CyTable tbl = network.getDefaultNodeTable();
    final Set<CyNode> nodesToSelect = new HashSet<CyNode>();
    for (final int index : rows) {
      nodesToSelect.addAll(partitions.get(index).getNodes());
    }
    for (final CyNode node : network.getNodeList()) {
      final boolean doSelect = nodesToSelect.contains(node);
      tbl.getRow(node.getSUID()).set(CyNetwork.SELECTED, doSelect);
    }
    view.updateView();
  }
}

class SplitButton extends JButton {
  static final int GAP = 5;
  final JLabel mainText;
  volatile boolean actionListenersEnabled = true;
  JPopupMenu menu = null;

  public SplitButton(final String text, final Icon btnIcon, final Icon menuIcon) {
    mainText = new JLabel(text, btnIcon, JLabel.LEFT);
    final JLabel menuLabel = new JLabel(menuIcon);
    super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    super.add(mainText);
    super.add(Box.createRigidArea(new Dimension(GAP, 0)));
    super.add(new JSeparator(JSeparator.VERTICAL));
    super.add(Box.createRigidArea(new Dimension(GAP, 0)));
    super.add(menuLabel);

    super.addMouseListener(new MouseAdapter() {
      public void mousePressed(final MouseEvent e) {
        if (!SplitButton.this.isEnabled())
          return;
        final int x = e.getX();
        final int w = e.getComponent().getWidth();
        if (x >= (2 * w / 3)) {
          actionListenersEnabled = false;
          if (menu != null) {
            menu.show(e.getComponent(), e.getX(), e.getY());
          }
        } else {
          actionListenersEnabled = true;
        }
      }
    });
  }

  public void setText(final String label) {
    mainText.setText(label);
  }

  protected void fireActionPerformed(final ActionEvent e) {
    if (actionListenersEnabled) {
      super.fireActionPerformed(e);
    }
  }

  public void setMenu(final JPopupMenu menu) {
    this.menu = menu;
  }
}

enum IconCode {
  SCISSORS("\uf0c4"),
  PLUS("\uf067"),
  MINUS("\uf068"),
  TABLE("\uf0ce"),
  FILE_TEXT_O("\uf0f6"),
  REFRESH("\uf021"),
  SIGN_IN("\uf090"),
  CHEVRON_DOWN("\uf078"),
  CHECK("\uf00c"),
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