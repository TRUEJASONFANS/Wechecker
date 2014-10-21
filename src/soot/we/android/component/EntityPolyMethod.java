package soot.we.android.component;

import soot.Value;

public class EntityPolyMethod {

	private Value base;
	private String superClass;
	private String subClass;

	public EntityPolyMethod(Value base, String superClass, String subClass) {
		super();
		this.base = base;
		this.superClass = superClass;
		this.subClass = subClass;
	}

	public Value getBase() {
		return base;
	}

	public String getSuperClass() {
		return superClass;
	}

	public String getSubClass() {
		return subClass;
	}

}
