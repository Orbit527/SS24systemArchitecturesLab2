package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    public interface MediaStationCommand {}

    public MediaStation(ActorContext<MediaStationCommand> context) {
        super(context);
    }

    public static final class PowerMediaStation implements MediaStationCommand {
        final Optional<Boolean> value;

        public PowerMediaStation(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class MediaStationPlayMovie implements  MediaStationCommand {
        public MediaStationPlayMovie() {
        }
    }

    public static final class MediaStationStopMovie implements  MediaStationCommand {

        public MediaStationStopMovie() {
        }
    }

    public static Behavior<MediaStationCommand> create (ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, blinds, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private boolean moviePlaying = false;
    private boolean poweredOn = true;

    public MediaStation(ActorContext<MediaStationCommand> context, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        this(context);
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("MediaStation started");
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PowerMediaStation.class, this::onPowerMediaStation)
                .onMessage(MediaStationPlayMovie.class, this::onMediaStationPlayMovie)
                .onMessage(MediaStationStopMovie.class, this::onMediaStationStopMovie)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onMediaStationPlayMovie(MediaStationPlayMovie val) {
        if (this.poweredOn == true) {
            this.moviePlaying = true;
            this.blinds.tell(new Blinds.setMediaStationStatus(true));
            getContext().getLog().info("Media Station now playing Movie!");
        } else {
            getContext().getLog().info("Media Station is not turned on!");
        }

        return this;
    }

    private Behavior<MediaStationCommand> onMediaStationStopMovie(MediaStationStopMovie val) {
        if (this.poweredOn == true) {
            this.moviePlaying = false;
            this.blinds.tell(new Blinds.setMediaStationStatus(false));
            getContext().getLog().info("Media Station now stopped Movie!");
        } else {
            getContext().getLog().info("Media Station is not turned on!");
        }

        return this;
    }

    private Behavior<MediaStationCommand> onPowerMediaStation(PowerMediaStation r) {
        boolean value = r.value.orElse(false);
        if (value) {
            return onPowerMediaStationOn(r);
        } else {
            return onPowerMediaStationOff(r);
        }
    }

    private Behavior<MediaStationCommand> onPowerMediaStationOff(PowerMediaStation r) {
        getContext().getLog().info("Turning MediaStation to {}", r.value);

        if(r.value.get() == false) {
            this.moviePlaying = false;
            return this.powerOff();
        }
        return this;
    }

    private Behavior<MediaStationCommand> onPowerMediaStationOn(PowerMediaStation r) {
        getContext().getLog().info("Turning MediaStation to {}", r.value);

        if(r.value.get() == true) {
            return Behaviors.receive(MediaStationCommand.class)

                    .onMessage(PowerMediaStation.class, this::onPowerMediaStationOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private Behavior<MediaStationCommand> powerOff() {
        this.poweredOn = false;
        return Behaviors.receive(MediaStationCommand.class)
                .onMessage(PowerMediaStation.class, this::onPowerMediaStationOn)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }



    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
