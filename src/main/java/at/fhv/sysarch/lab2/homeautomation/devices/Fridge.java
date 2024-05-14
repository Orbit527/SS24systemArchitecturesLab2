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
import java.util.UUID;

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
        public double getPrice() {return price;}
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

    public static class ReceiptResponse implements FridgeCommand {
        final Product product;
        public ReceiptResponse(Product product) {this.product = product;}
    }

    public static class SubscribeProductCommand implements FridgeCommand {
        final Optional<String> productName;
        final Optional<Double> productPrice;
        final Optional<Double> productWeigth;

        public SubscribeProductCommand(Optional<String> productName, Optional<Double> productPrice, Optional<Double> productWeigth) {
            this.productName = productName;
            this.productPrice = productPrice;
            this.productWeigth = productWeigth;
        }
    }

    public static class UnsubscribeProductCommand implements FridgeCommand {
        final Optional<String> productName;
        public UnsubscribeProductCommand(Optional<String> productName) {this.productName = productName;}
    }

    public static class QueryOrdersCommand implements FridgeCommand {}


    private String groupId;
    private String deviceId;


    private ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;
    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;


    private int maxStorableProducts;
    private int maxWeightLoad;
    private ArrayList<Product> products = new ArrayList<>();

    private ArrayList<Product> subscribedProducts = new ArrayList<>();

    private ArrayList<Product> receipts = new ArrayList<>();

    private Fridge(ActorContext<FridgeCommand> context, String groupId, String deviceId, int maxStorableProducts, int maxWeightLoad) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        this.maxStorableProducts = maxStorableProducts;
        this.maxWeightLoad = maxWeightLoad;

        this.spaceSensor = getContext().spawn(SpaceSensor.create("7", "1", getContext().getSelf()), "SpaceSensor");
        this.weightSensor = getContext().spawn(WeightSensor.create("8", "1", getContext().getSelf()), "WeightSensor");

        getContext().getLog().info("Fridge started");


    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SpaceRequest.class, this::onSpaceRequest)
                .onMessage(WeightRequest.class, this::onWeightRequest)
                .onMessage(QueryProductsCommand.class, this::onQueryProductsRequest)
                .onMessage(ConsumeProductCommand.class, this::onConsumeProductCommand)
                .onMessage(OrderProductCommand.class, this::onOrderProductCommand)
                .onMessage(ReceiptResponse.class, this::onReceiptResponse)
                .onMessage(SubscribeProductCommand.class, this::onSubscribeProductCommand)
                .onMessage(UnsubscribeProductCommand.class, this::onUnsubscribeProductCommand)
                .onMessage(QueryOrdersCommand.class, this::onQueryOrdersCommand)

                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onQueryOrdersCommand(QueryOrdersCommand request) {
        int i = 1;
        String output = receipts.size() > 0 ? receipts.get(0).getName() : "";
        while (i < receipts.size()) {
            output = output + ", " + receipts.get(i).getName();
            i++;
        }
        getContext().getLog().info(output);
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onUnsubscribeProductCommand(UnsubscribeProductCommand command) {
        int i = 0;
        while(i < subscribedProducts.size()) {
            if(subscribedProducts.get(i).getName().equals(command.productName.get())) {
                subscribedProducts.remove(i);
                i = subscribedProducts.size();
            }
            i++;
        }
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onSubscribeProductCommand(SubscribeProductCommand command) {
        Product product = new Product(command.productName.get(), command.productPrice.get(), command.productWeigth.get());
        subscribedProducts.add(product);
        getContext().getLog().info(product.getName() + " added to subscription list");
        getContext().getSelf().tell(new OrderProductCommand(command.productName, command.productPrice, command.productWeigth));
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onReceiptResponse(ReceiptResponse response) {
        products.add(response.product);
        receipts.add(response.product);
        getContext().getLog().info(response.product.getWeight() + " kg of " + response.product.getName() + " ordered for " + response.product.getPrice());
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onOrderProductCommand(OrderProductCommand request) {
        Product product = new Product(request.productName.get(), request.productPrice.get(), request.productWeigth.get());
        ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor = getContext().spawn(OrderProcessor.create("9", "1", product, getContext().getSelf(), maxStorableProducts, maxWeightLoad, spaceSensor, weightSensor), UUID.randomUUID().toString());

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onConsumeProductCommand(ConsumeProductCommand request) {
        int i = 0;
        int j = 0;
        while( i < products.size()) {
            if (products.get(i).getName().equals(request.productName.get())) {
                Product product = products.get(i);
                getContext().getLog().info("Removed " + products.get(i).name + " from fridge");
                products.remove(i);
                spaceSensor.tell(new SpaceSensor.ProductsResponse(products));
                weightSensor.tell(new WeightSensor.ProductsResponse(products));
                while(j < subscribedProducts.size()) {
                    if((product.getName().equals(subscribedProducts.get(j).getName())) && (product.getPrice() == (subscribedProducts.get(j).getPrice())) && (product.getWeight() == (subscribedProducts.get(j).getWeight()))) {
                        getContext().getSelf().tell(new OrderProductCommand(Optional.ofNullable(product.getName()), Optional.of(product.getPrice()), Optional.of(product.getWeight())));
                        j = subscribedProducts.size();
                    }
                    j++;
                }
                i = products.size();
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


