package scissors.internal;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

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

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

class ScissorsPanel implements CytoPanelComponent, SetCurrentNetworkListener {
  final CyApplicationManager appMgr;
  final ImageIcon icon;
  final JComboBox attrComboBox;
  final JTable valuesTable;
  final JButton cutBtn;

  public ScissorsPanel(final CyApplicationManager appMgr) {
    this.appMgr = appMgr;
    icon = new ImageIcon(this.getClass().getResource("/icon.png"));
    attrComboBox = new JComboBox();
    valuesTable = new JTable(new ValuesTM());
    cutBtn = new JButton("Cut", icon);
    setupUIForNetwork(appMgr.getCurrentNetwork());
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

  public void handleEvent(SetCurrentNetworkEvent e) {
    final CyNetwork net = e.getNetwork();
    setupUIForNetwork(net);
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

  private void setupUIForNetwork(final CyNetwork net) {
    if (net == null) {
      resetUI();
      return;
    }
    resetComboBox(attrComboBox);

    final CyTable tbl = net.getDefaultNodeTable();
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
      cutBtn.setEnabled(true);
    }

    updateValuesList(tbl);
  }

  private void updateValuesList(final CyTable tbl) {
    final Map<Object,Integer> values = new TreeMap<Object,Integer>();
    if (attrComboBox.getSelectedItem() != null) {
      final CyColumn col = ((CyColWrapper) attrComboBox.getSelectedItem()).getColumn();
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
}