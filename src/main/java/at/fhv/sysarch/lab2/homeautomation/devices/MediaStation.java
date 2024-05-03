package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    public interface MediaStationCommand {}

    public MediaStation(ActorContext<MediaStationCommand> context) {
        super(context);
    }

    public static final class test implements  MediaStationCommand {

        String value;
        public test(String value) {
            this.value = value;
        }

    }

    public static Behavior<MediaStationCommand> create (String groupId, String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;
    private boolean moviePlaying = false;
    private boolean poweredOn = true;

    public MediaStation(ActorContext<MediaStationCommand> context, String groupId, String deviceId) {
        this(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("MediaStation started");
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(test.class, this::onTest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onTest(test val) {
        getContext().getLog().info("Media Station received: " + val.value);

        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
