package soot.we.android.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class ProcessJimpleClass {
	/**
	 * CallbackMethod including the AndroidLifeCycleMethod and
	 */
	public void collectCallbackMethod(EntityClass tempCompClass) {

		collectCallbackMethodfromLayout();
		collectCallbackMethodfromComponentClass();

	}

	public void collectCallbackMethodfromComponentClass() {

	}

	public void collectCallbackMethodfromLayout() {

	}

	public List<String> createDummySequence(EntityClass tempCompClas) {
		return null;
	}

	public static UnitGraph getCfgFromMethod(Body body) {
		UnitGraph g = new ExceptionalUnitGraph(body);
		return g;
	}

	public void searchInvoke(Body body) {
		// **********find Static invoke***************
		Iterator<Unit> iter = body.getUnits().snapshotIterator();
		while (iter.hasNext()) {
			Stmt s = (Stmt) iter.next();
			if (s.containsInvokeExpr()) {
				String declaringClass = s.getInvokeExpr().getMethod()
						.getDeclaringClass().getName();
				String name = s.getInvokeExpr().getMethod().getName();

				System.out.println("declaringClass: " + declaringClass);
				System.out.println("name: " + name);

				if (declaringClass.equals("android.content.Intent")) {
					for (ValueBox ss : s.getUseBoxes()) {
						System.out.println("declaringClass's values : "
								+ ss.getValue().toString());
					}
				}
			}
		}
	}

	public static List<String> findAnonyInnerClass(String Outerclassname) {

		List<String> outClassList = new ArrayList<String>();
		Iterator<SootClass> iterator = Scene.v().getApplicationClasses()
				.iterator();
		while (iterator.hasNext()) {
			String classname = iterator.next().getName();
			if (classname.contains(Outerclassname)
					&& !classname.equals(Outerclassname)) {
				System.out.println("Anonyinnerclass :" + classname);
				outClassList.add(classname);
			}
		}
		return outClassList;
	}

	public static List<SootClass> findApplicationClass() {

		List<SootClass> applicationClass = new ArrayList<SootClass>();
		Iterator<SootClass> iterator = Scene.v().getApplicationClasses().iterator();
		while (iterator.hasNext()) {
			SootClass temp = iterator.next();
			String classname = temp.getName();
			if (!classname.startsWith("android")&&!classname.startsWith("java"))
				applicationClass.add(temp);
		}
		return applicationClass;
	}

}
