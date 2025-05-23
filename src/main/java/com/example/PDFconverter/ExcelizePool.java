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

public class ExcelizePool {
    //private final Context context;
    private final BlockingQueue<Context> contexts;

    public ExcelizePool() throws IOException {
        int maxThreads = Runtime.getRuntime().availableProcessors();
        contexts = new LinkedBlockingQueue<>(maxThreads);
        System.out.println(maxThreads);
        final String INTERNAL_MODULE_URI_HEADER = "oracle:/mle/";

        // Use regular file paths
        byte[] excelizeWasmBytes = Files.readAllBytes(Paths.get("src/main/resources/excelize.wasm"));
        String test = Files.readString(Paths.get("src/main/resources/excelize_test.js"), StandardCharsets.UTF_8);
        String prep = Files.readString(Paths.get("src/main/resources/excelize_prep.js"), StandardCharsets.UTF_8);
       String excelizeLib = Files.readString(Paths.get("src/main/resources/excelize_m.js"), StandardCharsets.UTF_8);

        System.out.println("Executing excelize read...");

        // Configure engine options
        Map<String, String> options = new HashMap<>();
        options.put("js.ecmascript-version", "2023");
        options.put("js.top-level-await", "true");
        options.put("js.webassembly", "true");
        options.put("js.commonjs-require", "true");
        options.put("js.esm-eval-returns-exports", "true");
        options.put("js.unhandled-rejections", "throw");
        options.put("js.text-encoding","true");
        options.put("js.commonjs-require-cwd", Paths.get("./").toAbsolutePath().toString());

        Map<String, String> engineOptions = new HashMap<>();
        engineOptions.put("engine.CompilerThreads", "1");
        engineOptions.put("engine.WarnInterpreterOnly", "false");
        engineOptions.put("engine.MultiTier", "true");
        engineOptions.put("engine.Mode", "throughput");
        for (int i = 0; i < maxThreads; i++) {

            Engine engine = Engine.newBuilder("js", "wasm")
                    .allowExperimentalOptions(true)
                    .options(engineOptions)
                    .build();

            // Build the context
            Context context = Context.newBuilder("js", "wasm")
                    .engine(engine)
                    .allowIO(IOAccess.ALL)
                    .allowAllAccess(true)
                    .allowPolyglotAccess(PolyglotAccess.ALL)
                    .allowExperimentalOptions(true)
                    .allowHostClassLookup(s -> true)
                    .allowHostAccess(HostAccess.ALL)
                    .options(options)
                    .build();

            // Evaluate helper JS libraries
            context.eval(Source.newBuilder("js", prep, "prep.js").build());

            // Evaluate the Excelize WASM module
            Source excelizeModule = Source.newBuilder("js", excelizeLib, "excelize.mjs")
                    .mimeType("application/javascript+module")
                    .uri(URI.create(INTERNAL_MODULE_URI_HEADER + "excelize.mjs"))
                    .build();
            Value excelizeMod = context.eval(excelizeModule);
            context.getPolyglotBindings().putMember("excelize", excelizeMod);
            context.getBindings("js").putMember("wasmBytes", excelizeWasmBytes);

            // Evaluate test script
            context.eval(Source.newBuilder("js", test, "excelize_test.js").build());
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
