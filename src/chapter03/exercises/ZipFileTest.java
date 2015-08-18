package chapter03.exercises;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

/**
 * 通过将 c:\temp 目录下的文件 移动 到 c:\tmp\temp.zip 目录下来达到将特定目录下所有文件压缩的目的
 * 也可以是将 c:\temp\a.java 移动到 c:\tmp\temp.zip 来达到将特定文件压缩的目的
 * @author lucienSun
 *
 */
public class ZipFileTest{
	public static void main(String[] args) throws IOException {
		//ZipFile zipFile = new ZipFile("c:" + File.separator + "temp" +  File.separator + "fabfafe.abc.txtf");
		ZipFile zipFile = new ZipFile("c:" + File.separator + "temp");
		
		zipFile.zipTo("c:" + File.separator + "tmp");
		
	}

}
