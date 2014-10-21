package soot.we.android.component;

import soot.SootField;

public class EntityClassVariable {

	private String type;
	private String name;
	private boolean public_modify;
	private boolean private_modify;
	private boolean Static_modify;
	public SootField sf;
	public EntityClassVariable(String subsignature,String signature,SootField sf){
	     this.type = subsignature;
	     this.name = signature;
	     this.sf=sf;
	}
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public boolean isPublic_modify() {
		return public_modify;
	}
	public void setPublic_modify(boolean public_modify) {
		this.public_modify = public_modify;
	}
	public boolean isPrivate_modify() {
		return private_modify;
	}
	public void setPrivate_modify(boolean private_modify) {
		this.private_modify = private_modify;
	}
	public boolean isStatic_modify() {
		return Static_modify;
	}
	public void setStatic_modify(boolean static_modify) {
		Static_modify = static_modify;
	}
	
}
