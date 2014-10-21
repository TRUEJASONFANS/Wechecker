package soot.we.android.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;
public class AlarmLog {
	public static BufferedReader bufread;
	private static String path = System.getProperty("user.dir") + "/alarm.txt";
	private static String path2 = System.getProperty("user.dir") + "/commonAlarm.txt";
	private static String path3 = System.getProperty("user.dir") + "/controlFlowGraph.txt";
	private static File filename = new File(path);
	private static File filename2 = new File(path2);
	private static File filename3 = new File(path3);
	public static JTextArea LeakingPathAleartTextarea;
	public static  JTextArea CommonUserInplyArea;
	public static String result1="";
	public static String result2="";
	public static boolean enableRedirect = false;
	public static void main() throws IOException {
		result1="";
		result2="";
		if(enableRedirect) return;
		if (filename.exists()) {
			filename.delete();
			filename.createNewFile();
			System.err.println(filename + " is created！");
		}
		
		if (filename2.exists()) {
			filename2.delete();
			filename2.createNewFile();
			System.err.println(filename2 + " is created！");
		}
		if (filename3.exists()) {
			filename3.delete();
			filename3.createNewFile();
			System.err.println(filename3 + " is created！");
		}
	}
	
	public static void writeToCommonAlarm(String newStr)  {
		newStr = newStr+"\n";
		result1 = result1 + newStr;
		if(enableRedirect) return;
		BufferedWriter fw = null;
		try {
			fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename2, true), "UTF-8"));
			fw.append(newStr);
			fw.newLine();
			fw.flush(); //
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	public static void writeToAlarm(String newStr)  {
		if(enableRedirect) return;
		newStr = newStr+"\n";
		result2 = result2 + newStr;
		BufferedWriter fw = null;
		try {
			fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8"));
			fw.append(newStr);
			fw.newLine();
			fw.flush(); //
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public static void writeToControlFlowGrpah(String newStr){
		if(enableRedirect) return;
		BufferedWriter fw = null;
		try {
			fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename3, true), "UTF-8"));
			fw.append(newStr);
			fw.newLine();
			fw.flush(); //
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public static void delFolder(String folderPath) {  
		try {
			delAllFile(folderPath); // 删除完里面所有内容
			String filePath = folderPath;
			filePath = filePath.toString();
//			java.io.File myFilePath = new java.io.File(filePath);
//			myFilePath.delete(); // 删除空文件夹
			} catch (Exception e) {
			System.out.println("删除文件夹操作出错");
		}
	}
	private static boolean delAllFile(String folderPath) {
		boolean bea = false;
		File file = new File(folderPath);
		if (!file.exists()) {
			return bea;
		}
		if (!file.isDirectory()) {
			return bea;
		}
		String[] tempList = file.list();
		File temp = null;
		for (int i = 0; i < tempList.length; i++) {
			if (folderPath.endsWith(File.separator)) {
				temp = new File(folderPath + tempList[i]);
			} 
			else {
				temp = new File(folderPath + File.separator + tempList[i]);
			}
			if (temp.isFile()) {
				temp.delete();
			}
			if (temp.isDirectory()) {
				delAllFile(folderPath + "/" + tempList[i]);// 先删除文件夹里面的文件
				delFolder(folderPath + "/" + tempList[i]);// 再删除空文件夹
				bea = true;
			}
		}
		return bea;
	} 
	public static void updateToScreen(){
		LeakingPathAleartTextarea.append(result2);
		CommonUserInplyArea.append(result1);
	}
	public static Map<String, String> getCommonAlarm(String fileName) {
		Map<String, String> AlarmMap = new HashMap<String, String>();
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				// 显示行号
				if (tempString.startsWith("<")) {
					int split = tempString.indexOf("^");
					String alarmString = tempString.substring(0, split - 1);
//					System.out.println("alramString: " + alarmString);
					String friendlyAlarm = tempString.substring(split + 2,
							tempString.length());
//					System.out.println("friendlyAlarm: " + friendlyAlarm);
					AlarmMap.put(alarmString, friendlyAlarm);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}

		return AlarmMap;

	}
}
