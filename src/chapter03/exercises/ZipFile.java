package chapter03.exercises;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 通过将 c:\temp 目录下的文件 移动 到 c:\tmp\temp.zip 目录下来达到将特定目录下所有文件压缩的目的 也可以是将
 * c:\temp\a.java 移动到 c:\tmp\temp.zip 来达到将特定文件压缩的目的
 * 
 * @author lucienSun
 *
 */
public class ZipFile extends File {

	private String destinationPath;
	private FileInputStream fis;
	private FileOutputStream fos;
	private ZipOutputStream zos;
	private ZipEntry zipEntry;

	public ZipFile(String pathname) {
		super(pathname);
	}

	public void zipTo(String dstPath) throws IOException {
		// zipName of the destination file
		String zipName = null;
		
		File dstDirectory = new File(dstPath);
		// Creates the directory named by this abstract pathname
		dstDirectory.mkdirs();
		
		// if the ZipFile is a directory then compress to dirName.zip
		// else if the ZipFile is a file then compress to fileName.zip
		if (this.isFile()) {
			System.out.println("this is a file");
			if (this.getName().contains(".")) {

				if (this.getName().indexOf(".") == this.getName().lastIndexOf(
						".")) {
					// fabfafe.txtf compress to fabfafe.zip
					zipName = this.getName().substring(0,
							this.getName().lastIndexOf("."))
							+ ".zip";

				} else {
					// fabfafe.abc.txtf compress to fabfafe.abc.txtf.zip
					zipName = this.getName() + ".zip";

				}
			} else {
				// abbc compress to abbc.zip
				zipName = this.getName() + ".zip";
			}

			// zip self to destination path
			fos = new FileOutputStream(dstPath + File.separator + zipName);
			zos = new ZipOutputStream(fos);
			zipSingleFileTo(zos);

		} else if (this.isDirectory()) {
			
			zipName = this.getName() + ".zip";
			fos = new FileOutputStream(dstPath + File.separator + zipName);
			zos = new ZipOutputStream(fos);
			
			String[] fileNames = this.list();
			for (String fileName : fileNames) {
				ZipFile srcFile = new ZipFile(this.getAbsolutePath() + File.separator + fileName);
				if (srcFile.isFile()) {		
					srcFile.zipSingleFileTo(zos);
				} else if (srcFile.isDirectory()) {
					srcFile.zipSingleFileTo(zos);
				}
			}
					
		}

		zos.close();
		fos.close();
		// System.out.println(dstpath + File.separator + zipName);
	}
	
	// this method only zip a singleFile to destination path(just apply to a single file no path)
	public void zipSingleFileTo(ZipOutputStream zos) throws IOException {

		// if the ZipFile is a directory then compress to dirName.zip
		// else if the ZipFile is a file then compress to fileName.zip
		if (this.isFile()) {
		
			// zip self to destination path
			fis = new FileInputStream(this);
			zipEntry = new ZipEntry(this.getName());

			zos.putNextEntry(zipEntry);
			int count = 0;
			byte[] buffer = new byte[10240];
			while ((count = fis.read(buffer)) != -1) {
				zos.write(buffer, 0, count);
			}
			zos.flush();
			zos.closeEntry();
			fis.close();
		} else if (this.isDirectory()) {
			zos.putNextEntry(new ZipEntry(this.getName() + "/"));
			String[] fileNames = this.list();
			for (String fileName : fileNames) {
				ZipFile srcFile = new ZipFile(this.getAbsolutePath() + File.separator + fileName);
				if (srcFile.isFile()) {		
					srcFile.zipSingleFileTo(zos);
				} else if (srcFile.isDirectory()) {
					srcFile.zipSingleFileTo(zos);
				}
			}
			System.out.println("this is a directory");
			// do nothing at all
			
		}


	}
}
