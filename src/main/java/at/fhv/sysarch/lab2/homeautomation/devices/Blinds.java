package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public Blinds(ActorContext<BlindsCommand> context) {
        super(context);
    }

    public interface BlindsCommand {}

    public static final class setWeatherStatus implements BlindsCommand {

        Environment.Weather weather;

        public setWeatherStatus(Environment.Weather weather) {
            this.weather = weather;
        }

    }

    public static final class setMediaStationStatus implements BlindsCommand {

        Boolean mediaStationPlaying;
        public setMediaStationStatus(Boolean mediaStationPlaying) {
            this.mediaStationPlaying = mediaStationPlaying;
        }

    }

    public static Behavior<BlindsCommand> create (String groupId, String deviceId) {

        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;

    private Boolean mediaStationPlaying = false;
    private Boolean isClosed;

    public Blinds(ActorContext<BlindsCommand> context, String groupId, String deviceId) {
        this(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("Blinds started");
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(setWeatherStatus.class, this::onReadBlindsStatus)
                .onMessage(setMediaStationStatus.class, this::onMediaStationStatus)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onReadBlindsStatus(setWeatherStatus bs) {
        getContext().getLog().info("Blinds got: " + bs.weather);

        if (this.mediaStationPlaying == false) {
            if (bs.weather == Environment.Weather.SUNNY) {
                this.isClosed = true;
            } else {
                this.isClosed = false;
            }
        } else {
            this.isClosed = true;
        }

        getContext().getLog().info("Blinds are closed? " + this.isClosed);

        return this;
    }

    private Behavior<BlindsCommand> onMediaStationStatus(setMediaStationStatus ms) {

        getContext().getLog().info("Blinds reveiced from MediaStation: " + ms.mediaStationPlaying);
        this.mediaStationPlaying = ms.mediaStationPlaying;
        if (this.mediaStationPlaying == true) {
            this.isClosed = true;
        } else {
            this.isClosed = false;
        }

        return this;
    }


    private Blinds onPostStop() {
        getContext().getLog().info("BlindsSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
