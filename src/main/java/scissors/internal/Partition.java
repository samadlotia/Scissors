package scissors.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.BitSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyColumn;

class Partition {
  String name;
  final Set<CyNode> nodes;

  Partition(final String name, final Set<CyNode> nodes) {
    this.name = name;
    this.nodes = nodes;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<CyNode> getNodes() {
    return nodes;
  }

  private static List<Set<CyNode>> extractNodeSets(final List<NodeList> nodeLists, final CyNetwork network) {
    final List<Set<CyNode>> nodeSets = new ArrayList<Set<CyNode>>(nodeLists.size());
    for (final NodeList nodeList : nodeLists) {
      nodeSets.add(nodeList.getNodes(network));
    }
    return nodeSets;
  }

  private static BitSet calculateNodeMembership(final CyNode node, final List<Set<CyNode>> nodeSets) {
    final BitSet membership = new BitSet(nodeSets.size());
    for (int i = 0; i < nodeSets.size(); i++) {
      final Set<CyNode> nodeSet = nodeSets.get(i);
      if (nodeSet.contains(node)) {
        membership.set(i);
      }
    }
    return membership;
  }

  private static String generatePartitionName(final BitSet membership, final List<NodeList> nodeLists, final StringBuffer nameBuffer) {
    if (membership.isEmpty()) {
      return "None";
    } else {
      nameBuffer.setLength(0); // clear the name buffer before using it
      for (int i = membership.nextSetBit(0); i >= 0; i = membership.nextSetBit(i+1)) { // loop thru each set bit in membership
        final NodeList nodeList = nodeLists.get(i);
        nameBuffer.append(nodeList.getName());
        nameBuffer.append(", ");
      }
      final int length = nameBuffer.length();
      nameBuffer.delete(length - 2, length); // delete the final comma and space
      return nameBuffer.toString();
    }
  }

  public static List<Partition> generateFromNodeLists(final List<NodeList> nodeLists, final CyNetwork network) {
    if (network == null) {
      return Collections.emptyList();
    }

    final List<Set<CyNode>> nodeSets = extractNodeSets(nodeLists, network);

    // calculate all possible intersections of node lists
    final Map<BitSet,Set<CyNode>> intersections = new TreeMap<BitSet,Set<CyNode>>(new BitSetComparator());
    for (final CyNode node : network.getNodeList()) {
      final BitSet membership = calculateNodeMembership(node, nodeSets);
      if (!intersections.containsKey(membership)) {
        intersections.put(membership, new HashSet<CyNode>());
      }
      intersections.get(membership).add(node);
    }

    // convert the intersections into partition objects
    final List<Partition> partitions = new ArrayList<Partition>();
    final StringBuffer nameBuffer = new StringBuffer();
    for (final Map.Entry<BitSet,Set<CyNode>> entry : intersections.entrySet()) {
      final String name = generatePartitionName(entry.getKey(), nodeLists, nameBuffer);
      final Partition partition = new Partition(name, entry.getValue());
      partitions.add(partition);
    }

    return partitions;
  }

  public static String storePartitionsInNodeColumn(final List<Partition> partitions, final CyNetwork network, final String suggestedColName) {
    final CyTable table = network.getDefaultNodeTable();
    String colName = suggestedColName;
    int counter = 0;
    while (table.getColumn(colName) != null && !table.getColumn(colName).getType().equals(String.class)) {
      colName = suggestedColName + " " + (++counter);
    }
    if (table.getColumn(colName) == null) {
      table.createColumn(colName, String.class, false);
    }
    storePartitionsInColumn(partitions, table, colName);
    return colName;
  }

  public static void storePartitionsInColumn(final List<Partition> partitions, final CyTable table, final String colName) {
    final CyColumn col = table.getColumn(colName);
    if (col == null || !String.class.equals(col.getType())) {
      throw new IllegalArgumentException(String.format("Column '%s' does not exist or is not a string", colName));
    }

    for (final Partition partition : partitions) {
      final String value = partition.getName();
      for (final CyNode node : partition.getNodes()) {
        table.getRow(node.getSUID()).set(colName, value);
      }
    }
  }
}

class BitSetComparator implements Comparator<BitSet> {
  public int compare(BitSet o1, BitSet o2) {
    // Compare the cardinality first. Here cardinality
    // represents the number of node lists a partition
    // belongs to. Partitions that belong
    // to fewer node lists should appear higher.
    final int card1 = o1.cardinality();
    final int card2 = o2.cardinality();
    final int cardD = card1 - card2;
    if (cardD != 0) {
      return cardD;
    }

    // Do a bitwise comparison so that the partitions' orders
    // reflect the order of node lists
    final int len = Math.max(o1.length(), o2.length());
    for (int i = len - 1; i >= 0; i--) {
      if (o1.get(i) != o2.get(i)) {
        return o1.get(i) ? 1 : -1;
      }
    }
    return 0;
  }
}