package soot.we.android.callGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.we.android.MainThread;
import soot.we.android.component.EntityClass;
import soot.we.android.component.EntityMethod;
import soot.we.android.resource.ARSCFileParser.AbstractResource;
import soot.we.android.resource.ARSCFileParser.StringResource;

/**
 * This is class for us to collect the callback methods from classlist.
 */
public class GetCallback {

	private static String AndroidCallbackFilePath = "AndroidCallbacks.txt";
	private Map<EntityClass, Set<EntityMethod>> ComponentCallBacks = new HashMap<EntityClass, Set<EntityMethod>>();
	// Callback sequences
	private List<List<EntityMethod>> lle = new ArrayList<List<EntityMethod>>();

	public Map<EntityClass, Set<EntityMethod>> getCallBackSet() {
		return ComponentCallBacks;
	}

	public void setCallBackSet(
			HashMap<EntityClass, Set<EntityMethod>> callBackSet) {
		ComponentCallBacks = callBackSet;
	}

	public Map<EntityClass, Set<EntityMethod>> getCallBackFromLifeCycleMethod(
			EntityClass eCompClass) throws IOException {

		Set<SootClass> callbackClasses = new HashSet<SootClass>();
		AndroidLifeCycleCallGraph aCL = new AndroidLifeCycleCallGraph();
		if (eCompClass.getMethodList() == null|| eCompClass.getMethodList().size() == 0)
			return null;
		for (EntityMethod eMethod : eCompClass.getMethodList()) {
			// ZTZT
			if (eMethod.getMethodName().equals("onCreate")) {
				for (Unit u : eMethod.getCfgUnit())
					if (u instanceof Stmt) {
						Stmt stmt = (Stmt) u;
						if (stmt.containsInvokeExpr()) {
							InvokeExpr inv = stmt.getInvokeExpr();
							if (inv.getMethod().getName().equals("setContentView")&& inv.getMethod().getDeclaringClass().getName().equals("android.app.Activity")) {
								for (Value val : inv.getArgs())
									if (val instanceof IntConstant) {
										IntConstant constVal = (IntConstant) val;
										System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
										System.out.println("layoutID: "+ constVal.value);
										AbstractResource resource = MainThread.resParser.findResource(constVal.value);
										if (resource instanceof StringResource) {
											StringResource strRes = (StringResource) resource;
											System.out.println(strRes.getValue());
											if (MainThread.layoutCallBack.containsKey(strRes.getValue())) {
												for (String lyoutMethod : MainThread.layoutCallBack.get(strRes.getValue())) {
													System.out.println("layoutCallback: "+ lyoutMethod);
													for (EntityMethod eMethodTemp : eCompClass.getMethodList()) {
														if (eMethodTemp.getMethodName().equals(lyoutMethod)) {
															if (this.ComponentCallBacks.containsKey(eCompClass)) {
																this.ComponentCallBacks.get(eCompClass).add(eMethodTemp);
															} else {
																Set<EntityMethod> methods = new HashSet<EntityMethod>();
																methods.add(eMethodTemp);
																this.ComponentCallBacks.put(eCompClass,methods);
															}
														}
													}
												}
											}
										}
										System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
									}
							}
						}
					}
			}
			// ZTZT--end
			if (aCL.isLifeCycle(eMethod.getMethodName())) { // if method is a
															// lifeCycleMethod
				System.out.println("LifeCycleInClass: "+ eCompClass.getclassName());
				System.out.println("LifeCycleMethodName : "+ eMethod.getMethodName());
				if (!eMethod.hasbeenCheckforCallback) {
					SearchInMethod(eMethod, callbackClasses);
					eMethod.hasbeenCheckforCallback = true;
				}
			} else {
				if (aCL.isSpecificCallBack(eMethod.getMethodName())) {
					if (this.ComponentCallBacks.containsKey(eCompClass)) {
						this.ComponentCallBacks.get(eCompClass).add(eMethod);
					} else {
						Set<EntityMethod> methods = new HashSet<EntityMethod>();
						methods.add(eMethod);
						this.ComponentCallBacks.put(eCompClass, methods);
					}
				}
			}
		}
		// for (SootClass callbackClass : callbackClasses) {
		// System.out.println("CallBackClass: " + callbackClass.getName());
		// }
		// Analyze all found callback classes
		for (SootClass callbackClass : callbackClasses) {
			analyzeClass(callbackClass, eCompClass);
		}
		Set<EntityClass> set = this.ComponentCallBacks.keySet();
		Iterator<EntityClass> it = set.iterator();
		while (it.hasNext()) {
			EntityClass temp = it.next();
			System.out.println("CallBackClass: " + temp.getclassName());
			Iterator<EntityMethod> ite = this.ComponentCallBacks.get(temp)
					.iterator();
			while (ite.hasNext()) {
				EntityMethod temp2 = ite.next();
				System.out.println("CallBackMethod: " + temp2.getMethodName());
			}
		}
		if (ComponentCallBacks.size() > 0) {
			return ComponentCallBacks;
		} else {
			return null;
		}
	}

	public static Set<String> loadAndroidCallbacks() throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(AndroidCallbackFilePath));
			String line;
			while ((line = rdr.readLine()) != null)
				if (!line.isEmpty())
					androidCallbacks.add(line);
		} finally {
			if (rdr != null)
				rdr.close();
		}
		return androidCallbacks;
	}

	private void SearchInMethod(EntityMethod eMethod,
			Set<SootClass> callbackClasses) {
		if (eMethod.sootmethod.getDeclaringClass().getName()
				.startsWith("android.")
				|| eMethod.sootmethod.getDeclaringClass().getName()
						.startsWith("java."))
			return;
		if (!eMethod.sootmethod.isConcrete())
			return;
		Set<String> androidCallbacks = BuildCallGraph.androidCallbacks;
		UnitGraph g = eMethod.getCfgUnit();
		SmartLocalDefs smd;
		try {
			smd = new SmartLocalDefs(g, new SimpleLiveLocals(g));// Jason
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		for (Unit u : g.getBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			// Callback registrations are always instance invoke expressions in
			// Jimple;
			if (stmt.containsInvokeExpr()&& stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();// InvokeStament
				for (int i = 0; i < iinv.getArgCount(); i++) {
					Value arg = iinv.getArg(i);
					Type argType = iinv.getArg(i).getType(); // Get the Augments type
					Type paramType = iinv.getMethod().getParameterType(i);// Get the Parameter Type
					if (paramType instanceof RefType&& argType instanceof RefType) {
						if (androidCallbacks.contains(((RefType) paramType).getSootClass().getName())) {//
							// We have a formal parameter type that corresponds
							// to one of the Android
							// callback interfaces. Look for definitions of the
							// parameter to estimate
							// the actual type.
							if (arg instanceof Local) //
								for (Unit def : smd.getDefsOfAt((Local) arg, u)) {// get the the definition
									assert def instanceof DefinitionStmt;
									Type tp = ((DefinitionStmt) def).getRightOp().getType();
									if (tp instanceof RefType) {
										SootClass callbackClass = ((RefType) tp).getSootClass();
										if (callbackClass.isInterface())
											for (SootClass impl : Scene.v().getActiveHierarchy().getImplementersOf(callbackClass))
												for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(impl))
													callbackClasses.add(c);
										else
											for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(callbackClass))
												callbackClasses.add(c);
									}
								}
						}
					}
				}
			}
		}
	}

	private void analyzeClass(SootClass sootClass, EntityClass eCompClass) {
		// Do not analyze system classes
		if (sootClass.getName().startsWith("android.")|| sootClass.getName().startsWith("java."))
			return;
		// Check for callback handlers implemented via interfaces
		analyzeClassInterfaceCallbacks(sootClass, sootClass, eCompClass);
	}

	private void analyzeClassInterfaceCallbacks(SootClass baseClass,
			SootClass sootClass, EntityClass lifecycleElement) {
		// We cannot create instances of abstract classes anyway, so there is no
		// reason to look for interface implementations
		if (!baseClass.isConcrete())
			return;

		// For a first take, we consider all classes in the android.* packages
		// to be part of the operating system
		if (baseClass.getName().startsWith("android."))
			return;

		// If we are a class, one of our superclasses might implement an Android
		// interface
		if (sootClass.hasSuperclass()) {
			// System.out.println("BaseClass: "+baseClass.getName()+", SupperClas: "+sootClass.getSuperclass().toString());
			analyzeClassInterfaceCallbacks(baseClass,sootClass.getSuperclass(), lifecycleElement);
		}
		// Do we implement one of the well-known interfaces?
		for (SootClass i : collectAllInterfaces(sootClass)) {
			if (BuildCallGraph.androidCallbacks.contains(i.getName())) { // Implements one of listener
				// AndroidCallback
				// interface
				for (SootMethod sm : i.getMethods()) {
					SootMethod tempmethod = getMethodFromHierarchyEx(baseClass,
							sm.getSubSignature());
					checkAndAddMethod(tempmethod, lifecycleElement);
				}
			}
		}

	}

	private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
		Set<SootClass> interfaces = new HashSet<SootClass>(
				sootClass.getInterfaces());
		for (SootClass i : sootClass.getInterfaces())
			interfaces.addAll(collectAllInterfaces(i));
		return interfaces;
	}

	private void checkAndAddMethod(SootMethod method, EntityClass baseClass) {
		SootClass dc = method.getDeclaringClass();
		EntityClass entitydeclaringclass = findEntityClass(dc);
		if (entitydeclaringclass == null)
			return;
		// System.out.println("Method : "+method.getName()+" ,declaringclass: "+entitydeclaringclass.getclassName());
		EntityMethod eMethod = findEntityMethodInEntityClass(method,
				entitydeclaringclass);

		// AndroidMethod am = new AndroidMethod(method);

		// Do not call system methods
		if (eMethod.getMethodName().startsWith("android.")
				|| eMethod.getMethodName().startsWith("java."))
			return;

		// Skip empty methods
		if (method.isConcrete() && isEmpty(eMethod.getCfgUnit().getBody()))
			return;

		boolean isNew;
		if (this.ComponentCallBacks.containsKey(entitydeclaringclass))
			isNew = this.ComponentCallBacks.get(entitydeclaringclass).add(
					eMethod);
		else {
			Set<EntityMethod> methods = new HashSet<EntityMethod>();
			isNew = methods.add(eMethod);
			this.ComponentCallBacks.put(entitydeclaringclass, methods);
		}
		//
		// if (isNew)
		// if (this.callbackWorklist.containsKey(baseClass.getName()))
		// this.callbackWorklist.get(baseClass.getName()).add(am);
		// else {
		// Set<AndroidMethod> methods = new HashSet<AndroidMethod>();
		// isNew = methods.add(am);
		// this.callbackWorklist.put(baseClass.getName(), methods);
		// }
	}

	private EntityClass findEntityClass(SootClass dc) {
		for (EntityClass temp : MainThread.classList) {
			if (temp.getclassName().equals(dc.getName()))
				return temp;
		}
		return null;
	}

	private EntityMethod findEntityMethodInEntityClass(SootMethod dcMethod,
			EntityClass dcClass) {
		for (EntityMethod temp : dcClass.getMethodList()) {
			if (temp.getMethodName().equals(dcMethod.getName())) {
				return temp;
			}
		}
		return null;
	}

	private SootMethod getMethodFromHierarchyEx(SootClass c,
			String methodSignature) {
		if (c.declaresMethod(methodSignature))
			return c.getMethod(methodSignature);
		if (c.hasSuperclass())
			return getMethodFromHierarchyEx(c.getSuperclass(), methodSignature);
		throw new RuntimeException("Could not find method");
	}

	private boolean isEmpty(Body activeBody) {
		for (Unit u : activeBody.getUnits())
			if (!(u instanceof IdentityStmt || u instanceof ReturnVoidStmt))
				return false;
		return true;
	}

	public List<List<EntityMethod>> getArbitraryCallbackSequence() {
		Collection<Set<EntityMethod>> connection = ComponentCallBacks.values();
		Iterator<Set<EntityMethod>> iterator = connection.iterator();
		// store all callback methods in methodSet
		Set<EntityMethod> methodSet = new HashSet<>();
		while (iterator.hasNext()) {
			methodSet.addAll(iterator.next());
		}
		List<EntityMethod> methodList = new ArrayList<EntityMethod>(methodSet);
		arrange(methodList, 0, methodList.size());
		return this.lle;
	}

	public static void swap(List<EntityMethod> listM, int i, int j) {
		EntityMethod temp = new EntityMethod(null, null);
		temp = listM.get(i);
		listM.set(i, listM.get(j));
		listM.set(j, temp);
	}

	public  void arrange(List<EntityMethod> listM, int st, int len) {

		List<EntityMethod> le = new ArrayList<EntityMethod>();
		if (st == len - 1) {
			for (int i = 0; i < len; i++) {
				le.add(listM.get(i));
			}
			lle.add(le);
		} else {
			for (int i = st; i < len; i++) {
				swap(listM, st, i);
				arrange(listM, st + 1, len);
				swap(listM, st, i);
			}
		}

	}

	public void getCallBackFromCallbackMethod(Set<EntityMethod> methodSet,
			EntityClass eCompClass) {
		Set<SootClass> callbackClasses = new HashSet<SootClass>();
		for (EntityMethod eMethod : methodSet) {
			if (!eMethod.hasbeenCheckforCallback) {
				SearchInMethod(eMethod, callbackClasses);
				eMethod.hasbeenCheckforCallback = true;
			}
		}
		for (SootClass callbackClass : callbackClasses) {
			analyzeClass(callbackClass, eCompClass);
		}
		Map<EntityClass, Set<EntityMethod>> ComponentCallBacks = eCompClass
				.getGetCallBack().getCallBackSet();
		Collection<Set<EntityMethod>> connection = ComponentCallBacks.values();
		Iterator<Set<EntityMethod>> iterator = connection.iterator();
		// store all callback methods in methodSet
		while (iterator.hasNext()) {
			methodSet.addAll(iterator.next());
		}
	}
}
