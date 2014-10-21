package soot.we.android.callGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.SootClass;
import soot.SootField;
import soot.jimple.Stmt;
import soot.we.android.component.EntitySourceInf;
import soot.we.android.component.ThisPointer;

public class ClassVarsStore {
	public Set<taintedEntityClassVarible> publicStaticVars;
	public Map<ThisPointer,ArrayList<taintedEntityClassVarible>> testClassVars;
	public ClassVarsStore() {
		publicStaticVars = new  HashSet<taintedEntityClassVarible> ();
		testClassVars = new HashMap<ThisPointer,ArrayList<taintedEntityClassVarible>>();
	}
	public ClassVarsStore(ClassVarsStore cstore){
		
		publicStaticVars = new  HashSet<taintedEntityClassVarible> ();
		testClassVars = new HashMap<ThisPointer,ArrayList<taintedEntityClassVarible>>();
		assign(cstore);
	}
	public void assign(ClassVarsStore classstoreBefore) {
		// TODO Auto-generated method stub
		this.publicStaticVars.clear();;
		this.testClassVars.clear();
		publicStaticVars.addAll(classstoreBefore.publicStaticVars);
		testClassVars.putAll(classstoreBefore.testClassVars);
		
	}
	public void merge(ClassVarsStore classstoreAfterOnecallbackMethod) {
		// TODO Auto-generated method stub
		publicStaticVars.addAll(classstoreAfterOnecallbackMethod.publicStaticVars);
		testClassVars.putAll(classstoreAfterOnecallbackMethod.testClassVars);
		
	}
	public void clear() {
		// TODO Auto-generated method stub
		publicStaticVars.clear();
		testClassVars.clear();
	}
}
class taintedEntityClassVarible{
	public Stmt stmt;
	public EntitySourceInf pre;
	public SootField sf;
	public SootClass sc;
	public taintedEntityClassVarible(SootClass sc,SootField sf,Stmt stmt,EntitySourceInf s) {
		this.sc = sc;
		this.sf = sf;
		this.stmt = stmt;
		this.pre = s;
	}
	
}