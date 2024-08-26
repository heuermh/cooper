# cooper

Command line tools for [AWS s3](https://aws.amazon.com/s3/).

<br/>

![cooper project logo](https://github.com/heuermh/cooper/raw/main/images/cooper-branding.jpg)

https://en.wikipedia.org/wiki/Cooper_(profession)

## Motivation

Primarily a workaround for this issue:

Broken pipe error when piping "s3 ls" output to grep -q<br/>
https://github.com/aws/aws-cli/issues/5899


## Hacking cooper

Install

 * JDK 11 or later, https://openjdk.java.net
 * Apache Maven 3.6.3 or later, https://maven.apache.org

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
  coop [-hV] [COMMAND]

List s3 paths recursively with content sizes.

E.g.
   $ cat uris.txt | xargs coop ls
   $ coop ls s3://... | head -n 4
   $ coop ls s3://... | grep -m 10 -e '...'
   $ coop ls s3://... | cut -f 2 | sort -n -r


OPTIONS
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

COMMANDS
  ls, list             List s3 paths recursively with content sizes.
  help                 Display help information about the specified command.
  generate-completion  Generate bash/zsh completion script for coop.
```

At present the only command is `ls`/`list`
```bash
$ coop ls --help
USAGE
  coop ls [-hV] [--human-readable] [--reverse-columns] [--show-header] [--verbose] [--region=<region>] <uris>...

List s3 paths recursively with content sizes.

E.g.
   $ cat uris.txt | xargs coop ls
   $ coop ls s3://... | head -n 4
   $ coop ls s3://... | grep -m 10 -e '...'
   $ coop ls s3://... | cut -f 2 | sort -n -r


PARAMETERS
      <uris>...           One or more s3 URIs.

OPTIONS
      --region=<region>   AWS region, default us-west-2.
      --human-readable    Format content sizes in binary multi-byte units.
      --show-header       Show column header row in output.
      --reverse-columns   Reverse the order of output columns.
      --summarize         Summarize counts and sizes per input URI.
      --verbose           Show additional logging messages.
  -h, --help              Show this help message and exit.
  -V, --version           Print version information and exit.
```
