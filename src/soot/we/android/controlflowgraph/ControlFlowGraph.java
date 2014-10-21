package soot.we.android.controlflowgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.toolkits.graph.Block;

public class ControlFlowGraph {
	private List<Block> entrys;
	private List<Block> exits;
	private List<List<Block>> sequences;
	private Map<Block,ControlFlowGraphNode> map;
	private int maximumBlockSequence;
	public List<Block> getExits() {
		return exits;
	}
	public void setExits(List<Block> exits) {
		this.exits = exits;
	}
	public List<Block> getEntrys() {
		return this.entrys;
	}
	public void setEntrys(List<Block> entrys) {
		this.entrys = entrys;
	}
	public ControlFlowGraph(List<Block> entrys,List<Block> exits){
		this.entrys = entrys;
		this.exits = exits;
		
	}
	public ControlFlowGraph(){
		this.exits = new ArrayList<Block>();
		this.entrys = new ArrayList<Block>();
		this.sequences = new ArrayList<List<Block>>();
		this.map = new HashMap<Block,ControlFlowGraphNode>();
		this.maximumBlockSequence = 1000;
	}

	public List<List<Block>> getSequences() {
		return sequences;
	}
	public void setSequences(List<List<Block>> sequences) {
		this.sequences = sequences;
	}
	public  void generatePath() {
		for(int i=0;i<entrys.size();i++){
			Block entry = entrys.get(i);
			List<Block> sequence = new ArrayList<Block>();
			sequence.add(entry);
			Dfs(entry,sequence);
		}
	}
	private void Dfs(Block startb,List<Block> order) {
		if(this.sequences.size() > maximumBlockSequence) return;
		ControlFlowGraphNode bNode = map.get(startb);
		List<ControlFlowGraphEdge> edges = bNode.getEdges();
		if(edges.size()>0) {
			for(ControlFlowGraphEdge e: edges){
				if(!e.isVisited()){
					order.add(e.getEnd());
					e.setVisited(true);
					Dfs(e.getEnd(),order);
					e.setVisited(false);
					order.remove(order.size()-1);
				}
			}
		}
		else{
			List<Block> tmpOrder = new ArrayList<Block>();
			tmpOrder.addAll(order);
			this.sequences.add(tmpOrder);
		}
	}
	public Map<Block,ControlFlowGraphNode> getMap() {
		return map;
	}
	public void setMap(Map<Block,ControlFlowGraphNode> map) {
		this.map = map;
	}

}
