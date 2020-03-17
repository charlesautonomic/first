# Edge Client Java Examples

## Copyright Statement

Autonomic Proprietary 1.0

Copyright (C) 2018 Autonomic, LLC - All rights reserved

Proprietary and confidential.

NOTICE:  All information contained herein is, and remains the property of
Autonomic, LLC and its suppliers, if any.  The intellectual and technical
concepts contained herein are proprietary to Autonomic, LLC and its suppliers
and may be covered by U.S. and Foreign Patents, patents in process, and are
protected by trade secret or copyright law. Dissemination of this information
or reproduction of this material is strictly forbidden unless prior written
permission is obtained from Autonomic, LLC.

Unauthorized copy of this file, via any medium is strictly prohibited.

## This is a Java "Gradle" Project

It may be possible to load this tree into your IDE and run it. If
this does not work then:

Copy `ExamplePublishClient.java` and all subdirectories
into a Java project workspace, add the dependencies on the
normal GRPC package, a JSON package, and the protocol buffer
definitions from Autonomic.

## Starting the Example Application

Compile the Edge Client application, by running:

  `./gradlew jar`
  
to run the Edge Client application, run:

```
# java -jar edge/build/libs/edge.jar
USAGE: ExamplePublishClient host port trustCertCollectionFilePath clientCertChainFilePath clientPrivateKeyFilePath VIN
```

The credential file arguments are can be retrieved from the [bootstrap grpcurl](https://developer.autonomic.ai/services/bootstrap-service/obtaining-a-signed-certificate-from-bootstrap) examples.

## Terminating the Example Application

The example code does not implement a graceful termination, retries, and channel state monitoring.  These are all important functions of production quality grpc clients which must be implemented.

## Portability and Future Development

**This client is example code and is not production quality.  Do not copy this code into production code.**
***You must update this code to meet your security needs.***
