package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.ArrayList;

public class WeightSensor extends AbstractBehavior<WeightSensor.WeightSensorCommand> {

    public interface WeightSensorCommand {}

    public static Behavior<WeightSensor.WeightSensorCommand> create(String groupId, String deviceId, ActorRef<Fridge.FridgeCommand> fridge){
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new WeightSensor(context, groupId, deviceId, fridge, timers)));
    }

    public static class RequestProducts implements WeightSensorCommand {}

    public static class ProductsResponse implements WeightSensorCommand {
        public final ArrayList<Fridge.Product> result;
        public ProductsResponse(ArrayList<Fridge.Product> result) {
            this.result = result;
        }
    }

    public static class CurrentWeightRequest implements WeightSensorCommand {
        public final String query;
        public final ActorRef<OrderProcessor.OrderProcessorCommand> replyTo;
        public CurrentWeightRequest(String query, ActorRef<OrderProcessor.OrderProcessorCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    private String groupId;
    private String deviceId;
    private ActorRef<Fridge.FridgeCommand> fridge;

    private double productWeight;

    private final TimerScheduler<WeightSensorCommand> fridgeTimeScheduler;

    private WeightSensor(ActorContext<WeightSensor.WeightSensorCommand> context, String groupId, String deviceId, ActorRef<Fridge.FridgeCommand> fridge, TimerScheduler<WeightSensorCommand> fridgeTimeScheduler) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.fridge = fridge;
        this.fridgeTimeScheduler = fridgeTimeScheduler;
        fridgeTimeScheduler.startTimerAtFixedRate(new RequestProducts(), Duration.ofSeconds(5));

        getContext().getLog().info("Weight Sensor started");
    }

    @Override
    public Receive<WeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProductsResponse.class, this::onProductsResponse)
                .onMessage(RequestProducts.class, this::onRequestProducts)
                .onMessage(CurrentWeightRequest.class, this::onCurrentWeightRequest)

                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeightSensorCommand> onCurrentWeightRequest(CurrentWeightRequest request) {
        request.replyTo.tell(new OrderProcessor.CurrentWeightResponse(productWeight));
        return Behaviors.same();
    }

    private Behavior<WeightSensorCommand> onRequestProducts(RequestProducts response) {
        fridge.tell(new Fridge.WeightRequest("Asking Fridge for Product List", this.getContext().getSelf()));
        return Behaviors.same();
    }

    private Behavior<WeightSensorCommand> onProductsResponse(ProductsResponse response) {
        int i = 0;
        double temp = 0;
        while (i < response.result.size()) {
            temp = temp + response.result.get(i).getWeight();
            i++;
        }
        productWeight = temp;
        getContext().getLog().debug("Products in fridge weigh " + productWeight + " kg");
        return Behaviors.same();
    }

    private Behavior<WeightSensorCommand> onPostStop() {
        getContext().getLog().info("Weight Sensor actor stopped");
        return this;
    }

}
