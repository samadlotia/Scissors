package scissors.internal;

import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.AbstractAction;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import javax.swing.table.AbstractTableModel;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

public class ImportNodeListDialog {
  final JDialog dialog;
  final CyApplicationManager appMgr;
  final CyNetworkManager netMgr;
  CyNetwork targetNetwork;

  final JButton targetNetworkBtn;
  final FilesTableModel filesTableModel;
  final JTable table;

  static JButton newSimpleBtn(final String label) {
    final JButton btn = new JButton(label);
    btn.setFocusPainted(false);
    btn.setMargin(new Insets(0, 3, 0, 3));
    btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13.0f));
    return btn;
  }

  public ImportNodeListDialog(
      final Frame parent,
      final CyNetworkManager netMgr,
      final CyApplicationManager appMgr
      ) {
    dialog = new JDialog(parent, "Import Node Lists");
    this.appMgr = appMgr;
    this.netMgr = netMgr;
    targetNetwork = appMgr.getCurrentNetwork();


    targetNetworkBtn = new JButton();
    updateTargetNetworkBtn();
    final JPopupMenu networksMenu = constructNetworksMenu(groupNetworksByRootNetwork(netMgr.getNetworkSet()));
    networksMenu.addPopupMenuListener(new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
        updateTargetNetworkBtn();
      }
      public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
        updateTargetNetworkBtn();
      }
      public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {}
    });
    targetNetworkBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        networksMenu.show(targetNetworkBtn, 0, 0);
      }
    });

    filesTableModel = new FilesTableModel();
    table = new JTable(filesTableModel);
    final JButton addBtn = newSimpleBtn("+");
    addBtn.addActionListener(new AddFileAction());
    final JButton rmBtn = newSimpleBtn("<html>&minus;</html>");
    rmBtn.addActionListener(new RemoveFileAction());

    final EasyGBC c = new EasyGBC();

    final JPanel targetNetworkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    targetNetworkPanel.add(new JLabel("Network: "));
    targetNetworkPanel.add(targetNetworkBtn);

    final JPanel filesBtnsPanel = new JPanel(new FlowLayout());
    filesBtnsPanel.add(addBtn);
    filesBtnsPanel.add(rmBtn);

    final JPanel filesPanel = new JPanel(new GridBagLayout());
    filesPanel.add(new JLabel("Files:"), c.reset().expandH());
    filesPanel.add(filesBtnsPanel, c.right().noExpand());
    filesPanel.add(new JScrollPane(table), c.down().expandHV().spanH(2));

    dialog.setLayout(new GridBagLayout());
    dialog.add(targetNetworkPanel, c.reset().expandH());
    dialog.add(filesPanel, c.down().expand(1.0, 0.3));

    dialog.pack();
    dialog.setVisible(true);
  }

  void updateTargetNetworkBtn() {
    final String netName = targetNetwork.getRow(targetNetwork).get(CyNetwork.NAME, String.class);
    final String displayName = netName == null ? "Untitled Network" : netName;
    targetNetworkBtn.setText(displayName + " \u25bc");
  }

  class AddFileAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Choose a node list file");
      final int choice = chooser.showOpenDialog(dialog);
      if (choice != JFileChooser.APPROVE_OPTION) {
        return;
      }
      filesTableModel.addFile(chooser.getSelectedFile());
    }
  }

  class RemoveFileAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final int row = table.getSelectedRow();
      if (row < 0) {
        return;
      }
      filesTableModel.removeFile(row);
    }
  }

  static class FileModel {
    final File file;
    String colName;

    public FileModel(final File file) {
      this.file = file;
      this.colName = fileNameWithoutExtension(file.getName());
    }

    public File getFile() {
      return file;
    }

    public String getColName() {
      return colName;
    }

    public void setColName(final String colName) {
      this.colName = colName;
    }
  }

  static String fileNameWithoutExtension(final String fullname) {
    final int index = fullname.indexOf('.');
    if (index < 0) {
      return fullname;
    } else {
      return fullname.substring(0, index);
    }
  }

  static class FilesTableModel extends AbstractTableModel {
    final List<FileModel> files = new ArrayList<FileModel>();

    public int getRowCount() {
      return files.size();
    }

    public int getColumnCount() {
      return 2;
    }

    public String getColumnName(int col) {
      switch(col) {
        case 0:
          return "File";
        case 1:
          return "Column Name";
      }
      return null;
    }

    public Object getValueAt(int row, int col) {
      final FileModel fileModel = files.get(row);
      switch(col) {
        case 0:
          return fileModel.getFile().getName();
        case 1:
          return fileModel.getColName();
      }
      return null;
    }

    public boolean isCellEditable(int row, int col) {
      if (col == 1) {
        return true;
      } else {
        return false;
      }
    }

    public void setValueAt(Object val, int row, int col) {
      if (col != 1)
        return;
      files.get(row).setColName((String) val);
    }

    public void addFile(final File file) {
      final int newRowIndex = files.size();
      files.add(new FileModel(file));
      super.fireTableRowsInserted(newRowIndex, newRowIndex);
    }

    public void removeFile(final int index) {
      files.remove(index);
      super.fireTableRowsDeleted(index, index);
    }
  }

  static Map<CyRootNetwork,List<CyNetwork>> groupNetworksByRootNetwork(final Set<CyNetwork> networks) {
    final Map<CyRootNetwork,List<CyNetwork>> groups = new HashMap<CyRootNetwork,List<CyNetwork>>();
    for (final CyNetwork network : networks) {
      final CySubNetwork subnetwork = (CySubNetwork) network;
      final CyRootNetwork root = subnetwork.getRootNetwork();
      if (!groups.containsKey(root)) {
        groups.put(root, new ArrayList<CyNetwork>());
      }
      groups.get(root).add(network);
    }
    return groups;
  }

  JPopupMenu constructNetworksMenu(final Map<CyRootNetwork,List<CyNetwork>> networks) {
    final JPopupMenu menu = new JPopupMenu();
    for (final Map.Entry<CyRootNetwork,List<CyNetwork>> entry : networks.entrySet()) {
      final CyRootNetwork root = entry.getKey();
      final String rootName = root.getRow(root).get(CyNetwork.NAME, String.class);
      menu.add(rootName == null ? "Untitled Collection" : rootName).setEnabled(false);

      final List<CyNetwork> subnets = entry.getValue();
      for (final CyNetwork subnet : subnets) {
        final String subnetName = subnet.getRow(subnet).get(CyNetwork.NAME, String.class);
        menu.add(new AbstractAction("   " + (subnetName == null ? "Untitled Network" : subnetName)) {
          public void actionPerformed(ActionEvent e) {
            targetNetwork = subnet;
          }
        });
      }
    }
    return menu;
  }
}