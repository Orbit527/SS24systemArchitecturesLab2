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

    public static class CurrentSpaceResponse implements OrderProcessorCommand {
        public int productAmount;
        public CurrentSpaceResponse(int productAmount) {this.productAmount = productAmount;}
    }

    public static class CurrentWeightResponse implements OrderProcessorCommand {
        public double productWeight;
        public CurrentWeightResponse(double productWeight) {this.productWeight = productWeight;}
    }

    private String groupId;
    private String deviceId;

    private Fridge.Product product;

    private ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;
    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;

    private ActorRef<Fridge.FridgeCommand> fridge;
    private int maxStorableProducts;
    private int maxWeightLoad;

    private boolean storable = false;
    private boolean loadable = false;

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

        getContext().getLog().info("Order Processor started");

        spaceSensor.tell(new SpaceSensor.CurrentSpaceRequest("Asking for current Space taken", getContext().getSelf()));
        weightSensor.tell(new WeightSensor.CurrentWeightRequest("Asking for current Weight", getContext().getSelf()));
    }

    @Override
    public Receive<OrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CurrentSpaceResponse.class, this::onCurrentSpaceResponse)
                .onMessage(CurrentWeightResponse.class, this::onCurrentWeightResponse)

                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<OrderProcessorCommand> onCurrentWeightResponse(CurrentWeightResponse response) {
        System.out.println("Weight Test " + (response.productWeight + product.getWeight()));
        if(response.productWeight + product.getWeight() <= maxWeightLoad) {
            loadable = true;
        }
        if(storable == true && loadable == true) {
            //TODO: Make it order things
            System.out.println("MADE IT");
            fridge.tell(new Fridge.ReceiptResponse(product));
        }
        if(loadable == false) {
            getContext().getLog().info("Fridge has no Space for this product");
        }
        return Behaviors.same();
    }

    private Behavior<OrderProcessorCommand> onCurrentSpaceResponse(CurrentSpaceResponse response) {
        System.out.println("Space Test " + response.productAmount);
        if(response.productAmount + 1 <= maxStorableProducts) {
            storable = true;
        }
        if(storable == true && loadable == true) {
            //TODO: Make it order things
            System.out.println("MADE IT");
            fridge.tell(new Fridge.ReceiptResponse(product));
        }
        if(storable == false) {
            getContext().getLog().info("Fridge has no Space for this product");
        }
        return Behaviors.same();
    }

    //TODO: Figure out how to stop an actor
    private OrderProcessor onPostStop() {
        getContext().getLog().info("Order Processor actor stopped");
        return this;
    }

}
