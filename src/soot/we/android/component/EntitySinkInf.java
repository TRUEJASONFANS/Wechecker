package soot.we.android.component;

import soot.Value;
import soot.jimple.Stmt;


public class EntitySinkInf {
	private Value value;
	private Stmt stmt;
	private String methodName;
	private String ClassName;

	public Value getValue() {
		return value;
	}

	public Stmt getStmt() {
		return stmt;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getClassName() {
		return ClassName;
	}

	public EntitySinkInf(Value value, Stmt stmt, String methodName,
			String className) {
		super();
		this.value = value;
		this.stmt = stmt;
		this.methodName = methodName;
		ClassName = className;
	}

}