package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import javax.naming.Context;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {}

    // Product Class start
    public class Product {
        private String name;
        private double price;
        private double weight;
        public Product(String name, double price, double weight) {
            this.name = name;
            this.price = price;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }
        public double getWeight() {return weight;}
    }
    // Product Class end

    public static Behavior<FridgeCommand> create(String groupId, String deviceId, int maxStorableProducts, int maxWeightLoad){
        return Behaviors.setup(context -> new Fridge(context, groupId, deviceId, maxStorableProducts, maxWeightLoad));
    }

    public static class SpaceRequest implements FridgeCommand {
        public final String query;
        public final ActorRef<SpaceSensor.SpaceSensorCommand> replyTo;

        public SpaceRequest(String query, ActorRef<SpaceSensor.SpaceSensorCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static class WeightRequest implements FridgeCommand {
        public final String query;
        public final ActorRef<WeightSensor.WeightSensorCommand> replyTo;

        public WeightRequest(String query, ActorRef<WeightSensor.WeightSensorCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static class QueryProductsCommand implements FridgeCommand {}

    public static final class ConsumeProductCommand implements FridgeCommand {
        final Optional<String> productName;

        public ConsumeProductCommand(Optional<String> productName) {
            this.productName = productName;
        }
    }

    public static final class OrderProductCommand implements FridgeCommand {
        final Optional<String> productName;
        final Optional<Double> productPrice;
        final Optional<Double> productWeigth;

        public OrderProductCommand(Optional<String> productName, Optional<Double> productPrice, Optional<Double> productWeigth) {
            this.productName = productName;
            this.productPrice = productPrice;
            this.productWeigth = productWeigth;
        }
    }


    private String groupId;
    private String deviceId;


    private ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;
    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;


    private int maxStorableProducts;
    private int maxWeightLoad;
    private ArrayList<Product> products = new ArrayList<>();

    private Fridge(ActorContext<FridgeCommand> context, String groupId, String deviceId, int maxStorableProducts, int maxWeightLoad) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        this.maxStorableProducts = maxStorableProducts;
        this.maxWeightLoad = maxWeightLoad;

        this.spaceSensor = getContext().spawn(SpaceSensor.create("7", "1", getContext().getSelf()), "SpaceSensor");
        this.weightSensor = getContext().spawn(WeightSensor.create("8", "1", getContext().getSelf()), "WeightSensor");


        products.add(new Product("Milk", 5, 5));
        products.add(new Product("Eggs", 5, 3));

        getContext().getLog().info("Fridge started");


    }

    // TODO: Add Method for ordering Products -> returns receipt
    public void addProduct(Product product) {
        products.add(product);
    }


    // TODO: Add Method for Querying history of orders
    // TODO: Add automatic ordering logic, if product runs out

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SpaceRequest.class, this::onSpaceRequest)
                .onMessage(WeightRequest.class, this::onWeightRequest)
                .onMessage(QueryProductsCommand.class, this::onQueryProductsRequest)
                .onMessage(ConsumeProductCommand.class, this::onConsumeProductCommand)
                .onMessage(OrderProductCommand.class, this::onOrderProductCommand)

                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onOrderProductCommand(OrderProductCommand request) {
        Product product = new Product(request.productName.get(), request.productPrice.get(), request.productWeigth.get());

        System.out.println(product.getName());
        //TODO: Create Order Processor, which handles ordering stuff
        ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor = getContext().spawn(OrderProcessor.create("9", "1", product, getContext().getSelf(), maxStorableProducts, maxWeightLoad, spaceSensor, weightSensor), "OrderProcessor");

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onConsumeProductCommand(ConsumeProductCommand request) {
        int i = 0;
        while( i < products.size()) {
            if (products.get(i).getName().equals(request.productName.get())) {
                getContext().getLog().info("Removed " + products.get(i).name + " from fridge");
                products.remove(i);
            }

            i++;
        }
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onQueryProductsRequest(QueryProductsCommand request) {
        int i = 1;
        String output = products.size() > 0 ? products.get(0).getName() : "";
        while (i < products.size()) {
            output = output + ", " + products.get(i).getName();
            i++;
        }
        getContext().getLog().info(output);
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onSpaceRequest(SpaceRequest request) {
        request.replyTo.tell(new SpaceSensor.ProductsResponse(products));
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onWeightRequest(WeightRequest request) {
        request.replyTo.tell(new WeightSensor.ProductsResponse(products));
        return Behaviors.same();
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor stopped");
        return this;
    }
}


