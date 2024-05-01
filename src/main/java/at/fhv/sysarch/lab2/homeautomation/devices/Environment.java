package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class TemperatureChanger implements EnvironmentCommand {

    }

    public static final class WeatherConditionsChanger implements EnvironmentCommand {
        final Optional<Boolean> isSunny;

        public WeatherConditionsChanger(Optional<Boolean> isSunny) {
            this.isSunny = isSunny;
        }
    }

    private double temperature = 20.0;
    private boolean isSunny = false;

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    // TODO: Provide the means for manually setting the temperature
    // TODO: Provide the means for manually setting the weather


    private ActorRef<WeatherSensor.WeatherSensorCommand> weatherSensor;


    public static Behavior<EnvironmentCommand> create(ActorRef<WeatherSensor.WeatherSensorCommand> weatherSensor){
        return Behaviors.setup(context ->  Behaviors.withTimers(timers -> new Environment(context, timers, timers, weatherSensor)));
    }

    private Environment(ActorContext<EnvironmentCommand> context,TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer, ActorRef<WeatherSensor.WeatherSensorCommand> weatherSensor) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(5));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.of(isSunny)), Duration.ofSeconds(3)); //TODO extend duration
        this.weatherSensor = weatherSensor;
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
        // TODO: Implement behavior for random changes to temperature

        Random r = new Random();
        int low = -10;
        int high = 10;
        int randomResult = r.nextInt(high-low) + low;
        this.temperature += (double) randomResult / 10;
        getContext().getLog().info("Environment received {}", temperature);

        // TODO: Handling of temperature change. Are sensors notified or do they read the temperature?
        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
        getContext().getLog().info("Environment Change Sun to {}", !isSunny);
        // TODO: Implement behavior for random changes to weather. Include more than just sunny and not sunny
        isSunny = !isSunny;
        // TODO: Handling of weather change. Are sensors notified or do they read the weather information?

        this.weatherSensor.tell(new WeatherSensor.ReadWeather(isSunny));

        return this;
    }


    private Environment onPostStop() {
        getContext().getLog().info("Environment actor stopped");
        return this;
    }
}


