# cooper

Command line tools for [AWS s3](https://aws.amazon.com/s3/).

Primarily a workaround for this issue:

Broken pipe error when piping "s3 ls" output to grep -q
https://github.com/aws/aws-cli/issues/5899


![cooper project logo](https://github.com/heuermh/cooper/raw/main/images/cooper-branding.png)

https://en.wikipedia.org/wiki/Cooper_(profession)

## Hacking cooper

Install

 * JDK 11 or later, https://openjdk.java.net
 * Apache Maven 3.3.9 or later, https://maven.apache.org

To build
```bash
$ mvn package

$ export PATH=$PATH:`pwd`/target/appassembler/bin
```

## Using cooper

### Usage

```bash
$ coop --help
USAGE
  coop [-hV] [--verbose] [<uris>...] [COMMAND]


PARAMETERS
      [<uris>...]   Zero or more s3 URIs.

OPTIONS
      --verbose     Show additional logging messages.
  -h, --help        Show this help message and exit.
  -V, --version     Print version information and exit.

COMMANDS
  help                 Display help information about the specified command.
  generate-completion  Generate bash/zsh completion script for coop.
```
