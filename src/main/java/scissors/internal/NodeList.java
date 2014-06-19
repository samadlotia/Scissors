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
import org.cytoscape.model.CyTableUtil;

class NodeList {
  String listName;
  final Set<String> nodeNames = new HashSet<String>();

  public static NodeList newFromFile(final File file) throws IOException, FileNotFoundException {
    BufferedReader input = null;
    try {
      input = new BufferedReader(new FileReader(file));
      final NodeList nodeList = new NodeList();
      nodeList.listName = file.getName();
      while (true) {
        String line = input.readLine();
        if (line == null) {
          break;
        }
        if (line.trim().length() == 0) {
          continue;
        }

        nodeList.nodeNames.add(line);
      }
      return nodeList;
    } finally {
      try {
        input.close();
      } catch (IOException e) {}
    }
  }

  public static NodeList newFromSelection(final CyNetwork network) {
    final List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, "selected", true);
    final NodeList nodeList = new NodeList();
    for (final CyNode node : selectedNodes) {
      final String nodeName = network.getRow(node).get(CyNetwork.NAME, String.class);
      nodeList.nodeNames.add(nodeName);
    }
    return nodeList;
  }

  public String getName() {
    return listName;
  }

  public void setName(String name) {
    this.listName = name;
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

  public Set<CyNode> convertToNodes(final CyNetwork network) {
    final Set<CyNode> nodes = new HashSet<CyNode>();
    for (final CyNode node : network.getNodeList()) {
      final String nodeName = network.getRow(node).get(CyNetwork.NAME, String.class);
      if (nodeNames.contains(nodeName)) {
        nodes.add(node);
      }
    }
    return nodes;
  }
}