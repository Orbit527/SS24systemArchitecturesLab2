package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

public class HomeAutomationController extends AbstractBehavior<Void>{
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private  ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherSensorCommand> weatherSensor;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private ActorRef<Environment.EnvironmentCommand> environment;

    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private  HomeAutomationController(ActorContext<Void> context) {
        super(context);
        this.airCondition = getContext().spawn(AirCondition.create("2", "1"), "AirCondition");

        this.blinds = getContext().spawn(Blinds.create("4", "1"), "blinds");
        this.mediaStation = getContext().spawn(MediaStation.create(this.blinds, "5", "1"), "mediaStation");

        this.environment = getContext().spawn(Environment.create(), "Environment");

        this.tempSensor = getContext().spawn(TemperatureSensor.create(this.environment, this.airCondition, "1", "1"), "temperatureSensor");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.environment, this.blinds, "3", "1"), "weatherSensor");

        this.fridge = getContext().spawn(Fridge.create("6", "1", 5, 10), "Fridge");

        ActorRef<Void> ui = getContext().spawn(UI.create(this.tempSensor, this.airCondition, this.environment, this.mediaStation, this.fridge), "UI");

        getContext().getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}
