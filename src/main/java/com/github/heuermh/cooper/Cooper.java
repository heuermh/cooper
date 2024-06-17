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

import picocli.AutoComplete.GenerateCompletion;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;

/**
 * Cooper.
 *
 * @author  Michael Heuer
 */
@Command(
  name = "coop",
  scope = ScopeType.INHERIT,
  subcommands = {
      Ls.class,
      HelpCommand.class,
      GenerateCompletion.class
  },
  mixinStandardHelpOptions = true,
  sortOptions = false,
  usageHelpAutoWidth = true,
  resourceBundle = "com.github.heuermh.cooper.Messages",
  versionProvider = com.github.heuermh.cooper.About.class
)
public final class Cooper {

    @Parameters(hidden = true)
    private List<String> ignored;


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

        System.exit(new CommandLine(new Cooper()).execute(args));
    }
}
