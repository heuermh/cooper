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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;

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

    @picocli.CommandLine.Option(
        names = { "--region" },
        type = Region.class,
        converter = RegionConverter.class,
        defaultValue = "us-east-1"
    )
    private Region region;

    @picocli.CommandLine.Option(names = { "--anonymous" })
    private boolean anonymous;

    @picocli.CommandLine.Option(names = { "--human-readable" })
    private boolean humanReadable;

    @picocli.CommandLine.Option(names = { "--show-header" })
    private boolean showHeader;

    @picocli.CommandLine.Option(names = { "--reverse-columns" })
    private boolean reverseColumns;

    @picocli.CommandLine.Option(names = { "--summarize" })
    private boolean summarize;

    @picocli.CommandLine.Option(names = { "--verbose" })
    private boolean verbose;

    @picocli.CommandLine.Parameters(index = "0..*", arity = "1..*", descriptionKey = "uris")
    private List<String> uris;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Ls.class);

    /** Human readable formatter. */
    static final HumanReadableFormatter FORMATTER = new HumanReadableFormatter();

    /** s3 bucket and prefix regex pattern. */
    static final Pattern S3_URI = Pattern.compile("^s3:\\/\\/([a-zA-Z-]+)\\/*(.*)$");


    @Override
    public Integer call() throws Exception {

        S3ClientBuilder builder = S3Client.builder()
            .region(region);

        if (anonymous) {
            builder = builder.credentialsProvider(AnonymousCredentialsProvider.create());
        }
        S3Client s3 = builder.build();

        if (showHeader) {
            if (summarize) {
                System.out.println(reverseColumns ? "size\tcount\turi" : "uri\tcount\tsize");
            }
            else {
                System.out.println(reverseColumns ? "size\turi" : "uri\tsize");
            }
        }

        Map<String, Integer> counts = new HashMap<String, Integer>();
        Map<String, Long> sizes = new HashMap<String, Long>();

        for (String uri : uris) {
            Matcher m = S3_URI.matcher(uri);
            if (m.matches()) {
                String bucket = m.group(1);
                String prefix = m.group(2);
                
                logger.info("valid uri={} bucket={} prefix={}", uri, bucket, prefix);

                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket);

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

                            String size = humanReadable ? FORMATTER.format(content.size()) : String.valueOf(content.size());

                            if (summarize) {
                                counts.put(uri, counts.containsKey(uri) ? counts.get(uri) + 1 : 1);
                                sizes.put(uri, sizes.containsKey(uri) ? sizes.get(uri) + content.size() : content.size());
                            }
                            else {
                                System.out.println(reverseColumns ? size + "\t" + s3Path : s3Path + "\t" + size);
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
                String size = humanReadable ? FORMATTER.format(sizes.get(uri)) : String.valueOf(sizes.get(uri));
                System.out.println(reverseColumns ? size + "\t" + count + "\t" + uri : uri + "\t" + count + "\t" + size);
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
