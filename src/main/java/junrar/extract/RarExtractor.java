package junrar.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junrar.Archive;
import junrar.exception.RarException;
import junrar.rarfile.FileHeader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * extract an archive to the given location
 * 
 * @author edmund wagner
 * 
 */
public class RarExtractor {

	private static Log logger = LogFactory.getLog(RarExtractor.class);

	public void extractArchive(String archive, String destination) throws RarException, IOException {
		extractArchive(new File(archive), new File(destination));
	}

	public void extractArchive(File archive, File destination) throws RarException, IOException {
		Archive arch = null;
		try {
			arch = new Archive(archive);
		}
		catch (RarException re) {
			logger.error(re);
			throw re;
		}
		catch (IOException ioe) {
			logger.error(ioe);
			throw ioe;
		}
		if (arch != null) {
			if (arch.isEncrypted()) {
				logger.warn("Unsupported encrypted archive " + archive.getName());
				try {
					arch.close();
				}
				catch (Exception e) {
					logger.warn(e);
				}
				return;
			}
			else {
				logger.info("Extracting from " + archive.getName());
			}
			try {
				FileHeader fh = null;
				while (true) {
					fh = arch.nextFileHeader();
					if (fh == null) {
						break;
					}
					String fileNameString = fh.getFileNameString();
					if (fh.isEncrypted()) {
						logger.warn("Unsupported encrypted file " + fileNameString);
						continue;
					}
					OutputStream stream = null;
					try {
						if (fh.isDirectory()) {
							createDirectory(fh, destination);
						}
						else {
							logger.info("Extracting  " + fileNameString);
							File f = createFile(fh, destination);
							stream = new FileOutputStream(f);
							arch.extractFile(fh, stream);
						}
					}
					catch (IOException ioe) {
						logger.error("Error extracting  " + fileNameString, ioe);
						throw ioe;
					}
					catch (RarException re) {
						logger.error("Error extracting  " + fileNameString, re);
						throw re;
					}
					finally {
						try {
							if (stream != null) {
								stream.close();
							}
						}
						catch (Exception e) {
							logger.warn(e);
						}
					}
				}
			}
			finally {
				try {
					arch.close();
					logger.info("Extraction completed.");
				}
				catch (Exception e) {
					logger.warn(e);
				}
			}
		}
	}

	private File createFile(FileHeader fh, File destination) {
		File f = null;
		String name = null;
		if (fh.isFileHeader() && fh.isUnicode()) {
			name = fh.getFileNameW();
		}
		else {
			name = fh.getFileNameString();
		}
		f = new File(destination, name);
		if (!f.exists()) {
			try {
				f = makeFile(destination, name);
			}
			catch (IOException e) {
				logger.error("Error creating the new file  " + f.getName(), e);
			}
		}
		return f;
	}

	private static File makeFile(File destination, String name) throws IOException {
		String[] dirs = name.split("\\\\");
		if (dirs == null) {
			return null;
		}
		String path = "";
		int size = dirs.length;
		if (size == 1) {
			return new File(destination, name);
		}
		else if (size > 1) {
			for (int i = 0; i < dirs.length - 1; i++) {
				path = path + File.separator + dirs[i];
				new File(destination, path).mkdir();
			}
			path = path + File.separator + dirs[dirs.length - 1];
			File f = new File(destination, path);
			f.createNewFile();
			return f;
		}
		else {
			return null;
		}
	}

	private static void createDirectory(FileHeader fh, File destination) {
		File f = null;
		if (fh.isDirectory() && fh.isUnicode()) {
			f = new File(destination, fh.getFileNameW());
			if (!f.exists()) {
				makeDirectory(destination, fh.getFileNameW());
			}
		}
		else if (fh.isDirectory() && !fh.isUnicode()) {
			f = new File(destination, fh.getFileNameString());
			if (!f.exists()) {
				makeDirectory(destination, fh.getFileNameString());
			}
		}
	}

	private static void makeDirectory(File destination, String fileName) {
		String[] dirs = fileName.split("\\\\");
		if (dirs == null) {
			return;
		}
		String path = "";
		for (String dir : dirs) {
			path = path + File.separator + dir;
			new File(destination, path).mkdir();
		}

	}
}
