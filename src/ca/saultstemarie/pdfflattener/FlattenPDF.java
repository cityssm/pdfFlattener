package ca.saultstemarie.pdfflattener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
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
	
	/**
	 * Whether or not debug output is printed to the console.
	 */
	public static final boolean SHOW_DEBUG = true;
	
	/**
	 * The suggested prefix to add to output files. 
	 */
	public static final String DESTINATION_FILENAME_PREFIX = "flat--";
	
	/**
	 * The DPI that should be used when generating images.
	 * Higher DPI increases the memory requirements and output file sizes, but also produces sharper images.
	 */
	public static int IMAGE_DPI = 200;
	

	public static void main(String[] args) {
		
		// Settings
		
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
				
		/*
		 * Set up JFileChooser
		 */
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			// ignore and use default
		}
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
		fileChooser.setAcceptAllFileFilterUsed(false);
		
		/*
		 * Get the source file
		 */
		
		File sourceFile;

		if (args.length >= 1) {
			sourceFile = new File(args[0]);
			if (!sourceFile.exists() || !sourceFile.isFile() || !sourceFile.canRead() || !sourceFile.getName().toLowerCase().endsWith(".pdf")) {
				JOptionPane.showMessageDialog(null, "Invalid source file.\n" + args[0],
						"Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		} else {
			fileChooser.setDialogTitle("Select the Source PDF File to Flatten");
			fileChooser.setApproveButtonText("Set Source PDF");

			int returnValue = fileChooser.showOpenDialog(null);
			
			if (returnValue != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(null, "No Source PDF file selected.", "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
			
			sourceFile = fileChooser.getSelectedFile();
		}
		
		/*
		 * Get the destination file
		 */
		
		File destinationFile;
		
		if (args.length >= 2) {
			destinationFile = new File(args[1]);

			if (!destinationFile.getName().toLowerCase().endsWith(".pdf")) {
				JOptionPane.showMessageDialog(null, "Invalid destination file.\n" +
						args[1], "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
		else {

			destinationFile = new File(sourceFile.getAbsolutePath() + File.separator + DESTINATION_FILENAME_PREFIX + sourceFile.getName());
			
			fileChooser.setDialogTitle("Select the Destination PDF File");
			fileChooser.setApproveButtonText("Set Destination PDF");
			fileChooser.setSelectedFile(destinationFile);
			
			int returnValue = fileChooser.showSaveDialog(null);
			
			if (returnValue != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(null, "No Destination PDF file selected.", "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
			
			destinationFile = fileChooser.getSelectedFile();
		}
		
		if (sourceFile.getPath().equals(destinationFile.getPath()) && sourceFile.getName().equals(destinationFile.getName())) {
			JOptionPane.showMessageDialog(null, "You cannot select the same PDF file as both the source and the destination.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		if (!destinationFile.getName().toLowerCase().endsWith(".pdf")) {
			destinationFile = new File(destinationFile.getAbsolutePath() + ".pdf");
		}
		
		
		/*
		 * Set the DPI
		 */
		
		
		String possibleDPI = JOptionPane.showInputDialog(
				"What is your preferred output image DPI?\n" +
				"The higher the number, the higher the quality, but also, the higher the output file size.  If unsure, 200 gives quite good results.", 
				IMAGE_DPI);
		
		try {
			IMAGE_DPI = Integer.parseInt(possibleDPI);
		}
		catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Invalid DPI.  Using " + IMAGE_DPI + ".", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		
		/*
		 * Do the flattening
		 */
		
		JOptionPane.showMessageDialog(null, "You are about to flatten\n" + 
				sourceFile.getAbsolutePath() + "\n" + 
				"into the new file\n" +
				destinationFile.getAbsolutePath() + "\n" +
				"at " + IMAGE_DPI + " DPI.\n\n" +
				"This will run in the background.  Please be patient as it may take some time.",
				"Ready to Start", 
				JOptionPane.INFORMATION_MESSAGE);

		int returnValue = flattenPDF (sourceFile, destinationFile);
		
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
		
		JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		
		JFrame frame = new JFrame("Flattening PDF");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(progressBar);
		frame.setSize(400, 75);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
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
				
				/*
				 * Update progress bar
				 */
				
				int progress = (int) (((i + 1) / (float) pageCount) * 100);
				progressBar.setValue(progress);
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
			
			frame.dispose();
		}
		
		return 0;
	}

	private static void log(String message) {
		if (SHOW_DEBUG) {
			System.out.println(new Date().toString() + " - " + message);
		}
	}
}
