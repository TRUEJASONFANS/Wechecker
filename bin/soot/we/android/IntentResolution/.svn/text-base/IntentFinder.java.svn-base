package soot.we.android.IntentResolution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import soot.we.android.callGraph.AndroidMethod;
import soot.we.android.parser.PermissionMethodParser;

public class IntentFinder {
	public PermissionMethodParser permissionMethodParser;
	public String intentFilter="IntentFilter.txt";
	public Set<AndroidMethod> methodList;
	public Map<String, AndroidMethod> intentRelatedMethods;
	public  IntentFinder() throws IOException {
		permissionMethodParser = PermissionMethodParser.fromFile(intentFilter);
		this.methodList = permissionMethodParser.parse();
		intentRelatedMethods = new HashMap<String, AndroidMethod>();
		for(AndroidMethod am:this.methodList) {
			this.intentRelatedMethods.put(am.getSignature(), am);
		}
	}
} 