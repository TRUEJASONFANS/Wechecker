package soot.we.android.XML;

public class EntityLayout {
	private String LayoutName;
	private String ElementName;
	private String FuncName;

	public EntityLayout(String layoutName, String elementName, String funcName) {
		super();
		LayoutName = layoutName;
		ElementName = elementName;
		FuncName = funcName;
	}
	public String getLayoutName() {
		return LayoutName;
	}
	public void setLayoutName(String layoutName) {
		LayoutName = layoutName;
	}
	public String getElementName() {
		return ElementName;
	}
	public void setElementName(String elementName) {
		ElementName = elementName;
	}
	public String getFuncName() {
		return FuncName;
	}
	public void setFuncName(String funcName) {
		FuncName = funcName;
	}
	public void print(){
		System.out.println("layoutName:"+this.LayoutName);
		System.out.println("ElementName:"+this.ElementName);
		System.out.println("FuncName:"+this.FuncName);
	}
	
}
