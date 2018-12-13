package ca.saultstemarie.pdfflattener;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class FlattenPDF {

	public static void main(String[] args) {
		
		// Settings
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
		
		/*
		 * Get the source file
		 */
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
		fileChooser.setAcceptAllFileFilterUsed(false);
		
		int returnValue = fileChooser.showOpenDialog(null);
		
		if (returnValue != JFileChooser.APPROVE_OPTION) {
			JOptionPane.showMessageDialog(null, "No source file selected.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		File sourceFile = fileChooser.getSelectedFile();
		
		/*
		 * Get the destination file
		 */

		File destinationFile = new File(sourceFile.getAbsolutePath() + File.separator + "FLAT-" + sourceFile.getName());
		
		fileChooser.setSelectedFile(destinationFile);
		
		returnValue = fileChooser.showSaveDialog(null);
		
		if (returnValue != JFileChooser.APPROVE_OPTION) {
			JOptionPane.showMessageDialog(null, "No destination file selected.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		destinationFile = fileChooser.getSelectedFile();
		
		if (sourceFile.getPath().equals(destinationFile.getPath()) && sourceFile.getName().equals(destinationFile.getName())) {
			JOptionPane.showMessageDialog(null, "You cannot select the same file as both the source and the destination.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		/*
		 * Do the flattening
		 */

		returnValue = flattenPDF (sourceFile, destinationFile);
		
		if (returnValue == 0) {
			JOptionPane.showMessageDialog(null, "The PDF file was flattened successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			JOptionPane.showMessageDialog(null, "An error occurred while flattening the file.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static int flattenPDF (File sourceFile, File destinationFile) {
		
		PDDocument sourceDoc = null;
		PDDocument destDoc = new PDDocument();
		
		try {
		
			sourceDoc = PDDocument.load(sourceFile);
			PDFRenderer pdfRenderer = new PDFRenderer(sourceDoc);
			
			PDPageTree pageTree = sourceDoc.getDocumentCatalog().getPages();
			
			for (int i = 0; i < pageTree.getCount(); i += 1) {
				
				BufferedImage img = pdfRenderer.renderImageWithDPI(i, 300, ImageType.RGB);
				
				PDPage imagePage = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
				destDoc.addPage(imagePage);
				
				PDImageXObject imgObj = LosslessFactory.createFromImage(destDoc, img);
							
				PDPageContentStream imagePageContentStream = new PDPageContentStream(destDoc, imagePage);
				imagePageContentStream.drawImage(imgObj, 0, 0);
				
				imagePageContentStream.close();
			}
			
			
			destDoc.save(destinationFile);
			
		
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return 1;
		}
		finally {
			
			try {
				sourceDoc.close();
			}
			catch (Exception e) {
				// ignore
			}
			
			try {
				destDoc.close();
			}
			catch (Exception e) {
				// ignore
			}
		}
		
		return 0;
	}
}
