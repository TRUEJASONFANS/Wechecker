package soot.we.android.component;


public class EntityApplicationClass {
	public EntityClass baseClass;
	public String globalRegisterClass;
	public String ComponentCallback;
	public EntityApplicationClass(EntityClass bc){
		this.baseClass = bc;
		this.globalRegisterClass = new String();
		this.ComponentCallback = new String();
	}
}
