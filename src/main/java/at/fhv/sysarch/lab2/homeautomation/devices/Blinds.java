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

    public static Behavior<BlindsCommand> create (String groupId, String deviceId) {

        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;
    Boolean isClosed;

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
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onReadBlindsStatus(setWeatherStatus bs) {
        getContext().getLog().info("Blinds got: " + bs.weather);

        //TODO: Logic, if Movie is running, then dont open Blinds
        if (bs.weather == Environment.Weather.SUNNY) {
           this.isClosed = true;
        } else {
            this.isClosed = false;
        }

        getContext().getLog().info("Blinds are closed? " + this.isClosed);

        return this;
    }

    private Blinds onPostStop() {
        getContext().getLog().info("BlindsSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
