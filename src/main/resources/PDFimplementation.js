

globalThis.createPDFfrom2DArray = function(tableData) {
    const doc = new PDFDocument()
    const pageWidth = 595
    const pageHeight = 842
    const margin = 40
    const rowHeight = 30
    const colWidth = 100
    const fontSize = 12
    const fontName = "TiRo"

    let currentPage = doc.addPage([0, 0, pageWidth, pageHeight], 0, null, "")
    doc.insertPage(0, currentPage)
    let page = doc.loadPage(0)

    let y = pageHeight - margin

    for (let row = 0; row < tableData.length; row++) {
        const cols = tableData[row]
        y -= rowHeight

        if (y < margin) {
            // Create a new page if we run out of space
            const newPageNum = doc.countPages()
            currentPage = doc.addPage([0, 0, pageWidth, pageHeight], 0, null, "")
            doc.insertPage(newPageNum, currentPage)
            page = doc.loadPage(newPageNum)
            y = pageHeight - margin - rowHeight
        }

        for (let col = 0; col < cols.length; col++) {
            const x = margin + col * colWidth
            const text = cols[col]

            const annot = page.createAnnotation("FreeText")
            annot.setRect([x, y, x + colWidth, y + rowHeight])
            annot.setContents(text)
            annot.setDefaultAppearance(fontName, fontSize, [0])
        }
    }

    page.update()

    // Export the PDF
    const buffer = doc.saveToBuffer().asUint8Array()
    Polyglot.export("Buffer", buffer)
}



