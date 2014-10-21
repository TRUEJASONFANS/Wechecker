package soot.we.android.controlflowgraph;

import soot.toolkits.graph.Block;

public class ControlFlowGraphEdge {
	private Block start;
	private Block end;
	private boolean visited;
	public boolean isVisited() {
		return visited;
	}
	public boolean isVisited(Block b1,Block b2){
		return visited;
	}
	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	public void setVisited(Block b1,Block b2,boolean statu){
		this.visited = statu;
	}
	public Block getEnd() {
		return end;
	}
	public void setEnd(Block end) {
		this.end = end;
	}
	public Block getStart() {
		return start;
	}
	public void setStart(Block start) {
		this.start = start;
	}
	public ControlFlowGraphEdge(Block s,Block e,boolean v){
		start = s;
		end = e;
		visited = v;
	}
}
