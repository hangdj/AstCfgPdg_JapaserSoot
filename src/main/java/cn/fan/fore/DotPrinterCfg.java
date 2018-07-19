package cn.fan.fore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;

import cn.fan.model.Edge;
import cn.fan.model.CfgNode;
import cn.fan.model.PrinterI;
import cn.fan.model.SourceSubPathEnum;
import cn.fan.service.PaserSourceCodeToAst;
import cn.fan.service.TranformSourceCodeToLine;
import cn.fan.tool.LoadSourceCode;

/**
 * 将Node数据结构打印成Dot文件的工具类
 * @author fan
 *
 */
public class DotPrinterCfg implements PrinterI {

	private TranformSourceCodeToLine tranformSourceCodeToLine;
	private PaserSourceCodeToAst paserSourceCodeToAst;
	private HashMap<Integer, CfgNode<String>> allEdges;
	private Logger logger = Logger.getLogger(DotPrinterCfg.class);

	public DotPrinterCfg(TranformSourceCodeToLine tranformSourceCodeToLine, PaserSourceCodeToAst paserSourceCodeToAst,
						HashMap<Integer, CfgNode<String>> allEdges) {
		// TODO Auto-generated constructor stub
		this.tranformSourceCodeToLine = tranformSourceCodeToLine;
		this.paserSourceCodeToAst = paserSourceCodeToAst;
		this.allEdges = allEdges;
	}

	/**
	 * 输出dot文件
	 * 
	 * @see cn.fan.fore.Printer#print(com.github.javaparser.ast.Node,
	 * cn.fan.model.PrinterType)
	 */
	@Override
	public void print(String methodName) {
		// TODO Auto-generated method stub
		//获取源代码的行号
		HashMap<Integer, String> transform = tranformSourceCodeToLine.transform();
		//获取一个方法的范围
		List<Integer> linesFromOneMethod = paserSourceCodeToAst.getLinesFromOneMethod(methodName);
		String contents = productContents(methodName, transform, linesFromOneMethod);
		createDotFile(methodName, contents);
	}

	private String productContents(String methodName, HashMap<Integer, String> transform, List<Integer> linesFromOneMethod) {
		StringBuilder contents = new StringBuilder("digraph \"" + methodName + "\" {");
		contents.append("node [shape=box];\n");
		List<Edge> readyEdges = new ArrayList<Edge>();
		for (int i = linesFromOneMethod.get(0); i <= linesFromOneMethod.get(1); i++) {
			contents.append("\"" + i + "\" [ label=\"" + transform.get(i).trim() + "\"];\n");
		}
		for (Entry<Integer, CfgNode<String>> entry : allEdges.entrySet()) {
			//所有前驱节点
			for (String preds : entry.getValue().getPreds()) {
				contents.append("\"" + preds + "\"->\"" + entry.getKey() + "\";\n");
				readyEdges.add(new Edge(preds, entry.getKey() + ""));
			}
			//所有后继节点
			for (String succs : entry.getValue().getPreds()) {
				contents.append("\"" + entry.getKey() + "\"->\"" + succs + "\";\n");
				readyEdges.add(new Edge(entry.getKey() + "", succs));
			}
		}
		for (int i = linesFromOneMethod.get(0); i < linesFromOneMethod.get(1); i++) {
			Edge eg = new Edge(String.valueOf(i), String.valueOf(i + 1));
			boolean flag = false; //不存在一条边
			for (Edge edge : readyEdges) {
				if (edge.equals(eg)) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				contents.append("\"" + i + "\"->\"" + (i + 1) + "\";\n");
			}

		}
		contents.append("}");
		return contents.toString();
	}

	private void createDotFile(String methodName, String contents) {
		Random random = new Random();
		Path load = null;
		try {
			load = LoadSourceCode.load(SourceSubPathEnum.Dot_FILE_PATH, methodName + "_" + random.nextInt() + ".dot");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File file = new File(load.toString());
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(contents);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("创建dot文件成功:" + load);
	}
}