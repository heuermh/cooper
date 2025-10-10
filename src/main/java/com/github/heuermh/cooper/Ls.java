/*
 * The authors of this file license it to you under the
 * Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.heuermh.cooper;

import static org.dishevelled.compress.Writers.writer;

import java.io.PrintWriter;

import java.nio.file.Path;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;

import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * Cooper ls command.
 *
 * @author  Michael Heuer
 */
@Command(name = "ls", aliases={"list"})
public final class Ls implements Callable<Integer> {

    @Option(
        names = { "--region" },
        type = Region.class,
        converter = RegionConverter.class,
        defaultValue = "us-east-2"
    )
    private Region region;

    @Option(names = { "--anonymous" })
    private boolean anonymous;

    @Option(names = { "--bytes" })
    private boolean bytes;

    @Option(names = { "--human-readable" })
    private boolean humanReadable;

    @Option(names = { "--show-header" })
    private boolean showHeader;

    @Option(names = { "--reverse-columns" })
    private boolean reverseColumns;

    @Option(names = { "--checksums" })
    private boolean checksums;

    @Option(names = { "--summarize" })
    private boolean summarize;

    @Option(names = { "--output-path", "-o" })
    private Path outputPath;

    @Option(names = { "--verbose" })
    private boolean verbose;

    @Parameters(index = "0..*", arity = "1..*", descriptionKey = "uris")
    private List<String> uris;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Ls.class);

    /** Human readable formatter. */
    static final HumanReadableFormatter FORMATTER = new HumanReadableFormatter();

    /** s3 bucket and prefix regex pattern. */
    static final Pattern S3_URI = Pattern.compile("^s3:\\/\\/([a-zA-Z0-9.-]+)\\/*(.*)$");


    @Override
    public Integer call() throws Exception {

        S3ClientBuilder builder = S3Client.builder()
            .region(region);

        if (anonymous) {
            builder = builder.credentialsProvider(AnonymousCredentialsProvider.create());
        }
        S3Client s3 = builder.build();

        // warn if --summarize and --checksums
        if (summarize && checksums) {
            logger.warn("--summarize does not show checksums, even if --checksums provided");
        }

        try (PrintWriter writer = writer(outputPath)) {

            // show header, if --show-header
            if (showHeader) {
                if (summarize) {
                    if (bytes && humanReadable) {
                        writer.println(reverseColumns ? "bytes\thuman_readable\tcount\turi" : "uri\tcount\tbytes\thuman_readable");
                    }
                    else {
                        writer.println(reverseColumns ? "size\tcount\turi" : "uri\tcount\tsize");
                    }
                }
                else if (checksums) {
                    if (bytes && humanReadable) {
                        writer.println(reverseColumns ? "bytes\thuman_readable\tchecksum_type\tchecksum_algorithms\te_tag\turi" : "uri\tchecksum_type\tchecksum_algorithms\te_tag\tbytes\thuman_readable");
                    }
                    else {
                        writer.println(reverseColumns ? "size\tchecksum_type\tchecksum_algorithms\te_tag\turi" : "uri\tchecksum_type\tchecksum_algorithms\te_tag\tsize");
                    }
                }
                else {
                    if (bytes && humanReadable) {
                        writer.println(reverseColumns ? "bytes\thuman_readable\turi" : "uri\tbytes\thuman_readable");
                    }
                    else {
                        writer.println(reverseColumns ? "size\turi" : "uri\tsize");
                    }
                }
            }

            Joiner joiner = Joiner.on("\t");
            Map<String, Integer> counts = new HashMap<String, Integer>();
            Map<String, Long> sizes = new HashMap<String, Long>();

            for (String uri : uris) {
                Matcher m = S3_URI.matcher(uri);
                if (m.matches()) {
                    String bucket = m.group(1);
                    String prefix = m.group(2);

                    logger.info("valid uri={} bucket={} prefix={}", uri, bucket, prefix);

                    ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder().bucket(bucket);

                    if (prefix != null && !prefix.trim().isEmpty()) {
                        requestBuilder = requestBuilder.prefix(prefix);
                    }

                    ListObjectsV2Request request = requestBuilder.build();
                    logger.info("ListObjectsV2 request={}", request.toString());

                    ListObjectsV2Iterable responses = s3.listObjectsV2Paginator(request);
                    for (ListObjectsV2Response response : responses) {

                        logger.info("ListObjectsV2 response={}", response.toString());

                        for (S3Object content : response.contents()) {
                            String s3Path = "s3://" + bucket + "/" + content.key();

                            if (s3Path.startsWith(uri)) {
                                String byteSize = String.valueOf(content.size());
                                String humanReadableSize = FORMATTER.format(content.size());

                                if (summarize) {
                                    counts.put(uri, counts.containsKey(uri) ? counts.get(uri) + 1 : 1);
                                    sizes.put(uri, sizes.containsKey(uri) ? sizes.get(uri) + content.size() : content.size());
                                }
                                else if (checksums) {
                                    String checksumType = content.checksumTypeAsString();
                                    String checksumAlgorithms = Joiner.on(",").join(content.checksumAlgorithmAsStrings());

                                    // why is this value quoted?
                                    String eTag = content.eTag().replace("\"", "");

                                    // format per --bytes, --human-readable, --reverse-columns
                                    if (bytes && humanReadable) {
                                        if (reverseColumns) {
                                            writer.println(joiner.join(byteSize, humanReadableSize, checksumType, checksumAlgorithms, eTag, s3Path));
                                        }
                                        else {
                                            writer.println(joiner.join(s3Path, checksumType, checksumAlgorithms, eTag, byteSize, humanReadableSize));
                                        }
                                    }
                                    else if (humanReadable) {
                                        if (reverseColumns) {
                                            writer.println(joiner.join(humanReadableSize, checksumType, checksumAlgorithms, eTag, s3Path));
                                        }
                                        else {
                                            writer.println(joiner.join(s3Path, checksumType, checksumAlgorithms, eTag, humanReadableSize));
                                        }
                                    }
                                    else {
                                        if (reverseColumns) {
                                            writer.println(joiner.join(byteSize, checksumType, checksumAlgorithms, eTag, s3Path));
                                        }
                                        else {
                                            writer.println(joiner.join(s3Path, checksumType, checksumAlgorithms, eTag, byteSize));
                                        }
                                    }
                                }
                                else {
                                    // format per --bytes, --human-readable, --reverse-columns
                                    if (bytes && humanReadable) {
                                        writer.println(reverseColumns ? joiner.join(byteSize, humanReadableSize, s3Path) : joiner.join(s3Path, byteSize, humanReadableSize));
                                    }
                                    else if (humanReadable) {
                                        writer.println(reverseColumns ? joiner.join(humanReadableSize, s3Path) : joiner.join(s3Path, humanReadableSize));
                                    }
                                    else {
                                        writer.println(reverseColumns ? joiner.join(byteSize, s3Path) : joiner.join(s3Path, byteSize));
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    logger.warn("uri {} not a valid s3 URI", uri);
                }
            }
            if (summarize) {
                for (String uri : counts.keySet()) {
                    Integer count = counts.get(uri);
                    String byteSize = String.valueOf(sizes.get(uri));
                    String humanReadableSize = FORMATTER.format(sizes.get(uri));

                    // format per --bytes, --human-readable, --reverse-columns
                    if (bytes && humanReadable) {
                        writer.println(reverseColumns ? joiner.join(byteSize, humanReadableSize, count, uri) : joiner.join(uri, count, byteSize, humanReadableSize));
                    }
                    else if (humanReadable) {
                        writer.println(reverseColumns ? joiner.join(humanReadableSize, count, uri) : joiner.join(uri, count, humanReadableSize));
                    }
                    else {
                        writer.println(reverseColumns ? joiner.join(byteSize, count, uri) : joiner.join(uri, count, byteSize));
                    }
                }
            }
        }

        return 0;
    }


    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {

        // cheat to set system property before initializing logger
        if (Arrays.asList(args).contains("--verbose")) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        }

        // install a signal handler to exit on SIGPIPE
        sun.misc.Signal.handle(new sun.misc.Signal("PIPE"), new sun.misc.SignalHandler() {
                @Override
                public void handle(final sun.misc.Signal signal) {
                    System.exit(0);
                }
            });

        System.exit(new CommandLine(new Ls()).execute(args));
    }
}
