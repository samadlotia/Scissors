package scissors.internal;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;

abstract class NodeList {
  protected String listName;

  public abstract Set<CyNode> getNodes(final CyNetwork network);

  public String getName() {
    return listName;
  }

  public void setName(String name) {
    this.listName = name;
  }

  public static NodeList fromFile(final File file) throws IOException, FileNotFoundException {
    BufferedReader input = null;
    try {
      input = new BufferedReader(new FileReader(file));
      final Set<String> nodeNames = new HashSet<String>();
      while (true) {
        String line = input.readLine();
        if (line == null) {
          break;
        }
        if (line.trim().length() == 0) {
          continue;
        }
        nodeNames.add(line);
      }
      return new NodeListFromNames(file.getName(), nodeNames);
    } finally {
      try {
        input.close();
      } catch (IOException e) {}
    }
  }

  static int selectionInvokedCount = 0;
  public static NodeList fromSelection(final CyNetwork network) {
    final List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, "selected", true);
    return new NodeListFromSet("Selection " + (++selectionInvokedCount), new HashSet<CyNode>(selectedNodes));
  }

  public static NodeList fromDiscreteNodeTableColumns(final String name, final String[] cols, final Object value) {
    return new NodeListFromDiscreteValueInColumns(name, cols, value);
  }

  public static Set<Object> getAllPossibleDiscreteValues(final CyTable tbl, final String[] cols) {
    final Set<Object> values = new HashSet<Object>();
    final List<CyRow> rows = tbl.getAllRows();
    for (final String colName : cols) {
      final CyColumn col = tbl.getColumn(colName);
      if (col.getType().equals(List.class)) {
        final Class<?> elemType = col.getListElementType();
        for (final CyRow row : rows) {
          if (!row.isSet(colName)) {
            continue;
          }
          final List<?> rowValues = row.getList(colName, elemType);
          values.addAll(rowValues);
        }
      } else {
        for (final CyRow row : rows) {
          if (!row.isSet(colName)) {
            continue;
          }
          final Object rowValue = row.getRaw(colName);
          values.add(rowValue);
        }
      }
    }
    return values;
  }
}

class NodeListFromNames extends NodeList {
  final Set<String> nodeNames;

  public NodeListFromNames(final String name, final Set<String> nodeNames) {
    super.listName = name;
    this.nodeNames = nodeNames;
  }

  public Set<String> getUnknownNodes(final CyNetwork network) {
    final Set<String> unknownNodes = new HashSet<String>();
    for (final CyNode node : network.getNodeList()) {
      final String nodeName = network.getRow(node).get(CyNetwork.NAME, String.class);
      if (!nodeNames.contains(nodeName)) {
        unknownNodes.add(nodeName);
      }
    }
    return unknownNodes;
  }

  public Set<CyNode> getNodes(final CyNetwork network) {
    final Set<CyNode> nodesInNetwork = new HashSet<CyNode>();
    for (final CyNode node : network.getNodeList()) {
      final String nodeName = network.getRow(node).get(CyNetwork.NAME, String.class);
      if (nodeNames.contains(nodeName)) {
        nodesInNetwork.add(node);
      }
    }
    return nodesInNetwork;
  }
}

class NodeListFromSet extends NodeList {
  final Set<CyNode> nodes;

  public NodeListFromSet(final String name, final Set<CyNode> nodes) {
    super.listName = name;
    this.nodes = nodes;
  }

  public Set<CyNode> getNodes(final CyNetwork network) {
    final Set<CyNode> nodesInNetwork = new HashSet<CyNode>();
    for (final CyNode node : nodes) {
      if (network.containsNode(node)) {
        nodesInNetwork.add(node);
      }
    }
    return nodesInNetwork;
  }
}

class NodeListFromDiscreteValueInColumns extends NodeList {
  final String[] cols;
  final Object value;

  public NodeListFromDiscreteValueInColumns(final String name, final String[] cols, final Object value) {
    super.listName = name;
    this.cols = cols;
    this.value = value;
  }

  public Set<CyNode> getNodes(final CyNetwork network) {
    final Set<CyNode> nodesInNetwork = new HashSet<CyNode>();
    final CyTable tbl = network.getDefaultNodeTable();
    for (final String colName : cols) {
      final CyColumn col = tbl.getColumn(colName);
      if (col == null) {
        continue; // column doesn't exist for this node table or isn't scalar; skip it
      }
      if (col.getType().equals(List.class)) {
        final Class<?> colListType = col.getListElementType();
        for (final CyNode node : network.getNodeList()) {
          final CyRow row = tbl.getRow(node.getSUID());
          if (!row.isSet(colName)) {
            continue;
          }
          final List<?> list = row.getList(colName, colListType);
          if (list.contains(value)) {
            nodesInNetwork.add(node);
          }
        }
      } else {
        for (final CyNode node : network.getNodeList()) {
          final CyRow row = tbl.getRow(node.getSUID());
          if (!row.isSet(colName)) {
            continue;
          }
          if (row.getRaw(colName).equals(value)) {
            nodesInNetwork.add(node);
          }
        }
      }
    }
    return nodesInNetwork;
  }
}
