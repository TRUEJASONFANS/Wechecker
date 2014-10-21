package soot.we.android.XML;

import java.util.ArrayList;

public class EntityComponent {

	
	private String type;
	private String name;//.
	private boolean exported;
	private ArrayList<EntityIntentFilter> intentFilter = null;
	private String requiredPermission;
	private String path;
	
	
	public void setPath(String s){
		path = s;
	}
	
	public String getPath(){
		return path;
	}
	
	public EntityComponent(String name, String type, Boolean exported, String requiredPermission){
		this.name = name;
		this.type = type;
		this.exported = exported;
		this.requiredPermission = requiredPermission;
		intentFilter = new ArrayList<EntityIntentFilter>();
	}
	
	public void addIntentFilter(ArrayList<String> action, ArrayList<String> category){
		intentFilter.add(new EntityIntentFilter(action, category));
	}
	public void addIntentFilter(EntityIntentFilter temp){
		intentFilter.add(temp);
	}
	
	public ArrayList<EntityIntentFilter> getIntentFilter(){
		return intentFilter;
	}
	
	public void setExported(boolean exported) {
		this.exported = exported;
	}

	public void setRequiredPermission(String requiredPermission) {
		this.requiredPermission = requiredPermission;
	}

	public ArrayList<String> getAction(){
		ArrayList<String> a = new ArrayList<String>();
		for (EntityIntentFilter i : intentFilter){//for each intentfilter
			for (String s : i.actions){//for each action in each intentfilter
				a.add(s);
			}
		}
		return a;
	}
	
	public ArrayList<String> getCategory(){
		ArrayList<String> c = new ArrayList<String>();
		for (EntityIntentFilter i : intentFilter){//for each intentfilter
			for (String s : i.categorys){//for each category in each intentfilter
				c.add(s);
			}
		}
		return c;
	}
	
	public String getType(){
		return type;
	}
	
	public boolean getExported(){
		return exported;
	}
	
	public String getRequiredPermission(){
		return requiredPermission;
	}
	
	public String getComponnetName(){
		return name;
	}
    public void printComponent() {
    	System.out.println("type :"+type);
    	System.out.println("name :"+name);
    	System.out.println("exported :"+exported);
    	for(int i = 0; i < intentFilter.size();i++) {
    		intentFilter.get(i).printIntentFilter();
    	}
    	System.out.println("requiredPermission :"+requiredPermission);
    	System.out.println("path :"+path);
    	
    }
  
}
