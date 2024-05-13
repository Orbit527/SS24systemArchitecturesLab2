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

    public static final class PowerMediaStationOn implements MediaStationCommand {
        final boolean value;

        public PowerMediaStationOn(boolean value) {
            this.value = value;
        }
    }

    public static final class MediaStationPlayMovie implements  MediaStationCommand {
        boolean play;
        public MediaStationPlayMovie(boolean play) {
            this.play = play;
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
                .onMessage(PowerMediaStationOn.class, this::onPowerMediaStationOn)
                .onMessage(MediaStationPlayMovie.class, this::onMediaStationPlayMovie)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onMediaStationPlayMovie(MediaStationPlayMovie val) {
        if (this.poweredOn == true) {
            if (this.moviePlaying != val.play) {
                this.moviePlaying = val.play;
                this.blinds.tell(new Blinds.setMediaStationStatus(val.play));
                getContext().getLog().info("Media Station " + (this.moviePlaying ? "now playing Movie!" : "stopped playing Movie!"));
            } else {
                getContext().getLog().info("Media Station already " + (this.moviePlaying ? "playing Movie!" : "stopped playing Movie!"));
            }
        } else {
            getContext().getLog().info("Media Station is not turned on!");
        }
        return this;
    }

    private Behavior<MediaStationCommand> onPowerMediaStationOn(PowerMediaStationOn p) {
        getContext().getLog().info("Turning MediaStation to {}", p.value);
        this.poweredOn = p.value;
        if (this.poweredOn == false) {
            this.moviePlaying = false;
            this.blinds.tell(new Blinds.setMediaStationStatus(false));
        }
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
