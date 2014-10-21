package soot.we.android.callGraph;

import java.util.ArrayList;

public class AndroidLifeCycleCallGraph {

	// private ArrayList<String> CycleSequence1 = new ArrayList<String>();
	// private ArrayList<String> CycleSequence2 = new ArrayList<String>();
	// private ArrayList<String> CycleSequence3 = new ArrayList<String>();

	public ArrayList<String[]> sequence = new ArrayList<String[]>();

	public ArrayList<String[]> getActivityAndroidLifeCycleCallGraph() {

		String[] temp1 = { "attachBaseContext","onCreate", "onStart","onResume",
				"ActivityRunning", "onPause","onStop", "onDestroy" };
		
		// String[] temp2 = { "onRestart", "onStart", "onResume",
		// "ActivityRunning", "onPause", "onStop", "onDestroy" };
		//
		// String[] temp3 = { "onCreate", "onStart", "onResume",
		// "ActivityRunning", "onPause", "onStop", "onRestart" };
		
		String[] temp4 = {"onSaveInstanceState","onBackPressed","onRestoreInstanceState","onActivityResult","finish"};
		sequence.add(temp1);
		sequence.add(temp4);
		// ActivitySequence.add(temp2);
		// ActivitySequence.add(temp3);
		return sequence;
	}

	public ArrayList<String[]> getBroadcastAndroidLifeCycleCallGraph() {
		String[] temp1 = { "onReceive" };
		sequence.add(temp1);
		return sequence;

	}

	public ArrayList<String[]> getServiceAndroidLifeCycleCallGraph() {
		String[] temp1 = { "onCreate", "onStartCommand", "onLowMemory",
				"onDestroy" };
		// String[] temp2 = { "onCreate", "onBind", "onUnbind", "onLowMemory",
		// "onDestroy" };
		sequence.add(temp1);
		// ActivitySequence.add(temp2);
		return sequence;

	}
	
	public String[] getApplicationAndroidLifeCycleCallGraph() {
		String[] sequence = { "onCreate","onLowMemory"};
		return sequence;
	}
	public String[] getApplicationGlobleCallbackCallGraph(){
		String[] sequence = { "init","onActivityCreated","onActivityStarted","onActivityResumed",
				"onActivityPaused","onActivityStopped", "onActivityDestroyed"};
		return sequence;
	}
	public String[] getApplicationComponentCallbackCallGraph(){
		String[] sequence = { "onLowMemory","onConfigurationChanged","onTrimMemory"};
		return sequence;
	}
	public String[] getContentProviderAndroidLifeCycleCallGraph() {
		String[] sequence = { "onCreate"};
		return sequence;
	}
	
	public String[] getFragmentAndroidLifeCycleCallGraph() {
		String[] sequence = { "onAttach","onCreate","onCreatView","onActivityCreated","onStart","onResume",
				"onActivitySaveInstanceState","onPause","onStop","onDestroyView","onDestroy","onDetach"};
		return sequence;
	}
	private static final String[] activityMethods = { "onCreate", "onStart",
			"onResume", "ActivityRunning", "onPause", "onStop", "onDestroy",
			"onRestart" };
	
	private static final String[] SpecificCallBack = { "onKeyDown",
			"onTouchEvent","onListItemClick"};
	

	public boolean isSpecificCallBack(String methodName) {
		for (String lc : SpecificCallBack) {
			if (lc.equals(methodName)) {
				return true;
			}
		}
		return false;
	}

	public boolean isLifeCycle(String methodName) {
		for (String lc : activityMethods) {
			if (lc.endsWith(methodName)) {
				return true;
			}
		}
		return false;
	}
}
