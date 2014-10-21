package soot.we.android.controlflowgraph;

import java.util.List;

import soot.toolkits.graph.Block;

public class ControlFlowGraphNode {
	private Block b;
	private List<ControlFlowGraphEdge> edges;
	public List<ControlFlowGraphEdge> getEdges() {
		return edges;
	}
	public void setEdges(List<ControlFlowGraphEdge> edges) {
		this.edges = edges;
	}
	public Block getB() {
		return b;
	}
	public void setB(Block b) {
		this.b = b;
	}
	public ControlFlowGraphNode(Block b,List<ControlFlowGraphEdge> edges){
		this.b = b;
		this.edges = edges;
	}
	
}
