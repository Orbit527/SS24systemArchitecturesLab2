package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

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
    }
    // Product Class end

    public static Behavior<FridgeCommand> create(String groupId, String deviceId){
        return Behaviors.setup(context -> new Fridge(context, groupId, deviceId));
    }

    private String groupId;
    private String deviceId;

    // TODO:
    private int maxStorableProducts;
    private int maxWeightLoad;
    private ArrayList<Product> products = new ArrayList<>();

    private Fridge(ActorContext<FridgeCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        products.add(new Product("Milk", 5, 5));
        products.add(new Product("Eggs", 5, 5));
        getStoredProducts();

        getContext().getLog().info("Fridge started");


    }

    // TODO: Add Method for consuming Products
    // TODO: Add Method for ordering Products -> returns receipt
    public void addProduct(Product product) {
        products.add(product);
    }

    // TODO: Add Method for Querying stored products - unfinished right meow -> make a proper UI call
    public void getStoredProducts() {
        int i = 0;
        while (i < products.size()) {
            System.out.println(products.get(i).getName());
            i++;
        }
    }
    // TODO: Add Method for Querying history of orders
    // TODO: Add automatic ordering logic, if product runs out

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor stopped");
        return this;
    }
}


