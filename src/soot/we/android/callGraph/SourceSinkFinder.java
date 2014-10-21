package soot.we.android.callGraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.we.android.MainThread;
import soot.we.android.IntentResolution.ComponentInvokeGraphNode;
import soot.we.android.IntentResolution.ComponentInvokeGraphNodeIntentPair;
import soot.we.android.IntentResolution.IntentValue;
import soot.we.android.IntentResolution.TaintedIntent;
import soot.we.android.XML.EntityApplicationBase;
import soot.we.android.XML.EntityComponent;
import soot.we.android.XML.EntityIntentFilter;
import soot.we.android.component.EntityClass;
import soot.we.android.component.EntityMethod;
import soot.we.android.component.EntityPolyMethod;
import soot.we.android.component.EntitySinkInf;
import soot.we.android.component.EntitySourceInf;
import soot.we.android.component.LeakingPath;
import soot.we.android.component.ThisPointer;
import soot.we.android.log.AlarmLog;
import soot.we.android.parser.PermissionMethodParser;
import soot.we.android.resource.SourceInfo;
import soot.we.android.test.Test;

public class SourceSinkFinder {

	private Map<String, AndroidMethod> sourceMethods;
	private Map<String, AndroidMethod> sinkMethods;
	private static final SourceInfo sourceInfo = new SourceInfo(true);
	private UnitGraph unitgraph;
	private EntityClass eclass;
	private EntityMethod emethod;
	public ThisPointer thisPointer;
	public EntityClass getEntityclass() {
		return eclass;
	}

	public void setEntityclass(EntityClass eclass) {
		this.eclass = eclass;
	}

	public EntityMethod getEntitymethod() {
		return emethod;
	}

	public void setEntitymethod(EntityMethod emethod) {
		this.emethod = emethod;
	}

	public UnitGraph getUnitgraph() {
		return unitgraph;
	}

	public void setUnitgraph(UnitGraph unitgraph) {
		this.unitgraph = unitgraph;
	}

	public Map<String, AndroidMethod> getSourceMethods() {
		return sourceMethods;
	}

	public Map<String, AndroidMethod> getSinkMethods() {
		return sinkMethods;
	}

	public void calculateSourcesSinksEntrypoints(String sourceSinkFile)
			throws IOException {
		PermissionMethodParser parser = PermissionMethodParser.fromFile(sourceSinkFile);
		Set<AndroidMethod> sources = new HashSet<AndroidMethod>();
		Set<AndroidMethod> sinks = new HashSet<AndroidMethod>();
		for (AndroidMethod am : parser.parse()) {
			if (am.isSource())
				sources.add(am);
			if (am.isSink())
				sinks.add(am);
		}
		this.sourceMethods = new HashMap<String, AndroidMethod>();
		for (AndroidMethod am : sources)
			this.sourceMethods.put(am.getSignature(), am);

		this.sinkMethods = new HashMap<String, AndroidMethod>();
		for (AndroidMethod am : sinks)
			this.sinkMethods.put(am.getSignature(), am);
		System.out.println("Created a SourceSinkManager with "
				+ this.sourceMethods.size() + " sources, "
				+ this.sinkMethods.size() + " sinks ");
	
	}

	public boolean isSink(Stmt sCallSite,EntityClass curClass) {
		assert sCallSite != null;
		if(sCallSite.containsFieldRef()&&sCallSite.getInvokeExpr().getMethod().
				equals("<android.app.Activity: void setResult(int,android.content.Intent)>")){
			if(curClass.isEntryPointClass())
				return true;
			else
				return false;
		}
			
		return sCallSite.containsInvokeExpr()
				&& isSinkMethod(sCallSite.getInvokeExpr().getMethod());
	}

	public SourceInfo getSourceInfo(Stmt sCallSite) {
		assert sCallSite != null;
		if (!sCallSite.containsInvokeExpr())
			return null;
		return getSourceMethodInfo(sCallSite.getInvokeExpr().getMethod());
	}

	public SourceInfo getSourceMethodInfo(SootMethod sMethod) {
		if (!MainThread.sourceMethods.containsKey(sMethod.toString()))
			return null;
		return sourceInfo;
	}

	public boolean isSinkMethod(SootMethod sMethod) {
		return MainThread.sinkMethods.containsKey(sMethod.toString());
	}

	public boolean isDefinitionStmt(Unit stmt) {
		// TODO Auto-generated method stub
		if (stmt instanceof DefinitionStmt) {
			return true;
		}
		return false;
	}

	public boolean isReturnStmt(Unit stmt) {
		// TODO Auto-generated method stub
		if (stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt) {
			return true;
		}
		return false;
	}

	public boolean isIntraProceduralStmt(Unit u) {
		if (u instanceof IfStmt || u instanceof LookupSwitchStmt
				|| u instanceof TableSwitchStmt || u instanceof GotoStmt) {
			return true;
		}
		return false;
	}

	public boolean isInterProceduralStmt(Stmt stmt) {
		// TODO Auto-generated method stub
		if (stmt.containsInvokeExpr())
			return true;
		else
			return false;
	}
	public boolean isIntentRelatedStmt(Stmt sCallSite) {
		assert sCallSite != null;
		return sCallSite.containsInvokeExpr()
				&& isIntentRelatedMethods(sCallSite.getInvokeExpr().getMethod());
	}
	public boolean isIntentRelatedMethods(SootMethod sm){
		return MainThread.intentRelatedMethods.containsKey(sm.toString());
		
	}
	public void updateComponentInvokeTree(Stmt stmt, EntityMethod curMethod,EntityClass curClass,List<EntityClass> classes) {
		int argCount = stmt.getInvokeExpr().getArgCount();
		List<Value> argValues=stmt.getInvokeExpr().getArgs();
		List<ValueBox> useValues =  stmt.getInvokeExpr().getUseBoxes();
		Value v = useValues.get(useValues.size()-1).getValue();//jason
		if(!v.getType().toString().equals("android.content.Intent")) return;
		if(argCount==2) {//explict way to use intent
			Value tempV1 = argValues.get(0);
			Value tempV2 = argValues.get(1);
			if(tempV1.getType().equals(curClass.sootclass.getType())){
				if(tempV2 instanceof ClassConstant) {
					ClassConstant tv2classconstant = (ClassConstant) tempV2;
					String className = tv2classconstant.getValue();
					if(className.contains("/")){
							className=className.replace('/', '.');
						}
					for(EntityClass eclass:classes) {
						if(eclass.getclassName().equals(className)&&!eclass.getclassName().equals(curClass.getclassName())) {
							//Some Component can invoke itself
							if(curClass.getComponentInvokeGraph()==null) continue;
							if(curClass.getComponentInvokeGraph().Addchildren(eclass,v,curMethod.getMethodName(),
								curClass.getclassName())) {
//								AlarmLog.writeToAlarm("a intent:");
//								AlarmLog.writeToAlarm(curClass.getclassName()+"->"+eclass.getclassName());
//								AlarmLog.writeToAlarm("!!!!!!!!!!");
							}
						}
					}
				}
			}
			
		}
		if(argCount==1){//implicit way to use intent
			Value tempV1= argValues.get(0);
			String temps = tempV1.toString();
			temps=temps.replace('"',' ');
			temps=temps.trim();
			EntityApplicationBase application = Test.app;
			ArrayList<EntityComponent> components = application.getComponents();
			for(int i=0;i<components.size();i++){
				ArrayList<EntityIntentFilter> intentFilter=components.get(i).getIntentFilter();
				for(int j=0;j<intentFilter.size();j++) {
					ArrayList<String> actions = intentFilter.get(j).actions;
					for(String s:actions){
						if(s.equals(temps)) {
							for(EntityClass eclass:classes){
								if(eclass.getclassName().equals(components.get(i).getComponnetName())
										&&!eclass.getclassName().equals(curClass.getclassName())) {
									if(curClass.getComponentInvokeGraph()==null) continue;
									if(curClass.getComponentInvokeGraph().Addchildren(eclass,v,curMethod.getMethodName(),
										curClass.getclassName())){
//										AlarmLog.writeToAlarm("a intent:");
//										AlarmLog.writeToAlarm(curClass.getclassName()+"->"+eclass.getclassName());
//										AlarmLog.writeToAlarm("!!!!!!!!!!");
									}
								}
							}
							
						}
					}
				}
			}
		}
		
	}

	public List<EntitySourceInf> initializeTaintedLocalVarsList(
			List<EntitySourceInf> oldVars,
			Map<Integer, EntitySourceInf> paraTmap, EntityMethod em,
			EntityClass ec,Stack<InvokMethodInfo> invokestack) {

		List<EntitySourceInf> NewtaintedVariables = new ArrayList<EntitySourceInf>();
		AndroidLifeCycleCallGraph androidlifecyclegrpah = new AndroidLifeCycleCallGraph();
		if (paraTmap != null && !paraTmap.isEmpty()) {
			Iterator<Entry<Integer, EntitySourceInf>> iter = paraTmap.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<Integer, EntitySourceInf> entry = iter.next();
				Integer key = entry.getKey();
				EntitySourceInf val = entry.getValue();
				InvokMethodInfo lastMethodinfo = invokestack.peek();
				Stmt lastInvokestmt = (Stmt)lastMethodinfo.getUnit();
				Value taintedparameter = new ParameterRef(em.sootmethod.getParameterType(key.intValue()), key.intValue());
				EntitySourceInf newTaintedParamsource = new EntitySourceInf(taintedparameter,lastInvokestmt , val, 
				em.getMethodName(),ec.getclassName());
				NewtaintedVariables.add(newTaintedParamsource);
			}

		} else if (ec.isEntryPointClass()&& androidlifecyclegrpah.isLifeCycle(em.getMethodName())) {
			NewtaintedVariables.addAll(oldVars);
		}
		return NewtaintedVariables;
	}
	public List<taintedEntityClassVarible> initializeTaintedlistClassVarsList(EntityClass curClass,
			List<taintedEntityClassVarible> taintedClassVars){
		ClassVarsStore classvarstore = BuildCallGraph.classvarstore;
		if(taintedClassVars==null)
			taintedClassVars = new ArrayList<taintedEntityClassVarible>();
		taintedClassVars.addAll(classvarstore.publicStaticVars);
		thisPointer = ThisPointer.getThisPointer(classvarstore, thisPointer);
		if(classvarstore.testClassVars.get(thisPointer)!=null)
			taintedClassVars.addAll(classvarstore.testClassVars.get(thisPointer));

		
		Set<ThisPointer> pointers=classvarstore.testClassVars.keySet();
		String[] compType = {"android.app.Activity","android.app.ListActivity","android.content.BroadcastReceiver","android.app.Service",
				"android.app.Application","GlobalCallback","callback"};
		for(ThisPointer p:pointers) {
			String type = p.vThis.toString();
			for(String s:compType){
				if(s.equals(type)) {
					taintedClassVars.addAll(classvarstore.testClassVars.get(p));
				}
			}
		}
		return taintedClassVars;
	}
	/** This function is to get tainted augments in a function invoke;
	 * @param stmt
	 * @param taintedVariables
	 * @return
	 */
	public Map<Integer, EntitySourceInf> getTaitedfParameters(Stmt stmt,
			List<EntitySourceInf> taintedVariables) {
		List<Value> invokeAugments = stmt.getInvokeExpr().getArgs();
		Map<Integer, EntitySourceInf> paraTaintMap = new HashMap<Integer, EntitySourceInf>();

		for (int i = 0; i < invokeAugments.size(); i++) {
			for (EntitySourceInf t : taintedVariables) {
				if (t.getSource().equivTo(invokeAugments.get(i)))
					paraTaintMap.put(i, t);
			}
		}
		return paraTaintMap;
	}

	public boolean checkAStmt(Stmt stmt,
			List<EntitySourceInf> taintedVariables, EntityMethod curMethod,
			EntityClass curClass) {
		// TODO Auto-generated method stub
		System.err.println("Sink found: " + stmt);
		String curmethodname = curMethod.getMethodName();
		String curclassname = curClass.getclassName();
		//InvokeStmt is = (InvokeStmt) stmt;
		ValueBox ivbBox = stmt.getInvokeExprBox();
		List<ValueBox> valuebox = ivbBox.getValue().getUseBoxes();
		boolean rs = false;
		for (ValueBox itrator : valuebox) {
			Value tempV = itrator.getValue();
			for (int i = 0; i < taintedVariables.size(); i++) {
				if (taintedVariables.get(i).getSource().equivTo(tempV)) {		
					for(int j=0;j<BuildCallGraph.leakingPaths.size();j++) {
						EntitySinkInf sinkBefore = BuildCallGraph.leakingPaths.get(j).sink;
						if(sinkBefore.getStmt().toString().equals(stmt.toString()))
							return false;
					}
					rs = true;
					EntitySinkInf sink = new EntitySinkInf(tempV, stmt,curmethodname, curclassname);
					RetriveTheLeakingPath(taintedVariables.get(i), sink,curClass);
				}
			}
		}
		return rs;
	}

	private void RetriveTheLeakingPath(EntitySourceInf entitySourceInf,
			EntitySinkInf sink, EntityClass curClass) {
		// TODO Auto-generated method stub
		EntitySourceInf p = entitySourceInf;
		EntitySourceInf prep = entitySourceInf;
		List<List<EntitySourceInf>> sumLeakingPath = new ArrayList<List<EntitySourceInf>>();
		List<EntitySourceInf> currentComponentLeakingPath = new ArrayList<EntitySourceInf>();
		while (p != null) {
			currentComponentLeakingPath.add(p);
			prep = p;
			p = p.getPre();
		}
		if(curClass.taintedIntent.size()>0&&prep.getStmt().toString().contains("android.content.Intent")) {
			 RetriveInterComponentPath(prep,curClass,currentComponentLeakingPath,sumLeakingPath,0);
		}
		if(sumLeakingPath.size()==0) {
			List<EntitySourceInf> sources = new ArrayList<EntitySourceInf>();
			for(int i=currentComponentLeakingPath.size()-1;i>=0;i--) {
				sources.add(currentComponentLeakingPath.get(i));
			}
			sumLeakingPath.add(sources);
		}
		for(List<EntitySourceInf> sourcepath:sumLeakingPath){
			LeakingPath lp = new LeakingPath(sourcepath,sink);
			if(!lp.IsCorrectPath(sourcepath.get(0).getClassName()))
				return;
			for(int j=0;j<BuildCallGraph.leakingPaths.size();j++) {
				if(BuildCallGraph.leakingPaths.get(j).equalTo(lp)){
					return;
				}
			}
			BuildCallGraph.leakingPaths.add(lp);//get rid of the repeating leakingPath;
			outputPrint(sourcepath,sink);
		}
	}

	private void RetriveInterComponentPath(EntitySourceInf lastTaintedInComponent,
			EntityClass curClass, List<EntitySourceInf> currentComponentLeakingPath, List<List<EntitySourceInf>> sumLeakingPath,int hop) {
			if(hop>2) return;
			String type = lastTaintedInComponent.getSource().getType().toString();
			for(TaintedIntent taintedintent:curClass.taintedIntent)	{
				if(type.equals(taintedintent.getType())){
					List<EntitySourceInf> LeakingPath = new ArrayList<EntitySourceInf>();
					LeakingPath.addAll(currentComponentLeakingPath);
					EntitySourceInf p = taintedintent.getTaintedsource();
					EntitySourceInf prep = p;
					while(p!=null) {
						LeakingPath.add(p);
						prep = p;
						p = p.getPre();
					}
					if(prep.getStmt().toString().contains("android.content.Intent")&&
						!(prep.getStmt().toString().equals(lastTaintedInComponent.getStmt().toString()))){
						RetriveInterComponentPath(prep,taintedintent.getSourceclass(),LeakingPath,sumLeakingPath,hop+1);
					}
					else{
						reverseLeakingPath(LeakingPath);
						sumLeakingPath.add(LeakingPath);
					}
				}
			}

	}
	private void reverseLeakingPath(List<EntitySourceInf> sources) {
		int head = 0;
		int tail = sources.size()-1;
		while(head<tail) {
			EntitySourceInf temp = sources.get(head);
			sources.set(head, sources.get(tail));
			sources.set(tail, temp);
			head++;
			tail--;
		}
	}
    public void outputPrint(List<EntitySourceInf> path, EntitySinkInf sink) {
		System.err.println("Alert There exits leaking path !!!!");
		System.err.println("Found a flow from Source to Sink:");
    	System.err.println("****************************************************************************************************");
		AlarmLog.writeToAlarm("*************************************************************************************************");
		AlarmLog.writeToCommonAlarm("*******************************************************************************************");
		AlarmLog.writeToCommonAlarm("Your application may be in danger, detailed information as following : ");
		int i = 1;
		EntitySourceInf s = path.get(0);
		AlarmLog.writeToAlarm("step " + i + "  Source:");
		AlarmLog.writeToAlarm("Class:" + s.getClassName() + "  Method:"+ s.getMethodName());	
		System.out.println("step " + i + "  Source:");
		System.out.println("Class:" + s.getClassName() + "  Method:"+ s.getMethodName());
		if (s.getStmt() != null) {
			System.out.println(s.getStmt().toString());
			AlarmLog.writeToAlarm("Value: "+s.getSource().toString()+"        Stmt:"+s.getStmt().toString());
		}
		// alert for common users***start*******
		AlarmLog.writeToCommonAlarm(MainThread.FriendlyAlarmMap.get(s.getStmt().getInvokeExpr().getMethod().toString()));		
		// alert for common users***end*******
		i = i + 1;	
		for (int k = 1; k <path.size(); k++) {
			s = path.get(k);
			AlarmLog.writeToAlarm("step " + i + "  propagated:");
			AlarmLog.writeToAlarm("Class:" + s.getClassName() + "  Method:"+ s.getMethodName());
			
			System.out.println("step " + i + "  propagated:");
			System.out.println("Class:" + s.getClassName() + "  Method:"+ s.getMethodName());
			
			if (s.getStmt() != null) {
				System.out.println(s.getStmt().toString());
				AlarmLog.writeToAlarm("Value: "+s.getSource().toString()+"        Stmt:"+s.getStmt().toString());
			}
			i++;
		}
		System.out.println("step " + i + "  : Sink");
		System.out.println("Class:" + sink.getClassName() + "  Method:"+ sink.getMethodName());
		System.out.println(sink.getStmt().toString());
		AlarmLog.writeToAlarm("step " + i + "  : Sink");
		AlarmLog.writeToAlarm("Class:" + sink.getClassName() + "  Method:"+ sink.getMethodName());
		AlarmLog.writeToAlarm(sink.getStmt().toString());
		System.err.println("********************************************************************************************************");
		AlarmLog.writeToAlarm("*************************************************************************************************");
    }
    /**
     * 该方法是判断这条方法调用语句的方法是开发者所写还是由android sdk提供或者java自身的库方法。通过其包名前缀判断。
     * @param stmt
     * @param curClassName
     * @param eClassList
     * @return
     */
	public boolean isDefinebyDeveloper(Stmt stmt, String curClassName,
			List<EntityClass> eClassList) {

		String declaringClass = stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
		// method in current class
		if (declaringClass.equals(curClassName)) {
			return true;
		} else {
			for (EntityClass eClass : eClassList) {
				if (declaringClass.equals(eClass.getclassName())) {
					return true;
				}
			}
		}
		return false;

	}

	/**
	 * Update tainedList according to different "tag"
	 * 
	 * @param stmt
	 * @param taintedLocalVariables
	 * @param curMethodName
	 * @param curClassName
	 */
	public void updateTaintedList(Stmt stmt,
			List<EntitySourceInf> taintedLocalVariables, String curMethodName,
			String curClassName, Stack<InvokMethodInfo> invokestack, String Tag) {
		// TODO Auto-generated method stub

		if (Tag.equals("Add")) {
			if (stmt.toString().contains("boolean equals(java.lang.Object)")) return;
			DefinitionStmt ds = (DefinitionStmt) stmt;
			Value lv = ds.getLeftOp();
			if(containSourceAugmentsinLeft(stmt,taintedLocalVariables)) 
				return;
			if(lv instanceof StaticFieldRef||lv instanceof JInstanceFieldRef) {
				ClassVarsStore classvarstore = BuildCallGraph.classvarstore;
				Value rv = null;
				boolean check=false;
				for (ValueBox rvBox : ds.getUseBoxes()) {
					rv = rvBox.getValue();
					if(check) break;
					for (int i = 0; i < taintedLocalVariables.size(); i++) {
						EntitySourceInf tmpsource = taintedLocalVariables.get(i);
						check = tmpsource.getSource().toString().equals(rv.toString())
								&& tmpsource.getClassName().equals(curClassName);
						if(check){
							if(lv instanceof StaticFieldRef){//static
								StaticFieldRef staticfrlv=(StaticFieldRef)lv;
								SootField lvsf = staticfrlv.getField();
								SootClass lvsc=lvsf.getDeclaringClass();
								taintedEntityClassVarible tclassvar = new taintedEntityClassVarible
								(lvsc, lvsf,stmt, tmpsource);
								EntitySourceInf classsource = new EntitySourceInf(lv, stmt,tmpsource, curMethodName, curClassName);
								classvarstore.publicStaticVars.add(tclassvar);
								taintedLocalVariables.add(classsource);
								classsource.print();
							}
							else if(lv instanceof JInstanceFieldRef) {//nonstatic
								JInstanceFieldRef jifrlv = (JInstanceFieldRef) lv;
								SootField lvsf =jifrlv.getField();
								SootClass lvsc =lvsf.getDeclaringClass();
								EntitySourceInf classsource = new EntitySourceInf(lv, stmt,tmpsource, curMethodName, curClassName);
								taintedEntityClassVarible tclassvar = new taintedEntityClassVarible(lvsc, lvsf, stmt, classsource);
								if (lv.toString().contains("$r0")){//jason
									thisPointer = ThisPointer.getThisPointer(classvarstore, thisPointer);
									ArrayList<taintedEntityClassVarible> classsvarsinfolist=classvarstore.testClassVars.get(thisPointer);
									if(classsvarsinfolist==null)
										classsvarsinfolist = new ArrayList<taintedEntityClassVarible>();
									classsvarsinfolist.add(tclassvar);
									classvarstore.testClassVars.put(thisPointer, classsvarsinfolist);
								}
								else {
									thisPointer = ThisPointer.getThisPointer(classvarstore, thisPointer);
									ArrayList<taintedEntityClassVarible> classsvarsinfolist=classvarstore.testClassVars.get(thisPointer);
									if(classsvarsinfolist==null)
										classsvarsinfolist = new ArrayList<taintedEntityClassVarible>();
									classsvarsinfolist.add(tclassvar);
									classvarstore.testClassVars.put(thisPointer, classsvarsinfolist);
								}
								taintedLocalVariables.add(classsource);
								classsource.print();
								BackwardAliasAnalysis(lv, stmt, invokestack,classsource);							
							}
							break;
						}
					}
				}			
			}
			//update the local tainted list;
			else{
				Value rv = null;
				boolean check = false;
				for (ValueBox rvBox : ds.getUseBoxes()) {
					rv = rvBox.getValue();
					if(check) break;
					for (int i = 0; i < taintedLocalVariables.size(); i++) {
						EntitySourceInf tmpsource = taintedLocalVariables.get(i);					
						check = tmpsource.getSource().toString().equals(rv.toString())
								&& tmpsource.getClassName().equals(curClassName)
								&& tmpsource.getMethodName().equals(curMethodName);
						if (check) {
							lv = ds.getLeftOp();
							// ***********start*************
							if (!rv.equivTo(lv)) {
								for(EntitySourceInf tmp:taintedLocalVariables){
									if(tmp.getSource().equals(lv))
										return;
								}
								EntitySourceInf sourceinf = new EntitySourceInf(lv,stmt, taintedLocalVariables.get(i),curMethodName, curClassName);
								taintedLocalVariables.add(sourceinf);
								sourceinf.print();
								if (lv instanceof JInstanceFieldRef) {// if the left one is object.field
									BackwardAliasAnalysis(lv, stmt, invokestack,sourceinf);
								} else if (!rv.getType().toString().equals("java.lang.string")) {
									// BackwardAliasAnalysis(lv,stmt,invokestack,sourceinf);
								}
							}
						}
					}
				}
			}
		} else if (Tag.equals("Delete")) {
			DefinitionStmt ds = (DefinitionStmt) stmt;
			Value lv = null;
			List<ValueBox> lvBoxs = ds.getDefBoxes();
			for (ValueBox lb : lvBoxs) {
				lv = lb.getValue();
				if (lv != null) {
					for (int i = 0; i < taintedLocalVariables.size(); i++)
						if (taintedLocalVariables.get(i).getSource().equivTo(lv)) {
							System.err.println("DELETE SOURCE"+ taintedLocalVariables.get(i).getSource().toString());
							taintedLocalVariables.remove(i);
						}
				}
			}
		}
	}

	/**
	 * This function is to implement backwardAliasAnalysi to build a Alias tree
	 * @param v
	 * @param stmt
	 * @param invokestack
	 * @param tmpsource
	 */
	private void BackwardAliasAnalysis(Value v, Stmt stmt,
			Stack<InvokMethodInfo> invokestack, EntitySourceInf tmpsource) {
		// TODO Auto-generated method stub
		JInstanceFieldRef objectfield = (JInstanceFieldRef) v;
		Value target = objectfield.getBase();

		invokestack.add(new InvokMethodInfo(this.getEntityclass(), this.getEntitymethod(), stmt));//It's the startpoint to every method to start backwardAliasAnalysis
		Stack<InvokMethodInfo> tmpstack = new Stack<InvokMethodInfo>();
		int LastAugmentAliasindex = -1;
		InvokMethodInfo pretopMethod = null;
		InvokMethodInfo topMethod;
		AliasElement root = null;
		List<AliasMap> maplist = new ArrayList<AliasMap>();

		while (!invokestack.isEmpty()) {

			topMethod = invokestack.pop();
			SootMethod sc = topMethod.getEmethod().sootmethod;
			UnitGraph g = topMethod.getEmethod().getCfgUnit();
			PatchingChain<Unit> lu = g.getBody().getUnits();
			Unit u1 = topMethod.getUnit();
			Stmt s1 = (Stmt) u1;//u1 is the startpoint stmt of method to backward.
			if (s1 instanceof JInvokeStmt) {//That's means it has come back to the top method.
				JInvokeStmt jinvokstmt = (JInvokeStmt) s1;
				List<Value> Arguments = jinvokstmt.getInvokeExpr().getArgs();
				SootMethod invokeMethod = jinvokstmt.getInvokeExpr().getMethod();
				Value tmpParameter = new ParameterRef(invokeMethod.getParameterType(LastAugmentAliasindex),LastAugmentAliasindex);
				Value Augment = Arguments.get(LastAugmentAliasindex);
				AliasElement left = new AliasElement(tmpParameter,pretopMethod.getEmethod(), pretopMethod.getEclass());
				AliasElement right = new AliasElement(Augment,topMethod.getEmethod(), topMethod.getEclass());
				AliasMap map = new AliasMap(left, right);
				maplist.add(map);
				target = Augment;
			}
			Unit u2 = null;
			boolean hasParameterAlias = false;
			while (true) {
				u2 = lu.getPredOf(u1);
				Stmt s2 = (Stmt) u2;
				EntityMethod curMethod = topMethod.getEmethod();
				EntityClass curClass = topMethod.getEclass();
				if (s2 instanceof JAssignStmt) {// a = b
					JAssignStmt assignstmt = (JAssignStmt) s2;
					Value lv = assignstmt.getLeftOp();
					Value rv = assignstmt.getRightOp();
					if (lv.equals(target) && rv.getUseBoxes().size() == 0) {
						root = new AliasElement(target, curMethod, curClass);
						break;
					} 
					else if (rv.equals(target)&& !(lv instanceof JInstanceFieldRef)) {// Alias = target
						AliasElement left = new AliasElement(lv, curMethod,curClass);
						AliasElement right = new AliasElement(rv, curMethod,curClass);
						AliasMap map = new AliasMap(left, right);
						maplist.add(map);
					}
					else if (lv.equals(target)&& rv instanceof JInstanceFieldRef) {// target = nextTarget.field
						AliasElement left = new AliasElement(lv, curMethod,curClass);
						AliasElement right = new AliasElement(rv, curMethod,curClass);
						AliasMap map = new AliasMap(left, right);
						maplist.add(map);
						JInstanceFieldRef rinf = (JInstanceFieldRef) rv;
						target = rinf.getBase();
					}
					else if (!s2.containsInvokeExpr()) {
						AliasElement left = new AliasElement(lv, curMethod,curClass);
						AliasElement right = new AliasElement(rv, curMethod,curClass);
						AliasMap map = new AliasMap(left, right);
						maplist.add(map);
					}

				} 
				else if (s2 instanceof JIdentityStmt) { //object = paramter0;
					JIdentityStmt identitystmt = (JIdentityStmt) s2;
					Value lv = identitystmt.getLeftOp();
					if (lv.equals(target)) {
						Value rv = identitystmt.getRightOp();
						Value tmpParameter;
						for (int i = 0; i < sc.getParameterCount(); i++) {
							tmpParameter = new ParameterRef(sc.getParameterType(i), i);
							if (tmpParameter.equivTo(rv)) {
								hasParameterAlias = true;
								LastAugmentAliasindex = i;
								AliasElement left = new AliasElement(lv,curMethod, curClass);
								AliasElement right = new AliasElement(rv,curMethod, curClass);
								AliasMap map = new AliasMap(left, right);
								maplist.add(map);
							}
						}
					}
				}
				if (u2 == null)
					break;
				u1 = u2;
			}
			tmpstack.push(topMethod);
			pretopMethod = topMethod;
			if (!hasParameterAlias)
				break;
		}
		while (!tmpstack.isEmpty()) {
			InvokMethodInfo tmpTop = tmpstack.pop();
			invokestack.add(tmpTop);
		}
		invokestack.pop();//back to the original state of InvokeStack
		//building a AliasTree according to the AliasMap and root node
		if(maplist.size()>0){
			AliasTree alias = new AliasTree(root, maplist, tmpsource,this.getEntityclass(), this.getEntitymethod());
			BuildCallGraph.aliasTreeList.add(alias);
		}
	}

	public boolean containLocalSourceAugmentsinRight(Stmt stmt,List<EntitySourceInf> taintedVariables) {
		DefinitionStmt ds = (DefinitionStmt) stmt;
		Value rv = null;
		List<ValueBox> rvBoxs = ds.getUseBoxes();
		for (ValueBox rb : rvBoxs) {
			rv = rb.getValue();
			if (rv != null) {
				for (int i = 0; i < taintedVariables.size(); i++) {
					EntitySourceInf temp = taintedVariables.get(i);
					Value tempV = temp.getSource();
					if (tempV.toString().equals(rv.toString()))
						return true;
				}
			}

		}
		return false;
	}
	public void checkClassSourceAugmentsinRight(Stmt stmt,
			List<taintedEntityClassVarible> taintedClassVars,List<EntitySourceInf> taintedLocalVars, 
			String curMethodName,String curClassName) {
		DefinitionStmt ds = (DefinitionStmt) stmt;
		Value lv = ds.getLeftOp();
		Value rv = null;
		boolean checked=false;
		List<ValueBox> rvBoxs = ds.getUseBoxes();
		for (ValueBox rb : rvBoxs) {
			rv = rb.getValue();
			if(checked) break;
			if (rv != null&&!(rv instanceof Local)) {
				for (int i = 0; i < taintedClassVars.size(); i++) {
					taintedEntityClassVarible temp = taintedClassVars.get(i);
					if(rv instanceof StaticFieldRef) {
						StaticFieldRef rvsfr =(StaticFieldRef)rv;
						if(rvsfr.getField().equals(temp.sf)){
							EntitySourceInf classsource = new EntitySourceInf(rv, stmt,temp.pre, curMethodName, curClassName);
							EntitySourceInf localsource = new EntitySourceInf(lv, stmt,classsource, curMethodName, curClassName);
							classsource.print();
							localsource.print();
							taintedLocalVars.add(classsource);
							taintedLocalVars.add(localsource);
							checked = true;
							break;
						}
					}
					else if(rv instanceof JInstanceFieldRef) {
						JInstanceFieldRef rvjifr = (JInstanceFieldRef)rv;
						if(rvjifr.getField().equals(temp.sf)){
							EntitySourceInf classsource = new EntitySourceInf(rv, stmt,temp.pre, curMethodName, curClassName);
							EntitySourceInf localsource = new EntitySourceInf(lv, stmt,classsource, curMethodName, curClassName);
							classsource.print();
							localsource.print();
							taintedLocalVars.add(classsource);
							taintedLocalVars.add(localsource);
							checked = true;
							break;
						}
					}
				}
			}
		}

	}
	public boolean containSourceAugmentsinLeft(Stmt stmt,
			List<EntitySourceInf> taintedVariables) {
		DefinitionStmt ds = (DefinitionStmt) stmt;
		Value lv = null;
		List<ValueBox> lvBoxs = ds.getDefBoxes();
		for (ValueBox lb : lvBoxs) {
			lv = lb.getValue();
			if (lv != null) {
				for (int i = 0; i < taintedVariables.size(); i++) {
					EntitySourceInf temp = taintedVariables.get(i);
					Value tempV = temp.getSource();
					if (tempV.toString().equals(lv.toString()))
						return true;
				}
			}

		}
		return false;
	}
	
	public EntitySourceInf checkJReturnStmt(Stmt stmt,
			List<EntitySourceInf> taintedVariables) {
		if (stmt instanceof JReturnVoidStmt) {
			return null;
		}
		JReturnStmt rstmt = (JReturnStmt) stmt;
		Value returnValue = rstmt.getOp();
		for (EntitySourceInf source : taintedVariables) {
			if (source.getSource().equals(returnValue)) {
				return source;
			}
		}
		return null;
	}

	public void CheckTaintedWrapper(UnitGraph g, Stmt stmt,
			List<EntitySourceInf> taintedVariables, EntityMethod curMethod, EntityClass curClass) {
		String curMethodName = curMethod.getMethodName();
		String curClassName = curClass.getclassName();
		FlowSet fsb = null;
		Iterator it1 = null;
		FlowSet fsa = null;
		Iterator it2 = null;
		try {
			FlowAnalysisProblem fa = new FlowAnalysisProblem(g);
			fsb = (FlowSet) fa.getFlowBefore(stmt);
			it1 = fsb.iterator();
			fsa = (FlowSet) fa.getFlowAfter(stmt);
			it2 = fsa.iterator();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return;
		}
		List<Value> listV = new ArrayList<Value>();
		for (ValueBox vb : stmt.getUseAndDefBoxes()) {
			listV.add(vb.getValue());
		}
		Value rv = null;
		Value lv = null;
		int BeforeSize = fsb.size();
		int AfterSize = fsa.size();
		List<Value> rvList = new ArrayList<Value>();
		List<Value> lvList = new ArrayList<Value>();
		for (int i = 0; i < fsb.size(); i++) {
			rv = (Value) it1.next();
			if (!rv.toString().equals("$r0")) {
				if (listV.contains(rv))
					rvList.add(rv);
			}

		}
		for (int i = 0; i < fsa.size(); i++) {
			lv = (Value) it2.next();
			if (!lv.toString().equals("$r0")) {
				if (listV.contains(lv))
					lvList.add(lv);
			}

		}
		BeforeSize = rvList.size();
		AfterSize = lvList.size();
		if ((BeforeSize == AfterSize) && BeforeSize != 0) {
			for (int i = rvList.size(); i > 0; i--) {
				rv = rvList.get(i - 1);
				lv = lvList.get(i - 1);
				if (!rv.equivTo(lv)) {
					System.out.println(lv.toString() + " = " + rv.toString());
					updateTaintedListWrapper(stmt, lv, rv, taintedVariables,
							curMethod, curClass, g);
				}
			}
		}
		if ((BeforeSize > AfterSize) && BeforeSize > 1 && AfterSize > 0) {
			for (int i = 0; i < AfterSize; i++) {
				rv = rvList.get(i);
				lv = lvList.get(i);
				if (rv.equivTo(lv)) {
					continue;
				}
			}
			int j = rvList.indexOf(rv);
			rv = rvList.get(j + 1);
			assert (rv != null);
			System.out.println(lv.toString() + " = " + rv.toString());
			updateTaintedListWrapper(stmt, lv, rv, taintedVariables, curMethod,
					curClass, g);

		}
	}
	public void updateTaintedListWrapper(Stmt stmt, Value lv, Value rv,
			List<EntitySourceInf> taintedVariables, EntityMethod curMethod, EntityClass curClass, UnitGraph g) {
		String curMethodName = curMethod.getMethodName();
		String curClassName = curClass.getclassName();
		assert (rv != null);
		for (int i = 0; i < taintedVariables.size(); i++) {
			if (taintedVariables.get(i).getSource().equivTo(rv)) {
				boolean loop = false;
				for (int j = 0; j < taintedVariables.size(); j++) {
					if (taintedVariables.get(j).getSource().equivTo(lv)) {
						loop = true;
					}
				}//prevent the repeating
				if (!loop) {
					EntitySourceInf sourceinf = new EntitySourceInf(lv, stmt,taintedVariables.get(i), curMethodName, curClassName);
					sourceinf.print();
					taintedVariables.add(sourceinf);
					if(sourceinf.getSource().getType().toString().equals("android.content.Intent")) {
						updateTaintedIntent(g,stmt,sourceinf,curMethod,curClass);
					}
				}
			}
		}
		
	}
	private void updateTaintedIntent(UnitGraph g, Stmt stmt,EntitySourceInf sourceinf, EntityMethod curMethod, EntityClass curClass) {
		// TODO Auto-generated method stub
		try{
		Value v = sourceinf.getSource();
		String methodname = curMethod.getMethodName();
		String classname  = curClass.getclassName();
		ComponentInvokeGraphNode cNode =curClass.getComponentInvokeGraph();
		Set<ComponentInvokeGraphNodeIntentPair> set=cNode.childrenNodes;
		for(ComponentInvokeGraphNodeIntentPair pair:set) {
			ComponentInvokeGraphNode Node = pair.getComponentInvokeTreeNode();
			IntentValue intentValue = pair.getIntent();
			if(intentValue.v.equals(v)&&intentValue.methodname.equals(methodname)&&intentValue.classname.equals(classname)){
				String type = sourceinf.getPre().getSource().getType().toString();
				TaintedIntent taintedintent = new TaintedIntent(curClass, Node.eclass, sourceinf, stmt,type);
				for(TaintedIntent element:Node.eclass.taintedIntent){//jason
					if(element.getTaintedsource().equalTo(taintedintent.getTaintedsource())){
						return;
					}
				}
				Node.eclass.taintedIntent.add(taintedintent);
			}
		}
		}
		catch(Exception e){
			
		}
	}
	public EntityPolyMethod findPolyMethod(SootClass declaredClass,
			String declaringClassName, Stmt stmt, List<EntityClass> curClasslist) {
		EntityPolyMethod epm = null;
		if (declaredClass.hasSuperclass()) {
			String superClass = declaredClass.getSuperclass().toString();
			if (ProcessCfg.isContain(superClass, curClasslist)) {
				if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr ii = (InstanceInvokeExpr) stmt.getInvokeExpr();
					Value base = ii.getBase();
					epm = new EntityPolyMethod(base, superClass,
							declaringClassName);
				}
			}
		}
		return epm;
	}

	public String changeToRealClass(Stmt stmt,
			List<EntityPolyMethod> polyMethodList, String declaringClassName,
			SootMethod declaringMethod, List<EntityClass> curClasslist) {
		if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr ii = (InstanceInvokeExpr) stmt.getInvokeExpr();
			Value base = ii.getBase();
			for (EntityPolyMethod epm : polyMethodList) {
				if (epm.getBase().equivTo(base)
						&& epm.getSuperClass().toString()
								.equals(declaringClassName)) {
					// see if the subclass has this method
					if (ProcessCfg.isMethodContain(declaringMethod,
							epm.getSubClass(), curClasslist)) {
						declaringClassName = epm.getSubClass();
					}

				}
			}
		}
		return declaringClassName;
	}



	/**
	 * The is method is to check whether the value exists the Alias
	 * 
	 * @param stmt
	 * @param taintedVariables
	 * @param curMethodName
	 * @param curClassName
	 * @return
	 */
	public boolean containSourceAliasObject(Stmt stmt,
			List<EntitySourceInf> taintedVariables, EntityMethod curMethodName,
			EntityClass curClassName) {
		// List<AliasTree> aliaslist=BuildCallGraph.AliasList;
		if (stmt instanceof JAssignStmt) {
			JAssignStmt assignment = (JAssignStmt) stmt;
			Value rv = assignment.getRightOp();
			if (rv instanceof JInstanceFieldRef) {
				checkContainAliasValue(rv, assignment, taintedVariables,curMethodName, curClassName);
			} else if (rv instanceof JimpleLocal) {
				checkContainAliasValue(rv, assignment, taintedVariables,curMethodName, curClassName);
			}
		}
		return false;
	}

	/**
	 * The is method is to implement Alias analysis
	 * @param instanceref
	 * @param stmt
	 * @param taintedVariables
	 * @param curMethod
	 * @param curClass
	 * @return
	 **/
	public void checkContainAliasValue(Value rv, JAssignStmt stmt,
			List<EntitySourceInf> taintedVariables, EntityMethod curMethod,
			EntityClass curClass) {

		if (rv instanceof JInstanceFieldRef) {
			AliasElement target;
			Type targetType;
			Type basetargetType;
			AliasElement basetarget;
			Value lv = stmt.getLeftOp();
			JInstanceFieldRef rinf = (JInstanceFieldRef) rv;

			ArrayList<AliasTree> tmpTreeList = BuildCallGraph.aliasTreeList;
			if (tmpTreeList.size() == 0)
				return;
			target = new AliasElement(rv, curMethod, curClass);
			targetType = target.v.getType();
			basetarget = new AliasElement(rinf.getBase(), curMethod, curClass);
			basetargetType = basetarget.v.getType();

			for (AliasTree tree : tmpTreeList) {
				AliasTreeNode targetnode = tree.searchTree(targetType, target);
				AliasTreeNode basetargetnode = tree.searchTree(basetargetType,basetarget);
				if (targetnode != null && basetargetnode == null)
					continue;
				if (targetnode != null) {
					if (targetnode.deep == tree.depth) {
						EntitySourceInf source1 = new EntitySourceInf(rv, stmt,
								tree.Source, curMethod.getMethodName(),
								curClass.getclassName());
						EntitySourceInf source2 = new EntitySourceInf(lv, stmt,
								source1, curMethod.getMethodName(),
								curClass.getclassName());
						taintedVariables.add(source1);
						taintedVariables.add(source2);
					} else
						tree.updateAliasTree(targetnode, lv, curMethod,
								curClass);

				} 
				else if (basetargetnode != null) {
					if (basetargetnode.deep == tree.depth - 1) {
						if (targetType.equals(tree.Source.getSource().getType())) {
							EntitySourceInf source1 = new EntitySourceInf(rv,
									stmt, tree.Source,
									curMethod.getMethodName(),
									curClass.getclassName());
							EntitySourceInf source2 = new EntitySourceInf(lv,
									stmt, source1, curMethod.getMethodName(),
									curClass.getclassName());
							source1.print();
							source2.print();
							taintedVariables.add(source1);
							taintedVariables.add(source2);
						}
					} else {
						tree.updateAliasTree(basetargetnode, lv, curMethod,curClass);
						tree.updateAliasTree(basetargetnode, rv, curMethod,curClass);
					}

				}
			}
		} else {
			AliasElement target;
			Type targetType;
			Value lv = stmt.getLeftOp();
			ArrayList<AliasTree> tmpTreeList = BuildCallGraph.aliasTreeList;
			if (tmpTreeList.size() == 0)
				return;
			target = new AliasElement(rv, curMethod, curClass);
			targetType = target.v.getType();
			for (AliasTree tree : tmpTreeList) {
				AliasTreeNode targetnode = tree.searchTree(targetType, target);
				if (targetnode != null) {
					if (targetnode.deep == tree.depth) {
						EntitySourceInf source1 = new EntitySourceInf(rv, stmt,
								tree.Source, curMethod.getMethodName(),
								curClass.getclassName());
						EntitySourceInf source2 = new EntitySourceInf(lv, stmt,
								source1, curMethod.getMethodName(),
								curClass.getclassName());
						taintedVariables.add(source1);
						taintedVariables.add(source2);
					} else
						tree.updateAliasTree(targetnode, lv, curMethod,curClass);
				}
			}
		}
	}

	public void generateReturnVariables(Stmt stmt,
			List<EntitySourceInf> taintedVariables,
			List<EntitySourceInf> returnVars, EntityMethod curMethod,
			EntityClass curClass) {

		Type methodRet = curMethod.sootmethod.getReturnType();
		String curClassName = curClass.getclassName();
		String curMethodName = curMethod.getMethodName();
		if (methodRet.toString().equals("void")) {
			if (taintedVariables.size() > 0) {
				returnVars.addAll(taintedVariables);
			}
		} else {
			JReturnStmt rstmt = (JReturnStmt) stmt;
			Value v = rstmt.getOp();
			int length = taintedVariables.size();
			for (int i = 0; i < length; i++) {
				EntitySourceInf tmpsource = taintedVariables.get(i);
				if (tmpsource.getSource().toString().equals(v.toString())
						&& tmpsource.getClassName().equals(curClassName)
						&& tmpsource.getMethodName().equals(curMethodName)) {
					EntitySourceInf reSource = new EntitySourceInf(v, stmt,tmpsource, curMethodName, curClassName);
					taintedVariables.add(reSource);
				}
			}
			if (taintedVariables.size() > 0) {
				returnVars.addAll(taintedVariables);
			}
		}
	}
	/**
	 * The EntitySource is original one;
	 * @param stmt
	 * @param u
	 * @param taintedVariables
	 * @param curMethodName
	 * @param curClassName
	 */
	public void addOriginalSource(Stmt stmt, Unit u,
			List<EntitySourceInf> taintedVariables, String curMethodName,
			String curClassName) {
		if (u instanceof DefinitionStmt) {
			System.err.println("Source found: " + u);
			DefinitionStmt ds = (DefinitionStmt) stmt;
			Value value = ds.getLeftOp();
			EntitySourceInf sourceinf = new EntitySourceInf(value, stmt, null,curMethodName, curClassName);
			taintedVariables.add(sourceinf);
		}
	}

	public void updateTaintedList(Stmt stmt, Value lv, Value rv,
			List<EntitySourceInf> taintedLocalVars, EntityMethod curMethod,
			EntityClass curClass, UnitGraph g) {
		// TODO Auto-generated method stub
		updateTaintedListWrapper(stmt, lv, rv, taintedLocalVars, curMethod, curClass, g);//jason
	}

	public Value getNextVThis(Stmt stmt) {
		// TODO Auto-generated method stub
		List<ValueBox> valueBoxs= stmt.getInvokeExpr().getUseBoxes();
		Value vThis = valueBoxs.get(valueBoxs.size()-1).getValue();
		return vThis;
	}

	public boolean isFragmentRelatedStmt(Unit u, UnitGraph g,EntityClass eclss) {
		// TODO Auto-generated method stub
		Stmt stmt = (Stmt)u;
		String invokeString =stmt.getInvokeExpr().getMethod().toString();
		if(invokeString.equals("<android.app.FragmentTransaction: android.app.FragmentTransaction add(int,android.app.Fragment)>")){
			InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();// InvokeStament
			SmartLocalDefs smd = new SmartLocalDefs(g, new SimpleLiveLocals(g));//Jason;
			for (int i = 0; i < iinv.getArgCount(); i++) {
				Value arg = iinv.getArg(i);
				Type argType = iinv.getArg(i).getType(); //Get the Augments type
				Type paramType = iinv.getMethod().getParameterType(i);// Get the Parameter Type
				if (paramType instanceof RefType&& argType instanceof RefType) {
					if (paramType.toString().equals("android.app.Fragment")) {//
						if (arg instanceof Local) // 
							for (Unit def : smd.getDefsOfAt((Local) arg, u)) {// get the  the definition
								assert def instanceof DefinitionStmt;
								Type tp = ((DefinitionStmt) def).getRightOp().getType();
								eclss.fragmentManagerList.add(tp.toString());
							}
					}
				}
			}
			return true;
		}
		return false;
	}

	public void isRegisterGlobalCallback(Unit u, UnitGraph g,
			EntityClass curClass, List<EntityClass> classlist) {
		// TODO Auto-generated method stub
		Stmt stmt = (Stmt) u;
		String invokeString =stmt.getInvokeExpr().getMethod().toString();
		if(invokeString.contains("void registerActivityLifecycleCallbacks(android.app.Application$ActivityLifecycleCallbacks)")
			||invokeString.contains("void registerComponentCallbacks(android.content.ComponentCallbacks)"))
		{
			SmartLocalDefs smd = new SmartLocalDefs(g, new SimpleLiveLocals(g));
			InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();// InvokeStament
			for (int i = 0; i < iinv.getArgCount(); i++) {
				Value arg = iinv.getArg(i);
				Type argType = iinv.getArg(i).getType(); //Get the Augments type
				Type paramType = iinv.getMethod().getParameterType(i);// Get the Parameter Type
				if (paramType instanceof RefType&& argType instanceof RefType) {
					if (paramType.toString().equals("android.app.Application$ActivityLifecycleCallbacks")) {//
						if (arg instanceof Local){
							for (Unit def : smd.getDefsOfAt((Local) arg, u)) {// get the  the definition
								assert def instanceof DefinitionStmt;
								Type tp = ((DefinitionStmt) def).getRightOp().getType();
								if (tp instanceof RefType) {
									SootClass callbackClass = ((RefType) tp).getSootClass();
									if (callbackClass.isInterface())
										for (SootClass impl : Scene.v().getActiveHierarchy().getImplementersOf(callbackClass))
											for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(impl))
												BuildCallGraph.applicationCalss.globalRegisterClass = c.getName();
									else
										for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(callbackClass))
											BuildCallGraph.applicationCalss.globalRegisterClass = c.getName();
								}
							}
						}
							
					}
					else if(paramType.toString().equals("android.content.ComponentCallbacks")){
						if (arg instanceof Local){
							for (Unit def : smd.getDefsOfAt((Local) arg, u)) {// get the  the definition
								assert def instanceof DefinitionStmt;
								Type tp = ((DefinitionStmt) def).getRightOp().getType();
								System.out.println(tp.toString());
								if (tp instanceof RefType) {
									SootClass callbackClass = ((RefType) tp).getSootClass();
									if (callbackClass.isInterface())
										for(EntityClass ec:classlist){
											if(ec.sootclass.implementsInterface(callbackClass.toString())){
												BuildCallGraph.applicationCalss.ComponentCallback = ec.getclassName();
											}
										}

								}
							}
						}
					}
				}
			}
		}
	}


}
