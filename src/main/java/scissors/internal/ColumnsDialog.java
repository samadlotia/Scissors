package scissors.internal;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.JPanel;
import javax.swing.border.Border;

import java.awt.Frame;
import java.awt.Component;
import java.awt.GridBagLayout;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

class ColumnsDialog {
  static final Set<Class<?>> DISCRETE_TYPES = new HashSet<Class<?>>(Arrays.asList(String.class, Integer.class, Boolean.class));

  final JDialog dialog;
  public ColumnsDialog(Frame parent) {
    dialog = new JDialog(parent, "Choose Columns from the Node Table");

    dialog.pack();
    dialog.setVisible(true);
  }
}

class CheckBoxList extends JList {
  protected static Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);

  public CheckBoxList() {
    setCellRenderer(new CellRenderer());

    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
       int index = locationToIndex(e.getPoint());

       if (index != -1) {
        JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
        checkbox.setSelected(!checkbox.isSelected());
        repaint();
      }
    }});

    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  protected class CellRenderer implements ListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      JCheckBox checkbox = (JCheckBox) value;
      checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
      checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
      checkbox.setEnabled(isEnabled());
      checkbox.setFont(getFont());
      checkbox.setFocusPainted(false);
      checkbox.setBorderPainted(true);
      checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
      return checkbox;
    }
  }
}