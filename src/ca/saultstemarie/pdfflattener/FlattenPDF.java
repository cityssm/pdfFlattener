package ca.saultstemarie.pdfflattener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class FlattenPDF {
	
	public static int IMAGE_DPI = 200;
	public static boolean SHOW_DEBUG = true;

	public static void main(String[] args) {
		
		// Settings
		
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			// ignore and use default
		}
		
		/*
		 * Get the source file
		 */
		
		JFileChooser fileChooser = new JFileChooser();
		
		fileChooser.setDialogTitle("Select the Source PDF File to Flatten");
		fileChooser.setApproveButtonText("Set Source PDF");
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
		fileChooser.setAcceptAllFileFilterUsed(false);
		
		int returnValue = fileChooser.showOpenDialog(null);
		
		if (returnValue != JFileChooser.APPROVE_OPTION) {
			JOptionPane.showMessageDialog(null, "No Source PDF file selected.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		File sourceFile = fileChooser.getSelectedFile();
		
		/*
		 * Get the destination file
		 */

		File destinationFile = new File(sourceFile.getAbsolutePath() + File.separator + "FLAT-" + sourceFile.getName());
		
		fileChooser.setDialogTitle("Select the Destination PDF File");
		fileChooser.setApproveButtonText("Set Destination PDF");
		fileChooser.setSelectedFile(destinationFile);
		
		returnValue = fileChooser.showSaveDialog(null);
		
		if (returnValue != JFileChooser.APPROVE_OPTION) {
			JOptionPane.showMessageDialog(null, "No Destination PDF file selected.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		destinationFile = fileChooser.getSelectedFile();
		
		if (sourceFile.getPath().equals(destinationFile.getPath()) && sourceFile.getName().equals(destinationFile.getName())) {
			JOptionPane.showMessageDialog(null, "You cannot select the same PDF file as both the source and the destination.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		/*
		 * Do the flattening
		 */
		
		JOptionPane.showMessageDialog(null, "You are about to flatten\n" + 
				sourceFile.getAbsolutePath() + "\n" + 
				"into the new file\n" +
				destinationFile.getAbsolutePath() + "\n\n" +
				"This will run in the background.  Please be patient as it may take some time.",
				"Ready to Start", 
				JOptionPane.INFORMATION_MESSAGE);

		returnValue = flattenPDF (sourceFile, destinationFile);
		
		if (returnValue == 0) {
			JOptionPane.showMessageDialog(null, "The PDF file was flattened successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			JOptionPane.showMessageDialog(null, "An error occurred while flattening the file.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	
	/**
	 * Takes a PDF file and flattens it into a new PDF file.
	 * The new PDF file is a series of images generated from the source PDF file.
	 * No metadata from the source PDF file is copied into the new PDF file.
	 * @param sourceFile - The source PDF file to be flattened.
	 * @param destinationFile - The output PDF file.
	 * @return - 0 if successful
	 */
	public static int flattenPDF (File sourceFile, File destinationFile) {
		
		long startMillis = System.currentTimeMillis();
		
		PDDocument sourceDoc = null;
		PDDocument destDoc = new PDDocument();
		
		try {

			long maxAvailableMemoryInMB = Runtime.getRuntime().maxMemory() / 1024 / 1024;
			
			// If less than 1GB available, be more memory conscious
			if (maxAvailableMemoryInMB < 2048) {
				
				log("Max memory limited to " + maxAvailableMemoryInMB + "MB. Resource cache will be disabled.");
				
				sourceDoc = PDDocument.load(sourceFile, MemoryUsageSetting.setupTempFileOnly());

				sourceDoc.setResourceCache(new DefaultResourceCache() {
					public void put (COSObject indirect, PDXObject xobject) {
						// discard
					}
				});
			}
			else {
				sourceDoc = PDDocument.load(sourceFile);
			}
			
			PDFRenderer pdfRenderer = new PDFRenderer(sourceDoc);
			
			final int pageCount = sourceDoc.getDocumentCatalog().getPages().getCount();
			
			log(pageCount + " page" + (pageCount == 1 ? "" : "s") + " to flatten.");
			
			for (int i = 0; i < pageCount; i += 1) {
				
				log("Flattening page " + (i + 1) + " of " + pageCount + "...");
				
				BufferedImage img = pdfRenderer.renderImageWithDPI(i, IMAGE_DPI, ImageType.RGB);
				
				log("  Image rendered in memory (" + img.getWidth() + "x" + img.getHeight() + " " + IMAGE_DPI + "DPI).  Adding to PDF...");
				
				PDPage imagePage = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
				destDoc.addPage(imagePage);
				
				PDImageXObject imgObj = LosslessFactory.createFromImage(destDoc, img);
							
				PDPageContentStream imagePageContentStream = new PDPageContentStream(destDoc, imagePage);
				imagePageContentStream.drawImage(imgObj, 0, 0);
				
				log("  Image added successfully.");
				
				/*
				 * Close and clear images
				 */
				
				imagePageContentStream.close();
				
				imgObj = null;
				
				img.flush();
				img = null;
			}
			
			log("New flattened PDF created in memory.");
			
			
			/*
			 * Remove links to the source document before saving.
			 * (Get back as much memory as possible.)
			 */

			pdfRenderer = null;
			
			sourceDoc.close();
			sourceDoc = null;
			
			/*
			 * Write the new PDF file
			 */
			
			log("Saving new flattened PDF...");
			
			destDoc.save(destinationFile);
			destDoc.close();
			
			log("Saved successfully (" + ((System.currentTimeMillis() - startMillis) / 1000.0) + " seconds).");
		}
		catch (Exception e) {
			log("Error: " + e.getMessage());
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
	
	private static void log(String message) {
		if (SHOW_DEBUG) {
			System.out.println(new Date().toString() + " - " + message);
		}
	}
}
