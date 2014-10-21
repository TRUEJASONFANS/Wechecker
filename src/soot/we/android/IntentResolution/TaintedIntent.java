package soot.we.android.IntentResolution;

import soot.jimple.Stmt;
import soot.we.android.component.EntityClass;
import soot.we.android.component.EntitySourceInf;

public class TaintedIntent {
	private EntityClass sourceclass;//where tainted intent coming from
	private EntityClass targetclass;
	private EntitySourceInf taintedsource;//the one who propagate  
	private Stmt stmt;
	private String Type;//this intent contain what kind of type of tainted message
	public EntityClass getSourceclass() {
		return sourceclass;
	}
	public void setSourceclass(EntityClass sourceclass) {
		this.sourceclass = sourceclass;
	}
	public EntitySourceInf getTaintedsource() {
		return taintedsource;
	}
	public void setTaintedsource(EntitySourceInf taintedsource) {
		this.taintedsource = taintedsource;
	}
	public Stmt getStmt() {
		return stmt;
	}
	public void setStmt(Stmt stmt) {
		this.stmt = stmt;
	}
	public EntityClass getTargetclass() {
		return targetclass;
	}
	public void setTargetclass(EntityClass targetclass) {
		this.targetclass = targetclass;
	}
	public TaintedIntent(EntityClass sclass,EntityClass tclass,EntitySourceInf taintedsource,Stmt stmt,String type){
		this.sourceclass = sclass;
		this.targetclass = tclass;
		this.taintedsource = taintedsource;
		this.stmt = stmt;
		this.Type = type;
	}
	public String getType() {
		return Type;
	}
	public void setType(String type) {
		Type = type;
	}
}
