package com.blackducksoftware.integration.hub.detect.workflow.bdio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.detect.configuration.DetectConfiguration;
import com.blackducksoftware.integration.hub.detect.configuration.DetectProperty;
import com.blackducksoftware.integration.hub.detect.configuration.PropertyAuthority;
import com.blackducksoftware.integration.hub.detect.exception.DetectUserFriendlyException;
import com.blackducksoftware.integration.hub.detect.exitcode.ExitCodeType;
import com.blackducksoftware.integration.hub.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackducksoftware.integration.hub.detect.workflow.codelocation.DetectCodeLocation;
import com.blackducksoftware.integration.hub.detect.workflow.codelocation.FileNameUtils;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.DependencyGraph;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.util.IntegrationEscapeUtil;
import com.synopsys.integration.util.NameVersion;

public class AggregateBdioCreator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SimpleBdioFactory simpleBdioFactory;
    private final IntegrationEscapeUtil integrationEscapeUtil;
    private final CodeLocationNameManager codeLocationNameManager;
    private final DetectConfiguration detectConfiguration;
    private final DetectBdioWriter detectBdioWriter;

    public AggregateBdioCreator(final SimpleBdioFactory simpleBdioFactory, final IntegrationEscapeUtil integrationEscapeUtil,
        final CodeLocationNameManager codeLocationNameManager, final DetectConfiguration detectConfiguration, DetectBdioWriter detectBdioWriter) {this.simpleBdioFactory = simpleBdioFactory;
        this.integrationEscapeUtil = integrationEscapeUtil;
        this.codeLocationNameManager = codeLocationNameManager;
        this.detectConfiguration = detectConfiguration;
        this.detectBdioWriter = detectBdioWriter;
    }

    public File createAggregateBdioFile(File sourcePath, File bdioDirectory, final List<DetectCodeLocation> codeLocations, NameVersion projectNameVersion) throws DetectUserFriendlyException {
        final DependencyGraph aggregateDependencyGraph = createAggregateDependencyGraph(sourcePath, codeLocations);

        final SimpleBdioDocument aggregateBdioDocument = createAggregateSimpleBdioDocument(projectNameVersion, aggregateDependencyGraph);
        final String filename = String.format("%s.jsonld", integrationEscapeUtil.escapeForUri(detectConfiguration.getProperty(DetectProperty.DETECT_BOM_AGGREGATE_NAME, PropertyAuthority.None)));
        final File aggregateBdioFile = new File(bdioDirectory, filename);

        detectBdioWriter.writeBdioFile(aggregateBdioFile, aggregateBdioDocument);

        return aggregateBdioFile;
    }


    private SimpleBdioDocument createAggregateSimpleBdioDocument(NameVersion projectNameVersion, final DependencyGraph dependencyGraph) {
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(new Forge("/", "/", ""), projectNameVersion.getName(), projectNameVersion.getVersion());
        final String codeLocationName = codeLocationNameManager.createAggregateCodeLocationName(projectNameVersion);
        return simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectNameVersion.getName(), projectNameVersion.getVersion(), projectExternalId, dependencyGraph);
    }

    private DependencyGraph createAggregateDependencyGraph(File sourcePath, final List<DetectCodeLocation> codeLocations) {
        final MutableDependencyGraph aggregateDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();

        for (final DetectCodeLocation detectCodeLocation : codeLocations) {
            final Dependency codeLocationDependency = createAggregateDependency(sourcePath, detectCodeLocation);
            aggregateDependencyGraph.addChildrenToRoot(codeLocationDependency);
            aggregateDependencyGraph.addGraphAsChildrenToParent(codeLocationDependency, detectCodeLocation.getDependencyGraph());
        }

        return aggregateDependencyGraph;
    }

    private Dependency createAggregateDependency(File sourcePath, final DetectCodeLocation codeLocation) {
        String name = null;
        String version = null;
        try {
            name = codeLocation.getExternalId().name;
            version = codeLocation.getExternalId().version;
        } catch (final Exception e) {
            logger.warn("Failed to get name or version to use in the wrapper for a code location.", e);
        }
        final ExternalId original = codeLocation.getExternalId();
        final String codeLocationSourcePath = codeLocation.getSourcePath();
        final String bomToolType = codeLocation.getDetectorType().toString();
        final String relativePath = FileNameUtils.relativize(sourcePath.getAbsolutePath(), codeLocationSourcePath);
        final List<String> externalIdPieces = new ArrayList<>();
        externalIdPieces.addAll(Arrays.asList(original.getExternalIdPieces()));
        externalIdPieces.add(relativePath);
        externalIdPieces.add(bomToolType);
        final String[] pieces = externalIdPieces.toArray(new String[externalIdPieces.size()]);
        return new Dependency(name, version, new ExternalIdFactory().createModuleNamesExternalId(original.forge, pieces));
    }
}
