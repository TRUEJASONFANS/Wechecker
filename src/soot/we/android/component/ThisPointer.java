package soot.we.android.component;

import java.util.Set;

import soot.Value;
import soot.we.android.callGraph.ClassVarsStore;

public class ThisPointer {
     public Value vThis;
     public String classname;
     public String methodname;
     /**
      * A this point is an instance of object. the className,methodName tell us when the object was created.
      * @param v
      * @param className
      * @param methodName
      */
	public ThisPointer(Value v, String className, String methodName) {
		vThis = v;
		classname = className;
		methodname = methodName;
	}
    public boolean equalTo(ThisPointer p) {
    	if(vThis.equals(p.vThis)&&classname.equals(p.classname)&&methodname.equals(p.methodname))
    		return true;
    	return false;
    }
    public static ThisPointer getThisPointer(ClassVarsStore classvarstore,ThisPointer tmp){
    	Set<ThisPointer> pointers=classvarstore.testClassVars.keySet();
		for(ThisPointer p:pointers) {
			if(p.equalTo(tmp)) {
				tmp = p;
				break;
			}
		}
		return tmp;
    }
}
