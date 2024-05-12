package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<Environment.EnvironmentCommand> environment;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private ActorRef<Fridge.FridgeCommand> fridge;


    public static Behavior<Void> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<Environment.EnvironmentCommand> environment, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, environment, mediaStation, fridge));
    }

    private  UI(ActorContext<Void> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<Environment.EnvironmentCommand> environment, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.environment = environment;
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        new Thread(() -> { this.runCommandLine(); }).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        // TODO: Create Actor for UI Input-Handling?
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling
            String[] command = reader.split(" ");
            if(command[0].equals("t")) {
                this.tempSensor.tell(new TemperatureSensor.RequestTemperature());
            }
            if(command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("e") && command[1].equals("w")) {
                this.environment.tell(new Environment.WeatherConditionsChanger(Optional.of(Environment.Weather.valueOf(command[2]))));
            }
            if(command[0].equals("e") && command[1].equals("t")) {
                this.environment.tell(new Environment.TemperatureChanger(Optional.of(Double.valueOf(command[2]))));
            }
            if(command[0].equals("m") && command[1].equals("play")) {
                this.mediaStation.tell(new MediaStation.MediaStationPlayMovie(true));
            }
            if(command[0].equals("m") && command[1].equals("stop")) {
                this.mediaStation.tell(new MediaStation.MediaStationPlayMovie(false));
            }
            if((command[0].equals("m") && command[1].equals("true")) || (command[0].equals("m") && command[1].equals("false"))) {
                this.mediaStation.tell(new MediaStation.PowerMediaStationOn(Boolean.valueOf(command[1])));
            }
            // command: fridge query products
            if(command[0].equals("fridge") && command[1].equals("query") && command[2].equals("products")) {
                this.fridge.tell(new Fridge.QueryProductsCommand());
            }
            // command structure: fridge consume productName
            // command example: fridge consume Bacon
            if(command[0].equals("fridge") && command[1].equals("consume")) {
                this.fridge.tell(new Fridge.ConsumeProductCommand(Optional.of(String.valueOf(command[2]))));
            }
            // command structure: fridge order productName productPrice productWeight
            // command example: fridge order Bacon 15.99 0.8
            if(command[0].equals("fridge") && command[1].equals("order")) {
                this.fridge.tell(new Fridge.OrderProductCommand(Optional.of(String.valueOf(command[2])), Optional.of(Double.valueOf(command[3])), Optional.of(Double.valueOf(command[4]))));
            }
            // command structure: fridge subscribe productName productPrice productWeight
            // command example: fridge subscribe Bacon 15.99 0.8
            if(command[0].equals("fridge") && command[1].equals("subscribe")) {
                this.fridge.tell(new Fridge.SubscribeProductCommand(Optional.of(String.valueOf(command[2])), Optional.of(Double.valueOf(command[3])), Optional.of(Double.valueOf(command[4]))));
            }


                // TODO: process Input
        }
        getContext().getLog().info("UI done");
    }
}
