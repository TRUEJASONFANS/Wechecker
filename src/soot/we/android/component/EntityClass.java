package soot.we.android.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import soot.SootClass;
import soot.we.android.IntentResolution.ComponentInvokeGraphNode;
import soot.we.android.IntentResolution.TaintedIntent;
import soot.we.android.callGraph.GetCallback;

public class EntityClass {
	private String className;
	public List<EntityMethod> methodList;
	private List<EntityClassVariable> classVariable;
	private boolean isEntryPointClass;
	private boolean containClassvars;
    private ComponentInvokeGraphNode componentInvokeGraphNode;
	private GetCallback getCallBack;
	public  List<List<EntityMethod>> callbackSeq;
    public  SootClass sootclass;
    public  List<EntityClass> outclasses;
    public  Set<TaintedIntent> taintedIntent;
    public ArrayList<String> fragmentManagerList;
	public EntityClass() {
		super();
	}

	public EntityClass(String className, List<EntityMethod> methodList,
			List<EntityClassVariable> classVariable) {
		super();
		this.className = className;
		this.methodList = methodList;
		this.classVariable = classVariable;
		this.setComponentInvokeGraph(null);
	}

	public List<EntityClassVariable> getClassVariable() {
		return classVariable;
	}

	public void setClassVariable(List<EntityClassVariable> classVariable) {
		this.classVariable = classVariable;
	}

	public String getclassName() {
		return className;
	}

	public void setclassName(String className) {
		this.className = className;
	}

	public List<EntityMethod> getMethodList() {
		return methodList;
	}

	public void setMethodList(List<EntityMethod> methodList) {
		this.methodList = methodList;
	}

	public boolean isEntryPointClass() {
		return isEntryPointClass;
	}

	public void setEntryPointClass(boolean entry) {
		this.isEntryPointClass = entry;
	}
    public boolean hasSelfSuperClass(EntityClass eclass) {
    	SootClass tmpsuperclass = eclass.sootclass.getSuperclass();
    	String packageName = tmpsuperclass.getPackageName();
    	if(packageName.startsWith("java"))
    		return false;
    	if(packageName.startsWith("android"))
    		return false;
    	return true;
    }
    /**
     * merge the superclass's methods to the class
     * @param eclass
     * @param classList
     */
	public void mergeSuperClassMethod(EntityClass eclass,List<EntityClass> classList) {
		SootClass tmpsuperclass = eclass.sootclass.getSuperclass();
		String superclassName = tmpsuperclass.getName();
		for(EntityClass ec:classList) {
			if(superclassName.equals(ec.getclassName())) {
				List<EntityMethod> list = ec.methodList;
				List<EntityMethod> morglist = eclass.methodList;
				for(int i = 0;i < list.size();i++) {
					boolean add = true;
					EntityMethod superM = list.get(i); 
					if(superM.getMethodName().equals(("<init>"))) continue;
					for(EntityMethod subM: morglist) {
						if(superM.sootmethod.equals(subM.sootmethod)) {
							add=false;continue;
							}							
					}
					if(add!=false&&superM!=null)
						morglist.add(superM);				
				}
				break;
			}
		}
		
	}

	public EntityMethod getstaticinitMethod() {
		// TODO Auto-generated method stub
		List<EntityMethod> list = this.methodList;
		for(EntityMethod em:list) {
			if(em.getMethodName().endsWith("<clinit>")) {
				return em;
			}
		}
		return null;
	}
	/**
	 * This function is to judge whether is innerclass or not
	 * @return
	 */
	public boolean isInnerClass(){
		if(this.className.contains("$")){
			if(!this.className.startsWith("java")&&!this.className.startsWith("android")){
				return true;
			}
		}
		return false;
	} 
	/**
	 * This function is to return the Outerclasses of the class(if the one is a inner class)
	 * @param classname
	 * @param classList
	 * @return
	 */
	public void setOutclasses(String classname,List<EntityClass> classList) {
		List<EntityClass> outclasses = new ArrayList<EntityClass>();
		String[] classnames = classname.split("\\$");
		String outclassname="";
		for(int i=0;i<classnames.length-1;i++) {
			if(outclassname.length()==0)
				outclassname = outclassname+classnames[i];
			else
				outclassname = outclassname+"$"+classnames[i];
			for(EntityClass eclass:classList) {
				if(eclass.className.equals(outclassname)) {
					outclasses.add(eclass);
					break;
				}
			}
		}
		this.outclasses = outclasses;	
	}
    public String getCompType(){
    	SootClass sc = null;
    	String packageName="";
    	while(true) {
			if (sc == null)
				sc = this.sootclass.getSuperclass();
			else{
				if(sc.getType().toString().equals("java.lang.Object"))
					return "";
				sc = sc.getSuperclass();
				if(sc==null) return "";
			}
    		packageName = sc.getPackageName();
        	if(packageName.startsWith("android"))
        		return sc.toString();
    	}
    }

	public boolean isContainClassvars() {
		return containClassvars;
	}

	public void setContainClassvars(boolean containClassvars) {
		this.containClassvars = containClassvars;
	}

	public ComponentInvokeGraphNode getComponentInvokeGraph() {
		return componentInvokeGraphNode;
	}

	public void setComponentInvokeGraph(ComponentInvokeGraphNode componentInvokeTree) {
		this.componentInvokeGraphNode = componentInvokeTree;
	}

	public GetCallback getGetCallBack() {
		return getCallBack;
	}

	public void setGetCallBack(GetCallback getCallBack) {
		this.getCallBack = getCallBack;
	}


}
