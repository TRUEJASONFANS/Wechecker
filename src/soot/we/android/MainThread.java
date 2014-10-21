package soot.we.android;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.we.android.IntentResolution.IntentFinder;
import soot.we.android.XML.ProcessLayout;
import soot.we.android.callGraph.AndroidMethod;
import soot.we.android.callGraph.BuildCallGraph;
import soot.we.android.callGraph.EasyTaintWrapper;
import soot.we.android.callGraph.SourceSinkFinder;
import soot.we.android.component.EntityApplicationClass;
import soot.we.android.component.EntityClass;
import soot.we.android.component.GetCompJimpleClass;
import soot.we.android.log.AlarmLog;
import soot.we.android.resource.ARSCFileParser;
import soot.we.android.test.Test;

public class MainThread {
	public static List<EntityClass> classList;
	public static Map<String, AndroidMethod> sourceMethods;
	public static Map<String, AndroidMethod> sinkMethods;
	public static Map<String, AndroidMethod> intentRelatedMethods;
	public static Map<String, String> FriendlyAlarmMap;
	public static List<Integer> sensitivelayouts;
	//ZTZT
	public static Map<String, Set<String>> layoutCallBack;
	public static ARSCFileParser resParser;

	public static void run(String[] args) throws Exception {
		//get alarm statements for common users
		FriendlyAlarmMap = AlarmLog.getCommonAlarm("Friendly alert.txt");
		if(FriendlyAlarmMap==null){
			System.err.println("no found 'Friendly alert.txt' file!");
		}
		//get LayoutControls<ID,Property>
		sensitivelayouts = ProcessLayout.getLayoutControls(Test.apkpath,Test.sdkPlatform, Test.app);
		if(sensitivelayouts==null||sensitivelayouts.size()==0){
			System.err.println("no sensitive layouts!");
		}
		// *********get JimpleClass in classes*******
		GetCompJimpleClass getjimpleclass = new GetCompJimpleClass();
		classList = getjimpleclass.run();
		// **************end*************
		
		// *********Searching classlist*********
		String applicationClassName = Test.app.getApplicationName();
		EntityApplicationClass applicationCalss = null;
		EntityClass providerClass = null;
		
		for (int i = 0; i < classList.size(); i++) {
			if (classList.get(i).isEntryPointClass()) {
				AlarmLog.writeToAlarm("Exported Component:" + classList.get(i).getclassName());
				AlarmLog.writeToCommonAlarm("Exported Component:" + classList.get(i).getclassName());
				System.out.println("entrypoint: "+classList.get(i).getclassName());
				if(classList.get(i).getCompType().equals("android.content.ContentProvider")){
					providerClass = classList.get(i);
				}
			}
			if(applicationClassName.equals(classList.get(i).getclassName())){
				applicationCalss = new EntityApplicationClass(classList.get(i));
			}
		}
		// **************end*************
		
		// ***********Setup the list of Source And Sink methods
		SourceSinkFinder ssfinder = new SourceSinkFinder();
		ssfinder.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
		sourceMethods = ssfinder.getSourceMethods();
		sinkMethods = ssfinder.getSinkMethods();
		intentRelatedMethods = new IntentFinder().intentRelatedMethods;
		EasyTaintWrapper easyWrapper = new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt"));
		// ***************end****************
		
		// **************build call graph*********
		BuildCallGraph buildCallGraph = new BuildCallGraph(classList,1,applicationCalss,providerClass);
		buildCallGraph.setEasyTaintWrapper(easyWrapper);
		buildCallGraph.run();
		if(BuildCallGraph.leakingPaths==null||BuildCallGraph.leakingPaths.size()==0){
			if(BuildCallGraph.leakingPaths.size()==0)
			AlarmLog.writeToAlarm("The app is secure");
			AlarmLog.writeToCommonAlarm("The app is secure");
		}
		// ***************end**************	
		clear();
	}

	private static void clear() {
		// TODO Auto-generated method stub
		classList = null;
		sourceMethods = null;
		sinkMethods = null;
		intentRelatedMethods = null;
		FriendlyAlarmMap = null;
		sensitivelayouts = null;
		layoutCallBack = null;
		resParser = null;
		
	}
}
