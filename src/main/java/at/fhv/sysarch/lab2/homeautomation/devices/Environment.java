package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

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

    // TODO: Provide the means for manually setting the temperature

    public static class Request implements EnvironmentCommand{
        public final String query;
        public final ActorRef<TemperatureSensor.TemperatureCommand> replyTo;

        public Request(String query, ActorRef<TemperatureSensor.TemperatureCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static class Response implements EnvironmentCommand{
        public final String result;

        public Response(String result) {
            this.result = result;
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
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.ofNullable(null)), Duration.ofSeconds(10)); //TODO extend duration
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChanger.class, this::onChangeTemperature)
                .onMessage(WeatherConditionsChanger.class, this::onChangeWeather)
                .onMessage(Request.class, this::onRequest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private Behavior<EnvironmentCommand> onRequest(Request request) {
        // ... process request ...
        request.replyTo.tell(new TemperatureSensor.GetReadTemperature(Optional.of(this.temperature)));

        getContext().getLog().info(request.query);

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

        // TODO: Handling of temperature change. Are sensors notified or do they read the temperature?
        //TODO: change to Request Response
        //this.temperatureSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(temperature)));

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

        // TODO: Handling of weather change. Are sensors notified or do they read the weather information?
        //TODO: change to Request Response
        //this.weatherSensor.tell(new WeatherSensor.ReadWeather(isSunny));

        return this;
    }


    private Environment onPostStop() {
        getContext().getLog().info("Environment actor stopped");
        return this;
    }
}


