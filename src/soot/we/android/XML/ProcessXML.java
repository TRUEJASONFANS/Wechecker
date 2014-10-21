package soot.we.android.XML;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import test.AXMLPrinter;

import android.content.res.AXmlResourceParser;

public class ProcessXML {

	EntityApplicationBase entityApplicationBase;

	// Store the callback methods registered in the layout file
	private ArrayList<EntityLayout> listLayout;
	private Set<String> permissionSet;
	
    public ProcessXML(){
    	entityApplicationBase = new EntityApplicationBase();
    	permissionSet = new HashSet<String>();
    	listLayout = new ArrayList<EntityLayout>();
    }
	public EntityApplicationBase getEntityApplicationBase() {
		return entityApplicationBase;
	}

	public void setEntityApplicationBase(EntityApplicationBase entityApplicationBase) {
		this.entityApplicationBase = entityApplicationBase;
	}

	/**
	 * interface to load ManifestFile
	 * 
	 * @param apk
	 *            (string)
	 */
	public void loadXMLFile(String apk) {
		handleAndroidXMLFile(apk);
	}

	/**
	 * Opens the given apk file and accessing the contained android manifest
	 * file
	 * 
	 * @param apk
	 */
	public void handleAndroidXMLFile(String apk) {

		File apkF = new File(apk);
		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!");
		boolean found = false;
		int countLayout = 0;
		try {
			ZipFile archive = null;
			try {
				archive = new ZipFile(apkF);
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();

//					// we are dealing with the Layout file
//					if (entryName.contains("res")) {
//						try {
//							countLayout++;
//							loadCallBackFromLayout(
//									archive.getInputStream(entry), entryName);
//						} catch (Exception e) {
//							throw new RuntimeException(
//									"Error when looking for Layout file in apk: "
//											+ e);
//						}
//
//					}

					// We are dealing with the Android manifest
					if (entryName.equals("AndroidManifest.xml")) {
						try {
							found = true;
							loadClassesFromBinaryManifest(archive.getInputStream(entry));
							break;
						} catch (Exception e) {
							throw new RuntimeException("Error when looking for manifest in apk: "+ e);
						}
					}

				}
			} finally {
				if (archive != null)
					archive.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(
					"Error when extracting XML from apk file: " + e);
		}
		if (!found)
			throw new RuntimeException("No manifest file found in apk");

//		System.out.println("Found " + countLayout
//				+ " callback methods in layout file");
	}

	protected void loadClassesFromBinaryManifest(InputStream manifestIS) {
		try {
			AXmlResourceParser parser = new AXmlResourceParser();
			parser.open(manifestIS);
			int i=0;
			int type = -1;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
				switch (type) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					String tagName = parser.getName();
					if (tagName.equals("manifest")) {
						entityApplicationBase.setPackageName(getAttributeValue(parser, "package"));
						String versionCode = getAttributeValue(parser,"versionCode");
						if (versionCode != null && versionCode.length() > 0)
							entityApplicationBase.setVersionCode(Integer.valueOf(versionCode));
						entityApplicationBase.setVersionName(getAttributeValue(parser, "versionName"));
					}
					else if (tagName.equals("activity")
							|| tagName.equals("receiver")
							|| tagName.equals("service")
							|| tagName.equals("provider")) {
						String attrValue = getAttributeValue(parser, "enabled");
						if (attrValue != null && attrValue.equals("false"))
							continue;
						EntityComponent temp = parseComponentInManifest(parser, tagName);
						entityApplicationBase.getComponents().add(temp);
						
					}

					else if (tagName.equals("uses-permission")) {
						String permissionName = getAttributeValue(parser,"name");
						permissionSet.add(permissionName);
					}

					else if (tagName.equals("uses-sdk")) {
						String minVersion = getAttributeValue(parser,"minSdkVersion");
						if (minVersion != null && minVersion.length() > 0)
							entityApplicationBase.setMinSdkVersion(Integer.valueOf(minVersion));
						String targetVersion = getAttributeValue(parser,"targetSdkVersion");
						if (targetVersion != null && targetVersion.length() > 0)
							entityApplicationBase.setTargetSdkVersion(Integer.valueOf(targetVersion));
					}

					else if (tagName.equals("application")) {
						String applicationClassName = getAttributeValue(parser,"name");
						entityApplicationBase.setApplicationName(applicationClassName);
					}

					break;
				case XmlPullParser.END_TAG:
					entityApplicationBase.setEntryPointsClasses();
					break;
				case XmlPullParser.TEXT:
					break;
				}
			}
			entityApplicationBase.setGrantedPermission(permissionSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getAttributeValue(AXmlResourceParser parser,
			String attributeName) {
		for (int i = 0; i < parser.getAttributeCount(); i++)
			if (parser.getAttributeName(i).equals(attributeName))
				return AXMLPrinter.getAttributeValue(parser, i);
		return "";
	}

	public EntityComponent parseComponentInManifest(AXmlResourceParser parser,
			String ComponentType) throws XmlPullParserException, IOException {

		String tempexported;
		boolean exported = false,hasintentfilter=false;
		String componentName;
		String requiredpemission=null;
		String tagName;
		EntityComponent currComp = null;
		EntityIntentFilter tempIntentfilter=null;
		
		componentName = getAttributeValue(parser, "name");
//		if(componentName.equals("com.outfit7.funnetworks.push.GcmIntentService")){
//			System.out.println();
//		}
		componentName = expandClassName(componentName);
		tempexported = getAttributeValue(parser, "exported");
			// **************************
		currComp = new EntityComponent(componentName,ComponentType, exported, null);
		parser.next();
		while (!((tagName = parser.getName()).equals(ComponentType))) {
			if (tagName.equals("intent-filter")) {  //parse the <intent-filter> tag
				ArrayList<String> actions = new ArrayList<String>();
				ArrayList<String> categorys = new ArrayList<String>();
				parser.next();
				tagName = parser.getName();
				while (!tagName.equals("intent-filter")) {
						// **************************
					exported = true;
					if (tagName.equals("action")) {
						actions.add(getAttributeValue(parser, "name"));
						} 
					else if (tagName.equals("category")) {
							categorys.add(getAttributeValue(parser, "name"));
						}
						parser.next();
						tagName = parser.getName();
						tempIntentfilter = new EntityIntentFilter(actions,
								categorys);
					}
				}
				if (tempIntentfilter != null) { 
					hasintentfilter = true;
					currComp.addIntentFilter(tempIntentfilter);
					tempIntentfilter = null;
				}
				if (tagName.equals("permission")) {
					requiredpemission = getAttributeValue(parser, "name");
				}			
				parser.next();
				
			}
			if (tempexported.equals("")) {// judge whether the component is exported or not
				if (hasintentfilter) {
					exported = true;
				} else {
					exported = false;
				}
			} else if (tempexported.equals("true")) {
				exported = true;
			} else {
				exported = false;
			}
			currComp.setExported(exported);
			currComp.setRequiredPermission(requiredpemission);
		
		return currComp;

	}

	/**
	 * Generates a full class name from a short class name by appending the
	 * globally-defined package when necessary
	 * 
	 * @param className
	 *            The class name to expand
	 * @return The expanded class name for the given short name
	 */
	private String expandClassName(String className) {
		if (className.startsWith("."))
			return entityApplicationBase.getPackageName() + className;
		else if (className.substring(0, 1).equals(className.substring(0, 1).toUpperCase()))
			return entityApplicationBase.getPackageName() + "." + className;
		else
			return className;
	}

//	protected void loadCallBackFromLayout(InputStream manifestIS,
//			String layoutName) {
//		try {
//			AXmlResourceParser parser = new AXmlResourceParser();
//			parser.open(manifestIS);
//			int type = -1;
//			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
//				switch (type) {
//				case XmlPullParser.START_DOCUMENT:
//					break;
//				case XmlPullParser.START_TAG:
//					String tagName = parser.getClassAttribute();
//					if (tagName.equals("RelativeLayout")) {
//						System.out.println("name:" + layoutName);
//						System.out.println("depth:"
//								+ parser.getAttributeCount());
//					}
//
//					 if (tagName.equals("manifest")) {
//					 entityApplicationBase.setPackageName(getAttributeValue(
//					 parser, "package"));
//					 String versionCode = getAttributeValue(parser,
//					 "versionCode");
//					 if (versionCode != null && versionCode.length() > 0)
//					 entityApplicationBase.setVersionCode(Integer
//					 .valueOf(versionCode));
//					 entityApplicationBase.setVersionName(getAttributeValue(
//					 parser, "versionName"));
//					 }
//					break;
//				case XmlPullParser.END_TAG:
//					break;
//				case XmlPullParser.TEXT:
//					break;
//				}
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}
}
