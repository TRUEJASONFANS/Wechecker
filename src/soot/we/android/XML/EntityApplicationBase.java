package soot.we.android.XML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.we.android.component.EntityClass;

public class EntityApplicationBase {
	
	private int versionCode = -1;
	private String versionName = "";
	private int minSdkVersion = -1;
	private int targetSdkVersion = -1;
	
	private Set<String> entryPointsClasses;//firstly we think all components as potential Entrypointclass
	private Set<String> grantedPermission;
	private Set<String> definedPermission;//defined by developer
    private ArrayList<EntityComponent>  components;
   
	private String applicationName = "";
	private String packageName = "";
	
	public Map<EntityComponent,EntityClass> map;
	public EntityApplicationBase(){
		entryPointsClasses = new HashSet<String>();
		grantedPermission = new HashSet<String>();
		definedPermission = new HashSet<String>();
		components = new ArrayList<EntityComponent>();
		map = new HashMap<EntityComponent,EntityClass>();
	}
	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}


	/*** GrantedPermission ***/
	public Set<String> getGrantedPermission() {
		return grantedPermission;
	}
	public void setGrantedPermission(Set<String> grantedPermission) {
		this.grantedPermission = grantedPermission;
	}
	public void addGrantedPermission(String s){
		grantedPermission.add(s);
	}
	
	/*** DefinedPermission ***/
	public Set<String> getDefinedPermission() {
		return definedPermission;
	}

	public void setDefinedPermission(Set<String> definedPermission) {
		this.definedPermission = definedPermission;
	}
	public void addDefinedPermission(String s){
		definedPermission.add(s);
	}

	public void setMinSdkVersion(Integer valueOf) {

		this.minSdkVersion=valueOf;
	}

	public void setTargetSdkVersion(Integer valueOf) {

		this.targetSdkVersion =valueOf;
	}

	
	/******************EntryPoint******************/
	public void setEntryPointsClasses() {
		for(int i = 0;i<this.getComponents().size();i++) {
			EntityComponent tempComponent = this.getComponents().get(i);
			if(tempComponent.getExported()==true) {
				this.entryPointsClasses.add(tempComponent.getComponnetName());
			}
		}
			
	}
	public void printEntryPointsClasses(){
		Iterator<String> it = entryPointsClasses.iterator();
		while (it.hasNext()) {
		  String str = it.next();
		  System.out.println(str);
		}
	}

	public Set<String> getEntryPointsClasses() {
		return entryPointsClasses;
	}

	/********************************************/
	
	public void setVersionCode(Integer valueOf) {
	     this.versionCode = valueOf;	
	}

	public void setVersionName(String attributeValue) {
		this.versionName = attributeValue;
	}

	public ArrayList<EntityComponent> getComponents() {
		return components;
	}

	public void setComponents(ArrayList<EntityComponent> components) {
		this.components = components;
	}

	/************Print*************************/
	public void printApplication(){
		for(int i=0;i<this.components.size();i++) {
			 this.components.get(i).printComponent();
		}
	}

}
