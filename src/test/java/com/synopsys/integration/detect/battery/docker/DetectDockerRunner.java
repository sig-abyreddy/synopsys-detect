package com.synopsys.integration.detect.battery.docker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.synopsys.integration.common.util.Bds;
import com.synopsys.integration.util.OperatingSystemType;

public class DetectDockerRunner {
    public DockerDetectResult runDetect(String image, File dockerfile, String cmd, File detectJar, File outputDirectory) throws IOException, InterruptedException {
        DockerClient dockerClient = connectToDocker();
        buildImageIfMissing(image, dockerfile, dockerClient);
        return runContainer(image, cmd, detectJar, outputDirectory, dockerClient);
    }

    private DockerDetectResult runContainer(String image, String cmd, File detectJar, File outputDirectory, DockerClient dockerClient) throws InterruptedException, IOException {
        String containerId = dockerClient.createContainerCmd(image)
                                 .withHostConfig(HostConfig.newHostConfig().withBinds(Bind.parse(detectJar.getParentFile().getCanonicalPath() + ":/opt/detect"), Bind.parse(outputDirectory.getCanonicalPath() + ":/opt/results")))
                                 .withCmd(cmd.split(" "))
                                 .exec().getId();

        dockerClient.startContainerCmd(containerId).exec();

        int exitCode = dockerClient.waitContainerCmd(containerId)
                           .exec(new WaitContainerResultCallback())
                           .awaitStatusCode();

        String logs = dockerClient.logContainerCmd(containerId)
                          .withStdErr(true)
                          .withStdOut(true)
                          .exec(new LongFormExampleDockerBattery.LogContainerTestCallback()).awaitCompletion().toString();

        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();

        return new DockerDetectResult(exitCode, logs);
    }

    private void buildImageIfMissing(String imageTag, File dockerfile, DockerClient dockerClient) {
        List<Image> images = dockerClient.listImagesCmd().exec();
        List<String> tags = images.stream().flatMap(image -> Arrays.stream(image.getRepoTags())).collect(Collectors.toList());
        boolean foundImage = tags.contains(imageTag);
        if (!foundImage) {
            dockerClient.buildImageCmd(dockerfile)
                .withTags(Bds.of(imageTag).toSet())
                .exec(new BuildImageResultCallback())
                .awaitImageId();
        }
    }

    private DockerClient connectToDocker() {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        // The java-docker library's default docker host value is the Linux/Mac default value, so no action required
        // But for Windows, unless told not to: use the Windows default docker host value
        if (OperatingSystemType.determineFromSystem() == OperatingSystemType.WINDOWS) {
            builder.withDockerHost("npipe:////./pipe/docker_engine");
        }
        DockerClientConfig config = builder.build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                                          .dockerHost(config.getDockerHost())
                                          .sslConfig(config.getSSLConfig())
                                          .maxConnections(100)
                                          .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
