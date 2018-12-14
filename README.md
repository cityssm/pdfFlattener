# PDF Flattener

Properly redacting PDF files can be an expensive job.
Oftentimes, files are released with the impression that they are redacted because you can *see* the redacted text.
In reality, those big black rectangles can be removed quite easily with the [right editor](https://inkscape.org/),
leaving behind the confidential text that should have been removed.

Preseving PDF form content can be a concern as well.  How can we be assured that form content is not altered after we receive it?

When a PDF is flattened, the layered content, like those floating black rectangles and form fields, are all combined into a single image layer.

There are many high cost solutions to achieve this.
It can also be done with freely available PDF libraries.
This solution uses [Apache PDFBox](https://pdfbox.apache.org/), which is available under [the Apache License](https://www.apache.org/licenses/).
This project is licensed under the same license.

## Libraries Used

This project, along with all the libraries it uses are licensed under [the Apache License](https://www.apache.org/licenses/).

- [Apache PDFBox](https://pdfbox.apache.org/)
- [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/)

For convenience, these binaries are bundled into the main build.

## Requirements

- PDFBox requires at least Java 8 to run.  This project is compiled to work with Java 8 or better.
- Memory.  Depending on the size of your PDF, it may take over 1 GB of memory to process it.

## Usage

Check the Releases for an executable JAR file.  Note that the executable JAR makes use of Java's `JFileChooser` class, so a GUI is required.

Alternatively, you can include the JAR file in your project, and call the static `flattenPDF(File src, File dest)` method. 
