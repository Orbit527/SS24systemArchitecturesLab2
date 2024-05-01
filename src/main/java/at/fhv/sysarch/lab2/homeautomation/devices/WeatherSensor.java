package at.fhv.sysarch.lab2.homeautomation.devices;

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

        Boolean value;

        public ReadWeather(Boolean value) {
            this.value = value;
        }



    }

    public static Behavior<WeatherSensorCommand> create (String groupId, String deviceId) {
        return Behaviors.setup(context -> new WeatherSensor(context, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;


    public WeatherSensor(ActorContext<WeatherSensorCommand> context, String groupId, String deviceId) {
        this(context);
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
        //TODO: send to AC
        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
