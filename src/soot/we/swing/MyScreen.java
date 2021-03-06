package soot.we.swing;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import soot.we.android.callGraph.BuildCallGraph;
import soot.we.android.log.AlarmLog;
import soot.we.android.test.Test;

public class MyScreen implements ActionListener{
	public JSplitPane  centerParts;
	public JFrame frame;
	public JTextArea LeakingPathAleartTextarea;
	public JTextArea CommonUserInplyArea;
	public JFileChooser fileChooser = null;
	public Thread thread1;
	public File file;
	public JProgressBar progressbar;
	public JTextField jtextfiled;
	public JButton b1;
	public JButton b2;
	public JButton inputBtn1;
	public JMenu  menu1;
	public JMenuItem item1;
	public JMenuItem item2;

	public MyScreen() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException{
		frame = new JFrame("APK Static Checking");
		frame.setLocation(250, 150);
		frame.setSize(1400, 800);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMenurelated();
		setComponents();
		fileChooser = new JFileChooser("");
		frame.setVisible(true);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
	
	}
	private void setComponents() {
		LeakingPathAleartTextarea = new JTextArea();
		CommonUserInplyArea = new JTextArea();
		JScrollPane scrollpane1,scrollpane2;
		scrollpane1 = new JScrollPane(LeakingPathAleartTextarea);
		scrollpane1.setToolTipText("LeakingPath");
		scrollpane2 = new JScrollPane(CommonUserInplyArea);
		scrollpane2.setToolTipText("Sensative Message May leak");
		centerParts = new JSplitPane();
		centerParts.setOneTouchExpandable(true);
		centerParts.setContinuousLayout(true);
		centerParts.setSize(1400,400);
		centerParts.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		centerParts.setLeftComponent(scrollpane1);
		centerParts.setRightComponent(scrollpane2);
		centerParts.setDividerSize(3);
		centerParts.setDividerLocation(centerParts.getWidth()*2/3);
		frame.getContentPane().add(centerParts,BorderLayout.CENTER);
		JPanel buttonpanel = new JPanel();
		JPanel otherspanel = new JPanel(new GridLayout(2, 1));
		JPanel inputspanel = new JPanel();
		JPanel southpanel = new JPanel(new GridLayout(2, 1));
		jtextfiled = new JTextField(30);
		jtextfiled.setToolTipText("Please input the folder path of Apk");
		inputBtn1 = new JButton("Browse");
		inputBtn1.addActionListener(this);
		inputBtn1.setToolTipText("browse the apk filefolder");
		b1 = new JButton("Running");
		b1.addActionListener(this);
		b1.setToolTipText("to start the Checking");
		b2 = new JButton("Clear");
		b2.setToolTipText("to reset and clearn the screen");
		b2.addActionListener(this);
		buttonpanel.add(b1);
		buttonpanel.add(b2);
		progressbar = new JProgressBar(JProgressBar.HORIZONTAL);
		progressbar.setToolTipText("The progress of Checking");
		progressbar.setMaximum(100);
		progressbar.setMinimum(0);
		progressbar.setValue(0);
		progressbar.setStringPainted(true);
		inputspanel.add(jtextfiled);
		inputspanel.add(inputBtn1);
		otherspanel.add(progressbar);
		otherspanel.add(inputspanel);
		southpanel.add(otherspanel);
		southpanel.add(buttonpanel);
		frame.getContentPane().add(southpanel,BorderLayout.SOUTH);
		
	}
	private void setMenurelated(){
		JMenuBar menubar = new JMenuBar();
		frame.setJMenuBar(menubar);
		menu1 = new JMenu("Functions");
		menubar.add(menu1);
		item1 = new JMenuItem("Confirguration");
		item1.addActionListener(this);
		item2 = new JMenuItem("Exit");
		menu1.add(item1);
		menu1.add(item2);
	
	}
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {

		MyScreen screen = new MyScreen();
		
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		int result;
		if(e.getActionCommand().equals("Running")) {
			final String checkFolderPath = jtextfiled.getText();
			jtextfiled.setEditable(false);
			LeakingPathAleartTextarea.setText("");
			CommonUserInplyArea.setText("");
			progressbar.setValue(0);
			if(checkFolderPath.length()>0) {
				b1.setEnabled(false);
				b2.setEnabled(false);
				inputBtn1.setEnabled(false);
				final SwingWorker<DefaultTableModel, Void> worker1 = new SwingWorker<DefaultTableModel, Void>() {
					protected DefaultTableModel doInBackground() throws Exception {
							AlarmLog.LeakingPathAleartTextarea = LeakingPathAleartTextarea;
							AlarmLog.CommonUserInplyArea = CommonUserInplyArea;
							Test.sdkPlatform = System.getProperty("user.dir")+"\\platforms";
							Test.testSingleApk(checkFolderPath);
							
						return null;
					}
					protected void done() {
						// TODO Auto-generated method stub
						System.out.println("The background method finished");			
						AlarmLog.updateToScreen();
						jtextfiled.setEditable(true);
						b1.setEnabled(true);
						b2.setEnabled(true);
						inputBtn1.setEnabled(true);
						progressbar.setValue(100);
					}
					};
				worker1.execute();
				new SwingWorker<DefaultTableModel, Void>() {
					protected DefaultTableModel doInBackground() throws Exception {
						// TODO Auto-generated method stub
						int value = progressbar.getValue();
						while (value < 100) {
							if(!worker1.isDone()) {
								if (value < 80)
									value++;
								progressbar.setValue(value);
								Thread.sleep(100);
							}
							else {
								value++;
								progressbar.setValue(value);
								Thread.sleep(50);
							}
						}
						return null;
					}
				}.execute();
			
			}
			else {
				JOptionPane.showMessageDialog(null,"Please Inpute the folder Path of Apk file��","EROR",JOptionPane.ERROR_MESSAGE);
				jtextfiled.setEditable(true);
			}
			
		}
		else if(e.getActionCommand().equals("Clear")){
			LeakingPathAleartTextarea.setText("");
			CommonUserInplyArea.setText("");
			jtextfiled.setText("");
			progressbar.setValue(0);
			jtextfiled.setEditable(true);
		}
		else if(e.getActionCommand().equals("Browse")) {
			fileChooser.setApproveButtonText("Confirm");
			fileChooser.setDialogTitle("Open the folder");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			ApkFileFilter fileFilter = new ApkFileFilter();
			fileChooser.addChoosableFileFilter(fileFilter);
			
			result = fileChooser.showOpenDialog(frame);
		
			if(result==JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
				String filename = file.getAbsolutePath();
				jtextfiled.setText(filename);
				LeakingPathAleartTextarea.setText("");
				CommonUserInplyArea.setText("");
			}
		}
		else if(e.getSource().equals(item1)) {
			ConfigurationFrame cframe = new ConfigurationFrame();
			
		}
	}
	class ApkFileFilter extends FileFilter {  
	    public String getDescription() {  
	        return "*.apk";  
	    }  
	  
	    public boolean accept(File file) {  
	        String name = file.getName();  
	        return name.toLowerCase().endsWith(".apk");  
	    }  
	}    
}
