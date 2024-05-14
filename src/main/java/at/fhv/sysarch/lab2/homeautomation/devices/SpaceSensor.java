package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.ArrayList;

public class SpaceSensor extends AbstractBehavior<SpaceSensor.SpaceSensorCommand> {

    public interface SpaceSensorCommand {}

    public static Behavior<SpaceSensor.SpaceSensorCommand> create(String groupId, String deviceId, ActorRef<Fridge.FridgeCommand> fridge){
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new SpaceSensor(context, groupId, deviceId, fridge, timers)));
    }

    public static class RequestProducts implements SpaceSensorCommand {}

    public static class ProductsResponse implements SpaceSensorCommand {
        public final ArrayList<Fridge.Product> result;
        public ProductsResponse(ArrayList<Fridge.Product> result) {
            this.result = result;
        }
    }

    public static class CurrentSpaceRequest implements SpaceSensorCommand {
        public final String query;
        public final ActorRef<OrderProcessor.OrderProcessorCommand> replyTo;
        public CurrentSpaceRequest(String query, ActorRef<OrderProcessor.OrderProcessorCommand> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    private String groupId;
    private String deviceId;
    private ActorRef<Fridge.FridgeCommand> fridge;

    private int productAmount;

    private final TimerScheduler<SpaceSensorCommand> fridgeTimeScheduler;

    private SpaceSensor(ActorContext<SpaceSensor.SpaceSensorCommand> context, String groupId, String deviceId, ActorRef<Fridge.FridgeCommand> fridge, TimerScheduler<SpaceSensorCommand> fridgeTimeScheduler) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.fridge = fridge;
        this.fridgeTimeScheduler = fridgeTimeScheduler;
        fridgeTimeScheduler.startTimerAtFixedRate(new RequestProducts(), Duration.ofSeconds(5));

        getContext().getLog().info("Space Sensor started");
    }

    @Override
    public Receive<SpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProductsResponse.class, this::onProductsResponse)
                .onMessage(RequestProducts.class, this::onRequestProducts)
                .onMessage(CurrentSpaceRequest.class, this::onCurrentSpaceRequest)

                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<SpaceSensorCommand> onCurrentSpaceRequest (CurrentSpaceRequest request) {
        request.replyTo.tell(new OrderProcessor.CurrentSpaceResponse(productAmount));
        return Behaviors.same();
    }

    private Behavior<SpaceSensorCommand> onRequestProducts(RequestProducts response) {
        fridge.tell(new Fridge.SpaceRequest("Asking Fridge for Product List", this.getContext().getSelf()));
        return Behaviors.same();
    }

    private Behavior<SpaceSensorCommand> onProductsResponse(ProductsResponse response) {
        productAmount = response.result.size();
        getContext().getLog().debug("Products are taking up " + response.result.size() + " space in fridge");
        return Behaviors.same();
    }

    private Behavior<SpaceSensorCommand> onPostStop() {
        getContext().getLog().info("Space Sensor actor stopped");
        return this;
    }

}
