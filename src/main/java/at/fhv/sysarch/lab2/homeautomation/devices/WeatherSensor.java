package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherSensorCommand> {

    public interface WeatherSensorCommand {}

    public WeatherSensor(ActorContext<WeatherSensorCommand> context) {
        super(context);
    }



    public static final class ReadWeather implements WeatherSensorCommand {

        Environment.Weather value;

        public ReadWeather(Environment.Weather value) {
            this.value = value;
        }

    }

    public static Behavior<WeatherSensorCommand> create (ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> new WeatherSensor(context, blinds, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;
    private ActorRef<Blinds.BlindsCommand> blinds;


    public WeatherSensor(ActorContext<WeatherSensorCommand> context, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        this(context);
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("WeatherSensor started");
    }


    @Override
    public Receive<WeatherSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherSensorCommand> onReadWeather(ReadWeather t) {
        getContext().getLog().info("WeatherSensor: " + t.value);

        this.blinds.tell(new Blinds.setWeatherStatus(t.value));

        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
