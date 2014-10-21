package soot.we.android.IntentResolution;

import soot.Value;

public class IntentValue{
	public  Value v;
	public  String methodname;
	public  String classname;
	public IntentValue(Value tv,String mname,String cname){
		v = tv;
		methodname = mname;
		classname = cname;
	}
}