package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public enum Weather {
        SUNNY,
        CLOUDY,
        RAINY,
        STORMY
    }

    public interface EnvironmentCommand {}

    public static final class TemperatureChanger implements EnvironmentCommand {
        final Optional<Double> temperature;
        public TemperatureChanger(Optional<Double> temperature) {this.temperature = temperature;}
    }

    public static final class WeatherConditionsChanger implements EnvironmentCommand {
        final Optional<Weather> isSunny;

        public WeatherConditionsChanger(Optional<Weather> isSunny) {
            this.isSunny = isSunny;
        }
    }

    private double temperature = 15.0;
    private Weather isSunny = Weather.SUNNY;

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    public static class TemperatureSensorRequest implements EnvironmentCommand{
        public final String query;
        public final ActorRef<TemperatureSensor.TemperatureCommand> replyTo;

        public TemperatureSensorRequest(String query, ActorRef<TemperatureSensor.TemperatureCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static class WeatherSensorRequest implements EnvironmentCommand{
        public final String query;
        public final ActorRef<WeatherSensor.WeatherSensorCommand> replyTo;

        public WeatherSensorRequest(String query, ActorRef<WeatherSensor.WeatherSensorCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static Behavior<EnvironmentCommand> create(){
        return Behaviors.setup(context ->  Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    private Environment(ActorContext<EnvironmentCommand> context,TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(Optional.ofNullable((null))), Duration.ofSeconds(2));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.ofNullable(null)), Duration.ofSeconds(10));
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChanger.class, this::onChangeTemperature)
                .onMessage(WeatherConditionsChanger.class, this::onChangeWeather)
                .onMessage(TemperatureSensorRequest.class, this::onTemperatureSensorRequest)
                .onMessage(WeatherSensorRequest.class, this::onWeatherSensorRequest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private Behavior<EnvironmentCommand> onTemperatureSensorRequest(TemperatureSensorRequest temperatureSensorRequest) {
        temperatureSensorRequest.replyTo.tell(new TemperatureSensor.ResponseTemperature(Optional.of(this.temperature)));
        //getContext().getLog().info(temperatureSensorRequest.query);
        return Behaviors.same();
    }

    private Behavior<EnvironmentCommand> onWeatherSensorRequest(WeatherSensorRequest weatherSensorRequest) {
        weatherSensorRequest.replyTo.tell(new WeatherSensor.ResponseWeather(isSunny));
        //getContext().getLog().info(weatherSensorRequest.query);
        return Behaviors.same();
    }

    private Behavior<EnvironmentCommand> onChangeTemperature(TemperatureChanger t) {
        if (t.temperature.isPresent()) {
            this.temperature = t.temperature.get();
        }
        else {
            this.temperature += -2 + (float) (Math.random() * (2 - -2)); // makes random number changes within a range (-2, 2)
        }
        getContext().getLog().info("Environment received {}", temperature);

        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
        // randomly changes weather
        Weather[] weathers = Weather.values();
        int i;
        do {
            i = (int) (Math.random() * (0 + weathers.length));
        } while (weathers[i] == isSunny);
        // sets either the w or the random weather value
        isSunny = w.isSunny.orElse(weathers[i]);
        getContext().getLog().info("Environment Change Sun to {}", isSunny);

        return this;
    }


    private Environment onPostStop() {
        getContext().getLog().info("Environment actor stopped");
        return this;
    }
}


