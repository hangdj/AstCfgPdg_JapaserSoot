package cn.fan.fore;

import static com.github.javaparser.utils.Utils.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.fan.model.AstNode;
import cn.fan.model.Edge;

import com.github.javaparser.ast.Node;

public class DotPrinterAst {
    private int nodeCount;
    private final boolean outputNodeType;
    // 是否需要假如cfg的边
    private boolean addCfgLines;
    private HashMap<Integer, String> lineToDotLabel;

    // 所有cfg中需要的边 数据流和控制流的边
    private HashMap<String, Set<Edge>> allmethodToCfgEdges;
    private HashMap<String, Set<Edge>> allmethodToallDataFlowEdges;
    private Pattern p;
    private Pattern dotP;
    private Pattern quotationP;

    /**
     * 
     * @param outputNodeType
     * @param lineToTypeName
     *            这个行和类型名字结合的map 就是用来生成行和label的 这样才能为dot增加边
     */
    public DotPrinterAst(boolean outputNodeType, boolean addCfgLines, HashMap<String, Set<Edge>> allmethodToCfgEdges, HashMap<String, Set<Edge>> allmethodToallDataFlowEdges) {
        this.outputNodeType = outputNodeType;
        this.lineToDotLabel = new HashMap<Integer, String>();
        this.addCfgLines = addCfgLines;
        this.allmethodToallDataFlowEdges = allmethodToallDataFlowEdges;
        this.allmethodToCfgEdges = allmethodToCfgEdges;
        p = Pattern.compile("\r|\n|\r\n");
        dotP = Pattern.compile(",");
        quotationP = Pattern.compile("\"");
    }

    public String output(AstNode node) {
        nodeCount = 0;
        StringBuilder outputstrBuilder = new StringBuilder();
        outputstrBuilder.append("digraph {");
        output(node, null, "root", outputstrBuilder, node.getRootPrimary());
        if (addCfgLines && (allmethodToallDataFlowEdges != null || allmethodToCfgEdges != null)) {
            addCfgAddDataFlowEdge(outputstrBuilder);
        }
        outputstrBuilder.append(System.lineSeparator() + "}");
        return outputstrBuilder.toString();
    }

    private void output(AstNode node, String parentNodeName, String name, StringBuilder builder, Node root) {
        assertNotNull(node);
        List<String> attributes = node.getAttributes();
        List<AstNode> subNodes = node.getSubNodes();
        List<String> subLists = node.getSubLists();
        List<List<AstNode>> subListNodes = node.getSubListNodes();
        List<String> subLists_name = node.getSubLists_name();
        List<Node> subNodesPrimary = node.getSubNodesPrimary();
        List<List<Node>> subListNodesPrimary = node.getSubListNodesPrimary();

        // 原来Ast的节点 因为需要用到各个节点的行号信息，所以必须一起遍历

        String ndName = nextNodeName();
        if (!root.toString().equals("")) {
            if (outputNodeType) {
                // 只有第一次才能
                if (lineToDotLabel.get(root.getBegin().get().line) == null) {
                    lineToDotLabel.put(root.getBegin().get().line, ndName);
                }
                Matcher matcher = p.matcher(node.getTypeName());
                Matcher matcher2 = dotP.matcher(matcher.replaceAll(""));
                Matcher matcher3 = quotationP.matcher(matcher2.replaceAll("."));
                builder.append(System.lineSeparator() + ndName + " [label=\"" + matcher3.replaceAll("'") + ")\"];");
            }
            else {
                if (lineToDotLabel.get(root.getBegin().get().line) == null) {
                    lineToDotLabel.put(root.getBegin().get().line, ndName);
                }
                Matcher matcher = p.matcher(node.getTypeName());
                Matcher matcher2 = dotP.matcher(matcher.replaceAll(""));
                Matcher matcher3 = quotationP.matcher(matcher2.replaceAll("."));
                builder.append(System.lineSeparator() + ndName + " [label=\"" + matcher3.replaceAll("'") + "\"];");
            }

            if (parentNodeName != null)
                builder.append(System.lineSeparator() + parentNodeName + " -> " + ndName + ";");

            for (String a : attributes) {
                String attrName = nextNodeName();
                Matcher matcher = p.matcher(a);
                Matcher matcher2 = dotP.matcher(matcher.replaceAll(""));
                Matcher matcher3 = quotationP.matcher(matcher2.replaceAll("."));
                builder.append(System.lineSeparator() + attrName + " [label=\"" + matcher3.replaceAll("'") + "'\"];");
                builder.append(System.lineSeparator() + ndName + " -> " + attrName + ";");
            }

            for (int i = 0; i < subNodes.size(); i++) {
                output(subNodes.get(i), ndName, subNodes.get(i).getName(), builder, subNodesPrimary.get(i));
            }

            for (int i = 0; i < subLists.size(); i++) {
                String ndLstName = nextNodeName();
                Matcher matcher = p.matcher(subLists.get(i));
                Matcher matcher2 = dotP.matcher(matcher.replaceAll(""));
                Matcher matcher3 = quotationP.matcher(matcher2.replaceAll("."));
                builder.append(System.lineSeparator() + ndLstName + " [label=\"" + matcher3.replaceAll("'") + "\"];");
                builder.append(System.lineSeparator() + ndName + " -> " + ndLstName + ";");
                for (int j = 0; j < subListNodes.get(i).size(); j++) {
                    output(subListNodes.get(i).get(j), ndLstName, subLists_name.get(i), builder, subListNodesPrimary.get(i).get(j));
                }
            }
        }
    }

    private void addCfgAddDataFlowEdge(StringBuilder outputstrBuilder) {
        // 假如cfg lines
        Collection<Set<Edge>> cfgEdges = allmethodToCfgEdges.values();
        // 一个方法所有的边
        for (Set<Edge> methodEdges : cfgEdges) {
            for (Edge edge : methodEdges) {
                if (lineToDotLabel.get(Integer.parseInt(edge.getHead())) != null && lineToDotLabel.get(Integer.parseInt(edge.getTail())) != null) {
                    if (!lineToDotLabel.get(Integer.parseInt(edge.getHead())).equals("null") && !lineToDotLabel.get(Integer.parseInt(edge.getTail())).equals("null")) {
                        outputstrBuilder.append(System.lineSeparator() + lineToDotLabel.get(Integer.parseInt(edge.getHead())) + " -> "
                                + lineToDotLabel.get(Integer.parseInt(edge.getTail())) + " [color=\"red\"];");
                    }
                }
            }
        }

        Collection<Set<Edge>> dataFlowEdges = allmethodToallDataFlowEdges.values();
        // 一个方法所有的边
        for (Set<Edge> dataflowEdges : dataFlowEdges) {
            for (Edge edge : dataflowEdges) {
                if (lineToDotLabel.get(Integer.parseInt(edge.getHead())) != null && lineToDotLabel.get(Integer.parseInt(edge.getTail())) != null) {
                    if (!lineToDotLabel.get(Integer.parseInt(edge.getHead())).equals("null") && !lineToDotLabel.get(Integer.parseInt(edge.getTail())).equals("null")) {
                        outputstrBuilder.append(System.lineSeparator() + lineToDotLabel.get(Integer.parseInt(edge.getHead())) + " -> "
                                + lineToDotLabel.get(Integer.parseInt(edge.getTail())) + " [color=\"green\"];");
                    }
                }
            }
        }
    }

    private String nextNodeName() {
        return "n" + (nodeCount++);
    }
}
