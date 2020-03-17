package com.autonomic.example.edge;

import com.autonomic.ext.edge.EdgeGrpc;
import com.autonomic.ext.edge.EdgeGrpc.EdgeStub;
import com.autonomic.ext.edge.PublishRequest;
import com.autonomic.ext.edge.PublishResponse;
import com.autonomic.ext.telemetry.Metric;
import com.autonomic.ext.telemetry.Report;
import com.autonomic.ext.telemetry.signal.Signal;
import com.autonomic.ext.telemetry.signal.WellKnownSignal;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;

public class ExamplePublishClient {

    private static final Logger logger = Logger.getLogger(ExamplePublishClient.class.getName());
    private static String VIN;

    private final ManagedChannel channel;
    private final EdgeStub asyncStub;

    private static SslContext buildSslContext(String trustCertCollectionFilePath,
        String clientCertChainFilePath,
        String clientPrivateKeyFilePath) throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }
        if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
            builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
        }
        return builder.build();
    }

    public ExamplePublishClient(String host, int port, SslContext sslContext) throws SSLException {
        this(NettyChannelBuilder.forAddress(host, port)
                 .negotiationType(NegotiationType.TLS)
                 .sslContext(sslContext)
                 .build());
    }

    public ExamplePublishClient(ManagedChannel channel) {
        this.channel = channel;
        asyncStub = EdgeGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

   public void publish(int numMetrics) throws InterruptedException {
       StreamObserver<PublishResponse> responseObserver = new StreamObserver<PublishResponse>() {
           @Override
           public void onNext(PublishResponse publishResponse) {
               logger.info("Received Response: " + publishResponse.toString());

           }

           @Override
           public void onError(Throwable throwable) {
               // Decodes the GRPC status from the throwable method
               logger.info("Error sending message: " + Status.fromThrowable((throwable)));
           }

           @Override
           public void onCompleted() {
               logger.info("Finished publishing");
           }

       };

       StreamObserver<PublishRequest> requestObserver = asyncStub.publish(responseObserver);

       try {
           for (int i = 0; i < numMetrics; ++i) {
               PublishRequest request = generatePublishRequest();
               logger.info("sending metric " + i);
               requestObserver.onNext(request);
               // Sleep for 1 second before sending the next one.
               Thread.sleep(1000);
           }
       } catch (RuntimeException e) {
           requestObserver.onError(e);
           throw e;
       }
       requestObserver.onCompleted();

       shutdown();
   }

    private PublishRequest generatePublishRequest() {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                                  .setSeconds(now.getEpochSecond())
                                  .setNanos(now.getNano())
                                  .build();
        return PublishRequest.newBuilder()
            .addReports(Report.newBuilder()
                .setSource("aui:edge:vehicle:" + VIN)
                .addMetrics(
                     Metric.newBuilder()
                                 .setStartTime(timestamp)
                                 .setMetricKind(Metric.MetricKind.GAUGE)
                                 .setSignal(Signal.newBuilder()
                                                .setWksSignal(WellKnownSignal.ENGINE_SPEED))
                                 .setInt64Value(4000)
                                 .build()
                )
            ).build();
    }


    public static void main(String[] args) throws Exception {

        if (args.length < 5 || args.length > 6) {
            System.out.println("USAGE: ExamplePublishClient host port " +
                                   "trustCertCollectionFilePath " +
                                   "clientCertChainFilePath " +
                                   "clientPrivateKeyFilePath " +
                                   "VIN\n");
            System.exit(1);
        }

        ExamplePublishClient client;
        client = new ExamplePublishClient(args[0], Integer.parseInt(args[1]),
            buildSslContext(args[2], args[3], args[4]));
        VIN = args[5];
        client.publish(3);
    }
}
