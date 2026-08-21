package edu.kit.ifbc.editor;

import edu.kit.cbc.common.corc.FileUtil;
import edu.kit.cbc.common.corc.cbcmodel.CbCFormula;
import edu.kit.cbc.common.corc.parsing.TokenSource;
import edu.kit.cbc.common.corc.parsing.lexer.Lexer;
import edu.kit.cbc.common.corc.parsing.program.ProgramLexer;
import edu.kit.cbc.common.corc.parsing.program.ProgramParser;
import edu.kit.cbc.common.corc.proof.ProofContext;
import edu.kit.cbc.projects.files.controller.FilesController;
import edu.kit.ifbc.common.ifbcmodel.LatticeResultContext;
import edu.kit.ifbc.common.ifbcmodel.IFbCContext;
import edu.kit.ifbc.common.ifbcmodel.IFbCFormula;
import io.micronaut.serde.jackson.JacksonJsonMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.Getter;

@Singleton
public class VerificationJob extends Thread {

    private static final String LOGGER_FORMAT = "%s %s\n";

    @Getter private String log;
    @Getter private boolean hasResult = false;
    private HashSet<Function<String, Boolean>> listeners;

    private Optional<String> projectId;
    @Getter private IFbCFormula formula;
    private FilesController filesController;
    private Runnable onFinished;

    private static final Logger LOGGER = Logger.getGlobal();

    VerificationJob(Optional<String> projectId, IFbCFormula formula, FilesController filesController, Runnable onFinished) throws IOException {
        log = "";
        listeners = new HashSet<Function<String, Boolean>>();
        this.projectId = projectId;
        this.formula = formula;
        this.filesController = filesController;
        this.onFinished = onFinished;

        log("confidentiality check initialized");
    }

    public void run() {
        log("ifbc check started");
        IFbCContext context = formula.prove();
        hasResult = true;

        LatticeResultContext confidentialityResult = context.getConfidentiality();
        LatticeResultContext integrityResult = context.getIntegrity();
        
        if (confidentialityResult != null) {
            if (confidentialityResult.isSuccessfull()) {
                formula.setConfidential(true);
                log("all statements were successfully for confidentiality!");
            } else {
                formula.setConfidential(false);
                log("WebCorC was unable to check for confidentiality. See the log for further information...");
            }
        }

        if (integrityResult != null) {
            if (integrityResult.isSuccessfull()) {
                formula.setIntegral(true);
                log("all statements were successfully for integrity!");
            } else {
                formula.setIntegral(false);
                log("WebCorC was unable to check for integrity. See the log for further information...");
            }
        }
        log("ifbc check complete");

        //Keep job output and result available for some time before it is deleted
        try {
            Thread.sleep(Duration.ofMinutes(60));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onFinished.run();
    }

    public void addListener(Function<String, Boolean> listener) {
        listeners.add(listener);
    }

    private void log(String message) {
        log += String.format(LOGGER_FORMAT, this.getCurrentTimestamp(), message);

        //Call all listeners. The listener returns true if it detects that its WebSocket connection was closed,
        //so it will be removed from the listener pool
        listeners.removeIf(l -> l.apply(message));
    }

    private String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'['HH:mm:ss']'");
        return LocalTime.now().format(formatter);
    }

    private void listDirectory(Path dir) {
        System.out.println("LISTING DIRECTORY: " + dir);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            System.out.println("Error: Path is not a valid directory: " + dir);
            return;
        }

        try (Stream<Path> stream = Files.list(dir)) {

            stream.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    fileName += "/";
                }
                System.out.println(fileName);
            });

        } catch (IOException e) {
            System.err.println("Failed to read directory: " + e.getMessage());
        }
    }
}
