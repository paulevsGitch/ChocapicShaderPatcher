package paulevs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class ShaderPatcher {
	public static final String VERSION = "patcher-0.3";
	private static final byte[] BUFFER = new byte[1024];
	
	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			new ShaderPatcher(args[0]);
		}
		else {
			new ShaderPatcher();
		}
	}
	
	private ShaderPatcher() throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		JFileChooser choser = new JFileChooser();
		choser.setCurrentDirectory(new File(System.getProperty("user.home")));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Shaderpack File", "zip");
		choser.setFileFilter(filter);
		
		JFrame frame = new JFrame();
		frame.setTitle("Chocapic13 Shaders Patcher");
		
		JPanel panel = new JPanel();
		panel.setLayout(null);
		frame.add(panel);
		
		JTextField pathText = new JTextField();
		pathText.setSize(240, 24);
		pathText.setLocation(10, 10);
		panel.add(pathText);
		
		JButton search = new JButton();
		search.setSize(80, 24);
		search.setText("Search");
		search.setLocation(260, 10);
		panel.add(search);
		
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int result = choser.showOpenDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					pathText.setForeground(Color.BLACK);
					pathText.setText(choser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		
		JCheckBox patchSky = new JCheckBox();
		patchSky.setLocation(10, 44);
		patchSky.setText("Patch Sky");
		patchSky.setSize(80, 24);
		patchSky.setSelected(true);
		panel.add(patchSky);
		
		JCheckBox patchLight = new JCheckBox();
		patchLight.setLocation(100, 44);
		patchLight.setText("Patch Light");
		patchLight.setSize(100, 24);
		patchLight.setSelected(true);
		panel.add(patchLight);
		
		JButton patch = new JButton();
		patch.setSize(80, 24);
		patch.setText("Patch");
		patch.setLocation(260, 44);
		panel.add(patch);
		
		patch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String path = pathText.getText();
				if (path == null || path.isEmpty()) {
					return;
				}
				File file = new File(path);
				if (!file.exists() || !file.isFile() || !file.getName().endsWith(".zip")) {
					pathText.setForeground(Color.RED);
					return;
				}
				boolean sky = patchSky.isSelected();
				boolean light = patchLight.isSelected();
				File temp = new File("./temp-" + VERSION);
				temp.mkdir();
				try {
					unzip(file, temp);
					patch(temp, sky, light);
					zipFile(temp, path.substring(0, path.lastIndexOf('.')) + "_patched.zip");
				}
				catch (IOException e) {}
				delete(temp);
				pathText.setForeground(Color.GREEN);
			}
		});
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
		setSize(frame, panel);
		frame.setLocationRelativeTo(null);
	}
	
	private void setSize(JFrame frame, JPanel panel) {
		Dimension dim = new Dimension();
		for (Component component: panel.getComponents()) {
			int x = component.getX() + component.getWidth();
			int y = component.getY() + component.getHeight();
			dim.width = Math.max(x, dim.width);
			dim.height = Math.max(y, dim.height);
		}
		dim.width += 10 + frame.getInsets().left + frame.getInsets().right;
		dim.height += 10 + frame.getInsets().top + frame.getInsets().bottom;
		frame.setSize(dim);
	}
	
	private ShaderPatcher(String path) throws IOException {
		if (!path.endsWith(".zip")) {
			System.out.println("File/Path " + path + " is not a zip archive!");
			return;
		}
		
		File file = new File(path);
		if (!file.exists()) {
			System.out.println("File doesn't exist!");
			return;
		}
		else if (!file.isFile()) {
			System.out.println("Path is not a file!");
			return;
		}
		
		System.out.println("Running " + VERSION);
		System.out.println("Making temp directory...");
		File temp = new File("./temp-" + VERSION);
		temp.mkdir();
		System.out.println("Unzipping...");
		unzip(file, temp);
		System.out.println("Patching fog...");
		patch(temp, true, true);
		System.out.println("Packing...");
		zipFile(temp, path.substring(0, path.lastIndexOf('.')) + "_patched.zip");
		System.out.println("Cleanup...");
		delete(temp);
		System.out.println("Done!");
	}
	
	private void patch(File temp, boolean sky, boolean light) throws IOException {
		File input = new File(temp, "shaders/world1/composite2.fsh");
		List<String> lines;
		int index = 0;
		String line;
		
		if (input.exists()) {
			lines = readFile(input);
			
			if (light) {
				for (int i = 0; i < 50; i++) {
					if (lines.get(i).startsWith("#define TORCH")) {
						index = i;
						break;
					}
				}
				
				line = lines.get(index);
				line = line.substring(line.indexOf('/'));
				lines.set(index, "#define TORCH_R 0.3 " + line);
				
				index ++;
				line = lines.get(index);
				line = line.substring(line.indexOf('/'));
				lines.set(index, "#define TORCH_G 0.3 " + line);
				
				index ++;
				line = lines.get(index);
				line = line.substring(line.indexOf('/'));
				lines.set(index, "#define TORCH_B 0.3 " + line);
				
				for (int i = 0; i < lines.size(); i++) {
					if (lines.get(i).startsWith("	if (z >=1.0) {")) {
						index = i + 1;
						break;
					}
				}
			}
			
			if (sky) {
				line = lines.get(index);
				line = line.substring(0, line.lastIndexOf(';')) + " * biomeColor;";
				lines.add(index + 1, line);
				lines.set(index, "		vec3 biomeColor = gl_Fog.color.rgb * 10;");
				
				for (int i = index; i < lines.size(); i++) {
					if (lines.get(i).startsWith("		gl_FragData[0].rgb = ")) {
						index = i;
						break;
					}
				}
				
				line = lines.get(index);
				line = line.substring(0, line.lastIndexOf(';')) + " * biomeColor;";
				lines.set(index, line);
			}
			
			if (sky || light) {
				writeFile(lines, input);
			}
		}
		else {
			System.out.println("File shaders/world1/composite2.fsh doesn't exist!");
		}
		
		if (sky) {
			input = new File(temp, "shaders/world1/composite5.fsh");
			if (input.exists()) {
				lines = readFile(input);
				index = 0;
				
				for (int i = index; i < lines.size(); i++) {
					if (lines.get(i).startsWith("		gl_FragData[0]")) {
						index = i;
						break;
					}
					index ++;
				}
				
				line = lines.get(index);
				line = line.substring(0, line.lastIndexOf(';')) + " * vec4(biomeColor, 1);";
				lines.set(index, line);
				lines.add(index, "		vec3 biomeColor = gl_Fog.color.rgb * 10;");
				
				writeFile(lines, input);
			}
			else {
				System.out.println("File shaders/world1/composite5.fsh doesn't exist!");
			}
		}
	}
	
	private List<String> readFile(File file) throws IOException {
		List<String> result = new ArrayList<String>();
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			result.add(line);
		}
		br.close();
		fr.close();
		return result;
	}
	
	private void writeFile(List<String> lines, File file) throws IOException {
		FileWriter fr = new FileWriter(file);
		BufferedWriter br = new BufferedWriter(fr);
		lines.forEach((line) -> {
			try {
				br.append(line + "\n");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
		br.close();
		fr.close();
	}
	
	private void delete(File file) {
		if (file.isDirectory()) {
			for (File f: file.listFiles()) {
				delete(f);
			}
		}
		file.delete();
	}
	
	private void zipFile(File input, String name) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(name));
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (File file: input.listFiles()) {
        	zipFile(file, file.getName(), zipOut);
        }
        zipOut.close();
        fos.close();
	}
	
	private void zipFile(File input, String name, ZipOutputStream stream) throws IOException {
		if (input.isDirectory()) {
			if (name.endsWith("/")) {
				stream.putNextEntry(new ZipEntry(name));
				stream.closeEntry();
			}
			else {
				stream.putNextEntry(new ZipEntry(name + "/"));
				stream.closeEntry();
			}
			File[] children = input.listFiles();
			for (File childFile : children) {
				zipFile(childFile, name + "/" + childFile.getName(), stream);
			}
			return;
		}
		FileInputStream fis = new FileInputStream(input);
		ZipEntry zipEntry = new ZipEntry(name);
		stream.putNextEntry(zipEntry);
		int length;
		while ((length = fis.read(BUFFER)) >= 0) {
			stream.write(BUFFER, 0, length);
		}
		fis.close();
	}
	
	@SuppressWarnings("resource")
	private void unzip(File input, File output) throws IOException {
		FileInputStream fis = new FileInputStream(input);
		ZipInputStream zis = new ZipInputStream(fis);
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {
			File newFile = newFile(output, zipEntry);
			if (zipEntry.isDirectory()) {
				if (!newFile.isDirectory() && !newFile.mkdirs()) {
					throw new IOException("Failed to create directory " + newFile);
				}
			}
			else {
				File parent = newFile.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory " + parent);
				}
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(BUFFER)) > 0) {
					fos.write(BUFFER, 0, len);
				}
				fos.close();
			}
			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
		fis.close();
	}
	
	private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
	    File destFile = new File(destinationDir, zipEntry.getName());

	    String destDirPath = destinationDir.getCanonicalPath();
	    String destFilePath = destFile.getCanonicalPath();

	    if (!destFilePath.startsWith(destDirPath + File.separator)) {
	        throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
	    }

	    return destFile;
	}
}
