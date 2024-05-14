package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherSensorCommand> {

    public interface WeatherSensorCommand {}

    public WeatherSensor(ActorContext<WeatherSensorCommand> context) {
        super(context);
    }

    public static final class RequestWeather implements WeatherSensorCommand {

        public RequestWeather() {
        }
    }

    public static final class ResponseWeather implements WeatherSensorCommand {

        Environment.Weather value;

        public ResponseWeather(Environment.Weather value) {
            this.value = value;
        }
    }

    public static Behavior<WeatherSensorCommand> create (ActorRef<Environment.EnvironmentCommand> environment, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new WeatherSensor(context, timers, environment, blinds, groupId, deviceId)));
    }

    private String groupId;
    private String deviceId;
    private ActorRef<Environment.EnvironmentCommand> environment;
    private ActorRef<Blinds.BlindsCommand> blinds;


    public WeatherSensor(ActorContext<WeatherSensorCommand> context, TimerScheduler<WeatherSensorCommand> weatherTimer, ActorRef<Environment.EnvironmentCommand> environment, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        this(context);
        weatherTimer.startTimerAtFixedRate(new RequestWeather(), Duration.ofSeconds(5));
        this.environment = environment;
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("WeatherSensor started");
    }


    @Override
    public Receive<WeatherSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestWeather.class, this::onRequestWeather)
                .onMessage(ResponseWeather.class, this::onResponseWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherSensorCommand> onRequestWeather(RequestWeather r) {
        environment.tell(new Environment.WeatherSensorRequest("WeatherSensor request from Environment", this.getContext().getSelf()));
        return this;
    }

    private Behavior<WeatherSensorCommand> onResponseWeather(ResponseWeather r) {
        getContext().getLog().debug("WeatherSensor: " + r.value);

        //send received Weather to Blinds
        this.blinds.tell(new Blinds.setWeatherStatus(r.value));

        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
