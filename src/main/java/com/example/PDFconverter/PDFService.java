package com.example.PDFconverter;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component

public class PDFService {
    private final ExcelizePool excelizePool;
    private final Pdfpool pdfpool;

    public PDFService(ExcelizePool excelizePool, Pdfpool pdfpool) {
        this.excelizePool = excelizePool;
        this.pdfpool = pdfpool;
    }

    public byte [] toPDF(byte [] excelBytes) throws IOException {
        Context context1 = excelizePool.getContext();
        Context context =pdfpool.getContext();
        context1.getBindings("js").putMember("excelFileBytes", excelBytes);
        Value readFunc = context1.getBindings("js").getMember("readExcel");
        readFunc.execute();
        Value bufferArray = context1.getPolyglotBindings().getMember("resultArray");
        Value x =context.getBindings("js").getMember("createPDFfrom2DArray");
        x.execute(bufferArray);
        Value jsBuffer = context.getPolyglotBindings().getMember("Buffer");
        int length = (int) jsBuffer.getArraySize();
        byte[] byteArray = new byte[length];
        for (int i = 0; i < length; i++) {
            int val = jsBuffer.getArrayElement(i).asInt();
            byteArray[i] = (byte) (val & 0xFF);
        }
        pdfpool.release(context);
        excelizePool.release(context1);
        return byteArray;
    }
    public void fromPDF(byte [] pdfBytes){
        Context context =pdfpool.getContext();
        Value x =context.getBindings("js").getMember("frompdf");
        x.execute(pdfBytes);

    }
}
