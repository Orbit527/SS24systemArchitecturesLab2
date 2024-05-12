package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.OrderProcessorCommand> {

    public interface OrderProcessorCommand {}

    public static Behavior<OrderProcessor.OrderProcessorCommand> create(String groupId, String deviceId, Fridge.Product product, ActorRef<Fridge.FridgeCommand> fridge, int maxStorableProducts, int maxWeightLoad, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor, ActorRef<WeightSensor.WeightSensorCommand> weightSensor){
        return Behaviors.setup(context -> new OrderProcessor(context, groupId, deviceId, product, fridge, maxStorableProducts, maxWeightLoad, spaceSensor, weightSensor));
    }

    private String groupId;
    private String deviceId;

    private Fridge.Product product;

    //TODO: Actor Refs
    private ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;
    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;

    private ActorRef<Fridge.FridgeCommand> fridge;
    private int maxStorableProducts;
    private int maxWeightLoad;

    private OrderProcessor(ActorContext<OrderProcessor.OrderProcessorCommand> context, String groupId, String deviceId, Fridge.Product product, ActorRef<Fridge.FridgeCommand> fridge, int maxStorableProducts, int maxWeightLoad, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor, ActorRef<WeightSensor.WeightSensorCommand> weightSensor) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.product = product;
        this.fridge = fridge;
        this.maxStorableProducts = maxStorableProducts;
        this.maxWeightLoad = maxWeightLoad;
        this.spaceSensor = spaceSensor;
        this.weightSensor = weightSensor;

        System.out.println("FOR THE ORDER " + product.getName() + " " + maxStorableProducts);

        getContext().getLog().info("Order Processor started");

    }

    @Override
    public Receive<OrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    //TODO: Figure out how to stop an actor
    private OrderProcessor onPostStop() {
        getContext().getLog().info("Order Processor actor stopped");
        return this;
    }

}
