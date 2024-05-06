package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {

    public interface TemperatureCommand {}

    public static final class RequestTemperature implements TemperatureCommand {

        public RequestTemperature() {

        }
    }

    public static final class ResponseTemperature implements TemperatureCommand {
        final Optional<Double> value;

        public ResponseTemperature(Optional<Double> value) {
            this.value = value;
        }
    }

    public static Behavior<TemperatureCommand> create(ActorRef<Environment.EnvironmentCommand> environment, ActorRef<AirCondition.AirConditionCommand> airCondition, String groupId, String deviceId) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new TemperatureSensor(context, timers, environment, airCondition, groupId, deviceId)));
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<Environment.EnvironmentCommand> environment;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;


    public TemperatureSensor(ActorContext<TemperatureCommand> context, TimerScheduler<TemperatureCommand> tempTimer,  ActorRef<Environment.EnvironmentCommand> environment, ActorRef<AirCondition.AirConditionCommand> airCondition, String groupId, String deviceId) {
        super(context);
        tempTimer.startTimerAtFixedRate(new RequestTemperature(), Duration.ofSeconds(5));
        this.environment = environment;
        this.airCondition = airCondition;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("TemperatureSensor started");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestTemperature.class, this::onRequestTemperature)
                .onMessage(ResponseTemperature.class, this::onResponseTemperature)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private Behavior<TemperatureCommand> onRequestTemperature(RequestTemperature r) {

        environment.tell(new Environment.Request("TemperatureSensor request from Environment", this.getContext().getSelf()));
        return this;
    }

    private Behavior<TemperatureCommand> onResponseTemperature(ResponseTemperature r) {

        getContext().getLog().info("TemperatureSensor received from Environment {}", r.value.get());

        //send received Temp to AC
        this.airCondition.tell(new AirCondition.EnrichedTemperature(r.value, Optional.of("Celsius")));

        return this;
    }


    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
