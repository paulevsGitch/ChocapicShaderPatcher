package paulevs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ShaderPatcher {
	public static final String VERSION = "patcher-0.1";
	private static final byte[] BUFFER = new byte[1024];
	
	public static void main(String[] args) throws URISyntaxException, IOException {
		if (args.length != 1) {
			System.out.println("Usage: " + getJarName() + " path-to-shaderpack.zip");
			return;
		}
		
		if (!args[0].endsWith(".zip")) {
			System.out.println("File/Path " + args[0] + " is not a zip archive!");
			return;
		}
		
		File file = new File(args[0]);
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
		File temp = new File("./temp");
		temp.mkdir();
		System.out.println("Unzipping...");
		unzip(file, temp);
		System.out.println("Patching fog...");
		patch();
		System.out.println("Packing...");
		zipFile(temp, args[0].substring(0, args[0].lastIndexOf('.')) + "_patched.zip");
		System.out.println("Cleanup...");
		delete(temp);
		System.out.println("Done!");
	}
	
	private static void patch() throws IOException {
		File input = new File("./temp/shaders/world1/composite2.fsh");
		if (input.exists()) {
			List<String> lines = readFile(input);
			
			int index = 0;
			for (int i = 0; i < 50; i++) {
				if (lines.get(i).startsWith("#define TORCH")) {
					index = i;
					break;
				}
			}
			
			String line = lines.get(index);
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
			
			writeFile(lines, input);
		}
		else {
			System.out.println("File shaders/world1/composite2.fsh doesn't exist!");
		}
		
		input = new File("./temp/shaders/world1/composite5.fsh");
		if (input.exists()) {
			List<String> lines = readFile(input);
			int index = 0;
			
			for (int i = index; i < lines.size(); i++) {
				if (lines.get(i).startsWith("		gl_FragData[0]")) {
					index = i;
					break;
				}
				index ++;
			}
			
			String line = lines.get(index);
			line = line.substring(0, line.lastIndexOf(';')) + " * vec4(biomeColor, 1);";
			lines.set(index, line);
			lines.add(index, "		vec3 biomeColor = gl_Fog.color.rgb * 10;");
			
			writeFile(lines, input);
		}
		else {
			System.out.println("File shaders/world1/composite5.fsh doesn't exist!");
		}
	}
	
	private static List<String> readFile(File file) throws IOException {
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
	
	private static void writeFile(List<String> lines, File file) throws IOException {
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
	
	private static void delete(File file) {
		if (file.isDirectory()) {
			for (File f: file.listFiles()) {
				delete(f);
			}
		}
		file.delete();
	}
	
	private static void zipFile(File input, String name) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(name));
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (File file: input.listFiles()) {
        	zipFile(file, file.getName(), zipOut);
        }
        zipOut.close();
        fos.close();
	}
	
	private static void zipFile(File input, String name, ZipOutputStream stream) throws IOException {
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
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			stream.write(bytes, 0, length);
		}
		fis.close();
	}
	
	@SuppressWarnings("resource")
	private static void unzip(File input, File output) throws IOException {
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
	
	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
	    File destFile = new File(destinationDir, zipEntry.getName());

	    String destDirPath = destinationDir.getCanonicalPath();
	    String destFilePath = destFile.getCanonicalPath();

	    if (!destFilePath.startsWith(destDirPath + File.separator)) {
	        throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
	    }

	    return destFile;
	}

	private static String getJarName() throws URISyntaxException {
		 String jarPath = ShaderPatcher.class
                 .getProtectionDomain()
                 .getCodeSource()
                 .getLocation()
                 .toURI()
                 .getPath();
		 if (jarPath.contains("/")) {
			 jarPath = jarPath.substring(jarPath.lastIndexOf('/') + 1);
		 }
		 if (jarPath.isEmpty()) {
			 return "patcher.jar";
		 }
		 return jarPath;
	}
}
