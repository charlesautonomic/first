package com.autonomic.example.edge;

import com.autonomic.ext.command.CommandTrigger;
import com.autonomic.ext.command.CommandTrigger.CommandRequestType;
import com.autonomic.ext.command.DeviceCommandGrpc;
import com.autonomic.ext.command.DeviceCommandGrpc.DeviceCommandBlockingStub;
import com.autonomic.ext.command.DeviceCommandGrpc.DeviceCommandStub;
import com.autonomic.ext.command.RegisterCommandListenerRequest;
import com.autonomic.ext.command.actuation.ActuationRequest;
import com.autonomic.ext.command.actuation.ActuationRequest.ActuationType;
import com.autonomic.ext.command.wifi.WifiConfigRequest;
import com.autonomic.ext.edge.EdgeGrpc;
import com.autonomic.ext.edge.EdgeGrpc.EdgeStub;
import com.autonomic.ext.edge.PublishRequest;
import com.autonomic.ext.edge.PublishResponse;
import com.autonomic.ext.event.Event;
import com.autonomic.ext.event.StateTransition;
import com.autonomic.ext.event.WellKnownFiniteStateMachine;
import com.autonomic.ext.event.state.actuation.WellKnownActuationState;
import com.autonomic.ext.event.state.actuation.WellKnownActuationTransitionTrigger;
import com.autonomic.ext.telemetry.Metric;
import com.autonomic.ext.telemetry.Report;
import com.autonomic.ext.telemetry.signal.Signal;
import com.autonomic.ext.telemetry.signal.WellKnownSignal;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.SQLOutput;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import org.apache.commons.codec.binary.Hex;

public class ExampleCommandTrigger {

    public static Event.Builder setEventCommon() {

        Instant now = Instant.now();

        return Event.newBuilder()
            // set the URI of this event, complete with label which uniquely identifies the type of
            // triggered vehicle event, and UUID for this specific event.
                //add something to test git
                //test push in local git in master branch
            .setId("aui:edge:vehicle:event:" + UUID.randomUUID().toString())
            // Include the time of day when this event occurred.
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano()));

    }

    private static final Logger logger = Logger.getLogger(ExampleCommandTrigger.class.getName());
    private static String VIN;

    private final ManagedChannel channel;
    private final EdgeStub asyncStub;
    private final DeviceCommandBlockingStub asyncBlockingStub;

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

    public ExampleCommandTrigger(String host, int port, SslContext sslContext) throws SSLException {
        this(NettyChannelBuilder.forAddress(host, port)
                 .negotiationType(NegotiationType.TLS)
                 .sslContext(sslContext)
                 .build());
    }

    public ExampleCommandTrigger(ManagedChannel channel) {
        this.channel = channel;
        this.asyncStub = EdgeGrpc.newStub(channel);
        this.asyncBlockingStub = DeviceCommandGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

   public void registerCommandListener(int numMetrics)
       throws InterruptedException, InvalidProtocolBufferException {

        //Register Command Listener
       RegisterCommandListenerRequest request = generateRegisterCommandListenerRequest();
       java.util.Iterator<CommandTrigger> commandTriggerIterator = asyncBlockingStub.registerCommandListener(request);
       CommandTrigger ct;
       while (commandTriggerIterator.hasNext()){
           //get command trigger
           ct = commandTriggerIterator.next();

           System.out.println(ct.getRequestCase());

           //get the encrypt command
           byte[] requestBytes = ct.getEncryptedRequest().toByteArray();

           System.out.println("request is -----------------");
           System.out.println(Hex.encodeHex(requestBytes));

           //get the payload
           //This is a SyncP(High Bandwidth) message
           int size = requestBytes.length;
           int payLoadSize = size - 40 - 16;
           int payLoadStart = 40;
           byte[] paylaodBytes= new byte[payLoadSize];
           for(int i = 0 ; i< payLoadSize; i++){
               paylaodBytes[i]=requestBytes[payLoadStart];
               payLoadStart ++;
           }

           byte[] iv = new byte[16];
           int ivStart=24;
           for(int i=0;i<16;i++) {
               iv[i] = requestBytes[ivStart];
               ivStart++;
           }
           System.out.println("iv is"+ Hex.encodeHex(iv));

           //decrypt payloadBytes (use AES/CCM to decrypt the payload)
           //the decrypt ACTUATION UNLOCK payload is b'x08x02'
           byte [] testActuationPayload = new byte[]{0x08,0x02};
           byte [] testNullPayload = new byte[]{};

           //find out which command type
           switch (ct.getType()) {
               //ACTUATION Request
               case ACTUATION:
                   ActuationRequest actuationRequest = ActuationRequest.parseFrom(testActuationPayload);
                   switch (actuationRequest.getActuationType()) {
                       case LOCK:
                           // lock logic
                           break;
                       case UNLOCK:
                           // unlock logic
                           // ..... some device logic code ....

                           //update stat_transiation (Talk to TMC ,I am finished the unlock command)
                           publishActuationUnlockEvent(ct);
                           break;
                       case REMOTE_START:
                           // remote start logic
                           break;
                       case CANCEL_REMOTE_START:
                           // cancel remote start logic
                           break;
                       case UNRECOGNIZED:
                       default:
                           break;
                   }

                   break;

               //WIFI_CONFIG
               case WIFI_CONFIG:
                   //you can parse the decrypt paylaod to the specify command request (the testNullPayload will not work , it is a fake code here)
                   WifiConfigRequest wifiConfigRequest = WifiConfigRequest.parseFrom(testNullPayload);
                   // logic code here
                   // .....
                   // update wifi configuration
                   break;

               //OTHER COMMANDS
               // .....

               default:
                   break;
           }




           System.out.println("===================publish===================");
           Thread.sleep(5000);

       }
       System.out.println("=========end===========");
       shutdown();
   }

   public void publishActuationUnlockEvent(CommandTrigger ct){


       System.out.println("command ID is +++++"+ ct.getCorrelationId().toStringUtf8());

       System.out.println("====================here===========");
       PublishRequest actuationSucceeded = PublishRequest.newBuilder()
           .addEvents(setEventCommon()
               .setCorrelationId(ct.getCorrelationId().toStringUtf8())
               .setSource("aui:edge:vehicle:"+VIN)
               .setPayload(Any.pack(StateTransition.newBuilder()
                   .setWkFsmName(WellKnownFiniteStateMachine.ACTUATION_UNLOCK)
                   .setUnlockTrigger(
                       WellKnownActuationTransitionTrigger.ACTUATION_SUCCEEDED)
                   .setUnlockFromState(
                       WellKnownActuationState.REQUEST_DELIVERY_IN_PROGRESS)
                   .setUnlockToState(WellKnownActuationState.SUCCESS)
                   .setMessage("hello---this--is---a--actuation--unlock-test")
                   .build())))
           .build();

       System.out.println(actuationSucceeded.toByteString().toStringUtf8());
       StreamObserver<PublishResponse> responseObserver = new StreamObserver<PublishResponse>() {
           @Override
           public void onNext(PublishResponse publishResponse) {
               System.out.println("------------------");
               System.out.println(publishResponse.toString());
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
       requestObserver.onNext(actuationSucceeded);
       requestObserver.onCompleted();
   }

    private RegisterCommandListenerRequest generateRegisterCommandListenerRequest() {
        return RegisterCommandListenerRequest.newBuilder().build();
    }


    public static void main(String[] args) throws Exception {

        if (args.length < 5 || args.length > 6) {
            System.out.println("USAGE: ExampleCommandTrigger host port " +
                                   "trustCertCollectionFilePath " +
                                   "clientCertChainFilePath " +
                                   "clientPrivateKeyFilePath " +
                                   "VIN\n");
            System.exit(1);
        }

        ExampleCommandTrigger client;
        client = new ExampleCommandTrigger(args[0], Integer.parseInt(args[1]),
            buildSslContext(args[2], args[3], args[4]));
        VIN = args[5];
        client.registerCommandListener(1);
    }
}
