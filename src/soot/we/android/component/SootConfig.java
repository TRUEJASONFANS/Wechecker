package soot.we.android.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class SootConfig {
	private static String AndroidPlatform;

	public static String getAndroidPlatform() {
		return AndroidPlatform;
	}

	public static void setAndroidPlatform(String androidPlatform) {
		AndroidPlatform = androidPlatform;
	}

	public static String getAkpPath() {
		return AkpPath;
	}

	public static void setAkpPath(String akpPath) {
		AkpPath = akpPath;
	}

	public static String AkpPath;

	public SootConfig() {

		// Options.v().set_verbose(true);
		// Options.v().set_no_bodies_for_excluded(true);
		// Options.v().set_whole_program(true);
		// Options.v().set_process_dir(Collections.singletonList(AkpPath));
		// Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(AndroidPlatform,
		// AkpPath));
		// Options.v().set_android_jars(AndroidPlatform);
		// Main.v().autoSetOptions();
		// Options.v().setPhaseOption("cg.spark", "on");

		// Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
		// Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
		// Scene.v().addBasicClass("java.lang.ThreadGroup",
		// SootClass.SIGNATURES);

		// Pack jtp = PackManager.v().getPack("jtp");
		// jtp.add(new Transform("jtp.myTrans1", new AndroidInstrument()));

		// Scene.v().loadNecessaryClasses();
	}
	public String[] getSootArgs() {
		G.reset();
		Options.v().set_whole_program(true);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		List<String> argsList = new ArrayList<String>();
		argsList.addAll(Arrays.asList(new String[] {"-w","-O","-android-jars",AndroidPlatform,"-process-dir",AkpPath,"-allow-phantom-refs", "-f","n"
		}));
		Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
		Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
		String[] soot_args = argsList.toArray(new String[0]);
        
		return soot_args;
	}

}
