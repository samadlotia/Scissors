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
  final ImageIcon icon;
  final JComboBox attrComboBox;
  final JTable valuesTable;
  final JButton cutBtn;
  final TaskManager taskMgr;

  public ScissorsPanel(final CyApplicationManager appMgr, final TaskManager taskMgr) {
    this.appMgr = appMgr;
    this.taskMgr = taskMgr;
    final IconCreator iconCreater = new IconCreator();
    icon = iconCreater.newIcon(15.0f, "\uf0c4");
    attrComboBox = new JComboBox();
    valuesTable = new JTable(new ValuesTM());
    cutBtn = new JButton("Cut", icon);
    setupUIForNetworkView(appMgr.getCurrentNetworkView());
  }

  public Component getComponent() {
    final EasyGBC c = new EasyGBC();

    final JPanel attrPanel = new JPanel(new GridBagLayout());
    attrPanel.add(new JLabel("Choose a node column: "), c.reset());
    attrPanel.add(attrComboBox, c.right().expandH());

    final JPanel valuesPanel = new JPanel(new GridBagLayout());
    valuesPanel.add(new JLabel("Inspect groups from this column:"), c.reset().expandH());
    valuesPanel.add(new JScrollPane(valuesTable), c.down().expandHV());

    final JPanel btnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    btnsPanel.add(cutBtn);

    final JPanel panel = new JPanel(new GridBagLayout());
    panel.add(attrPanel, c.reset().expandH().insets(10, 10, 0, 10));
    panel.add(valuesPanel, c.down().expandHV().insets(10, 10, 0, 10));
    panel.add(btnsPanel, c.down().expandH().insets(0, 10, 0, 10));

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
    final CyNetworkView netView = e.getNetworkView();
    setupUIForNetworkView(netView);
  }

  private void resetUI() {
    resetComboBox(attrComboBox);
    attrComboBox.setEnabled(false);
    valuesTable.setModel(new ValuesTM());
    valuesTable.setEnabled(false);
    cutBtn.setEnabled(false);
  }

  private static <T> boolean isOneOf(final T t, final T ... vals) {
    for (final T val : vals) {
      if (t.equals(val))
        return true;
    }
    return false;
  }

  private static void resetComboBox(final JComboBox comboBox) {
    for (final ItemListener listener : comboBox.getItemListeners()) {
      comboBox.removeItemListener(listener);
    }
    comboBox.removeAllItems();
  }

  private void setupUIForNetworkView(final CyNetworkView netView) {
    if (netView == null) {
      resetUI();
      return;
    }
    resetComboBox(attrComboBox);

    final CyTable tbl = netView.getModel().getDefaultNodeTable();
    for (final CyColumn col : tbl.getColumns()) {
      if (col.isImmutable()) {
        continue;
      }
      if (!isOneOf(col.getType(), String.class, Integer.class, Boolean.class)) {
        continue;
      }
      attrComboBox.addItem(new CyColWrapper(col));
    }
    if (attrComboBox.getItemCount() == 0) {
      attrComboBox.addItem("No columns available");
      attrComboBox.setEnabled(false);
      cutBtn.setEnabled(false);
    } else {
      attrComboBox.setEnabled(true);
      attrComboBox.addItemListener(new UpdateValuesList(tbl));

      for (final ActionListener listener : cutBtn.getActionListeners()) {
        cutBtn.removeActionListener(listener);
      }
      cutBtn.setEnabled(true);
      cutBtn.addActionListener(new RunLayout(netView));
    }

    updateValuesList(tbl);
  }

  private void updateValuesList(final CyTable tbl) {
    final Map<Object,Integer> values = new TreeMap<Object,Integer>();
    final Object selected = attrComboBox.getSelectedItem();
    if (selected != null && (selected instanceof CyColWrapper)) {
      final CyColumn col = ((CyColWrapper) selected).getColumn();
      final String colName = col.getName();
      for (final CyRow row : tbl.getAllRows()) {
        final Object value = row.getRaw(colName);
        if (value == null)
          continue;
        if (!values.containsKey(value)) {
          values.put(value, 0);
        }
        values.put(value, values.get(value) + 1);
      }
      valuesTable.setEnabled(true);
    } else {
      valuesTable.setEnabled(false);
    }
    valuesTable.setModel(new ValuesTM(values));
  }

  class UpdateValuesList implements ItemListener {
    final CyTable tbl;

    public UpdateValuesList(final CyTable tbl) {
      this.tbl = tbl;
    }

    public void itemStateChanged(final ItemEvent e) {
      updateValuesList(tbl);
    }
  }

  static class CyColWrapper {
    final CyColumn col;
    public CyColWrapper(final CyColumn col) {
      this.col = col;
    }

    public CyColumn getColumn() {
      return col;
    }

    public String toString() {
      return col.getName();
    }
  }

  static class ValuesTM extends AbstractTableModel {
    final Object[] values;
    final int[] counts;
    public ValuesTM() {
      values = new Object[0];
      counts = new int[0];
    }

    public ValuesTM(final Map<Object,Integer> valuesToCounts) {
      final int n = valuesToCounts.size();
      values = new Object[n];
      counts = new int[n];
      int i = 0;
      for (final Map.Entry<Object,Integer> entry : valuesToCounts.entrySet()) {
        values[i] = entry.getKey();
        counts[i] = entry.getValue();
        i++;
      }
    }

    public int getRowCount() {
      return values.length;
    }

    public int getColumnCount() {
      return 2;
    }

    public Object getValueAt(int row, int column) {
      switch (column) {
      case 0:
        return values[row];
      case 1:
        return counts[row];
      }
      return null;
    }

    public String getColumnName(final int col) {
      switch (col) {
      case 0:
        return "Group value";
      case 1:
        return "Number of nodes";
      }
      return null;
    }
  }

  private List<List<CyNode>> groupOnDiscreteColumn(final CyNetwork network, final String col) {
    final CyTable tbl = network.getDefaultNodeTable();
    final Map<Object,List<CyNode>> valToNodes = new TreeMap<Object,List<CyNode>>();
    for (final CyNode node : network.getNodeList()) {
      final Object val = tbl.getRow(node.getSUID()).getRaw(col);
      if (!valToNodes.containsKey(val)) {
        valToNodes.put(val, new ArrayList<CyNode>());
      }
      valToNodes.get(val).add(node);
    }
    return new ArrayList<List<CyNode>>(valToNodes.values());
  }

  class RunLayout implements ActionListener {
    final CyNetworkView netView;
    public RunLayout(final CyNetworkView netView) {
      this.netView = netView;
    }

    public void actionPerformed(ActionEvent e) {
      final CyNetwork net = appMgr.getCurrentNetwork();
      final CyColumn col = ((CyColWrapper) attrComboBox.getSelectedItem()).getColumn();
      final List<List<CyNode>> groups = groupOnDiscreteColumn(net, col.getName());
      final Task task = new ScissorsLayout(netView, groups);
      taskMgr.execute(new TaskIterator(task));
    }
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