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
import java.util.List;

import java.util.concurrent.Callable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.AutoComplete.GenerateCompletion;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ScopeType;

import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.s3.S3Client;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * Cooper.
 */
@Command(
  name = "coop",
  scope = ScopeType.INHERIT,
  subcommands = {
      HelpCommand.class,
      GenerateCompletion.class
  },
  mixinStandardHelpOptions = true,
  sortOptions = false,
  usageHelpAutoWidth = true,
  resourceBundle = "com.github.heuermh.cooper.Messages",
  versionProvider = com.github.heuermh.cooper.About.class
)
public final class Cooper implements Callable<Integer> {

    @picocli.CommandLine.Option(names = { "--verbose" })
    private boolean verbose;

    @picocli.CommandLine.Parameters(index = "0..*", descriptionKey = "uris")
    private List<String> uris;

    static Logger logger;

    static Pattern S3_URI = Pattern.compile("^s3:\\/\\/([a-zA-Z-]+)\\/(.+)$");

    @Override
    public Integer call() throws Exception {

        S3Client s3 = S3Client.builder()
            .region(Region.US_WEST_2)
            .build();

        for (String uri : uris) {
            Matcher m = S3_URI.matcher(uri);
            if (m.matches()) {
                String bucket = m.group(1);
                String prefix = m.group(2);
                
                logger.info("valid uri={} bucket={} prefix={}", uri, bucket, prefix);

                ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

                logger.info("ListObjectsV2 request={}", request.toString());

                ListObjectsV2Iterable responses = s3.listObjectsV2Paginator(request);
                for (ListObjectsV2Response response : responses) {

                    logger.info("ListObjectsV2 response={}", response.toString());

                    for (S3Object content : response.contents()) {

                        if (uri.equals("s3://" + bucket + "/" + content.key())) {
                            System.out.println(uri + "\t" + content.size());
                        }
                    }
                }
            }
            else {
                logger.warn("uri {} not a valid s3 URI", uri);
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
        logger = LoggerFactory.getLogger(Cooper.class);

        System.exit(new CommandLine(new Cooper()).execute(args));
    }
}
