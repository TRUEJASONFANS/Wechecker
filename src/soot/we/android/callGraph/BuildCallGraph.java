package soot.we.android.callGraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.SootMethod;
import soot.Value;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.UnitGraph;
import soot.we.android.IntentResolution.ComponentInvokeGraph;
import soot.we.android.IntentResolution.ComponentInvokeGraphNode;
import soot.we.android.IntentResolution.ComponentInvokeGraphNodeIntentPair;
import soot.we.android.component.EntityApplicationClass;
import soot.we.android.component.EntityClass;
import soot.we.android.component.EntityMethod;
import soot.we.android.component.EntitySourceInf;
import soot.we.android.component.LeakingPath;
import soot.we.android.component.ThisPointer;
import soot.we.android.log.AlarmLog;
import soot.we.android.test.Test;
public class BuildCallGraph {
	private ProcessCfg processCfg;
	public List<EntityClass> classList;
	public EasyTaintWrapper easyTaintWrapper;
	
	public static Set<String> androidCallbacks = new HashSet<String>();
	public static ClassVarsStore classvarstore;
	public static ClassVarsStore appInitialclassvarstore;
	public static ClassVarsStore componentInStackclassvarstore;
    public static ArrayList<AliasTree> aliasTreeList;
    public static ComponentInvokeGraph componentInvokeTrees;
    public static ArrayList<String> visitedEntityComponents;
    public static ArrayList<LeakingPath> leakingPaths = new ArrayList<LeakingPath>();;
    public static int processCfgTimes=0;
    public static EntityApplicationClass applicationCalss;
    public final static int processCfgMaximumTimes=30;
    public final static int maximumHopTimes = 5;
    public final EntityClass contentProviderClass;
    public Stack<EntityClass> componentStack;
    
	public  enum  CallbackPolicy {
		ArbitrarySequence,TwoRoundChecking
	};
	
	public CallbackPolicy callbackPolicy;
	public void setEasyTaintWrapper(EasyTaintWrapper easyTaintWrapper) {
		this.easyTaintWrapper = easyTaintWrapper;
	}

	public BuildCallGraph(List<EntityClass> classList,int policy,EntityApplicationClass appCalss,EntityClass contentProviderClass) {
		processCfg = new ProcessCfg();
		applicationCalss = appCalss;
		this.contentProviderClass = contentProviderClass;
		this.classList = classList;
		switch(policy){
			case 0:callbackPolicy = CallbackPolicy.ArbitrarySequence;break;
			case 1:callbackPolicy = CallbackPolicy.TwoRoundChecking;break;
		}
	}

	public void run() throws IOException {
		
		
		classvarstore = new ClassVarsStore();
		aliasTreeList = new ArrayList<AliasTree> ();
		componentInStackclassvarstore = new ClassVarsStore();
		componentStack = new Stack<EntityClass>();
		
		if(contentProviderClass!=null) {
			androidContentProviderClassSpecific();
		}
		if(applicationCalss!=null) {
			androidApplicationClassSpecific();
		}
		androidCallbacks = GetCallback.loadAndroidCallbacks();
		componentInvokeTrees = new ComponentInvokeGraph();
		
		System.out.println("Component size: "+Test.app.getComponents().size());
		
		for (EntityClass eclass : classList) {
			if (eclass.isEntryPointClass()) {
				visitedEntityComponents = new ArrayList<String>();
				runCurrentClass(eclass,0,true);
			}
		}
		
		
	}
	

	public void runCurrentClass(EntityClass eclass,int hop, boolean enableHop) throws IOException{
		if(hop>maximumHopTimes) //The maximum hop we concern
			return;
		visitedEntityComponents.add(eclass.getclassName());
		ComponentInvokeGraphNode componentInvoketreeNode = new ComponentInvokeGraphNode(eclass);
		eclass.setComponentInvokeGraph(componentInvoketreeNode);
		if(eclass.hasSelfSuperClass(eclass)) {
			eclass.mergeSuperClassMethod(eclass,classList);
		}
		String compType = eclass.getCompType();
		eclass.setGetCallBack(new GetCallback());
		Map<EntityClass, Set<EntityMethod>> comCallbackMap = null;
		Map<EntityClass, Set<EntityMethod>> ComponentCallBacks = null;
		Collection<Set<EntityMethod>> connection = null;
		Set<EntityMethod> methodSet = null;
		// get callback method from current class if is instance of Activity
		if (compType.equals("android.app.Activity")||compType.equals("android.app.ListActivity")
				||compType.equals("android.app.FragmentActivity")
				||compType.equals("android.support.v4.app.FragmentActivity")){
			
			comCallbackMap = eclass.getGetCallBack().getCallBackFromLifeCycleMethod(eclass);
			ComponentCallBacks=eclass.getGetCallBack().getCallBackSet();		 
			connection = ComponentCallBacks.values();
			Iterator<Set<EntityMethod>> iterator = connection.iterator();
			// store all callback methods in methodSet
			methodSet = new HashSet<>();
			while (iterator.hasNext()) {
				methodSet.addAll(iterator.next());
			}
		}

		AlarmLog.writeToControlFlowGrpah("Checking Component: "+eclass.getclassName()+"Type:"+compType);
		ArrayList<String[]> LifeCycle = new ArrayList<String[]>();
		System.out.println("CHOOSELIFECYCLE: " + compType);
		
		if (compType.equals("android.app.Activity")||compType.equals("android.app.ListActivity")
				||compType.equals("android.app.FragmentActivity")
				||compType.equals("android.support.v4.app.FragmentActivity")) {
			AndroidLifeCycleCallGraph ALCG = new AndroidLifeCycleCallGraph();
			LifeCycle = ALCG.getActivityAndroidLifeCycleCallGraph();
			eclass.fragmentManagerList = new ArrayList<String>();
		} else if (compType.equals("android.content.BroadcastReceiver")) {
			AndroidLifeCycleCallGraph ALCG = new AndroidLifeCycleCallGraph();
			LifeCycle = ALCG.getBroadcastAndroidLifeCycleCallGraph();
		} else if (compType.equals("android.app.Service")) {
			AndroidLifeCycleCallGraph ALCG = new AndroidLifeCycleCallGraph();
			LifeCycle = ALCG.getServiceAndroidLifeCycleCallGraph();
		}
		CallbackPolicy callbackPolicy = this.callbackPolicy;
		classvarstore.clear();
		for (String[] Sequence : LifeCycle) {		
			List<EntitySourceInf> taintedLocalVariables = new ArrayList<EntitySourceInf>();
			restoreIntanceComponenClassvars(componentInStackclassvarstore,classvarstore,componentStack);
			if(appInitialclassvarstore!=null)
				classvarstore.assign(appInitialclassvarstore);
			for (int i = 0; i < Sequence.length; i++) {
				String mName = Sequence[i];
				// running lifecycle method
				Stack<InvokMethodInfo> invokestack = new Stack<InvokMethodInfo>();
				aliasTreeList.clear();
				BuildCallGraph.processCfgTimes=0;
				if (!mName.equals("ActivityRunning")) {
					// Traverse the CFG of method
					for (EntityMethod eMethod : eclass.getMethodList()) {
						// start form lifecycle method ----onCreate
						if(mName.equals("onActivityResult")&&eclass.getclassName().equals("SetupQueActivity")){
							System.out.println();
						}
						if (eMethod.getMethodName().equals(mName)) {
							SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
							if (sm == null)
								continue;
							if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
								continue;
							Value vThis = new JimpleLocal(eclass.getCompType(),eclass.sootclass.getType());
							ThisPointer thisPointer = new ThisPointer(vThis, eclass.getclassName(),"");
							UnitGraph uGraph = eMethod.getCfgUnit();
							processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, eclass, classList,
									taintedLocalVariables, null,invokestack,thisPointer);
							break;
						}
					}
				} 
				else {// running callback method
					switch(callbackPolicy){
					case ArbitrarySequence:
						 if (comCallbackMap != null) 
							// get all combination of callback methods
							eclass.callbackSeq= eclass.getGetCallBack().getArbitraryCallbackSequence();
						 if(eclass.callbackSeq==null) continue;
						 for (List<EntityMethod> se : eclass.callbackSeq){
							for (EntityMethod eMethod : se) {
								
								SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
								if (sm == null)
									continue;
								if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
									continue;
								UnitGraph uGraph = eMethod.getCfgUnit();
								EntityClass callbackClass = null;
								for(EntityClass callbackclass:classList){
									if(callbackclass.sootclass.
									  equals(eMethod.sootmethod.getDeclaringClass())){
										callbackClass = callbackclass;
										break;
									}
								}
								Value vThis = new JimpleLocal("callback",callbackClass.sootclass.getType());
								ThisPointer thisPointer = new ThisPointer(vThis, callbackClass.getclassName(),"");
								BuildCallGraph.processCfgTimes=0;
								processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, callbackClass, 
										classList,taintedLocalVariables, null,invokestack,thisPointer);
							}
							break;
						 }
					break;
					case TwoRoundChecking:
						
						 eclass.getGetCallBack().getCallBackFromCallbackMethod(methodSet,eclass);
						 ClassVarsStore classstoreBefore = new ClassVarsStore(classvarstore);
						 HashMap<EntityMethod,ClassVarsStore> tmpclassstoreMaps = new  HashMap<EntityMethod,ClassVarsStore>();
						 for(EntityMethod eMethod:methodSet) {//first round
							UnitGraph uGraph = eMethod.getCfgUnit();
							EntityClass callbackClass = null;
								for(EntityClass callbackclass:classList){
									if(callbackclass.sootclass.
									  equals(eMethod.sootmethod.getDeclaringClass())){
										callbackClass = callbackclass;
										break;
									}
								}
							 Value vThis = new JimpleLocal("callback",callbackClass.sootclass.getType());
							 ThisPointer thisPointer = new ThisPointer(vThis, callbackClass.getclassName(),"");
							 BuildCallGraph.processCfgTimes=0;
							 processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, callbackClass, 
										classList,taintedLocalVariables, null,invokestack,thisPointer);
							 ClassVarsStore classstoreAfterOnecallbackMethod = new ClassVarsStore(classvarstore);
							 tmpclassstoreMaps.put(eMethod, classstoreAfterOnecallbackMethod);
							 classvarstore.assign(classstoreBefore);
							
						 }
						 for(EntityMethod eMethodTest:methodSet) {//Second Round
							 classvarstore.assign(classstoreBefore);
							 for(EntityMethod eMethodOthers:methodSet){
								 if(!CheckWhetherIsSameMethod(eMethodOthers,eMethodTest)) {
									 ClassVarsStore classstoreAfterOnecallbackMethod = tmpclassstoreMaps.get(eMethodOthers);
									 classvarstore.merge(classstoreAfterOnecallbackMethod);
								 }
							 }
							UnitGraph uGraph = eMethodTest.getCfgUnit();
							EntityClass callbackClass = null;
							for(EntityClass callbackclass:classList){
								if(callbackclass.sootclass.
									 equals(eMethodTest.sootmethod.getDeclaringClass())){
									 callbackClass = callbackclass;
									 break;
									}
								}
							Value vThis = new JimpleLocal("callback",callbackClass.sootclass.getType());
							ThisPointer thisPointer = new ThisPointer(vThis, callbackClass.getclassName(),"");
							processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethodTest, callbackClass, 
							classList,taintedLocalVariables, null,invokestack,thisPointer);
						 }
				        break;
					default:
						break;
					}
				}
			}
		}
		if(eclass.fragmentManagerList!=null&&eclass.fragmentManagerList.size()>0) {
			for(String fragment:eclass.fragmentManagerList)
				for(EntityClass ec:classList){
					if(ec.getclassName().equals(fragment)){
						androidFragmentSpecific(ec, hop);
					}
				}
			
		}
		if(enableHop)
			componentStack.push(eclass);
		saveIntanceComponenClassvars(componentInStackclassvarstore,classvarstore,componentStack);
		if(!enableHop)
			return;
		DFSRunChildrens(eclass,hop,enableHop);
	}
	private boolean CheckWhetherIsSameMethod(EntityMethod eMethodOthers,
			EntityMethod eMethodTest) {
		String eMethodOthersName = eMethodOthers.getMethodName();
		String eMethodTestName = eMethodTest.getMethodName();
		String eMethodOthersClassName =  eMethodOthers.sootmethod.getDeclaringClass().getName();
		String eMethodTestClassName =  eMethodTest.sootmethod.getDeclaringClass().getName();
		if(eMethodOthersName.equals(eMethodTestName)&&eMethodOthersClassName.equals(eMethodTestClassName))
			 return true;
		return false;
	}

	private void androidFragmentSpecific(EntityClass ec, int hop) {
		// TODO Auto-generated method stub
		AndroidLifeCycleCallGraph ALCG = new AndroidLifeCycleCallGraph();
		String[] sequence = ALCG.getFragmentAndroidLifeCycleCallGraph();
			for(String methodName:sequence){
				BuildCallGraph.processCfgTimes=0;
				for (EntityMethod eMethod : ec.getMethodList()) {
					if(methodName.equals(eMethod.getMethodName())){
						List<EntitySourceInf> taintedLocalVariables = new ArrayList<EntitySourceInf>();
						SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
						if (sm == null)
							continue;
						if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
							continue;
						Value vThis = new JimpleLocal(ec.getCompType(),ec.sootclass.getType());
						ThisPointer thisPointer = new ThisPointer(vThis, ec.getclassName(),"");
						UnitGraph uGraph = eMethod.getCfgUnit();
						Stack<InvokMethodInfo> invokestack = new Stack<InvokMethodInfo>();
						processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, ec, classList,
								taintedLocalVariables, null,invokestack,thisPointer);
					}
				}
			}
			
	}

	private void saveIntanceComponenClassvars(
			ClassVarsStore componentInStackclassvarstore,
			ClassVarsStore classvarstore, Stack<EntityClass> componentStack) {
		// TODO Auto-generated method stub
		componentInStackclassvarstore.publicStaticVars.addAll(classvarstore.publicStaticVars);
		
	}

	private void restoreIntanceComponenClassvars(
			ClassVarsStore componentInStackclassvarstore,
			ClassVarsStore classvarstore, Stack<EntityClass> componentStack) {
		// TODO Auto-generated method stub
		classvarstore.publicStaticVars.addAll(componentInStackclassvarstore.publicStaticVars);		
	}
	/**
	 * 
	 * @param eclass
	 * @param hop
	 * @param enableHop
	 * @throws IOException
	 */
	private void DFSRunChildrens(EntityClass eclass,int hop, boolean enableHop) throws IOException {
		Set<ComponentInvokeGraphNodeIntentPair> childrens = eclass.getComponentInvokeGraph().childrenNodes;
		if(childrens!=null) {
			Set<ComponentInvokeGraphNodeIntentPair> tmp = new HashSet<ComponentInvokeGraphNodeIntentPair>();
			tmp.addAll(childrens);
			childrens = null;
			for(ComponentInvokeGraphNodeIntentPair c:tmp) {
					ComponentInvokeGraphNode cNode=c.getComponentInvokeTreeNode();
					int num=0;
					if(cNode.eclass.taintedIntent==null&cNode.eclass.taintedIntent.size()==0) continue;//update
					for(int i=0;i<visitedEntityComponents.size();i++) {
						if(visitedEntityComponents.get(i).equals(cNode.eclass.getclassName())) {
							num++;
						}
					}
					if(num>1) //get rid of the recursive visited the in some components;
						return;
					else 
						runCurrentClass(cNode.eclass,hop+1,enableHop);
					c = null;
			}
			tmp = null;
		}
	}
	public void androidApplicationClassSpecific(){
		AndroidLifeCycleCallGraph ALCG = new AndroidLifeCycleCallGraph();
		String[] sequence = ALCG.getApplicationAndroidLifeCycleCallGraph();
			for(String methodName:sequence){
				BuildCallGraph.processCfgTimes=0;
				EntityClass applicationbaseCalss = applicationCalss.baseClass;
				for (EntityMethod eMethod : applicationbaseCalss.getMethodList()) {
					if(methodName.equals(eMethod.getMethodName())){
						List<EntitySourceInf> taintedLocalVariables = new ArrayList<EntitySourceInf>();
						SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
						if (sm == null)
							continue;
						if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
							continue;
						Value vThis = new JimpleLocal(applicationbaseCalss.getCompType(),applicationbaseCalss.sootclass.getType());
						ThisPointer thisPointer = new ThisPointer(vThis, applicationbaseCalss.getclassName(),"");
						UnitGraph uGraph = eMethod.getCfgUnit();
						Stack<InvokMethodInfo> invokestack = new Stack<InvokMethodInfo>();
						processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, applicationbaseCalss, classList,
								taintedLocalVariables, null,invokestack,thisPointer);
					}				
				}
			
		}
	
		if(!applicationCalss.globalRegisterClass.equals("")){
				String globalRegisterClass = applicationCalss.globalRegisterClass;
					for(EntityClass ec:classList){
						if(globalRegisterClass.equals(ec.getclassName())){
							 sequence = ALCG.getApplicationGlobleCallbackCallGraph();
								for(String methodName:sequence){
									BuildCallGraph.processCfgTimes=0;
									for (EntityMethod eMethod : ec.getMethodList()) {
										if(methodName.equals(eMethod.getMethodName())){
										List<EntitySourceInf> taintedLocalVariables = new ArrayList<EntitySourceInf>();
										SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
										if (sm == null)
											continue;
										if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
											continue;
										Value vThis = new JimpleLocal("GlobalCallback",ec.sootclass.getType());
										ThisPointer thisPointer = new ThisPointer(vThis, ec.getclassName(),"");
										UnitGraph uGraph = eMethod.getCfgUnit();
										Stack<InvokMethodInfo> invokestack = new Stack<InvokMethodInfo>();
										processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, ec, classList,
												taintedLocalVariables, null,invokestack,thisPointer);
									}
								}
							}
						}
					}
			}
		if(!applicationCalss.ComponentCallback.equals("")){
			String globalRegisterClass = applicationCalss.ComponentCallback;
			for(EntityClass ec:classList){
				if(globalRegisterClass.equals(ec.getclassName())){
					 sequence = ALCG.getApplicationComponentCallbackCallGraph();
						for(String methodName:sequence){
							BuildCallGraph.processCfgTimes=0;
							for (EntityMethod eMethod : ec.getMethodList()) {
								if(methodName.equals(eMethod.getMethodName())){
								List<EntitySourceInf> taintedLocalVariables = new ArrayList<EntitySourceInf>();
								SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
								if (sm == null)
									continue;
								if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
									continue;
								Value vThis = new JimpleLocal("GlobalCallback",ec.sootclass.getType());
								ThisPointer thisPointer = new ThisPointer(vThis, ec.getclassName(),"");
								UnitGraph uGraph = eMethod.getCfgUnit();
								Stack<InvokMethodInfo> invokestack = new Stack<InvokMethodInfo>();
								processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, ec, classList,
										taintedLocalVariables, null,invokestack,thisPointer);
							}
						}
					}
				}
			}
		}
		appInitialclassvarstore = new ClassVarsStore();
		appInitialclassvarstore.assign(classvarstore);
		ALCG = null;
	}
	private void androidContentProviderClassSpecific() {
		// TODO Auto-generated method stub
		AndroidLifeCycleCallGraph ALCG = new AndroidLifeCycleCallGraph();
		String[] sequence = ALCG.getContentProviderAndroidLifeCycleCallGraph();
		for(String methodName:sequence){
			BuildCallGraph.processCfgTimes=0;
			for (EntityMethod eMethod : contentProviderClass.getMethodList()) {
				if(methodName.equals(eMethod.getMethodName())){
					List<EntitySourceInf> taintedLocalVariables = new ArrayList<EntitySourceInf>();
					SootMethod sm = eMethod.getCfgUnit().getBody().getMethod();
					if (sm == null)
						continue;
					if (sm.isAbstract()|| sm.getDeclaringClass().isInterface()|| sm.isPrivate())
						continue;
					Value vThis = new JimpleLocal(contentProviderClass.getCompType(),contentProviderClass.sootclass.getType());
					ThisPointer thisPointer = new ThisPointer(vThis, contentProviderClass.getclassName(),"");
					UnitGraph uGraph = eMethod.getCfgUnit();
					Stack<InvokMethodInfo> invokestack = new Stack<InvokMethodInfo>();
					processCfg.TraverseCfg(easyTaintWrapper,uGraph, eMethod, contentProviderClass, classList,
							taintedLocalVariables, null,invokestack,thisPointer);
				}				
			}		
		}
	}
}