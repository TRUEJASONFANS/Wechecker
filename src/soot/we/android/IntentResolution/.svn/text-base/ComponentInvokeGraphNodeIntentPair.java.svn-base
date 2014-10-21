package soot.we.android.IntentResolution;

import soot.Value;
import soot.we.android.component.EntityClass;

public class ComponentInvokeGraphNodeIntentPair{
	private ComponentInvokeGraphNode componentInvokeTreeNode;
	private IntentValue intent;
	private boolean visited;
	public ComponentInvokeGraphNodeIntentPair(EntityClass teclass, Value v,String methodname,String classname) {
		componentInvokeTreeNode = new ComponentInvokeGraphNode(teclass);
		IntentValue tv = new IntentValue(v, methodname, classname);
		setIntent(tv);
		visited = false;
	}

	public ComponentInvokeGraphNode getComponentInvokeTreeNode() {
		return componentInvokeTreeNode;
	}
	public void setComponentInvokeTreeNode(ComponentInvokeGraphNode componentInvokeTreeNode) {
		this.componentInvokeTreeNode = componentInvokeTreeNode;
	}
	public IntentValue getIntent() {
		return intent;
	}

	public void setIntent(IntentValue intent) {
		this.intent = intent;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	
}