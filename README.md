# PDF Flattener

Properly redacting PDF files is an expensive job.
Oftentimes, files are released with the impression that they are redacted.
In reality, those big black rectangles can be removed quite easily with the [right editor](https://inkscape.org/),
leaving behind the confidential text that should have been removed.

When a PDF is flattened, the layered content (i.e. those floating black rectangles) is all combined into a single layer.

There are many high cost solutions to achieve this.
It can also be done with freely available PDF libraries.
This solution uses [Apache PDFBox](https://pdfbox.apache.org/), which is available under [the Apache License](https://www.apache.org/licenses/).
This project is licensed under the same license.

## Libraries Used

All libraries used are licensed under [the Apache License](https://www.apache.org/licenses/).

- [Apache PDFBox](https://pdfbox.apache.org/)
- [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/)

For convenience, these binaries are bundled into the main build.