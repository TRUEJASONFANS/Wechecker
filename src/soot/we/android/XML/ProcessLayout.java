package soot.we.android.XML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;
import soot.we.android.MainThread;
import soot.we.android.resource.ARSCFileParser;
import soot.we.android.resource.LayoutControl;
import soot.we.android.resource.LayoutFileParser;

public class ProcessLayout {
	public static List<Integer> getLayoutControls(String apkpath,
			String sdkPlatform, EntityApplicationBase app) throws Exception {
		G.reset();
		MainThread.resParser = new ARSCFileParser();
		try {
			MainThread.resParser.parse(apkpath);
			Options.v().set_no_bodies_for_excluded(true);
			Options.v().set_allow_phantom_refs(true);
			Options.v().set_whole_program(true);
			Options.v().set_android_jars(sdkPlatform);
			Options.v().set_process_dir(Collections.singletonList(apkpath));
			Options.v().set_soot_classpath(
					Scene.v().getAndroidJarPath(sdkPlatform, apkpath));

			LayoutFileParser lfp = new LayoutFileParser(app.getPackageName(),
					MainThread.resParser);
			lfp.parseLayoutFile(apkpath, app.getEntryPointsClasses());

			PackManager.v().getPack("wjpp").apply();
			PackManager.v().getPack("cg").apply();
			PackManager.v().getPack("wjtp").apply();
			Map<Integer, LayoutControl> layoutControls = lfp.getUserControls();
			// ZTZT
			MainThread.layoutCallBack = lfp.getCallbackMethods();
			// Set<Map.Entry<Integer, LayoutControl>> layoutControlsSet =
			// layoutControls.entrySet();

			List<Integer> sensitiveLayouts = new ArrayList<Integer>();

			Collection<LayoutControl> connection = layoutControls.values();
			Iterator<LayoutControl> iterator = connection.iterator();
			while (iterator.hasNext()) {
				LayoutControl lc = iterator.next();
				System.out.println("ID: " + lc.getID());

				// System.out.println("ViewClass: "+lc.getViewClass().getName());
				System.out.println("sensitive: " + lc.isSensitive());
				if (lc.isSensitive()) {
					sensitiveLayouts.add(lc.getID());
				}
			}
			System.out.println("Found " + layoutControls.size()
					+ " layout controls");
			return sensitiveLayouts;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
