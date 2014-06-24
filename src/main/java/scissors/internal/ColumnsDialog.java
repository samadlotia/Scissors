package scissors.internal;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.JPanel;
import javax.swing.border.Border;

import java.awt.Frame;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Arrays;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Comparator;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;

class ColumnsDialog {
  static final Set<Class<?>> DISCRETE_TYPES = new HashSet<Class<?>>(Arrays.asList(String.class, Integer.class, Boolean.class));

  public static interface OkListener {
    public void invoked(final List<NodeList> nodeLists);
  }

  final JDialog dialog;
  public ColumnsDialog(final Frame parent, final CyTable tbl, final OkListener okListener) {
    dialog = new JDialog(parent, "Choose discrete columns in the node table");
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    final DefaultListModel colModel = new DefaultListModel();
    final JList colList = new JList(colModel);
    colList.setCellRenderer(new CyColRenderer());
    colList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    for (final CyColumn col : tbl.getColumns()) {
      final Class<?> type = col.getType();
      final boolean isDiscreteCol = DISCRETE_TYPES.contains(type);
      final boolean isDisceteListCol = List.class.equals(type) && DISCRETE_TYPES.contains(col.getListElementType());
      if (isDiscreteCol || isDisceteListCol) {
        colModel.addElement(new CyColModelElem(col));
      }
    }

    final DefaultListModel valModel = new DefaultListModel();
    final JList valList = new JList(valModel);
    valList.setCellRenderer(new ValRenderer());
    valList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    valList.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
       int index = valList.locationToIndex(e.getPoint());
       if (index != -1) {
        final ValModelElem elem = (ValModelElem) valModel.getElementAt(index);
        elem.setSelected(!elem.isSelected());
        valList.repaint();
      }
    }});

    final JButton okBtn = new JButton("Add All");
    okBtn.setEnabled(false);

    colList.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
       int index = colList.locationToIndex(e.getPoint());
       if (index != -1) {
        final CyColModelElem elem = (CyColModelElem) colModel.getElementAt(index);
        elem.setSelected(!elem.isSelected());
        colList.repaint();

        valModel.removeAllElements();
        final String[] cols = getSelectedColumnNames(colModel);
        final Set<Object> vals = NodeList.getAllPossibleDiscreteValues(tbl, cols);
        final Set<Object> sortedVals = new TreeSet<Object>(new StringComparator());
        sortedVals.addAll(vals);
        for (final Object val : sortedVals) {
          valModel.addElement(new ValModelElem(val));
        }

        okBtn.setEnabled(valModel.size() > 0);
      }
    }});

    okBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String[] cols = getSelectedColumnNames(colModel);
        final List<NodeList> nodeLists = new ArrayList<NodeList>();
        for (int i = 0; i < valModel.size(); i++) {
          final ValModelElem elem = (ValModelElem) valModel.get(i);
          if (elem.isSelected()) {
            final Object val = elem.getValue();
            nodeLists.add(NodeList.fromDiscreteNodeTableColumns(val.toString(), cols, val));
          }
        }
        okListener.invoked(nodeLists);
        dialog.dispose();
      }
    });

    final JButton cancelBtn = new JButton("Cancel");
    cancelBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    });

    dialog.setLayout(new GridBagLayout());
    final EasyGBC c = new EasyGBC();

    dialog.add(new JLabel("Discrete columns in the node table:"), c.reset().expandH().insets(5, 5, 0, 5));
    dialog.add(new JScrollPane(colList), c.down().expandHV().insets(5, 5, 0, 5));

    dialog.add(new JLabel("Values from selected columns:"), c.down().expandH().insets(10, 5, 0, 5));
    dialog.add(new JScrollPane(valList), c.down().expandHV().insets(5, 5, 0, 5));

    final JPanel btnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    btnsPanel.add(cancelBtn);
    btnsPanel.add(okBtn);

    dialog.add(btnsPanel, c.down().expandH().insets(5, 5, 5, 5));

    dialog.pack();
    dialog.setVisible(true);
  }

  static String[] getSelectedColumnNames(final DefaultListModel model) {
    final List<String> colNames = new ArrayList<String>();
    for (int i = 0; i < model.size(); i++) {
      final CyColModelElem elem = (CyColModelElem) model.get(i);
      if (elem.isSelected()) {
        colNames.add(elem.getColumn().getName());
      }
    }
    return colNames.toArray(new String[colNames.size()]);
  }
}

class CyColModelElem {
  boolean selected = false;
  final CyColumn col;

  public CyColModelElem(final CyColumn col) {
    this.col = col;
  }

  public CyColumn getColumn() {
    return col;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public boolean isSelected() {
    return selected;
  }

  public String getFormattedTitle() {
    final String name = col.getName();
    final Class<?> type = col.getType();
    final String typeStr = type.equals(List.class) ? String.format("List of %ss", col.getListElementType().getSimpleName()) : type.getSimpleName();
    return String.format("<html>%s <font size=\"-2\">(%s)</font></html>", name, typeStr);
  }
}

class StringComparator implements Comparator<Object> {
  public int compare(Object o1, Object o2) {
    return o1.toString().compareTo(o2.toString());
  }

  public boolean equals(Object obj) {
    return false;
  }
}

class CyColRenderer extends JCheckBox implements ListCellRenderer {
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    final CyColModelElem elem = (CyColModelElem) value;
    super.setText(elem.getFormattedTitle());
    super.setSelected(elem.isSelected());
    super.setBackground(isSelected ? list.getSelectionBackground() : getBackground());
    super.setFocusPainted(false);
    super.setBorderPainted(true);
    return this;
  }
}

class ValModelElem {
  boolean selected = true;
  final Object val;

  public ValModelElem(final Object val) {
    this.val = val;
  }

  public Object getValue() {
    return val;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public boolean isSelected() {
    return selected;
  }

  public String toString() {
    return val.toString();
  }
}

class ValRenderer extends JCheckBox implements ListCellRenderer {
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    final ValModelElem elem = (ValModelElem) value;
    super.setText(elem.toString());
    super.setSelected(elem.isSelected());
    super.setBackground(isSelected ? list.getSelectionBackground() : getBackground());
    super.setFocusPainted(false);
    super.setBorderPainted(true);
    return this;
  }
}

