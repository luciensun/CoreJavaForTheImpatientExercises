package chapter03.exercises;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * get data from or set data to clipboard
 * 
 * @author lucienSun
 *
 */
public class ClipboardInfoUtil {
	
	private Clipboard clipBoard;
	
	ClipboardInfoUtil() {
		clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}
	
	public String getStringFlavor() throws UnsupportedFlavorException, IOException {
		Transferable transferable = clipBoard.getContents(null);
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			return (String) transferable
					.getTransferData(DataFlavor.stringFlavor);
		} else {
			return null;
		}
	}
	
	public int getNumberOfRows(String content) {
		int rowCounts = 0;
		Pattern pat = Pattern.compile("/\\*([\\s|\\S])*\\*/");
		Matcher mat = pat.matcher(content);
		//System.out.println("whether matche the given pattern or not ===> " + mat.find());
		String newContent = mat.replaceAll("");
		String[] rows = newContent.split("\n");
		for (String row : rows) {
			if (row.trim().length() > 0 && ! row.trim().startsWith("//")) {
				rowCounts++;
			}
		}
		return rowCounts;
	}
	
	public void setStringFlavor(String contents) {
		clipBoard.setContents(new StringSelection(contents), null);
	}
	public static void main(String[] args) {
		ClipboardInfoUtil clipboardInfoUtil = new ClipboardInfoUtil();

		try {
			String contents = clipboardInfoUtil.getStringFlavor();
			System.out.println(contents);
			
			System.out.println("The content is " + clipboardInfoUtil.getNumberOfRows(contents) + " rows ");
			
		} catch (UnsupportedFlavorException | IOException e) {
			System.err.println(e.getMessage());
		}

	}
}
