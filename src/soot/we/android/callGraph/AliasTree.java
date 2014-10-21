package soot.we.android.callGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Type;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.we.android.component.EntityClass;
import soot.we.android.component.EntityMethod;
import soot.we.android.component.EntitySourceInf;

public class AliasTree {
	
	public AliasTreeNode root;
	public int depth;
	public EntitySourceInf Source;
	
	public Map<Type,AliasTreeNode>  typemap;
	
	public AliasTree(AliasElement r,List<AliasMap> maplist,EntitySourceInf source, EntityClass entityClass, EntityMethod entityMethod){
		typemap=new HashMap<Type,AliasTreeNode>();
		Source = source;
		root = buildAliasTree(maplist,r,entityClass,entityMethod);
		
	}
	public AliasTreeNode buildAliasTree(List<AliasMap> list, AliasElement root, EntityClass entityClass, EntityMethod entityMethod){
		if(root==null) return null;	
		int maxDepth=0;
		AliasTreeNode Root = new AliasTreeNode(root,0);
		typemap.put(Root.type, Root);
		AliasTreeNode MaxDeepTarget = null;
		for (int i = list.size()-1; i >= 0; i--) {//Retrieve the map(=) from the list to construct a tree
			AliasMap tmp = list.get(i);
			tmp.print();
			AliasElement left = tmp.a1;
			AliasElement right = tmp.a2;
			Value rValue = right.v;
			AliasTreeNode target = searchTree(Root, tmp.a2);
			if (target!=null) {//the target(right one) in the tree,if can find ,thus means the left one can directly add to the tree
				target.nodes.add(left);
			} 
			else if (rValue instanceof JInstanceFieldRef) {//if rv is base.ref type, it may need to further check the base whether it exits in the Alias Tree
				JInstanceFieldRef rinf = (JInstanceFieldRef) rValue;
				AliasElement pElement = new AliasElement(rinf.getBase(),right.emethod,right.eclass);
				target = searchTree(Root, pElement);
				if (target!=null&&target.childrens == null) {//if can find the target,that's means leftone and rightone is the subnode of target.
						target.childrens = new AliasTreeNode(left,target.deep + 1);
						target.childrens.addBrother(right);
						if (maxDepth < target.deep + 1) {
							maxDepth = target.deep + 1;
							MaxDeepTarget = target.childrens;
						}
						typemap.put(left.v.getType(),target.childrens);
					} 
				else if(target!=null&&target.childrens!=null){
						target.childrens.addBrother(left);
						target.childrens.addBrother(right);
					}
				else continue;
			}
		}
		AliasElement source = new AliasElement(Source.getSource(),entityMethod,entityClass);
		if(MaxDeepTarget!=null){
			 MaxDeepTarget.childrens = new AliasTreeNode(source,MaxDeepTarget.deep + 1);
			 typemap.put(Source.getSource().getType(), MaxDeepTarget.childrens);
			}
		else if(Root!=null){
			Root.childrens = new AliasTreeNode(source,Root.deep + 1);
			typemap.put(Source.getSource().getType(), Root.childrens);
			}
		depth = maxDepth+1;
		Root.print();
		return Root;
	}

	public AliasTreeNode searchTree(AliasTreeNode r, AliasElement a2) {
		for (Iterator<AliasElement> it = r.nodes.iterator(); it.hasNext();) {
			AliasElement tmp = it.next();
			if(tmp.eqaulTo(a2))
				return r;
		}
		if (r.childrens != null)
			return searchTree(r.childrens,a2);
		else
			return null;
	}
	public void updateAliasTree(JAssignStmt s,EntityClass curCalss,EntityMethod curMethod) {
		Value lv = s.getLeftOp();
		Value rv = s.getRightOp();
		Type targetType= rv.getType();
		AliasTreeNode tmpnode = typemap.get(targetType);
		for (Iterator<AliasElement> it = tmpnode.nodes.iterator(); it.hasNext();) {
			AliasElement tmp = it.next();
			if(tmp.equalTO(rv, curCalss, curMethod)) {
				AliasElement left = new AliasElement (lv, curMethod, curCalss);
				tmpnode.nodes.add(left);
			}
		}
		
	}
	public void updateAliasTree(AliasTreeNode targetnode, Value v,EntityMethod m,EntityClass c){
		AliasElement tmp = new AliasElement(v,m,c);
		targetnode.nodes.add(tmp);
	}
	public AliasTreeNode searchTree(Type targetType, AliasElement target) {
		// TODO Auto-generated method stub
		 AliasTreeNode tmpNode = typemap.get(targetType);
		 if(tmpNode==null) return null; 
		 for(Iterator<AliasElement> it = tmpNode.nodes.iterator();it.hasNext();){
				AliasElement tmpe = it.next();
				if(tmpe.eqaulTo(target)){
					return tmpNode;
				}
		 }
		 
		return null;
	}
	
}
class AliasMap {
	public AliasElement a1;
	public AliasElement a2;
	public AliasMap(AliasElement a1,AliasElement a2){
		this.a1 = a1;
		this.a2 = a2;
	}
	public void print() {
		System.out.println(a1.emethod.getMethodName()+a1.v+"="+a2.emethod.getMethodName()+a2.v);
	}
}
class AliasTreeNode {
	public Set<AliasElement> nodes; 
	public AliasTreeNode childrens;
	public int deep = 0;
	public Type type;
	public AliasTreeNode(AliasElement node,int d){
		nodes = new HashSet<AliasElement>();
		nodes.add(node);
		this.deep = d;
		this.childrens = null;
		this.type = node.v.getType();
	}
	
	public void addBrother(AliasElement e){
		nodes.add(e);

	}
	public void Setchildren(AliasTreeNode child){
		this.childrens = child;
	}
	public void print(){
		System.out.println("deep:"+this.deep);
		for (Iterator<AliasElement> it = nodes.iterator(); it.hasNext();) {
			AliasElement tmp = it.next();
			tmp.print();
		}
		if(childrens!=null)
			childrens.print();
	}
}
class AliasElement {
	Value v;
	EntityClass eclass;
	EntityMethod emethod;
 
	public AliasElement(Value v1, EntityMethod emethod,EntityClass eclass) {
		this.v = v1;
		this.eclass = eclass;
		this.emethod = emethod;
	}
	public boolean eqaulTo(AliasElement e){
		if(this.v.toString().equals(e.v.toString()))
				if(this.emethod.equals(e.emethod))
					if(this.eclass.equals(e.eclass))
						return true;
		return false;
	}
	public boolean equalTO(Value v,EntityClass curCalss,EntityMethod curMethod) {
		if(this.v.toString().equals(v.toString()))
			if(this.emethod.equals(curMethod))
				if(this.eclass.equals(curCalss))
					return true;
	return false;
	}
	public void print() {
		System.out.println(emethod.getMethodName()+" Type:"+v.getType()+" Value:"+v);
	}
}