package soot.we.android.callGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.jimple.Stmt;


/**
 * A list of methods is passed which contains signatures of instance methods
 * that taint their base objects if they are called with a tainted parameter.
 * When a base object is tainted, all return values are tainted, too. For static
 * methods, only the return value is assumed to be tainted when the method is
 * called with a tainted parameter value.
 * 
 */
public class EasyTaintWrapper implements Cloneable {

	private final Map<String, Set<String>> classList;
	private final Map<String, Set<String>> excludeList;
	private final Map<String, Set<String>> killList;
	private final Set<String> includeList;



	private boolean aggressiveMode = false;
	private boolean alwaysModelEqualsHashCode = true;

	private enum MethodWrapType {
		CreateTaint, KillTaint, Exclude, NotRegistered
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList) {
		this(classList, new HashMap<String, Set<String>>(),
				new HashMap<String, Set<String>>(), new HashSet<String>());
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList,
			Map<String, Set<String>> excludeList) {
		this(classList, excludeList, new HashMap<String, Set<String>>(),
				new HashSet<String>());
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList,
			Map<String, Set<String>> excludeList,
			Map<String, Set<String>> killList) {
		this(classList, excludeList, killList, new HashSet<String>());
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList,
			Map<String, Set<String>> excludeList,
			Map<String, Set<String>> killList, Set<String> includeList) {
		this.classList = classList;
		this.excludeList = excludeList;
		this.killList = killList;
		this.includeList = includeList;
	}

	public EasyTaintWrapper(String f) throws IOException {
		this(new File(f));
	}

	public EasyTaintWrapper(File f) throws IOException {
		BufferedReader reader = null;
		try {
			FileReader freader = new FileReader(f);
			reader = new BufferedReader(freader);
			String line = reader.readLine();
			List<String> methodList = new LinkedList<String>();
			List<String> excludeList = new LinkedList<String>();
			List<String> killList = new LinkedList<String>();
			this.includeList = new HashSet<String>();
			while (line != null) {
				if (!line.isEmpty() && !line.startsWith("%"))
					if (line.startsWith("~"))
						excludeList.add(line.substring(1));
					else if (line.startsWith("-"))
						killList.add(line.substring(1));
					else if (line.startsWith("^"))
						includeList.add(line.substring(1));
					else
						methodList.add(line);
				line = reader.readLine();
			}
			this.classList = SootMethodRepresentationParser.v().parseClassNames(methodList, true);
			this.excludeList = SootMethodRepresentationParser.v().parseClassNames(excludeList, true);
			this.killList = SootMethodRepresentationParser.v().parseClassNames(killList, true);

			Set<Entry<String, Set<String>>> set1 = classList.entrySet();
			Iterator<Entry<String, Set<String>>> iterator1 = set1.iterator();

//			while (iterator1.hasNext()) {
//				Entry<String, Set<String>> entry1 = iterator1.next();
//				System.out.println("ClassList键是：" + entry1.getKey() + "值是："
//						+ entry1.getValue());
//			}
//
//			Set<Entry<String, Set<String>>> set2 = this.excludeList.entrySet();
//			Iterator<Entry<String, Set<String>>> iterator2 = set2.iterator();
//
//			while (iterator2.hasNext()) {
//				Entry<String, Set<String>> entry2 = iterator2.next();
//				System.out.println("excludeList键是：" + entry2.getKey() + "值是："
//						+ entry2.getValue());
//			}
//
//			Set<Entry<String, Set<String>>> set3 = this.killList.entrySet();
//			Iterator<Entry<String, Set<String>>> iterator3 = set3.iterator();
//
//			while (iterator3.hasNext()) {
//				Entry<String, Set<String>> entry3 = iterator3.next();
//				System.out.println("excludeList键是：" + entry3.getKey() + "值是："
//						+ entry3.getValue());
//			}
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	public EasyTaintWrapper(EasyTaintWrapper taintWrapper) {
		this(taintWrapper.classList, taintWrapper.excludeList,taintWrapper.killList, taintWrapper.includeList);
	}

	/**
	 * Checks whether at least one method in the given class is registered in
	 * the taint wrapper
	 * 
	 * @param parentClass
	 *            The class to check
	 * @param newTaints
	 *            Check the list for creating new taints
	 * @param killTaints
	 *            Check the list for killing taints
	 * @param excludeTaints
	 *            Check the list for excluding taints
	 * @return True if at least one method of the given class has been
	 *         registered with the taint wrapper, otherwise
	 */
	public boolean hasWrappedMethodsForClass(Stmt stmt, boolean newTaints,
			boolean killTaints, boolean excludeTaints) {
		SootClass parentClass = stmt.getInvokeExpr().getMethod().getDeclaringClass();
		if (newTaints && classList.containsKey(parentClass.getName()))
			return true;
		if (excludeTaints && excludeList.containsKey(parentClass.getName()))
			return true;
		if (killTaints && killList.containsKey(parentClass.getName()))
			return true;
		return false;
	}

	/**
	 * Gets the type of action the taint wrapper shall perform on a given method
	 * 
	 * @param subSig
	 *            The subsignature of the method to look for
	 * @param parentClass
	 *            The parent class in which to start looking
	 * @return The type of action to be performed on the given method
	 */
	private MethodWrapType getMethodWrapType(String subSig,
			SootClass parentClass) {
		// If this is not one of the supported classes, we skip it
		boolean isSupported = false;
		for (String supportedClass : this.includeList)
			if (parentClass.getName().startsWith(supportedClass)) {
				isSupported = true;
				break;
			}

		// Do we always model equals() and hashCode()?
		if (alwaysModelEqualsHashCode
				&& (subSig.equals("boolean equals(java.lang.Object)") || subSig
						.equals("int hashCode()")))
			return MethodWrapType.CreateTaint;

		// Do not process unsupported classes
		if (!isSupported)
			return MethodWrapType.NotRegistered;

		if (parentClass.isInterface())
			return getInterfaceWrapType(subSig, parentClass);
		else {
			// We have to walk up the hierarchy to also include all methods
			// registered for superclasses
			List<SootClass> superclasses = Scene.v().getActiveHierarchy()
					.getSuperclassesOfIncluding(parentClass);
			for (SootClass sclass : superclasses) {
				MethodWrapType wtClass = getMethodWrapTypeDirect(
						sclass.getName(), subSig);
				if (wtClass != MethodWrapType.NotRegistered)
					return wtClass;

				for (SootClass ifc : sclass.getInterfaces()) {
					MethodWrapType wtIface = getInterfaceWrapType(subSig, ifc);
					if (wtIface != MethodWrapType.NotRegistered)
						return wtIface;
				}
			}
		}

		return MethodWrapType.NotRegistered;
	}

	/**
	 * Checks whether the taint wrapper has an entry for the given combination
	 * of class/interface and method subsignature. This method does not take the
	 * hierarchy into account.
	 * 
	 * @param className
	 *            The name of the class to look for
	 * @param subSignature
	 *            The method subsignature to look for
	 * @return The type of wrapping if the taint wrapper has been configured
	 *         with the given class or interface name and method subsignature,
	 *         otherwise NotRegistered.
	 */
	private MethodWrapType getMethodWrapTypeDirect(String className,
			String subSignature) {
		if (alwaysModelEqualsHashCode
				&& (subSignature.equals("boolean equals(java.lang.Object)") || subSignature
						.equals("int hashCode()")))
			return MethodWrapType.CreateTaint;

		Set<String> cEntries = classList.get(className);
		Set<String> eEntries = excludeList.get(className);
		Set<String> kEntries = killList.get(className);

		if (cEntries != null && cEntries.contains(subSignature))
			return MethodWrapType.CreateTaint;
		if (eEntries != null && eEntries.contains(subSignature))
			return MethodWrapType.Exclude;
		if (kEntries != null && kEntries.contains(subSignature))
			return MethodWrapType.KillTaint;
		return MethodWrapType.NotRegistered;
	}

	/**
	 * Checks whether the taint wrapper has been configured for the given method
	 * in the given interface or one of its parent interfaces.
	 * 
	 * @param subSig
	 *            The method subsignature to look for
	 * @param ifc
	 *            The interface where to start the search
	 * @return The configured type of wrapping if the given method is
	 *         implemented in the given interface or one of its super
	 *         interfaces, otherwise NotRegistered
	 */
	private MethodWrapType getInterfaceWrapType(String subSig, SootClass ifc) {
		if (ifc.isPhantom())
			return getMethodWrapTypeDirect(ifc.getName(), subSig);

		assert ifc.isInterface() : "Class " + ifc.getName()
				+ " is not an interface, though returned "
				+ "by getInterfaces().";
		for (SootClass pifc : Scene.v().getActiveHierarchy()
				.getSuperinterfacesOfIncluding(ifc)) {
			MethodWrapType wt = getMethodWrapTypeDirect(pifc.getName(), subSig);
			if (wt != MethodWrapType.NotRegistered)
				return wt;
		}
		return MethodWrapType.NotRegistered;
	}

	/**
	 * Sets whether the taint wrapper shall always assume the return value of a
	 * call "a = x.foo()" to be tainted if the base object is tainted, even if
	 * the respective method is not in the data file.
	 * 
	 * @param aggressiveMode
	 *            True if return values shall always be tainted if the base
	 *            object on which the method is invoked is tainted, otherwise
	 *            false
	 */
	public void setAggressiveMode(boolean aggressiveMode) {
		this.aggressiveMode = aggressiveMode;
	}

	/**
	 * Gets whether the taint wrapper shall always consider return values as
	 * tainted if the base object of the respective invocation is tainted
	 * 
	 * @return True if return values shall always be tainted if the base object
	 *         on which the method is invoked is tainted, otherwise false
	 */
	public boolean getAggressiveMode() {
		return this.aggressiveMode;
	}

	/**
	 * Sets whether the equals() and hashCode() methods shall always be modeled,
	 * regardless of the target type.
	 * 
	 * @param alwaysModelEqualsHashCode
	 *            True if the equals() and hashCode() methods shall always be
	 *            modeled, regardless of the target type, otherwise false
	 */
	public void setAlwaysModelEqualsHashCode(boolean alwaysModelEqualsHashCode) {
		this.alwaysModelEqualsHashCode = alwaysModelEqualsHashCode;
	}

	/**
	 * Gets whether the equals() and hashCode() methods shall always be modeled,
	 * regardless of the target type.
	 * 
	 * @return True if the equals() and hashCode() methods shall always be
	 *         modeled, regardless of the target type, otherwise false
	 */
	public boolean getAlwaysModelEqualsHashCode() {
		return this.alwaysModelEqualsHashCode;
	}

	/**
	 * Registers a prefix of class names to be included when generating taints.
	 * All classes whose names don't start with a registered prefix will be
	 * skipped.
	 * 
	 * @param prefix
	 *            The prefix to register
	 */
	public void addIncludePrefix(String prefix) {
		this.includeList.add(prefix);
	}

	/**
	 * Adds a method to which the taint wrapping rules shall apply
	 * 
	 * @param className
	 *            The class containing the method to be wrapped
	 * @param subSignature
	 *            The subsignature of the method to be wrapped
	 */
	public void addMethodForWrapping(String className, String subSignature) {
		Set<String> methods = this.classList.get(className);
		if (methods == null) {
			methods = new HashSet<String>();
			this.classList.put(className, methods);
		}
		methods.add(subSignature);
	}

	@Override
	public EasyTaintWrapper clone() {
		return new EasyTaintWrapper(this);
	}

}
