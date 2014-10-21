package soot.we.android.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.we.android.MainThread;
import soot.we.android.XML.EntityApplicationBase;
import soot.we.android.XML.ProcessXML;
import soot.we.android.component.SootConfig;
import soot.we.android.log.AlarmLog;

public class Test {

	public static EntityApplicationBase app;
	private static List<String> fileList;
	public static String folderPath;
	public static String apkpath;
	public static String sdkPlatform;

	public static void main(String[] args) throws IOException {
		fileList = new ArrayList<String>();
//		if(args.length<2) {
//			printUsage();
//			try {
//				Thread.sleep(3000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return ;
//		}
//		folderPath =args[0];
//		sdkPlatform = args[1];
		folderPath = "C:\\Users\\jason\\Desktop\\testcase\\Apk";
		sdkPlatform = System.getProperty("user.dir")+"\\platforms";
		System.out.println(folderPath);
		find(folderPath, "\\S+\\.apk");
		assert (fileList.size() > 0);
		AlarmLog.main();
		
		for (String s : fileList) {
			long startTime=System.currentTimeMillis();
			apkpath = s;
			ProcessXML p = new ProcessXML();
			p.handleAndroidXMLFile(apkpath);
			app = p.getEntityApplicationBase();
			app.printApplication();
			SootConfig.setAndroidPlatform(sdkPlatform);
			SootConfig.AkpPath = apkpath;
			AlarmLog.writeToAlarm("FileName: " + s);
			AlarmLog.writeToCommonAlarm("FileName: " + s);
			AlarmLog.writeToAlarm("*******************************************************************************");
			AlarmLog.writeToAlarm("permission:");
			AlarmLog.writeToCommonAlarm("permission:");
			for(String permission:app.getGrantedPermission()) {
				AlarmLog.writeToAlarm(permission);
				AlarmLog.writeToCommonAlarm(permission);
			}
			// ********detecting started************
			try {
				MainThread.run(args);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long endTime=System.currentTimeMillis(); 
			AlarmLog.writeToAlarm("Program running: "+(endTime-startTime)+"ms");
			AlarmLog.writeToCommonAlarm("Program running: "+(endTime-startTime)+"ms");
			s = null;
		}
		
	}

	private static void printUsage() {
		// TODO Auto-generated method stub
		System.out.println("[0] apk files directory");
		System.out.println("[1] android-jar directory");
	}

	public static void find(String path, String reg) {
		Pattern pat = Pattern.compile(reg);
		File file = new File(path);
		File[] arr = file.listFiles();
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].isDirectory()) {
				find(arr[i].getAbsolutePath(), reg);
			}
			Matcher mat = pat.matcher(arr[i].getAbsolutePath());
			if (mat.matches()) {
				fileList.add(arr[i].getAbsolutePath());
				// System.out.println(arr[i].getAbsolutePath());
			}
		}
	}
	public static void testSingleApk(String apk) throws Exception{
		long startTime=System.currentTimeMillis();
		apkpath = apk;
		AlarmLog.main();
		ProcessXML p = new ProcessXML();
		p.handleAndroidXMLFile(apkpath);
		app = p.getEntityApplicationBase();
		app.printApplication();
		SootConfig.setAndroidPlatform(sdkPlatform);
		SootConfig.AkpPath = apkpath;

		AlarmLog.writeToAlarm("FileName: " + apkpath);
		AlarmLog.writeToCommonAlarm("FileName: " + apkpath);
		AlarmLog.writeToAlarm("*******************************************************************************");
		
		// ********detecting started************
		MainThread.run(null);
		long endTime=System.currentTimeMillis();  
		System.out.println("Program running: "+(endTime-startTime)+"ms"); 
		AlarmLog.writeToAlarm("Program running: "+(endTime-startTime)+"ms");
		AlarmLog.writeToCommonAlarm("Program running: "+(endTime-startTime)+"ms");
		AlarmLog.writeToAlarm("/n");
	}
}
