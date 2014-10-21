package soot.we.android.component;

import java.util.ArrayList;
import java.util.List;

import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.we.android.log.AlarmLog;
import soot.we.android.transformer.CompTransfor;

public class GetCompJimpleClass {
	public static List<EntityClass> classList;

	public List<EntityClass> run() {
	
		classList = new ArrayList<EntityClass>();
		SootConfig sc = new SootConfig();
		String[] soot_args = sc.getSootArgs();
		Pack pack1=PackManager.v().getPack("wjtp");
		pack1.add(new Transform("wjtp.myTrans", new CompTransfor()));
		try {
		soot.Main.main(soot_args);
		}
		catch(Exception e) {
			AlarmLog.writeToAlarm("There may be some error for the Soot");
			AlarmLog.writeToCommonAlarm("There may be some error for the Soot");
		}
		return classList;

	}

}
