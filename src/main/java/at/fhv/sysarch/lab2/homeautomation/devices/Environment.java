package at.fhv.sysarch.lab2.homeautomation.devices;

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
    // TODO: Provide the means for manually setting the weather

    public static Behavior<EnvironmentCommand> create(){
        return Behaviors.setup(context ->  Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    private Environment(ActorContext<EnvironmentCommand> context,TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(5));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.of(isSunny)), Duration.ofSeconds(5));
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChanger.class, this::onChangeTemperature)
                .onMessage(WeatherConditionsChanger.class, this::onChangeWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<EnvironmentCommand> onChangeTemperature(TemperatureChanger t) {
        System.out.println(t + " AH");

        this.temperature += -2 + (float)(Math.random() * (2 - -2)); // makes random number changes within a range (-2, 2)
        getContext().getLog().info("Environment received {}", temperature);
        // TODO: Handling of temperature change. Are sensors notified or do they read the temperature?
        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
        // TODO: Proccess w input correctly
        System.out.println(w.isSunny + " CH");
        // randomly changes weather
        Weather[] weathers = Weather.values();
        int i;
        do {
            i = (int) (Math.random() * (0 + weathers.length));
        } while (weathers[i] == isSunny);
        isSunny = weathers[i];
        getContext().getLog().info("Environment Change Sun to {}", isSunny);

        // TODO: Handling of weather change. Are sensors notified or do they read the weather information?
        return this;
    }


    private Environment onPostStop(){
        getContext().getLog().info("Environment actor stopped");
        return this;
    }
}


