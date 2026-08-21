package edu.kit.ifbc.editor;

import edu.kit.cbc.common.Problem;
import edu.kit.cbc.common.corc.cbcmodel.CbCFormula;
import edu.kit.cbc.common.corc.codegeneration.CodeGenerator;
import edu.kit.ifbc.common.ifbcmodel.Lattice.Level;
import edu.kit.ifbc.common.dto.VariableStateDTO;
import edu.kit.ifbc.common.ifbcmodel.IFbCFormula;
import edu.kit.ifbc.common.ifbcmodel.Lattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.integrity.IntegrityLattice;
import edu.kit.ifbc.editor.lattice.LatticeDTO;
import edu.kit.cbc.editor.llm.LLMClientRegistry;
import edu.kit.cbc.editor.llm.LLMQueryDto;
import edu.kit.cbc.editor.llm.LLMResponse;
import edu.kit.cbc.projects.ProjectService;
import edu.kit.cbc.projects.files.controller.FilesController;
import io.micronaut.context.annotation.Bean;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.bind.ArgumentBinder.BindingResult;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.propagation.MutablePropagatedContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.json.JsonMapper;
import io.micronaut.objectstorage.ObjectStorageException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller("/ifbc/editor")
@ExecuteOn(TaskExecutors.BLOCKING)
public class EditorController {

    // #region agent log
    private static final Logger LOGGER = Logger.getGlobal();
    // #endregion
    private final FilesController filesController;
    private final LLMClientRegistry llmRegistry;
    private final VerificationOrchestrator orchestrator;
    private final JsonMapper jsonMapper;
    private final ProjectService projectService;

    EditorController(ProjectService projectService, FilesController filesController, LLMClientRegistry llmRegistry, VerificationOrchestrator orchestrator, JsonMapper jsonMapper) {
        this.filesController = filesController;
        this.llmRegistry = llmRegistry;
        this.orchestrator = orchestrator;
        this.jsonMapper = jsonMapper;
        this.projectService = projectService;
    }

    private HttpResponse<?> retrieveLattice(String urn, Optional<String> projectId, Supplier<Lattice> defaultLattice) throws IOException {
        if (projectId.isEmpty()) {
            return HttpResponse.ok(new LatticeDTO(null, null, defaultLattice.get()));
        }
        if (!projectService.existsById(projectId.get())) {
            return HttpResponse.notFound(new Problem("about:blank", "Not found", 404,
                String.format("project with id %s was not found", projectId), "about:blank"));
        }

        try {
            Optional<HttpResponse<StreamedFile>> fileResponse = filesController.getFile(projectId.get(), "confidentiallity.lattice");
            if (fileResponse.isEmpty()) {
                return HttpResponse.ok(defaultLattice.get());
            }
            InputStream is = fileResponse.get().body().getInputStream();
            LatticeDTO dto = jsonMapper.readValue(is, Argument.of(LatticeDTO.class));
            return HttpResponse.ok(dto);
        } catch (ObjectStorageException exception) {
            return HttpResponse.ok(new LatticeDTO(null, null, defaultLattice.get()));
        }
    }

    @Get(uri = "/lattice/confidentiality")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> getConfidentialityLattice(@QueryValue Optional<String> projectId) throws IOException {
        return retrieveLattice("confidentiality.lattice", projectId, ConfidentialityLattice::defaultConfidentialityLattice);
    }

    @Post(uri = "/lattice/confidentiality")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> saveConfidentialityLatticeState(
        @QueryValue Optional<String> projectId,
        VariableStateDTO preVariableState,
        VariableStateDTO postVariableState,
        List<Lattice.Level> levels
    ) throws IOException {
        Lattice lattice = new ConfidentialityLattice(levels);
        LatticeDTO dto = new LatticeDTO(
            preVariableState,
            postVariableState,
            lattice
        );
        return HttpResponse.ok(dto);

        // String json = jsonMapper.writeValueAsString(dto);
        // filesController.uploadBytes(json.getBytes(StandardCharsets.UTF_8), projectId, Path.of("confidentiallity.lattice"));
        // return HttpResponse.ok();
    }

    @Get(uri = "/lattice/integrity")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> getIntegrityLattice(@QueryValue Optional<String> projectId) throws IOException {
        return retrieveLattice("integrity.lattice", projectId, IntegrityLattice::new);
    }

    @Post(uri = "/lattice/integrity")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> saveIntegrityLatticeState(
        @QueryValue String projectId,
        VariableStateDTO preVariableState,
        VariableStateDTO postVariableState
    ) throws IOException {
        Lattice lattice = new IntegrityLattice();
        LatticeDTO dto = new LatticeDTO(
            preVariableState,
            postVariableState,
            lattice
        );
        String json = jsonMapper.writeValueAsString(dto);
        filesController.uploadBytes(json.getBytes(StandardCharsets.UTF_8), projectId, Path.of("integrity.lattice"));
        return HttpResponse.ok();
    }

    @Post(uri = "/lattice/validate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> validateLattice(
        List<Lattice.Level> levels
    ) throws IOException {
        try {
            Lattice lattice = new Lattice(levels);
            return HttpResponse.ok(lattice);
        } catch (Lattice.LatticeException exception) {
            return HttpResponse.badRequest(Map.of("error", exception.getMessage()));
        }
    }


    @Post(uri = "/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // public HttpResponse<?> verify(@QueryValue Optional<String> projectId, @Body @Valid IFbCFormula formula) throws IOException {
    public HttpResponse<?> verify(@QueryValue Optional<String> projectId, @Body IFbCFormula formula) throws IOException {
        UUID jobId = orchestrator.addJob(projectId, formula, filesController);
        // Logger.getGlobal().info("is confidential: " + isConfidential);
        // return HttpResponse.ok(isConfidential);
        return HttpResponse.ok(jobId);
    }

    @Get(uri = "/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> getJobs(@QueryValue UUID jobId) {
        IFbCFormula result = orchestrator.getVerificationResult(jobId);
        if (result == null) {
            return HttpResponse.serverError(Problem.JOB_NOT_FINISHED);
        } else {
            return HttpResponse.ok(result);
        }
    }

    // @ServerFilter("/ifbc/editor/**")
    // @Singleton
    // public static class ConfidentialityLatticeFilter {

    //     @RequestFilter
    //     public HttpRequest<?> latticeFilter(
    //         // @QueryValue @Nullable String projectId,
    //         HttpRequest<?> request
    //         // MutablePropagatedContext propagatedContext
    //     ) {
    //         LOGGER.warning("adding attribute");
    //         var lattice = ConfidentialityLattice.defaultLConfidentialityLattice();
    //         request.setAttribute(ConfidentialityLattice.class.getName(), lattice);
    //         return request;
    //     }
    // }

    @Singleton
    public static class ConfidentialityLatticeBinder implements TypedRequestArgumentBinder<ConfidentialityLattice> {
        @Override
        public Argument<ConfidentialityLattice> argumentType() {
            return Argument.of(ConfidentialityLattice.class);
        }

        @Override
        public BindingResult<ConfidentialityLattice> bind(ArgumentConversionContext<ConfidentialityLattice> context, HttpRequest<?> source) {
            Optional<ConfidentialityLattice> attribute = source.getAttribute(ConfidentialityLattice.class.getName(), ConfidentialityLattice.class);
            return () -> attribute;
        }
    }
}
