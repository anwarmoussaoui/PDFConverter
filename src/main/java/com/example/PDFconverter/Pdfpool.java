package com.example.PDFconverter;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component

public class Pdfpool {
    private final BlockingQueue<Context> contexts;

    private static Map<String, String> getLanguageOptions() {

        Map<String, String> options = new HashMap<>();

        options.put("js.ecmascript-version", "2023");
        options.put("js.top-level-await", "true");
        options.put("js.webassembly", "true");
        options.put("js.commonjs-require", "true");
        options.put("js.mle-mode", "true");
        options.put("js.esm-eval-returns-exports", "true");
        options.put("js.unhandled-rejections", "throw");
        options.put("js.commonjs-require-cwd", Paths.get("./src/main/resources").toAbsolutePath().toString());
        return options;
    }
    public Pdfpool() throws IOException {
        int maxThreads = Runtime.getRuntime().availableProcessors();
        contexts = new LinkedBlockingQueue<>(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            Context context = Context.newBuilder("js", "wasm")
                    .allowHostAccess(HostAccess.ALL)
                    .allowIO(true)
                    .option("engine.WarnInterpreterOnly", "false")
                    .option("js.esm-eval-returns-exports", "true")
                    .option("js.text-encoding", "true")
                    .option("js.unhandled-rejections", "throw")
                    .allowAllAccess(true)
                    .allowHostClassLookup(s -> true)
                    .options(getLanguageOptions())
                    .build();

            byte[] wasmfile = Files.readAllBytes(Paths.get("./src/main/resources/mupdf-wasm.wasm"));
            context.eval(Source.newBuilder("js", Pdfpool.class.getResource("/polyfills.js"))
                    .mimeType("application/javascript+module")
                    .build());
            context.getBindings("js").putMember("wasmBinary", wasmfile);
            context.eval(Source.newBuilder("js", Pdfpool.class.getResource("/mud.js"))
                    .mimeType("application/javascript+module")
                    .build());
            contexts.add(context);
        }
    }

    public Context getContext() {
        try {
            return contexts.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    void release(Context context) {
        contexts.add(context);
    }
}
