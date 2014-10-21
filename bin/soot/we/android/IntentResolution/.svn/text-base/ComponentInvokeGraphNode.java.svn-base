package soot.we.android.IntentResolution;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Value;
import soot.we.android.component.EntityClass;

public class ComponentInvokeGraphNode{
	public EntityClass eclass;
	public Set<ComponentInvokeGraphNodeIntentPair> childrenNodes;
	public ComponentInvokeGraphNode(EntityClass ec,HashSet<ComponentInvokeGraphNodeIntentPair> cNodes){
		this.eclass = ec;
		this.childrenNodes=cNodes;
	}
	public ComponentInvokeGraphNode(EntityClass ec) {
		eclass = ec;
		childrenNodes = new HashSet<ComponentInvokeGraphNodeIntentPair>();
	}
	/**
	 * teclass: represent the comoponent to invoke next
	 * Value: represent the Intent obeject in jimple ;
	 * @param teclass
	 * @param v
	 * @return
	 */
	public boolean Addchildren(EntityClass teclass,Value v,String methodname,String classname) {
		if(childrenNodes!=null) {
			Iterator<ComponentInvokeGraphNodeIntentPair> iterator = childrenNodes.iterator();
			while(iterator.hasNext()) {
				ComponentInvokeGraphNodeIntentPair temp = iterator.next();
				ComponentInvokeGraphNode t = temp.getComponentInvokeTreeNode();
				if(t.eclass.getclassName().equals(teclass.getclassName())&&temp.getIntent().equals(v))
					return false;
			}
			childrenNodes.add(new ComponentInvokeGraphNodeIntentPair(teclass,v, methodname, classname));
			return true;
		}
		else return false;
	}
}

