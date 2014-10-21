package soot.we.android.callGraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.UnitGraph;
import soot.we.android.MainThread;
import soot.we.android.component.EntityClass;
import soot.we.android.component.EntityMethod;
import soot.we.android.component.EntityPolyMethod;
import soot.we.android.component.EntitySourceInf;
import soot.we.android.component.ThisPointer;
import soot.we.android.controlflowgraph.ControlFlowGraph;
import soot.we.android.controlflowgraph.ControlFlowGraphEdge;
import soot.we.android.controlflowgraph.ControlFlowGraphNode;
import soot.we.android.log.AlarmLog;

public class ProcessCfg {
	/**
	 * This is class to simulate running a single method in program.
	 * praraTaintedMap: indicated the tainted parameter in the method
	 * invokeStack : It's a stack to store the method has been invoked.The head
	 * of stack is the last method which was invoked
	 * @param easyTaintWrapper
	 * @param g
	 * @param curMethod
	 * @param curClass
	 * @param classlist
	 * @param OldTaintedVariablelist
	 * @param paraTaintMap
	 * @param invokeStack
	 * @param thisPointer 
	 * @return
	 */
	public List<EntitySourceInf> TraverseCfg(EasyTaintWrapper easyTaintWrapper,
			UnitGraph g, EntityMethod curMethod, EntityClass curClass,
			List<EntityClass> classlist,
			List<EntitySourceInf> OldTaintedVariablelist,
			Map<Integer, EntitySourceInf> paraTaintMap,
			Stack<InvokMethodInfo> invokeStack, ThisPointer thisPointer) {
		if(BuildCallGraph.processCfgTimes++>BuildCallGraph.processCfgMaximumTimes)
			return OldTaintedVariablelist;
		System.out.println ("Checking Class: " + curClass.getclassName());
		System.out.println("Checking Method: " + curMethod.getMethodName());
		AlarmLog.writeToControlFlowGrpah("Checking Class: "+curClass.getclassName());
		AlarmLog.writeToControlFlowGrpah("Checking Method: "+curMethod.getMethodName());
		
		String curClassName = curClass.getclassName();

		SourceSinkFinder ssFinder = new SourceSinkFinder();
		ssFinder.setEntityclass(curClass);
		ssFinder.setEntitymethod(curMethod);
		ssFinder.setUnitgraph(g);
		ssFinder.thisPointer = thisPointer;
		
		// judge the class whether is innerClass or not
		if (curClass.isInnerClass()) {
			curClass.setOutclasses(curClassName, classlist);
		}
		// rebuild the local tainted list
		List<EntitySourceInf> taintedLocalVars = ssFinder.initializeTaintedLocalVarsList(OldTaintedVariablelist,paraTaintMap,
				curMethod, curClass, invokeStack);
		List<taintedEntityClassVarible> taintedClassVars = ssFinder.initializeTaintedlistClassVarsList(curClass,null);

		List<EntitySourceInf> returnVariables = new ArrayList<EntitySourceInf>();
		
		List<EntityPolyMethod> polyMethodList = new ArrayList<EntityPolyMethod>();
		ControlFlowGraph cfg = reConstructCfg(g);
		List<List<Block>> sequecens = cfg.getSequences();
		List<ClassVarsStore> classVarsStoreList = new ArrayList<ClassVarsStore>();
		for (int i = 0; i < sequecens.size(); i++) {
			List<Block>  order = sequecens.get(i);
			List<Unit> units = new ArrayList<Unit>();
			List<EntitySourceInf> tmptaintedLocalVars = new ArrayList<EntitySourceInf>();
			
			tmptaintedLocalVars.addAll(taintedLocalVars);
			ClassVarsStore classvarstore = new ClassVarsStore(BuildCallGraph.classvarstore);
			AlarmLog.writeToControlFlowGrpah("proccessing a order in method:"+curMethod.getMethodName()+" in class"
			+curClass.getclassName());
			String orderString="Block order:";
			for(Block b: order){
				orderString = orderString+" "+"b["+b.getIndexInMethod()+"]";
			}
			AlarmLog.writeToControlFlowGrpah(orderString);
			for(Block b: order) {
				System.out.println("processing block:  " + b.getIndexInMethod()+" in order :"+ i);
				AlarmLog.writeToControlFlowGrpah("block:  " + b.getIndexInMethod()+" in order :"+ i);
				Iterator<Unit> iterator = b.iterator();
				while(iterator.hasNext()){
					units.add(iterator.next());
				}
				returnVariables=processBlock(ssFinder,units,taintedLocalVars,taintedClassVars,polyMethodList,returnVariables,
						curMethod,curClass,classlist,invokeStack,easyTaintWrapper,g,thisPointer);
				units.clear();
			}
			classVarsStoreList.add(new ClassVarsStore(BuildCallGraph.classvarstore));
			taintedLocalVars.clear();
			taintedLocalVars.addAll(tmptaintedLocalVars);
			BuildCallGraph.classvarstore.assign(classvarstore);
			if(BuildCallGraph.processCfgTimes++>BuildCallGraph.processCfgMaximumTimes)
				return OldTaintedVariablelist;
		}
		for(ClassVarsStore c:classVarsStoreList){
			BuildCallGraph.classvarstore.merge(c);
		}
		classVarsStoreList.clear();
		return returnVariables;
	}

	public static boolean isMethodContain(SootMethod method, String className,
			List<EntityClass> classlist) {
		EntityClass ec = locateDeclaringClass(className, classlist);
		if (ec != null) {
			EntityMethod em = locateDeclaringMethod(method, ec);
			if (em != null) {
				return true;
			} else {
				return false;
			}
		}
		return false;

	}

	public static EntityClass locateDeclaringClass(String declaringClassName,
			List<EntityClass> classlist) {
		for (EntityClass ec : classlist)
			if (ec.getclassName().equals(declaringClassName))
				return ec;

		return null;
	}

	public static boolean isContain(String className,
			List<EntityClass> eCompClassList) {
		for (EntityClass eCompClass : eCompClassList)
			if (eCompClass.getclassName().equals(className))
				return true;
		return false;
	}

	private static EntityMethod locateDeclaringMethod(
			SootMethod declaringMethod, EntityClass declaringClass) {
		if (declaringClass.getMethodList() != null) {
			for (EntityMethod em : declaringClass.getMethodList())

				if (em.getMethodName().equals(declaringMethod.getName())) {
					List<Type> list1 = em.getCfgUnit().getBody().getMethod().getParameterTypes();
					List<Type> list2 = declaringMethod.getParameterTypes();

					if ((list1.equals(list2))|| (list1.isEmpty() && list2.isEmpty())) {
						return em;
					}

				}
		}
		return null;
	}
	
	public ControlFlowGraph reConstructCfg(UnitGraph g) {

		BlockGraph blockGraph = new ExceptionalBlockGraph(g.getBody());
		List<Block> blockLs = blockGraph.getBlocks();
		ControlFlowGraph cfg = new ControlFlowGraph();
		AlarmLog.writeToControlFlowGrpah("This method contains :" + blockLs.size()+" blocks");
		for (Block b : blockLs) {
			if(b.getPreds().size()==0){
				cfg.getEntrys().add(b);
			}
			if(b.getSuccs().size()==0){
				cfg.getExits().add(b);
			}
			List<ControlFlowGraphEdge> blockEdges= new ArrayList<ControlFlowGraphEdge>();
			List<Block> succsBlocks = b.getSuccs();	
			if(succsBlocks!=null) {
				for(Block sb: succsBlocks) {
				 ControlFlowGraphEdge edeg = new ControlFlowGraphEdge(b,sb,false);
				 blockEdges.add(edeg);
				}
			}
			ControlFlowGraphNode node = new ControlFlowGraphNode(b,blockEdges);
			cfg.getMap().put(b, node);
			System.out.println(b.toString());
			AlarmLog.writeToControlFlowGrpah(b.toString());
		}
		cfg.generatePath();
		return cfg;
	}
	public List<EntitySourceInf> processBlock(SourceSinkFinder ssFinder, List<Unit> unitList, List<EntitySourceInf> 
	taintedLocalVars, List<taintedEntityClassVarible> taintedClassVars, List<EntityPolyMethod> polyMethodList,
	List<EntitySourceInf> returnVariables, EntityMethod curMethod, EntityClass curClass, 
	List<EntityClass> classlist, Stack<InvokMethodInfo> invokeStack, EasyTaintWrapper easyTaintWrapper,
	UnitGraph g, ThisPointer thisPointer){

		String curMethodName = curMethod.getMethodName();
		String curClassName = curClass.getclassName();
		for (int i = 0; i < unitList.size(); i++) {
			Unit u = unitList.get(i);
			Stmt stmt = (Stmt) u;
			
			if (ssFinder.isDefinitionStmt(u)) {
				if (taintedLocalVars.size() > 0) {
					if (ssFinder.containLocalSourceAugmentsinRight(stmt,taintedLocalVars))
						ssFinder.updateTaintedList(stmt, taintedLocalVars,curMethodName, curClassName, invokeStack, "Add");
					else if (ssFinder.containSourceAugmentsinLeft(stmt,taintedLocalVars))
						ssFinder.updateTaintedList(stmt, taintedLocalVars,curMethodName, curClassName, invokeStack,"Delete");					
				}
				else if (BuildCallGraph.aliasTreeList.size()>0){
					ssFinder.containSourceAliasObject(stmt,taintedLocalVars, curMethod, curClass);
				}
				if (taintedClassVars.size() > 0) {
					ssFinder.checkClassSourceAugmentsinRight(stmt,taintedClassVars, taintedLocalVars, curMethodName,curClassName);
				}
				if (stmt.toString().contains("android.view.View findViewById")) {
					Iterator<ValueBox> it = stmt.getUseBoxes().iterator();
					while (it.hasNext()) {
						Value value = it.next().getValue();
						System.out.println("findViewByID: " + value);
						try {
							int layoutId = Integer.parseInt(value.toString());
							if (MainThread.sensitivelayouts.contains(layoutId)) {
								DefinitionStmt ds = (DefinitionStmt) stmt;
								Value lv = ds.getLeftOp();
								EntitySourceInf es = new EntitySourceInf(lv,stmt, null, curMethodName, curClassName);
								taintedLocalVars.add(es);
								es.print();
							}
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				}
			}
			if (ssFinder.isIntraProceduralStmt(u)) { // IfStmt,GotoStmtTable,switchStmt,LookupSwitchStmt
				// **********start****************
				if (u instanceof IfStmt) {

				}
				if ((Stmt) u instanceof GotoStmt) {
				}
				// **************end****************
			}
			if (ssFinder.isInterProceduralStmt(stmt)) { // contain InvokeStmt

				if (easyTaintWrapper.hasWrappedMethodsForClass(stmt, true,
						false, false)) {
					// System.out.println("taintedWrapper: " + u.toString());
					ssFinder.CheckTaintedWrapper(g, stmt, taintedLocalVars,curMethod, curClass);

				}
				if (ssFinder.getSourceInfo(stmt) != null) {
					ssFinder.addOriginalSource(stmt, u, taintedLocalVars,curMethodName, curClassName);
				}
				if (ssFinder.isSink(stmt,curClass)) {
					// check whether it has a path or not
					ssFinder.checkAStmt(stmt, taintedLocalVars, curMethod,curClass);
				}
				if (ssFinder.isIntentRelatedStmt(stmt)) {
					ssFinder.updateComponentInvokeTree(stmt, curMethod,curClass, classlist);
				}
				if(ssFinder.isFragmentRelatedStmt(u,g, curClass)){
					
				}
				if(curClass.getCompType().equals("android.app.Application")){//check if the instance of Application
					ssFinder.isRegisterGlobalCallback(u,g, curClass,classlist);
				}
				// If the Stmt is defined by developer
				// 1.the parameters only contain the base type:int,string,float.
				// 2.the parameters contain object defined by developer;
				if (ssFinder.isDefinebyDeveloper(stmt, curClassName, classlist)) {
					
					String declaringClassName = stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
					SootMethod declaringMethod = stmt.getInvokeExpr().getMethod();
					ThisPointer nextThisPointer;
					if(stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
						 nextThisPointer = thisPointer;
					}
					else{
						Value nextVThis = ssFinder.getNextVThis(stmt);
					 nextThisPointer = new ThisPointer(nextVThis, curClassName,curMethodName);
					}
					// *******************polymorphic *************************
					if (declaringMethod.getName().equals("<init>")) {

						SootClass declaredClass = stmt.getInvokeExpr().getMethod().getDeclaringClass();
						EntityPolyMethod epm = ssFinder.findPolyMethod(declaredClass, declaringClassName, stmt,classlist);
						if (epm != null) {
							polyMethodList.add(epm);
						}
					} else {
						declaringClassName = ssFinder.changeToRealClass(stmt,
								polyMethodList, declaringClassName,
								declaringMethod, classlist);
					}
					// ******************polymorphic end*******************
					EntityClass declaringClass = locateDeclaringClass(declaringClassName, classlist);
					EntityMethod em = locateDeclaringMethod(declaringMethod,declaringClass);
					if (em == null) continue;
					Map<Integer, EntitySourceInf> AugTaintMap = ssFinder.getTaitedfParameters(stmt, taintedLocalVars);
					Type subMethodRet = em.sootmethod.getReturnType();
					// Save the invoke Method
					invokeStack.add(new InvokMethodInfo(curClass, curMethod, u));
					if (subMethodRet.toString().equals("void")) {
						 TraverseCfg(easyTaintWrapper, em.getCfgUnit(), em,declaringClass, classlist, taintedLocalVars,
								AugTaintMap, invokeStack,nextThisPointer);
					} else {
						Value lv = null;
						Value rv;
						if (stmt instanceof DefinitionStmt) {
							lv = ((DefinitionStmt) stmt).getLeftOp();
						}
						List<EntitySourceInf> subMethodreturnSourceVars = TraverseCfg(
								easyTaintWrapper, em.getCfgUnit(), em,
								declaringClass, classlist, taintedLocalVars,
								AugTaintMap, invokeStack,nextThisPointer);
						if (subMethodreturnSourceVars != null&&subMethodreturnSourceVars.size()>0) {
								rv = subMethodreturnSourceVars.get(subMethodreturnSourceVars.size() - 1).getSource();
								if (rv != null && lv != null) 
								ssFinder.updateTaintedList(stmt, lv, rv,taintedLocalVars, curMethod,curClass, g);						
						}
					}
					invokeStack.pop();
					taintedClassVars = ssFinder.initializeTaintedlistClassVarsList(curClass,taintedClassVars);
				}

			}
			// deal with the returnStmt
			if (ssFinder.isReturnStmt(stmt)) {
				if (curMethodName.equals("<init>")) {
					if (curClass.isInnerClass()) {
						EntityMethod em = curClass.getstaticinitMethod();
						if (em != null)
						TraverseCfg(easyTaintWrapper, em.getCfgUnit(), em,curClass, classlist, 
						taintedLocalVars,null, invokeStack,thisPointer);
					}
				}
				EntitySourceInf rsource=ssFinder.checkJReturnStmt(stmt, taintedLocalVars);
				if (rsource!= null) {
						for(EntitySourceInf s:returnVariables){
							if(s.equalTo(rsource))
								continue;
							else {
								returnVariables.add(rsource);
								break;
							}
						}
				}
			}
			System.out.println("      " + u.toString());
			AlarmLog.writeToControlFlowGrpah(u.toString());
		}
		return returnVariables;
	}
}