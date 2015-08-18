package chapter03.exercises;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
	private String comment;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
	void zip(String src, String dst, List filter) throws Exception {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dst));
		File srcFile = new File(src);
		zip(zos, srcFile, "", filter);
		zos.close();
	}
	
	void zip(ZipOutputStream zos, File srcFile, String base, List filter) throws Exception {
		if (srcFile.exists()) {
			throw new RuntimeException("压缩目录不存在!");
		}
		
		if (srcFile.isDirectory()) {
			File[] files = srcFile.listFiles();
			base = base.length() == 0 ? "":base+"/";
			if (isExists(base, filter)) {
				zos.putNextEntry(new ZipEntry(base));
			}
		}
	}
	
	boolean isExists(String base, List filter) {
		return true;
	}
}
